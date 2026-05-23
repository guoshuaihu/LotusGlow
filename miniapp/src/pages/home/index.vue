<template>
  <scroll-view scroll-y class="page-shell home-page">
    <view class="brand-hero">
      <view class="brand-hero__copy">
        <text class="brand-hero__kicker">QIN NEW ORIENTAL JEWELRY</text>
        <text class="brand-hero__title">把东方意境戴在身上</text>
        <text class="brand-hero__desc">甄选珍珠、18K 金与天然彩宝，提供证书溯源、礼盒发货与轻定制服务。</text>
        <view class="brand-hero__actions">
          <button class="pill-button brand-hero__button" @tap="switchTab('/pages/catalog/index')">立即选购</button>
          <button class="ghost-button brand-hero__button" @tap="switchTab('/pages/profile/index')">会员权益</button>
        </view>
      </view>
    </view>

    <view class="trust-strip glass-card">
      <view v-for="item in trustItems" :key="item.title" class="trust-strip__item">
        <text class="trust-strip__title">{{ item.title }}</text>
        <text class="trust-strip__desc">{{ item.desc }}</text>
      </view>
    </view>

    <view class="category-row">
      <view
        v-for="category in categoryTabs"
        :key="category.id"
        class="category-entry"
        :class="{ 'category-entry--active': activeCategoryId === category.id }"
        @tap="changeCategory(category.id)"
      >
        <text class="category-entry__icon">{{ category.icon || "◇" }}</text>
        <text class="category-entry__name">{{ category.name }}</text>
      </view>
    </view>

    <view class="content-section">
      <SectionTitle title="本周热卖" subtitle="适合礼赠、纪念日与日常通勤" />
      <view class="goods-grid">
        <view
          v-for="product in featuredProducts"
          :key="product.id"
          class="goods-card"
          @tap="openProduct(product.id)"
        >
          <image :src="product.coverImage" mode="aspectFill" class="goods-card__image" />
          <view class="goods-card__body">
            <view class="goods-card__tags">
              <text v-for="tag in product.tags?.slice(0, 2)" :key="tag" class="goods-card__tag">{{ tag }}</text>
            </view>
            <text class="goods-card__title">{{ product.name }}</text>
            <text class="goods-card__subtitle">{{ product.subtitle }}</text>
            <text class="goods-card__price">{{ formatMoney(product.price) }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="double-campaign">
      <view class="campaign-card campaign-card--jade" @tap="switchTab('/pages/catalog/index')">
        <text class="campaign-card__eyebrow">新品上新</text>
        <text class="campaign-card__title">清雅通勤系列</text>
        <text class="campaign-card__desc">低饱和玉色、珍珠光泽，更适合日常搭配。</text>
      </view>
      <view class="campaign-card campaign-card--ivory" @tap="switchTab('/pages/cart/index')">
        <text class="campaign-card__eyebrow">轻定制</text>
        <text class="campaign-card__title">刻字与圈口备注</text>
        <text class="campaign-card__desc">下单前记录需求，客服人工跟进确认。</text>
      </view>
    </view>

    <view class="content-section" v-if="homeData.banners.length">
      <SectionTitle title="品牌专题" subtitle="活动、礼赠与新品内容由后台配置" />
      <scroll-view scroll-x class="banner-list">
        <view v-for="banner in homeData.banners" :key="banner.id" class="banner-card">
          <image :src="banner.imageUrl" mode="aspectFill" class="banner-card__image" />
          <view class="banner-card__overlay">
            <text class="banner-card__title">{{ banner.title }}</text>
            <text class="banner-card__subtitle">{{ banner.subtitle }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="brand-story glass-card">
      <text class="brand-story__title">{{ homeData.brandStory?.title || "品牌承诺" }}</text>
      <text class="brand-story__content">
        {{ homeData.brandStory?.content || "每一件作品均支持材质说明、售后咨询与礼盒发货，适合自戴与送礼。" }}
      </text>
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import SectionTitle from "../../components/SectionTitle.vue";
import { api } from "../../services/api";
import { formatMoney } from "../../utils/format";

const homeData = reactive<any>({
  banners: [],
  categories: [],
  brandStory: null,
});

const featuredResult = reactive<any>({
  items: [],
});

const activeCategoryId = ref<number | null>(null);

const trustItems = [
  { title: "证书溯源", desc: "材质与证书信息可查" },
  { title: "礼盒发货", desc: "顺丰保价配送" },
  { title: "轻定制", desc: "刻字与尺寸备注" },
];

const categoryTabs = computed(() => homeData.categories.slice(0, 4));
const featuredProducts = computed(() => featuredResult.items.slice(0, 6));

onMounted(async () => {
  Object.assign(homeData, await api.getHome());
  if (homeData.categories.length) {
    activeCategoryId.value = homeData.categories[0].id;
  }
  await loadFeaturedProducts();
});

async function loadFeaturedProducts() {
  Object.assign(
    featuredResult,
    await api.getProducts({
      categoryId: activeCategoryId.value,
    }),
  );
}

async function changeCategory(categoryId: number) {
  activeCategoryId.value = categoryId;
  await loadFeaturedProducts();
}

function openProduct(productId: number) {
  uni.navigateTo({
    url: `/pages/product/index?productId=${productId}`,
  });
}

function switchTab(path: string) {
  uni.switchTab({ url: path });
}
</script>

<style scoped lang="scss">
.home-page {
  padding: 28rpx 22rpx 54rpx;
}

.brand-hero {
  min-height: 430rpx;
  padding: 44rpx 34rpx;
  border-radius: 32rpx;
  overflow: hidden;
  background:
    linear-gradient(110deg, rgba(39, 54, 39, 0.88), rgba(93, 119, 81, 0.78)),
    url("https://images.unsplash.com/photo-1617038260897-41a1f14a8ca0?auto=format&fit=crop&w=1200&q=80") center/cover;
}

.brand-hero__copy {
  max-width: 560rpx;
}

.brand-hero__kicker,
.brand-hero__title,
.brand-hero__desc,
.trust-strip__title,
.trust-strip__desc,
.category-entry__icon,
.category-entry__name,
.goods-card__title,
.goods-card__subtitle,
.goods-card__price,
.campaign-card__eyebrow,
.campaign-card__title,
.campaign-card__desc,
.banner-card__title,
.banner-card__subtitle,
.brand-story__title,
.brand-story__content {
  display: block;
}

.brand-hero__kicker {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.72);
}

.brand-hero__title {
  margin-top: 28rpx;
  font-size: 58rpx;
  line-height: 1.16;
  color: #fffdf7;
}

.brand-hero__desc {
  margin-top: 20rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.86);
}

