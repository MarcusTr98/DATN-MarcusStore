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
<<<<<<< HEAD
            <div class="hfs-spec">
              <span v-if="product.spec">{{ product.spec }}</span>
              <span v-if="product.spec && product.variant"><br/></span>
              <span v-if="product.variant">{{ product.variant }}</span>
            </div>

            <div class="hfs-stock">
              <div class="hfs-stock-row">
                <span class="hfs-sold-text">Đã bán {{ product.soldPercent }}%</span>
=======
            <div class="hfs-spec">{{ product.spec }}</div>

            <div class="hfs-stock">
              <div class="hfs-stock-row">
                <span>Đã bán {{ product.soldPercent }}%</span>
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
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
<<<<<<< HEAD

            <button
              type="button"
              class="hfs-buy-btn"
              :class="{ 'hfs-buy-btn--disabled': product.left <= 0 || product.addingToCart }"
              :disabled="product.left <= 0 || product.addingToCart"
              @click.stop="product.left > 0 && !product.addingToCart && buyNow(product)"
            >
              <svg v-if="product.left > 0" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2"
                   stroke-linecap="round" stroke-linejoin="round">
                <path
                  d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm-8 2a2 2 0 1 1-4 0 2 2 0 0 1 4 0z"/>
              </svg>
              <span>{{ product.left > 0 ? 'Mua ngay' : 'Hết hàng' }}</span>
            </button>
=======
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
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
<<<<<<< HEAD
              {{
                remainingCount > 0
                  ? `Còn ${remainingCount} sản phẩm đang giảm giá`
                  : 'Khám phá toàn bộ Flash Sale hôm nay'
              }}
=======
              {{ remainingCount > 0
                ? `Còn ${remainingCount} sản phẩm đang giảm giá`
                : 'Khám phá toàn bộ Flash Sale hôm nay' }}
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
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
<<<<<<< HEAD

    <!-- Modal thông báo Flash Sale đã bị admin hủy -->
    <CancelledFlashSaleModal
      :visible="showCancelledModal"
      @close="handleCancelledClose"
      @confirm="handleCancelledConfirm"
    />

    <!-- Modal yêu cầu đăng nhập (khi guest nhận 401) -->
    <LoginRequiredModal
      :visible="showLoginRequiredModal"
      :title="loginRequiredTitle"
      :message="loginRequiredMessage"
      @close="showLoginRequiredModal = false"
    />
=======
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
  </section>
</template>

<script setup>
<<<<<<< HEAD
import {ref, reactive, computed, onMounted, onUnmounted, nextTick, watch} from 'vue'
import {useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import {useFlashSaleStore} from '@/stores/FlashSaleStore'
import {useCartStore} from '@/stores/cartStore'
import {useFlashSaleCountdown} from '@/composables/useFlashSaleCountdown'
import {useFlashSaleModals} from '@/composables/useFlashSaleModals'
import CancelledFlashSaleModal from '@/components/CancelledFlashSaleModal.vue'
import LoginRequiredModal from '@/components/LoginRequiredModal.vue'
import {expandVariantColorNames} from '@/utils/colorUtils'
=======
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useFlashSaleStore } from '@/stores/FlashSaleStore'
import { useFlashSaleCountdown } from '@/composables/useFlashSaleCountdown'
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
import '@/assets/css/HomeFlashSale.css'

const router = useRouter()
const flashSaleStore = useFlashSaleStore()
<<<<<<< HEAD
const cartStore = useCartStore()
const {clientSlots, clientLoading} = storeToRefs(flashSaleStore)

// ==== Modal state — dùng chung composable với FlashSalePage.vue ====
const {
  showCancelledModal,
  showLoginRequiredModal,
  loginRequiredTitle,
  loginRequiredMessage,
  openCancelledModal,
  handleCancelledClose,
  handleCancelledConfirm,
  handleAuthRequired,
} = useFlashSaleModals({
  onCancelledConfirm: async () => {
    await flashSaleStore.fetchClientSlots(20)
  },
})

const MAX_PRODUCTS = 10
const VISIBLE_DEFAULT = 5

// Tên slot Flash Sale đang hiển thị - đồng bộ với nearestSlot
const featuredSlotName = computed(() => nearestSlot.value?.name || '')

