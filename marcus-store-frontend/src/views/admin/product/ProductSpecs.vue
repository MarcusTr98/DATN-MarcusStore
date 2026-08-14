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
            <button class="btn btn-pink w-100 filter-action" type="button" @click="openAttrForm()">
              <i class="bi bi-sliders me-2"></i>Thêm vào bộ thông số
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
                      class="act-btn edit-btn"
                      title="Sửa cấu trúc thông số"
                      @click="openAttrForm(item)"
                    >
                      <i class="bi bi-pencil"></i>
                    </button>
                    <button
                      class="act-btn clear-btn"
                      title="Xóa giá trị của sản phẩm này"
                      :disabled="!hasValue(item.valueText)"
                      @click="clearValue(item)"
                    >
                      <i class="bi bi-eraser"></i>
                    </button>
                    <button
                      class="act-btn hide-btn"
                      title="Xóa khỏi bộ thông số"
                      @click="deleteAttribute(item)"
                    >
                      <i class="bi bi-trash"></i>
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

    <div class="modal fade" id="specAttrFormModal" tabindex="-1" ref="attrFormEl">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4 border-0 shadow-lg">
          <div class="modal-header border-0 pb-0">
            <div>
              <span class="modal-eyebrow">BỘ THÔNG SỐ DANH MỤC</span>
              <h5 class="fw-bold mb-0">
                {{ attrForm.id ? 'Cập nhật thông số' : 'Thêm thông số mới' }}
              </h5>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body pt-3">
            <div class="scope-warning">
              <i class="bi bi-info-circle"></i>
              Thay đổi cấu trúc tại đây áp dụng cho các sản phẩm thuộc phạm vi đã chọn, không chỉ
              sản phẩm hiện tại.
            </div>
            <div class="mb-3">
              <label class="flabel">Phạm vi áp dụng <span class="text-danger">*</span></label>
              <select
                v-model.number="attrForm.categoryId"
                class="form-select finput"
                :disabled="Boolean(attrForm.id)"
              >
                <option
                  v-for="scope in categoryScopes"
                  :key="scope.categoryId"
                  :value="scope.categoryId"
                >
                  {{
                    scope.productCategory
                      ? `Riêng ${scope.categoryName}`
                      : `Dùng chung: ${scope.categoryName}`
                  }}
                </option>
              </select>
              <small v-if="attrForm.id" class="form-note"
                >Không thể chuyển phạm vi của thông số đã có dữ liệu.</small
              >
            </div>
            <div class="mb-3">
              <label class="flabel">Tên thông số <span class="text-danger">*</span></label>
              <input
                v-model="attrForm.name"
                maxlength="100"
                class="form-control finput"
                placeholder="VD: Dung lượng pin"
              />
            </div>
            <div class="row g-3">
              <div class="col-6">
                <label class="flabel">Đơn vị</label>
                <input
                  v-model="attrForm.unit"
                  maxlength="20"
                  class="form-control finput"
                  placeholder="mAh, inch..."
                />
              </div>
              <div class="col-6">
                <label class="flabel">Kiểu dữ liệu</label>
                <select v-model="attrForm.dataType" class="form-select finput">
                  <option value="text">Văn bản</option>
                  <option value="number">Số</option>
                  <option value="boolean">Có / Không</option>
                </select>
              </div>
            </div>
            <div class="mt-3">
              <label class="flabel">Thứ tự hiển thị</label>
              <input
                v-model.number="attrForm.displayOrder"
                type="number"
                min="0"
                class="form-control finput"
              />
            </div>
          </div>
          <div class="modal-footer border-0 pt-1">
            <button class="btn btn-outline-secondary rounded-3" data-bs-dismiss="modal">Hủy</button>
            <button class="btn btn-pink rounded-3" :disabled="savingAttr" @click="saveAttribute">
              <span v-if="savingAttr" class="spinner-border spinner-border-sm me-1"></span>
              {{ attrForm.id ? 'Cập nhật' : 'Thêm vào bộ thông số' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { Modal } from 'bootstrap'
import BaseModal from '@/components/BaseModal.vue'
import api from '@/utils/api'
import '@/assets/css/Product.css'

const route = useRoute()
const router = useRouter()
const productId = Number(route.params.productId)

const specsApi = {
  getAttributes: (categoryId) => api.get('/admin/specs/attributes', { params: { categoryId } }),
  getCategoryScopes: (categoryId) =>
    api.get('/admin/specs/category-scopes', { params: { categoryId } }),
  createAttribute: (payload) => api.post('/admin/specs/attributes', payload),
  updateAttribute: (id, payload) => api.put(`/admin/specs/attributes/${id}`, payload),
  deleteAttribute: (id) => api.delete(`/admin/specs/attributes/${id}`),
  getProductSpecs: (id) => api.get(`/admin/specs/products/${id}`),
  saveProductSpecs: (payload) => api.put('/admin/specs/products', payload),
}

const productApi = { getById: (id) => api.get(`/admin/product/${id}`) }

const product = ref(null)
const pageLoading = ref(true)
const saving = ref(false)
const savingAttr = ref(false)
const specAttributes = ref([])
const specRows = ref([])
const categoryScopes = ref([])
const specSearch = ref('')
const specFilter = ref('all')
const initialSignature = ref('[]')

const attrFormEl = ref(null)
let bsAttrForm = null
const attrForm = ref({
  id: null,
  categoryId: null,
  name: '',
  unit: '',
  dataType: 'text',
  displayOrder: 0,
})
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
    : 'Bấm “Thêm vào bộ thông số” để cấu hình trường đầu tiên.',
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

function ensureValuesSaved() {
  if (!isDirty.value) return true
  showModal(
    'error',
    'Còn thay đổi chưa lưu',
    'Hãy lưu giá trị sản phẩm trước khi thay đổi cấu trúc bộ thông số.',
  )
  return false
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
  const [attributeResponse, valueResponse, scopeResponse] = await Promise.all([
    specsApi.getAttributes(product.value.categoryId),
    specsApi.getProductSpecs(product.value.productId),
    specsApi.getCategoryScopes(product.value.categoryId),
  ])
  specAttributes.value = attributeResponse.data?.data || []
  categoryScopes.value = scopeResponse.data?.data || []
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

function openAttrForm(row = null) {
  if (!ensureValuesSaved()) return
  attrForm.value = row
    ? {
        id: row.specAttributeId,
        categoryId: row.categoryId,
        name: row.specAttributeName,
        unit: row.unit || '',
        dataType: row.dataType || 'text',
        displayOrder: row.displayOrder ?? 0,
      }
    : {
        id: null,
        categoryId: product.value.categoryId,
        name: '',
        unit: '',
        dataType: 'text',
        displayOrder: 0,
      }
  bsAttrForm.show()
}

async function saveAttribute() {
  if (!attrForm.value.name?.trim()) {
    showModal('error', 'Thiếu tên thông số', 'Tên thông số không được để trống.')
    return
  }
  savingAttr.value = true
  try {
    const payload = {
      categoryId: attrForm.value.categoryId,
      name: attrForm.value.name.trim(),
      unit: attrForm.value.unit?.trim() || null,
      dataType: attrForm.value.dataType || 'text',
      displayOrder: attrForm.value.displayOrder ?? 0,
    }
    if (attrForm.value.id) await specsApi.updateAttribute(attrForm.value.id, payload)
    else await specsApi.createAttribute(payload)
    bsAttrForm.hide()
    await fetchSpecs()
    showModal(
      'success',
      'Đã cập nhật bộ thông số',
      'Cấu trúc thông số đã được áp dụng đúng phạm vi danh mục.',
    )
  } catch (error) {
    showModal(
      'error',
      'Không thể lưu thông số',
      error.response?.data?.message ?? 'Vui lòng kiểm tra lại dữ liệu.',
    )
  } finally {
    savingAttr.value = false
  }
}

function deleteAttribute(row) {
  if (!ensureValuesSaved()) return
  showModal(
    'confirm',
    'Xóa khỏi bộ thông số?',
    `Thông số “${row.specAttributeName}” áp dụng cho các sản phẩm thuộc ${row.categoryName}. Chỉ có thể xóa khi chưa sản phẩm nào sử dụng.`,
    async () => {
      try {
        await specsApi.deleteAttribute(row.specAttributeId)
        await fetchSpecs()
        showModal('success', 'Đã xóa thông số', 'Bộ thông số danh mục đã được cập nhật.')
      } catch (error) {
        showModal(
          'error',
          'Không thể xóa thông số',
          error.response?.data?.message ?? 'Thông số đang được sử dụng.',
        )
      }
    },
  )
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
  if (attrFormEl.value) bsAttrForm = new Modal(attrFormEl.value)
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
.header-eyebrow,
.modal-eyebrow {
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
  width: 155px;
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
.scope-warning {
  display: flex;
  gap: 9px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 12px;
  color: #4f627d;
  background: #eef6ff;
  font-size: 13px;
  line-height: 1.45;
}
.form-note {
  display: block;
  margin-top: 5px;
  color: #8290a1;
}
@media (max-width: 767.98px) {
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
