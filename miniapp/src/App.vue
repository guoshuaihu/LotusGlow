<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";
import { ensureSession } from "./services/session";

onLaunch(() => {
  ensureSession();
});
</script>

<style lang="scss">
page {
  background: #f6f2ea;
  color: #27231c;
  font-family: "Songti SC", "STSong", "Noto Serif SC", serif;
}

view,
text,
button,
input,
textarea,
image,
scroll-view {
  box-sizing: border-box;
}

button::after {
  border: none;
}

.page-shell {
  min-height: 100vh;
  background:
    linear-gradient(135deg, rgba(220, 231, 216, 0.8) 0%, transparent 30%),
    linear-gradient(180deg, #fffdf8 0%, #f7f3eb 52%, #efe8db 100%);
}

.glass-card {
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.9);
  border: 1rpx solid rgba(174, 187, 160, 0.5);
  box-shadow: 0 14rpx 34rpx rgba(71, 84, 58, 0.08);
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 22rpx;
}

.section-heading__title {
  display: inline-block;
  position: relative;
  font-size: 34rpx;
  line-height: 1.35;
  font-weight: 600;
  color: #31422f;
}

.section-heading__title::after {
  content: "";
  display: block;
  width: 72rpx;
  height: 6rpx;
  margin-top: 10rpx;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #5f7d58, #c7d7bb);
}

.section-heading__subtitle {
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.55;
  color: #87917d;
}

.pill-button {
  border: none;
  border-radius: 999rpx;
  padding: 0 32rpx;
  height: 74rpx;
  line-height: 74rpx;
  background: linear-gradient(135deg, #2d3c2c, #799265);
  color: #fffdf7;
  font-size: 26rpx;
  box-shadow: 0 12rpx 24rpx rgba(86, 112, 76, 0.22);
}

.ghost-button {
  border-radius: 999rpx;
  border: 1rpx solid rgba(93, 119, 81, 0.42);
  background: #fffdf8;
  color: #536b4b;
  font-size: 24rpx;
}
</style>
