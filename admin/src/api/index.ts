import { apiRequest, persistToken } from "./client";

export const adminApi = {
  async login(payload: { username: string; password: string }) {
    const data = await apiRequest<{
      token: string;
      username: string;
      displayName: string;
      roleName: string;
      permissions: string[];
    }>("/api/admin/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    persistToken(data.token);
    return data;
  },
  getCategories() {
    return apiRequest<any[]>("/api/admin/categories");
  },
  getProducts(params: { keyword?: string; categoryId?: number | ""; lowStockOnly?: boolean; stockThreshold?: number | "" } = {}) {
    const search = new URLSearchParams();
    if (params.keyword) {
      search.set("keyword", params.keyword);
    }
    if (params.categoryId) {
      search.set("categoryId", String(params.categoryId));
    }
    if (params.lowStockOnly) {
      search.set("lowStockOnly", "true");
    }
    if (params.stockThreshold !== undefined && params.stockThreshold !== "") {
      search.set("stockThreshold", String(params.stockThreshold));
    }
    const query = search.toString();
    return apiRequest<any[]>(`/api/admin/products${query ? `?${query}` : ""}`);
  },
  getProduct(productId: number) {
    return apiRequest<any>(`/api/admin/products/${productId}`);
  },
  createCategory(payload: { name: string; icon: string; sortOrder: number }) {
    return apiRequest<any>("/api/admin/categories", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  createProduct(payload: Record<string, unknown>) {
    return apiRequest<any>("/api/admin/products", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  updateProduct(productId: number, payload: Record<string, unknown>) {
    return apiRequest<any>(`/api/admin/products/${productId}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
  batchUpdateProducts(payload: { productIds: number[]; status?: string; categoryId?: number; tags?: string[] }) {
    return apiRequest<any>("/api/admin/products/batch", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  adjustStock(productId: number, payload: { skuId: number; newStock: number; reason: string }) {
    return apiRequest<any>(`/api/admin/products/${productId}/stock`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
  getInventoryRecords(productId: number, skuId?: number) {
    const search = new URLSearchParams();
    if (skuId) {
      search.set("skuId", String(skuId));
    }
    const query = search.toString();
    return apiRequest<any[]>(`/api/admin/products/${productId}/inventory-records${query ? `?${query}` : ""}`);
  },
  getOrders() {
    return apiRequest<any[]>("/api/admin/orders");
  },
  shipOrder(orderNo: string, payload: { company: string; trackingNo: string }) {
    return apiRequest<any>(`/api/admin/orders/${orderNo}/ship`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  getAfterSales() {
    return apiRequest<any[]>("/api/admin/after-sales");
  },
  auditAfterSale(afterSaleId: number, payload: { approved: boolean; remark?: string }) {
    return apiRequest<any>(`/api/admin/after-sales/${afterSaleId}/audit`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  getHomeConfig() {
    return apiRequest<any>("/api/admin/content/home");
  },
  getCustomRequests() {
    return apiRequest<any[]>("/api/admin/custom-requests");
  },
};
