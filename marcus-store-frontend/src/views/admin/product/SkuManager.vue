<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon-box">
          <i class="fa-solid fa-layer-group"></i>
        </div>
        <div class="header-text-group">
          <p class="breadcrumb-text">Sản phẩm</p>
          <h1 class="page-title">Tạo Biến thể SKU</h1>
          <p class="page-subtitle">Tạo và quản lý các biến thể SKU theo thuộc tính sản phẩm</p>
        </div>
      </div>
      <div class="header-right">
        <div class="step-indicator">
          <div class="step" :class="{ active: currentStep >= 1, done: currentStep > 1 }">
            <span class="step-num">1</span><span class="step-label">Chọn SP</span>
          </div>
          <div class="step-line" :class="{ active: currentStep > 1 }"></div>
          <div class="step" :class="{ active: currentStep >= 2, done: currentStep > 2 }">
            <span class="step-num">2</span><span class="step-label">Chọn thuộc tính</span>
          </div>
          <div class="step-line" :class="{ active: currentStep > 2 }"></div>
          <div class="step" :class="{ active: currentStep >= 3 }">
            <span class="step-num">3</span><span class="step-label">Điền & Lưu</span>
          </div>
        </div>
      </div>
    </div>

    <!-- BƯỚC 1: CHỌN SẢN PHẨM -->
    <div class="card step-card" :class="{ collapsed: currentStep > 1 }">
      <div class="card-header-row" @click="currentStep > 1 && (currentStep = 1)">
        <div class="card-title-group">
          <span class="step-badge">01</span>
          <div>
            <h3 class="card-title">Sản phẩm gốc</h3>
            <p class="card-subtitle">Chọn sản phẩm bạn muốn tạo biến thể SKU</p>
          </div>
        </div>
        <div v-if="selectedProduct" class="selected-summary">
          <span class="selected-tag">{{ selectedProduct.productName }}</span>
        </div>
      </div>

      <div class="card-body" v-show="currentStep === 1">
        <div class="filter-toolbar">
          <div class="search-box">
            <input
              v-model="searchQuery"
              placeholder="Nhập tên sản phẩm cần tìm..."
              @keyup.enter="handleSearch"
            />
            <button class="btn-search" @click="handleSearch" title="Tìm kiếm">
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </button>
          </div>

          <div class="filter-box">
            <select v-model="filterStatus" @change="handleFilterChange" class="status-select">
              <option value="all">Tất cả sản phẩm</option>
              <option value="no_sku">Chưa có biến thể</option>
              <option value="has_sku">Đã có biến thể</option>
            </select>
          </div>
        </div>

        <div class="product-select-grid">
          <div
            v-for="p in products"
            :key="p.productId"
            class="product-option"
            :class="{ active: selectedProductId === p.productId }"
            @click="selectProduct(p)"
          >
            <div class="product-option-check">
              <svg
                v-if="selectedProductId === p.productId"
                width="12"
                height="12"
                viewBox="0 0 12 12"
                fill="none"
              >
                <path
                  d="M2 6l3 3 5-5"
                  stroke="#fff"
                  stroke-width="1.8"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </div>
            <div class="product-option-thumb">
              {{ p.productName ? p.productName.charAt(0).toUpperCase() : 'P' }}
            </div>
            <div class="product-option-info">
              <p class="product-option-name">{{ p.productName }}</p>
              <p class="product-option-brand">{{ p.brand }}</p>
            </div>
          </div>
          <div v-if="products.length === 0" class="empty-state">
            <p>Không tìm thấy sản phẩm nào phù hợp.</p>
          </div>
        </div>

        <div class="pagination-wrapper" v-if="totalPages > 1">
          <button
            class="btn-page"
            :disabled="currentPage === 0"
            @click="changePage(currentPage - 1)"
          >
            ← Trước
          </button>
          <span class="page-info">Trang {{ currentPage + 1 }} / {{ totalPages }}</span>
          <button
            class="btn-page"
            :disabled="currentPage >= totalPages - 1"
            @click="changePage(currentPage + 1)"
          >
            Sau →
          </button>
        </div>

        <div class="card-footer-row">
          <button class="btn-primary" :disabled="!selectedProductId" @click="currentStep = 2">
            Tiếp tục →
          </button>
        </div>
      </div>
    </div>

    <!-- BẢNG SKU ĐÃ TỒN TẠI TRONG CSDL -->
    <div class="card step-card" v-if="selectedProductId && existingSkus.length > 0">
      <div class="card-header-row no-pointer">
        <div class="card-title-group">
          <span class="step-badge" style="background: #ecfdf5; color: #10b981">
            <i class="fa-solid fa-database"></i>
          </span>
          <div>
            <h3 class="card-title">SKU Đang Hoạt Động ({{ existingSkus.length }})</h3>
            <p class="card-subtitle">
              Các biến thể đã tồn tại. Hệ thống sẽ khóa nếu bạn tạo trùng thuộc tính với bảng này!
            </p>
          </div>
        </div>
      </div>
      <div class="card-body">
        <div class="sku-table-wrapper">
          <table class="sku-table">
            <thead>
              <tr>
                <th class="th-variant">Tổ hợp biến thể</th>
                <th class="th-sku">Mã SKU</th>
                <th class="th-price">Giá bán (₫)</th>
                <th class="th-stock">Tồn kho</th>
                <th class="th-del">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(sku, idx) in existingSkus" :key="sku.skuId" class="sku-row">
                <td>
                  <div class="variant-name-cell">
                    <span class="variant-label">
                      {{
                        sku.attributeValues
                          ? sku.attributeValues.map((v) => v.valueString).join(' / ')
                          : '---'
                      }}
                    </span>
                  </div>
                </td>
                <td>
                  <code style="color: #db2777; font-weight: bold">{{ sku.skuCode }}</code>
                </td>
                <td>{{ formatMoney(sku.price) }}</td>
                <td>{{ sku.stockQuantity }}</td>
                <td>
                  <button
                    class="btn-row-del"
                    @click="deleteExistingSku(sku.skuId, idx)"
                    title="Vô hiệu hóa SKU"
                  >
                    <i class="fa-solid fa-trash"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- BƯỚC 2: CHỌN THUỘC TÍNH -->
    <div class="card step-card" :class="{ collapsed: currentStep !== 2, locked: currentStep < 2 }">
      <div class="card-header-row" @click="currentStep > 2 && (currentStep = 2)">
        <div class="card-title-group">
          <span class="step-badge" :class="{ dim: currentStep < 2 }">02</span>
          <div>
            <h3 class="card-title">Chọn Thuộc tính & Giá trị</h3>
            <p class="card-subtitle">Tick vào các giá trị muốn tạo biến thể</p>
          </div>
        </div>
        <div v-if="currentStep > 2" class="selected-summary">
          <span class="selected-tag">{{ getTotalSelected() }} giá trị đã chọn</span>
          <span class="edit-hint">Nhấn để thay đổi</span>
        </div>
      </div>

      <div class="card-body" v-show="currentStep === 2">
        <div class="attributes-section">
          <div v-for="attr in attributes" :key="attr.attributeId" class="attribute-block">
            <div class="attribute-block-header">
              <label class="attribute-toggle">
                <input
                  type="checkbox"
                  :checked="isAttributeSelected(attr.attributeId)"
                  @change="toggleAttribute(attr.attributeId)"
                />
                <span class="toggle-track"><span class="toggle-thumb"></span></span>
                <span class="attribute-block-name">{{ attr.attributeName }}</span>
              </label>
              <span class="attr-value-count">
                {{ getSelectedValueCount(attr.attributeId) }}/{{
                  getAttrValues(attr.attributeId).length
                }}
                đã chọn
              </span>
            </div>

            <div class="values-tag-row" v-if="isAttributeSelected(attr.attributeId)">
              <button
                v-for="val in getAttrValues(attr.attributeId)"
                :key="val.valueId"
                class="value-tag"
                :class="{ active: isValueSelected(val.valueId) }"
                @click="toggleValue(val)"
              >
                <span class="value-tag-dot" :style="getColorStyle(val.valueString)"></span>
                {{ val.valueString }}
                <svg
                  v-if="isValueSelected(val.valueId)"
                  width="10"
                  height="10"
                  viewBox="0 0 10 10"
                  fill="none"
                >
                  <path
                    d="M2 5l2.5 2.5L8 2.5"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </button>
            </div>
            <p v-else class="attr-disabled-hint">Bật để chọn giá trị cho thuộc tính này</p>
          </div>
        </div>

        <div class="preview-count" v-if="getTotalSelected() > 0">
          <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
            <path
              d="M13 7.5A5.5 5.5 0 1 1 2 7.5a5.5 5.5 0 0 1 11 0z"
              stroke="#ec4899"
              stroke-width="1.5"
            />
            <path d="M7.5 5v3l2 1.5" stroke="#ec4899" stroke-width="1.5" stroke-linecap="round" />
          </svg>
          Hệ thống sẽ sinh ra <strong>{{ getCartesianCount() }} biến thể</strong>
        </div>

        <div class="card-footer-row">
          <button class="btn-cancel" @click="currentStep = 1">← Quay lại</button>
          <button
            class="btn-primary"
            :disabled="getTotalSelected() === 0"
            @click="generateVariantsAndNext"
          >
            Tạo Ma trận SKU →
          </button>
        </div>
      </div>
    </div>

    <!-- BƯỚC 3: MA TRẬN SKU MỚI -->
    <div class="card step-card" :class="{ locked: currentStep < 3 }">
      <div class="card-header-row">
        <div class="card-title-group">
          <span class="step-badge" :class="{ dim: currentStep < 3 }">03</span>
          <div>
            <h3 class="card-title">
              Ma trận SKU
              <span v-if="generatedSkus.length" class="count-pill"
                >{{ generatedSkus.length }} biến thể</span
              >
            </h3>
            <p class="card-subtitle">Điền giá bán, tồn kho và chỉnh sửa mã SKU trước khi lưu</p>
          </div>
        </div>
      </div>

      <div class="card-body" v-if="currentStep === 3 && generatedSkus.length > 0">
        <div class="bulk-bar">
          <div class="bulk-bar-label">
            <i class="fa-solid fa-wand-magic-sparkles" style="color: #db2777"></i> Áp dụng hàng loạt
          </div>
          <div class="bulk-inputs">
            <div class="bulk-field">
              <label>Giá bán (₫)</label>
              <input
                v-model="bulkPrice"
                type="number"
                placeholder="VD: 29990000"
                class="bulk-input"
              />
            </div>
            <div class="bulk-field">
              <label>Tồn kho</label>
              <input v-model="bulkStock" type="number" placeholder="VD: 10" class="bulk-input" />
            </div>
            <button class="btn-bulk-apply" @click="applyBulkSettings">Áp dụng tất cả</button>
          </div>
        </div>

        <div class="sku-table-wrapper">
          <table class="sku-table">
            <thead>
              <tr>
                <th class="th-variant">Biến thể</th>
                <th class="th-sku">Mã SKU</th>
                <th class="th-price">Giá bán (₫)</th>
                <th class="th-stock">Tồn kho</th>
                <th class="th-del"></th>
              </tr>
            </thead>
            <tbody>
              <!-- NÂNG CẤP: Dòng nào có Combo trùng với DB sẽ bị áp class .is-duplicate-row -->
              <tr
                v-for="(sku, index) in generatedSkus"
                :key="index"
                class="sku-row"
                :class="{ 'is-duplicate-row': isComboExists(sku.comboValues) }"
              >
                <td>
                  <div class="variant-name-cell">
                    <div class="variant-dots">
                      <span
                        v-for="(val, vi) in sku.comboValues"
                        :key="vi"
                        class="variant-dot"
                        :style="getColorStyle(val)"
                        :title="val"
                      ></span>
                    </div>
                    <span class="variant-label">{{ sku.variantName }}</span>
                    <!-- Cảnh báo đỏ kế bên tên biến thể -->
                    <span v-if="isComboExists(sku.comboValues)" class="badge-duplicate">
                      <i class="fa-solid fa-triangle-exclamation"></i> Đã có trong DB
                    </span>
                  </div>
                </td>

                <td>
                  <input
                    v-model="sku.skuCode"
                    @input="clearFieldError(index, 'skuCode')"
                    class="table-input mono"
                    :class="{
                      'is-invalid':
                        fieldErrors[`skus[${index}].skuCode`] || isDuplicateSku(sku.skuCode, index),
                    }"
                    placeholder="VD: IP16-BLK"
                  />
                  <div class="error-text" v-if="isDuplicateSku(sku.skuCode, index)">
                    Mã SKU bị trùng lặp!
                  </div>
                  <div class="error-text" v-else-if="fieldErrors[`skus[${index}].skuCode`]">
                    {{ fieldErrors[`skus[${index}].skuCode`] }}
                  </div>
                </td>

                <td>
                  <input
                    v-model="sku.price"
                    @input="clearFieldError(index, 'price')"
                    type="number"
                    class="table-input"
                    :class="{ 'is-invalid': fieldErrors[`skus[${index}].price`] }"
                    placeholder="0"
                    min="0"
                  />
                  <div class="error-text" v-if="fieldErrors[`skus[${index}].price`]">
                    {{ fieldErrors[`skus[${index}].price`] }}
                  </div>
                </td>

                <td>
                  <input
                    v-model="sku.stock"
                    @input="clearFieldError(index, 'stock')"
                    type="number"
                    class="table-input narrow"
                    :class="{ 'is-invalid': fieldErrors[`skus[${index}].stock`] }"
                    placeholder="0"
                    min="0"
                  />
                  <div class="error-text" v-if="fieldErrors[`skus[${index}].stock`]">
                    {{ fieldErrors[`skus[${index}].stock`] }}
                  </div>
                </td>

                <td>
                  <button
                    class="btn-row-del"
                    @click="generatedSkus.splice(index, 1)"
                    title="Xóa dòng này"
                  >
                    <i class="fa-solid fa-xmark"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Block cảnh báo lớn nếu có bất kỳ lỗi trùng lặp nào -->
        <div v-if="hasAnyDuplicate" class="duplicate-warning-box">
          <i class="fa-solid fa-triangle-exclamation"></i>
          Phát hiện các biến thể đã tồn tại hoặc Mã SKU bị trùng. Vui lòng đổi Mã SKU hoặc xóa các
          dòng bị bôi đỏ!
        </div>

        <div class="card-footer-row">
          <button class="btn-cancel" @click="currentStep = 2">← Quay lại</button>
          <button
            class="btn-primary btn-save"
            @click="saveAllSkus"
            :disabled="isSaving || hasAnyDuplicate"
          >
            <span v-if="isSaving" class="spinner"></span>
            {{ isSaving ? 'Đang lưu...' : `Lưu ${generatedSkus.length} SKU vào Database` }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Thông báo -->
    <Transition name="modal">
      <div class="modal-backdrop" v-if="alertModal.show" @click.self="alertModal.show = false">
        <div class="modal-box alert-modal-box">
          <div class="modal-body text-center">
            <div class="alert-icon" :class="alertModal.type">
              <span v-if="alertModal.type === 'success'">✓</span>
              <span v-else>✕</span>
            </div>
            <h4 class="alert-title">
              {{ alertModal.type === 'success' ? 'Thành công' : 'Thông báo lỗi' }}
            </h4>
            <p class="alert-message">{{ alertModal.message }}</p>
          </div>
          <div class="modal-footer justify-center">
            <button class="btn-primary" @click="alertModal.show = false">Đóng</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '@/utils/api'

// ── STATE ──
const currentStep = ref(1)
const isSaving = ref(false)
const fieldErrors = ref({})

// Phân trang & Lọc
const products = ref([])
const searchQuery = ref('')
const filterStatus = ref('all')
const currentPage = ref(0)
const totalPages = ref(0)

const selectedProductId = ref('')
const selectedProduct = ref(null)

const attributes = ref([])
const attributeValues = ref({})
const selectedAttributeIds = ref(new Set())
const selectedValueIds = ref(new Set())

const generatedSkus = ref([])
const bulkPrice = ref('')
const bulkStock = ref('')
const alertModal = ref({ show: false, message: '', type: 'success' })
const existingSkus = ref([])

// FORMAT TIỀN TỆ
const formatMoney = (value) => {
  return new Intl.NumberFormat('vi-VN').format(value || 0) + '₫'
}

// ── QUẢN LÝ DỮ LIỆU CŨ ──
watch(selectedProductId, async (newVal) => {
  if (!newVal) {
    existingSkus.value = []
    return
  }
  try {
    const res = await api.get(`/admin/skus/product/${newVal}`)
    existingSkus.value = res.data?.data || []
  } catch (error) {
    console.error('Lỗi khi tải danh sách SKU hiện có:', error)
  }
})

const deleteExistingSku = async (skuId, index) => {
  if (!confirm('Bạn chắc chắn muốn vô hiệu hóa SKU này khỏi hệ thống?')) return
  try {
    await api.delete(`/admin/skus/${skuId}`)
    existingSkus.value.splice(index, 1)
    showAlert('Đã vô hiệu hóa SKU thành công!', 'success')
  } catch (e) {
    showAlert('Lỗi khi vô hiệu hóa: ' + (e.response?.data?.message || e.message), 'error')
  }
}

// ── THUẬT TOÁN KIỂM TRA TRÙNG LẶP ──
// So sánh tổ hợp thuộc tính (Ví dụ mới chọn Đỏ - 256GB, mà DB đã có Đỏ - 256GB)
const isComboExists = (comboValues) => {
  return existingSkus.value.some((sku) => {
    if (!sku.attributeValues) return false
    const existingCombo = sku.attributeValues.map((v) => v.valueString)
    if (existingCombo.length !== comboValues.length) return false
    return comboValues.every((val) => existingCombo.includes(val))
  })
}

const clearFieldError = (index, fieldName) => {
  const key = `skus[${index}].${fieldName}`
  if (fieldErrors.value[key]) delete fieldErrors.value[key]
}

// Kiểm tra mã SKU Code
const isDuplicateSku = (code, currentIndex) => {
  if (!code) return false
  const inNew =
    generatedSkus.value.findIndex((s, idx) => idx !== currentIndex && s.skuCode === code) !== -1
  const inDb = existingSkus.value.some((s) => s.skuCode === code)
  return inNew || inDb
}

// KHÓA NÚT LƯU
const hasAnyDuplicate = computed(() => {
  const codes = generatedSkus.value.map((s) => s.skuCode).filter((c) => c)
  const hasDuplicateCode = new Set(codes).size !== codes.length
  const hasExistingCombo = generatedSkus.value.some((s) => isComboExists(s.comboValues))
  return hasDuplicateCode || hasExistingCombo
})

// ── API CALLS CƠ BẢN ──
const fetchProducts = async () => {
  try {
    const res = await api.get('/admin/product', {
      params: {
        page: currentPage.value,
        size: 9,
        keyword: searchQuery.value,
        filter: filterStatus.value,
      },
    })
    products.value = res.data.data?.content || res.data.data || []
    totalPages.value = res.data.data?.totalPages || 0
  } catch (error) {
    showAlert('Lỗi tải sản phẩm: ' + error.message, 'error')
  }
}

const handleSearch = () => {
  currentPage.value = 0
  fetchProducts()
}
const handleFilterChange = () => {
  currentPage.value = 0
  fetchProducts()
}
const changePage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    fetchProducts()
  }
}

