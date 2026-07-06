import api from '@/utils/api.js'

const UserOrderApi = {
  userOrder() {
    return api.get('/orders')
  },

  userOrderDetail(orderCode) {
    return api.get(`/orders/${orderCode}`)
  },

  cancelOrder(orderCode, payload = {}) {
    return api.post(`/orders/${orderCode}/cancel`, payload)
  },
}

export default UserOrderApi
