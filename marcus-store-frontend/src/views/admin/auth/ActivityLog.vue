<template>
  <div class="al-page">

    <!-- Header quản trị thống nhất -->
    <AdminPageHeader
      class="al-page-header"
      eyebrow="Kiểm soát hệ thống"
      eyebrow-icon="bi bi-shield-check"
      title="Quản lý thao tác"
      description="Theo dõi lịch sử tạo, cập nhật và xoá dữ liệu trong toàn bộ hệ thống."
      icon="bi bi-clock-history"
    >
      <template #actions>
        <div class="header-actions">
          <div class="dynamic-total" v-if="filteredLogs.length > 0">
            Tổng log lọc: <strong>{{ filteredLogs.length }}</strong>
          </div>
          <button class="btn-export-excel" :disabled="exporting" @click="handleExport">
            <i class="bi" :class="exporting ? 'bi-arrow-repeat spin' : 'bi-download'"></i>
            {{ exporting ? 'Đang xuất...' : 'Xuất CSV' }}
          </button>
        </div>
      </template>
    </AdminPageHeader>

    <div v-if="loading" class="state-box">
      <i class="bi bi-arrow-repeat spin"></i> Đang tải dữ liệu...
    </div>

    <div v-else-if="loadError" class="state-box state-error">
      <i class="bi bi-exclamation-circle"></i> {{ loadError }}
      <button class="btn-retry" @click="loadAll">Thử lại</button>
    </div>

    <template v-else>
      <!-- Stats Grid -->
      <section class="stats-grid">
        <article class="stat-card">
          <div class="stat-icon stat-icon-blue"><i class="bi bi-collection"></i></div>
          <div class="stat-body">
            <span>Tổng số log</span>
            <strong>{{ stats.total }}</strong>
          </div>
        </article>
        <article class="stat-card">
          <div class="stat-icon stat-icon-green"><i class="bi bi-plus-circle"></i></div>
          <div class="stat-body">
            <span>Tạo mới (Create)</span>
            <strong class="fin-accent">{{ stats.create }}</strong>
          </div>
        </article>
        <article class="stat-card">
          <div class="stat-icon stat-icon-amber"><i class="bi bi-pencil"></i></div>
          <div class="stat-body">
            <span>Cập nhật (Update)</span>
            <strong>{{ stats.update }}</strong>
          </div>
        </article>
        <article class="stat-card">
          <div class="stat-icon stat-icon-red"><i class="bi bi-trash"></i></div>
          <div class="stat-body">
            <span>Xoá (Delete)</span>
            <strong>{{ stats.delete }}</strong>
          </div>
        </article>
      </section>

      <!-- Chart Panel -->
      <section class="chart-panel" v-if="chartDays.length > 0">
        <div class="chart-header">
          <i class="bi bi-bar-chart-fill"></i> Phân bổ hoạt động theo ngày
          <span class="chart-subnote">(theo khoảng ngày đang lọc)</span>
        </div>
        <div class="chart-bars">
          <div class="chart-col" v-for="d in chartDays" :key="d.date">
            <div class="chart-bar" :title="`${d.date}: ${d.total} log`">
              <div class="seg seg-red" :style="{ height: pct(d.delete, chartMax) }"></div>
              <div class="seg seg-amber" :style="{ height: pct(d.update, chartMax) }"></div>
              <div class="seg seg-green" :style="{ height: pct(d.create, chartMax) }"></div>
            </div>
            <span class="chart-label">{{ d.date }}</span>
          </div>
        </div>
      </section>

      <!-- Filters & Table -->
      <div class="table-panel">
        <!-- Bộ lọc -->
        <div class="toolbar-panel">
          <div class="toolbar-row">
            <div class="field field-keyword">
              <label class="form-label">Tìm kiếm (Mô tả, Username, IP)</label>
              <div class="input-group">
                <span class="input-group-text"><i class="bi bi-search"></i></span>
                <input
                  v-model="filters.search"
                  type="text"
                  class="form-control"
                  placeholder="Nhập từ khóa..."
                />
              </div>
            </div>

            <div class="field">
              <label class="form-label">Hành động</label>
              <select v-model="filters.actionType" class="form-select">
                <option value="">Tất cả</option>
                <option v-for="a in actionOptions" :key="a" :value="a">{{ actionLabel(a) }}</option>
              </select>
            </div>

            <div class="field">
              <label class="form-label">Bảng dữ liệu</label>
              <select v-model="filters.tableName" class="form-select">
                <option value="">Tất cả</option>
                <option v-for="t in tableOptions" :key="t" :value="t">{{ tableLabel(t) }}</option>
              </select>
            </div>

            <div class="field field-dates">
              <div class="d-flex justify-content-between align-items-center mb-1">
                <label class="form-label mb-0">Thời gian</label>
                <div class="quick-dates">
                  <button @click="setPeriod('today')" :class="{ active: activePeriod === 'today' }" class="btn-quick-date">Hôm nay</button>
                  <button @click="setPeriod('7d')" :class="{ active: activePeriod === '7d' }" class="btn-quick-date">7 Ngày</button>
                  <button @click="setPeriod('month')" :class="{ active: activePeriod === 'month' }" class="btn-quick-date">Tháng này</button>
                  <button @click="setPeriod('lastMonth')" :class="{ active: activePeriod === 'lastMonth' }" class="btn-quick-date">Tháng trước</button>
                </div>
              </div>
              <div class="d-flex gap-2">
                <input v-model="filters.fromDate" type="date" class="form-control" title="Từ ngày" />
                <input v-model="filters.toDate" type="date" class="form-control" title="Đến ngày" />
                <button class="btn-soft" @click="resetFilters" title="Đặt lại bộ lọc">
                  <i class="bi bi-arrow-counterclockwise"></i>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Bảng hiển thị -->
        <div class="table-wrapper">
          <table class="financial-table">
            <thead>
              <tr>
                <th style="width: 56px">STT</th>
                <th style="width: 150px">HÀNH ĐỘNG</th>
                <th style="width: 180px">BẢNG DỮ LIỆU</th>
                <th style="width: 200px">NGƯỜI THỰC HIỆN</th>
                <th style="width: 160px">THỜI GIAN</th>
                <th style="width: 70px" class="text-center">THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="6" class="text-center py-4">
                  <i class="bi bi-arrow-repeat spin"></i> Đang tải dữ liệu...
                </td>
              </tr>
              <tr v-else-if="paged.length === 0">
                <td colspan="6" class="text-center py-4">
                  <i class="bi bi-inbox" style="font-size: 1.6rem; color: #9db8de"></i>
                  <div class="mt-2" style="color: #6b7280">Không có log nào phù hợp.</div>
                </td>
              </tr>
              <tr v-else v-for="(log) in paged" :key="log.logId">
                <td class="td-id">#{{ log.logId }}</td>
                <td>
                  <span class="badge" :class="getActionClass(log.actionType)">
                    {{ actionLabel(log.actionType) }}
                  </span>
                </td>
                <td class="fw-bold">{{ tableLabel(log.tableName) }}</td>
                <td>
                  <template v-if="log.userId">
                    <div class="user-name">{{ log.fullName || log.username }}</div>
                    <div class="user-sub">@{{ log.username }}</div>
                  </template>
                  <span v-else class="text-muted fst-italic">Hệ thống</span>
                </td>
                <td>{{ formatDate(log.createdAt) }}</td>
                <td class="text-center">
                  <button class="btn-icon" @click="openDetail(log.logId)" title="Xem chi tiết">
                    <i class="bi bi-eye"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Phân trang -->
        <div class="fin-pagination">
          <div class="pagination-summary">
            Tổng <strong>{{ filteredLogs.length }}</strong> log
          </div>
          <div class="pagination-controls">
            <div class="page-size-group">
              <span class="page-size-label">Hiển thị</span>
              <select v-model.number="pageSize" class="form-select page-size-select" @change="page = 1">
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
                <option :value="100">100</option>
              </select>
              <span class="page-size-label page-size-suffix">/ trang</span>
            </div>
            <nav class="pager" aria-label="Phân trang">
              <button class="pager-arrow" :disabled="page === 1" @click="page = 1">
                <i class="bi bi-chevron-bar-left"></i>
              </button>
              <button class="pager-arrow" :disabled="page === 1" @click="page--">
                <i class="bi bi-chevron-left"></i>
              </button>
              <ul class="pager-list">
                <li v-for="(p, i) in pageItems" :key="i">
                  <span v-if="p === '...'" class="pager-ellipsis">…</span>
                  <button v-else class="pager-num" :class="{ active: p === page }" @click="page = p">
                    {{ p }}
                  </button>
                </li>
              </ul>
              <button class="pager-arrow" :disabled="page === totalPages" @click="page++">
                <i class="bi bi-chevron-right"></i>
              </button>
              <button class="pager-arrow" :disabled="page === totalPages" @click="page = totalPages">
                <i class="bi bi-chevron-bar-right"></i>
              </button>
            </nav>
          </div>
        </div>
      </div>
    </template>

    <!-- Modal Chi tiết -->
    <div v-if="detailLog" class="modal-overlay" @click.self="detailLog = null">
      <div class="modal-content">
        <div class="modal-header">
          <div class="modal-header-title">
            <span class="modal-header-icon"><i class="bi bi-file-earmark-text"></i></span>
            <h5 class="mb-0">Chi tiết thao tác #{{ detailLog.logId }}</h5>
          </div>
          <button class="btn-close-modal" @click="detailLog = null" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">

          <!-- Hành động + Bảng -->
          <div class="detail-section">
            <div class="detail-section-row">
              <div class="detail-item">
                <span class="detail-label">Hành động</span>
                <span :class="['badge', 'badge-lg', getActionClass(detailLog.actionType)]">
                  {{ actionLabel(detailLog.actionType) }}
                </span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Bảng dữ liệu</span>
                <strong class="detail-value">{{ tableLabel(detailLog.tableName) }}</strong>
              </div>
            </div>
          </div>

          <!-- Nội dung thao tác -->
          <div class="desc-section">
            <div class="desc-section-header">
              <i class="bi bi-pencil-square"></i> Nội dung thao tác
            </div>
            <p class="desc-text">{{ translateDescription(detailLog.description) }}</p>
          </div>

          <!-- Người thực hiện -->
          <div class="person-section">
            <div class="person-section-header">
              <i class="bi bi-person-fill-check"></i> Người thực hiện
            </div>

            <div v-if="!detailLog.userId" class="guest-note">
              <i class="bi bi-gear-wide-connected"></i>
              Thao tác thực hiện bởi Hệ thống tự động
            </div>

            <div v-else class="person-grid">
              <div class="person-item">
                <span class="detail-label">Họ tên</span>
                <strong class="detail-value">{{ detailLog.fullName || '---' }}</strong>
              </div>
              <div class="person-item">
                <span class="detail-label">Tài khoản</span>
                <span class="mono-tag">@{{ detailLog.username || '---' }}</span>
              </div>
              <div class="person-item person-item-full">
                <span class="detail-label">Địa chỉ IP</span>
                <span class="ip-tag">
                  <i class="bi bi-hdd-network"></i>
                  {{ detailLog.ipAddress || '---' }}
                </span>
              </div>
            </div>
          </div>

          <!-- Footer thời gian -->
          <div class="modal-footer-note">
            <i class="bi bi-clock-history"></i>
            Thực hiện lúc: <strong>{{ formatDate(detailLog.createdAt) }}</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast.show" class="toast-alert" :class="{ error: toast.type === 'error' }">
        <i class="bi" :class="toast.type === 'error' ? 'bi-x-circle-fill' : 'bi-check-circle-fill'"></i>
        <div>
          <strong>{{ toast.title }}</strong>
          <span>{{ toast.message }}</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch, onMounted } from 'vue';
