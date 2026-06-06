import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000, // ngắt kết nối nếu Server phản hồi quá 10 giây
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('ACCESS_TOKEN')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// Xử lý các lỗi chung từ Backend trả về trước khi ném vào Component
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response) {
      const status = error.response.status

      switch (status) {
        case 401:
          console.error('Lỗi 401: Token hết hạn hoặc chưa đăng nhập!')
          // xóa token rác
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          // trình duyệt chuyển hướng về trang Login
          window.location.href = '/auth/login'
          break
        case 403:
          console.error('Lỗi 403: Không có quyền truy cập (Sai Role)!')
          alert('Bạn không có quyền thực hiện chức năng này!')
          break
        case 500:
          console.error('Lỗi 500: Server Backend đang bị sập hoặc lỗi logic!')
          break
        default:
          console.error(`Lỗi HTTP ${status}:`, error.response.data)
      }
    } else if (error.request) {
      // Lỗi do mất kết nối mạng hoặc Server Spring Boot chưa bật
      console.error('Không thể kết nối đến máy chủ Backend!')
      alert('Lỗi kết nối mạng hoặc máy chủ đang bảo trì.')
    }

    return Promise.reject(error)
  },
)

export default api
