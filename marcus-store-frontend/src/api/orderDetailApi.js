import api from '@/utils/api.js'
const OrderDetailApi = {
  getOrderDetail(orderCode) {
    return api.get(`/admin/orders/${orderCode}`)
  },
  updateStatusOrder(orderCode, data) {
    return api.put(`/admin/orders/${orderCode}/status`, data)
  },
  assignOrderToStaff(orderCode, data) {
    return api.put(`/admin/order-assignments/${orderCode}`, data)
  },
  // Marcus thêm: Admin thử tạo lại vận đơn khi GHN trả lỗi hoặc mất kết nối.
  retryGhnShipment(orderCode) {
    return api.post(`/admin/orders/${orderCode}/shipment/retry`)
  },

  // marcus them refund
  getRefund(orderCode) {
    return api.get(`/admin/orders/${orderCode}/refund`)
  },
  createRefund(orderCode, reason) {
    return api.post(`/admin/orders/${orderCode}/refunds`, { reason })
  },
  approveRefund(refundId) {
    return api.post(`/admin/refunds/${refundId}/approve`)
  },
  retryRefund(refundId) {
    return api.post(`/admin/refunds/${refundId}/retry`)
  },
  // Marcus thêm API QueryDR để admin chủ động đối soát trạng thái VNPAY.
  reconcileRefund(refundId) {
    return api.post(`/admin/refunds/${refundId}/reconcile`)
  },
  // Marcus thêm xác nhận thủ công chỉ dành cho Sandbox, kèm ghi chú audit.

  getImeiPreview(orderCode) {
    return api.get(`/admin/orders/${orderCode}/imeis/preview`)
  },
  assignOrderImeis(orderCode, data) {
    return api.put(`/admin/orders/${orderCode}/imeis`, data)
  },

  startProcessingWithImei(orderCode, requests) {
    return api.post(`/admin/orders/${orderCode}/imeis/processing`, requests)
  },
}
export default OrderDetailApi
