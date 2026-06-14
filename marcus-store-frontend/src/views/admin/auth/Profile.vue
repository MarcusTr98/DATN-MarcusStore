<template>
  <div class="admin-profile-page container-fluid px-4 py-4">
    <div class="page-header mb-4">
      <div class="page-header-left">
        <div class="page-header-icon">
          <i class="fas fa-user-astronaut"></i>
        </div>
        <div>
          <h3 class="page-header-title">Hồ sơ Quản trị viên</h3>
          <p class="page-header-sub">Quản lý thông tin cá nhân và bảo mật tài khoản</p>
        </div>
      </div>
      <div class="page-header-meta">
        <span class="last-updated-badge">
          <i class="fas fa-clock me-1"></i> Cập nhật lần cuối: {{ lastUpdated || 'Đang tải...' }}
        </span>
      </div>
    </div>

    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-card">
        <div class="spinner-ring"></div>
        <p class="loading-text">Đang tải dữ liệu từ Server...</p>
        <p class="loading-sub">Vui lòng chờ trong giây lát</p>
      </div>
    </div>

    <div v-else class="row g-4">
      <div class="col-xl-3 col-lg-4">
        <div class="sidebar-card">
          <div class="avatar-block">
            <div class="avatar-circle">
              <span class="avatar-initials">{{ getInitials(profile.fullName) }}</span>
              <div class="avatar-ring"></div>
            </div>
            <h5 class="avatar-name">{{ profile.fullName || 'Chưa cập nhật' }}</h5>
            <span
              :class="['role-pill', profile.roleName === 'ADMIN' ? 'role-admin' : 'role-staff']"
            >
              <i class="fas fa-shield-alt me-1"></i>{{ profile.roleName || '—' }}
            </span>
          </div>

          <hr class="sidebar-divider" />

          <div class="account-detail-list">
            <div class="account-detail-item">
              <div class="detail-icon-wrap bg-pink">
                <i class="fas fa-at"></i>
              </div>
              <div class="detail-content">
                <span class="detail-label">Tên đăng nhập</span>
                <span class="detail-value fw-bold">{{ profile.username || '—' }}</span>
              </div>
            </div>

            <div class="account-detail-item">
              <div class="detail-icon-wrap bg-blue">
                <i class="fas fa-envelope"></i>
              </div>
              <div class="detail-content">
                <span class="detail-label">Email</span>
                <span class="detail-value fw-bold text-truncate" :title="profile.email">
                  {{ profile.email || '—' }}
                </span>
              </div>
            </div>

            <div class="account-detail-item">
              <div class="detail-icon-wrap bg-green">
                <i class="fas fa-phone-alt"></i>
              </div>
              <div class="detail-content">
                <span class="detail-label">Số điện thoại</span>
                <span class="detail-value fw-bold">{{ profile.phoneNumber || '—' }}</span>
              </div>
            </div>
          </div>

          <hr class="sidebar-divider" />

          <div class="sidebar-notice">
            <i class="fas fa-info-circle me-2 text-muted"></i>
            <span>Email và tên đăng nhập <strong>không thể thay đổi</strong> sau khi đăng ký.</span>
          </div>
        </div>
      </div>

      <div class="col-xl-9 col-lg-8">
        <div class="row g-4">
          <div class="col-12">
            <div class="form-card">
              <div class="form-card-header">
                <div class="form-card-title-wrap">
                  <div class="form-card-icon bg-orange">
                    <i class="fas fa-id-badge"></i>
                  </div>
                  <div>
                    <h5 class="form-card-title">Thông tin cá nhân</h5>
                    <p class="form-card-sub">Chỉnh sửa tên hiển thị và số điện thoại liên hệ</p>
                  </div>
                </div>
                <div v-if="profileSaved" class="save-success-badge">
                  <i class="fas fa-check-circle me-1"></i> Đã lưu
                </div>
              </div>

              <div class="form-card-body">
                <form @submit.prevent="handleUpdateProfile">
                  <div class="row g-3">
                    <div class="col-md-6">
                      <div class="field-group">
                        <label class="field-label">
                          Tên đăng nhập
                          <span class="readonly-tag">Chỉ đọc</span>
                        </label>
                        <div class="field-input-wrap">
                          <i class="fas fa-at field-icon"></i>
                          <input
                            type="text"
                            class="field-input readonly"
                            v-model="profile.username"
                            readonly
                            disabled
                          />
                          <span class="field-lock"><i class="fas fa-lock"></i></span>
                        </div>
                      </div>
                    </div>

                    <div class="col-md-6">
                      <div class="field-group">
                        <label class="field-label">
                          Email
                          <span class="readonly-tag">Chỉ đọc</span>
                        </label>
                        <div class="field-input-wrap">
                          <i class="fas fa-envelope field-icon"></i>
                          <input
                            type="email"
                            class="field-input readonly"
                            v-model="profile.email"
                            readonly
                            disabled
                          />
                          <span class="field-lock"><i class="fas fa-lock"></i></span>
                        </div>
                      </div>
                    </div>

                    <div class="col-12"><div class="field-divider"></div></div>

                    <div class="col-md-6">
                      <div class="field-group">
                        <label class="field-label">
                          Họ và tên <span class="required-star">*</span>
                        </label>
                        <div class="field-input-wrap">
                          <i class="fas fa-user-edit field-icon text-primary"></i>
                          <input
                            type="text"
                            class="field-input editable"
                            v-model.trim="profile.fullName"
                            placeholder="Nhập họ và tên đầy đủ"
                            required
                            :class="{ 'has-value': profile.fullName }"
                          />
                        </div>
                        <p class="field-hint">Tên này sẽ hiển thị trên hệ thống</p>
                      </div>
                    </div>

                    <div class="col-md-6">
                      <div class="field-group">
                        <label class="field-label">
                          Số điện thoại <span class="required-star">*</span>
                        </label>
                        <div class="field-input-wrap">
                          <i class="fas fa-phone-alt field-icon text-success"></i>
                          <input
                            type="tel"
                            class="field-input editable"
                            v-model.trim="profile.phoneNumber"
                            placeholder="VD: 0901 234 567"
                            required
                            :class="{ 'has-value': profile.phoneNumber }"
                          />
                        </div>
                        <p class="field-hint">Dùng để liên hệ khẩn cấp</p>
                      </div>
                    </div>
                  </div>

                  <div class="form-actions mt-4">
                    <div class="form-actions-left">
                      <i class="fas fa-exclamation-circle text-muted me-1"></i>
                      <span class="text-muted" style="font-size: 13px">
                        Các trường có dấu <span class="required-star">*</span> là bắt buộc
                      </span>
                    </div>
                    <button type="submit" class="btn-save" :disabled="isSavingProfile">
                      <i
                        :class="
                          isSavingProfile ? 'fas fa-spinner fa-spin' : 'fas fa-cloud-upload-alt'
                        "
                      ></i>
                      {{ isSavingProfile ? 'Đang lưu...' : 'Lưu thay đổi' }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>

          <div class="col-12">
            <div class="form-card">
              <div class="form-card-header">
                <div class="form-card-title-wrap">
                  <div class="form-card-icon bg-blue">
                    <i class="fas fa-key"></i>
                  </div>
                  <div>
                    <h5 class="form-card-title">Bảo mật tài khoản</h5>
                    <p class="form-card-sub">Thay đổi mật khẩu định kỳ để bảo vệ tài khoản</p>
                  </div>
                </div>
                <div class="security-level-badge">
                  <i class="fas fa-shield-check me-1"></i>
                  <span>Bảo mật {{ passwordStrengthLabel }}</span>
                </div>
              </div>

              <div class="form-card-body">
                <form @submit.prevent="handleChangePassword">
                  <div class="row g-3">
                    <div class="col-md-12">
                      <div class="field-group">
                        <label class="field-label">
                          Mật khẩu hiện tại <span class="required-star">*</span>
                        </label>
                        <div class="field-input-wrap">
                          <i class="fas fa-unlock-alt field-icon text-secondary"></i>
                          <input
                            :type="showOldPw ? 'text' : 'password'"
                            class="field-input editable"
                            v-model="passwordForm.oldPassword"
                            placeholder="Nhập mật khẩu hiện tại"
                            required
                          />
                          <button
                            type="button"
                            class="toggle-pw-btn"
                            @click="showOldPw = !showOldPw"
                          >
                            <i :class="showOldPw ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                          </button>
                        </div>
                      </div>
                    </div>

                    <div class="col-md-6">
                      <div class="field-group">
                        <label class="field-label">
                          Mật khẩu mới <span class="required-star">*</span>
                        </label>
                        <div class="field-input-wrap">
                          <i class="fas fa-lock field-icon text-primary"></i>
                          <input
                            :type="showNewPw ? 'text' : 'password'"
                            class="field-input editable"
                            v-model="passwordForm.newPassword"
                            @input="calcStrength"
                            placeholder="Tối thiểu 6 ký tự"
                            required
                          />
                          <button
                            type="button"
                            class="toggle-pw-btn"
                            @click="showNewPw = !showNewPw"
                          >
                            <i :class="showNewPw ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                          </button>
                        </div>

                        <div class="strength-bar-wrap mt-2" v-if="passwordForm.newPassword">
                          <div class="strength-bar">
                            <div
                              class="strength-fill"
                              :style="{ width: strengthPercent + '%' }"
                              :class="strengthClass"
                            ></div>
                          </div>
                          <span class="strength-text" :class="strengthClass">
                            {{ passwordStrengthLabel }}
                          </span>
                        </div>

                        <div class="password-rules mt-2" v-if="passwordForm.newPassword">
                          <span
                            :class="[
                              'rule',
                              passwordForm.newPassword.length >= 6 ? 'rule-ok' : 'rule-no',
                            ]"
                          >
                            <i
                              :class="
                                passwordForm.newPassword.length >= 6
                                  ? 'fas fa-check'
                                  : 'fas fa-times'
                              "
                            ></i>
                            Ít nhất 6 ký tự
                          </span>
                          <span
                            :class="[
                              'rule',
                              /[A-Z]/.test(passwordForm.newPassword) ? 'rule-ok' : 'rule-no',
                            ]"
                          >
                            <i
                              :class="
                                /[A-Z]/.test(passwordForm.newPassword)
                                  ? 'fas fa-check'
                                  : 'fas fa-times'
                              "
                            ></i>
                            Có chữ hoa
                          </span>
                          <span
                            :class="[
                              'rule',
                              /[0-9]/.test(passwordForm.newPassword) ? 'rule-ok' : 'rule-no',
                            ]"
                          >
                            <i
                              :class="
                                /[0-9]/.test(passwordForm.newPassword)
                                  ? 'fas fa-check'
                                  : 'fas fa-times'
                              "
                            ></i>
                            Có số
                          </span>
                        </div>
                      </div>
                    </div>

                    <div class="col-md-6">
                      <div class="field-group">
                        <label class="field-label">
                          Xác nhận mật khẩu <span class="required-star">*</span>
                        </label>
                        <div class="field-input-wrap">
                          <i class="fas fa-check-double field-icon text-success"></i>
                          <input
                            :type="showConfirmPw ? 'text' : 'password'"
                            class="field-input editable"
                            v-model="passwordForm.confirmPassword"
                            placeholder="Nhập lại mật khẩu mới"
                            required
                            :class="{
                              'match-ok':
                                passwordForm.confirmPassword &&
                                passwordForm.newPassword === passwordForm.confirmPassword,
                              'match-fail':
                                passwordForm.confirmPassword &&
                                passwordForm.newPassword !== passwordForm.confirmPassword,
                            }"
                          />
                          <button
                            type="button"
                            class="toggle-pw-btn"
                            @click="showConfirmPw = !showConfirmPw"
                          >
                            <i :class="showConfirmPw ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                          </button>
                        </div>
                        <p
                          v-if="
                            passwordForm.confirmPassword &&
                            passwordForm.newPassword !== passwordForm.confirmPassword
                          "
                          class="field-error-hint"
                        >
                          <i class="fas fa-exclamation-triangle me-1"></i>Mật khẩu chưa khớp
                        </p>
                        <p
                          v-if="
                            passwordForm.confirmPassword &&
                            passwordForm.newPassword === passwordForm.confirmPassword
                          "
                          class="field-success-hint"
                        >
                          <i class="fas fa-check-circle me-1"></i>Mật khẩu khớp
                        </p>
                      </div>
                    </div>
                  </div>

                  <div class="form-actions mt-4">
                    <div class="security-tip">
                      <i class="fas fa-lightbulb text-warning me-1"></i>
                      <span>Nên thay đổi mật khẩu định kỳ mỗi 3 tháng</span>
                    </div>
                    <button type="submit" class="btn-change-pw" :disabled="isChangingPassword">
                      <i
                        :class="isChangingPassword ? 'fas fa-spinner fa-spin' : 'fas fa-sync-alt'"
                      ></i>
                      {{ isChangingPassword ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <BaseModal
      :visible="localModal.visible"
      :type="localModal.type"
      :title="localModal.title"
      :message="localModal.message"
      @close="localModal.visible = false"
      @confirm="execConfirm"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '@/utils/api'
import BaseModal from '@/components/common/BaseModal.vue'
import '@/assets/css/profile-admin.css'
const lastUpdated = ref('')
const isLoading = ref(false)
const isSavingProfile = ref(false)
const isChangingPassword = ref(false)
const profileSaved = ref(false)

const showOldPw = ref(false)
const showNewPw = ref(false)
const showConfirmPw = ref(false)

const strengthScore = ref(0)

const profile = reactive({
  username: '',
  email: '',
  fullName: '',
  phoneNumber: '',
  roleName: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const localModal = reactive({
  visible: false,
  type: 'info',
  title: '',
  message: '',
  actionCallback: null,
})
const getInitials = (name) => {
  if (!name) return 'AD'
  const parts = name.trim().split(' ')
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

const calcStrength = () => {
  const pw = passwordForm.newPassword
  let score = 0
  if (pw.length >= 6) score++
  if (pw.length >= 10) score++
  if (/[A-Z]/.test(pw)) score++
  if (/[0-9]/.test(pw)) score++
  if (/[^A-Za-z0-9]/.test(pw)) score++
  strengthScore.value = score
}

const strengthPercent = computed(() => (strengthScore.value / 5) * 100)

const strengthClass = computed(() => {
  if (strengthScore.value <= 1) return 'strength-weak'
  if (strengthScore.value <= 3) return 'strength-medium'
  return 'strength-strong'
})

const passwordStrengthLabel = computed(() => {
  if (!passwordForm.newPassword) return 'Chưa đánh giá'
  if (strengthScore.value <= 1) return 'Yếu'
  if (strengthScore.value <= 3) return 'Trung bình'
  return 'Mạnh'
})

const showAlert = (type, title, msg) => {
  Object.assign(localModal, { type, title, message: msg, actionCallback: null, visible: true })
}

const execConfirm = () => {
  if (localModal.actionCallback) localModal.actionCallback()
}

// ── API
const adminProfileApi = {
  getProfile: () => api.get('/admin/profile'),
  updateProfile: (data) => api.put('/admin/profile', data),
}

onMounted(() => {
  fetchProfile()
})

const fetchProfile = async () => {
  isLoading.value = true
  try {
    const res = await adminProfileApi.getProfile()

    if (res.data && res.data.data) {
      Object.assign(profile, res.data.data)
    } else {
      Object.assign(profile, res.data)
    }

    // thời gian lấy dữ liệu thành công
    lastUpdated.value = formatDateTime(new Date())
  } catch (error) {
    console.error('Lỗi tải profile', error)
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      showAlert(
        'error',
        'Lỗi phân quyền',
        'Phiên đăng nhập không hợp lệ hoặc bạn không có quyền truy cập. Vui lòng đăng nhập lại.',
      )
    } else {
      showAlert(
        'error',
        'Lỗi kết nối',
        'Không thể tải thông tin cá nhân. Vui lòng kiểm tra API Backend.',
      )
    }
  } finally {
    isLoading.value = false
  }
}

const handleUpdateProfile = async () => {
  isSavingProfile.value = true
  profileSaved.value = false
  try {
    const payload = {
      fullName: profile.fullName,
      phoneNumber: profile.phoneNumber,
    }

    const res = await adminProfileApi.updateProfile(payload)
    if (res.data && res.data.data) {
      Object.assign(profile, res.data.data)
    }

    localStorage.setItem('USERNAME', profile.fullName)

    profileSaved.value = true
    setTimeout(() => (profileSaved.value = false), 3000)

    // sửa lại mốc thời gian ngay khi lưu DB thành công
    lastUpdated.value = formatDateTime(new Date())

    showAlert('success', 'Thành công', 'Thông tin hồ sơ cá nhân đã được lưu vào hệ thống.')
  } catch (error) {
    const resData = error.response?.data
    let errorMsg = resData?.message || 'Có lỗi xảy ra khi lưu thông tin.'
    if (resData?.data && typeof resData.data === 'object' && Object.keys(resData.data).length > 0) {
      errorMsg = Object.values(resData.data).join('\n')
    }
    showAlert('warning', 'Dữ liệu không hợp lệ', errorMsg)
  } finally {
    isSavingProfile.value = false
  }
}

const handleChangePassword = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    showAlert('error', 'Lỗi xác nhận', 'Mật khẩu xác nhận không khớp!')
    return
  }
  // TODO: API đổi mật khẩu - đợi Ngọc làm
  showAlert(
    'info',
    'Chờ ghép nối API',
    'Giao diện Đổi mật khẩu đã xong. Chờ API Backend hoàn thiện để gắn vào.',
  )
}
// khai báo giờ
const formatDateTime = (dateObj) => {
  if (!dateObj) return ''
  const pad = (n) => n.toString().padStart(2, '0')
  const h = pad(dateObj.getHours())
  const m = pad(dateObj.getMinutes())
  const s = pad(dateObj.getSeconds())
  const d = pad(dateObj.getDate())
  const mo = pad(dateObj.getMonth() + 1)
  const y = dateObj.getFullYear()

  return `${h}:${m}:${s} - ${d}/${mo}/${y}`
}
</script>
