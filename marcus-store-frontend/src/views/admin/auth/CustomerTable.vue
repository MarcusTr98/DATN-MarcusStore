<template>
  <div class="table-card">
    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Khách hàng</th>
          <th>Liên hệ</th>
          <th>Hạng thành viên</th>
          <th>Tổng chi tiêu</th>
          <th>Ngày tạo</th>
          <th>Email Verify</th>
          <th>Trạng thái</th>
          <th v-if="canManage" class="action-col">Thao tác</th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="!users.length">
          <td :colspan="canManage ? 9 : 8" class="empty-state">
            <i class="bi bi-person-x"></i>
            <h3>Không có khách hàng nào</h3>
            <p>Hãy thay đổi từ khóa tìm kiếm.</p>
          </td>
        </tr>

        <tr v-for="item in users" :key="item.userId">
          <td class="user-id">#{{ item.userId }}</td>

          <td>
            <div class="user-info">
              <div class="user-name">{{ item.fullName }}</div>
              <div v-if="item.username" class="user-username">{{ item.username }}</div>
            </div>
          </td>

          <td>
            <div class="contact-info">
              <div class="contact-email">{{ item.email }}</div>
              <div class="contact-phone">{{ item.phoneNumber }}</div>
            </div>
          </td>

          <!-- Hạng thành viên -->
          <td>
            <span class="tier-badge" :class="getTier(item.totalSpent).cls">
              {{ getTier(item.totalSpent).icon }} {{ getTier(item.totalSpent).label }}
            </span>
          </td>

          <!-- Tổng chi tiêu -->
          <td class="spent-cell">
            {{ formatVND(item.totalSpent) }}
          </td>

          <!-- Ngày tạo -->
          <td>
            {{ item.createdAt ? new Date(item.createdAt).toLocaleDateString('vi-VN') : '—' }}
          </td>

          <!-- Email Verify -->
          <td>
            <span class="status-badge" :class="item.emailVerified ? 'active' : 'warning'">
              {{ item.emailVerified ? 'Đã xác thực' : 'Chưa xác thực' }}
            </span>
          </td>

          <!-- Trạng thái -->
          <td>
            <span class="status-badge" :class="item.active ? 'active' : 'locked'">
              {{ item.active ? 'Hoạt động' : 'Đã khóa' }}
            </span>
          </td>

          <!-- Thao tác -->
          <td v-if="canManage" class="action-cell">
            <div class="action-group">
              <button class="btn-action btn-edit" title="Chỉnh sửa" @click="$emit('edit', item)">
                <i class="bi bi-pencil-square"></i>
              </button>

              <button
                v-if="!item.emailVerified"
                class="btn-action btn-verify"
                title="Gửi email xác thực"
                @click="$emit('send-verify', item.userId)"
              >
                <i class="bi bi-envelope-check"></i>
              </button>

              <button
                v-if="item.active"
                class="btn-action btn-lock"
                title="Khóa tài khoản"
                @click="$emit('lock', item.userId)"
              >
                <i class="bi bi-lock"></i>
              </button>

              <button
                v-else
                class="btn-action btn-unlock"
                title="Mở khóa tài khoản"
                @click="$emit('unlock', item.userId)"
              >
                <i class="bi bi-unlock"></i>
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- ===== Pagination ===== -->
    <div v-if="pagination.totalPages > 0" class="pagination-bar">
      <div class="pagination-total">
        Tổng <strong>{{ pagination.totalElements }}</strong> khách hàng
      </div>

      <div class="pagination-actions">
        <label class="page-size-box">
          <span>Hiển thị</span>
          <select v-model.number="localPageSize" @change="onPageSizeChange">
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>

        <button
          type="button"
          class="page-btn"
          :disabled="currentPage === 0"
          @click="$emit('page-change', currentPage - 1)"
        >
          Trước
        </button>

        <span class="page-current">
          Trang {{ currentPage + 1 }} / {{ pagination.totalPages }}
        </span>

        <button
          type="button"
          class="page-btn"
          :disabled="currentPage + 1 >= pagination.totalPages"
          @click="$emit('page-change', currentPage + 1)"
        >
          Sau
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  users: {
    type: Array,
    default: () => [],
  },
  canManage: {
    type: Boolean,
    default: false,
  },
  pagination: {
    type: Object,
    default: () => ({ totalElements: 0, totalPages: 0 }),
  },
  currentPage: {
    type: Number,
    default: 0,
  },
  pageSize: {
    type: Number,
    default: 5,
  },
})

const emit = defineEmits([
  'edit',
  'lock',
  'unlock',
  'send-verify',
  'page-change',
  'page-size-change',
])

const localPageSize = ref(props.pageSize)

watch(
  () => props.pageSize,
  (val) => {
    localPageSize.value = val
  },
)

function onPageSizeChange() {
  emit('page-size-change', localPageSize.value)
}

