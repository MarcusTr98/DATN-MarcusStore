<template>
  <div class="page-content">
    <div class="page-heading">
      <h3 class="page-title-text mb-4">Quản lý Biến thể Sản phẩm (SKUs)</h3>
    </div>

    <div class="row">
      <div class="col-md-4">
        <div class="card shadow-sm">
          <div class="card-header bg-white py-3">
            <h6 class="m-0 font-weight-bold text-primary">1. Thiết lập Thuộc tính</h6>
          </div>
          <div class="card-body">
            <div class="form-group mb-3">
              <label class="fw-bold mb-2">Chọn Sản Phẩm Gốc:</label>
              <select v-model="selectedProductId" class="form-select">
                <option value="" disabled>-- Chọn sản phẩm --</option>
                <option v-for="p in products" :key="p.id" :value="p.id">
                  {{ p.name }}
                </option>
              </select>
            </div>

            <hr />
            <p class="text-muted small">Thêm các thuộc tính như Màu sắc, Bộ nhớ...</p>

            <div
              v-for="(opt, index) in options"
              :key="index"
              class="border p-3 rounded mb-3 bg-light"
            >
              <div class="d-flex justify-content-between mb-2">
                <label class="fw-bold">Thuộc tính {{ index + 1 }}</label>
                <button class="btn btn-sm btn-danger" @click="removeOption(index)">
                  <i class="bi bi-trash"></i>
                </button>
              </div>
              <input v-model="opt.name" placeholder="Tên (VD: Màu sắc)" class="form-control mb-2" />
              <input
                v-model="opt.rawValues"
                placeholder="Giá trị (VD: Đỏ, Xanh, Đen)"
                class="form-control"
              />
            </div>

            <button class="btn btn-outline-primary w-100 mb-2" @click="addOption">
              <i class="bi bi-plus"></i> Thêm Thuộc Tính
            </button>
            <button
              class="btn btn-success w-100"
              :disabled="!selectedProductId || options.length === 0"
              @click="generateVariants"
            >
              <i class="bi bi-magic"></i> Tạo Ma Trận SKU
            </button>
          </div>
        </div>
      </div>

      <div class="col-md-8">
        <div class="card shadow-sm">
          <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
            <h6 class="m-0 font-weight-bold text-primary">2. Danh sách SKUs Sinh Ra</h6>
            <button
              v-if="generatedSkus.length > 0"
              class="btn btn-primary btn-sm"
              @click="saveAllSkus"
            >
              <i class="bi bi-cloud-arrow-up"></i> Lưu toàn bộ SKU lên DB
            </button>
          </div>

          <div class="card-body p-0">
            <div v-if="generatedSkus.length === 0" class="text-center text-muted p-5">
              Vui lòng thiết lập thuộc tính và bấm "Tạo Ma Trận SKU"
            </div>

            <template v-if="generatedSkus.length > 0">
              <div class="bg-light border-bottom p-3">
                <div class="row align-items-end g-2">
                  <div class="col-md-4">
                    <label class="form-label fw-bold text-sm mb-1">Giá thấp nhất (₫)</label>
                    <input
                      v-model="bulkPrice"
                      type="number"
                      class="form-control form-control-sm"
                      placeholder="VD: 25000000"
                    />
                  </div>
                  <div class="col-md-4">
                    <label class="form-label fw-bold text-sm mb-1">Tồn kho chung</label>
                    <input
                      v-model="bulkStock"
                      type="number"
                      class="form-control form-control-sm"
                      placeholder="VD: 100"
                    />
                  </div>
                  <div class="col-md-4">
                    <button class="btn btn-sm btn-warning w-100 fw-bold" @click="applyBulkSettings">
                      <i class="bi bi-lightning-charge-fill"></i> Áp dụng hàng loạt
                    </button>
                  </div>
                </div>
                <small class="text-muted mt-2 d-block">
                  * Chức năng này sẽ ghi đè giá và tồn kho cho toàn bộ
                  {{ generatedSkus.length }} biến thể bên dưới.
                </small>
              </div>

              <div class="table-responsive p-3">
                <table class="table table-bordered align-middle mb-0">
                  <thead class="table-light">
                    <tr>
                      <th>Biến thể</th>
                      <th>Mã SKU</th>
                      <th width="150">Giá bán (₫)</th>
                      <th width="120">Tồn kho</th>
                      <th width="50">Xóa</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(sku, index) in generatedSkus" :key="index">
                      <td class="fw-bold text-primary">{{ sku.variantName }}</td>
                      <td>
                        <input
                          v-model="sku.skuCode"
                          class="form-control form-control-sm"
                          placeholder="VD: IPH-RED-128"
                        />
                      </td>
                      <td>
                        <input
                          v-model="sku.price"
                          type="number"
                          class="form-control form-control-sm"
                          min="0"
                        />
                      </td>
                      <td>
                        <input
                          v-model="sku.stock"
                          type="number"
                          class="form-control form-control-sm"
                          min="0"
                        />
                      </td>
                      <td class="text-center">
                        <button
                          class="btn btn-sm btn-outline-danger"
                          @click="generatedSkus.splice(index, 1)"
                        >
                          <i class="bi bi-trash"></i>
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
// TODO: Thay đổi đường dẫn import này cho khớp với file cấu hình Axios của bạn
import api from '@/utils/api'

//1. STATE DỮ LIỆU
const products = ref([])
const selectedProductId = ref('')
const options = ref([{ name: '', rawValues: '' }])
const generatedSkus = ref([])
const bulkPrice = ref('')
const bulkStock = ref('')

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
  if (validOptions.length === 0) return alert('Vui lòng nhập ít nhất 1 thuộc tính hợp lệ!')

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
  if (!selectedProductId.value) return alert('Chưa chọn sản phẩm gốc!')
  if (generatedSkus.value.length === 0) return alert('Chưa có biến thể nào được tạo!')

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

    alert('Tuyệt vời! Đã lưu toàn bộ ma trận SKU vào Database thành công!')
    generatedSkus.value = []
    options.value = [{ name: '', rawValues: '' }]
  } catch (error) {
    console.error('Lỗi khi lưu dữ liệu:', error)
    alert('Hệ thống từ chối lưu: ' + (error.response?.data?.message || error.message))
  }
}
</script>

<style scoped>
.page-content {
  padding: 20px;
  background-color: #f8f9fc;
  min-height: 100vh;
}
.text-sm {
  font-size: 0.875rem;
}
</style>
