<template>
  <div class="permission-page">

    <!-- Header -->
    <div class="page-header">
      <div class="header-icon">
        <i class="bi bi-shield-check"></i>
      </div>
      <div class="header-text">
        <h2>Phân quyền nhân viên</h2>
        <p>Cấp hoặc thu hồi quyền truy cập từng chức năng</p>
      </div>
    </div>

    <!-- Chọn nhân viên -->
    <div class="staff-select-card">

      <label class="staff-label">Nhân viên</label>

      <div class="staff-select-wrapper">

        <input
          type="text"
          class="staff-search"
          placeholder="Nhập tên nhân viên..."
          v-model="searchKeyword"
          @focus="showDropdown = true"
        />

        <button type="button" class="dropdown-btn" @click="toggleDropdown">
          <i class="bi" :class="showDropdown ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
        </button>

        <div v-if="showDropdown" class="staff-dropdown">

          <div v-if="filteredStaff.length === 0" class="staff-empty">
            Không tìm thấy nhân viên
          </div>

          <div
            v-for="staff in filteredStaff"
            :key="staff.userId"
            class="staff-item"
            @click="selectStaff(staff)"
          >
            <div class="staff-avatar">
              <i class="bi bi-person-circle"></i>
            </div>

            <div class="staff-info">
              <div class="staff-name">{{ staff.fullName }}</div>
              <div class="staff-email">{{ staff.email }}</div>
            </div>

            <i
              v-if="selectedStaff === staff.userId"
              class="bi bi-check-circle-fill selected-icon"
            ></i>
          </div>

        </div>

      </div>

      <span class="staff-hint">
        {{ selectedStaff ? `Đang chỉnh quyền: ${selectedStaffName}` : "Chọn nhân viên để xem quyền hiện tại" }}
      </span>

      <span v-if="selectedStaff" class="total-perm-badge">
        <i class="bi bi-key-fill"></i>
        Tổng số quyền:
        <strong>{{ totalChecked }}</strong> / {{ totalAll }}
      </span>

    </div>

    <!-- Sections -->
    <template v-for="(section, sIndex) in permissionData" :key="section.section">

      <div v-if="sIndex > 0" class="section-divider"></div>

      <div class="section-label">{{ section.section }}</div>

      <!-- Module có subs (vd: Bài viết -> Bài viết / Danh mục bài viết) được
           "làm phẳng" thành từng card riêng để tự chảy vào lưới chung,
           tránh bị chiếm nguyên 1 hàng gây thừa khoảng trống -->
      <div class="modules-row">
        <div v-for="card in getCards(section)" :key="card.key" class="module-col">

          <div class="module-title">
            <i :class="['bi', card.icon]"></i>
            {{ card.name }}
          </div>

          <label
            v-for="perm in card.perms"
            :key="perm.code"
            class="perm-row"
            :class="{ disabled: isPermDisabled(perm, card.perms) }"
          >
            <input
              type="checkbox"
              v-model="perm.checked"
              :disabled="isPermDisabled(perm, card.perms)"
              @change="onPermChange(perm, card.perms)"
            />
            {{ perm.label }}
          </label>

        </div>
      </div>

    </template>

    <!-- Save -->
    <div class="save-bar">
      <span class="perm-counter">
        Đã chọn <strong>{{ totalChecked }}</strong> / <strong>{{ totalAll }}</strong> quyền
      </span>

      <button class="btn-save" :disabled="!selectedStaff" @click="savePermissions">
        <i class="bi bi-check-circle-fill"></i>
        Lưu thay đổi
      </button>
    </div>

    <BaseModal
      :visible="modal.visible"
      :type="modal.type"
      :title="modal.title"
      :message="modal.message"
      @close="closeModal"
    />

  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from "vue"
import permissionService from "@/api/permissionService"
import permissionMenu from "@/api/permissionMenu"
// TODO: đổi đúng đường dẫn tới component modal của bạn nếu khác chỗ này
import BaseModal from "@/components/BaseModal.vue"

