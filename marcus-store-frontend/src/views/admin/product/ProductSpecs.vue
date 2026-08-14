<template>
  <div class="container-fluid px-4 py-3">
    <div class="page-header d-flex align-items-center justify-content-between mb-4">
      <div class="d-flex align-items-center gap-3">
        <button class="btn btn-outline-secondary rounded-3" @click="router.back()">
          <i class="bi bi-arrow-left"></i>
        </button>
        <div class="header-icon">
          <i class="bi bi-list-columns-reverse"></i>
        </div>
        <div>
          <h4 class="header-title mb-0">Thông số kỹ thuật</h4>
          <p class="header-sub mb-0" v-if="product">
            {{ product.productName }}
            <span v-if="product.categoryName" class="text-muted"> — {{ product.categoryName }}</span>
          </p>
        </div>
      </div>
    </div>

    <div v-if="pageLoading" class="text-center py-5">
      <div class="spinner-border" style="color: #e91e63" role="status"></div>
      <p class="mt-2 text-muted small">Đang tải dữ liệu...</p>
    </div>

    <template v-else-if="product">
      <div class="filter-card mb-4">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-md-5">
            <label class="filter-label">TÌM THUỘC TÍNH</label>
            <div class="input-wrapper">
              <i class="bi bi-search search-icon"></i>
              <input v-model="specSearch" type="text" class="form-control f-input" placeholder="Tìm theo tên thông số..." />
            </div>
          </div>
          <div class="col-12 col-md-4">
            <label class="filter-label">LỌC THEO</label>
            <select v-model="specFilter" class="form-select f-input">
              <option value="all">Tất cả</option>
              <option value="filled">Đã có giá trị</option>
              <option value="empty">Chưa có giá trị</option>
            </select>
          </div>
          <div class="col-12 col-md-3">
            <button class="btn btn-pink w-100" @click="openAttrForm()">
              <i class="bi bi-plus-lg me-1"></i>Thêm thuộc tính
            </button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th class="th">#</th>
                <th class="th">Thuộc tính</th>
                <th class="th">Đơn vị</th>
                <th class="th">Giá trị</th>
                <th class="th text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!filteredSpecs.length">
                <td colspan="5" class="text-center py-5 text-muted">
                  <i class="bi bi-inbox fs-3 d-block mb-2 opacity-50"></i>
                  <span v-if="specSearch || specFilter !== 'all'">Không có thông số nào khớp bộ lọc.</span>
                  <span v-else-if="specAttributes.length === 0">
                    Danh mục này chưa có thuộc tính nào. Bấm "Thêm thuộc tính" để bắt đầu.
                  </span>
                  <span v-else>Sản phẩm chưa có thông số nào.</span>
                </td>
              </tr>
              <tr v-for="(item, i) in filteredSpecs" :key="item.specAttributeId">
                <td class="text-muted small">{{ i + 1 }}</td>
                <td class="fw-500">{{ item.specAttributeName }}</td>
                <td class="text-muted small">{{ item.unit || '—' }}</td>
                <td>
                  <input
                    v-model="item.valueText"
                    type="text"
                    class="form-control form-control-sm"
                    :placeholder="`Nhập ${item.specAttributeName?.toLowerCase() || 'giá trị'}`"
                  />
                </td>
                <td class="text-center">
                  <div class="d-flex justify-content-center gap-2">
                    <button class="act-btn edit-btn" title="Sửa thuộc tính" @click="openAttrForm(item)">
                      <i class="bi bi-pencil"></i>
                    </button>
                    <button class="act-btn delete-btn" title="Xóa giá trị" @click="clearValue(item)">
                      <i class="bi bi-x-lg"></i>
                    </button>
                    <button class="act-btn hide-btn" title="Xóa thuộc tính khỏi danh mục" @click="deleteAttribute(item)">
                      <i class="bi bi-trash"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="d-flex justify-content-between align-items-center p-3">
          <small class="text-muted">
            <i class="bi bi-info-circle me-1"></i>Nhớ bấm "Lưu thay đổi" sau khi chỉnh giá trị.
          </small>
          <button class="btn btn-pink rounded-3" :disabled="saving" @click="saveSpecs">
            <span v-if="saving" class="spinner-border spinner-border-sm me-1"></span>
            <i v-else class="bi bi-check2 me-1"></i>Lưu thay đổi
          </button>
        </div>
      </div>
    </template>

    <div v-else class="text-center py-5 text-muted">
      <i class="bi bi-exclamation-circle fs-3 d-block mb-2 opacity-50"></i>
      Không tìm thấy sản phẩm
    </div>

    <BaseModal
      :visible="baseModal.visible"
      :show-confirm="baseModal.type === 'confirm'"
      :type="baseModal.type"
      :title="baseModal.title"
      :message="baseModal.message"
      @close="baseModal.visible = false"
      @confirm="onModalConfirm"
    />

    <div class="modal fade" id="specAttrFormModal" tabindex="-1" ref="attrFormEl">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4 border-0 shadow-lg">
          <div class="modal-header border-0 pb-0">
            <h5 class="fw-bold mb-0" style="color: #e91e63">
              <i class="bi bi-plus-circle me-2"></i>
              {{ attrForm.id ? 'Cập nhật thuộc tính' : 'Thêm thuộc tính mới' }}
            </h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body pt-3">
            <div class="mb-3">
              <label class="flabel">Tên thuộc tính <span class="text-danger">*</span></label>
              <input v-model="attrForm.name" type="text" class="form-control finput" placeholder="VD: Dung lượng pin, RAM, Màn hình..." />
            </div>
            <div class="row g-3">
              <div class="col-6">
                <label class="flabel">Đơn vị</label>
                <input v-model="attrForm.unit" type="text" class="form-control finput" placeholder="VD: mAh, GB, inch..." />
              </div>
              <div class="col-6">
                <label class="flabel">Kiểu dữ liệu</label>
                <select v-model="attrForm.dataType" class="form-select finput">
                  <option value="text">Văn bản</option>
                  <option value="number">Số</option>
                  <option value="boolean">Có/Không</option>
                </select>
              </div>
            </div>
            <div class="mt-3">
              <label class="flabel">Thứ tự hiển thị</label>
              <input v-model.number="attrForm.displayOrder" type="number" min="0" class="form-control finput" placeholder="0" />
            </div>
          </div>
          <div class="modal-footer border-0 pt-1">
            <button class="btn btn-outline-secondary rounded-3" data-bs-dismiss="modal">Hủy</button>
            <button class="btn btn-pink rounded-3" :disabled="savingAttr" @click="saveAttribute">
              <span v-if="savingAttr" class="spinner-border spinner-border-sm me-1"></span>
              {{ attrForm.id ? 'Cập nhật' : 'Thêm mới' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseModal from '@/components/BaseModal.vue'
import api from '@/utils/api'
import '@/assets/css/Product.css'
import { Modal } from 'bootstrap'

const route = useRoute()
const router = useRouter()
const productId = Number(route.params.productId)

const specsApi = {
  getAttributes: (categoryId) => api.get('/admin/specs/attributes', { params: { categoryId } }),
  createAttribute: (payload) => api.post('/admin/specs/attributes', payload),
  updateAttribute: (id, payload) => api.put(`/admin/specs/attributes/${id}`, payload),
  deleteAttribute: (id) => api.delete(`/admin/specs/attributes/${id}`),
  getProductSpecs: (productId) => api.get(`/admin/specs/products/${productId}`),
  saveProductSpecs: (payload) => api.put('/admin/specs/products', payload),
}

const productApi = {
  getById: (id) => api.get(`/admin/product/${id}`),
}

const product = ref(null)
const pageLoading = ref(true)
const saving = ref(false)
const savingAttr = ref(false)

const specAttributes = ref([])
const specRows = ref([])
const specSearch = ref('')
const specFilter = ref('all')

const attrFormEl = ref(null)
let bsAttrForm = null
const attrForm = ref({ id: null, name: '', unit: '', dataType: 'text', displayOrder: 0 })

const baseModal = ref({ visible: false, type: 'error', title: '', message: '', onConfirm: null })

function showModal(type, title, message, onConfirm = null) {
  baseModal.value = { visible: true, type, title, message, onConfirm }
}
function onModalConfirm() {
  if (baseModal.value.onConfirm) baseModal.value.onConfirm()
  baseModal.value.visible = false
}

const filteredSpecs = computed(() => {
  const q = specSearch.value.trim().toLowerCase()
  return specRows.value.filter((row) => {
    const name = (row.specAttributeName || '').toLowerCase()
    if (q && !name.includes(q)) return false
    if (specFilter.value === 'filled' && !(row.valueText || '').trim()) return false
    if (specFilter.value === 'empty' && (row.valueText || '').trim()) return false
    return true
  })
})

async function loadAll() {
  pageLoading.value = true
  try {
    const prodRes = await productApi.getById(productId)
    product.value = prodRes.data?.data ?? prodRes.data
    if (!product.value?.categoryId) {
      showModal('error', 'Lỗi', 'Sản phẩm chưa có danh mục — không thể quản lý thông số.')
      return
    }
    await fetchSpecs()
  } catch (e) {
    showModal('error', 'Lỗi', e.response?.data?.message ?? 'Không tải được dữ liệu sản phẩm')
  } finally {
    pageLoading.value = false
  }
}

async function fetchSpecs() {
  const [attrRes, valRes] = await Promise.all([
    specsApi.getAttributes(product.value.categoryId),
    specsApi.getProductSpecs(product.value.productId),
  ])
  specAttributes.value = attrRes.data?.data || []
  const currentValues = valRes.data?.data || []
  const valueByAttr = new Map(currentValues.map((v) => [v.specAttributeId, v]))
  specRows.value = specAttributes.value.map((attr) => {
    const existing = valueByAttr.get(attr.specAttributeId)
    return {
      id: existing?.id ?? null,
      specAttributeId: attr.specAttributeId,
      specAttributeName: attr.name,
      unit: attr.unit,
      displayOrder: attr.displayOrder,
      valueText: existing?.valueText ?? '',
    }
  })
}

function clearValue(item) {
  item.valueText = ''
}

async function saveSpecs() {
  saving.value = true
  try {
    const payload = {
      productId: product.value.productId,
      specs: specRows.value.map((r) => ({
        id: r.id || null,
        specAttributeId: r.specAttributeId,
        valueText: r.valueText ?? '',
      })),
    }
    await specsApi.saveProductSpecs(payload)
    showModal('success', 'Thành công', 'Đã lưu thông số kỹ thuật!')
    await fetchSpecs()
  } catch (e) {
    showModal('error', 'Lỗi', e.response?.data?.message ?? 'Lưu thông số thất bại')
  } finally {
    saving.value = false
  }
}

function openAttrForm(row = null) {
  if (row) {
    attrForm.value = {
      id: row.specAttributeId,
      name: row.specAttributeName,
      unit: row.unit || '',
      dataType: row.dataType || 'text',
      displayOrder: row.displayOrder ?? 0,
    }
  } else {
    attrForm.value = { id: null, name: '', unit: '', dataType: 'text', displayOrder: 0 }
  }
  bsAttrForm.show()
}

async function saveAttribute() {
  if (!attrForm.value.name?.trim()) {
    showModal('error', 'Lỗi', 'Tên thuộc tính không được để trống')
    return
  }
  savingAttr.value = true
  try {
    const payload = {
      categoryId: product.value.categoryId,
      name: attrForm.value.name.trim(),
      unit: attrForm.value.unit?.trim() || null,
      dataType: attrForm.value.dataType || 'text',
      displayOrder: attrForm.value.displayOrder ?? 0,
    }
    if (attrForm.value.id) {
      await specsApi.updateAttribute(attrForm.value.id, payload)
    } else {
      await specsApi.createAttribute(payload)
    }
    bsAttrForm.hide()
    showModal('success', 'Thành công', 'Đã lưu thuộc tính!')
    await fetchSpecs()
  } catch (e) {
    showModal('error', 'Lỗi', e.response?.data?.message ?? 'Lưu thuộc tính thất bại')
  } finally {
    savingAttr.value = false
  }
}

function deleteAttribute(row) {
  showModal(
    'confirm',
    'Xác nhận xóa thuộc tính',
    `Xóa hẳn thuộc tính "${row.specAttributeName}" khỏi danh mục? Thao tác này áp dụng cho mọi sản phẩm cùng danh mục.`,
    async () => {
      try {
        await specsApi.deleteAttribute(row.specAttributeId)
        showModal('success', 'Thành công', 'Đã xóa thuộc tính!')
        await fetchSpecs()
      } catch (e) {
        showModal('error', 'Lỗi', e.response?.data?.message ?? 'Xóa thuộc tính thất bại')
      }
    },
  )
}

onMounted(async () => {
  if (attrFormEl.value) bsAttrForm = new Modal(attrFormEl.value)
  await loadAll()
})
</script>

<style scoped>
.act-btn.hide-btn {
  background: #ffebee;
  color: #c62828;
  border: 1px solid #ffcdd2;
}
.act-btn.hide-btn:hover {
  background: #ffcdd2;
  color: #b71c1c;
}
</style>