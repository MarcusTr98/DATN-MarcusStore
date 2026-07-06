import api from '@/utils/api'
export default api

// Lấy toàn bộ phòng chat đang có dữ liệu (chưa nhận + đã nhận)
// Dùng khi Admin vừa load trang, để không bỏ lỡ chat đến trước khi Admin online
export const getActiveRooms = () => {
  return api.get('/admin/chat/active-rooms')
}

// Lấy lịch sử tin nhắn của 1 phòng cụ thể (dùng chung endpoint với Client)
export const getChatHistory = (roomId) => {
  return api.get(`/client/chat/rooms/${roomId}/history`)
}

export const claimRoomChat = (roomId) => {
  return api.put(`/public/chat/rooms/${roomId}/claim`)
}
