import axios from 'axios'

// 1. Khởi tạo instance với Base URL trỏ về Spring Boot
const axiosClient = axios.create({
  //khớp với cổng và mapping của Backend
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

axiosClient.interceptors.request.use(
  (config) => {
    // Lấy token từ Local Storage hoặc Pinia Store
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}` // Gắn JWT vào Header
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

axiosClient.interceptors.response.use(
  (response) => {
    // Chỉ trả về phần data
    return response.data
  },
  (error) => {
    const status = error.response ? error.response.status : null

    if (status === 401) {
      console.error('Token hết hạn hoặc không có quyền truy cập!')
      // ***cần bổ sung logic tự động gọi API Refresh Token hoặc đẩy User về trang Login
    } else if (status === 403) {
      console.error('Không đủ quyền (Role) để thực hiện thao tác này!')
    }

    return Promise.reject(error)
  },
)

export default axiosClient
