<template>
  <scroll-view scroll-y class="page-shell cart-page">
    <view class="cart-page__header">
      <text class="cart-page__eyebrow">ORDER CHECKOUT</text>
      <text class="cart-page__title">购物车与结算</text>
      <text class="cart-page__subtitle">提交前确认规格、刻字备注、收货地址与库存状态。</text>
    </view>

    <view v-if="cart.items.length" class="cart-list">
      <view v-for="item in cart.items" :key="item.id" class="cart-item glass-card">
        <image :src="item.coverImage" mode="aspectFill" class="cart-item__image" />
        <view class="cart-item__body">
          <text class="cart-item__title">{{ item.productName }}</text>
          <text class="cart-item__summary">{{ item.skuSummary }}</text>
          <text class="cart-item__remark" v-if="item.engravingText">刻字：{{ item.engravingText }}</text>
          <view class="cart-item__footer">
            <text class="cart-item__price">{{ formatMoney(item.price) }}</text>
            <text class="cart-item__qty">x{{ item.quantity }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="empty-card glass-card">
      <text class="empty-card__title">购物车还是空的</text>
      <text class="empty-card__desc">先去甄选珠宝里挑一件适合纪念日、通勤或礼赠的作品。</text>
      <button class="pill-button" @tap="goShopping">去选购</button>
    </view>

    <view class="address-card glass-card">
      <view class="section-heading">
        <view>
          <text class="section-heading__title">收货地址</text>
          <view class="section-heading__subtitle">V2 先支持快递发货，到店自提留到后续版本。</view>
        </view>
      </view>
      <view v-if="selectedAddress" class="selected-address">
        <text>{{ selectedAddress.receiverName }} {{ selectedAddress.receiverPhone }}</text>
        <text>{{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.detailAddress }}</text>
      </view>
      <view class="address-form">
        <input v-model="addressForm.receiverName" class="form-input" placeholder="收件人" />
        <input v-model="addressForm.receiverPhone" class="form-input" placeholder="手机号" />
        <input v-model="addressForm.province" class="form-input" placeholder="省份" />
        <input v-model="addressForm.city" class="form-input" placeholder="城市" />
        <input v-model="addressForm.district" class="form-input" placeholder="区县" />
        <input v-model="addressForm.detailAddress" class="form-input" placeholder="详细地址" />
        <button class="ghost-button address-form__button" @tap="saveAddress">保存并设为默认</button>
      </view>
    </view>

    <view class="checkout-card glass-card">
      <view class="checkout-card__row">
        <text>商品合计</text>
        <text>{{ formatMoney(cart.totalAmount || 0) }}</text>
      </view>
      <view class="checkout-card__row">
        <text>配送方式</text>
        <text>顺丰保价发货</text>
      </view>
      <view class="checkout-card__notice">真实支付前会再次校验库存、价格快照与地址完整性。</view>
    </view>

    <view class="checkout-bar glass-card">
      <view>
        <text class="checkout-bar__label">应付</text>
        <text class="checkout-bar__price">{{ formatMoney(cart.totalAmount || 0) }}</text>
      </view>
      <button class="pill-button checkout-bar__button" @tap="checkout">提交订单</button>
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { api } from "../../services/api";
import { formatMoney } from "../../utils/format";

const cart = reactive<any>({
  items: [],
  totalAmount: 0,
});

const selectedAddress = ref<any | null>(null);
const addressForm = reactive({
  receiverName: "林小姐",
  receiverPhone: "13800138000",
  province: "上海市",
  city: "上海市",
  district: "浦东新区",
  detailAddress: "世纪大道 100 号",
});

onMounted(async () => {
  await Promise.all([loadCart(), loadAddresses()]);
});

async function loadCart() {
  Object.assign(cart, await api.getCart());
}

async function loadAddresses() {
  const addresses = await api.getAddresses();
  selectedAddress.value = addresses[0] || null;
}

async function saveAddress() {
  selectedAddress.value = await api.createAddress({
    ...addressForm,
    isDefault: true,
  });
  uni.showToast({ title: "地址已保存", icon: "success" });
}

async function checkout() {
  if (!cart.items.length) {
    uni.showToast({ title: "购物车为空", icon: "none" });
    return;
  }
  if (!selectedAddress.value) {
    await saveAddress();
  }
  const order = await api.createOrder({
    addressId: selectedAddress.value.id,
    cartItemIds: cart.items.map((item: any) => item.id),
    buyerRemark: "小程序端下单",
  });
  await api.payOrder(order.orderNo);
  uni.showToast({ title: "订单已创建，等待支付回调", icon: "none" });
  await Promise.all([loadCart(), loadAddresses()]);
}

function goShopping() {
  uni.switchTab({
    url: "/pages/catalog/index",
  });
}
</script>

<style scoped lang="scss">
.cart-page {
  padding: 28rpx 22rpx 46rpx;
}

.cart-page__eyebrow,
.cart-page__title,
.cart-page__subtitle,
.cart-item__title,
.cart-item__summary,
.cart-item__remark,
.cart-item__price,
.cart-item__qty,
.empty-card__title,
.empty-card__desc,
.checkout-bar__label,
.checkout-bar__price {
  display: block;
}

.cart-page__eyebrow {
  font-size: 20rpx;
  color: #87917d;
}

.cart-page__title {
  margin-top: 10rpx;
  font-size: 46rpx;
  color: #31422f;
}

.cart-page__subtitle {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #7d8576;
}

.cart-list {
  display: grid;
  gap: 18rpx;
  margin-top: 24rpx;
}

.cart-item {
  display: flex;
  gap: 18rpx;
  padding: 18rpx;
}

.cart-item__image {
  width: 174rpx;
  height: 174rpx;
  border-radius: 22rpx;
  background: #eee8da;
}

.cart-item__body {
  flex: 1;
}

.cart-item__title {
  font-size: 30rpx;
  color: #27231c;
}

.cart-item__summary,
.cart-item__remark {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #7d8576;
}

.cart-item__footer {
  display: flex;
  justify-content: space-between;
  margin-top: 18rpx;
}

.cart-item__price {
  font-size: 30rpx;
  color: #47633f;
}

.cart-item__qty {
  font-size: 24rpx;
  color: #858d7d;
}

.empty-card,
.address-card,
.checkout-card,
.checkout-bar {
  margin-top: 24rpx;
  padding: 28rpx;
}

.empty-card__title {
  font-size: 32rpx;
  color: #27231c;
}

.empty-card__desc {
  margin: 12rpx 0 24rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #7d8576;
}

.selected-address {
  display: grid;
  gap: 10rpx;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #54624d;
}

.address-form {
  display: grid;
  gap: 12rpx;
  margin-top: 18rpx;
}

.form-input {
  width: 100%;
  padding: 20rpx 18rpx;
  border-radius: 18rpx;
  background: #fffdf8;
  border: 1rpx solid rgba(181, 191, 171, 0.8);
  font-size: 24rpx;
}

.address-form__button {
  height: 72rpx;
  line-height: 72rpx;
}

.checkout-card {
  display: grid;
  gap: 16rpx;
  font-size: 24rpx;
  color: #5f6758;
}

.checkout-card__row {
  display: flex;
  justify-content: space-between;
}

.checkout-card__notice {
  padding-top: 14rpx;
  border-top: 1rpx solid rgba(181, 191, 171, 0.5);
  color: #8a907f;
  line-height: 1.6;
}

.checkout-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.checkout-bar__label {
  font-size: 22rpx;
  color: #858d7d;
}

.checkout-bar__price {
  margin-top: 8rpx;
  font-size: 42rpx;
  color: #47633f;
}

.checkout-bar__button {
  width: 230rpx;
}
</style>
