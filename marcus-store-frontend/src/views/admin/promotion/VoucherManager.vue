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
              <tr v-for="voucher in filteredVouchers" :key="voucher.voucher_id">
                <td class="fw-bold">#{{ voucher.voucher_id }}</td>
                <td>
                  <div class="voucher-code">{{ voucher.voucher_code }}</div>

                </td>
                <td>
                  <span class="type-badge" :class="voucher.discount_type.toLowerCase()">
                    {{ formatDiscountType(voucher.discount_type) }}
                  </span>
                </td>
                <td class="fw-semibold">{{ formatDiscountValue(voucher) }}</td>
                <td>{{ formatCurrency(voucher.max_discount_amount) }}</td>
                <td>{{ formatCurrency(voucher.min_order_value) }}</td>
                <td>
                  <div class="date-line">Từ: {{ formatDateTime(voucher.start_date) }}</div>
                  <div class="date-line">Đến: {{ formatDateTime(voucher.end_date) }}</div>
                </td>
                <td>
                  <span :class="voucher.quantity === 0 ? 'text-danger fw-bold' : 'fw-bold'">
                    {{ voucher.quantity }}
                  </span>
                </td>
                <td>
                  <span class="status-badge" :class="{ inactive: !voucher.is_active }">
                    {{ voucher.is_active ? 'Đang sử dụng' : 'Ngừng sử dụng' }}
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

        <form class="modal-body-grid" novalidate @submit.prevent="saveVoucher">
          <div>
            <label class="form-label">Mã voucher <span>*</span></label>
            <input
              v-model.trim="form.voucher_code"
              type="text"
              class="form-control text-uppercase"
              :class="{ 'is-invalid': isSubmitted && errors.max_discount_amount }"
              placeholder="VD: SUMMER2026"
            />
            <div v-if="errors.voucher_code" class="invalid-feedback">
              {{ errors.voucher_code }}
            </div>
          </div>

          <div>
            <label class="form-label">Trạng thái</label>
            <select v-model="form.is_active" :disabled="isZeroQuantity(form.quantity)" class="form-select">
              <option :value="true">Đang sử dụng</option>
              <option :value="false">Ngừng sử dụng</option>
            </select>
            <small v-if="isZeroQuantity(form.quantity)" class="form-help text-danger">
              Số lượng voucher = 0 nên trạng thái tự chuyển thành Ngừng sử dụng.
            </small>
          </div>

          <div class="wide-field">
            <label class="form-label">Loại giảm giá <span>*</span></label>
            <div class="discount-choice-grid">
              <label class="discount-choice" :class="{ active: form.discount_type === 'PERCENT' }">
                <input v-model="form.discount_type" type="radio" value="PERCENT" />
                <span>
                  <strong>Giảm theo phần trăm</strong>
                  <small>Ví dụ giảm 10%, cần nhập số tiền giảm tối đa.</small>
                </span>
              </label>

              <label class="discount-choice" :class="{ active: form.discount_type === 'AMOUNT' }">
                <input v-model="form.discount_type" type="radio" value="AMOUNT" />
                <span>
                  <strong>Giảm tiền trực tiếp</strong>
                  <small>Ví dụ giảm 50.000đ, không cần giảm tối đa.</small>
                </span>
              </label>
            </div>
          </div>

          <div>
            <label class="form-label">Giá trị giảm <span>*</span></label>
            <div class="input-group">
              <input
                v-model.number="form.discount_value"
                type="number"
                min="0"
                :max="form.discount_type === 'PERCENT' ? 100 : undefined"
                :step="form.discount_type === 'PERCENT' ? 1 : 1000"
                class="form-control"
                :class="{ 'is-invalid': isSubmitted && errors.max_discount_amount }"

              />
              <span class="input-group-text">{{ form.discount_type === 'PERCENT' ? '%' : 'VNĐ' }}</span>
              <div v-if="errors.discount_value" class="invalid-feedback">
                {{ errors.discount_value }}
              </div>
            </div>
          </div>

          <div>
            <label class="form-label">Đơn tối thiểu</label>
            <input
              v-model.number="form.min_order_value"
              type="number"
              min="0"
              step="1000"
              class="form-control"
              :class="{ 'is-invalid': isSubmitted && errors.max_discount_amount }"
              placeholder="VD: 500000"
            />
            <div v-if="errors.min_order_value" class="invalid-feedback">
              {{ errors.min_order_value }}
            </div>
          </div>

          <div >
            <label class="form-label">Số tiền giảm tối đa <span>*</span></label>
            <input
              v-model.number="form.max_discount_amount"
              type="number"
              min="0"
              step="1000"
              class="form-control"
              :class="{ 'is-invalid': isSubmitted && errors.max_discount_amount }"
              placeholder="VD: 100000"
              :disabled="form.discount_type === 'AMOUNT'"
            />
            <div v-if="isSubmitted && errors.max_discount_amount" class="invalid-feedback">
              {{ errors.max_discount_amount }}
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
              :class="{ 'is-invalid': isSubmitted && errors.max_discount_amount }"
              placeholder="VD: 100 lượt dùng"
            />
            <small class="form-help">
              Đây là số lượt voucher có thể được sử dụng, không phải số lượng sản phẩm.
            </small>
            <div v-if="errors.quantity" class="invalid-feedback">
              {{ errors.quantity }}
            </div>
          </div>

          <div>
            <label class="form-label">Ngày bắt đầu <span>*</span></label>
            <input
              v-model="form.start_date"
              type="datetime-local"
              class="form-control"
              :class="{ 'is-invalid': isSubmitted && errors.max_discount_amount }"
            />
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
              :class="{ 'is-invalid': isSubmitted && errors.max_discount_amount }"
            />
            <div v-if="errors.end_date" class="invalid-feedback">
              {{ errors.end_date }}
            </div>
            <div v-if="errors.time" class="invalid-feedback d-block">
              {{ errors.time }}
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-soft" @click="resetForm">
              Làm mới
            </button>
            <button type="button" class="btn btn-soft" @click="closeModal">
              Hủy
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
import { computed, reactive, ref, watch } from 'vue'

