<template>
  <div class="pd-policy">
    <div class="pd-policy__heading">Cam kết sản phẩm:</div>

    <div class="pd-policy__grid">
      <div v-for="item in items" :key="item.title" class="pd-policy__item">
        <i :class="item.icon" aria-hidden="true" />
        <div class="pd-policy__text">
          <span class="pd-policy__title">{{ item.title }}</span>
          <span class="pd-policy__desc">{{ item.desc }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  totalStock: { type: Number, default: 0 },
})

const items = computed(() => [
  {
    icon: 'ti ti-shield-check',
    title: 'Bảo hành chính hãng',
    desc: '12 tháng tại trung tâm',
  },
  {
    icon: 'ti ti-refresh',
    title: 'Đổi trả 30 ngày',
    desc: 'Miễn phí nếu lỗi nhà sản xuất',
  },
  {
    icon: 'ti ti-truck',
    title: props.totalStock > 0 ? 'Giao nhanh 2h' : 'Đặt trước',
    desc: 'Freeship đơn từ 299k',
  },
])
</script>

<style scoped>
.pd-policy {
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 14px 16px;
}

.pd-policy__heading {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 10px;
}

.pd-policy__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.pd-policy__item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.pd-policy__item i {
  font-size: 22px;
  color: #e11d1d;
  flex-shrink: 0;
  margin-top: 2px;
}

.pd-policy__text {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  min-width: 0;
}

.pd-policy__title {
  font-size: 13.5px;
  font-weight: 600;
  color: #1a1a1a;
}

.pd-policy__desc {
  font-size: 12px;
  color: #777;
  white-space: normal;
  word-break: break-word;
}

@media (max-width: 768px) {
  .pd-policy__grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>