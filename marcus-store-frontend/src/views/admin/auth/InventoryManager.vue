<template>
  <div class="inv-page">
    <div class="inv-shell">
      <div class="page-header">
        <div class="header-wrap">
          <div class="header-icon">
            <i class="bi bi-box-seam"></i>
          </div>
          <div>
            <h4 class="header-title">Quản lý tồn kho</h4>
            <p class="header-sub">Theo dõi và quản lý hàng tồn kho trong hệ thống.</p>
          </div>
        </div>
      </div>

      <div class="stats-row">
        <div class="stats-grid">
          <div class="stat-card">
            <p class="stat-label">Tổng Biến Thể</p>
            <p class="stat-value pink">{{ summary.totalSkus || 0 }}</p>
          </div>
          <div class="stat-card">
            <p class="stat-label">Tổng đơn vị</p>
            <p class="stat-value">{{ summary.totalStockUnits || 0 }}</p>
          </div>
          <div class="stat-card">
            <p class="stat-label">Tổng giá trị</p>
            <p class="stat-value">{{ formatCurrency(summary.totalStockValue) }}</p>
          </div>
        </div>
        <div class="stats-grid">
          <div class="stat-card">
            <p class="stat-label">Còn hàng</p>
            <p class="stat-value success">{{ summary.totalInStock || 0 }}</p>
          </div>
          <div class="stat-card">
            <p class="stat-label">Sắp hết</p>
            <p class="stat-value warning">{{ summary.totalLowStock || 0 }}</p>
          </div>
          <div class="stat-card">
            <p class="stat-label">Hết hàng</p>
            <p class="stat-value danger">{{ summary.totalOutOfStock || 0 }}</p>
          </div>
        </div>
      </div>

      <div class="filter-card">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-md-6">
            <label class="filter-label">Tìm kiếm</label>
            <div class="input-wrapper">
              <i class="bi bi-search search-icon"></i>
              <input
                v-model="keyword"
                type="search"
                class="form-control f-input"
                placeholder="Tìm theo mã SKU, tên sản phẩm..."
              />
            </div>
          </div>

          <div class="col-12 col-md-4">
            <label class="filter-label">Lọc trạng thái</label>
            <select v-model="stockStatus" class="form-select f-input">
              <option :value="null">Tất cả</option>
              <option value="IN_STOCK">Còn hàng</option>
              <option value="LOW_STOCK">Sắp hết</option>
              <option value="OUT_OF_STOCK">Hết hàng</option>
            </select>
          </div>

          <div class="col-12 col-md-2">
            <button class="btn btn-reset" @click="resetFilters" title="Đặt lại bộ lọc">
              <i class="bi bi-arrow-clockwise"></i>
            </button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <div v-if="loading" class="text-center py-5">
          <div class="spinner-border spinner-pink" role="status"></div>
          <p class="mt-2 text-muted small">Đang tải dữ liệu...</p>
        </div>

        <template v-else>
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0 inv-table">
              <thead>
                <tr>
                  <th class="th text-center" style="width: 60px">STT</th>
                  <th class="th">Biến thể</th>
                  <th class="th">Sản phẩm</th>
                  <th class="th">Danh mục</th>
                  <th class="th">Thương hiệu</th>
                  <th class="th text-end">Giá</th>
                  <th class="th text-center">Tồn kho</th>
                  <th class="th text-center">Trạng thái</th>
                  <th class="th text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!inventoryList.length">
                  <td colspan="9" class="text-center py-5 text-muted">
                    <div class="empty-state">
                      <i class="bi bi-box-seam"></i>
                      <h5>Không có dữ liệu tồn kho</h5>
                      <p class="small mb-0">Hãy thay đổi bộ lọc hoặc thêm sản phẩm mới.</p>
                    </div>
                  </td>
                </tr>
                <tr v-for="(item, idx) in inventoryList" :key="item.skuId">
                  <td class="text-center text-muted">{{ currentPage * pageSize + idx + 1 }}</td>
                  <td><span class="sku-code">{{ item.skuCode }}</span></td>
                  <td>
                    <div class="d-flex align-items-center gap-2">
                      <img
                        v-if="item.skuImageUrl"
                        :src="item.skuImageUrl"
                        class="thumb-img"
                      />
                      <div v-else class="thumb-placeholder">
                        <i class="bi bi-image"></i>
                      </div>
                      <span class="fw-500">{{ item.productName }}</span>
                    </div>
                  </td>
                  <td class="text-muted small">{{ item.categoryName || '—' }}</td>
                  <td class="text-muted small">{{ item.brand || '—' }}</td>
                  <td class="text-end fw-500">{{ formatCurrency(item.price) }}</td>
                  <td class="text-center">
                    <span
                      class="badge-stock"
                      :class="stockClass(item.stockStatus)"
                    >
                      {{ item.stockQuantity ?? 0 }}
                    </span>
                  </td>
                  <td class="text-center">
                    <span class="status-text" :class="stockClass(item.stockStatus)">
                      {{ statusLabel(item.stockStatus) }}
                    </span>
                  </td>
                  <td class="text-center">
                    <div class="d-flex justify-content-center gap-2">
                      <button class="act-btn" title="Nhập kho" @click="openImportModal(item)">
                        <i class="bi bi-box-arrow-in-down"></i>
                      </button>
                      <button
                        class="act-btn info"
                        title="Quản lý IMEI"
                        @click="goToImeiPage(item)"
                      >
                        <i class="bi bi-upc-scan"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <nav v-if="totalElements > 0" class="pagination-bar">
            <div class="pagination-total">
              Tổng <strong>{{ totalElements }}</strong> SKU
            </div>

            <div class="pagination-actions">
              <label class="page-size-box">
                <span>Hiển thị</span>
                <select v-model.number="pageSize" @change="onSizeChange">
                  <option :value="5">5</option>
                  <option :value="10">10</option>
                  <option :value="20">20</option>
                  <option :value="50">50</option>
                </select>
              </label>

              <button
                type="button"
                class="page-btn"
                :disabled="currentPage === 0"
                @click="goToPage(currentPage - 1)"
              >
                Trước
              </button>

              <span class="page-current"> Trang {{ currentPage + 1 }} / {{ totalPages }} </span>

              <button
                type="button"
                class="page-btn"
                :disabled="currentPage + 1 >= totalPages"
                @click="goToPage(currentPage + 1)"
              >
                Sau
              </button>
            </div>
          </nav>
        </template>
      </div>
    </div>

    <div class="modal fade inv-modal" id="importModal" tabindex="-1" ref="importModalRef">
      <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">
              <i class="bi bi-box-arrow-in-down me-2"></i>Nhập kho
            </h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <div class="row g-3">
              <div class="col-md-6">
                <label class="filter-label">SKU</label>
                <input v-model="formImport.skuCode" type="text" class="form-control" readonly />
              </div>
              <div class="col-md-6">
                <label class="filter-label">Sản phẩm</label>
                <input v-model="formImport.productName" type="text" class="form-control" readonly />
              </div>
            </div>

            <div class="mt-3">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <label class="filter-label mb-0">
                  Danh sách IMEI/Serial <span class="text-danger">*</span>
                  <span class="text-muted small ms-1">(mỗi dòng 1 mã, hoặc phân cách bằng dấu phẩy)</span>
                </label>
              </div>
              <textarea
                v-model="formImport.imeiText"
                class="form-control f-input imei-textarea"
                rows="6"
                placeholder="VD:&#10;352099001761481&#10;352099001761482&#10;352099001761483"
                style="padding-left: 12px"
              ></textarea>
              <div class="d-flex justify-content-between align-items-center mt-2">
                <small :class="imeiCountMismatch ? 'text-danger' : 'text-success'">
                  <i class="bi bi-info-circle me-1"></i>
                  Đã nhập <strong>{{ parsedImeis.length }}</strong> mã hợp lệ
                </small>
                <div class="d-flex gap-2 align-items-center">
                  <button
                    v-if="formImport.imeiText"
                    type="button"
                    class="btn btn-sm btn-link text-danger p-0"
                    @click="clearImeiText"
                  >
                    <i class="bi bi-x-circle me-1"></i>Xóa hết
                  </button>
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-pink"
                    :disabled="parsingExcel"
                    onclick="window.__importImeiFromExcel && window.__importImeiFromExcel()"
                  >
                    <i class="bi bi-file-earmark-excel me-1"></i>
                    {{ parsingExcel ? 'Đang đọc...' : 'Import từ Excel' }}
                  </button>
                </div>
              </div>
            </div>

            <div class="mt-3">
              <label class="filter-label">Số lượng nhập <span class="text-danger">*</span></label>
              <input
                v-model.number="formImport.importQuantity"
                type="number"
                class="form-control f-input"
                min="1"
                placeholder="Số lượng = số IMEI đã nhập"
                readonly
                style="padding-left: 12px"
              />
              <small class="text-muted d-block mt-1">
                <i class="bi bi-lock me-1"></i>Số lượng tự động = số IMEI hợp lệ bạn đã nhập ở trên.
              </small>
            </div>

            <div class="mt-3">
              <label class="filter-label">Ghi chú</label>
              <textarea
                v-model="formImport.note"
                class="form-control"
                rows="2"
                placeholder="Ghi chú (tùy chọn)"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-outline-secondary" data-bs-dismiss="modal">Hủy</button>
            <button
              class="btn btn-pink"
              :disabled="submitting || !canSubmitImport"
              @click="submitImport"
            >
              <span v-if="submitting" class="spinner-border spinner-border-sm me-1"></span>
              Xác nhận nhập kho
            </button>
          </div>
        </div>
      </div>
    </div>

    <BaseModal
      :visible="baseModal.visible"
      :type="baseModal.type"
      :title="baseModal.title"
      :message="baseModal.message"
      @close="baseModal.visible = false"
    />

    <div class="inv-toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Modal } from 'bootstrap'
