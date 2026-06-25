<template>
  <section class="kpi-grid">
    <component
      v-for="item in kpiItems"
      :key="item.key"
      :is="item.link ? 'a' : 'article'"
      :href="item.link || undefined"
      class="kpi-card"
      :class="{ warning: item.type === 'warning' }"
    >
      <div class="kpi-top">
        <span
          class="kpi-icon"
          style="width:48px;height:48px;min-width:48px;border-radius:16px;display:inline-flex;align-items:center;justify-content:center;font-size:22px;flex:none;"
          :style="{ background: item.type === 'warning' ? '#ffedd5' : '#fff2f7', color: item.type === 'warning' ? '#c2410c' : '#ff4d8d' }"
        ><i :class="item.icon"></i></span>
        <small>{{ item.growth }}</small>
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
  kpiSummary:         { type: Object,  default: () => ({ totalRevenue: 0, totalOrders: 0, totalProductsSold: 0 }) },
  pendingOrdersCount: { type: Number,  default: 0 },
  lowStockData:       { type: Array,   default: () => [] },
  periodLabel:        { type: String,  default: 'tháng này' },
})

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(value || 0)
}

const inventoryAlerts = computed(() =>
  props.lowStockData.slice(0, 3).map((item) => ({
    title: item.status === 'Hết hàng'
      ? `Hết hàng: ${item.productName} (${item.skuCode})`
      : `Sắp hết hàng: ${item.productName} chỉ còn ${item.stockQuantity} sản phẩm`,
  })),
)

const kpiItems = computed(() => [
  {
    key:    'revenue',
    title:  'Doanh thu',
    value:  formatCurrency(props.kpiSummary.totalRevenue),
    growth: '',
    note:   `Tổng doanh thu ${props.periodLabel}`,
    icon:   'bi bi-currency-dollar',
    type:   'normal',
  },
  {
    key:    'orders',
    title:  'Tổng đơn hàng',
    value:  String(props.kpiSummary.totalOrders),
    growth: '',
    note:   `Đơn hàng đã ghi nhận ${props.periodLabel}`,
    icon:   'bi bi-bag-check',
    type:   'normal',
  },
  {
    key:    'soldProducts',
    title:  'Sản phẩm đã bán',
    value:  String(props.kpiSummary.totalProductsSold),
    growth: '',
    note:   `Số lượng SKU đã bán ${props.periodLabel}`,
    icon:   'bi bi-box-seam',
    type:   'normal',
  },
  {
    key:    'pendingOrders',
    title:  'Đơn mới chưa xử lý',
    value:  String(props.pendingOrdersCount),
    growth: 'Ưu tiên',
    note:   'Ưu tiên duyệt để tránh trễ SLA',
    icon:   'bi bi-exclamation-triangle',
    type:   'warning',
    link:   '/admin/orders?status=PENDING',
  },
  {
    key:    'lowStock',
    title:  'SP sắp / hết hàng',
    value:  String(props.lowStockData.length),
growth: props.lowStockData.length > 0 ? 'Cần xử lý' : '',
    note:   'Cần nhập thêm hàng',
    icon:   'bi bi-archive',
    type:   props.lowStockData.length > 0 ? 'warning' : 'normal',
    link:   '/admin/inventory?filter=low',
  },
  {
    key:    'alerts',
    title:  'Cảnh báo tồn kho',
    value:  String(inventoryAlerts.value.length),
    growth: inventoryAlerts.value.length > 0 ? 'Mới' : '',
    note:   inventoryAlerts.value.length > 0
      ? inventoryAlerts.value[0].title
      : 'Không có cảnh báo',
    icon:   'bi bi-bell',
    type:   inventoryAlerts.value.length > 0 ? 'warning' : 'normal',
    link:   inventoryAlerts.value.length > 0 ? '/admin/inventory?filter=low' : undefined,
  },
])
</script>

<style>
/* Không dùng scoped vì <component :is> dynamic không nhận được scoped hash */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.kpi-grid .kpi-card {
  background: #fff;
  border: 1px solid #ffe0ec;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.06);
  min-height: 170px;
  border-radius: 22px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  text-decoration: none;
  color: inherit;
}

.kpi-grid .kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(37, 99, 235, 0.15);
}

.kpi-grid .kpi-card.warning {
  background: #fff7ed;
  border-color: #fed7aa;
}

.kpi-grid .kpi-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.kpi-grid .kpi-icon {
  width: 48px !important;
  height: 48px !important;
  min-width: 48px;
  min-height: 48px;
  border-radius: 16px;
  background: #fff2f7;
  color: #ff4d8d;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex: none;
}

.kpi-grid .kpi-icon i {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1em;
  height: 1em;
  line-height: 1;
}

.kpi-grid .kpi-card.warning .kpi-icon {
  background: #ffedd5;
  color: #c2410c;
}

.kpi-grid .kpi-top small {
  padding: 5px 10px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  font-size: 12px;
  font-weight: 900;
}

.kpi-grid .kpi-card.warning .kpi-top small {
  background: #ffedd5;
  color: #c2410c;
}

.kpi-grid .kpi-card p {
  margin: 0;
  font-size: 13px;
  font-weight: 800;
  color: #6b7280;
}

.kpi-grid .kpi-card strong {
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

.kpi-grid .kpi-card span {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #6b7280;
}

@media (max-width: 992px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>