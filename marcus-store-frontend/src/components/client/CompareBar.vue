<template>
  <Teleport to="body">
    <!-- ========== THANH NỔI DƯỚI CÙNG ========== -->
    <Transition name="compare-slide">
      <div v-if="visible" class="compare-bar">
        <div class="compare-bar-inner">
          <div class="compare-bar-info">
            <strong>So sánh sản phẩm</strong>
            <span class="compare-bar-count">({{ state.items.length }}/{{ MAX_ITEMS }})</span>
          </div>

          <div class="compare-bar-list">
            <div
              v-for="item in state.items"
              :key="item.productId"
              class="compare-bar-item"
            >
              <img
                v-if="item.thumbnailUrl"
                :src="item.thumbnailUrl"
                :alt="item.productName"
                class="compare-bar-thumb"
              />
              <div v-else class="compare-bar-thumb compare-bar-thumb--placeholder">
                <i class="fa-solid fa-mobile-screen"></i>
              </div>
              <div class="compare-bar-name">{{ item.productName }}</div>
            </div>

            <div
              v-for="n in Math.max(0, MAX_ITEMS - state.items.length)"
              :key="`empty-${n}`"
              class="compare-bar-item compare-bar-item--empty"
            >
              <div class="compare-bar-thumb compare-bar-thumb--placeholder">
                <i class="fa-solid fa-plus"></i>
              </div>
              <div class="compare-bar-name">Chọn sản phẩm</div>
            </div>
          </div>

          <div class="compare-bar-actions">
            <button
              type="button"
              class="btn-cancel"
              @click="cancelCompare"
            >
              Hủy so sánh
            </button>

            <button
              type="button"
              class="btn-compare"
              :disabled="!canCompare || loading"
              @click="doCompare"
            >
              <i v-if="loading" class="fa-solid fa-spinner fa-spin"></i>
              <i v-else ></i>
              {{ loading ? 'Đang phân tích...' : 'So sánh' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ========== MODAL KẾT QUẢ SO SÁNH ========== -->
    <Transition name="modal-fade">
      <div v-if="resultVisible" class="compare-modal-backdrop" @click.self="closeResult">
        <div class="compare-modal" role="dialog">
          <header class="compare-modal-header">
            <h5 class="compare-modal-title">
              <i class="fa-solid fa-not-equal"></i>
              So sánh sản phẩm
            </h5>
            <button
              type="button"
              class="compare-modal-close"
              title="Đóng"
              @click="closeResult"
            >
              ×
            </button>
          </header>

          <!-- Gắn biến CSS --cmp-cols theo đúng số sản phẩm đang so (2 hoặc 3)
               để bảng overview + bảng specs luôn chia cột đều nhau. -->
          <div
            class="compare-modal-body"
            v-if="state.resultProducts?.length"
            :style="{ '--cmp-cols': state.resultProducts.length }"
          >
            <!-- Tổng quan -->
            <section class="cmp-section cmp-overview">
              <div class="cmp-overview-grid">
                <div
                  v-for="p in state.resultProducts"
                  :key="p.productId"
                  class="cmp-overview-col"
                >
                  <router-link
                    v-if="p.slug"
                    :to="`/product/${p.slug}`"
                    class="cmp-overview-link"
                  >
                    <img
                      v-if="p.thumbnailUrl"
                      :src="p.thumbnailUrl"
                      :alt="p.productName"
                      class="cmp-overview-thumb"
                    />
                  </router-link>
                  <div class="cmp-overview-name">{{ p.productName }}</div>
                  <div class="cmp-overview-price">{{ formatPrice(p.price) }}</div>

                  <!-- MỚI: nút thêm giỏ + mua ngay ngay trong bảng so sánh,
                       giúp kích cầu mua ngay sau khi khách xem xong so sánh -->
                  <div class="cmp-overview-cta">
                    <button
                      type="button"
                      class="cmp-btn cmp-btn--cart"
                      :disabled="ctaLoadingId === p.productId"
                      @click="onAddToCartFromCompare(p)"
                    >
                      <i class="fa-solid fa-cart-plus"></i>
                      Thêm giỏ
                    </button>
                    <button
                      type="button"
                      class="cmp-btn cmp-btn--buy"
                      :disabled="ctaLoadingId === p.productId"
                      @click="onBuyNowFromCompare(p)"
                    >
                      <i class="fa-solid fa-bolt"></i>
                      Mua ngay
                    </button>
                  </div>
                </div>
              </div>

              <div
                v-if="state.result?.overallWinner"
                class="cmp-overall-winner"
                :class="{ 'is-empty': state.result.overallWinner.includes('Không') || state.result.overallWinner.includes('Xem chi tiết') }"
              >
                <i class="fa-solid fa-trophy"></i>
                <div>
                  <div class="cmp-overall-label">Tổng thể tốt nhất</div>
                  <div class="cmp-overall-name">{{ state.result.overallWinner }}</div>
                  <div v-if="state.result.overallReason" class="cmp-overall-reason">
                    {{ state.result.overallReason }}
                  </div>
                </div>
              </div>
            </section>

            <!-- Bảng thông số kỹ thuật -->
            <section class="cmp-section">
              <h6 class="cmp-section-title">
                <i class="fa-solid fa-list-check"></i>
                Thông số kỹ thuật
              </h6>

              <div class="cmp-specs-table">
                <div class="cmp-specs-row cmp-specs-head">
                  <div class="cmp-spec-label"></div>
                  <div
                    v-for="p in state.resultProducts"
                    :key="`head-${p.productId}`"
                    class="cmp-spec-value cmp-spec-value--head"
                  >
                    {{ p.productName }}
                  </div>
                </div>

                <div
                  v-for="row in specRows"
                  :key="row.specName"
                  class="cmp-specs-row"
                  :class="{ 'is-different': row.isDifferent }"
                >
                  <div class="cmp-spec-label">{{ row.specName }}</div>
                  <div
                    v-for="cell in row.cells"
                    :key="`${row.specName}-${cell.productId}`"
                    class="cmp-spec-value"
                    :class="{ 'is-better': cell.isBetter }"
                  >
                    <span v-if="cell.value" :class="{ 'text-muted': !cell.isBetter && row.isDifferent }">
                      {{ cell.value }}
                    </span>
                    <span v-else class="text-muted">—</span>
                    <i
                      v-if="cell.isBetter"
                      class="fa-solid fa-arrow-up cmp-better-icon"
                      title="Vượt trội hơn"
                    ></i>
                  </div>
                </div>
              </div>
            </section>

            <!-- Đánh giá theo nhu cầu -->
            <section v-if="state.result?.useCases?.length" class="cmp-section">
              <h6 class="cmp-section-title">
                <i class="fa-solid fa-robot"></i>
                Đánh giá theo nhu cầu (AI)
              </h6>
              <div class="cmp-usecase-list">
                <div
                  v-for="(uc, idx) in state.result.useCases"
                  :key="idx"
                  class="cmp-usecase-card"
                >
                  <div class="cmp-usecase-head">
                    <span class="cmp-usecase-icon">
                      <i :class="getUsecaseIcon(uc.useCase)"></i>
                    </span>
                    <span class="cmp-usecase-name">{{ uc.useCase }}</span>
                  </div>
                  <div class="cmp-usecase-winner">
                    <i class="fa-solid fa-medal"></i>
                    <strong>{{ uc.winner || '—' }}</strong>
                  </div>
                  <div class="cmp-usecase-reason">{{ uc.reason }}</div>
                </div>
              </div>
            </section>
          </div>

          <div v-else-if="loading" class="compare-modal-body text-center text-muted py-5">
            <i class="fa-solid fa-spinner fa-spin fa-2x"></i>
            <p class="mt-3 mb-0">AI đang phân tích thông số...</p>
          </div>

          <footer class="compare-modal-footer">
            <button type="button" class="btn btn-outline-secondary" @click="closeResult">
              Đóng
            </button>
            <button type="button" class="btn btn-danger" @click="resetAll">
              <i class="fa-solid fa-trash"></i>
              Xóa so sánh
            </button>
          </footer>
        </div>
      </div>
    </Transition>

    <!-- Notification Modal (kết quả thêm giỏ / mua ngay) -->
    <BaseModal
      :visible="notifyModal.visible"
      :type="notifyModal.type"
      :title="notifyModal.title"
      :message="notifyModal.message"
      @close="notifyModal.visible = false"
    />

    <!-- Login Required Modal -->
    <LoginRequiredModal
      :visible="loginModal.visible"
      :title="loginModal.title"
      :message="loginModal.message"
      @close="loginModal.visible = false"
    />
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCompareBar } from '@/composables/useCompareBar'
import { useCartStore } from '@/stores/cartStore'
import api from '@/utils/api'
import BaseModal from '@/components/BaseModal.vue'
import LoginRequiredModal from '@/components/LoginRequiredModal.vue'

const {
  state,
  canCompare,
  removeFromCompare,
  clearCompare,
  MAX_ITEMS,
} = useCompareBar()

const router = useRouter()
const cartStore = useCartStore()

const visible = computed(() => state.items.length > 0 || loading.value)
const loading = ref(false)
const resultVisible = ref(false)

// ----- MỚI: thêm giỏ hàng / mua ngay ngay trong modal so sánh -----
const ctaLoadingId = ref(null)

const notifyModal = reactive({
  visible: false,
  type: 'info', // 'success' | 'error' | 'info'
  title: 'Thông báo',
  message: '',
})

const loginModal = reactive({
  visible: false,
  title: '',
  message: '',
})

function isLoggedIn() {
  return !!localStorage.getItem('ACCESS_TOKEN')
}

function openLoginModal(title, message) {
  loginModal.title = title
  loginModal.message = message
  loginModal.visible = true
}

function showNotify(type, title, message) {
  notifyModal.type = type
  notifyModal.title = title
  notifyModal.message = message
  notifyModal.visible = true
}

async function onAddToCartFromCompare(product) {
  if (!isLoggedIn()) {
    openLoginModal(
      'Thêm vào giỏ hàng',
      'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng và tiến hành thanh toán.',
    )
    return
  }
  // Response /client/compare trả defaultSkuId (BE mới thêm). Nếu vì lý do gì
  // đó không có (sản phẩm hết hàng mọi SKU / dữ liệu cũ chưa migrate) thì
  // điều hướng sang trang chi tiết để khách tự chọn variant, thay vì im lặng.
  if (!product.defaultSkuId) {
    if (product.slug) {
      router.push(`/product/${product.slug}`)
    } else {
      showNotify('info', 'Sản phẩm tạm hết hàng', 'Vui lòng chọn sản phẩm khác.')
    }
    return
  }

  ctaLoadingId.value = product.productId
  try {
    const ok = await cartStore.addToCart(product.defaultSkuId, 1)
    if (ok) {
      showNotify('success', 'Thêm vào giỏ hàng', `Đã thêm "${product.productName}" vào giỏ hàng`)
    } else {
      showNotify('error', 'Thêm thất bại', cartStore.error || 'Thêm vào giỏ hàng thất bại')
    }
  } finally {
    ctaLoadingId.value = null
  }
}

async function onBuyNowFromCompare(product) {
  if (!isLoggedIn()) {
    openLoginModal(
      'Mua ngay',
      'Vui lòng đăng nhập để mua sản phẩm này.',
    )
    return
  }
  if (!product.defaultSkuId) {
    if (product.slug) {
      router.push(`/product/${product.slug}`)
    } else {
      showNotify('info', 'Sản phẩm tạm hết hàng', 'Vui lòng chọn sản phẩm khác.')
    }
    return
  }

  ctaLoadingId.value = product.productId
  try {
    const ok = await cartStore.addToCart(product.defaultSkuId, 1)
    if (!ok) {
      showNotify('error', 'Mua thất bại', cartStore.error || 'Không thể tiến hành mua ngay.')
      return
    }
    // Giống logic Buy Now ở trang chi tiết sản phẩm: chốt đúng cartItemId + quantity
    // vừa thêm để checkout không lấy nhầm số lượng có sẵn trong giỏ.
    const addedItem = cartStore.items.find((i) => i.skuId === product.defaultSkuId)
    const cartItemId = addedItem?.cartItemId
    if (cartItemId) {
      sessionStorage.setItem('buyNowCartItemIds', JSON.stringify([cartItemId]))
      sessionStorage.setItem(
        'buyNowCartItemQuantities',
        JSON.stringify({ [cartItemId]: 1 }),
      )
    }
    closeResult()
    router.push('/checkout')
  } finally {
    ctaLoadingId.value = null
  }
}

// ----- Build bảng thông số: mỗi specName là 1 hàng, mỗi sản phẩm là 1 cột -----
const specRows = computed(() => {
  if (!state.resultProducts?.length) return []
  const products = state.resultProducts

  // Map specName -> winnerProductId do AI (Gemini) xác định.
  // Ưu tiên dùng cái này thay vì tự đoán bằng số, vì AI hiểu đúng chiều
  // tốt/xấu của từng loại spec (VD: trọng lượng thấp hơn mới tốt).
  const aiWinnerMap = new Map()
  for (const sw of state.result?.specWinners || []) {
    aiWinnerMap.set(sw.specName, sw.winnerProductId)
  }

  const allSpecs = new Map()
  for (const p of products) {
    for (const spec of p.specs || []) {
      if (!allSpecs.has(spec.specName)) {
        allSpecs.set(spec.specName, {})
      }
      allSpecs.get(spec.specName)[p.productId] = spec
    }
  }
  const rows = []
  for (const [specName, byProduct] of allSpecs.entries()) {
    const values = products.map((p) => {
      const s = byProduct[p.productId]
      let val = ''
      if (s) {
        val = s.specValue || ''
        if (s.unit && s.unit.trim()) val += ' ' + s.unit
      }
      return { productId: p.productId, value: val.trim() }
    })
    const distinctValues = new Set(values.map((v) => v.value))
    const isDifferent = distinctValues.size > 1
    const cells = values.map((v) => ({
      productId: v.productId,
      value: v.value,
      isBetter: false,
    }))

    if (isDifferent) {
      if (aiWinnerMap.has(specName)) {
        const winnerId = aiWinnerMap.get(specName)
        cells.forEach((c) => {
          if (c.productId === winnerId) c.isBetter = true
        })
      } else {
        const numeric = values.map((v) => parseNumeric(v.value))
        if (numeric.every((n) => n != null)) {
          const max = Math.max(...numeric)
          numeric.forEach((n, idx) => {
            if (n === max) cells[idx].isBetter = true
          })
        }
      }
    }
    rows.push({ specName, cells, isDifferent })
  }
  return rows
})

function parseNumeric(value) {
  if (!value) return null
  const match = String(value).match(/(\d+(?:[.,]\d+)?)/)
  if (!match) return null
  return parseFloat(match[1].replace(',', '.'))
}

function getUsecaseIcon(name) {
  if (!name) return 'fa-solid fa-circle'
  const n = name.toLowerCase()
  if (n.includes('gaming') || n.includes('hiệu năng')) return 'fa-solid fa-gamepad'
  if (n.includes('chụp') || n.includes('ảnh')) return 'fa-solid fa-camera'
  if (n.includes('pin')) return 'fa-solid fa-battery-full'
  if (n.includes('giá')) return 'fa-solid fa-tag'
  return 'fa-solid fa-circle'
}

function formatPrice(value) {
  if (value == null) return ''
  return new Intl.NumberFormat('vi-VN').format(value) + 'đ'
}

// ----- Hành động -----
async function doCompare() {
  if (!canCompare.value) return
  loading.value = true
  state.error = null
  try {
    const productIds = state.items.map((p) => p.productId)
    const { data } = await api.post('/client/compare', { productIds })
    state.result = data?.data?.result || null
    state.resultProducts = data?.data?.products || []
    resultVisible.value = true
  } catch (e) {
    console.error('compare error', e)
    state.error = 'Không thể so sánh lúc này. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

function cancelCompare() {
  clearCompare()
}

function closeResult() {
  resultVisible.value = false
}

function resetAll() {
  clearCompare()
  resultVisible.value = false
}
</script>

<style scoped>
/* ========== THANH SO SÁNH ========== */
.compare-bar {
  position: fixed;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1050;
  width: min(960px, calc(100vw - 24px));
}

.compare-bar-inner {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  padding: 14px 18px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 18px;
  align-items: center;
}

.compare-bar-info {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
}
.compare-bar-count {
  color: #999;
  margin-left: 4px;
  font-weight: 500;
}

.compare-bar-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
}

.compare-bar-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f7f7f7;
  border: 1px solid #eee;
  border-radius: 10px;
  min-width: 180px;
  max-width: 220px;
}
.compare-bar-item--empty {
  border-style: dashed;
  background: transparent;
  opacity: 0.7;
}

.compare-bar-thumb {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.compare-bar-thumb img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.compare-bar-thumb--placeholder {
  color: #bbb;
  font-size: 18px;
}

.compare-bar-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}

.compare-bar-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: #d70018;
  color: #fff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.compare-bar-actions {
  display: flex;
  gap: 8px;
  white-space: nowrap;
}

