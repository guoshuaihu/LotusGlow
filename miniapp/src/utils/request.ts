import { API_BASE_URL } from "../config/env";
import { getToken } from "../services/session";

type RequestMethod = "GET" | "POST" | "PUT" | "DELETE";
type RequestPayload = Record<string, unknown> | string | ArrayBuffer | undefined;

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export function request<T>(url: string, method: RequestMethod = "GET", data?: RequestPayload) {
  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${url}`,
      method,
      data,
      header: getToken()
        ? {
            Authorization: `Bearer ${getToken()}`,
          }
        : {},
      success: (response) => {
        const result = response.data as ApiResponse<T>;
        if (response.statusCode && response.statusCode >= 200 && response.statusCode < 300 && result.code === 0) {
          resolve(result.data);
          return;
        }
        uni.showToast({
          title: result?.message || "请求失败",
          icon: "none",
        });
        reject(result);
      },
      fail: (error) => {
        uni.showToast({
          title: "网络连接失败",
          icon: "none",
        });
        reject(error);
      },
    });
  });
}
