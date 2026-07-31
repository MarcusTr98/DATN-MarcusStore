import api from '@/utils/api.js'

const UserOrderApi = {
  userOrder() {
    return api.get('/orders')
  },

  userOrderDetail(orderCode) {
    return api.get(`/orders/${orderCode}`)
  },

  // Marcus thêm API để khách chỉ xem trạng thái hoàn tiền của chính mình.
  userRefund(orderCode) {
    return api.get(`/orders/${orderCode}/refund`)
  },

  cancelOrder(orderCode, payload = {}) {
    return api.post(`/orders/${orderCode}/cancel`, payload)
  },

  // Marcus thêm: endpoint chuyên biệt, không gửi trạng thái từ client.
  confirmReceived(orderCode) {
    return api.post(`/orders/${orderCode}/confirm-received`)
  },
}

export default UserOrderApi
