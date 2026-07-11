<template>
  <div
    v-if="visible"
    class="modal-overlay"
    role="dialog"
    aria-modal="true"
    @click.self="onClose"
  >
    <div class="voucher-modal fs-detail-modal">
      <!-- ====== HEADER ====== -->
      <header class="voucher-modal-header fs-detail-header">
        <div class="fs-detail-title-wrap">
          <i class="bi bi-bar-chart-line-fill"></i>
          <div>
            <h2 class="voucher-modal-title fs-detail-title">Thống kê Flash Sale</h2>
            <p class="fs-detail-subtitle">
              Xem chi tiết các sản phẩm áp dụng và tiến độ bán hàng
            </p>
          </div>
        </div>
        <button class="close-btn fs-close-btn" aria-label="Đóng" @click="onClose">
          <i class="bi bi-x-lg"></i>
        </button>
      </header>

      <!-- ====== BODY (scrollable) ====== -->
      <div class="voucher-modal-body fs-detail-body">
        <!-- Loading -->
        <div v-if="loading" class="fs-detail-loading">
          <div class="spinner-border text-primary" role="status"></div>
          <span>Đang tải dữ liệu...</span>
        </div>

        <!-- Error -->
        <div v-else-if="error" class="fs-detail-error">
          <i class="bi bi-exclamation-triangle-fill"></i>
          <span>{{ error }}</span>
        </div>

        <!-- Content -->
        <template v-else-if="slot">
          <!-- ====== KHỐI 1: Thông tin chiến dịch ====== -->
          <section class="fs-block">
            <div class="fs-block-head">
              <div class="fs-step-icon">1</div>
              <div>
                <h3 class="fs-block-title">Thông tin chiến dịch</h3>
                <p class="fs-block-desc">
                  Thời gian diễn ra và các thông tin cơ bản của chương trình.
                </p>
              </div>
            </div>

            <div class="fs-block-body">
              <div class="fs-grid-2">
                <!-- Tên -->
                <div class="fs-field fs-field-full">
                  <label class="fs-label">Tên chiến dịch</label>
                  <div class="fs-readonly-box">{{ slot.name }}</div>
                </div>

                <!-- Bắt đầu -->
                <div class="fs-field">
                  <label class="fs-label">Thời gian bắt đầu</label>
                  <div class="fs-readonly-box">
                    <i class="bi bi-calendar-event"></i>
                    <span>{{ formatDate(slot.startDate) }}</span>
                  </div>
                </div>

                <!-- Kết thúc -->
                <div class="fs-field">
                  <label class="fs-label">Thời gian kết thúc</label>
                  <div class="fs-readonly-box">
                    <i class="bi bi-calendar-event"></i>
                    <span>{{ formatDate(slot.endDate) }}</span>
                  </div>
                </div>

                <!-- Trạng thái + Mã slot -->
                <div class="fs-field">
                  <label class="fs-label">Trạng thái</label>
                  <div class="fs-readonly-box">
                    <span :class="['fs-status-badge', statusClass]">
                      {{ statusText }}
                    </span>
                  </div>
                </div>

                <div class="fs-field">
                  <label class="fs-label">Mã slot</label>
                  <div class="fs-readonly-box">
                    <span class="fs-slot-id">#{{ slot.slotId }}</span>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- ====== KHỐI 2: Thống kê tổng quan ====== -->
          <section class="fs-block">
            <div class="fs-block-head">
              <div class="fs-step-icon">2</div>
              <div>
                <h3 class="fs-block-title">Thống kê tổng quan</h3>
                <p class="fs-block-desc">
                  Số liệu tổng hợp về lượt mua và doanh thu của chiến dịch.
                </p>
              </div>
            </div>

            <div class="fs-block-body">
              <div class="fs-stats-grid">
                <div class="fs-stat-card">
                  <div class="fs-stat-icon fs-stat-icon-blue">
                    <i class="bi bi-people-fill"></i>
                  </div>
                  <div class="fs-stat-info">
                    <div class="fs-stat-value">{{ stats.buyerCount }}</div>
                    <div class="fs-stat-label">Người mua</div>
                  </div>
                </div>

                <div class="fs-stat-card">
                  <div class="fs-stat-icon fs-stat-icon-green">
                    <i class="bi bi-box-seam-fill"></i>
                  </div>
                  <div class="fs-stat-info">
                    <div class="fs-stat-value">
                      {{ stats.soldQuantity }}<span class="fs-stat-sub">/{{ stats.totalQuantity }}</span>
                    </div>
                    <div class="fs-stat-label">SP đã bán / tổng</div>
                  </div>
                </div>

                <div class="fs-stat-card">
                  <div class="fs-stat-icon fs-stat-icon-orange">
                    <i class="bi bi-cash-stack"></i>
                  </div>
                  <div class="fs-stat-info">
                    <div class="fs-stat-value">{{ formatCurrency(stats.revenue) }}</div>
                    <div class="fs-stat-label">Doanh thu</div>
                  </div>
                </div>

                <div class="fs-stat-card">
                  <div class="fs-stat-icon fs-stat-icon-purple">
                    <i class="bi bi-percent"></i>
                  </div>
                  <div class="fs-stat-info">
                    <div class="fs-stat-value">{{ stats.soldPercent }}%</div>
                    <div class="fs-stat-label">Tỷ lệ bán</div>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- ====== KHỐI 3: Banner & Banner status ====== -->
          <section v-if="slot.bannerImageUrl" class="fs-block">
            <div class="fs-block-head">
              <div class="fs-step-icon">3</div>
              <div>
                <h3 class="fs-block-title">Banner quảng cáo</h3>
                <p class="fs-block-desc">
                  Ảnh được overlay lên ảnh sản phẩm khi hiển thị ở trang chủ.
                </p>
              </div>
            </div>

            <div class="fs-block-body">
              <div class="fs-banner-frame">
                <img :src="slot.bannerImageUrl" alt="Banner" class="fs-banner-img" />
              </div>
            </div>
          </section>

          <!-- ====== KHỐI 4: Danh sách sản phẩm ====== -->
          <section class="fs-block">
            <div class="fs-block-head">
              <div class="fs-step-icon">{{ slot.bannerImageUrl ? '4' : '3' }}</div>
              <div>
                <h3 class="fs-block-title">
                  Danh sách sản phẩm
                  <span class="fs-block-count">({{ slot.items?.length || 0 }} sản phẩm)</span>
                </h3>
                <p class="fs-block-desc">
                  Tiến độ bán hàng của từng sản phẩm trong chiến dịch.
                </p>
              </div>
            </div>

            <div class="fs-block-body">
              <div
                v-if="!slot.items || slot.items.length === 0"
                class="fs-detail-empty"
              >
                <i class="bi bi-inbox"></i>
                <span>Chưa có sản phẩm nào trong chiến dịch này</span>
              </div>

              <div v-else class="fs-table-wrap">
                <table class="fs-detail-table">
                  <thead>
                    <tr>
                      <th style="width: 50px">#</th>
                      <th>Sản phẩm</th>
                      <th style="width: 200px">Giá Flash Sale</th>
                      <th style="width: 280px">Tiến độ bán hàng</th>
                      <th style="width: 100px" class="text-center">Còn lại</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(item, idx) in slot.items" :key="item.skuId">
                      <td class="text-center">{{ idx + 1 }}</td>
                      <td>
                        <div class="fs-product-cell">
                          <div class="fs-product-img">
                            <img
                              v-if="item.skuImageUrl"
                              :src="item.skuImageUrl"
                              :alt="item.productName"
                            />
                            <i v-else class="bi bi-image"></i>
                          </div>
                          <div class="fs-product-info">
                            <div class="fs-product-name">
                              {{ item.productName || '—' }}
                            </div>
                            <div class="fs-product-sku">
                              SKU: {{ item.skuCode || item.skuId }}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <div class="fs-price-cell">
                          <div class="fs-price-flash">
                            {{ formatCurrency(item.flashSalePrice) }}
                          </div>
                          <div class="fs-price-original">
                            <s>{{ formatCurrency(item.originalPrice) }}</s>
                            <span class="fs-discount-badge">
                              -{{ getDiscountPercent(item) }}%
                            </span>
                          </div>
                        </div>
                      </td>
                      <td>
                        <div class="fs-progress-cell">
                          <div class="fs-progress-text">
                            <strong>{{ item.soldQuantity || 0 }}</strong>
                            <span> / {{ item.flashSaleQuantity }} sản phẩm</span>
                          </div>
                          <div class="fs-progress-bar-wrap">
                            <div
                              class="fs-progress-bar"
                              :class="getProgressClass(item)"
                              :style="{ width: getProgressPercent(item) + '%' }"
                            ></div>
                          </div>
                          <div class="fs-progress-percent">
                            {{ getProgressPercent(item) }}% đã bán
                          </div>
                        </div>
                      </td>
                      <td class="text-center">
                        <span
                          :class="[
                            'fs-remaining-badge',
                            (item.remainingQuantity || 0) === 0 ? 'fs-remaining-empty' : ''
                          ]"
                        >
                          {{ item.remainingQuantity || 0 }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </template>
      </div>

      <!-- ====== FOOTER ====== -->
      <footer class="voucher-modal-footer fs-detail-footer">
        <div class="footer-info">
          <i class="bi bi-clock-history"></i>
          <span>Cập nhật lúc: {{ lastUpdated || '—' }}</span>
        </div>
        <button class="btn btn-secondary-action fs-btn-close" @click="onClose">
          <i class="bi bi-x-circle"></i> Đóng
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useFlashSaleStore } from '@/stores/FlashSaleStore.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  slotId: { type: Number, default: null },
})
const emit = defineEmits(['close'])

