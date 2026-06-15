<template>
  <div class="container-fluid px-4 py-3">

    <!-- ═══════════════════════════════════════════
         HEADER
    ═══════════════════════════════════════════ -->
    <div class="page-header d-flex align-items-center justify-content-between mb-4">
      <div class="d-flex align-items-center gap-3">
        <div class="header-icon">
          <i class="bi bi-grid-fill"></i>
        </div>
        <div>
          <h4 class="header-title mb-0">Quản lý Danh mục</h4>
          <p class="header-sub mb-0">Quản lý danh mục sản phẩm, danh mục con và trạng thái hiển thị.</p>
        </div>
      </div>
      <button class="btn btn-pink" @click="openCreate">
        <i class="bi bi-plus-lg me-1"></i> Thêm Danh mục
      </button>
    </div>

    <!-- ═══════════════════════════════════════════
         STATS — 4 thẻ thống kê
    ═══════════════════════════════════════════ -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-md-3" v-for="stat in stats" :key="stat.label">
        <div class="stat-card">
          <p class="stat-label">{{ stat.label }}</p>
          <p class="stat-value" :class="{ 'pink': stat.highlight }">{{ stat.value }}</p>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════
         FILTER
    ═══════════════════════════════════════════ -->
    <div class="filter-card mb-4">
      <div class="row g-3 align-items-end">

        <div class="col-12 col-md-6">
          <label class="filter-label">TÌM KIẾM</label>
          <div class="input-wrapper">
            <i class="bi bi-search search-icon"></i>
            <input
              v-model="filter.search"
              type="text"
              class="form-control f-input"
              placeholder="Tìm theo tên danh mục..."
              @input="page = 1"
            />
          </div>
        </div>

        <div class="col-12 col-md-4">
          <label class="filter-label">TRẠNG THÁI</label>
          <select v-model="filter.status" class="form-select f-input" @change="page = 1">
            <option value="">Tất cả</option>
            <option value="true">Đang hiển thị</option>
            <option value="false">Đã ẩn</option>
          </select>
        </div>

        <div class="col-12 col-md-2">
          <button class="btn btn-reset" @click="onReset" title="Đặt lại bộ lọc">
            <i class="bi bi-arrow-clockwise"></i>
          </button>
        </div>

      </div>
    </div>

    <!-- ═══════════════════════════════════════════
         TABLE
    ═══════════════════════════════════════════ -->
    <div class="table-card">

      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border" style="color:#e91e63" role="status"></div>
        <p class="mt-2 text-muted small">Đang tải dữ liệu...</p>
      </div>

      <template v-else>
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th class="th">#</th>
                <th class="th">Tên danh mục</th>
                <th class="th">Slug</th>
                <th class="th">Danh mục cha</th>
                <th class="th text-center">Trạng thái</th>
                <th class="th text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!pagedRows.length">
                <td colspan="6" class="text-center py-5 text-muted">
                  <i class="bi bi-inbox fs-3 d-block mb-2 opacity-50"></i>
                  Không có dữ liệu
                </td>
              </tr>
              <tr v-for="(item, i) in pagedRows" :key="item.categoryId">
                <td class="text-muted small">{{ (page - 1) * PAGE_SIZE + i + 1 }}</td>
                <td class="fw-500">{{ item.categoryName }}</td>
                <td><span class="slug-badge">{{ item.slug }}</span></td>
                <td class="text-muted small">{{ item.parentName || '—' }}</td>
                <td class="text-center">
                  <span :class="item.status ? 'badge-on' : 'badge-off'">
                    {{ item.status ? 'Hiển thị' : 'Đã ẩn' }}
                  </span>
                </td>
                <td class="text-center">
                  <div class="d-flex justify-content-center gap-2">
                    <button class="act-btn edit-btn" title="Chỉnh sửa" @click="openEdit(item)">
                      <i class="bi bi-pencil"></i>
                    </button>
                    <button
                      class="act-btn hide-btn"
                      title="Ẩn danh mục"
                      :disabled="!item.status"
                      @click="onHide(item)"
                    >
                      <i class="bi bi-eye-slash"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="d-flex justify-content-between align-items-center px-3 pt-3 pb-2">
          <span class="text-muted small">
            Hiển thị {{ pagedRows.length }} / {{ filteredList.length }} danh mục
          </span>
          <nav>
            <ul class="pagination pagination-sm mb-0">
              <li class="page-item" :class="{ disabled: page === 1 }">
                <button class="page-link pg" @click="page--">‹</button>
              </li>
              <li v-for="p in totalPages" :key="p" class="page-item" :class="{ active: p === page }">
                <button class="page-link pg" @click="page = p">{{ p }}</button>
              </li>
              <li class="page-item" :class="{ disabled: page === totalPages }">
                <button class="page-link pg" @click="page++">›</button>
              </li>
            </ul>
          </nav>
        </div>
      </template>

    </div>

    <!-- ═══════════════════════════════════════════
         TOAST
    ═══════════════════════════════════════════ -->
    <div class="toast-container position-fixed bottom-0 end-0 p-3" style="z-index:9999">
      <div
        id="cateToast"
        class="toast align-items-center text-white border-0"
        :class="toast.type === 'success' ? 'bg-success' : 'bg-danger'"
        role="alert" aria-live="assertive" aria-atomic="true"
      >
        <div class="d-flex">
          <div class="toast-body fw-500">
            <i class="bi me-2" :class="toast.type === 'success' ? 'bi-check-circle' : 'bi-x-circle'"></i>
            {{ toast.msg }}
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto"></button>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════
         MODAL thêm / sửa
    ═══════════════════════════════════════════ -->
    <div class="modal fade" id="cateModal" tabindex="-1" ref="modalEl">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4 border-0 shadow-lg">

          <div class="modal-header border-0 pb-0">
            <h5 class="fw-bold mb-0" style="color:#e91e63">
              <i class="bi me-2" :class="isEdit ? 'bi-pencil-square' : 'bi-plus-circle-fill'"></i>
              {{ isEdit ? 'Chỉnh sửa danh mục' : 'Thêm danh mục mới' }}
            </h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>

          <div class="modal-body pt-3">

            <div class="mb-3">
              <label class="flabel">Tên danh mục <span class="text-danger">*</span></label>
              <input
                v-model="form.categoryName"
                type="text"
                class="form-control finput"
                :class="{ 'is-invalid': err.categoryName }"
                placeholder="Nhập tên danh mục..."
              />
              <div class="invalid-feedback">{{ err.categoryName }}</div>
            </div>

            <div class="mb-3">
              <label class="flabel">Danh mục cha</label>
              <select v-model="form.parentId" class="form-select finput">
                <option :value="null">— Không có (danh mục gốc) —</option>
                <option v-for="c in parentOptions" :key="c.categoryId" :value="c.categoryId">
                  {{ c.categoryName }}
                </option>
              </select>
            </div>

            <div class="mb-2" v-if="isEdit">
              <label class="flabel">Trạng thái</label>
              <div class="d-flex gap-4 mt-1">
                <div class="form-check">
                  <input class="form-check-input" type="radio" :value="true" v-model="form.status" id="sOn" />
                  <label class="form-check-label" for="sOn">Hiển thị</label>
                </div>
                <div class="form-check">
                  <input class="form-check-input" type="radio" :value="false" v-model="form.status" id="sOff" />
                  <label class="form-check-label" for="sOff">Ẩn</label>
                </div>
              </div>
            </div>

          </div>

          <div class="modal-footer border-0 pt-1">
            <button class="btn btn-outline-secondary rounded-3" data-bs-dismiss="modal">Hủy</button>
            <button class="btn btn-pink rounded-3" :disabled="saving" @click="onSubmit">
              <span v-if="saving" class="spinner-border spinner-border-sm me-1"></span>
              {{ isEdit ? 'Cập nhật' : 'Thêm mới' }}
            </button>
          </div>

        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { Modal, Toast } from 'bootstrap'
