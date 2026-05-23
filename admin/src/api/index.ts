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
  getProducts() {
    return apiRequest<any[]>("/api/admin/products");
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
  adjustStock(productId: number, payload: { skuId: number; newStock: number; reason: string }) {
    return apiRequest<any>(`/api/admin/products/${productId}/stock`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
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
