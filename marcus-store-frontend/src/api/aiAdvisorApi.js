import api from '@/utils/api'

// Marcus thêm: AI dùng public API nhưng key nhà cung cấp chỉ tồn tại ở backend.
export const askAiAdvisor = (message, history, sessionId) =>
  // Marcus sửa: chat có trạng thái đang trả lời riêng trong widget, không che toàn
  // bộ website bằng GlobalSpinner.
  api.post('/public/ai-advisor/chat', { message, history, sessionId }, { skipGlobalLoading: true })

// Marcus thêm: dùng fetch để đọc POST SSE; EventSource mặc định không hỗ trợ body.
export const streamAiAdvisor = async (message, history, sessionId, handlers = {}) => {
  const response = await fetch(`${api.defaults.baseURL}/public/ai-advisor/chat-stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify({ message, history, sessionId }),
  })

  if (!response.ok || !response.body) {
    const errorBody = await response.json().catch(() => ({}))
    throw new Error(errorBody.message || 'Không thể kết nối Marcus AI.')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''

    events.forEach((block) => {
      const eventName = block.match(/^event:(.+)$/m)?.[1]?.trim() || 'message'
      const rawData = block.match(/^data:(.+)$/m)?.[1]?.trim()
      if (!rawData) return
      const data = JSON.parse(rawData)
      if (eventName === 'token') handlers.onToken?.(data.token)
      if (eventName === 'done') handlers.onDone?.(data)
      if (eventName === 'advisor-error') throw new Error(data.message)
    })
  }
}

// Marcus thêm: tracking tối thiểu, không gửi nội dung chat hay thông tin người dùng.
export const trackAiProductClick = (productId, sessionId) =>
  api.post(
    '/public/ai-advisor/product-click',
    { productId, sessionId },
    { skipGlobalLoading: true },
  )