const store = useFlashSaleStore()
const localSlot = ref(null)
const loading = ref(false)
const error = ref('')
const lastUpdated = ref('')

const slot = computed(() => {
  if (store.selectedSlot && store.selectedSlot.slotId === props.slotId) {
    return store.selectedSlot
  }
  return localSlot.value
})

const stats = computed(() => {
  if (!slot.value || !slot.value.items) {
    return {
      buyerCount: 0,
      soldQuantity: 0,
      totalQuantity: 0,
      revenue: 0,
      soldPercent: 0,
    }
  }
  const items = slot.value.items
  const soldQuantity = items.reduce(
    (sum, it) => sum + (it.soldQuantity || 0),
    0
  )
  const totalQuantity = items.reduce(
    (sum, it) => sum + (it.flashSaleQuantity || 0),
    0
  )
  const revenue = items.reduce((sum, it) => {
    const sold = it.soldQuantity || 0
    const price = Number(it.flashSalePrice || 0)
    return sum + sold * price
  }, 0)
  const buyerCount = items.filter((it) => (it.soldQuantity || 0) > 0).length
  const soldPercent =
    totalQuantity > 0 ? Math.round((soldQuantity / totalQuantity) * 100) : 0
  return {
    buyerCount,
    soldQuantity,
    totalQuantity,
    revenue,
    soldPercent,
  }
})