// ── Hạng thành viên ────────────────────────────────────
function getTier(totalSpent) {
  const amount = Number(totalSpent) || 0
  if (amount >= 10_000_000) return { label: 'Kim Cương', icon: '💎', cls: 'diamond' }
  if (amount >= 2_000_000) return { label: 'Vàng', icon: '🥇', cls: 'gold' }
  if (amount >= 500_000) return { label: 'Bạc', icon: '🥈', cls: 'silver' }
  return { label: 'Đồng', icon: '🥉', cls: 'bronze' }
}

// ── Format tiền VNĐ ────────────────────────────────────
function formatVND(amount) {
  const num = Number(amount) || 0
  return num.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })
}
</script>

<style scoped>
.table-card {
  background: #ffffff;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.table {
  margin-bottom: 0;
}

.table thead th {
  background: #fff0f7;
  color: #b4557d;
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
  white-space: nowrap;
  border-bottom: 1px solid #f3d6e3 !important;
  padding: 14px 16px !important;
}

.table tbody td {
  padding: 14px 16px !important;
  vertical-align: middle;
  color: #4b5563;
  font-size: 0.9rem;
  border-bottom: 1px solid #f3d6e3;
}

.table tbody tr:last-child td {
  border-bottom: none;
}

.action-col,
.action-cell {
  width: 176px;
  min-width: 176px;
  text-align: center;
}

.user-id,
.user-name {
  color: #202636;
  font-weight: 800;
}

.user-info,
.contact-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.user-username,
.contact-phone {
  color: #7c8798;
  font-size: 0.8rem;
  font-weight: 600;
}

.contact-email {
  color: #202636;
  font-weight: 700;
}

.spent-cell {
  font-weight: 700;
  color: #d63384;
  white-space: nowrap;
}

/* ===== Hạng thành viên ===== */
.tier-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 26px;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.76rem;
  font-weight: 800;
  white-space: nowrap;
}

.tier-badge.bronze {
  background: #fdf3e7;
  color: #a0522d;
  border: 1px solid #f5cfa0;
}

.tier-badge.silver {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
}

.tier-badge.gold {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fcd34d;
}

.tier-badge.diamond {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #93c5fd;
}

/* ===== Status badges ===== */
.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.76rem;
  font-weight: 800;
  white-space: nowrap;
}

.status-badge.active {
  background: #dcfce7;
  color: #15803d;
}

.status-badge.locked {
  background: #ffe2e8;
  color: #c72250;
}

.status-badge.warning {
  background: #fff0d9;
  color: #9a5b00;
}

/* ===== Action buttons ===== */
.action-group {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
  min-width: 128px;
}

.btn-action {
  display: inline-grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  transition: 0.18s;
}

.btn-edit {
  color: #d63384;
}

.btn-edit:hover {
  background: #ffe4ef;
  border-color: #efbdd2;
}

.btn-verify {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #2563eb;
}

.btn-verify:hover {
  background: #dbeafe;
  border-color: #93c5fd;
}

.btn-lock {
  border-color: #f5c2c7;
  background: #fff5f6;
  color: #dc3545;
}

.btn-lock:hover {
  background: #f8d7da;
}

.btn-unlock {
  color: #15803d;
}

.btn-unlock:hover {
  background: #f0fdf4;
}

/* ===== Empty state ===== */
.empty-state {
  text-align: center;
  padding: 42px 16px !important;
}

.empty-state i {
  color: #f55d9b;
  font-size: 2.4rem;
  display: block;
  margin-bottom: 12px;
}

.empty-state h3 {
  margin: 0 0 4px;
  font-size: 1.1rem;
  font-weight: 800;
  color: #202636;
}

.empty-state p {
  margin: 0;
  color: #6b7280;
}

/* ===== Pagination ===== */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 20px;
  border-top: 1px solid #f3d6e3;
  background: #fffafc;
}

.pagination-total {
  color: #38445a;
  font-size: 14px;
  font-weight: 500;
}

.pagination-total strong {
  color: #0f1f3a;
  font-weight: 800;
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-size-box {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #5f6675;
  font-size: 14px;
  font-weight: 600;
}

.page-size-box select {
  min-width: 76px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #ffd0e0;
  border-radius: 6px;
  background: #fff;
  color: #0f1f3a;
  font-size: 14px;
  font-weight: 700;
  outline: none;
}

.page-size-box select:focus {
  border-color: #ff7aaa;
  box-shadow: 0 0 0 2px rgba(255, 77, 141, 0.12);
}

.page-btn {
  min-width: 72px;
  height: 36px;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #fff;
  color: #344057;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: #ff9abc;
  color: #ff4d8d;
  background: #fff3f8;
}

.page-btn:disabled {
  color: #9ca3af;
  background: #fafafa;
  cursor: not-allowed;
}

.page-current {
  min-width: 90px;
  text-align: center;
  color: #0f1f3a;
  font-size: 14px;
  font-weight: 800;
}

@media (max-width: 768px) {
  .pagination-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-actions {
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .page-current {
    order: -1;
    width: 100%;
  }
}
</style>
