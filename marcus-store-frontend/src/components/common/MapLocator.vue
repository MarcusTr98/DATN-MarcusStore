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
        <button class="btn btn-secondary" @click.prevent="locateMe" title="Lấy vị trí hiện tại">
          <i class="fas fa-crosshairs"></i>
        </button>
        <button class="btn btn-danger" @click.prevent="searchLocation" :disabled="isLoading">
          <i class="fas fa-search me-1"></i> {{ isLoading ? '...' : 'Tìm' }}
        </button>
      </div>
    </div>

    <div ref="mapContainer" class="map-container"></div>

    <div v-if="!readonly && selectedAddress" class="selected-info mt-3">
      <div class="d-flex justify-content-between align-items-center gap-3">
        <div style="flex: 1">
          <p class="mb-1 text-dark small"><strong>📍 Gợi ý từ Bản đồ:</strong></p>
          <p class="mb-0 text-muted small">{{ selectedAddress }}</p>
        </div>
        <button
          class="btn btn-sm btn-danger text-nowrap shadow-sm fw-bold"
          @click.prevent="emitApply"
        >
          <i class="fas fa-level-down-alt me-1"></i> Điền xuống Form
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
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

// KHAI BÁO THÊM SỰ KIỆN apply-address
const emit = defineEmits(['update:location', 'apply-address'])

const mapContainer = ref(null)
const searchQuery = ref('')
const selectedAddress = ref('')
const isLoading = ref(false)

let currentLat = props.initialLat || 20.846733
let currentLng = props.initialLng || 106.666014

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

    if (!props.initialLat) {
      locateMe()
    } else {
      reverseGeocode(currentLat, currentLng)
    }
  } else {
    if (props.markers.length > 0) {
      props.markers.forEach((mk) => {
        L.marker([mk.lat, mk.lng]).addTo(map).bindPopup(`<b>${mk.name}</b><br>${mk.address}`)
      })
      map.setView([props.markers[0].lat, props.markers[0].lng], 13)
    }
  }

  setTimeout(() => {
    if (map) {
      map.invalidateSize()
    }
  }, 350)
}

const locateMe = () => {
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        await updateMarker(position.coords.latitude, position.coords.longitude)
      },
      () => {
        console.warn('User từ chối định vị.')
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

const lastParsedData = ref(null)

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

      // 1. HÀM DỌN RÁC TIẾNG ANH (Làm sạch ngay từ đầu)
      const cleanText = (text) => {
        if (!text) return ''
        // Xóa các chữ thừa: Province, City, District, Ward, Commune, Town...
        return text
          .replace(/Province|City|District|Ward|Commune|County|Town|Municipality/gi, '')
          .trim()
      }

      // 2. LẤY THEO CẤU TRÚC CHUẨN CỦA NOMINATIM (Độ chính xác cao nhất)
      // Tỉnh / TP Trực thuộc TW thường rơi vào city, state, province
      let province = addr.city || addr.state || addr.province || addr.region || ''

      // Quận / Huyện thường rơi vào county, city_district, district, town
      let district =
        addr.county || addr.city_district || addr.district || addr.town || addr.municipality || ''

      // Phường / Xã thường rơi vào suburb, quarter, village, ward, hamlet
      let ward =
        addr.suburb ||
        addr.quarter ||
        addr.village ||
        addr.ward ||
        addr.hamlet ||
        addr.neighbourhood ||
        ''

      // 3. THUẬT TOÁN FALLBACK (CỨU CÁNH NẾU NOMINATIM BỊ KHUYẾT DỮ LIỆU)
      // Nếu API bị điên, không trả về đủ Tỉnh/Quận/Xã, ta mới cắt chuỗi display_name
      if (!province || !district || !ward) {
        const parts = data.display_name.split(',').map((s) => s.trim())

        // BỘ LỌC THÉP: Bỏ Vietnam VÀ bỏ MỌI chuỗi có chứa chữ số (diệt tận gốc mã bưu điện 18000, 100000...)
        const cleanParts = parts.filter(
          (p) => !p.includes('Vietnam') && !p.includes('Việt Nam') && !/\d/.test(p),
        )

        const len = cleanParts.length
        // Điền bù lỗ hổng từ dưới lên (Tỉnh -> Quận -> Xã)
        if (!province && len >= 1) province = cleanParts[len - 1]
        if (!district && len >= 2) district = cleanParts[len - 2]
        if (!ward && len >= 3) ward = cleanParts[len - 3]
      }

      // 4. LÀM SẠCH TEXT CUỐI CÙNG TRƯỚC KHI ĐỔ RA FORM
      province = cleanText(province)
      district = cleanText(district)
      ward = cleanText(ward)

      // 5. XỬ LÝ SỐ NHÀ, TÊN ĐƯỜNG
      let detailArr = []
      if (addr.house_number) detailArr.push(addr.house_number)
      if (addr.road) detailArr.push(addr.road)
      let detail = detailArr.join(', ')

      // Nếu không có tên đường rõ ràng, lấy cụm đầu tiên của display_name (thường là tên tòa nhà / địa danh)
      if (!detail) {
        detail = data.display_name.split(',')[0].trim()
      }

      lastParsedData.value = { lat, lng, province, district, ward, detail, full: data.display_name }

      // Bắn tọa độ ngầm ra ngoài để lưu DB
      emit('update:location', lastParsedData.value)
    }
  } catch (error) {
    console.error('Lỗi Geocode:', error)
  }
}

const emitApply = () => {
  if (lastParsedData.value) {
    // Khi bấm nút, bắn toàn bộ text ra để điền form
    emit('apply-address', lastParsedData.value)
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
