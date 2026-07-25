<template>
  <div class="imei-page">
    <!-- Header -->
    <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
      <div>
        <button class="btn btn-back" @click="goBack">
          <i class="bi bi-arrow-left me-1"></i>Quay lại
        </button>
      </div>
      <button class="btn btn-pink" :disabled="addingOne" @click="openAddOneModal">
        <i class="bi bi-plus-lg me-1"></i>Thêm IMEI lẻ
      </button>
    </div>

    <!-- Info SKU -->
    <div class="sku-info-card mb-3">
      <div class="row g-3 align-items-center">
        <div class="col-md-1 col-3">
          <div class="sku-thumb">
            <i class="bi bi-upc-scan"></i>
          </div>
        </div>
        <div class="col-md-7 col-9">
          <p class="stat-label mb-1">SKU</p>
          <p class="sku-title">{{ skuInfo.code }}</p>
          <p class="text-muted mb-0">{{ skuInfo.name }}</p>
        </div>
        <div class="col-md-4">
          <div class="row text-center g-2">
            <div class="col-3">
              <p class="stat-label">Tổng</p>
              <p class="imei-stat pink">{{ counts.total }}</p>
            </div>
            <div class="col-3">
              <p class="stat-label">Trong kho</p>
              <p class="imei-stat success">{{ counts.inStock }}</p>
            </div>
            <div class="col-3">
              <p class="stat-label">Đã bán</p>
              <p class="imei-stat warning">{{ counts.sold }}</p>
            </div>
            <div class="col-3">
              <p class="stat-label">Lỗi</p>
              <p class="imei-stat danger">{{ counts.error }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Filter IMEI -->
    <div class="filter-card">
      <div class="row g-3 align-items-end">
        <div class="col-md-6">
          <label class="filter-label">Tìm IMEI</label>
          <div class="input-wrapper">
            <i class="bi bi-search search-icon"></i>
            <input
              v-model="keyword"
              type="search"
              class="form-control f-input"
              placeholder="Nhập mã IMEI cần tìm..."
            />
          </div>
        </div>
        <div class="col-md-4">
          <label class="filter-label">Trạng thái</label>
          <select v-model="statusFilter" class="form-select f-input">
            <option :value="null">Tất cả</option>
            <option :value="1">Trong kho</option>
            <option :value="2">Đã bán</option>
            <option :value="3">Bảo hành</option>
            <option :value="4">Lỗi</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-reset" @click="resetFilter" title="Đặt lại">
            <i class="bi bi-arrow-clockwise"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="table-card mt-3">
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border spinner-pink" role="status"></div>
        <p class="mt-2 text-muted small">Đang tải danh sách IMEI...</p>
      </div>

      <template v-else>
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0 inv-table">
            <thead>
              <tr>
                <th class="th text-center" style="width: 60px">STT</th>
                <th class="th">IMEI / Serial</th>
                <th class="th">Trạng thái</th>
                <th class="th">Mã đơn hàng</th>
                <th class="th">Ngày tạo</th>
                <th class="th">Cập nhật</th>
                <th class="th text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!pagedItems.length">
                <td colspan="7" class="text-center py-5 text-muted">
                  <div class="empty-state">
                    <i class="bi bi-upc-scan"></i>
                    <h5>Chưa có IMEI nào cho SKU này</h5>
                    <p class="small mb-0">Hãy nhập kho hoặc thêm IMEI lẻ.</p>
                  </div>
                </td>
              </tr>
              <tr v-for="(item, idx) in pagedItems" :key="item.itemId">
                <td class="text-center text-muted">{{ currentPage * pageSize + idx + 1 }}</td>
                <td><span class="sku-code">{{ item.imeiCode }}</span></td>
                <td>
                  <span class="status-text" :class="statusBadgeClass(item.status)">
                    {{ item.statusLabel }}
                  </span>
                </td>
                <td>
                  <span v-if="item.orderItemId" class="badge bg-light text-dark">
                    #{{ item.orderItemId }}
                  </span>
                  <span v-else class="text-muted small">—</span>
                </td>
                <td class="small text-muted">{{ formatDate(item.createdAt) }}</td>
                <td class="small text-muted">{{ formatDate(item.updatedAt) }}</td>
                <td class="text-center">
                  <div class="d-flex justify-content-center gap-2">
                    <button
                      class="act-btn"
                      title="Sửa"
                      @click="openEditModal(item)"
                    >
                      <i class="bi bi-pencil"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <nav v-if="totalElements > 0" class="pagination-bar">
          <div class="pagination-total">
            Tổng <strong>{{ totalElements }}</strong> IMEI
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

    <!-- Modal thêm / sửa IMEI -->
    <div class="modal fade inv-modal" tabindex="-1" ref="imeiModalRef">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">
              <i class="bi bi-upc-scan me-2"></i>
              {{ editing ? 'Cập nhật IMEI' : 'Thêm IMEI lẻ' }}
            </h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="filter-label">IMEI / Serial <span class="text-danger">*</span></label>
              <input
                v-model="form.imeiCode"
                type="text"
                class="form-control f-input"
                placeholder="Nhập mã IMEI"
                style="padding-left: 12px"
              />
            </div>
            <div class="mb-3">
              <label class="filter-label">Trạng thái</label>
              <select v-model.number="form.status" class="form-select f-input">
                <option :value="1">Trong kho</option>
                <option :value="2">Đã bán</option>
                <option :value="3">Bảo hành</option>
                <option :value="4">Lỗi</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-outline-secondary" data-bs-dismiss="modal">Hủy</button>
            <button class="btn btn-pink" :disabled="submitting" @click="submitForm">
              <span v-if="submitting" class="spinner-border spinner-border-sm me-1"></span>
              {{ editing ? 'Lưu thay đổi' : 'Thêm IMEI' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="inv-toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal } from 'bootstrap'
import api from '@/utils/api'
import '@/assets/css/Inventory.css'

const route = useRoute()
const router = useRouter()

const skuId = computed(() => Number(route.params.skuId))
const skuInfo = ref({
  code: route.query.code || '',
  name: route.query.name || '',
  stock: Number(route.query.stock || 0),
})

const BASE = '/admin/inventory'
const itemApi = {
  list: (id) => api.get(`${BASE}/${id}/items`),
  create: (id, data) => api.post(`${BASE}/${id}/items`, data),
  update: (itemId, data) => api.put(`${BASE}/items/${itemId}`, data),
}

const items = ref([])
const loading = ref(false)
const submitting = ref(false)
const addingOne = ref(false)
const toastMessage = ref('')

const keyword = ref('')
const statusFilter = ref(null)

const currentPage = ref(0)
const pageSize = ref(5)

const imeiModalRef = ref(null)
let imeiModalInstance = null

const editing = ref(false)
const form = ref({ itemId: null, imeiCode: '', status: 1 })

const counts = computed(() => {
  const c = { total: items.value.length, inStock: 0, sold: 0, warranty: 0, error: 0 }
  for (const it of items.value) {
    if (it.status === 1) c.inStock++
    else if (it.status === 2) c.sold++
    else if (it.status === 3) c.warranty++
    else if (it.status === 4) c.error++
  }
  return c
})

const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return items.value.filter((it) => {
    if (statusFilter.value != null && it.status !== statusFilter.value) return false
    if (kw && !(it.imeiCode || '').toLowerCase().includes(kw)) return false
    return true
  })
})

const totalElements = computed(() => filteredItems.value.length)
const totalPages = computed(() => Math.max(1, Math.ceil(totalElements.value / pageSize.value)))
const pagedItems = computed(() => {
  const start = currentPage.value * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})

function showToast(msg) {
  toastMessage.value = msg
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => (toastMessage.value = ''), 2600)
}

