<template>
  <div class="container-fluid px-3 px-lg-4 py-3 product-spec-page">
    <section class="spec-page-header mb-4">
      <button class="back-button" type="button" title="Quay lại" @click="router.back()">
        <i class="bi bi-arrow-left"></i>
      </button>
      <div class="header-icon"><i class="bi bi-list-columns-reverse"></i></div>
      <div class="header-copy">
        <span class="header-eyebrow">CATALOG SẢN PHẨM</span>
        <h1>Thông số kỹ thuật sản phẩm</h1>
        <p v-if="product">
          Hoàn thiện thông tin của <strong>{{ product.productName }}</strong>
          <span v-if="product.categoryName"> trong danh mục {{ product.categoryName }}</span
          >.
        </p>
      </div>
    </section>

    <div v-if="pageLoading" class="spec-loading">
      <div class="spinner-border" role="status"></div>
      <p>Đang tải bộ thông số...</p>
    </div>

    <template v-else-if="product">
      <!-- Marcus sửa: khôi phục khối tiến độ và diễn giải rõ ranh giới giữa thông số, SKU và Kho. -->
      <section class="spec-overview mb-4">
        <div class="completion-card">
          <div class="completion-heading">
            <div>
              <span>Mức độ hoàn thiện thông số</span>
              <small>{{ filledCount }}/{{ totalSpecCount }} thông số đã có dữ liệu</small>
            </div>
            <strong>{{ completionPercent }}%</strong>
          </div>
          <div
            class="progress completion-progress"
            role="progressbar"
            aria-label="Mức độ hoàn thiện thông số kỹ thuật"
            :aria-valuenow="completionPercent"
            aria-valuemin="0"
            aria-valuemax="100"
          >
            <div class="progress-bar" :style="{ width: `${completionPercent}%` }"></div>
          </div>
          <p>Đây là chỉ số hỗ trợ nhập liệu, không phải điều kiện để mở bán sản phẩm.</p>
        </div>

        <div class="spec-guidance">
          <div class="guidance-icon"><i class="bi bi-info-circle"></i></div>
          <div>
            <strong>Phạm vi thông tin cần nhập tại đây</strong>
            <p>
              Admin chỉ nhập <b>thông số kỹ thuật dùng chung của sản phẩm</b>. Màu sắc, dung lượng,
              mã SKU, giá bán và khối lượng đóng gói được lấy từ từng SKU; số lượng tồn được quản lý
              theo module Kho nên không nhập lặp tại đây. Các trường thông số bên dưới là không bắt
              buộc, hãy bổ sung theo dữ liệu thực tế của sản phẩm.
            </p>
          </div>
        </div>
      </section>

      <section class="filter-card mb-4">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-lg-5">
            <label class="filter-label">TÌM THÔNG SỐ</label>
            <div class="input-wrapper">
              <i class="bi bi-search search-icon"></i>
              <input
                v-model="specSearch"
                type="search"
                class="form-control f-input"
                placeholder="Ví dụ: màn hình, camera, pin..."
              />
            </div>
          </div>
          <div class="col-12 col-md-6 col-lg-4">
            <label class="filter-label">TRẠNG THÁI DỮ LIỆU</label>
            <select v-model="specFilter" class="form-select f-input">
              <option value="all">Tất cả thông số</option>
              <option value="filled">Đã có giá trị</option>
              <option value="empty">Còn thiếu giá trị</option>
            </select>
          </div>
          <div class="col-12 col-md-6 col-lg-3">
            <button
              class="btn btn-pink w-100 filter-action"
              type="button"
              @click="openSpecificationManager"
            >
              <i class="bi bi-ui-checks-grid me-2"></i>Quản lý bộ thông số
            </button>
          </div>
        </div>
      </section>

      <section class="table-card spec-table-card">
        <div class="table-responsive">
          <table class="table align-middle mb-0 spec-table">
            <thead>
              <tr>
                <th class="th number-column">#</th>
                <th class="th">Thông số</th>
                <th class="th value-column">Giá trị</th>
                <th class="th text-center action-column">Thao tác</th>
              </tr>
            </thead>
            <tbody v-if="!filteredSpecs.length">
              <tr>
                <td colspan="4" class="empty-state">
                  <i class="bi bi-inbox"></i>
                  <strong>{{ emptyStateTitle }}</strong>
                  <span>{{ emptyStateMessage }}</span>
                </td>
              </tr>
            </tbody>
            <tbody v-for="group in groupedFilteredSpecs" v-else :key="group.categoryId">
              <tr class="scope-row">
                <td colspan="4">
                  <span :class="['scope-badge', { 'is-shared': !group.productCategory }]">
                    <i :class="group.productCategory ? 'bi bi-phone' : 'bi bi-diagram-3'"></i>
                    {{
                      group.productCategory
                        ? `Riêng cho ${group.categoryName}`
                        : `Dùng chung từ ${group.categoryName}`
                    }}
                  </span>
                  <small>{{ group.rows.length }} thông số</small>
                </td>
              </tr>
              <tr v-for="item in group.rows" :key="item.specAttributeId" class="spec-row">
                <td class="row-number">{{ rowNumber(item) }}</td>
                <td>
                  <div class="spec-name">{{ item.specAttributeName }}</div>
                  <div class="spec-meta">
                    <span>{{ dataTypeLabel(item.dataType) }}</span>
                    <span v-if="item.unit">Đơn vị: {{ item.unit }}</span>
                  </div>
                </td>
                <td>
                  <div v-if="item.dataType === 'boolean'" class="value-input-wrap">
                    <select v-model="item.valueText" class="form-select spec-value-input">
                      <option value="">Chưa cập nhật</option>
                      <option value="Có">Có</option>
                      <option value="Không">Không</option>
                    </select>
                  </div>
                  <div v-else class="value-input-wrap">
                    <input
                      v-model="item.valueText"
                      :type="item.dataType === 'number' ? 'number' : 'text'"
                      :step="item.dataType === 'number' ? 'any' : undefined"
                      maxlength="255"
                      class="form-control spec-value-input"
                      :placeholder="valuePlaceholder(item)"
                    />
                    <span v-if="item.unit" class="unit-addon">{{ item.unit }}</span>
                  </div>
                </td>
                <td class="text-center">
                  <div class="row-actions">
                    <button
                      class="act-btn clear-btn"
                      title="Xóa giá trị của sản phẩm này"
                      :disabled="!hasValue(item.valueText)"
                      @click="clearValue(item)"
                    >
                      <i class="bi bi-eraser"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <footer class="spec-save-bar">
          <div>
            <span :class="['save-state', { 'has-changes': isDirty }]">
              <i :class="isDirty ? 'bi bi-pencil-square' : 'bi bi-check-circle'"></i>
              {{ isDirty ? 'Có thay đổi chưa lưu' : 'Dữ liệu đã đồng bộ' }}
            </span>
            <small>Thông số đã lưu sẽ hiển thị ở trang sản phẩm và làm căn cứ cho Marcus AI.</small>
          </div>
          <button
            class="btn btn-pink save-button"
            :disabled="saving || !isDirty"
            @click="saveSpecs"
          >
            <span v-if="saving" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="bi bi-check2-circle me-2"></i>Lưu thay đổi
          </button>
        </footer>
      </section>
    </template>

    <div v-else class="spec-loading is-error">
      <i class="bi bi-exclamation-circle"></i>
      <p>Không tìm thấy sản phẩm.</p>
    </div>

    <BaseModal
      :visible="baseModal.visible"
      :show-confirm="baseModal.type === 'confirm'"
      :type="baseModal.type"
      :title="baseModal.title"
      :message="baseModal.message"
      @close="closeBaseModal"
      @confirm="onModalConfirm"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import BaseModal from '@/components/BaseModal.vue'
