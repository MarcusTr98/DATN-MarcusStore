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
 
      <button
        v-if="canManage"
        class="btn-pink"
        @click="openCreate"
      >
        <i class="bi bi-plus-circle"></i>
        Thêm khách hàng
      </button>
    </div>
 
    <!-- Statistic -->
    <div class="row g-3 mb-4">
      <div class="col-md-3">
        <div class="stat-card">
          <span>Tổng khách hàng</span>
          <h3>{{ users.length }}</h3>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card">
          <span>Email xác thực</span>
          <h3>{{ verifiedCount }}</h3>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card">
          <span>Hoạt động</span>
          <h3>{{ activeCount }}</h3>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card">
          <span>Đã khóa</span>
          <h3>{{ lockedCount }}</h3>
        </div>
      </div>
    </div>
 
    <!-- Search -->
    <div class="filter-card">
      <div class="row g-3 align-items-end">
        <div class="col-12">
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
      </div>
    </div>
 
    <CustomerTable
      :users="pagedUsers"
      :can-manage="canManage"
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
        { value: 'CUSTOMER', label: 'Khách hàng' }
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
import CustomerTable from './CustomerTable.vue'
import Use from '@/api/Use.js'
import UserFormModal from '@/components/UserFormModal.vue'
import BaseModal from '@/components/BaseModal.vue'
 
// ── Data ───────────────────────────────────────────────
const users   = ref([])
const keyword = ref('')
 
// ── Pagination state ───────────────────────────────────
const currentPage = ref(0)
const pageSize    = ref(5)
 
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
 
// ── Helpers ────────────────────────────────────────────
const canManage = computed(() => {
  const roles = JSON.parse(localStorage.getItem('USER_ROLE') || '[]')
  return roles.includes('ROLE_ADMIN')
})
 
const showModal = (type, title, message) => {
  modalType.value    = type
  modalTitle.value   = title
  modalMessage.value = message
  modalVisible.value = true
}
 
// ── Load toàn bộ khách hàng 1 lần ─────────────────────
const loadData = async () => {
  try {
    const res = await Use.getAll({ page: 0, size: 1000 })
    users.value = (res.data.data.content || []).filter(
      x => x.roleName === 'CUSTOMER'
    )
  } catch (e) {
    showModal('error', 'Lỗi tải dữ liệu', 'Không thể lấy danh sách khách hàng.')
  }
}
 
onMounted(loadData)
 
// ── Filter client-side ─────────────────────────────────
const filteredCustomers = computed(() => {
  const kw = keyword.value.toLowerCase()
  return users.value.filter(x =>
    x.fullName?.toLowerCase().includes(kw) ||
    x.email?.toLowerCase().includes(kw) ||
    x.phoneNumber?.toLowerCase().includes(kw)
  )
})
 
// Reset về trang 0 khi keyword thay đổi
watch(keyword, () => {
  currentPage.value = 0
})
 
// ── Pagination computed ────────────────────────────────
const pagination = computed(() => ({
  totalElements : filteredCustomers.value.length,
  totalPages    : Math.ceil(filteredCustomers.value.length / pageSize.value)
}))
 
const pagedUsers = computed(() => {
  const start = currentPage.value * pageSize.value
  return filteredCustomers.value.slice(start, start + pageSize.value)
})
 
// ── Pagination handlers ────────────────────────────────
const goToPage = (page) => {
  if (page < 0 || page >= pagination.value.totalPages) return
  currentPage.value = page
}
 
const onPageSizeChange = (size) => {
  pageSize.value    = size
  currentPage.value = 0
}
 
// ── Stats ──────────────────────────────────────────────
const verifiedCount = computed(() => users.value.filter(x =>  x.emailVerified).length)
const activeCount   = computed(() => users.value.filter(x =>  x.active).length)
const lockedCount   = computed(() => users.value.filter(x => !x.active).length)
 
// ── Lock / Unlock ──────────────────────────────────────
const lockUser = async (id) => {
  try {
    await Use.lock(id)
    showModal('success', 'Khóa tài khoản thành công', 'Tài khoản khách hàng đã được khóa.')
    await loadData()
  } catch (e) {
    showModal('error', 'Khóa tài khoản thất bại', e.response?.data?.message || 'Có lỗi xảy ra.')
  }
}
 
const unlockUser = async (id) => {
  try {
    await Use.unlock(id)
    showModal('success', 'Mở khóa thành công', 'Tài khoản khách hàng đã được kích hoạt lại.')
    await loadData()
  } catch (e) {
    showModal('error', 'Mở khóa thất bại', e.response?.data?.message || 'Có lỗi xảy ra.')
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
 
const closeModal = () => {
  isModalOpen.value = false
}
 
const saveUser = async (payload) => {
  saving.value = true
  try {
    if (isEdit.value) {
      await Use.update(payload.userId, {
        fullName    : payload.fullName,
        email       : payload.email,
        phoneNumber : payload.phoneNumber,
        roleId      : 3
      })
      showModal('success', 'Cập nhật thành công', 'Thông tin khách hàng đã được cập nhật.')
    } else {
      await Use.create({
        username    : payload.username,
        password    : payload.password,
        email       : payload.email,
        phoneNumber : payload.phoneNumber,
        fullName    : payload.fullName,
        roleId      : 3
      })
      showModal('success', 'Thêm thành công', 'Khách hàng mới đã được tạo.')
    }
    closeModal()
    await loadData()
  } catch (e) {
    showModal(
      'error',
      'Thao tác thất bại',
      e.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại'
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
 
.f-input::placeholder {
  color: #9ca3af;
}
 
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