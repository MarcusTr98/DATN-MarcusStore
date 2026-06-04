import axiosClient from './axiosClient'

export const loginApi = (data) => {
  return axiosClient.post('/auth/login', data)
}