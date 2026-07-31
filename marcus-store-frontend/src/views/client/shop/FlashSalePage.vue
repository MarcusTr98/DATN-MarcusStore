<template>
  <div class="flash-sale-page">
    <!-- Timeline Bar -->
    <section class="flash-timeline">
      <span class="timeline-label">Flash Sale hôm nay</span>
      <div
        v-for="(slot, index) in flashSlots"
        :key="slot.slotId || index"
        class="slot"
        :class="{
          live: slot.isLive,
          active: slot.slotId === selectedSlotId,
          upcoming: !slot.isLive,
        }"
        role="button"
        tabindex="0"
        @click="onSelectSlot(slot)"
        @keydown.enter="onSelectSlot(slot)"
      >
        <span class="dot"></span>
        {{ slot.time }}
        <span v-if="!slot.isLive" class="bell">🔔</span>
      </div>
    </section>

    <!-- Hero Banner Section -->
    <section class="flash-hero" :class="{ 'has-banner': showBanner && selectedSlot?.bannerImageUrl }">
      <!-- Banner image làm background khi có.
           Guard: chỉ hiện khi slot được chọn còn valid (ACTIVE/SCHEDULED + còn thời gian).
           Nếu admin vừa hủy slot → showBanner=false → banner được ẩn hoàn toàn. -->
      <div v-if="showBanner && selectedSlot?.bannerImageUrl" class="hero-banner-bg">
        <img :src="selectedSlot.bannerImageUrl" alt="Flash Sale Banner" />
        <div class="hero-banner-overlay"></div>
      </div>

      <div class="hero-bg">
        <!-- Wave shapes -->
        <div class="wave wave-1"></div>
        <div class="wave wave-2"></div>
        <div class="wave wave-3"></div>

        <!-- Decorative circles -->
        <div class="deco-circle deco-1"></div>
        <div class="deco-circle deco-2"></div>

        <!-- Lightning decorations -->
        <div class="lightning-deco lightning-1">
          <svg viewBox="0 0 24 24"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
        </div>
        <div class="lightning-deco lightning-2">
          <svg viewBox="0 0 24 24"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
        </div>

        <!-- Falling Dots Container - inside hero-bg for proper positioning -->
        <div class="falling-dots" ref="fallingDotsContainer"></div>
      </div>

      <div class="hero-content">
        <div class="hero-left">
          <div class="hero-badge">
            <span class="badge-icon">⚡</span>
            <span>FLASH SALE</span>
          </div>
          <h1 class="hero-title">
            <span class="title-line accent">FLASH SALE</span>
            <span class="title-line">{{ slotName }}</span>

          </h1>


          <div class="countdown-wrapper">
            <span class="countdown-label">{{ countdownLabel || 'Kết thúc sau' }}</span>
            <div class="countdown" id="countdown">
              <div class="unit">
                <div class="digits">
                  <span class="digit">{{ timer.hours[0] }}</span>
                  <span class="digit">{{ timer.hours[1] }}</span>
                </div>
                <div class="unit-label">Giờ</div>
              </div>
              <div class="colon">:</div>
              <div class="unit">
                <div class="digits">
                  <span class="digit">{{ timer.minutes[0] }}</span>
                  <span class="digit">{{ timer.minutes[1] }}</span>
                </div>
                <div class="unit-label">Phút</div>
              </div>
              <div class="colon">:</div>
              <div class="unit">
                <div class="digits">
                  <span class="digit">{{ timer.seconds[0] }}</span>
                  <span class="digit">{{ timer.seconds[1] }}</span>
                </div>
                <div class="unit-label">Giây</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Stats chỉ hiện khi không có banner -->
        <div v-if="!selectedSlot?.bannerImageUrl" class="hero-right">
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-number">{{ stats.totalProducts || '200+' }}</span>
              <span class="stat-label">Sản phẩm</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-number">{{ stats.maxDiscount || '50%' }}</span>
              <span class="stat-label">Giảm giá</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-number">{{ stats.liveLabel || '24h' }}</span>
              <span class="stat-label">{{ stats.liveSubLabel || 'Chỉ hôm nay' }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Màn chờ cho slot SCHEDULED — chỉ hiện khi slot được chọn chưa LIVE -->
    <section v-if="selectedSlot && !isSelectedSlotLive" class="flash-waiting">
      <div class="waiting-card">
        <div class="waiting-icon">
          <svg viewBox="0 0 24 24" class="bell-icon">
            <path d="M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2zm6-6V11a6 6 0 1 0-12 0v5l-2 2v1h16v-1l-2-2z"/>
          </svg>
        </div>
        <h2 class="waiting-title">{{ selectedSlot.name || 'Flash Sale sắp diễn ra' }}</h2>
        <p class="waiting-subtitle">Vui lòng quay lại lúc</p>

        <!-- Countdown tới startDate -->
        <div class="waiting-countdown">
          <div class="waiting-unit">
            <span class="waiting-digit">{{ waitingTimer.hours }}</span>
            <span class="waiting-unit-label">Giờ</span>
          </div>
          <span class="waiting-colon">:</span>
          <div class="waiting-unit">
            <span class="waiting-digit">{{ waitingTimer.minutes }}</span>
            <span class="waiting-unit-label">Phút</span>
          </div>
          <span class="waiting-colon">:</span>
          <div class="waiting-unit">
            <span class="waiting-digit">{{ waitingTimer.seconds }}</span>
            <span class="waiting-unit-label">Giây</span>
          </div>
        </div>

        <p class="waiting-note">Hệ thống sẽ tự động mở bán khi đến giờ</p>
      </div>
    </section>

    <!-- Product Section -->
    <template v-if="isSelectedSlotLive">
      <div class="section-head">
        <h2>Đang <span>cháy</span> hàng</h2>
        <router-link to="/" class="see-all">← Quay lại trang chủ</router-link>
      </div>

      <section class="product-grid">
      <article
        v-for="(product, index) in flashSaleProducts"
        :key="product.id"
        class="flash-card"
        :class="{ 'low-stock': product.left <= 3 }"
        :style="{ '--enter-delay': (index * 80) + 'ms' }"
        @click="goToProduct(product)"
      >
        <!-- Khối badge góc trên trái: SALE + % giảm -->
        <div class="card-badges">
          <span class="card-tag" :style="{ animationDelay: (index * 0.35) + 's' }">
            <svg viewBox="0 0 24 24" class="lightning-icon"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
            SALE
          </span>
          <span class="card-discount">Giảm {{ product.discount }}%</span>
        </div>

        <!-- Nút góc trên phải: giỏ hàng -->
        <div class="card-actions" @click.stop>
          <button
            type="button"
            class="icon-btn"
            :class="{ adding: product.addingToCart }"
            title="Thêm vào giỏ hàng"
            @click.stop="addToCart(product)"
          >
            <svg viewBox="0 0 24 24" class="icon-svg">
              <path
                v-if="!product.addingToCart"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm-8 2a2 2 0 1 1-4 0 2 2 0 0 1 4 0z"
              />
              <path
                v-else
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M5 13l4 4L19 7"
              />
            </svg>
          </button>
        </div>

        <!-- Ảnh sản phẩm -->
        <div class="thumb">
          <img
            v-if="product.image"
            :src="product.image"
            :alt="product.name"
            class="product-image"
            @error="(e) => { console.warn('[FlashSalePage] image failed:', product.image, e); e.target.style.display='none' }"
          />
          <span v-else class="product-emoji">{{ product.emoji }}</span>
        </div>

        <!-- Tên + mô tả -->
        <div class="card-name">{{ product.name }}</div>
        <div class="card-spec">{{ product.spec }}</div>

        <!-- Thanh tiến trình kho -->
        <div class="scarcity">
          <div class="bar-row">
            <span>Đã bán {{ product.soldPercent }}%</span>
            <span class="blink">Sắp cháy · Còn {{ product.left }} SP</span>
          </div>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: product.soldPercent + '%' }">
              <span class="bar-shimmer"></span>
            </div>
          </div>
        </div>

        <!-- Giá -->
        <div class="price-row">
          <span class="price-now">{{ formatPrice(product.displayPrice) }}</span>
          <span class="price-old">{{ formatPrice(product.originalPrice) }}</span>
        </div>

        <!-- Chip biến thể -->
        <div v-if="product.variants && product.variants.length" class="variant-chips">
          <span
            v-for="(variant, vIdx) in product.variants"
            :key="vIdx"
            class="variant-chip"
          >
            {{ variant }}
          </span>
        </div>

        <!-- Tag khuyến mãi đi kèm -->
        <ul v-if="product.promos && product.promos.length" class="promo-tags">
          <li v-for="(promo, pIdx) in product.promos" :key="pIdx" class="promo-tag">
            <span class="promo-icon" v-html="promo.icon"></span>
            <span class="promo-text">{{ promo.text }}</span>
          </li>
        </ul>

        <!-- CTA lớn: MUA NGAY -->
        <button
          type="button"
          class="cta"
          :class="{ 'cta--disabled': product.left <= 0 }"
          :disabled="product.left <= 0"
          @click.stop="product.left > 0 && handleBuyClick($event, product)"
        >
          <span class="cta-text">{{ product.left > 0 ? 'MUA NGAY' : 'HẾT HÀNG' }}</span>
          <span v-if="product.left > 0" class="cta-icon">
            <svg viewBox="0 0 24 24"><path
              fill="none" stroke="currentColor" stroke-width="2.5"
              stroke-linecap="round" stroke-linejoin="round"
              d="M5 12h14M13 6l6 6-6 6"
            /></svg>
          </span>
          <span
            v-for="ripple in product.ripples"
            :key="ripple.id"
            class="ripple"
            :style="{ left: ripple.x + 'px', top: ripple.y + 'px' }"
          ></span>
        </button>
      </article>
    </section>

    <!-- Pagination -->
    <div class="fsp-pagination" v-if="totalPages > 1">
      <button
        class="fsp-page-btn fsp-page-prev"
        :disabled="currentPage === 1"
        @click="goToPage(currentPage - 1)"
      >
        <svg viewBox="0 0 24 24"><path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>

      <button
        v-for="page in visiblePages"
        :key="page"
        class="fsp-page-btn"
        :class="{ active: page === currentPage }"
        @click="goToPage(page)"
      >
        {{ page }}
      </button>

      <button
        class="fsp-page-btn fsp-page-next"
        :disabled="currentPage === totalPages"
        @click="goToPage(currentPage + 1)"
      >
        <svg viewBox="0 0 24 24"><path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>
    </div>

    <!-- Pagination Info -->
    <div class="fsp-pagination-info" v-if="totalProducts > 0">
      Hiển thị {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, totalProducts) }} của {{ totalProducts }} sản phẩm
    </div>
    </template>

    <!-- Promo Banners -->
    <section class="promo-grid">
      <div class="promo promo-b">
        <div class="tag">Ưu đãi thêm</div>
        <h3>Thu cũ đổi mới</h3>
        <p>Trợ giá đến 3 triệu khi lên đời máy mới.</p>
      </div>
      <div class="promo promo-b">
        <div class="tag">Thanh toán linh hoạt</div>
        <h3>Trả góp 0%</h3>
        <p>Duyệt nhanh 5 phút, không cần chứng minh thu nhập.</p>
      </div>
      <div class="promo promo-b">
        <div class="tag">Giao hàng siêu tốc</div>
        <h3>SHIP HỎA TỐC </h3>
        <p>Nhận hàng ngay trong 2 giờ tại nội thành. Miễn phí vận chuyển cho đơn hàng Flash Sale.</p>
      </div>
      <div class="promo promo-b">
        <div class="tag">Độc quyền MarcusStore
        </div>
        <h3>BẢO HÀNH 1 ĐỔI 1</h3>
        <p>Lỗi là đổi mới trong 30 ngày đầu tiên. Bảo hành chính hãng lên đến 24 tháng.</p>
      </div>
    </section>

    <!-- Footer -->
    <footer class="flash-footer">
      <div class="footer-left">
        <router-link to="/" class="footer-logo">
          <i class="fas fa-mobile-alt"></i>
          <span>Marcus<strong>STORE</strong></span>
        </router-link>
        <p>Hệ thống cửa hàng công nghệ hàng đầu Việt Nam</p>
      </div>
      <div class="footer-right">
        <router-link to="/" class="back-home">
          <i class="fas fa-home"></i>
          Quay về trang chủ
        </router-link>
      </div>
    </footer>

    <!-- FOMO Popup -->
    <div class="fomo" :class="{ show: fomoVisible }" role="status" aria-live="polite">
      <button class="fomo-close" @click="closeFomo">✕</button>
      <div class="fomo-dot">
        <svg viewBox="0 0 24 24"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
      </div>
      <div class="fomo-text">
        <b>{{ fomoMessage }}</b>
        <span>{{ fomoSub }}</span>
      </div>
      <div class="fomo-progress-track">
        <div v-if="fomoVisible" :key="fomoKey" class="fomo-progress-bar"></div>
      </div>
    </div>

    <!-- Toast thông báo — Teleport ra body để tránh clip bởi overflow/transform -->
    <Teleport to="body">
      <Transition name="fsp-toast">
        <div v-if="toast.show" class="fsp-toast-alert">
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

    <!-- Modal yêu cầu đăng nhập (khi guest nhận 401) -->
    <LoginRequiredModal
      :visible="showLoginRequiredModal"
      :title="loginRequiredTitle"
      :message="loginRequiredMessage"
      @close="showLoginRequiredModal = false; suppressErrorToast = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useFlashSaleStore } from '@/stores/FlashSaleStore'
