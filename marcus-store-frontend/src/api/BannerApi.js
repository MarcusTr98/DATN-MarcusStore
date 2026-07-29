import api from '@/utils/api';

const BASE = '/admin/banners';
const API_ROOT = 'http://localhost:8080/api'; // trùng baseURL trong api.js

const bannerApi = {
  getAll() {
    return api.get(BASE).then((res) => res.data.data);
  },

  // Dùng cho HomeBanner.vue (public, không cần token)
    getPublicPositions() {
      return api.get('/public/banners/positions').then((res) => res.data?.data ?? res.data);
    },
    getPublicBanners() {
      return api.get('/public/banners').then((res) => res.data?.data ?? res.data);
    },

  getPositions() {
    return api.get(`${BASE}/positions`).then((res) => res.data.data);
  },

  getOne(id) {
    return api.get(`${BASE}/${id}`).then((res) => res.data.data);
  },

  create(payload) {
    return api.post(BASE, payload).then((res) => res.data.data);
  },

  update(id, payload) {
    return api.put(`${BASE}/${id}`, payload).then((res) => res.data.data);
  },

  remove(id) {
    return api.delete(`${BASE}/${id}`).then((res) => res.data.data);
  },

  uploadImage(file, onUploadProgress) {
    const formData = new FormData();
    formData.append('file', file);

    const token = localStorage.getItem('ACCESS_TOKEN');

    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('POST', `${API_ROOT}${BASE}/upload-image`);

      if (token) {
        xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      }
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable && onUploadProgress) {
          onUploadProgress(e);
        }
      };

      xhr.onload = () => {
        let res;
        try {
          res = JSON.parse(xhr.responseText);
        } catch {
          reject(new Error('Phản hồi từ server không hợp lệ'));
          return;
        }
        if (xhr.status >= 200 && xhr.status < 300) {
          const url = res.data || res.message;
          resolve(url);
        } else {
          reject(new Error(res.message || `Upload thất bại (mã lỗi ${xhr.status})`));
        }
      };

      xhr.onerror = () => reject(new Error('Lỗi mạng khi upload'));
      xhr.send(formData);
    });
  },
};

export { bannerApi };
export default bannerApi;
