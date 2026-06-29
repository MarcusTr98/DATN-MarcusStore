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
              @input="onSearch"
            />
          </div>
        </div>

        <div class="col-12 col-md-4">
          <label class="filter-label">TRẠNG THÁI</label>
          <select v-model="filter.status" class="form-select f-input" @change="onSearch">
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
                <th class="th">Ảnh</th>
                <th class="th">Tên danh mục</th>
                <th class="th">Slug</th>
                <th class="th">Danh mục cha</th>
                <th class="th text-center">Trạng thái</th>
                <th class="th text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!pagedRows.length">
                <td colspan="7" class="text-center py-5 text-muted">
                  <i class="bi bi-inbox fs-3 d-block mb-2 opacity-50"></i>
                  Không có dữ liệu
                </td>
              </tr>
              <tr v-for="(item, i) in pagedRows" :key="item.categoryId">
                <td class="text-muted small">{{ currentPage * pageSize + i + 1 }}</td>

                <td>
                  <img
                    v-if="item.categoryImg"
                    :src="item.categoryImg"
                    class="thumb-img"
                    :alt="item.categoryName"
                  />
                  <div v-else class="thumb-placeholder">
                    <i class="bi bi-image"></i>
                  </div>
                </td>

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

        <!-- Pagination -->
        <div v-if="totalElements > 0" class="pagination-bar">
          <div class="pagination-total">
            Tổng <strong>{{ totalElements }}</strong> danh mục
          </div>

          <div class="pagination-actions">
            <label class="page-size-box">
              <span>Hiển thị</span>
              <select v-model.number="pageSize">
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
        </div>
      </template>
    </div>

    <BaseModal
      :visible="baseModal.visible"
      :show-confirm="baseModal.type === 'confirm'"
      :type="baseModal.type"
      :title="baseModal.title"
      :message="baseModal.message"
      @close="onModalClose"
      @confirm="onModalConfirm"
    />

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

            <!-- Logo danh mục -->
            <div class="mb-3">
              <label class="flabel">Ảnh logo danh mục</label>
              <div class="upload-section">
                <div class="input-group">
                  <input
                    type="file"
                    class="form-control finput"
                    accept="image/*"
                    ref="logoFileInput"
                    @change="onLogoSelect"
                  />
                </div>

                <div v-if="logoPreview" class="upload-preview mt-3">
                  <img :src="logoPreview" alt="preview" class="preview-thumb" />
                  <span class="preview-name ms-2 text-muted small">{{ logoFile?.name }}</span>
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-danger ms-auto"
                    @click="clearLogo"
                  >
                    <i class="bi bi-x"></i>
                  </button>
                </div>

                <div v-else-if="isEdit && currentLogoUrl" class="upload-preview mt-3">
                  <img :src="currentLogoUrl" alt="current logo" class="preview-thumb" />
                  <span class="preview-name ms-2 text-muted small">Ảnh hiện tại</span>
                </div>
              </div>
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
import { ref, computed, watch, onMounted } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import { Modal } from 'bootstrap'
import api from '@/utils/api'
import '@/assets/css/Category.css'

const BASE_URL = '/admin/categories'

