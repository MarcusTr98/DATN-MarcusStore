import api from '@/utils/api'

const userApi = {
  getMyProfile() {
    return api.get('/client/profile')
  },
  updateProfile(data) {
    return api.put('/client/profile', data)
  },
}

export default userApi
