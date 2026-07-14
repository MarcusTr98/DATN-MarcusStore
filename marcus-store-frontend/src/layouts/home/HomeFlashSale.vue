<template>
  <section class="home-flash-sale">
    <!-- Header -->
    <header class="hfs-head">
      <div class="hfs-title-wrap">
        <h2 class="hfs-title">
          <span class="bolt" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
          </span>
          <span>FLASH SALE</span>
          <span class="accent">ĐANG CHÁY HÀNG</span>
        </h2>
        <span v-if="featuredSlotName" class="hfs-mini-timer" aria-label="Đếm ngược flash sale">
          <span class="mini-label">{{ miniLabel || 'Kết thúc sau' }}</span>
          <span class="mini-digit">{{ miniTimer.hours[0] }}</span>
          <span class="mini-digit">{{ miniTimer.hours[1] }}</span>
          <span class="mini-colon">:</span>
          <span class="mini-digit">{{ miniTimer.minutes[0] }}</span>
          <span class="mini-digit">{{ miniTimer.minutes[1] }}</span>
          <span class="mini-colon">:</span>
          <span class="mini-digit">{{ miniTimer.seconds[0] }}</span>
          <span class="mini-digit">{{ miniTimer.seconds[1] }}</span>
        </span>
      </div>

      <router-link to="/khuyen-mai" class="see-all">
        Xem tất cả
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
             stroke-linecap="round" stroke-linejoin="round">
          <path d="M5 12h14M13 6l6 6-6 6"/>
        </svg>
      </router-link>
    </header>

    <!-- Carousel -->
    <div class="hfs-carousel" @mouseenter="paused = true" @mouseleave="paused = false">
      <button
        type="button"
        class="hfs-nav hfs-nav--prev"
        :disabled="currentIndex === 0"
        aria-label="Sản phẩm trước"
        @click="scrollBy(-1)"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
             stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6"/>
        </svg>
      </button>

      <div class="hfs-viewport" ref="viewport">
        <div
          class="hfs-track"
          :style="{ transform: `translateX(${trackOffset}px)` }"
        >
          <article
            v-for="(product, index) in displayedProducts"
            :key="product.id"
            class="hfs-card"
            :class="{ 'low-stock': product.left <= 3 }"
            :style="{ '--enter-delay': (index * 50) + 'ms' }"
            @click="goToProduct(product)"
          >
            <span class="hfs-discount" aria-label="Giảm giá">
              <svg viewBox="0 0 24 24"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
              -{{ product.discount }}%
            </span>

            <div class="hfs-thumb">
              <img
                v-if="product.image"
                :src="product.image"
                :alt="product.name"
                @error="(e) => { e.target.style.display = 'none' }"
              />
              <span v-else class="hfs-emoji">{{ product.emoji }}</span>
            </div>

            <div class="hfs-name">{{ product.name }}</div>
            <div class="hfs-spec">{{ product.spec }}</div>

            <div class="hfs-stock">
              <div class="hfs-stock-row">
                <span class="hfs-sold-text">Đã bán {{ product.soldPercent }}%</span>
                <span class="blink">Còn {{ product.left }} SP</span>
              </div>
              <div class="hfs-stock-track">
                <div class="hfs-stock-fill" :style="{ width: product.soldPercent + '%' }"></div>
              </div>
            </div>

            <div class="hfs-price-row">
              <span class="hfs-price-now">{{ formatPrice(product.displayPrice) }}</span>
              <span class="hfs-price-old">{{ formatPrice(product.originalPrice) }}</span>
            </div>

            <button type="button" class="hfs-buy-btn" @click.stop="goToProduct(product)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                   stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm-8 2a2 2 0 1 1-4 0 2 2 0 0 1 4 0z"/>
              </svg>
              <span>Mua ngay</span>
            </button>
          </article>

          <!-- Nút Xem thêm chen vào cuối danh sách -->
          <router-link
            v-if="displayedProducts.length > 0"
            to="/khuyen-mai"
            class="hfs-card--more"
            aria-label="Xem thêm sản phẩm Flash Sale"
          >
            <span class="more-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
                   stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 12h14M13 6l6 6-6 6"/>
              </svg>
            </span>
            <span class="more-title">Xem thêm</span>
            <span class="more-sub">
              {{ remainingCount > 0
                ? `Còn ${remainingCount} sản phẩm đang giảm giá`
                : 'Khám phá toàn bộ Flash Sale hôm nay' }}
            </span>
            <span class="more-arrow">
              SANG TRANG
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                   stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 12h14M13 6l6 6-6 6"/>
              </svg>
            </span>
          </router-link>

          <div v-if="displayedProducts.length === 0" class="hfs-empty">
            <span v-if="clientLoading">Đang tải sản phẩm Flash Sale...</span>
            <span v-else>Hiện chưa có Flash Sale nào đang diễn ra. Quay lại sau nhé!</span>
          </div>
        </div>
      </div>

      <button
        type="button"
        class="hfs-nav hfs-nav--next"
        :disabled="currentIndex >= maxIndex"
        aria-label="Sản phẩm tiếp theo"
        @click="scrollBy(1)"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
             stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 6l6 6-6 6"/>
        </svg>
      </button>
    </div>

    <!-- Toast thông báo — Teleport ra body để tránh bị clip bởi overflow/transform của container -->
    <Teleport to="body">
      <Transition name="hfs-toast">
        <div v-if="toast.show" class="hfs-toast-alert">
          <strong>{{ toast.title }}</strong>
          <span>{{ toast.message }}</span>
        </div>
      </Transition>
    </Teleport>

    <!-- Modal thông báo Flash Sale đã bị admin hủy -->
    <CancelledFlashSaleModal
      :visible="showCancelledModal"
      @close="handleCancelledClose"
      @confirm="handleCancelledConfirm"
    />
  </section>