// ==== Bộ đếm ngược: dùng chung composable với FlashSalePage.vue ====
// Khi timer chạm 00:00:00 sẽ tự gọi lại fetchClientSlots để cập nhật slot mới.
const {label: miniLabel, timer: miniTimer} = useFlashSaleCountdown(
=======
const { clientSlots, clientLoading } = storeToRefs(flashSaleStore)

const MAX_PRODUCTS = 10
const VISIBLE_DEFAULT = 5

// Tên slot Flash Sale đang hiển thị - đồng bộ với nearestSlot
const featuredSlotName = computed(() => nearestSlot.value?.name || '')

// ==== Bộ đếm ngược: dùng chung composable với FlashSalePage.vue ====
// Khi timer chạm 00:00:00 sẽ tự gọi lại fetchClientSlots để cập nhật slot mới.
const { label: miniLabel, timer: miniTimer } = useFlashSaleCountdown(
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
  () => flashSaleStore.clientSlots,
  () => flashSaleStore.fetchClientSlots(8),
)

// ==== Slot gần nhất (featured) ====
<<<<<<< HEAD
=======
// Quy tắc chọn slot "gần nhất":
//   1. Ưu tiên slot ACTIVE (status=2) - lấy slot ACTIVE có endDate sớm nhất
//      (slot sắp kết thúc nhất = nóng nhất, đẩy lên carousel chính)
//   2. Nếu không có ACTIVE → lấy slot SCHEDULED (status=1) có startDate sớm nhất
//      (slot sắp bắt đầu nhất = sắp diễn ra kế tiếp)
// Trả về slot đó + chỉ items của slot đó (KHÔNG gộp nhiều slot).
//
// Robust: chấp nhận endDate/startDate bị null/missing → fallback sort theo thứ tự gốc.
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
function safeDate(v) {
  if (!v) return null
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? null : d
}

const nearestSlot = computed(() => {
  const slots = Array.isArray(clientSlots.value) ? clientSlots.value : []
<<<<<<< HEAD
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
function deriveVariantsFromSku(skuCode) {
  if (!skuCode) return []
  const parts = String(skuCode).split('-').filter(Boolean)
  const tail = parts.slice(-2)
  return tail.map((p) => expandVariantColorNames(p.replace(/_/g, ' ').trim())).filter((p) => p.length > 0)
}

=======
  // Debug log: in cấu trúc BE trả về để dễ chẩn đoán
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
    // Sort: slot có endDate hợp lệ → sớm nhất trước; null → xếp cuối
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

  // 3. Fallback cuối cùng: lấy slot bất kỳ (kể cả ENDED, CANCELLED) để user vẫn thấy sản phẩm
  //    miễn là slot có items. Đây là trường hợp API hiếm khi trả về status lạ.
  const any = slots.find((s) => Array.isArray(s.items) && s.items.length > 0)
  return any || null
})

// ==== Dữ liệu sản phẩm ====
// Lấy 100% từ BE (Pinia store), chỉ từ 1 slot gần nhất (xem nearestSlot ở trên).
// Không fallback — nếu BE không có data hoặc slot không có items → hiện empty state.
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
function mapItemToCard(item, idx) {
  const totalQty = Number(item.flashSaleQuantity ?? 0)
  const soldQty = Number(item.soldQuantity ?? 0)
  const soldPercent = totalQty > 0 ? Math.min(100, Math.round((soldQty / totalQty) * 100)) : 0
  const remaining = Math.max(0, Number(item.remainingQuantity ?? totalQty - soldQty))
<<<<<<< HEAD
  const flashPrice = Number(item.flashSalePrice ?? 0)
  const origPrice = Number(item.originalPrice ?? 0)
  const price = flashPrice > 0 ? flashPrice : origPrice
=======
  // Ưu tiên flashSalePrice (giá khuyến mãi) — đây là giá thực user phải trả.
  // originalPrice (giá gốc) là giá để tính % giảm + gạch ngang.
  // Khi flashSalePrice = 0/null (FS kết thúc), dùng originalPrice thay thế.
  const flashPrice = Number(item.flashSalePrice ?? 0)
  const origPrice = Number(item.originalPrice ?? 0)
  const price = flashPrice > 0 ? flashPrice : origPrice
  // Tính % giảm từ BE: ưu tiên discountPercent BE trả, fallback tự tính
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
  const discountFromBE = Number(item.discountPercent ?? 0)
  const discount = discountFromBE > 0
    ? discountFromBE
    : (origPrice > 0 && price > 0 && price < origPrice
<<<<<<< HEAD
      ? Math.floor(((origPrice - price) / origPrice) * 100)
      : 0)
=======
        ? Math.floor(((origPrice - price) / origPrice) * 100)
        : 0)
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
  return {
    id: item.skuId ?? `fs-${idx}`,
    skuId: item.skuId ?? item.id,
    name: item.productName || item.skuCode || `Sản phẩm Flash Sale #${idx + 1}`,
    slug: item.skuCode || `flash-sale-${item.skuId ?? idx}`,
    variant: deriveVariantsFromSku(item.skuCode).join(' / ') || '',
    spec: item.skuCode ? `Mã: ${item.skuCode}` : '',
    variants: deriveVariantsFromSku(item.skuCode),
    emoji: '🛍️',
<<<<<<< HEAD
    image: item.thumbnailUrl || null,
=======
    image: item.skuImageUrl || null,
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
    price: price,
    originalPrice: origPrice,
    discount,
    soldPercent,
    left: remaining,
    displayPrice: 0,
<<<<<<< HEAD
    addingToCart: false,
    slotId: item._slotId || null,
  }
}