const staffList = ref([])
const selectedStaff = ref(null)
const searchKeyword = ref("")
const showDropdown = ref(false)
const permissionData = ref([])

// Chặn watch(searchKeyword) tự mở lại dropdown ngay sau khi vừa chọn
// xong 1 nhân viên (selectStaff gán searchKeyword -> watch chạy sau đó
// -> set showDropdown = true -> đè lên showDropdown = false vừa set)
const suppressKeywordWatch = ref(false)

const modal = reactive({
  visible: false,
  type: "info", // success | error | confirm | info
  title: "",
  message: ""
})

function showModal(type, title, message) {
  modal.type = type
  modal.title = title
  modal.message = message
  modal.visible = true
}

function closeModal() {
  modal.visible = false
}

const selectedStaffName = computed(() => {
  const staff = staffList.value.find(s => s.userId === selectedStaff.value)
  return staff ? staff.fullName : ""
})

const filteredStaff = computed(() => {
  if (!searchKeyword.value.trim()) return staffList.value

  return staffList.value.filter(staff =>
    staff.fullName.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const totalAll = computed(() =>
  permissionData.value
    .flatMap(section => section.modules)
    .flatMap(module => getAllPerms(module))
    .length
)

const totalChecked = computed(() =>
  permissionData.value
    .flatMap(section => section.modules)
    .flatMap(module => getAllPerms(module))
    .filter(permission => permission.checked)
    .length
)

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
}

function selectStaff(staff) {
  suppressKeywordWatch.value = true
  selectedStaff.value = staff.userId
  searchKeyword.value = staff.fullName
  showDropdown.value = false

  onStaffChange()
}

async function loadStaff() {
  try {
    const res = await permissionService.getAllStaff({ page: 0, size: 100 })
    const users = res.data.data.content ?? res.data.data

    staffList.value = users.filter(u => u.roleName === "STAFF")

    if (selectedStaff.value && !searchKeyword.value) {
      const current = staffList.value.find(s => s.userId === selectedStaff.value)
      if (current) searchKeyword.value = current.fullName
    }
  } catch (e) {
    console.log(e)
    showModal("error", "Không tải được danh sách nhân viên", "Vui lòng thử tải lại trang.")
  }
}

async function loadPermissions() {
  try {
    const res = await permissionService.getAllPermissions()
    const permissions = res.data.data
    const sections = JSON.parse(JSON.stringify(permissionMenu))

    permissions.forEach(permission => {
      sections.forEach(section => {
        section.modules.forEach(module => {

          // Module có subs (vd: Sản phẩm, Voucher, Bài viết)
          if (module.subs) {
            module.subs.forEach(sub => {
              if (!sub.perms) sub.perms = []

              if (sub.key === permission.module) {
                sub.perms.push({
                  id: permission.id,
                  code: permission.permission,
                  label: permission.description,
                  checked: false
                })
              }
            })
            return
          }

          // Module thẳng, không có subs
          if (!module.perms) module.perms = []

          if (module.key === permission.module) {
            module.perms.push({
              id: permission.id,
              code: permission.permission,
              label: permission.description,
              checked: false
            })
          }
        })
      })
    })

    permissionData.value = sections
  } catch (e) {
    console.log(e)
    showModal("error", "Không tải được danh sách quyền", "Vui lòng thử tải lại trang.")
  }
}

async function onStaffChange() {
  if (!selectedStaff.value) return

  try {
    const res = await permissionService.getUserPermissions(selectedStaff.value)
    const ids = res.data.data

    permissionData.value.forEach(section => {
      section.modules.forEach(module => {
        getAllPerms(module).forEach(permission => {
          permission.checked = ids.includes(permission.id)
        })
      })
    })
  } catch (e) {
    console.log(e)
    showModal("error", "Không tải được quyền của nhân viên", "Vui lòng chọn lại nhân viên hoặc thử lại sau.")
  }
}

async function savePermissions() {
  const permissionIds = []

  permissionData.value.forEach(section => {
    section.modules.forEach(module => {
      getAllPerms(module).forEach(permission => {
        if (permission.checked) permissionIds.push(permission.id)
      })
    })
  })

  try {
    await permissionService.updateUserPermissions(selectedStaff.value, permissionIds)
    showModal("success", "Lưu thành công", `Đã cập nhật quyền cho ${selectedStaffName.value}.`)
  } catch (e) {
    console.log(e)
    showModal("error", "Lưu thất bại", "Có lỗi xảy ra khi lưu quyền, vui lòng thử lại.")
  }
}

// Làm phẳng modules trong 1 section thành danh sách card để hiển thị trên
// lưới. Module có subs (vd: "Bài viết" -> "Bài viết" / "Danh mục bài viết")
// được tách thành từng card riêng, mỗi sub 1 card, thay vì gộp chung 1
// khối chiếm hết chiều ngang, nhờ đó các card tự chảy vào lưới chung.
function getCards(section) {
  const cards = []

  section.modules.forEach(mod => {
    if (mod.subs) {
      mod.subs.forEach(sub => {
        cards.push({
          key: `${mod.key}_${sub.key}`,
          icon: mod.icon,
          name: sub.label,
          perms: sub.perms || []
        })
      })
      return
    }

    cards.push({
      key: mod.key,
      icon: mod.icon,
      name: mod.name,
      perms: mod.perms || []
    })
  })

  return cards
}

// Lấy toàn bộ perms của 1 module, kể cả khi module có subs
function getAllPerms(module) {
  if (module.subs) return module.subs.flatMap(sub => sub.perms || [])
  return module.perms || []
}

// ===== Logic khóa quyền theo VIEW =====
// Nếu nhóm perms có quyền *_VIEW thì các quyền khác chỉ được tick
// sau khi quyền VIEW đã được tick.

function hasViewChecked(perms) {
  const viewPerm = perms.find(p => p.code && p.code.endsWith("_VIEW"))
  if (!viewPerm) return true // nhóm không có quyền VIEW thì không khóa gì cả
  return viewPerm.checked
}

function isPermDisabled(perm, perms) {
  if (perm.code && perm.code.endsWith("_VIEW")) return false
  return !hasViewChecked(perms)
}

// Khi bỏ tick VIEW thì tự động bỏ tick hết các quyền còn lại trong nhóm
function onPermChange(perm, perms) {
  if (perm.code && perm.code.endsWith("_VIEW") && !perm.checked) {
    perms.forEach(p => {
      if (p.code !== perm.code) p.checked = false
    })
  }
}

watch(searchKeyword, value => {
  if (suppressKeywordWatch.value) {
    suppressKeywordWatch.value = false
    return
  }

  if (value === "") selectedStaff.value = null

  showDropdown.value = true
})

onMounted(async () => {
  await loadStaff()
  await loadPermissions()
})
</script>

<style scoped>
.permission-page {
  padding: 24px;
  background: #fff7fa;
  min-height: 100vh;
}

/* ================= HEADER ================= */

.page-header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
  background: #fff;
  border: 1px solid #f3d6e3;
  border-radius: 14px;
  padding: 20px 24px;
  margin-bottom: 22px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, .05);
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #f55d9b;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 22px;
  color: #fff;
  flex-shrink: 0;
}

