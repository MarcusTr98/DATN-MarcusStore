<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon-box">
          <i class="fa-solid fa-sliders"></i>
        </div>
        <div class="header-text-group">
          <p class="breadcrumb-text">Sản phẩm</p>
          <h1 class="page-title">Quản lý Biến thể SKU</h1>
          <p class="page-subtitle">Thiết lập thuộc tính và sinh mã SKU cho từng sản phẩm</p>
        </div>
      </div>
    </div>

    <div class="manage-grid">
      <!-- ───────── CỘT TRÁI: THIẾT LẬP THUỘC TÍNH ───────── -->
      <div class="card config-card">
        <div class="card-header-row no-pointer">
          <div class="card-title-group">
            <span class="step-badge">01</span>
            <div>
              <h3 class="card-title">Thiết lập Thuộc tính</h3>
              <p class="card-subtitle">Chọn sản phẩm gốc và khai báo thuộc tính biến thể</p>
            </div>
          </div>
        </div>

        <div class="card-body">
          <div class="field-group">
            <label class="field-label">Sản phẩm gốc</label>
            <select v-model="selectedProductId" class="status-select full-width">
              <option value="" disabled>-- Chọn sản phẩm --</option>
              <option v-for="p in products" :key="p.id" :value="p.id">
                {{ p.name }}
              </option>
            </select>
          </div>

          <p class="hint-text">Thêm các thuộc tính như Màu sắc, Bộ nhớ...</p>

          <div v-for="(opt, index) in options" :key="index" class="option-block">
            <div class="option-block-header">
              <span class="option-block-title">Thuộc tính {{ index + 1 }}</span>
              <button class="btn-row-del" @click="removeOption(index)" title="Xóa thuộc tính này">
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
            </div>
            <input
              v-model="opt.name"
              placeholder="Tên (VD: Màu sắc)"
              class="table-input option-input"
            />
            <input
              v-model="opt.rawValues"
              placeholder="Giá trị (VD: Đỏ, Xanh, Đen)"
              class="table-input option-input"
            />
          </div>

          <button class="btn-cancel full-width" @click="addOption">+ Thêm Thuộc Tính</button>

          <div class="card-footer-row column-footer">
            <button
              class="btn-primary full-width"
              :disabled="!selectedProductId || options.length === 0"
              @click="generateVariants"
            >
              Tạo Ma Trận SKU →
            </button>
          </div>
        </div>
      </div>

      <!-- ───────── CỘT PHẢI: DANH SÁCH SKU SINH RA ───────── -->
      <div class="card results-card">
        <div class="card-header-row no-pointer">
          <div class="card-title-group">
            <span class="step-badge">02</span>
            <div>
              <h3 class="card-title">
                Danh sách SKU sinh ra
                <span v-if="generatedSkus.length" class="count-pill"
                  >{{ generatedSkus.length }} biến thể</span
                >
              </h3>
              <p class="card-subtitle">Điền giá bán, tồn kho và chỉnh sửa mã SKU trước khi lưu</p>
            </div>
          </div>
          <button
            v-if="generatedSkus.length > 0"
            class="btn-primary btn-save"
            :disabled="isSaving"
            @click="saveAllSkus"
          >
            <span v-if="isSaving" class="spinner"></span>
            {{ isSaving ? 'Đang lưu...' : 'Lưu toàn bộ SKU lên DB' }}
          </button>
        </div>

        <div class="card-body" v-if="generatedSkus.length === 0">
          <div class="empty-state">
            <p>Vui lòng thiết lập thuộc tính và bấm "Tạo Ma Trận SKU"</p>
          </div>
        </div>

        <div class="card-body no-pad-top" v-else>
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
                <label>Giá thấp nhất (₫)</label>
                <input
                  v-model="bulkPrice"
                  type="number"
                  placeholder="VD: 25000000"
                  class="bulk-input"
                />
              </div>
              <div class="bulk-field">
                <label>Tồn kho chung</label>
                <input v-model="bulkStock" type="number" placeholder="VD: 100" class="bulk-input" />
              </div>
              <button class="btn-bulk-apply" @click="applyBulkSettings">Áp dụng tất cả</button>
            </div>
            <small class="bulk-note">
              * Sẽ ghi đè giá và tồn kho cho toàn bộ {{ generatedSkus.length }} biến thể bên dưới.
            </small>
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
                      placeholder="VD: IPH-RED-128"
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
// TODO: Thay đổi đường dẫn import này cho khớp với file cấu hình Axios của bạn
import api from '@/utils/api'
import '@/assets/css/sku.css'

