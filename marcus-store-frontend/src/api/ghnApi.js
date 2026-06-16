import axios from 'axios'

// 1.Token DEV che API
const GHN_TOKEN = import.meta.env.VITE_GHN_TOKEN

const ghnClient = axios.create({
  baseURL: 'https://dev-online-gateway.ghn.vn/shiip/public-api/master-data',
  headers: {
    token: GHN_TOKEN,
    'Content-Type': 'application/json',
  },
})

const ghnApi = {
  // Lấy danh sách Tỉnh/Thành phố
  getProvinces() {
    return ghnClient.get('/province')
  },

  // Lấy danh sách Quận/Huyện dựa vào provinceId
  getDistricts(provinceId) {
    return ghnClient.get('/district', {
      params: { province_id: provinceId },
    })
  },

  // Lấy danh sách Phường/Xã dựa vào districtId
  getWards(districtId) {
    return ghnClient.get('/ward', {
      params: { district_id: districtId },
    })
  },
}

export default ghnApi