function statusBadgeClass(status) {
  if (status === 1) return 'in-stock'
  if (status === 2) return 'out-stock'
  if (status === 3) return 'low-stock'
  if (status === 4) return 'status-error'
  return ''
}

function formatDate(iso) {
  if (!iso) return ''
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return ''
  }
}

async function fetchItems() {
  loading.value = true
  try {
    const res = await itemApi.list(skuId.value)
    items.value = res.data?.data || []
    currentPage.value = 0
  } catch (e) {
    showToast(e?.response?.data?.message || 'Không thể tải danh sách IMEI')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push({ name: 'InventoryManager' })
}

function goToPage(p) {
  if (p < 0 || p >= totalPages.value) return
  currentPage.value = p
}

function onSizeChange() {
  currentPage.value = 0
}

function resetFilter() {
  keyword.value = ''
  statusFilter.value = null
  currentPage.value = 0
}

function openAddOneModal() {
  editing.value = false
  form.value = { itemId: null, imeiCode: '', status: 1 }
  imeiModalInstance?.show()
}

function openEditModal(item) {
  editing.value = true
  form.value = {
    itemId: item.itemId,
    imeiCode: item.imeiCode,
    status: item.status || 1,
  }
  imeiModalInstance?.show()
}

async function submitForm() {
  if (!form.value.imeiCode?.trim()) {
    showToast('Vui lòng nhập mã IMEI')
    return
  }
  submitting.value = true
  try {
    if (editing.value) {
      await itemApi.update(form.value.itemId, {
        imeiCode: form.value.imeiCode.trim(),
        status: form.value.status,
      })
    } else {
      await itemApi.create(skuId.value, {
        imeiCode: form.value.imeiCode.trim(),
        status: form.value.status,
      })
    }
    imeiModalInstance?.hide()
    showToast(editing.value ? 'Cập nhật thành công' : 'Thêm IMEI thành công')
    await fetchItems()
    window.dispatchEvent(
      new CustomEvent('inventory:imei-changed', {
        detail: { skuId: skuId.value, action: editing.value ? 'update' : 'create' },
      }),
    )
  } catch (e) {
    showToast(e?.response?.data?.message || 'Thao tác thất bại')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  imeiModalInstance = new Modal(imeiModalRef.value)
  fetchItems()
})
</script>

<style scoped>
.sku-info-card {
  background: #fff;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  padding: 18px;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.06);
}

.sku-thumb {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff0f7, #fde2ec);
  color: #f55d9b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.6rem;
}

.sku-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #b4557d;
  margin: 0;
  font-family: monospace;
}

.imei-stat {
  font-size: 1.4rem;
  font-weight: 800;
  margin: 4px 0 0;
  line-height: 1;
}
.imei-stat.pink { color: #f55d9b; }
.imei-stat.success { color: #10b981; }
.imei-stat.warning { color: #f59e0b; }
.imei-stat.danger { color: #dc2626; }

.btn-back {
  background: #fff;
  border: 1px solid #f3d6e3;
  color: #b4557d;
  padding: 8px 14px;
  border-radius: 8px;
  font-weight: 600;
}
.btn-back:hover { background: #fff0f7; color: #d63384; }

.btn-reset {
  width: 100%;
  height: 42px;
  border: 1px solid #f3d6e3;
  border-radius: 8px;
  background: #fff;
  color: #b4557d;
}
.btn-reset:hover { background: #fff0f7; color: #d63384; }

/* Status Lỗi (4) - đỏ */
.status-text.status-error {
  color: #dc2626;
  background: #fee2e2;
  border: 1px solid #fca5a5;
}

.status-text.out-stock {
  color: #f59e0b;
}

@media (max-width: 575.98px) {
  .imei-stat { font-size: 1.1rem; }
  .stat-label { font-size: 0.72rem; }
}
</style>
