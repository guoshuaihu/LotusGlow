<template>
  <scroll-view scroll-y class="page-shell profile-page">
    <view class="profile-card">
      <view class="profile-card__meta">
        <view class="profile-card__avatar">{{ initial }}</view>
        <view class="profile-card__copy">
          <text class="profile-card__name">{{ user?.nickname || "珠宝用户" }}</text>
          <text class="profile-card__note">
            {{ user?.phone ? `已绑定手机号 ${user.phone}` : "绑定手机号后，可用于订单、售后和客服回访。" }}
          </text>
        </view>
      </view>
      <view class="profile-card__stats">
        <view class="stat-item">
          <text class="stat-item__value">{{ orders.length }}</text>
          <text class="stat-item__label">订单</text>
        </view>
        <view class="stat-item">
          <text class="stat-item__value">{{ favorites.length }}</text>
          <text class="stat-item__label">收藏</text>
        </view>
        <view class="stat-item">
          <text class="stat-item__value">{{ afterSales.length }}</text>
          <text class="stat-item__label">售后</text>
        </view>
      </view>
      <view class="profile-actions">
        <button class="action-button" @tap="bindDemoPhone">绑定手机号</button>
        <button class="action-button secondary-button" @tap="refreshProfile">刷新登录态</button>
      </view>
    </view>

    <view class="quick-grid">
      <view v-for="entry in quickEntries" :key="entry.title" class="quick-card glass-card">
        <text class="quick-card__title">{{ entry.title }}</text>
        <text class="quick-card__desc">{{ entry.desc }}</text>
      </view>
    </view>

    <view class="module-card glass-card">
      <view class="section-heading">
        <view>
          <text class="section-heading__title">地址管理</text>
          <view class="section-heading__subtitle">支持地址新增、更新和删除。</view>
        </view>
      </view>
      <view class="section-actions">
        <button class="mini-button" @tap="createDemoAddress">新增示例地址</button>
        <button class="mini-button secondary-button" @tap="updatePrimaryAddress" :disabled="!addresses.length">更新默认地址</button>
      </view>
      <view v-if="addresses.length" class="address-list">
        <view v-for="address in addresses" :key="address.id" class="address-item">
          <view>
            <text class="address-item__name">{{ address.receiverName }} {{ address.receiverPhone }}</text>
            <text class="address-item__detail">
              {{ address.province }} {{ address.city }} {{ address.district }} {{ address.detailAddress }}
            </text>
          </view>
          <button class="mini-button danger-button" @tap="removeAddress(address.id)">删除</button>
        </view>
      </view>
      <view v-else class="placeholder-text">还没有保存地址，新增后即可体验完整下单和售后流程。</view>
    </view>

    <view class="module-card glass-card">
      <view class="section-heading">
        <view>
          <text class="section-heading__title">订单中心</text>
          <view class="section-heading__subtitle">支持发货后确认收货，以及完成订单发起售后。</view>
        </view>
      </view>
      <view v-if="orders.length" class="order-list">
        <view v-for="order in orders" :key="order.orderNo" class="order-item">
          <view class="order-item__top">
            <text>{{ order.orderNo }}</text>
            <text class="order-item__status">{{ formatOrderStatus(order.status) }}</text>
          </view>
          <text class="order-item__product">{{ order.items[0]?.productName || "珠宝订单" }}</text>
          <view class="order-item__bottom">
            <text>{{ formatMoney(order.payAmount) }}</text>
            <text>{{ order.logisticsCompany || "等待履约" }}</text>
          </view>
          <view class="order-item__actions">
            <button v-if="order.status === 'SHIPPED'" class="mini-button" @tap="confirmReceipt(order.orderNo)">确认收货</button>
            <button
              v-if="order.status === 'COMPLETED'"
              class="mini-button secondary-button"
              @tap="applyAfterSale(order)"
            >
              申请售后
            </button>
          </view>
        </view>
      </view>
      <view v-else class="placeholder-text">还没有订单，可以先去首页挑一件珠宝。</view>
    </view>

    <view class="module-card glass-card">
      <view class="section-heading">
        <view>
          <text class="section-heading__title">售后记录</text>
          <view class="section-heading__subtitle">审核结果和退款状态会在这里联动回显。</view>
        </view>
      </view>
      <view v-if="afterSales.length" class="after-sale-list">
        <view v-for="item in afterSales" :key="item.id" class="after-sale-item">
          <view class="order-item__top">
            <text>{{ item.orderNo }}</text>
            <text class="order-item__status">{{ formatAfterSaleStatus(item.status) }}</text>
          </view>
          <text class="after-sale-item__meta">原因：{{ item.reason }}</text>
          <text class="after-sale-item__meta">退款状态：{{ formatRefundStatus(item.refundStatus) }}</text>
        </view>
      </view>
      <view v-else class="placeholder-text">当前还没有售后记录。</view>
    </view>

    <view class="module-card glass-card">
      <view class="section-heading">
        <view>
          <text class="section-heading__title">我的收藏</text>
          <view class="section-heading__subtitle">收藏商品会保留，方便回看和复购。</view>
        </view>
      </view>
      <view v-if="favorites.length" class="favorite-grid">
        <view
          v-for="favorite in favorites"
          :key="favorite.id"
          class="favorite-card"
          @tap="openProduct(favorite.id)"
        >
          <image :src="favorite.coverImage" mode="aspectFill" class="favorite-card__image" />
          <text class="favorite-card__name">{{ favorite.name }}</text>
          <text class="favorite-card__price">{{ formatMoney(favorite.price) }}</text>
        </view>
      </view>
      <view v-else class="placeholder-text">点一下收藏，就能把心动款暂时留下来。</view>
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { api } from "../../services/api";
import { refreshSession, sessionState, updateSession } from "../../services/session";
import { formatAfterSaleStatus, formatMoney, formatOrderStatus, formatRefundStatus } from "../../utils/format";