import BaseModal from '@/components/BaseModal.vue'
import api from '@/utils/api'
import { setupGlobalExcelImporter } from '@/utils/excelImport'
import '@/assets/css/Inventory.css'

const router = useRouter()

const BASE = '/admin/inventory'

const inventoryApi = {
  list: (params) => api.get(`${BASE}/inventory`, { params }),
  summary: () => api.get(`${BASE}/summary`),
  import: (data) => api.post(`${BASE}/import`, data),
  adjust: (data) => api.put(`${BASE}/adjust`, data),
}

const loading = ref(false)
const submitting = ref(false)
const toastMessage = ref('')

const inventoryList = ref([])
const summary = ref({
  totalSkus: 0,
  totalInStock: 0,
  totalLowStock: 0,
  totalOutOfStock: 0,
  totalStockUnits: 0,
  totalStockValue: 0,
})

const keyword = ref('')
const stockStatus = ref(null)

const currentPage = ref(0)
const pageSize = ref(5)
const totalPages = ref(0)
const totalElements = ref(0)

const importModalRef = ref(null)
let importModalInstance = null

const parsingExcel = ref(false)

const formImport = ref({
  skuId: null,
  skuCode: '',
  productName: '',
  importQuantity: null,
  imeiText: '',
  note: '',
})

