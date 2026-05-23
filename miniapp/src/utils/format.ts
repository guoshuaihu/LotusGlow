export function formatMoney(value: unknown) {
  const amount = Number(value || 0);
  if (!Number.isFinite(amount)) {
    return "¥0";
  }
  return `¥${amount.toLocaleString("zh-CN", {
    minimumFractionDigits: amount % 1 === 0 ? 0 : 2,
    maximumFractionDigits: 2,
  })}`;
}

export function formatOrderStatus(status: string) {
  const statusMap: Record<string, string> = {
    WAITING_PAYMENT: "待支付",
    WAITING_SHIPMENT: "待发货",
    SHIPPED: "运输中",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
    AFTER_SALE: "售后中",
    REFUNDED: "已退款",
  };
  return statusMap[status] || status;
}

export function formatAfterSaleStatus(status: string) {
  const statusMap: Record<string, string> = {
    PENDING: "待审核",
    APPROVED: "已通过",
    REJECTED: "已拒绝",
    REFUNDED: "已退款",
  };
  return statusMap[status] || status;
}

export function formatRefundStatus(status: string) {
  const statusMap: Record<string, string> = {
    NONE: "未退款",
    PENDING: "退款中",
    SUCCESS: "退款成功",
    FAILED: "退款失败",
  };
  return statusMap[status] || status || "未退款";
}
