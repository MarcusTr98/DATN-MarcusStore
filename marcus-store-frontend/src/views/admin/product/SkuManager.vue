<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon-box">
          <i class="fa-solid fa-tags"></i>
        </div>
        <div class="header-text-group">
          <p class="breadcrumb-text">Sản phẩm</p>
          <h1 class="page-title">
            Quản lý Thuộc tính
            <span class="page-badge"> <span class="page-badge-dot"></span>MASTER DATA </span>
          </h1>
          <p class="page-subtitle">Quản lý các thuộc tính và giá trị dùng để tạo biến thể SKU</p>
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

<style scoped></style>
