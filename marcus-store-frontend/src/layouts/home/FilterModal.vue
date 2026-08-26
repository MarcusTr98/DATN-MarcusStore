<template>
  <Teleport to="body">
    <Transition name="fm">
      <div v-if="visible" class="fm-overlay" @click.self="$emit('close')">
        <div class="fm-panel">

          <!-- Header -->
          <div class="fm-header">
            <h5 class="fm-title">Bộ lọc</h5>
            <button class="fm-close" @click="$emit('close')">
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="fm-body text-center py-5">
            <span class="text-muted">Đang tải bộ lọc...</span>
          </div>

          <!-- Error -->
          <div v-else-if="loadError" class="fm-body text-center py-5 text-danger">
            {{ loadError }}
          </div>

          <!-- Nội dung filter -->
          <div v-else class="fm-body">

            <div
              v-for="group in filterGroups"
              :key="group.attributeId ?? group.attributeName"
              class="fm-group"
            >
              <template v-if="group.attributeName === 'Hãng'">
                <div class="group-label">Hãng</div>
                <div class="brand-grid">
                  <button
                    v-for="opt in group.options"
                    :key="opt.valueId"
                    type="button"
                    class="brand-item"
                    :class="{ active: localSelectedBrands.includes(opt.valueId) }"
                    @click="toggleBrand(opt.valueId)"
                  >
                    <img :src="opt.categoryImg" :alt="opt.label" class="brand-logo" />
                  </button>
                </div>
              </template>

              <!-- ===== Giá: chips nhanh + input khoảng ===== -->
              <template v-else-if="group.attributeName === 'Giá'">
                <div class="group-label">Giá</div>
                <div class="price-chips">
                  <button
                    v-for="opt in group.options"
                    :key="opt.extra"
                    type="button"
                    class="price-chip"
                    :class="{ active: localPriceRange === opt.extra }"
                    @click="togglePrice(opt.extra)"
                  >
                    {{ opt.label }}
                  </button>
                </div>
                <div class="price-range-row">
                  <div class="price-input-wrap">
                    <span class="price-input-label">Từ</span>
                    <input
                      type="number"
                      class="price-input"
                      placeholder="Giá thấp nhất"
                      v-model.number="localMinPrice"
                    />
                    <span class="price-unit">đ</span>
                  </div>
                  <span class="price-sep">—</span>
                  <div class="price-input-wrap">
                    <span class="price-input-label">Đến</span>
                    <input
                      type="number"
                      class="price-input"
                      placeholder="Giá cao nhất"
                      v-model.number="localMaxPrice"
                    />
                    <span class="price-unit">đ</span>
                  </div>
                </div>
              </template>

              <!-- ===== Nhu cầu / attributes: grid icon + tên ===== -->
              <template v-else>
                <div class="group-label">{{ group.attributeName }}</div>
                <div class="demand-grid">
                  <button
                    v-for="opt in group.options"
                    :key="opt.valueId"
                    type="button"
                    class="demand-chip"
                    :class="{ active: localSelectedValues.includes(opt.valueId) }"
                    @click="toggleValue(opt.valueId)">
                    <span class="demand-chip-name">{{ opt.label }}</span>
                  </button>
                </div>
              </template>
            </div>
          </div>

          <!-- Footer -->
          <div class="fm-footer">
            <button type="button" class="fm-btn fm-btn-outline" @click="onReset">
              Đặt lại
            </button>
            <button type="button" class="fm-btn fm-btn-primary" @click="onApply">
              Xem kết quả
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '@/utils/api'

const props = defineProps({
  visible: Boolean,
  categoryId: Number,
})

const emit = defineEmits(['close', 'apply'])

const loading = ref(false)
const loadError = ref(null)
const filterGroups = ref([])

// Local selections (synced when modal opens)
const localSelectedBrands = ref([])
const localPriceRange = ref(null)
const localMinPrice = ref(null)
const localMaxPrice = ref(null)
const localSelectedValues = ref([])

// Saved selections (persistent across opens)
const savedSelectedBrands = ref([])
const savedPriceRange = ref(null)
const savedMinPrice = ref(null)
const savedMaxPrice = ref(null)
const savedSelectedValues = ref([])

watch(
  () => props.visible,
  async (isVisible) => {
    if (!isVisible) return

    // Khôi phục trạng thái đã lưu
    localSelectedBrands.value = [...savedSelectedBrands.value]
    localPriceRange.value = savedPriceRange.value
    localMinPrice.value = savedMinPrice.value
    localMaxPrice.value = savedMaxPrice.value
    localSelectedValues.value = [...savedSelectedValues.value]

    loading.value = true
    loadError.value = null
    try {
      const res = await api.get(`/client/categories/${props.categoryId}/filters`)
      filterGroups.value = res.data?.data ?? []
    } catch (err) {
      console.error('Lỗi khi tải bộ lọc:', err)
      loadError.value = 'Không thể tải bộ lọc, vui lòng thử lại.'
    } finally {
      loading.value = false
    }
  },
)

