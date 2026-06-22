import api from '@/utils/api'

const statisticsApi = {
  getRevenueByDay(period = 'month', startDate = '', endDate = '') {
    const dateParams = startDate ? `&startDate=${startDate}&endDate=${endDate}` : ''
    return api.get(`/admin/statistics/revenue/daily?period=${period}${dateParams}`)
  },
  getRevenueByMonth() {
    const year = new Date().getFullYear()
    return api.get(`/admin/statistics/revenue/monthly?year=${year}`)
  },
  getOrdersByWeekday(period = 'month', startDate = '', endDate = '') {
    const dateParams = startDate ? `&startDate=${startDate}&endDate=${endDate}` : ''
    return api.get(`/admin/statistics/orders/weekday?period=${period}${dateParams}`)
  },
  getRevenueByBrand(period = 'month', startDate = '', endDate = '') {
    const dateParams = startDate ? `&startDate=${startDate}&endDate=${endDate}` : ''
    return api.get(`/admin/statistics/revenue/by-brand?period=${period}${dateParams}`)
  },
  getTopProducts(topN = 10, period = 'month', startDate = '', endDate = '') {
    const dateParams = startDate ? `&startDate=${startDate}&endDate=${endDate}` : ''
    return api.get(`/admin/statistics/top-products?topN=${topN}&period=${period}${dateParams}`)
  },
  getTopCustomers(topN = 10, period = 'month', startDate = '', endDate = '') {
    const dateParams = startDate ? `&startDate=${startDate}&endDate=${endDate}` : ''
    return api.get(`/admin/statistics/top-customers?topN=${topN}&period=${period}${dateParams}`)
  },
  getRecentOrders(limit = 10, period = 'month', startDate = '', endDate = '') {
    const dateParams = startDate ? `&startDate=${startDate}&endDate=${endDate}` : ''
    return api.get(`/admin/statistics/recent-orders?limit=${limit}&period=${period}${dateParams}`)
  },
  getLowStockProducts() {
    return api.get('/admin/statistics/low-stock')
  },
  getRevenueCompare(period = 'month') {
    return api.get(`/admin/statistics/revenue/compare?period=${period}`)
  },
  getPendingOrdersCount() {
    return api.get('/admin/statistics/pending-orders/count')
  },
}

export default statisticsApi