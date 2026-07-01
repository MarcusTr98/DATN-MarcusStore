<template>
  <div class="user-page">

    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <i class="bi bi-people-fill"></i>
        </div>
        <div>
          <h2>Quản lý nhân viên</h2>
          <p>Danh sách Admin và Staff</p>
        </div>
      </div>

      <button v-if="canCreate" class="btn-pink" @click="openCreate">
        <i class="bi bi-plus-circle"></i>
        Thêm nhân viên
      </button>
    </div>

    <!-- Statistic -->
    <div class="row g-3 mb-4">
      <div class="col-md-3">
        <div class="stat-card">
          <span>Admin</span>
          <h3>{{ stats.totalAdmin }}</h3>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card">
          <span>Staff</span>
          <h3>{{ stats.totalStaff }}</h3>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card">
          <span>Hoạt động</span>
          <h3>{{ stats.active }}</h3>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card">
          <span>Đã khóa</span>
          <h3>{{ stats.locked }}</h3>
        </div>
      </div>
    </div>

    <!-- Search -->
    <div class="filter-card">
      <div class="row g-3 align-items-end">

        <div class="col-12 col-md-8">
          <label class="filter-label">Tìm kiếm</label>
          <div class="input-wrapper">
            <i class="bi bi-search search-icon"></i>
            <input
              class="form-control f-input"
              placeholder="Tìm theo tên, email hoặc số điện thoại"
              v-model="keyword"
            >
          </div>
        </div>

        <div class="col-12 col-md-4">
          <label class="filter-label">Vai trò</label>
          <select v-model="roleFilter" class="form-select f-input">
            <option value="ALL">Tất cả</option>
            <option value="ADMIN">Admin</option>
            <option value="STAFF">Staff</option>
          </select>
        </div>

      </div>
    </div>

    <EmployeeTable
      :users="users"
      :can-edit="canEdit"
      :can-lock="canLock"
      :can-unlock="canUnlock"
      :pagination="pagination"
      :current-page="currentPage"
      :page-size="pageSize"
      @lock="lockUser"
      @unlock="unlockUser"
      @edit="openEdit"
      @page-change="goToPage"
      @page-size-change="onPageSizeChange"
    />

    <UserFormModal
      :visible="isModalOpen"
      :is-edit="isEdit"
      :saving="saving"
      :initial-data="editForm"
      :allowed-roles="[
        { value: 'ADMIN', label: 'Admin' },
        { value: 'STAFF', label: 'Staff' }
      ]"
      @close="closeModal"
      @submit="saveUser"
    />

    <BaseModal
      :visible="modalVisible"
      :type="modalType"
      :title="modalTitle"
      :message="modalMessage"
      @close="modalVisible = false"
    />

  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import EmployeeTable from './EmployeeTable.vue'
import adminUserApi from '@/api/adminUserApi.js'
import UserFormModal from '@/components/UserFormModal.vue'
import BaseModal from '@/components/BaseModal.vue'

const ROLE_MAP = { ADMIN: 1, STAFF: 2 }

// ── Data ───────────────────────────────────────────────
const users      = ref([])
const keyword    = ref('')
const roleFilter = ref('ALL')

// ── Pagination ─────────────────────────────────────────
const currentPage = ref(0)
const pageSize    = ref(5)
const pageInfo    = ref({ totalElements: 0, totalPages: 0 })

// ── Stats (toàn bộ, không phụ thuộc trang) ────────────
const stats = ref({ totalAdmin: 0, totalStaff: 0, active: 0, locked: 0 })

// ── Modal form ─────────────────────────────────────────
const isModalOpen = ref(false)
const isEdit      = ref(false)
const saving      = ref(false)
const editForm    = ref({})

// ── Modal thông báo ────────────────────────────────────
const modalVisible = ref(false)
const modalType    = ref('success')
const modalTitle   = ref('')
const modalMessage = ref('')

// ── Helpers: check theo PERMISSION cụ thể, không hardcode role ──
// Trang này quản lý cả Admin lẫn Staff nên chỉ ADMIN mới thao tác được
// (khớp với route 'employee' trong router.js: meta.roles = ['ROLE_ADMIN'])
const isAdmin = computed(() => {
  const roles = JSON.parse(localStorage.getItem('USER_ROLE') || '[]')
  return roles.includes('ROLE_ADMIN')
})
const permissions = computed(() =>
  JSON.parse(localStorage.getItem('USER_PERMISSIONS') || '[]'),
)
const hasPermission = (perm) => isAdmin.value || permissions.value.includes(perm)

const canCreate = computed(() => hasPermission('USER_CREATE'))
const canEdit = computed(() => hasPermission('USER_UPDATE'))
const canLock = computed(() => hasPermission('USER_LOCK'))
const canUnlock = computed(() => hasPermission('USER_UNLOCK'))

const showModal = (type, title, message) => {
  modalType.value    = type
  modalTitle.value   = title
  modalMessage.value = message
  modalVisible.value = true
}

const getRolesParam = () =>
  roleFilter.value === 'ALL' ? ['ADMIN', 'STAFF'] : [roleFilter.value]

// ── Load trang hiện tại ────────────────────────────────
const loadData = async () => {
  try {
    const res      = await adminUserApi.getAll({
      keyword : keyword.value || undefined,
      roles   : getRolesParam(),
      page    : currentPage.value,
      size    : pageSize.value
    })
    const pageData = res.data.data
    users.value    = pageData.content      || []
    pageInfo.value = {
      totalElements : pageData.totalElements || 0,
      totalPages    : pageData.totalPages    || 0
    }
  } catch {
    showModal('error', 'Lỗi tải dữ liệu', 'Không thể lấy danh sách nhân viên.')
  }
}

