<template>
  <section class="analytics-kpis" aria-label="Chỉ số kinh doanh">
    <article
      v-for="card in cards"
      :key="card.key"
      class="analytics-kpi"
      :class="`analytics-kpi--${card.tone}`"
    >
      <div class="analytics-kpi__top">
        <span class="analytics-kpi__icon"><i :class="card.icon"></i></span>
        <span v-if="card.change !== null" class="analytics-kpi__change" :class="changeClass(card)">
          <i :class="changeIcon(card)"></i>
          {{ formatChange(card.change, card.rate) }}
        </span>
        <span v-else class="analytics-kpi__change analytics-kpi__change--new">Mới</span>
      </div>
      <p>{{ card.label }}</p>
      <strong>{{ card.value }}</strong>
      <small>So với kỳ trước: {{ card.previous }}</small>
    </article>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  overview: { type: Object, required: true },
})

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})
const integerFormatter = new Intl.NumberFormat('vi-VN')

const cards = computed(() => [
  metricCard(
    'sales',
    'Doanh thu đã thu & hoàn tất',
    'bi bi-cash-stack',
    'blue',
    props.overview.completedSales,
    money,
  ),
  metricCard(
    'orders',
    'Đơn hoàn tất',
    'bi bi-bag-check',
    'green',
    props.overview.completedOrders,
    integer,
  ),
  metricCard(
    'units',
    'Sản phẩm đã bán',
    'bi bi-box-seam',
    'violet',
    props.overview.unitsSold,
    integer,
  ),
  metricCard(
    'aov',
    'Giá trị đơn trung bình',
    'bi bi-receipt',
    'orange',
    props.overview.averageOrderValue,
    money,
  ),
  rateCard(
    'completion',
    'Tỷ lệ hoàn tất',
    'bi bi-check2-circle',
    'cyan',
    props.overview.completionRate,
  ),
  rateCard(
    'cancellation',
    'Tỷ lệ hủy',
    'bi bi-x-circle',
    'red',
    props.overview.cancellationRate,
    true,
  ),
  metricCard(
    'customers',
    'Khách có đơn hoàn tất',
    'bi bi-people',
    'indigo',
    props.overview.orderingCustomers,
    integer,
  ),
  metricCard(
    'refund',
    'Hoàn tiền thành công',
    'bi bi-arrow-counterclockwise',
    'slate',
    props.overview.successfulRefundAmount,
    money,
    true,
  ),
])

function metricCard(key, label, icon, tone, metric, formatter, reverse = false) {
  return {
    key,
    label,
    icon,
    tone,
    reverse,
    rate: false,
    value: formatter(metric?.currentValue),
    previous: formatter(metric?.previousValue),
    change: metric?.changePercent ?? null,
  }
}

function rateCard(key, label, icon, tone, metric, reverse = false) {
  return {
    key,
    label,
    icon,
    tone,
    reverse,
    rate: true,
    value: percent(metric?.currentPercent),
    previous: percent(metric?.previousPercent),
    change: metric?.percentagePointChange ?? 0,
  }
}

function money(value) {
  return moneyFormatter.format(Number(value || 0))
}

function integer(value) {
  return integerFormatter.format(Number(value || 0))
}

function percent(value) {
  return `${Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 })}%`
}

function formatChange(value, rate) {
  const absolute = Math.abs(Number(value || 0)).toLocaleString('vi-VN', {
    maximumFractionDigits: 2,
  })
  return `${absolute}${rate ? ' điểm' : '%'}`
}

function isPositive(card) {
  const improved = card.reverse ? card.change <= 0 : card.change >= 0
  return improved
}

function changeClass(card) {
  return isPositive(card) ? 'analytics-kpi__change--up' : 'analytics-kpi__change--down'
}

function changeIcon(card) {
  const rawDirectionUp = card.change >= 0
  return rawDirectionUp ? 'bi bi-arrow-up-right' : 'bi bi-arrow-down-right'
}
</script>