.btn-cancel,
.btn-compare {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s ease;
}

.btn-cancel {
  background: #fff;
  border-color: #ddd;
  color: #555;
}
.btn-cancel:hover {
  border-color: #bbb;
  background: #f8f8f8;
}

.btn-compare {
  background: #d70018;
  color: #fff;
  border-color: #d70018;
}
.btn-compare:hover:not(:disabled) {
  background: #b80015;
  border-color: #b80015;
}
.btn-compare:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Slide animation */
.compare-slide-enter-active,
.compare-slide-leave-active {
  transition: all 0.25s ease;
}
.compare-slide-enter-from,
.compare-slide-leave-to {
  opacity: 0;
  transform: translate(-50%, 30px);
}

/* ========== MODAL ========== */
.compare-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1060;
  padding: 16px;
}

.compare-modal {
  background: #fff;
  border-radius: 14px;
  width: min(960px, 100%);
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.compare-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}
.compare-modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}
.compare-modal-title i {
  color: #d70018;
}
.compare-modal-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: #f1f1f1;
  font-size: 20px;
  color: #555;
  cursor: pointer;
}
.compare-modal-close:hover {
  background: #e5e5e5;
}

.compare-modal-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.compare-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #eee;
  background: #fafafa;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

/* ========== OVERVIEW ========== */
.cmp-overview {
  margin-bottom: 24px;
}
.cmp-overview-grid {
  display: grid;
  grid-template-columns: repeat(var(--cmp-cols, 2), 1fr);
  gap: 12px;
  margin-bottom: 14px;
}
.cmp-overview-col {
  text-align: center;
  padding: 12px;
  background: #f8f8f8;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
}
.cmp-overview-link {
  display: block;
}
.cmp-overview-thumb {
  width: 110px;
  height: 110px;
  object-fit: contain;
  margin: 0 auto 8px;
}
.cmp-overview-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  min-height: 38px;
}
.cmp-overview-price {
  color: #d70018;
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 10px;
}

