<template>
  <div class="dashboard-shell">
    <div class="dashboard-backdrop"></div>

    <section v-if="!session.token" class="login-panel">
      <div class="login-copy">
        <p class="eyebrow">JEWELRY V2 OPS</p>
        <h1>珠宝商城运营后台</h1>
        <p class="login-copy__text">
          V2 版本已经补齐商品运营、订单履约、售后审核和基础角色分权。你可以直接用不同角色登录，体验真实运营链路。
        </p>
        <div class="account-hints">
          <article v-for="account in accountHints" :key="account.username" class="hint-card">
            <strong>{{ account.label }}</strong>
            <span>{{ account.username }} / {{ account.password }}</span>
          </article>
        </div>
      </div>

      <form class="login-card" @submit.prevent="handleLogin">
        <label>
          <span>账号</span>
          <input v-model="loginForm.username" placeholder="admin / merch / service" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </label>
        <button type="submit">进入控制台</button>
        <p class="login-tip">{{ errorMessage || "推荐先试 merch 和 service，对比商品运营与客服权限差异。" }}</p>
      </form>
    </section>

    <section v-else class="workspace">
      <aside class="sidebar">
        <div>
          <p class="eyebrow">V2 CONTROL CENTER</p>
          <h2>Jewelry Console</h2>
          <p class="sidebar__user">{{ session.displayName }}</p>
          <p class="sidebar__role">{{ session.roleName || "UNKNOWN" }}</p>
        </div>

        <nav class="nav-list">
          <button
            v-for="item in visibleNavItems"
            :key="item.key"
            :class="['nav-item', { 'nav-item--active': currentView === item.key }]"
            @click="currentView = item.key"
          >
            <span>{{ item.label }}</span>
            <small>{{ item.note }}</small>
          </button>
        </nav>

        <button class="logout-button" @click="logout">退出登录</button>
      </aside>

      <main class="content">
        <header class="hero">
          <div>
            <p class="eyebrow">运营概览</p>
            <h1>V2 已进入可运营阶段，重点看库存、待发货和待审核售后。</h1>
          </div>
          <div class="hero__metrics">
            <article class="metric-card">
              <span>商品数</span>
              <strong>{{ products.length }}</strong>
            </article>
            <article class="metric-card">
              <span>待发货</span>
              <strong>{{ waitingShipmentCount }}</strong>
            </article>
            <article class="metric-card">
              <span>待审核售后</span>
              <strong>{{ pendingAfterSalesCount }}</strong>
            </article>
          </div>
        </header>

        <section v-if="currentView === 'orders'" class="panel">
          <div class="panel__header">
            <div>
              <h3>订单履约</h3>
              <p>客服角色可以直接录入物流单号，完成从待发货到已发货的流转。</p>
            </div>
          </div>
          <div class="order-table">
            <article v-for="order in orders" :key="order.orderNo" class="order-row">
              <div>
                <strong>{{ order.orderNo }}</strong>
                <p>{{ order.items?.[0]?.productName || "珠宝订单" }}</p>
              </div>
              <div>
                <span class="status-chip">{{ order.status }}</span>
                <p>￥{{ order.payAmount }}</p>
              </div>
              <button v-if="order.status === 'WAITING_SHIPMENT'" @click="shipOrder(order.orderNo)">录入发货</button>
              <button v-else class="button-muted" disabled>已处理</button>
            </article>
          </div>
        </section>

        <section v-if="currentView === 'products'" class="panel">
          <div class="panel__header">
            <div>
              <h3>商品运营</h3>
              <p>支持新增分类、创建样例商品，以及对 SKU 库存做快速补货。</p>
            </div>
            <div class="panel__actions">
              <button @click="createCategory">新增分类</button>
              <button @click="createSampleProduct">新增样例商品</button>
            </div>
          </div>

          <div class="product-grid">
            <article v-for="product in products" :key="product.id" class="product-card">
              <img :src="product.coverImage" :alt="product.name" />
              <div class="product-card__body">
                <div class="product-card__top">
                  <div>
                    <h4>{{ product.name }}</h4>
                    <p>{{ product.subtitle }}</p>
                  </div>
                  <span class="status-chip">{{ product.status }}</span>
                </div>
                <div class="product-card__meta">
                  <span>￥{{ product.basePrice }}</span>
                  <span>SKU {{ product.skus?.length || 0 }}</span>
                </div>
                <div v-if="product.skus?.length" class="sku-list">
                  <div v-for="sku in product.skus" :key="sku.id" class="sku-row">
                    <span>{{ sku.material }} / {{ sku.ringSize }}</span>
                    <strong>库存 {{ sku.stock }}</strong>
                  </div>
                </div>
                <button class="secondary-button" @click="adjustStock(product)">调整库存</button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="currentView === 'afterSales'" class="panel">
          <div class="panel__header">
            <div>
              <h3>售后审核</h3>
              <p>用户申请后，客服可以直接审批并联动退款状态与订单状态。</p>
            </div>
          </div>
          <div class="request-list">
            <article v-for="item in afterSales" :key="item.id" class="request-card">
              <div class="request-card__top">
                <strong>{{ item.orderNo }}</strong>
                <span class="status-chip">{{ item.status }}</span>
              </div>
              <p>原因：{{ item.reason }}</p>
              <p>说明：{{ item.description || "无补充说明" }}</p>
              <p>退款状态：{{ item.refundStatus }}</p>
              <div class="request-card__actions">
                <button v-if="item.status === 'PENDING'" @click="auditAfterSale(item, true)">通过并退款</button>
                <button v-if="item.status === 'PENDING'" class="secondary-button" @click="auditAfterSale(item, false)">驳回</button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="currentView === 'content'" class="panel">
          <div class="panel__header">
            <div>
              <h3>首页内容</h3>
              <p>这里继续保留 Banner 和内容块预览，方便确认品牌展示与转化入口的平衡。</p>
            </div>
          </div>
          <div class="banner-grid">
            <article v-for="banner in homeConfig.banners || []" :key="banner.id" class="banner-card">
              <img :src="banner.imageUrl" :alt="banner.title" />
              <div>
                <strong>{{ banner.title }}</strong>
                <p>{{ banner.subtitle }}</p>
              </div>
            </article>
          </div>
        </section>

        <section v-if="currentView === 'custom'" class="panel">
          <div class="panel__header">
            <div>
              <h3>定制线索</h3>
              <p>轻定制依旧保留为客服跟进入口，重点看刻字和尺寸偏好。</p>
            </div>
          </div>
          <div class="request-list">
            <article v-for="request in customRequests" :key="request.id" class="request-card">
              <div class="request-card__top">
                <strong>{{ request.contactName }}</strong>
                <span class="status-chip">{{ request.status }}</span>
              </div>
              <p>联系电话：{{ request.contactPhone }}</p>
              <p>刻字：{{ request.engravingText || "无" }}</p>
              <p>尺寸备注：{{ request.sizeRemark || "无" }}</p>
              <p>材质偏好：{{ request.materialRemark || "无" }}</p>
            </article>
          </div>
        </section>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { adminApi } from "../api";