import { useFlashSaleCountdown } from '@/composables/useFlashSaleCountdown'
import { useCartStore } from '@/stores/cartStore'
import { useFlashSaleModals } from '@/composables/useFlashSaleModals'
import CancelledFlashSaleModal from '@/components/CancelledFlashSaleModal.vue'
import LoginRequiredModal from '@/components/LoginRequiredModal.vue'
import { expandVariantColorNames } from '@/utils/colorUtils'
import '@/assets/css/FlashSalePage.css'

const router = useRouter()
const flashSaleStore = useFlashSaleStore()
const cartStore = useCartStore()
const { clientSlots, displaySlots, bannerStats } = storeToRefs(flashSaleStore)
const stats = computed(() => bannerStats.value || {})

// ==== Modal state — dùng chung composable với HomeFlashSale.vue ====
// suppressErrorToast: true khi vừa nhận auth-required (401) — chặn toast lỗi trùng lặp với modal login.
const suppressErrorToast = ref(false)
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
    await router.replace({ path: '/' }).catch(() => {
      window.location.href = '/'
    })
  },
})

// Override handleAuthRequired để set suppressErrorToast
function handleAuthRequiredPage(event) {
  handleAuthRequired(event)
  suppressErrorToast.value = true
}

// ==== Lấy dữ liệu Flash Sale từ BE ====
async function fetchFlashSales() {
  await flashSaleStore.fetchClientSlots(20)
}

