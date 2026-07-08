<template>
  <div class="rte">
    <div class="rte-toolbar">
      <button type="button" class="rte-btn" @mousedown.prevent="exec('bold')" title="In đậm (Ctrl+B)">
        <b>B</b>
      </button>
      <button type="button" class="rte-btn" @mousedown.prevent="exec('italic')" title="In nghiêng (Ctrl+I)">
        <i>I</i>
      </button>
      <button type="button" class="rte-btn" @mousedown.prevent="exec('underline')" title="Gạch chân (Ctrl+U)">
        <u>U</u>
      </button>

      <span class="rte-sep"></span>

      <label class="rte-color-btn" title="Màu chữ">
        <span class="color-swatch" :style="{ background: currentColor }"></span>
        <input type="color" v-model="currentColor" @input="applyColor" />
      </label>
      <button type="button" class="rte-btn" title="Bỏ định dạng" @mousedown.prevent="exec('removeFormat')">
        A<span class="strike-x">×</span>
      </button>

      <span class="rte-sep"></span>

      <button type="button" class="rte-btn" @mousedown.prevent="exec('insertUnorderedList')" title="Danh sách chấm">
        •≡
      </button>
      <button type="button" class="rte-btn" @mousedown.prevent="exec('insertOrderedList')" title="Danh sách số">
        1≡
      </button>
      <button type="button" class="rte-btn" @mousedown.prevent="insertLink" title="Chèn liên kết">
        🔗
      </button>

      <span class="rte-sep"></span>

      <button type="button" class="rte-btn" @mousedown.prevent="exec('undo')" title="Hoàn tác (Ctrl+Z)">↶</button>
      <button type="button" class="rte-btn" @mousedown.prevent="exec('redo')" title="Làm lại (Ctrl+Y)">↷</button>
    </div>

    <div
      ref="editorEl"
      class="rte-editable"
      contenteditable="true"
      @input="onInput"
      @blur="$emit('blur')"
      @paste="onPaste"
    ></div>
  </div>
</template>

<script setup>
// Trình soạn thảo WYSIWYG dùng contenteditable + document.execCommand của trình duyệt —
// KHÔNG cần cài thêm thư viện ngoài nào. Ctrl+Z/Ctrl+Y hoạt động tự nhiên vì đây là
// hành vi gốc của trình duyệt với vùng contenteditable, không phải tự viết tay.
import { ref, onMounted, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'blur'])

const editorEl = ref(null)
const currentColor = ref('#202636')

onMounted(() => {
  editorEl.value.innerHTML = props.modelValue || ''
})

// Đồng bộ khi form.content bị set từ bên ngoài (ví dụ khi load bài để sửa),
// nhưng không ghi đè trong lúc đang gõ (tránh nhảy con trỏ).
watch(
  () => props.modelValue,
  (val) => {
    if (editorEl.value && document.activeElement !== editorEl.value && val !== editorEl.value.innerHTML) {
      editorEl.value.innerHTML = val || ''
    }
  },
)

function onInput() {
  emit('update:modelValue', editorEl.value.innerHTML)
}

function exec(command, value = null) {
  editorEl.value.focus()
  document.execCommand(command, false, value)
  onInput()
}

function applyColor() {
  exec('foreColor', currentColor.value)
}

function insertLink() {
  const url = window.prompt('Nhập URL liên kết:', 'https://')
  if (!url) return
  exec('createLink', url)
}

function onPaste(e) {
  e.preventDefault()
  const text = (e.clipboardData || window.clipboardData).getData('text/plain')
  document.execCommand('insertText', false, text)
  onInput()
}
</script>

<style scoped>
.rte {
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  overflow: hidden;
  background: #fffafd;
}
.rte-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  background: #fff0f7;
  border-bottom: 1px solid #f3d6e3;
  padding: 6px;
}
.rte-sep {
  width: 1px;
  height: 20px;
  background: #f3d6e3;
  margin: 0 4px;
}
.rte-btn {
  min-width: 30px;
  height: 30px;
  padding: 0 6px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #b4557d;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.rte-btn:hover {
  background: #ffe4ef;
}
.rte-color-btn {
  position: relative;
  width: 30px;
  height: 30px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.rte-color-btn:hover {
  background: #ffe4ef;
}
.color-swatch {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid rgba(0, 0, 0, 0.15);
}
.rte-color-btn input[type='color'] {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.strike-x {
  font-size: 10px;
  margin-left: 1px;
}

.rte-editable {
  min-height: 220px;
  padding: 12px;
  outline: none;
  font-size: 13.5px;
  line-height: 1.6;
  color: #202636;
}
.rte-editable:empty::before {
  content: 'Nhập nội dung chi tiết bài viết tại đây...';
  color: #94a3b8;
}
.rte-editable p {
  margin: 0 0 0.75em;
}
.rte-editable ul,
.rte-editable ol {
  padding-left: 1.4em;
  margin: 0 0 0.75em;
}
.rte-editable a {
  color: #d63384;
  text-decoration: underline;
}
</style>