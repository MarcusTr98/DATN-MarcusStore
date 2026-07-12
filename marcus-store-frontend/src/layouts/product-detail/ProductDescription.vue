<template>
  <div class="pd-desc">
    <div class="pd-desc__heading">Thông số kỹ thuật</div>

    <div
      v-if="specs.length > 0"
      class="pd-desc__specs-wrap"
      :class="{ 'is-expanded': expanded }"
    >
      <div class="pd-desc__specs" ref="specsBox">
        <div v-for="(row, idx) in specs" :key="idx" class="pd-desc__spec-row">
          <span class="pd-desc__spec-label">{{ row.label }}</span>
          <span class="pd-desc__spec-value">{{ row.value }}</span>
        </div>
      </div>

      <div v-if="!expanded && isOverflowing" class="pd-desc__fade" />
    </div>
    <p v-else class="pd-desc__empty">Chưa có thông số kỹ thuật.</p>

    <button
      v-if="isOverflowing"
      type="button"
      class="pd-desc__toggle-btn"
      @click="expanded = !expanded"
    >
      {{ expanded ? 'Thu gọn' : 'Xem thêm' }}
      <i :class="expanded ? 'ti ti-chevron-up' : 'ti ti-chevron-down'" aria-hidden="true" />
    </button>
  </div>
</template>

<script setup>
import { computed, ref, nextTick, onMounted, watch } from 'vue'

const props = defineProps({
  description: { type: String, default: '' },
  specifications: { type: Array, default: () => [] },
  currentSku: { type: Object, default: null },
  productName: { type: String, default: '' },
  brand: { type: String, default: '' },
  totalSkus: { type: Number, default: 0 },
  totalStock: { type: Number, default: 0 },
})

const COLLAPSED_HEIGHT = 360 // px - khớp chiều cao với ProductSuggestions bên phải

const expanded = ref(false)
const isOverflowing = ref(false)
const specsBox = ref(null)

const specs = computed(() => {
  const rows = []
  if (props.specifications?.length > 0) {
    for (const spec of props.specifications) {
      const label = spec.specAttributeName || '—'
      let value = spec.valueText || '—'
      if (spec.unit && value !== '—') {
        value = `${value} ${spec.unit}`
      }
      rows.push({ label, value })
    }
  }

  if (props.brand) rows.push({ label: 'Thương hiệu', value: props.brand })
  rows.push({ label: 'Tên sản phẩm', value: props.productName })
  if (props.totalSkus > 0) rows.push({ label: 'Số phiên bản', value: `${props.totalSkus} phiên bản` })
  if (props.totalStock > 0)
    rows.push({ label: 'Tổng tồn kho', value: `${props.totalStock} sản phẩm` })

  const sku = props.currentSku
  if (sku) {
    if (sku.skuCode) rows.push({ label: 'Mã SKU', value: sku.skuCode })
    if (sku.weightGram) rows.push({ label: 'Khối lượng', value: `${sku.weightGram} g` })
    if (sku.stockQuantity != null)
      rows.push({ label: 'Tồn kho phiên bản', value: `${sku.stockQuantity} sản phẩm` })
    for (const av of sku.attributeValues || []) {
      rows.push({ label: av.attributeName || 'Thuộc tính', value: av.valueString || '—' })
    }
  }
  return rows
})

function checkOverflow() {
  nextTick(() => {
    if (specsBox.value) {
      isOverflowing.value = specsBox.value.scrollHeight > COLLAPSED_HEIGHT
    }
  })
}

onMounted(checkOverflow)
watch(specs, checkOverflow)
</script>

<style scoped>
.pd-desc {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 20px 24px;
}

.pd-desc__heading {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 16px;
}

.pd-desc__specs-wrap {
  position: relative;
  max-height: 360px;
  overflow: hidden;
}
.pd-desc__specs-wrap.is-expanded {
  max-height: none;
}

.pd-desc__specs {
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
}

.pd-desc__spec-row {
  display: grid;
  grid-template-columns: minmax(140px, 220px) 1fr;
  gap: 24px;
  padding: 14px 20px;
  font-size: 14px;
}
.pd-desc__spec-row:nth-child(odd) {
  background: #fafafa;
}
.pd-desc__spec-row:not(:last-child) {
  border-bottom: 1px solid #f0f0f0;
}

.pd-desc__spec-label {
  color: #666;
  font-weight: 500;
}
.pd-desc__spec-value {
  color: #1a1a1a;
  font-weight: 600;
  line-height: 1.5;
  word-break: break-word;
}

.pd-desc__fade {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 70px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0), rgba(255, 255, 255, 1));
  pointer-events: none;
}

.pd-desc__toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 14px;
  padding: 10px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 600;
  color: #e11d1d;
  cursor: pointer;
  transition: background 0.15s ease;
}
.pd-desc__toggle-btn:hover {
  background: #f5f5f5;
}

.pd-desc__empty {
  font-size: 14px;
  color: #999;
  text-align: center;
  padding: 20px 0;
}

@media (max-width: 576px) {
  .pd-desc__spec-row {
    grid-template-columns: 1fr;
    gap: 4px;
    padding: 12px 16px;
  }
}
</style>