import api from '@/utils/api'
import '@/assets/css/Product.css'

const route = useRoute()
const router = useRouter()
const productId = Number(route.params.productId)

const specsApi = {
  getAttributes: (categoryId) => api.get('/admin/specs/attributes', { params: { categoryId } }),
  getProductSpecs: (id) => api.get(`/admin/specs/products/${id}`),
  saveProductSpecs: (payload) => api.put('/admin/specs/products', payload),
}

const productApi = { getById: (id) => api.get(`/admin/product/${id}`) }

const product = ref(null)
const pageLoading = ref(true)
const saving = ref(false)
const specAttributes = ref([])
const specRows = ref([])
const specSearch = ref('')
const specFilter = ref('all')
const initialSignature = ref('[]')

const baseModal = ref({ visible: false, type: 'error', title: '', message: '', onConfirm: null })
let leaveResolver = null

const hasValue = (value) => String(value ?? '').trim().length > 0
const rowsSignature = () =>
  JSON.stringify(
    specRows.value.map((row) => ({
      id: row.id,
      specAttributeId: row.specAttributeId,
      valueText: String(row.valueText ?? '').trim(),
    })),
  )

const isDirty = computed(() => rowsSignature() !== initialSignature.value)
// Marcus sửa: tiến độ chỉ phản ánh số trường đã nhập, không dùng để khóa thao tác bán hàng.
const totalSpecCount = computed(() => specRows.value.length)
const filledCount = computed(() => specRows.value.filter((row) => hasValue(row.valueText)).length)
const completionPercent = computed(() =>
  totalSpecCount.value ? Math.round((filledCount.value / totalSpecCount.value) * 100) : 0,
)