const accountHints = [
  { label: "超级管理员", username: "admin", password: "Admin@123" },
  { label: "商品运营", username: "merch", password: "Merch@123" },
  { label: "订单客服", username: "service", password: "Service@123" },
];

const navItems = [
  { key: "orders", label: "订单履约", note: "发货与履约", permission: "ORDER_MANAGE" },
  { key: "products", label: "商品运营", note: "分类 / 商品 / 库存", permission: "PRODUCT_MANAGE" },
  { key: "afterSales", label: "售后审核", note: "退款闭环", permission: "AFTER_SALE_MANAGE" },
  { key: "content", label: "首页内容", note: "Banner 预览", permission: "CONTENT_MANAGE" },
  { key: "custom", label: "定制线索", note: "客服跟进", permission: "USER_VIEW" },
];

const currentView = ref("orders");
const errorMessage = ref("");
const loginForm = reactive({
  username: "admin",
  password: "Admin@123",
});

const session = reactive({
  token: localStorage.getItem("JEWELRY_ADMIN_TOKEN") || "",
  displayName: localStorage.getItem("JEWELRY_ADMIN_NAME") || "珠宝运营管理员",
  roleName: localStorage.getItem("JEWELRY_ADMIN_ROLE") || "",
  permissions: JSON.parse(localStorage.getItem("JEWELRY_ADMIN_PERMISSIONS") || "[]") as string[],
});

