<template>
  <div class="filter-wrap">
    <div class="filter-group">
      <label class="filter-label">TÌM KIẾM</label>
      <div class="search-box">
        <i class="ti ti-search search-icon"></i>
        <input
          class="filter-input"
          type="text"
          placeholder="Tìm theo tiêu đề banner..."
          :value="filters.search"
          @input="$emit('update:search', $event.target.value)"
        />
      </div>
    </div>

    <div class="filter-group">
      <label class="filter-label">VỊ TRÍ</label>
      <div class="select-wrap">
        <select
          class="filter-select"
          :value="filters.position"
          @change="$emit('update:position', $event.target.value)"
        >
          <option value="">Tất cả</option>
          <option v-for="p in positions" :key="p.value" :value="p.value">
            {{ p.label }}
          </option>
        </select>
        <i class="ti ti-chevron-down select-arrow"></i>
      </div>
    </div>

    <div class="filter-group">
      <label class="filter-label">TRẠNG THÁI</label>
      <div class="select-wrap">
        <select
          class="filter-select"
          :value="filters.status"
          @change="$emit('update:status', $event.target.value)"
        >
          <option value="">Tất cả</option>
          <option value="active">Đang hiển thị</option>
          <option value="scheduled">Lên lịch chạy</option>
          <option value="hidden">Tạm ẩn</option>
          <option value="expired">Hết hạn</option>
        </select>
        <i class="ti ti-chevron-down select-arrow"></i>
      </div>
    </div>

    <button class="btn-reset" @click="onReset" title="Đặt lại">
      <i class="ti ti-refresh"></i>
    </button>
  </div>
</template>

<script setup>
defineProps({
  filters: { type: Object, required: true },
  positions: { type: Array, default: () => [] },
});
const emit = defineEmits(['update:search', 'update:position', 'update:status']);
function onReset() {
  emit('update:search', '');
  emit('update:position', '');
  emit('update:status', '');
}
</script>

<style scoped>
.filter-wrap {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
  padding: 20px 24px;
  border-bottom: 1px solid #f3e8ee;
}
.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 180px;
  flex: 1;
}
.filter-label {
  font-size: 11px;
  font-weight: 600;
  color: #f55d9b;
  letter-spacing: 0.06em;
}
.search-box { position: relative; }
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  font-size: 15px;
  pointer-events: none;
}
.filter-input {
  width: 100%;
  padding: 9px 12px 9px 34px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #111827;
  background: #fff;
  outline: none;
  transition: border 0.15s;
}
.filter-input:focus {
  border-color: #f55d9b;
  box-shadow: 0 0 0 3px rgba(245,93,155,0.08);
}
.filter-input::placeholder { color: #9ca3af; }
.select-wrap { position: relative; }
.filter-select {
  width: 100%;
  padding: 9px 32px 9px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #111827;
  background: #fff;
  outline: none;
  appearance: none;
  cursor: pointer;
  transition: border 0.15s;
}
.filter-select:focus {
  border-color: #f55d9b;
  box-shadow: 0 0 0 3px rgba(245,93,155,0.08);
}
.select-arrow {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  font-size: 14px;
  pointer-events: none;
}
.btn-reset {
  padding: 9px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.15s;
  flex-shrink: 0;
  margin-bottom: 0;
}
.btn-reset:hover {
  border-color: #f55d9b;
  color: #f55d9b;
  background: #fff0f7;
}
</style>