import { auditLogApi } from '@/api/AuditLogApi';
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue';

const logs = ref([]);
const loading = ref(true);
const loadError = ref('');
const exporting = ref(false);
const detailLog = ref(null);

// Toast
const toast = reactive({ show: false, type: 'success', title: '', message: '' });
let toastTimer = null;
const showToast = (type, title, message) => {
  toast.show = true; toast.type = type; toast.title = title; toast.message = message;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (toast.show = false), 3000);
};

const filters = reactive({ search: '', actionType: '', tableName: '', fromDate: '', toDate: '' });
const activePeriod = ref(null);

// ---- Label Maps ----
const STANDARD_ACTIONS = [
  'CREATE', 'UPDATE', 'DELETE', 'REVIEW_REPLIED',
  'BACKUP_REQUESTED', 'BACKUP_COMPLETED', 'BACKUP_FAILED',
  'BACKUP_DOWNLOADED', 'BACKUP_DELETED', 'BACKUP_RESTORE_TESTED'
];

const actionOptions = computed(() => {
  const found = new Set(logs.value.map((l) => l.actionType).filter(Boolean));
  const extra = [...found].filter((a) => !STANDARD_ACTIONS.includes(a)).sort();
  return [...STANDARD_ACTIONS, ...extra];
});

const tableOptions = computed(() =>
  [...new Set(logs.value.map((l) => l.tableName).filter(Boolean))].sort()
);