const orders = ref<any[]>([]);
const favorites = ref<any[]>([]);
const addresses = ref<any[]>([]);
const afterSales = ref<any[]>([]);
const user = computed(() => sessionState.user);
const initial = computed(() => (user.value?.nickname || "珠").slice(0, 1));

const quickEntries = [
  { title: "待支付", desc: "检查价格与库存" },
  { title: "待发货", desc: "后台录入物流" },
  { title: "售后", desc: "申请与退款进度" },
  { title: "收藏", desc: "心动款回看" },
];

onMounted(async () => {
  await loadProfile();
});

async function loadProfile() {
  const [orderData, favoriteData, addressData, afterSaleData] = await Promise.all([
    api.getOrders(),
    api.getFavorites(),
    api.getAddresses(),
    api.getAfterSales(),
  ]);
  orders.value = orderData;
  favorites.value = favoriteData;
  addresses.value = addressData;
  afterSales.value = afterSaleData;
}

async function bindDemoPhone() {
  const userData = await api.bindPhone({
    phone: "13900000001",
  });
  updateSession(sessionState.token, userData);
  uni.showToast({
    title: "手机号已绑定",
    icon: "success",
  });
}

async function refreshProfile() {
  const data = await refreshSession();
  uni.showToast({
    title: data.user.phone ? "登录态已刷新" : "登录态已刷新，可继续绑定手机号",
    icon: "none",
  });
  await loadProfile();
}

async function createDemoAddress() {
  await api.createAddress({
    receiverName: "Alice",
    receiverPhone: "13900000001",
    province: "Shanghai",
    city: "Shanghai",
    district: "Pudong",
    detailAddress: "Century Avenue No.1",
    isDefault: true,
  });
  await loadProfile();
}

async function updatePrimaryAddress() {
  const address = addresses.value[0];
  if (!address) return;
  await api.updateAddress(address.id, {
    receiverName: "Alice Chen",
    receiverPhone: "13900000002",
    province: "Shanghai",
    city: "Shanghai",
    district: "Minhang",
    detailAddress: "Luxury Road No.88",
    isDefault: true,
  });
  await loadProfile();
}

