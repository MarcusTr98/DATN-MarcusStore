<template>
  <div
    v-if="visible"
    class="modal-backdrop-custom"
    @click.self="$emit('close')"
  >
    <div class="user-modal">

      <!-- Header -->
      <div class="modal-head">
        <div>
          <h2>
            {{ isEdit ? 'Cập nhật tài khoản' : 'Thêm tài khoản mới' }}
          </h2>

          <p>
            {{
              isEdit
                ? 'Chỉnh sửa thông tin tài khoản.'
                : 'Điền đầy đủ thông tin để tạo tài khoản mới.'
            }}
          </p>
        </div>

        <button
          type="button"
          class="icon-button"
          title="Đóng"
          @click="$emit('close')"
        >
          <i class="bi bi-x-lg"></i>
        </button>
      </div>

      <!-- Form -->
      <form
        class="user-form"
        novalidate
        @submit.prevent="onSubmit"
      >

        <!-- Thông tin -->
        <div class="form-grid">

          <!-- Họ tên -->
          <div>
            <label class="form-label">
              Họ và tên <span>*</span>
            </label>

            <input
              v-model.trim="form.fullName"
              type="text"
              class="form-control finput"
              :class="{ 'is-invalid': isSubmitted && errors.fullName }"
              placeholder="Nguyễn Văn A"
            >

            <div
              v-if="errors.fullName"
              class="invalid-feedback"
            >
              {{ errors.fullName }}
            </div>
          </div>

          <!-- Username -->
          <div>
            <label class="form-label">
              Tên đăng nhập <span>*</span>
            </label>

            <input
              v-model.trim="form.username"
              type="text"
              class="form-control finput"
              :class="{ 'is-invalid': isSubmitted && errors.username }"
              placeholder="nguyenvana123"
              :disabled="isEdit"
            >

            <div
              v-if="errors.username"
              class="invalid-feedback"
            >
              {{ errors.username }}
            </div>
          </div>

          <!-- Email -->
          <div>
            <label class="form-label">
              Email <span>*</span>
            </label>

            <input
              v-model.trim="form.email"
              type="email"
              class="form-control finput"
              :class="{ 'is-invalid': isSubmitted && errors.email }"
              placeholder="email@example.com"
            >

            <div
              v-if="errors.email"
              class="invalid-feedback"
            >
              {{ errors.email }}
            </div>
          </div>

          <!-- SĐT -->
          <div>
            <label class="form-label">
              Số điện thoại <span>*</span>
            </label>

            <input
              v-model.trim="form.phoneNumber"
              type="text"
              class="form-control finput"
              :class="{ 'is-invalid': isSubmitted && errors.phoneNumber }"
              placeholder="0901234567"
            >

            <div
              v-if="errors.phoneNumber"
              class="invalid-feedback"
            >
              {{ errors.phoneNumber }}
            </div>
          </div>

          <!-- Password -->
          <div v-if="!isEdit">

            <label class="form-label">
              Mật khẩu <span>*</span>
            </label>

            <div class="password-wrapper">

              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                class="form-control finput"
                :class="{ 'is-invalid': isSubmitted && errors.password }"
                placeholder="Tối thiểu 6 ký tự"
              >

              <i
                class="bi password-eye"
                :class="showPassword ? 'bi-eye-slash' : 'bi-eye'"
                @click="showPassword = !showPassword"
              ></i>

            </div>

            <div
              v-if="errors.password"
              class="invalid-feedback d-block"
            >
              {{ errors.password }}
            </div>

          </div>

          <!-- Role -->
          <div>

            <label class="form-label">
              Vai trò <span>*</span>
            </label>

            <select
              v-model="form.roleName"
              class="form-select finput"
            >

              <option
                v-for="role in allowedRoles"
                :key="role.value"
                :value="role.value"
              >
                {{ role.label }}
              </option>

            </select>

          </div>

        </div>

        <!-- Module -->
        <div
          v-if="form.roleName === 'STAFF'"
          class="permission-box"
        >

          <div class="permission-header">

            <h4>Module được cấp quyền</h4>

            <p>
              Nhân viên sẽ được cấp toàn bộ quyền của các module được chọn.
            </p>

          </div>

          <div
            v-for="group in groupedModules"
            :key="group.section"
            class="permission-group"
          >

            <div class="permission-group-title">
              {{ group.section }}
            </div>

            <div class="module-grid">

              <label
                v-for="module in group.modules"
                :key="module.key"
                class="module-card"
              >

                <input
                  type="checkbox"
                  :value="module.key"
                  v-model="form.moduleNames"
                >

                <i
                  class="bi"
                  :class="module.icon"
                ></i>

                <span>
                  {{ module.name }}
                </span>

              </label>

            </div>

          </div>

        </div>

        <!-- Footer -->
        <div class="form-actions">

          <button
            type="button"
            class="btn btn-soft"
            @click="$emit('close')"
          >
            Hủy
          </button>

          <button
            type="submit"
            class="btn btn-pink"
            :disabled="saving"
          >

            <span
              v-if="saving"
              class="spinner-border spinner-border-sm me-1"
            ></span>

            {{ isEdit ? 'Cập nhật' : 'Thêm mới' }}

          </button>

        </div>

      </form>

    </div>
  </div>