function toggleBrand(valueId) {
  const idx = localSelectedBrands.value.indexOf(valueId)
  if (idx === -1) {
    localSelectedBrands.value.push(valueId)
  } else {
    localSelectedBrands.value.splice(idx, 1)
  }
}

function togglePrice(extra) {
  localPriceRange.value = localPriceRange.value === extra ? null : extra
}

function toggleValue(valueId) {
  const idx = localSelectedValues.value.indexOf(valueId)
  if (idx === -1) {
    localSelectedValues.value.push(valueId)
  } else {
    localSelectedValues.value.splice(idx, 1)
  }
}

function onReset() {
  localSelectedBrands.value = []
  localPriceRange.value = null
  localMinPrice.value = null
  localMaxPrice.value = null
  localSelectedValues.value = []
}

function onApply() {
  // Lưu trạng thái
  savedSelectedBrands.value = [...localSelectedBrands.value]
  savedPriceRange.value = localPriceRange.value
  savedMinPrice.value = localMinPrice.value
  savedMaxPrice.value = localMaxPrice.value
  savedSelectedValues.value = [...localSelectedValues.value]

  // Tính min/max price: ưu tiên input thủ công, fallback chip nhanh
  let minPrice = localMinPrice.value ?? null
  let maxPrice = localMaxPrice.value ?? null
  if (minPrice == null && localPriceRange.value) {
    const parts = localPriceRange.value.split('-')
    if (parts[0] && parts[0] !== '') minPrice = Number(parts[0])
    if (parts[1] && parts[1] !== '') maxPrice = Number(parts[1])
  }

  emit('apply', {
    brandIds: localSelectedBrands.value,
    minPrice,
    maxPrice,
    valueIds: localSelectedValues.value,
  })
  emit('close')
}
</script>

<style scoped>
.fm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1050;
  display: flex;
  justify-content: center;
  align-items: center;
}

.fm-panel {
  width: min(600px, 96vw);
  max-height: 88vh;
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.22);
}

/* Header */
.fm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px 14px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.fm-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.fm-close {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 4px;
  line-height: 1;
}

.fm-close:hover {
  color: #333;
}

/* Body */
.fm-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.fm-group {
  margin-bottom: 26px;
}

.fm-group:last-child {
  margin-bottom: 0;
}

.group-label {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

/* ===== Brand: dùng lại style từ BrandFilterBar ===== */
.brand-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
}

.brand-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 46px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 6px 10px;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.brand-item:hover {
  border-color: #bbb;
}

.brand-item.active {
  border-color: #d70018;
  box-shadow: 0 0 0 1px #d70018 inset;
}

.brand-logo {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

/* ===== Price: chips + input range ===== */
.price-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.price-chip {
  padding: 7px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  background: #fff;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    color 0.15s ease,
    background 0.15s ease;
}

.price-chip:hover {
  border-color: #bbb;
}

.price-chip.active {
  border-color: #d70018;
  color: #d70018;
  background: #fff0f0;
}

.price-range-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.price-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 8px 10px;
  background: #fafafa;
}

.price-input-label {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.price-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 13px;
  color: #333;
  outline: none;
  min-width: 0;
}

.price-input::placeholder {
  color: #bbb;
}

.price-unit {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.price-sep {
  color: #bbb;
  font-size: 16px;
  flex-shrink: 0;
}

/* ===== Nhu cầu / attributes: grid icon + tên ===== */
.demand-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 10px;
}

.demand-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 10px 8px 8px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
  min-height: 74px;
}

.demand-chip:hover {
  border-color: #ccc;
}

.demand-chip.active {
  border-color: #d70018;
  box-shadow: 0 0 0 2px rgba(215, 0, 24, 0.15);
}

.demand-chip-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 20px;
  color: #999;
}

.demand-chip.active .demand-chip-icon {
  background: #fff0f0;
  color: #d70018;
}

.demand-chip-name {
  font-size: 12px;
  font-weight: 500;
  color: #333;
  text-align: center;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Footer */
.fm-footer {
  padding: 14px 24px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.fm-btn {
  flex: 1;
  height: 42px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.fm-btn-outline {
  background: #f1f1f1;
  border: none;
  color: #444;
}

.fm-btn-outline:hover {
  background: #e2e2e2;
}

.fm-btn-primary {
  background: #d70018;
  border: none;
  color: #fff;
}

.fm-btn-primary:hover {
  background: #b80015;
}

/* Transition */
.fm-enter-active,
.fm-leave-active {
  transition: opacity 0.2s ease;
}

.fm-enter-from,
.fm-leave-to {
  opacity: 0;
}
</style>