const ACTION_LABELS = { 
  CREATE: 'Tạo mới', 
  UPDATE: 'Cập nhật', 
  DELETE: 'Xoá',
  REVIEW_REPLIED: 'Đã phản hồi',
  BACKUP_REQUESTED: 'Yêu cầu sao lưu',
  BACKUP_COMPLETED: 'Sao lưu hoàn tất',
  BACKUP_FAILED: 'Sao lưu thất bại',
  BACKUP_DOWNLOADED: 'Đã tải bản sao lưu',
  BACKUP_DELETED: 'Đã xóa bản sao lưu',
  BACKUP_RESTORE_TESTED: 'Đã kiểm tra khả năng phục hồi'
};

function actionLabel(a) { return a ? (ACTION_LABELS[a.toUpperCase()] || a) : '—'; }

function getActionClass(a) {
  const v = (a || '').toUpperCase();
  if (v === 'REVIEW_REPLIED') return 'bg-info';
  if (v === 'CREATE' || v === 'BACKUP_COMPLETED' || v === 'BACKUP_RESTORE_TESTED') return 'bg-success';
  if (v === 'UPDATE' || v === 'BACKUP_REQUESTED' || v === 'BACKUP_DOWNLOADED') return 'bg-warning';
  if (v === 'DELETE' || v === 'BACKUP_FAILED' || v === 'BACKUP_DELETED') return 'bg-danger';
  return 'bg-secondary';
}

