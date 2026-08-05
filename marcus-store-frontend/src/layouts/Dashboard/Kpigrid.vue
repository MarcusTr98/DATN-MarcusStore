<template>
  <section class="kpi-grid">
    <component
      v-for="item in kpiItems"
      :key="item.key"
      :is="item.link ? 'router-link' : 'article'"
      :to="item.link || undefined"
      class="kpi-card"
      :class="{ warning: item.type === 'warning', clickable: !!item.link || !!item.action }"
      :style="{ background: item.bg }"
      @click="item.action ? $emit('action', item.action) : undefined"
    >
      <div class="kpi-top">
        <span
          class="kpi-icon"
          :class="item.icon"
          :style="{ background: item.iconBg, color: item.iconColor }"
        ></span>
        <small v-if="item.badge" :class="item.badgeClass ?? 'badge-warning'">
          {{ item.badge }}
        </small>
      </div>
      <div class="kpi-body">
        <p class="kpi-title">{{ item.title }}</p>
        <strong class="kpi-value">{{ item.value }}</strong>
        <span class="kpi-note">
          <template v-if="item.change !== undefined && item.change !== null">
            <span :class="item.change >= 0 ? 'trend-up' : 'trend-down'">
              <i :class="item.change >= 0 ? 'bi bi-arrow-up-short' : 'bi bi-arrow-down-short'"></i>
              {{ Math.abs(item.change) }}%
            </span>
            so với {{ kd.previousLabel }}
          </template>
          <template v-else-if="item.change === null">
            <span class="trend-neutral">— Chưa có dữ liệu kỳ trước</span>
          </template>
          <template v-else>
            {{ item.note }}
          </template>
        </span>
      </div>
    </component>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  kpiCompare:         { type: Object, default: () => ({}) },
  pendingOrdersCount: { type: Number, default: 0 },
  lowStockData:       { type: Array,  default: () => [] },
  lowStockTotal:      { type: Number, default: 0 },   // ← THÊM: tổng từ PagedResponseDTO
  periodLabel:        { type: String, default: 'hôm nay' },
})

const emit = defineEmits(['action'])

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(value || 0)
}

const kd = computed(() => props.kpiCompare)

const periodTitle = computed(() => {
  switch (props.periodLabel) {
    case 'hôm nay':     return 'hôm nay'
    case 'hôm qua':     return 'hôm qua'
    case '7 ngày qua':  return '7 ngày qua'
    case '30 ngày qua': return '30 ngày qua'
    case 'tuần này':    return 'tuần này'
    case 'năm nay':     return 'năm nay'
    default:            return 'tháng này'
  }
})

function trendStyle(change, icon) {
  if (change === null || change === undefined) {
    return { bg: 'linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%)', iconBg: '#fde68a', iconColor: '#92400e', icon }
  }
  if (change > 0) {
    return { bg: 'linear-gradient(135deg, #f0fdf4 0%, #bbf7d0 100%)', iconBg: '#86efac', iconColor: '#14532d', icon }
  }
  if (change < 0) {
    return { bg: 'linear-gradient(135deg, #fff1f2 0%, #fecdd3 100%)', iconBg: '#fda4af', iconColor: '#881337', icon }
  }
  return { bg: 'linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%)', iconBg: '#fde68a', iconColor: '#92400e', icon }
}

// ── FIX: dùng lowStockTotal thay vì lowStockData.length ──────────────────────
const lowStockCount = computed(() =>
  props.lowStockTotal > 0 ? props.lowStockTotal : props.lowStockData.length
)