import api from '@/utils/api'

const BASE_URL = '/admin/categories'

const categoryApi = {

  getAll: () =>
    api.get(BASE_URL, { params: { page: 0, size: 999 } }),

  create: (payload) =>
    api.post(BASE_URL, payload),

  update: (id, payload) =>
    api.put(`${BASE_URL}/${id}`, payload),

  hide: (id) =>
    api.put(`${BASE_URL}/hidden/${id}`),
}

const PAGE_SIZE     = 10
const allCategories = ref([])
const loading       = ref(false)
const saving        = ref(false)
const page          = ref(1)
const filter        = ref({ search: '', status: '' })
const isEdit        = ref(false)
const editId        = ref(null)
const form          = ref({ categoryName: '', parentId: null, status: true })
const err           = ref({})
const toast         = ref({ msg: '', type: 'success' })
const modalEl       = ref(null)
let bsModal = null
let bsToast = null

const stats = computed(() => [
  { label: 'Tổng danh mục', value: allCategories.value.length,                                  highlight: false },
  { label: 'Đang hiển thị', value: allCategories.value.filter(c => c.status).length,            highlight: true  },
  { label: 'Đã ẩn',         value: allCategories.value.filter(c => !c.status).length,           highlight: false },
  { label: 'Danh mục con',  value: allCategories.value.filter(c => c.parentId != null).length,  highlight: false },
])

