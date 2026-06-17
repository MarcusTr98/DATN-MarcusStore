<template>
  <div class="container-fluid px-4 py-3">
    <div class="page-header d-flex align-items-center justify-content-between mb-4">
      <div class="d-flex align-items-center gap-3">
        <div class="header-icon">
          <i class="bi bi-grid-fill"></i>
        </div>
        <div>
          <h4 class="header-title mb-0">Quản lý Danh mục</h4>
          <p class="header-sub mb-0">
            Quản lý danh mục sản phẩm, danh mục con và trạng thái hiển thị.
          </p>
        </div>
      </div>
      <button class="btn btn-pink" @click="openCreate">
        <i class="bi bi-plus-lg me-1"></i> Thêm Danh mục
      </button>
    </div>

    <div class="row g-3 mb-4">
      <div class="col-6 col-md-3" v-for="stat in stats" :key="stat.label">
        <div class="stat-card">
          <p class="stat-label">{{ stat.label }}</p>
          <p class="stat-value" :class="{ pink: stat.highlight }">{{ stat.value }}</p>
        </div>
      </div>
    </div>

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

    <div class="table-card">
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border" style="color: #e91e63" role="status"></div>
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

                <td>
                  <div class="cat-name" :style="{ paddingLeft: item.level * 28 + 'px' }">

                    <i
                      v-if="item.hasChildren"
                      class="bi bi-folder2-open cat-toggle"
                      :class="{ 'is-leaf': !item.hasChildren }"
                      title="Có danh mục con"
                    ></i>
                    <i
                      v-else
                      class="bi bi-circle cat-toggle is-leaf"
                      style="font-size: 0.35rem"
                    ></i>

                    <span :class="item.level === 0 ? 'cat-root' : 'cat-child'">
                      {{ item.categoryName }}
                    </span>

                    <span v-if="item.hasChildren" class="badge-count">
                      {{ countAllChildren(item) }} con
                    </span>
                  </div>
                </td>

                <td>
                  <span class="slug-badge">{{ item.slug }}</span>
                </td>
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

        <div
          v-if="totalPages > 1"
          class="d-flex justify-content-between align-items-center px-3 pt-3 pb-2"
        >
          <span class="text-muted small">
            Hiển thị {{ pagedRows.length }} / {{ filteredList.length }} danh mục
          </span>
          <nav>
            <ul class="pagination pagination-sm mb-0">
              <li class="page-item" :class="{ disabled: page === 1 }">
                <button class="page-link pg" @click="page--">‹</button>
              </li>
              <li
                v-for="p in totalPages"
                :key="p"
                class="page-item"
                :class="{ active: p === page }"
              >
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

    <div class="toast-container position-fixed top-0 end-0 p-3 mt-3" style="z-index: 9999">
      <div
        id="cateToast"
        class="toast align-items-center text-white border-0"
        :class="toast.type === 'success' ? 'bg-success' : 'bg-danger'"
        role="alert"
        aria-live="assertive"
        aria-atomic="true"
      >
        <div class="d-flex">
          <div class="toast-body fw-500">
            <i
              class="bi me-2"
              :class="toast.type === 'success' ? 'bi-check-circle' : 'bi-x-circle'"
            ></i>
            {{ toast.msg }}
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto"></button>
        </div>
      </div>
    </div>

    <div class="modal fade" id="cateModal" tabindex="-1" ref="modalEl">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4 border-0 shadow-lg">
          <div class="modal-header border-0 pb-0">
            <h5 class="fw-bold mb-0" style="color: #e91e63">
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
                  <input
                    class="form-check-input"
                    type="radio"
                    :value="true"
                    v-model="form.status"
                    id="sOn"
                  />
                  <label class="form-check-label" for="sOn">Hiển thị</label>
                </div>
                <div class="form-check">
                  <input
                    class="form-check-input"
                    type="radio"
                    :value="false"
                    v-model="form.status"
                    id="sOff"
                  />
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
import '@/assets/css/Category.css'

