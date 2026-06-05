import api from '@/utils/api'

export const loginApi = (data) => {
  return api.post('/auth/login', data)
}