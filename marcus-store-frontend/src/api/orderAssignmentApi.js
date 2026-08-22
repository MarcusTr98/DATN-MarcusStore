import api from '@/utils/api.js'

export default {
  getDashboard(params = {}) {
    return api.get('/admin/order-assignments/dashboard', { params })
  },
  assign(orderCode, payload) {
    return api.put(`/admin/order-assignments/${orderCode}`, payload)
  },
  updateStaffSettings(staffId, payload) {
    return api.put(`/admin/order-assignments/staff/${staffId}/settings`, payload)
  },
}
