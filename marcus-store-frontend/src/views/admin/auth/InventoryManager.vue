<template>
  <div class="inv-page">
    <div class="inv-shell">
      <div class="page-header">
        <div class="header-wrap">
          <div class="header-icon">
            <i class="bi bi-box-seam"></i>
          </div>
          <div>
            <h4 class="header-title">{{ pageTitle }}</h4>
            <p class="header-sub">{{ pageSub }}</p>
          </div>
        </div>
        <button class="btn btn-outline-pink btn-switch-scope" @click="switchScope">
          <i class="bi bi-arrow-left-right me-1"></i>
          {{ imeiScope ? 'Sang kho Không IMEI' : 'Sang kho Có IMEI' }}
        </button>
      </div>

      <div class="stats-row">
        <div class="stats-grid">
          <div class="stat-card pink">
            <div class="stat-icon"><i class="bi bi-grid-3x3-gap"></i></div>
            <div class="stat-body">
              <p class="stat-label">Tổng Biến Thể</p>
              <p class="stat-value pink">
                {{ summary.totalSkus || 0 }}<span class="stat-unit">biến thể</span>
              </p>
            </div>
          </div>
          <div class="stat-card value">
            <div class="stat-icon"><i class="bi bi-box-seam"></i></div>
            <div class="stat-body">
              <p class="stat-label">Tồn kho</p>
              <p class="stat-value value">
                {{ summary.totalStockUnits || 0 }}<span class="stat-unit">cái</span>
              </p>
            </div>
          </div>
          <div class="stat-card value">
            <div class="stat-icon"><i class="bi bi-cash-coin"></i></div>
            <div class="stat-body">
              <p class="stat-label">Tổng giá trị</p>
              <p class="stat-value value">{{ formatCurrency(summary.totalStockValue) }}</p>
            </div>
          </div>
        </div>
        <div class="stats-grid">
          <div class="stat-card success">
            <div class="stat-icon"><i class="bi bi-check2-circle"></i></div>
            <div class="stat-body">
              <p class="stat-label">Còn hàng</p>
              <p class="stat-value success">
                {{ summary.totalInStock || 0 }}<span class="stat-unit">cái</span>
              </p>
            </div>
          </div>
          <div class="stat-card warning">
            <div class="stat-icon"><i class="bi bi-exclamation-triangle"></i></div>
            <div class="stat-body">
              <p class="stat-label">Sắp hết</p>
              <p class="stat-value warning">
                {{ summary.totalLowStock || 0 }}<span class="stat-unit">cái</span>
              </p>
            </div>
          </div>
          <div class="stat-card danger">
            <div class="stat-icon"><i class="bi bi-x-octagon"></i></div>
            <div class="stat-body">
              <p class="stat-label">Hết hàng</p>
              <p class="stat-value danger">
                {{ summary.totalOutOfStock || 0 }}<span class="stat-unit">cái</span>
              </p>
            </div>
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
                        v-if="!imeiScope"
                        class="act-btn warning"
                        title="Điều chỉnh tồn kho"
                        @click="openAdjustModal(item)"
                      >
                        <i class="bi bi-sliders"></i>
                      </button>
                      <button
                        v-if="imeiScope"
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

            <template v-if="imeiScope">
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
                    class="btn btn-sm btn-success"
                    :disabled="parsingExcel"
                    onclick="window.__importImeiFromExcel && window.__importImeiFromExcel()"
                  >
                    <i class="bi bi-file-earmark-excel me-1"></i>
                    {{ parsingExcel ? 'Đang đọc...' : 'Import từ Excel' }}
                  </button>
                </div>
              </div>
            </div>
            </template>

            <div class="mt-3">
              <label class="filter-label">Số lượng nhập <span class="text-danger">*</span></label>
              <input
                v-model.number="formImport.importQuantity"
                type="number"
                class="form-control f-input"
                min="1"
                placeholder="Số lượng = số IMEI đã nhập"
                :readonly="imeiScope"
                style="padding-left: 12px"
              />
              <small class="text-muted d-block mt-1">
                <i class="bi bi-lock me-1" v-if="imeiScope"></i>
                <span v-if="imeiScope">Số lượng tự động = số IMEI hợp lệ bạn đã nhập ở trên.</span>
                <span v-else>Nhập số lượng cần nhập kho cho SKU phụ kiện này.</span>
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

    <!-- Modal điều chỉnh tồn kho -->
    <div class="modal fade inv-modal" id="adjustModal" tabindex="-1" ref="adjustModalRef">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">
              <i class="bi bi-sliders me-2"></i>Điều chỉnh tồn kho
            </h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="filter-label">SKU</label>
              <input v-model="formAdjust.skuCode" type="text" class="form-control" readonly />
            </div>
            <div class="mb-3">
              <label class="filter-label">Sản phẩm</label>
              <input v-model="formAdjust.productName" type="text" class="form-control" readonly />
            </div>
            <div class="mb-3">
              <label class="filter-label">Tồn kho hiện tại</label>
              <input
                :value="formAdjust.currentStock ?? 0"
                type="text"
                class="form-control"
                readonly
              />
            </div>
            <div class="mb-3">
              <label class="filter-label d-flex align-items-center gap-1">
                Số lượng điều chỉnh
                <span class="text-danger">*</span>
              </label>
              <div class="input-group">
                <button class="btn btn-outline-danger" type="button" @click="decrementAdjust">−</button>
                <input
                  v-model.number="formAdjust.adjustQuantity"
                  type="number"
                  class="form-control text-center fw-bold"
                  :class="{ 'is-invalid': errAdjustQuantity }"
                  placeholder="VD: -3 hoặc +5"
                  style="max-width: 140px"
                />
                <button class="btn btn-outline-success" type="button" @click="incrementAdjust">+</button>
              </div>
              <div class="d-flex justify-content-between mt-1">
                <small v-if="errAdjustQuantity" class="text-danger">{{ errAdjustQuantity }}</small>
                <small v-else class="text-muted">Âm = giảm tồn, Dương = tăng tồn</small>
                <small class="text-muted">Tồn mới: {{ tentativeStock }}</small>
              </div>
            </div>
            <div class="mb-2">
              <label class="filter-label d-flex align-items-center gap-1">
                Lý do điều chỉnh <span class="text-danger">*</span>
              </label>
              <textarea
                v-model="formAdjust.reason"
                class="form-control"
                rows="3"
                placeholder="VD: Kiểm kê phát hiện thừa hàng, Hàng trả lại từ đơn hàng #123..."
                :class="{ 'is-invalid': errAdjustReason }"
                maxlength="500"
              ></textarea>
              <div class="d-flex justify-content-between mt-1">
                <small v-if="errAdjustReason" class="text-danger">{{ errAdjustReason }}</small>
                <small v-else class="text-muted">Bắt buộc. Ghi rõ lý do để đối soát.</small>
                <small class="text-muted">{{ (formAdjust.reason || '').length }}/500</small>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-outline-secondary" data-bs-dismiss="modal">Hủy</button>
            <button
              class="btn btn-warning"
              :disabled="submittingAdjust || !canSubmitAdjust"
              @click="submitAdjust"
            >
              <span v-if="submittingAdjust" class="spinner-border spinner-border-sm me-1"></span>
              Xác nhận điều chỉnh
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
import { useRoute, useRouter } from 'vue-router'
import { Modal } from 'bootstrap'
import BaseModal from '@/components/BaseModal.vue'
import api from '@/utils/api'
import { setupGlobalExcelImporter } from '@/utils/excelImport'
import '@/assets/css/Inventory.css'

