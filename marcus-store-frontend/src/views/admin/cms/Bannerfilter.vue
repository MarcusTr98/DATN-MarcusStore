<template>
  <div class="filter-bar">
    <div class="search-wrap">
      <i class="ti ti-search"></i>
      <input
        class="search-input"
        type="text"
        placeholder="Tìm theo tiêu đề..."
        :value="filters.search"
        @input="$emit('update:search', $event.target.value)"
      />
    </div>

    <select
      class="filter-select"
      :value="filters.position"
      @change="$emit('update:position', $event.target.value)"
    >
      <option value="">Tất cả vị trí</option>
      <option v-for="p in positions" :key="p.value" :value="p.value">
        {{ p.label }}
      </option>
    </select>

    <select
      class="filter-select"
      :value="filters.status"
      @change="$emit('update:status', $event.target.value)"
    >
      <option value="">Tất cả trạng thái</option>
      <option value="active">Đang hiển thị</option>
      <option value="scheduled">Lên lịch chạy</option>
      <option value="hidden">Tạm ẩn</option>
      <option value="expired">Hết hạn</option>
    </select>
  </div>
</template>

<script setup>
defineProps({
  filters: {
    type: Object,
    required: true,
    // { search: '', position: '', status: '' }
  },
  positions: {
    type: Array,
    default: () => [], // positionId thật là số, nên không dùng default giả dạng string nữa
  },
});

defineEmits(['update:search', 'update:position', 'update:status']);
</script>

<style scoped>
.filter-bar {
  background: #fff;
  border: 1px solid #f3d6e3;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.search-wrap {
  position: relative;
  flex: 1;
  min-width: 180px;
}
.search-wrap i {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #b4557d;
  font-size: 15px;
}
.search-input {
  width: 100%;
  padding: 7px 12px 7px 32px;
  border: 1px solid #f3d6e3;
  border-radius: 7px;
  font-size: 13px;
  color: #202636;
  background: #fffafd;
  outline: none;
  transition: border 0.15s;
}
.search-input:focus {
  border-color: #f55d9b;
  background: #fff;
}
.filter-select {
  padding: 7px 12px;
  border: 1px solid #f3d6e3;
  border-radius: 7px;
  font-size: 13px;
  color: #344054;
  background: #fffafd;
  outline: none;
  cursor: pointer;
  min-width: 130px;
}
.filter-select:focus {
  border-color: #f55d9b;
}
</style>