</template>
<script setup>
import { ref, reactive, computed, watch } from 'vue'

import permissionMenu from '@/api/permissionMenu'
import permissionService from '@/api/permissionService'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  isEdit: {
    type: Boolean,
    default: false
  },
  saving: {
    type: Boolean,
    default: false
  },
  initialData: {
    type: Object,
    default: () => ({})
  },
  allowedRoles: {
    type: Array,
    default: () => ([
      { value: 'ADMIN', label: 'Admin' },
      { value: 'STAFF', label: 'Staff' },
      { value: 'CUSTOMER', label: 'Khách hàng' }
    ])
  }
})

const emit = defineEmits([
  'close',
  'submit'
])

const buildDefaultForm = () => ({
  userId: null,
  fullName: '',
  username: '',
  email: '',
  phoneNumber: '',
  password: '',
  roleName: props.allowedRoles[0]?.value || 'STAFF',

  // các module được chọn
  moduleNames: []
})

const form = reactive(buildDefaultForm())

const isSubmitted = ref(false)

const showPassword = ref(false)

const modules = ref([])

watch(
  () => props.visible,
  async (visible) => {

    if (!visible) return

    isSubmitted.value = false
    
    showPassword.value = false

    // Load modules trước
    await loadModules()

    // Set form data
    Object.assign(
      form,
      buildDefaultForm(),
      props.initialData
    )

    if (!Array.isArray(form.moduleNames)) {
      form.moduleNames = []
    }

    // Load permissions của user SAU khi form đã set xong
    if (props.isEdit && props.initialData?.userId) {
      await loadUserPermissions(props.initialData.userId)
    }

  }
)

const errors = computed(() => {

  if (!isSubmitted.value) return {}

  const result = {}

  if (!form.fullName.trim()) {
    result.fullName = 'Vui lòng nhập họ tên'
  }

  if (!form.username.trim()) {
    result.username = 'Vui lòng nhập tên đăng nhập'
  }

  if (!form.email.trim()) {
    result.email = 'Vui lòng nhập email'
  }
  else if (
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)
  ) {
    result.email = 'Email không hợp lệ'
  }

  if (!form.phoneNumber.trim()) {
    result.phoneNumber = 'Vui lòng nhập số điện thoại'
  }
  else if (
    !/^[0-9]{9,11}$/.test(form.phoneNumber)
  ) {
    result.phoneNumber = 'Số điện thoại không hợp lệ'
  }

  if (!props.isEdit) {

    if (
      !form.password ||
      form.password.length < 6
    ) {
      result.password =
        'Mật khẩu tối thiểu 6 ký tự'
    }

  }

  return result

})

function onSubmit() {

  isSubmitted.value = true

  if (Object.keys(errors.value).length) {
    return
  }

  const payload = {
    ...form
  }

  if (props.isEdit) {
    delete payload.password
  }

  emit(
    'submit',
    payload
  )

}