const kpiItems = computed(() => [
  {
    key: 'revenue',
    title: `Doanh thu ${periodTitle.value}`,
    value: formatCurrency(kd.value.totalRevenue),
    change: kd.value.revenueChangePercent ?? null,
    ...trendStyle(kd.value.revenueChangePercent ?? null, 'bi bi-currency-dollar'),
    type: 'normal', link: null, action: null,
  },
  {
    key: 'orders',
    title: `Tổng đơn hàng ${periodTitle.value}`,
    value: String(kd.value.totalOrders ?? 0),
    change: kd.value.ordersChangePercent ?? null,
    ...trendStyle(kd.value.ordersChangePercent ?? null, 'bi bi-bag-check'),
    type: 'normal', link: null, action: null,
  },
  {
    key: 'completedOrders',
    title: `Đơn hoàn thành ${periodTitle.value}`,
    value: String(kd.value.completedOrders ?? 0),
    change: kd.value.completedOrdersChangePercent ?? null,
    ...trendStyle(kd.value.completedOrdersChangePercent ?? null, 'bi bi-patch-check'),
    type: 'normal', link: null, action: null,
  },
  {
    key: 'soldProducts',
    title: `Sản phẩm đã bán ${periodTitle.value}`,
    value: String(kd.value.totalProductsSold ?? 0),
    change: kd.value.productsSoldChangePercent ?? null,
    ...trendStyle(kd.value.productsSoldChangePercent ?? null, 'bi bi-box-seam'),
    type: 'normal', link: null, action: null,
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
    bg: props.pendingOrdersCount > 0
      ? 'linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%)'
      : 'linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%)',
    iconBg:   props.pendingOrdersCount > 0 ? '#ffedd5' : '#f3f4f6',
    iconColor: props.pendingOrdersCount > 0 ? '#c2410c' : '#6b7280',
  },
  {
    key: 'lowStock',
    title: 'Sản phẩm sắp hết / hết kho',
    value: String(lowStockCount.value),   // ← dùng computed
    badge: lowStockCount.value > 0 ? 'Cần nhập thêm' : '',
    badgeClass: 'badge-warning',
    note: lowStockCount.value > 0
      ? `${props.lowStockData.filter(i => i.status === 'Hết hàng').length} sản phẩm hết hàng · ${props.lowStockData.filter(i => i.status !== 'Hết hàng').length} sắp hết`
      : 'Tồn kho đang ở mức an toàn',
    icon: 'bi bi-archive',
    type: lowStockCount.value > 0 ? 'warning' : 'normal',
    link: null, action: 'lowStock',
    bg: lowStockCount.value > 0
      ? 'linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%)'
      : 'linear-gradient(135deg, #f0fdf4 0%, #bbf7d0 100%)',
    iconBg:   lowStockCount.value > 0 ? '#ffedd5' : '#86efac',
    iconColor: lowStockCount.value > 0 ? '#c2410c' : '#14532d',
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
  border: 1px solid rgba(0,0,0,0.06);
  box-shadow: 0 2px 16px rgba(0,0,0,0.05);
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
  box-shadow: 0 16px 40px rgba(0,0,0,0.1);
}

.kpi-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.kpi-icon {
  width: 46px;
  height: 46px;
  min-width: 46px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex: none;
}

.kpi-icon::before { display: block; line-height: 1; }

.kpi-top small {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
}

.badge-warning { background: rgba(0,0,0,0.08); color: #c2410c; }

.kpi-body { display: flex; flex-direction: column; gap: 2px; }

.kpi-title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #6b7280;
}

.kpi-value {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 22px;
  line-height: 1.15;
  font-weight: 900;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kpi-note {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  font-weight: 700;
  color: #6b7280;
  flex-wrap: wrap;
}

.trend-up {
  display: inline-flex;
  align-items: center;
  gap: 1px;
  color: #15803d;
  font-weight: 900;
  font-size: 13px;
}

.trend-down {
  display: inline-flex;
  align-items: center;
  gap: 1px;
  color: #b91c1c;
  font-weight: 900;
  font-size: 13px;
}

.trend-neutral {
  color: #9ca3af;
  font-style: italic;
}

@media (max-width: 992px) { .kpi-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) { .kpi-grid { grid-template-columns: 1fr; } }
</style>