import api from '@/utils/api'

function queryString(params = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      search.set(key, value)
    }
  })
  return search.toString()
}

function get(path, params) {
  const query = queryString(params)
  // Marcus thêm: trang Phân tích dùng loading cục bộ, không khóa toàn bộ website.
  return api.get(`${path}${query ? `?${query}` : ''}`, { skipGlobalLoading: true })
}

const analyticsApi = {
  getOverview(params) {
    return get('/admin/analytics/overview', params)
  },

  getSalesTrend(params) {
    return get('/admin/analytics/sales-trend', params)
  },

  getProductTrends(params) {
    return get('/admin/analytics/product-trends', params)
  },

  getSavedAiReport(params) {
    return get('/admin/analytics/ai-report', params)
  },

  generateAiReport(params) {
    const query = queryString(params)
    // Marcus thêm: POST là thao tác duy nhất gọi AI; GET phía trên chỉ đọc DB.
    return api.post(`/admin/analytics/ai-report${query ? `?${query}` : ''}`, null, {
      skipGlobalLoading: true,
      timeout: 35_000,
    })
  },
}

export default analyticsApi
