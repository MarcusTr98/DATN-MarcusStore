import api from '@/utils/api.js'

export default {
  getDashboard() {
    return api.get('/admin/order-assignments/dashboard')
  },
  assign(orderCode, payload) {
    return api.put(`/admin/order/${orderCode}/assignment`, payload)
  },
}
