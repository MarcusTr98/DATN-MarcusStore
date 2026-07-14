<template>
  <div class="al-page">

    <div class="page-header">
      <div class="page-header-left">
        <div class="page-icon">
          <i class="bi bi-clock-history"></i>
        </div>
        <div>
          <h2 class="page-title">Nhật ký hoạt động</h2>
          <p class="page-sub">Theo dõi toàn bộ thao tác tạo/sửa/xoá dữ liệu trong hệ thống.</p>
        </div>
      </div>
      <button class="btn-add" :disabled="exporting" @click="handleExport">
        <i class="bi" :class="exporting ? 'bi-arrow-repeat spin' : 'bi-download'"></i>
        {{ exporting ? 'Đang xuất...' : 'Xuất CSV' }}
      </button>
    </div>

    <section class="stats-grid">
      <article class="stat-card">
        <span>Tổng số log</span>
        <strong>{{ stats.total }}</strong>
      </article>
      <article class="stat-card">
        <span>Tạo mới (Create)</span>
        <strong class="text-success">{{ stats.create }}</strong>
      </article>
      <article class="stat-card">
        <span>Cập nhật (Update)</span>
        <strong class="text-warning">{{ stats.update }}</strong>
      </article>
      <article class="stat-card">
        <span>Xoá (Delete)</span>
        <strong class="text-danger">{{ stats.delete }}</strong>
      </article>
    </section>

    <div class="page-card">
      <div v-if="loading" class="state-box">
        <i class="bi bi-arrow-repeat spin"></i> Đang tải dữ liệu...
      </div>

      <div v-else-if="loadError" class="state-box state-error">
        <i class="bi bi-exclamation-circle"></i> {{ loadError }}
        <button class="btn-retry" @click="loadAll">Thử lại</button>
      </div>

      <template v-else>
        <!-- Bộ lọc -->
        <div class="filter-wrap">
          <div class="filter-group">
            <label class="filter-label">TÌM KIẾM</label>
            <div class="search-box">
              <i class="bi bi-search search-icon"></i>
              <input class="filter-input" type="text" v-model="filters.search"
                placeholder="Tìm theo mô tả, username, IP..." />
            </div>
          </div>
          <div class="filter-group">
            <label class="filter-label">HÀNH ĐỘNG</label>
            <div class="select-wrap">
              <select class="filter-select" v-model="filters.actionType">
                <option value="">Tất cả</option>
                <option v-for="a in actionOptions" :key="a" :value="a">{{ actionLabel(a) }}</option>
              </select>
              <i class="bi bi-chevron-down select-arrow"></i>
            </div>
          </div>
          <div class="filter-group">
            <label class="filter-label">BẢNG DỮ LIỆU</label>
            <div class="select-wrap">
              <select class="filter-select" v-model="filters.tableName">
                <option value="">Tất cả</option>
                <option v-for="t in tableOptions" :key="t" :value="t">{{ tableLabel(t) }}</option>
              </select>
              <i class="bi bi-chevron-down select-arrow"></i>
            </div>
          </div>
          <button class="btn-reset" title="Đặt lại" @click="resetFilters">
            <i class="bi bi-arrow-clockwise"></i>
          </button>
        </div>

        <!-- Bảng -->
        <div class="table-section">
          <table class="tbl">
            <thead>
              <tr>
                <th style="width:56px">#</th>
                <th style="width:100px">HÀNH ĐỘNG</th>
                <th style="width:150px">BẢNG</th>
                <th>MÔ TẢ</th>
                <th style="width:170px">NGƯỜI THỰC HIỆN</th>
                <th style="width:120px">IP</th>
                <th style="width:150px">THỜI GIAN</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!paged.length">
                <td colspan="7" class="empty-row">
                  <i class="bi bi-inbox empty-icon"></i>
                  <span>Không có log nào khớp bộ lọc</span>
                </td>
              </tr>
              <tr v-for="(log, i) in paged" :key="log.logId" :class="i % 2 === 1 ? 'row-alt' : ''">
                <td class="td-id">#{{ log.logId }}</td>
                <td>
                  <span class="action-badge" :class="actionClass(log.actionType)">{{ actionLabel(log.actionType) }}</span>
                </td>
                <td class="table-cell" :title="log.tableName">{{ tableLabel(log.tableName) }}</td>
                <td class="desc-cell" :title="log.description">{{ log.description || '—' }}</td>
                <td class="user-cell">
                  <template v-if="log.userId">
                    <div class="user-name">{{ log.fullName || log.username }}</div>
                    <div class="user-sub">@{{ log.username }}</div>
                  </template>
                  <span v-else class="text-deleted">Người dùng đã xoá</span>
                </td>
                <td class="ip-cell mono">{{ log.ipAddress || '—' }}</td>
                <td class="date-cell">{{ log.createdAt }}</td>
              </tr>
            </tbody>
          </table>

          <div v-if="filteredLogs.length > 0" class="voucher-pagination">
            <div class="pagination-summary">
              Tổng <strong>{{ filteredLogs.length }}</strong> log
            </div>
            <div class="pagination-controls">
              <label class="page-size-control">
                <span>Hiển thị</span>
                <select v-model.number="pageSize" class="form-select form-select-sm">
                  <option :value="10">10</option>
                  <option :value="20">20</option>
                  <option :value="50">50</option>
                  <option :value="100">100</option>
                </select>
              </label>
              <button type="button" class="pagination-button" :disabled="page === 1" @click="page--">Trước</button>
              <span class="page-indicator">Trang <strong>{{ page }}</strong> / {{ totalPages }}</span>
              <button type="button" class="pagination-button" :disabled="page === totalPages" @click="page++">Sau</button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <div class="toast-wrap">
      <div v-for="t in toasts" :key="t.id" class="toast" :class="t.type">{{ t.message }}</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch, onMounted } from 'vue';
