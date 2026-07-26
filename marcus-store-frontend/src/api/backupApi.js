import api from '@/utils/api'

const BASE_URL = '/admin/backups'

// Marcus thêm: toàn bộ request backup dùng API có JWT; backend vẫn là lớp chặn quyền chính.
export const backupApi = {
  getOverview() {
    return api.get(`${BASE_URL}/overview`, { skipGlobalLoading: true })
  },
  getHistory() {
    return api.get(BASE_URL, { skipGlobalLoading: true })
  },
  create(type, note) {
    return api.post(BASE_URL, { type, note }, { skipGlobalLoading: true })
  },
  download(id) {
    return api.get(`${BASE_URL}/${id}/download`, {
      responseType: 'blob',
      timeout: 120000,
      skipGlobalLoading: true,
    })
  },
  remove(id) {
    return api.delete(`${BASE_URL}/${id}`, { skipGlobalLoading: true })
  },
}