// ID của slot user đang chọn xem trên timeline (mặc định = slot ACTIVE nếu có, fallback SCHEDULED đầu tiên).
// Click vào 1 mốc giờ trên timeline → setSlotId → đổi "tab" đang xem.
const selectedSlotId = ref(null)

// Đồng bộ selectedSlotId với clientSlots:
//  - Nếu slot đang chọn vẫn còn trong danh sách → giữ nguyên.
//  - Nếu slot đang chọn đã biến mất (admin xoá, hoặc reload) → fallback ACTIVE trước, SCHEDULED sau.
watch(clientSlots, (slots) => {
  if (!Array.isArray(slots) || slots.length === 0) {
    selectedSlotId.value = null
    return
  }
  if (selectedSlotId.value && slots.some((s) => s.slotId === selectedSlotId.value)) return
  const firstActive = slots.find((s) => Number(s.status) === 2)
  const firstUpcoming = slots.find((s) => Number(s.status) === 1)
  selectedSlotId.value = firstActive?.slotId ?? firstUpcoming?.slotId ?? null
}, { immediate: true })

// Slot đang được chọn để hiển thị (object đầy đủ).
const selectedSlot = computed(() => {
  if (!selectedSlotId.value || !Array.isArray(clientSlots.value)) return null
  return clientSlots.value.find((s) => s.slotId === selectedSlotId.value) || null
})

