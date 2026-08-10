import api from '@/utils/api'

const SESSION_KEY = 'MARCUS_BEHAVIOR_SESSION'

export const getBehaviorSessionId = () => {
  let value = sessionStorage.getItem(SESSION_KEY)
  if (!value) {
    value = crypto.randomUUID()
    sessionStorage.setItem(SESSION_KEY, value)
  }
  return value
}

// Marcus thêm: chỉ gửi loại event, UUID phiên và productId; không gửi nội dung/IP/userId.
export const trackBehavior = (eventType, productId = null) =>
  api.post(
    '/public/behavior/events',
    { eventType, productId, sessionId: getBehaviorSessionId() },
    { skipGlobalLoading: true },
  )
