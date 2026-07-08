<template>
  <div class="pf-wrap">

    <div class="page-header">
      <div class="page-header-left">
        <div class="page-icon">
          <i class="bi bi-file-earmark-text"></i>
        </div>
        <div>
          <div class="crumbs">
            <span class="crumb-link" @click="attemptCancel">Bài viết</span>
            <i class="bi bi-chevron-right"></i>
            <span class="current">{{ isEdit ? 'Chỉnh sửa' : 'Tạo mới' }}</span>
          </div>
          <h2 class="page-title">{{ isEdit ? 'Chỉnh sửa bài viết' : 'Tạo mới bài viết' }}</h2>
        </div>
      </div>
      <div class="header-actions">
        <span class="status-pill" :class="form.isPublished ? 'live' : 'draft'">
          {{ form.isPublished ? 'Sẽ xuất bản' : 'Lưu nháp' }}
        </span>
        <button class="btn-cancel" @click="attemptCancel">Hủy bỏ</button>
        <button class="btn-save" :disabled="saving" @click="onSave">
          <i class="bi bi-check-lg"></i>
          {{ saving ? 'Đang lưu…' : 'Lưu bài viết' }}
        </button>
      </div>
    </div>

    <div class="layout">
      <div class="main-col">
        <div class="page-card pad">
          <div class="form-row" :class="{ error: errors.title }">
            <label>Tiêu đề bài viết <span class="req">*</span></label>
            <input
              class="form-input"
              type="text"
              v-model="form.title"
              maxlength="255"
              placeholder="Ví dụ: Apple chính thức ra mắt iPhone 15 Series"
              @blur="validateField('title')"
            />
            <div class="counter-row">
              <span v-if="errors.title" class="error-msg">{{ errors.title }}</span>
              <span v-else></span>
              <span class="counter" :class="{ warn: form.title.length > 240 }">{{ form.title.length }}/255</span>
            </div>
          </div>

          <div class="form-row">
            <label>Đường dẫn tĩnh (slug)</label>
            <div class="slug-box">
              <span class="slug-prefix mono">/bai-viet/</span>
              <span class="slug-value mono">{{ slugPreview || 'duong-dan-se-tu-dong-tao' }}</span>
              <span class="slug-lock" title="Tự động sinh từ tiêu đề, không thể sửa tay">
                <i class="bi bi-lock-fill"></i>
              </span>
            </div>
            <div class="field-hint">
              Đây là <strong>slug dự kiến</strong> — máy chủ tự sinh slug chính thức khi lưu (có thể khác nhẹ).
              <span v-if="slugCheckDone && !slugDuplicate" class="badge-unique">Hợp lệ</span>
              <span v-if="slugCheckDone && slugDuplicate" class="badge-dup">Đã tồn tại</span>
            </div>
          </div>

          <div class="form-row no-mb" :class="{ error: errors.postCategoryId }">
            <label>Danh mục bài viết <span class="req">*</span></label>
            <select class="form-input" v-model="form.postCategoryId" @change="validateField('postCategoryId')">
              <option value="">Chọn danh mục...</option>
              <option v-for="c in categories" :key="c.value" :value="c.value">{{ c.label }}</option>
            </select>
            <span v-if="errors.postCategoryId" class="error-msg">{{ errors.postCategoryId }}</span>
          </div>
        </div>

        <div class="page-card pad">
          <p class="section-title">Nội dung bài viết <span class="req">*</span></p>
          <p class="section-sub">Bản chính thức nên tích hợp Rich Text Editor như TipTap hoặc CKEditor.</p>

          <div class="rte-toolbar">
            <button type="button" class="rte-btn" title="In đậm" @click="insertTag('<strong>', '</strong>')"><b>B</b></button>
            <button type="button" class="rte-btn" title="In nghiêng" @click="insertTag('<em>', '</em>')"><i>I</i></button>
            <button type="button" class="rte-btn" title="Danh sách" @click="insertList(false)">•≡</button>
            <button type="button" class="rte-btn" title="Danh sách số" @click="insertList(true)">1≡</button>
            <button type="button" class="rte-btn" title="Liên kết" @click="insertTag(&quot;<a href='#'>&quot;, '</a>')">🔗</button>
          </div>
          <textarea
            ref="contentArea"
            class="content-area"
            :class="{ error: errors.content }"
            v-model="form.content"
            rows="10"
            placeholder="Nhập nội dung chi tiết bài viết tại đây..."
            @blur="validateField('content')"
          ></textarea>
          <span v-if="errors.content" class="error-msg">{{ errors.content }}</span>
        </div>

        <div class="page-card pad">
          <p class="section-title">Tóm tắt ngắn</p>
          <p class="section-sub">Không bắt buộc — nếu bỏ trống, hệ thống tự lấy 150 ký tự đầu của nội dung khi lưu.</p>
          <textarea class="form-input" rows="3" maxlength="500" v-model="form.excerpt"
            placeholder="Một đoạn tóm tắt hấp dẫn để hiển thị ngoài trang danh sách..."></textarea>
          <div class="counter-row">
            <span v-if="usingAutoExcerpt" class="field-hint" style="margin-top:0;">Đang dùng tóm tắt tự động từ nội dung.</span>
            <span v-else></span>
            <span class="counter" :class="{ warn: form.excerpt.length > 480 }">{{ form.excerpt.length }}/500</span>
          </div>
        </div>

        <div class="page-card pad">
          <p class="section-title">Ảnh đại diện</p>
          <div class="upload-dropzone" :class="{ 'is-uploading': uploading, 'has-image': form.thumbnailUrl && !imgBroken }"
            @click="!uploading && $refs.fileInput.click()" @dragover.prevent @drop.prevent="onDrop">
            <div v-if="uploading" class="upload-state">
              <div class="upload-spinner"></div>
              <span>Đang tải lên... {{ uploadPercent }}%</span>
            </div>
            <img v-else-if="form.thumbnailUrl && !imgBroken" :src="form.thumbnailUrl" class="upload-preview-img"
              @error="imgBroken = true" @load="imgBroken = false" alt="Xem trước ảnh đại diện" />
            <div v-else class="upload-state">
              <i class="bi bi-cloud-upload upload-icon"></i>
              <span class="upload-text">Kéo thả hoặc <u>chọn ảnh từ máy</u></span>
              <span class="upload-hint">JPG, PNG, WEBP · Tối đa 5MB</span>
            </div>
            <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />
          </div>
          <div class="url-or"><span>hoặc nhập URL trực tiếp</span></div>
          <input class="form-input" type="url" v-model="form.thumbnailUrl" maxlength="500"
            placeholder="https://cdn.marcusstore.vn/uploads/anh-bai-viet.jpg"
            @input="imgBroken = false" @blur="validateField('thumbnailUrl')" />
          <span v-if="errors.thumbnailUrl" class="error-msg">{{ errors.thumbnailUrl }}</span>
          <div v-if="uploadError" class="error-msg">{{ uploadError }}</div>
        </div>
      </div>

      <div class="side-col">
        <div class="page-card pad sticky">
          <p class="section-title">Xuất bản</p>
          <div class="toggle-row">
            <span class="toggle-label">
              <i class="bi bi-eye"></i>Công khai bài viết
            </span>
            <label class="toggle">
              <input type="checkbox" v-model="form.isPublished" />
              <span class="toggle-slider"></span>
            </label>
          </div>
          <div class="form-row" style="margin-top:14px;">
            <label>Ngày giờ xuất bản</label>
            <input class="form-input" type="datetime-local" v-model="form.publishedAt" :disabled="!form.isPublished" />
            <div class="field-hint">Có thể chọn ngày trong tương lai để hẹn giờ đăng bài. Bỏ trống sẽ lấy thời gian hiện tại.</div>
          </div>
          <div class="author-row">
            <div class="avatar">{{ authorInitial }}</div>
            <div>
              <div class="author-name">{{ authorName }}</div>
              <div class="author-role">Tự động lấy từ phiên đăng nhập</div>
            </div>
          </div>
        </div>

        <div class="page-card pad">
          <p class="section-title">Xem trước trên trang</p>
          <div class="preview-card">
            <div class="preview-thumb">
              <img v-if="form.thumbnailUrl && !imgBroken" :src="form.thumbnailUrl" alt="" />
              <span v-else>Chưa có ảnh đại diện</span>
            </div>
            <div class="preview-body">
              <span v-if="categoryName" class="pos-badge">{{ categoryName }}</span>
              <p class="preview-title">{{ form.title || 'Tiêu đề bài viết sẽ hiển thị ở đây' }}</p>
              <p class="preview-excerpt" :class="{ auto: usingAutoExcerpt }">
                {{ effectiveExcerpt || 'Phần tóm tắt sẽ hiển thị ở đây...' }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showLeaveConfirm" class="bn-modal-overlay" @click.self="cancelLeaveNav">
        <div class="banner-modal-box confirm-box" role="dialog" aria-modal="true" aria-labelledby="leaveModalTitle">
          <div class="bn-modal-header">
            <span class="bn-modal-title" id="leaveModalTitle">
              <i class="bi bi-exclamation-triangle-fill warn-icon"></i>
              Thay đổi của bạn chưa được lưu
            </span>
            <button class="btn-close" @click="cancelLeaveNav" aria-label="Đóng">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <div class="bn-modal-body">
            <p class="confirm-text">
              Bạn có chắc chắn muốn rời đi? Mọi nội dung chưa lưu sẽ bị mất.
            </p>
          </div>

          <div class="bn-modal-footer">
            <button class="btn-cancel" @click="cancelLeaveNav">Ở lại chỉnh sửa</button>
            <button class="btn-danger" @click="confirmLeaveNav">
              <i class="bi bi-box-arrow-right"></i> Rời đi, không lưu
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <div class="toast-wrap">
      <div v-for="t in toasts" :key="t.id" class="toast" :class="t.type">{{ t.message }}</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { postApi } from '@/api/PostApi';

const props = defineProps({
  post: { type: Object, default: null },
  categories: { type: Array, default: () => [] },
});
const emit = defineEmits(['saved', 'cancel']);

const isEdit = computed(() => !!props.post);

const authorName = computed(() => props.post?.authorName || localStorage.getItem('USERNAME') || 'Admin');
const authorInitial = computed(() => authorName.value?.trim()?.slice(-1)?.toUpperCase() || 'A');

const saving = ref(false);

function emptyForm() {
  return {
    title: '', postCategoryId: '', content: '', excerpt: '',
    thumbnailUrl: '', isPublished: false, publishedAt: '',
  };
}
const form = reactive(emptyForm());
const errors = reactive({});
const initialSnapshot = ref('');

const slugCheckDone = ref(false);
const slugDuplicate = ref(false);

const contentArea = ref(null);
const imgBroken = ref(false);
const uploading = ref(false);
const uploadPercent = ref(0);
const uploadError = ref('');

const toasts = ref([]);
function pushToast(type, message) {
  const id = Date.now() + Math.random();
  toasts.value.push({ id, type, message });
  setTimeout(() => { toasts.value = toasts.value.filter((t) => t.id !== id); }, 3200);
}

function stripDiacritics(str) {
  str = str.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  str = str.replace(/đ/g, 'd').replace(/Đ/g, 'D');
  return str;
}
function slugify(str) {
  if (!str) return '';
  let s = stripDiacritics(str).toLowerCase().trim();
  s = s.replace(/[^a-z0-9\s-]/g, '');
  s = s.replace(/\s+/g, '-').replace(/-+/g, '-');
  s = s.replace(/^-+|-+$/g, '');
  return s;
}
function stripHtml(html) {
  const tmp = document.createElement('div');
  tmp.innerHTML = html || '';
  return tmp.textContent || tmp.innerText || '';
}
function autoExcerpt(content, limit = 150) {
  const plain = stripHtml(content).replace(/\s+/g, ' ').trim();
  return plain.length > limit ? plain.slice(0, limit) + '…' : plain;
}

const slugPreview = computed(() => slugify(form.title));
const usingAutoExcerpt = computed(() => !form.excerpt.trim() && stripHtml(form.content).trim().length > 0);
const effectiveExcerpt = computed(() => (form.excerpt.trim() ? form.excerpt.trim() : autoExcerpt(form.content)));
const categoryName = computed(() => props.categories.find((c) => String(c.value) === String(form.postCategoryId))?.label);
const isDirty = computed(() => JSON.stringify(form) !== initialSnapshot.value);

function nowLocalDatetime() {
  const d = new Date();
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

watch(() => form.title, () => {
  slugCheckDone.value = false;
  slugDuplicate.value = false;
  if (errors.title) validateField('title');
});

watch(() => form.isPublished, (val) => {
  if (val && !form.publishedAt) form.publishedAt = nowLocalDatetime();
});

function validateField(field) {
  if (field === 'title') {
    if (!form.title.trim()) errors.title = 'Tiêu đề bài viết không được để trống.';
    else if (form.title.length > 255) errors.title = 'Tiêu đề vượt quá 255 ký tự.';
    else delete errors.title;
  }
  if (field === 'postCategoryId') {
    if (!form.postCategoryId) errors.postCategoryId = 'Vui lòng chọn danh mục bài viết.';
    else delete errors.postCategoryId;
  }
  if (field === 'content') {
    if (!stripHtml(form.content).trim()) errors.content = 'Nội dung bài viết không được để trống.';
    else delete errors.content;
  }
  if (field === 'thumbnailUrl') {
    const url = form.thumbnailUrl.trim();
    if (url && !/^https?:\/\/.+\.(jpg|jpeg|png|webp|gif)(\?.*)?$/i.test(url)) {
      errors.thumbnailUrl = 'Đường dẫn ảnh không đúng định dạng (.jpg, .png, .webp...).';
    } else delete errors.thumbnailUrl;
  }
}

function validateAll() {
  validateField('title');
  validateField('postCategoryId');
  validateField('content');
  validateField('thumbnailUrl');
  return Object.keys(errors).length === 0;
}

function insertTag(tagStart, tagEnd) {
  const ta = contentArea.value;
  const start = ta.selectionStart, end = ta.selectionEnd;
  const val = form.content;
  const selected = val.slice(start, end) || 'văn bản';
  form.content = val.slice(0, start) + tagStart + selected + tagEnd + val.slice(end);
  nextTick(() => {
    ta.focus();
    ta.selectionStart = start + tagStart.length;
    ta.selectionEnd = start + tagStart.length + selected.length;
  });
}
function insertList(ordered) {
  const ta = contentArea.value;
  const start = ta.selectionStart;
  const val = form.content;
  const tag = ordered
    ? '<ol>\n  <li>Mục 1</li>\n  <li>Mục 2</li>\n</ol>\n'
    : '<ul>\n  <li>Mục 1</li>\n  <li>Mục 2</li>\n</ul>\n';
  form.content = val.slice(0, start) + tag + val.slice(start);
  nextTick(() => ta.focus());
}

async function uploadFile(file) {
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) { uploadError.value = 'Ảnh quá lớn, tối đa 5MB'; return; }
  if (!file.type.startsWith('image/')) { uploadError.value = 'Chỉ chấp nhận file ảnh (JPG, PNG, WEBP...)'; return; }
  uploading.value = true;
  uploadError.value = '';
  uploadPercent.value = 0;
  try {
    const url = await postApi.uploadImage(file, (e) => {
      if (e.lengthComputable) uploadPercent.value = Math.round((e.loaded / e.total) * 100);
    });
    form.thumbnailUrl = url;
    imgBroken.value = false;
  } catch (err) {
    uploadError.value = err.message || 'Upload thất bại, thử lại hoặc nhập URL thủ công';
  } finally {
    uploading.value = false;
  }
}
function onFileChange(e) {
  const file = e.target.files?.[0];
  if (file) uploadFile(file);
  e.target.value = '';
}
function onDrop(e) {
  const file = e.dataTransfer.files?.[0];
  if (file && file.type.startsWith('image/')) uploadFile(file);
}

// Khởi tạo form từ props.post (nếu sửa) — list đã có sẵn đầy đủ dữ liệu,
// không cần gọi lại API getById.
function initFromProps() {
  if (props.post) {
    Object.assign(form, {
      title: props.post.title || '',
      postCategoryId: props.post.postCategoryId ?? '',
      content: props.post.content || '',
      excerpt: props.post.excerpt || '',
      thumbnailUrl: props.post.thumbnailUrl || '',
      isPublished: !!props.post.isPublished,
      publishedAt: props.post.publishedAt ? props.post.publishedAt.slice(0, 16) : '',
    });
  } else {
    Object.assign(form, emptyForm());
  }
  initialSnapshot.value = JSON.stringify(form);
}

onMounted(initFromProps);

// Cảnh báo khi đóng tab / F5 lúc đang có thay đổi chưa lưu
function handleBeforeUnload(e) {
  if (isDirty.value) {
    e.preventDefault();
    e.returnValue = '';
  }
}
onMounted(() => window.addEventListener('beforeunload', handleBeforeUnload));
onBeforeUnmount(() => window.removeEventListener('beforeunload', handleBeforeUnload));

async function onSave() {
  form.title = form.title.trim();
  form.excerpt = form.excerpt.trim();

  if (!validateAll()) {
    pushToast('error', 'Vui lòng kiểm tra lại các trường bắt buộc.');
    return;
  }

  saving.value = true;
  try {
    const { exists } = await postApi.checkSlug(slugPreview.value, isEdit.value ? props.post.id : undefined);
    slugCheckDone.value = true;
    slugDuplicate.value = !!exists;
    if (exists) {
      pushToast('error', 'Đường dẫn bài viết đã tồn tại, vui lòng thay đổi tiêu đề.');
      return;
    }

    const payload = {
      title: form.title,
      postCategoryId: form.postCategoryId,
      content: form.content,
      excerpt: form.excerpt || autoExcerpt(form.content),
      thumbnailUrl: form.thumbnailUrl || null,
      isPublished: form.isPublished,
      publishedAt: form.isPublished ? (form.publishedAt || nowLocalDatetime()) : null,
    };

    if (isEdit.value) {
      await postApi.update(props.post.id, payload);
    } else {
      await postApi.create(payload);
    }

    initialSnapshot.value = JSON.stringify(form);
    pushToast('success', 'Đã lưu bài viết.');
    setTimeout(() => emit('saved'), 500);
  } catch (err) {
    pushToast('error', err?.response?.data?.message || 'Lưu bài viết thất bại. Vui lòng thử lại.');
  } finally {
    saving.value = false;
  }
}

const showLeaveConfirm = ref(false);

function attemptCancel() {
  if (isDirty.value) {
    showLeaveConfirm.value = true;
  } else {
    emit('cancel');
  }
}
function confirmLeaveNav() {
  showLeaveConfirm.value = false;
  emit('cancel');
}
function cancelLeaveNav() {
  showLeaveConfirm.value = false;
}
</script>

<style scoped>
.pf-wrap { }

.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; gap: 16px; }
.page-header-left { display: flex; align-items: center; gap: 16px; }
.page-icon {
  width: 48px; height: 48px; border-radius: 12px;
  background: linear-gradient(135deg, #f55d9b, #ec4d8d);
  display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; flex-shrink: 0;
}
.crumbs { font-size: 12.5px; color: #6b7280; display: flex; align-items: center; gap: 4px; }
.crumbs .current { color: #d63384; font-weight: 600; }
.crumb-link { color: #6b7280; cursor: pointer; }
.crumb-link:hover { color: #f55d9b; text-decoration: underline; }
.page-title { font-size: 22px; font-weight: 700; color: #f55d9b; margin: 2px 0 0; }

.header-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.status-pill { display: inline-flex; align-items: center; padding: 5px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.status-pill.draft { background: #f1f5f9; color: #64748b; }
.status-pill.live { background: #f0fdf4; color: #15803d; }

.btn-cancel {
  background: #fff; border: 1px solid #f3d6e3; color: #6b7280; border-radius: 8px;
  padding: 9px 18px; font-size: 13px; cursor: pointer; transition: all 0.15s;
}
.btn-cancel:hover { background: #fff0f7; color: #d63384; border-color: #efbdd2; }
.btn-save {
  background: #f55d9b; color: #fff; border: none; border-radius: 8px; padding: 9px 20px;
  font-size: 13px; font-weight: 500; cursor: pointer; display: flex; align-items: center; gap: 6px;
  transition: background 0.15s;
}
.btn-save:hover:not(:disabled) { background: #ec4d8d; }
.btn-save:disabled { background: #f3d6e3; color: #b4557d; cursor: not-allowed; }

.layout { display: grid; grid-template-columns: 1fr 320px; gap: 20px; align-items: start; }
@media (max-width: 960px) { .layout { grid-template-columns: 1fr; } }

.page-card { background: #fff; border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); margin-bottom: 20px; }
.page-card.pad { padding: 20px 22px; }
.sticky { position: sticky; top: 20px; }

.section-title { font-size: 14px; font-weight: 700; color: #202636; margin: 0 0 4px; }
.section-sub { font-size: 12.5px; color: #6b7280; margin: 0 0 14px; }

.form-row { margin-bottom: 16px; }
.form-row.no-mb { margin-bottom: 0; }
.form-row label { display: block; font-size: 12px; font-weight: 600; color: #344054; margin-bottom: 6px; }
.req { color: #f55d9b; margin-left: 2px; }
.form-input {
  width: 100%; padding: 9px 12px; border: 1px solid #f3d6e3; border-radius: 8px;
  font-size: 13.5px; color: #202636; background: #fffafd; outline: none; transition: border 0.15s;
  font-family: inherit;
}
.form-input:focus { border-color: #efbdd2; box-shadow: 0 0 0 3px rgba(245,93,155,0.12); background: #fff; }
.form-input:disabled { background: #f1f5f9; color: #94a3b8; cursor: not-allowed; }
.form-row.error .form-input, .content-area.error { border-color: #f5c2c7; background: #fff5f6; }

.counter-row { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
.counter { font-size: 11.5px; color: #64748b; }
.counter.warn { color: #d63384; font-weight: 600; }
.error-msg { display: block; font-size: 12.5px; color: #d63384; margin-top: 6px; font-weight: 500; }
.field-hint { font-size: 12px; color: #64748b; margin-top: 6px; }

.slug-box { display: flex; align-items: center; background: #f1f5f9; border: 1px solid #f3d6e3; border-radius: 8px; overflow: hidden; }
.slug-prefix { padding: 9px 0 9px 12px; font-size: 13px; color: #64748b; white-space: nowrap; }
.slug-value { flex: 1; padding: 9px 12px 9px 0; font-size: 13.5px; color: #d63384; font-weight: 500; overflow-x: auto; white-space: nowrap; }
.slug-lock { padding: 0 12px; color: #94a3b8; }
.mono { font-family: 'JetBrains Mono', 'Courier New', monospace; }
.badge-unique { display: inline-flex; align-items: center; gap: 4px; background: #f0fdf4; color: #15803d; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 20px; margin-left: 8px; }
.badge-dup { display: inline-flex; align-items: center; gap: 4px; background: #f8d7da; color: #dc3545; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 20px; margin-left: 8px; }

.rte-toolbar { display: flex; gap: 4px; background: #fff0f7; border: 1px solid #f3d6e3; border-bottom: none; border-radius: 8px 8px 0 0; padding: 6px; }
.rte-btn {
  width: 30px; height: 30px; border-radius: 6px; border: none; background: transparent;
  color: #b4557d; font-weight: 700; font-size: 13px; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.rte-btn:hover { background: #ffe4ef; }
.content-area {
  width: 100%; min-height: 220px; border: 1px solid #f3d6e3; border-radius: 0 0 8px 8px;
  padding: 12px; font-size: 13.5px; line-height: 1.6; color: #202636; background: #fffafd;
  outline: none; resize: vertical; font-family: inherit;
}
.content-area:focus { border-color: #efbdd2; box-shadow: 0 0 0 3px rgba(245,93,155,0.12); background: #fff; }

.upload-dropzone {
  border: 2px dashed #f3d6e3; border-radius: 10px; background: #fffafd; min-height: 110px;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  transition: border-color 0.15s, background 0.15s; overflow: hidden; margin-bottom: 8px;
}
.upload-dropzone:hover { border-color: #f55d9b; background: #fff0f7; }
.upload-dropzone.is-uploading { cursor: not-allowed; opacity: 0.8; }
.upload-dropzone.has-image { border-style: solid; min-height: 140px; }
.upload-preview-img { width: 100%; max-height: 180px; object-fit: contain; display: block; }
.upload-state { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 16px; pointer-events: none; }
.upload-icon { font-size: 28px; color: #efbdd2; }
.upload-text { font-size: 13px; color: #b4557d; }
.upload-text u { color: #f55d9b; }
.upload-hint { font-size: 11px; color: #d0a0b5; }
.upload-spinner { width: 28px; height: 28px; border: 3px solid #f3d6e3; border-top-color: #f55d9b; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.url-or { display: flex; align-items: center; gap: 8px; margin: 8px 0; color: #b4557d; font-size: 12px; }
.url-or::before, .url-or::after { content: ''; flex: 1; height: 1px; background: #f3d6e3; }

.toggle-row { display: flex; align-items: center; justify-content: space-between; background: #fffafd; border: 1px solid #f3d6e3; border-radius: 8px; padding: 10px 14px; }
.toggle-label { font-size: 13px; color: #344054; font-weight: 500; display: flex; align-items: center; }
.toggle-label i { font-size: 15px; margin-right: 6px; color: #f55d9b; }
.toggle { position: relative; width: 36px; height: 20px; display: inline-block; }
.toggle input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; inset: 0; background: #f3d6e3; border-radius: 20px; cursor: pointer; transition: 0.2s; }
.toggle-slider:before { content: ''; position: absolute; width: 14px; height: 14px; left: 3px; top: 3px; background: #fff; border-radius: 50%; transition: 0.2s; }
.toggle input:checked + .toggle-slider { background: #f55d9b; }
.toggle input:checked + .toggle-slider:before { transform: translateX(16px); }

.author-row { display: flex; align-items: center; gap: 10px; padding: 10px; background: #fff0f7; border-radius: 8px; margin-top: 16px; }
.avatar { width: 34px; height: 34px; border-radius: 50%; background: #ffe4ef; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; color: #d63384; flex-shrink: 0; }
.author-name { font-size: 13px; font-weight: 600; color: #202636; }
.author-role { font-size: 11px; color: #6b7280; }

.preview-card { border: 1px solid #f3d6e3; border-radius: 10px; overflow: hidden; background: #fffafd; }
.preview-thumb { width: 100%; height: 130px; background: #f1f5f9; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.preview-thumb img { width: 100%; height: 100%; object-fit: cover; }
.preview-thumb span { font-size: 11.5px; color: #94a3b8; }
.preview-body { padding: 14px; }
.pos-badge { display: inline-block; font-size: 10.5px; font-weight: 700; background: #ffe4ef; color: #d63384; padding: 3px 9px; border-radius: 20px; margin-bottom: 8px; }
.preview-title { font-size: 14px; font-weight: 700; color: #111827; margin: 0 0 6px; line-height: 1.4; }
.preview-excerpt { font-size: 12.5px; color: #6b7280; line-height: 1.55; margin: 0; }
.preview-excerpt.auto { color: #b4557d; font-style: italic; }

.bn-modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.46); display: flex; align-items: center; justify-content: center; z-index: 9999; padding: 20px; }
.banner-modal-box { background: #fff; border-radius: 14px; width: 520px; max-width: 95vw; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 60px rgba(15, 23, 42, 0.18); }
.banner-modal-box.confirm-box { width: 420px; }
.bn-modal-header { padding: 18px 22px 14px; border-bottom: 1px solid #f3d6e3; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.bn-modal-title { font-size: 16px; font-weight: 600; color: #202636; display: flex; align-items: center; }
.warn-icon { color: #f59e0b; margin-right: 8px; font-size: 17px; }
.btn-close { background: none; border: none; cursor: pointer; color: #b4557d; font-size: 20px; padding: 2px; border-radius: 5px; line-height: 1; flex-shrink: 0; }
.btn-close:hover { color: #d63384; background: #fff0f7; }
.bn-modal-body { padding: 18px 22px; }
.confirm-text { font-size: 13.5px; color: #4b5563; line-height: 1.6; margin: 0; }
.bn-modal-footer { padding: 14px 22px; border-top: 1px solid #f3d6e3; display: flex; align-items: center; justify-content: flex-end; gap: 8px; background: #fffafd; border-radius: 0 0 14px 14px; }
.btn-danger { background: #fff5f6; color: #dc3545; border: 1px solid #f5c2c7; border-radius: 8px; padding: 8px 18px; font-size: 13px; font-weight: 500; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: background 0.15s; }
.btn-danger:hover { background: #f8d7da; }

.toast-wrap { position: fixed; right: 24px; bottom: 24px; z-index: 10000; display: flex; flex-direction: column; gap: 10px; }
.toast { min-width: 280px; max-width: 360px; padding: 13px 16px; border-radius: 12px; box-shadow: 0 10px 30px rgba(15,23,42,0.18); font-size: 13.5px; font-weight: 500; }
.toast.success { background: #f0fdf4; color: #15803d; border: 1px solid #bbf0cc; }
.toast.error { background: #fef2f2; color: #b91c1c; border: 1px solid #f5c2c7; }
</style>