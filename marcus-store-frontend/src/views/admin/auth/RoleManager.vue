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
      <label
        class="staff-label"
        for="staff-select"
      >
        Nhân viên
      </label>

      <select
        id="staff-select"
        class="staff-select"
        v-model="selectedStaff"
        @change="onStaffChange"
      >
        <option value="">
          -- Chọn nhân viên --
        </option>

        <option
          v-for="s in staffList"
          :key="s.userId"
          :value="s.userId"
        >
          {{ s.fullName }}
        </option>
      </select>

      <span class="staff-hint">
        {{
          selectedStaff
            ? `Đang chỉnh quyền: ${selectedStaffName}`
            : "Chọn nhân viên để xem quyền hiện tại"
        }}
      </span>
    </div>

    <!-- Sections -->
    <template
      v-for="(section, sIndex) in permissionData"
      :key="section.section"
    >

      <!-- Gạch ngang phân cách giữa các section -->
      <div
        v-if="sIndex > 0"
        class="section-divider"
      ></div>

      <div class="section-label">
        {{ section.section }}
      </div>

      <!-- Các module trong section, xếp thành hàng cột -->
      <div class="modules-row">

        <div
          v-for="mod in section.modules"
          :key="mod.key"
          class="module-col"
        >

          <div class="module-title">
            <i :class="['bi', mod.icon]"></i>
            {{ mod.name }}
          </div>

          <!-- Module có nhiều nhóm con -->
          <template v-if="mod.subs">

            <div
              v-for="sub in mod.subs"
              :key="sub.key"
              class="sub-block"
            >

              <div class="sub-title">
                {{ sub.label }}
              </div>

              <label
                v-for="perm in sub.perms"
                :key="perm.code"
                class="perm-row"
                :class="{
                  disabled: isPermDisabled(perm, sub.perms)
                }"
              >

                <input
                  type="checkbox"
                  v-model="perm.checked"
                  :disabled="isPermDisabled(perm, sub.perms)"
                  @change="onPermChange(perm, sub.perms)"
                >

                {{ perm.label }}

              </label>

            </div>

          </template>

          <!-- Module không có nhóm con -->
          <template v-else>

            <label
              v-for="perm in mod.perms"
              :key="perm.code"
              class="perm-row"
              :class="{
                disabled: isPermDisabled(perm, mod.perms)
              }"
            >

              <input
                type="checkbox"
                v-model="perm.checked"
                :disabled="isPermDisabled(perm, mod.perms)"
                @change="onPermChange(perm, mod.perms)"
              >

              {{ perm.label }}

            </label>

          </template>

        </div>

      </div>

    </template>

    <!-- Save -->
    <div class="save-bar">

      <span class="perm-counter">
        Đã chọn
        <strong>{{ totalChecked }}</strong>
        /
        <strong>{{ totalAll }}</strong>
        quyền
      </span>

      <button
        class="btn-save"
        :disabled="!selectedStaff"
        @click="savePermissions"
      >
        <i class="bi bi-check-circle-fill"></i>
        Lưu thay đổi
      </button>

    </div>

    <!-- Modal thông báo (thành công / lỗi) -->
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
import { ref, computed, reactive, onMounted } from "vue"
import permissionService from "@/api/permissionService"
import permissionMenu from "@/api/permissionMenu"
// TODO: đổi đúng đường dẫn tới component modal của bạn nếu khác chỗ này
import BaseModal from "@/components/BaseModal.vue"

const staffList = ref([])
const selectedStaff = ref(null)
const permissionData = ref([])
const expanded = reactive({})

// ===== State cho modal thông báo =====
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
  const staff = staffList.value.find(
    s => s.userId == selectedStaff.value
  )
  return staff ? staff.fullName : ""
})

async function loadStaff() {
  try {
    const res = await permissionService.getAllStaff({
      page: 0,
      size: 100
    })

    const users = res.data.data.content ?? res.data.data

    staffList.value = users.filter(
      u => u.roleName === "STAFF"
    )

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

              if (!sub.perms) {
                sub.perms = []
              }

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
          if (!module.perms) {
            module.perms = []
          }

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

    const res = await permissionService.getUserPermissions(
      selectedStaff.value
    )

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

        if (permission.checked) {
          permissionIds.push(permission.id)
        }

      })

    })

  })

  try {

    await permissionService.updateUserPermissions(
      selectedStaff.value,
      permissionIds
    )

    showModal("success", "Lưu thành công", `Đã cập nhật quyền cho ${selectedStaffName.value}.`)

  } catch (e) {

    console.log(e)
    showModal("error", "Lưu thất bại", "Có lỗi xảy ra khi lưu quyền, vui lòng thử lại.")

  }

}

onMounted(async () => {

  await loadStaff()
  await loadPermissions()

})

function toggleModule(key) {
  expanded[key] = !expanded[key]
}

// Lấy toàn bộ perms của 1 module, kể cả khi module có subs
function getAllPerms(module) {

  if (module.subs) {
    return module.subs.flatMap(sub => sub.perms || [])
  }

  return module.perms || []

}

function checkedCount(module) {

  return getAllPerms(module)
    .filter(p => p.checked)
    .length

}

function totalCount(module) {

  return getAllPerms(module).length

}

function badgeClass(module) {

  const checked = checkedCount(module)
  const total = totalCount(module)

  if (checked === 0) return "badge-none"

  if (checked === total) return "badge-all"

  return "badge-some"

}

