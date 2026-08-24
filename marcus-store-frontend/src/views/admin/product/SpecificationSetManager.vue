<template>
  <div class="container-fluid px-3 px-lg-4 py-3 spec-set-page">
    <section class="page-heading">
      <div class="heading-icon"><i class="bi bi-ui-checks-grid"></i></div>
      <div class="heading-copy">
        <span>CATALOG SẢN PHẨM</span>
        <h1>Quản lý bộ thông số</h1>
        <p>Cấu hình cấu trúc thông số theo danh mục và kiểm soát phần kế thừa cha – con.</p>
      </div>
      <button
        class="btn btn-primary heading-action"
        :disabled="!selectedCategory"
        @click="openForm()"
      >
        <i class="bi bi-plus-lg me-2"></i>{{ addButtonLabel }}
      </button>
    </section>

    <section class="summary-grid">
      <article>
        <i class="bi bi-folder2-open"></i>
        <div>
          <span>Danh mục</span><strong>{{ categories.length }}</strong>
        </div>
      </article>
      <article>
        <i class="bi bi-list-check"></i>
        <div>
          <span>Thông số đã cấu hình</span><strong>{{ totalDirectAttributes }}</strong>
        </div>
      </article>
      <article>
        <i class="bi bi-diagram-3"></i>
        <div>
          <span>Danh mục có kế thừa</span><strong>{{ inheritedCategoryCount }}</strong>
        </div>
      </article>
      <article>
        <i class="bi bi-exclamation-circle"></i>
        <div>
          <span>Chưa có thông số</span><strong>{{ emptyCategoryCount }}</strong>
        </div>
      </article>
    </section>

    <section class="workspace-card">
      <aside class="category-panel">
        <div class="panel-heading">
          <div><span>DANH MỤC ÁP DỤNG</span><small>Chọn danh mục để xem bộ thông số</small></div>
        </div>
        <div class="category-search">
          <i class="bi bi-search"></i>
          <input v-model="categorySearch" placeholder="Tìm danh mục..." />
        </div>
        <div v-if="loadingOverview" class="panel-loading">
          <span class="spinner-border spinner-border-sm"></span>Đang tải...
        </div>
        <div v-else class="category-tree">
          <button
            v-for="category in filteredCategoryTree"
            :key="category.categoryId"
            type="button"
            class="category-item"
            :class="{
              active: selectedCategoryId === category.categoryId,
              hidden: !category.status,
            }"
            :style="{ '--level': category.level }"
            @click="selectCategory(category.categoryId)"
          >
            <i :class="category.level ? 'bi bi-folder2' : 'bi bi-folder2-open'"></i>
            <span class="category-name">
              {{ category.categoryName }}
              <small>{{ category.level ? 'Bộ riêng' : 'Bộ chung' }}</small>
            </span>
            <span class="category-count">{{ category.effectiveAttributeCount }}</span>
          </button>
          <div v-if="!filteredCategoryTree.length" class="panel-empty">
            Không tìm thấy danh mục.
          </div>
        </div>
      </aside>

      <main class="detail-panel">
        <div v-if="!selectedCategory" class="empty-selection">
          <i class="bi bi-layout-text-sidebar-reverse"></i>
          <strong>Chọn một danh mục</strong>
          <p>Bộ thông số hiệu lực của danh mục sẽ được hiển thị tại đây.</p>
        </div>
        <template v-else>
          <header class="detail-heading">
            <div>
              <span class="detail-eyebrow">BỘ THÔNG SỐ HIỆU LỰC</span>
              <h2>{{ selectedCategory.categoryName }}</h2>
              <p>
                <span v-if="selectedCategory.parentName"
                  >Kế thừa từ <b>{{ selectedCategory.parentName }}</b> ·
                </span>
                {{ selectedCategory.productCount }} sản phẩm thuộc trực tiếp danh mục
              </p>
            </div>
            <div class="detail-counts">
              <span
                ><b>{{ selectedCategory.directAttributeCount }}</b> riêng</span
              >
              <span
                ><b>{{ selectedCategory.inheritedAttributeCount }}</b> kế thừa</span
              >
              <span
                ><b>{{ selectedCategory.effectiveAttributeCount }}</b> tổng</span
              >
            </div>
          </header>

          <div class="detail-toolbar">
            <div class="attribute-search">
              <i class="bi bi-search"></i
              ><input v-model="attributeSearch" placeholder="Tìm tên thông số..." />
            </div>
            <div class="view-toggle">
              <button :class="{ active: viewMode === 'manage' }" @click="viewMode = 'manage'">
                <i class="bi bi-sliders"></i> Quản lý
              </button>
              <button :class="{ active: viewMode === 'preview' }" @click="viewMode = 'preview'">
                <i class="bi bi-eye"></i> Xem trước
              </button>
            </div>
          </div>

          <div class="scope-guide" :class="{ 'is-shared': isSharedScope }">
            <div class="scope-guide-icon">
              <i :class="isSharedScope ? 'bi bi-diagram-3-fill' : 'bi bi-folder-check'"></i>
            </div>
            <div>
              <strong>{{ scopeTitle }}</strong>
              <p>{{ scopeDescription }}</p>
            </div>
            <span>{{ selectedCategory.parentName ? 'DANH MỤC CON' : 'DANH MỤC GỐC' }}</span>
          </div>

          <div v-if="duplicateAttributes.length" class="duplicate-warning">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <div>
              <strong
                >Phát hiện {{ duplicateAttributes.length }} thông số trùng với bộ chung</strong
              >
              <p>
                {{ duplicateAttributeNames }}. Hãy chạy lại file seed đã sửa để chuyển giá trị về
                danh mục cha và dọn cấu trúc trùng.
              </p>
            </div>
          </div>

          <div v-if="loadingAttributes" class="detail-loading">
            <span class="spinner-border"></span>
            <p>Đang tải bộ thông số...</p>
          </div>

          <template v-else-if="viewMode === 'manage'">
            <section v-if="inheritedAttributes.length" class="attribute-section inherited-section">
              <div class="section-title">
                <div><i class="bi bi-diagram-3"></i><span>Thông số kế thừa</span></div>
                <small>Chỉ chỉnh sửa tại danh mục nguồn</small>
              </div>
              <div class="attribute-list">
                <article
                  v-for="attribute in inheritedAttributes"
                  :key="attribute.specAttributeId"
                  class="attribute-row inherited"
                >
                  <div class="attribute-order"><i class="bi bi-lock"></i></div>
                  <div class="attribute-main">
                    <strong>{{ attribute.name }}</strong
                    ><span
                      >{{ typeLabel(attribute.dataType)
                      }}<template v-if="attribute.unit"> · {{ attribute.unit }}</template></span
                    >
                  </div>
                  <span class="source-badge">Từ {{ attribute.categoryName }}</span>
                  <button
                    class="icon-action"
                    title="Mở danh mục nguồn"
                    @click="selectCategory(attribute.categoryId)"
                  >
                    <i class="bi bi-box-arrow-up-right"></i>
                  </button>
                </article>
              </div>
            </section>

            <section class="attribute-section">
              <div class="section-title">
                <div>
                  <i class="bi bi-pencil-square"></i><span>{{ directSectionTitle }}</span>
                </div>
                <small>{{ directSectionHint }}</small>
              </div>
              <div v-if="directAttributes.length" class="attribute-list">
                <article
                  v-for="(attribute, index) in directAttributes"
                  :key="attribute.specAttributeId"
                  class="attribute-row"
                >
                  <div class="attribute-order">{{ index + 1 }}</div>
                  <div class="attribute-main">
                    <strong>{{ attribute.name }}</strong
                    ><span
                      >{{ typeLabel(attribute.dataType)
                      }}<template v-if="attribute.unit"> · {{ attribute.unit }}</template></span
                    >
                  </div>
                  <div class="attribute-actions">
                    <button
                      class="icon-action"
                      :disabled="Boolean(attributeSearch.trim()) || index === 0"
                      title="Đưa lên"
                      @click="moveAttribute(index, -1)"
                    >
                      <i class="bi bi-arrow-up"></i>
                    </button>
                    <button
                      class="icon-action"
                      :disabled="
                        Boolean(attributeSearch.trim()) || index === directAttributes.length - 1
                      "
                      title="Đưa xuống"
                      @click="moveAttribute(index, 1)"
                    >
                      <i class="bi bi-arrow-down"></i>
                    </button>
                    <button class="icon-action edit" title="Chỉnh sửa" @click="openForm(attribute)">
                      <i class="bi bi-pencil"></i>
                    </button>
                    <button
                      class="icon-action delete"
                      title="Xóa"
                      @click="confirmDelete(attribute)"
                    >
                      <i class="bi bi-trash"></i>
                    </button>
                  </div>
                </article>
              </div>
              <div v-else class="section-empty">
                <i class="bi bi-plus-circle"></i><strong>{{ emptyDirectTitle }}</strong>
                <p>{{ emptyDirectDescription }}</p>
                <button class="btn btn-primary" @click="openForm()">Thêm thông số đầu tiên</button>
              </div>
            </section>
          </template>

          <section v-else class="preview-card">
            <div class="preview-title">
              <div>
                <span>XEM TRƯỚC TRÊN TRANG SẢN PHẨM</span>
                <h3>Thông số kỹ thuật</h3>
              </div>
              <span>{{ effectiveAttributes.length }} trường</span>
            </div>
            <div v-if="effectiveAttributes.length" class="preview-table">
              <div
                v-for="attribute in effectiveAttributes"
                :key="attribute.specAttributeId"
                class="preview-row"
              >
                <span>{{ attribute.name }}</span
                ><strong>{{ previewValue(attribute) }}</strong>
              </div>
            </div>
            <div v-else class="section-empty">
              <i class="bi bi-inbox"></i><strong>Chưa có dữ liệu xem trước</strong>
            </div>
          </section>
        </template>
      </main>
    </section>

    <BaseModal
      :visible="baseModal.visible"
      :show-confirm="baseModal.type === 'confirm'"
      :type="baseModal.type"
      :title="baseModal.title"
      :message="baseModal.message"
      @close="closeBaseModal"
      @confirm="runModalConfirm"
    />

    <div ref="formModalEl" class="modal fade" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
          <div class="modal-header border-0">
            <div>
              <span class="modal-eyebrow">{{
                form.id ? 'CHỈNH SỬA CẤU TRÚC' : 'THÊM VÀO BỘ THÔNG SỐ'
              }}</span>
              <h5 class="mb-0 fw-bold">{{ selectedCategory?.categoryName }}</h5>
            </div>
            <button class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body pt-1">
            <div class="form-note"><i class="bi bi-info-circle"></i>{{ formScopeNote }}</div>
            <label class="form-label">Tên thông số <span>*</span></label
            ><input
              v-model="form.name"
              maxlength="100"
              class="form-control"
              placeholder="Ví dụ: Dung lượng pin"
            />
            <div class="row g-3 mt-1">
              <div class="col-6">
                <label class="form-label">Kiểu dữ liệu</label
                ><select v-model="form.dataType" class="form-select">
                  <option value="text">Văn bản</option>
                  <option value="number">Số</option>
                  <option value="boolean">Có / Không</option>
                </select>
              </div>
              <div class="col-6">
                <label class="form-label">Đơn vị</label
                ><input
                  v-model="form.unit"
                  maxlength="20"
                  class="form-control"
                  placeholder="mAh, inch..."
                />
              </div>
            </div>
          </div>
          <div class="modal-footer border-0">
            <button class="btn btn-light" data-bs-dismiss="modal">Hủy</button
            ><button class="btn btn-primary" :disabled="saving" @click="saveAttribute">
              <span v-if="saving" class="spinner-border spinner-border-sm me-2"></span
              >{{ form.id ? 'Lưu thay đổi' : 'Thêm thông số' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Modal } from 'bootstrap'
import BaseModal from '@/components/BaseModal.vue'
import api from '@/utils/api'

// Marcus thêm: màn quản lý tập trung chỉ quản lý cấu trúc; giá trị cụ thể vẫn nhập tại từng sản phẩm.
const overviewApi = () => api.get('/admin/specs/categories-overview')
const attributesApi = (categoryId) => api.get('/admin/specs/attributes', { params: { categoryId } })
const createApi = (payload) => api.post('/admin/specs/attributes', payload)
const updateApi = (id, payload) => api.put(`/admin/specs/attributes/${id}`, payload)
const deleteApi = (id) => api.delete(`/admin/specs/attributes/${id}`)
const reorderApi = (payload) => api.put('/admin/specs/attributes/reorder', payload)

const categories = ref([])
const attributes = ref([])
const selectedCategoryId = ref(null)
const loadingOverview = ref(false)
const loadingAttributes = ref(false)
const saving = ref(false)
const categorySearch = ref('')
const attributeSearch = ref('')
const viewMode = ref('manage')
const formModalEl = ref(null)
let formModal
const form = ref({ id: null, name: '', unit: '', dataType: 'text', displayOrder: 0 })
const baseModal = ref({ visible: false, type: 'error', title: '', message: '', callback: null })

const selectedCategory = computed(
  () => categories.value.find((item) => item.categoryId === selectedCategoryId.value) || null,
)
const isSharedScope = computed(() =>
  Boolean(selectedCategory.value && !selectedCategory.value.parentId),
)
const addButtonLabel = computed(() =>
  isSharedScope.value ? 'Thêm thông số chung' : 'Thêm thông số riêng',
)
const scopeTitle = computed(() =>
  isSharedScope.value
    ? `Bộ thông số chung của ${selectedCategory.value?.categoryName || 'danh mục'}`
    : `Bộ thông số riêng của ${selectedCategory.value?.categoryName || 'danh mục'}`,
)
const scopeDescription = computed(() =>
  isSharedScope.value
    ? 'Mọi danh mục con sẽ tự động kế thừa các trường được cấu hình tại đây.'
    : `Chỉ thêm trường đặc trưng chưa có trong bộ chung ${selectedCategory.value?.parentName || ''}.`,
)
const directSectionTitle = computed(() =>
  isSharedScope.value
    ? `Thông số chung của ${selectedCategory.value?.categoryName || ''}`
    : `Thông số riêng của ${selectedCategory.value?.categoryName || ''}`,
)
const directSectionHint = computed(() =>
  isSharedScope.value
    ? 'Áp dụng cho toàn bộ cây danh mục bên dưới'
    : 'Không lặp lại trường đã được kế thừa từ danh mục cha',
)
const emptyDirectTitle = computed(() =>
  isSharedScope.value ? 'Bộ thông số chung đang trống' : 'Danh mục chưa có thông số riêng',
)
const emptyDirectDescription = computed(() =>
  isSharedScope.value
    ? 'Thêm các trường nền tảng mà mọi danh mục con đều sử dụng.'
    : 'Danh mục có thể chỉ dùng bộ chung hoặc bổ sung trường thật sự đặc trưng tại đây.',
)
const formScopeNote = computed(() =>
  isSharedScope.value
    ? 'Đây là thông số chung: trường sẽ áp dụng cho danh mục này và toàn bộ danh mục con.'
    : `Đây là thông số riêng: chỉ tạo khi bộ chung ${selectedCategory.value?.parentName || ''} chưa có trường tương đương.`,
)
const totalDirectAttributes = computed(() =>
  categories.value.reduce((sum, item) => sum + Number(item.directAttributeCount || 0), 0),
)
const inheritedCategoryCount = computed(
  () => categories.value.filter((item) => Number(item.inheritedAttributeCount) > 0).length,
)
const emptyCategoryCount = computed(
  () => categories.value.filter((item) => Number(item.effectiveAttributeCount) === 0).length,
)

function buildTreeRows(items) {
  const byParent = new Map()
  items.forEach((item) => {
    const key = item.parentId ?? null
    if (!byParent.has(key)) byParent.set(key, [])
    byParent.get(key).push(item)
  })
  byParent.forEach((rows) =>
    rows.sort((a, b) => a.categoryName.localeCompare(b.categoryName, 'vi')),
  )
  const result = []
  const visited = new Set()
  const walk = (parentId, level) =>
    (byParent.get(parentId) || []).forEach((item) => {
      if (visited.has(item.categoryId)) return
      visited.add(item.categoryId)
      result.push({ ...item, level })
      walk(item.categoryId, level + 1)
    })
  walk(null, 0)
  items
    .filter((item) => !visited.has(item.categoryId))
    .forEach((item) => result.push({ ...item, level: 0 }))
  return result
}

const categoryTree = computed(() => buildTreeRows(categories.value))
const filteredCategoryTree = computed(() => {
  const query = categorySearch.value.trim().toLowerCase()
  return query
    ? categoryTree.value.filter((item) => item.categoryName.toLowerCase().includes(query))
    : categoryTree.value
})
const filteredAttributes = computed(() => {
  const query = attributeSearch.value.trim().toLowerCase()
  return query
    ? attributes.value.filter((item) => item.name.toLowerCase().includes(query))
    : attributes.value
})
const inheritedAttributes = computed(() =>
  filteredAttributes.value.filter((item) => item.categoryId !== selectedCategoryId.value),
)
const allDirectAttributes = computed(() =>
  attributes.value
    .filter((item) => item.categoryId === selectedCategoryId.value)
    .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)),
)
const directAttributes = computed(() =>
  filteredAttributes.value
    .filter((item) => item.categoryId === selectedCategoryId.value)
    .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)),
)
const duplicateAttributes = computed(() => {
  const inheritedNames = new Set(
    attributes.value
      .filter((item) => item.categoryId !== selectedCategoryId.value)
      .map((item) => normalizeAttributeName(item.name)),
  )
  return attributes.value.filter(
    (item) =>
      item.categoryId === selectedCategoryId.value &&
      inheritedNames.has(normalizeAttributeName(item.name)),
  )
})
const duplicateAttributeNames = computed(() =>
  duplicateAttributes.value.map((item) => `“${item.name}”`).join(', '),
)
// Backend sắp cha → con. Xem trước chỉ giữ dòng đầu tiên theo tên để dữ liệu cũ
// bị trùng không xuất hiện hai lần trong lúc chờ chạy script chuẩn hóa.
const effectiveAttributes = computed(() => {
  const seen = new Set()
  return filteredAttributes.value.filter((item) => {
    const key = normalizeAttributeName(item.name)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
})

async function loadOverview(preserveSelection = true) {
  loadingOverview.value = true
  try {
    const response = await overviewApi()
    categories.value = response.data?.data || []
    if ((!preserveSelection || !selectedCategory.value) && categories.value.length)
      selectedCategoryId.value = categories.value[0].categoryId
  } catch (error) {
    showModal(
      'error',
      'Không thể tải danh mục',
      error.response?.data?.message || 'Vui lòng thử lại.',
    )
  } finally {
    loadingOverview.value = false
  }
}

async function selectCategory(categoryId) {
  selectedCategoryId.value = categoryId
  attributeSearch.value = ''
  viewMode.value = 'manage'
  await loadAttributes()
}

async function loadAttributes() {
  if (!selectedCategoryId.value) return
  loadingAttributes.value = true
  try {
    const response = await attributesApi(selectedCategoryId.value)
    attributes.value = response.data?.data || []
  } catch (error) {
    showModal(
      'error',
      'Không thể tải bộ thông số',
      error.response?.data?.message || 'Vui lòng thử lại.',
    )
  } finally {
    loadingAttributes.value = false
  }
}

function openForm(attribute = null) {
  const nextOrder =
    allDirectAttributes.value.reduce(
      (max, item) => Math.max(max, Number(item.displayOrder ?? 0)),
      -1,
    ) + 1
  form.value = attribute
    ? {
        id: attribute.specAttributeId,
        name: attribute.name,
        unit: attribute.unit || '',
        dataType: attribute.dataType || 'text',
        displayOrder: attribute.displayOrder ?? 0,
      }
    : {
        id: null,
        name: '',
        unit: '',
        dataType: 'text',
        displayOrder: nextOrder,
      }
  formModal.show()
}

function payloadFor(attribute) {
  return {
    categoryId: selectedCategoryId.value,
    name: attribute.name.trim(),
    unit: attribute.unit?.trim() || null,
    dataType: attribute.dataType || 'text',
    displayOrder: attribute.displayOrder ?? 0,
  }
}

async function saveAttribute() {
  if (!form.value.name?.trim())
    return showModal('error', 'Thiếu tên thông số', 'Vui lòng nhập tên thông số.')
  const normalizedName = normalizeAttributeName(form.value.name)
  const inheritedDuplicate = attributes.value.find(
    (item) =>
      item.categoryId !== selectedCategoryId.value &&
      normalizeAttributeName(item.name) === normalizedName,
  )
  if (inheritedDuplicate) {
    return showModal(
      'error',
      'Thông số đã có trong bộ chung',
      `“${inheritedDuplicate.name}” đang được kế thừa từ ${inheritedDuplicate.categoryName}. Không cần tạo lại trong bộ riêng.`,
    )
  }
  saving.value = true
  try {
    const payload = payloadFor(form.value)
    if (form.value.id) await updateApi(form.value.id, payload)
    else await createApi(payload)
    formModal.hide()
    await Promise.all([loadOverview(), loadAttributes()])
    showModal(
      'success',
      'Đã cập nhật bộ thông số',
      'Cấu trúc mới đã được áp dụng đúng phạm vi danh mục.',
    )
  } catch (error) {
    showModal(
      'error',
      'Không thể lưu thông số',
      error.response?.data?.message || 'Vui lòng kiểm tra lại dữ liệu.',
    )
  } finally {
    saving.value = false
  }
}

function confirmDelete(attribute) {
  showModal(
    'confirm',
    'Xóa thông số khỏi danh mục?',
    `Thông số “${attribute.name}” chỉ có thể xóa khi chưa có sản phẩm sử dụng.`,
    async () => {
      try {
        await deleteApi(attribute.specAttributeId)
        await Promise.all([loadOverview(), loadAttributes()])
        showModal('success', 'Đã xóa thông số', 'Bộ thông số danh mục đã được cập nhật.')
      } catch (error) {
        showModal(
          'error',
          'Không thể xóa thông số',
          error.response?.data?.message || 'Thông số đang được sử dụng.',
        )
      }
    },
  )
}

async function moveAttribute(index, offset) {
  if (attributeSearch.value.trim()) return
  const rows = directAttributes.value
  const current = rows[index]
  const target = rows[index + offset]
  if (!current || !target) return
  const orderedIds = rows.map((item) => item.specAttributeId)
  ;[orderedIds[index], orderedIds[index + offset]] = [orderedIds[index + offset], orderedIds[index]]
  try {
    await reorderApi({ categoryId: selectedCategoryId.value, attributeIds: orderedIds })
    await loadAttributes()
  } catch (error) {
    showModal('error', 'Không thể đổi thứ tự', error.response?.data?.message || 'Vui lòng thử lại.')
  }
}

function showModal(type, title, message, callback = null) {
  baseModal.value = { visible: true, type, title, message, callback }
}
function closeBaseModal() {
  baseModal.value.visible = false
}
function runModalConfirm() {
  const callback = baseModal.value.callback
  baseModal.value.visible = false
  if (callback) callback()
}
function typeLabel(type) {
  return { number: 'Dạng số', boolean: 'Có / Không', text: 'Văn bản' }[type] || 'Văn bản'
}
function normalizeAttributeName(value) {
  return String(value || '')
    .trim()
    .toLocaleLowerCase('vi-VN')
}
function previewValue(attribute) {
  if (attribute.dataType === 'boolean') return 'Có / Không'
  if (attribute.dataType === 'number') return `—${attribute.unit ? ` ${attribute.unit}` : ''}`
  return 'Chưa nhập giá trị'
}

onMounted(async () => {
  formModal = new Modal(formModalEl.value)
  await loadOverview(false)
  await loadAttributes()
})
</script>

<style scoped>
.spec-set-page {
  color: #122845;
}
.page-heading {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px 28px;
  border: 1px solid #cfe0f5;
  border-radius: 22px;
  background: linear-gradient(135deg, #eef6ff, #fff 58%, #fff3f8);
  box-shadow: 0 12px 28px rgba(41, 73, 112, 0.08);
}
.heading-icon {
  display: grid;
  place-items: center;
  width: 60px;
  height: 60px;
  border-radius: 18px;
  color: #fff;
  background: linear-gradient(145deg, #ec438a, #c91f67);
  font-size: 25px;
  box-shadow: 0 10px 20px rgba(201, 31, 103, 0.2);
}
.heading-copy {
  flex: 1;
}
.heading-copy span,
.detail-eyebrow,
.modal-eyebrow {
  color: #1561ba;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}
.heading-copy h1 {
  margin: 2px 0;
  font-size: clamp(25px, 2.4vw, 34px);
  font-weight: 850;
}
.heading-copy p,
.detail-heading p {
  margin: 0;
  color: #6d7e93;
}
.heading-action {
  min-height: 48px;
  border: 0;
  border-radius: 13px;
  background: #1768ce;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin: 18px 0;
}
.summary-grid article {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 17px 18px;
  border: 1px solid #e1e8f1;
  border-radius: 16px;
  background: #fff;
}
.summary-grid i {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: #c92168;
  background: #ffedf5;
  font-size: 18px;
}
.summary-grid span {
  display: block;
  color: #718197;
  font-size: 12px;
}
.summary-grid strong {
  display: block;
  margin-top: 1px;
  font-size: 22px;
}
.workspace-card {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  min-height: 650px;
  border: 1px solid #dce5f0;
  border-radius: 22px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 14px 32px rgba(38, 62, 95, 0.07);
}
.category-panel {
  border-right: 1px solid #e2e9f2;
  background: #f9fbfe;
}
.panel-heading {
  padding: 22px 20px 12px;
}
.panel-heading span {
  display: block;
  color: #355579;
  font-size: 12px;
  font-weight: 850;
}
.panel-heading small {
  color: #8290a2;
}
.category-search,
.attribute-search {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 16px 14px;
  padding: 0 13px;
  border: 1px solid #d8e3ef;
  border-radius: 12px;
  background: #fff;
}
.category-search input,
.attribute-search input {
  width: 100%;
  height: 43px;
  border: 0;
  outline: 0;
  background: transparent;
}
.category-tree {
  padding: 0 9px 18px;
}
.category-item {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
  margin: 3px 0;
  padding: 11px 12px 11px calc(12px + var(--level) * 18px);
  border: 1px solid transparent;
  border-radius: 11px;
  color: #405875;
  background: transparent;
  text-align: left;
}
.category-item:hover {
  background: #f0f6ff;
}
.category-item.active {
  border-color: #a9cff9;
  color: #0c59b0;
  background: #e8f3ff;
  font-weight: 750;
}
.category-item.hidden {
  opacity: 0.58;
}
.category-name {
  flex: 1;
}
.category-name small {
  display: block;
  margin-top: 1px;
  color: #91a0b2;
  font-size: 10px;
  font-weight: 650;
}
.category-item.active .category-name small {
  color: #4e81ba;
}
.category-count {
  min-width: 27px;
  padding: 3px 7px;
  border-radius: 999px;
  background: #e8edf4;
  text-align: center;
  font-size: 11px;
}
.category-item.active .category-count {
  background: #fff;
}
.panel-loading,
.panel-empty {
  padding: 35px;
  text-align: center;
  color: #8493a5;
}
.detail-panel {
  min-width: 0;
  padding: 24px;
}
.detail-heading {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid #e4eaf2;
}
.detail-heading h2 {
  margin: 3px 0;
  font-size: 26px;
  font-weight: 850;
}
.detail-counts {
  display: flex;
  align-items: center;
  gap: 8px;
}
.detail-counts span {
  padding: 9px 11px;
  border-radius: 10px;
  color: #5d6e84;
  background: #f2f6fb;
  font-size: 12px;
}
.detail-counts b {
  color: #175fb5;
  font-size: 16px;
}
.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  margin: 18px 0;
}
.scope-guide {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  gap: 13px;
  margin: 0 0 16px;
  padding: 14px 16px;
  border: 1px solid #f0d8e3;
  border-radius: 14px;
  background: #fff7fa;
}
.scope-guide.is-shared {
  border-color: #cfe1f8;
  background: #f2f8ff;
}
.scope-guide-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 11px;
  color: #c52367;
  background: #ffe4ef;
  font-size: 18px;
}
.scope-guide.is-shared .scope-guide-icon {
  color: #1762b7;
  background: #dfeeff;
}
.scope-guide strong,
.scope-guide p {
  display: block;
  margin: 0;
}
.scope-guide p {
  margin-top: 2px;
  color: #718198;
  font-size: 12px;
}
.scope-guide > span {
  padding: 6px 9px;
  border-radius: 8px;
  color: #a3265c;
  background: #fff;
  font-size: 10px;
  font-weight: 850;
  letter-spacing: 0.04em;
}
.scope-guide.is-shared > span {
  color: #1762b7;
}
.duplicate-warning {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  margin-bottom: 16px;
  padding: 13px 15px;
  border: 1px solid #f3c879;
  border-radius: 13px;
  color: #714a08;
  background: #fff9e9;
}
.duplicate-warning > i {
  margin-top: 2px;
  color: #e69a12;
}
.duplicate-warning strong,
.duplicate-warning p {
  display: block;
  margin: 0;
}
.duplicate-warning p {
  margin-top: 3px;
  color: #806329;
  font-size: 12px;
}
.attribute-search {
  flex: 1;
  max-width: 430px;
  margin: 0;
}
.view-toggle {
  display: flex;
  padding: 4px;
  border-radius: 11px;
  background: #eff3f8;
}
.view-toggle button {
  padding: 8px 12px;
  border: 0;
  border-radius: 8px;
  color: #63758c;
  background: transparent;
}
.view-toggle button.active {
  color: #145fac;
  background: #fff;
  box-shadow: 0 3px 9px rgba(28, 56, 92, 0.09);
}
.attribute-section {
  margin-top: 19px;
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.section-title > div {
  display: flex;
  align-items: center;
  gap: 9px;
  font-weight: 850;
}
.section-title i {
  color: #d22770;
}
.section-title small {
  color: #8290a1;
}
.attribute-list {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 15px;
}
.attribute-row {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 13px 15px;
  border-bottom: 1px solid #ebf0f5;
}
.attribute-row:last-child {
  border-bottom: 0;
}
.attribute-row.inherited {
  background: #f8fbff;
}
.attribute-order {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  color: #6a7d94;
  background: #f0f4f9;
  font-weight: 800;
}
.attribute-main strong,
.attribute-main span {
  display: block;
}
.attribute-main span {
  margin-top: 2px;
  color: #8190a1;
  font-size: 12px;
}
.source-badge {
  padding: 6px 9px;
  border-radius: 8px;
  color: #2162a8;
  background: #e7f2ff;
  font-size: 11px;
  font-weight: 750;
}
.attribute-actions {
  display: flex;
  gap: 6px;
}
.icon-action {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid #dce5ee;
  border-radius: 9px;
  color: #5e7188;
  background: #fff;
}
.icon-action:disabled {
  opacity: 0.35;
}
.icon-action.edit {
  color: #1765bd;
  background: #edf6ff;
}
.icon-action.delete {
  color: #c92b57;
  background: #fff1f4;
}
.detail-loading,
.empty-selection {
  display: grid;
  place-content: center;
  justify-items: center;
  min-height: 430px;
  color: #7d8da1;
}
.empty-selection i {
  font-size: 45px;
  color: #b8c6d7;
}
.empty-selection strong {
  margin-top: 10px;
  font-size: 20px;
}
.empty-selection p {
  margin-top: 4px;
}
.section-empty {
  display: grid;
  justify-items: center;
  padding: 38px;
  border: 1px dashed #ccd8e6;
  border-radius: 15px;
  color: #718197;
  text-align: center;
}
.section-empty i {
  font-size: 27px;
}
.section-empty strong {
  margin-top: 7px;
  color: #425a78;
}
.section-empty p {
  margin: 5px 0 13px;
}
.preview-card {
  overflow: hidden;
  border: 1px solid #dce5f0;
  border-radius: 17px;
}
.preview-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  background: linear-gradient(135deg, #fff2f7, #eef6ff);
}
.preview-title span {
  color: #b82161;
  font-size: 11px;
  font-weight: 850;
}
.preview-title h3 {
  margin: 2px 0 0;
  font-size: 21px;
}
.preview-title > span {
  padding: 6px 9px;
  border-radius: 8px;
  background: #fff;
}
.preview-row {
  display: grid;
  grid-template-columns: minmax(180px, 38%) 1fr;
  padding: 13px 20px;
  border-top: 1px solid #e9edf3;
}
.preview-row span {
  color: #607189;
}
.preview-row strong {
  color: #263c59;
}
.form-note {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
  padding: 11px 12px;
  border-radius: 11px;
  color: #526b89;
  background: #edf6ff;
  font-size: 13px;
}
.form-label {
  margin-top: 9px;
  color: #344d6b;
  font-weight: 750;
}
.form-label span {
  color: #d32766;
}
.form-control,
.form-select {
  min-height: 45px;
  border-color: #d8e2ee;
  border-radius: 11px;
}
.btn-primary {
  background: #1768ce;
  border-color: #1768ce;
}
.modal-eyebrow {
  color: #c62066;
}
.panel-empty {
  padding: 30px;
}
.detail-loading .spinner-border {
  color: #d52a71;
}
@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .workspace-card {
    grid-template-columns: 280px minmax(0, 1fr);
  }
  .detail-heading {
    flex-direction: column;
  }
  .detail-counts {
    flex-wrap: wrap;
  }
}
@media (max-width: 767.98px) {
  .page-heading {
    align-items: flex-start;
    flex-wrap: wrap;
    padding: 19px;
  }
  .heading-icon {
    display: none;
  }
  .heading-action {
    width: 100%;
  }
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }
  .workspace-card {
    display: block;
  }
  .category-panel {
    border-right: 0;
    border-bottom: 1px solid #e2e9f2;
  }
  .category-tree {
    max-height: 280px;
    overflow: auto;
  }
  .detail-panel {
    padding: 17px;
  }
  .detail-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
  .scope-guide {
    grid-template-columns: 40px minmax(0, 1fr);
  }
  .scope-guide > span {
    grid-column: 2;
    justify-self: start;
  }
  .attribute-search {
    max-width: none;
  }
  .attribute-row {
    grid-template-columns: 38px minmax(0, 1fr);
  }
  .source-badge,
  .attribute-actions {
    grid-column: 2;
  }
  .attribute-actions {
    flex-wrap: wrap;
  }
  .section-title {
    align-items: flex-start;
    flex-direction: column;
  }
  .preview-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }
}
</style>