</template>

<script setup>
import { ref, reactive, computed, onBeforeUnmount, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useFlashSaleStore } from '@/stores/FlashSaleStore'
import { useCartStore } from '@/stores/cartStore'
import { useFlashSaleCountdown } from '@/composables/useFlashSaleCountdown'
import CancelledFlashSaleModal from '@/components/CancelledFlashSaleModal.vue'
import '@/assets/css/HomeFlashSale.css'

const router = useRouter()
const flashSaleStore = useFlashSaleStore()
const cartStore = useCartStore()
const { clientSlots, clientLoading } = storeToRefs(flashSaleStore)

const MAX_PRODUCTS = 10
const VISIBLE_DEFAULT = 5

// Tên slot Flash Sale đang hiển thị - đồng bộ với nearestSlot
const featuredSlotName = computed(() => nearestSlot.value?.name || '')

// ==== Bộ đếm ngược: dùng chung composable với FlashSalePage.vue ====
// Khi timer chạm 00:00:00 sẽ tự gọi lại fetchClientSlots để cập nhật slot mới.
const { label: miniLabel, timer: miniTimer } = useFlashSaleCountdown(
  () => flashSaleStore.clientSlots,
  () => flashSaleStore.fetchClientSlots(8),
)

// ==== Slot gần nhất (featured) ====
function safeDate(v) {
  if (!v) return null
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? null : d
}

const nearestSlot = computed(() => {
  const slots = Array.isArray(clientSlots.value) ? clientSlots.value : []
  if (slots.length > 0) {
    console.log('[HomeFlashSale] clientSlots count:', slots.length)
    console.log('[HomeFlashSale] slot statuses:', slots.map((s) => ({
      id: s.slotId,
      status: s.status,
      startDate: s.startDate,
      endDate: s.endDate,
      itemsCount: Array.isArray(s.items) ? s.items.length : 0,
    })))
  }

  if (slots.length === 0) return null

  // 1. Ưu tiên ACTIVE (status=2)
  const active = slots.filter((s) => Number(s.status) === 2)
  if (active.length > 0) {
    return [...active].sort((a, b) => {
      const da = safeDate(a.endDate)
      const db = safeDate(b.endDate)
      if (da && db) return da - db
      if (da) return -1
      if (db) return 1
      return 0
    })[0]
  }

  // 2. SCHEDULED (status=1)
  const upcoming = slots.filter((s) => Number(s.status) === 1)
  if (upcoming.length > 0) {
    return [...upcoming].sort((a, b) => {
      const da = safeDate(a.startDate)
      const db = safeDate(b.startDate)
      if (da && db) return da - db
      if (da) return -1
      if (db) return 1
      return 0
    })[0]
  }

  return null
})

