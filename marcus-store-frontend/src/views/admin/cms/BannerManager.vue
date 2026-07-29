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

    <!-- Modal xác nhận thay thế banner đang hiển thị -->
    <BannerReplaceConfirm
      :visible="conflictModal.visible"
      :existing="conflictModal.existingBanner"
      @confirm="confirmReplace"
      @cancel="cancelReplace"
    />

  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue';
import BannerFilter from './Bannerfilter.vue';
import BannerTable from './Bannertable.vue';
import BannerModal from './Bannermodal.vue';
import BannerReplaceConfirm from './BannerReplaceConfirm.vue';
import { bannerApi } from '@/api/BannerApi';

// ---- State ----
const banners    = ref([]);
const rawPositions = ref([]); // danh sách vị trí thật từ API
const loading    = ref(true);
const loadError  = ref('');
const modalVisible  = ref(false);
const editingBanner = ref(null);

// State cho modal xác nhận thay thế banner (áp dụng cho vị trí 1-slot: HOME_MIDDLE, CATEGORY_TOP...)
const conflictModal = reactive({
  visible: false,
  existingBanner: null,    // banner đang chiếm vị trí (sẽ bị tạm ẩn nếu người dùng đồng ý)
  pendingPayload: null,    // payload chuẩn bị gửi lên API
  pendingForm: null,       // form đầy đủ (giữ id để phân biệt create vs update)
});

// positionOptions: lấy từ API /banners/positions → đủ tất cả vị trí kể cả chưa có banner
const positionOptions = computed(() =>
  rawPositions.value.map(p => ({
    value: p.positionId,
    label: p.description || p.positionCode,
    code:  p.positionCode,     // giữ lại để hiển thị/debug, không dùng để so sánh logic nữa
    allowsOrder: !!p.allowsOrder, // true = vị trí có nhiều banner chạy tuần tự (vd: Slider)
    maxSlots: p.maxSlots || 1,    // số thứ tự tối đa cho phép chọn khi allowsOrder = true
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

// Khối KPI tổng quan — tính trên toàn bộ banner, không phụ thuộc bộ lọc đang chọn
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

/**
 * Kiểm tra vị trí có phải dạng "1 banner duy nhất" hay không.
 * Dựa vào flag allowsOrder do API trả về:
 *   - allowsOrder = true  → vị trí cho phép nhiều banner chạy tuần tự (vd: HOME_HERO_SLIDER)
 *   - allowsOrder = false → vị trí chỉ hiển thị 1 banner (vd: HOME_MIDDLE, CATEGORY_TOP)
 */
function isSingleSlotPosition(positionId) {
  const position = positionOptions.value.find(
    p => String(p.value) === String(positionId)
  );
  return position ? !position.allowsOrder : false;
}

/**
 * Tìm banner đang hiển thị (isActive = true) ở 1 vị trí, ngoại trừ chính banner đang edit.
 * Trả về null nếu không có banner nào đang dùng vị trí đó.
 */
function findActiveBannerInPosition(positionId, excludeBannerId) {
  return banners.value.find(b =>
    String(b.positionId) === String(positionId) &&
    b.isActive &&
    b.id !== excludeBannerId
  ) || null;
}

function resetConflictModal() {
  conflictModal.visible = false;
  conflictModal.existingBanner = null;
  conflictModal.pendingPayload = null;
  conflictModal.pendingForm = null;
}

/** Thực hiện gọi API create/update, không hỏi xung đột */
async function doSave(formData, payload) {
  if (formData.id) {
    const updated = await bannerApi.update(formData.id, payload);
    const idx = banners.value.findIndex(b => b.id === formData.id);
    if (idx > -1) banners.value[idx] = mapFromApi(updated);
  } else {
    const created = await bannerApi.create(payload);
    banners.value.unshift(mapFromApi(created));
  }
  modalVisible.value = false;
}

/** Lưu banner — nếu vị trí 1-slot đã có banner đang hiển thị thì hỏi trước khi save */
async function handleSave(formData) {
  try {
    const payload = mapToApi(formData);

    // Chỉ cần kiểm tra xung đột khi vị trí chỉ cho phép hiển thị 1 banner
    if (isSingleSlotPosition(formData.positionId)) {
      const existing = findActiveBannerInPosition(formData.positionId, formData.id);

      if (existing) {
        // Lưu thông tin pending, mở modal xác nhận, return ở đây
        conflictModal.existingBanner = existing;
        conflictModal.pendingPayload = payload;
        conflictModal.pendingForm = formData;
        conflictModal.visible = true;
        return;
      }
    }

    // Vị trí nhiều-slot (slider) hoặc vị trí 1-slot chưa có banner → lưu bình thường
    await doSave(formData, payload);
  } catch (err) {
    console.error('Save banner error:', err);
    alert(err?.response?.data?.message || 'Lưu banner thất bại. Vui lòng thử lại.');
  }
}

/** Người dùng đồng ý thay thế: tạm ẩn banner cũ, sau đó lưu banner mới */
async function confirmReplace() {
  const { pendingPayload, pendingForm, existingBanner } = conflictModal;
  if (!pendingForm || !existingBanner) {
    resetConflictModal();
    return;
  }

  const originalActive = existingBanner.isActive;
  try {
    // Bước 1: tạm ẩn banner cũ (giữ nguyên nội dung, chỉ đổi isActive = false)
    const hiddenPayload = {
      ...mapToApi(existingBanner),
      isActive: false,
    };
    await bannerApi.update(existingBanner.id, hiddenPayload);

    // Cập nhật UI ngay để phản hồi tức thì
    const idx = banners.value.findIndex(b => b.id === existingBanner.id);
    if (idx > -1) banners.value[idx].isActive = false;

    // Bước 2: lưu banner mới (create hoặc update)
    await doSave(pendingForm, pendingPayload);

    resetConflictModal();
  } catch (err) {
    console.error('Replace banner error:', err);
    // Rollback trạng thái banner cũ nếu bước 1 đã thành công nhưng bước 2 lỗi
    const idx = banners.value.findIndex(b => b.id === existingBanner.id);
    if (idx > -1) banners.value[idx].isActive = originalActive;
    alert(err?.response?.data?.message || 'Thay thế banner thất bại. Vui lòng thử lại.');
  }
}

/** Người dùng chọn giữ nguyên: hủy modal, không lưu gì */
function cancelReplace() {
  resetConflictModal();
}

// Toggle isActive trực tiếp không cần confirm
async function handleToggle(banner) {
  const idx = banners.value.findIndex(b => b.id === banner.id);
  if (idx === -1) return;
  const newActive = !banner.isActive;
  // Optimistic update: cập nhật UI trước
  banners.value[idx].isActive = newActive;
  try {
    const payload = { ...mapToApi(banners.value[idx]), isActive: newActive };
    await bannerApi.update(banner.id, payload);
  } catch (err) {
    // Rollback nếu lỗi
    banners.value[idx].isActive = !newActive;
    alert(err?.response?.data?.message || 'Cập nhật trạng thái thất bại.');
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

/* Khối KPI tổng quan — copy nguyên từ Voucher.css để đồng bộ style */
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
</style>