// ── Load stats toàn bộ hệ thống ───────────────────────
const loadStats = async () => {
  try {
    const [adminRes, staffRes] = await Promise.all([
      adminUserApi.getAll({ roles: ['ADMIN'], page: 0, size: 1000 }),
      adminUserApi.getAll({ roles: ['STAFF'], page: 0, size: 1000 })
    ])
    const admins = adminRes.data.data.content || []
    const staffs = staffRes.data.data.content || []
    const all    = [...admins, ...staffs]
    stats.value  = {
      totalAdmin : admins.length,
      totalStaff : staffs.length,
      active     : all.filter(x =>  x.active).length,
      locked     : all.filter(x => !x.active).length
    }
  } catch { /* không block UI */ }
}

onMounted(() => {
  loadData()
  loadStats()
})

watch([keyword, roleFilter], () => {
  currentPage.value = 0
  loadData()
})

// ── Pagination ─────────────────────────────────────────
const pagination = computed(() => ({
  totalElements : pageInfo.value.totalElements,
  totalPages    : pageInfo.value.totalPages
}))

const goToPage = (page) => {
  if (page < 0 || page >= pagination.value.totalPages) return
  currentPage.value = page
  loadData()
}

const onPageSizeChange = (size) => {
  pageSize.value    = size
  currentPage.value = 0
  loadData()
}

// ── Lock / Unlock ──────────────────────────────────────
const lockUser = async (id) => {
  try {
    await adminUserApi.lock(id)
    showModal('success', 'Khóa thành công', 'Tài khoản đã được khóa.')
    await loadData()
    await loadStats()
  } catch (e) {
    showModal('error', 'Khóa thất bại',
      e.response?.data?.message || 'Không thể khóa tài khoản.')
  }
}

const unlockUser = async (id) => {
  try {
    await adminUserApi.unlock(id)
    showModal('success', 'Mở khóa thành công', 'Tài khoản đã được kích hoạt lại.')
    await loadData()
    await loadStats()
  } catch (e) {
    showModal('error', 'Mở khóa thất bại',
      e.response?.data?.message || 'Không thể mở khóa tài khoản.')
  }
}

// ── Modal form ─────────────────────────────────────────
const openCreate = () => {
  isEdit.value      = false
  editForm.value    = {}
  isModalOpen.value = true
}

const openEdit = (user) => {
  isEdit.value = true
  editForm.value = {
    userId      : user.userId,
    fullName    : user.fullName,
    username    : user.username,
    email       : user.email,
    phoneNumber : user.phoneNumber,
    roleName    : user.roleName,
    password    : ''
  }
  isModalOpen.value = true
}

const closeModal = () => { isModalOpen.value = false }

const saveUser = async (payload) => {
  saving.value = true
  try {
    if (isEdit.value) {
      await adminUserApi.update(payload.userId, {
        fullName    : payload.fullName,
        email       : payload.email,
        phoneNumber : payload.phoneNumber,
        roleId      : ROLE_MAP[payload.roleName]
      })
      showModal('success', 'Cập nhật thành công', 'Thông tin nhân viên đã được cập nhật.')
    } else {
      await adminUserApi.create({
        username    : payload.username,
        password    : payload.password,
        email       : payload.email,
        phoneNumber : payload.phoneNumber,
        fullName    : payload.fullName,
        roleId      : ROLE_MAP[payload.roleName]
      })
      showModal('success', 'Thêm thành công', 'Nhân viên mới đã được tạo.')
    }
    closeModal()
    await loadData()
    await loadStats()
  } catch (e) {
    const errors = e.response?.data?.data
    if (errors) {
      showModal('error', 'Thao tác thất bại', Object.values(errors).join('\n'))
    } else {
      showModal('error',
        isEdit.value ? 'Cập nhật thất bại' : 'Thêm thất bại',
        e.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại')
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.user-page {
  background: #fff7fa;
  min-height: 100vh;
  padding: 24px;
}

.page-header {
  background: #ffffff;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: #f55d9b;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.page-header h2 {
  color: #f55d9b;
  font-weight: 700;
  font-size: 22px;
  margin: 0;
}

.page-header p {
  color: #6b7280;
  margin: 2px 0 0;
  font-size: 14px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.stat-card span {
  color: #6b7280;
  font-size: 13px;
  font-weight: 600;
}

.stat-card h3 {
  margin-top: 8px;
  color: #111827;
  font-weight: 800;
  font-size: 26px;
}

.filter-card {
  background: #ffffff;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.filter-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  color: #b4557d;
  letter-spacing: 0.3px;
  margin-bottom: 8px;
}

.input-wrapper {
  position: relative;
}

.search-icon {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b4557d;
  font-size: 15px;
  pointer-events: none;
  z-index: 2;
}

.search-icon::after {
  content: '';
  position: absolute;
  right: -1px;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 20px;
  background: #f3d6e3;
}

.f-input {
  border: 1px solid #f3d6e3;
  background: #fffafd;
  border-radius: 10px;
  padding: 11px 14px;
  color: #344054;
  font-size: 14px;
  width: 100%;
  box-sizing: border-box;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.input-wrapper .f-input {
  padding-left: 50px;
}

.f-input::placeholder { color: #9ca3af; }

.f-input:hover {
  border-color: #efbdd2;
  background: #ffffff;
}

.f-input:focus {
  border-color: #f55d9b;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(245, 93, 155, 0.1);
  outline: none;
}

select.f-input {
  cursor: pointer;
  padding-right: 36px;
}

.btn-pink {
  background: #f55d9b;
  border: none;
  color: #ffffff;
  border-radius: 12px;
  padding: 10px 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.btn-pink:hover { background: #ec4d8d; }
</style>