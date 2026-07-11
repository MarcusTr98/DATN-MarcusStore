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

  // Kiểm tra khung giờ mới có đang đụng flash sale khác không.
  // Trả về danh sách slot overlap (rỗng = OK).
  // FE dùng khi admin nhập startDate/endDate để cảnh báo real-time,
  // BE vẫn validate lại lúc tạo nên đây chỉ là UI helper.
  checkOverlap({ startDate, endDate, excludeSlotId = null } = {}) {
    return api.get('/admin/flashsale/check-overlap', {
      params: { startDate, endDate, excludeSlotId },
    })
  },

  /**
   * Upload file ảnh banner từ thiết bị lên Cloudinary (qua BE).
   * Trả về { imageUrl: "https://res.cloudinary.com/..." } để FE gán vào form.bannerUrl.
   */
  uploadBanner(file) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/admin/flashsale-banner', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

export default flashSaleApi
