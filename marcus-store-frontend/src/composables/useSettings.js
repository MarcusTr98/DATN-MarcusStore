import { computed, ref } from 'vue'
import api from '@/utils/api'

const sysSettings = ref({})
const isLoaded = ref(false)
const DEFAULT_SITE_NAME = 'Marcus Store'
let pendingRefresh = null

const siteName = computed(() => sysSettings.value.SITE_NAME?.trim() || DEFAULT_SITE_NAME)
const siteLogoUrl = computed(() => sysSettings.value.SITE_LOGO_URL?.trim() || '')
const siteNameParts = computed(() => {
  const words = siteName.value.split(/\s+/).filter(Boolean)
  if (words.length <= 1) return { primary: words[0] || DEFAULT_SITE_NAME, secondary: '' }
  return { primary: words.slice(0, -1).join(' '), secondary: words.at(-1) }
})

const fetchSettingsFromApi = async () => {
  try {
    const res = await api.get('/public/settings')
    sysSettings.value = res.data?.data ?? res.data ?? {}
    isLoaded.value = true
    // Giải quyết pending refresh nếu có
    if (pendingRefresh && pendingRefresh.resolve) {
      pendingRefresh.resolve()
      pendingRefresh = null
    }
  } catch (error) {
    console.error('Lỗi tải cấu hình hệ thống:', error)
    if (pendingRefresh && pendingRefresh.reject) {
      pendingRefresh.reject(error)
      pendingRefresh = null
    }
  }
}

const onSettingsUpdated = () => {
  // Bắt buộc fetch mới khi nhận event từ admin
  fetchSettingsFromApi()
}

export function useSettings() {
  const fetchSettings = async (force = false) => {
    if (isLoaded.value && !force) return
    await fetchSettingsFromApi()
  }

  // Refresh đợi cho đến khi fetch hoàn tất (hữu ích sau khi admin lưu)
  const refreshAndWait = () => {
    return new Promise((resolve, reject) => {
      isLoaded.value = false
      pendingRefresh = { resolve, reject }
      fetchSettingsFromApi()
    })
  }

  const invalidateCache = () => {
    isLoaded.value = false
    sysSettings.value = {}
    localStorage.setItem('SETTINGS_VERSION', Date.now().toString())
    // Broadcast sang tất cả các tab cùng domain
    window.dispatchEvent(new Event('settings-updated'))
  }

  return { sysSettings, fetchSettings, refreshAndWait, siteName, siteLogoUrl, siteNameParts, invalidateCache }
}

window.addEventListener('settings-updated', onSettingsUpdated)

window.addEventListener('storage', (event) => {
  if (event.key === 'SETTINGS_VERSION') {
    onSettingsUpdated()
  }
})