const fetchAttributesAndValues = async () => {
  try {
    const attrRes = await api.get('/admin/attributes')
    attributes.value = attrRes.data?.data || []
    await Promise.all(
      attributes.value.map(async (attr) => {
        try {
          const valRes = await api.get(`/admin/attribute-values/attribute/${attr.attributeId}`)
          attributeValues.value[attr.attributeId] = valRes.data?.data || []
        } catch {
          attributeValues.value[attr.attributeId] = []
        }
      }),
    )
  } catch {
    showAlert('Lỗi tải thuộc tính', 'error')
  }
}

onMounted(async () => {
  await Promise.all([fetchProducts(), fetchAttributesAndValues()])
})

// ── THAO TÁC FORM ──
const selectProduct = (p) => {
  selectedProductId.value = p.productId
  selectedProduct.value = p
}

const getAttrValues = (attrId) => attributeValues.value[attrId] || []
const isAttributeSelected = (attrId) => selectedAttributeIds.value.has(attrId)
const isValueSelected = (valueId) => selectedValueIds.value.has(valueId)
const getSelectedValueCount = (attrId) =>
  getAttrValues(attrId).filter((v) => selectedValueIds.value.has(v.valueId)).length
const getTotalSelected = () => selectedValueIds.value.size

const toggleAttribute = (attrId) => {
  const set = new Set(selectedAttributeIds.value)
  if (set.has(attrId)) {
    set.delete(attrId)
    const vids = new Set(selectedValueIds.value)
    getAttrValues(attrId).forEach((v) => vids.delete(v.valueId))
    selectedValueIds.value = vids
  } else {
    set.add(attrId)
  }
  selectedAttributeIds.value = set
}

