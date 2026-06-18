import api from '@/utils/api'

const statisticsApi = {
  getRevenueByDay() {
    return api.get('/admin/statistics/revenue/daily')
  },
  getRevenueByMonth() {
    return api.get('/admin/statistics/revenue/monthly')
  },
  getTopProducts(topN = 5) {
    return api.get(`/admin/statistics/top-products?topN=${topN}`)
  },
  getOrdersByWeekday() {
    return api.get('/admin/statistics/orders/weekday')
  },
  getRevenueByBrand() {
    return api.get('/admin/statistics/revenue/by-brand')
  },
  getLowStockProducts() {
    return api.get('/admin/statistics/low-stock')
  },
  getTopCustomers(topN = 10) {
    return api.get(`/admin/statistics/top-customers?topN=${topN}`)
  },
  getRecentOrders(limit = 10) {
    return api.get(`/admin/statistics/recent-orders?limit=${limit}`)
  },
}

export default statisticsApi