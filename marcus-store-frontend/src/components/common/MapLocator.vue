<template>
  <div class="map-locator-wrapper">
    <div v-if="!readonly" class="search-box mb-2">
      <div class="input-group">
        <input
          v-model="searchQuery"
          @keyup.enter="searchLocation"
          type="text"
          class="form-control"
          placeholder="Nhập địa chỉ hoặc bấm nút bên cạnh..."
        />
        <button class="btn btn-secondary" @click="locateMe" title="Lấy vị trí hiện tại">
          <i class="fas fa-crosshairs"></i>
        </button>
        <button @click="searchLocation" class="btn btn-danger" :disabled="isLoading">
          <i class="fas fa-search me-1"></i> {{ isLoading ? '...' : 'Tìm' }}
        </button>
      </div>
    </div>

    <div ref="mapContainer" class="map-container"></div>

    <div v-if="!readonly && selectedAddress" class="selected-info mt-2">
      <p class="mb-0 text-dark small"><strong>Địa chỉ lấy từ Map:</strong> {{ selectedAddress }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).href,
  iconUrl: new URL('leaflet/dist/images/marker-icon.png', import.meta.url).href,
  shadowUrl: new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).href,
})

const props = defineProps({
  readonly: { type: Boolean, default: false },
  initialLat: { type: Number, default: null },
  initialLng: { type: Number, default: null },
  markers: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:location'])

const mapContainer = ref(null)
const searchQuery = ref('')
const selectedAddress = ref('')
const isLoading = ref(false)

// Mặc định FPT Polytechnic Hải Phòng nếu không có GPS
let currentLat = props.initialLat || 20.846733
let currentLng = props.initialLng || 106.666014

let map = null
let mainMarker = null

onMounted(() => {
  initMap()
})

onBeforeUnmount(() => {
  if (map) map.remove()
})

const initMap = () => {
  map = L.map(mapContainer.value).setView([currentLat, currentLng], 15)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map)

  if (!props.readonly) {
    mainMarker = L.marker([currentLat, currentLng], { draggable: true }).addTo(map)

    mainMarker.on('dragend', async (e) => {
      const pos = e.target.getLatLng()
      await updateMarker(pos.lat, pos.lng)
    })

    map.on('click', async (e) => {
      await updateMarker(e.latlng.lat, e.latlng.lng)
    })

    // Auto-location khi THÊM MỚI (initialLat bị null)
    if (!props.initialLat) {
      locateMe()
    } else {
      // Đang SỬA, load lại chữ
      reverseGeocode(currentLat, currentLng)
    }
  } else {
    // Mode xem nhiều điểm
    if (props.markers.length > 0) {
      props.markers.forEach((mk) => {
        L.marker([mk.lat, mk.lng]).addTo(map).bindPopup(`<b>${mk.name}</b><br>${mk.address}`)
      })
      map.setView([props.markers[0].lat, props.markers[0].lng], 13)
    }
  }
}

// Hàm lấy GPS thiết bị
const locateMe = () => {
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        await updateMarker(position.coords.latitude, position.coords.longitude)
      },
      () => {
        console.warn('User từ chối định vị. Dùng mặc định.')
        reverseGeocode(currentLat, currentLng)
      },
    )
  }
}

const updateMarker = async (lat, lng) => {
  currentLat = lat
  currentLng = lng
  mainMarker.setLatLng([lat, lng])
  map.setView([lat, lng], 16)
  await reverseGeocode(lat, lng)
}

const searchLocation = async () => {
  if (!searchQuery.value.trim()) return
  isLoading.value = true
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&q=${encodeURIComponent(searchQuery.value)}`,
    )
    const data = await res.json()
    if (data.length > 0) {
      await updateMarker(parseFloat(data[0].lat), parseFloat(data[0].lon))
    }
  } catch (err) {
    console.error(err)
  } finally {
    isLoading.value = false
  }
}

// Parse địa chỉ thông minh
const reverseGeocode = async (lat, lng) => {
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&addressdetails=1&lat=${lat}&lon=${lng}`,
    )
    const data = await res.json()
    if (data && data.address) {
      selectedAddress.value = data.display_name
      searchQuery.value = data.display_name

      const addr = data.address
      const province = addr.city || addr.state || addr.province || ''
      const district = addr.county || addr.city_district || addr.district || addr.town || ''
      const ward = addr.suburb || addr.village || addr.quarter || addr.neighbourhood || ''
      const detail = `${addr.house_number ? addr.house_number + ', ' : ''}${addr.road || ''}`

      emit('update:location', {
        lat,
        lng,
        province,
        district,
        ward,
        detail,
        full: data.display_name,
      })
    }
  } catch (error) {
    console.error('Lỗi Geocode:', error)
  }
}
</script>

<style scoped>
.map-locator-wrapper {
  width: 100%;
}
.map-container {
  height: 350px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  z-index: 1;
}
.selected-info {
  background: #fdf2f2;
  padding: 10px;
  border-radius: 8px;
  border: 1px dashed #fca5a5;
}
</style>
