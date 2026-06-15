import api from '@/utils/api'

export const loginApi = (data) => {
  return api.post('/auth/login', data)
}
export const forgotPasswordApi = (email) => {
  return api.post('/auth/forgot', {
    email,
  })
}

export const verifyForgotOtpApi = (email, otp) => {
  return api.post('/auth/verify-otp', {
    email,
    otp,
  })
}

export const resetPasswordApi = (data) => {
  return api.post('/auth/reset', data)
}