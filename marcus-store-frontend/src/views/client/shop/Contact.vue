<template>
  <main class="contact-page bg-gray">
    <section class="cps-map-section">
      <div class="cps-container">
        <div class="section-header">
          <h2 class="title">
            <i class="fa-solid fa-location-dot"></i> Hệ thống cửa hàng Marcus Store
          </h2>
          <p class="subtitle">
            Đến trực tiếp để trải nghiệm dịch vụ và sản phẩm công nghệ đỉnh cao
          </p>
        </div>

        <div class="map-wrapper">
          <div class="store-info-overlay">
            <h3 class="store-name">{{ storeInfo.name }}</h3>
            <div class="store-detail">
              <i class="fa-solid fa-map-location-dot"></i>
              <span>{{ storeInfo.address }}</span>
            </div>
            <div class="store-detail">
              <i class="fa-solid fa-phone-volume"></i>
              <span>Hotline: 1800.xxxx (Miễn phí)</span>
            </div>
            <div class="store-detail">
              <i class="fa-solid fa-clock"></i>
              <span>Giờ mở cửa: 08:00 - 22:00</span>
            </div>
            <a
              :href="`http://maps.google.com/maps?q=${storeInfo.lat},${storeInfo.lng}`"
              target="_blank"
              class="btn-direction"
            >
              <i class="fa-solid fa-location-arrow"></i> Chỉ đường
            </a>
          </div>

          <div class="map-frame">
            <div v-if="isLoadingMap" class="map-loading">
              <div class="spinner"></div>
              <p>Đang tải bản đồ...</p>
            </div>
            <div id="marcus-store-map" class="leaflet-map"></div>
          </div>
        </div>
      </div>
    </section>

    <section class="cps-contact-section">
      <div class="cps-container">
        <div class="contact-grid">
          <div class="company-info-col card-shadow">
            <h3 class="col-title">Thông tin liên hệ</h3>
            <div class="company-name">CÔNG TY CỔ PHẦN CÔNG NGHỆ MARCUS</div>

            <ul class="info-list">
              <li>
                <div class="info-icon"><i class="fa-solid fa-building"></i></div>
                <div class="info-text">
                  <strong>Trụ sở chính:</strong> 118 Cát Bi, Hải An, Hải Phòng<br />
                  <small class="text-muted">GPĐKKD số 0123456789 do Sở KHĐT Hải Phòng cấp</small>
                </div>
              </li>
              <li>
                <div class="info-icon"><i class="fa-solid fa-envelope-open-text"></i></div>
                <div class="info-text"><strong>Email hỗ trợ:</strong> cskh@marcusstore.com</div>
              </li>
            </ul>

            <hr class="divider" />

            <h4 class="hotline-title">Tổng đài hỗ trợ (Miễn phí gọi)</h4>
            <div class="hotline-grid">
              <div class="hotline-box">
                <i class="fa-solid fa-headset"></i>
                <div>
                  <span>Gọi mua hàng</span>
                  <strong>1800.1111</strong>
                </div>
              </div>
              <div class="hotline-box">
                <i class="fa-solid fa-screwdriver-wrench"></i>
                <div>
                  <span>Hỗ trợ kỹ thuật</span>
                  <strong>1800.2222</strong>
                </div>
              </div>
              <div class="hotline-box full-width">
                <i class="fa-solid fa-comments"></i>
                <div>
                  <span>Góp ý, Khiếu nại (8h00 - 22h00)</span>
                  <strong>1800.3333</strong>
                </div>
              </div>
            </div>
          </div>

          <div class="contact-form-col card-shadow">
            <h3 class="col-title">Gửi tin nhắn cho chúng tôi</h3>
            <p class="form-desc">
              Chúng tôi sẽ phản hồi yêu cầu của bạn trong vòng 24 giờ làm việc.
            </p>

            <form @submit.prevent="submitContact" class="cps-form">
              <div class="form-group">
                <label>Họ và tên <span class="required">*</span></label>
                <input
                  v-model="form.name"
                  type="text"
                  maxlength="100"
                  placeholder="Nhập họ tên của bạn"
                  required
                />
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>Số điện thoại <span class="required">*</span></label>
                  <input
                    v-model="form.phone"
                    type="tel"
                    placeholder="Nhập số điện thoại"
                    required
                  />
                </div>
                <div class="form-group">
                  <label>Email</label>
                  <input
                    v-model="form.email"
                    type="email"
                    maxlength="100"
                    placeholder="Nhập email (Không bắt buộc)"
                  />
                </div>
              </div>

              <div class="form-group">
                <label>Nội dung cần hỗ trợ <span class="required">*</span></label>
                <textarea
                  v-model="form.message"
                  rows="4"
                  maxlength="2000"
                  placeholder="Vui lòng mô tả chi tiết vấn đề của bạn..."
                  required
                ></textarea>
              </div>

              <button type="submit" class="btn-submit" :disabled="isSubmitting">
                <i v-if="isSubmitting" class="fa-solid fa-spinner fa-spin"></i>
                <i v-else class="fa-solid fa-paper-plane"></i>
                {{ isSubmitting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
              </button>
            </form>
          </div>
        </div>
      </div>
    </section>

    <div class="cps-toast" :class="{ show: toastMessage }">
      <i class="fa-solid fa-circle-check"></i> {{ toastMessage }}
    </div>

    <!-- Marcus sửa: lỗi form liên hệ có thể dài/nhiều trường nên dùng modal thay
         alert của trình duyệt. -->
    <div v-if="responseModal.open" class="contact-modal-backdrop" @click.self="closeResponseModal">
      <div class="contact-modal" role="dialog" aria-modal="true">
        <div class="contact-modal-icon" :class="responseModal.type">
          <i
            class="fa-solid"
            :class="responseModal.type === 'error' ? 'fa-circle-exclamation' : 'fa-circle-check'"
          ></i>
        </div>
        <h3>{{ responseModal.title }}</h3>
        <p>{{ responseModal.message }}</p>
        <ul v-if="responseModal.fieldErrors.length" class="contact-error-list">
          <li v-for="item in responseModal.fieldErrors" :key="item">{{ item }}</li>
        </ul>
        <button type="button" class="btn-submit" @click="closeResponseModal">Đã hiểu</button>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import api from '@/utils/api'

//Cấu hình Icon Leaflet
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

// --- State cho Map ---
const isLoadingMap = ref(true)
let mapInstance = null
const storeInfo = ref({
  lat: 20.82716,
  lng: 106.70466,
  name: 'Marcus Store Hải Phòng',
  address: '118 Cát Bi, Hải An, Hải Phòng',
})

// --- State cho Form ---
const isSubmitting = ref(false)
const toastMessage = ref('')
const responseModal = reactive({
  open: false,
  type: 'error',
  title: '',
  message: '',
  fieldErrors: [],
})
const form = reactive({
  name: '',
  phone: '',
  email: '',
  message: '',
})

// --- Logic Map ---
const fetchMapLocation = async () => {
  try {
    const res = await api.get('/public/settings/STORE_LOCATION')
    const rawValue = res.data?.data?.settingValue || res.data?.settingValue
    if (rawValue) {
      const dbLocation = JSON.parse(rawValue)
      storeInfo.value = { ...storeInfo.value, ...dbLocation }
    }
  } catch (error) {
    console.error('Không tải được tọa độ từ DB, dùng mặc định', error)
  } finally {
    isLoadingMap.value = false
    initMap(storeInfo.value)
  }
}

const initMap = async (loc) => {
  await nextTick()
  if (mapInstance) mapInstance.remove()

  mapInstance = L.map('marcus-store-map', {
    center: [loc.lat, loc.lng],
    zoom: 16,
    zoomControl: false,
  })

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap',
  }).addTo(mapInstance)

  const marker = L.marker([loc.lat, loc.lng]).addTo(mapInstance)
  marker
    .bindPopup(
      `
    <div style="text-align: center; min-width: 180px;">
      <h6 style="color: #d70018; margin-bottom: 5px; font-weight: 700; font-size: 14px;">${loc.name}</h6>
      <p style="margin: 0; font-size: 12px; color: #444;">${loc.address}</p>
    </div>
  `,
    )
    .openPopup()
}

