<template>
  <div class="user-page">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <i class="bi bi-person-lines-fill"></i>
        </div>
        <div>
          <h2>Quản lý khách hàng</h2>
          <p>Danh sách khách hàng trong hệ thống</p>
        </div>
      </div>

      <button v-if="canCreate" class="btn-pink" @click="openCreate">
        <i class="bi bi-plus-circle"></i>
        Thêm khách hàng
      </button>
    </div>

    <!-- Statistic -->
    <div class="row g-3 mb-4">
      <div class="col-md-3">
        <div class="stat-card">
          <span>Tổng khách hàng</span>
          <h3>{{ pageInfo.totalElements }}</h3>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card">
          <span>Email xác thực</span>
          <h3>{{ stats.verified }}</h3>
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

<div class="filter-card">
  <div class="row g-3 align-items-end">

    <!-- Search -->
    <div class="col-lg-5 col-md-12">
      <label class="filter-label">Tìm kiếm</label>

      <div class="input-wrapper">
        <i class="bi bi-search search-icon"></i>

        <input
          v-model="keyword"
          class="form-control f-input"
          placeholder="Tìm theo tên, email hoặc số điện thoại"
        />
      </div>
    </div>

    <!-- Trạng thái -->
    <div class="col-lg-2 col-md-4">
      <label class="filter-label">Trạng thái</label>

      <select
        class="form-select f-input"
        v-model="status"
      >
        <option value="">Tất cả</option>
        <option value="true">Hoạt động</option>
        <option value="false">Đã khóa</option>
      </select>
    </div>

    <!-- Email -->
    <div class="col-lg-2 col-md-4">
      <label class="filter-label">Email</label>

      <select
        class="form-select f-input"
        v-model="emailVerified"
      >
        <option value="">Tất cả</option>
        <option value="true">Đã xác thực</option>
        <option value="false">Chưa xác thực</option>
      </select>
    </div>

    <!-- Hạng -->
    <div class="col-lg-3 col-md-4">
      <label class="filter-label">Hạng thành viên</label>

      <select
        class="form-select f-input"
        v-model="membership"
      >
        <option value="">Tất cả</option>
        <option value="bronze">🥉 Đồng</option>
        <option value="silver">🥈 Bạc</option>
        <option value="gold">🥇 Vàng</option>
        <option value="diamond">💎 Kim Cương</option>
      </select>
    </div>

  </div>
</div>
    <CustomerTable
      :users="users"
      :can-edit="canEdit"
      :can-lock="canLock"
      :can-unlock="canUnlock"
      :can-send-verify="canSendVerify"
      :pagination="pagination"
      :current-page="currentPage"
      :page-size="pageSize"
      @lock="lockUser"
      @unlock="unlockUser"
      @edit="openEdit"
      @send-verify="sendVerifyEmail"
      @page-change="goToPage"
      @page-size-change="onPageSizeChange"
    />
    <UserFormModal
      :visible="isModalOpen"
      :is-edit="isEdit"
      :saving="saving"
      :initial-data="editForm"
      :allowed-roles="[{ value: 'CUSTOMER', label: 'Khách hàng' }]"
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
import CustomerTable from './CustomerTable.vue'
import adminUserApi from '@/api/adminUserApi.js'
import UserFormModal from '@/components/UserFormModal.vue'
import BaseModal from '@/components/BaseModal.vue'

/* =========================
   DATA
========================= */

const users = ref([])

const keyword = ref('')
const status = ref('')
const emailVerified = ref('')
const membership = ref('')

const currentPage = ref(0)
const pageSize = ref(5)

const pageInfo = ref({
  totalElements: 0,
  totalPages: 0,
})

const stats = ref({
  verified: 0,
  active: 0,
  locked: 0,
})

const isModalOpen = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const editForm = ref({})

const modalVisible = ref(false)
const modalType = ref('success')
const modalTitle = ref('')
const modalMessage = ref('')

