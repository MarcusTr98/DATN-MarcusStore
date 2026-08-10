<template>
  <div class="system-settings-page">
    <AdminPageHeader
      eyebrow="Thiết lập cửa hàng"
      eyebrow-icon="bi bi-sliders"
      title="Cấu hình hệ thống Website"
      description="Quản lý thông tin liên hệ, nội dung hiển thị, vị trí cửa hàng và Hero trang chủ."
      icon="bi bi-gear-wide-connected"
    >
      <template #actions>
        <button type="button" class="settings-restore-btn" :disabled="isLoading || isSaving" @click="confirmRestoreDefaults">
          <i class="fas fa-arrow-rotate-left"></i> Khôi phục mặc định
        </button>
        <button type="submit" form="system-settings-form" :disabled="isLoading || isSaving">
          <i class="fas" :class="isSaving ? 'fa-spinner fa-spin' : 'fa-floppy-disk'"></i>
          {{ isSaving ? 'Đang lưu...' : 'Lưu thay đổi' }}
        </button>
      </template>
    </AdminPageHeader>

    <div class="card settings-card shadow-sm border-0 rounded-4">
      <div class="card-body p-4">
        <div v-if="isLoading" class="text-center py-5 text-muted">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="mt-2">Đang tải cấu hình...</p>
        </div>

        <form v-else id="system-settings-form" @submit.prevent="saveSettings">
          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            1. Nhận diện Website
          </h5>
          <div class="branding-settings mb-4">
            <div class="branding-fields">
              <label class="form-label fw-semibold">Tên Website</label>
              <input type="text" class="form-control" v-model="settings.SITE_NAME" required maxlength="255" />
              <small class="setting-help">Tên này được dùng đồng bộ ở Header, Footer, Admin, Checkout và màn loading.</small>

              <label class="form-label fw-semibold mt-3">Dán đường dẫn ảnh</label>
              <input
                type="url"
                class="form-control"
                v-model="settings.SITE_LOGO_URL"
                @input="logoPreviewError = false"
                placeholder="https://res.cloudinary.com/.../logo.png"
              />
              <small class="setting-help">
                Link phải mở trực tiếp ra ảnh và bắt đầu bằng <code>https://</code>. Không dùng link trang Google Drive/Facebook.
              </small>

              <input
                ref="logoFileInput"
                type="file"
                class="d-none"
                accept="image/png,image/jpeg,image/webp"
                @change="handleLogoFile"
              />
              <button
                type="button"
                class="logo-upload-zone"
                :class="{ 'is-uploading': isLogoUploading }"
                :disabled="isLogoUploading"
                @click="logoFileInput?.click()"
                @dragover.prevent
                @drop.prevent="handleLogoDrop"
              >
                <i class="fas upload-zone-icon" :class="isLogoUploading ? 'fa-spinner fa-spin' : 'fa-cloud-arrow-up'"></i>
                <span>{{ isLogoUploading ? `Đang tải Logo... ${logoUploadProgress}%` : 'Kéo thả Logo vào đây hoặc nhấn để chọn ảnh' }}</span>
                <small>PNG, JPG, WEBP · tối đa 2 MB</small>
                <span v-if="isLogoUploading" class="upload-progress"><i :style="{ width: `${logoUploadProgress}%` }"></i></span>
              </button>

              <div class="branding-actions">
                <button
                  v-if="settings.SITE_LOGO_URL"
                  type="button"
                  class="brand-remove-btn"
                  :disabled="isLogoUploading"
                  @click="removeLogo"
                >
                  <i class="fas fa-trash"></i> Xóa Logo
                </button>
              </div>
              <small class="setting-help">Khuyên dùng PNG/WEBP nền trong suốt, ảnh vuông; tối đa 2 MB.</small>
            </div>

            <div class="branding-preview">
              <span class="preview-label">Xem trước</span>
              <div class="preview-logo-box">
                <img
                  v-if="logoDisplayUrl && !logoPreviewError"
                  :src="logoDisplayUrl"
                  :alt="settings.SITE_NAME"
                  class="site-logo-preview"
                  @error="logoPreviewError = true"
                />
                <i v-else class="fas fa-mobile-screen-button preview-fallback"></i>
              </div>
              <strong>{{ settings.SITE_NAME || 'Tên Website' }}</strong>
              <small v-if="logoPreviewError" class="text-danger">Không tải được ảnh từ URL này.</small>
              <small v-else class="text-muted">Logo thực tế sẽ tự co vừa từng vị trí.</small>
            </div>
          </div>

          <div class="settings-audit">
            <i class="fas fa-clock-rotate-left"></i>
            <span v-if="settingsMeta.updatedAt">
              Cập nhật gần nhất bởi <strong>{{ settingsMeta.updatedBy || 'Không rõ' }}</strong>
              lúc {{ formatDateTime(settingsMeta.updatedAt) }}
            </span>
            <span v-else>Chưa có lịch sử cập nhật cấu hình.</span>
          </div>

          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            2. Thông tin liên hệ (Header & Footer)
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
            3. Nội dung thông báo hiển thị
          </h5>
          <div class="mb-4">
            <label class="form-label fw-semibold">Chữ chạy trên thanh Topbar thông báo</label>
            <input type="text" class="form-control" v-model="settings.PROMO_TEXT" />
          </div>

          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            4. Đường dẫn Mạng xã hội (Footer Icons)
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

          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            5. Cấu hình Bản đồ & Cửa hàng (Store Location)
          </h5>
          <div class="row g-3 mb-4 bg-light p-3 rounded-3 border">
            <div class="col-md-6">
              <label class="form-label fw-semibold">Tên cửa hàng trên Map</label>
              <input
                type="text"
                class="form-control"
                v-model="mapData.name"
                required
                placeholder="VD: Marcus Store Hải Phòng"
              />
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Địa chỉ chi tiết trên Map</label>
              <input
                type="text"
                class="form-control"
                v-model="mapData.address"
                required
                placeholder="VD: 118 Cát Bi..."
              />
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Tọa độ Vĩ độ (Latitude)</label>
              <input
                type="number"
                step="any"
                class="form-control"
                v-model="mapData.lat"
                required
                placeholder="VD: 20.82716"
              />
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Tọa độ Kinh độ (Longitude)</label>
              <input
                type="number"
                step="any"
                class="form-control"
                v-model="mapData.lng"
                required
                placeholder="VD: 106.70466"
              />
            </div>
            <div class="col-12 text-muted small">
              <i class="fa-solid fa-circle-info text-primary"></i>
              Mẹo: Lên Google Maps, nhấp chuột phải vào địa điểm của bạn để lấy tọa độ Vĩ độ, Kinh
              độ.
            </div>
          </div>

          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            6. Nội dung Hero Trang chủ (Home.vue)
          </h5>
          <div class="row g-3 mb-4">
            <div class="col-12">
              <label class="form-label fw-semibold">Nhãn nhỏ phía trên tiêu đề (badge)</label>
              <input
                type="text"
                class="form-control"
                v-model="settings.HOME_HERO_BADGE"
                placeholder="VD: Cập nhật máy hot nhất 07/2026"
              />
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Tiêu đề chính (phần chữ trắng)</label>
              <input
                type="text"
                class="form-control"
                v-model="settings.HOME_HERO_TITLE"
                placeholder="VD: Đổi mới."
              />
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Tiêu đề chính (phần chữ nhấn đỏ)</label>
              <input
                type="text"
                class="form-control"
                v-model="settings.HOME_HERO_TITLE_ACCENT"
                placeholder="VD: Trả góp 0%."
              />
            </div>
            <div class="col-12">
              <label class="form-label fw-semibold">Đoạn mô tả ngắn dưới tiêu đề</label>
              <textarea
                class="form-control"
                rows="3"
                v-model="settings.HOME_HERO_LEAD"
                placeholder="VD: Sở hữu ngay iPhone, iPad, Samsung Galaxy chính hãng..."
              ></textarea>
            </div>
          </div>

          <h5
            class="fw-bold text-primary mb-3 border-bottom pb-2 d-flex align-items-center justify-content-between"
          >
            <span>7. Slide sản phẩm nổi bật (màn hình điện thoại Hero)</span>
            <button
              type="button"
              class="btn btn-sm btn-outline-primary rounded-pill"
              @click="addSlide"
            >
              <i class="fas fa-plus me-1"></i> Thêm slide
            </button>
          </h5>
          <div class="mb-4">
            <div
              v-for="(slide, idx) in slidesData"
              :key="idx"
              class="row g-2 align-items-end mb-3 p-3 bg-light rounded-3 border"
            >
              <div class="col-md-3">
                <label class="form-label fw-semibold small">Kicker (nhãn trên)</label>
                <input
                  type="text"
                  class="form-control"
                  v-model="slide.kicker"
                  placeholder="VD: FLAGSHIP 2026"
                />
              </div>
              <div class="col-md-3">
                <label class="form-label fw-semibold small">Tên sản phẩm</label>
                <input
                  type="text"
                  class="form-control"
                  v-model="slide.name"
                  placeholder="VD: iPhone 17 Pro Max"
                />
              </div>
              <div class="col-md-3">
                <label class="form-label fw-semibold small">Giá hiển thị</label>
                <input
                  type="text"
                  class="form-control"
                  v-model="slide.price"
                  placeholder="VD: 32.990.000đ"
                />
              </div>
              <div class="col-md-2">
                <label class="form-label fw-semibold small">Tag khuyến mãi</label>
                <input
                  type="text"
                  class="form-control"
                  v-model="slide.tag"
                  placeholder="VD: Trả góp 0%"
                />
              </div>
              <div class="col-md-1 text-end">
                <button
                  type="button"
                  class="btn btn-outline-danger btn-sm rounded-circle"
                  title="Xóa slide này"
                  :disabled="slidesData.length <= 1"
                  @click="removeSlide(idx)"
                >
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </div>
            <div v-if="slidesData.length === 0" class="text-muted small">
              Chưa có slide nào, bấm "Thêm slide" để tạo mới.
            </div>
          </div>

          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            8. Chính sách tư vấn Marcus AI
          </h5>
          <div class="mb-4 rounded-3 border bg-light p-3">
            <label class="form-label fw-semibold">Hướng dẫn bổ sung cho AI</label>
            <textarea
              v-model="settings.AI_ADVISOR_POLICY"
              class="form-control"
              rows="4"
              maxlength="1000"
              placeholder="VD: Ưu tiên sản phẩm còn hàng, giải thích dễ hiểu cho sinh viên..."
            ></textarea>
            <div class="form-text">
              Chỉ dùng để chỉnh giọng điệu và ưu tiên tư vấn. Quy tắc bảo mật trong backend không
              thể bị ghi đè.
            </div>
            <div v-if="aiClickStats.length" class="mt-3">
              <div class="fw-semibold mb-2">Sản phẩm được mở nhiều nhất từ Marcus AI</div>
              <div class="table-responsive rounded-3 border bg-white">
                <table class="table table-sm align-middle mb-0">
                  <thead>
                    <tr>
                      <th>Sản phẩm</th>
                      <th class="text-end">Lượt mở</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in aiClickStats" :key="item.productId">
                      <td>{{ item.productName }}</td>
                      <td class="text-end fw-bold">{{ item.clickCount }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="text-end mt-5">
            <button
              type="submit"
              class="btn text-white fw-bold px-5 py-2 rounded-pill shadow-sm btn-save-custom"
              :disabled="isSaving"
            >
              <i class="fas" :class="isSaving ? 'fa-spinner fa-spin' : 'fa-save'"></i>
              <span class="ms-2">{{
                isSaving ? 'Đang đồng bộ dữ liệu...' : 'Lưu toàn bộ thay đổi'
              }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
    <BaseModal
      :visible="localModal.visible"
      :type="localModal.type"
      :title="localModal.title"
      :message="localModal.message"
      @close="localModal.visible = false"
      @confirm="executeModalAction"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import api from '@/utils/api'
import BaseModal from '@/components/BaseModal.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import { useSettings } from '@/composables/useSettings'

const { fetchSettings: refreshPublicSettings } = useSettings()

const isLoading = ref(true)
const isSaving = ref(false)
const aiClickStats = ref([])
const settingsMeta = reactive({ updatedBy: '', updatedAt: null })
const pendingAction = ref('')

// State Modal
const localModal = reactive({
  visible: false,
  type: 'info',
  title: '',
  message: '',
})

const showAlert = (type, title, msg) => {
  localModal.type = type
  localModal.title = title
  localModal.message = msg
  localModal.visible = true
}

const mapData = ref({ lat: '', lng: '', name: '', address: '' })
const logoFileInput = ref(null)
const isLogoUploading = ref(false)
const logoUploadProgress = ref(0)
const logoPreviewError = ref(false)
const localLogoPreview = ref('')
const logoDisplayUrl = computed(() => localLogoPreview.value || settings.value.SITE_LOGO_URL)

const settings = ref({
  SITE_NAME: 'Marcus Store',
  SITE_LOGO_URL: '',
  HOTLINE: '',
  EMAIL: '',
  ADDRESS: '',
  WORKING_HOURS: '',
  PROMO_TEXT: '',
  FACEBOOK_URL: '',
  TIKTOK_URL: '',
  INSTAGRAM_URL: '',
  YOUTUBE_URL: '',
  STORE_LOCATION: '',
  HOME_HERO_BADGE: '',
  HOME_HERO_TITLE: '',
  HOME_HERO_TITLE_ACCENT: '',
  HOME_HERO_LEAD: '',
  HOME_HERO_SLIDES: '',
  AI_ADVISOR_POLICY: '',
})

// Slide gốc dùng làm mặc định khi DB chưa có dữ liệu HOME_HERO_SLIDES
const defaultSlides = [
  {
    kicker: 'FLAGSHIP 2026',
    name: 'Samsung Galaxy S26 Ultra',
    price: '26.990.000đ',
    tag: 'Trả góp 0%',
  },
  {
    kicker: 'BÁN CHẠY NHẤT',
    name: 'iPhone 17 Pro Max',
    price: '32.990.000đ',
    tag: 'Thu cũ trợ giá 5.000.000đ',
  },
  { kicker: 'ĐÁNG MUA', name: 'iPad Air M3', price: '16.990.000đ', tag: 'Tặng bút Apple Pencil' },
]

const slidesData = ref([])

const addSlide = () => {
  slidesData.value.push({ kicker: '', name: '', price: '', tag: '' })
}

const removeSlide = (idx) => {
  slidesData.value.splice(idx, 1)
}

const uploadLogoFile = async (file) => {
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    showAlert('error', 'Ảnh không hợp lệ', 'Logo chỉ hỗ trợ JPG, PNG hoặc WEBP.')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    showAlert('error', 'Ảnh quá lớn', 'Ảnh Logo không được vượt quá 2 MB.')
    return
  }

  // Marcus thêm: xem ảnh local ngay khi chọn, không phải chờ upload/lưu cấu hình.
  releaseLocalLogoPreview()
  localLogoPreview.value = URL.createObjectURL(file)
  logoPreviewError.value = false

  const formData = new FormData()
  formData.append('file', file)
  isLogoUploading.value = true
  logoUploadProgress.value = 0
  try {
    const response = await api.post('/admin/settings/upload-logo', formData, {
      skipGlobalLoading: true,
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          logoUploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
      },
    })
    const imageUrl = response.data?.data?.imageUrl
    if (!imageUrl) throw new Error('Server không trả về URL Logo.')
    settings.value.SITE_LOGO_URL = imageUrl
    releaseLocalLogoPreview()
    logoPreviewError.value = false
    showAlert('success', 'Đã tải Logo', 'Logo đã tải thành công. Nhấn “Lưu thay đổi” để áp dụng toàn hệ thống.')
  } catch (error) {
    showAlert('error', 'Không thể tải Logo', error.response?.data?.message || error.message || 'Vui lòng thử lại.')
  } finally {
    isLogoUploading.value = false
    logoUploadProgress.value = 0
  }
}