const products = ref<any[]>([]);
const orders = ref<any[]>([]);
const afterSales = ref<any[]>([]);
const homeConfig = ref<any>({});
const customRequests = ref<any[]>([]);

const visibleNavItems = computed(() =>
  navItems.filter((item) => hasPermission(item.permission)),
);

const waitingShipmentCount = computed(
  () => orders.value.filter((order) => order.status === "WAITING_SHIPMENT").length,
);

const pendingAfterSalesCount = computed(
  () => afterSales.value.filter((item) => item.status === "PENDING").length,
);

onMounted(async () => {
  if (session.token) {
    currentView.value = visibleNavItems.value[0]?.key || "orders";
    await loadDashboard();
  }
});

function hasPermission(permission: string) {
  return session.permissions.includes(permission) || session.roleName === "SUPER_ADMIN";
}

async function handleLogin() {
  try {
    errorMessage.value = "";
    const loginData = await adminApi.login(loginForm);
    session.token = loginData.token;
    session.displayName = loginData.displayName;
    session.roleName = loginData.roleName;
    session.permissions = loginData.permissions || [];
    localStorage.setItem("JEWELRY_ADMIN_NAME", session.displayName);
    localStorage.setItem("JEWELRY_ADMIN_ROLE", session.roleName);
    localStorage.setItem("JEWELRY_ADMIN_PERMISSIONS", JSON.stringify(session.permissions));
    currentView.value = visibleNavItems.value[0]?.key || "orders";
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "登录失败";
  }
}

async function loadDashboard() {
  const tasks: Promise<void>[] = [];

  if (hasPermission("PRODUCT_MANAGE")) {
    tasks.push(adminApi.getProducts().then((data) => { products.value = data; }));
  } else {
    products.value = [];
  }
  if (hasPermission("ORDER_MANAGE")) {
    tasks.push(adminApi.getOrders().then((data) => { orders.value = data; }));
  } else {
    orders.value = [];
  }
  if (hasPermission("AFTER_SALE_MANAGE")) {
    tasks.push(adminApi.getAfterSales().then((data) => { afterSales.value = data; }));
  } else {
    afterSales.value = [];
  }
  if (hasPermission("CONTENT_MANAGE")) {
    tasks.push(adminApi.getHomeConfig().then((data) => { homeConfig.value = data; }));
  } else {
    homeConfig.value = {};
  }
  if (hasPermission("USER_VIEW")) {
    tasks.push(adminApi.getCustomRequests().then((data) => { customRequests.value = data; }));
  } else {
    customRequests.value = [];
  }

  await Promise.all(tasks);
}

async function shipOrder(orderNo: string) {
  const company = window.prompt("请输入物流公司", "SF");
  const trackingNo = window.prompt("请输入物流单号", `SF-${Date.now()}`);
  if (!company || !trackingNo) {
    return;
  }
  await adminApi.shipOrder(orderNo, { company, trackingNo });
  await loadDashboard();
}

async function createCategory() {
  const name = window.prompt("请输入分类名称", "Bracelet");
  if (!name) return;
  const icon = window.prompt("请输入分类图标", "B") || "B";
  const sortOrder = Number(window.prompt("请输入排序值", "9") || 9);
  await adminApi.createCategory({ name, icon, sortOrder });
  await loadDashboard();
}

