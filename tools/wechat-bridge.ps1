param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("state", "activate", "screenshot", "click", "rightclick", "paste", "clickpaste", "hotkey", "text")]
    [string]$Action,

    [string]$Name = "wechat-window",
    [int]$X = 0,
    [int]$Y = 0,
    [string]$Text = "",
    [string]$Keys = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class WeChatBridgeWin32 {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);

    [DllImport("user32.dll")]
    public static extern bool SetProcessDPIAware();

    [DllImport("user32.dll")]
    public static extern bool ShowWindowAsync(IntPtr hWnd, int nCmdShow);

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);

    [DllImport("user32.dll")]
    public static extern bool SetCursorPos(int X, int Y);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint dwData, UIntPtr dwExtraInfo);

    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

    public const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    public const uint MOUSEEVENTF_LEFTUP = 0x0004;
    public const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
    public const uint MOUSEEVENTF_RIGHTUP = 0x0010;
    public const uint KEYEVENTF_KEYUP = 0x0002;
    public const uint SWP_NOMOVE = 0x0002;
    public const uint SWP_NOSIZE = 0x0001;
    public const uint SWP_SHOWWINDOW = 0x0040;
}
"@

[WeChatBridgeWin32]::SetProcessDPIAware() | Out-Null

function Get-WeChatWindow {
    $visibleWindows = Get-Process Weixin |
        Where-Object { $_.MainWindowTitle -and $_.MainWindowTitle.Trim() -ne "" }
    $window = $visibleWindows |
        Where-Object { $_.MainWindowTitle.Trim() -eq "微信" } |
        Select-Object -First 1
    if (-not $window) {
        $window = $visibleWindows | Select-Object -First 1
    }
    if (-not $window) {
        throw "No visible WeChat main window found."
    }
    return $window
}

function Get-WeChatRect {
    param(
        [Parameter(Mandatory = $true)]
        $Window
    )

    $rect = New-Object WeChatBridgeWin32+RECT
    $ok = [WeChatBridgeWin32]::GetWindowRect($Window.MainWindowHandle, [ref]$rect)
    if (-not $ok) {
        throw "Failed to read WeChat window bounds."
    }
    return [pscustomobject]@{
        Left = $rect.Left
        Top = $rect.Top
        Right = $rect.Right
        Bottom = $rect.Bottom
        Width = $rect.Right - $rect.Left
        Height = $rect.Bottom - $rect.Top
    }
}

