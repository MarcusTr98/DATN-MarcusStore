// Module JS nhỏ share state wishlist giữa ProductCard, Wishlist page và Header.
// Dùng `reactive` + computed/ref để Vue THEO DÕI được → UI tự cập nhật.
import { reactive, computed } from 'vue'
import api from '@/utils/api'

const state = reactive({
  productIds: new Set(),
  idsLoaded: false,
  inflight: null, // promise đang chạy để tránh gọi trùng
})

// Computed (reactive) — UI dùng cái này để auto-update
const wishlistCount = computed(() => state.productIds.size)
const isLoaded = computed(() => state.idsLoaded)

const api$ = {
  isWished(id) {
    return state.productIds.has(id)
  },
  totalCount() {
    return state.productIds.size
  },
  isLoaded() {
    return state.idsLoaded
  },
  async fetchIds(force = false) {
    if (state.idsLoaded && !force) return
    if (state.inflight) return state.inflight

    state.inflight = (async () => {
      try {
        const res = await api.get('/user/wishlist/ids')
        const ids = res.data?.data ?? []
        state.productIds.clear()
        ids.forEach((id) => state.productIds.add(id))
        state.idsLoaded = true
      } catch (err) {
        console.error('[wishlist] Lỗi load productIds:', err)
      } finally {
        state.inflight = null
      }
    })()

    return state.inflight
  },
  async toggle(productId) {
    const wasWished = state.productIds.has(productId)
    // Optimistic
    if (wasWished) state.productIds.delete(productId)
    else state.productIds.add(productId)

    try {
      const res = await api.post(`/user/wishlist/toggle/${productId}`)
      const data = res.data?.data
      if (data) {
        if (data.wished) state.productIds.add(productId)
        else state.productIds.delete(productId)
      }
      return { success: true, message: res.data?.message }
    } catch (err) {
      // Rollback
      if (wasWished) state.productIds.add(productId)
      else state.productIds.delete(productId)
      const msg = err.response?.data?.message || 'Có lỗi xảy ra'
      return { success: false, message: msg }
    }
  },
  async remove(productId) {
    try {
      const res = await api.delete(`/user/wishlist/${productId}`)
      state.productIds.delete(productId)
      return { success: true, message: res.data?.message }
    } catch (err) {
      const msg = err.response?.data?.message || 'Xóa thất bại'
      return { success: false, message: msg }
    }
  },
  reset() {
    state.productIds.clear()
    state.idsLoaded = false
  },
}
export default api$
export { wishlistCount, isLoaded, state }
