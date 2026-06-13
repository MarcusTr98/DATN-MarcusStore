<template>
  <div class="map-locator-wrapper">
    <div v-if="!readonly" class="d-flex justify-content-between align-items-center mb-2">
      <span class="small text-muted">
        <i class="fas fa-hand-pointer text-danger me-1"></i> Di chuyển ghim đỏ đến đúng vị trí của
        bạn
      </span>
      <button
        class="btn btn-sm btn-outline-danger shadow-sm fw-bold"
        @click.prevent="locateMe"
        :disabled="isLoading"
      >
        <i class="fas fa-crosshairs me-1"></i>
        {{ isLoading ? 'Đang định vị...' : 'Vị trí của tôi' }}
      </button>
    </div>

    <div ref="mapContainer" class="map-container shadow-sm"></div>

    <div
      v-if="!readonly && displayAddress"
      class="mt-2 p-2 bg-light border rounded small text-secondary"
    >
      <i class="fas fa-map-pin text-success me-1"></i> <strong>Đã ghim:</strong>
      {{ displayAddress }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'

// Cấu hình icon mặc định của Leaflet
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
const displayAddress = ref('')
const isLoading = ref(false)

// Mặc định ở giữa Việt Nam nếu chưa có tọa độ
let currentLat = props.initialLat || 16.047079
let currentLng = props.initialLng || 108.20623

let map = null
let mainMarker = null

onMounted(async () => {
  await nextTick()
  initMap()
})

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
})

const initMap = () => {
  map = L.map(mapContainer.value).setView([currentLat, currentLng], props.initialLat ? 16 : 5) // Nếu có tọa độ -> zoom gần, chưa có -> zoom xa toàn quốc
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

    // Nếu đã có tọa độ cũ, gọi hàm lấy tên nhãn
    if (props.initialLat && props.initialLng) {
      reverseGeocode(props.initialLat, props.initialLng)
    }
  } else {
    // Chế độ Read-only cho Store Locator
    if (props.markers.length > 0) {
      props.markers.forEach((mk) => {
        L.marker([mk.lat, mk.lng]).addTo(map).bindPopup(`<b>${mk.name}</b><br>${mk.address}`)
      })
      map.setView([props.markers[0].lat, props.markers[0].lng], 13)
    }
  }

  // Khắc phục lỗi xám bản đồ trong Modal
  setTimeout(() => {
    if (map) map.invalidateSize()
  }, 350)
}

// Chức năng lấy GPS hiện tại
const locateMe = () => {
  isLoading.value = true
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        await updateMarker(position.coords.latitude, position.coords.longitude)
        map.setView([position.coords.latitude, position.coords.longitude], 16)
        isLoading.value = false
      },
      (error) => {
        console.warn('Định vị thất bại:', error)
        alert('Không thể lấy được vị trí. Hãy chắc chắn bạn đã cấp quyền Định vị cho trình duyệt.')
        isLoading.value = false
      },
      { timeout: 10000 },
    )
  } else {
    isLoading.value = false
  }
}

const updateMarker = async (lat, lng) => {
  currentLat = lat
  currentLng = lng
  mainMarker.setLatLng([lat, lng])

  // Bắn tọa độ ngầm ra ngoài Form ngay lập tức
  emit('update:location', { lat, lng })

  // Lấy tên địa chỉ để hiển thị cho đẹp
  await reverseGeocode(lat, lng)
}

// Gọi API nhè nhẹ chỉ để lấy chữ hiển thị nhãn, không đè Form
const reverseGeocode = async (lat, lng) => {
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&accept-language=vi&lat=${lat}&lon=${lng}`,
    )
    const data = await res.json()
    if (data && data.display_name) {
      displayAddress.value = data.display_name
    } else {
      displayAddress.value = `${lat.toFixed(6)}, ${lng.toFixed(6)}`
    }
  } catch {
    displayAddress.value = `${lat.toFixed(6)}, ${lng.toFixed(6)}`
  }
}
</script>

<style scoped>
.map-locator-wrapper {
  width: 100%;
}
.map-container {
  height: 320px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  z-index: 1;
}
</style>