// Logic Form
const showToast = (msg) => {
  toastMessage.value = msg
  setTimeout(() => {
    toastMessage.value = ''
  }, 3000)
}

const closeResponseModal = () => {
  responseModal.open = false
}

const showContactError = (error) => {
  const payload = error.response?.data
  responseModal.open = true
  responseModal.type = 'error'
  responseModal.title = 'Chưa thể gửi yêu cầu'
  responseModal.message = payload?.message || 'Có lỗi xảy ra, vui lòng kiểm tra và thử lại.'
  responseModal.fieldErrors =
    payload?.data && typeof payload.data === 'object' && !Array.isArray(payload.data)
      ? Object.values(payload.data).filter(Boolean)
      : []
}

const submitContact = async () => {
  isSubmitting.value = true
  try {
    await api.post('/public/contact', form)

    showToast('Cảm ơn bạn! Yêu cầu đã được gửi thành công.')
    form.name = ''
    form.phone = ''
    form.email = ''
    form.message = ''
  } catch (error) {
    console.error(error)
    showContactError(error)
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  fetchMapLocation()
})
</script>

<style scoped>
.contact-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(3px);
}

.contact-modal {
  width: min(440px, 100%);
  padding: 28px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.24);
  text-align: center;
}

.contact-modal-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #fee2e2;
  color: #dc2626;
  font-size: 25px;
}

.contact-modal h3 {
  margin-bottom: 8px;
  color: #172033;
  font-size: 21px;
  font-weight: 800;
}