.header-text {
  text-align: left;
  margin-right: auto;
}

.header-text h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #f55d9b;
  text-align: left;
}

.header-text p {
  margin-top: 4px;
  color: #6b7280;
  font-size: 14px;
  text-align: left;
}

/* ================= STAFF ================= */

.staff-select-card {
  display: flex;
  align-items: center;
  gap: 15px;
  background: #fff;
  border: 1px solid #f3d6e3;
  border-radius: 14px;
  padding: 18px 20px;
  margin-bottom: 25px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, .05);
  flex-wrap: wrap;
}

.staff-label {
  font-weight: 700;
  color: #374151;
}

.staff-select-wrapper {
  position: relative;
  width: 330px;
}

.staff-search {
  width: 100%;
  height: 46px;
  border: 1px solid #e7d3dd;
  border-radius: 12px;
  padding: 0 42px 0 16px;
  background: #fff;
  transition: .25s;
  outline: none;
}

.staff-search:focus {
  border-color: #f55d9b;
  box-shadow: 0 0 0 4px rgba(245, 93, 155, .12);
}

.dropdown-btn {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: .2s;
}

.dropdown-btn:hover {
  background: #fff0f7;
  color: #f55d9b;
}

.staff-dropdown {
  position: absolute;
  left: 0;
  right: 0;
  top: 100%;
  margin-top: 8px;
  background: #fff;
  border: 1px solid #ececec;
  border-radius: 12px;
  overflow-y: auto;
  max-height: 260px;
  z-index: 999;
  box-shadow: 0 12px 30px rgba(0, 0, 0, .12);
}

