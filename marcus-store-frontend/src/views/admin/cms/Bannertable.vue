<template>
  <div class="table-section">
    <table class="tbl">
      <thead>
        <tr>
          <th style="width:36px">#</th>
          <th style="width:80px">ẢNH</th>
          <th>TIÊU ĐỀ</th>
          <th style="width:140px">VỊ TRÍ</th>
          <th style="width:60px">THỨ TỰ</th>
          <th style="width:120px">THỜI GIAN CHẠY</th>
          <th style="width:120px">TRẠNG THÁI</th>
          <th style="width:110px">THAO TÁC</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!paged.length">
          <td colspan="8" class="empty-row">
            <i class="bi bi-inbox empty-icon"></i>
            <span>Không có banner nào</span>
          </td>
        </tr>
        <tr v-for="(b, i) in paged" :key="b.id" :class="i % 2 === 1 ? 'row-alt' : ''">
          <td class="td-id">#{{ (page - 1) * pageSize + i + 1 }}</td>
          <td>
            <div class="img-cell">
              <img v-if="b.imageUrl" :src="b.imageUrl" :alt="b.title" class="thumb"
                @error="e => e.target.style.display='none'" />
              <div v-else class="thumb-placeholder"><i class="bi bi-image"></i></div>
            </div>
          </td>
          <td class="title-cell">
            <div class="title-main">{{ b.title }}</div>
            <div v-if="b.linkUrl" class="title-sub">
              <i class="bi bi-link-45deg"></i> {{ b.linkUrl }}
            </div>
          </td>
          <td>
            <span class="pos-badge" :title="posLabel(b.positionId)">
              {{ posLabel(b.positionId) }}
            </span>
          </td>
          <td class="td-center">
            <span v-if="isSlider(b.positionId)" class="order-badge">{{ b.displayOrder }}</span>
            <span v-else class="order-na">—</span>
          </td>
          <td>
            <div class="date-col">
              <span>{{ fmtDate(b.startDate) }}</span>
              <span class="date-sep">→</span>
              <span>{{ fmtDate(b.endDate) }}</span>
            </div>
          </td>
          <td>
            <span class="status-badge" :class="statusOf(b).cls">{{ statusOf(b).label }}</span>
          </td>
          <td>
            <div class="action-row">
              <button class="btn-action btn-edit" @click="$emit('edit', b)" title="Sửa">
                <i class="bi bi-pencil-square"></i>
              </button>
              <label class="toggle-switch"
                :title="b.isActive ? 'Đang hiển thị — click để ẩn' : 'Đang ẩn — click để hiện'">
                <input type="checkbox" :checked="b.isActive" @change="$emit('toggle', b)" />
                <span class="toggle-track"></span>
              </label>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Phân trang (đồng bộ style với Quản lý Voucher) -->
    <div v-if="banners.length > 0" class="voucher-pagination">
      <div class="pagination-summary">
        Tổng <strong>{{ banners.length }}</strong> banner
      </div>
      <div class="pagination-controls">
        <label class="page-size-control">
          <span>Hiển thị</span>
          <select v-model.number="pageSize" class="form-select form-select-sm">
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>
        <button type="button" class="pagination-button" :disabled="page === 1" @click="page--">
          Trước
        </button>
        <span class="page-indicator">
          Trang <strong>{{ page }}</strong> / {{ totalPages }}
        </span>
        <button type="button" class="pagination-button" :disabled="page === totalPages" @click="page++">
          Sau
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

const props = defineProps({
  banners:   { type: Array, default: () => [] },
  positions: { type: Array, default: () => [] },
});
defineEmits(['edit', 'toggle']);

// ---- Phân trang (đồng bộ style Voucher: dropdown chọn số dòng/trang) ----
const page     = ref(1);
const pageSize = ref(10);

watch(() => props.banners, () => { page.value = 1; });
watch(pageSize, () => { page.value = 1; });

const totalPages = computed(() => Math.max(1, Math.ceil(props.banners.length / pageSize.value)));
const paged       = computed(() => props.banners.slice((page.value - 1) * pageSize.value, page.value * pageSize.value));

// ---- Helpers ----
function posLabel(positionId) {
  const found = props.positions.find(p => String(p.value) === String(positionId));
  return found ? found.label : '—';
}

// Vị trí có cho phép nhiều banner chạy tuần tự (đọc từ allowsOrder do API trả về),
// không còn hardcode so sánh positionCode === 'HOME_SLIDER' nữa
function isSlider(positionId) {
  const found = props.positions.find(p => String(p.value) === String(positionId));
  return !!found?.allowsOrder;
}

function fmtDate(d) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function statusOf(b) {
  if (!b.isActive) return { label: 'Tạm ẩn', cls: 'st-hidden' };
  const now = new Date();
  const s = b.startDate ? new Date(b.startDate) : null;
  const e = b.endDate   ? new Date(b.endDate)   : null;
  if (e && now > e) return { label: 'Hết hạn',       cls: 'st-expired'   };
  if (s && now < s) return { label: 'Lên lịch chạy', cls: 'st-scheduled' };
  return { label: 'Đang hiển thị', cls: 'st-active' };
}
</script>