/* =========================
   PERMISSION
========================= */

const isAdmin = computed(() => {
  const roles = JSON.parse(localStorage.getItem('USER_ROLE') || '[]')
  return roles.includes('ROLE_ADMIN')
})

const permissions = computed(() =>
  JSON.parse(localStorage.getItem('USER_PERMISSIONS') || '[]'),
)

const hasPermission = (perm) =>
  isAdmin.value || permissions.value.includes(perm)

const canCreate = computed(() => hasPermission('USER_CREATE'))
const canEdit = computed(() => hasPermission('USER_UPDATE'))
const canLock = computed(() => hasPermission('USER_LOCK'))
const canUnlock = computed(() => hasPermission('USER_UNLOCK'))
const canSendVerify = computed(() => hasPermission('USER_SEND_EMAIL'))

/* =========================
   MODAL
========================= */

const showModal = (type, title, message) => {
  modalType.value = type
  modalTitle.value = title
  modalMessage.value = message
  modalVisible.value = true
}

/* =========================
   LOAD DATA
========================= */

const loadData = async () => {
  try {
    const res = await adminUserApi.getAll({
      keyword: keyword.value || undefined,
      roles: ['CUSTOMER'],

      status:
        status.value === ''
          ? undefined
          : status.value === 'true',

      emailVerified:
        emailVerified.value === ''
          ? undefined
          : emailVerified.value === 'true',

      page: currentPage.value,
      size: pageSize.value,
    })

    const pageData = res.data.data

    let list = pageData.content || []

    // ===== Lọc hạng thành viên ở FE =====
    if (membership.value) {
      list = list.filter((u) => {
        const spent = Number(u.totalSpent || 0)

        switch (membership.value) {
          case 'diamond':
            return spent >= 300000000

          case 'gold':
            return spent >= 150000000 && spent < 300000000

          case 'silver':
            return spent >= 50000000 && spent < 150000000

          case 'bronze':
            return spent < 50000000

          default:
            return true
        }
      })
    }

    users.value = list

    pageInfo.value = {
      totalElements: pageData.totalElements,
      totalPages: pageData.totalPages,
    }
  } catch (e) {
    showModal(
      'error',
      'Lỗi',
      e.response?.data?.message || 'Không tải được danh sách khách hàng',
    )
  }
}

/* =========================
   LOAD STATS
========================= */

const loadStats = async () => {
  try {
    const res = await adminUserApi.getAll({
      roles: ['CUSTOMER'],
      page: 0,
      size: 1000,
    })

    const list = res.data.data.content || []

    stats.value = {
      verified: list.filter((x) => x.emailVerified).length,
      active: list.filter((x) => x.active).length,
      locked: list.filter((x) => !x.active).length,
    }
  } catch {
    stats.value = { verified: 0, active: 0, locked: 0 }
  }
}

/* =========================
   INIT
========================= */

onMounted(async () => {
  await loadData()
  await loadStats()
})

watch(
  [keyword, status, emailVerified, membership],
  () => {
    currentPage.value = 0
    loadData()
  },
)

/* =========================
   PAGINATION
========================= */

const pagination = computed(() => ({
  totalElements: pageInfo.value.totalElements,
  totalPages: pageInfo.value.totalPages,
}))

const goToPage = (page) => {
  currentPage.value = page
  loadData()
}

const onPageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 0
  loadData()
}

// ── Gửi email xác thực ────────────────────────────────
const sendVerifyEmail = async (id) => {
  try {
    await adminUserApi.sendVerifyEmail(id)

    showModal(
      'success',
      'Đã gửi email xác thực',
      'Email xác thực đã được gửi đến khách hàng.',
    )

    await loadData()
    await loadStats()
  } catch (e) {
    showModal(
      'error',
      'Gửi email thất bại',
      e.response?.data?.message || 'Có lỗi xảy ra.',
    )
  }
}

