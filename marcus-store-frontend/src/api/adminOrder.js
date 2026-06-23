import api from '@/utils/api.js'

const AdminOrderApi = {
  // 1. Lấy danh sách đơn hàng (có phân trang và lọc)
  getAllOrders(params) {
    return api.get('/admin/orders', { params })
  },

  // 2. Lấy thống kê đơn hàng (tổng số, pending, completed...)
  getOrderStats(params) {
    return api.get('/admin/orders/stats', { params })
  },

  // 3. Lấy các option để filter (phương thức thanh toán, trạng thái)
  getOrderFilterOptions() {
    return api.get('/admin/orders/filter-options')
  },

  // 4. Lấy chi tiết 1 đơn hàng theo mã
  userOrderDetail(orderCode) {
    return api.get(`/admin/order/${orderCode}`)
  },

  // 5. Cập nhật trạng thái đơn hàng
  updateOrderStatus(orderCode, payload) {
    return api.put(`/admin/order/${orderCode}`, payload)
  },
}

export default AdminOrderApi
