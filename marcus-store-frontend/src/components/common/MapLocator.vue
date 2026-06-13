<template>
  <div class="mlc-wrapper">
    <div v-if="!readonly" class="mlc-toolbar">
      <span class="mlc-hint">
        <i class="fas fa-hand-pointer"></i>
        Kéo ghim hoặc nhấp bản đồ để đặt vị trí
      </span>
      <button class="mlc-btn-locate" @click.prevent="locateMe" :disabled="isLocating">
        <i :class="isLocating ? 'fas fa-spinner fa-spin' : 'fas fa-crosshairs'"></i>
        {{ isLocating ? 'Đang định vị...' : 'Vị trí của tôi' }}
      </button>
    </div>

    <div class="mlc-stage" :class="{ 'mlc-stage--flying': isFlying }">
      <div ref="mapContainer" class="mlc-canvas"></div>

      <transition name="mlc-fade">
        <div v-if="isFlying" class="mlc-overlay">
          <div class="mlc-pill">
            <i class="fas fa-location-arrow fa-spin"></i>
            Đang điều hướng tới khu vực...
          </div>
        </div>
      </transition>

      <div class="mlc-zoom-badge" v-if="!isFlying">
        <i class="fas fa-map-marker-alt"></i>
      </div>
    </div>

    <transition name="mlc-slide">
      <div v-if="!readonly && displayAddress" class="mlc-pinned">
        <span class="mlc-pinned__dot"></span>
        <span class="mlc-pinned__text">{{ displayAddress }}</span>
        <button class="mlc-pinned__clear" @click="clearPin" title="Bỏ ghim">
          <i class="fas fa-times"></i>
        </button>
      </div>
    </transition>
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
const emit = defineEmits(['update:location'])

const mapContainer = ref(null)
const displayAddress = ref('')
const isLocating = ref(false)
const isFlying = ref(false)

let currentLat = props.initialLat ?? 16.047079
let currentLng = props.initialLng ?? 108.20623
let map = null
let mainMarker = null

const makeRedIcon = () =>
  L.divIcon({
    className: '',
    html: `<div class="mlc-pin"><div class="mlc-pin__head"></div><div class="mlc-pin__shadow"></div></div>`,
    iconSize: [28, 40],
    iconAnchor: [14, 40],
  })

onMounted(async () => {
  await nextTick()
  initMap()
})

onBeforeUnmount(() => {
  map?.remove()
  map = null
})

const initMap = () => {
  const initialZoom = props.initialLat ? 16 : 5

  map = L.map(mapContainer.value, {
    zoomControl: true,
    attributionControl: false,
  }).setView([currentLat, currentLng], initialZoom)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
  }).addTo(map)

  if (!props.readonly) {
    mainMarker = L.marker([currentLat, currentLng], {
      draggable: true,
      icon: makeRedIcon(),
    }).addTo(map)

    mainMarker.on('dragend', async (e) => {
      const { lat, lng } = e.target.getLatLng()
      await commitMarker(lat, lng)
    })

    map.on('click', async (e) => {
      await commitMarker(e.latlng.lat, e.latlng.lng)
    })

    if (props.initialLat && props.initialLng) {
      reverseGeocode(props.initialLat, props.initialLng)
    }
  } else {
    if (props.markers.length > 0) {
      props.markers.forEach((mk) =>
        L.marker([mk.lat, mk.lng]).addTo(map).bindPopup(`<b>${mk.name}</b><br>${mk.address}`),
      )
      map.setView([props.markers[0].lat, props.markers[0].lng], 13)
    }
  }

  setTimeout(() => map?.invalidateSize(), 350)
}

const commitMarker = async (lat, lng) => {
  currentLat = lat
  currentLng = lng
  mainMarker.setLatLng([lat, lng])
  emit('update:location', { lat, lng })
  await reverseGeocode(lat, lng)
}

const reverseGeocode = async (lat, lng) => {
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&accept-language=vi&lat=${lat}&lon=${lng}`,
    )
    const data = await res.json()
    displayAddress.value = data?.display_name ?? `${lat.toFixed(6)}, ${lng.toFixed(6)}`
  } catch {
    displayAddress.value = `${lat.toFixed(6)}, ${lng.toFixed(6)}`
  }
}

const locateMe = () => {
  if (!('geolocation' in navigator)) return
  isLocating.value = true

  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      const { latitude: lat, longitude: lng } = pos.coords
      await commitMarker(lat, lng)
      map.flyTo([lat, lng], 17, { duration: 1.2 })
      isLocating.value = false
    },
    (err) => {
      console.warn('[MapLocator] Định vị thất bại:', err)
      alert('Không thể lấy vị trí. Hãy cấp quyền Định vị cho trình duyệt.')
      isLocating.value = false
    },
    { timeout: 10_000 },
  )
}

const clearPin = () => {
  displayAddress.value = ''
  emit('update:location', { lat: null, lng: null })
}

const flyToAddress = async (addressText) => {
  if (!addressText || !map) return
  isFlying.value = true
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&accept-language=vi&limit=1&q=${encodeURIComponent(addressText)}`,
    )
    const data = await res.json()
    if (data.length > 0) {
      const lat = parseFloat(data[0].lat)
      const lng = parseFloat(data[0].lon)

      map.flyTo([lat, lng], 14, { duration: 1.4, easeLinearity: 0.25 })

      setTimeout(() => {
        if (mainMarker) {
          mainMarker.setLatLng([lat, lng])
          currentLat = lat
          currentLng = lng
        }
      }, 1500)
    }
  } catch (err) {
    console.error('[MapLocator] flyToAddress lỗi, giữ nguyên vị trí:', err)
  } finally {
    setTimeout(() => {
      isFlying.value = false
    }, 1700)
  }
}

defineExpose({ flyToAddress })
</script>

<style scoped src="@/assets/css/MapLocator.css"></style>