const allProducts = computed(() => {
  const slot = nearestSlot.value
  if (!slot || !Array.isArray(slot.items) || slot.items.length === 0) return []
  const itemsWithSlot = slot.items.map((it) => ({...it, _slotId: slot.slotId}))
  const mapped = itemsWithSlot.map(mapItemToCard)
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
  new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(price)

// ==== Toast ====
const toast = reactive({
  show: false,
  type: 'warning',
  title: '',
  message: '',
})
let toastTimer = null

function showToast({type = 'warning', title, message}) {
  clearTimeout(toastTimer)
  toast.type = type
  toast.title = title
  toast.message = message
  toast.show = true
  toastTimer = setTimeout(() => {
    toast.show = false
  }, 2800)
}

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
    showToast({type: 'warning', title: 'Oops!', message: msg})
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
    skuCode: cartItem.skuCode || product.variant || product.skuCode || '',
    skuId: cartItem.skuId ?? product.skuId,
    thumbnailUrl: cartItem.thumbnailUrl || product.image || '',
    quantity: cartItem.quantity ?? 1,
    price:
      (cartItem.price || 0) > 0 ? cartItem.price : product.price,
    originalPrice: cartItem.originalPrice ?? product.originalPrice ?? null,
    // Nếu cartItem.totalPrice = 0/null/undefined thì TÍNH LẠI từ price * quantity.
    // Lưu ý: dùng || thay vì ?? vì backend có thể trả totalPrice = 0 do round/lỗi serialize.
    totalPrice:
      (cartItem.totalPrice || 0) > 0
        ? cartItem.totalPrice
        : ((cartItem.price || 0) > 0 ? cartItem.price : product.price) *
        (cartItem.quantity ?? 1),
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
        if (!isUnauthorizedError(cartStore.error)) {
          showToast({
            type: 'error',
            title: 'Lỗi!',
            message: cartStore.error || 'Không thể thêm vào giỏ',
          })
        }
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
    if (!isUnauthorizedError(error)) {
      showToast({
        type: 'error',
        title: 'Lỗi!',
        message: error.response?.data?.message || 'Không thể mua ngay',
      })
    }
  }
}

function addToCartProduct(product) {
  if (product.addingToCart) return
  product.addingToCart = true

  const ownSlot = flashSaleStore.getSlotById(product.slotId)
  if (!ownSlot || Number(ownSlot.status) !== 2) {
    showToast({
      type: 'warning',
      title: 'Oops!',
      message: 'Sản phẩm này không nằm trong Flash Sale đang diễn ra',
    })
    product.addingToCart = false
    return
  }

  if (product.left <= 0) {
    showToast({
      type: 'error',
      title: 'Hết hàng!',
      message: 'Sản phẩm đã hết hàng trong Flash Sale này',
    })
    product.addingToCart = false
    return
  }

  cartStore.addToCartWithFlashSale(
    product.skuId,
    1,
    ownSlot.slotId,
    product.price,
  ).then((success) => {
    if (success) {
      showToast({
        type: 'success',
        title: 'Thành công!',
        message: 'Đã thêm vào giỏ hàng',
      })
    } else {
      if (!isUnauthorizedError(cartStore.error)) {
        showToast({
          type: 'error',
          title: 'Lỗi!',
          message: cartStore.error || 'Không thể thêm vào giỏ',
        })
      }
    }
  }).catch((error) => {
    if (!isUnauthorizedError(error)) {
      showToast({
        type: 'error',
        title: 'Lỗi!',
        message: error.response?.data?.message || 'Không thể thêm vào giỏ hàng',
      })
    }
  }).finally(() => {
    setTimeout(() => {
      product.addingToCart = false
    }, 700)
  })
}

function goToProduct(product) {
  if (!ensureProductSlotIsBuyable(product)) return
  addToCartProduct(product)
}

