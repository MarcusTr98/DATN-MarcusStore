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

  getCancellationReasons(params) {
    return get('/admin/analytics/cancellation-reasons', params)
  },

  // Marcus thêm: đọc dữ liệu bảo hành đã tổng hợp cho Analytics.
  getWarrantyQuality(params) {
    return get('/admin/analytics/warranty-quality', params)
  },
  getBehaviorFunnel(params) {
    return get('/admin/analytics/behavior-funnel', params)
  },

  getSavedAiReport(params) {
    return get('/admin/analytics/ai-report', params)
  },

  // Marcus thêm: đọc telemetry AI đã ẩn danh, không chứa nội dung hội thoại.
  getAiUsageSummary(params) {
    return get('/admin/ai-advisor/usage-summary', params)
  },

  getAiSalesFunnel(params) {
    return get('/admin/ai-advisor/sales-funnel', params)
  },
  getActions() {
    return get('/admin/analytics/actions')
  },
  acceptAction(action) {
    return api.post('/admin/analytics/actions', action, { skipGlobalLoading: true })
  },
  updateActionStatus(actionId, status) {
    return api.patch(
      `/admin/analytics/actions/${actionId}/status`,
      { status },
      { skipGlobalLoading: true },
    )
  },

  generateAiReport(params) {
    const query = queryString(params)
    // Marcus thêm: POST là thao tác duy nhất gọi AI; GET phía trên chỉ đọc DB.
    return api.post(`/admin/analytics/ai-report${query ? `?${query}` : ''}`, null, {
      skipGlobalLoading: true,
      // Marcus sửa: backend chờ Gemini tối đa 60 giây; frontend phải sống lâu
      // hơn để không tự hủy một báo cáo vẫn đang được nhà cung cấp xử lý.
      timeout: 75_000,
    })
  },
}

export default analyticsApi