import { auditLogApi } from '@/api/Auditlogapi';

const logs = ref([]);
const loading = ref(true);
const loadError = ref('');
const exporting = ref(false);

const toasts = ref([]);
function pushToast(type, message) {
  const id = Date.now() + Math.random();
  toasts.value.push({ id, type, message });
  setTimeout(() => { toasts.value = toasts.value.filter((t) => t.id !== id); }, 3200);
}

const filters = reactive({ search: '', actionType: '', tableName: '' });

// Luôn hiện đủ 3 hành động chuẩn (đúng theo comment trong entity AuditLog:
// "CREATE, UPDATE, DELETE"), kể cả khi dữ liệu hiện tại chưa có action đó.
// Nếu sau này phát sinh action lạ khác ngoài 3 loại này, vẫn tự động thêm vào cuối.
const STANDARD_ACTIONS = ['CREATE', 'UPDATE', 'DELETE'];
const actionOptions = computed(() => {
  const found = new Set(logs.value.map((l) => l.actionType).filter(Boolean));
  const extra = [...found].filter((a) => !STANDARD_ACTIONS.includes(a)).sort();
  return [...STANDARD_ACTIONS, ...extra];
});
const tableOptions = computed(() => [...new Set(logs.value.map((l) => l.tableName).filter(Boolean))].sort());