async function createSampleProduct() {
  const categoryId = Number(window.prompt("请输入分类 ID", "1") || 1);
  const suffix = Date.now().toString().slice(-6);
  await adminApi.createProduct({
    categoryId,
    name: `Aurora Ring ${suffix}`,
    subtitle: "V2 后台创建的样例珠宝",
    productNo: `AURORA-${suffix}`,
    basePrice: 3999,
    description: "可用于演示商品运营、SKU 库存与售后闭环。",
    certificateInfo: "GIC Certified",
    serviceInfo: "Express shipping and gift box",
    supportCustom: true,
    hotFlag: false,
    newFlag: true,
    tags: ["v2", "ops"],
    status: "ON_SALE",
    media: [
      { mediaType: "IMAGE", mediaUrl: "https://example.com/aurora.jpg", sortOrder: 1 },
    ],
    skus: [
      {
        skuCode: `SKU-${suffix}`,
        material: "18K Gold",
        ringSize: "12",
        weightDesc: "3g",
        salePrice: 3999,
        stock: 5,
        status: "ENABLED",
      },
    ],
  });
  await loadDashboard();
}

async function adjustStock(product: any) {
  const sku = product.skus?.[0];
  if (!sku) {
    window.alert("当前商品没有可调整的 SKU。");
    return;
  }
  const newStock = Number(window.prompt(`请输入 SKU ${sku.skuCode} 的新库存`, String(sku.stock)) || sku.stock);
  const reason = window.prompt("请输入调整原因", "manual replenishment") || "manual replenishment";
  await adminApi.adjustStock(product.id, {
    skuId: sku.id,
    newStock,
    reason,
  });
  await loadDashboard();
}

async function auditAfterSale(item: any, approved: boolean) {
  const remark = window.prompt(
    approved ? "请输入通过备注" : "请输入驳回备注",
    approved ? "approved for refund" : "rejected after review",
  ) || "";
  await adminApi.auditAfterSale(item.id, { approved, remark });
  await loadDashboard();
}

function logout() {
  localStorage.removeItem("JEWELRY_ADMIN_TOKEN");
  localStorage.removeItem("JEWELRY_ADMIN_NAME");
  localStorage.removeItem("JEWELRY_ADMIN_ROLE");
  localStorage.removeItem("JEWELRY_ADMIN_PERMISSIONS");
  session.token = "";
  session.displayName = "珠宝运营管理员";
  session.roleName = "";
  session.permissions = [];
}
</script>

<style scoped>
.dashboard-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(197, 171, 109, 0.16), transparent 24%),
    linear-gradient(180deg, #f7f0e5 0%, #eadcca 100%);
  color: #2d2418;
}

.dashboard-backdrop {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(154, 125, 69, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(154, 125, 69, 0.05) 1px, transparent 1px);
  background-size: 28px 28px;
  pointer-events: none;
}

.login-panel,
.workspace {
  position: relative;
  z-index: 1;
}

.login-panel {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 32px;
  min-height: 100vh;
  padding: 56px;
  align-items: center;
}

.login-copy h1,
.hero h1,
.sidebar h2 {
  font-family: "Cormorant Garamond", "Times New Roman", serif;
}

.login-copy h1 {
  margin: 12px 0 16px;
  font-size: 68px;
  line-height: 1.05;
}

.login-copy__text {
  max-width: 560px;
  line-height: 1.8;
  color: #665844;
}

.account-hints {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 24px;
}

.hint-card,
.login-card,
.sidebar,
.hero,
.panel {
  background: rgba(255, 251, 245, 0.82);
  border: 1px solid rgba(152, 119, 56, 0.16);
  box-shadow: 0 24px 60px rgba(81, 59, 20, 0.08);
  backdrop-filter: blur(16px);
}

.hint-card {
  padding: 16px;
  border-radius: 20px;
  display: grid;
  gap: 6px;
}

.hint-card span {
  color: #7a684d;
  font-size: 14px;
}

.login-card {
  padding: 36px;
  border-radius: 28px;
  display: grid;
  gap: 18px;
}

.login-card label {
  display: grid;
  gap: 8px;
  color: #6a5a44;
}

.login-card input,
.login-card button,
.nav-item,
.panel button,
.logout-button {
  border-radius: 999px;
}