// ==== Dữ liệu sản phẩm ====
function mapItemToCard(item, idx) {
  const totalQty = Number(item.flashSaleQuantity ?? 0)
  const soldQty = Number(item.soldQuantity ?? 0)
  const soldPercent = totalQty > 0 ? Math.min(100, Math.round((soldQty / totalQty) * 100)) : 0
  const remaining = Math.max(0, Number(item.remainingQuantity ?? totalQty - soldQty))
  const flashPrice = Number(item.flashSalePrice ?? 0)
  const origPrice = Number(item.originalPrice ?? 0)
  const price = flashPrice > 0 ? flashPrice : origPrice
  const discountFromBE = Number(item.discountPercent ?? 0)
  const discount = discountFromBE > 0
    ? discountFromBE
    : (origPrice > 0 && price > 0 && price < origPrice
      ? Math.floor(((origPrice - price) / origPrice) * 100)
      : 0)
  return {
    id: item.skuId ?? `fs-${idx}`,
    skuId: item.skuId ?? item.id,
    name: item.productName || item.skuCode || `Sản phẩm Flash Sale #${idx + 1}`,
    slug: item.skuCode || `flash-sale-${item.skuId ?? idx}`,
    spec: item.skuCode ? `Mã: ${item.skuCode}` : 'Sản phẩm chính hãng',
    emoji: '🛍️',
    image: item.skuImageUrl || null,
    price: price,
    originalPrice: origPrice,
    discount,
    soldPercent,
    left: remaining,
    displayPrice: 0,
    slotId: item._slotId || null,
  }
}

const allProducts = computed(() => {
  const slot = nearestSlot.value
  if (!slot || !Array.isArray(slot.items) || slot.items.length === 0) return []
  const itemsWithSlot = slot.items.map((it) => ({ ...it, _slotId: slot.slotId }))
  const mapped = itemsWithSlot.map(mapItemToCard)
  console.log(
    `[HomeFlashSale] Slot #${slot.slotId} "${slot.name}" status=${slot.status}: ` +
    `${slot.items.length} items render carousel.`,
  )
  return mapped.slice(0, MAX_PRODUCTS)
})

const displayedProducts = computed(() => allProducts.value)
const remainingCount = computed(() => Math.max(0, allProducts.value.length - VISIBLE_DEFAULT))

// ==== Carousel state ====
const viewport = ref(null)
const currentIndex = ref(0)
const paused = ref(false)
const cardWidth = ref(0)
const gap = 14

const visibleCount = ref(VISIBLE_DEFAULT)
const trackOffset = computed(() => -(currentIndex.value * (cardWidth.value + gap)))
const maxIndex = computed(() => Math.max(0, displayedProducts.value.length - visibleCount.value))

function measureCardWidth() {
  const el = viewport.value
  if (!el) return
  const totalGaps = gap * (visibleCount.value - 1)
  cardWidth.value = Math.max(0, (el.clientWidth - totalGaps) / visibleCount.value)
}

function updateVisibleCount() {
  const w = window.innerWidth
  if (w <= 560) visibleCount.value = 2
  else if (w <= 820) visibleCount.value = 3
  else if (w <= 1080) visibleCount.value = 4
  else visibleCount.value = VISIBLE_DEFAULT
}

function scrollBy(delta) {
  const next = currentIndex.value + delta
  if (next < 0) currentIndex.value = 0
  else if (next > maxIndex.value) currentIndex.value = maxIndex.value
  else currentIndex.value = next
}

let resizeHandler = null
let refreshTimer = null

// ==== Auto-scroll nhẹ nhàng (chỉ chạy khi không hover) ====
let autoTimer = null
function startAutoScroll() {
  stopAutoScroll()
  autoTimer = setInterval(() => {
    if (paused.value) return
    if (displayedProducts.value.length <= visibleCount.value) return
    if (currentIndex.value >= maxIndex.value) currentIndex.value = 0
    else currentIndex.value += 1
  }, 5000)
}
function stopAutoScroll() {
  if (autoTimer) clearInterval(autoTimer)
  autoTimer = null
}