// Map tên bảng thô trong DB sang tên tiếng Việt thân thiện hơn khi hiển thị.
// Bảng nào chưa có trong map thì tự fallback: thay "_" bằng khoảng trắng.
const TABLE_LABELS = {
  Users: 'Người dùng',
  Roles: 'Vai trò',
  Permissions: 'Quyền hạn',
  Categories: 'Danh mục sản phẩm',
  Products: 'Sản phẩm',
  Product_Images: 'Ảnh sản phẩm',
  Product_Skus: 'Biến thể sản phẩm (SKU)',
  Product_Items: 'Kho IMEI',
  Attributes: 'Thuộc tính',
  Attribute_Values: 'Giá trị thuộc tính',
  Carts: 'Giỏ hàng',
  Cart_Items: 'Mục giỏ hàng',
  Wishlists: 'Yêu thích',
  Vouchers: 'Voucher',
  User_Vouchers: 'Voucher người dùng',
  Flash_Sale_Slots: 'Khung giờ Flash Sale',
  Flash_Sale_Items: 'Sản phẩm Flash Sale',
  Orders: 'Đơn hàng',
  Order_Items: 'Chi tiết đơn hàng',
  Order_Status_History: 'Lịch sử trạng thái đơn',
  Order_Transactions: 'Giao dịch đơn hàng',
  Post_Categories: 'Danh mục bài viết',
  Posts: 'Bài viết',
  Banner_Positions: 'Vị trí banner',
  Banners: 'Banner',
  Comments_Evaluations: 'Đánh giá sản phẩm',
  Contact_Requests: 'Yêu cầu liên hệ',
  Admin_Notifications: 'Thông báo admin',
  System_Settings: 'Cấu hình hệ thống',
  Shipping_Config: 'Cấu hình vận chuyển',
  User_Addresses: 'Địa chỉ người dùng',
  Provinces: 'Tỉnh/Thành phố',
  Districts: 'Quận/Huyện',
  Wards: 'Phường/Xã',
  Audit_Logs: 'Nhật ký hoạt động',
};
function tableLabel(name) {
  if (!name) return '—';
  return TABLE_LABELS[name] || name.replace(/_/g, ' ');
}

const stats = computed(() => {
  let create = 0, update = 0, del = 0;
  logs.value.forEach((l) => {
    const a = (l.actionType || '').toUpperCase();
    if (a === 'CREATE') create++;
    else if (a === 'UPDATE') update++;
    else if (a === 'DELETE') del++;
  });
  return { total: logs.value.length, create, update, delete: del };
});

const filteredLogs = computed(() => {
  let list = [...logs.value];
  if (filters.search.trim()) {
    const q = filters.search.trim().toLowerCase();
    list = list.filter((l) =>
      (l.description || '').toLowerCase().includes(q) ||
      (l.username || '').toLowerCase().includes(q) ||
      (l.fullName || '').toLowerCase().includes(q) ||
      (l.ipAddress || '').toLowerCase().includes(q)
    );
  }
  if (filters.actionType) list = list.filter((l) => l.actionType === filters.actionType);
  if (filters.tableName) list = list.filter((l) => l.tableName === filters.tableName);
  return list; // BE đã trả sẵn theo thứ tự createdAt desc
});

function actionClass(action) {
  const a = (action || '').toUpperCase();
  if (a === 'CREATE') return 'act-create';
  if (a === 'UPDATE') return 'act-update';
  if (a === 'DELETE') return 'act-delete';
  return 'act-other';
}

const ACTION_LABELS = {
  CREATE: 'Tạo mới',
  UPDATE: 'Cập nhật',
  DELETE: 'Xoá',
};
function actionLabel(action) {
  if (!action) return '—';
  return ACTION_LABELS[action.toUpperCase()] || action;
}

// Phân trang client
const page = ref(1);
const pageSize = ref(20);
watch(filteredLogs, () => { page.value = 1; });
watch(pageSize, () => { page.value = 1; });
const totalPages = computed(() => Math.max(1, Math.ceil(filteredLogs.value.length / pageSize.value)));
const paged = computed(() => filteredLogs.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value));

function resetFilters() {
  filters.search = '';
  filters.actionType = '';
  filters.tableName = '';
}

async function loadAll() {
  loading.value = true;
  loadError.value = '';
  try {
    logs.value = await auditLogApi.getAll();
  } catch {
    loadError.value = 'Không tải được nhật ký hoạt động. Vui lòng thử lại.';
  } finally {
    loading.value = false;
  }
}
onMounted(loadAll);

