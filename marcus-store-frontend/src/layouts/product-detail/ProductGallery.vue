<template>
  <div class="pd-gallery">
    <!-- Ảnh chính -->
    <div class="pd-gallery__main">
      <div v-if="discountPercent > 0" class="pd-gallery__discount-badge">
        -{{ discountPercent }}%
      </div>

      <img
        v-if="activeImage"
        :src="activeImage"
        :alt="productName"
        class="pd-gallery__main-img"
      />
      <div v-else class="pd-gallery__placeholder">
        <i class="ti ti-photo" aria-hidden="true"></i>
        <span>Chưa có ảnh</span>
      </div>
    </div>

    <!-- Thumbnails -->
    <div v-if="imageList.length > 1" class="pd-gallery__thumbs">
      <button
        v-for="(img, idx) in imageList"
        :key="img.imageId || idx"
        type="button"
        class="pd-gallery__thumb"
        :class="{ active: idx === activeIndex }"
        @click="selectImage(idx)"
        @mouseenter="onHover(idx)"
      >
        <img :src="img.imageUrl" :alt="`Ảnh ${idx + 1}`" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  productName: { type: String, default: '' },
  images: { type: Array, default: () => [] },
  thumbnailUrl: { type: String, default: '' },
  discountPercent: { type: Number, default: 0 },
})

// Gom tất cả ảnh: ưu tiên images[] từ BE, fallback thumbnailUrl
const imageList = computed(() => {
  if (Array.isArray(props.images) && props.images.length > 0) {
    return props.images
      .filter((i) => i?.imageUrl)
      .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
  }
  if (props.thumbnailUrl) {
    return [{ imageId: 0, imageUrl: props.thumbnailUrl, displayOrder: 0, isPrimary: true }]
  }
  return []
})

const activeIndex = ref(0)
const activeImage = computed(() => imageList.value[activeIndex.value]?.imageUrl || '')

function selectImage(idx) {
  activeIndex.value = idx
}
function onHover(idx) {
  // TGDD style: hover thumbnail -> đổi ảnh chính ngay
  activeIndex.value = idx
}

// Reset khi đổi sản phẩm
watch(
  () => props.images,
  () => {
    activeIndex.value = 0
  },
)
</script>

<style scoped>
.pd-gallery {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pd-gallery__main {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pd-gallery__main-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
}
.pd-gallery__main:hover .pd-gallery__main-img {
  transform: scale(1.04);
}

.pd-gallery__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #aaa;
  font-size: 14px;
}
.pd-gallery__placeholder i {
  font-size: 48px;
}

.pd-gallery__discount-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: #e11d1d;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 4px;
  z-index: 2;
}

.pd-gallery__thumbs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: thin;
  padding-bottom: 4px;
}

.pd-gallery__thumb {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  padding: 0;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.15s ease;
}
.pd-gallery__thumb:hover {
  border-color: #bbb;
}
.pd-gallery__thumb.active {
  border-color: #e11d1d;
  box-shadow: 0 0 0 1px #e11d1d inset;
}
.pd-gallery__thumb img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
</style>