<style scoped>
.table-section { overflow-x: auto; }
.tbl { width: 100%; min-width: 900px; border-collapse: collapse; font-size: 13px; }
.tbl thead tr { border-bottom: 2px solid #f3e8ee; }
.tbl th {
  padding: 12px 14px; text-align: left;
  font-size: 11px; font-weight: 700; color: #f55d9b;
  letter-spacing: 0.07em; white-space: nowrap;
}
.tbl td { padding: 10px 14px; color: #111827; border-bottom: 1px solid #f9f0f5; vertical-align: middle; }
.row-alt td { background: #fdf8fb; }
.tbl tr:hover td { background: #fff0f7; transition: background 0.1s; }
.td-id    { color: #9ca3af; font-size: 11px; font-weight: 600; }
.td-center { text-align: center; }

.img-cell { width:72px; height:42px; border-radius:6px; overflow:hidden; background:#f9f0f5; display:flex; align-items:center; justify-content:center; }
.thumb { width:100%; height:100%; object-fit:cover; }
.thumb-placeholder { color:#e0b8cc; font-size:20px; }

.title-cell { min-width:200px; max-width:300px; }
.title-main { font-weight:500; color:#111827; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.title-sub  { font-size:11px; color:#9ca3af; margin-top:2px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }

/* Badge vị trí — 1 dòng, không xuống dòng */
.pos-badge {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;
  background: #fff0f7;
  color: #d63384;
  border: 1px solid #f3d6e3;
  white-space: nowrap;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
}

.order-badge { display:inline-flex; align-items:center; justify-content:center; width:26px; height:26px; border-radius:8px; background:#f9f0f5; color:#d63384; font-size:13px; font-weight:600; }
.order-na { color:#d1d5db; }

.date-col { display:flex; flex-direction:column; gap:1px; font-size:12px; color:#4b5563; }
.date-sep { color:#d1d5db; font-size:10px; }

.status-badge { display:inline-flex; align-items:center; padding:3px 9px; border-radius:20px; font-size:11px; font-weight:500; white-space:nowrap; }
.st-active    { background:#f0fdf4; color:#15803d; }
.st-scheduled { background:#fff7ed; color:#c2410c; }
.st-expired   { background:#f1f5f9; color:#64748b; }
.st-hidden    { background:#fff0f7; color:#c72250; }

.action-row { display:flex; gap:5px; align-items:center; }
.btn-action { width:30px; height:30px; border-radius:7px; border:none; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:14px; transition:all .15s; }
.btn-edit { background:#fff0f7; color:#d63384; }
.btn-edit:hover { background:#ffe4ef; }

.toggle-switch { position:relative; width:34px; height:19px; display:inline-block; cursor:pointer; flex-shrink:0; }
.toggle-switch input { opacity:0; width:0; height:0; position:absolute; }
.toggle-track { position:absolute; inset:0; background:#e5e7eb; border-radius:20px; transition:background .2s; }
.toggle-track::before { content:''; position:absolute; width:13px; height:13px; left:3px; top:3px; background:#fff; border-radius:50%; transition:transform .2s; box-shadow:0 1px 3px rgba(0,0,0,.15); }
.toggle-switch input:checked + .toggle-track { background:#f55d9b; }
.toggle-switch input:checked + .toggle-track::before { transform:translateX(15px); }

/* Phân trang — copy nguyên từ Voucher.css để đồng bộ style */
.voucher-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 18px;
  border-top: 1px solid #f3d6e3;
  background: #fffafd;
}

.pagination-summary {
  color: #4b5563;
  font-size: 0.92rem;
}

.pagination-summary strong,
.page-indicator strong {
  color: #111827;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-size-control {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #6b7280;
  font-size: 0.86rem;
  font-weight: 700;
  white-space: nowrap;
}

.page-size-control .form-select {
  width: 78px;
  min-height: 38px;
  padding-top: 0.35rem;
  padding-bottom: 0.35rem;
  font-weight: 700;
}

.pagination-button {
  min-width: 72px;
  min-height: 38px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #ffffff;
  color: #344054;
  font-size: 0.9rem;
  font-weight: 800;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    color 0.18s ease;
}

.pagination-button:hover:not(:disabled),
.pagination-button:focus:not(:disabled) {
  border-color: #f55d9b;
  background: #fff0f7;
  color: #be3f75;
}

.pagination-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.page-indicator {
  min-width: 96px;
  color: #344054;
  font-size: 0.92rem;
  font-weight: 700;
  text-align: center;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .voucher-pagination {
    align-items: flex-start;
    flex-direction: column;
  }

  .pagination-controls {
    width: 100%;
    flex-wrap: wrap;
  }
}

.empty-row { text-align:center; padding:48px 20px; color:#9ca3af; }
.empty-icon { font-size:32px; display:block; margin:0 auto 8px; color:#e0b8cc; }
</style>