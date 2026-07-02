import api from '@/utils/api'

const flashSaleApi = {
  // Lấy danh sách có phân trang + filter
  // params: { page, size, keyword, status }
  getAllFlashSaleSlots(params) {
    return api.get('/admin/flashsales', { params })
  },

  // Lấy thống kê
  getFlashSaleStats(params) {
    return api.get('/admin/flashsales/stats', { params })
  },

  // Lấy chi tiết 1 slot
  getOneFlashSaleSlot(slotId) {
    return api.get(`/admin/flashsale/${slotId}`)
  },

  // Tạo mới
  createFlashSaleSlot(data) {
    return api.post('/admin/flashsale', data)
  },

  // Cập nhật
  updateFlashSaleSlot(slotId, data) {
    return api.put(`/admin/flashsale/${slotId}`, data)
  },

  // Xóa
  deleteFlashSaleSlot(slotId) {
    return api.delete(`/admin/flashsale/${slotId}`)
  },

  // Bật/tắt trạng thái nhanh
  toggleFlashSaleSlot(slotId, status) {
    return api.patch(`/admin/flashsale/${slotId}/status`, { status })
  },
}

export default flashSaleApi
