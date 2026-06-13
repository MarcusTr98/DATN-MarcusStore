<template>
  <div class="content-card">
    <div class="card-head">
      <div class="card-title-group">
        <span class="card-icon"><i class="fas fa-id-card"></i></span>
        <h5 class="card-title">Hồ sơ cá nhân</h5>
      </div>
    </div>

    <div v-if="isLoading" class="state-loading">
      <span class="spinner-cps"></span>
      <span>Đang tải thông tin...</span>
    </div>

    <form v-else @submit.prevent="updateProfile">
      <div class="row g-3">
        <div class="col-md-6">
          <label class="field-lbl">Tài khoản đăng nhập</label>
          <div class="input-wrap">
            <input type="text" class="f-input f-input--locked" :value="user.username" disabled />
            <i class="fas fa-lock lock-icon"></i>
          </div>
        </div>
        <div class="col-md-6">
          <label class="field-lbl">Email liên hệ</label>
          <div class="input-wrap">
            <input type="email" class="f-input f-input--locked" :value="user.email" disabled />
            <i class="fas fa-lock lock-icon"></i>
          </div>
        </div>
        <div class="col-md-6">
          <label class="field-lbl field-lbl--req">Họ và tên</label>
          <input v-model="user.fullName" type="text" class="f-input" placeholder="Nhập họ và tên" />
        </div>
        <div class="col-md-6">
          <label class="field-lbl field-lbl--req">Số điện thoại</label>
          <input
            v-model="user.phoneNumber"
            type="tel"
            class="f-input"
            placeholder="Nhập số điện thoại"
          />
        </div>
      </div>
      <div class="card-foot">
        <button type="submit" class="btn-red" :disabled="isSaving">
          <i class="fas fa-save me-2" v-if="!isSaving"></i>
          <i class="fas fa-spinner fa-spin me-2" v-else></i>
          {{ isSaving ? 'Đang lưu...' : 'Lưu thay đổi' }}
        </button>
      </div>
    </form>

    <BaseModal
      :visible="modal.visible"
      :type="modal.type"
      :title="modal.title"
      :message="modal.message"
      @close="modal.visible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import userApi from '@/api/userApi'
import BaseModal from '@/components/common/BaseModal.vue'

const user = reactive({ username: '', email: '', fullName: '', phoneNumber: '' })
const isLoading = ref(true)
const isSaving = ref(false)

const modal = reactive({ visible: false, type: 'info', title: '', message: '' })
const showAlert = (type, title, msg) => {
  modal.type = type
  modal.title = title
  modal.message = msg
  modal.visible = true
}

onMounted(() => {
  fetchProfile()
})

const fetchProfile = async () => {
  isLoading.value = true
  try {
    const res = await userApi.getMyProfile()
    if (res.data && res.data.data) {
      Object.assign(user, res.data.data)
    }
  } catch {
    showAlert('error', 'Lỗi tải dữ liệu', 'Không thể lấy thông tin cá nhân.')
  } finally {
    isLoading.value = false
  }
}

const updateProfile = async () => {
  if (!user.fullName || !user.phoneNumber) {
    showAlert('error', 'Lỗi nhập liệu', 'Vui lòng điền đủ Họ tên và Số điện thoại')
    return
  }
  isSaving.value = true
  try {
    await userApi.updateProfile({ fullName: user.fullName, phoneNumber: user.phoneNumber })
    showAlert('success', 'Thành công', 'Cập nhật hồ sơ thành công!')
    localStorage.setItem('USERNAME', user.fullName)
    window.dispatchEvent(new Event('auth-changed')) // Gửi event đổi tên lên Header
  } catch (error) {
    showAlert('error', 'Thất bại', error.response?.data?.message || 'Có lỗi xảy ra')
  } finally {
    isSaving.value = false
  }
}
</script>