/* MỚI: nút thêm giỏ / mua ngay trong overview */
.cmp-overview-cta {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.cmp-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 34px;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s ease;
}
.cmp-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.cmp-btn--cart {
  background: #fff;
  border-color: #d70018;
  color: #d70018;
}
.cmp-btn--cart:hover:not(:disabled) {
  background: #fff0f0;
}
.cmp-btn--buy {
  background: #d70018;
  border-color: #d70018;
  color: #fff;
}
.cmp-btn--buy:hover:not(:disabled) {
  background: #b80015;
}

.cmp-overall-winner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #fff7e6, #fff1cf);
  border: 1px solid #ffd591;
  border-radius: 10px;
}
.cmp-overall-winner.is-empty {
  background: #f5f5f5;
  border-color: #e5e5e5;
}
.cmp-overall-winner.is-empty > i {
  color: #999;
}
.cmp-overall-winner > i {
  color: #ff8800;
  font-size: 22px;
  margin-top: 2px;
}
.cmp-overall-label {
  font-size: 12px;
  color: #ad6800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
}
.cmp-overall-name {
  font-size: 16px;
  font-weight: 700;
  color: #333;
  margin: 2px 0 4px;
}
.cmp-overall-reason {
  font-size: 13px;
  color: #555;
  line-height: 1.4;
}