const route = useRoute()
const router = useRouter()

// Loại kho: true = sản phẩm có IMEI, false = sản phẩm không IMEI
const imeiScope = computed(() => route.meta?.imeiScope === true)
const pageTitle = computed(() =>
  imeiScope.value ? 'Quản lý kho (có IMEI)' : 'Quản lý kho (không IMEI)',
)
const pageSub = computed(() =>
  imeiScope.value
    ? 'Theo dõi tồn kho và quản lý IMEI/Serial cho sản phẩm có quản lý IMEI.'
    : 'Theo dõi tồn kho cho sản phẩm phụ kiện, không cần quản lý IMEI.',
)

const BASE = '/admin/inventory'

const inventoryApi = {
  list: (params) => api.get(`${BASE}/inventory`, { params }),
  summary: (params) => api.get(`${BASE}/summary`, { params }),
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

const adjustModalRef = ref(null)
let adjustModalInstance = null
const submittingAdjust = ref(false)
const errAdjustQuantity = ref('')
const errAdjustReason = ref('')

const formImport = ref({
  skuId: null,
  skuCode: '',
  productName: '',
  importQuantity: null,
  imeiText: '',
  note: '',
})

const formAdjust = ref({
  skuId: null,
  skuCode: '',
  productName: '',
  currentStock: 0,
  adjustQuantity: 0,
  reason: '',
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
    if (imeiScope.value) {
      formImport.value.importQuantity = list.length
    }
  },
  { immediate: true },
)

const imeiCountMismatch = computed(() => {
  if (!imeiScope.value) return false
  const q = Number(formImport.value.importQuantity || 0)
  return parsedImeis.value.length !== q
})

const tentativeStock = computed(() => {
  const current = Number(formAdjust.value.currentStock ?? 0)
  const delta = Number(formAdjust.value.adjustQuantity ?? 0)
  return current + delta
})

const canSubmitImport = computed(() => {
  const q = Number(formImport.value.importQuantity || 0)
  if (!q || q <= 0) return false
  if (imeiScope.value && imeiCountMismatch.value) return false
  return true
})

const canSubmitAdjust = computed(() => {
  const q = Number(formAdjust.value.adjustQuantity ?? 0)
  const r = (formAdjust.value.reason || '').trim()
  return q !== 0 && r.length >= 5
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
    params.hasImei = imeiScope.value
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
    const res = await inventoryApi.summary({ hasImei: imeiScope.value })
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
  if (!imeiScope.value) return
  router.push({
    name: 'InventoryImeiManager',
    params: { skuId: item.skuId },
    query: {
      code: item.skuCode,
      name: item.productName,
      stock: item.stockQuantity,
      from: 'with-imei',
    },
  })
}

function switchScope() {
  const target = imeiScope.value
    ? 'InventoryManagerNoImei'
    : 'InventoryManagerWithImei'
  router.push({ name: target })
}

async function submitImport() {
  if (!formImport.value.importQuantity || formImport.value.importQuantity <= 0) {
    showToast('Số lượng nhập phải lớn hơn 0')
    return
  }
  if (imeiScope.value && imeiCountMismatch.value) {
    showToast('Số IMEI phải bằng số lượng nhập')
    return
  }
  submitting.value = true
  try {
    const payload = {
      skuId: formImport.value.skuId,
      importQuantity: formImport.value.importQuantity,
      note: formImport.value.note,
    }
    if (imeiScope.value) {
      payload.imeis = parsedImeis.value
    }
    await inventoryApi.import(payload)
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

function openAdjustModal(item) {
  formAdjust.value = {
    skuId: item?.skuId || null,
    skuCode: item?.skuCode || '',
    productName: item?.productName || '',
    currentStock: item?.stockQuantity ?? 0,
    adjustQuantity: 0,
    reason: '',
  }
  errAdjustQuantity.value = ''
  errAdjustReason.value = ''
  adjustModalInstance?.show()
}

function incrementAdjust() {
  formAdjust.value.adjustQuantity = (formAdjust.value.adjustQuantity || 0) + 1
}

function decrementAdjust() {
  const current = formAdjust.value.adjustQuantity || 0
  const newVal = current - 1
  if (formAdjust.value.currentStock + newVal < 0) {
    errAdjustQuantity.value = 'Tồn kho không thể âm'
    formAdjust.value.adjustQuantity = -formAdjust.value.currentStock
    return
  }
  errAdjustQuantity.value = ''
  formAdjust.value.adjustQuantity = newVal
}

async function submitAdjust() {
  errAdjustQuantity.value = ''
  errAdjustReason.value = ''
  const q = Number(formAdjust.value.adjustQuantity ?? 0)
  const r = (formAdjust.value.reason || '').trim()
  if (q === 0) {
    errAdjustQuantity.value = 'Số lượng điều chỉnh không được bằng 0'
    showToast(errAdjustQuantity.value)
    return
  }
  if (tentativeStock.value < 0) {
    errAdjustQuantity.value = 'Tồn kho không thể âm'
    showToast(errAdjustQuantity.value)
    return
  }
  if (r.length < 5) {
    errAdjustReason.value = 'Lý do phải từ 5 ký tự trở lên'
    showToast(errAdjustReason.value)
    return
  }
  submittingAdjust.value = true
  try {
    await inventoryApi.adjust({
      skuId: formAdjust.value.skuId,
      adjustmentQuantity: q,
      reason: r,
    })
    adjustModalInstance?.hide()
    showModal('success', 'Thành công', `Đã điều chỉnh ${q > 0 ? 'tăng' : 'giảm'} ${Math.abs(q)} sản phẩm`)
    await Promise.all([fetchList(), fetchSummary()])
  } catch (e) {
    showToast(e?.response?.data?.message || 'Điều chỉnh thất bại')
  } finally {
    submittingAdjust.value = false
  }
}

onMounted(() => {
  importModalInstance = new Modal(importModalRef.value)
  if (adjustModalRef.value) adjustModalInstance = new Modal(adjustModalRef.value)
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

watch(imeiScope, () => {
  currentPage.value = 0
  keyword.value = ''
  stockStatus.value = null
  fetchList()
  fetchSummary()
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