const filteredSpecs = computed(() => {
  const query = specSearch.value.trim().toLowerCase()
  return specRows.value.filter((row) => {
    if (
      query &&
      !String(row.specAttributeName || '')
        .toLowerCase()
        .includes(query)
    )
      return false
    if (specFilter.value === 'filled' && !hasValue(row.valueText)) return false
    if (specFilter.value === 'empty' && hasValue(row.valueText)) return false
    return true
  })
})

const groupedFilteredSpecs = computed(() => {
  const groups = new Map()
  for (const row of filteredSpecs.value) {
    const categoryId = row.categoryId ?? 0
    if (!groups.has(categoryId)) {
      groups.set(categoryId, {
        categoryId,
        categoryName: row.categoryName || 'Danh mục chưa xác định',
        productCategory: categoryId === product.value?.categoryId,
        rows: [],
      })
    }
    groups.get(categoryId).rows.push(row)
  }
  return [...groups.values()]
})

const emptyStateTitle = computed(() =>
  specAttributes.value.length ? 'Không có thông số phù hợp bộ lọc' : 'Danh mục chưa có bộ thông số',
)
const emptyStateMessage = computed(() =>
  specAttributes.value.length
    ? 'Hãy đổi từ khóa hoặc trạng thái dữ liệu.'
    : 'Mở “Quản lý bộ thông số” để cấu hình trường cho danh mục này.',
)

function showModal(type, title, message, onConfirm = null) {
  baseModal.value = { visible: true, type, title, message, onConfirm }
}

function closeBaseModal() {
  baseModal.value.visible = false
  if (leaveResolver) {
    leaveResolver(false)
    leaveResolver = null
  }
}

function onModalConfirm() {
  const callback = baseModal.value.onConfirm
  baseModal.value.visible = false
  if (callback) callback()
}

async function loadAll() {
  pageLoading.value = true
  try {
    const response = await productApi.getById(productId)
    product.value = response.data?.data ?? response.data
    if (!product.value?.categoryId) {
      showModal('error', 'Không thể quản lý thông số', 'Sản phẩm chưa được gắn với danh mục.')
      return
    }
    await fetchSpecs()
  } catch (error) {
    showModal(
      'error',
      'Không thể tải dữ liệu',
      error.response?.data?.message ?? 'Không tìm thấy sản phẩm.',
    )
  } finally {
    pageLoading.value = false
  }
}

async function fetchSpecs() {
  const [attributeResponse, valueResponse] = await Promise.all([
    specsApi.getAttributes(product.value.categoryId),
    specsApi.getProductSpecs(product.value.productId),
  ])
  specAttributes.value = attributeResponse.data?.data || []
  const currentValues = valueResponse.data?.data || []
  const valueByAttribute = new Map(currentValues.map((value) => [value.specAttributeId, value]))
  specRows.value = specAttributes.value.map((attribute) => {
    const existing = valueByAttribute.get(attribute.specAttributeId)
    return {
      id: existing?.id ?? null,
      specAttributeId: attribute.specAttributeId,
      specAttributeName: attribute.name,
      categoryId: attribute.categoryId,
      categoryName: attribute.categoryName,
      unit: attribute.unit,
      dataType: attribute.dataType || 'text',
      displayOrder: attribute.displayOrder ?? 0,
      valueText: existing?.valueText ?? '',
    }
  })
  initialSignature.value = rowsSignature()
}

function clearValue(item) {
  item.valueText = ''
}