const statusText = computed(() => {
  const s = Number(slot.value?.status)
  return (
    {
      0: 'Chờ duyệt',
      1: 'Lên lịch',
      2: 'Đang diễn ra',
      3: 'Đã kết thúc',
      4: 'Đã hủy',
    }[s] || 'Không xác định'
  )
})

const statusClass = computed(() => {
  const s = Number(slot.value?.status)
  return (
    {
      0: 'fs-status-pending',
      1: 'fs-status-scheduled',
      2: 'fs-status-active',
      3: 'fs-status-ended',
      4: 'fs-status-cancelled',
    }[s] || 'fs-status-pending'
  )
})

watch(
  () => [props.visible, props.slotId],
  async ([visible, slotId]) => {
    if (visible && slotId) {
      await loadDetail()
    }
  }
)

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    const result = await store.fetchOneSlot(props.slotId)
    if (!result) {
      error.value = store.error || 'Không thể tải chi tiết flash sale'
      localSlot.value = null
    } else {
      localSlot.value = result
      lastUpdated.value = new Date().toLocaleString('vi-VN')
    }
  } catch (e) {
    error.value = e?.message || 'Đã có lỗi xảy ra'
  } finally {
    loading.value = false
  }
}

function onClose() {
  emit('close')
}

function formatDate(date) {
  if (!date) return '—'
  try {
    const d = new Date(date)
    return d.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return date
  }
}

