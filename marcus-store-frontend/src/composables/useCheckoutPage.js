import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import addressApi from '@/api/addressApi'
import userApi from '@/api/userApi'
import cartApi from '@/api/cartApi'
import ghnApi from '@/api/ghnApi'
import api from '@/utils/api'
import { useCartStore } from '@/stores/cartStore'
import { useFlashSaleStore } from '@/stores/FlashSaleStore'

const CHECKOUT_REQUEST_STORAGE_KEY = 'MARCUS_CHECKOUT_REQUEST'

// Marcus refactor: gom luồng địa chỉ, vận chuyển, voucher và đặt hàng khỏi Checkout.vue.
export function useCheckoutPage() {
  const router = useRouter()
  const cartStore = useCartStore()
  const flashSaleStore = useFlashSaleStore()
  const isProcessing = ref(false)
  // Marcus thêm: DELIVERY mặc định để tương thích luồng checkout hiện tại.
  const fulfillmentMethod = ref('DELIVERY')
  const storeInfo = ref({ ADDRESS: '', HOTLINE: '', WORKING_HOURS: '' })
  const isStorePickup = computed(() => fulfillmentMethod.value === 'STORE_PICKUP')
  const isFeeLoading = ref(false)
  const feeError = ref('')

  // ==== Modal thông báo Flash Sale bị admin hủy ====
  // Khi user vào trang thanh toán mà đơn hàng có chứa SP FS từ slot CANCELLED
  // (admin vừa hủy sau khi user checkout từ giỏ) → chặn + hiện modal.
  const showCancelledModal = ref(false)

  // Danh sách cart item vừa bị revert giá do admin hủy Flash Sale.
  // Dùng để hiện modal thông báo cho khách hiểu lý do giá đổi.
  const priceRevertedItems = ref([])

  // Track cartItemIds đã bị revert để dùng khi user bấm "Xóa khỏi giỏ hàng"
  // (vì sau sync với server, isFlashSale đã bị clear nên không thể filter lại)
  const revertedCartItemIds = ref([])

  // Cờ chặn vòng lặp modal bật liên tục. Sau khi user bấm "Đồng ý" 1 lần,
  // không tự động mở lại modal trong cùng phiên. Reset khi component unmount
  // (user rời trang) để lần sau quay lại modal vẫn hoạt động nếu vẫn còn lỗi.
  const hasHandledCancelled = ref(false)

  function openCancelledModal() {
    if (hasHandledCancelled.value) return
    showCancelledModal.value = true
  }

  // Ghi nhớ CHÍNH XÁC skuId của các SP gây lỗi Flash Sale, tại đúng thời điểm phát hiện lỗi
  // (lúc mount, lúc watch clientSlots, hoặc lúc backend trả 409 khi bấm Đặt hàng).
  //
  // Lý do cần biến này: KHÔNG được dựa vào flashSaleStore.isSlotCancelled() tại thời điểm
  // purge, vì flashSaleStore.clientSlots (lấy qua fetchClientSlots) rất có thể CHỈ chứa
  // các slot đang active/sắp diễn ra — slot đã bị admin hủy có thể không còn nằm trong
  // danh sách này nữa. Khi đó isSlotCancelled(slotId) trả về false cho 1 slot store
  // "chưa từng biết", khiến hàm purge lọc ra danh sách rỗng và KHÔNG gọi API xóa nào cả
  // (đây chính là nguyên nhân giỏ hàng không được xóa dù user đã bấm "Đồng ý").
  const pendingRemovalSkuIds = ref([])

  // Xóa hẳn các SP đã ghi nhận trong pendingRemovalSkuIds khỏi GIỎ HÀNG TRÊN SERVER.
  // Bắt buộc phải gọi API xóa thật (không chỉ xóa localStorage), vì cart_item trong DB
  // vẫn giữ tham chiếu flashSaleSlotId cũ -> backend /checkout sẽ luôn từ chối
  // (409 FLASH_SALE_CANCELLED) dù giá hiển thị trên UI đã về giá gốc.
  async function purgeCancelledFlashSaleItemsFromCart() {
    let skuIds = pendingRemovalSkuIds.value.filter(Boolean)

    // Fallback 1: dùng cartData.items (localStorage) - chỉ lọc items Flash Sale.
    if (skuIds.length === 0) {
      const items = Array.isArray(cartData.value.items) ? cartData.value.items : []
      skuIds = items
        .filter((item) => item.isFlashSale)
        .map((item) => item.skuId)
        .filter(Boolean)
    }

    // Fallback 2: dùng revertedCartItemIds (đã được track từ findCancelledFlashSaleItem)
    // Vì sau sync với server, isFlashSale đã bị clear nên cần dùng cartItemId để tìm lại
    // KHÔNG filter isFlashSale vì các SP này đã bị revert rồi (isFlashSale = false)
    if (skuIds.length === 0 && revertedCartItemIds.value.length > 0) {
      const items = Array.isArray(cartData.value.items) ? cartData.value.items : []
      skuIds = items
        .filter((item) => revertedCartItemIds.value.includes(item.cartItemId))
        .map((item) => item.skuId)
        .filter(Boolean)
    }

    // Fallback 3: refetch cart thật từ server để đảm bảo lấy đúng TẤT CẢ SP
    // còn thuộc Flash Sale (theo quan điểm server). Dùng cartStore.fetchCart() để
    // tự sync lại cartStore.items, rồi lọc isFlashSale=true.
    if (skuIds.length === 0) {
      try {
        await cartStore.fetchCart()
        const serverItems = Array.isArray(cartStore.items) ? cartStore.items : []
        skuIds = serverItems
          .filter((item) => item.isFlashSale)
          .map((item) => item.skuId)
          .filter(Boolean)
      } catch (e) {
        console.warn('Không thể refetch cart để lấy SP Flash Sale:', e)
      }
    }

    // Fallback 4: dùng revertedCartItemIds để refetch cart từ server
    // (phòng trường hợp cartData đã bị thay đổi nhưng revertedCartItemIds vẫn còn)
    if (skuIds.length === 0 && revertedCartItemIds.value.length > 0) {
      try {
        await cartStore.fetchCart()
        const serverItems = Array.isArray(cartStore.items) ? cartStore.items : []
        skuIds = serverItems
          .filter((item) => revertedCartItemIds.value.includes(item.cartItemId))
          .map((item) => item.skuId)
          .filter(Boolean)
      } catch (e) {
        console.warn('Không thể refetch cart để lấy SP theo cartItemId:', e)
      }
    }

    if (skuIds.length === 0) return

    try {
      await cartStore.removeManyItemFromCart(skuIds)
      pendingRemovalSkuIds.value = []
      revertedCartItemIds.value = []
    } catch (e) {
      console.warn('Không thể xóa SP Flash Sale đã hủy khỏi giỏ hàng server:', e)
    }
  }

  async function handleCancelledConfirm() {
    if (hasHandledCancelled.value) return
    hasHandledCancelled.value = true

    showCancelledModal.value = false

    // Khách chọn "Tiếp tục với giá gốc" → KHÔNG xóa SP, chỉ ẩn modal.
    // Giá đã được sync về giá gốc từ server, khách có thể tiếp tục checkout bình thường.

    // Marcus sửa: không xóa phạm vi sản phẩm đã chọn. findCancelledFlashSaleItem
    // đã đồng bộ giá server; giữ snapshot mới giúp F5 không kéo cả giỏ vào Checkout.
    persistCheckoutSelection()
    localStorage.removeItem('selectedVoucher')
  }

  // Đóng modal (khi user bấm X hoặc click ngoài overlay) — không thực hiện hành động gì.
  function handleCancelledClose() {
    showCancelledModal.value = false
  }

  // Khách chọn "Xóa khỏi giỏ hàng" → xóa thật SP FS khỏi cart server.
  async function handleCancelledRemove() {
    if (hasHandledCancelled.value) return
    hasHandledCancelled.value = true

    showCancelledModal.value = false

    // 1) Xóa thật SP khỏi giỏ hàng server trước — đây là bước quan trọng nhất,
    //    nếu bỏ qua thì lần sau user mua lại vẫn dính lỗi 409 y hệt.
    await purgeCancelledFlashSaleItemsFromCart()

    // 2) Dọn snapshot cục bộ để tránh đọc lại dữ liệu cũ ở lần vào Checkout kế tiếp.
    localStorage.removeItem('selectedCartItems')
    localStorage.removeItem('selectedCartItemIds')
    localStorage.removeItem('selectedVoucher')

    // 3) Điều hướng về trang chủ thay vì reload toàn trang.
    await router.replace({ path: '/' }).catch(() => {
      window.location.href = '/'
    })
  }

  // Kiểm tra có SP Flash Sale thuộc slot đã bị admin hủy không.
  //
  // Flow mới (đã sửa lỗi "giá đã về gốc nhưng vẫn bị chặn mua"):
  //   1. Refetch cart thật từ server TRƯỚC (không dựa vào localStorage).
  //   2. Nếu server đã KHÔNG còn đánh dấu isFlashSale (giá đã revert về gốc) →
  //      cho phép checkout tiếp tục, đồng thời sync lại cartData cục bộ.
  //   3. CHỈ chặn khi server vẫn xác nhận isFlashSale=true với slot đã bị hủy.
  //
  // Lý do: localStorage là snapshot tĩnh lưu lúc user rời Cart. Nếu admin hủy FS,
  // server revert giá về gốc nhưng localStorage vẫn còn isFlashSale=true. Logic cũ
  // dựa vào localStorage để chặn ngay → user kẹt vĩnh viễn không mua được.
  async function findCancelledFlashSaleItem() {
    let response
    try {
      response = await cartApi.getCart()
    } catch (e) {
      console.warn('Không thể refetch cart để kiểm tra Flash Sale:', e)
      // Lỗi mạng → fallback dùng localStorage (giữ hành vi cũ an toàn)
      return findCancelledFromLocal()
    }
    // Marcus sửa: chỉ khởi tạo dữ liệu sau khi request thành công, tránh state
    // trung gian thừa và giữ rõ nhánh fallback khi Cart API gián đoạn.
    const freshData = response.data?.data ?? response.data
    const serverItems = Array.isArray(freshData?.items) ? freshData.items : []

    // Sync cartData cục bộ với server (đảm bảo UI đúng giá)
    const revertedItems = [] // Lưu các cartItemId vừa bị revert giá (để hiện cảnh báo)
    // Marcus thêm: đảm bảo luồng Buy Now không bị server sync ghi đè quantity.
    // Nếu SP đã có sẵn trong cart, server có thể cộng dồn quantity khi addToCart,
    // và findCancelledFlashSaleItem sẽ vô tình ghi đè quantity bằng giá trị lớn hơn.
    const buyNowQuantities = readBuyNowCartItemQuantities()
    if (Array.isArray(cartData.value.items)) {
      for (const fresh of serverItems) {
        const idx = cartData.value.items.findIndex((i) => i.cartItemId === fresh.cartItemId)
        if (idx !== -1) {
          const oldItem = cartData.value.items[idx]
          // Phát hiện SP Flash Sale bị admin hủy → giá vừa revert từ giá FS về giá gốc
          const wasFlashSale = oldItem.isFlashSale === true
          const isNowRegular = fresh.isFlashSale !== true
          const priceChanged = (oldItem.price || 0) !== (fresh.price || 0)
          if (wasFlashSale && isNowRegular && priceChanged) {
            revertedItems.push({
              cartItemId: fresh.cartItemId,
              productName: fresh.productName || oldItem.productName,
              variantName: fresh.variantName || oldItem.variantName,
              oldPrice: oldItem.price,
              newPrice: fresh.price,
              slotName: oldItem.flashSaleSlotName || 'Flash Sale',
            })
            // Lưu cartItemId để handleCancelledRemove có thể xóa được SP
            // (vì sau khi sync, isFlashSale đã bị clear)
            if (!revertedCartItemIds.value.includes(fresh.cartItemId)) {
              revertedCartItemIds.value.push(fresh.cartItemId)
            }
          }

          // Cập nhật giá + trạng thái Flash Sale theo server
          cartData.value.items[idx] = {
            ...oldItem,
            // Marcus sửa: snapshot cũ từ Cart có thể thiếu skuId. Luôn bù lại
            // toàn bộ định danh và nội dung authoritative từ cart server.
            cartItemId: fresh.cartItemId ?? oldItem.cartItemId,
            skuId: fresh.skuId ?? oldItem.skuId,
            skuCode: fresh.skuCode ?? oldItem.skuCode,
            productName: fresh.productName ?? oldItem.productName,
            variantName: fresh.variantName ?? fresh.variantText ?? oldItem.variantName,
            thumbnailUrl: fresh.thumbnailUrl ?? oldItem.thumbnailUrl,
            // Marcus sửa: ưu tiên quantity Buy Now đã được user chốt (khi SP đã có
            // sẵn trong cart, server có thể cộng dồn quantity). Nếu không phải luồng
            // Buy Now → lấy quantity từ server.
            quantity:
              Number(buyNowQuantities[fresh.cartItemId]) ||
              Number(fresh.quantity ?? oldItem.quantity ?? 1),
            price: fresh.price ?? oldItem.price,
            // Marcus thêm: totalPrice phải đồng bộ với quantity Buy Now
            // (nếu có), không phụ thuộc vào server totalPrice đã bị cộng dồn.
            totalPrice: (() => {
              const useQty =
                Number(buyNowQuantities[fresh.cartItemId]) ||
                Number(fresh.quantity ?? oldItem.quantity ?? 1)
              const usePrice = Number(fresh.price ?? oldItem.price ?? 0)
              return Number((usePrice * useQty).toFixed(2))
            })(),
            originalPrice: fresh.originalPrice ?? oldItem.originalPrice,
            isFlashSale: fresh.isFlashSale === true,
            flashSaleSlotId: fresh.flashSaleSlotId ?? null,
            flashSaleSlotName: fresh.flashSaleSlotName ?? null,
            // Đánh dấu nếu vừa bị revert (dùng để hiện badge cảnh báo)
            priceReverted: wasFlashSale && isNowRegular && priceChanged,
            priceRevertedInfo:
              wasFlashSale && isNowRegular && priceChanged
                ? {
                    oldPrice: oldItem.price,
                    newPrice: fresh.price,
                    slotName: oldItem.flashSaleSlotName || 'Flash Sale',
                  }
                : null,
          }
        }
      }

      // Cập nhật tổng tiền + ghi đè localStorage
      cartData.value.totalAmount = cartData.value.items.reduce(
        (sum, i) => sum + (i.totalPrice || 0),
        0,
      )
      persistCheckoutSelection()
    }

    // Emit sự kiện revert để UI hiện modal thông báo
    if (revertedItems.length > 0) {
      priceRevertedItems.value = revertedItems
      // Marcus sửa: giá đổi phải kéo theo shipping/voucher mới; không giữ kết
      // quả được tính khi cartTotal còn bằng 0 hoặc đang ở giá Flash Sale cũ.
      await refreshPricingAfterCartChange()
      // Hiện modal cancelled để thông báo cho user biết giá đã revert
      if (!showCancelledModal.value) {
        openCancelledModal()
      }
    }

    // Tìm SP còn là Flash Sale trên server mà slot đã bị hủy
    const stillInvalid = serverItems.find(
      (item) =>
        item.isFlashSale === true &&
        item.flashSaleSlotId &&
        flashSaleStore.isSlotCancelled(item.flashSaleSlotId),
    )

    if (stillInvalid?.skuId) {
      pendingRemovalSkuIds.value = [stillInvalid.skuId]
    }

    return stillInvalid || null
  }

  // Fallback cũ: kiểm tra dựa trên localStorage khi không gọi được API
  function findCancelledFromLocal() {
    const items = Array.isArray(cartData.value.items) ? cartData.value.items : []
    const slots = flashSaleStore.clientSlots
    if (!Array.isArray(slots)) return null

    const staleFsItems = items.filter(
      (item) =>
        item.isFlashSale &&
        item.flashSaleSlotId &&
        flashSaleStore.isSlotCancelled(item.flashSaleSlotId),
    )

    if (staleFsItems.length === 0) return null

    if (staleFsItems[0]?.skuId) {
      pendingRemovalSkuIds.value = [staleFsItems[0].skuId]
    }
    return staleFsItems[0]
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

  function readSelectedCartItemIds() {
    try {
      const storedIds = JSON.parse(localStorage.getItem('selectedCartItemIds') || '[]')
      if (Array.isArray(storedIds) && storedIds.length) {
        return storedIds.map(Number).filter(Number.isInteger)
      }
    } catch {
      localStorage.removeItem('selectedCartItemIds')
    }

    // Marcus thêm: tương thích luồng Buy Now từ ProductDetail — fallback đọc sessionStorage
    try {
      const buyNowIds = sessionStorage.getItem('buyNowCartItemIds')
      if (buyNowIds) {
        const ids = JSON.parse(buyNowIds)
        if (Array.isArray(ids) && ids.length) {
          const cleaned = ids.map(Number).filter(Number.isInteger)
          if (cleaned.length) {
            // Promote sang localStorage để lần F5/re-mount vẫn giữ được lựa chọn
            localStorage.setItem('selectedCartItemIds', JSON.stringify(cleaned))
            return cleaned
          }
        }
      }
    } catch {
      sessionStorage.removeItem('buyNowCartItemIds')
    }

    // Marcus tương thích snapshot cũ trước khi Cart bổ sung selectedCartItemIds.
    return (cartData.value.items || [])
      .map((item) => Number(item.cartItemId))
      .filter(Number.isInteger)
  }

  // Marcus thêm: đọc snapshot quantity của Buy Now (đã chốt tại thời điểm user bấm
  // "Mua ngay"). Nếu SP đã có sẵn trong giỏ, backend có thể cộng dồn quantity
  // khi addToCart, khiến cartItem.quantity lớn hơn số user thật sự chọn mua.
  // Áp dụng snapshot này sẽ tránh lấy nhầm số lượng lớn hơn về Checkout.
  function readBuyNowCartItemQuantities() {
    try {
      const raw = sessionStorage.getItem('buyNowCartItemQuantities')
      if (!raw) return {}
      const parsed = JSON.parse(raw)
      if (!parsed || typeof parsed !== 'object') return {}
      const cleaned = {}
      for (const [key, value] of Object.entries(parsed)) {
        const cartItemId = Number(key)
        const quantity = Number(value)
        if (Number.isInteger(cartItemId) && Number.isInteger(quantity) && quantity > 0) {
          cleaned[cartItemId] = quantity
        }
      }
      return cleaned
    } catch {
      sessionStorage.removeItem('buyNowCartItemQuantities')
      return {}
    }
  }

  function persistCheckoutSelection() {
    const items = Array.isArray(cartData.value.items) ? cartData.value.items : []
    const ids = items.map((item) => Number(item.cartItemId)).filter(Number.isInteger)
    localStorage.setItem('selectedCartItems', JSON.stringify(items))
    localStorage.setItem('selectedCartItemIds', JSON.stringify(ids))
  }

  function normalizeServerCartItem(item) {
    const quantity = Number(item.quantity || 1)
    const price = Number(item.price || 0)
    return {
      ...item,
      cartItemId: Number(item.cartItemId),
      skuId: item.skuId == null ? null : Number(item.skuId),
      productName: item.productName || item.name || '',
      variantName:
        item.variantName ||
        item.variantText ||
        [item.color, item.storage].filter(Boolean).join(' / '),
      quantity,
      price,
      totalPrice: Number(item.totalPrice || price * quantity),
      isFlashSale: item.isFlashSale === true,
    }
  }

  function reconcileSelectedItems(fetchedCart) {
    const serverItems = Array.isArray(fetchedCart?.items) ? fetchedCart.items : []
    const selectedIds = new Set(readSelectedCartItemIds())

    // Marcus sửa: thiếu snapshot không được tự hiểu là khách chọn toàn bộ giỏ.
    if (selectedIds.size === 0) {
      cartData.value = { items: [], totalQuantity: 0, totalAmount: 0 }
      return
    }

    // Marcus thêm: snapshot quantity Buy Now. Áp dụng SAU khi filter theo
    // selectedIds để không override các cartItem khác (Cart bình thường).
    const buyNowQuantities = readBuyNowCartItemQuantities()

    const selectedItems = serverItems
      .filter((item) => selectedIds.has(Number(item.cartItemId)))
      .map((item) => {
        const normalized = normalizeServerCartItem(item)
        //Đức sửa
        // Nếu cartItem này thuộc luồng Buy Now và có snapshot quantity riêng
        // → override quantity + totalPrice theo snapshot. Tránh lấy nhầm
        // số lượng cộng dồn từ cart có sẵn.
        const overrideQty = buyNowQuantities[normalized.cartItemId]
        if (Number.isInteger(overrideQty) && overrideQty > 0) {
          const quantity = overrideQty
          const price = normalized.price
          return {
            ...normalized,
            quantity,
            totalPrice: Number((price * quantity).toFixed(2)),
          }
        }
        return normalized
      })

    cartData.value = {
      items: selectedItems,
      totalQuantity: selectedItems.reduce((sum, item) => sum + item.quantity, 0),
      totalAmount: selectedItems.reduce((sum, item) => sum + item.totalPrice, 0),
    }
    persistCheckoutSelection()
  }

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
    const finalShippingFee =
      isStorePickup.value || hasFreeshipVoucher.value
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

  async function refreshPricingAfterCartChange() {
    const tasks = []
    if (!isStorePickup.value && isAddressReady.value) {
      tasks.push(calculateShippingFee())
    }
    if (appliedVoucherCode.value) {
      tasks.push(previewVoucher())
    }
    await Promise.allSettled(tasks)
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

      // Marcus sửa: luôn lấy giá/SKU mới nhất từ server nhưng chỉ giữ đúng tập
      // cartItemId khách đã chọn, kể cả sau F5 hoặc Flash Sale vừa revert giá.
      reconcileSelectedItems(fetchedCart)
      if (!cartData.value.items.length) {
        showModal(
          'Chưa chọn sản phẩm',
          'Không tìm thấy sản phẩm đã chọn để thanh toán. Vui lòng quay lại giỏ hàng.',
          'redirect_cart',
        )
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

    // Chặn submit nếu đơn hàng có SP Flash Sale thuộc slot đã bị admin hủy VÀ
    // server xác nhận vẫn còn lỗi (đã refetch + đồng bộ lại giá bên trong hàm này).
    if (await findCancelledFlashSaleItem()) {
      openCancelledModal()
      return
    }

    // Marcus thêm: chặn sớm snapshot thiếu định danh thay vì tạo đơn xong mới
    // phát hiện không thể dọn đúng SKU khỏi giỏ.
    const hasInvalidIdentity = cartData.value.items.some(
      (item) => !Number.isInteger(Number(item.cartItemId)) || !Number.isInteger(Number(item.skuId)),
    )
    if (hasInvalidIdentity) {
      showModal(
        'Dữ liệu giỏ hàng chưa đồng bộ',
        'Không xác định được SKU của một sản phẩm. Vui lòng quay lại giỏ hàng và chọn lại sản phẩm.',
        'redirect_cart',
      )
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

    if (!isStorePickup.value && !selectedAddress.value) {
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

    if (!isStorePickup.value && !isAddressReady.value) {
      showModal(
        'Lỗi hệ thống',
        'Chưa nhận diện được mã địa chỉ giao hàng. Vui lòng tải lại trang hoặc chọn lại địa chỉ.',
      )
      return
    }

    if (!isStorePickup.value && (feeError.value || isFeeLoading.value)) {
      showModal(
        'Lỗi phí vận chuyển',
        'Không thể tính phí giao hàng. Vui lòng đợi trong giây lát hoặc chọn địa chỉ khác.',
      )
      return
    }

    const payload = {
      // Marcus thêm: cùng tập cart item trong cùng phiên luôn dùng lại một UUID,
      // kể cả F5 hoặc retry sau timeout.
      checkoutRequestId: getOrCreateCheckoutRequestId(cartData.value.items),
      cartItemIds: cartData.value.items.map((i) => i.cartItemId),
      recipientName: orderForm.value.recipientName,
      recipientPhone: orderForm.value.recipientPhone,
      email: orderForm.value.email,
      paymentMethod: orderForm.value.paymentMethod,
      note: orderForm.value.note,
      fulfillmentMethod: fulfillmentMethod.value,
      shippingAddress: isStorePickup.value ? storeInfo.value.ADDRESS : buildShippingAddress(),
      // ÉP LẤY DỮ LIỆU: Ưu tiên địa chỉ sổ (toDistrictId) => nếu không có thì lấy địa chỉ chọn tay
      toDistrictId: isStorePickup.value ? null : toDistrictId.value || manualDistrictId.value,
      toWardCode: isStorePickup.value ? null : toWardCode.value || manualWardCode.value,
      voucherCode: appliedVoucherCode.value || null,
    }

    isProcessing.value = true
    try {
      const { data } = await api.post('/checkout', payload)

      sessionStorage.removeItem(CHECKOUT_REQUEST_STORAGE_KEY)

      const paidSkuIds = cartData.value.items
        .map((item) => Number(item.skuId))
        .filter(Number.isInteger)
      await cartStore.removeManyItemFromCart(paidSkuIds)

      localStorage.removeItem('selectedCartItems')
      localStorage.removeItem('selectedCartItemIds')
      localStorage.removeItem('selectedSubtotal')
      localStorage.removeItem('selectedVoucher')
      // Marcus thêm: dọn flag Buy Now để lần checkout sau không bị kẹt với cartItemId cũ
      sessionStorage.removeItem('buyNowCartItemIds')
      sessionStorage.removeItem('buyNowCartItemQuantities')

      if (orderForm.value.paymentMethod === 'VNPAY' && data?.data?.paymentUrl) {
        window.location.href = data.data.paymentUrl
        return
      }

      const savedOrderCode = data?.data?.orderCode || data?.orderCode || 'Không xác định'
      router.push({ path: '/order-success', query: { orderCode: savedOrderCode } })
    } catch (error) {
      const responseData = error.response?.data

      // Fix: backend không phải lúc nào cũng trả mã lỗi qua field `data`.
      // Với lỗi FLASH_SALE_CANCELLED, `data` là null và mã lỗi thật nằm ở đầu
      // chuỗi `message`, dạng "FLASH_SALE_CANCELLED|Mô tả chi tiết...".
      // Ưu tiên đọc `data`, fallback sang tách từ `message` nếu `data` rỗng.
      const rawMessage = typeof responseData?.message === 'string' ? responseData.message : ''
      const errorCode = responseData?.data || rawMessage.split('|')[0]?.trim()
      const errorMessage = rawMessage.includes('|')
        ? rawMessage.split('|').slice(1).join('|').trim()
        : rawMessage || 'Hệ thống gián đoạn. Vui lòng thử lại.'

      // Nếu backend báo lỗi liên quan đến Flash Sale (slot đã hủy/hết hạn/hết hàng) →
      // hiện modal thông báo. Khi user bấm đóng/xác nhận modal này, hệ thống sẽ TỰ ĐỘNG
      // xóa sản phẩm khỏi giỏ hàng server (xem handleCancelledConfirm) rồi mới redirect,
      // đảm bảo lần mua lại sau đó không còn dính lỗi 409 tương tự.
      if (isFlashSaleCancelledCode(errorCode)) {
        // Fix: ghi nhận NGAY danh sách skuId cần xóa tại đây. Backend không trả về
        // skuId cụ thể trong response (chỉ có tên slot trong message), nên ta lấy
        // tất cả SP đang đánh dấu isFlashSale trong giỏ hiện tại làm danh sách cần xóa —
        // đây chính xác là các SP khiến /checkout bị từ chối.
        pendingRemovalSkuIds.value = cartData.value.items
          .filter((item) => item.isFlashSale)
          .map((item) => item.skuId)
          .filter(Boolean)

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

  function getOrCreateCheckoutRequestId(items) {
    const fingerprint = items
      .map((item) => Number(item.cartItemId))
      .filter(Number.isInteger)
      .sort((left, right) => left - right)
      .join('-')
    try {
      const stored = JSON.parse(sessionStorage.getItem(CHECKOUT_REQUEST_STORAGE_KEY) || 'null')
      if (stored?.fingerprint === fingerprint && stored?.requestId) return stored.requestId
    } catch {
      sessionStorage.removeItem(CHECKOUT_REQUEST_STORAGE_KEY)
    }
    const requestId = crypto.randomUUID()
    sessionStorage.setItem(
      CHECKOUT_REQUEST_STORAGE_KEY,
      JSON.stringify({ fingerprint, requestId }),
    )
    return requestId
  }

  onMounted(async () => {
    // Marcus thêm: lấy thông tin cửa hàng từ cấu hình chung, không hard-code trên giao diện.
    api
      .get('/public/settings')
      .then(({ data }) => {
        storeInfo.value = {
          ADDRESS: data?.ADDRESS || 'Marcus Store',
          HOTLINE: data?.HOTLINE || '',
          WORKING_HOURS: data?.WORKING_HOURS || '',
        }
      })
      .catch(() => {})
    await prefillUserEmail()
    // Marcus sửa thứ tự khởi tạo: phải có giỏ authoritative trước khi địa chỉ
    // gọi tính phí GHN; tránh cartTotal=0 rồi giữ cảnh báo tối thiểu bị stale.
    await Promise.allSettled([fetchGhnProvinces(), flashSaleStore.fetchClientSlots(20)])
    await fetchCart()
    await fetchMyAddresses()
    // Preview voucher nếu có voucher được chọn từ cart
    if (appliedVoucherCode.value) {
      await previewVoucher()
    }

    // Lắng nghe WebSocket event CANCELLED/EXPIRED từ FlashSaleStore
    window.addEventListener('fs-event', handleFsEvent)

    // Nếu đơn hàng hiện tại có SP Flash Sale thuộc slot bị admin hủy → hiện modal ngay.
    // Guard: bỏ qua nếu user đã xử lý trong phiên này rồi (tránh vòng lặp modal).
    // findCancelledFlashSaleItem() giờ tự refetch + đồng bộ giá thật với server trước
    // khi kết luận, nên sẽ không còn báo lỗi nhầm khi giá đã revert về gốc.
    if (await findCancelledFlashSaleItem()) {
      openCancelledModal()
    }
  })

  onBeforeUnmount(() => {
    // Reset cờ khi rời trang để lần sau vào lại modal vẫn hoạt động nếu lỗi còn.
    hasHandledCancelled.value = false
    pendingRemovalSkuIds.value = []
    revertedCartItemIds.value = []
    // Marcus thêm: dọn flag Buy Now khi user rời Checkout (back/điều hướng sang trang khác
    // mà không đặt hàng). Nếu không dọn, sessionStorage sẽ kẹt cartItemIds cũ và lần sau
    // user mở Cart bấm Đặt hàng → reconcileSelectedItems filter sai nhóm sản phẩm.
    sessionStorage.removeItem('buyNowCartItemIds')
    sessionStorage.removeItem('buyNowCartItemQuantities')
    // Cleanup event listener
    window.removeEventListener('fs-event', handleFsEvent)
  })

  // ==== WebSocket event handler cho Flash Sale CANCELLED/EXPIRED ====
  async function handleFsEvent(event) {
    const fsEvent = event.detail
    // Chỉ xử lý event CANCELLED hoặc EXPIRED
    if (fsEvent?.type !== 'CANCELLED' && fsEvent?.type !== 'EXPIRED') return
    // Bỏ qua nếu đã xử lý trong phiên này
    if (hasHandledCancelled.value) return

    // Refresh clientSlots để đảm bảo có trạng thái mới nhất từ server
    await flashSaleStore.fetchClientSlots(20)

    // Kiểm tra xem có SP nào trong cart thuộc slot bị ảnh hưởng
    const affectedItem = cartData.value.items?.find(
      (item) =>
        item.isFlashSale &&
        item.flashSaleSlotId &&
        flashSaleStore.isSlotCancelled(item.flashSaleSlotId),
    )
    if (affectedItem) {
      openCancelledModal()
    }
  }

  // Theo dõi khi clientSlots thay đổi (vd: refresh, scheduler reload) để phát hiện
  // slot vừa bị admin hủy trong khi user đang ở trang checkout.
  watch(
    () => flashSaleStore.clientSlots,
    async () => {
      // Chỉ mở modal khi: chưa xử lý + chưa mở + server xác nhận vẫn còn item lỗi thật
      if (hasHandledCancelled.value || showCancelledModal.value) return
      if (await findCancelledFlashSaleItem()) {
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

  watch(fulfillmentMethod, (method) => {
    // Marcus thêm: nhận tại quầy không giữ lại phí GHN của địa chỉ đã chọn trước đó.
    if (method === 'STORE_PICKUP') resetShippingState()
    else if (isAddressReady.value) calculateShippingFee()
  })

  // Marcus refactor: chỉ public state và action được template Checkout sử dụng.
  return {
    modal,
    handleModalConfirm,
    showCancelledModal,
    handleCancelledClose,
    handleCancelledConfirm,
    handleCancelledRemove,
    // Danh sách cart item bị revert giá
    priceRevertedItems,
    fulfillmentMethod,
    isStorePickup,
    storeInfo,
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