async function saveSpecs() {
  saving.value = true
  try {
    await specsApi.saveProductSpecs({
      productId: product.value.productId,
      specs: specRows.value.map((row) => ({
        id: row.id || null,
        specAttributeId: row.specAttributeId,
        valueText: String(row.valueText ?? ''),
      })),
    })
    await fetchSpecs()
    showModal(
      'success',
      'Đã lưu thông số',
      'Thông tin mới đã sẵn sàng hiển thị trên trang sản phẩm và Marcus AI.',
    )
  } catch (error) {
    showModal(
      'error',
      'Không thể lưu thông số',
      error.response?.data?.message ?? 'Vui lòng kiểm tra lại dữ liệu.',
    )
  } finally {
    saving.value = false
  }
}

// Marcus sửa: cấu trúc thông số chỉ thay đổi tại màn quản lý tập trung; màn này
// chỉ nhập giá trị của sản phẩm để tránh vô tình ảnh hưởng toàn danh mục.
function openSpecificationManager() {
  if (isDirty.value) {
    showModal(
      'error',
      'Còn thay đổi chưa lưu',
      'Hãy lưu giá trị sản phẩm trước khi mở bộ thông số.',
    )
    return
  }
  router.push({ name: 'SpecificationSetManager' })
}

function rowNumber(item) {
  return filteredSpecs.value.findIndex((row) => row.specAttributeId === item.specAttributeId) + 1
}

function dataTypeLabel(type) {
  return { number: 'Dạng số', boolean: 'Có / Không', text: 'Văn bản' }[type] || 'Văn bản'
}

function valuePlaceholder(item) {
  if (item.dataType === 'number')
    return item.unit ? `Chỉ nhập số, ví dụ 5000` : 'Nhập một giá trị số'
  return `Nhập ${String(item.specAttributeName || 'giá trị').toLowerCase()}`
}

onBeforeRouteLeave(() => {
  if (!isDirty.value) return true
  return new Promise((resolve) => {
    leaveResolver = resolve
    showModal(
      'confirm',
      'Rời trang khi chưa lưu?',
      'Các giá trị thông số vừa thay đổi sẽ bị mất.',
      () => {
        const resolver = leaveResolver
        leaveResolver = null
        if (resolver) resolver(true)
      },
    )
  })
})

onMounted(async () => {
  await loadAll()
})
</script>

