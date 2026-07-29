<template>
  <Teleport to="body">
    <div v-if="visible" class="rc-overlay" @click.self="$emit('cancel')">
      <div class="rc-box" role="dialog" aria-modal="true" aria-labelledby="rcTitle">
        <div class="rc-header">
          <i class="bi bi-exclamation-triangle-fill"></i>
          <span id="rcTitle">Banner đang được sử dụng</span>
        </div>

        <div class="rc-body">
          <p class="rc-text">
            Vị trí <strong>"{{ posLabel }}"</strong> hiện đang hiển thị 1 banner
            khác. Bạn có muốn <strong>thay thế</strong> banner này không?
          </p>

          <div v-if="existing" class="rc-existing">
            <img v-if="existing.imageUrl" :src="existing.imageUrl" :alt="existing.title" />
            <div v-else class="rc-thumb-placeholder"><i class="bi bi-image"></i></div>
            <div class="rc-info">
              <div class="rc-title">{{ existing.title || '(Không có tiêu đề)' }}</div>
              <div class="rc-meta">
                Trạng thái: <strong>Đang hiển thị</strong>
              </div>
            </div>
          </div>

          <p class="rc-note">
            <i class="bi bi-info-circle"></i>
            Nếu chọn <strong>Thay thế</strong>: banner cũ sẽ được chuyển sang trạng
            thái <em>"Tạm ẩn"</em> (vẫn lưu trong hệ thống, có thể bật lại sau).
            Banner mới sẽ hiển thị ngay trên trang chủ.
          </p>
        </div>

        <div class="rc-footer">
          <button class="btn-cancel" @click="$emit('cancel')">
            <i class="bi bi-x-lg"></i> Giữ nguyên
          </button>
          <button class="btn-confirm" @click="$emit('confirm')">
            <i class="bi bi-arrow-repeat"></i> Thay thế
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  visible:  { type: Boolean, default: false },
  existing: { type: Object, default: null },
});

defineEmits(['confirm', 'cancel']);

const posLabel = computed(() => {
  if (!props.existing) return '—';
  return props.existing.positionDescription
    || props.existing.positionCode
    || '—';
});
</script>

<style scoped>
.rc-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.46);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 16px;
}

.rc-box {
  background: #fff;
  border-radius: 14px;
  width: 480px;
  max-width: 100%;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.18);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.rc-header {
  padding: 16px 22px;
  background: linear-gradient(135deg, #fff0d9, #ffe4b5);
  color: #9a5b00;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 15px;
}

.rc-header i {
  font-size: 22px;
}

.rc-body {
  padding: 18px 22px;
  overflow-y: auto;
}

.rc-text {
  font-size: 14px;
  color: #344054;
  margin: 0 0 14px;
  line-height: 1.55;
}

.rc-existing {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  background: #f9f0f5;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  margin-bottom: 14px;
}

.rc-existing img {
  width: 72px;
  height: 44px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
  background: #fff;
}

.rc-thumb-placeholder {
  width: 72px;
  height: 44px;
  border-radius: 6px;
  background: #fff;
  border: 1px dashed #f3d6e3;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e0b8cc;
  flex-shrink: 0;
}

.rc-info {
  flex: 1;
  min-width: 0;
}

.rc-title {
  font-weight: 500;
  font-size: 13px;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rc-meta {
  font-size: 11px;
  color: #6b7280;
  margin-top: 4px;
}

.rc-note {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
  padding: 10px 12px;
  background: #fffafd;
  border-radius: 8px;
  border: 1px dashed #f3d6e3;
  line-height: 1.55;
  display: flex;
  gap: 6px;
}

.rc-note i {
  color: #f55d9b;
  font-size: 14px;
  flex-shrink: 0;
  margin-top: 1px;
}

.rc-footer {
  padding: 14px 22px;
  border-top: 1px solid #f3d6e3;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  background: #fffafd;
}

.btn-cancel {
  background: #fff;
  border: 1px solid #f3d6e3;
  color: #6b7280;
  border-radius: 8px;
  padding: 8px 18px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-cancel:hover {
  background: #fff0f7;
  color: #d63384;
  border-color: #efbdd2;
}

.btn-confirm {
  background: #f55d9b;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-confirm:hover {
  background: #ec4d8d;
}
</style>