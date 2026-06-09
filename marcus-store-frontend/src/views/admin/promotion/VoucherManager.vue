<template>
  <div class="voucher-page">
    <Transition name="fade">
      <div v-if="toast.show" class="toast-alert" :class="toast.type">
        <strong>{{ toast.title }}</strong>
        <span>{{ toast.message }}</span>
      </div>
    </Transition>

    <div class="voucher-shell">
      <section class="voucher-hero">
        <div class="hero-title">
          <div class="hero-icon">
            <i class="bi bi-tags-fill"></i>
          </div>
          <div>
            <h1>Quản lý Voucher</h1>
            <p>Quản lý mã giảm giá, thời gian áp dụng và số lượng còn lại.</p>
          </div>
        </div>

        <button type="button" class="btn btn-primary-action" @click="openCreateModal">
          <i class="bi bi-plus-lg"></i>
          Thêm Voucher
        </button>
      </section>

      <section class="stats-grid">
        <article class="stat-card">
          <span>Tổng voucher</span>
          <strong>{{ stats.total }}</strong>
        </article>

        <article class="stat-card">
          <span>Đang sử dụng</span>
          <strong class="text-accent">{{ stats.active }}</strong>
        </article>

        <article class="stat-card">
          <span>Giảm theo %</span>
          <strong>{{ stats.percent }}</strong>
        </article>

        <article class="stat-card">
          <span>Giảm tiền</span>
          <strong>{{ stats.amount }}</strong>
        </article>
      </section>

      <section class="toolbar-panel">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-lg-5">
            <label class="form-label">Tìm kiếm</label>
            <div class="input-group">
              <span class="input-group-text">
                <i class="bi bi-search"></i>
              </span>
              <input
                v-model.trim="filters.keyword"
                type="text"
                class="form-control"
                placeholder="Tìm theo mã voucher"
              />
            </div>
          </div>

          <div class="col-12 col-md-6 col-lg-3">
            <label class="form-label">Loại giảm giá</label>
            <select v-model="filters.discountType" class="form-select">
              <option value="ALL">Tất cả</option>
              <option value="PERCENT">Giảm theo %</option>
              <option value="AMOUNT">Giảm tiền</option>
            </select>
          </div>

          <div class="col-12 col-md-6 col-lg-3">
            <label class="form-label">Trạng thái</label>
            <select v-model="filters.status" class="form-select">
              <option value="ALL">Tất cả</option>
              <option value="ACTIVE">Đang sử dụng</option>
              <option value="INACTIVE">Ngừng sử dụng</option>
            </select>
          </div>

          <div class="col-12 col-lg-1">
            <button type="button" class="btn btn-soft w-100" title="Xóa lọc" @click="resetFilters">
              <i class="bi bi-arrow-counterclockwise"></i>
            </button>
          </div>
        </div>
      </section>

      <section class="table-panel">
        <div class="table-responsive">
          <table class="table align-middle voucher-table mb-0">
            <thead>
            <tr>
              <th>ID</th>
              <th>Mã voucher</th>
              <th>Loại</th>
              <th>Giá trị</th>
              <th>Giảm tối đa</th>
              <th>Đơn tối thiểu</th>
              <th>Thời gian</th>
              <th>SL voucher</th>
              <th>Trạng thái</th>
              <th class="text-end">Thao tác</th>
            </tr>
            </thead>

            <tbody>
            <tr v-for="(voucher, index) in filteredVouchers"  :key="voucher.voucherId">
              <td class="fw-bold">#{{ index + 1}}</td>
              <td>
                <div class="voucher-code">{{ voucher.voucherCode }}</div>

              </td>
              <td>
                  <span class="type-badge" :class="voucher.discountType.toLowerCase()">
                    {{ formatDiscountType(voucher.discountType) }}
                  </span>
              </td>
              <td class="fw-semibold">{{ formatDiscountValue(voucher) }}</td>
              <td>{{ formatCurrency(voucher.maxDiscountAmount) }}</td>
              <td>{{ formatCurrency(voucher.minOrderValue) }}</td>
              <td>
                <div class="date-line">Từ: {{ formatDateTime(voucher.startDate) }}</div>
                <div class="date-line">Đến: {{ formatDateTime(voucher.endDate) }}</div>
              </td>
              <td>
                  <span :class="voucher.quantity === 0 ? 'text-danger fw-bold' : 'fw-bold'">
                    {{ voucher.quantity }}
                  </span>
              </td>
              <td>
                  <span class="status-badge" :class="{ inactive: !voucher.isActive }">
                    {{ voucher.isActive ? 'Đang sử dụng' : 'Ngừng sử dụng' }}
                  </span>
              </td>
              <td>
                <div class="d-flex justify-content-end gap-2">
                  <button
                    type="button"
                    class="icon-button"
                    title="Sửa voucher"
                    @click="openEditModal(voucher)"
                  >
                    <i class="bi bi-pencil-square"></i>
                  </button>
                  <button
                    type="button"
                    class="icon-button danger"
                    title="Xóa voucher"
                    @click="deleteVoucher(voucher)"
                  >
                    <i class="bi bi-trash3"></i>
                  </button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div v-if="filteredVouchers.length === 0" class="empty-state">
          <i class="bi bi-ticket-perforated"></i>
          <h3>Không có voucher nào</h3>
          <p>Hãy thêm voucher mới hoặc thay đổi bộ lọc.</p>
        </div>
      </section>
    </div>

    <div v-if="isModalOpen" class="modal-backdrop-custom" @click.self="closeModal">
      <div class="voucher-modal">
        <div class="modal-head">
          <div>
            <h2>{{ isEditing ? 'Sửa Voucher' : 'Thêm Voucher' }}</h2>
            <p>Voucher mới mặc định ở trạng thái đang sử dụng.</p>
          </div>
          <button type="button" class="icon-button" title="Đóng" @click="closeModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <form class="voucher-form" novalidate @submit.prevent="saveVoucher">
          <section class="form-section">
            <div class="section-title">
              <span>1</span>
              <div>
                <h3>Thông tin Voucher chính</h3>
                <p>Mã Voucher và trạng thái hiển thị của voucher.</p>
              </div>
            </div>
            <div class="modal-body-grid compact">
              <div>
                <label class="form-label">Mã voucher <span>*</span></label>
                <input
                  v-model.trim="form.voucher_code"
                  type="text"
                  class="form-control text-uppercase"
                  :class="{ 'is-invalid': isSubmitted && errors.voucher_code }"
                  placeholder="VD: SUMMER2026"
                />
                <div v-if="errors.voucher_code" class="invalid-feedback">
                  {{ errors.voucher_code }}
                </div>
              </div>

              <div>
                <label class="form-label">Trạng thái</label>
                <select v-model="form.is_active" :disabled="isZeroQuantity(form.quantity)"
                        class="form-select">
                  <option :value="true">Đang sử dụng</option>
                  <option :value="false">Ngừng sử dụng</option>
                </select>
                <small v-if="isZeroQuantity(form.quantity)" class="form-help text-danger">
                  Số lượng voucher = 0 nên trạng thái tự chuyển thành Ngừng sử dụng.
                </small>
              </div>

            </div>
          </section>

          <section class="form-section">
            <div class="section-title">
              <span>2</span>
              <div>
                <h3>Chi tiết Giảm giá</h3>
                <p>Chọn loại giảm giá theo % hoặc theo giá cố định.</p>
              </div>
            </div>

            <div class="wide-field">
              <label class="form-label">Loại giảm giá <span>*</span></label>
              <div class="discount-choice-grid">
                <label class="discount-choice"
                       :class="{ active: form.discount_type === 'PERCENT' }">
                  <input v-model="form.discount_type" type="radio" value="PERCENT"/>
                  <span class="discount-choice-text">
                  <strong>Giảm theo phần trăm</strong>

                </span>
                </label>

                <label class="discount-choice" :class="{ active: form.discount_type === 'AMOUNT' }">
                  <input v-model="form.discount_type" type="radio" value="AMOUNT"/>
                  <span class="discount-choice-text">
                  <strong>Giảm tiền trực tiếp</strong>

                </span>
                </label>
              </div>
            </div>

            <div>
              <label class="form-label">
                {{
                  form.discount_type === 'PERCENT' ? 'Giá trị giảm (%)' : 'Số tiền giảm trực tiếp'
                }}
                <span>*</span>
              </label>
              <div class="input-group">
                <input
                  v-model.number="form.discount_value"
                  type="number"
                  min="0"
                  :max="form.discount_type === 'PERCENT' ? 100 : undefined"
                  :step="form.discount_type === 'PERCENT' ? 1 : 1000"
                  class="form-control"
                  :class="{ 'is-invalid': isSubmitted && errors.discount_value }"
                  :placeholder="form.discount_type === 'PERCENT' ? 'Nhập %, VD: 10' : 'Nhập số tiền, VD: 50.000đ'"

                />
                <span class="input-group-text">{{
                    form.discount_type === 'PERCENT' ? '%' : 'đ'
                  }}</span>
                <div v-if="errors.discount_value" class="invalid-feedback">
                  {{ errors.discount_value }}
                </div>
              </div>
            </div>

            <div v-if="form.discount_type === 'PERCENT'">
              <label class="form-label">Điều kiện giảm tối đa <span>*</span></label>
              <div class="input-group">
                <input
                  v-model.number="form.max_discount_amount"
                  type="number"
                  min="0"
                  step="1000"
                  class="form-control"
                  :class="{ 'is-invalid': isSubmitted && errors.max_discount_amount }"
                  placeholder="Nhập số tiền giảm tối đa"
                />
                <span class="input-group-text">đ</span>
                <div v-if="isSubmitted && errors.max_discount_amount" class="invalid-feedback">
                  {{ errors.max_discount_amount }}
                </div>
              </div>
            </div>

          </section>

          <section class="form-section">
            <div class="section-title">
              <span>3</span>
              <div>
                <h3>Điều kiện Sử dụng</h3>
                <p>Giá trị đơn hàng tối thiểu và số lượt có thể dùng.</p>
              </div>
            </div>

            <div class="modal-body-grid compact">
              <div>
                <label class="form-label with-help">
                  Đơn tối thiểu
                  <i class="bi bi-question-circle"
                     title="Áp dụng cho đơn hàng có giá trị từ số tiền này trở lên."></i>
                </label>
                <div class="input-group">
                  <input
                    v-model.number="form.min_order_value"
                    type="number"
                    min="0"
                    step="1000"
                    class="form-control"
                    :class="{ 'is-invalid': isSubmitted && errors.min_order_value }"
                    placeholder="Nhập giá trị đơn tối thiểu"
                  />
                  <span class="input-group-text">đ</span>
                  <div v-if="errors.min_order_value" class="invalid-feedback">
                    {{ errors.min_order_value }}
                  </div>
                </div>
              </div>

              <div>
                <label class="form-label">Số lượng voucher / lượt dùng <span>*</span></label>
                <input
                  v-model.number="form.quantity"
                  type="number"
                  min="0"
                  step="1"
                  class="form-control"
                  :class="{ 'is-invalid': isSubmitted && errors.quantity }"
                  placeholder="Nhập số lượt dùng"
                />

                <div v-if="errors.quantity" class="invalid-feedback">
                  {{ errors.quantity }}
                </div>
              </div>

            </div>
          </section>

          <section class="form-section">
            <div class="section-title">
              <span>4</span>
              <div>
                <h3>Thời gian Sử dụng</h3>
                <p>Ngày kết thúc phải lớn hơn ngày bắt đầu.</p>
              </div>
            </div>

            <div class="modal-body-grid compact">
              <div>
                <label class="form-label">Ngày bắt đầu <span>*</span></label>
                <input
                  v-model="form.start_date"
                  type="datetime-local"
                  class="form-control"
                  :min="todayDateTime"
                  :class="{ 'is-invalid': isSubmitted && errors.start_date }"
                />
                <small class="form-help">Gợi ý: chọn từ hôm nay.</small>
                <div v-if="errors.start_date" class="invalid-feedback">
                  {{ errors.start_date }}
                </div>
              </div>

              <div>
                <label class="form-label">Ngày kết thúc <span>*</span></label>
                <input
                  v-model="form.end_date"
                  type="datetime-local"
                  class="form-control"
                  :min="form.start_date || todayDateTime"
                  :class="{ 'is-invalid': isSubmitted && (errors.end_date || errors.time) }"
                />
                <small class="form-help">Không được trước ngày bắt đầu.</small>
                <div v-if="errors.end_date" class="invalid-feedback">
                  {{ errors.end_date }}
                </div>
                <div v-if="errors.time" class="invalid-feedback d-block">
                  {{ errors.time }}
                </div>
              </div>

            </div>
          </section>

          <section v-if="isPreviewVisible" class="voucher-preview">
            <div>
              <span class="preview-eyebrow">Xem trước</span>
              <strong>{{ previewVoucher.code }}</strong>
              <p>{{ previewVoucher.discountText }}</p>
              <small>{{ previewVoucher.conditionText }}</small>
            </div>
          </section>

          <div class="form-actions">
            <button type="button" class="btn btn-soft" @click="resetForm">
              Làm mới
            </button>
            <button type="button" class="btn btn-preview"
                    @click="isPreviewVisible = !isPreviewVisible">
              Xem trước
            </button>
            <button type="submit" class="btn btn-primary-action">
              Lưu Voucher
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, reactive, ref, watch, onMounted} from 'vue'
import {storeToRefs} from 'pinia'
import {useVoucherStore} from '@/stores/voucherStore'
import '@/assets/css/Voucher.css'
import voucherApi from "@/api/voucherApi.js";