<style scoped>
.product-spec-page {
  color: #102341;
}
.spec-page-header {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px 28px;
  border: 1px solid #f2c7dc;
  border-radius: 22px;
  background: linear-gradient(135deg, #fff6fb 0%, #fff 54%, #eef6ff 100%);
  box-shadow: 0 12px 28px rgba(49, 74, 112, 0.08);
}
.back-button {
  width: 46px;
  height: 46px;
  border: 1px solid #d8e2ef;
  border-radius: 14px;
  background: #fff;
  color: #355171;
  font-size: 18px;
}
.header-icon {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  border-radius: 17px;
  color: #fff;
  font-size: 25px;
  background: linear-gradient(145deg, #ee438c, #d72470);
  box-shadow: 0 10px 20px rgba(215, 36, 112, 0.22);
}
.header-copy h1 {
  margin: 2px 0 3px;
  font-size: clamp(24px, 2.5vw, 34px);
  font-weight: 800;
}
.header-copy p {
  margin: 0;
  color: #66758b;
}
.header-eyebrow {
  color: #c51f64;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}
.spec-loading {
  min-height: 300px;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 12px;
  color: #68788d;
}
.spec-loading .spinner-border {
  color: #e42c79;
}
.spec-loading.is-error i {
  color: #d72d43;
  font-size: 34px;
}
.spec-overview {
  display: grid;
  grid-template-columns: minmax(320px, 0.85fr) minmax(420px, 1.5fr);
  gap: 16px;
}
.completion-card,
.spec-guidance {
  border: 1px solid #e4eaf3;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(33, 57, 91, 0.06);
}
.completion-card {
  padding: 20px 22px;
}
.completion-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}
.completion-heading span,
.spec-guidance strong {
  color: #243b5c;
  font-weight: 800;
}
.completion-heading small {
  display: block;
  margin-top: 4px;
  color: #7b899c;
}
.completion-heading > strong {
  color: #7a35dc;
  font-size: 27px;
  line-height: 1;
}
.completion-progress {
  height: 9px;
  margin-top: 16px;
  border-radius: 999px;
  background: #edf1f7;
}
.completion-progress .progress-bar {
  border-radius: inherit;
  background: linear-gradient(90deg, #e83f85, #7b3fe4);
  transition: width 0.25s ease;
}
.completion-card > p {
  margin: 10px 0 0;
  color: #7b899c;
  font-size: 12px;
}
.spec-guidance {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px 22px;
  background: linear-gradient(135deg, #fff8fb, #f5f9ff);
}
.guidance-icon {
  display: grid;
  place-items: center;
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: #c51f64;
  background: #ffe7f1;
  font-size: 18px;
}
.spec-guidance p {
  margin: 6px 0 0;
  color: #62728a;
  line-height: 1.6;
}
.spec-guidance b {
  color: #344f73;
}
.filter-action {
  min-height: 48px;
  border-radius: 12px;
}
.spec-table-card {
  overflow: hidden;
  border: 1px solid #e2e9f3;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(33, 57, 91, 0.07);
}
.spec-table thead th {
  padding-top: 16px;
  padding-bottom: 16px;
  background: #fff4f9;
  border-bottom-color: #f2ccdc;
}
.number-column {
  width: 62px;
}
.value-column {
  width: 43%;
}
.action-column {
  width: 90px;
}
.scope-row td {
  padding: 13px 18px;
  background: #f7faff;
  border-bottom: 1px solid #e3ebf5;
}
.scope-row small {
  margin-left: 10px;
  color: #8090a5;
}
.scope-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 10px;
  border-radius: 9px;
  color: #a71958;
  background: #ffedf5;
  font-size: 12px;
  font-weight: 800;
}
.scope-badge.is-shared {
  color: #1d62ae;
  background: #eaf4ff;
}
.spec-row td {
  padding: 15px 18px;
  border-bottom-color: #edf1f6;
}
.row-number {
  color: #8090a6;
  font-weight: 700;
}
.spec-name {
  color: #172c4d;
  font-weight: 800;
}
.spec-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 6px;
}
.spec-meta span {
  padding: 3px 7px;
  border-radius: 7px;
  color: #6c7a8e;
  background: #f1f4f8;
  font-size: 11px;
}
.value-input-wrap {
  display: flex;
  align-items: stretch;
}
.spec-value-input {
  min-height: 44px;
  border-color: #d9e2ee;
  border-radius: 11px;
}
.value-input-wrap .spec-value-input:has(+ .unit-addon) {
  border-radius: 11px 0 0 11px;
}
.unit-addon {
  display: grid;
  place-items: center;
  min-width: 58px;
  padding: 0 12px;
  border: 1px solid #d9e2ee;
  border-left: 0;
  border-radius: 0 11px 11px 0;
  color: #a71958;
  background: #fff4f8;
  font-weight: 700;
}
.row-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}
.clear-btn {
  color: #7e5fd1;
  background: #f1edff;
  border: 1px solid #ded4ff;
}
.clear-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.empty-state {
  padding: 60px 20px !important;
  text-align: center;
}
.empty-state i {
  display: block;
  margin-bottom: 8px;
  color: #b5c1cf;
  font-size: 32px;
}
.empty-state strong,
.empty-state span {
  display: block;
}
.empty-state span {
  margin-top: 4px;
  color: #8290a2;
}
.spec-save-bar {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 17px 20px;
  border-top: 1px solid #e2e9f2;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
}
.spec-save-bar > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.spec-save-bar small {
  color: #7a899d;
}
.save-state {
  color: #19845d;
  font-weight: 800;
}
.save-state.has-changes {
  color: #d02a69;
}
.save-state i {
  margin-right: 7px;
}
.save-button {
  min-width: 170px;
  min-height: 46px;
  border-radius: 12px;
}
@media (max-width: 767.98px) {
  .spec-overview {
    grid-template-columns: 1fr;
  }
  .spec-guidance {
    padding: 17px;
  }
  .spec-page-header {
    align-items: flex-start;
    padding: 18px;
  }
  .header-icon {
    display: none;
  }
  .header-copy h1 {
    font-size: 23px;
  }
  .spec-table {
    min-width: 760px;
  }
  .spec-save-bar {
    align-items: stretch;
    flex-direction: column;
  }
  .save-button {
    width: 100%;
  }
}
</style>
