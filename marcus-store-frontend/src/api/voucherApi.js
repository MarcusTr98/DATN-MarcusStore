import api from '@/utils/api'

const  voucherApi = {
  getAllVoucher(params){
    return api.get('/admin/vouchers', { params })
  },
  getVoucherStats(params) {
    return api.get('/admin/vouchers/stats', { params })
  },

  getOneVoucher(voucherId){
  return api.get(`/admin/voucher/${voucherId}`)
  },
  deleteVoucherById(voucherId) {
    return api.delete(`/admin/voucher/${voucherId}`)
  },

  createVoucher(data) {
    return api.post('/admin/voucher', data)
  },
  updateVoucher(voucherId, data) {
    return api.put(`/admin/voucher/${voucherId}`, data)
  },
}
export default voucherApi
