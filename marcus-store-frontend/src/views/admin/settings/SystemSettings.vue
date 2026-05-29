<template>
  <div class="container-fluid px-4 py-3">
    <h3 class="fw-bold text-dark mb-4">
      <i class="fas fa-cogs me-2"></i>Cấu hình hệ thống Website
    </h3>

    <div class="card shadow-sm border-0 rounded-3">
      <div class="card-body p-4">
        <form @submit.prevent="saveSettings">
          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            1. Thông tin liên hệ (Header & Footer)
          </h5>
          <div class="row g-3 mb-4">
            <div class="col-md-6">
              <label class="form-label fw-semibold">Số điện thoại Hotline</label>
              <input type="text" class="form-control" v-model="settings.HOTLINE_NUMBER" />
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Email hỗ trợ</label>
              <input type="email" class="form-control" v-model="settings.STORE_EMAIL" />
            </div>
            <div class="col-12">
              <label class="form-label fw-semibold">Địa chỉ trụ sở chính</label>
              <input type="text" class="form-control" v-model="settings.STORE_ADDRESS" />
            </div>
          </div>

          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            2. Nội dung thông báo hiển thị
          </h5>
          <div class="mb-4">
            <label class="form-label fw-semibold">Chữ chạy trên thanh Topbar thông báo</label>
            <input type="text" class="form-control" v-model="settings.TOPBAR_TEXT" />
          </div>

          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            3. Đường dẫn Mạng xã hội (Footer Icons)
          </h5>
          <div class="row g-3 mb-4">
            <div class="col-md-4">
              <label class="form-label fw-semibold">Link Facebook</label>
              <input type="url" class="form-control" v-model="settings.LINK_FACEBOOK" />
            </div>
            <div class="col-md-4">
              <label class="form-label fw-semibold">Link TikTok</label>
              <input type="url" class="form-control" v-model="settings.LINK_TIKTOK" />
            </div>
            <div class="col-md-4">
              <label class="form-label fw-semibold">Link Instagram</label>
              <input type="url" class="form-control" v-model="settings.LINK_INSTAGRAM" />
            </div>
          </div>

          <div class="text-end">
            <button
              type="submit"
              class="btn btn-warning text-white fw-bold px-4 rounded-pill shadow-sm"
              style="background-color: #ff6b00"
            >
              <i class="fas fa-save me-2"></i>Cập nhật toàn bộ nội dung Web
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const settings = ref({
  HOTLINE_NUMBER: '(+84) 907 640 098',
  STORE_EMAIL: 'support@marcusstore.com',
  STORE_ADDRESS: '118 Cát Bi, Hải An, Hải Phòng, Việt Nam',
  TOPBAR_TEXT: 'Khuyến mãi hôm nay: Giảm đến 50% phụ kiện chính hãng',
  LINK_FACEBOOK: 'https://facebook.com/marcusstore',
  LINK_TIKTOK: 'https://tiktok.com/@marcusstore',
  LINK_INSTAGRAM: 'https://instagram.com/marcusstore',
})

const saveSettings = () => {
  alert(
    'Gửi API UPDATE xuống bảng System_Settings thành công! Toàn bộ nội dung chữ ngoài Client đã thay đổi!',
  )
}
</script>
