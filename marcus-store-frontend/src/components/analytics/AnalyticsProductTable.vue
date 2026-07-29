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
          <tr v-for="(product, index) in products" :key="product.productId">
            <td><span class="analytics-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span></td>
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
    <div v-else class="analytics-empty">
      <i class="bi bi-box-seam"></i>
      <p>Chưa có sản phẩm bán hoàn tất trong khoảng này.</p>
    </div>
  </article>
</template>

<script setup>
defineProps({
  products: { type: Array, required: true },
})

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
