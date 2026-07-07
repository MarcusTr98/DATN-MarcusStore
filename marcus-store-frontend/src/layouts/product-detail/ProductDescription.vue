<template>
  <div class="pd-desc">
    <div class="pd-desc__tabs">
      <button
        v-for="t in tabs"
        :key="t.value"
        type="button"
        class="pd-desc__tab"
        :class="{ active: activeTab === t.value }"
        @click="activeTab = t.value"
      >
        {{ t.label }}
      </button>
    </div>

    <div class="pd-desc__content">
      <!-- Mô tả -->
      <div v-if="activeTab === 'description'" class="pd-desc__section">
        <div v-if="descriptionHtml" class="pd-desc__html" v-html="descriptionHtml" />
        <p v-else class="pd-desc__empty">Chưa có mô tả cho sản phẩm này.</p>
      </div>

      <!-- Thông số kỹ thuật -->
      <div v-else-if="activeTab === 'specs'" class="pd-desc__section">
        <div v-if="specs.length > 0" class="pd-desc__specs">
          <div v-for="(row, idx) in specs" :key="idx" class="pd-desc__spec-row">
            <span class="pd-desc__spec-label">{{ row.label }}</span>
            <span class="pd-desc__spec-value">{{ row.value }}</span>
          </div>
        </div>
        <p v-else class="pd-desc__empty">Chưa có thông số kỹ thuật.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  description: { type: String, default: '' },
  specifications: { type: Array, default: () => [] },
  currentSku: { type: Object, default: null },
  productName: { type: String, default: '' },
  brand: { type: String, default: '' },
  totalSkus: { type: Number, default: 0 },
  totalStock: { type: Number, default: 0 },
})

const tabs = [
  { value: 'description', label: 'Mô tả sản phẩm' },
  { value: 'specs', label: 'Thông số kỹ thuật' },
]
const activeTab = ref('description')

const descriptionHtml = computed(() => {
  if (!props.description) return ''
  return props.description
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br/>')
})

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
</script>

<style scoped>
.pd-desc {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
  overflow: hidden;
}

.pd-desc__tabs {
  display: flex;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}

.pd-desc__tab {
  flex: 1;
  padding: 14px 16px;
  background: transparent;
  border: none;
  font-size: 14px;
  font-weight: 600;
  color: #555;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.15s ease;
}
.pd-desc__tab:hover {
  color: #e11d1d;
}
.pd-desc__tab.active {
  color: #e11d1d;
  background: #fff;
  border-bottom-color: #e11d1d;
}

.pd-desc__content {
  padding: 20px 24px;
}

.pd-desc__html {
  font-size: 14.5px;
  line-height: 1.7;
  color: #333;
  white-space: pre-wrap;
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
  grid-template-columns: 200px 1fr;
  gap: 12px;
  padding: 12px 16px;
  font-size: 14px;
  background: #fff;
}
.pd-desc__spec-row:nth-child(odd) {
  background: #fafafa;
}

.pd-desc__spec-label {
  color: #666;
  font-weight: 500;
}
.pd-desc__spec-value {
  color: #1a1a1a;
  font-weight: 600;
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
  }
}
</style>
