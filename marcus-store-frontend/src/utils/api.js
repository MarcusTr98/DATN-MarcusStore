import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    // Đảm bảo lấy đúng key ACCESS_TOKEN
    const token = localStorage.getItem('ACCESS_TOKEN')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401:
          console.error('Lỗi 401: Token hết hạn hoặc chưa đăng nhập!')
          localStorage.removeItem('ACCESS_TOKEN')
          localStorage.removeItem('USERNAME')
          localStorage.removeItem('USER_ROLE')
          window.location.href = '/auth/login'
          break
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