const TABLE_LABELS = {
  Users: 'Người dùng', Roles: 'Vai trò', Permissions: 'Quyền hạn',
  User_Permissions: 'Phân quyền nhân viên', Categories: 'Danh mục sản phẩm',
  Products: 'Sản phẩm', Product_Images: 'Ảnh sản phẩm',
  Product_Skus: 'Kho không IMEI (SKU)', Product_Items: 'Kho có IMEI (Số IMEI)',
  Attributes: 'Thuộc tính', Attribute_Values: 'Giá trị thuộc tính',
  Vouchers: 'Voucher', User_Vouchers: 'Voucher người dùng',
  Flash_Sale_Slots: 'Khung giờ Flash Sale', Flash_Sale_Items: 'Sản phẩm Flash Sale',
  Orders: 'Đơn hàng', Order_Items: 'Chi tiết đơn hàng',
  Order_Transactions: 'Giao dịch đơn hàng', Post_Categories: 'Danh mục bài viết',
  Posts: 'Bài viết', Banners: 'Banner', Contact_Requests: 'Yêu cầu liên hệ',
  Product_Reviews: 'Đánh giá & Bình luận',
  System_Settings: 'Cấu hình hệ thống', Shipping_Config: 'Cấu hình vận chuyển',
  Admin_Notifications: 'Thông báo admin', User_Addresses: 'Địa chỉ người dùng',
  Audit_Logs: 'Nhật ký hoạt động', Inventory: 'Quản lý kho hàng', InventoryService: 'Quản lý kho hàng',
  'SYSTEM BACKUP': 'Sao lưu hệ thống', 'SYSTEM_BACKUP': 'Sao lưu hệ thống'
};

function tableLabel(name) { return name ? (TABLE_LABELS[name] || name.replace(/_/g, ' ')) : '—'; }

const SKIP_TRANSLATE_KEYS = new Set(['Product_Skus', 'Product_Items']);
const TABLE_KEYS_SORTED = Object.keys(TABLE_LABELS)
  .filter((k) => !SKIP_TRANSLATE_KEYS.has(k))
  .sort((a, b) => b.length - a.length);

function translateDescription(desc) {
  if (!desc) return 'Không có mô tả';

  let result = desc;

  if (result.includes('BACKUP_COMPLETED')) {
    const fileName = result.match(/MarcusStore-[^\s()]+/)?.[0] || 'file sao lưu';
    return `Đã hoàn tất sao lưu dữ liệu hệ thống (Tên file: ${fileName})`;
  }
  if (result.includes('BACKUP_REQUESTED')) {
    return 'Đã gửi yêu cầu khởi tạo bản sao lưu dữ liệu hệ thống';
  }
  if (result.includes('BACKUP_FAILED')) {
    return 'Quá trình sao lưu dữ liệu hệ thống thất bại';
  }
  if (result.includes('BACKUP_DOWNLOADED')) {
    return 'Đã tải xuống bản sao lưu dữ liệu hệ thống';
  }
  if (result.includes('BACKUP_DELETED')) {
    return 'Đã xóa tập tin sao lưu dữ liệu khỏi hệ thống';
  }
  if (result.includes('BACKUP_RESTORE_TESTED')) {
    return 'Đã hoàn tất kiểm tra thử nghiệm khả năng phục hồi dữ liệu từ bản sao lưu';
  }

  result = result.replace(/đã tạo mới/g, 'đã thêm mới');
  result = result.replace(/đã cập nhật/g, 'đã chỉnh sửa');
  result = result.replace(/đã xoá/g, 'đã xóa');

  TABLE_KEYS_SORTED.forEach((key) => {
    result = result.replace(new RegExp(`\\b${key}\\b`, 'g'), TABLE_LABELS[key]);
  });

  return result;
}

