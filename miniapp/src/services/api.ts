import { request } from "../utils/request";

export const api = {
  getHome() {
    return request<any>("/api/app/home");
  },
  getProducts(params: { keyword?: string; categoryId?: number | null }) {
    const queryParts: string[] = [];
    if (params.keyword) {
      queryParts.push(`keyword=${encodeURIComponent(params.keyword)}`);
    }
    if (params.categoryId) {
      queryParts.push(`categoryId=${params.categoryId}`);
    }
    const suffix = queryParts.length ? `?${queryParts.join("&")}` : "";
    return request<any>(`/api/app/products${suffix}`);
  },
  getProductDetail(productId: number) {
    return request<any>(`/api/app/products/${productId}`);
  },
  getCart() {
    return request<any>("/api/app/cart");
  },
  addCartItem(payload: Record<string, unknown>) {
    return request<any>("/api/app/cart/items", "POST", payload);
  },
  deleteCartItem(cartItemId: number) {
    return request<any>(`/api/app/cart/items/${cartItemId}`, "DELETE");
  },
  getAddresses() {
    return request<any[]>("/api/app/addresses");
  },
  createAddress(payload: Record<string, unknown>) {
    return request<any>("/api/app/addresses", "POST", payload);
  },
  updateAddress(addressId: number, payload: Record<string, unknown>) {
    return request<any>(`/api/app/addresses/${addressId}`, "PUT", payload);
  },
  deleteAddress(addressId: number) {
    return request<any>(`/api/app/addresses/${addressId}`, "DELETE");
  },
  bindPhone(payload: { phone: string }) {
    return request<any>("/api/app/auth/bind-phone", "POST", payload);
  },
  refreshToken() {
    return request<any>("/api/app/auth/refresh", "POST");
  },
  createOrder(payload: Record<string, unknown>) {
    return request<any>("/api/app/orders", "POST", payload);
  },
  payOrder(orderNo: string) {
    return request<any>(`/api/app/orders/${orderNo}/pay`, "POST");
  },
  getOrders() {
    return request<any[]>("/api/app/orders");
  },
  getOrderDetail(orderNo: string) {
    return request<any>(`/api/app/orders/${orderNo}`);
  },
  confirmOrder(orderNo: string) {
    return request<any>(`/api/app/orders/${orderNo}/confirm`, "POST");
  },
  getFavorites() {
    return request<any[]>("/api/app/favorites");
  },
  toggleFavorite(productId: number) {
    return request<any>(`/api/app/favorites/${productId}`, "POST");
  },
  submitCustomRequest(payload: Record<string, unknown>) {
    return request<any>("/api/app/custom-requests", "POST", payload);
  },
  getAfterSales() {
    return request<any[]>("/api/app/after-sales");
  },
  submitAfterSale(payload: Record<string, unknown>) {
    return request<any>("/api/app/after-sales", "POST", payload);
  },
};
