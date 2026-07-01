<template>
  <Teleport to="body">
    <div v-if="visible" class="bn-modal-overlay" @click="closeIfOutside">
      <div class="banner-modal-box" role="dialog" aria-modal="true" aria-labelledby="modalTitleText">
        <div class="bn-modal-header">
          <span class="bn-modal-title" id="modalTitleText">
            {{ isEdit ? 'Chỉnh sửa banner' : 'Thêm banner mới' }}
          </span>
          <button class="btn-close" @click="$emit('close')" aria-label="Đóng">
            <i class="ti ti-x"></i>
          </button>
        </div>

      <div class="bn-modal-body">
        <div class="form-row">
          <label>Tiêu đề banner <span class="req">*</span></label>
          <input
            class="form-input"
            type="text"
            v-model="form.title"
            placeholder="VD: Banner khuyến mãi mùa hè"
          />
        </div>

        <div class="form-row-2">
          <div class="form-row no-mb">
            <label>Vị trí hiển thị <span class="req">*</span></label>
            <select class="form-input" v-model="form.positionId">
              <option value="">Chọn vị trí...</option>
              <option v-for="p in positions" :key="p.value" :value="p.value">
                {{ p.label }}
              </option>
            </select>
          </div>
          <div class="form-row no-mb">
            <label>Thứ tự hiển thị</label>
            <div class="order-input">
              <input
                type="number"
                v-model.number="form.displayOrder"
                min="0"
                class="order-field"
              />
              <span class="order-hint">≥ 0</span>
            </div>
          </div>
        </div>

        <div class="form-row">
          <label>URL hình ảnh <span class="req">*</span></label>
          <input
            class="form-input"
            type="url"
            v-model="form.imageUrl"
            placeholder="https://..."
          />
          <div class="img-preview-box">
            <img
              v-if="form.imageUrl"
              :src="form.imageUrl"
              alt="Preview"
              @error="imgBroken = true"
              @load="imgBroken = false"
              v-show="!imgBroken"
            />
            <span v-if="!form.imageUrl" class="img-hint">
              <i class="ti ti-photo"></i>Nhập URL để xem trước ảnh
            </span>
            <span v-if="form.imageUrl && imgBroken" class="img-hint">
              <i class="ti ti-photo-off"></i>URL ảnh không hợp lệ
            </span>
          </div>
        </div>

        <div class="form-row">
          <label>URL liên kết (tùy chọn)</label>
          <input class="form-input" type="url" v-model="form.linkUrl" placeholder="https://..." />
        </div>

        <div class="form-row-2">
          <div class="form-row no-mb">
            <label>Ngày bắt đầu</label>
            <input class="form-input" type="date" v-model="form.startDate" />
          </div>
          <div class="form-row no-mb">
            <label>Ngày kết thúc</label>
            <input class="form-input" type="date" v-model="form.endDate" />
          </div>
        </div>

        <div v-if="dateInvalid" class="date-alert show">
          <i class="ti ti-alert-triangle"></i>
          Ngày kết thúc không được nhỏ hơn ngày bắt đầu
        </div>

        <div class="toggle-section">
          <div class="toggle-row">
            <span class="toggle-label">
              <i class="ti ti-eye"></i>Hiển thị banner
            </span>
            <label class="toggle">
              <input type="checkbox" v-model="form.isActive" />
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>
      </div>

      <div class="bn-modal-footer">
        <button class="btn-cancel" @click="$emit('close')">Hủy bỏ</button>
        <button class="btn-save" :disabled="!canSave" @click="handleSave">
          <i class="ti ti-check"></i>Lưu banner
        </button>
      </div>
    </div>
    </div>
  </Teleport>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  banner: { type: Object, default: null }, // null = thêm mới, object = sửa
  nextOrder: { type: Number, default: 1 }, // số lượng banner hiện tại + 1
  positions: {
    type: Array,
    default: () => [
      { value: 'homepage', label: 'Trang chủ' },
      { value: 'product', label: 'Trang sản phẩm' },
      { value: 'sidebar', label: 'Sidebar' },
      { value: 'popup', label: 'Popup' },
    ],
  },
});

const emit = defineEmits(['close', 'save']);

const isEdit = computed(() => !!props.banner);
const imgBroken = ref(false);

const defaultForm = () => ({
  id: null,
  title: '',
  positionId: '',
  displayOrder: props.nextOrder,
  imageUrl: '',
  linkUrl: '',
  startDate: '',
  endDate: '',
  isActive: true,
});

const form = reactive(defaultForm());

// Mỗi khi modal mở lại (thêm mới hoặc sửa khác), nạp lại dữ liệu form
watch(
  () => props.visible,
  (val) => {
    if (val) {
      imgBroken.value = false;
      const base = props.banner ? { ...props.banner } : defaultForm();
      Object.assign(form, base);
    }
  }
);