const handleLogoFile = (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  uploadLogoFile(file)
}

const handleLogoDrop = (event) => {
  if (isLogoUploading.value) return
  uploadLogoFile(event.dataTransfer?.files?.[0])
}

const removeLogo = () => {
  releaseLocalLogoPreview()
  settings.value.SITE_LOGO_URL = ''
  logoPreviewError.value = false
}

const releaseLocalLogoPreview = () => {
  if (localLogoPreview.value) URL.revokeObjectURL(localLogoPreview.value)
  localLogoPreview.value = ''
}

const loadSettings = async () => {
  try {
    isLoading.value = true
    // Marcus sửa: trang quản trị đọc endpoint nội bộ; prompt AI không xuất hiện ở
    // API cấu hình công khai.
    const res = await api.get('/admin/settings')

    const payload = res.data?.data ?? res.data ?? {}
    const loadedSettings = payload.settings ?? payload
    settingsMeta.updatedBy = payload.updatedBy || ''
    settingsMeta.updatedAt = payload.updatedAt || null
    Object.keys(settings.value).forEach((key) => {
      if (loadedSettings[key] !== undefined) {
        settings.value[key] = loadedSettings[key]
      }
    })

    // Marcus thêm: thống kê chỉ chứa số click theo sản phẩm, không có dữ liệu chat
    // hay thông tin định danh khách hàng.
    try {
      const statsRes = await api.get('/admin/ai-advisor/top-clicked-products', {
        skipGlobalLoading: true,
      })
      aiClickStats.value = statsRes.data?.data ?? []
    } catch {
      aiClickStats.value = []
    }

    if (settings.value.STORE_LOCATION) {
      const parsedMap = JSON.parse(settings.value.STORE_LOCATION)
      mapData.value = { ...mapData.value, ...parsedMap }
    }

    if (settings.value.HOME_HERO_SLIDES) {
      try {
        const parsedSlides = JSON.parse(settings.value.HOME_HERO_SLIDES)
        slidesData.value =
          Array.isArray(parsedSlides) && parsedSlides.length ? parsedSlides : defaultSlides
      } catch (e) {
        console.error('Lỗi parse HOME_HERO_SLIDES:', e)
        slidesData.value = defaultSlides
      }
    } else {
      slidesData.value = defaultSlides
    }
  } catch (error) {
    console.error('Lỗi tải cấu hình:', error)
    showAlert('error', 'Lỗi', 'Không thể tải cấu hình hệ thống!')
  } finally {
    isLoading.value = false
  }
}

