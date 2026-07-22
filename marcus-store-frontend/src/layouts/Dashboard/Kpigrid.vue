<template>
  <section class="kpi-grid">
    <component
      v-for="item in kpiItems"
      :key="item.key"
      :is="item.link ? 'router-link' : 'article'"
      :to="item.link || undefined"
      class="kpi-card"
      :class="{ warning: item.type === 'warning', clickable: !!item.link || !!item.action }"
      @click="item.action ? $emit('action', item.action) : undefined"
    >
      <div class="kpi-top">
        <span
          class="kpi-icon"
          :class="item.icon"
          :style="{
            background: item.type === 'warning' ? '#ffedd5' : '#fff2f7',
            color: item.type === 'warning' ? '#c2410c' : '#ff4d8d',
            width: '46px', height: '46px', minWidth: '46px',
            borderRadius: '14px', display: 'inline-flex',
            alignItems: 'center', justifyContent: 'center',
            fontSize: '20px', flex: 'none',
          }"
        ></span>
        <!-- Badge % thay đổi (chỉ 4 ô đầu) -->
        <small
          v-if="item.change !== undefined"
          :class="changeBadgeClass(item.change)"
        >
          <i :class="item.change === null ? 'bi bi-dash' : item.change >= 0 ? 'bi bi-arrow-up' : 'bi bi-arrow-down'"></i>
          {{ item.change === null ? 'N/A' : Math.abs(item.change) + '%' }}
        </small>
        <!-- Badge cảnh báo (ô warning) -->
        <small v-else-if="item.badge" :class="item.badgeClass ?? 'badge-warning'">
          {{ item.badge }}
        </small>
      </div>
      <div>
        <p>{{ item.title }}</p>
        <strong>{{ item.value }}</strong>
        <span>{{ item.note }}</span>
      </div>
    </component>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  kpiCompare:         { type: Object, default: () => ({}) },   // từ /kpi-compare
  pendingOrdersCount: { type: Number, default: 0 },
  lowStockData:       { type: Array,  default: () => [] },
  // periodLabel: nhãn kỳ hiện tại — 'hôm nay' | '7 ngày qua' | 'tháng này' | ...
  // dùng để suy ra nhãn kỳ trước tương ứng hiển thị trong note
  periodLabel:        { type: String, default: 'hôm nay' },
})

const emit = defineEmits(['action'])

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(value || 0)
}

function changeBadgeClass(change) {
  if (change === null || change === undefined) return 'badge-neutral'
  if (change > 0)  return 'badge-up'
  if (change < 0)  return 'badge-down'
  return 'badge-neutral'
}

/**
 * Từ periodLabel của kỳ HIỆN TẠI, suy ra nhãn kỳ TRƯỚC để hiển thị trong note.
 * Ví dụ: periodLabel = '7 ngày qua'  → 'tuần trước'
 *        periodLabel = 'tháng này'   → 'tháng trước'
 *        periodLabel = 'hôm nay'     → 'hôm qua'
 */
function resolvePreviousPeriodName(periodLabel) {
  const map = {
    'hôm nay':     'hôm qua',
    '7 ngày qua':  'tuần trước',
    '30 ngày qua': 'tháng trước',
    'tháng này':   'tháng trước',
    'tuần này':    'tuần trước',
    'năm nay':     'năm ngoái',
  }
  return map[periodLabel] ?? 'kỳ trước'
}

/**
 * Tạo note rõ ràng dạng:
 * "So với tuần trước (08/07–14/07/2026)"
 * Badge % đã hiển thị tăng/giảm rồi nên note chỉ cần nói SO VỚI CÁI GÌ.
 */
function buildChangeNote(previousLabel, periodLabel) {
  if (!previousLabel) return ''
  const periodName = resolvePreviousPeriodName(periodLabel)
  return `So với ${periodName} (${previousLabel})`
}

const kd = computed(() => props.kpiCompare)

