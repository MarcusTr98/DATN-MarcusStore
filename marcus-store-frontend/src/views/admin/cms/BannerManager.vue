<template>
  <div class="bm-page">

    <!-- Page Header (đồng nhất với Quản lý đơn hàng) -->
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-icon">
          <i class="bi bi-images"></i>
        </div>
        <div>
          <h2 class="page-title">Quản lý Banner</h2>
          <p class="page-sub">Quản lý banner quảng cáo hiển thị trên website.</p>
        </div>
      </div>
      <button class="btn-add" @click="openAddModal">
        <i class="bi bi-plus-lg"></i> Thêm banner
      </button>
    </div>

    <!-- Khối tổng quan nhanh (đồng bộ style với Quản lý Voucher) -->
    <section class="stats-grid">
      <article class="stat-card">
        <span>Tổng banner</span>
        <strong>{{ stats.total }}</strong>
      </article>

      <article class="stat-card">
        <span>Đang hiển thị</span>
        <strong class="text-accent">{{ stats.active }}</strong>
      </article>

      <article class="stat-card">
        <span>Hết hạn</span>
        <strong>{{ stats.expired }}</strong>
      </article>

      <article class="stat-card">
        <span>Tạm ẩn</span>
        <strong>{{ stats.hidden }}</strong>
      </article>
    </section>

    <!-- Nội dung -->
    <div class="page-card">

      <!-- Loading -->
      <div v-if="loading" class="state-box">
        <i class="bi bi-arrow-repeat spin"></i> Đang tải dữ liệu...
      </div>

      <!-- Lỗi -->
      <div v-else-if="loadError" class="state-box state-error">
        <i class="bi bi-exclamation-circle"></i> {{ loadError }}
        <button class="btn-retry" @click="loadAll">Thử lại</button>
      </div>

      <template v-else>
        <BannerFilter
          :filters="filters"
          :positions="positionOptions"
          @update:search="filters.search = $event"
          @update:position="filters.position = $event"
          @update:status="filters.status = $event"
        />

        <BannerTable
          :banners="filteredBanners"
          :positions="positionOptions"
          @edit="openEditModal"
          @toggle="handleToggle"
          @delete="confirmDelete"
        />
      </template>
    </div>

    <!-- Modal thêm/sửa -->
    <BannerModal
      :visible="modalVisible"
      :banner="editingBanner"
      :next-order="banners.length + 1"
      :positions="positionOptions"
      @close="modalVisible = false"
      @save="handleSave"
    />

    <!-- MODAL THÔNG BÁO / LỖI THAY THẾ CHO ALERT BROWSER -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="dialog.show" class="dialog-backdrop" @click.self="dialog.show = false">
          <div class="dialog-card">
            <!-- Icon cảnh báo / lỗi -->
            <div class="dialog-icon" :class="dialog.type">
              <i v-if="dialog.type === 'error'" class="bi bi-exclamation-triangle-fill"></i>
              <i v-else-if="dialog.type === 'confirm'" class="bi bi-question-circle-fill"></i>
              <i v-else class="bi bi-check-circle-fill"></i>
            </div>

            <!-- Nội dung thông báo -->
            <div class="dialog-content">
              <h3 class="dialog-title">{{ dialog.title }}</h3>
              <p class="dialog-msg">{{ dialog.message }}</p>
            </div>

            <!-- Nút bấm -->
            <div class="dialog-actions">
              <button 
                v-if="dialog.isConfirm" 
                class="btn-dialog btn-dialog-cancel" 
                @click="dialog.show = false"
              >
                Hủy
              </button>
              <button 
                class="btn-dialog btn-dialog-ok" 
                :class="dialog.type"
                @click="handleDialogOk"
              >
                {{ dialog.isConfirm ? 'Xác nhận' : 'Đã hiểu' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue';
import BannerFilter from './Bannerfilter.vue';
import BannerTable from './Bannertable.vue';
import BannerModal from './Bannermodal.vue';
import { bannerApi } from '@/api/BannerApi';

// ---- State ----
const banners      = ref([]);
const rawPositions = ref([]); 
const loading      = ref(true);
const loadError    = ref('');
const modalVisible  = ref(false);
const editingBanner = ref(null);

// ---- State Custom Dialog Thông Báo (Thay alert) ----
const dialog = reactive({
  show: false,
  title: 'Thông báo',
  message: '',
  type: 'error', // 'error' | 'success' | 'confirm'
  isConfirm: false,
  onOk: null
});

function showAlertDialog(message, title = 'Thông báo', type = 'error') {
  dialog.title = title;
  dialog.message = message;
  dialog.type = type;
  dialog.isConfirm = false;
  dialog.onOk = null;
  dialog.show = true;
}

function showConfirmDialog(message, title = 'Xác nhận xóa', onConfirm) {
  dialog.title = title;
  dialog.message = message;
  dialog.type = 'confirm';
  dialog.isConfirm = true;
  dialog.onOk = onConfirm;
  dialog.show = true;
}

function handleDialogOk() {
  if (dialog.isConfirm && typeof dialog.onOk === 'function') {
    dialog.onOk();
  }
  dialog.show = false;
}

// positionOptions lấy từ API
const positionOptions = computed(() =>
  rawPositions.value.map(p => ({
    value: p.positionId,
    label: p.description || p.positionCode,
    code:  p.positionCode,
    allowsOrder: !!p.allowsOrder,
    maxSlots: p.maxSlots || 1,
  }))
);

// ---- Bộ lọc ----
const filters = reactive({ search: '', position: '', status: '' });

function computeStatus(b) {
  if (!b.isActive) return 'hidden';
  const now = new Date();
  const s = b.startDate ? new Date(b.startDate) : null;
  const e = b.endDate   ? new Date(b.endDate)   : null;
  if (e && now > e)  return 'expired';
  if (s && now < s)  return 'scheduled';
  return 'active';
}

const stats = computed(() => {
  let active = 0, expired = 0, hidden = 0;
  banners.value.forEach(b => {
    const s = computeStatus(b);
    if (s === 'active' || s === 'scheduled') active++;
    else if (s === 'expired') expired++;
    else if (s === 'hidden') hidden++;
  });
  return { total: banners.value.length, active, expired, hidden };
});

const filteredBanners = computed(() => {
  let list = [...banners.value];
  if (filters.search.trim()) {
    const q = filters.search.toLowerCase();
    list = list.filter(b => b.title?.toLowerCase().includes(q));
  }
  if (filters.position) {
    list = list.filter(b => String(b.positionId) === String(filters.position));
  }
  if (filters.status) {
    list = list.filter(b => computeStatus(b) === filters.status);
  }
  if (filters.position) {
    list.sort((a, b) => a.displayOrder - b.displayOrder);
  }
  return list;
});

// ---- Convert ngày ----
function toDateInput(iso) { return iso ? iso.slice(0, 10) : ''; }
function toApiDateTime(d) { return d ? `${d}T00:00:00` : null; }

function mapFromApi(b) {
  return {
    id: b.id,
    title: b.title,
    positionId: b.positionId,
    positionCode: b.positionCode,
    positionDescription: b.positionDescription,
    imageUrl: b.imageUrl,
    linkUrl: b.targetUrl || '',
    displayOrder: b.displayOrder,
    startDate: toDateInput(b.startDate),
    endDate: toDateInput(b.endDate),
    isActive: !!b.isActive,
  };
}

function mapToApi(form) {
  return {
    title: form.title?.trim() || null,
    imageUrl: form.imageUrl?.trim() || null,
    targetUrl: form.linkUrl?.trim() || null,
    displayOrder: form.displayOrder ?? 0,
    isActive: !!form.isActive,
    startDate: toApiDateTime(form.startDate),
    endDate: toApiDateTime(form.endDate),
    positionId: form.positionId ? Number(form.positionId) : null,
  };
}

// ---- API ----
async function loadAll() {
  loading.value = true;
  loadError.value = '';
  try {
    const [bannerRes, posRes] = await Promise.all([
      bannerApi.getAll(),
      bannerApi.getPositions(),
    ]);
    banners.value      = bannerRes.map(mapFromApi);
    rawPositions.value = posRes;
  } catch {
    loadError.value = 'Không tải được dữ liệu banner. Vui lòng thử lại.';
  } finally {
    loading.value = false;
  }
}

onMounted(loadAll);

function openAddModal() {
  editingBanner.value = null;
  modalVisible.value = true;
}
function openEditModal(banner) {
  editingBanner.value = banner;
  modalVisible.value = true;
}

async function handleSave(formData) {
  try {
    const payload = mapToApi(formData);
    if (formData.id) {
      const updated = await bannerApi.update(formData.id, payload);
      const idx = banners.value.findIndex(b => b.id === formData.id);
      if (idx > -1) banners.value[idx] = mapFromApi(updated);
    } else {
      const created = await bannerApi.create(payload);
      banners.value.unshift(mapFromApi(created));
    }
    modalVisible.value = false;
  } catch (err) {
    // THAY THẾ ALERT MẶC ĐỊNH BẰNG DIALOG MỚI DỄ NHÌN
    const msg = err?.response?.data?.message || 'Lưu banner thất bại. Vui lòng thử lại.';
    showAlertDialog(msg, 'Trùng vị trí / Thứ tự', 'error');
  }
}

async function handleToggle(banner) {
  const idx = banners.value.findIndex(b => b.id === banner.id);
  if (idx === -1) return;
  const newActive = !banner.isActive;
  banners.value[idx].isActive = newActive;
  try {
    const payload = { ...mapToApi(banners.value[idx]), isActive: newActive };
    await bannerApi.update(banner.id, payload);
  } catch (err) {
    banners.value[idx].isActive = !newActive;
    showAlertDialog(err?.response?.data?.message || 'Cập nhật trạng thái thất bại.', 'Lỗi hệ thống', 'error');
  }
}

function confirmDelete(banner) {
  showConfirmDialog(
    `Bạn có chắc chắn muốn xóa banner "${banner.title || 'này'}" không?`,
    'Xác nhận xóa',
    async () => {
      try {
        await bannerApi.delete(banner.id);
        banners.value = banners.value.filter(b => b.id !== banner.id);
      } catch (err) {
        showAlertDialog(err?.response?.data?.message || 'Xóa banner thất bại.', 'Lỗi', 'error');
      }
    }
  );
}
</script>

<style scoped>
.bm-page {
  padding: 24px;
  background: #f9fafb;
  min-height: 100%;
}

/* Page Header */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.page-header-left { display: flex; align-items: center; gap: 16px; }
.page-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f55d9b, #ec4d8d);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  flex-shrink: 0;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #f55d9b;
  margin: 0;
}
.page-sub {
  font-size: 13px;
  color: #6b7280;
  margin: 2px 0 0;
}
.btn-add {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f55d9b;
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-add:hover { background: #ec4d8d; }

/* Card */
.page-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow: hidden;
}

/* Khối KPI tổng quan */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 992px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  border: 1px solid #f3d6e3;
  background: #ffffff;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.06);
  padding: 20px 18px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 100px;
}

.stat-card span {
  display: block;
  color: #6b7280;
  font-size: 0.86rem;
  font-weight: 700;
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  font-size: 1.65rem;
  line-height: 1;
}

.text-accent {
  color: #f55d9b;
}

/* States */
.state-box {
  padding: 48px;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.state-error { color: #dc2626; }
.spin { animation: spin 1s linear infinite; font-size: 24px; }
@keyframes spin { to { transform: rotate(360deg); } }
.btn-retry {
  margin-top: 8px;
  background: #f55d9b;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 7px 18px;
  font-size: 13px;
  cursor: pointer;
}
.btn-retry:hover { background: #ec4d8d; }

/* --- STYLES CỦA CUSTOM DIALOG DIALOG THAY ALERT --- */
.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 99999;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.dialog-card {
  background: #ffffff;
  width: 100%;
  max-width: 400px;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 8px 10px -6px rgba(0,0,0,0.05);
  text-align: center;
  animation: dialog-pop 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.dialog-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  margin: 0 auto 16px;
}

.dialog-icon.error {
  background: #fef2f2;
  color: #dc2626;
}

.dialog-icon.confirm {
  background: #fffbebf;
  color: #d97706;
}

.dialog-icon.success {
  background: #f0fdf4;
  color: #16a34a;
}

.dialog-title {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px;
}

.dialog-msg {
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
  margin: 0 0 20px;
}

.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.btn-dialog {
  flex: 1;
  padding: 9px 16px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-dialog-cancel {
  background: #f1f5f9;
  color: #64748b;
}
.btn-dialog-cancel:hover {
  background: #e2e8f0;
}

.btn-dialog-ok.error {
  background: #f55d9b;
  color: #fff;
}
.btn-dialog-ok.error:hover {
  background: #ec4d8d;
}

.btn-dialog-ok.confirm {
  background: #dc2626;
  color: #fff;
}

@keyframes dialog-pop {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}

.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.2s;
}
.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
}
</style>