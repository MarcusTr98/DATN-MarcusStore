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

          <h5 class="fw-bold text-primary mb-3 border-bottom pb-2">
            4. Cấu hình Bản đồ & Cửa hàng (Store Location)
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
            5. Nội dung Hero Trang chủ (Home.vue)
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
            <span>6. Slide sản phẩm nổi bật (màn hình điện thoại Hero)</span>
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
            7. Chính sách tư vấn Marcus AI
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
              Chỉ dùng để chỉnh giọng điệu và ưu tiên tư vấn. Quy tắc bảo mật trong backend không thể bị ghi đè.
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
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '@/utils/api'
import BaseModal from '@/components/BaseModal.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const isLoading = ref(true)
const isSaving = ref(false)
const aiClickStats = ref([])

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

const loadSettings = async () => {
  try {
    isLoading.value = true
    // Marcus sửa: trang quản trị đọc endpoint nội bộ; prompt AI không xuất hiện ở
    // API cấu hình công khai.
    const res = await api.get('/admin/settings')

    Object.keys(settings.value).forEach((key) => {
      if (res.data[key] !== undefined) {
        settings.value[key] = res.data[key]
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

const saveSettings = async () => {
  try {
    isSaving.value = true
    settings.value.STORE_LOCATION = JSON.stringify(mapData.value)
    settings.value.HOME_HERO_SLIDES = JSON.stringify(slidesData.value)

    // ĐÃ FIX: Bỏ "const res =" đi
    await api.put('/admin/settings/bulk-update', settings.value)

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
</script>

<style scoped>
.system-settings-page {
  min-height: 100%;
  padding: 28px;
  background: #f4f7fb;
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
