<template>
  <scroll-view scroll-y class="page-shell product-page">
    <scroll-view scroll-x class="media-strip" v-if="detail.media?.length">
      <image
        v-for="media in detail.media"
        :key="media.id"
        :src="media.mediaUrl"
        mode="aspectFill"
        class="media-strip__image"
      />
    </scroll-view>
    <image v-else :src="detail.coverImage" mode="aspectFill" class="detail-cover" />

    <view class="detail-panel">
      <view class="detail-header">
        <view class="detail-header__tags">
          <text v-for="tag in detail.tags?.slice(0, 3)" :key="tag" class="detail-tag">{{ tag }}</text>
          <text v-if="detail.supportCustom" class="detail-tag detail-tag--custom">支持轻定制</text>
        </view>
        <text class="detail-title">{{ detail.name }}</text>
        <text class="detail-subtitle">{{ detail.subtitle }}</text>
        <view class="price-row">
          <text class="price-row__price">{{ formatMoney(selectedSku?.salePrice || detail.price) }}</text>
          <text class="price-row__tag">证书可追溯</text>
        </view>
      </view>

      <view class="detail-section">
        <text class="detail-section__label">规格与库存</text>
        <view class="sku-grid">
          <view
            v-for="sku in detail.skus"
            :key="sku.id"
            class="sku-chip"
            :class="{ 'sku-chip--active': selectedSku?.id === sku.id, 'sku-chip--disabled': sku.stock <= 0 }"
            @tap="chooseSku(sku)"
          >
            <view>
              <text class="sku-chip__main">{{ sku.material }}</text>
              <text class="sku-chip__sub">{{ sku.ringSize }} / {{ sku.weightDesc }}</text>
            </view>
            <text class="sku-chip__stock">库存 {{ sku.stock }}</text>
          </view>
        </view>
      </view>

      <view class="detail-section" v-if="detail.supportCustom">
        <text class="detail-section__label">轻定制备注</text>
        <input v-model="customForm.engravingText" class="form-input" placeholder="刻字内容，例如 FOREVER" />
        <input v-model="customForm.sizeRemark" class="form-input" placeholder="尺寸备注，例如 12 号圈" />
        <input v-model="customForm.materialRemark" class="form-input" placeholder="材质偏好，例如 18K 金" />
      </view>

      <view class="promise-grid">
        <view class="promise-item">
          <text class="promise-item__title">核心卖点</text>
          <text class="promise-item__value">{{ detail.description || "精选材质与东方线条，适合日常佩戴与礼赠。" }}</text>
        </view>
        <view class="promise-item">
          <text class="promise-item__title">证书材质</text>
          <text class="promise-item__value">{{ detail.certificateInfo || "支持材质说明与证书信息展示。" }}</text>
        </view>
        <view class="promise-item">
          <text class="promise-item__title">配送售后</text>
          <text class="promise-item__value">{{ detail.serviceInfo || "礼盒包装，顺丰保价配送，客服跟进售后。" }}</text>
        </view>
      </view>

      <view class="consult-card">
        <text class="consult-card__title">不确定圈口或搭配？</text>
        <text class="consult-card__desc">提交轻定制需求后，客服会人工确认刻字、尺寸与材质偏好。</text>
      </view>
    </view>

    <view class="sticky-actions">
      <button class="ghost-button sticky-actions__small" @tap="toggleFavorite">
        {{ detail.favorited ? "已收藏" : "收藏" }}
      </button>
      <button class="ghost-button sticky-actions__small" @tap="submitCustomRequest">咨询</button>
      <button class="pill-button sticky-actions__main" @tap="addToCart">加入购物车</button>
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
import { onLoad } from "@dcloudio/uni-app";
import { reactive, ref } from "vue";
import { api } from "../../services/api";
import { sessionState } from "../../services/session";
import { formatMoney } from "../../utils/format";

const detail = reactive<any>({
  media: [],
  skus: [],
});
const selectedSku = ref<any>(null);
const customForm = reactive({
  engravingText: "",
  sizeRemark: "",
  materialRemark: "",
});

onLoad(async (query) => {
  Object.assign(detail, await api.getProductDetail(Number(query?.productId || 0)));
  selectedSku.value = detail.skus?.[0] || null;
});

function chooseSku(sku: any) {
  if (sku.stock <= 0) {
    uni.showToast({ title: "该规格暂时缺货", icon: "none" });
    return;
  }
  selectedSku.value = sku;
}