const voucherStore = useVoucherStore()

const {vouchers, loading, error} = storeToRefs(voucherStore)

onMounted(() => {
  voucherStore.fetchVouchers()
})
const isModalOpen = ref(false)
const isEditing = ref(false)
const isSubmitted = ref(false)
const isPreviewVisible = ref(false)

const toast = reactive({
  show: false,
  type: 'success',
  title: '',
  message: '',
})

const filters = reactive({
  keyword: '',
  discountType: 'ALL',
  status: 'ALL',
})


const defaultForm = {
  voucher_id: null,
  voucher_code: '',
  discount_value: null,
  discount_type: 'PERCENT',
  max_discount_amount: null,
  min_order_value: 0,
  start_date: '',
  end_date: '',
  quantity: null,
  is_active: true,
}

const form = reactive({...defaultForm})

const todayDateTime = computed(() => {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 16)
})

const previewVoucher = computed(() => {
  const code = form.voucher_code.trim().toUpperCase() || 'SUMMER2026'
  const discountValue = Number(form.discount_value || 0)
  const maxDiscount = Number(form.max_discount_amount || 0)
  const minOrder = Number(form.min_order_value || 0)

  return {
    code,
    discountText:
      form.discount_type === 'PERCENT'
        ? `Giảm ${discountValue || 0}%${maxDiscount > 0 ? `, tối đa ${formatCurrency(maxDiscount)}` : ''}`
        : `Giảm trực tiếp ${formatCurrency(discountValue)}`,
    conditionText: minOrder > 0 ? `Áp dụng cho đơn từ ${formatCurrency(minOrder)}` : 'Áp dụng cho mọi đơn hàng',
  }
})

