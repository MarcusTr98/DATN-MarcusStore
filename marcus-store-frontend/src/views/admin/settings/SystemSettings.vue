<template>
  <div class="container-fluid px-4 py-3">
    <h3 class="fw-bold text-dark mb-4">
      <i class="fas fa-cogs me-2"></i>Cấu hình hệ thống Website
    </h3>

    <div class="card shadow-sm border-0 rounded-3">
      <div class="card-body p-4">
        <div v-if="isLoading" class="text-center py-5 text-muted">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="mt-2">Đang tải cấu hình...</p>
        </div>

        <form v-else @submit.prevent="saveSettings">
          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            1. Thông tin liên hệ (Header & Footer)
          </h5>
          <div class="row g-3 mb-4">
            <div class="col-md-4">
              <label class="form-label fw-semibold">Số điện thoại Hotline</label>
              <input type="text" class="form-control" v-model="settings.HOTLINE" required />
            </div>
            <div class="col-md-4">
              <label class="form-label fw-semibold">Email hỗ trợ</label>
              <input type="email" class="form-control" v-model="settings.EMAIL" required />
            </div>
            <div class="col-md-4">
              <label class="form-label fw-semibold">Giờ làm việc</label>
              <input type="text" class="form-control" v-model="settings.WORKING_HOURS" />
            </div>
            <div class="col-12">
              <label class="form-label fw-semibold">Địa chỉ trụ sở chính</label>
              <input type="text" class="form-control" v-model="settings.ADDRESS" required />
            </div>
          </div>

          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            2. Nội dung thông báo hiển thị
          </h5>
          <div class="mb-4">
            <label class="form-label fw-semibold">Chữ chạy trên thanh Topbar thông báo</label>
            <input type="text" class="form-control" v-model="settings.PROMO_TEXT" />
          </div>

          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            3. Đường dẫn Mạng xã hội (Footer Icons)
          </h5>
          <div class="row g-3 mb-4">
            <div class="col-md-6">
              <label class="form-label fw-semibold">Link Facebook</label>
              <input type="url" class="form-control" v-model="settings.FACEBOOK_URL" />
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Link TikTok</label>
              <input type="url" class="form-control" v-model="settings.TIKTOK_URL" />
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Link Instagram</label>
              <input type="url" class="form-control" v-model="settings.INSTAGRAM_URL" />
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Link Youtube</label>
              <input type="url" class="form-control" v-model="settings.YOUTUBE_URL" />
            </div>
          </div>

          <div class="text-end mt-4">
            <button
              type="submit"
              class="btn btn-warning text-white fw-bold px-4 rounded-pill shadow-sm"
              style="background-color: #ff6b00; border: none; transition: 0.3s"
              :disabled="isSaving"
            >
              <i class="fas" :class="isSaving ? 'fa-spinner fa-spin' : 'fa-save'"></i>
              <span class="ms-2">{{
                isSaving ? 'Đang lưu dữ liệu...' : 'Cập nhật toàn bộ nội dung Web'
              }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'

const isLoading = ref(true)
const isSaving = ref(false)

const settings = ref({
  HOTLINE: '',
  EMAIL: '',
  ADDRESS: '',
  WORKING_HOURS: '',
  PROMO_TEXT: '',
  FACEBOOK_URL: '',
  TIKTOK_URL: '',
  INSTAGRAM_URL: '',
  YOUTUBE_URL: '',
})

// Gọi API lấy dữ liệu lúc vừa vào trang
const loadSettings = async () => {
  try {
    isLoading.value = true
    const res = await api.get('/public/settings')

    // Đổ dữ liệu từ DB vào Object Setting
    Object.keys(settings.value).forEach((key) => {
      if (res.data[key] !== undefined) {
        settings.value[key] = res.data[key]
      }
    })
  } catch (error) {
    console.error('Lỗi tải cấu hình:', error)
    alert('Không thể tải cấu hình hệ thống!')
  } finally {
    isLoading.value = false
  }
}

// Gọi API Cập nhật
const saveSettings = async () => {
  try {
    isSaving.value = true
    const res = await api.put('/admin/settings/bulk-update', settings.value)
    alert('🎉 ' + res.data.message)
  } catch (error) {
    console.error('Lỗi khi cập nhật:', error)
    alert('Lưu thất bại: ' + (error.response?.data?.message || 'Lỗi hệ thống'))
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.form-control {
  border-radius: 8px;
  padding: 10px 15px;
  border: 1px solid #dee2e6;
}
.form-control:focus {
  border-color: #ff6b00;
  box-shadow: 0 0 0 0.25rem rgba(255, 107, 0, 0.25);
}
.btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(255, 107, 0, 0.3) !important;
}
</style>