async function addToCart() {
  if (!selectedSku.value) {
    uni.showToast({ title: "请选择规格", icon: "none" });
    return;
  }
  if (selectedSku.value.stock <= 0) {
    uni.showToast({ title: "库存不足", icon: "none" });
    return;
  }
  await api.addCartItem({
    skuId: selectedSku.value.id,
    quantity: 1,
    ...customForm,
  });
  uni.showToast({ title: "已加入购物车", icon: "success" });
}

async function submitCustomRequest() {
  if (!selectedSku.value) {
    uni.showToast({ title: "请先选择规格", icon: "none" });
    return;
  }
  await api.submitCustomRequest({
    productId: detail.id,
    contactName: sessionState.user?.nickname || "珠宝用户",
    contactPhone: sessionState.user?.phone || "13800138000",
    remark: "来自商品详情页的轻定制咨询",
    ...customForm,
  });
  uni.showToast({ title: "顾问会尽快联系你", icon: "none" });
}

async function toggleFavorite() {
  const favoriteState = await api.toggleFavorite(detail.id);
  detail.favorited = favoriteState.favorited;
  uni.showToast({
    title: favoriteState.favorited ? "已加入收藏" : "已取消收藏",
    icon: "none",
  });
}
</script>

<style scoped lang="scss">
.product-page {
  padding-bottom: 132rpx;
}

.media-strip {
  white-space: nowrap;
  padding: 22rpx 22rpx 0;
}

.media-strip__image {
  display: inline-block;
  width: 610rpx;
  height: 610rpx;
  margin-right: 18rpx;
  border-radius: 30rpx;
  background: #eee8da;
}

.detail-cover {
  width: calc(100% - 44rpx);
  height: 610rpx;
  margin: 22rpx;
  border-radius: 30rpx;
}

.detail-panel {
  margin: 20rpx 22rpx 0;
  padding: 30rpx 26rpx 36rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14rpx 30rpx rgba(82, 101, 70, 0.08);
}

.detail-title,
.detail-subtitle,
.price-row__price,
.detail-section__label,
.sku-chip__main,
.sku-chip__sub,
.sku-chip__stock,
.promise-item__title,
.promise-item__value,
.consult-card__title,
.consult-card__desc {
  display: block;
}

.detail-header__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.detail-tag {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #edf4e8;
  color: #5d7653;
  font-size: 20rpx;
}

.detail-tag--custom {
  background: #fff3d9;
  color: #8a682a;
}

.detail-title {
  margin-top: 18rpx;
  font-size: 42rpx;
  line-height: 1.35;
  color: #27231c;
}

.detail-subtitle {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #7f8778;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 22rpx;
}

.price-row__price {
  font-size: 46rpx;
  color: #47633f;
}

.price-row__tag {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #edf4e8;
  font-size: 22rpx;
  color: #617b56;
}

.detail-section {
  margin-top: 34rpx;
}

.detail-section__label {
  font-size: 30rpx;
  color: #31422f;
}

.sku-grid {
  display: grid;
  gap: 14rpx;
  margin-top: 18rpx;
}

.sku-chip {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  padding: 22rpx;
  border-radius: 22rpx;
  background: #fffdf8;
  border: 1rpx solid rgba(181, 191, 171, 0.9);
}

.sku-chip--active {
  background: #edf4e8;
  border-color: #6e8d63;
}

.sku-chip--disabled {
  opacity: 0.48;
}

.sku-chip__main {
  font-size: 26rpx;
  color: #2e3429;
}

.sku-chip__sub,
.sku-chip__stock {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #858d7d;
}

.form-input {
  width: 100%;
  margin-top: 14rpx;
  padding: 20rpx 18rpx;
  border-radius: 18rpx;
  background: #fffdf8;
  border: 1rpx solid rgba(181, 191, 171, 0.9);
  font-size: 24rpx;
}

.promise-grid {
  display: grid;
  gap: 16rpx;
  margin-top: 34rpx;
}

.promise-item {
  padding: 22rpx;
  border-radius: 22rpx;
  background: #f8f6ee;
}

.promise-item__title {
  font-size: 26rpx;
  color: #31422f;
}

.promise-item__value {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.75;
  color: #747b6e;
}

.consult-card {
  margin-top: 22rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #edf4e8, #fff8e8);
}

.consult-card__title {
  font-size: 28rpx;
  color: #31422f;
}

.consult-card__desc {
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 1.6;
  color: #7a806f;
}

.sticky-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  gap: 14rpx;
  padding: 18rpx 22rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 253, 248, 0.96);
  box-shadow: 0 -10rpx 24rpx rgba(68, 82, 59, 0.08);
}

.sticky-actions__small {
  width: 150rpx;
  height: 74rpx;
  line-height: 74rpx;
}

.sticky-actions__main {
  flex: 1;
}
</style>