//1. STATE DỮ LIỆU
const products = ref([])
const selectedProductId = ref('')
const options = ref([{ name: '', rawValues: '' }])
const generatedSkus = ref([])
const bulkPrice = ref('')
const bulkStock = ref('')
const isSaving = ref(false)
const alertModal = ref({ show: false, message: '', type: 'success' })

// 2. API CALLS
const fetchProducts = async () => {
  try {
    const res = await api.get('/admin/products')
    products.value = res.data
  } catch (err) {
    console.warn('Backend báo lỗi 403. Tạm thời dùng dữ liệu Mock để test UI!')
    console.error('Lỗi tải sản phẩm', err)
    // Bơm dữ liệu giả đợi Đức làm Product CRUD xong
    products.value = [
      { id: 1, name: 'iPhone 15 Pro Max (Mock)' },
      { id: 2, name: 'Samsung Galaxy S24 Ultra (Mock)' },
    ]
  }
}

onMounted(fetchProducts)

// 3. XỬ LÝ FORM THUỘC TÍNH
const addOption = () => {
  options.value.push({ name: '', rawValues: '' })
}

const removeOption = (index) => {
  options.value.splice(index, 1)
}

const getValidOptions = () =>
  options.value
    .filter((o) => o.name.trim() !== '' && o.rawValues.trim() !== '')
    .map((o) => ({
      name: o.name,
      values: o.rawValues
        .split(',')
        .map((v) => v.trim())
        .filter((v) => v !== ''),
    }))

//4. THUẬT TOÁN SINH MÃ
const getInitials = (str) => {
  const cleanStr = str
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'd')
    .replace(/[^a-zA-Z0-9 ]/g, '')
    .toUpperCase()

  const words = cleanStr.split(' ').filter((word) => word.length > 0)
  let prefix = ''
  words.forEach((word) => {
    if (/\d/.test(word)) {
      prefix += word.substring(0, 4)
    } else {
      prefix += word[0]
    }
  })
  return prefix
}

const generateValueCode = (val) => {
  const cleanStr = val
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'd')
    .replace(/[^a-zA-Z0-9 ]/g, '')
    .toUpperCase()

  const words = cleanStr.split(' ').filter((w) => w.length > 0)
  if (words.length === 1 && /\d/.test(words[0])) {
    return words[0].substring(0, 5)
  }
  return words.map((w) => w[0]).join('')
}

const cartesian = (args) =>
  args.reduce(
    (a, b) => a.map((x) => b.map((y) => x.concat([y]))).reduce((c, d) => c.concat(d), []),
    [[]],
  )

//5. SINH TỔ HỢP MA TRẬN
const generateVariants = () => {
  const validOptions = getValidOptions()
  if (validOptions.length === 0)
    return showAlert('Vui lòng nhập ít nhất 1 thuộc tính hợp lệ!', 'error')

  const product = products.value.find((p) => p.id === selectedProductId.value)
  const baseCode = product ? getInitials(product.name) : 'PRD'
  const combinations = cartesian(validOptions.map((opt) => opt.values))

  generatedSkus.value = combinations.map((combo) => ({
    variantName: combo.join(' - '),
    skuCode: `${baseCode}-${combo.map(generateValueCode).join('-')}`, // Auto Gen Mã
    price: bulkPrice.value || 0,
    stock: bulkStock.value || 0,
    comboValues: combo, // Giữ lại mảng giá trị để map ID khi lưu BE
  }))
}

//6. XỬ LÝ HÀNG LOẠT
const applyBulkSettings = () => {
  if (generatedSkus.value.length === 0) return

  generatedSkus.value.forEach((sku) => {
    if (bulkPrice.value !== '') sku.price = Number(bulkPrice.value)
    if (bulkStock.value !== '') sku.stock = Number(bulkStock.value)
  })
}