const categoryApi = {
  getAll: () => api.get(BASE_URL, { params: { page: 0, size: 999 } }),

  create: (formData) =>
    api.post(BASE_URL, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  update: (id, formData) =>
    api.put(`${BASE_URL}/${id}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  hide: (id) => api.put(`${BASE_URL}/hidden/${id}`),
}

const allCategories = ref([])
const loading = ref(false)
const saving = ref(false)
const filter = ref({ search: '', status: '' })
const isEdit = ref(false)
const editId = ref(null)
const form = ref({ categoryName: '', parentId: null, status: true })
const err = ref({})
const modalEl = ref(null)
let bsModal = null

// Pagination state
const currentPage = ref(0)
const pageSize = ref(5)
const totalElements = ref(0)

// Modal state
const baseModal = ref({
  visible: false,
  type: 'error',
  title: '',
  message: '',
  onConfirm: null,
})

// Logo state
const logoFileInput = ref(null)
const logoFile = ref(null)
const logoPreview = ref(null)
const currentLogoUrl = ref(null)

function onLogoSelect(event) {
  const file = event.target.files[0]
  if (!file) return
  logoFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    logoPreview.value = e.target.result
  }
  reader.readAsDataURL(file)
}

function clearLogo() {
  logoFile.value = null
  logoPreview.value = null
  if (logoFileInput.value) logoFileInput.value.value = ''
}

function buildCategoryFormData(payload, file) {
  const formData = new FormData()
  formData.append('categoryName', payload.categoryName)
  if (payload.parentId !== null && payload.parentId !== undefined) {
    formData.append('parentId', payload.parentId)
  }
  if (payload.status !== undefined) {
    formData.append('status', payload.status)
  }
  if (file) formData.append('file', file)
  return formData
}

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
  list.forEach((c) => map.set(c.categoryId, { ...c, children: [] }))
  list.forEach((c) => {
    const node = map.get(c.categoryId)
    if (c.parentId != null && map.has(c.parentId)) {
      map.get(c.parentId).children.push(node)
    } else {
      roots.push(node)
    }
  })
  const sort = (arr) => arr.sort((a, b) => a.categoryName.localeCompare(b.categoryName, 'vi'))
  sort(roots)
  roots.forEach((r) => sort(r.children))
  return roots
}

function flattenTree(nodes, level = 0, out = []) {
  for (const n of nodes) {
    out.push({ ...n, level, hasChildren: n.children.length > 0 })
    if (n.children.length) flattenTree(n.children, level + 1, out)
  }
  return out
}

function countAllChildren(node) {
  let total = node.children.length
  for (const child of node.children) total += countAllChildren(child)
  return total
}

const treeRows = computed(() => flattenTree(buildTree(filteredList.value)))
const parentOptions = computed(() =>
  allCategories.value.filter((c) => c.categoryId !== editId.value),
)

const totalPages = computed(() => Math.max(1, Math.ceil(treeRows.value.length / pageSize.value)))
const pagedRows = computed(() => {
  const start = currentPage.value * pageSize.value
  return treeRows.value.slice(start, start + pageSize.value)
})

watch(pageSize, () => {
  currentPage.value = 0
})

function goToPage(page) {
  if (page < 0 || page >= totalPages.value) return
  currentPage.value = page
}

function onSearch() {
  currentPage.value = 0
}

function showModal(type, title, message, onConfirm = null) {
  baseModal.value.type = type
  baseModal.value.title = title
  baseModal.value.message = message
  baseModal.value.onConfirm = onConfirm
  baseModal.value.visible = true
}

function onModalClose() {
  baseModal.value.visible = false
}

function onModalConfirm() {
  if (baseModal.value.onConfirm) baseModal.value.onConfirm()
}

async function fetchAll() {
  loading.value = true
  try {
    const res = await categoryApi.getAll()
    const payload = res.data?.data ?? res.data
    allCategories.value = Array.isArray(payload) ? payload : (payload.content ?? [])
    totalElements.value = treeRows.value.length
  } catch {
    showModal('error', 'Lỗi', 'Không thể tải danh sách danh mục')
  } finally {
    loading.value = false
  }
}

async function doCreate() {
  saving.value = true
  try {
    const formData = buildCategoryFormData(
      {
        categoryName: form.value.categoryName,
        parentId: form.value.parentId,
      },
      logoFile.value,
    )
    await categoryApi.create(formData)

    showModal('success', 'Thành công', 'Thêm danh mục thành công!')
    clearLogo()
    bsModal.hide()
    await fetchAll()
  } catch (e) {
    showModal('error', 'Lỗi', e.response?.data?.message ?? 'Lỗi khi thêm danh mục')
  } finally {
    saving.value = false
  }
}

async function doUpdate() {
  saving.value = true
  try {
    const formData = buildCategoryFormData(
      {
        categoryName: form.value.categoryName,
        parentId: form.value.parentId,
        status: form.value.status,
      },
      logoFile.value,
    )
    await categoryApi.update(editId.value, formData)

    showModal('success', 'Thành công', 'Cập nhật thành công!')
    clearLogo()
    bsModal.hide()
    await fetchAll()
  } catch (e) {
    showModal('error', 'Lỗi', e.response?.data?.message ?? 'Lỗi khi cập nhật')
  } finally {
    saving.value = false
  }
}

async function onHide(item) {
  showModal(
    'confirm',
    'Xác nhận ẩn danh mục',
    `Bạn có chắc muốn ẩn danh mục "${item.categoryName}"?`,
    async () => {
      try {
        await categoryApi.hide(item.categoryId)
        showModal('success', 'Thành công', 'Đã ẩn danh mục')
        await fetchAll()
      } catch (e) {
        showModal('error', 'Lỗi', e.response?.data?.message ?? 'Lỗi khi ẩn danh mục')
      }
    },
  )
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.value = { categoryName: '', parentId: null, status: true }
  err.value = {}
  currentLogoUrl.value = null
  clearLogo()
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
  currentLogoUrl.value = item.categoryImg ?? null
  clearLogo()
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

function onReset() {
  filter.value = { search: '', status: '' }
  currentPage.value = 0
  pageSize.value = 5
}

onMounted(async () => {
  bsModal = new Modal(modalEl.value)
  await fetchAll()
})
</script>

<style scoped></style>