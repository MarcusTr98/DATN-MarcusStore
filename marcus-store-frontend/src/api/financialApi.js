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
}

export default financialApi
