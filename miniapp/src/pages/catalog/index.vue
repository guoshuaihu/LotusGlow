<template>
  <view class="page-shell catalog-page">
    <view class="catalog-head">
      <text class="catalog-head__eyebrow">SELECTED JEWELRY</text>
      <text class="catalog-head__title">甄选珠宝</text>
      <text class="catalog-head__desc">按戒指、项链、耳饰、手链快速筛选，也可以搜索材质、场景或系列名称。</text>
    </view>

    <view class="search-box">
      <input
        v-model="keyword"
        placeholder="搜索手串、项链、开运礼物"
        placeholder-class="input-placeholder"
        class="search-input"
        confirm-type="search"
        @confirm="fetchProducts"
      />
      <button class="search-button" @tap="fetchProducts">搜索</button>
    </view>

    <scroll-view scroll-x class="category-strip">
      <view
        class="category-pill"
        :class="{ 'category-pill--active': selectedCategoryId === null }"
        @tap="selectCategory(null)"
      >
        全部
      </view>
      <view
        v-for="category in categories"
        :key="category.id"
        class="category-pill"
        :class="{ 'category-pill--active': selectedCategoryId === category.id }"
        @tap="selectCategory(category.id)"
      >
        {{ category.name }}
      </view>
    </scroll-view>

    <view class="result-meta">
      <text>共 {{ result.total }} 件商品</text>
      <text>现货下单 · 支持轻定制备注</text>
    </view>

    <view class="catalog-grid">
      <view
        v-for="product in result.items"
        :key="product.id"
        class="catalog-card"
        @tap="openProduct(product.id)"
      >
        <image :src="product.coverImage" mode="aspectFill" class="catalog-card__image" />
        <view class="catalog-card__body">
          <view class="catalog-card__tags">
            <text v-for="tag in product.tags?.slice(0, 2)" :key="tag" class="catalog-card__tag">{{ tag }}</text>
          </view>
          <text class="catalog-card__title">{{ product.name }}</text>
          <text class="catalog-card__subtitle">{{ product.subtitle }}</text>
          <view class="catalog-card__footer">
            <text class="catalog-card__price">{{ formatMoney(product.price) }}</text>
            <text class="catalog-card__sales">已售 {{ product.salesCount }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from "@dcloudio/uni-app";
import { onMounted, reactive, ref } from "vue";
import { api } from "../../services/api";
import { formatMoney } from "../../utils/format";

const keyword = ref("");
const selectedCategoryId = ref<number | null>(null);
const categories = ref<any[]>([]);
const result = reactive<any>({
  total: 0,
  items: [],
});

onLoad((query) => {
  if (query?.categoryId) {
    selectedCategoryId.value = Number(query.categoryId);
  }
});

onMounted(async () => {
  const homeData = await api.getHome();
  categories.value = homeData.categories;
  await fetchProducts();
});

async function fetchProducts() {
  Object.assign(
    result,
    await api.getProducts({
      keyword: keyword.value,
      categoryId: selectedCategoryId.value,
    }),
  );
}

function selectCategory(categoryId: number | null) {
  selectedCategoryId.value = categoryId;
  fetchProducts();
}

function openProduct(productId: number) {
  uni.navigateTo({
    url: `/pages/product/index?productId=${productId}`,
  });
}
</script>

<style scoped lang="scss">
.catalog-page {
  padding: 28rpx 22rpx 46rpx;
}

.catalog-head__eyebrow,
.catalog-head__title,
.catalog-head__desc,
.catalog-card__title,
.catalog-card__subtitle,
.catalog-card__price,
.catalog-card__sales {
  display: block;
}

.catalog-head {
  padding: 8rpx 4rpx 18rpx;
}

.catalog-head__eyebrow {
  font-size: 20rpx;
  color: #87917d;
}

.catalog-head__title {
  margin-top: 10rpx;
  font-size: 48rpx;
  color: #31422f;
}

.catalog-head__desc {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #7d8576;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 18rpx;
  padding: 14rpx 16rpx 14rpx 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10rpx 24rpx rgba(81, 99, 71, 0.08);
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #2d2a22;
}

.input-placeholder {
  color: #a5ac9c;
}

.search-button {
  width: 118rpx;
  height: 58rpx;
  line-height: 58rpx;
  border-radius: 999rpx;
  background: #31422f;
  color: #fffdf8;
  font-size: 24rpx;
}

.category-strip {
  margin-top: 24rpx;
  white-space: nowrap;
}

.category-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 14rpx 30rpx;
  margin-right: 14rpx;
  border-radius: 999rpx;
  background: #fffdf8;
  color: #78816f;
  font-size: 24rpx;
  border: 1rpx solid rgba(180, 190, 170, 0.6);
}

.category-pill--active {
  background: linear-gradient(135deg, #31422f, #799265);
  color: #fffdf7;
}

.result-meta {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  margin: 26rpx 4rpx 18rpx;
  font-size: 22rpx;
  color: #858d7b;
}

.catalog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.catalog-card {
  overflow: hidden;
  border-radius: 24rpx;
  background: #fffdf9;
  box-shadow: 0 10rpx 24rpx rgba(84, 101, 72, 0.08);
}

.catalog-card__image {
  width: 100%;
  height: 320rpx;
  background: #eee8da;
}

.catalog-card__body {
  padding: 18rpx;
}

.catalog-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  min-height: 32rpx;
}

.catalog-card__tag {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  background: #edf4e8;
  color: #64795a;
  font-size: 18rpx;
}

.catalog-card__title {
  margin-top: 10rpx;
  font-size: 28rpx;
  line-height: 1.45;
  color: #27231c;
}

.catalog-card__subtitle {
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.55;
  color: #85867a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.catalog-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
}

.catalog-card__price {
  font-size: 30rpx;
  color: #47633f;
}

.catalog-card__sales {
  font-size: 20rpx;
  color: #9da596;
}
</style>
