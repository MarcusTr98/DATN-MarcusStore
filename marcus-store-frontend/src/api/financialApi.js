import api from '@/utils/api'

const financialApi = {
  getTransactions() {
    return api.get('/admin/finance-reports/list')
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
