<template>
  <div class="dashboard-shell">
    <section v-if="!session.token" class="login-panel">
      <div class="login-copy">
        <p class="eyebrow">LOTUSGLOW OPS</p>
        <h1>LotusGlow 运营后台</h1>
        <p class="login-copy__text">
          商品、库存、订单和售后集中处理。商品运营可以维护商品与库存，客服账号只保留订单履约和售后能力。
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
        <p class="login-tip">{{ errorMessage || "推荐使用 merch 体验商品运营能力。" }}</p>
      </form>
    </section>

    <section v-else class="workspace">
      <aside class="sidebar">
        <div>
          <p class="eyebrow">CONTROL CENTER</p>
          <h2>LotusGlow</h2>
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
            <h1>围绕商品、库存和履约推进日常运营。</h1>
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
              <p>录入物流信息后，订单会从待发货流转到已发货。</p>
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
              <p>支持搜索、分类筛选、编辑商品和快速调整 SKU 库存。</p>
            </div>
            <div class="panel__actions">
              <button @click="createCategory">新增分类</button>
              <button @click="createSampleProduct">新增样例商品</button>
            </div>
          </div>

          <div class="product-toolbar">
            <label>
              <span>关键词</span>
              <input v-model="productFilters.keyword" placeholder="名称 / 副标题 / 编号" @keyup.enter="loadProducts" />
            </label>
            <label>
              <span>分类</span>
              <select v-model="productFilters.categoryId">
                <option value="">全部分类</option>
                <option v-for="category in categories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
            </label>
            <button @click="loadProducts">查询</button>
            <button class="secondary-button" @click="resetProductFilters">重置</button>
          </div>

          <div class="product-grid">
            <article v-for="product in products" :key="product.id" class="product-card">
              <img :src="product.coverImage || fallbackImage" :alt="product.name" />
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
                  <span>{{ categoryName(product.categoryId) }}</span>
                  <span>SKU {{ product.skus?.length || 0 }}</span>
                </div>
                <div v-if="product.skus?.length" class="sku-list">
                  <div v-for="sku in product.skus" :key="sku.id" class="sku-row">
                    <span>{{ sku.material }} / {{ sku.ringSize }} / {{ sku.weightDesc }}</span>
                    <strong>库存 {{ sku.stock }}</strong>
                    <button class="tiny-button" @click="adjustStock(product, sku)">改库存</button>
                  </div>
                </div>
                <div class="product-card__actions">
                  <button class="secondary-button" @click="openEditor(product)">编辑商品</button>
                </div>
              </div>
            </article>
          </div>

          <form v-if="editingProduct" class="editor-panel" @submit.prevent="saveProduct">
            <div class="panel__header">
              <div>
                <h3>编辑商品</h3>
                <p>{{ editingProduct.name || "未命名商品" }}</p>
              </div>
              <button type="button" class="secondary-button" @click="closeEditor">关闭</button>
            </div>

            <div class="editor-grid">
              <label>
                <span>商品名称</span>
                <input v-model="productForm.name" required />
              </label>
              <label>
                <span>商品编号</span>
                <input v-model="productForm.productNo" required />
              </label>
              <label>
                <span>分类</span>
                <select v-model.number="productForm.categoryId" required>
                  <option v-for="category in categories" :key="category.id" :value="category.id">
                    {{ category.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>基础价</span>
                <input v-model.number="productForm.basePrice" type="number" min="0" step="0.01" required />
              </label>
              <label>
                <span>副标题</span>
                <input v-model="productForm.subtitle" />
              </label>
              <label>
                <span>状态</span>
                <select v-model="productForm.status">
                  <option value="ON_SALE">ON_SALE</option>
                  <option value="OFF_SALE">OFF_SALE</option>
                </select>
              </label>
              <label class="editor-span">
                <span>标签</span>
                <input v-model="productForm.tagsText" placeholder="多个标签用逗号分隔" />
              </label>
              <label class="editor-span">
                <span>描述</span>
                <textarea v-model="productForm.description" rows="3" />
              </label>
              <label>
                <span>证书信息</span>
                <input v-model="productForm.certificateInfo" />
              </label>
              <label>
                <span>服务信息</span>
                <input v-model="productForm.serviceInfo" />
              </label>
              <label class="checkbox-row">
                <input v-model="productForm.supportCustom" type="checkbox" />
                <span>支持定制</span>
              </label>
              <label class="checkbox-row">
                <input v-model="productForm.hotFlag" type="checkbox" />
                <span>热卖</span>
              </label>
              <label class="checkbox-row">
                <input v-model="productForm.newFlag" type="checkbox" />
                <span>新品</span>
              </label>
            </div>

            <div class="editor-section">
              <div class="section-title">
                <strong>图片</strong>
                <button type="button" class="tiny-button" @click="addMedia">新增图片</button>
              </div>
              <div v-for="(media, index) in productForm.media" :key="index" class="media-row">
                <input v-model="media.mediaUrl" placeholder="图片 URL" />
                <input v-model.number="media.sortOrder" type="number" min="1" />
                <button type="button" class="tiny-button" @click="removeMedia(index)">删除</button>
              </div>
            </div>

            <div class="editor-section">
              <div class="section-title">
                <strong>SKU</strong>
                <button type="button" class="tiny-button" @click="addSku">新增 SKU</button>
              </div>
              <div v-for="(sku, index) in productForm.skus" :key="index" class="sku-editor-row">
                <input v-model="sku.skuCode" placeholder="SKU 编码" />
                <input v-model="sku.material" placeholder="材质" />
                <input v-model="sku.ringSize" placeholder="规格" />
                <input v-model="sku.weightDesc" placeholder="克重" />
                <input v-model.number="sku.salePrice" type="number" min="0" step="0.01" placeholder="售价" />
                <input v-model.number="sku.stock" type="number" min="0" placeholder="库存" />
                <select v-model="sku.status">
                  <option value="ENABLED">ENABLED</option>
                  <option value="DISABLED">DISABLED</option>
                </select>
                <button type="button" class="tiny-button" @click="removeSku(index)">移除</button>
              </div>
            </div>

            <div class="editor-actions">
              <button type="submit">保存商品</button>
              <span>{{ editorMessage }}</span>
            </div>
          </form>
        </section>

        <section v-if="currentView === 'afterSales'" class="panel">
          <div class="panel__header">
            <div>
              <h3>售后审核</h3>
              <p>审核通过后会联动退款状态和订单状态。</p>
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
              <p>Banner 和内容块预览。</p>
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
              <p>客服跟进刻字、尺寸和材质偏好。</p>
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

const fallbackImage = "https://images.unsplash.com/photo-1605100804763-247f67b3557e?auto=format&fit=crop&w=1200&q=80";

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

const categories = ref<any[]>([]);
const products = ref<any[]>([]);
const orders = ref<any[]>([]);
const afterSales = ref<any[]>([]);
const homeConfig = ref<any>({});
const customRequests = ref<any[]>([]);
const editingProduct = ref<any | null>(null);
const editorMessage = ref("");

const productFilters = reactive({
  keyword: "",
  categoryId: "" as number | "",
});

const productForm = reactive({
  categoryId: 0,
  name: "",
  subtitle: "",
  productNo: "",
  basePrice: 0,
  description: "",
  certificateInfo: "",
  serviceInfo: "",
  supportCustom: false,
  hotFlag: false,
  newFlag: false,
  tagsText: "",
  status: "ON_SALE",
  media: [] as any[],
  skus: [] as any[],
});

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
    tasks.push(loadProducts());
    tasks.push(loadCategories());
  } else {
    products.value = [];
    categories.value = [];
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

async function loadProducts() {
  products.value = await adminApi.getProducts({
    keyword: productFilters.keyword.trim(),
    categoryId: productFilters.categoryId,
  });
}

async function loadCategories() {
  categories.value = await adminApi.getCategories();
}

async function resetProductFilters() {
  productFilters.keyword = "";
  productFilters.categoryId = "";
  await loadProducts();
}

function categoryName(categoryId: number) {
  return categories.value.find((item) => item.id === categoryId)?.name || `分类 ${categoryId}`;
}

async function shipOrder(orderNo: string) {
  const company = window.prompt("请输入物流公司", "SF");
  const trackingNo = window.prompt("请输入物流单号", `SF-${Date.now()}`);
  if (!company || !trackingNo) return;
  await adminApi.shipOrder(orderNo, { company, trackingNo });
  await loadDashboard();
}

async function createCategory() {
  const name = window.prompt("请输入分类名称", "Bracelet");
  if (!name) return;
  const icon = window.prompt("请输入分类图标", "B") || "B";
  const sortOrder = Number(window.prompt("请输入排序值", "9") || 9);
  await adminApi.createCategory({ name, icon, sortOrder });
  await loadCategories();
}

async function createSampleProduct() {
  const categoryId = Number(window.prompt("请输入分类 ID", String(categories.value[0]?.id || 1)) || categories.value[0]?.id || 1);
  const suffix = Date.now().toString().slice(-6);
  await adminApi.createProduct({
    categoryId,
    name: `Aurora Ring ${suffix}`,
    subtitle: "后台创建的样例珠宝",
    productNo: `AURORA-${suffix}`,
    basePrice: 3999,
    description: "用于演示商品运营、SKU 库存与售后闭环。",
    certificateInfo: "GIC Certified",
    serviceInfo: "Express shipping and gift box",
    supportCustom: true,
    hotFlag: false,
    newFlag: true,
    tags: ["v2", "ops"],
    status: "ON_SALE",
    media: [
      { mediaType: "IMAGE", mediaUrl: fallbackImage, sortOrder: 1 },
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
  await loadProducts();
}

async function adjustStock(product: any, sku: any) {
  const newStock = Number(window.prompt(`请输入 SKU ${sku.skuCode} 的新库存`, String(sku.stock)) || sku.stock);
  if (Number.isNaN(newStock) || newStock < 0) {
    window.alert("库存必须是大于等于 0 的数字。");
    return;
  }
  const reason = window.prompt("请输入调整原因", "manual replenishment") || "manual replenishment";
  await adminApi.adjustStock(product.id, {
    skuId: sku.id,
    newStock,
    reason,
  });
  await loadProducts();
}

async function openEditor(product: any) {
  const detail = await adminApi.getProduct(product.id);
  editingProduct.value = detail;
  editorMessage.value = "";
  fillProductForm(detail);
}

function fillProductForm(product: any) {
  productForm.categoryId = product.categoryId;
  productForm.name = product.name || "";
  productForm.subtitle = product.subtitle || "";
  productForm.productNo = product.productNo || "";
  productForm.basePrice = Number(product.basePrice || 0);
  productForm.description = product.description || "";
  productForm.certificateInfo = product.certificateInfo || "";
  productForm.serviceInfo = product.serviceInfo || "";
  productForm.supportCustom = Boolean(product.supportCustom);
  productForm.hotFlag = Boolean(product.hotFlag);
  productForm.newFlag = Boolean(product.newFlag);
  productForm.tagsText = (product.tags || []).join(",");
  productForm.status = product.status || "ON_SALE";
  productForm.media = (product.media || []).map((item: any, index: number) => ({
    mediaType: item.mediaType || "IMAGE",
    mediaUrl: item.mediaUrl || "",
    sortOrder: item.sortOrder || index + 1,
  }));
  productForm.skus = (product.skus || []).map((item: any) => ({ ...item }));
}

async function saveProduct() {
  if (!editingProduct.value) return;
  editorMessage.value = "保存中...";
  const payload = {
    categoryId: productForm.categoryId,
    name: productForm.name,
    subtitle: productForm.subtitle,
    productNo: productForm.productNo,
    basePrice: productForm.basePrice,
    description: productForm.description,
    certificateInfo: productForm.certificateInfo,
    serviceInfo: productForm.serviceInfo,
    supportCustom: productForm.supportCustom,
    hotFlag: productForm.hotFlag,
    newFlag: productForm.newFlag,
    tags: productForm.tagsText.split(",").map((tag) => tag.trim()).filter(Boolean),
    status: productForm.status,
    media: productForm.media.map((item, index) => ({
      mediaType: item.mediaType || "IMAGE",
      mediaUrl: item.mediaUrl,
      sortOrder: item.sortOrder || index + 1,
    })),
    skus: productForm.skus,
  };
  editingProduct.value = await adminApi.updateProduct(editingProduct.value.id, payload);
  fillProductForm(editingProduct.value);
  await loadProducts();
  editorMessage.value = "已保存";
}

function closeEditor() {
  editingProduct.value = null;
  editorMessage.value = "";
}

function addMedia() {
  productForm.media.push({ mediaType: "IMAGE", mediaUrl: fallbackImage, sortOrder: productForm.media.length + 1 });
}

function removeMedia(index: number) {
  productForm.media.splice(index, 1);
}

function addSku() {
  productForm.skus.push({
    id: null,
    skuCode: `SKU-${Date.now().toString().slice(-6)}`,
    material: "18K Gold",
    ringSize: "12",
    weightDesc: "3g",
    salePrice: productForm.basePrice || 0,
    stock: 0,
    status: "ENABLED",
  });
}

function removeSku(index: number) {
  if (productForm.skus.length <= 1) {
    window.alert("商品至少需要保留一个 SKU。");
    return;
  }
  productForm.skus.splice(index, 1);
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
    linear-gradient(rgba(154, 125, 69, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(154, 125, 69, 0.05) 1px, transparent 1px),
    linear-gradient(180deg, #f7f0e5 0%, #eadcca 100%);
  background-size: 28px 28px, 28px 28px, auto;
  color: #2d2418;
}

.login-panel,
.workspace {
  min-height: 100vh;
}

.login-panel {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 32px;
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
.panel,
.editor-panel {
  background: rgba(255, 251, 245, 0.88);
  border: 1px solid rgba(152, 119, 56, 0.16);
  box-shadow: 0 24px 60px rgba(81, 59, 20, 0.08);
  backdrop-filter: blur(16px);
}

.hint-card {
  padding: 16px;
  border-radius: 8px;
  display: grid;
  gap: 6px;
}

.hint-card span,
.login-tip,
.panel__header p,
.product-card__body p,
.request-card p {
  color: #7a684d;
}

.login-card {
  padding: 36px;
  border-radius: 8px;
  display: grid;
  gap: 18px;
}

.login-card label,
.product-toolbar label,
.editor-grid label {
  display: grid;
  gap: 8px;
  color: #6a5a44;
}

input,
select,
textarea {
  width: 100%;
  border: 1px solid rgba(133, 104, 53, 0.18);
  border-radius: 8px;
  padding: 12px 14px;
  background: #fffdf9;
  color: #2d2418;
}

textarea {
  resize: vertical;
}

button {
  border: none;
  border-radius: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #2a2015, #8d6a34);
  color: #fff8ef;
  cursor: pointer;
}

.secondary-button,
.button-muted,
.tiny-button {
  background: #efe1c9 !important;
  color: #6f5428 !important;
}

.tiny-button {
  padding: 8px 10px;
  font-size: 13px;
  white-space: nowrap;
}

.workspace {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  padding: 24px;
}

.sidebar {
  border-radius: 8px;
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
  color: #2d2418;
  text-align: left;
  display: grid;
  gap: 6px;
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
.panel,
.editor-panel {
  border-radius: 8px;
  padding: 28px 32px;
}

.hero h1 {
  margin: 10px 0 0;
  font-size: 44px;
  line-height: 1.1;
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
  border-radius: 8px;
  background: rgba(255, 248, 235, 0.92);
}

.metric-card span {
  display: block;
  color: #7d6a49;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 36px;
  color: #7c5b24;
}

.panel__header,
.product-card__top,
.product-card__meta,
.sku-row,
.request-card__top,
.request-card__actions,
.section-title,
.editor-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.panel__header {
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
}

.panel__actions {
  display: flex;
  gap: 12px;
}

.product-toolbar {
  display: grid;
  grid-template-columns: 1.5fr 1fr auto auto;
  gap: 12px;
  align-items: end;
  margin-bottom: 18px;
}

.order-table,
.request-list,
.sku-list,
.editor-section {
  display: grid;
  gap: 14px;
}

.order-row,
.request-card {
  display: grid;
  gap: 16px;
  align-items: center;
  padding: 18px;
  border-radius: 8px;
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
  white-space: nowrap;
}

.product-grid,
.banner-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.product-card,
.banner-card {
  border-radius: 8px;
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

.product-card__body h4 {
  margin: 0;
  font-size: 22px;
}

.product-card__body p {
  margin: 8px 0 0;
}

.product-card__meta {
  margin: 16px 0 12px;
  color: #7e5b26;
  flex-wrap: wrap;
}

.sku-row {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f4ead8;
}

.product-card__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.editor-panel {
  margin-top: 18px;
}

.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.editor-span {
  grid-column: 1 / -1;
}

.checkbox-row {
  display: flex !important;
  grid-template-columns: auto 1fr;
  align-items: center;
}

.checkbox-row input {
  width: auto;
}

.editor-section {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid rgba(133, 104, 53, 0.14);
}

.media-row,
.sku-editor-row {
  display: grid;
  gap: 10px;
}

.media-row {
  grid-template-columns: 1fr 96px auto;
}

.sku-editor-row {
  grid-template-columns: repeat(4, minmax(0, 1fr)) 110px 90px 120px auto;
}

.editor-actions {
  margin-top: 20px;
  justify-content: flex-start;
}

.eyebrow {
  letter-spacing: 0.18em;
  font-size: 12px;
  text-transform: uppercase;
  color: #8f7544;
}

@media (max-width: 1180px) {
  .login-panel,
  .workspace {
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .account-hints,
  .hero__metrics,
  .product-grid,
  .banner-grid,
  .editor-grid,
  .product-toolbar {
    grid-template-columns: 1fr;
  }

  .order-row,
  .media-row,
  .sku-editor-row {
    grid-template-columns: 1fr;
  }
}
</style>
