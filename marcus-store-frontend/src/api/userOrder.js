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
}

export default UserOrderApi
