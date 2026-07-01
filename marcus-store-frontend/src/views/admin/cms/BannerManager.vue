<template>
  <div class="pg">
    <div class="topbar">
      <div class="topbar-left">
        <i class="ti ti-layout-board topbar-icon"></i>
        <span class="page-title">Quản lý Banner</span>
        <span class="badge-count">{{ filteredBanners.length }} banner</span>
      </div>
      <div class="topbar-right">
        <button class="btn-add" @click="openAddModal">
          <i class="ti ti-plus"></i>
          Thêm banner
        </button>
      </div>
    </div>

    <div class="content">
      <div v-if="loading" class="state-box">Đang tải dữ liệu...</div>
      <div v-else-if="loadError" class="state-box state-error">
        {{ loadError }}
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
          @delete="handleDelete"
        />
      </template>
    </div>

    <BannerModal
      :visible="modalVisible"
      :banner="editingBanner"
      :next-order="banners.length + 1"
      :positions="positionOptions"
      @close="modalVisible = false"
      @save="handleSave"
    />
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue';
import BannerFilter from './BannerFilter.vue';
import BannerTable from './BannerTable.vue';
import BannerModal from './BannerModal.vue';
import { bannerApi } from '@/api/bannerApi';

// ---- State chính ----
const banners = ref([]); // dữ liệu thật từ API, field khớp BannerResponseDTO
const loading = ref(true);
const loadError = ref('');

// positionOptions suy ra trực tiếp từ field positionId/positionCode/positionDescription
// đã có sẵn trong mỗi banner (BannerService.toResponse() đã gắn kèm).
// Lưu ý: chỉ liệt kê được vị trí đã có ít nhất 1 banner. Nếu sau này cần cả
// vị trí "trống" chưa có banner nào, sẽ bổ sung API GET /banner-positions riêng.
const positionOptions = computed(() => {
  const map = new Map();
  banners.value.forEach((b) => {
    if (b.positionId != null && !map.has(b.positionId)) {
      map.set(b.positionId, {
        value: b.positionId,
        label: b.positionDescription || b.positionCode,
      });
    }
  });
  return Array.from(map.values());
});

// ---- Convert ngày giữa input[type=date] (YYYY-MM-DD) và LocalDateTime backend (YYYY-MM-DDTHH:mm:ss) ----
function toDateInput(isoDateTime) {
  if (!isoDateTime) return '';
  return isoDateTime.slice(0, 10); // lấy phần YYYY-MM-DD
}

function toApiDateTime(dateInput) {
  if (!dateInput) return null;
  return `${dateInput}T00:00:00`; // backend nhận LocalDateTime
}

// Chuyển 1 banner từ response backend -> format mà các component con đang dùng
function mapBannerFromApi(b) {
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

// Chuyển dữ liệu form -> đúng format BannerRequestDTO mà backend cần
function mapBannerToApi(form) {
  return {
    title: form.title?.trim() || null,
    imageUrl: form.imageUrl?.trim() || null,
    targetUrl: form.linkUrl?.trim() || null,
    displayOrder: form.displayOrder ?? 0,
    isActive: !!form.isActive,
    startDate: toApiDateTime(form.startDate),
    endDate: toApiDateTime(form.endDate),
    positionId: form.positionId ? Number(form.positionId) : null, // đảm bảo gửi số nguyên
  };
}

// ---- Tải dữ liệu ----
async function loadAll() {
  loading.value = true;
  loadError.value = '';
  try {
    const bannerRes = await bannerApi.getAll();
    banners.value = bannerRes.map(mapBannerFromApi);
  } catch (err) {
    loadError.value = 'Không tải được dữ liệu banner. Vui lòng thử lại.';
    console.error(err);
  } finally {
    loading.value = false;
  }
}

onMounted(loadAll);

// ---- Bộ lọc ----
const filters = reactive({
  search: '',
  position: '',
  status: '',
});

// Tính trạng thái real-time cho 1 banner (đồng bộ logic với BannerTable)
function computeStatus(b) {
  if (!b.isActive) return 'hidden';
  const now = new Date();
  const start = b.startDate ? new Date(b.startDate) : null;
  const end = b.endDate ? new Date(b.endDate) : null;
  if (end && now > end) return 'expired';
  if (start && now < start) return 'scheduled';
  return 'active';
}

const filteredBanners = computed(() => {
  let list = [...banners.value];

  if (filters.search.trim()) {
    const q = filters.search.toLowerCase();
    list = list.filter((b) => b.title.toLowerCase().includes(q));
  }
  // filters.position là string từ <select>, positionId trong data là số -> so sánh ép kiểu
  if (filters.position) {
    list = list.filter((b) => String(b.positionId) === String(filters.position));
  }
  if (filters.status) {
    list = list.filter((b) => computeStatus(b) === filters.status);
  }

  // UX logic: khi lọc theo 1 vị trí cụ thể, tự sắp xếp theo display_order tăng dần
  if (filters.position) {
    list.sort((a, b) => a.displayOrder - b.displayOrder);
  }

  return list;
});

// ---- Modal state ----
const modalVisible = ref(false);
const editingBanner = ref(null);

function openAddModal() {
  editingBanner.value = null;
  modalVisible.value = true;
}

function openEditModal(banner) {
  editingBanner.value = banner;
  modalVisible.value = true;
}

// ---- CRUD handlers gọi API thật ----
async function handleSave(formData) {
  try {
    const payload = mapBannerToApi(formData);
    if (formData.id) {
      const updated = await bannerApi.update(formData.id, payload);
      const idx = banners.value.findIndex((b) => b.id === formData.id);
      if (idx > -1) banners.value[idx] = mapBannerFromApi(updated);
    } else {
      const created = await bannerApi.create(payload);
      banners.value.unshift(mapBannerFromApi(created));
    }
    modalVisible.value = false;
  } catch (err) {
    const msg = err?.response?.data?.message || 'Lưu banner thất bại. Vui lòng thử lại.';
    alert(msg);
    console.error(err);
  }
}

async function handleDelete(banner) {
  if (!confirm(`Xóa banner "${banner.title}"?`)) return;
  try {
    await bannerApi.remove(banner.id);
    // Backend xóa mềm (set isActive = false), nên cập nhật trạng thái local tương ứng
    const idx = banners.value.findIndex((b) => b.id === banner.id);
    if (idx > -1) banners.value[idx].isActive = false;
  } catch (err) {
    const msg = err?.response?.data?.message || 'Xóa banner thất bại. Vui lòng thử lại.';
    alert(msg);
    console.error(err);
  }
}
</script>

<style scoped>
.pg {
  background: #fff7fa;
  min-height: 100vh;
}
.topbar {
  background: #fff;
  border-bottom: 1px solid #f3d6e3;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.topbar-icon {
  font-size: 20px;
  color: #f55d9b;
}
.page-title {
  font-size: 17px;
  font-weight: 500;
  color: #202636;
}
.badge-count {
  background: #ffe4ef;
  color: #d63384;
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 20px;
}
.btn-add {
  background: #f55d9b;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background 0.15s;
}
.btn-add:hover {
  background: #ec4d8d;
}
.content {
  padding: 16px 20px;
}
.state-box {
  background: #fff;
  border: 1px solid #f3d6e3;
  border-radius: 10px;
  padding: 40px 20px;
  text-align: center;
  color: #b4557d;
  font-size: 14px;
}
.state-error {
  color: #b91c1c;
}
.btn-retry {
  display: block;
  margin: 12px auto 0;
  background: #f55d9b;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
}
.btn-retry:hover {
  background: #ec4d8d;
}
</style>