const toggleValue = (val) => {
  const vids = new Set(selectedValueIds.value)
  if (vids.has(val.valueId)) vids.delete(val.valueId)
  else vids.add(val.valueId)
  selectedValueIds.value = vids
}

const getCartesianCount = () => {
  const perAttr = attributes.value
    .filter((a) => isAttributeSelected(a.attributeId))
    .map((a) => getAttrValues(a.attributeId).filter((v) => isValueSelected(v.valueId)).length)
    .filter((c) => c > 0)
  return perAttr.length ? perAttr.reduce((a, b) => a * b, 1) : 0
}

// ── THUẬT TOÁN GEN MÃ SKU ──
const getInitials = (str) => {
  if (!str) return 'PRD'
  return str
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'd')
    .replace(/[^a-zA-Z0-9 ]/g, '')
    .toUpperCase()
    .split(' ')
    .filter((w) => w.length > 0)
    .map((w) => (/\d/.test(w) ? w.substring(0, 4) : w[0]))
    .join('')
}

const generateValueCode = (val) => {
  const clean = val
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'D')
    .replace(/[^a-zA-Z0-9 ]/g, '')
    .toUpperCase()
  const words = clean.split(' ').filter((w) => w.length > 0)
  if (words.length === 0) return ''
  if (words.length === 1 && /\d/.test(words[0])) return words[0].substring(0, 5)
  if (words.length === 1) return words[0].substring(0, 3)
  let code = ''
  for (let i = 0; i < words.length - 1; i++) code += words[i][0]
  code += words[words.length - 1].substring(0, 2)
  return code
}

