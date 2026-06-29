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

  // Đơn hàng theo thứ (chỉ dùng khi period = week)
  getOrdersByWeekday(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/orders/weekday${buildQuery({ period, startDate, endDate })}`)
  },

  // Doanh thu theo thương hiệu
  getRevenueByBrand(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/revenue/by-brand${buildQuery({ period, startDate, endDate })}`)
  },

  // Top sản phẩm bán chạy
  getTopProducts(topN = 10, period = 'month', startDate = '', endDate = '', keyword = '') {
    return api.get(`/admin/statistics/top-products${buildQuery({ topN, period, startDate, endDate, keyword })}`)
  },

  // Top khách hàng
  getTopCustomers(topN = 10, period = 'month', startDate = '', endDate = '', keyword = '') {
    return api.get(`/admin/statistics/top-customers${buildQuery({ topN, period, startDate, endDate, keyword })}`)
  },

  // Đơn hàng gần nhất
  getRecentOrders(limit = 10, period = 'month', startDate = '', endDate = '', keyword = '', status = '', brand = '') {
    return api.get(`/admin/statistics/recent-orders${buildQuery({ limit, period, startDate, endDate, keyword, status, brand })}`)
  },

  // Sản phẩm sắp / hết hàng (không phụ thuộc period)
  getLowStockProducts(keyword = '', brand = '', status = '') {
    return api.get(`/admin/statistics/low-stock${buildQuery({ keyword, brand, status })}`)
  },

  // So sánh doanh thu kỳ này vs kỳ trước
  getRevenueCompare(period, startDate = '', endDate = '') {
  return api.get(`/admin/statistics/revenue/compare${buildQuery({ period, startDate, endDate })}`)
},
  // Đếm đơn đang chờ xử lý
  getPendingOrdersCount() {
    return api.get('/admin/statistics/pending-orders/count')
  },

  // Tài khoản mới đăng ký theo ngày
  getNewUsers(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/users/new${buildQuery({ period, startDate, endDate })}`)
  },

  getChildCategories() {
    return api.get('/admin/categories/children')
  },
  
}

export default statisticsApi