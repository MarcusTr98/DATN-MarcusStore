<template>
  <article class="analytics-panel analytics-products">
    <div class="analytics-panel__header">
      <div>
        <span class="analytics-panel__eyebrow">Hiệu suất sản phẩm</span>
        <h2>Sản phẩm nổi bật trong kỳ</h2>
      </div>
      <span class="analytics-products__count">{{ products.length }} sản phẩm</span>
    </div>

    <div v-if="products.length" class="analytics-table-wrap">
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Sản phẩm</th>
            <th>Đã bán</th>
            <th>So kỳ trước</th>
            <th>Doanh số hàng hóa</th>
            <th>Xu hướng</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(product, index) in pagedProducts" :key="product.productId">
            <td>
              <span class="analytics-rank" :class="{ top: absoluteIndex(index) < 3 }">
                {{ absoluteIndex(index) + 1 }}
              </span>
            </td>
            <td>
              <strong>{{ product.productName }}</strong>
              <small>{{ product.brand || 'Chưa có thương hiệu' }}</small>
            </td>
            <td>
              <strong>{{ formatInteger(product.currentUnits) }}</strong>
              <small>sản phẩm</small>
            </td>
            <td>
              <strong>{{ formatInteger(product.previousUnits) }}</strong>
              <small>sản phẩm</small>
            </td>
            <td class="analytics-products__money">
              {{ formatMoney(product.currentMerchandiseSales) }}
            </td>
            <td>
              <span
                class="analytics-product-trend"
                :class="trendClass(product.unitsChangePercent)"
              >
                <i :class="trendIcon(product.unitsChangePercent)"></i>
                {{ formatTrend(product.unitsChangePercent) }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <!-- Marcus thêm: phân trang cục bộ để bảng dài không kéo vỡ bố cục Analytics. -->
    <nav
      v-if="totalPages > 1"
      class="analytics-table-pagination"
      aria-label="Phân trang sản phẩm"
    >
      <span>Trang {{ page }}/{{ totalPages }}</span>
      <div>
        <button type="button" :disabled="page === 1" @click="page--"><i class="bi bi-chevron-left"></i></button>
        <button
          v-for="number in totalPages"
          :key="number"
          type="button"
          :class="{ active: page === number }"
          @click="page = number"
        >
          {{ number }}
        </button>
        <button type="button" :disabled="page === totalPages" @click="page++"><i class="bi bi-chevron-right"></i></button>
      </div>
    </nav>
    <div v-if="!products.length" class="analytics-empty">
      <i class="bi bi-box-seam"></i>
      <p>Chưa có sản phẩm bán hoàn tất trong khoảng này.</p>
    </div>
  </article>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  products: { type: Array, required: true },
})
const page = ref(1)
const pageSize = 6
const totalPages = computed(() => Math.max(1, Math.ceil(props.products.length / pageSize)))
const pagedProducts = computed(() => props.products.slice((page.value - 1) * pageSize, page.value * pageSize))
watch(
  () => props.products,
  () => {
    page.value = 1
  },
)
const absoluteIndex = (index) => (page.value - 1) * pageSize + index

const integerFormatter = new Intl.NumberFormat('vi-VN')
const moneyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

function formatInteger(value) {
  return integerFormatter.format(Number(value || 0))
}

function formatMoney(value) {
  return moneyFormatter.format(Number(value || 0))
}

function formatTrend(value) {
  if (value === null || value === undefined) return 'Sản phẩm mới'
  const sign = value > 0 ? '+' : ''
  return `${sign}${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 2 })}%`
}

function trendClass(value) {
  if (value === null || value === undefined) return 'new'
  if (value > 0) return 'up'
  if (value < 0) return 'down'
  return 'steady'
}

function trendIcon(value) {
  if (value === null || value === undefined) return 'bi bi-stars'
  if (value > 0) return 'bi bi-arrow-up-right'
  if (value < 0) return 'bi bi-arrow-down-right'
  return 'bi bi-dash'
}
</script>
