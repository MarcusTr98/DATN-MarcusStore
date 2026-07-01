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
      <label class="staff-label" for="staff-select">
        Nhân viên
      </label>

      <select
        id="staff-select"
        class="staff-select"
        v-model="selectedStaff"
        @change="onStaffChange"
      >
        <option value="">-- Chọn nhân viên --</option>

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
      v-for="section in permissionData"
      :key="section.section"
    >

      <div class="section-label">
        {{ section.section }}
      </div>

      <!-- Module -->
      <div
        v-for="mod in section.modules"
        :key="mod.key"
        class="module-card"
      >

        <!-- Header -->
        <div
          class="module-head"
          @click="toggleModule(mod.key)"
        >

          <div class="module-name">
            <i :class="['bi', mod.icon]"></i>
            {{ mod.name }}
          </div>

          <div class="module-right">

            <span
              :class="['badge', badgeClass(mod)]"
            >
              {{ checkedCount(mod) }}/{{ totalCount(mod) }}
            </span>

            <i
              :class="[
                'bi',
                'bi-chevron-down',
                'chevron',
                { open: expanded[mod.key] }
              ]"
            ></i>

          </div>

        </div>

        <!-- Body -->
        <div
          v-if="expanded[mod.key]"
          class="module-body"
        >

          <!-- Module có sub -->
          <template v-if="mod.subs">

            <div
              v-for="sub in mod.subs"
              :key="sub.key"
              class="sub-section"
            >

              <div class="sub-header">

                <div class="sub-title">
                  {{ sub.label }}
                </div>

                <div class="sub-count">
                  {{ (sub.perms || []).filter(p => p.checked).length }}
                  /
                  {{ (sub.perms || []).length }}
                </div>

              </div>

              <div class="perms-grid">

                <label
                  v-for="perm in sub.perms"
                  :key="perm.code"
                  class="perm-label"
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

            </div>

          </template>

          <!-- Module thường -->
          <template v-else>

            <div class="perms-grid">

              <label
                v-for="perm in mod.perms"
                :key="perm.code"
                class="perm-label"
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

            </div>

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

  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from "vue"
import permissionService from "@/api/permissionService"
import permissionMenu from "@/api/permissionMenu"

const staffList = ref([])
const selectedStaff = ref(null)
const permissionData = ref([])
const expanded = reactive({})

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

    alert("Lưu thành công")

  } catch (e) {

    console.log(e)

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
  background:#fff0f7;
  display:flex;
  justify-content:center;
  align-items:center;
  font-size:22px;
  color:#f55d9b;
}

.header-text h2{
  margin:0;
  font-size:20px;
  font-weight:700;
  color:#1f2937;
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
  font-weight:600;
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
}

/* ================= SECTION ================= */

.section-label{
  margin:24px 0 10px;
  color:#c33d7a;
  font-weight:700;
  font-size:13px;
  text-transform:uppercase;
  letter-spacing:1px;
}

/* ================= MODULE ================= */

.module-card{
  background:#fff;
  border:1px solid #f3d6e3;
  border-radius:14px;
  margin-bottom:14px;
  overflow:hidden;
  box-shadow:0 2px 10px rgba(0,0,0,.05);
}

.module-head{
  display:flex;
  justify-content:space-between;
  align-items:center;
  padding:16px 20px;
  cursor:pointer;
  transition:.2s;
}

.module-head:hover{
  background:#fff3f8;
}

.module-name{
  display:flex;
  align-items:center;
  gap:12px;
  font-size:15px;
  font-weight:600;
  color:#374151;
}

.module-name i{
  color:#f55d9b;
  font-size:18px;
}

.module-right{
  display:flex;
  align-items:center;
  gap:12px;
}

.chevron{
  transition:.25s;
  color:#b4557d;
}

.chevron.open{
  transform:rotate(180deg);
}

/* ================= BADGE ================= */

.badge{
  min-width:52px;
  text-align:center;
  padding:5px 12px;
  border-radius:20px;
  font-size:12px;
  font-weight:700;
}

.badge-none{
  background:#f3f4f6;
  color:#6b7280;
}

.badge-some{
  background:#fff0f7;
  color:#d63384;
}

.badge-all{
  background:#e8fff2;
  color:#15803d;
}

/* ================= BODY ================= */

.module-body{
  padding:18px 20px;
  border-top:1px solid #f3d6e3;
  background:#fffdfd;
}

/* ================= SUB ================= */

.sub-section{
  margin-bottom:22px;
}

.sub-section:last-child{
  margin-bottom:0;
}

.sub-header{
  display:flex;
  justify-content:space-between;
  align-items:center;
  margin-bottom:14px;
}

.sub-title{
  font-size:17px;
  font-weight:600;
  color:#374151;
}

.sub-count{
  background:#fff0f7;
  color:#d63384;
  padding:4px 12px;
  border-radius:20px;
  font-size:13px;
  font-weight:700;
}

/* ================= PERMISSIONS ================= */

.perms-grid{
  display:grid;
  grid-template-columns:repeat(auto-fit,minmax(230px,1fr));
  gap:12px;
}

.perm-label{
  display:flex;
  align-items:center;
  gap:12px;

  background:#fff;

  border:1px solid #f3d6e3;
  border-radius:10px;

  padding:12px 14px;

  cursor:pointer;

  transition:.2s;

  color:#374151;
  font-size:14px;
}

.perm-label:hover{
  background:#fff5f9;
  border-color:#f55d9b;
  transform:translateY(-1px);
}

.perm-label input{
  width:17px;
  height:17px;
  accent-color:#f55d9b;
}

.perm-label.disabled{
  opacity:.45;
  cursor:not-allowed;
}

.perm-label.disabled:hover{
  transform:none;
  border-color:#f3d6e3;
  background:#fff;
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

  font-weight:600;

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

  .perms-grid{
    grid-template-columns:1fr;
  }
}
</style>