const filteredList = computed(() => {
  let list = allCategories.value
  const q = filter.value.search.trim().toLowerCase()
  if (q) list = list.filter(c => c.categoryName.toLowerCase().includes(q))
  if (filter.value.status !== '') {
    list = list.filter(c => c.status === (filter.value.status === 'true'))
  }
  return list
})

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredList.value.length / PAGE_SIZE))
)

const pagedRows = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filteredList.value.slice(start, start + PAGE_SIZE)
})

const parentOptions = computed(() =>
  allCategories.value.filter(c => c.categoryId !== editId.value)
)

async function fetchAll() {
  loading.value = true
  try {
    const res     = await categoryApi.getAll()
    const payload = res.data?.data ?? res.data
    allCategories.value = Array.isArray(payload) ? payload : (payload.content ?? [])
  } catch {
    showToast('Không thể tải danh sách danh mục', 'error')
  } finally {
    loading.value = false
  }
}

async function doCreate() {
  saving.value = true
  try {
    await categoryApi.create({ categoryName: form.value.categoryName, parentId: form.value.parentId })
    showToast('Thêm danh mục thành công!', 'success')
    bsModal.hide()
    await fetchAll()
  } catch (e) {
    showToast(e.response?.data?.message ?? 'Lỗi khi thêm danh mục', 'error')
  } finally {
    saving.value = false
  }
}

async function doUpdate() {
  saving.value = true
  try {
    await categoryApi.update(editId.value, {
      categoryName: form.value.categoryName,
      parentId:     form.value.parentId,
      status:       form.value.status,
    })
    showToast('Cập nhật thành công!', 'success')
    bsModal.hide()
    await fetchAll()
  } catch (e) {
    showToast(e.response?.data?.message ?? 'Lỗi khi cập nhật', 'error')
  } finally {
    saving.value = false
  }
}

async function onHide(item) {
  if (!confirm(`Ẩn danh mục "${item.categoryName}"?`)) return
  try {
    await categoryApi.hide(item.categoryId)
    showToast('Đã ẩn danh mục', 'success')
    await fetchAll()
  } catch (e) {
    showToast(e.response?.data?.message ?? 'Lỗi khi ẩn danh mục', 'error')
  }
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.value   = { categoryName: '', parentId: null, status: true }
  err.value    = {}
  bsModal.show()
}

function openEdit(item) {
  isEdit.value = true
  editId.value = item.categoryId
  form.value   = { categoryName: item.categoryName, parentId: item.parentId ?? null, status: item.status }
  err.value    = {}
  bsModal.show()
}

function validate() {
  err.value = {}
  if (!form.value.categoryName.trim()) {
    err.value.categoryName = 'Tên danh mục không được để trống'
    return false
  }
  return true
}

function onSubmit() {
  if (!validate()) return
  isEdit.value ? doUpdate() : doCreate()
}

function showToast(msg, type = 'success') {
  toast.value = { msg, type }
  nextTick(() => {
    if (!bsToast) bsToast = new Toast(document.getElementById('cateToast'), { delay: 3000 })
    bsToast.show()
  })
}

function onReset() {
  filter.value = { search: '', status: '' }
  page.value   = 1
}

onMounted(async () => {
  bsModal = new Modal(modalEl.value)
  await fetchAll()
})
</script>

