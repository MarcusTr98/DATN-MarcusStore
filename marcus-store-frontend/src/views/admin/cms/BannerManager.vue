<template>
  <div class="bm-page">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-icon">
          <i class="ti ti-layout-board"></i>
        </div>
        <div>
          <h2 class="page-title">Quản lý Banner</h2>
          <p class="page-sub">Quản lý banner quảng cáo hiển thị trên website.</p>
        </div>
      </div>
      <button class="btn-add" @click="openAddModal">
        <i class="ti ti-plus"></i> Thêm banner
      </button>
    </div>

    <!-- Nội dung -->
    <div class="page-card">

      <!-- Loading -->
      <div v-if="loading" class="state-box">
        <i class="ti ti-loader-2 spin"></i> Đang tải dữ liệu...
      </div>

      <!-- Lỗi -->
      <div v-else-if="loadError" class="state-box state-error">
        <i class="ti ti-alert-circle"></i> {{ loadError }}
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
          @delete="openDeleteConfirm"
        />

        <!-- Pagination info -->
        <div class="pagination-row">
          <span class="pg-info">Hiển thị {{ filteredBanners.length }} / {{ banners.length }} banner</span>
        </div>
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

    <!-- Modal xác nhận xóa (thay thế confirm() xấu xí) -->
    <Teleport to="body">
      <div v-if="deleteTarget" class="confirm-overlay" @click.self="deleteTarget = null">
        <div class="confirm-box">
          <div class="confirm-icon">
            <i class="ti ti-alert-triangle"></i>
          </div>
          <h3 class="confirm-title">Xác nhận xóa banner</h3>
          <p class="confirm-msg">
            Bạn có chắc muốn xóa banner
            <strong>"{{ deleteTarget.title }}"</strong>?<br />
            <span class="confirm-note">Banner sẽ bị ẩn khỏi website (có thể khôi phục sau).</span>
          </p>
          <div class="confirm-actions">
            <button class="btn-cancel-confirm" @click="deleteTarget = null">Hủy bỏ</button>
            <button class="btn-confirm-del" :disabled="deleting" @click="confirmDelete">
              <i class="ti ti-trash"></i>
              {{ deleting ? 'Đang xóa...' : 'Xác nhận xóa' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue';
import BannerFilter from './BannerFilter.vue';
import BannerTable from './BannerTable.vue';
import BannerModal from './BannerModal.vue';
import { bannerApi } from '@/api/BannerApi';

// ---- State ----
const banners    = ref([]);
const rawPositions = ref([]); // danh sách vị trí thật từ API
const loading    = ref(true);
const loadError  = ref('');
const modalVisible  = ref(false);
const editingBanner = ref(null);
const deleteTarget  = ref(null);
const deleting      = ref(false);

// positionOptions: lấy từ API /banners/positions → đủ tất cả vị trí kể cả chưa có banner
const positionOptions = computed(() =>
  rawPositions.value.map(p => ({
    value: p.positionId,
    label: p.description || p.positionCode,
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
    banners.value    = bannerRes.map(mapFromApi);
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
function openDeleteConfirm(banner) {
  deleteTarget.value = banner;
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
    alert(err?.response?.data?.message || 'Lưu banner thất bại. Vui lòng thử lại.');
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) return;
  deleting.value = true;
  try {
    await bannerApi.remove(deleteTarget.value.id);
    // Soft-delete: cập nhật isActive = false thay vì xóa khỏi mảng
    const idx = banners.value.findIndex(b => b.id === deleteTarget.value.id);
    if (idx > -1) banners.value[idx].isActive = false;
    deleteTarget.value = null;
  } catch (err) {
    alert(err?.response?.data?.message || 'Xóa banner thất bại.');
  } finally {
    deleting.value = false;
  }
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

/* Pagination row */
.pagination-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 14px 24px;
  border-top: 1px solid #f3e8ee;
}
.pg-info { font-size: 13px; color: #6b7280; }

/* Modal xác nhận xóa */
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,0.46);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.confirm-box {
  background: #fff;
  border-radius: 16px;
  padding: 32px 28px;
  width: 420px;
  max-width: 95vw;
  text-align: center;
  box-shadow: 0 20px 60px rgba(15,23,42,0.18);
}
.confirm-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #fff5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 26px;
  color: #dc2626;
}
.confirm-title {
  font-size: 17px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 10px;
}
.confirm-msg {
  font-size: 14px;
  color: #4b5563;
  margin: 0 0 24px;
  line-height: 1.6;
}
.confirm-note {
  font-size: 12px;
  color: #9ca3af;
}
.confirm-actions { display: flex; gap: 10px; justify-content: center; }
.btn-cancel-confirm {
  padding: 9px 22px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-cancel-confirm:hover { border-color: #d1d5db; background: #f9fafb; }
.btn-confirm-del {
  padding: 9px 22px;
  border: none;
  border-radius: 8px;
  background: #dc2626;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background 0.15s;
}
.btn-confirm-del:hover:not(:disabled) { background: #b91c1c; }
.btn-confirm-del:disabled { opacity: 0.6; cursor: not-allowed; }
</style>