<template>
  <section class="cps-map-section">
    <div class="cps-container">
      <div class="map-header">
        <h2 class="title">
          <i class="fa-solid fa-location-dot"></i> Hệ thống cửa hàng Marcus Store
        </h2>
        <p class="subtitle">Đến trực tiếp để trải nghiệm dịch vụ và sản phẩm công nghệ đỉnh cao</p>
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
            :href="`https://www.google.com/maps/dir/?api=1&destination=${storeInfo.lat},${storeInfo.lng}`"
            target="_blank"
            class="btn-direction"
          >
            <i class="fa-solid fa-location-arrow"></i> Chỉ đường
          </a>
        </div>

        <div class="map-frame">
          <div v-if="isLoading" class="map-loading">
            <div class="spinner"></div>
            <p>Đang tải bản đồ...</p>
          </div>
          <div id="marcus-store-map" class="leaflet-map"></div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import api from '@/utils/api'

// --- Sửa lỗi mất icon mặc định của Leaflet trong Vue/Vite ---
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

// --- State ---
const isLoading = ref(true)
let mapInstance = null
const storeInfo = ref({
  lat: 20.82716,
  lng: 106.70466,
  name: 'Marcus Store Hải Phòng',
  address: '118 Cát Bi, Hải An, Hải Phòng',
})

// --- Lấy data từ System Settings ---
const fetchMapLocation = async () => {
  try {
    const res = await api.get('/public/settings/STORE_LOCATION')
    const rawValue = res.data?.data?.settingValue || res.data?.settingValue

    if (rawValue) {
      const dbLocation = JSON.parse(rawValue)
      storeInfo.value = { ...storeInfo.value, ...dbLocation }
    }
  } catch (error) {
    console.error('Không tải được tọa độ từ DB, dùng tọa độ mặc định', error)
  } finally {
    isLoading.value = false
    initMap(storeInfo.value)
  }
}

// --- Khởi tạo bản đồ ---
const initMap = async (loc) => {
  await nextTick() // Chờ DOM render div #marcus-store-map

  if (mapInstance) {
    mapInstance.remove()
  }

  mapInstance = L.map('marcus-store-map', {
    center: [loc.lat, loc.lng],
    zoom: 16,
    zoomControl: false, // Tắt nút zoom mặc định để tự custom nếu cần
  })

  // Thêm layer bản đồ đường phố
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap',
  }).addTo(mapInstance)

  // Cắm Marker
  const marker = L.marker([loc.lat, loc.lng]).addTo(mapInstance)

  // Gắn Popup chuẩn màu CellphoneS
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

onMounted(() => {
  fetchMapLocation()
})
</script>

<style scoped>
/* Tone màu CellphoneS: Đỏ (#d70018) - Nền Trắng/Xám nhạt */
.cps-map-section {
  padding: 40px 0;
  background-color: #f3f4f6; /* Nền xám nhạt làm nổi bật khối trắng */
}

.cps-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}

.map-header {
  text-align: center;
  margin-bottom: 25px;
}

.map-header .title {
  color: #333;
  font-size: 24px;
  font-weight: 800;
  text-transform: uppercase;
  margin-bottom: 8px;
}

.map-header .title i {
  color: #d70018;
}

.map-header .subtitle {
  color: #666;
  font-size: 14px;
}

.map-wrapper {
  position: relative;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  height: 500px;
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
  z-index: 1; /* Để popup Overlay nổi lên trên */
}

/* Khung Overlay (Đè lên góc trái bản đồ) */
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
  box-shadow: 0 4px 10px rgba(215, 0, 24, 0.3);
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

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* Responsive */
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
    height: 400px;
  }
}
</style>