<style scoped>
.page-header {
  background: #fff; border-radius: 14px; padding: 20px 24px;
  border: 1px solid #fce4ec; box-shadow: 0 1px 6px rgba(233,30,99,.07);
  display: flex; align-items: center; justify-content: space-between;
}
.header-icon {
  width: 48px; height: 48px; background: #e91e63; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 1.3rem; flex-shrink: 0;
}
.header-title { font-size: 1.2rem; font-weight: 700; color: #e91e63; }
.header-sub   { font-size: .82rem; color: #aaa; }

.btn-pink {
  background: #e91e63; color: #fff; border: none;
  border-radius: 10px; font-weight: 600; font-size: .9rem;
  padding: 10px 20px; transition: background .2s;
}
.btn-pink:hover    { background: #c2185b; color: #fff; }
.btn-pink:disabled { opacity: .7; }

.stat-card {
  background: #fff; border-radius: 12px; padding: 20px 24px;
  border: 1px solid #fce4ec; box-shadow: 0 1px 6px rgba(233,30,99,.07);
}
.stat-label { font-size: .8rem; color: #999; font-weight: 500; margin-bottom: 6px; }
.stat-value { font-size: 2rem; font-weight: 700; color: #222; margin: 0; line-height: 1; }
.stat-value.pink { color: #e91e63; }

.filter-card {
  background: #fff; border-radius: 12px; padding: 20px 24px;
  border: 1px solid #fce4ec; box-shadow: 0 1px 6px rgba(233,30,99,.07);
}
.filter-label {
  display: block; font-size: .72rem; font-weight: 700;
  color: #e91e63; letter-spacing: .08em; margin-bottom: 6px;
}
.input-wrapper { position: relative; }
.search-icon {
  position: absolute; left: 12px; top: 50%;
  transform: translateY(-50%); color: #bbb; font-size: .9rem;
}
.f-input { border-radius: 8px; border: 1px solid #f0c0d0; font-size: .9rem; height: 42px; }
.f-input:focus { border-color: #e91e63; box-shadow: 0 0 0 3px rgba(233,30,99,.1); }
input.f-input { padding-left: 36px; }
.btn-reset {
  width: 42px; height: 42px; border-radius: 8px;
  border: 1px solid #f0c0d0; background: #fff5f8; color: #e91e63;
  font-size: 1rem; display: flex; align-items: center; justify-content: center;
}
.btn-reset:hover { background: #fce4ec; }

.table-card {
  background: #fff; border-radius: 12px;
  border: 1px solid #fce4ec; box-shadow: 0 1px 6px rgba(233,30,99,.07);
  overflow: hidden; padding-bottom: 4px;
}
.th {
  background: #fff5f8; color: #e91e63;
  font-size: .75rem; font-weight: 700; letter-spacing: .05em;
  border-bottom: 2px solid #fce4ec !important;
  padding: 12px 16px; white-space: nowrap;
}
.table tbody tr:hover { background: #fff9fb; }
.fw-500 { font-weight: 500; }
.slug-badge {
  background: #fce4ec; color: #c2185b;
  font-size: .74rem; padding: 3px 8px;
  border-radius: 6px; font-family: monospace;
}
.badge-on {
  background: #e8f5e9; color: #2e7d32; font-size: .78rem; font-weight: 600;
  padding: 4px 10px; border-radius: 20px; display: inline-block;
}
.badge-off {
  background: #f5f5f5; color: #9e9e9e; font-size: .78rem; font-weight: 600;
  padding: 4px 10px; border-radius: 20px; display: inline-block;
}
.act-btn {
  width: 32px; height: 32px; border-radius: 7px; border: none;
  font-size: .85rem; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background .15s;
}
.edit-btn { background: #fce4ec; color: #e91e63; }
.edit-btn:hover { background: #f48fb1; color: #fff; }
.hide-btn { background: #f5f5f5; color: #757575; }
.hide-btn:hover:not(:disabled) { background: #e0e0e0; }
.hide-btn:disabled { opacity: .4; cursor: not-allowed; }

.pg { color: #e91e63; border-color: #fce4ec; }
.pg:hover { background: #fce4ec; color: #e91e63; }
.page-item.active .pg {
  background: #e91e63 !important; border-color: #e91e63 !important; color: #fff !important;
}

.flabel { display: block; font-size: .82rem; font-weight: 600; color: #555; margin-bottom: 5px; }
.finput { border-radius: 8px; border: 1px solid #f0c0d0; font-size: .9rem; }
.finput:focus { border-color: #e91e63; box-shadow: 0 0 0 3px rgba(233,30,99,.1); }
</style>