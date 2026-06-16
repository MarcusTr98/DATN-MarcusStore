import axios from 'axios'
import { useLoadingStore } from '@/stores/useLoadingStore'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    // 1. Kích hoạt hiệu ứng loading khi bắt đầu gọi API
    useLoadingStore().show()

    // 2. Đảm bảo lấy đúng key ACCESS_TOKEN
    const token = localStorage.getItem('ACCESS_TOKEN')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    // Tắt loading nếu request bị lỗi ngay từ client
    useLoadingStore().hide()
    return Promise.reject(error)
  },
)

api.interceptors.response.use(
  (response) => {
    // Tắt loading khi API trả về kết quả thành công
    useLoadingStore().hide()
    return response
  },
  (error) => {
    // Tắt loading khi API trả về lỗi (4xx, 5xx)
    useLoadingStore().hide()

    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401: {
          console.error('Lỗi 401: Bạn chưa đăng nhập')

          // Chỉ redirect nếu người dùng đã từng đăng nhập
          const token = localStorage.getItem('ACCESS_TOKEN')

          if (token) {
            localStorage.removeItem('ACCESS_TOKEN')
            localStorage.removeItem('USERNAME')
            localStorage.removeItem('USER_ROLE')

            window.dispatchEvent(new Event('auth-changed'))

            window.location.href = '/auth/login'
          }

          break
        }
        case 403:
          console.error('Lỗi 403: Bạn không có quyền truy cập!')
          alert('Bạn không có quyền thực hiện chức năng này!')
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