// Tên slot được chọn (hiển thị trong hero title).
const slotName = computed(() => selectedSlot.value?.name || '')

// Slot đang chọn có LIVE (status=2 + đang nằm trong khoảng [startDate, endDate]) hay không.
const isSelectedSlotLive = computed(() => {
  const s = selectedSlot.value
  if (!s) return false
  if (Number(s.status) !== 2) return false
  const now = Date.now()
  const startMs = s.startDate ? new Date(s.startDate).getTime() : null
  const endMs = s.endDate ? new Date(s.endDate).getTime() : null
  if (startMs && now < startMs) return false
  if (endMs && now >= endMs) return false
  return true
})

// ==== Giữ activeSlot để không phá vỡ các nơi khác vẫn đang tham chiếu (HomeFlashSale, etc.) ====
// Trên trang này activeSlot = selectedSlot để đảm bảo tương thích ngược với showBanner.
const activeSlot = selectedSlot

const showBanner = computed(() => {
  const slot = activeSlot.value
  if (!slot) return false
  if (!slot.bannerImageUrl) return false

  const status = Number(slot.status)
  if (status !== 1 && status !== 2) return false

  const now = Date.now()
  const startMs = slot.startDate ? new Date(slot.startDate).getTime() : null
  const endMs = slot.endDate ? new Date(slot.endDate).getTime() : null

  if (status === 2) {
    if (startMs && now < startMs) return false
    if (endMs && now >= endMs) return false
    return true
  }

  if (status === 1 && endMs && now >= endMs) return false
  return true
})

