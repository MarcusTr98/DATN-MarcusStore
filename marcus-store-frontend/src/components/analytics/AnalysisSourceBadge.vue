<template>
  <!-- Marcus thêm: phân biệt rõ kết luận do AI và phép tính thuật toán tạo ra. -->
  <span class="analysis-source-badge" :class="`analysis-source-badge--${source}`" tabindex="0">
    <i class="bi bi-info-circle"></i>
    {{ label }}
    <span class="analysis-source-badge__tooltip" role="tooltip">{{ description }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ source: { type: String, default: 'algorithm' } })
const isAi = computed(() => props.source === 'ai')
const label = computed(() => (isAi.value ? 'AI' : 'Thuật toán'))
const description = computed(() =>
  isAi.value
    ? 'Nội dung do Gemini phân tích từ dữ liệu tổng hợp đã kiểm duyệt. Kết quả có thể sai và cần người quản lý xác nhận.'
    : 'Kết quả được hệ thống tính trực tiếp bằng công thức, so sánh kỳ và hồi quy thống kê; không gọi mô hình AI.',
)
</script>

<style scoped>
.analysis-source-badge{position:absolute;z-index:5;top:14px;right:14px;display:inline-flex;align-items:center;gap:5px;padding:6px 9px;border-radius:999px;font-size:11px;font-weight:800;letter-spacing:.03em;cursor:help;outline:none}.analysis-source-badge--ai{color:#5b21b6;background:#f1eaff;border:1px solid #d8c4ff}.analysis-source-badge--algorithm{color:#1559a7;background:#e9f3ff;border:1px solid #c6ddf8}.analysis-source-badge__tooltip{position:absolute;right:0;top:calc(100% + 9px);width:min(265px,calc(100vw - 40px));padding:10px 12px;border-radius:11px;background:#10233f;color:#fff;font-size:12px;font-weight:500;line-height:1.45;letter-spacing:0;box-shadow:0 12px 30px rgba(15,35,65,.24);opacity:0;visibility:hidden;transform:translateY(-4px);transition:.18s;pointer-events:none}.analysis-source-badge__tooltip::before{content:"";position:absolute;right:18px;top:-5px;width:10px;height:10px;background:#10233f;transform:rotate(45deg)}.analysis-source-badge:hover .analysis-source-badge__tooltip,.analysis-source-badge:focus .analysis-source-badge__tooltip,.analysis-source-badge:focus-within .analysis-source-badge__tooltip{opacity:1;visibility:visible;transform:translateY(0)}

/* Marcus sửa: thu gọn badge ở màn nhỏ để không che nội dung card. */
@media (max-width: 575px){.analysis-source-badge{top:10px;right:10px;padding:5px 7px;font-size:10px}.analysis-source-badge__tooltip{right:-2px}}
</style>