const kpiItems = computed(() => [
  {
    key: 'revenue',
    title: 'Doanh thu',
    value: formatCurrency(kd.value.totalRevenue),
    change: kd.value.revenueChangePercent ?? null,
    note: buildChangeNote(kd.value.previousLabel, props.periodLabel),
    icon: 'bi bi-currency-dollar', type: 'normal', link: null, action: null,
  },
  {
    key: 'orders',
    title: 'Tổng đơn hàng',
    value: String(kd.value.totalOrders ?? 0),
    change: kd.value.ordersChangePercent ?? null,
    note: buildChangeNote(kd.value.previousLabel, props.periodLabel),
    icon: 'bi bi-bag-check', type: 'normal', link: null, action: null,
  },
  {
    key: 'completedOrders',
    title: 'Đơn hoàn thành',
    value: String(kd.value.completedOrders ?? 0),
    change: kd.value.completedOrdersChangePercent ?? null,
    note: buildChangeNote(kd.value.previousLabel, props.periodLabel),
    icon: 'bi bi-patch-check', type: 'normal', link: null, action: null,
  },
  {
    key: 'soldProducts',
    title: 'Sản phẩm đã bán',
    value: String(kd.value.totalProductsSold ?? 0),
    change: kd.value.productsSoldChangePercent ?? null,
    note: buildChangeNote(kd.value.previousLabel, props.periodLabel),
    icon: 'bi bi-box-seam', type: 'normal', link: null, action: null,
  },
  {
    key: 'pendingOrders',
    title: 'Đơn chờ xử lý',
    value: String(props.pendingOrdersCount),
    badge: props.pendingOrdersCount > 0 ? 'Cần xử lý' : '',
    badgeClass: 'badge-warning',
    note: props.pendingOrdersCount > 0
      ? 'Cần xử lý ngay để tránh chậm trễ đơn hàng'
      : 'Không có đơn nào đang chờ xử lý',
    icon: 'bi bi-exclamation-triangle',
    type: props.pendingOrdersCount > 0 ? 'warning' : 'normal',
    link: '/admin/order', action: null,
  },
  {
    key: 'lowStock',
    title: 'Sản phẩm sắp hết / hết hàng',
    value: String(props.lowStockData.length),
    badge: props.lowStockData.length > 0 ? 'Cần nhập thêm' : '',
    badgeClass: 'badge-warning',
    note: props.lowStockData.length > 0
      ? `${props.lowStockData.filter(i => i.status === 'Hết hàng').length} sản phẩm hết hàng · ${props.lowStockData.filter(i => i.status !== 'Hết hàng').length} sắp hết`
      : 'Tồn kho đang ở mức an toàn',
    icon: 'bi bi-archive',
    type: props.lowStockData.length > 0 ? 'warning' : 'normal',
    link: null, action: 'lowStock',
  },
])
</script>

<style scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.kpi-card {
  background: #fff;
  border: 1px solid #ffe0ec;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.06);
  min-height: 160px;
  border-radius: 22px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  text-decoration: none;
  color: inherit;
}

.kpi-card.clickable { cursor: pointer; }
.kpi-card.clickable:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(37, 99, 235, 0.15);
}
.kpi-card.warning {
  background: #fff7ed;
  border-color: #fed7aa;
}

.kpi-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.kpi-icon::before { display: block; line-height: 1; }

/* ── Badges ── */
.kpi-top small {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
}

.badge-up      { background: #ecfdf5; color: #047857; }
.badge-down    { background: #fef2f2; color: #b91c1c; }
.badge-neutral { background: #f3f4f6; color: #6b7280; }
.badge-warning { background: #ffedd5; color: #c2410c; }

.kpi-card p {
  margin: 0;
  font-size: 13px;
  font-weight: 800;
  color: #6b7280;
}
.kpi-card strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 20px;
  line-height: 1.1;
  font-weight: 900;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.kpi-card span {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #6b7280;
}

@media (max-width: 992px) { .kpi-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) { .kpi-grid { grid-template-columns: 1fr; } }
</style>