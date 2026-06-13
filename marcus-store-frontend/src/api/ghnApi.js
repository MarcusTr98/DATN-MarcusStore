import axios from 'axios'

const GHN_TOKEN = '481d9b47-6663-11f1-968b-a2d7db98b190'

const ghnClient = axios.create({
  baseURL: 'https://online-gateway.ghn.vn/shiip/public-api/master-data',
  headers: {
    token: GHN_TOKEN,
    'Content-Type': 'application/json',
  },
})

const ghnApi = {
  //danh sách Tỉnh/Thành phố
  getProvinces() {
    return ghnClient.get('/province')
  },
  //danh sách Quận/Huyện dựa vào provinceId
  getDistricts(provinceId) {
    return ghnClient.get('/district', {
      params: { province_id: provinceId },
    })
  },
  //danh sách Phường/Xã dựa vào districtId
  getWards(districtId) {
    return ghnClient.get('/ward', {
      params: { district_id: districtId },
    })
  },
}

export default ghnApi