function formatCurrency(value) {
  const num = Number(value || 0)
  // Hiển thị giá chính xác tuyệt đối (KHÔNG làm tròn sang triệu/tỷ).
  // Dùng VND với dấu chấm phân cách hàng nghìn theo locale vi-VN.
  return num.toLocaleString('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  })
}

function getDiscountPercent(item) {
  if (!item?.originalPrice || !item?.flashSalePrice) return 0
  const orig = Number(item.originalPrice)
  const flash = Number(item.flashSalePrice)
  if (orig <= 0) return 0
  return Math.round(((orig - flash) / orig) * 100)
}

function getProgressPercent(item) {
  if (!item?.flashSaleQuantity) return 0
  const total = Number(item.flashSaleQuantity)
  const sold = Number(item.soldQuantity || 0)
  if (total <= 0) return 0
  return Math.min(100, Math.round((sold / total) * 100))
}

function getProgressClass(item) {
  const pct = getProgressPercent(item)
  if (pct >= 100) return 'fs-progress-complete'
  if (pct >= 70) return 'fs-progress-high'
  if (pct >= 30) return 'fs-progress-medium'
  return 'fs-progress-low'
}
</script>

<style scoped>
/* ===========================================
   PINK-THEME VARIABLES
   =========================================== */
.fs-detail-modal {
  --pink-main: #ff4d94;          /* hồng đậm chính */
  --pink-dark: #e63d80;          /* hover/đậm hơn */
  --pink-soft: #ffe0f0;          /* hồng rất nhạt - nền nút, nền input */
  --pink-border: #ffb3d9;        /* hồng nhạt - viền input */
  --pink-circle: #ffe5f1;        /* nền icon số */
  --pink-circle-border: #ff4d94; /* viền icon số */
  --gray-soft-bg: #f5f5f7;       /* nền xám nhạt - banner preview */
  --gray-text-main: #000000;
  --gray-text-sub: #555555;
  --gray-text-muted: #6b7280;
  --gray-border: #e5e7eb;
}

/* ===========================================
   OVERLAY - làm mờ dashboard
   =========================================== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow-y: auto;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.fs-detail-modal {
  width: 100%;
  max-width: 1080px;
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(255, 77, 148, 0.25),
              0 0 0 1px rgba(255, 77, 148, 0.05);
  background: #ffffff;
  animation: slideUp 0.25s ease;
  overflow: hidden;
}
@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* ===========================================
   HEADER
   =========================================== */
.fs-detail-header {
  background: linear-gradient(135deg, #ffffff 0%, #fff5fa 100%);
  border-bottom: 2px solid var(--pink-soft);
  padding: 22px 28px;
}
.fs-detail-title-wrap {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}
.fs-detail-title-wrap > i {
  font-size: 32px;
  color: var(--pink-main);
  flex-shrink: 0;
}
.fs-detail-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--pink-main);
  line-height: 1.2;
}
.fs-detail-subtitle {
  margin: 4px 0 0 0;
  font-size: 13.5px;
  color: var(--gray-text-sub);
  font-weight: 400;
}
.fs-close-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f5f5f7;
  color: #6b7280;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.fs-close-btn:hover {
  background: var(--pink-soft);
  color: var(--pink-main);
  transform: rotate(90deg);
}

/* ===========================================
   BODY + SCROLLBAR
   =========================================== */
.fs-detail-body {
  padding: 24px 28px;
  overflow-y: auto;
  flex: 1;
  background: #ffffff;
}
/* Scrollbar mỏng màu xám */
.fs-detail-body::-webkit-scrollbar {
  width: 8px;
}
.fs-detail-body::-webkit-scrollbar-track {
  background: transparent;
}
.fs-detail-body::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
.fs-detail-body::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* ===========================================
   BLOCK (section card)
   =========================================== */
.fs-block {
  background: #ffffff;
  border: 1px solid var(--gray-border);
  border-radius: 12px;
  padding: 22px;
  margin-bottom: 20px;
  transition: box-shadow 0.2s ease;
}
.fs-block:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}
.fs-block:last-child {
  margin-bottom: 0;
}