function parseVnDateTime(str) {
  if (!str) return null;
  const [datePart, timePart] = str.split(' ');
  const [d, m, y] = datePart.split('/').map(Number);
  const [hh, mm, ss] = (timePart || '00:00:00').split(':').map(Number);
  return new Date(y, m - 1, d, hh || 0, mm || 0, ss || 0);
}
function toDateOnly(d) { return new Date(d.getFullYear(), d.getMonth(), d.getDate()); }
function fmtInputDate(d) {
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
function formatDate(str) {
  if (!str) return '';
  const d = parseVnDateTime(str);
  return d ? d.toLocaleString('vi-VN') : str;
}

function setPeriod(period) {
  activePeriod.value = period;
  const today = new Date();
  if (period === 'today') {
    filters.fromDate = fmtInputDate(today); filters.toDate = fmtInputDate(today);
  } else if (period === '7d') {
    const from = new Date(today); from.setDate(from.getDate() - 6);
    filters.fromDate = fmtInputDate(from); filters.toDate = fmtInputDate(today);
  } else if (period === 'month') {
    filters.fromDate = fmtInputDate(new Date(today.getFullYear(), today.getMonth(), 1));
    filters.toDate = fmtInputDate(today);
  } else if (period === 'lastMonth') {
    filters.fromDate = fmtInputDate(new Date(today.getFullYear(), today.getMonth() - 1, 1));
    filters.toDate = fmtInputDate(new Date(today.getFullYear(), today.getMonth(), 0));
  }
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
  if (filters.fromDate) {
    const from = new Date(filters.fromDate + 'T00:00:00');
    list = list.filter((l) => { const d = parseVnDateTime(l.createdAt); return d && d >= from; });
  }
  if (filters.toDate) {
    const to = new Date(filters.toDate + 'T23:59:59');
    list = list.filter((l) => { const d = parseVnDateTime(l.createdAt); return d && d <= to; });
  }
  return list;
});

const chartDays = computed(() => {
  const byDate = new Map();
  filteredLogs.value.forEach((l) => {
    const d = parseVnDateTime(l.createdAt);
    if (!d) return;
    const key = fmtInputDate(toDateOnly(d));
    if (!byDate.has(key)) byDate.set(key, { create: 0, update: 0, delete: 0 });
    const bucket = byDate.get(key);
    const a = (l.actionType || '').toUpperCase();
    if (a === 'CREATE') bucket.create++;
    else if (a === 'UPDATE') bucket.update++;
    else if (a === 'DELETE') bucket.delete++;
  });
  return [...byDate.entries()]
    .sort((a, b) => a[0].localeCompare(b[0]))
    .slice(-14)
    .map(([key, v]) => {
      const [y, m, d] = key.split('-');
      return { date: `${d}/${m}`, create: v.create, update: v.update, delete: v.delete, total: v.create + v.update + v.delete };
    });
});
const chartMax = computed(() => Math.max(1, ...chartDays.value.map((d) => d.total)));
function pct(value, max) { return max ? `${(value / max) * 100}%` : '0%'; }

const page = ref(1);
const pageSize = ref(10);
watch(filteredLogs, () => { page.value = 1; });
const totalPages = computed(() => Math.max(1, Math.ceil(filteredLogs.value.length / pageSize.value)));
const paged = computed(() => filteredLogs.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value));
const pageItems = computed(() => {
  const total = totalPages.value, current = page.value, delta = 1;
  const items = [], range = [];
  for (let i = Math.max(2, current - delta); i <= Math.min(total - 1, current + delta); i++) range.push(i);
  items.push(1);
  if (range.length && range[0] > 2) items.push('...');
  items.push(...range);
  if (range.length && range[range.length - 1] < total - 1) items.push('...');
  if (total > 1) items.push(total);
  return items;
});

function resetFilters() {
  filters.search = ''; filters.actionType = ''; filters.tableName = '';
  filters.fromDate = ''; filters.toDate = ''; activePeriod.value = null;
}

async function loadAll() {
  loading.value = true; loadError.value = '';
  try { logs.value = await auditLogApi.getAll(); }
  catch { loadError.value = 'Không tải được nhật ký hoạt động. Vui lòng thử lại.'; }
  finally { loading.value = false; }
}
onMounted(loadAll);

async function openDetail(id) {
  try { detailLog.value = await auditLogApi.getOne(id); }
  catch { showToast('error', 'Lỗi', 'Không tải được chi tiết log.'); }
}

