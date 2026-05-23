const API_BASE_URL = "http://127.0.0.1:8080";

interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

function getToken() {
  return localStorage.getItem("JEWELRY_ADMIN_TOKEN") || "";
}

export async function apiRequest<T>(url: string, options: RequestInit = {}) {
  const response = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(getToken()
        ? {
            Authorization: `Bearer ${getToken()}`,
          }
        : {}),
      ...(options.headers || {}),
    },
  });
  const result = (await response.json()) as ApiEnvelope<T>;
  if (!response.ok || result.code !== 0) {
    throw new Error(result.message || "请求失败");
  }
  return result.data;
}

export function persistToken(token: string) {
  localStorage.setItem("JEWELRY_ADMIN_TOKEN", token);
}
