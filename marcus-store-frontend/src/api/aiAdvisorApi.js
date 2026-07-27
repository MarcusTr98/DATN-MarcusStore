import api from '@/utils/api'

// Marcus thêm: AI dùng public API nhưng key nhà cung cấp chỉ tồn tại ở backend.
export const askAiAdvisor = (message, history) =>
  // Marcus sửa: chat có trạng thái đang trả lời riêng trong widget, không che toàn
  // bộ website bằng GlobalSpinner.
  api.post('/public/ai-advisor/chat', { message, history }, { skipGlobalLoading: true })
