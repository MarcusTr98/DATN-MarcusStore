import { ref } from 'vue'
import api from '@/utils/api'

const sysSettings = ref({})
const isLoaded = ref(false)

export function useSettings() {
  const fetchSettings = async () => {
    if (isLoaded.value) return
    try {
      const res = await api.get('/public/settings')
      sysSettings.value = res.data
      isLoaded.value = true
    } catch (error) {
      console.error('Lỗi tải cấu hình hệ thống:', error)
    }
  }

  return { sysSettings, fetchSettings }
}
