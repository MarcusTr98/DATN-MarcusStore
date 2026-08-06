import api from '@/utils/api.js'

export function getWarrantyStats() {
  return api.get('/admin/warranties/stats')
}