// ==== Helpers ====
const formatPrice = (price) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)

// ==== Toast ====
const toast = reactive({
  show: false,
  type: 'warning',
  title: '',
  message: '',
})
let toastTimer = null
function showToast({ type = 'warning', title, message }) {
  clearTimeout(toastTimer)
  toast.type = type
  toast.title = title
  toast.message = message
  toast.show = true
  toastTimer = setTimeout(() => {
    toast.show = false
  }, 2800)
}

// ==== Modal thông báo Flash Sale bị admin hủy ====
const showCancelledModal = ref(false)
const hasHandledCancelled = ref(false)

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
  try {
    await flashSaleStore.fetchClientSlots(20)
  } catch (e) {
    console.warn('Refetch slots failed:', e)
  }
}

onBeforeUnmount(() => {
  hasHandledCancelled.value = false
})

// ==== Điều hướng sản phẩm ====
const isFlashSaleActive = computed(() => {
  if (!Array.isArray(clientSlots.value) || clientSlots.value.length === 0) return false
  return clientSlots.value.some((s) => Number(s.status) === 2)
})

function ensureProductSlotIsBuyable(product) {
  if (product.slotId && flashSaleStore.isSlotCancelled(product.slotId)) {
    openCancelledModal()
    return false
  }
  if (product.slotId && !flashSaleStore.isSlotActive(product.slotId)) {
    const ownSlot = flashSaleStore.getSlotById(product.slotId)
    let msg = 'Sản phẩm này không nằm trong Flash Sale đang diễn ra'
    if (ownSlot) {
      const status = Number(ownSlot.status)
      if (status === 1) msg = 'Flash Sale này chưa bắt đầu'
      else if (status === 3) msg = 'Flash Sale này đã kết thúc'
      else if (status === 4) msg = 'Flash Sale này đã bị admin hủy'
    }
    showToast({ type: 'warning', title: 'Oops!', message: msg })
    return false
  }
  if (!isFlashSaleActive.value) {
    showToast({
      type: 'warning',
      title: 'Oops!',
      message: 'Flash Sale chưa bắt đầu, hãy chờ thêm nhé!',
    })
    return false
  }
  return true
}

function prepareCheckoutSelection(cartItem, product, ownSlot) {
  const item = {
    cartItemId: cartItem.cartItemId,
    productName: cartItem.name || product.name,
    variantName: cartItem.variant || '',
    skuCode: cartItem.skuCode || product.spec?.replace('Mã: ', '') || '',
    skuId: cartItem.skuId ?? product.skuId,
    imageUrl: cartItem.imageUrl || product.image || '',
    quantity: cartItem.quantity ?? 1,
    price: cartItem.price ?? product.price,
    totalPrice:
      cartItem.totalPrice ?? (cartItem.price ?? product.price) * (cartItem.quantity ?? 1),
    isFlashSale: true,
    flashSaleSlotId: ownSlot?.slotId ?? product.slotId ?? null,
    flashSaleSlotName:
      ownSlot?.name || ownSlot?.slotName || product.slotName || 'Flash Sale',
  }

  localStorage.setItem('selectedCartItems', JSON.stringify([item]))
  localStorage.setItem('selectedSubtotal', String(item.totalPrice))
}

