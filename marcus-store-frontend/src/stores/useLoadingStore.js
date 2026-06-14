import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLoadingStore = defineStore('loading', () => {
  const activeRequests = ref(0)
  const isLoading = ref(false)

  const show = () => {
    activeRequests.value++
    isLoading.value = true
  }

  const hide = () => {
    if (activeRequests.value > 0) {
      activeRequests.value--
    }
    if (activeRequests.value === 0) {
      setTimeout(() => {
        if (activeRequests.value === 0) isLoading.value = false
      }, 300)
    }
  }

  return { isLoading, show, hide }
})
