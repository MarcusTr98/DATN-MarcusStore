<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <p class="breadcrumb-text">Sản phẩm</p>
        <h1 class="page-title">Tạo Biến thể SKU</h1>
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
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path
                d="M7 1.5L8.8 5.1l4 .6-2.9 2.8.7 3.9L7 10.5l-3.6 1.9.7-3.9L1.2 5.7l4-.6L7 1.5z"
                fill="#f472b6"
              />
            </svg>
            Áp dụng hàng loạt
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
              <tr v-for="(sku, index) in generatedSkus" :key="index" class="sku-row">
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
                  </div>
                </td>
                <td>
                  <input
                    v-model="sku.skuCode"
                    class="table-input mono"
                    placeholder="IP16-BLK-256"
                  />
                </td>
                <td>
                  <input
                    v-model="sku.price"
                    type="number"
                    class="table-input"
                    placeholder="0"
                    min="0"
                  />
                </td>
                <td>
                  <input
                    v-model="sku.stock"
                    type="number"
                    class="table-input narrow"
                    placeholder="0"
                    min="0"
                  />
                </td>
                <td>
                  <button
                    class="btn-row-del"
                    @click="generatedSkus.splice(index, 1)"
                    title="Xóa dòng này"
                  >
                    <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
                      <path
                        d="M2 3.5h9M5 3.5V2.5a.5.5 0 0 1 .5-.5h2a.5.5 0 0 1 .5.5v1M4 3.5l.667 7h3.666L9 3.5H4z"
                        stroke="currentColor"
                        stroke-width="1.5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      />
                    </svg>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="card-footer-row">
          <button class="btn-cancel" @click="currentStep = 2">← Quay lại</button>
          <button class="btn-primary btn-save" @click="saveAllSkus" :disabled="isSaving">
            <span v-if="isSaving" class="spinner"></span>
            {{ isSaving ? 'Đang lưu...' : `Lưu ${generatedSkus.length} SKU vào Database` }}
          </button>
        </div>
      </div>
    </div>

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
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import '@/assets/css/sku.css'

// ── STATE ──
const currentStep = ref(1)
const isSaving = ref(false)

// Phân trang & Lọc (NEW)
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
const selectedValueObjects = ref([])

const generatedSkus = ref([])
const bulkPrice = ref('')
const bulkStock = ref('')
const alertModal = ref({ show: false, message: '', type: 'success' })

const fetchProducts = async () => {
  try {
    // phân trang
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
    showAlert(
      'Không lấy được sản phẩm: ' + (error.response?.data?.message || error.message),
      'error',
    )
  }
}

// Hàm Xử lý Search & Lọc
const handleSearch = () => {
  currentPage.value = 0 // Reset về trang 1
  fetchProducts()
}

const handleFilterChange = () => {
  currentPage.value = 0 // Reset về trang 1
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
    showAlert('Lỗi tải danh sách thuộc tính', 'error')
  }
}

onMounted(async () => {
  await Promise.all([fetchProducts(), fetchAttributesAndValues()])
})

// ── ACTIONS & LOGIC GIỮ NGUYÊN ──
const selectProduct = (p) => {
  selectedProductId.value = p.productId
  selectedProduct.value = p
}

const getAttrValues = (attrId) => {
  return attributeValues.value[attrId] || []
}

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
    const removedValueIds = new Set(getAttrValues(attrId).map((v) => v.valueId))
    removedValueIds.forEach((id) => vids.delete(id))
    selectedValueIds.value = vids
    selectedValueObjects.value = selectedValueObjects.value.filter(
      (v) => !removedValueIds.has(v.valueId),
    )
  } else {
    set.add(attrId)
  }
  selectedAttributeIds.value = set
}

const toggleValue = (val) => {
  const vids = new Set(selectedValueIds.value)
  const vobjs = [...selectedValueObjects.value]
  if (vids.has(val.valueId)) {
    vids.delete(val.valueId)
    const idx = vobjs.findIndex((v) => v.valueId === val.valueId)
    if (idx !== -1) vobjs.splice(idx, 1)
  } else {
    vids.add(val.valueId)
    vobjs.push(val)
  }
  selectedValueIds.value = vids
  selectedValueObjects.value = vobjs
}

const getCartesianCount = () => {
  const perAttr = attributes.value
    .filter((a) => isAttributeSelected(a.attributeId))
    .map((a) => getAttrValues(a.attributeId).filter((v) => isValueSelected(v.valueId)).length)
    .filter((c) => c > 0)
  return perAttr.length ? perAttr.reduce((a, b) => a * b, 1) : 0
}

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
    .replace(/[đĐ]/g, 'd')
    .replace(/[^a-zA-Z0-9 ]/g, '')
    .toUpperCase()
  const words = clean.split(' ').filter((w) => w.length > 0)
  if (words.length === 1 && /\d/.test(words[0])) return words[0].substring(0, 5)
  return words.map((w) => w[0]).join('')
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
    price: bulkPrice.value || 0,
    stock: bulkStock.value || 0,
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

const saveAllSkus = async () => {
  if (!selectedProductId.value || generatedSkus.value.length === 0) return
  isSaving.value = true
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
    generatedSkus.value = []
    selectedValueIds.value = new Set()
    selectedAttributeIds.value = new Set()
    selectedValueObjects.value = []
    currentStep.value = 1
  } catch (error) {
    showAlert(error.response?.data?.message || 'Có lỗi khi lưu SKU vào CSDL', 'error')
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

<style scoped></style>
