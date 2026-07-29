import api from '@/utils/api'

// Marcus tách: public chỉ được xem trạng thái tổng quát của Live Chat.
export const getAdminPresence = () => api.get('/public/chat/presence')

// Marcus thêm: mọi dữ liệu phiên được backend suy ra từ tài khoản đăng nhập.
export const startChatSession = () => api.post('/user/live-chat/session')
export const getCurrentChatSession = () => api.get('/user/live-chat/session')
export const getChatHistory = () => api.get('/user/live-chat/history')
export const endChatSession = () => api.delete('/user/live-chat/session')
