import api from '@/utils/api'
export const getAdminPresence = () => {
  return api.get('/public/chat/presence')
}

export const getChatHistory = (roomId) => {
  return api.get(`/public/chat/rooms/${roomId}/history`)
}
