import { computed, ref } from 'vue'
import api from '@/utils/api'

const sysSettings = ref({})
const isLoaded = ref(false)
const DEFAULT_SITE_NAME = 'Marcus Store'

// Marcus thêm: một nguồn nhận diện thương hiệu dùng chung cho toàn bộ giao diện.
// Component không tự gọi lại API; useSettings dùng cache module-level.
const siteName = computed(() => sysSettings.value.SITE_NAME?.trim() || DEFAULT_SITE_NAME)
const siteLogoUrl = computed(() => sysSettings.value.SITE_LOGO_URL?.trim() || '')
const siteNameParts = computed(() => {
  const words = siteName.value.split(/\s+/).filter(Boolean)
  if (words.length <= 1) return { primary: words[0] || DEFAULT_SITE_NAME, secondary: '' }
  return { primary: words.slice(0, -1).join(' '), secondary: words.at(-1) }
})

export function useSettings() {
  const fetchSettings = async (force = false) => {
    if (isLoaded.value && !force) return
    try {
      const res = await api.get('/public/settings')
      sysSettings.value = res.data?.data ?? res.data ?? {}
      isLoaded.value = true
    } catch (error) {
      console.error('Lỗi tải cấu hình hệ thống:', error)
    }
  }

  return { sysSettings, fetchSettings, siteName, siteLogoUrl, siteNameParts }
}
