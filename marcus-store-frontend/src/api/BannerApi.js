import api from '@/utils/api';

const BASE = '/admin/banners';

const bannerApi = {
  // Lấy tất cả banner
  getAll() {
    return api.get(BASE).then((res) => res.data.data);
  },

  // Lấy chi tiết 1 banner
  getOne(id) {
    return api.get(`${BASE}/${id}`).then((res) => res.data.data);
  },

  // Thêm banner mới
  create(payload) {
    return api.post(BASE, payload).then((res) => res.data.data);
  },

  // Sửa banner
  update(id, payload) {
    return api.put(`${BASE}/${id}`, payload).then((res) => res.data.data);
  },

  // Xóa mềm banner (backend tự set isActive = false)
  remove(id) {
    return api.delete(`${BASE}/${id}`).then((res) => res.data.data);
  },
};

export { bannerApi };
export default bannerApi;