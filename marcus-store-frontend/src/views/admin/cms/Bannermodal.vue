<template>
  <Teleport to="body">
    <div v-if="visible" class="bn-modal-overlay" @click="closeIfOutside">
      <div class="banner-modal-box" role="dialog" aria-modal="true" aria-labelledby="modalTitleText">
        <div class="bn-modal-header">
          <span class="bn-modal-title" id="modalTitleText">
            {{ isEdit ? 'Chỉnh sửa banner' : 'Thêm banner mới' }}
          </span>
          <button class="btn-close" @click="$emit('close')" aria-label="Đóng">
            <i class="bi bi-x-lg"></i>
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
          <label>Hình ảnh banner <span class="req">*</span></label>

          <!-- Dropzone upload file -->
          <div
            class="upload-dropzone"
            :class="{ 'is-uploading': uploading, 'has-image': form.imageUrl && !imgBroken }"
            @click="!uploading && $refs.fileInput.click()"
            @dragover.prevent
            @drop.prevent="onDrop"
          >
            <!-- Đang upload -->
            <div v-if="uploading" class="upload-state">
              <div class="upload-spinner"></div>
              <span>Đang tải lên... {{ uploadPercent }}%</span>
            </div>

            <!-- Đã có ảnh hợp lệ -->
            <img
              v-else-if="form.imageUrl && !imgBroken"
              :src="form.imageUrl"
              alt="Preview"
              class="upload-preview-img"
              @error="imgBroken = true"
              @load="imgBroken = false"
            />

            <!-- Chưa có ảnh hoặc URL lỗi -->
            <div v-else class="upload-state">
              <i class="bi bi-cloud-upload upload-icon"></i>
              <span class="upload-text">Kéo thả hoặc <u>chọn ảnh từ máy</u></span>
              <span class="upload-hint">JPG, PNG, WEBP · Tối đa 5MB</span>
            </div>

            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              style="display:none"
              @change="onFileChange"
            />
          </div>

          <!-- Hoặc nhập URL trực tiếp -->
          <div class="url-or">
            <span>hoặc nhập URL trực tiếp</span>
          </div>
          <input
            class="form-input"
            type="url"
            v-model="form.imageUrl"
            placeholder="https://res.cloudinary.com/..."
            @input="imgBroken = false"
          />
          <div v-if="uploadError" class="upload-error-msg">
            <i class="bi bi-exclamation-circle"></i> {{ uploadError }}
          </div>
        </div>

        <div class="form-row">
          <label>Đường dẫn khi click (tùy chọn)</label>
          <input
            class="form-input"
            v-model="form.linkUrl"
            placeholder="VD: /san-pham/iphone-15-pro-max hoặc /flash-sale"
          />
          <span class="field-hint">Slug nội bộ hoặc URL đầy đủ — để trống nếu banner không cần link</span>
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
          <i class="bi bi-exclamation-triangle"></i>
          Ngày kết thúc không được nhỏ hơn ngày bắt đầu
        </div>

        <div class="toggle-section">
          <div class="toggle-row">
            <span class="toggle-label">
              <i class="bi bi-eye"></i>Hiển thị banner
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
          <i class="bi bi-check-lg"></i>Lưu banner
        </button>
      </div>
    </div>
    </div>
  </Teleport>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue';
import bannerApi from '@/api/BannerApi';

const props = defineProps({
  visible: { type: Boolean, default: false },
  banner: { type: Object, default: null },
  nextOrder: { type: Number, default: 1 },
  positions: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(['close', 'save']);

const isEdit = computed(() => !!props.banner);
const imgBroken = ref(false);

// ---- Upload state ----
const uploading = ref(false);
const uploadPercent = ref(0);
const uploadError = ref('');
const fileInput = ref(null);

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

watch(
  () => props.visible,
  (val) => {
    if (val) {
      imgBroken.value = false;
      uploadError.value = '';
      uploading.value = false;
      uploadPercent.value = 0;
      const base = props.banner ? { ...props.banner } : defaultForm();
      Object.assign(form, base);
    }
  }
);

// ---- Upload ảnh qua bannerApi.uploadImage (dùng chung axios instance `api`) ----
async function uploadToCloudinary(file) {
  if (!file) return;

  if (file.size > 5 * 1024 * 1024) {
    uploadError.value = 'Ảnh quá lớn, tối đa 5MB';
    return;
  }
  if (!file.type.startsWith('image/')) {
    uploadError.value = 'Chỉ chấp nhận file ảnh (JPG, PNG, WEBP...)';
    return;
  }

  uploading.value = true;
  uploadError.value = '';
  uploadPercent.value = 0;

  try {
    const url = await bannerApi.uploadImage(file, (e) => {
      if (e.total) {
        uploadPercent.value = Math.round((e.loaded / e.total) * 100);
      }
    });

    form.imageUrl = url;
    imgBroken.value = false;
  } catch (err) {
    uploadError.value =
      err?.response?.data?.message || 'Upload thất bại, thử lại hoặc nhập URL thủ công';
    console.error('Upload error:', err);
  } finally {
    uploading.value = false;
  }
}

function onFileChange(e) {
  const file = e.target.files?.[0];
  if (file) uploadToCloudinary(file);
  // Reset input để có thể chọn lại cùng file
  e.target.value = '';
}

function onDrop(e) {
  const file = e.dataTransfer.files?.[0];
  if (file) uploadToCloudinary(file);
}

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

/* ---- Upload dropzone ---- */
.upload-dropzone {
  border: 2px dashed #f3d6e3;
  border-radius: 10px;
  background: #fffafd;
  min-height: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  overflow: hidden;
  margin-bottom: 8px;
}
.upload-dropzone:hover {
  border-color: #f55d9b;
  background: #fff0f7;
}
.upload-dropzone.is-uploading {
  cursor: not-allowed;
  opacity: 0.8;
}
.upload-dropzone.has-image {
  border-style: solid;
  border-color: #f3d6e3;
  min-height: 130px;
}
.upload-preview-img {
  width: 100%;
  max-height: 160px;
  object-fit: contain;
  display: block;
}
.upload-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px;
  pointer-events: none;
}
.upload-icon {
  font-size: 28px;
  color: #efbdd2;
}
.upload-text {
  font-size: 13px;
  color: #b4557d;
}
.upload-text u {
  color: #f55d9b;
}
.upload-hint {
  font-size: 11px;
  color: #d0a0b5;
}
.upload-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #f3d6e3;
  border-top-color: #f55d9b;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.url-or {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
  color: #b4557d;
  font-size: 12px;
}
.url-or::before,
.url-or::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #f3d6e3;
}
.upload-error-msg {
  margin-top: 6px;
  font-size: 12px;
  color: #b91c1c;
  display: flex;
  align-items: center;
  gap: 4px;
}
.field-hint {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: #b4557d;
}
</style>