const cartesian = (args) => args.reduce((a, b) => a.flatMap((x) => b.map((y) => [...x, y])), [[]])

const generateVariantsAndNext = () => {
  const selectedAttrs = attributes.value
    .filter((a) => isAttributeSelected(a.attributeId))
    .map((a) => ({
      ...a,
      chosenValues: getAttrValues(a.attributeId).filter((v) => isValueSelected(v.valueId)),
    }))
    .filter((a) => a.chosenValues.length > 0)

  if (selectedAttrs.length === 0) return

  const product = selectedProduct.value
  const baseCode = product ? getInitials(product.productName) : 'PRD'
  const valueCombos = cartesian(selectedAttrs.map((a) => a.chosenValues))

  generatedSkus.value = valueCombos.map((combo) => ({
    variantName: combo.map((v) => v.valueString).join(' / '),
    skuCode: `${baseCode}-${combo.map((v) => generateValueCode(v.valueString)).join('-')}`,
    price: bulkPrice.value || '',
    stock: bulkStock.value || '',
    comboValues: combo.map((v) => v.valueString),
    valueIds: combo.map((v) => v.valueId),
  }))
  currentStep.value = 3
}

const applyBulkSettings = () => {
  generatedSkus.value.forEach((sku) => {
    if (bulkPrice.value !== '') sku.price = Number(bulkPrice.value)
    if (bulkStock.value !== '') sku.stock = Number(bulkStock.value)
  })
}

