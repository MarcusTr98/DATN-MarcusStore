// src/api/AuditLogApi.js
import api from '@/utils/api'

export const auditLogApi = {
  // GET /api/admin/audit-logs -> ApiResponse<Page<AuditLogResponseDTO>>
  // BE chỉ nhận Pageable (không có filter server-side) -> lấy 1 lượt lớn,
  // lọc/phân trang phía client giống cách đang làm với Posts.
  getAll: (params) =>
    api
      .get('/admin/audit-logs', { params: { size: 1000, sort: 'createdAt,desc', ...params } })
      .then((r) => r.data.data.content),

  getOne: (id) => api.get(`/admin/audit-logs/${id}`).then((r) => r.data.data),

  // GET /api/admin/audit-logs/export -> file CSV (không phải JSON, cần responseType blob)
  exportCsv: () =>
    api.get('/admin/audit-logs/export', { responseType: 'blob' }).then((r) => r.data),
}

export default auditLogApi