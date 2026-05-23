package com.zyx.jewelry.content.controller;

import com.zyx.jewelry.common.ApiResponse;
import com.zyx.jewelry.content.service.HomeContentService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class HomeContentController {

    private final HomeContentService homeContentService;

    @GetMapping("/home")
    public ApiResponse<Map<String, Object>> home() {
        return ApiResponse.success(homeContentService.loadHomePage());
    }
}