// Validate ngày: end phải >= start
const dateInvalid = computed(() => {
  if (form.startDate && form.endDate) {
    return new Date(form.endDate) < new Date(form.startDate);
  }
  return false;
});

// Validate bắt buộc: title, positionId, imageUrl (@NotBlank/@NotNull trong BannerRequestDTO)
const canSave = computed(() => {
  return !!form.title?.trim() && !!form.positionId && !!form.imageUrl && !dateInvalid.value;
});

function closeIfOutside(e) {
  if (e.target.classList.contains('bn-modal-overlay')) emit('close');
}

function handleSave() {
  if (!canSave.value) return;
  emit('save', { ...form });
}
</script>

<style scoped>
.bn-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.46);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.banner-modal-box {
  background: #fff;
  border-radius: 14px;
  width: 520px;
  max-width: 95vw;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.18);
}
.bn-modal-header {
  padding: 18px 22px 14px;
  border-bottom: 1px solid #f3d6e3;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.bn-modal-title {
  font-size: 16px;
  font-weight: 500;
  color: #202636;
}
.btn-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #b4557d;
  font-size: 20px;
  padding: 2px;
  border-radius: 5px;
  line-height: 1;
}
.btn-close:hover {
  color: #d63384;
  background: #fff0f7;
}
.bn-modal-body {
  padding: 18px 22px;
}
.form-row {
  margin-bottom: 16px;
}
.form-row.no-mb {
  margin-bottom: 0;
}
.form-row label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #344054;
  margin-bottom: 5px;
}
.req {
  color: #f55d9b;
  margin-left: 2px;
}
.form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #f3d6e3;
  border-radius: 7px;
  font-size: 13px;
  color: #202636;
  background: #fff;
  outline: none;
  transition: border 0.15s;
}
.form-input:focus {
  border-color: #f55d9b;
  box-shadow: 0 0 0 3px rgba(245, 93, 155, 0.08);
}
.form-input::placeholder {
  color: #b4557d;
  opacity: 0.5;
}
.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}
.img-preview-box {
  margin-top: 8px;
  border-radius: 8px;
  overflow: hidden;
  background: #fff0f7;
  border: 1px dashed #f3d6e3;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.img-preview-box img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.img-hint {
  color: #b4557d;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.order-input {
  display: flex;
  align-items: center;
  gap: 6px;
}
.order-field {
  width: 80px;
  padding: 7px 10px;
  border: 1px solid #f3d6e3;
  border-radius: 7px;
  font-size: 13px;
  text-align: center;
  outline: none;
}
.order-field:focus {
  border-color: #f55d9b;
}
.order-hint {
  font-size: 11px;
  color: #b4557d;
}
.toggle-section {
  margin-top: 16px;
}
.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fffafd;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  padding: 10px 14px;
}
.toggle-label {
  font-size: 13px;
  color: #344054;
  font-weight: 500;
  display: flex;
  align-items: center;
}
.toggle-label i {
  font-size: 15px;
  margin-right: 6px;
  color: #f55d9b;
}
.toggle {
  position: relative;
  width: 36px;
  height: 20px;
  display: inline-block;
}
.toggle input {
  opacity: 0;
  width: 0;
  height: 0;
}
.toggle-slider {
  position: absolute;
  inset: 0;
  background: #f3d6e3;
  border-radius: 20px;
  cursor: pointer;
  transition: 0.2s;
}
.toggle-slider:before {
  content: '';
  position: absolute;
  width: 14px;
  height: 14px;
  left: 3px;
  top: 3px;
  background: #fff;
  border-radius: 50%;
  transition: 0.2s;
}
.toggle input:checked + .toggle-slider {
  background: #f55d9b;
}
.toggle input:checked + .toggle-slider:before {
  transform: translateX(16px);
}
.date-alert {
  background: #fff0d9;
  border: 1px solid #fbbf24;
  border-radius: 7px;
  padding: 8px 12px;
  font-size: 12px;
  color: #9a5b00;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: -4px;
  margin-bottom: 16px;
}
.bn-modal-footer {
  padding: 14px 22px;
  border-top: 1px solid #f3d6e3;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  background: #fffafd;
  border-radius: 0 0 14px 14px;
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
}
.btn-cancel:hover {
  background: #fff0f7;
  color: #d63384;
  border-color: #efbdd2;
}
.btn-save {
  background: #f55d9b;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  display: flex;
  align-items: center;
  gap: 6px;
}
.btn-save:hover:not(:disabled) {
  background: #ec4d8d;
}
.btn-save:disabled {
  background: #f3d6e3;
  color: #b4557d;
  cursor: not-allowed;
}
</style>