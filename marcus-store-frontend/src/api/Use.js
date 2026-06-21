import api from "@/utils/api"



const API_URL = '/admin/user'

export default {
  getAll(params) {
    return api.get(API_URL, { params })
  },

  getById(id) {
    return api.get(`${API_URL}/${id}`)
  },

  create(data) {
    return api.post(API_URL, data)
  },

  update(id, data) {
    return api.put(`${API_URL}/${id}`, data)
  },

  lock(id) {
    return api.put(`${API_URL}/${id}/lock`)
  },

  unlock(id) {
    return api.put(`${API_URL}/${id}/unLock`)
  }
}