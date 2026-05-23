import { reactive } from "vue";
import { DEMO_LOGIN } from "../config/env";
import { request } from "../utils/request";

const TOKEN_KEY = "JEWELRY_TOKEN";
const USER_KEY = "JEWELRY_USER";

type SessionUser = {
  id: number;
  nickname: string;
  phone?: string;
  avatarUrl?: string;
  openId?: string;
  unionId?: string;
  lastLoginAt?: string;
};

export const sessionState = reactive<{
  token: string;
  user: null | SessionUser;
  ready: boolean;
}>({
  token: uni.getStorageSync(TOKEN_KEY) || "",
  user: uni.getStorageSync(USER_KEY) || null,
  ready: false,
});

export function getToken() {
  return sessionState.token;
}

export function updateSession(token: string, user: SessionUser) {
  sessionState.token = token;
  sessionState.user = user;
  sessionState.ready = true;
  uni.setStorageSync(TOKEN_KEY, token);
  uni.setStorageSync(USER_KEY, user);
}

export async function ensureSession() {
  if (sessionState.token) {
    sessionState.ready = true;
    return sessionState.token;
  }
  const loginData = await request<{
    token: string;
    user: SessionUser;
  }>("/api/app/auth/wx-login", "POST", DEMO_LOGIN);
  updateSession(loginData.token, loginData.user);
  return loginData.token;
}

export async function refreshSession() {
  const data = await request<{
    token: string;
    user: SessionUser;
  }>("/api/app/auth/refresh", "POST");
  updateSession(data.token, data.user);
  return data;
}