.contact-modal p {
  color: #64748b;
  line-height: 1.6;
}

.contact-error-list {
  margin: 14px 0 18px;
  padding: 12px 14px 12px 32px;
  border-radius: 10px;
  background: #fff7f7;
  color: #b91c1c;
  text-align: left;
}
.bg-gray {
  background-color: #f3f4f6;
  min-height: 100vh;
  padding-bottom: 60px;
}

.cps-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}

/* --- SECTION MAP --- */
.cps-map-section {
  padding: 40px 0 20px;
}

.section-header {
  text-align: center;
  margin-bottom: 25px;
}

.section-header .title {
  color: #333;
  font-size: 24px;
  font-weight: 800;
  text-transform: uppercase;
  margin-bottom: 8px;
}

.section-header .title i {
  color: #d70018;
}

.section-header .subtitle {
  color: #666;
  font-size: 14px;
}

.map-wrapper {
  position: relative;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  height: 450px;
  display: flex;
}

.map-frame {
  flex: 1;
  height: 100%;
  position: relative;
}

.leaflet-map {
  width: 100%;
  height: 100%;
  z-index: 1;
}

.store-info-overlay {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 10;
  background: white;
  width: 320px;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border-top: 4px solid #d70018;
}

.store-name {
  color: #d70018;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 15px;
  text-transform: uppercase;
}

.store-detail {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
  color: #444;
  font-size: 14px;
  line-height: 1.5;
}

.store-detail i {
  color: #d70018;
  margin-top: 3px;
  width: 16px;
  text-align: center;
}

.btn-direction {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  background: #d70018;
  color: white;
  padding: 10px;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 700;
  margin-top: 20px;
  transition: all 0.3s ease;
}

.btn-direction:hover {
  background: #b00012;
}

.map-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  z-index: 20;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #d70018;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

/* --- SECTION CONTACT FORM --- */
.cps-contact-section {
  padding-top: 20px;
}

.contact-grid {
  display: grid;
  grid-template-columns: 4fr 5fr;
  gap: 24px;
}

.card-shadow {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.col-title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin-bottom: 20px;
  text-transform: uppercase;
}

/* Company Info */
.company-name {
  font-weight: 800;
  color: #d70018;
  font-size: 16px;
  margin-bottom: 15px;
}

.info-list {
  list-style: none;
  padding: 0;
  margin: 0 0 25px 0;
}

.info-list li {
  display: flex;
  gap: 12px;
  margin-bottom: 15px;
  align-items: flex-start;
}

.info-icon i {
  color: #d70018;
  font-size: 18px;
  margin-top: 2px;
}

.info-text {
  font-size: 14px;
  color: #444;
  line-height: 1.5;
}

.divider {
  border: 0;
  height: 1px;
  background: #eee;
  margin: 20px 0;
}

.hotline-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 15px;
  color: #333;
}

.hotline-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.hotline-box {
  background: #fdf2f2;
  border: 1px solid #fecaca;
  padding: 15px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.hotline-box.full-width {
  grid-column: 1 / -1;
}

.hotline-box i {
  font-size: 24px;
  color: #d70018;
}

.hotline-box span {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}

.hotline-box strong {
  display: block;
  font-size: 16px;
  color: #d70018;
  font-weight: 800;
}

/* Contact Form */
.form-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
}

.cps-form .form-group {
  margin-bottom: 18px;
}

.cps-form .form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.cps-form label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.cps-form .required {
  color: #d70018;
}

.cps-form input,
.cps-form textarea {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.3s;
}

.cps-form input:focus,
.cps-form textarea:focus {
  outline: none;
  border-color: #d70018;
  box-shadow: 0 0 0 3px rgba(215, 0, 24, 0.1);
}

.btn-submit {
  width: 100%;
  background: #d70018;
  color: white;
  border: none;
  padding: 14px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: 0.3s;
}

.btn-submit:hover:not(:disabled) {
  background: #b00012;
}

.btn-submit:disabled {
  background: #fca5a5;
  cursor: not-allowed;
}

/* Toast */
.cps-toast {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%) translateY(100px);
  background: #22c55e;
  color: white;
  padding: 12px 24px;
  border-radius: 99px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);
  opacity: 0;
  transition: all 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  z-index: 9999;
}

.cps-toast.show {
  transform: translateX(-50%) translateY(0);
  opacity: 1;
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}

/* Responsive */
@media (max-width: 992px) {
  .contact-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .map-wrapper {
    flex-direction: column;
    height: auto;
  }
  .store-info-overlay {
    position: relative;
    top: 0;
    left: 0;
    width: 100%;
    border-radius: 0;
    box-shadow: none;
    border-top: none;
    border-bottom: 1px solid #eee;
  }
  .map-frame {
    height: 350px;
  }
  .cps-form .form-row {
    grid-template-columns: 1fr;
  }
  .hotline-grid {
    grid-template-columns: 1fr;
  }
}
</style>
