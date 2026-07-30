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
    // Marcus sửa: dùng object DTO thay vì gửi boolean thô để backend validate rõ ràng.
    return api.post(`/admin/finance-reports/${id}/reconcile`, { status })
  },
}

export default financialApi
