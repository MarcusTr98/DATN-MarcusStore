import api from '@/utils/api.js'
const OrderDetailApi = {
  getOrderDetail(orderCode) {
    return api.get(`/admin/order/${orderCode}`)
  },
  updateStatusOrder(orderCode, data) {
    return api.put(`/admin/order/${orderCode}`, data)
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
    return api.get(`/admin/order/${orderCode}/imei-preview`)
  },
  assignOrderImeis(orderCode, data) {
    return api.put(`/admin/order/${orderCode}/imeis`, data)
  },
}
export default OrderDetailApi