const buyNow = async (product) => {
  try {
    if (!ensureProductSlotIsBuyable(product)) return

    const ownSlot = flashSaleStore.getSlotById(product.slotId)
    if (!ownSlot || Number(ownSlot.status) !== 2) {
      showToast({
        type: 'warning',
        title: 'Oops!',
        message: 'Sản phẩm này không nằm trong Flash Sale đang diễn ra',
      })
      return
    }

    if (product.left <= 0) {
      showToast({
        type: 'error',
        title: 'Hết hàng!',
        message: 'Sản phẩm đã hết hàng trong Flash Sale này',
      })
      return
    }

    if (!Array.isArray(cartStore.items) || cartStore.items.length === 0) {
      try {
        await cartStore.fetchCart()
      } catch (e) {
        console.warn('[HomeFlashSale] fetchCart thất bại:', e)
      }
    }

    const existingItem =
      (cartStore.items || []).find(
        (ci) => ci.skuId === product.skuId && ci.isFlashSale === true,
      ) ||
      (cartStore.items || []).find((ci) => ci.skuId === product.skuId) ||
      null

    let matchedItem = existingItem

    if (!matchedItem) {
      const success = await cartStore.addToCartWithFlashSale(
        product.skuId,
        1,
        ownSlot.slotId,
        product.price,
      )

      if (!success) {
        showToast({
          type: 'error',
          title: 'Lỗi!',
          message: cartStore.error || 'Không thể thêm vào giỏ',
        })
        return
      }

      matchedItem =
        (cartStore.items || []).find(
          (ci) => ci.skuId === product.skuId && ci.isFlashSale === true,
        ) ||
        (cartStore.items || []).find((ci) => ci.skuId === product.skuId) ||
        null
    }

    if (!matchedItem) {
      showToast({
        type: 'error',
        title: 'Lỗi!',
        message: 'Không tìm thấy sản phẩm trong giỏ, vui lòng thử lại.',
      })
      return
    }

    prepareCheckoutSelection(matchedItem, product, ownSlot)
    router.push('/checkout')
  } catch (error) {
    console.error('Lỗi mua ngay:', error)
    showToast({
      type: 'error',
      title: 'Lỗi!',
      message: error.response?.data?.message || 'Không thể mua ngay',
    })
  }
}

function goToProduct(product) {
  if (!ensureProductSlotIsBuyable(product)) return
  buyNow(product)
}

// Animate count up cho giá
const animateCountUp = (product, delayMs = 0) => {
  const duration = 800
  const to = product.price
  let startTs = null
  const step = (now) => {
    if (startTs === null) startTs = now + delayMs
    if (now < startTs) { requestAnimationFrame(step); return }
    const t = Math.min((now - startTs) / duration, 1)
    const eased = 1 - Math.pow(1 - t, 3)
    product.displayPrice = Math.round(to * eased)
    if (t < 1) requestAnimationFrame(step)
    else product.displayPrice = to
  }
  requestAnimationFrame(step)
}

// FIX: Watch displayedProducts thay vì chỉ gọi animateCountUp 1 lần trong onMounted.
// Trước đây: mỗi lần refreshTimer (60s) fetch lại dữ liệu → allProducts computed
// chạy lại → mapItemToCard tạo object MỚI với displayPrice=0, nhưng không có gì
// gọi lại animateCountUp cho các object mới này → giá bị "đứng" ở 0đ cho tới khi F5.
// Giờ dùng watch({ immediate: true }) để tự động chạy animation mỗi khi
// displayedProducts thay đổi (bao gồm cả lần đầu mount và mỗi lần refetch sau này).
watch(
  displayedProducts,
  (products) => {
    products.forEach((p, i) => animateCountUp(p, i * 50))
  },
  { immediate: true },
)

onMounted(async () => {
  updateVisibleCount()
  await nextTick()
  measureCardWidth()

  try {
    await flashSaleStore.fetchClientSlots(20)
  } catch (e) {
    console.warn('[HomeFlashSale] không tải được dữ liệu flash sale:', e)
  }

  await nextTick()
  measureCardWidth()

  // Đo lại sau khi ảnh load xong (ảnh có thể làm thay đổi flex/height)
  setTimeout(() => measureCardWidth(), 350)

  // Refresh dữ liệu mỗi 60s để cập nhật giá/sold realtime (không spam BE)
  refreshTimer = setInterval(() => {
    flashSaleStore.fetchClientSlots(20).catch(() => {})
  }, 60000)

  resizeHandler = () => {
    updateVisibleCount()
    measureCardWidth()
    if (currentIndex.value > maxIndex.value) currentIndex.value = maxIndex.value
  }
  window.addEventListener('resize', resizeHandler)

  startAutoScroll()
})

onUnmounted(() => {
  stopAutoScroll()
  if (refreshTimer) clearInterval(refreshTimer)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (toastTimer) clearTimeout(toastTimer)
})
</script>
