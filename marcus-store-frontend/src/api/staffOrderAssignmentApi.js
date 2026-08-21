import api from '@/utils/api.js'

export default {
  getStatus() {
    return api.get('/admin/staff/order-assignments/status')
  },
  setAvailability(acceptingOrders) {
    return api.put('/admin/staff/order-assignments/availability', { acceptingOrders })
  },
  claimNext() {
    return api.post('/admin/staff/order-assignments/claim-next')
  },
}
