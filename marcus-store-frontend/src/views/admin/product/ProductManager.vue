<template>
  <div class="container-fluid px-4 py-3">
    <!-- ═══════════════════════════════════════════
         HEADER
    ═══════════════════════════════════════════ -->
    <div class="page-header d-flex align-items-center justify-content-between mb-4">
      <div class="d-flex align-items-center gap-3">
        <div class="header-icon">
          <i class="bi bi-box-seam-fill"></i>
        </div>
        <div>
          <h4 class="header-title mb-0">Quản lý Sản phẩm</h4>
          <p class="header-sub mb-0">Quản lý sản phẩm, thương hiệu và trạng thái hiển thị.</p>
        </div>
      </div>
      <button class="btn btn-pink" @click="openCreate">
        <i class="bi bi-plus-lg me-1"></i> Thêm Sản phẩm
      </button>
    </div>

    <!-- ═══════════════════════════════════════════
         STATS
    ═══════════════════════════════════════════ -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-md-4" v-for="stat in stats" :key="stat.label">
        <div class="stat-card">
          <p class="stat-label">{{ stat.label }}</p>
          <p class="stat-value" :class="{ pink: stat.highlight }">{{ stat.value }}</p>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════
         FILTER
    ═══════════════════════════════════════════ -->
    <div class="filter-card mb-4">
      <div class="row g-3 align-items-end">
        <!-- Tìm kiếm -->
        <div class="col-12 col-md-5">
          <label class="filter-label">TÌM KIẾM</label>
          <div class="input-wrapper">
            <i class="bi bi-search search-icon"></i>
            <input
              v-model="filter.keyword"
              type="text"
              class="form-control f-input"
              placeholder="Tìm theo tên sản phẩm..."
              @input="onFilterChange"
            />
          </div>
        </div>

        <!-- Thương hiệu — dùng filter.brand, list lấy từ allBrands -->
        <div class="col-12 col-md-3">
          <label class="filter-label">THƯƠNG HIỆU</label>
          <select v-model="filter.brand" class="form-select f-input" @change="onFilterChange">
            <option value="">Tất cả thương hiệu</option>
            <option v-for="brand in allBrands" :key="brand" :value="brand">{{ brand }}</option>
          </select>
        </div>

        <!-- Lọc trạng thái -->
        <div class="col-12 col-md-3">
          <label class="filter-label">TRẠNG THÁI</label>
          <select v-model="filter.status" class="form-select f-input" @change="onFilterChange">
            <option value="all">Tất cả</option>
            <option value="active">Đang hiển thị</option>
            <option value="hidden">Đã ẩn</option>
          </select>
        </div>

        <!-- Reset -->
        <div class="col-12 col-md-1">
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
                <th class="th">Tên sản phẩm</th>
                <th class="th">Thương hiệu</th>
                <th class="th">Danh mục</th>
                <th class="th">Slug</th>
                <th class="th text-center">Trạng thái</th>
                <th class="th text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!products.length">
                <td colspan="8" class="text-center py-5 text-muted">
                  <i class="bi bi-inbox fs-3 d-block mb-2 opacity-50"></i>
                  Không có dữ liệu
                </td>
              </tr>
              <tr v-for="(item, i) in products" :key="item.productId">
                <td class="text-muted small">{{ page * pageSize + i + 1 }}</td>
                <td>
                  <img
                    v-if="item.thumbnailUrl"
                    :src="item.thumbnailUrl"
                    class="thumb-img"
                    :alt="item.productName"
                  />
                  <div v-else class="thumb-placeholder">
                    <i class="bi bi-image"></i>
                  </div>
                </td>
                <td class="fw-500" style="max-width: 200px">
                  <div class="text-truncate" :title="item.productName">{{ item.productName }}</div>
                </td>
                <td class="text-muted small">{{ item.brand || '—' }}</td>
                <td>
                  <span class="cate-badge">{{ item.categoryName || 'Chưa phân loại' }}</span>
                </td>
                <td>
                  <span class="slug-badge">{{ item.slug }}</span>
                </td>
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
                      class="act-btn img-btn"
                      title="Xem danh sách ảnh"
                      @click="openImagesModal(item)"
                    >
                      <i class="bi bi-images"></i>
                    </button>
                    <button
                      class="act-btn hide-btn"
                      title="Ẩn sản phẩm"
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
        <div v-if="totalPages > 0" class="pagination-bar">
          <div class="pagination-total">
            Tổng <strong>{{ totalElements }}</strong> sản phẩm
          </div>

          <div class="pagination-actions">
            <label class="page-size-box">
              <span>Hiển thị</span>
              <select v-model.number="pageSize" @change="onFilterChange">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
            </label>

            <button
              type="button"
              class="page-btn"
              :disabled="page === 0"
              @click="changePage(page - 1)"
            >
              Trước
            </button>

            <span class="page-current">Trang {{ page + 1 }} / {{ totalPages }}</span>

            <button
              type="button"
              class="page-btn"
              :disabled="page + 1 >= totalPages"
              @click="changePage(page + 1)"
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

    <div class="modal fade" id="prodModal" tabindex="-1" ref="modalEl">
      <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content rounded-4 border-0 shadow-lg">
          <div class="modal-header border-0 pb-0">
            <h5 class="fw-bold mb-0" style="color: #e91e63">
              <i class="bi me-2" :class="isEdit ? 'bi-pencil-square' : 'bi-plus-circle-fill'"></i>
              {{ isEdit ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm mới' }}
            </h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>

          <div class="modal-body pt-3">
            <div class="row g-3">
              <div class="col-12">
                <label class="flabel">Tên sản phẩm <span class="text-danger">*</span></label>
                <input
                  v-model="form.productName"
                  type="text"
                  class="form-control finput"
                  :class="{ 'is-invalid': err.productName }"
                  placeholder="Nhập tên sản phẩm..."
                />
                <div class="invalid-feedback">{{ err.productName }}</div>
              </div>

              <!-- Thương hiệu -->
              <div class="col-md-6">
                <label class="flabel">Thương hiệu <span class="text-danger">*</span></label>
                <input
                  v-model="form.brand"
                  type="text"
                  class="form-control finput"
                  :class="{ 'is-invalid': err.brand }"
                  placeholder="Apple, Samsung, Xiaomi..."
                />
                <div class="invalid-feedback">{{ err.brand }}</div>
              </div>

              <!-- Danh mục -->
              <div class="col-md-6">
                <label class="flabel">Danh mục <span class="text-danger">*</span></label>
                <select
                  v-model="form.categoryId"
                  class="form-select finput"
                  :class="{ 'is-invalid': err.categoryId }"
                >
                  <option :value="null">— Chọn danh mục —</option>
                  <option v-for="c in categories" :key="c.categoryId" :value="c.categoryId">
                    {{ c.categoryName }}
                  </option>
                </select>
                <div class="invalid-feedback">{{ err.categoryId }}</div>
              </div>

              <!-- Thumbnail URL -->
              <div class="col-12" v-if="!isEdit">
                <label class="flabel">Ảnh thumbnail</label>
                <div class="upload-section">
                  <div class="input-group">
                    <input
                      type="file"
                      class="form-control finput"
                      accept="image/*"
                      ref="thumbnailFileInput"
                      @change="onThumbnailSelect"
                    />
                  </div>
                  <div v-if="thumbnailPreview" class="upload-preview mt-3">
                    <img :src="thumbnailPreview" alt="preview" class="preview-thumb" />
                    <span class="preview-name ms-2 text-muted small">{{
                      thumbnailFile?.name
                    }}</span>
                    <button
                      type="button"
                      class="btn btn-sm btn-outline-danger ms-auto"
                      @click="clearThumbnail"
                    >
                      <i class="bi bi-x"></i>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Mô tả -->
              <div class="col-12">
                <label class="flabel">Mô tả</label>
                <textarea
                  v-model="form.description"
                  class="form-control finput"
                  rows="3"
                  placeholder="Nhập mô tả sản phẩm..."
                ></textarea>
              </div>

              <!-- Trạng thái — chỉ khi sửa -->
              <div class="col-12" v-if="isEdit">
                <label class="flabel">Trạng thái</label>
                <div class="d-flex gap-4 mt-1">
                  <div class="form-check">
                    <input
                      class="form-check-input"
                      type="radio"
                      :value="true"
                      v-model="form.status"
                      id="pOn"
                    />
                    <label class="form-check-label" for="pOn">Hiển thị</label>
                  </div>
                  <div class="form-check">
                    <input
                      class="form-check-input"
                      type="radio"
                      :value="false"
                      v-model="form.status"
                      id="pOff"
                    />
                    <label class="form-check-label" for="pOff">Ẩn</label>
                  </div>
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

    <div class="modal fade" id="imagesModal" tabindex="-1" ref="imagesModalEl">
      <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content rounded-4 border-0 shadow-lg">
          <div class="modal-header border-0 pb-0">
            <div>
              <h5 class="fw-bold mb-1" style="color: #e91e63">
                <i class="bi bi-images me-2"></i>Danh sách ảnh sản phẩm
              </h5>
              <p class="text-muted small mb-0" v-if="selectedProduct">
                {{ selectedProduct.productName }}
              </p>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>

          <div class="modal-body pt-3">
            <div class="upload-section mb-4">
              <div class="d-flex align-items-center gap-3">
                <div class="flex-grow-1">
                  <label class="flabel">THÊM ẢNH MỚI</label>
                  <div class="input-group">
                    <input
                      type="file"
                      class="form-control finput"
                      accept="image/*"
                      ref="fileInput"
                      @change="onFileSelect"
                    />
                    <button
                      class="btn btn-pink"
                      :disabled="!selectedFile || uploading"
                      @click="uploadNewImage"
                    >
                      <span v-if="uploading" class="spinner-border spinner-border-sm me-1"></span>
                      <i v-else class="bi bi-cloud-upload me-1"></i>
                      Upload
                    </button>
                  </div>
                </div>
                <div class="upload-options">
                  <div class="form-check me-3">
                    <input
                      class="form-check-input"
                      type="checkbox"
                      v-model="newImageIsPrimary"
                      id="imgPrimary"
                    />
                    <label class="form-check-label" for="imgPrimary">Ảnh chính</label>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="selectedFilePreview" class="upload-preview mb-4">
              <img :src="selectedFilePreview" alt="preview" class="preview-thumb" />
              <span class="preview-name ms-2 text-muted small">{{ selectedFileName }}</span>
            </div>

            <div v-if="imagesLoading" class="text-center py-4">
              <div class="spinner-border" style="color: #e91e63" role="status"></div>
              <p class="mt-2 text-muted small">Đang tải danh sách ảnh...</p>
            </div>

            <div v-else-if="!productImages.length" class="text-center py-5 text-muted">
              <i class="bi bi-image fs-1 d-block mb-2 opacity-50"></i>
              <p class="mb-0">Chưa có ảnh nào cho sản phẩm này</p>
            </div>

            <div v-else class="images-grid">
              <div
                v-for="img in productImages"
                :key="img.imageId"
                class="img-card"
                :class="{ 'is-primary': img.isPrimary }"
              >
                <div class="img-wrapper">
                  <img :src="img.imageUrl" :alt="'Ảnh ' + img.imageId" />
                  <div class="img-overlay">
                    <button
                      class="overlay-btn view-btn"
                      title="Xem ảnh lớn"
                      @click="viewFullImage(img.imageUrl)"
                    >
                      <i class="bi bi-zoom-in"></i>
                    </button>
                    <button
                      v-if="!img.isPrimary"
                      class="overlay-btn primary-btn"
                      title="Đặt làm ảnh chính"
                      @click="setPrimaryImage(img)"
                    >
                      <i class="bi bi-star"></i>
                    </button>
                    <button
                      class="overlay-btn delete-btn"
                      title="Xóa ảnh"
                      @click="deleteImage(img)"
                    >
                      <i class="bi bi-trash"></i>
                    </button>
                  </div>
                </div>
                <div class="img-footer">
                  <span v-if="img.isPrimary" class="primary-badge">
                    <i class="bi bi-star-fill me-1"></i>Ảnh chính
                  </span>
                  <span v-else class="order-badge">Thứ tự: {{ img.displayOrder }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="modal-footer border-0 pt-1">
            <button class="btn btn-outline-secondary rounded-3" data-bs-dismiss="modal">
              Đóng
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal fade" id="fullImageModal" tabindex="-1" ref="fullImageModalEl">
      <div class="modal-dialog modal-dialog-centered modal-fullscreen">
        <div class="modal-content bg-dark border-0">
          <div class="modal-header border-0">
            <button
              type="button"
              class="btn-close btn-close-white"
              data-bs-dismiss="modal"
            ></button>
          </div>
          <div class="modal-body d-flex align-items-center justify-content-center p-0">
            <img v-if="fullImageUrl" :src="fullImageUrl" class="full-image" alt="Xem ảnh" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import api from '@/utils/api'
import '@/assets/css/Product.css'
import { Modal } from 'bootstrap'

const productApi = {
  getAll: (keyword, status, brand, page, size) =>
    api.get('/admin/product', { params: { keyword, filter: status, brand, page, size } }),

  create: (payload) => api.post('/admin/product', payload),
  update: (id, payload) => api.put(`/admin/product/${id}`, payload),
  hide: (id) => api.put(`/admin/product/hidden/${id}`),
}

const categoryApi = {
  getAll: () => api.get('/admin/categories', { params: { page: 0, size: 999 } }),
}

const productImgApi = {
  getImages: (productId) => api.get(`/admin/products/${productId}/images`),
  uploadImage: (productId, formData) =>
    api.post(`/admin/products/${productId}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  updateImage: (imageId, formData) =>
    api.put(`/admin/products/images/${imageId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  deleteImage: (imageId) => api.delete(`/admin/products/images/${imageId}`),
}

const products = ref([])
const categories = ref([])
const allBrands = ref([])
const loading = ref(false)
const saving = ref(false)

const page = ref(0)
const pageSize = ref(5)
const totalPages = ref(1)
const totalElements = ref(0)

const filter = ref({ keyword: '', status: 'all', brand: '' })

const isEdit = ref(false)
const editId = ref(null)
const form = ref({
  productName: '',
  brand: '',
  categoryId: null,
  thumbnailUrl: '',
  description: '',
  status: true,
})
const err = ref({})

const modalEl = ref(null)
let bsModal = null

const imagesModalEl = ref(null)
const fileInput = ref(null)
let bsImagesModal = null

const selectedProduct = ref(null)
const productImages = ref([])
const imagesLoading = ref(false)
const selectedFile = ref(null)
const selectedFilePreview = ref(null)
const selectedFileName = ref('')
const newImageIsPrimary = ref(false)
const uploading = ref(false)

const fullImageModalEl = ref(null)
const fullImageUrl = ref(null)
let bsFullImageModal = null

const stats = computed(() => [
  { label: 'Tổng sản phẩm', value: totalElements.value, highlight: false },
  { label: 'Đang hiển thị', value: products.value.filter((p) => p.status).length, highlight: true },
  { label: 'Đã ẩn', value: products.value.filter((p) => !p.status).length, highlight: false },
])

const baseModal = ref({
  visible: false,
  type: 'error',
  title: '',
  message: '',
  onConfirm: null,
})

const thumbnailFileInput = ref(null)
const thumbnailFile = ref(null)
const thumbnailPreview = ref(null)

function onThumbnailSelect(event) {
  const file = event.target.files[0]
  if (!file) return
  thumbnailFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => { thumbnailPreview.value = e.target.result }
  reader.readAsDataURL(file)
}

function clearThumbnail() {
  thumbnailFile.value = null
  thumbnailPreview.value = null
  if (thumbnailFileInput.value) thumbnailFileInput.value.value = ''
}

function showModal(type, title, message, onConfirm = null) {
  console.log('showModal called:', type, title)
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

async function fetchProducts() {
  loading.value = true
  try {
    const res = await productApi.getAll(
      filter.value.keyword || undefined,
      filter.value.status,
      filter.value.brand || undefined,
      page.value,
      pageSize.value,
    )
    console.log('raw response:', res.data)
    const payload = res.data?.data ?? res.data
    const content = Array.isArray(payload) ? payload : (payload.content ?? [])
    products.value = content
    totalPages.value = payload.totalPages ?? 1
    totalElements.value = payload.totalElements ?? content.length
  } catch {
    showToast('Không thể tải danh sách sản phẩm', 'error')
  } finally {
    loading.value = false
  }
}

const fetchAllBrands = async () => {
  try {
    const res = await api.get('/admin/product/brands')
    allBrands.value = res.data
  } catch (err) {
    console.error('Lỗi lấy brands:', err)
  }
}

async function fetchCategories() {
  try {
    const res = await categoryApi.getAll()
    const payload = res.data?.data ?? res.data
    categories.value = Array.isArray(payload) ? payload : (payload.content ?? [])
  } catch {}
}

async function doCreate() {
  saving.value = true
  try {
    const res =await productApi.create({
      productName: form.value.productName,
      brand: form.value.brand,
      categoryId: form.value.categoryId,
      thumbnailUrl: '',
      description: form.value.description,
    })

    const newProductId = res.data?.data?.productId ?? res.data?.productId

    if (thumbnailFile.value && newProductId) {
      const formData = new FormData()
      formData.append('file', thumbnailFile.value)
      formData.append('isPrimary', true)
      await productImgApi.uploadImage(newProductId, formData)
    }
    
    showToast('Thêm sản phẩm thành công!', 'success')
    clearThumbnail()
    bsModal.hide()
    await Promise.all([fetchProducts(), fetchAllBrands()])
  } catch (e) {
    showToast(e.response?.data?.message ?? 'Lỗi khi thêm sản phẩm', 'error')
  } finally {
    saving.value = false
  }
}

async function doUpdate() {
  saving.value = true
  try {
    await productApi.update(editId.value, {
      productName: form.value.productName,
      brand: form.value.brand,
      categoryId: form.value.categoryId,
      thumbnailUrl: form.value.thumbnailUrl,
      description: form.value.description,
      status: form.value.status,
    })
    showToast('Cập nhật thành công!', 'success')
    bsModal.hide()
    await Promise.all([fetchProducts(), fetchAllBrands()])
  } catch (e) {
    showToast(e.response?.data?.message ?? 'Lỗi khi cập nhật', 'error')
  } finally {
    saving.value = false
  }
}

async function onHide(item) {
  showModal(
    'confirm',
    'Xác nhận ẩn sản phẩm',
    `Bạn có chắc muốn ẩn sản phẩm "${item.productName}"?`,
    async () => {
      try {
        await productApi.hide(item.productId)
        const idx = products.value.findIndex((p) => p.productId === item.productId)
        if (idx !== -1) products.value[idx].status = false
        showModal('success', 'Thành công', 'Đã ẩn sản phẩm')
      } catch (e) {
        showModal('error', 'Lỗi', e.response?.data?.message ?? 'Lỗi khi ẩn sản phẩm')
      }
    },
  )
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.value = {
    productName: '',
    brand: '',
    categoryId: null,
    thumbnailUrl: '',
    description: '',
    status: true,
  }
  err.value = {}
  clearThumbnail()
  bsModal.show()
}

function openEdit(item) {
  isEdit.value = true
  editId.value = item.productId
  form.value = {
    productName: item.productName,
    brand: item.brand ?? '',
    categoryId:
      categories.value.find((c) => c.categoryName === item.categoryName)?.categoryId ?? null,
    thumbnailUrl: item.thumbnailUrl ?? '',
    description: item.description ?? '',
    status: item.status,
  }
  err.value = {}
  bsModal.show()
}

function validate() {
  err.value = {}
  if (!form.value.productName.trim()) err.value.productName = 'Tên sản phẩm không được để trống'
  if (!form.value.brand.trim()) err.value.brand = 'Thương hiệu không được để trống'
  if (!form.value.categoryId) err.value.categoryId = 'Vui lòng chọn danh mục'
  return Object.keys(err.value).length === 0
}

function onSubmit() {
  if (!validate()) return
  isEdit.value ? doUpdate() : doCreate()
}

function onFilterChange() {
  page.value = 0
  fetchProducts()
}

function onReset() {
  filter.value = { keyword: '', status: 'all', brand: '' }
  page.value = 0
  pageSize.value = 5
  fetchProducts()
}

function changePage(p) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  fetchProducts()
}

function showToast(msg, type = 'success') {
  showModal(type, type === 'success' ? 'Thành công' : 'Lỗi', msg)
}

async function openImagesModal(product) {
  selectedProduct.value = product
  productImages.value = []
  resetUploadForm()
  bsImagesModal.show()
  await fetchProductImages(product.productId)
}

async function fetchProductImages(productId) {
  imagesLoading.value = true
  try {
    const res = await productImgApi.getImages(productId)
    const payload = res.data?.data ?? res.data
    productImages.value = Array.isArray(payload) ? payload : (payload.content ?? [])
  } catch {
    showToast('Không thể tải danh sách ảnh', 'error')
  } finally {
    imagesLoading.value = false
  }
}

function onFileSelect(event) {
  const file = event.target.files[0]
  if (!file) {
    resetUploadForm()
    return
  }
  selectedFile.value = file
  selectedFileName.value = file.name
  const reader = new FileReader()
  reader.onload = (e) => {
    selectedFilePreview.value = e.target.result
  }
  reader.readAsDataURL(file)
}

function resetUploadForm() {
  selectedFile.value = null
  selectedFilePreview.value = null
  selectedFileName.value = ''
  newImageIsPrimary.value = false
  if (fileInput.value) fileInput.value.value = ''
}

async function uploadNewImage() {
  if (!selectedFile.value || !selectedProduct.value) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('isPrimary', newImageIsPrimary.value)
    const res = await productImgApi.uploadImage(selectedProduct.value.productId, formData)
    showToast('Upload ảnh thành công!', 'success')
    if (newImageIsPrimary.value) {
      const uploadedImageUrl = res.data?.data?.imageUrl ?? res.data?.imageUrl
      if (uploadedImageUrl) {
        const idx = products.value.findIndex((p) => p.productId === selectedProduct.value.productId)
        if (idx !== -1) products.value[idx].thumbnailUrl = uploadedImageUrl
      }
    }
    resetUploadForm()
    await fetchProductImages(selectedProduct.value.productId)
  } catch (e) {
    showToast(e.response?.data?.message ?? 'Lỗi khi upload ảnh', 'error')
  } finally {
    uploading.value = false
  }
}

async function setPrimaryImage(img) {
  if (!selectedProduct.value) return
  try {
    const formData = new FormData()
    formData.append('isPrimary', true)
    await productImgApi.updateImage(img.imageId, formData)
    showToast('Đã đặt làm ảnh chính!', 'success')
    const idx = products.value.findIndex((p) => p.productId === selectedProduct.value.productId)
    if (idx !== -1) products.value[idx].thumbnailUrl = img.imageUrl
    await fetchProductImages(selectedProduct.value.productId)
  } catch (e) {
    showToast(e.response?.data?.message ?? 'Lỗi khi cập nhật ảnh', 'error')
  }
}

async function deleteImage(img) {
  showModal('confirm', 'Xác nhận xóa ảnh', 'Bạn có chắc muốn xóa ảnh này?', async () => {
    try {
      await productImgApi.deleteImage(img.imageId)
      showModal('success', 'Thành công', 'Xóa ảnh thành công!')
      await fetchProductImages(selectedProduct.value.productId)
    } catch (e) {
      showModal('error', 'Lỗi', e.response?.data?.message ?? 'Lỗi khi xóa ảnh')
    }
  })
}

function viewFullImage(url) {
  fullImageUrl.value = url
  bsFullImageModal.show()
}
onMounted(async () => {
  bsModal = new Modal(modalEl.value)
  if (imagesModalEl.value) bsImagesModal = new Modal(imagesModalEl.value)
  if (fullImageModalEl.value) bsFullImageModal = new Modal(fullImageModalEl.value)

  await Promise.all([fetchProducts(), fetchCategories(), fetchAllBrands()])
})
</script>

<style scoped></style>