// ==== Bộ đếm ngược động (chạy mỗi giây) ====
// Truyền thêm getSelectedSlotId để countdown ưu tiên slot user đang chọn trên timeline.
// Nếu đang chọn slot SCHEDULED → countdown đếm tới startDate (label "BẮT ĐẦU SAU").
// Nếu đang chọn slot ACTIVE → countdown đếm tới endDate (label "KẾT THÚC SAU").
const { label: countdownLabel, timer } = useFlashSaleCountdown(
  () => flashSaleStore.clientSlots,
  () => fetchFlashSales(),
  () => selectedSlotId.value,
)

// Timeline
const flashSlots = computed(() => {
  const list = displaySlots.value
  if (Array.isArray(list) && list.length > 0) return list
  return [
    { time: 'Đang diễn ra · 09:00–12:00', isLive: true },
    { time: '12:00 SA', isLive: false },
    { time: '16:00 CH', isLive: false },
    { time: '20:00 CH', isLive: false },
  ]
})

// ==== Chuẩn hoá dữ liệu thô từ BE (KHÔNG chứa state UI) ====
function buildRawItem(item, idx) {
  const totalQty = Number(item.flashSaleQuantity ?? 0)
  const soldQty = Number(item.soldQuantity ?? 0)
  const soldPercent = totalQty > 0 ? Math.min(100, Math.round((soldQty / totalQty) * 100)) : 0
  const remaining = Math.max(0, Number(item.remainingQuantity ?? totalQty - soldQty))
  return {
    id: item.skuId ?? `fs-${idx}`,
    skuId: item.skuId ?? item.id,
    name: item.productName || item.skuCode || `Sản phẩm Flash Sale #${idx + 1}`,
    slug: item.skuCode || `flash-sale-${item.skuId ?? idx}`,
    spec: item.skuCode ? `Mã: ${item.skuCode}` : 'Sản phẩm chính hãng',
    emoji: '🛍️',
    image: item.thumbnailUrl || null,
    price: Number(item.flashSalePrice ?? item.originalPrice ?? 0),
    originalPrice: Number(item.originalPrice ?? item.flashSalePrice ?? 0),
    discount: item.discountPercent ?? 0,
    soldPercent,
    left: remaining,
    variants: deriveVariantsFromSku(item.skuCode),
    promos: derivePromoTags(item),
    slotId: item._slotId || null,
    slotName: item._slotName || null,
  }
}

function deriveVariantsFromSku(skuCode) {
  if (!skuCode) return []
  const parts = String(skuCode).split('-').filter(Boolean)
  const tail = parts.slice(-2)
  return tail.map((p) => expandVariantColorNames(p.replace(/_/g, ' ').trim())).filter((p) => p.length > 0)
}

function derivePromoTags(item) {
  const tags = []
  const price = Number(item.originalPrice ?? 0)
  if (price > 10000000) {
    tags.push({ icon: '🎓', text: 'Học sinh/Sinh viên giảm đến 500.000đ' })
  }
  tags.push({ icon: '🛡️', text: 'Bảo hành chính hãng 12 tháng' })
  return tags
}

// Chỉ lấy items của slot đang được chọn (selectedSlotId) — tách riêng ACTIVE và SCHEDULED.
// Trước đây: gộp tất cả items từ mọi slot ACTIVE/SCHEDULED → làm user thấy sản phẩm của slot
// sắp diễn ra chung với slot đang chạy. Bây giờ: khi click vào 1 mốc giờ trên timeline →
// chỉ hiển thị items của slot đó (hoặc màn chờ nếu slot chưa LIVE).
const rawFlashSaleItems = computed(() => {
  if (!Array.isArray(clientSlots.value) || clientSlots.value.length === 0) return []
  if (!selectedSlotId.value) return []
  const slot = clientSlots.value.find((s) => s.slotId === selectedSlotId.value)
  if (!slot || !Array.isArray(slot.items)) return []
  return slot.items
    .filter((it) => !slot.isCancelled)
    .map((it) => buildRawItem({ ...it, _slotId: slot.slotId, _slotName: slot.name }))
})

// ==== Phân trang sản phẩm ====
const currentPage = ref(1)
const pageSize = 12

const totalProducts = computed(() => rawFlashSaleItems.value.length)
const totalPages = computed(() => Math.ceil(totalProducts.value / pageSize) || 1)

// Reset về trang 1 khi tổng sản phẩm thay đổi (slot mới, xóa sp...)
watch(totalProducts, (newTotal) => {
  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value
  }
  if (newTotal === 0) currentPage.value = 1
})

