<template>
  <div class="map-locator-wrapper">
    <div v-if="!readonly" class="search-box">
      <input
        v-model="searchQuery"
        @keyup.enter="searchLocation"
        type="text"
        placeholder="Nhập địa chỉ (VD: FPT Polytechnic Hải Phòng)..."
        class="form-control"
      />
      <button @click="searchLocation" class="btn btn-primary" :disabled="isLoading">
        {{ isLoading ? 'Đang tìm...' : 'Tìm kiếm' }}
      </button>
    </div>

    <div ref="mapContainer" class="map-container"></div>

    <div v-if="!readonly && selectedAddress" class="selected-info">
      <p><strong>Địa chỉ ghim:</strong> {{ selectedAddress }}</p>
      <small class="text-muted">Tọa độ: {{ currentLat }}, {{ currentLng }}</small>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'

// Fix lỗi mất icon mặc định của Leaflet khi build bằng Vite
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).href,
  iconUrl: new URL('leaflet/dist/images/marker-icon.png', import.meta.url).href,
  shadowUrl: new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).href,
})

const props = defineProps({
  // Nếu readonly = true, chỉ hiển thị, không cho kéo thả tìm kiếm (dùng cho Store Locator)
  readonly: { type: Boolean, default: false },
  // Tọa độ khởi tạo mặc định (Mặc định: Hà Nội)
  initialLat: { type: Number, default: 21.028511 },
  initialLng: { type: Number, default: 105.804817 },
  // Mảng các điểm cần ghim (Dành cho chức năng hiển thị nhiều cửa hàng)
  markers: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:location'])

const mapContainer = ref(null)
const searchQuery = ref('')
const selectedAddress = ref('')
const currentLat = ref(props.initialLat)
const currentLng = ref(props.initialLng)
const isLoading = ref(false)

let map = null
let mainMarker = null

onMounted(() => {
  initMap()
})

onBeforeUnmount(() => {
  if (map) map.remove()
})

const initMap = () => {
  map = L.map(mapContainer.value).setView([currentLat.value, currentLng.value], 15)

  // Load tile bản đồ từ OpenStreetMap
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)

  if (!props.readonly) {
    // Mode cắm ghim chọn địa chỉ (Sổ địa chỉ)
    mainMarker = L.marker([currentLat.value, currentLng.value], { draggable: true }).addTo(map)

    // Bắt sự kiện thả ghim
    mainMarker.on('dragend', async (e) => {
      const position = e.target.getLatLng()
      currentLat.value = position.lat
      currentLng.value = position.lng
      await reverseGeocode(position.lat, position.lng)
      emitLocation()
    })

    // Bắt sự kiện click trên bản đồ để di chuyển ghim
    map.on('click', async (e) => {
      currentLat.value = e.latlng.lat
      currentLng.value = e.latlng.lng
      mainMarker.setLatLng(e.latlng)
      await reverseGeocode(e.latlng.lat, e.latlng.lng)
      emitLocation()
    })
  } else {
    // Mode chỉ đọc (Store Locator)
    if (props.markers.length > 0) {
      props.markers.forEach((mk) => {
        L.marker([mk.lat, mk.lng]).addTo(map).bindPopup(`<b>${mk.name}</b><br>${mk.address}`)
      })
      // Căn giữa bản đồ dựa trên ghim đầu tiên
      map.setView([props.markers[0].lat, props.markers[0].lng], 13)
    }
  }
}

// Hàm 1: Tìm tọa độ từ địa chỉ text (Geocoding - dùng API Nominatim miễn phí)
const searchLocation = async () => {
  if (!searchQuery.value.trim()) return
  isLoading.value = true

  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(searchQuery.value)}`,
    )
    const data = await res.json()

    if (data && data.length > 0) {
      const { lat, lon, display_name } = data[0]
      currentLat.value = parseFloat(lat)
      currentLng.value = parseFloat(lon)
      selectedAddress.value = display_name

      map.setView([currentLat.value, currentLng.value], 16)
      mainMarker.setLatLng([currentLat.value, currentLng.value])
      emitLocation()
    } else {
      alert('Không tìm thấy địa điểm này. Vui lòng thử từ khóa khác.')
    }
  } catch (error) {
    console.error('Lỗi tìm kiếm địa chỉ:', error)
  } finally {
    isLoading.value = false
  }
}

// Hàm 2: Lấy địa chỉ text từ tọa độ cắm ghim (Reverse Geocoding)
const reverseGeocode = async (lat, lng) => {
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`,
    )
    const data = await res.json()
    if (data && data.display_name) {
      selectedAddress.value = data.display_name
      searchQuery.value = data.display_name // Cập nhật ngược lên ô tìm kiếm cho đẹp
    }
  } catch (error) {
    console.error('Lỗi lấy thông tin địa chỉ:', error)
  }
}

const emitLocation = () => {
  emit('update:location', {
    lat: currentLat.value,
    lng: currentLng.value,
    address: selectedAddress.value,
  })
}
</script>

<style scoped>
.map-locator-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}
.search-box {
  display: flex;
  gap: 8px;
}
.search-box input {
  flex: 1;
}
.map-container {
  height: 400px;
  width: 100%;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  z-index: 1; /* Fix lỗi đè z-index của leaflet lên header */
}
.selected-info {
  background: #fdf2f8;
  padding: 12px;
  border-radius: 8px;
  border: 1px dashed #fbcfe8;
  font-size: 14px;
}
.selected-info p {
  margin: 0 0 4px;
  color: #1e1b2e;
}
</style>