async function removeAddress(addressId: number) {
  await api.deleteAddress(addressId);
  await loadProfile();
}

async function confirmReceipt(orderNo: string) {
  await api.confirmOrder(orderNo);
  await loadProfile();
}

async function applyAfterSale(order: any) {
  const detail = await api.getOrderDetail(order.orderNo);
  const firstItem = detail.items?.[0];
  if (!firstItem) {
    uni.showToast({
      title: "订单明细为空",
      icon: "none",
    });
    return;
  }
  await api.submitAfterSale({
    orderNo: order.orderNo,
    orderItemId: firstItem.id,
    reason: "quality_issue",
    description: "stone loose after delivery",
  });
  await loadProfile();
}

function openProduct(productId: number) {
  uni.navigateTo({
    url: `/pages/product/index?productId=${productId}`,
  });
}
</script>

<style scoped lang="scss">
.profile-page {
  padding: 28rpx 22rpx 46rpx;
}

.profile-card {
  padding: 32rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #2d3c2c, #789164);
  color: #fffdf7;
}

.profile-card__meta {
  display: flex;
  gap: 20rpx;
  align-items: center;
}

.profile-card__avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.18);
  color: #fffdf7;
  font-size: 42rpx;
}

.profile-card__copy {
  flex: 1;
}

.profile-card__name,
.profile-card__note,
.stat-item__value,
.stat-item__label,
.quick-card__title,
.quick-card__desc,
.address-item__name,
.address-item__detail,
.order-item__product,
.favorite-card__name,
.favorite-card__price,
.after-sale-item__meta,
.placeholder-text {
  display: block;
}

.profile-card__name {
  font-size: 36rpx;
}

.profile-card__note {
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.78);
}

.profile-card__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 28rpx;
}

.stat-item {
  padding: 20rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.14);
}

.stat-item__value {
  font-size: 36rpx;
}

.stat-item__label {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
}

.profile-actions,
.section-actions,
.order-item__actions {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
}

.action-button,
.mini-button {
  border: none;
  border-radius: 999rpx;
  background: #fffdf7;
  color: #31422f;
}

.action-button {
  flex: 1;
  font-size: 24rpx;
}

.mini-button {
  padding: 12rpx 22rpx;
  font-size: 22rpx;
  background: linear-gradient(135deg, #31422f, #799265);
  color: #fffdf7;
}

.secondary-button {
  background: #efe5cf;
  color: #5f563f;
}

.danger-button {
  background: #8b4537;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 22rpx;
}

.quick-card {
  padding: 20rpx 12rpx;
  text-align: center;
}

.quick-card__title {
  font-size: 24rpx;
  color: #31422f;
}

.quick-card__desc {
  margin-top: 8rpx;
  font-size: 18rpx;
  color: #858d7d;
}

.module-card {
  padding: 28rpx;
  margin-top: 24rpx;
}

.address-list,
.order-list,
.after-sale-list {
  display: grid;
  gap: 16rpx;
  margin-top: 18rpx;
}

.address-item,
.order-item,
.after-sale-item {
  padding: 22rpx;
  border-radius: 22rpx;
  background: #fffdf8;
}

.address-item {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  align-items: center;
}

.address-item__name {
  font-size: 26rpx;
  color: #27231c;
}

.address-item__detail,
.placeholder-text,
.after-sale-item__meta {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #7d8576;
  line-height: 1.7;
}

.order-item__top,
.order-item__bottom {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
  font-size: 22rpx;
  color: #777f70;
}

.order-item__status {
  color: #47633f;
}

.order-item__product {
  margin: 12rpx 0;
  font-size: 28rpx;
  color: #27231c;
}

.favorite-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.favorite-card__image {
  width: 100%;
  height: 230rpx;
  border-radius: 20rpx;
  background: #eee8da;
}

.favorite-card__name {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #27231c;
}

.favorite-card__price {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #47633f;
}
</style>
