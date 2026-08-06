import axios from 'axios'
import { useLoadingStore } from '@/stores/useLoadingStore'

// Marcus sửa: dùng cùng cấu hình môi trường với WebSocket, tránh REST bị gọi cứng về localhost.
const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/+$/, '')

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    // Marcus sửa: instance mặc định gửi JSON, nhưng upload phải để trình duyệt tự
    // sinh Content-Type multipart/form-data kèm boundary. Nếu giữ application/json,
    // Spring sẽ báo "Current request is not a multipart request".
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

    // 1. Kích hoạt hiệu ứng loading khi bắt đầu gọi API
    if (!config.skipGlobalLoading) useLoadingStore().show()

    // 2. Đảm bảo lấy đúng key ACCESS_TOKEN
    const token = localStorage.getItem('ACCESS_TOKEN')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    // Tắt loading nếu request bị lỗi ngay từ client
    if (!error.config?.skipGlobalLoading) useLoadingStore().hide()
    return Promise.reject(error)
  },
)

api.interceptors.response.use(
  (response) => {
    // Tắt loading khi API trả về kết quả thành công
    if (!response.config.skipGlobalLoading) useLoadingStore().hide()
    return response
  },
  (error) => {
    // Tắt loading khi API trả về lỗi (4xx, 5xx)
    if (!error.config?.skipGlobalLoading) useLoadingStore().hide()

    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401: {
          console.error('Lỗi 401: Bạn chưa đăng nhập')

          // Chỉ redirect nếu người dùng đã từng đăng nhập (có token)
          const token = localStorage.getItem('ACCESS_TOKEN')

          if (token) {
            localStorage.removeItem('ACCESS_TOKEN')
            localStorage.removeItem('USERNAME')
            localStorage.removeItem('USER_ROLE')

            window.dispatchEvent(new Event('auth-changed'))

            window.location.href = '/auth/login'
          } else {
            // Guest (không có token) → dispatch event để các page hiển thị modal đăng nhập
            window.dispatchEvent(
              new CustomEvent('auth-required', {
                detail: { message: 'Vui lòng đăng nhập để tiếp tục.' },
              }),
            )
          }

          break
        }
// case 403: {
//   const token = localStorage.getItem('ACCESS_TOKEN')

//   if (token) {
//     localStorage.removeItem('ACCESS_TOKEN')
//     localStorage.removeItem('USERNAME')
//     localStorage.removeItem('USER_ROLE')

//     window.dispatchEvent(new Event('auth-changed'))

//     window.location.href = '/auth/login'
//   }

//   break
// }
case 403:
  console.error("403: Bạn không có quyền truy cập API này")

  // KHÔNG logout
  // KHÔNG xóa token
  // KHÔNG redirect login

  break
        case 500:
          console.error('Lỗi 500: Server Backend lỗi!')
          break
        default:
          console.error(`Lỗi HTTP ${status}:`, error.response.data)
      }
    }
    return Promise.reject(error)
  },
)

export default api