function checkAllModule(module) {

  getAllPerms(module).forEach(p => {

    p.checked = true

  })

}

function uncheckAllModule(module) {

  getAllPerms(module).forEach(p => {

    p.checked = false

  })

}

// Bulk action cho từng sub-section (vd: riêng "Sản phẩm gốc" trong "Sản phẩm")
function checkAllSub(sub) {

  if (!sub.perms) return

  sub.perms.forEach(p => {
    p.checked = true
  })

}

function uncheckAllSub(sub) {

  if (!sub.perms) return

  sub.perms.forEach(p => {
    p.checked = false
  })

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

const totalAll = computed(() => {

  return permissionData.value
    .flatMap(section => section.modules)
    .flatMap(module => getAllPerms(module))
    .length

})

const totalChecked = computed(() => {

  return permissionData.value
    .flatMap(section => section.modules)
    .flatMap(module => getAllPerms(module))
    .filter(permission => permission.checked)
    .length

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
  gap: 16px;
  background: #fff;
  border: 1px solid #f3d6e3;
  border-radius: 14px;
  padding: 20px 24px;
  margin-bottom: 22px;
  box-shadow: 0 2px 10px rgba(0,0,0,.05);
}

.header-icon{
  width:48px;
  height:48px;
  border-radius:12px;
  background:#f55d9b;
  display:flex;
  justify-content:center;
  align-items:center;
  font-size:22px;
  color:#fff;
}

.header-text h2{
  margin:0;
  font-size:20px;
  font-weight:700;
  color:#f55d9b;
}

.header-text p{
  margin-top:4px;
  color:#6b7280;
  font-size:14px;
}

/* ================= STAFF ================= */

.staff-select-card{
  display:flex;
  align-items:center;
  gap:15px;
  background:#fff;
  border:1px solid #f3d6e3;
  border-radius:14px;
  padding:18px 20px;
  margin-bottom:25px;
  box-shadow:0 2px 10px rgba(0,0,0,.05);
}

.staff-label{
  font-weight:700;
  color:#374151;
}

.staff-select{
  min-width:240px;
  padding:10px 14px;
  border-radius:10px;
  border:1px solid #e8c9d7;
  background:#fff;
  transition:.2s;
}

.staff-select:focus{
  outline:none;
  border-color:#f55d9b;
  box-shadow:0 0 0 3px rgba(245,93,155,.15);
}

.staff-hint{
  margin-left:auto;
  color:#6b7280;
  font-size:13px;
  font-weight:600;
}

/* ================= SECTION ================= */

.section-label{
  margin:24px 0 14px;
  color:#c33d7a;
  font-weight:800;
  font-size:15px;
  text-transform:uppercase;
  letter-spacing:1px;
}

.section-divider{
  border-top: 2px dashed #e8b8cf;
  margin: 26px 0;
}

/* ================= MODULES ROW (dạng cột như hình mẫu) ================= */

.modules-row{
  display:grid;
  grid-template-columns:repeat(auto-fit,minmax(220px,1fr));
  gap:32px;
  margin-bottom:6px;
}

.module-col{
  display:flex;
  flex-direction:column;
  gap:10px;
}

.module-title{
  display:flex;
  align-items:center;
  gap:8px;
  font-size:16px;
  font-weight:800;
  color:#1f2937;
  margin-bottom:4px;
}

.module-title i{
  color:#f55d9b;
  font-size:17px;
}

/* ================= SUB ================= */

.sub-block{
  display:flex;
  flex-direction:column;
  gap:8px;
  margin-bottom:8px;
}

.sub-title{
  font-size:14px;
  font-weight:800;
  color:#c33d7a;
}

/* ================= PERMISSIONS (dạng dòng, không có khung) ================= */

.perm-row{
  display:flex;
  align-items:center;
  gap:10px;

  cursor:pointer;

  font-weight:700;
  font-size:14.5px;
  color:#1f2937;

  padding:2px 0;
}

.perm-row input{
  width:17px;
  height:17px;
  accent-color:#f55d9b;
}

.perm-row.disabled{
  opacity:.45;
  cursor:not-allowed;
}

/* ================= SAVE ================= */

.save-bar{
  margin-top:28px;
  display:flex;
  justify-content:space-between;
  align-items:center;

  background:#fff;
  border:1px solid #f3d6e3;

  border-radius:14px;

  padding:18px 22px;

  box-shadow:0 2px 10px rgba(0,0,0,.05);
}

.perm-counter{
  color:#6b7280;
  font-size:14px;
  font-weight:600;
}

.perm-counter strong{
  color:#f55d9b;
}

.btn-save{
  display:flex;
  align-items:center;
  gap:8px;

  border:none;

  background:#f55d9b;

  color:#fff;

  padding:11px 24px;

  border-radius:10px;

  font-weight:700;

  cursor:pointer;

  transition:.2s;
}

.btn-save:hover:not(:disabled){
  background:#ea4a8c;
  transform:translateY(-1px);
}

.btn-save:disabled{
  opacity:.5;
  cursor:not-allowed;
}

/* ================= RESPONSIVE ================= */

@media (max-width:768px){

  .staff-select-card{
    flex-direction:column;
    align-items:flex-start;
  }

  .staff-hint{
    margin-left:0;
  }

  .save-bar{
    flex-direction:column;
    gap:15px;
  }

  .btn-save{
    width:100%;
    justify-content:center;
  }

  .modules-row{
    grid-template-columns:1fr;
    gap:20px;
  }
}
</style>