// Items của trang hiện tại
const paginatedRawItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return rawFlashSaleItems.value.slice(start, end)
})

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// Xử lý khi user click 1 slot trên timeline.
function onSelectSlot(slot) {
  if (!slot || !slot.slotId) return
  if (slot.slotId === selectedSlotId.value) return
  selectedSlotId.value = slot.slotId
  // Reset về trang 1 khi chuyển slot để tránh trang hiện tại vượt quá tổng trang của slot mới.
  currentPage.value = 1
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// Hiển thị tối đa 5 nút trang, có "..." nếu cần
const visiblePages = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1)

  const pages = []
  if (cur <= 3) {
    pages.push(1, 2, 3, 4, '...', total)
  } else if (cur >= total - 2) {
    pages.push(1, '...', total - 3, total - 2, total - 1, total)
  } else {
    pages.push(1, '...', cur - 1, cur, cur + 1, '...', total)
  }
  return pages
})

// --- Hiệu ứng "đếm số" cho giá khi card xuất hiện ---
// LƯU Ý: khai báo TRƯỚC syncFlashSaleProducts/watch bên dưới, vì watch chạy
// { immediate: true } ngay lúc setup() — nếu store đã có sẵn dữ liệu (cache từ
// lần ghé trang trước), watcher sẽ gọi animateCountUp ngay lập tức. Nếu hàm này
// khai báo bằng const ở PHÍA DƯỚI, sẽ dính lỗi TDZ (Cannot access before initialization).
const animateCountUp = (product, delayMs = 0) => {
  const duration = 900
  const to = product.price
  let startTs = null

  const step = (now) => {
    if (startTs === null) startTs = now + delayMs
    if (now < startTs) {
      requestAnimationFrame(step)
      return
    }
    const elapsed = now - startTs
    const t = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - t, 3)
    product.displayPrice = Math.round(to * eased)
    if (t < 1) requestAnimationFrame(step)
    else product.displayPrice = to
  }
  requestAnimationFrame(step)
}

// ==== Danh sách sản phẩm hiển thị thật (reactive, giữ state UI qua các lần refetch) ====
// Đây KHÔNG phải computed nữa. Mỗi khi rawFlashSaleItems đổi, ta merge thủ công vào
// mảng này: sản phẩm cũ giữ nguyên displayPrice/ripples/addingToCart,
// chỉ cập nhật field đến từ BE (giá, tồn kho, discount...). Sản phẩm mới thì thêm vào
// với displayPrice=0 rồi animate count-up. Sản phẩm không còn trong raw thì bị loại bỏ.
const flashSaleProducts = ref([])

function syncFlashSaleProducts() {
  const rawList = paginatedRawItems.value
  const existingMap = new Map(flashSaleProducts.value.map((p) => [p.id, p]))

  const newlyAdded = []

  const merged = rawList.map((raw) => {
    const existing = existingMap.get(raw.id)
    if (existing) {
      // Sản phẩm đã có từ trước -> cập nhật field từ BE, giữ nguyên state UI
      Object.assign(existing, {
        name: raw.name,
        slug: raw.slug,
        spec: raw.spec,
        image: raw.image,
        price: raw.price,
        originalPrice: raw.originalPrice,
        discount: raw.discount,
        soldPercent: raw.soldPercent,
        left: raw.left,
        variants: raw.variants,
        promos: raw.promos,
        slotId: raw.slotId,
        slotName: raw.slotName,
      })
      return existing
    }

    // Sản phẩm mới -> khởi tạo state UI mặc định
    const created = reactive({
      ...raw,
      displayPrice: 0,
      ripples: [],
      addingToCart: false,
    })
    newlyAdded.push(created)
    return created
  })

  flashSaleProducts.value = merged

  // Chỉ animate count-up cho sản phẩm mới xuất hiện (lần đầu load, hoặc slot mới)
  newlyAdded.forEach((product, idx) => {
    animateCountUp(product, 100 + idx * 80)
  })
}

// Chạy sync mỗi khi dữ liệu slot đổi HOẶC khi người dùng chuyển trang (kể cả lần đầu)
watch([rawFlashSaleItems, currentPage], syncFlashSaleProducts, { immediate: true })

// formatPrice
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

const goToProduct = (product) => {
  if (!ensureProductSlotIsBuyable(product)) return
  addToCart(product)
}

