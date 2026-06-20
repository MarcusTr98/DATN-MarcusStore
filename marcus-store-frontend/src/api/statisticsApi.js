import api from '@/utils/api'

const statisticsApi = {
  getRevenueByDay(period = 'month') {
    return api.get(`/admin/statistics/revenue/daily?period=${period}`)
  },
  getRevenueByMonth() {
    const year = new Date().getFullYear()
    return api.get(`/admin/statistics/revenue/monthly?year=${year}`)
  },
  getOrdersByWeekday(period = 'month') {
    return api.get(`/admin/statistics/orders/weekday?period=${period}`)
  },
  getRevenueByBrand(period = 'month') {
    return api.get(`/admin/statistics/revenue/by-brand?period=${period}`)
  },
  getTopProducts(topN = 10, period = 'month') {
    return api.get(`/admin/statistics/top-products?topN=${topN}&period=${period}`)
  },
  getTopCustomers(topN = 10, period = 'month') {
    return api.get(`/admin/statistics/top-customers?topN=${topN}&period=${period}`)
  },
  getRecentOrders(limit = 10, period = 'month') {
    return api.get(`/admin/statistics/recent-orders?limit=${limit}&period=${period}`)
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