/* ===========================
      LOAD MODULE
=========================== */

async function loadModules() {

  try {

    const res = await permissionService.getAllPermissions()

    const permissions = res.data.data || []
    
    const moduleNames = [
      ...new Set(
        permissions.map(item => item.module)
      )
    ]

    modules.value = moduleNames
      .map(getModuleInfo)
      .filter(Boolean)

  }
  catch (e) {

    console.error(e)

  }

}

// Load permissions hiện tại của user (khi sửa)
async function loadUserPermissions(userId) {
  try {
    const res = await permissionService.getUserPermissions(userId)
    const permissionIds = res.data.data || []

    // Lấy toàn bộ permission list để map id -> moduleName
    const allRes = await permissionService.getAllPermissions()
    const allPermissions = allRes.data.data || []

    // Từ permissionIds (Integer) -> lấy module tương ứng
    const userModuleNames = allPermissions
      .filter(p => permissionIds.includes(p.id))
      .map(p => p.module)

    // Lấy unique module names (vì 1 module có thể có nhiều permission con)
    form.moduleNames = [...new Set(userModuleNames)]

  } catch (e) {
    console.error('Không thể load permissions của user:', e)
  }
}

/* ===========================
      MENU -> MODULE
=========================== */

function getModuleInfo(moduleName) {

  for (const section of permissionMenu) {

    for (const module of section.modules) {

      if (module.key === moduleName) {

        return {
          key: module.key,
          name: module.name,
          icon: module.icon,
          section: section.section
        }

      }

      if (module.subs) {

        const sub = module.subs.find(
          x => x.key === moduleName
        )

        if (sub) {

          return {
            key: sub.key,
            name: sub.label,
            icon: module.icon,
            section: section.section
          }

        }

      }

    }

  }

  return null

}

/* ===========================
      GROUP THEO SECTION
=========================== */

const groupedModules = computed(() => {

  const result = []

  permissionMenu.forEach(section => {

    const list = modules.value.filter(
      item =>
        item.section === section.section
    )

    if (list.length) {

      result.push({

        section: section.section,

        modules: list

      })

    }

  })

  return result

})
</script>

<style scoped>
.modal-backdrop-custom{
  position:fixed;
  inset:0;
  z-index:1050;
  display:flex;
  align-items:center;
  justify-content:center;
  padding:18px;
  background:rgba(15, 23, 42, 0.46);
}

.user-modal{
  display:flex;
  flex-direction:column;
  width:min(100%, 760px);
  max-height:92vh;
  overflow:hidden;
  border-radius:16px;
  background:#ffffff;
}

.modal-head{
  display:flex;
  align-items:flex-start;
  justify-content:space-between;
  gap:14px;
  padding:24px;
  border-bottom:1px solid #f3d6e3;
}

.modal-head h2{
  margin:0;
  color:#f55d9b;
  font-size:1.3rem;
  font-weight:800;
}

.modal-head p{
  margin:4px 0 0;
  color:#6b7280;
  font-size:0.86rem;
}

.icon-button{
  display:inline-grid;
  width:36px;
  height:36px;
  place-items:center;
  border:1px solid #f3d6e3;
  border-radius:10px;
  background:#ffffff;
  color:#202636;
  cursor:pointer;
  transition:.18s;
}

.icon-button:hover{
  background:#fff0f7;
  color:#d63384;
}

.user-form{
  display:flex;
  flex-direction:column;
  gap:20px;
  overflow-y:auto;
  padding:24px;
}

.form-grid{
  display:grid;
  grid-template-columns:repeat(2, minmax(0, 1fr));
  gap:18px;
}

.form-label{
  display:block;
  font-size:12px;
  font-weight:700;
  text-transform:uppercase;
  color:#202636;
  letter-spacing:.3px;
  margin-bottom:8px;
}

.form-label span{
  color:#dc3545;
}

.finput{
  border:1px solid #f3d6e3;
  background:#fffafd;
  border-radius:10px;
  padding:11px 14px;
  color:#344054;
  font-size:14px;
  width:100%;
  box-sizing:border-box;
  transition:border-color .18s ease, box-shadow .18s ease, background-color .18s ease;
}

