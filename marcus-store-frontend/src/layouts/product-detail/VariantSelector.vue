<template>
  <div class="pd-variants">
    <div
      v-for="group in groupedAttributes"
      :key="group.attributeId"
      class="pd-variants__group"
    >
      <div class="pd-variants__label">
        <span class="pd-variants__label-name">{{ group.attributeName }}:</span>
        <span class="pd-variants__label-value">{{ group.selectedValue || '—' }}</span>
      </div>

      <div class="pd-variants__chips">
        <button
          v-for="val in group.values"
          :key="val.valueId"
          type="button"
          class="pd-variants__chip"
          :class="{
            active: group.selectedValueId === val.valueId,
            disabled: !val.available,
          }"
          :disabled="!val.available"
          :title="!val.available ? 'Phiên bản tạm hết hàng' : ''"
          @click="onSelect(group.attributeId, val)"
        >
          <span v-if="val.valueMeta" class="pd-variants__chip-meta" :style="metaStyle(val.valueMeta)" />
          <span class="pd-variants__chip-text">{{ val.valueString }}</span>
          <span v-if="!val.available" class="pd-variants__chip-strike" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  skus: { type: Array, default: () => [] },
  selectedSku: { type: Object, default: null },
})
const emit = defineEmits(['change'])

// ===== Gom các value theo attributeId =====
const groupedAttributes = computed(() => {
  const map = new Map()

  for (const sku of props.skus || []) {
    if (!sku?.isActive) continue
    for (const av of sku.attributeValues || []) {
      if (!av?.attributeId) continue

      if (!map.has(av.attributeId)) {
        map.set(av.attributeId, {
          attributeId: av.attributeId,
          attributeName: av.attributeName || `Thuộc tính ${av.attributeId}`,
          values: new Map(),
          selectedValueId: null,
          selectedValue: null,
        })
      }

      const group = map.get(av.attributeId)
      const existing = group.values.get(av.valueId)
      // value có thể xuất hiện ở nhiều SKU, gộp lại - ưu tiên SKU còn hàng
      if (!existing) {
        group.values.set(av.valueId, {
          valueId: av.valueId,
          valueString: av.valueString,
          valueMeta: av.valueMeta,
          available: sku.inStock === true,
        })
      } else if (sku.inStock === true) {
        existing.available = true
      }
    }
  }

  // Xác định selectedValueId dựa trên selectedSku hiện tại
  for (const av of props.selectedSku?.attributeValues || []) {
    const group = map.get(av.attributeId)
    if (group) {
      group.selectedValueId = av.valueId
      group.selectedValue = av.valueString
    }
  }

  return Array.from(map.values()).map((g) => ({
    ...g,
    values: Array.from(g.values.values()),
  }))
})

// Style cho value meta (thường là màu: #hex)
function metaStyle(meta) {
  if (!meta) return {}
  // Nếu là mã màu (#aabbcc hoặc rgb)
  if (/^#([0-9a-f]{3}|[0-9a-f]{6})$/i.test(meta) || /^rgb/i.test(meta)) {
    return { background: meta }
  }
  return {}
}

function onSelect(attributeId, val) {
  if (!val.available) return
  emit('change', { attributeId, valueId: val.valueId, valueString: val.valueString })
}
</script>

<style scoped>
.pd-variants {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.pd-variants__group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pd-variants__label {
  font-size: 14px;
  color: #444;
}
.pd-variants__label-name {
  font-weight: 600;
  color: #1a1a1a;
}
.pd-variants__label-value {
  margin-left: 4px;
  color: #e11d1d;
  font-weight: 600;
}

.pd-variants__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pd-variants__chip {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 64px;
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13.5px;
  color: #1a1a1a;
  cursor: pointer;
  transition: all 0.15s ease;
}
.pd-variants__chip:hover:not(.disabled) {
  border-color: #e11d1d;
  color: #e11d1d;
}
.pd-variants__chip.active {
  border-color: #e11d1d;
  background: #fff5f5;
  color: #e11d1d;
  font-weight: 600;
}
.pd-variants__chip.disabled {
  background: #f5f5f5;
  color: #bbb;
  cursor: not-allowed;
}

.pd-variants__chip-meta {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid #ddd;
}

.pd-variants__chip-strike {
  position: absolute;
  top: 50%;
  left: 6px;
  right: 6px;
  height: 1px;
  background: #bbb;
  transform: rotate(-12deg);
}
</style>
