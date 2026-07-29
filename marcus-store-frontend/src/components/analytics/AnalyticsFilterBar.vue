<template>
  <section class="analytics-filter" aria-label="Bộ lọc thời gian">
    <div class="analytics-filter__presets">
      <button
        v-for="preset in presets"
        :key="preset.key"
        type="button"
        :class="{ active: activePreset === preset.key }"
        :disabled="loading"
        @click="$emit('select-preset', preset.key)"
      >
        {{ preset.label }}
      </button>
    </div>

    <form class="analytics-filter__dates" @submit.prevent="$emit('apply-custom')">
      <label>
        <span>Từ ngày</span>
        <input
          :value="fromDate"
          type="date"
          min="2021-01-01"
          :max="toDate || today"
          @input="$emit('update:fromDate', $event.target.value)"
        />
      </label>
      <span class="analytics-filter__separator"><i class="bi bi-arrow-right"></i></span>
      <label>
        <span>Đến ngày</span>
        <input
          :value="toDate"
          type="date"
          :min="fromDate"
          :max="today"
          @input="$emit('update:toDate', $event.target.value)"
        />
      </label>
      <button class="analytics-filter__apply" type="submit" :disabled="loading">
        <i class="bi bi-funnel"></i>
        Áp dụng
      </button>
    </form>
  </section>
</template>

<script setup>
defineProps({
  activePreset: { type: String, required: true },
  fromDate: { type: String, required: true },
  loading: { type: Boolean, default: false },
  presets: { type: Array, required: true },
  toDate: { type: String, required: true },
  today: { type: String, required: true },
})

defineEmits(['apply-custom', 'select-preset', 'update:fromDate', 'update:toDate'])
</script>