async function handleExport() {
  exporting.value = true;
  try {
    if (filteredLogs.value.length === 0) { showToast('error', 'Lỗi', 'Không có dữ liệu để xuất!'); return; }
    const blob = await auditLogApi.exportCsv();
    const url = window.URL.createObjectURL(new Blob([blob]));
    const link = document.createElement('a');
    link.href = url; link.setAttribute('download', `AuditLog_${new Date().getTime()}.csv`);
    document.body.appendChild(link); link.click(); link.remove();
    window.URL.revokeObjectURL(url);
    showToast('success', 'Thành công', 'Xuất log hoàn tất!');
  } catch { showToast('error', 'Lỗi', 'Xuất file CSV thất bại.'); }
  finally { exporting.value = false; }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@300;400;500;600;700&display=swap');

.al-page {
  min-height: 100%;
  padding: 28px;
  background: #eef3fb;
  font-family: 'Be Vietnam Pro', sans-serif;
}

.al-page-header { margin-bottom: 22px; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.dynamic-total {
  padding: 10px 16px;
  border: 1px solid #d3e5f8;
  border-radius: 9px;
  color: #36516f;
  background: #eef6ff;
  font-size: 0.9rem;
}
.btn-export-excel {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  border: 1px solid #c5d9f1; border-radius: 10px;
  background: #fff; color: #0b3d91; font-weight: 700; padding: 12px 20px; font-size: 0.92rem;
  box-shadow: 0 4px 12px rgba(21,89,165,0.08); transition: all 0.18s ease; cursor: pointer;
}
.btn-export-excel:hover:not(:disabled) { background: #f0f6ff; transform: translateY(-1px); }
.btn-export-excel:disabled { opacity: 0.7; cursor: not-allowed; }

/* State */
.state-box {
  background: #fff; border-radius: 12px; padding: 48px;
  text-align: center; color: #9ca3af; font-size: 14px;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
}
.state-error { color: #dc2626; }
.btn-retry { margin-top: 8px; background: #0b3d91; color: #fff; border: none; border-radius: 8px; padding: 8px 20px; font-size: 13px; cursor: pointer; font-weight: 600; }

/* Stats */
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 22px; }
@media (max-width: 992px) { .stats-grid { grid-template-columns: repeat(2,1fr); } }
@media (max-width: 576px) { .stats-grid { grid-template-columns: 1fr; } }
.stat-card {
  display: flex; align-items: center; gap: 14px;
  background: #fff; border: 1px solid #dce8f9; border-radius: 14px; padding: 18px 20px;
  box-shadow: 0 4px 14px -8px rgba(28,100,214,0.18); transition: all 0.16s ease;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px -8px rgba(28,100,214,0.3); }
.stat-icon { flex-shrink: 0; width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; color: #fff; }
.stat-icon-blue  { background: linear-gradient(135deg, #2f80ed, #1c64d6); }
.stat-icon-green { background: linear-gradient(135deg, #34c77b, #1f9d5e); }
.stat-icon-amber { background: linear-gradient(135deg, #ffb547, #f29c1f); }
.stat-icon-red   { background: linear-gradient(135deg, #f5625a, #e0445c); }
.stat-body { display: flex; flex-direction: column; gap: 4px; }
.stat-body span { font-size: 0.8rem; color: #6b7c93; font-weight: 600; }
.stat-body strong { font-size: 1.4rem; font-weight: 800; color: #0f2c5c; }
.fin-accent { color: #1c64d6; }

/* Chart */
.chart-panel { background: #fff; border: 1px solid #dce8f9; border-radius: 14px; padding: 18px; box-shadow: 0 4px 14px -10px rgba(28,100,214,0.16); margin-bottom: 22px; }
.chart-header { display: flex; align-items: baseline; gap: 8px; font-weight: 700; color: #1f3a63; margin-bottom: 10px; font-size: 0.95rem; }
.chart-subnote { font-weight: 500; font-size: 0.78rem; color: #8ba0c2; }
.chart-bars { display: flex; align-items: flex-end; gap: 10px; height: 160px; overflow-x: auto; padding-top: 6px; }
.chart-col { display: flex; flex-direction: column; align-items: center; flex-shrink: 0; width: 34px; }
.chart-bar { width: 22px; height: 130px; display: flex; flex-direction: column-reverse; justify-content: flex-start; border-radius: 4px; overflow: hidden; background: #f3f4f6; }
.seg { width: 100%; }
.seg-green { background: #1f9d5e; }
.seg-amber { background: #f29c1f; }
.seg-red   { background: #e0445c; }
.chart-label { font-size: 10px; color: #9ca3af; margin-top: 6px; white-space: nowrap; }

/* Table panel */
.table-panel { background: #fff; border: 1px solid #dce8f9; border-radius: 14px; overflow: hidden; box-shadow: 0 6px 20px -12px rgba(28,100,214,0.22); }
.toolbar-panel { padding: 20px 22px; border-bottom: 1px solid #e9f1fb; }
.toolbar-row { display: grid; grid-template-columns: 2fr 1fr 1fr 2fr; gap: 20px; align-items: end; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-keyword { grid-column: 1/3; }
.field-dates { grid-column: 3/5; gap: 8px; }
.form-label { font-size: 0.78rem; font-weight: 700; color: #2f6fc4; text-transform: uppercase; margin: 0; }
.form-control, .form-select {
  height: 42px; padding: 0 12px; border: 1px solid #d6e6fb; border-radius: 9px;
  background-color: #f7fbff; font-size: 0.88rem; outline: none; transition: all 0.15s ease;
}
.form-control:focus, .form-select:focus { border-color: #2f80ed; background-color: #fff; box-shadow: 0 0 0 0.18rem rgba(47,128,237,0.16); }
.input-group { display: flex; align-items: stretch; border: 1px solid #d6e6fb; border-radius: 9px; background-color: #f7fbff; }
.input-group-text { display: flex; align-items: center; padding: 0 12px; color: #5d8fd9; border-right: 1px solid #d6e6fb; }
.input-group .form-control { width: 100%; border: none; border-radius: 0; background: transparent; }
.quick-dates { display: flex; gap: 6px; }
.btn-quick-date { font-size: 0.75rem; padding: 4px 10px; border-radius: 6px; border: 1px solid #d6e6fb; background: #fff; color: #2f80ed; cursor: pointer; transition: all 0.15s ease; }
.btn-quick-date:hover, .btn-quick-date.active { background: #2f80ed; color: #fff; }
.btn-soft { display: inline-flex; align-items: center; justify-content: center; height: 42px; padding: 0 14px; border: 1px solid #d6e6fb; border-radius: 9px; background: #f7fbff; color: #1c64d6; cursor: pointer; transition: all 0.15s ease; }
.btn-soft:hover { background: #e3effd; transform: rotate(-25deg); }
.d-flex { display: flex; }
.gap-2 { gap: 8px; }
.gap-3 { gap: 12px; }
.align-items-center { align-items: center; }
.justify-content-between { justify-content: space-between; }
.mb-1 { margin-bottom: 4px; }
.mb-0 { margin-bottom: 0; }

/* Table */
.table-wrapper { overflow-x: auto; }
.financial-table { width: 100%; border-collapse: collapse; }
.financial-table th, .financial-table td { padding: 14px 18px; text-align: left; border-bottom: 1px solid #e9f1fb; font-size: 0.9rem; color: #1f3a63; white-space: nowrap; }
.financial-table thead th { background: linear-gradient(180deg, #eef6ff, #e3effd); color: #1c64d6; font-size: 0.76rem; font-weight: 800; text-transform: uppercase; border-bottom: 1px solid #d6e6fb; }
.financial-table tbody tr { transition: background-color 0.12s ease; }
.financial-table tbody tr:hover { background: #f5faff; }
.badge { display: inline-block; padding: 5px 13px; border-radius: 20px; color: white; font-size: 0.76rem; font-weight: 700; letter-spacing: 0.01em; }
.bg-info    { background-color: #0284c7; }
.bg-success { background-color: #1f9d5e; }
.bg-warning { background-color: #f29c1f; }
.bg-danger  { background-color: #e0445c; }
.bg-secondary { background-color: #6b7c93; }
.td-id { color: #9ca3af; font-size: 11px; font-weight: 600; }
.user-name { font-weight: 600; color: #111827; }
.user-sub { font-size: 11px; color: #9ca3af; }
.text-muted { color: #6b7280; }
.fst-italic { font-style: italic; }
.text-center { text-align: center; }
.fw-bold { font-weight: 700; }
.btn-icon { background: none; border: none; color: #2f80ed; font-size: 1.1rem; cursor: pointer; padding: 4px; border-radius: 6px; transition: background-color 0.15s ease; }
.btn-icon:hover { background: #eef6ff; }
.py-4 { padding: 24px 0; }
.mt-2 { margin-top: 8px; }

/* Pagination */
.fin-pagination { display: flex; flex-wrap: nowrap; align-items: center; justify-content: space-between; gap: 16px; padding: 20px 24px; border-top: 1px solid #e9f1fb; background: #fff; overflow-x: auto; }
.pagination-summary { flex-shrink: 0; color: #6b7c93; font-size: 0.9rem; white-space: nowrap; }
.pagination-controls { display: flex; flex-shrink: 0; align-items: center; gap: 28px; }
.page-size-group { display: flex; flex-shrink: 0; align-items: center; gap: 8px; white-space: nowrap; }
.page-size-label { white-space: nowrap; font-size: 0.9rem; }
.page-size-select { width: 70px; }
.pager { display: flex; flex-shrink: 0; gap: 6px; padding: 4px; background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; }
.pager-arrow, .pager-num { width: 34px; height: 34px; border: none; border-radius: 9px; background: transparent; color: #4a5d80; font-weight: 700; cursor: pointer; transition: all 0.15s ease; }
.pager-arrow:hover:not(:disabled), .pager-num:hover { background: #eef6ff; color: #1c64d6; }
.pager-num.active { background: #0b3d91; color: #fff; }
.pager-list { display: flex; list-style: none; margin: 0; padding: 0; }
.pager-ellipsis { display: flex; align-items: center; justify-content: center; color: #9ca3af; }

/* Modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15,44,92,0.45); display: flex; align-items: center; justify-content: center; z-index: 1050; backdrop-filter: blur(3px); padding: 20px; }
.modal-content { background: #fff; width: 520px; max-width: 100%; max-height: calc(100vh - 40px); display: flex; flex-direction: column; border-radius: 16px; box-shadow: 0 24px 60px rgba(11,61,145,0.28); overflow: hidden; animation: modalPop 0.18s ease-out; }
.modal-header { flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 18px 22px; background: linear-gradient(120deg, #0b3d91 0%, #1c64d6 65%, #2f80ed 100%); }
.modal-header-title { display: flex; align-items: center; gap: 12px; }
.modal-header-title h5 { font-size: 1.02rem; font-weight: 800; color: #fff; letter-spacing: -0.01em; margin: 0; }
.modal-header-icon { flex-shrink: 0; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border-radius: 10px; background: rgba(255,255,255,0.16); border: 1px solid rgba(255,255,255,0.28); font-size: 1.05rem; color: #fff; }
.btn-close-modal { flex-shrink: 0; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.14); border: none; border-radius: 9px; font-size: 1rem; cursor: pointer; color: #fff; transition: all 0.15s ease; }
.btn-close-modal:hover { background: rgba(224,68,92,0.85); transform: rotate(90deg); }
.modal-body { padding: 22px; overflow-y: auto; display: flex; flex-direction: column; gap: 14px; }

/* Modal sections */
.detail-section { background: #f7fbff; border: 1px solid #e3effd; border-radius: 12px; padding: 16px; }
.detail-section-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 6px; }
.detail-label { font-size: 0.72rem; color: #6b7c93; text-transform: uppercase; letter-spacing: 0.04em; font-weight: 700; }
.detail-value { font-size: 0.95rem; font-weight: 700; color: #0f2c5c; }
.badge-lg { padding: 7px 16px; font-size: 0.82rem; width: fit-content; }

/* Mô tả section */
.desc-section { background: #f7fbff; border: 1px solid #e3effd; border-radius: 12px; padding: 16px; }
.desc-section-header { display: flex; align-items: center; gap: 8px; font-size: 0.8rem; font-weight: 800; color: #1c64d6; text-transform: uppercase; letter-spacing: 0.03em; margin-bottom: 10px; }
.desc-text { margin: 0; color: #1f3a63; font-size: 0.92rem; line-height: 1.65; font-weight: 500; word-break: break-word; white-space: pre-wrap; }

/* Người thực hiện section */
.person-section { background: #fff; border: 1px dashed #c5d9f6; border-radius: 12px; padding: 16px; }
.person-section-header { display: flex; align-items: center; gap: 8px; font-size: 0.8rem; font-weight: 800; color: #1c64d6; text-transform: uppercase; letter-spacing: 0.03em; margin-bottom: 14px; }
.person-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.person-item { display: flex; flex-direction: column; gap: 5px; }
.person-item-full { grid-column: 1 / -1; }
.mono-tag { display: inline-block; font-family: 'JetBrains Mono', monospace; font-size: 0.85rem; color: #1c64d6; background: #eef6ff; padding: 4px 10px; border-radius: 6px; font-weight: 600; width: fit-content; }
.ip-tag { display: inline-flex; align-items: center; gap: 7px; font-family: 'JetBrains Mono', monospace; font-size: 0.85rem; color: #4a5d80; background: #f1f5fb; padding: 6px 12px; border-radius: 8px; border: 1px solid #dce8f9; font-weight: 600; width: fit-content; }

.guest-note {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f7fbff;
  border: 1px solid #e3effd;
  border-radius: 10px;
  color: #6b7c93;
  font-size: 0.88rem;
  font-style: italic;
}
.guest-note i { font-size: 1rem; color: #9db8de; flex-shrink: 0; }
.modal-footer-note { display: flex; align-items: center; justify-content: flex-end; gap: 6px; color: #9aa8bf; font-size: 0.78rem; padding-top: 4px; }
.modal-footer-note strong { color: #4a5d80; }

/* Toast */
.toast-alert { position: fixed; top: 80px; right: 24px; z-index: 1100; display: flex; align-items: center; gap: 12px; padding: 16px 20px; border-radius: 12px; background: #fff; border-left: 4px solid #1f9d5e; box-shadow: 0 14px 32px rgba(15,64,152,0.2); }
.toast-alert div { display: flex; flex-direction: column; gap: 2px; }
.toast-alert strong { font-size: 13px; color: #111827; }
.toast-alert span { font-size: 12px; color: #6b7280; }
.toast-alert.error { border-left-color: #e0445c; }
.toast-alert i { font-size: 1.2rem; color: #1f9d5e; flex-shrink: 0; }
.toast-alert.error i { color: #e0445c; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* Animations */
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.spin { animation: spin 1s linear infinite; display: inline-block; }
@keyframes modalPop { from { transform: translateY(-12px) scale(0.98); opacity: 0; } to { transform: translateY(0) scale(1); opacity: 1; } }

@media (max-width: 992px) {
  .toolbar-row { grid-template-columns: 1fr 1fr; }
  .field-keyword { grid-column: 1/-1; }
  .field-dates { grid-column: 1/-1; }
}
@media (max-width: 576px) {
  .header-actions { width: 100%; flex-direction: column; }
  .dynamic-total, .btn-export-excel { width: 100%; text-align: center; }
  .toolbar-row { grid-template-columns: 1fr; }
  .detail-section-row, .person-grid { grid-template-columns: 1fr; }
  .person-item-full { grid-column: 1; }
}
</style>