import api from '@/utils/api'

const addressApi = {
  getMyAddresses() {
    return api.get('/client/addresses')
  },
  getAddressById(addressId) {
    return api.get(`/client/addresses/${addressId}`)
  },
  addAddress(data) {
    return api.post('/client/addresses', data)
  },
  updateAddress(addressId, data) {
    return api.put(`/client/addresses/${addressId}`, data)
  },
  setAsDefault(addressId) {
    return api.put(`/client/addresses/${addressId}/default`)
  },
  deleteAddress(addressId) {
    return api.delete(`/client/addresses/${addressId}`)
  },
}

export default addressApi
