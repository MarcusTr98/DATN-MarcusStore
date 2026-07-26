import api from '@/utils/api'

// Marcus sửa: toàn bộ thao tác phòng chat quản trị nằm dưới endpoint được phân quyền.
export const getActiveRooms = () => api.get('/admin/live-chat/active-rooms')
export const getChatHistory = (roomId) =>
  api.get(`/admin/live-chat/rooms/${encodeURIComponent(roomId)}/history`)
export const claimRoomChat = (roomId) =>
  api.put(`/admin/live-chat/rooms/${encodeURIComponent(roomId)}/claim`)
export const endRoomChat = (roomId) =>
  api.delete(`/admin/live-chat/rooms/${encodeURIComponent(roomId)}`)
export const getChatAvailability = () => api.get('/admin/live-chat/availability')
export const updateChatAvailability = (available) =>
  api.put('/admin/live-chat/availability', { available })
