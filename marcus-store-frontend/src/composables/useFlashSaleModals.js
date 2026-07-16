import { ref, onBeforeUnmount } from 'vue'


export function useFlashSaleModals({ onCancelledConfirm } = {}) {
  // ==== Modal thông báo Flash Sale bị admin hủy ====
  const showCancelledModal = ref(false)
  const hasHandledCancelled = ref(false)

  // ==== Modal yêu cầu đăng nhập (khi guest nhận 401) ====
  const showLoginRequiredModal = ref(false)
  const loginRequiredTitle = ref('Đăng nhập để tiếp tục')
  const loginRequiredMessage = ref('Vui lòng đăng nhập để mua sản phẩm Flash Sale.')

  function openCancelledModal() {
    if (hasHandledCancelled.value) return
    showCancelledModal.value = true
  }

  function handleCancelledClose() {
    showCancelledModal.value = false
  }

  async function handleCancelledConfirm() {
    if (hasHandledCancelled.value) return
    hasHandledCancelled.value = true
    showCancelledModal.value = false
    if (typeof onCancelledConfirm === 'function') {
      try {
        await onCancelledConfirm()
      } catch (e) {
        console.warn('[useFlashSaleModals] onCancelledConfirm error:', e)
      }
    }
  }

  function handleAuthRequired(event) {
    loginRequiredTitle.value = event.detail?.title || 'Đăng nhập để tiếp tục'
    loginRequiredMessage.value =
      event.detail?.message || 'Vui lòng đăng nhập để mua sản phẩm Flash Sale.'
    showLoginRequiredModal.value = true
  }

  onBeforeUnmount(() => {
    hasHandledCancelled.value = false
    if (typeof window !== 'undefined') {
      window.removeEventListener('auth-required', handleAuthRequired)
    }
  })

  return {
    // Cancelled modal
    showCancelledModal,
    hasHandledCancelled,
    openCancelledModal,
    handleCancelledClose,
    handleCancelledConfirm,
    // Login required modal
    showLoginRequiredModal,
    loginRequiredTitle,
    loginRequiredMessage,
    handleAuthRequired,
  }
}