// --- Hiệu ứng ripple khi bấm "Mua ngay" ---
const handleBuyClick = (event, product) => {
  if (product.left <= 0) return
  const btn = event.currentTarget
  const rect = btn.getBoundingClientRect()
  const ripple = {
    id: Date.now() + Math.random(),
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  }
  product.ripples.push(ripple)
  setTimeout(() => {
    product.ripples = product.ripples.filter((r) => r.id !== ripple.id)
  }, 650)

  buyNow(product)
}

function prepareCheckoutSelection(cartItem, product, ownSlot) {
  const item = {
    cartItemId: cartItem.cartItemId,
    productName: cartItem.name || product.name,
    variantName: cartItem.variant || '',
    skuCode: cartItem.skuCode || product.spec?.replace('Mã: ', '') || '',
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
  if (product.addingToCart) return
  product.addingToCart = true

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
        console.warn('[buyNow] fetchCart thất bại, tiếp tục với items hiện có:', e)
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
        // Nếu lỗi này đến từ 401 (guest chưa đăng nhập), modal LoginRequiredModal
        // đã hiển thị thông báo rồi -> không cần show thêm toast lỗi trùng lặp.
        if (!suppressErrorToast.value && !isUnauthorizedError(cartStore.error)) {
          showToast({
            type: 'error',
            title: 'Lỗi!',
            message: cartStore.error || 'Không thể thêm vào giỏ',
          })
        }
        suppressErrorToast.value = false
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
    // Tương tự: nếu là lỗi 401 đã được xử lý bằng modal đăng nhập, không show toast lỗi nữa.
    if (!suppressErrorToast.value && !isUnauthorizedError(error)) {
      showToast({
        type: 'error',
        title: 'Lỗi!',
        message: error.response?.data?.message || 'Không thể mua ngay',
      })
    }
    suppressErrorToast.value = false
  } finally {
    setTimeout(() => {
      product.addingToCart = false
    }, 700)
  }
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

const addToCart = async (product) => {
  if (product.addingToCart) return
  product.addingToCart = true

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

    const success = await cartStore.addToCartWithFlashSale(
      product.skuId,
      1,
      ownSlot.slotId,
      product.price
    )

    if (success) {
      showToast({
        type: 'success',
        title: 'Thành công!',
        message: 'Đã thêm vào giỏ hàng',
      })
    } else {
      // Nếu lỗi này đến từ 401 (guest chưa đăng nhập), modal LoginRequiredModal
      // đã hiển thị thông báo rồi -> không cần show thêm toast lỗi trùng lặp.
      if (!suppressErrorToast.value && !isUnauthorizedError(cartStore.error)) {
        showToast({
          type: 'error',
          title: 'Lỗi!',
          message: cartStore.error || 'Không thể thêm vào giỏ',
        })
      }
      suppressErrorToast.value = false
    }
  } catch (error) {
    console.error('Lỗi thêm vào giỏ:', error)
    // Tương tự: nếu là lỗi 401 đã được xử lý bằng modal đăng nhập, không show toast lỗi nữa.
    if (!suppressErrorToast.value && !isUnauthorizedError(error)) {
      showToast({
        type: 'error',
        title: 'Lỗi!',
        message: error.response?.data?.message || 'Không thể thêm vào giỏ hàng',
      })
    }
    suppressErrorToast.value = false
  } finally {
    setTimeout(() => {
      product.addingToCart = false
    }, 700)
  }
}

// FOMO popup
const fomoVisible = ref(false)
const fomoMessage = ref('')
const fomoSub = ref('')
const fomoKey = ref(0)
const fomoMessages = [
  ['Anh T. vừa mua iPhone 15 Pro Max', 'TP. Hồ Chí Minh · 1 phút trước'],
  ['Chỉ còn 3 chiếc Loa JBL Flip 6 cuối cùng!', 'Kho Hà Nội · vừa xong'],
  ['Chị H. vừa đặt Samsung Galaxy S24 Ultra', 'Đà Nẵng · 3 phút trước'],
  ['Apple Watch Series 9 sắp hết size 45mm', 'Kho Hà Nội · vừa xong'],
  ['Anh K. vừa mua AirPods Pro 2', 'Hải Phòng · 4 phút trước'],
]
let fomoIdx = 0
let fomoTimer = null

const showNextFomo = () => {
  const [msg, sub] = fomoMessages[fomoIdx % fomoMessages.length]
  fomoMessage.value = msg
  fomoSub.value = sub
  fomoVisible.value = true
  fomoKey.value++
  fomoIdx++
  setTimeout(() => { fomoVisible.value = false }, 4200)
}

