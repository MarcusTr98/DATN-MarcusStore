<template>
  <header class="dashboard-heading">
    <div>
      <p class="eyebrow">Bảng điều khiển kinh doanh</p>
      <h1>Màn hình quản trị</h1>
      <span>Theo dõi doanh thu, đơn hàng, tồn kho và khách hàng trong một màn hình.</span>
    </div>
    <div class="time-filter">
      <button
        v-for="item in timeFilters"
        :key="item.value"
        type="button"
        :class="{ active: selectedTime === item.value && !customDate }"
        @click="onSelectPreset(item.value)"
      >{{ item.label }}</button>
      <input
        type="date"
        class="date-picker"
        :class="{ active: !!customDate }"
        :max="todayStr"
        v-model="localCustomDate"
        @change="onSelectCustomDate"
      />
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  selectedTime: { type: String, default: 'month' },
  customDate:   { type: String, default: '' },
})

const emit = defineEmits(['update:selectedTime', 'update:customDate'])

const timeFilters = [
  { label: 'Tuần',  value: 'week'  },
  { label: 'Tháng', value: 'month' },
  { label: 'Năm',   value: 'year'  },
]

const todayStr = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})()

const localCustomDate = ref(props.customDate)

function onSelectPreset(val) {
  localCustomDate.value = ''
  emit('update:customDate', '')
  emit('update:selectedTime', val)
}

function onSelectCustomDate() {
  if (!localCustomDate.value) return
  emit('update:selectedTime', '')
  emit('update:customDate', localCustomDate.value)
}
</script>

<style scoped>
.dashboard-heading {
  background: #fff;
  border: 1px solid #ffe0ec;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.06);
  border-radius: 24px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #f0528f;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.dashboard-heading h1 {
  margin: 0;
  color: #111827;
  font-weight: 900;
  font-size: 34px;
}

.dashboard-heading span {
  color: #6b7280;
}

/* Time filter */
.time-filter {
  display: inline-flex;
  gap: 8px;
  padding: 8px;
  border: 1px solid #ffe0ec;
  border-radius: 18px;
  background: #fff;
}

.time-filter button {
  padding: 11px 18px;
  border-radius: 13px;
  color: #6b7280;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-weight: 800;
  transition: 0.2s ease;
  white-space: nowrap;
}

.time-filter button.active {
  background: #ff4d8d;
  color: #fff;
  box-shadow: 0 10px 22px rgba(255, 77, 141, 0.22);
}

.time-filter .date-picker {
  border: 1.5px solid #e5e7eb;
  border-radius: 999px;
  padding: 6px 16px;
  font-size: 13px;
  color: #374151;
  background: #fff;
  cursor: pointer;
  outline: none;
transition: border-color 0.2s, background 0.2s, color 0.2s;
  font-family: inherit;
}

.time-filter .date-picker:hover {
  border-color: #ff4d8d;
}

.time-filter .date-picker.active {
  background: #ff4d8d;
  color: #fff;
  border-color: #ff4d8d;
}

.time-filter .date-picker::-webkit-calendar-picker-indicator {
  opacity: 0.4;
  cursor: pointer;
}

.time-filter .date-picker.active::-webkit-calendar-picker-indicator {
  filter: brightness(10);
}

@media (max-width: 992px) {
  .dashboard-heading {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>