const parsedImeis = computed(() => {
  const raw = formImport.value.imeiText || ''
  return raw
    .split(/[\n,;\t\r]+/g)
    .map((s) => s.trim())
    .filter(Boolean)
})

watch(
  parsedImeis,
  (list) => {
    formImport.value.importQuantity = list.length
  },
  { immediate: true },
)

const imeiCountMismatch = computed(() => {
  const q = Number(formImport.value.importQuantity || 0)
  return parsedImeis.value.length !== q
})

const canSubmitImport = computed(() => {
  const q = Number(formImport.value.importQuantity || 0)
  if (!q || q <= 0) return false
  if (imeiCountMismatch.value) return false
  return true
})

const baseModal = ref({ visible: false, type: 'info', title: '', message: '' })

function showToast(msg) {
  toastMessage.value = msg
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 2600)
}

function showModal(type, title, message) {
  baseModal.value = { visible: true, type, title, message }
}

async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value?.trim() || undefined,
    }
    if (stockStatus.value) params.stockStatus = stockStatus.value
    const res = await inventoryApi.list(params)
    const data = res.data?.data ?? {}
    inventoryList.value = data.content || []
    totalPages.value = data.totalPages || 0
    totalElements.value = data.totalElements || 0
  } catch (e) {
    showToast(e?.response?.data?.message || 'Không thể tải danh sách tồn kho')
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  try {
    const res = await inventoryApi.summary()
    summary.value = res.data?.data || {}
  } catch {
    /* ignore */
  }
}