.login-card input {
  border: 1px solid rgba(133, 104, 53, 0.16);
  padding: 14px 18px;
  background: #fffdf9;
}

.login-card button,
.panel button,
.logout-button {
  border: none;
  padding: 14px 18px;
  background: linear-gradient(135deg, #2a2015, #8d6a34);
  color: #fff8ef;
  cursor: pointer;
}

.secondary-button,
.button-muted {
  background: #efe1c9 !important;
  color: #6f5428 !important;
}

.login-tip {
  margin: 0;
  color: #8d7755;
}

.workspace {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  padding: 24px;
}

.sidebar {
  border-radius: 28px;
  padding: 28px;
  display: grid;
  align-content: start;
  gap: 28px;
}

.sidebar h2 {
  margin: 12px 0 8px;
  font-size: 40px;
}

.sidebar__user,
.sidebar__role {
  margin: 0;
  color: #7f6947;
}

.nav-list {
  display: grid;
  gap: 12px;
}

.nav-item {
  border: 1px solid rgba(133, 104, 53, 0.16);
  padding: 16px 18px;
  background: #fffaf3;
  text-align: left;
  display: grid;
  gap: 6px;
  cursor: pointer;
}

.nav-item--active {
  background: linear-gradient(135deg, #2d2217, #8a6733);
  color: #fff7ea;
}

.logout-button {
  margin-top: auto;
}

.content {
  display: grid;
  gap: 20px;
}

.hero,
.panel {
  border-radius: 28px;
  padding: 28px 32px;
}

.hero h1 {
  margin: 10px 0 0;
  font-size: 52px;
  line-height: 1.06;
  max-width: 880px;
}

.hero__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.metric-card {
  padding: 20px 22px;
  border-radius: 24px;
  background: rgba(255, 248, 235, 0.92);
}

.metric-card span {
  display: block;
  color: #7d6a49;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 40px;
  color: #7c5b24;
}

.panel__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  margin-bottom: 20px;
}

.panel__header h3 {
  margin: 0;
  font-size: 28px;
  font-family: "Cormorant Garamond", "Times New Roman", serif;
}

.panel__header p {
  margin: 8px 0 0;
  color: #73634c;
}

.panel__actions {
  display: flex;
  gap: 12px;
}

.order-table,
.request-list {
  display: grid;
  gap: 14px;
}

.order-row,
.request-card {
  display: grid;
  gap: 16px;
  align-items: center;
  padding: 18px;
  border-radius: 22px;
  background: #fff9f1;
}

.order-row {
  grid-template-columns: 1.6fr 0.8fr 180px;
}

.status-chip {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: #efe1c9;
  color: #7a5e2e;
}

.product-grid,
.banner-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.product-card,
.banner-card {
  border-radius: 24px;
  overflow: hidden;
  background: #fff9f2;
}

.product-card img,
.banner-card img {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.product-card__body,
.banner-card div {
  padding: 18px;
}

.product-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.product-card__body h4 {
  margin: 0;
  font-size: 22px;
}

.product-card__body p {
  margin: 8px 0 0;
  color: #79684c;
}

.product-card__meta,
.sku-row,
.request-card__top,
.request-card__actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.product-card__meta {
  margin: 16px 0 12px;
  color: #7e5b26;
}

.sku-list {
  display: grid;
  gap: 10px;
  margin-bottom: 16px;
}

.sku-row {
  padding: 10px 12px;
  border-radius: 16px;
  background: #f4ead8;
}

.request-card p {
  margin: 0;
  color: #7e6a50;
}

.eyebrow {
  letter-spacing: 0.28em;
  font-size: 12px;
  text-transform: uppercase;
  color: #8f7544;
}

@media (max-width: 1080px) {
  .login-panel,
  .workspace {
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .account-hints,
  .hero__metrics,
  .product-grid,
  .banner-grid {
    grid-template-columns: 1fr;
  }

  .order-row {
    grid-template-columns: 1fr;
  }
}
</style>