function Activate-WeChatWindow {
    param(
        [Parameter(Mandatory = $true)]
        $Window
    )

    [WeChatBridgeWin32]::ShowWindowAsync($Window.MainWindowHandle, 9) | Out-Null
    Start-Sleep -Milliseconds 500
    [WeChatBridgeWin32]::SetWindowPos(
        $Window.MainWindowHandle,
        [IntPtr](-1),
        0,
        0,
        0,
        0,
        [WeChatBridgeWin32]::SWP_NOMOVE -bor [WeChatBridgeWin32]::SWP_NOSIZE -bor [WeChatBridgeWin32]::SWP_SHOWWINDOW
    ) | Out-Null
    Start-Sleep -Milliseconds 250
    [WeChatBridgeWin32]::SetForegroundWindow($Window.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 250
    [WeChatBridgeWin32]::SetWindowPos(
        $Window.MainWindowHandle,
        [IntPtr](-2),
        0,
        0,
        0,
        0,
        [WeChatBridgeWin32]::SWP_NOMOVE -bor [WeChatBridgeWin32]::SWP_NOSIZE -bor [WeChatBridgeWin32]::SWP_SHOWWINDOW
    ) | Out-Null
    Start-Sleep -Milliseconds 300
}

function Save-WeChatScreenshot {
    param(
        [Parameter(Mandatory = $true)]
        $Rect,
        [Parameter(Mandatory = $true)]
        [string]$OutputPath
    )

    $bitmap = New-Object System.Drawing.Bitmap $Rect.Width, $Rect.Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.CopyFromScreen($Rect.Left, $Rect.Top, 0, 0, $bitmap.Size)
    $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}

function Click-WeChatRelative {
    param(
        [Parameter(Mandatory = $true)]
        $Rect,
        [int]$OffsetX,
        [int]$OffsetY
    )

    $absoluteX = $Rect.Left + $OffsetX
    $absoluteY = $Rect.Top + $OffsetY
    [WeChatBridgeWin32]::SetCursorPos($absoluteX, $absoluteY) | Out-Null
    Start-Sleep -Milliseconds 120
    [WeChatBridgeWin32]::mouse_event([WeChatBridgeWin32]::MOUSEEVENTF_LEFTDOWN, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 60
    [WeChatBridgeWin32]::mouse_event([WeChatBridgeWin32]::MOUSEEVENTF_LEFTUP, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 250
}

function RightClick-WeChatRelative {
    param(
        [Parameter(Mandatory = $true)]
        $Rect,
        [int]$OffsetX,
        [int]$OffsetY
    )

    $absoluteX = $Rect.Left + $OffsetX
    $absoluteY = $Rect.Top + $OffsetY
    [WeChatBridgeWin32]::SetCursorPos($absoluteX, $absoluteY) | Out-Null
    Start-Sleep -Milliseconds 120
    [WeChatBridgeWin32]::mouse_event([WeChatBridgeWin32]::MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 60
    [WeChatBridgeWin32]::mouse_event([WeChatBridgeWin32]::MOUSEEVENTF_RIGHTUP, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 300
}

function Send-WeChatKeys {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    [System.Windows.Forms.SendKeys]::SendWait($Value)
    Start-Sleep -Milliseconds 300
}

function Press-Key {
    param(
        [Parameter(Mandatory = $true)]
        [byte]$VirtualKey
    )

    [WeChatBridgeWin32]::keybd_event($VirtualKey, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 60
    [WeChatBridgeWin32]::keybd_event($VirtualKey, 0, [WeChatBridgeWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
}

function Press-KeyCombo {
    param(
        [Parameter(Mandatory = $true)]
        [byte]$Modifier,
        [Parameter(Mandatory = $true)]
        [byte]$VirtualKey
    )

    [WeChatBridgeWin32]::keybd_event($Modifier, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [WeChatBridgeWin32]::keybd_event($VirtualKey, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [WeChatBridgeWin32]::keybd_event($VirtualKey, 0, [WeChatBridgeWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [WeChatBridgeWin32]::keybd_event($Modifier, 0, [WeChatBridgeWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 120
}

function Paste-WeChatText {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    [System.Windows.Forms.Clipboard]::SetText($Value)
    Start-Sleep -Milliseconds 250
    Press-KeyCombo -Modifier 0x11 -VirtualKey 0x41
    Press-Key -VirtualKey 0x08
    Press-KeyCombo -Modifier 0x11 -VirtualKey 0x56
    Start-Sleep -Milliseconds 400
}

$window = Get-WeChatWindow

switch ($Action) {
    "state" {
        $rect = Get-WeChatRect -Window $window
        [pscustomobject]@{
            ProcessId = $window.Id
            Title = $window.MainWindowTitle
            Left = $rect.Left
            Top = $rect.Top
            Width = $rect.Width
            Height = $rect.Height
        } | ConvertTo-Json -Depth 2
    }
    "activate" {
        Activate-WeChatWindow -Window $window
        $rect = Get-WeChatRect -Window $window
        [pscustomobject]@{
            Activated = $true
            Left = $rect.Left
            Top = $rect.Top
            Width = $rect.Width
            Height = $rect.Height
        } | ConvertTo-Json -Depth 2
    }
    "screenshot" {
        Activate-WeChatWindow -Window $window
        $rect = Get-WeChatRect -Window $window
        $dir = Join-Path (Get-Location) "runtime-logs"
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir | Out-Null
        }
        $outputPath = Join-Path $dir ($Name + ".png")
        Save-WeChatScreenshot -Rect $rect -OutputPath $outputPath
        $outputPath
    }
    "click" {
        Activate-WeChatWindow -Window $window
        $rect = Get-WeChatRect -Window $window
        Click-WeChatRelative -Rect $rect -OffsetX $X -OffsetY $Y
        [pscustomobject]@{
            Clicked = $true
            X = $X
            Y = $Y
        } | ConvertTo-Json -Depth 2
    }
    "rightclick" {
        Activate-WeChatWindow -Window $window
        $rect = Get-WeChatRect -Window $window
        RightClick-WeChatRelative -Rect $rect -OffsetX $X -OffsetY $Y
        [pscustomobject]@{
            RightClicked = $true
            X = $X
            Y = $Y
        } | ConvertTo-Json -Depth 2
    }
    "paste" {
        if (-not $Text) {
            throw "paste action requires -Text."
        }
        Activate-WeChatWindow -Window $window
        Paste-WeChatText -Value $Text
        [pscustomobject]@{
            Pasted = $true
            Length = $Text.Length
        } | ConvertTo-Json -Depth 2
    }
    "clickpaste" {
        if (-not $Text) {
            throw "clickpaste action requires -Text."
        }
        Activate-WeChatWindow -Window $window
        $rect = Get-WeChatRect -Window $window
        Click-WeChatRelative -Rect $rect -OffsetX $X -OffsetY $Y
        Start-Sleep -Milliseconds 200
        Paste-WeChatText -Value $Text
        [pscustomobject]@{
            ClickPasted = $true
            X = $X
            Y = $Y
            Length = $Text.Length
        } | ConvertTo-Json -Depth 2
    }
    "hotkey" {
        if (-not $Keys) {
            throw "hotkey action requires -Keys."
        }
        Activate-WeChatWindow -Window $window
        Send-WeChatKeys -Value $Keys
        [pscustomobject]@{
            Sent = $true
            Keys = $Keys
        } | ConvertTo-Json -Depth 2
    }
    "text" {
        if (-not $Text) {
            throw "text action requires -Text."
        }
        Activate-WeChatWindow -Window $window
        [System.Windows.Forms.SendKeys]::SendWait($Text)
        Start-Sleep -Milliseconds 300
        [pscustomobject]@{
            Sent = $true
            Length = $Text.Length
        } | ConvertTo-Json -Depth 2
    }
}