const filteredVouchers = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()

  return vouchers.value.filter((voucher) => {
    const voucherCode = voucher.voucherCode?.toLowerCase() || ''

    const matchKeyword = voucherCode.includes(keyword)

    const matchType =
      filters.discountType === 'ALL' ||
      voucher.discountType === filters.discountType

    const matchStatus =
      filters.status === 'ALL' ||
      (filters.status === 'ACTIVE' && voucher.isActive) ||
      (filters.status === 'INACTIVE' && !voucher.isActive)

    return matchKeyword && matchType && matchStatus
  })
})

const stats = computed(() => ({
  total: vouchers.value.length,
  active: vouchers.value.filter((voucher) => voucher.isActive).length,
  percent: vouchers.value.filter((voucher) => voucher.discountType === 'PERCENT').length,
  amount: vouchers.value.filter((voucher) => voucher.discountType === 'AMOUNT').length,
}))

const errors = computed(() => {
  if (!isSubmitted.value) {
    return {}
  }

  const result = {}
  const voucherCode = form.voucher_code.trim().toUpperCase()

  if (!voucherCode) {
    result.voucher_code = 'Vui lòng nhập mã voucher'
  }

  const duplicated = vouchers.value.some((voucher) => {
    return (
      voucher.voucher_code.toLowerCase() === voucherCode.toLowerCase() &&
      voucher.voucher_id !== form.voucher_id
    )
  })

  if (voucherCode && duplicated) {
    result.voucher_code = 'Mã voucher đã tồn tại'
  }

  if (form.discount_value === null || form.discount_value === '' || Number(form.discount_value) <= 0) {
    result.discount_value = 'Giá trị giảm phải lớn hơn 0'
  }

  if (form.discount_type === 'PERCENT' && Number(form.discount_value) > 100) {
    result.discount_value = 'Giảm theo phần trăm không nên vượt quá 100%'
  }

  if (Number(form.min_order_value) < 0) {
    result.min_order_value = 'Đơn tối thiểu không được âm'
  }

  if (
    form.discount_type === 'PERCENT' &&
    (form.max_discount_amount === null ||
      form.max_discount_amount === '' ||
      Number(form.max_discount_amount) <= 0)
  ) {
    result.max_discount_amount = 'Bắt buộc nhập số tiền giảm tối đa'
  }

  if (form.discount_type === 'AMOUNT' && form.max_discount_amount !== null) {
    result.max_discount_amount = 'Với AMOUNT, max_discount_amount phải là null'
  }

  if (form.quantity === null || form.quantity === '' || Number(form.quantity) < 0) {
    result.quantity = 'Số lượng phải lớn hơn hoặc bằng 0'
  }

  if (!form.start_date) {
    result.start_date = 'Vui lòng chọn ngày bắt đầu'
  }

  if (!form.end_date) {
    result.end_date = 'Vui lòng chọn ngày kết thúc'
  }

  if (form.start_date && form.end_date) {
    const start = new Date(form.start_date)
    const end = new Date(form.end_date)

    if (end <= start) {
      result.time = 'Ngày kết thúc phải lớn hơn ngày bắt đầu'
    }
  }

  return result
})

