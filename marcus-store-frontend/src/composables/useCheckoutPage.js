import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import addressApi from '@/api/addressApi'
import userApi from '@/api/userApi'
import cartApi from '@/api/cartApi'
import ghnApi from '@/api/ghnApi'
import api from '@/utils/api'
import { useCartStore } from '@/stores/cartStore'
import { useFlashSaleStore } from '@/stores/FlashSaleStore'

// Marcus refactor: gom luồng địa chỉ, vận chuyển, voucher và đặt hàng khỏi Checkout.vue.
export function useCheckoutPage() {
  const router = useRouter()
  const cartStore = useCartStore()
  const flashSaleStore = useFlashSaleStore()
  const isProcessing = ref(false)
  const isFeeLoading = ref(false)
  const feeError = ref('')

  // ==== Modal thông báo Flash Sale bị admin hủy ====
  // Khi user vào trang thanh toán mà đơn hàng có chứa SP FS từ slot CANCELLED
  // (admin vừa hủy sau khi user checkout từ giỏ) → chặn + hiện modal.
  const showCancelledModal = ref(false)

  // Cờ chặn vòng lặp modal bật liên tục. Sau khi user bấm "Đồng ý" 1 lần,
  // không tự động mở lại modal trong cùng phiên. Reset khi component unmount
  // (user rời trang) để lần sau quay lại modal vẫn hoạt động nếu vẫn còn lỗi.
  const hasHandledCancelled = ref(false)

  function openCancelledModal() {
    if (hasHandledCancelled.value) return
    showCancelledModal.value = true
  }

  async function handleCancelledConfirm() {
    if (hasHandledCancelled.value) return
    hasHandledCancelled.value = true

    showCancelledModal.value = false
    // Điều hướng về trang chủ thay vì reload toàn trang.
    // Lý do: reload sẽ trigger lại onMounted → watch(clientSlots) → lại mở modal nếu
    // cartData trong localStorage vẫn chứa SP CANCELLED. router.replace('/') thì an toàn.
    await router.replace({ path: '/' }).catch(() => {
      window.location.href = '/'
    })
  }

  // Kiểm tra trong cartData có SP Flash Sale thuộc slot đã bị admin hủy không.
  // cartData lấy từ localStorage 'selectedCartItems' (đã có isFlashSale + flashSaleSlotId).
  function findCancelledFlashSaleItem() {
    const items = Array.isArray(cartData.value.items) ? cartData.value.items : []
    const slots = flashSaleStore.clientSlots
    if (!Array.isArray(slots)) return null
    return (
      items.find(
        (item) =>
          item.isFlashSale &&
          item.flashSaleSlotId &&
          flashSaleStore.isSlotCancelled(item.flashSaleSlotId),
      ) || null
    )
  }

  const modal = ref({ show: false, title: 'Thông báo', message: '', action: null })

  const showModal = (title, message, action = null) => {
    modal.value = { show: true, title, message, action }
  }

  const handleModalConfirm = () => {
    modal.value.show = false
    if (modal.value.action === 'redirect_cart') router.push('/cart')
    if (modal.value.action === 'redirect_login') router.push('/auth/login')
    modal.value.action = null
  }

  // ─── Order & Cart Data (Kết hợp LocalStorage từ HEAD)
  const getInitialCartData = () => {
    const savedItems = localStorage.getItem('selectedCartItems')
    if (savedItems) {
      try {
        const items = JSON.parse(savedItems)
        if (Array.isArray(items) && items.length > 0) {
          // Đảm bảo mỗi item có price/totalPrice hợp lệ — nếu không thì TÍNH LẠI.
          // Trường hợp totalPrice = 0 (do bug "Mua ngay" từ Flash Sale) → fallback về price * quantity.
          const safeItems = items.map((i) => {
            const safePrice = (i.price || 0) > 0 ? i.price : 0
            const computedTotal =
              (i.totalPrice || 0) > 0 ? i.totalPrice : safePrice * (i.quantity || 1)
            return {
              ...i,
              price: safePrice,
              totalPrice: computedTotal,
            }
          })
          return {
            items: safeItems,
            totalQuantity: safeItems.reduce((sum, i) => sum + (i.quantity || 0), 0),
            totalAmount: safeItems.reduce((sum, i) => sum + (i.totalPrice || 0), 0),
          }
        }
      } catch (e) {
        console.warn('Lỗi parse selectedCartItems:', e)
      }
    }
    return { items: [], totalQuantity: 0, totalAmount: 0 }
  }

  const cartData = ref(getInitialCartData())

  // Marcus sửa: dữ liệu localStorage hỏng không được làm trang Checkout bị trắng.
  const getSavedVoucher = () => {
    try {
      return JSON.parse(localStorage.getItem('selectedVoucher') || 'null')
    } catch {
      localStorage.removeItem('selectedVoucher')
      return null
    }
  }

  const savedVoucher = getSavedVoucher()
  const appliedVoucherCode = ref(savedVoucher?.code || '')

  // Tích hợp Shipping Data mới theo Backend (Cơ chế Upsell/Freeship của Marcus)
  const getDefaultShippingData = () => ({
    standardShippingFee: 0,
    discountedShippingFee: 0,
    isFreeship: false,
    isAllowedToOrder: true,
    blockMessage: '',
    amountUntilFreeship: 0,
    suggestionMessage: '',
  })

  const estimatedDelivery = ref('')
  const shippingData = ref(getDefaultShippingData())

  // Reset toàn bộ state phí ship + ngày giao dự kiến (dùng khi đổi địa chỉ/tỉnh/huyện/xã)
  const resetShippingState = () => {
    shippingData.value = getDefaultShippingData()
    estimatedDelivery.value = ''
  }

  const discountAmount = ref(0)
  const hasFreeshipVoucher = ref(false)
  const voucherError = ref('')
  const isVoucherLoading = ref(false)

  // ─── Modal chọn lại voucher (sau khi backend re-validate fail)
  //      Hiển thị danh sách voucher khả dụng để user chọn cái khác thay thế.
  const isReSelectVoucherModalOpen = ref(false)

  const reSelectVoucherMessage = ref('')
  const availableVouchers = ref([])
  const isAvailableVouchersLoading = ref(false)
  const v2SelectedId = ref(null)

  // ─── Modal thông báo voucher không khả dụng (Modal 1 - bước 1)
  //      Hiện khi backend re-validate fail khi user bấm Đặt hàng.
  //      Bấm "Đồng ý" mới mở Modal 2 (chọn lại voucher).
  const isVoucherInvalidModalOpen = ref(false)
  const voucherInvalidMessage = ref('')

  const openVoucherInvalidModal = (message) => {
    voucherInvalidMessage.value =
      message ||
      'Voucher hiện không còn khả dụng. Vui lòng chọn voucher khác để tiếp tục thanh toán.'
    isVoucherInvalidModalOpen.value = true
  }

  const closeVoucherInvalidModal = () => {
    isVoucherInvalidModalOpen.value = false
    voucherInvalidMessage.value = ''
  }

  const handleVoucherInvalidConfirm = () => {
    const message = voucherInvalidMessage.value
    isVoucherInvalidModalOpen.value = false
    voucherInvalidMessage.value = ''
    openReSelectVoucherModal(message)
  }

  const VOUCHER_RE_SELECT_CODES = new Set([
    'VOUCHER_INACTIVE',
    'VOUCHER_EXPIRED',
    'VOUCHER_QUOTA_EXHAUSTED',
    'VOUCHER_NOT_FOUND',
  ])

  const isVoucherReSelectCode = (code) => code && VOUCHER_RE_SELECT_CODES.has(code)

  // Mã lỗi từ CheckoutService khi Flash Sale bị admin hủy/hết hạn/hết hàng.
  // Khi nhận các mã này → hiện modal "Flash Sale bị admin hủy" thay vì toast lỗi.
  // Định dạng mã: "FLASH_SALE_CANCELLED|message..." → split('|')[0] để lấy code.
  const FLASH_SALE_CANCELLED_CODES = new Set([
    'FLASH_SALE_CANCELLED',
    'FLASH_SALE_ENDED',
    'FLASH_SALE_NOT_STARTED',
    'FLASH_SALE_UNAVAILABLE',
    'FLASH_SALE_INVALID',
    'FLASH_SALE_NOT_FOUND',
    'FLASH_SALE_OUT_OF_STOCK',
  ])

  function isFlashSaleCancelledCode(errorCode) {
    if (!errorCode) return false
    // errorCode có thể là "FLASH_SALE_CANCELLED|message..." → tách lấy phần đầu
    const codeOnly = String(errorCode).split('|')[0].trim()
    return FLASH_SALE_CANCELLED_CODES.has(codeOnly)
  }

  // Modal thông báo áp dụng voucher thành công
  // ─── Re-select voucher modal (mở khi backend re-validate fail) ───
  const fetchAvailableVouchers = async () => {
    isAvailableVouchersLoading.value = true
    try {
      const res = await api.get('/client/vouchers/available')
      availableVouchers.value = deduplicateVouchers(res.data?.data ?? res.data ?? [])
    } catch (e) {
      console.error('Lỗi tải danh sách voucher khả dụng:', e)
      availableVouchers.value = []
    } finally {
      isAvailableVouchersLoading.value = false
    }
  }

  const deduplicateVouchers = (vouchers) => {
    const uniqueVouchers = new Map()
    for (const voucher of Array.isArray(vouchers) ? vouchers : []) {
      const key = voucher.voucherId ?? voucher.voucherCode?.trim().toUpperCase()
      if (key != null && !uniqueVouchers.has(key)) uniqueVouchers.set(key, voucher)
    }
    return [...uniqueVouchers.values()]
  }

  const openReSelectVoucherModal = (message) => {
    reSelectVoucherMessage.value =
      message || 'Voucher bạn chọn không còn khả dụng. Vui lòng chọn voucher khác.'
    v2SelectedId.value = null
    isReSelectVoucherModalOpen.value = true
    fetchAvailableVouchers()
  }

  const closeReSelectVoucherModal = () => {
    isReSelectVoucherModalOpen.value = false
    reSelectVoucherMessage.value = ''
    v2SelectedId.value = null
  }

  const clearAppliedVoucher = () => {
    appliedVoucherCode.value = ''
    discountAmount.value = 0
    hasFreeshipVoucher.value = false
    voucherError.value = ''
    localStorage.removeItem('selectedVoucher')
  }

  // ─── Modal chọn voucher từ checkout
  const isCheckoutVoucherModalOpen = ref(false)

  const openVoucherModal = async () => {
    await fetchAvailableVouchers()
    // Pre-select current voucher nếu có
    if (appliedVoucherCode.value) {
      const current = availableVouchers.value.find(
        (v) => v.voucherCode?.toUpperCase() === appliedVoucherCode.value.toUpperCase(),
      )
      if (current) {
        v2SelectedId.value = current.voucherId
      }
    }
    isCheckoutVoucherModalOpen.value = true
  }

  const closeCheckoutVoucherModal = () => {
    isCheckoutVoucherModalOpen.value = false
    v2SelectedId.value = null
  }

  // Lưu voucher được chọn vào state + localStorage (dùng chung cho cả 2 modal voucher)
  const persistSelectedVoucher = (picked) => {
    appliedVoucherCode.value = picked.voucherCode
    localStorage.setItem(
      'selectedVoucher',
      JSON.stringify({ code: picked.voucherCode, type: picked.discountType }),
    )
  }

  // Handle voucher confirmation từ VoucherModal (re-select)
  const handleVoucherModalConfirm = (picked) => {
    clearAppliedVoucher()
    if (picked) persistSelectedVoucher(picked)
    closeReSelectVoucherModal()
  }

  // Handle voucher confirmation từ VoucherModal (checkout)
  const handleCheckoutVoucherConfirm = (picked) => {
    clearAppliedVoucher()
    if (picked) {
      persistSelectedVoucher(picked)
      previewVoucher() // Preview voucher ngay sau khi chọn
    }
    closeCheckoutVoucherModal()
  }

  const orderForm = ref({
    recipientName: '',
    recipientPhone: '',
    email: '',
    paymentMethod: 'COD',
    note: '',
  })

  // ─── GHN Data
  const toDistrictId = ref(null)
  const toWardCode = ref('')

  const ghnProvinces = ref([])
  const ghnDistricts = ref([])
  const ghnWards = ref([])
  const manualProvinceId = ref(null)
  const manualDistrictId = ref(null)
  const manualWardCode = ref('')
  const detailAddress = ref('')

  // ─── Saved Addresses
  const savedAddresses = ref([])
  const selectedAddress = ref(null)
  const activeAddressId = ref(null)

  // ─── Computed
  const finalAmount = computed(() => {
    const total = (cartData.value.totalAmount ?? 0) - discountAmount.value
    const finalShippingFee = hasFreeshipVoucher.value
      ? 0
      : shippingData.value.discountedShippingFee || 0
    const finalTotal = total + finalShippingFee
    return finalTotal > 0 ? finalTotal : 0
  })

  const isAddressReady = computed(() => !!toDistrictId.value && !!toWardCode.value)

  // ─── Utils (Validation & Formatting)
  const formatDeliveryDate = (isoDate) => {
    if (!isoDate) return ''
    try {
      const d = new Date(isoDate)
      return `Dự kiến giao ${d.toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'numeric' })}`
    } catch {
      return ''
    }
  }

  const validatePhone = (phone) => /^(03|05|07|08|09)\d{8}$/.test(phone)

  const validateEmail = (email) => {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return re.test(email)
  }

  // ─── Voucher preview
  // Chỉ dùng để hiển thị số tiền giảm trong checkout summary.
  // KHÔNG clear voucher code khi fail — voucher chỉ bị reject khi backend validate lúc "Đặt hàng".
  const previewVoucher = async () => {
    const code = appliedVoucherCode.value
    if (!code) {
      discountAmount.value = 0
      hasFreeshipVoucher.value = false
      voucherError.value = ''
      return
    }

    isVoucherLoading.value = true
    voucherError.value = ''

    try {
      const res = await api.post('/client/vouchers/preview', {
        voucherCode: code,
        orderAmount: cartData.value.totalAmount || 0,
        shippingFee: 0,
      })

      const result = res.data?.data ?? res.data
      if (result?.applied) {
        discountAmount.value = Number(result.discountAmount) || 0
        hasFreeshipVoucher.value = result.discountType === 'FREESHIP'
        voucherError.value = ''
      } else {
        discountAmount.value = 0
        hasFreeshipVoucher.value = false
        voucherError.value = result?.message || ''
      }
    } catch (e) {
      discountAmount.value = 0
      hasFreeshipVoucher.value = false
      voucherError.value = e.response?.data?.message || 'Không thể xác minh voucher'
    } finally {
      isVoucherLoading.value = false
    }
  }

  // ─── 1. Phí vận chuyển GHN & Tính toán Freeship
  const calculateShippingFee = async () => {
    if (!toDistrictId.value || !toWardCode.value) {
      resetShippingState()
      return
    }

    isFeeLoading.value = true
    feeError.value = ''

    try {
      const res = await api.post('/client/shipping/calculate', {
        toDistrictId: toDistrictId.value,
        toWardCode: toWardCode.value,
        totalWeightGram: 500,
        cartTotal: cartData.value.totalAmount,
      })

      if (res.data?.code === 200 && res.data?.data) {
        shippingData.value = res.data.data
        const futureDate = new Date()
        futureDate.setDate(futureDate.getDate() + 3)
        estimatedDelivery.value = formatDeliveryDate(futureDate.toISOString())
      } else {
        throw new Error('Không thể lấy thông tin phí vận chuyển')
      }
    } catch {
      resetShippingState()
      feeError.value = 'Không thể tính phí vận chuyển. Vui lòng kiểm tra địa chỉ hoặc thử lại!'
    } finally {
      isFeeLoading.value = false
    }
  }

  // ─── 2. Sổ địa chỉ
  const fetchMyAddresses = async () => {
    try {
      const res = await addressApi.getMyAddresses()
      savedAddresses.value = (res.data?.data ?? []).slice(0, 4)

      const defaultAddr = savedAddresses.value.find((a) => a.isDefault) ?? savedAddresses.value[0]
      if (defaultAddr) await applySavedAddress(defaultAddr)
    } catch (error) {
      console.error('Lỗi tải sổ địa chỉ:', error)
    }
  }

  const applySavedAddress = async (addr) => {
    activeAddressId.value = addr.addressId
    selectedAddress.value = addr
    orderForm.value.recipientName = addr.recipientName
    orderForm.value.recipientPhone = addr.phoneNumber
    toDistrictId.value = addr.districtId
    toWardCode.value = addr.wardCode
    await calculateShippingFee()
  }

  const clearSelectedAddress = () => {
    selectedAddress.value = null
    activeAddressId.value = null
    toDistrictId.value = null
    toWardCode.value = ''
    resetShippingState()
    manualProvinceId.value = null
    manualDistrictId.value = null
    manualWardCode.value = ''
    detailAddress.value = ''
  }

  // ─── 3. Dropdown GHN
  const fetchGhnProvinces = async () => {
    try {
      const res = await ghnApi.getProvinces()
      ghnProvinces.value = res.data?.data ?? []
    } catch (error) {
      console.error('Lỗi tải Tỉnh GHN:', error)
    }
  }

  const onManualProvinceChange = async () => {
    manualDistrictId.value = null
    manualWardCode.value = ''
    toDistrictId.value = null
    toWardCode.value = ''
    resetShippingState()
    ghnDistricts.value = []
    ghnWards.value = []
    if (!manualProvinceId.value) return

    try {
      const res = await ghnApi.getDistricts(manualProvinceId.value)
      ghnDistricts.value = res.data?.data ?? []
    } catch (error) {
      console.error('Lỗi tải Quận GHN:', error)
    }
  }

  const onManualDistrictChange = async () => {
    manualWardCode.value = ''
    toWardCode.value = ''
    toDistrictId.value = manualDistrictId.value ?? null
    resetShippingState()
    ghnWards.value = []
    if (!manualDistrictId.value) return

    try {
      const res = await ghnApi.getWards(manualDistrictId.value)
      ghnWards.value = res.data?.data ?? []
    } catch (error) {
      console.error('Lỗi tải Phường GHN:', error)
    }
  }

  const onManualWardChange = async () => {
    toWardCode.value = manualWardCode.value
    await calculateShippingFee()
  }

  // ─── 4. API phụ trợ
  const fetchCart = async () => {
    try {
      const res = await cartApi.getCart()
      const data = res.data
      const fetchedCart = data?.data ?? data

      if (!cartData.value.items?.length && fetchedCart.items?.length) {
        cartData.value = fetchedCart
      }
    } catch (error) {
      if (error.response?.status === 401) {
        showModal(
          'Yêu cầu đăng nhập',
          'Vui lòng đăng nhập để tiếp tục thanh toán.',
          'redirect_login',
        )
      }
    }
  }

  const prefillUserEmail = async () => {
    try {
      const res = await userApi.getMyProfile()
      orderForm.value.email = res.data?.data?.email ?? ''
    } catch (error) {
      console.warn('Không thể pre-fill email:', error)
    }
  }

  // ─── 5. Submit Checkout
  const buildShippingAddress = () => {
    if (selectedAddress.value) {
      const a = selectedAddress.value
      return `${a.detailAddress}, ${a.wardName}, ${a.districtName}, ${a.provinceName}`
    }
    const wardName = ghnWards.value.find((w) => w.WardCode === manualWardCode.value)?.WardName ?? ''
    const districtName =
      ghnDistricts.value.find((d) => d.DistrictID === manualDistrictId.value)?.DistrictName ?? ''
    const provinceName =
      ghnProvinces.value.find((p) => p.ProvinceID === manualProvinceId.value)?.ProvinceName ?? ''
    return `${detailAddress.value}, ${wardName}, ${districtName}, ${provinceName}`
  }

  const handleCheckout = async () => {
    // Marcus thêm: khóa ngay từ đầu để tránh tạo hai đơn khi double click/submit liên tiếp.
    if (isProcessing.value || !cartData.value.items?.length) return

    // Chặn submit nếu đơn hàng có SP Flash Sale thuộc slot đã bị admin hủy.
    // Hiện modal — user bấm "Đồng ý" sẽ reload trang, khi đó Cart.vue sẽ tự dọn các SP này.
    if (findCancelledFlashSaleItem()) {
      openCancelledModal()
      return
    }

    if (!validatePhone(orderForm.value.recipientPhone)) {
      showModal(
        'Số điện thoại không hợp lệ',
        'Vui lòng nhập đúng số điện thoại Việt Nam (VD: 0901234567).',
      )
      return
    }

    if (!validateEmail(orderForm.value.email)) {
      showModal(
        'Email không hợp lệ',
        'Vui lòng nhập đúng định dạng email (VD: nguyenvan_a@gmail.com).',
      )
      return
    }

    if (!selectedAddress.value) {
      if (!manualProvinceId.value) {
        showModal('Thiếu địa chỉ', 'Vui lòng chọn <strong>Tỉnh / Thành phố</strong> giao hàng.')
        return
      }
      if (!manualDistrictId.value) {
        showModal('Thiếu địa chỉ', 'Vui lòng chọn <strong>Quận / Huyện</strong> giao hàng.')
        return
      }
      if (!manualWardCode.value) {
        showModal('Thiếu địa chỉ', 'Vui lòng chọn <strong>Phường / Xã</strong> giao hàng.')
        return
      }
      if (!detailAddress.value.trim()) {
        showModal('Thiếu địa chỉ', 'Vui lòng nhập <strong>Số nhà, tên đường</strong>.')
        return
      }
    }

    if (!isAddressReady.value) {
      showModal(
        'Lỗi hệ thống',
        'Chưa nhận diện được mã địa chỉ giao hàng. Vui lòng tải lại trang hoặc chọn lại địa chỉ.',
      )
      return
    }

    if (feeError.value || isFeeLoading.value) {
      showModal(
        'Lỗi phí vận chuyển',
        'Không thể tính phí giao hàng. Vui lòng đợi trong giây lát hoặc chọn địa chỉ khác.',
      )
      return
    }

    const payload = {
      cartItemIds: cartData.value.items.map((i) => i.cartItemId),
      recipientName: orderForm.value.recipientName,
      recipientPhone: orderForm.value.recipientPhone,
      email: orderForm.value.email,
      paymentMethod: orderForm.value.paymentMethod,
      note: orderForm.value.note,
      shippingAddress: buildShippingAddress(),
      // ÉP LẤY DỮ LIỆU: Ưu tiên địa chỉ sổ (toDistrictId) => nếu không có thì lấy địa chỉ chọn tay
      toDistrictId: toDistrictId.value || manualDistrictId.value,
      toWardCode: toWardCode.value || manualWardCode.value,
      voucherCode: appliedVoucherCode.value || null,
    }

    isProcessing.value = true
    try {
      const { data } = await api.post('/checkout', payload)

      const paidSkuIds = cartData.value.items.map((i) => i.skuId)
      await cartStore.removeManyItemFromCart(paidSkuIds)

      localStorage.removeItem('selectedCartItems')
      localStorage.removeItem('selectedSubtotal')
      localStorage.removeItem('selectedVoucher')

      if (orderForm.value.paymentMethod === 'VNPAY' && data?.data?.paymentUrl) {
        window.location.href = data.data.paymentUrl
        return
      }

      const savedOrderCode = data?.data?.orderCode || data?.orderCode || 'Không xác định'
      router.push({ path: '/order-success', query: { orderCode: savedOrderCode } })
    } catch (error) {
      const responseData = error.response?.data
      const errorCode = responseData?.data // ResponseStatusException -> data = errorCode string
      const errorMessage = responseData?.message ?? 'Hệ thống gián đoạn. Vui lòng thử lại.'

      // Nếu backend báo lỗi liên quan đến Flash Sale (slot đã hủy/hết hạn/hết hàng) →
      // mở modal thông báo để user hiểu vấn đề, đồng thời reload để Cart.vue tự dọn SP hỏng.
      // Đây là tuyến phòng thủ cuối cùng phía client — backend đã chặn ngay từ đầu,
      // nhưng FE cần phản hồi thân thiện thay vì hiện toast lỗi khô khan.
      if (isFlashSaleCancelledCode(errorCode)) {
        showCancelledModal.value = true
        return
      }

      // Nếu backend báo voucher không khả dụng -> clear state + mở Modal 1 (thông báo)
      // User bấm "Đồng ý" ở Modal 1 mới mở Modal 2 (chọn voucher khác).
      if (isVoucherReSelectCode(errorCode) && appliedVoucherCode.value) {
        clearAppliedVoucher()
        openVoucherInvalidModal(errorMessage)
        return
      }

      showModal('Lỗi đặt hàng', errorMessage)
    } finally {
      isProcessing.value = false
    }
  }

  onMounted(async () => {
    await prefillUserEmail()
    await Promise.allSettled([
      fetchGhnProvinces(),
      fetchCart(),
      fetchMyAddresses(),
      // Tải slot FS để phát hiện slot CANCELLED trước khi user bấm Đặt hàng.
      flashSaleStore.fetchClientSlots(20),
    ])
    // Preview voucher nếu có voucher được chọn từ cart
    if (appliedVoucherCode.value) {
      await previewVoucher()
    }

    // Nếu đơn hàng hiện tại có SP Flash Sale thuộc slot bị admin hủy → hiện modal ngay.
    // Guard: bỏ qua nếu user đã xử lý trong phiên này rồi (tránh vòng lặp modal).
    if (findCancelledFlashSaleItem()) {
      openCancelledModal()
    }
  })

  onBeforeUnmount(() => {
    // Reset cờ khi rời trang để lần sau vào lại modal vẫn hoạt động nếu lỗi còn.
    hasHandledCancelled.value = false
  })

  // Theo dõi khi clientSlots thay đổi (vd: refresh, scheduler reload) để phát hiện
  // slot vừa bị admin hủy trong khi user đang ở trang checkout.
  watch(
    () => flashSaleStore.clientSlots,
    () => {
      // Chỉ mở modal khi: chưa xử lý + chưa mở + có item CANCELLED trong đơn
      if (!hasHandledCancelled.value && !showCancelledModal.value && findCancelledFlashSaleItem()) {
        openCancelledModal()
      }
    },
    { deep: true },
  )

  // Watch appliedVoucherCode để tự động preview khi thay đổi
  watch(appliedVoucherCode, async (newCode) => {
    if (newCode) {
      await previewVoucher()
    } else {
      discountAmount.value = 0
      hasFreeshipVoucher.value = false
      voucherError.value = ''
    }
  })

  // Marcus refactor: chỉ public state và action được template Checkout sử dụng.
  return {
    modal,
    handleModalConfirm,
    showCancelledModal,
    handleCancelledConfirm,
    isVoucherInvalidModalOpen,
    voucherInvalidMessage,
    closeVoucherInvalidModal,
    handleVoucherInvalidConfirm,
    isReSelectVoucherModalOpen,
    availableVouchers,
    cartData,
    isAvailableVouchersLoading,
    reSelectVoucherMessage,
    closeReSelectVoucherModal,
    handleVoucherModalConfirm,
    isCheckoutVoucherModalOpen,
    v2SelectedId,
    closeCheckoutVoucherModal,
    handleCheckoutVoucherConfirm,
    savedAddresses,
    activeAddressId,
    applySavedAddress,
    orderForm,
    selectedAddress,
    clearSelectedAddress,
    manualProvinceId,
    onManualProvinceChange,
    ghnProvinces,
    manualDistrictId,
    onManualDistrictChange,
    ghnDistricts,
    manualWardCode,
    onManualWardChange,
    ghnWards,
    detailAddress,
    estimatedDelivery,
    isFeeLoading,
    toWardCode,
    hasFreeshipVoucher,
    shippingData,
    appliedVoucherCode,
    voucherError,
    isVoucherLoading,
    discountAmount,
    openVoucherModal,
    finalAmount,
    isProcessing,
    handleCheckout,
  }
}