const confirmRestoreDefaults = () => {
  pendingAction.value = 'RESTORE_DEFAULTS'
  localModal.type = 'confirm'
  localModal.title = 'Khôi phục cấu hình mặc định?'
  localModal.message = 'Tên Website, Logo, liên hệ, mạng xã hội, bản đồ và Hero sẽ trở về bộ mặc định của Marcus Store.'
  localModal.visible = true
}

const executeModalAction = async () => {
  if (pendingAction.value !== 'RESTORE_DEFAULTS') return
  localModal.visible = false
  pendingAction.value = ''
  try {
    isSaving.value = true
    await api.post('/admin/settings/restore-defaults')
    await loadSettings()
    await refreshPublicSettings(true)
    showAlert('success', 'Đã khôi phục', 'Cấu hình mặc định đã được áp dụng.')
  } catch (error) {
    showAlert('error', 'Không thể khôi phục', error.response?.data?.message || 'Vui lòng thử lại.')
  } finally {
    isSaving.value = false
  }
}

const formatDateTime = (value) => value ? new Date(value).toLocaleString('vi-VN') : '—'

const saveSettings = async () => {
  try {
    isSaving.value = true
    settings.value.STORE_LOCATION = JSON.stringify(mapData.value)
    settings.value.HOME_HERO_SLIDES = JSON.stringify(slidesData.value)

    // ĐÃ FIX: Bỏ "const res =" đi
    // Marcus sửa đồng bộ DTO backend: payload cấu hình nằm trong field settings.
    await api.put('/admin/settings/bulk-update', { settings: settings.value })
    // Marcus thêm: cập nhật cache nhận diện dùng chung ngay, không bắt Admin F5.
    await refreshPublicSettings(true)

    // ĐÃ FIX: Dùng Modal thay cho alert()
    showAlert('success', 'Thành công', 'Đã cập nhật cấu hình hệ thống thành công!')
  } catch (error) {
    console.error('Lỗi khi cập nhật:', error)
    showAlert('error', 'Cập nhật thất bại', error.response?.data?.message || 'Lỗi hệ thống')
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  loadSettings()
})
onBeforeUnmount(releaseLocalLogoPreview)
</script>