// =========================
// Khóa tài khoản
// =========================
const lockUser = async (id) => {
  try {
    await adminUserApi.lock(id)

    showModal(
      'success',
      'Khóa tài khoản thành công',
      'Tài khoản khách hàng đã được khóa.',
    )

    await loadData()
    await loadStats()
  } catch (e) {
    showModal(
      'error',
      'Khóa tài khoản thất bại',
      e.response?.data?.message || 'Có lỗi xảy ra.',
    )
  }
}

// =========================
// Mở khóa
// =========================
const unlockUser = async (id) => {
  try {
    await adminUserApi.unlock(id)

    showModal(
      'success',
      'Mở khóa thành công',
      'Tài khoản khách hàng đã được kích hoạt.',
    )

    await loadData()
    await loadStats()
  } catch (e) {
    showModal(
      'error',
      'Mở khóa thất bại',
      e.response?.data?.message || 'Có lỗi xảy ra.',
    )
  }
}

// =========================
// Modal
// =========================
const openCreate = () => {
  isEdit.value = false
  editForm.value = {}
  isModalOpen.value = true
}

const openEdit = (user) => {
  isEdit.value = true

  editForm.value = {
    userId: user.userId,
    fullName: user.fullName,
    username: user.username,
    email: user.email,
    phoneNumber: user.phoneNumber,
    roleName: user.roleName,
    password: '',
  }

  isModalOpen.value = true
}

const closeModal = () => {
  isModalOpen.value = false
}

// =========================
// Lưu
// =========================
const saveUser = async (payload) => {
  saving.value = true

  try {
    if (isEdit.value) {
      await adminUserApi.update(payload.userId, {
        fullName: payload.fullName,
        email: payload.email,
        phoneNumber: payload.phoneNumber,
        roleId: 3,
      })

      showModal(
        'success',
        'Cập nhật thành công',
        'Thông tin khách hàng đã được cập nhật.',
      )
    } else {
      await adminUserApi.create({
        username: payload.username,
        password: payload.password,
        email: payload.email,
        phoneNumber: payload.phoneNumber,
        fullName: payload.fullName,
        roleId: 3,
      })

      showModal(
        'success',
        'Thêm thành công',
        'Khách hàng mới đã được tạo.',
      )
    }

    closeModal()

    await loadData()
    await loadStats()
  } catch (e) {
    showModal(
      'error',
      'Thao tác thất bại',
      e.response?.data?.message || 'Có lỗi xảy ra.',
    )
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
  background: #fff;
  border: 1px solid #f1d7e2;
  border-radius: 14px;
  padding: 22px 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,.04);
}

.filter-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #c15d87;
  text-transform: uppercase;
  letter-spacing: .5px;
}

.input-wrapper {
  position: relative;
}

.search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #d26a95;
  font-size: 16px;
}

.search-icon::after {
  content: "";
  position: absolute;
  right: -14px;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 20px;
  background: #f2d7e3;
}

.f-input {
  height: 48px;
  border-radius: 12px;
  border: 1px solid #f2d7e3;
  background: #fff;
  color: #333;
  font-size: 14px;
  transition: all .25s ease;
}

.input-wrapper .f-input {
  padding-left: 52px;
}

.f-input::placeholder {
  color: #9ca3af;
}

.f-input:hover {
  border-color: #f4a8c6;
}

.f-input:focus {
  border-color: #f55d9b;
  box-shadow: 0 0 0 4px rgba(245,93,155,.12);
}

.form-select.f-input {
  cursor: pointer;
  padding-left: 14px;
  font-weight: 500;
}

.form-select.f-input:focus {
  background-color: #fff;
}

@media (max-width: 992px) {
  .filter-card {
    padding: 18px;
  }

  .f-input {
    height: 44px;
  }
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

.btn-pink:hover {
  background: #ec4d8d;
}
</style>
