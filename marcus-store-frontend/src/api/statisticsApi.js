import api from '@/utils/api'

function buildQuery(params = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      search.append(key, value)
    }
  })
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}

const statisticsApi = {
  // KPI summary
  getKpiSummary(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/kpi-summary${buildQuery({ period, startDate, endDate })}`)
  },

  // Doanh thu theo ngày
  getRevenueByDay(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/revenue/daily${buildQuery({ period, startDate, endDate })}`)
  },

  // Đơn hàng theo thứ
  getOrdersByWeekday(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/orders/weekday${buildQuery({ period, startDate, endDate })}`)
  },

  // Doanh thu theo thương hiệu
  getRevenueByBrand(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/revenue/by-brand${buildQuery({ period, startDate, endDate })}`)
  },

  // Top sản phẩm bán chạy — keyword: lọc theo tên sản phẩm
  getTopProducts(topN = 10, period = 'month', startDate = '', endDate = '', keyword = '') {
    return api.get(`/admin/statistics/top-products${buildQuery({ topN, period, startDate, endDate, keyword })}`)
  },

  // Top khách hàng — keyword: lọc theo tên hoặc email
  getTopCustomers(topN = 10, period = 'month', startDate = '', endDate = '', keyword = '') {
    return api.get(`/admin/statistics/top-customers${buildQuery({ topN, period, startDate, endDate, keyword })}`)
  },

  // Đơn hàng gần nhất — keyword: mã đơn / tên / SĐT; status: trạng thái đơn; brand: thương hiệu SP
  getRecentOrders(limit = 10, period = 'month', startDate = '', endDate = '', keyword = '', status = '', brand = '') {
    return api.get(`/admin/statistics/recent-orders${buildQuery({ limit, period, startDate, endDate, keyword, status, brand })}`)
  },

  // Sản phẩm sắp / hết hàng — keyword: tên SP hoặc SKU; brand: thương hiệu; status: "Hết hàng" / "Sắp hết hàng"
  getLowStockProducts(keyword = '', brand = '', status = '') {
    return api.get(`/admin/statistics/low-stock${buildQuery({ keyword, brand, status })}`)
  },

  // So sánh doanh thu kỳ này vs kỳ trước
  getRevenueCompare(period = 'month') {
    return api.get(`/admin/statistics/revenue/compare${buildQuery({ period })}`)
  },

  // Đếm đơn đang chờ xử lý
  getPendingOrdersCount() {
    return api.get('/admin/statistics/pending-orders/count')
  },

  // Tài khoản mới đăng ký theo ngày
  getNewUsers(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/users/new${buildQuery({ period, startDate, endDate })}`)
  },
}

export default statisticsApi