const BASE_URL = '/admin/categories'

const categoryApi = {
  getAll: () => api.get(BASE_URL, { params: { page: 0, size: 999 } }),

  create: (payload) => api.post(BASE_URL, payload),

  update: (id, payload) => api.put(`${BASE_URL}/${id}`, payload),

  hide: (id) => api.put(`${BASE_URL}/hidden/${id}`),
}

const PAGE_SIZE = 5
const allCategories = ref([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const filter = ref({ search: '', status: '' })
const isEdit = ref(false)
const editId = ref(null)
const form = ref({ categoryName: '', parentId: null, status: true })
const err = ref({})
const toast = ref({ msg: '', type: 'success' })
const modalEl = ref(null)
let bsModal = null
let bsToast = null

const stats = computed(() => [
  { label: 'Tổng danh mục', value: allCategories.value.length, highlight: false },
  {
    label: 'Đang hiển thị',
    value: allCategories.value.filter((c) => c.status).length,
    highlight: true,
  },
  { label: 'Đã ẩn', value: allCategories.value.filter((c) => !c.status).length, highlight: false },
  {
    label: 'Danh mục con',
    value: allCategories.value.filter((c) => c.parentId != null).length,
    highlight: false,
  },
])

const filteredList = computed(() => {
  let list = allCategories.value
  const q = filter.value.search.trim().toLowerCase()
  if (q) list = list.filter((c) => c.categoryName.toLowerCase().includes(q))
  if (filter.value.status !== '') {
    list = list.filter((c) => c.status === (filter.value.status === 'true'))
  }
  return list
})

function buildTree(list) {
  const map = new Map()
  const roots = []
  list.forEach(c => map.set(c.categoryId, { ...c, children: [] }))
  list.forEach(c => {
    const node = map.get(c.categoryId)
    if (c.parentId != null && map.has(c.parentId)) {
      map.get(c.parentId).children.push(node)
    } else {
      roots.push(node)
    }
  })
  const sort = arr => arr.sort((a, b) => a.categoryName.localeCompare(b.categoryName, 'vi'))
  sort(roots)
  roots.forEach(r => sort(r.children))
  return roots
}
// 2. Flatten cây → mảng phẳng, có level + hasChildren
function flattenTree(nodes, level = 0, out = []) {
  for (const n of nodes) {
    out.push({ ...n, level, hasChildren: n.children.length > 0 })
    if (n.children.length) flattenTree(n.children, level + 1, out)
  }
  return out
}
// 3. Đếm tổng con cháu (đệ quy)
function countAllChildren(node) {
  let total = node.children.length
  for (const child of node.children) total += countAllChildren(child)
  return total
}
// 4. Tree rows đã flatten theo thứ tự cây
const treeRows = computed(() => flattenTree(buildTree(filteredList.value)))
const parentOptions = computed(() =>
  allCategories.value.filter((c) => c.categoryId !== editId.value),
)
// 5. Phân trang trên treeRows (thay thế computed cũ)
const totalPages = computed(() => Math.max(1, Math.ceil(treeRows.value.length / PAGE_SIZE)))
const pagedRows = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return treeRows.value.slice(start, start + PAGE_SIZE)
})

async function fetchAll() {
  loading.value = true
  try {
    const res = await categoryApi.getAll()
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
    await categoryApi.create({
      categoryName: form.value.categoryName,
      parentId: form.value.parentId,
    })
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
      parentId: form.value.parentId,
      status: form.value.status,
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
  form.value = { categoryName: '', parentId: null, status: true }
  err.value = {}
  bsModal.show()
}

function openEdit(item) {
  isEdit.value = true
  editId.value = item.categoryId
  form.value = {
    categoryName: item.categoryName,
    parentId: item.parentId ?? null,
    status: item.status,
  }
  err.value = {}
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
  page.value = 1
}

onMounted(async () => {
  bsModal = new Modal(modalEl.value)
  await fetchAll()
})
</script>

<style scoped>
</style>