/* ========== SECTION ========== */
.cmp-section {
  margin-bottom: 24px;
}
.cmp-section:last-child {
  margin-bottom: 0;
}
.cmp-section-title {
  font-size: 15px;
  font-weight: 700;
  color: #333;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.cmp-section-title i {
  color: #d70018;
}

/* ========== BẢNG SPECS ========== */
.cmp-specs-table {
  border: 1px solid #eee;
  border-radius: 10px;
  overflow: hidden;
}
.cmp-specs-row {
  display: grid;
  grid-template-columns: 160px repeat(var(--cmp-cols, 2), 1fr);
  gap: 0;
  border-bottom: 1px solid #f1f1f1;
}
.cmp-specs-row:last-child {
  border-bottom: none;
}
.cmp-specs-head {
  background: #fafafa;
  font-weight: 700;
}
.cmp-specs-row.is-different {
  background: #fffaf0;
}
.cmp-specs-row.is-different:nth-child(odd) {
  background: #fff5e6;
}

.cmp-spec-label {
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #555;
  background: #f7f7f7;
  border-right: 1px solid #eee;
}
.cmp-specs-head .cmp-spec-label {
  background: #fafafa;
}

.cmp-spec-value {
  padding: 10px 14px;
  font-size: 13px;
  color: #333;
  border-right: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 6px;
}
.cmp-spec-value:last-child {
  border-right: none;
}
.cmp-spec-value--head {
  font-weight: 700;
  color: #333;
  font-size: 13px;
}
.cmp-spec-value.is-better {
  font-weight: 700;
  color: #1f8a3e;
  background: rgba(31, 138, 62, 0.06);
}
.cmp-better-icon {
  color: #1f8a3e;
  font-size: 11px;
}