// Helper: phát hiện lỗi 401/Unauthorized để bỏ qua toast
// (modal LoginRequiredModal đã hiển thị thông báo rồi, không cần toast trùng)
function isUnauthorizedError(errOrMessage) {
  if (!errOrMessage) return false
  const status = errOrMessage?.response?.status
  if (status === 401) return true
  const msg = typeof errOrMessage === 'string' ? errOrMessage : errOrMessage?.message
  if (typeof msg === 'string' && /unauthor/i.test(msg)) return true
  return false
}

=======
  }
}

// Chỉ lấy items của slot gần nhất (nearestSlot) — không gộp slot.
// KHÔNG filter price>0 ở đây để tránh loại nhầm SP admin đang cập nhật.
// animateCountUp đã có early-return nếu price=0.
// Lấy tối đa MAX_PRODUCTS.
const allProducts = computed(() => {
  const slot = nearestSlot.value
  if (!slot || !Array.isArray(slot.items) || slot.items.length === 0) return []
  const mapped = slot.items.map(mapItemToCard)
  console.log(
    `[HomeFlashSale] Slot #${slot.slotId} "${slot.name}" status=${slot.status}: ` +
    `${slot.items.length} items render carousel.`,
  )
  return mapped.slice(0, MAX_PRODUCTS)
})

// 10 sản phẩm hiển thị (cộng thêm 1 slot cho nút "Xem thêm")
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
// Teleport ra body + bỏ nextTick để tránh flicker (visible→hidden→visible khi click liên tục)
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

// ==== Điều hướng sản phẩm ====
// Nếu chưa có slot ACTIVE (status=2) thì chặn + hiện thông báo,
// user vẫn thấy sản phẩm nhưng click sẽ không chuyển trang.
const isFlashSaleActive = computed(() => {
  if (!Array.isArray(clientSlots.value) || clientSlots.value.length === 0) return false
  return clientSlots.value.some((s) => Number(s.status) === 2)
})

function goToProduct(product) {
  if (!isFlashSaleActive.value) {
    showToast({
      type: 'warning',
      title: 'Oops!',
      message: 'Flash Sale chưa bắt đầu, hãy chờ thêm nhé!',
    })
    return
  }
  router.push(`/product/${product.slug}`)
}

>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
// Animate count up cho giá
const animateCountUp = (product, delayMs = 0) => {
  const duration = 800
  const to = product.price
  let startTs = null
  const step = (now) => {
    if (startTs === null) startTs = now + delayMs
<<<<<<< HEAD
    if (now < startTs) {
      requestAnimationFrame(step);
      return
    }
=======
    if (now < startTs) { requestAnimationFrame(step); return }
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
    const t = Math.min((now - startTs) / duration, 1)
    const eased = 1 - Math.pow(1 - t, 3)
    product.displayPrice = Math.round(to * eased)
    if (t < 1) requestAnimationFrame(step)
    else product.displayPrice = to
  }
  requestAnimationFrame(step)
}

<<<<<<< HEAD
watch(
  displayedProducts,
  (products) => {
    products.forEach((p, i) => animateCountUp(p, i * 50))
  },
  {immediate: true},
)

onMounted(async () => {
  // Lắng nghe event auth-required (khi guest nhận 401 từ API)
  window.addEventListener('auth-required', handleAuthRequired)

  updateVisibleCount()
  await nextTick()
  measureCardWidth()

  try {
=======
onMounted(async () => {
  updateVisibleCount()
  await nextTick()
  measureCardWidth()

  try {
    // Lấy tối đa 20 slot (mỗi slot có nhiều items) để đảm bảo đủ 10 sản phẩm
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
    await flashSaleStore.fetchClientSlots(20)
  } catch (e) {
    console.warn('[HomeFlashSale] không tải được dữ liệu flash sale:', e)
  }

  await nextTick()
  measureCardWidth()
<<<<<<< HEAD
=======
  displayedProducts.value.forEach((p, i) => animateCountUp(p, 200 + i * 50))
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)

  // Đo lại sau khi ảnh load xong (ảnh có thể làm thay đổi flex/height)
  setTimeout(() => measureCardWidth(), 350)

  // Refresh dữ liệu mỗi 60s để cập nhật giá/sold realtime (không spam BE)
  refreshTimer = setInterval(() => {
<<<<<<< HEAD
    flashSaleStore.fetchClientSlots(20).catch(() => {
    })
=======
    flashSaleStore.fetchClientSlots(20).catch(() => {})
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
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
<<<<<<< HEAD
</script>
=======
</script>
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)