.staff-empty {
  padding: 16px;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

.staff-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  transition: .2s;
}

.staff-item:hover {
  background: #fff0f7;
}

.staff-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff0f7;
  color: #f55d9b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.staff-info {
  flex: 1;
  min-width: 0;
}

.staff-name {
  font-weight: 700;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.staff-email {
  font-size: 12.5px;
  color: #8b8b8b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.selected-icon {
  color: #f55d9b;
  font-size: 16px;
  flex-shrink: 0;
}

.staff-hint {
  margin-left: auto;
  color: #6b7280;
  font-size: 13px;
  font-weight: 600;
}

.total-perm-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff0f5;
  color: #f55d9b;
  font-size: 13px;
  font-weight: 700;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid #f3d6e3;
  white-space: nowrap;
}

.total-perm-badge i {
  font-size: 14px;
}

.total-perm-badge strong {
  color: #c33d7a;
}

/* ================= SECTION ================= */

.section-label {
  margin: 24px 0 14px;
  color: #c33d7a;
  font-weight: 800;
  font-size: 15px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.section-divider {
  border-top: 2px dashed #e8b8cf;
  margin: 26px 0;
}

/* ================= MODULES ROW (dạng cột như hình mẫu) ================= */

.modules-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 32px;
  margin-bottom: 6px;
  align-items: start;
}

.module-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.module-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 800;
  color: #1f2937;
  margin-bottom: 4px;
}

.module-title i {
  color: #f55d9b;
  font-size: 17px;
}

/* ================= PERMISSIONS (dạng dòng, không có khung) ================= */

.perm-row {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-weight: 700;
  font-size: 14.5px;
  color: #1f2937;
  padding: 2px 0;
}

.perm-row input {
  width: 17px;
  height: 17px;
  accent-color: #f55d9b;
}

.perm-row.disabled {
  opacity: .45;
  cursor: not-allowed;
}

/* ================= SAVE ================= */

.save-bar {
  margin-top: 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border: 1px solid #f3d6e3;
  border-radius: 14px;
  padding: 18px 22px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, .05);
}

.perm-counter {
  color: #6b7280;
  font-size: 14px;
  font-weight: 600;
}

.perm-counter strong {
  color: #f55d9b;
}

.btn-save {
  display: flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: #f55d9b;
  color: #fff;
  padding: 11px 24px;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  transition: .2s;
}

.btn-save:hover:not(:disabled) {
  background: #ea4a8c;
  transform: translateY(-1px);
}

.btn-save:disabled {
  opacity: .5;
  cursor: not-allowed;
}

/* ================= RESPONSIVE ================= */

@media (max-width: 768px) {
  .staff-select-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .staff-select-wrapper {
    width: 100%;
  }

  .staff-hint {
    margin-left: 0;
  }

  .total-perm-badge {
    margin-left: 0;
  }

  .save-bar {
    flex-direction: column;
    gap: 15px;
  }

  .btn-save {
    width: 100%;
    justify-content: center;
  }

  .modules-row {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>