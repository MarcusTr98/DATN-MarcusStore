import api from '@/utils/api.js'

const WarrantyApi = {
  // Lấy danh sách yêu cầu bảo hành của khách
  getMyWarranties() {
    return api.get('/client/warranties')
  },

  // Lấy chi tiết một yêu cầu bảo hành
  getWarrantyDetail(warrantyId) {
    return api.get(`/client/warranties/${warrantyId}`)
  },

  // Tạo yêu cầu bảo hành mới
  createWarranty(payload) {
    return api.post('/client/warranties', payload)
  },

  // Kiểm tra có thể yêu cầu bảo hành không
  checkCanWarranty(orderItemId) {
    return api.get(`/client/warranties/check/${orderItemId}`)
  },

  // Lấy warranty theo orderItemId (để xem lại thông tin đã gửi)
  getWarrantyByOrderItem(orderItemId) {
    return api.get(`/client/warranties/order-item/${orderItemId}`)
  },
}

// API cho Admin
const AdminWarrantyApi = {
  // Lấy danh sách tất cả yêu cầu bảo hành
  getAllWarranties(params = {}) {
    return api.get('/admin/warranties', { params })
  },

  // Lấy chi tiết một yêu cầu bảo hành
  getWarrantyDetail(warrantyId) {
    return api.get(`/admin/warranties/${warrantyId}`)
  },

  // Cập nhật trạng thái yêu cầu bảo hành
  updateStatus(warrantyId, payload) {
    return api.put(`/admin/warranties/${warrantyId}/status`, payload)
  },
}

export { WarrantyApi, AdminWarrantyApi }
export default WarrantyApi
