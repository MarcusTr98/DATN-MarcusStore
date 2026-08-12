import api from '@/utils/api'

const userApi = {
  getMyProfile() {
    return api.get('/client/profile')
  },
  updateProfile(data) {
    return api.put('/client/profile', data)
  },
  getMyTier() {
    return api.get('/client/profile/tier')
  },
}

export default userApi