<style scoped>
.system-settings-page {
  min-height: 100%;
  padding: 28px;
  background: #f4f7fb;
}

.branding-settings {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 230px;
  gap: 24px;
  padding: 20px;
  border: 1px solid #dbe6f5;
  border-radius: 16px;
  background: #f8fbff;
}

.branding-fields { min-width: 0; }
.setting-help { display: block; margin-top: 6px; color: #64748b; line-height: 1.45; }
.logo-upload-zone {
  width: 100%;
  min-height: 132px;
  margin-top: 18px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 2px dashed #a9c8f5;
  border-radius: 14px;
  color: #174b91;
  background: #fff;
  transition: border-color .2s ease, background .2s ease, transform .2s ease;
}
.logo-upload-zone:hover:not(:disabled) { border-color: #2563eb; background: #f1f7ff; transform: translateY(-1px); }
.logo-upload-zone.is-uploading { cursor: wait; background: #f6f9fe; }
.upload-zone-icon { font-size: 28px; color: #2563eb; }
.logo-upload-zone > span:not(.upload-progress) { font-weight: 750; }
.logo-upload-zone small { color: #64748b; }
.upload-progress { width: min(320px, 90%); height: 5px; overflow: hidden; border-radius: 99px; background: #dbeafe; }
.upload-progress i { display: block; height: 100%; border-radius: inherit; background: #2563eb; transition: width .15s ease; }
.branding-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 16px; }
.brand-remove-btn {
  border-radius: 10px;
  padding: 9px 14px;
  font-weight: 700;
  border: 1px solid #bfd5f4;
}
.brand-remove-btn { color: #dc2626; background: #fff; border-color: #fecaca; }
.branding-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 210px;
  padding: 16px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e1e9f5;
  text-align: center;
}
.settings-audit { display: flex; gap: 8px; margin: -6px 0 18px; color: #64748b; font-size: 13px; }
.settings-restore-btn { border-color: #b9d2f3 !important; background: #fff !important; color: #175ca8 !important; }
.preview-label { align-self: flex-start; color: #64748b; font-size: 12px; font-weight: 700; text-transform: uppercase; }
.preview-logo-box { width: 96px; height: 96px; padding: 10px; border-radius: 22px; background: #eff6ff; }
.preview-fallback { font-size: 52px; color: #2563eb; line-height: 76px; }
.site-logo-preview {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
@media (max-width: 768px) {
  .branding-settings { grid-template-columns: 1fr; }
}

.settings-card {
  margin-top: 20px;
  border: 1px solid #c7dcef !important;
  box-shadow: 0 8px 24px rgba(15, 35, 64, 0.05) !important;
}

.form-control {
  border-radius: 8px;
  padding: 12px 15px;
  border: 1px solid #c7dcef;
  font-size: 14px;
}
.form-control:focus {
  border-color: #ff6b00;
  box-shadow: 0 0 0 0.25rem rgba(255, 107, 0, 0.15);
}

.system-settings-page :deep(.border) {
  border-color: #c7dcef !important;
}
.btn-save-custom {
  background: linear-gradient(135deg, #ff6b00, #ff8e3c);
  border: none;
  transition: all 0.3s ease;
}
.btn-save-custom:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(255, 107, 0, 0.4) !important;
}

@media (max-width: 760px) {
  .system-settings-page {
    padding: 16px;
  }
}
</style>