const isModalOpen = ref(false)
const isEditing = ref(false)
const isSubmitted = ref(false)

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

const vouchers = ref([
  {
    voucher_id: 1,
    voucher_code: 'SUMMER2026',
    discount_value: 10,
    discount_type: 'PERCENT',
    max_discount_amount: 100000,
    min_order_value: 500000,
    start_date: '2026-06-05T08:00',
    end_date: '2026-06-10T23:59',
    quantity: 100,
    is_active: true,
  },
  {
    voucher_id: 2,
    voucher_code: 'FREESHIP50K',
    discount_value: 50000,
    discount_type: 'AMOUNT',
    max_discount_amount: null,
    min_order_value: 300000,
    start_date: '2026-06-01T00:00',
    end_date: '2026-06-30T23:59',
    quantity: 50,
    is_active: true,
  },
  {
    voucher_id: 3,
    voucher_code: 'WELCOME15',
    discount_value: 15,
    discount_type: 'PERCENT',
    max_discount_amount: 150000,
    min_order_value: 800000,
    start_date: '2026-06-02T00:00',
    end_date: '2026-06-15T23:59',
    quantity: 0,
    is_active: false,
  },
])

const defaultForm = {
  voucher_id: null,
  voucher_code: '',
  discount_value: null,
  discount_type: 'PERCENT',
  max_discount_amount: null,
  min_order_value: 0,
  start_date: '',
  end_date: '',
  quantity: 100,
  is_active: true,
}

const form = reactive({ ...defaultForm })

const filteredVouchers = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()

  return vouchers.value.filter((voucher) => {
    const matchKeyword = voucher.voucher_code.toLowerCase().includes(keyword)
    const matchType = filters.discountType === 'ALL' || voucher.discount_type === filters.discountType
    const matchStatus =
      filters.status === 'ALL' ||
      (filters.status === 'ACTIVE' && voucher.is_active) ||
      (filters.status === 'INACTIVE' && !voucher.is_active)

    return matchKeyword && matchType && matchStatus
  })
})

