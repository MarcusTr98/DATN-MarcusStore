import api from '@/utils/api'

export const postApi = {
  getAll: (params) =>
    api
      .get('/admin/posts', { params: { size: 1000, ...params } })
      .then((r) => r.data.data.content),
  getById: (id) => api.get(`/admin/posts/${id}`).then((r) => r.data.data),
  getCategories: () => api.get('/admin/post-categories').then((r) => r.data.data),
  checkSlug: (slug, excludeId) =>
    api
      .get('/admin/posts/check-slug', { params: { slug, excludeId } })
      .then((r) => r.data.data), // { exists: boolean }

  create: (payload) => api.post('/admin/posts', payload).then((r) => r.data.data),

  update: (id, payload) => api.put(`/admin/posts/${id}`, payload).then((r) => r.data.data),

  togglePublish: (post, isPublished) =>
    api
      .put(`/admin/posts/${post.id}`, {
        title: post.title,
        postCategoryId: post.postCategoryId,
        content: post.content,
        excerpt: post.excerpt,
        thumbnailUrl: post.thumbnailUrl,
        isPublished,
      })
      .then((r) => r.data.data),

  // Lưu ý: BE hiện chỉ set isPublished=false (soft-hide), không xóa khỏi DB.
  delete: (id) => api.delete(`/admin/posts/${id}`).then((r) => r.data),

  // POST /api/admin/posts/upload-image -> { url: "..." }
  uploadImage: (file, onProgress) => {
    const formData = new FormData()
    formData.append('file', file)
    return api
      .post('/admin/posts/upload-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: onProgress,
      })
      .then((r) => r.data.data.url)
  },
}

// API PUBLIC cho phía client (khách vãng lai, không cần đăng nhập).
export const postPublicApi = {
  getAll: (params) =>
    api
      .get('/public/posts', { params: { size: 6, sort: 'publishedAt,desc', ...params } })
      .then((r) => r.data.data.content),

  // dùng cho trang danh sách /blog cần nút "Xem thêm".
  getPage: (params) =>
    api
      .get('/public/posts', { params: { size: 9, sort: 'publishedAt,desc', ...params } })
      .then((r) => r.data.data),

  // GET /api/public/posts/:slug -> chi tiết 1 bài (lỗi nếu chưa xuất bản hoặc không tồn tại)
  getBySlug: (slug) => api.get(`/public/posts/${slug}`).then((r) => r.data.data),
}

export default postApi