watch(
  () => form.discount_type,
  (newType) => {
    if (newType === 'AMOUNT') {
      form.max_discount_amount = null
    }
  },
)

watch(
  () => form.quantity,
  (newQuantity) => {
    if (isZeroQuantity(newQuantity)) {
      form.is_active = false
    }
  },
)

function isZeroQuantity(quantity) {
  if (quantity === null || quantity === '') {
    return false
  }

  const numberQuantity = Number(quantity)

  return !Number.isNaN(numberQuantity) && numberQuantity <= 0
}

function showToast({type = 'success', title, message}) {
  toast.show = true
  toast.type = type
  toast.title = title
  toast.message = message

  window.setTimeout(() => {
    toast.show = false
  }, 2500)
}

function resetFilters() {
  filters.keyword = ''
  filters.discountType = 'ALL'
  filters.status = 'ALL'
}

function resetForm() {
  isSubmitted.value = false
  isPreviewVisible.value = false
  Object.assign(form, {...defaultForm})
  isEditing.value = false
  isSubmitted.value = false
}

function openCreateModal() {
  resetForm()
  isModalOpen.value = true
}

function openEditModal(voucher) {
  isSubmitted.value = false
  Object.assign(form, {
    voucher_id: voucher.voucher_id,
    voucher_code: voucher.voucher_code,
    discount_value: voucher.discount_value,
    discount_type: voucher.discount_type,
    max_discount_amount: voucher.max_discount_amount,
    min_order_value: voucher.min_order_value,
    start_date: voucher.start_date,
    end_date: voucher.end_date,
    quantity: voucher.quantity,
    is_active: voucher.is_active,
  })

  isEditing.value = true
  isModalOpen.value = true
}