function openImportModal(item) {
  formImport.value = {
    skuId: item?.skuId || null,
    skuCode: item?.skuCode || '',
    productName: item?.productName || '',
    importQuantity: null,
    imeiText: '',
    note: '',
  }
  importModalInstance?.show()
}

function goToImeiPage(item) {
  router.push({
    name: 'InventoryImeiManager',
    params: { skuId: item.skuId },
    query: {
      code: item.skuCode,
      name: item.productName,
      stock: item.stockQuantity,
    },
  })
}

async function submitImport() {
  if (!formImport.value.importQuantity || formImport.value.importQuantity <= 0) {
    showToast('Số lượng nhập phải lớn hơn 0')
    return
  }
  if (imeiCountMismatch.value) {
    showToast('Số IMEI phải bằng số lượng nhập')
    return
  }
  submitting.value = true
  try {
    await inventoryApi.import({
      skuId: formImport.value.skuId,
      importQuantity: formImport.value.importQuantity,
      note: formImport.value.note,
      imeis: parsedImeis.value,
    })
    importModalInstance?.hide()
    showModal('success', 'Thành công', 'Nhập kho thành công!')
    await Promise.all([fetchList(), fetchSummary()])
  } catch (e) {
    showToast(e?.response?.data?.message || 'Nhập kho thất bại')
  } finally {
    submitting.value = false
  }
}

function goToPage(page) {
  if (page < 0 || page >= totalPages.value) return
  currentPage.value = page
  fetchList()
}

function onSizeChange() {
  currentPage.value = 0
  fetchList()
}

function resetFilters() {
  keyword.value = ''
  stockStatus.value = null
  currentPage.value = 0
  fetchList()
}

const formatCurrency = (value) => {
  if (!value) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value)
}

const statusLabel = (status) => {
  const map = {
    IN_STOCK: 'Còn hàng',
    LOW_STOCK: 'Sắp hết',
    OUT_OF_STOCK: 'Hết hàng',
  }
  return map[status] || status
}

const stockClass = (status) => {
  const map = {
    IN_STOCK: 'in-stock',
    LOW_STOCK: 'low-stock',
    OUT_OF_STOCK: 'out-stock',
  }
  return map[status] || ''
}

function onImeiChanged() {
  fetchList()
  fetchSummary()
}

function clearImeiText() {
  formImport.value.imeiText = ''
}

onMounted(() => {
  importModalInstance = new Modal(importModalRef.value)
  fetchList()
  fetchSummary()
  window.addEventListener('inventory:imei-changed', onImeiChanged)
  setupGlobalExcelImporter({
    getFormImport: () => formImport.value,
    getParsingExcel: () => parsingExcel.value,
    setParsingExcel: (v) => (parsingExcel.value = v),
    showToast,
    onSuccess: (count, fileName) => {
      showModal('success', 'Đã đọc file Excel', `Đã nhập ${count} mã IMEI từ "${fileName}".`)
    },
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('inventory:imei-changed', onImeiChanged)
  delete window.__importImeiFromExcel
})

let debounceTimer = null

watch(
  [keyword, stockStatus],
  () => {
    currentPage.value = 0
    window.clearTimeout(debounceTimer)
    debounceTimer = window.setTimeout(() => {
      fetchList()
    }, 350)
  },
)
</script>