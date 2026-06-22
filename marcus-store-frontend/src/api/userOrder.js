import api from '@/utils/api.js'

const UserOrderApi = {
  userOrder() {
    return api.get('/orders')
  },

  userOrderDetail(orderCode) {
    return api.get(`/orders/${orderCode}`)
  },
}

export default UserOrderApi