// ── LƯU LÊN BACKEND ──
const saveAllSkus = async () => {
  if (!selectedProductId.value || generatedSkus.value.length === 0) return
  isSaving.value = true
  fieldErrors.value = {}

  try {
    const payload = {
      productId: selectedProductId.value,
      skus: generatedSkus.value.map((sku) => ({
        skuCode: sku.skuCode,
        price: Number(sku.price),
        stock: Number(sku.stock),
        valueIds: sku.valueIds,
      })),
    }
    await api.post('/admin/skus/batch', payload)
    showAlert(`Đã lưu ${generatedSkus.value.length} SKU thành công!`, 'success')

    // Tự động load lại danh sách DB để hiển thị ngay
    const res = await api.get(`/admin/skus/product/${selectedProductId.value}`)
    existingSkus.value = res.data?.data || []

    generatedSkus.value = []
    selectedValueIds.value = new Set()
    selectedAttributeIds.value = new Set()
    currentStep.value = 1
  } catch (error) {
    if (error.response?.status === 400 && error.response?.data?.data) {
      fieldErrors.value = error.response.data.data
      showAlert('Vui lòng kiểm tra lại các trường báo đỏ trong bảng!', 'error')
    } else {
      showAlert(error.response?.data?.message || 'Có lỗi khi lưu SKU vào CSDL', 'error')
    }
  } finally {
    isSaving.value = false
  }
}

const showAlert = (msg, type = 'success') => {
  alertModal.value = { show: true, message: msg, type }
}

const colorKeywords = {
  đen: '#1a1a2e',
  black: '#1a1a2e',
  trắng: '#f0f0f0',
  white: '#f0f0f0',
  hồng: '#f472b6',
  pink: '#f472b6',
  đỏ: '#ef4444',
  red: '#ef4444',
  'xanh lam': '#3b82f6',
  blue: '#3b82f6',
  'xanh lá': '#22c55e',
  green: '#22c55e',
  vàng: '#eab308',
  gold: '#eab308',
  titan: '#8b8fa8',
  titanium: '#8b8fa8',
  xám: '#6b7280',
  gray: '#6b7280',
  tím: '#a855f7',
  purple: '#a855f7',
}
const getColorStyle = (valueStr) => {
  if (!valueStr) return {}
  const lower = valueStr.toLowerCase()
  for (const [key, color] of Object.entries(colorKeywords)) {
    if (lower.includes(key)) return { background: color }
  }
  return { background: 'linear-gradient(135deg, #fce7f3, #fbcfe8)' }
}
</script>

<style scoped>
/* Không cần định nghĩa CSS ở đây nữa vì đã được chuyển sang sku.css dùng chung */
</style>
