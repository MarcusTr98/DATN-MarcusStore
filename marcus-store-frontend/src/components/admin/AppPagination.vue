<template>
  <nav class="app-pagination" aria-label="Phân trang">
    <span>Trang {{ modelValue }} / {{ totalPages }}</span>
    <div>
      <button
        type="button"
        :disabled="modelValue <= 1"
        @click="$emit('update:modelValue', modelValue - 1)"
      >
        <i class="bi bi-chevron-left"></i>
      </button>
      <button
        type="button"
        :disabled="modelValue >= totalPages"
        @click="$emit('update:modelValue', modelValue + 1)"
      >
        <i class="bi bi-chevron-right"></i>
      </button>
    </div>
  </nav>
</template>

<script setup>
defineProps({
  modelValue: { type: Number, required: true },
  totalPages: { type: Number, required: true },
})
defineEmits(['update:modelValue'])
</script>

<style scoped src="@/assets/css/components/AppPagination.css"></style>
