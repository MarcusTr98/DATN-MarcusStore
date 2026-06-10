import api from '@/utils/api'

const  voucherApi = {
  getAllVoucher(){
    return api.get('/admin/vouchers')
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