//7. LƯU VÀO BACKEND
const saveAllSkus = async () => {
  if (!selectedProductId.value) return showAlert('Chưa chọn sản phẩm gốc!', 'error')
  if (generatedSkus.value.length === 0) return showAlert('Chưa có biến thể nào được tạo!', 'error')

  isSaving.value = true
  try {
    //Lưu Options lên DB & lập Value ID
    const globalValueIdMap = {}
    for (const opt of getValidOptions()) {
      const res = await api.post(`/admin/products/${selectedProductId.value}/options`, {
        name: opt.name,
        values: opt.values,
      })
      Object.assign(globalValueIdMap, res.data.valueIds)
    }

    //Map ID và đẩy đồng loạt SKU lên Server
    await Promise.all(
      generatedSkus.value.map((sku) =>
        api.post(`/admin/products/${selectedProductId.value}/skus`, {
          skuCode: sku.skuCode,
          price: sku.price,
          stock: sku.stock,
          imageUrl: '',
          optionValueIds: sku.comboValues.map((val) => globalValueIdMap[val]),
        }),
      ),
    )

    showAlert(
      `Đã lưu toàn bộ ${generatedSkus.value.length} SKU vào Database thành công!`,
      'success',
    )
    generatedSkus.value = []
    options.value = [{ name: '', rawValues: '' }]
  } catch (error) {
    console.error('Lỗi khi lưu dữ liệu:', error)
    showAlert('Hệ thống từ chối lưu: ' + (error.response?.data?.message || error.message), 'error')
  } finally {
    isSaving.value = false
  }
}

const showAlert = (msg, type = 'success') => {
  alertModal.value = { show: true, message: msg, type }
}

// 8. MÀU GỢI Ý CHO CHẤM TRÒN BIẾN THỂ (đồng bộ với màn Tạo SKU)
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
/* ════════════════════ FONT & NỀN CHUNG ════════════════════ */
.page-wrapper {
  font-family: 'Be Vietnam Pro', 'Segoe UI', sans-serif;
  color: #111827;
}

/* ════════════════════ HEADER (đồng bộ màn Tạo SKU) ════════════════════ */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon-box {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff4d94, #ff7eb3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 22px;
  flex-shrink: 0;
  box-shadow: 0 8px 18px rgba(255, 77, 148, 0.35);
}

.header-text-group .breadcrumb-text {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  color: #ff4d94;
  margin: 0 0 4px 0;
}

.header-text-group .page-title {
  font-size: 26px;
  font-weight: 900;
  color: #111827;
  margin: 0 0 4px 0;
}

.header-text-group .page-subtitle {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin: 0;
}

/* ════════════════════ LAYOUT 2 CỘT ════════════════════ */
.manage-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
  align-items: start;
}

@media (max-width: 900px) {
  .manage-grid {
    grid-template-columns: 1fr;
  }
}

/* ════════════════════ CARD CHUNG ════════════════════ */
.card {
  background: #ffffff;
  border-radius: 18px;
  border: 1px solid #f3e8ee;
  box-shadow: 0 4px 18px rgba(17, 24, 39, 0.05);
  overflow: hidden;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 18px 22px;
  border-bottom: 1px solid #f3e8ee;
}

