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
                <span>Đã bán {{ product.soldPercent }}%</span>
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
  </section>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useFlashSaleStore } from '@/stores/FlashSaleStore'
import { useFlashSaleCountdown } from '@/composables/useFlashSaleCountdown'
import '@/assets/css/HomeFlashSale.css'

const router = useRouter()
const flashSaleStore = useFlashSaleStore()
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
// Quy tắc chọn slot "gần nhất":
//   1. Ưu tiên slot ACTIVE (status=2) - lấy slot ACTIVE có endDate sớm nhất
//      (slot sắp kết thúc nhất = nóng nhất, đẩy lên carousel chính)
//   2. Nếu không có ACTIVE → lấy slot SCHEDULED (status=1) có startDate sớm nhất
//      (slot sắp bắt đầu nhất = sắp diễn ra kế tiếp)
// Trả về slot đó + chỉ items của slot đó (KHÔNG gộp nhiều slot).
//
// Robust: chấp nhận endDate/startDate bị null/missing → fallback sort theo thứ tự gốc.
function safeDate(v) {
  if (!v) return null
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? null : d
}

const nearestSlot = computed(() => {
  const slots = Array.isArray(clientSlots.value) ? clientSlots.value : []
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
function mapItemToCard(item, idx) {
  const totalQty = Number(item.flashSaleQuantity ?? 0)
  const soldQty = Number(item.soldQuantity ?? 0)
  const soldPercent = totalQty > 0 ? Math.min(100, Math.round((soldQty / totalQty) * 100)) : 0
  const remaining = Math.max(0, Number(item.remainingQuantity ?? totalQty - soldQty))
  // Ưu tiên flashSalePrice (giá khuyến mãi) — đây là giá thực user phải trả.
  // originalPrice (giá gốc) là giá để tính % giảm + gạch ngang.
  // Khi flashSalePrice = 0/null (FS kết thúc), dùng originalPrice thay thế.
  const flashPrice = Number(item.flashSalePrice ?? 0)
  const origPrice = Number(item.originalPrice ?? 0)
  const price = flashPrice > 0 ? flashPrice : origPrice
  // Tính % giảm từ BE: ưu tiên discountPercent BE trả, fallback tự tính
  const discountFromBE = Number(item.discountPercent ?? 0)
  const discount = discountFromBE > 0
    ? discountFromBE
    : (origPrice > 0 && price > 0 && price < origPrice
        ? Math.floor(((origPrice - price) / origPrice) * 100)
        : 0)
  return {
    id: item.skuId ?? `fs-${idx}`,
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

onMounted(async () => {
  updateVisibleCount()
  await nextTick()
  measureCardWidth()

  try {
    // Lấy tối đa 20 slot (mỗi slot có nhiều items) để đảm bảo đủ 10 sản phẩm
    await flashSaleStore.fetchClientSlots(20)
  } catch (e) {
    console.warn('[HomeFlashSale] không tải được dữ liệu flash sale:', e)
  }

  await nextTick()
  measureCardWidth()
  displayedProducts.value.forEach((p, i) => animateCountUp(p, 200 + i * 50))

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