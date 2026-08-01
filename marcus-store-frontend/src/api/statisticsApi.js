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
  getKpiSummary(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/kpi-summary${buildQuery({ period, startDate, endDate })}`)
  },

  getKpiCompare(period = 'today', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/kpi-compare${buildQuery({ period, startDate, endDate })}`)
  },

  getRevenueByDay(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/revenue/daily${buildQuery({ period, startDate, endDate })}`)
  },

  getOrdersByWeekday(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/orders/weekday${buildQuery({ period, startDate, endDate })}`)
  },

  getRevenueByBrand(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/revenue/by-brand${buildQuery({ period, startDate, endDate })}`)
  },

  // FIX 7: thêm page + size
  getTopProducts(period = 'month', startDate = '', endDate = '', keyword = '', page = 1, size = 10) {
    return api.get(`/admin/statistics/top-products${buildQuery({ period, startDate, endDate, keyword, page, size })}`)
  },

  // FIX 7: thêm page + size
  getTopCustomers(period = 'month', startDate = '', endDate = '', keyword = '', page = 1, size = 10) {
    return api.get(`/admin/statistics/top-customers${buildQuery({ period, startDate, endDate, keyword, page, size })}`)
  },

  // FIX 7: thêm page + size, bỏ limit
  getRecentOrders(period = 'month', startDate = '', endDate = '', keyword = '', status = '', brand = '', page = 1, size = 10) {
    return api.get(`/admin/statistics/recent-orders${buildQuery({ period, startDate, endDate, keyword, status, brand, page, size })}`)
  },

  // FIX 7: thêm page + size, bỏ limit
  getLowStockProducts(keyword = '', brand = '', status = '', page = 1, size = 10) {
    return api.get(`/admin/statistics/low-stock${buildQuery({ keyword, brand, status, page, size })}`)
  },

  getRevenueCompare(period, startDate = '', endDate = '') {
    return api.get(`/admin/statistics/revenue/compare${buildQuery({ period, startDate, endDate })}`)
  },

  getPendingOrdersCount() {
    return api.get('/admin/statistics/pending-orders/count')
  },

  // FIX 7: thêm page + size, bỏ limit
  getPendingOrders(keyword = '', page = 1, size = 10) {
    return api.get(`/admin/statistics/pending-orders${buildQuery({ keyword, page, size })}`)
  },

  getPaymentStats(period = 'today', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/payment-stats${buildQuery({ period, startDate, endDate })}`)
  },

  getNewUsers(period = 'month', startDate = '', endDate = '') {
    return api.get(`/admin/statistics/users/new${buildQuery({ period, startDate, endDate })}`)
  },

  getChildCategories() {
    return api.get('/admin/categories/children')
  },
}

export default statisticsApi