/* ========== USE CASE ==========
   Luôn có đúng 4 mục (Gaming / Camera / Pin / Giá trị) -> fix cứng 4 cột
   1 hàng cho khoa học, thay vì auto-fit dễ vỡ dòng khi label dài. */
.cmp-usecase-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.cmp-usecase-card {
  padding: 14px;
  background: #f8f8f8;
  border-radius: 10px;
  border: 1px solid #eee;
  display: flex;
  flex-direction: column;
}
.cmp-usecase-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.cmp-usecase-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #d70018;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cmp-usecase-name {
  font-weight: 700;
  font-size: 13px;
  color: #333;
}
.cmp-usecase-winner {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  color: #333;
  font-size: 13px;
}
.cmp-usecase-winner i {
  color: #d70018;
}
.cmp-usecase-reason {
  font-size: 12.5px;
  color: #666;
  line-height: 1.45;
}

/* ========== RESPONSIVE ========== */
@media (max-width: 768px) {
  .compare-bar-inner {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .compare-bar-info {
    text-align: center;
  }
  .compare-bar-list {
    justify-content: center;
  }
  .compare-bar-actions {
    justify-content: center;
  }
  .cmp-specs-row {
    grid-template-columns: 110px repeat(var(--cmp-cols, 2), 1fr);
    font-size: 12px;
  }
  .cmp-overview-grid {
    grid-template-columns: 1fr;
  }
  /* Màn nhỏ: 4 cột use-case không đủ chỗ -> xuống 2 cột x 2 hàng cho dễ đọc */
  .cmp-usecase-list {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>