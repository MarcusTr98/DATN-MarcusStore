import api from '@/utils/api'

const financialApi = {
  // Marcus thêm: truyền bộ lọc ngày xuống query backend để lấy đúng khoảng đối soát.
  getTransactions(fromDate = '', toDate = '') {
    return api.get('/admin/finance-reports/list', {
      params: {
        ...(fromDate ? { fromDate } : {}),
        ...(toDate ? { toDate } : {}),
      },
    })
  },

  exportExcel() {
    return api.get('/admin/finance-reports/export', {
      responseType: 'blob',
    })
  },
  reconcile(id, status) {
    return api.post(`/admin/finance-reports/${id}/reconcile`, status, {
      headers: { 'Content-Type': 'application/json' },
    })
  },
}

export default financialApi
