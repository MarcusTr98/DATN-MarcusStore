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
            width: '46px',
            height: '46px',
            minWidth: '46px',
            borderRadius: '14px',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '20px',
            flex: 'none',
          }"
        ></span>
        <small v-if="item.growth">{{ item.growth }}</small>
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
  kpiSummary:         { type: Object, default: () => ({ totalRevenue: 0, totalOrders: 0, totalProductsSold: 0 }) },
  pendingOrdersCount: { type: Number, default: 0 },
  lowStockData:       { type: Array,  default: () => [] },
  periodLabel:        { type: String, default: 'tháng này' },
})

const emit = defineEmits(['action'])

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(value || 0)
}

const inventoryAlerts = computed(() =>
  props.lowStockData.slice(0, 3).map(item => ({
    title: item.status === 'Hết hàng'
      ? `Hết hàng: ${item.productName} (${item.skuCode})`
      : `Sắp hết hàng: ${item.productName} chỉ còn ${item.stockQuantity} sản phẩm`,
  }))
)

const kpiItems = computed(() => [
  {
    key: 'revenue', title: 'Doanh thu',
    value: formatCurrency(props.kpiSummary.totalRevenue), growth: '',
    note: `Tổng doanh thu ${props.periodLabel}`,
    icon: 'bi bi-currency-dollar', type: 'normal', link: null, action: null,
  },
  {
    key: 'orders', title: 'Tổng đơn hàng',
    value: String(props.kpiSummary.totalOrders), growth: '',
    note: `Đơn hàng đã ghi nhận ${props.periodLabel}`,
    icon: 'bi bi-bag-check', type: 'normal', link: null, action: null,
  },
  {
    key: 'soldProducts', title: 'Sản phẩm đã bán',
    value: String(props.kpiSummary.totalProductsSold), growth: '',
    note: `Số lượng SKU đã bán ${props.periodLabel}`,
    icon: 'bi bi-box-seam', type: 'normal', link: null, action: null,
  },
  {
    key: 'pendingOrders', title: 'Đơn mới chưa xử lý',
    value: String(props.pendingOrdersCount),
    growth: props.pendingOrdersCount > 0 ? 'Ưu tiên' : '',
    note: 'Ưu tiên duyệt để tránh trễ SLA',
    icon: 'bi bi-exclamation-triangle',
    type: props.pendingOrdersCount > 0 ? 'warning' : 'normal',
    link: '/admin/order', action: null,
  },
  {
    key: 'lowStock', title: 'SP sắp / hết hàng',
    value: String(props.lowStockData.length),
    growth: props.lowStockData.length > 0 ? 'Cần xử lý' : '',
    note: 'Cần nhập thêm hàng',
    icon: 'bi bi-archive',
    type: props.lowStockData.length > 0 ? 'warning' : 'normal',
    link: null, action: 'lowStock',
  },
  {
    key: 'alerts', title: 'Cảnh báo tồn kho',
    value: String(inventoryAlerts.value.length),
    growth: inventoryAlerts.value.length > 0 ? 'Mới' : '',
    note: inventoryAlerts.value.length > 0 ? inventoryAlerts.value[0].title : 'Không có cảnh báo',
    icon: 'bi bi-bell',
    type: inventoryAlerts.value.length > 0 ? 'warning' : 'normal',
    link: null, action: inventoryAlerts.value.length > 0 ? 'lowStock' : null,
  },
])
</script>

<style scoped>
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

.kpi-grid .kpi-card.clickable {
  cursor: pointer;
}

.kpi-grid .kpi-card.clickable:hover {
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
  width: 46px;
  height: 46px;
  min-width: 46px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  font-size: 20px;
}

.kpi-grid .kpi-icon::before {
  display: block;
  line-height: 1;
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
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .kpi-grid { grid-template-columns: 1fr; }
}
</style>