.finput::placeholder{
  color:#9ca3af;
}

.finput:hover{
  border-color:#efbdd2;
  background:#ffffff;
}

.finput:focus{
  border-color:#f55d9b;
  background:#ffffff;
  box-shadow:0 0 0 4px rgba(245, 93, 155, 0.1);
  outline:none;
}

.finput:disabled{
  background:#f1f5f9;
  color:#94a3b8;
  cursor:not-allowed;
}

.finput.is-invalid{
  border-color:#f5c2c7;
}

.invalid-feedback{
  margin-top:6px;
  color:#dc3545;
  font-size:12px;
}

.password-wrapper{
  position:relative;
}

.password-wrapper .finput{
  padding-right:42px;
}

.password-eye{
  position:absolute;
  right:14px;
  top:50%;
  transform:translateY(-50%);
  color:#9ca3af;
  cursor:pointer;
  font-size:15px;
}

select.finput{
  cursor:pointer;
}

.form-actions{
  display:flex;
  justify-content:flex-end;
  gap:10px;
  padding-top:8px;
  border-top:1px solid #f3d6e3;
}

.btn-soft{
  border:1px solid #f3d6e3;
  border-radius:10px;
  background:#ffffff;
  color:#202636;
  font-weight:700;
  padding:10px 18px;
}

.btn-soft:hover{
  border-color:#efbdd2;
  background:#fff0f7;
}

.btn-pink{
  border:none;
  border-radius:10px;
  background:#f55d9b;
  color:#ffffff;
  font-weight:700;
  padding:10px 20px;
}

.btn-pink:hover{
  background:#ec4d8d;
}

.btn-pink:disabled{
  background:#f3a9c6;
  cursor:not-allowed;
}

@media (max-width: 600px){
  .form-grid{
    grid-template-columns:1fr;
  }
}
/* =========================
   MODULE PERMISSION
========================= */

.permission-box{
  margin-top:8px;
  border:1px solid #f3d6e3;
  border-radius:14px;
  background:#fffafd;
  padding:18px;
}

.permission-header{
  margin-bottom:18px;
}

.permission-header h4{
  margin:0;
  font-size:16px;
  font-weight:700;
  color:#202636;
}

.permission-header p{
  margin:6px 0 0;
  font-size:13px;
  color:#6b7280;
}

.permission-group{
  margin-top:18px;
}

.permission-group:first-child{
  margin-top:0;
}

.permission-group-title{
  margin-bottom:12px;
  font-size:13px;
  font-weight:700;
  color:#f55d9b;
  text-transform:uppercase;
  letter-spacing:.4px;
}

.module-grid{
  display:grid;
  grid-template-columns:repeat(auto-fill,minmax(180px,1fr));
  gap:12px;
}

.module-card{
  position:relative;
  display:flex;
  align-items:center;
  gap:10px;
  padding:12px 14px;
  border:1px solid #f3d6e3;
  border-radius:12px;
  background:#fff;
  cursor:pointer;
  transition:.2s;
  user-select:none;
}

.module-card:hover{
  border-color:#f55d9b;
  background:#fff0f7;
}

.module-card input{
  width:18px;
  height:18px;
  accent-color:#f55d9b;
  cursor:pointer;
  flex-shrink:0;
}

.module-card i{
  font-size:18px;
  color:#f55d9b;
  width:20px;
  text-align:center;
}

.module-card span{
  font-size:14px;
  font-weight:600;
  color:#374151;
}

.module-card input:checked + i{
  color:#d63384;
}

.module-card:has(input:checked){
  border-color:#f55d9b;
  background:#ffeaf3;
  box-shadow:0 0 0 3px rgba(245,93,155,.08);
}

/* =========================
   RESPONSIVE
========================= */

@media(max-width:768px){

  .module-grid{
    grid-template-columns:1fr;
  }

}

@media(max-width:600px){

  .permission-box{
    padding:14px;
  }

  .module-card{
    padding:10px 12px;
  }

}
</style>