function closeModal() {
  isModalOpen.value = false
}

function buildPayload() {
  const quantity = Number(form.quantity)

  return {
    voucher_id: form.voucher_id,
    voucher_code: form.voucher_code.trim().toUpperCase(),
    discount_value: Number(form.discount_value),
    discount_type: form.discount_type,
    max_discount_amount: form.discount_type === 'AMOUNT' ? null : Number(form.max_discount_amount),
    min_order_value: Number(form.min_order_value || 0),
    start_date: form.start_date,
    end_date: form.end_date,
    quantity,
    is_active: quantity > 0 ? Boolean(form.is_active) : false,
  }
}

function saveVoucher() {
  isSubmitted.value = true

  function saveVoucher() {
    isSubmitted.value = true

    if (Object.keys(errors.value).length > 0) {
      return
    }

    // phần lưu voucher bên dưới
  }

  if (Object.keys(errors.value).length > 0) {
    showToast({
      type: 'error',
      title: 'Chưa thể lưu voucher',
      message: 'Vui lòng kiểm tra lại các thông tin bị báo lỗi.',
    })
    return
  }

  const payload = buildPayload()
  const isUpdate = Boolean(payload.voucher_id)

  if (isUpdate) {
    const index = vouchers.value.findIndex((voucher) => voucher.voucher_id === payload.voucher_id)

    if (index !== -1) {
      vouchers.value[index] = payload
    }
  } else {
    const maxId = Math.max(...vouchers.value.map((voucher) => voucher.voucher_id), 0)

    vouchers.value.unshift({
      ...payload,
      voucher_id: maxId + 1,
    })
  }

  closeModal()

  showToast({
    type: 'success',
    title: isUpdate ? 'Cập nhật voucher thành công' : 'Thêm voucher thành công',
    message:
      payload.quantity <= 0
        ? 'Voucher đã được lưu và tự chuyển sang Ngừng sử dụng vì số lượng bằng 0.'
        : `Voucher ${payload.voucher_code} đã được lưu.`,
  })
}

async function deleteVoucher(voucher) {
  const confirmed = window.confirm(`Bạn có chắc muốn xóa voucher ${voucher.voucherCode} không?`)

  if (!confirmed) return
  const success = await voucherStore.deleteVoucherById(voucher.voucherId)
if(success){
  showToast({
    type: 'success',
    title: 'Xóa voucher thành công',
    message: `Voucher ${voucher.voucherCode} đã được xóa khỏi danh sách.`,
  })

}



}

function formatDiscountValue(voucher) {
  if (voucher.discountType === 'PERCENT') {
    return `${voucher.discountValue}%`
  }

  return formatCurrency(voucher.discountValue)
}

function formatDiscountType(discountType) {
  if (discountType === 'PERCENT') {
    return 'Giảm theo %'
  }

  if (discountType === 'AMOUNT') {
    return 'Giảm tiền'
  }

  return discountType || '-'
}

function formatCurrency(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(Number(value))
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<style scoped>

</style>