const stats = computed(() => ({
  total: vouchers.value.length,
  active: vouchers.value.filter((voucher) => voucher.is_active).length,
  percent: vouchers.value.filter((voucher) => voucher.discount_type === 'PERCENT').length,
  amount: vouchers.value.filter((voucher) => voucher.discount_type === 'AMOUNT').length,
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

function showToast({ type = 'success', title, message }) {
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
  Object.assign(form, { ...defaultForm })
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

function deleteVoucher(voucher) {
  const confirmed = window.confirm(`Bạn có chắc muốn xóa voucher ${voucher.voucher_code} không?`)

  if (!confirmed) return

  vouchers.value = vouchers.value.filter((item) => item.voucher_id !== voucher.voucher_id)

  showToast({
    type: 'success',
    title: 'Xóa voucher thành công',
    message: `Voucher ${voucher.voucher_code} đã được xóa khỏi danh sách.`,
  })
}

function formatDiscountValue(voucher) {
  if (voucher.discount_type === 'PERCENT') {
    return `${voucher.discount_value}%`
  }

  return formatCurrency(voucher.discount_value)
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
.voucher-page {
  position: relative;
  min-height: calc(100vh - 64px);
  background: #fff7fa;
  color: #202636;
}

.toast-alert {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 1100;
  display: grid;
  gap: 4px;
  width: min(360px, calc(100vw - 32px));
  padding: 14px 16px;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  background: #f0fdf4;
  color: #15803d;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.18);
}

.toast-alert.error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.toast-alert strong {
  font-size: 0.95rem;
}

.toast-alert span {
  font-size: 0.86rem;
}

.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.voucher-shell {
  width: min(100%, 1280px);
  margin: 0 auto;
  padding: 24px;
}

.voucher-hero,
.toolbar-panel,
.table-panel,
.voucher-modal,
.stat-card {
  border: 1px solid #f3d6e3;
  background: #ffffff;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.06);
}

.voucher-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding: 20px;
  border-radius: 8px;
}

.hero-title {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 8px;
  background: #f55d9b;
  color: #ffffff;
  font-size: 1.45rem;
}

.hero-title h1,
.modal-head h2 {
  margin: 0;
  color: #f55d9b;
  font-size: 1.45rem;
  font-weight: 800;
}

.hero-title p,
.modal-head p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 0.9rem;
}

.btn-primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 8px;
  background: #f55d9b;
  color: #ffffff;
  font-weight: 700;
  padding: 10px 16px;
}

.btn-primary-action:hover,
.btn-primary-action:focus {
  background: #ec4d8d;
  color: #ffffff;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.stat-card {
  padding: 18px;
  border-radius: 8px;
}

.stat-card span {
  display: block;
  color: #6b7280;
  font-size: 0.86rem;
  font-weight: 700;
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  font-size: 1.65rem;
  line-height: 1;
}

.text-accent {
  color: #f55d9b;
}

.toolbar-panel {
  margin-bottom: 18px;
  padding: 16px;
  border-radius: 8px;
}

.form-label {
  color: #b4557d;
  font-size: 0.76rem;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.form-label span {
  color: #dc3545;
}

.form-control,
.form-select,
.input-group-text {
  border-color: #f3d6e3;
  background-color: #fffafd;
}

.form-control:focus,
.form-select:focus {
  border-color: #f55d9b;
  box-shadow: 0 0 0 0.18rem rgba(245, 93, 155, 0.12);
}

.form-select:disabled {
  cursor: not-allowed;
  background-color: #f1f5f9;
  color: #94a3b8;
}

.form-help {
  display: block;
  margin-top: 5px;
  color: #6b7280;
  font-size: 0.76rem;
}

.btn-soft {
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  background: #ffffff;
  color: #202636;
  font-weight: 700;
}

.btn-soft:hover,
.btn-soft:focus {
  border-color: #efbdd2;
  background: #fff0f7;
}

.table-panel {
  overflow: hidden;
  border-radius: 8px;
}

.voucher-table {
  min-width: 1060px;
}

.voucher-table thead th {
  background: #fff0f7;
  color: #b4557d;
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
  white-space: nowrap;
}

.voucher-table td {
  color: #4b5563;
  font-size: 0.9rem;
}

.voucher-code {
  color: #202636;
  font-weight: 800;
}

.voucher-table small,
.date-line {
  color: #6b7280;
  font-size: 0.78rem;
}

.type-badge,
.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.76rem;
  font-weight: 800;
}

.type-badge.percent,
.status-badge {
  background: #ffe4ef;
  color: #d63384;
}

.type-badge.amount {
  background: #fff0d9;
  color: #9a5b00;
}

.status-badge.inactive {
  background: #f1f5f9;
  color: #64748b;
}

.icon-button {
  display: inline-grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  background: #ffffff;
  color: #202636;
}

.icon-button:hover,
.icon-button:focus {
  background: #fff0f7;
  color: #d63384;
}

.icon-button.danger {
  border-color: #f5c2c7;
  background: #fff5f6;
  color: #dc3545;
}

.icon-button.danger:hover,
.icon-button.danger:focus {
  background: #f8d7da;
}

.empty-state {
  padding: 42px 16px;
  text-align: center;
}

.empty-state i {
  color: #f55d9b;
  font-size: 2.4rem;
}

.empty-state h3 {
  margin: 12px 0 4px;
  font-size: 1.1rem;
  font-weight: 800;
}

.empty-state p {
  margin: 0;
  color: #6b7280;
}

.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  background: rgba(15, 23, 42, 0.46);
}

.voucher-modal {
  width: min(100%, 880px);
  max-height: 92vh;
  overflow: auto;
  border-radius: 8px;
}

.modal-head {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 20px;
  border-bottom: 1px solid #f3d6e3;
  background: #ffffff;
}

.modal-body-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  padding: 20px;
}

.wide-field,
.form-actions {
  grid-column: 1 / -1;
}

.discount-choice-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.discount-choice {
  display: flex;
  gap: 10px;
  min-height: 96px;
  padding: 14px;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
}

.discount-choice.active {
  border-color: #f55d9b;
  background: #ffe4ef;
}

.discount-choice input {
  margin-top: 4px;
  accent-color: #f55d9b;
}

.discount-choice strong,
.discount-choice small,
.amount-note strong,
.amount-note span {
  display: block;
}

.discount-choice strong {
  color: #202636;
}

.discount-choice small,
.amount-note span {
  margin-top: 4px;
  color: #6b7280;
}

.amount-note {
  min-height: 88px;
  padding: 14px;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  background: #fffafd;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 18px;
  border-top: 1px solid #f3d6e3;
}

@media (max-width: 992px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .voucher-shell {
    padding: 16px;
  }

  .voucher-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .stats-grid,
  .modal-body-grid,
  .discount-choice-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions .btn {
    width: 100%;
  }
}
</style>
