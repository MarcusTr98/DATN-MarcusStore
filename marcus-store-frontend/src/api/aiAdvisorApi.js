import api from '@/utils/api'

// Marcus thêm: AI dùng public API nhưng key nhà cung cấp chỉ tồn tại ở backend.
export const askAiAdvisor = (message, history) =>
  api.post('/public/ai-advisor/chat', { message, history })