.brand-hero__actions {
  display: flex;
  gap: 16rpx;
  margin-top: 34rpx;
}

.brand-hero__button {
  width: 190rpx;
  margin: 0;
}

.trust-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 20rpx;
  padding: 24rpx 18rpx;
}

.trust-strip__item {
  text-align: center;
}

.trust-strip__title {
  font-size: 26rpx;
  color: #31422f;
}

.trust-strip__desc {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #8d9785;
}

.category-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14rpx;
  margin: 24rpx 0;
}

.category-entry {
  padding: 22rpx 8rpx;
  border-radius: 22rpx;
  background: #fffdf8;
  text-align: center;
  border: 1rpx solid rgba(190, 199, 180, 0.64);
}

.category-entry--active {
  background: #edf4e8;
  border-color: #83a276;
}

.category-entry__icon {
  font-size: 36rpx;
}

.category-entry__name {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #40523b;
}

.content-section {
  margin-top: 28rpx;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.goods-card {
  overflow: hidden;
  border-radius: 24rpx;
  background: #fffdf9;
  box-shadow: 0 10rpx 24rpx rgba(83, 101, 72, 0.08);
}

.goods-card__image {
  width: 100%;
  height: 320rpx;
  background: #eee8da;
}

.goods-card__body {
  padding: 18rpx 18rpx 22rpx;
}

.goods-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  min-height: 32rpx;
}

.goods-card__tag {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  background: #edf4e8;
  color: #667b57;
  font-size: 18rpx;
}

.goods-card__title {
  margin-top: 10rpx;
  font-size: 28rpx;
  line-height: 1.45;
  color: #27231c;
}

.goods-card__subtitle {
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: #87877b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goods-card__price {
  margin-top: 14rpx;
  font-size: 32rpx;
  color: #47633f;
}

.double-campaign {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 30rpx;
}

.campaign-card {
  min-height: 210rpx;
  padding: 28rpx 24rpx;
  border-radius: 26rpx;
}

.campaign-card--jade {
  background: linear-gradient(135deg, #eaf3e3, #d1e0c6);
}

.campaign-card--ivory {
  background: linear-gradient(135deg, #fff8e8, #eadcc4);
}

.campaign-card__eyebrow {
  font-size: 20rpx;
  color: #728060;
}

.campaign-card__title {
  margin-top: 10rpx;
  font-size: 32rpx;
  line-height: 1.25;
  color: #30422e;
}

.campaign-card__desc {
  margin-top: 14rpx;
  font-size: 22rpx;
  line-height: 1.55;
  color: #7a806f;
}

.banner-list {
  white-space: nowrap;
}

.banner-card {
  display: inline-block;
  position: relative;
  width: 560rpx;
  height: 270rpx;
  margin-right: 18rpx;
  border-radius: 26rpx;
  overflow: hidden;
}

.banner-card__image {
  width: 100%;
  height: 100%;
}

.banner-card__overlay {
  position: absolute;
  inset: 0;
  padding: 26rpx;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background: linear-gradient(180deg, transparent 20%, rgba(34, 46, 32, 0.72) 100%);
}

.banner-card__title {
  font-size: 30rpx;
  color: #fffdf7;
}

.banner-card__subtitle {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.86);
}

.brand-story {
  margin-top: 30rpx;
  padding: 28rpx;
}

.brand-story__title {
  font-size: 30rpx;
  color: #31422f;
}

.brand-story__content {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: #747b6e;
}
</style>
