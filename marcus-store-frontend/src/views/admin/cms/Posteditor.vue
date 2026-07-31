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

      <!-- FIX #2: Thay window.prompt bằng inline link input -->
      <button type="button" class="rte-btn" @mousedown.prevent="toggleLinkInput" title="Chèn liên kết">
        🔗
      </button>

      <span class="rte-sep"></span>

      <button type="button" class="rte-btn" @mousedown.prevent="exec('undo')" title="Hoàn tác (Ctrl+Z)">↶</button>
      <button type="button" class="rte-btn" @mousedown.prevent="exec('redo')" title="Làm lại (Ctrl+Y)">↷</button>

      <!-- FIX #4: Counter ký tự — cảnh báo khi gần đến giới hạn 50000 -->
      <span class="rte-counter" :class="{ warn: charCount > 45000, danger: charCount > 49000 }">
        {{ charCount.toLocaleString('vi-VN') }}/50.000
      </span>
    </div>

    <!-- FIX #2: Inline link input thay window.prompt -->
    <div v-if="showLinkInput" class="rte-link-bar">
      <input
        ref="linkInputEl"
        class="rte-link-input"
        type="url"
        v-model="linkUrl"
        placeholder="https://..."
        @keydown.enter.prevent="confirmLink"
        @keydown.escape="cancelLink"
      />
      <button type="button" class="rte-link-btn confirm" @mousedown.prevent="confirmLink">Chèn</button>
      <button type="button" class="rte-link-btn cancel" @mousedown.prevent="cancelLink">Huỷ</button>
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
/**
 * PostEditor — WYSIWYG dùng contenteditable + execCommand (không cần thư viện ngoài).
 * Lưu ý: execCommand đã bị W3C mark deprecated từ 2016 nhưng vẫn hoạt động
 * trên tất cả trình duyệt hiện tại. Nếu dự án mở rộng lớn hơn thì nên
 * chuyển sang TipTap hoặc Quill.
 */
import { ref, onMounted, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'blur'])

const editorEl = ref(null)
const currentColor = ref('#202636')

// FIX #4: Đếm ký tự plain text (không tính thẻ HTML)
const charCount = ref(0)

function updateCharCount() {
  if (!editorEl.value) return
  const text = editorEl.value.innerText || ''
  charCount.value = text.length
}

onMounted(() => {
  editorEl.value.innerHTML = props.modelValue || ''
  updateCharCount()
})

// Đồng bộ khi form.content bị set từ bên ngoài (ví dụ khi load bài để sửa),
// nhưng không ghi đè trong lúc đang gõ (tránh nhảy con trỏ).
watch(
  () => props.modelValue,
  (val) => {
    if (
      editorEl.value &&
      document.activeElement !== editorEl.value &&
      val !== editorEl.value.innerHTML
    ) {
      editorEl.value.innerHTML = val || ''
      updateCharCount()
    }
  },
)