const closeFomo = () => {
  fomoVisible.value = false
  clearInterval(fomoTimer)
}

// Special Effects: Falling Dots
const fallingDotsContainer = ref(null)

const createFallingDots = () => {
  const container = fallingDotsContainer.value
  if (!container) {
    console.log('Container not found')
    return
  }

  const isMobile = window.innerWidth < 640
  const count = isMobile ? 60 : 120
  const fallDistance = container.offsetHeight || 340

  container.innerHTML = ''

  for (let i = 0; i < count; i++) {
    const dot = document.createElement('div')
    dot.classList.add('falling-dot')

    const x = Math.random() * 100
    const size = 3 + Math.random() * 8
    const duration = 2.5 + Math.random() * 4
    const delay = Math.random() * 6
    const isWhite = Math.random() > 0.4

    dot.style.position = 'absolute'
    dot.style.left = `${x}%`
    dot.style.top = '-20px'
    dot.style.width = `${size}px`
    dot.style.height = `${size}px`
    dot.style.background = isWhite ? '#fff' : '#fbbf24'
    dot.style.borderRadius = '50%'
    dot.style.setProperty('--fall-distance', `${fallDistance}px`)
    dot.style.animationName = 'fallDown'
    dot.style.animationDuration = `${duration}s`
    dot.style.animationDelay = `${delay}s`
    dot.style.animationTimingFunction = 'linear'
    dot.style.animationIterationCount = 'infinite'
    dot.style.boxShadow = `0 0 ${size}px ${isWhite ? 'rgba(255,255,255,0.9)' : 'rgba(251,191,36,0.9)'}`
    dot.style.opacity = '0'

    container.appendChild(dot)
  }
  console.log('Created', count, 'falling dots')
}

let resizeHandler = null
let refreshTimer = null

// ==== Countdown đếm ngược cho màn chờ (slot SCHEDULED) ====
// Chỉ chạy khi user đang chọn 1 slot chưa LIVE — đếm tới startDate của slot đó.
// Khi startDate trôi qua → gọi fetchFlashSales() để store đồng bộ lại (slot sẽ chuyển sang ACTIVE).
const waitingTimer = reactive({
  hours: '00',
  minutes: '00',
  seconds: '00',
})

function pad2(n) {
  return String(Math.max(0, Math.floor(n))).padStart(2, '0')
}

function tickWaiting() {
  const slot = selectedSlot.value
  if (!slot || !slot.startDate) return
  const diff = new Date(slot.startDate).getTime() - Date.now()
  if (diff <= 0) {
    // Đã tới giờ → reload dữ liệu để slot chuyển sang ACTIVE.
    fetchFlashSales()
    return
  }
  const totalSec = Math.floor(diff / 1000)
  const h = Math.floor(totalSec / 3600)
  const m = Math.floor((totalSec % 3600) / 60)
  const s = totalSec % 60
  waitingTimer.hours = pad2(h)
  waitingTimer.minutes = pad2(m)
  waitingTimer.seconds = pad2(s)
}

let waitingTimerId = null
watch(
  selectedSlot,
  (slot) => {
    if (waitingTimerId) {
      clearInterval(waitingTimerId)
      waitingTimerId = null
    }
    // Chỉ chạy countdown khi slot được chọn chưa LIVE (đang ở màn chờ).
    if (slot && !isSelectedSlotLive.value) {
      tickWaiting()
      waitingTimerId = setInterval(tickWaiting, 1000)
    }
  },
  { immediate: true }
)

onMounted(async () => {
  // Lắng nghe event auth-required (khi guest nhận 401 từ API)
  window.addEventListener('auth-required', handleAuthRequiredPage)

  // Tải dữ liệu Flash Sale ACTIVE + sắp diễn ra từ BE
  // (watch(rawFlashSaleItems, ..., { immediate: true }) sẽ tự lo phần merge + animate)
  await fetchFlashSales()
  createFallingDots()

  resizeHandler = () => createFallingDots()
  window.addEventListener('resize', resizeHandler)

  setTimeout(showNextFomo, 1800)
  fomoTimer = setInterval(showNextFomo, 6000)

  // Refresh dữ liệu mỗi 30s để cập nhật status slot (cancel/restore từ admin).
  refreshTimer = setInterval(() => {
    flashSaleStore.fetchClientSlots(20).catch(() => {})
  }, 30000)
})

onUnmounted(() => {
  clearInterval(fomoTimer)
  if (refreshTimer) clearInterval(refreshTimer)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (waitingTimerId) clearInterval(waitingTimerId)
})
</script>
