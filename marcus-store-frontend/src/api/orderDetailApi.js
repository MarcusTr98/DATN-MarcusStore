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
}
export default OrderDetailApi