async function handleExport() {
  exporting.value = true;
  try {
    const blob = await auditLogApi.exportCsv();
    const url = window.URL.createObjectURL(new Blob([blob]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'audit-logs.csv');
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  } catch {
    pushToast('error', 'Xuất file CSV thất bại. Vui lòng thử lại.');
  } finally {
    exporting.value = false;
  }
}
</script>

<style scoped>
.al-page { padding: 24px; background: #f9fafb; min-height: 100%; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.page-header-left { display: flex; align-items: center; gap: 16px; }
.page-icon {
  width: 48px; height: 48px; border-radius: 12px;
  background: linear-gradient(135deg, #f55d9b, #ec4d8d);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 22px; flex-shrink: 0;
}
.page-title { font-size: 22px; font-weight: 700; color: #f55d9b; margin: 0; }
.page-sub { font-size: 13px; color: #6b7280; margin: 2px 0 0; }
.btn-add {
  display: flex; align-items: center; gap: 6px;
  background: #f55d9b; color: #fff; border: none; border-radius: 10px;
  padding: 10px 20px; font-size: 14px; font-weight: 500; cursor: pointer;
  transition: background 0.15s;
}
.btn-add:hover:not(:disabled) { background: #ec4d8d; }
.btn-add:disabled { background: #f3d6e3; color: #b4557d; cursor: not-allowed; }

.page-card { background: #fff; border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); overflow: hidden; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
@media (max-width: 992px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
.stat-card {
  border: 1px solid #f3d6e3; background: #ffffff; box-shadow: 0 4px 18px rgba(15, 23, 42, 0.06);
  padding: 20px 18px; border-radius: 8px; display: flex; flex-direction: column;
  justify-content: space-between; min-height: 100px;
}
.stat-card span { display: block; color: #6b7280; font-size: 0.86rem; font-weight: 700; }
.stat-card strong { display: block; margin-top: 6px; font-size: 1.65rem; line-height: 1; }
.text-success { color: #15803d; }
.text-warning { color: #c2410c; }
.text-danger { color: #dc2626; }

.state-box { padding: 48px; text-align: center; color: #9ca3af; font-size: 14px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.state-error { color: #dc2626; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.btn-retry {
  margin-top: 8px; background: #f55d9b; color: #fff; border: none; border-radius: 8px;
  padding: 7px 18px; font-size: 13px; cursor: pointer;
}
.btn-retry:hover { background: #ec4d8d; }

/* Filter */
.filter-wrap {
  display: flex; align-items: flex-end; gap: 16px; flex-wrap: wrap;
  padding: 20px 24px; border-bottom: 1px solid #f3e8ee;
}
.filter-group { display: flex; flex-direction: column; gap: 6px; min-width: 180px; flex: 1; }
.filter-label { font-size: 11px; font-weight: 600; color: #f55d9b; letter-spacing: 0.06em; }
.search-box { position: relative; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: #9ca3af; font-size: 15px; pointer-events: none; }
.filter-input {
  width: 100%; padding: 9px 12px 9px 34px; border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 13px; color: #111827; background: #fff; outline: none; transition: border 0.15s;
}
.filter-input:focus { border-color: #f55d9b; box-shadow: 0 0 0 3px rgba(245,93,155,0.08); }
.select-wrap { position: relative; }
.filter-select {
  width: 100%; padding: 9px 32px 9px 12px; border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 13px; color: #111827; background: #fff; outline: none; appearance: none; cursor: pointer; transition: border 0.15s;
}
.filter-select:focus { border-color: #f55d9b; box-shadow: 0 0 0 3px rgba(245,93,155,0.08); }
.select-arrow { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); color: #9ca3af; font-size: 14px; pointer-events: none; }
.btn-reset {
  padding: 9px 14px; border: 1px solid #e5e7eb; border-radius: 8px; background: #fff; color: #6b7280;
  cursor: pointer; font-size: 16px; transition: all 0.15s; flex-shrink: 0; margin-bottom: 0;
}
.btn-reset:hover { border-color: #f55d9b; color: #f55d9b; background: #fff0f7; }

/* Table */
.table-section { overflow-x: auto; }
.tbl { width: 100%; min-width: 960px; border-collapse: collapse; font-size: 13px; }
.tbl thead tr { border-bottom: 2px solid #f3e8ee; }
.tbl th {
  padding: 12px 14px; text-align: left; font-size: 11px; font-weight: 700; color: #f55d9b;
  letter-spacing: 0.07em; white-space: nowrap;
}
.tbl td { padding: 10px 14px; color: #111827; border-bottom: 1px solid #f9f0f5; vertical-align: middle; }
.row-alt td { background: #fdf8fb; }
.tbl tr:hover td { background: #fff0f7; transition: background 0.1s; }
.td-id { color: #9ca3af; font-size: 11px; font-weight: 600; }
.mono { font-family: 'JetBrains Mono', 'Courier New', monospace; font-size: 12px; }

.action-badge {
  display: inline-flex; align-items: center; padding: 3px 10px; border-radius: 20px;
  font-size: 11px; font-weight: 700; letter-spacing: 0.02em;
}
.act-create { background: #f0fdf4; color: #15803d; }
.act-update { background: #fff7ed; color: #c2410c; }
.act-delete { background: #fef2f2; color: #dc2626; }
.act-other { background: #f1f5f9; color: #64748b; }

.table-cell { color: #4b5563; }
.desc-cell {
  max-width: 340px; color: #4b5563; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.user-cell { min-width: 130px; }
.user-name { font-weight: 500; color: #111827; }
.user-sub { font-size: 11px; color: #9ca3af; }
.text-deleted { font-size: 12px; color: #adb5bd; font-style: italic; }
.ip-cell { color: #6b7280; }
.date-cell { color: #4b5563; white-space: nowrap; }

.voucher-pagination {
  display: flex; align-items: center; justify-content: space-between; gap: 14px;
  padding: 14px 18px; border-top: 1px solid #f3d6e3; background: #fffafd;
}
.pagination-summary { color: #4b5563; font-size: 0.92rem; }
.pagination-summary strong, .page-indicator strong { color: #111827; }
.pagination-controls { display: flex; align-items: center; gap: 10px; }
.page-size-control { display: flex; align-items: center; gap: 8px; margin: 0; color: #6b7280; font-size: 0.86rem; font-weight: 700; white-space: nowrap; }
.page-size-control .form-select { width: 78px; min-height: 38px; padding-top: 0.35rem; padding-bottom: 0.35rem; font-weight: 700; }
.pagination-button {
  min-width: 72px; min-height: 38px; border: 1px solid #d8dee8; border-radius: 8px;
  background: #ffffff; color: #344054; font-size: 0.9rem; font-weight: 800;
  transition: border-color 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}
.pagination-button:hover:not(:disabled), .pagination-button:focus:not(:disabled) {
  border-color: #f55d9b; background: #fff0f7; color: #be3f75;
}
.pagination-button:disabled { cursor: not-allowed; opacity: 0.55; }
.page-indicator { min-width: 96px; color: #344054; font-size: 0.92rem; font-weight: 700; text-align: center; white-space: nowrap; }

@media (max-width: 768px) {
  .voucher-pagination { align-items: flex-start; flex-direction: column; }
  .pagination-controls { width: 100%; flex-wrap: wrap; }
}

.empty-row { text-align: center; padding: 48px 20px; color: #9ca3af; }
.empty-icon { font-size: 32px; display: block; margin: 0 auto 8px; color: #e0b8cc; }

.toast-wrap { position: fixed; right: 24px; bottom: 24px; z-index: 10000; display: flex; flex-direction: column; gap: 10px; }
.toast { min-width: 260px; max-width: 360px; padding: 13px 16px; border-radius: 12px; box-shadow: 0 10px 30px rgba(15,23,42,0.18); font-size: 13.5px; font-weight: 500; }
.toast.success { background: #f0fdf4; color: #15803d; border: 1px solid #bbf0cc; }
.toast.error { background: #fef2f2; color: #b91c1c; border: 1px solid #f5c2c7; }
</style>