.card-title-group {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.step-badge {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 9px;
  background: linear-gradient(135deg, #ff4d94, #ff7eb3);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-title {
  font-size: 16px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 2px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card-subtitle {
  font-size: 12.5px;
  font-weight: 500;
  color: #6b7280;
  margin: 0;
}

.count-pill {
  background: #fce7f3;
  color: #db2777;
  font-size: 11px;
  font-weight: 800;
  padding: 2px 9px;
  border-radius: 999px;
}

.card-body {
  padding: 22px;
}

.card-body.no-pad-top {
  padding-top: 0;
}

.card-footer-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.card-footer-row.column-footer {
  flex-direction: column;
}

/* ════════════════════ FORM CHUNG ════════════════════ */
.field-group {
  margin-bottom: 16px;
}

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #374151;
  margin-bottom: 6px;
}

.status-select,
.table-input {
  width: 100%;
  border: 1.5px solid #f0d9e3;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 13.5px;
  font-family: inherit;
  color: #111827;
  background: #fff;
  outline: none;
  transition: border-color 0.15s ease;
}

.status-select:focus,
.table-input:focus {
  border-color: #ff4d94;
}

.full-width {
  width: 100%;
}

.hint-text {
  font-size: 12.5px;
  color: #6b7280;
  margin: 0 0 14px 0;
}

/* ════════════════════ KHỐI THUỘC TÍNH ════════════════════ */
.option-block {
  border: 1.5px solid #f3e8ee;
  background: #fff8fb;
  border-radius: 14px;
  padding: 14px;
  margin-bottom: 14px;
}

.option-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.option-block-title {
  font-size: 13px;
  font-weight: 800;
  color: #111827;
}

.option-input {
  margin-bottom: 8px;
}

.option-input:last-child {
  margin-bottom: 0;
}

/* ════════════════════ NÚT BẤM ════════════════════ */
.btn-primary {
  background: linear-gradient(135deg, #ff4d94, #ff7eb3);
  color: #fff;
  border: none;
  border-radius: 11px;
  padding: 11px 20px;
  font-size: 13.5px;
  font-weight: 800;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition:
    transform 0.12s ease,
    box-shadow 0.12s ease;
  box-shadow: 0 8px 18px rgba(255, 77, 148, 0.3);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
}

.btn-primary:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  box-shadow: none;
  cursor: not-allowed;
}

.btn-save {
  white-space: nowrap;
}

.btn-cancel {
  background: #fff;
  color: #ff4d94;
  border: 1.5px solid #ffc1dc;
  border-radius: 11px;
  padding: 10px 18px;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  margin-bottom: 14px;
}

.btn-cancel:hover {
  background: #fff5f9;
}

.btn-row-del {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid #f3e8ee;
  background: #fff;
  color: #9ca3af;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
}

.btn-row-del:hover {
  color: #ef4444;
  border-color: #fecaca;
  background: #fef2f2;
}

.spinner {
  width: 13px;
  height: 13px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-top-color: #fff;
  border-radius: 50%;
  display: inline-block;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ════════════════════ EMPTY STATE ════════════════════ */
.empty-state {
  text-align: center;
  color: #9ca3af;
  font-size: 13.5px;
  padding: 40px 20px;
}

/* ════════════════════ BULK BAR ════════════════════ */
.bulk-bar {
  background: #fff8fb;
  border: 1.5px solid #f3e8ee;
  border-radius: 14px;
  padding: 14px 16px;
  margin-bottom: 18px;
}

.bulk-bar-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 800;
  color: #111827;
  margin-bottom: 10px;
}

.bulk-inputs {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.bulk-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bulk-field label {
  font-size: 11.5px;
  font-weight: 700;
  color: #6b7280;
}

.bulk-input {
  border: 1.5px solid #f0d9e3;
  border-radius: 9px;
  padding: 8px 10px;
  font-size: 13px;
  width: 160px;
  outline: none;
}

.bulk-input:focus {
  border-color: #ff4d94;
}

.btn-bulk-apply {
  background: #fde2ee;
  color: #db2777;
  border: none;
  border-radius: 9px;
  padding: 9px 16px;
  font-size: 12.5px;
  font-weight: 800;
  cursor: pointer;
  white-space: nowrap;
}

.btn-bulk-apply:hover {
  background: #fbcfe8;
}

.bulk-note {
  display: block;
  margin-top: 10px;
  font-size: 11.5px;
  color: #9ca3af;
}

/* ════════════════════ BẢNG SKU ════════════════════ */
.sku-table-wrapper {
  overflow-x: auto;
  border: 1px solid #f3e8ee;
  border-radius: 14px;
}

.sku-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.sku-table thead {
  background: #fff5f9;
}

.sku-table th {
  text-align: left;
  font-size: 11.5px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  color: #db2777;
  padding: 10px 12px;
}

.sku-row td {
  padding: 8px 12px;
  border-top: 1px solid #f3e8ee;
}

.sku-row:hover td {
  background: #fffafc;
}

.variant-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.variant-dots {
  display: flex;
  gap: 3px;
  flex-shrink: 0;
}

.variant-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.variant-label {
  font-weight: 700;
  color: #111827;
}

.table-input.mono {
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 12.5px;
}

.table-input.narrow {
  max-width: 90px;
}

.th-price,
.th-stock {
  width: 130px;
}

.th-del {
  width: 40px;
}

/* ════════════════════ MODAL THÔNG BÁO ════════════════════ */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-box {
  background: #fff;
  border-radius: 18px;
  width: 360px;
  max-width: 90vw;
  overflow: hidden;
  box-shadow: 0 20px 50px rgba(17, 24, 39, 0.25);
}

.modal-body {
  padding: 30px 24px 18px;
}

.text-center {
  text-align: center;
}

.alert-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 800;
  color: #fff;
  margin: 0 auto 14px;
}

.alert-icon.success {
  background: linear-gradient(135deg, #34d399, #10b981);
}

.alert-icon.error {
  background: linear-gradient(135deg, #f87171, #ef4444);
}

.alert-title {
  font-size: 17px;
  font-weight: 800;
  margin: 0 0 6px 0;
  color: #111827;
}

.alert-message {
  font-size: 13.5px;
  color: #6b7280;
  margin: 0;
  line-height: 1.5;
}

.modal-footer {
  display: flex;
  padding: 16px 24px 24px;
}

.modal-footer.justify-center {
  justify-content: center;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.18s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
