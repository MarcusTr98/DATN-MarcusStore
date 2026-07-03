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

  /**
   * Lấy cây brand -> categoryL2 -> sku để admin chọn sản phẩm cho Flash Sale.
   * @param {Object} options
   * @param {boolean} options.includeOutOfStock - true = lấy cả SKU hết hàng
   *   (stockQuantity = 0). Mặc định false cho modal Flash Sale.
   */
  getProductCascade({ includeOutOfStock = false } = {}) {
    return api.get('/admin/products/cascade', {
      params: { includeOutOfStock },
    })
  },

  /** Lấy cây của 1 brand cụ thể (ít dùng). */
  getProductCascadeByBrand(brand, { includeOutOfStock = false } = {}) {
    return api.get(`/admin/products/cascade/${encodeURIComponent(brand)}`, {
      params: { includeOutOfStock },
    })
  },
}

export default flashSaleApi