function onInput() {
  updateCharCount()
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

// ── FIX #2: Inline link input ──────────────────────────────────────────
const showLinkInput = ref(false)
const linkUrl = ref('https://')
const linkInputEl = ref(null)
let savedRange = null  // lưu vị trí con trỏ trước khi focus vào input

function toggleLinkInput() {
  if (showLinkInput.value) {
    cancelLink()
    return
  }
  // Lưu selection hiện tại trước khi mất focus vào input
  const sel = window.getSelection()
  if (sel && sel.rangeCount > 0) {
    savedRange = sel.getRangeAt(0).cloneRange()
  }
  showLinkInput.value = true
  linkUrl.value = 'https://'
  nextTick(() => linkInputEl.value?.focus())
}

function confirmLink() {
  const url = linkUrl.value.trim()
  if (!url || url === 'https://') {
    cancelLink()
    return
  }
  // Restore selection rồi insert link
  editorEl.value.focus()
  if (savedRange) {
    const sel = window.getSelection()
    sel.removeAllRanges()
    sel.addRange(savedRange)
  }
  exec('createLink', url)
  cancelLink()
}

function cancelLink() {
  showLinkInput.value = false
  linkUrl.value = 'https://'
  savedRange = null
}
// ───────────────────────────────────────────────────────────────────────

// ── FIX #1: onPaste — cho phép paste HTML nhưng sanitize trước ──────────
// Trước đây strip toàn bộ về plain text → mất formatting khi copy từ bài cũ.
// Giờ: nếu clipboard có HTML thì giữ lại, nhưng chạy qua DOMParser để
// loại bỏ <script>, onclick, style nguy hiểm — giữ lại bold/italic/link/list.
const ALLOWED_TAGS = new Set([
  'p', 'br', 'b', 'strong', 'i', 'em', 'u', 's', 'strike',
  'ul', 'ol', 'li', 'a', 'span', 'div', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
])
const ALLOWED_ATTRS = { a: ['href', 'data-product-link'], span: ['data-hot-badge', 'style'], '*': ['style'] }

function sanitizePastedHtml(html) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  function cleanNode(node) {
    if (node.nodeType === Node.TEXT_NODE) return
    if (node.nodeType === Node.ELEMENT_NODE) {
      const tag = node.tagName.toLowerCase()
      if (!ALLOWED_TAGS.has(tag)) {
        // Thay thế node không hợp lệ bằng nội dung text của nó
        node.replaceWith(document.createTextNode(node.textContent))
        return
      }
      // Xoá attribute không được phép
      const allowedForTag = [...(ALLOWED_ATTRS[tag] || []), ...(ALLOWED_ATTRS['*'] || [])]
      Array.from(node.attributes).forEach(attr => {
        if (!allowedForTag.includes(attr.name)) node.removeAttribute(attr.name)
      })
      // Xoá style nguy hiểm (giữ color, font-weight, font-style, text-decoration)
      if (node.style) {
        const keepStyles = ['color', 'font-weight', 'font-style', 'text-decoration']
        Array.from(node.style).forEach(prop => {
          if (!keepStyles.includes(prop)) node.style.removeProperty(prop)
        })
      }
    }
    Array.from(node.childNodes).forEach(cleanNode)
  }

  cleanNode(doc.body)
  return doc.body.innerHTML
}

function onPaste(e) {
  e.preventDefault()
  const clipboardData = e.clipboardData || window.clipboardData
  const htmlData = clipboardData.getData('text/html')

  if (htmlData) {
    // Có HTML → sanitize rồi insert, giữ formatting
    const clean = sanitizePastedHtml(htmlData)
    document.execCommand('insertHTML', false, clean)
  } else {
    // Không có HTML (paste từ terminal/text editor) → insert plain text
    const text = clipboardData.getData('text/plain')
    document.execCommand('insertText', false, text)
  }
  onInput()
}
// ───────────────────────────────────────────────────────────────────────
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
.rte-btn:hover { background: #ffe4ef; }

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
.rte-color-btn:hover { background: #ffe4ef; }
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
.strike-x { font-size: 10px; margin-left: 1px; }

/* FIX #4: Counter ký tự */
.rte-counter {
  margin-left: auto;
  font-size: 11.5px;
  color: #94a3b8;
  padding: 0 6px;
  font-weight: 500;
  white-space: nowrap;
}
.rte-counter.warn  { color: #f59e0b; }
.rte-counter.danger { color: #dc3545; font-weight: 700; }

/* FIX #2: Inline link bar */
.rte-link-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  background: #fff0f7;
  border-bottom: 1px solid #f3d6e3;
}
.rte-link-input {
  flex: 1;
  padding: 6px 10px;
  border: 1px solid #f3d6e3;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  background: #fff;
  color: #202636;
}
.rte-link-input:focus { border-color: #efbdd2; box-shadow: 0 0 0 3px rgba(245,93,155,0.12); }
.rte-link-btn {
  height: 32px;
  padding: 0 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  white-space: nowrap;
}
.rte-link-btn.confirm { background: #f55d9b; color: #fff; }
.rte-link-btn.confirm:hover { background: #ec4d8d; }
.rte-link-btn.cancel  { background: #fff; color: #6b7280; border: 1px solid #f3d6e3; }
.rte-link-btn.cancel:hover { background: #f1f5f9; }

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
.rte-editable p  { margin: 0 0 0.75em; }
.rte-editable ul,
.rte-editable ol { padding-left: 1.4em; margin: 0 0 0.75em; }
.rte-editable a  { color: #d63384; text-decoration: underline; }
</style>