.fs-block-head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px dashed var(--pink-border);
}
.fs-step-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--pink-circle);
  border: 2px solid var(--pink-circle-border);
  color: var(--pink-main);
  font-weight: 700;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(255, 77, 148, 0.15);
}
.fs-block-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--gray-text-main);
  display: flex;
  align-items: center;
  gap: 8px;
}
.fs-block-count {
  font-size: 13px;
  color: var(--gray-text-muted);
  font-weight: 500;
}
.fs-block-desc {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: var(--gray-text-sub);
  line-height: 1.5;
}
.fs-block-body {
  padding-top: 4px;
}

/* ===========================================
   FIELDS
   =========================================== */
.fs-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.fs-field-full {
  grid-column: 1 / -1;
}
.fs-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.fs-label {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--gray-text-main);
  display: flex;
  align-items: center;
  gap: 4px;
}

.fs-readonly-box {
  background: #ffffff;
  border: 1.5px solid var(--pink-border);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 14px;
  color: var(--gray-text-main);
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.fs-readonly-box i {
  color: var(--pink-main);
  font-size: 16px;
  flex-shrink: 0;
}
.fs-readonly-box:hover {
  border-color: var(--pink-main);
  box-shadow: 0 0 0 3px rgba(255, 77, 148, 0.08);
}

/* ===========================================
   STATUS BADGE
   =========================================== */
.fs-status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
}
.fs-status-scheduled { background: #dbeafe; color: #1d4ed8; }
.fs-status-active    { background: #d1fae5; color: #047857; }
.fs-status-ended     { background: #e5e7eb; color: #4b5563; }
.fs-status-cancelled { background: #fee2e2; color: #b91c1c; }
.fs-status-pending   { background: #fef3c7; color: #92400e; }

.fs-slot-id {
  font-family: 'Courier New', monospace;
  font-weight: 700;
  color: var(--pink-main);
}

/* ===========================================
   STATS GRID (4 cards)
   =========================================== */
.fs-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.fs-stat-card {
  background: #ffffff;
  border: 1.5px solid var(--gray-border);
  border-radius: 12px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}
.fs-stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.06);
  border-color: var(--pink-border);
}
.fs-stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  flex-shrink: 0;
}
.fs-stat-icon-blue   { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
.fs-stat-icon-green  { background: linear-gradient(135deg, #10b981, #047857); }
.fs-stat-icon-orange { background: linear-gradient(135deg, #f59e0b, #d97706); }
.fs-stat-icon-purple { background: linear-gradient(135deg, #8b5cf6, #6d28d9); }

.fs-stat-info { flex: 1; min-width: 0; }
.fs-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--gray-text-main);
  line-height: 1.1;
  word-wrap: break-word;
}
.fs-stat-sub {
  font-size: 14px;
  color: #9ca3af;
  font-weight: 500;
}
.fs-stat-label {
  font-size: 12px;
  color: var(--gray-text-sub);
  margin-top: 6px;
}

/* ===========================================
   BANNER
   =========================================== */
.fs-banner-frame {
  width: 100%;
  max-height: 240px;
  border-radius: 10px;
  overflow: hidden;
  border: 1.5px solid var(--pink-border);
  background: var(--gray-soft-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}
.fs-banner-img {
  width: 100%;
  height: 100%;
  max-height: 240px;
  object-fit: cover;
}

/* ===========================================
   TABLE
   =========================================== */
.fs-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--gray-border);
  border-radius: 10px;
  background: #ffffff;
}
.fs-detail-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13.5px;
}
.fs-detail-table thead {
  background: linear-gradient(180deg, #fff5fa 0%, #ffffff 100%);
}
.fs-detail-table th {
  text-align: left;
  padding: 12px 14px;
  font-weight: 700;
  color: var(--pink-main);
  border-bottom: 1.5px solid var(--pink-border);
  white-space: nowrap;
  font-size: 12.5px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.fs-detail-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: middle;
}
.fs-detail-table tbody tr:hover {
  background: #fff5fa;
}
.fs-detail-table tbody tr:last-child td {
  border-bottom: none;
}

/* Product cell */
.fs-product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.fs-product-img {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--gray-soft-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #9ca3af;
  border: 1px solid var(--gray-border);
}
.fs-product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.fs-product-name {
  font-weight: 600;
  color: var(--gray-text-main);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.fs-product-sku {
  font-size: 11.5px;
  color: var(--gray-text-muted);
  margin-top: 3px;
  font-family: 'Courier New', monospace;
}

/* Price cell */
.fs-price-cell { display: flex; flex-direction: column; gap: 4px; }
.fs-price-flash {
  font-weight: 700;
  color: var(--pink-main);
  font-size: 14.5px;
}
.fs-price-original {
  font-size: 12px;
  color: var(--gray-text-muted);
  display: flex;
  align-items: center;
  gap: 6px;
}
.fs-discount-badge {
  background: var(--pink-soft);
  color: var(--pink-main);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
}

/* Progress cell */
.fs-progress-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.fs-progress-text {
  font-size: 12.5px;
  color: var(--gray-text-sub);
}
.fs-progress-text strong {
  color: var(--gray-text-main);
  font-size: 14px;
}
.fs-progress-bar-wrap {
  width: 100%;
  height: 10px;
  background: #f3f4f6;
  border-radius: 5px;
  overflow: hidden;
}
.fs-progress-bar {
  height: 100%;
  border-radius: 5px;
  transition: width 0.4s ease;
}
.fs-progress-low      { background: linear-gradient(90deg, #fbbf24, #f59e0b); }
.fs-progress-medium   { background: linear-gradient(90deg, #60a5fa, #3b82f6); }
.fs-progress-high     { background: linear-gradient(90deg, #34d399, #10b981); }
.fs-progress-complete { background: linear-gradient(90deg, var(--pink-main), var(--pink-dark)); }
.fs-progress-percent {
  font-size: 11.5px;
  color: var(--gray-text-muted);
  font-weight: 500;
}

/* Remaining badge */
.fs-remaining-badge {
  display: inline-block;
  padding: 4px 12px;
  background: #f0fdf4;
  color: #047857;
  border-radius: 12px;
  font-weight: 700;
  font-size: 13px;
}
.fs-remaining-empty {
  background: #fee2e2;
  color: #b91c1c;
}

/* ===========================================
   EMPTY / LOADING / ERROR
   =========================================== */
.fs-detail-empty {
  text-align: center;
  padding: 50px 20px;
  color: var(--gray-text-muted);
}
.fs-detail-empty i {
  font-size: 44px;
  display: block;
  margin-bottom: 10px;
  color: var(--pink-border);
}
.fs-detail-loading,
.fs-detail-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 14px;
  color: var(--gray-text-muted);
}
.fs-detail-error {
  color: #dc2626;
}
.fs-detail-error i {
  font-size: 40px;
}

/* ===========================================
   FOOTER
   =========================================== */
.fs-detail-footer {
  background: var(--gray-soft-bg);
  border-top: 1px solid var(--gray-border);
  display: flex;
  align-items: center;
  padding: 14px 28px;
  gap: 12px;
}
.footer-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--gray-text-sub);
  flex: 1;
}
.footer-info i {
  color: var(--pink-main);
}
.fs-btn-close {
  background: var(--pink-soft);
  color: var(--pink-main);
  border: none;
  padding: 9px 22px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.fs-btn-close:hover {
  background: var(--pink-main);
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(255, 77, 148, 0.25);
}
.fs-btn-close:active {
  transform: translateY(0);
}

/* ===========================================
   RESPONSIVE
   =========================================== */
@media (max-width: 768px) {
  .fs-detail-modal {
    max-width: 100%;
    margin: 0;
    border-radius: 12px;
  }
  .fs-grid-2 {
    grid-template-columns: 1fr;
  }
  .fs-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .fs-detail-header,
  .fs-detail-body,
  .fs-detail-footer {
    padding-left: 18px;
    padding-right: 18px;
  }
}
@media (max-width: 480px) {
  .fs-stats-grid {
    grid-template-columns: 1fr;
  }
  .fs-detail-title {
    font-size: 18px;
  }
}
</style>
