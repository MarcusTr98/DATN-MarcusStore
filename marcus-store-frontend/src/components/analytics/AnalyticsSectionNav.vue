<template>
  <nav class="analytics-section-nav" aria-label="Điều hướng nhanh báo cáo phân tích">
    <span><i class="bi bi-compass"></i> Xem nhanh</span>
    <button v-for="item in items" :key="item.id" type="button" @click="goTo(item.id)">
      <i :class="item.icon"></i>{{ item.label }}
    </button>
  </nav>
</template>

<script setup>
// Marcus thêm: trang phân tích dài cần menu theo mục, không buộc Admin cuộn dò card.
const items = [
  { id: 'ai-conclusion', label: 'Kết luận AI', icon: 'bi bi-stars' },
  { id: 'forecast', label: 'Dự báo', icon: 'bi bi-graph-up-arrow' },
  { id: 'ai-effectiveness', label: 'Hiệu quả AI', icon: 'bi bi-bullseye' },
  { id: 'business-evidence', label: 'Số liệu kiểm chứng', icon: 'bi bi-database-check' },
  { id: 'product-quality', label: 'Sản phẩm & chất lượng', icon: 'bi bi-box-seam' },
]

function goTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.analytics-section-nav {
  position: sticky;
  z-index: 20;
  top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0 18px;
  padding: 10px;
  border: 1px solid #cfc2fb;
  border-radius: 16px;
  background: linear-gradient(105deg, rgba(245, 240, 255, 0.97), rgba(239, 247, 255, 0.97));
  box-shadow: 0 12px 28px rgba(91, 33, 182, 0.14);
  backdrop-filter: blur(12px);
}
.analytics-section-nav > span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 10px;
  color: #5b21b6;
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
}
.analytics-section-nav button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 12px;
  border: 1px solid #ddd4fb;
  border-radius: 11px;
  background: #fff;
  color: #3f3a73;
  font-size: 11px;
  font-weight: 850;
  white-space: nowrap;
  box-shadow: 0 3px 9px rgba(80, 61, 143, 0.07);
  transition: 0.18s;
}
.analytics-section-nav button i {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border-radius: 7px;
  color: #fff;
  background: linear-gradient(135deg, #6d28d9, #3b82f6);
}
.analytics-section-nav button:nth-of-type(2) i {
  background: linear-gradient(135deg, #2563eb, #06b6d4);
}
.analytics-section-nav button:nth-of-type(3) i {
  background: linear-gradient(135deg, #7c3aed, #ec4899);
}
.analytics-section-nav button:nth-of-type(4) i {
  background: linear-gradient(135deg, #0f766e, #22c55e);
}
.analytics-section-nav button:nth-of-type(5) i {
  background: linear-gradient(135deg, #ea580c, #f59e0b);
}
.analytics-section-nav button:hover {
  border-color: #9f85ed;
  background: #fff;
  color: #4c1d95;
  box-shadow: 0 7px 16px rgba(91, 33, 182, 0.16);
  transform: translateY(-2px);
}
@media (max-width: 900px) {
  .analytics-section-nav {
    overflow-x: auto;
  }
  .analytics-section-nav > span {
    display: none;
  }
}
</style>
