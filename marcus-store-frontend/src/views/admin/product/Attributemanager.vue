<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-content">
        <div>
          <p class="breadcrumb-text">Sản phẩm</p>
          <h1 class="page-title">Quản lý Thuộc tính</h1>
        </div>
        <div class="header-badge"><span class="badge-dot"></span> Master Data</div>
      </div>
    </div>

    <div class="main-layout">
      <div class="panel panel-left">
        <div class="panel-header">
          <div class="panel-title-row">
            <h3 class="panel-title">
              Thuộc tính <span class="count-pill">{{ attributes.length }}</span>
            </h3>
            <button class="btn-icon-add" @click="openAddAttributeModal">Thêm mới</button>
          </div>
          <p class="panel-subtitle">
            Nhấn vào một thuộc tính để xem và quản lý các giá trị của nó.
          </p>
        </div>

        <div class="attr-list">
          <div
            v-for="attr in attributes"
            :key="attr.attributeId"
            class="attr-item"
            :class="{ active: selectedAttribute?.attributeId === attr.attributeId }"
            @click="selectAttribute(attr)"
          >
            <div class="attr-item-content">
              <div class="attr-icon">A</div>
              <div>
                <p class="attr-name">{{ attr.attributeName }}</p>
                <p class="attr-meta">{{ getValueCount(attr.attributeId) }} giá trị</p>
              </div>
            </div>
            <div class="attr-item-actions">
              <button class="btn-ghost-sm btn-edit" @click.stop="openEditAttributeModal(attr)">
                Sửa
              </button>
              <button class="btn-ghost-sm btn-del" @click.stop="deleteAttribute(attr)">Xóa</button>
            </div>
          </div>
          <div v-if="attributes.length === 0" class="empty-state-sm">
            <p>Chưa có thuộc tính nào.</p>
          </div>
        </div>
      </div>

      <div class="panel panel-right">
        <template v-if="selectedAttribute">
          <div class="panel-header">
            <div class="panel-title-row">
              <div>
                <h3 class="panel-title">
                  Giá trị của:
                  <span class="highlight-name">{{ selectedAttribute.attributeName }}</span>
                  <span class="count-pill">{{ currentValues.length }}</span>
                </h3>
                <p class="panel-subtitle">Thêm, chỉnh sửa hoặc xóa các giá trị cụ thể.</p>
              </div>
              <button class="btn-icon-add" @click="openAddValueModal">Thêm giá trị</button>
            </div>
          </div>

          <div class="values-grid">
            <div v-for="val in currentValues" :key="val.valueId" class="value-chip-card">
              <div class="value-chip-preview" :style="getColorStyle(val.valueString)"></div>
              <span class="value-chip-name">{{ val.valueString }}</span>
              <div class="value-chip-actions">
                <button class="btn-ghost-sm btn-edit" @click="openEditValueModal(val)">Sửa</button>
                <button class="btn-ghost-sm btn-del" @click="deleteValue(val)">Xóa</button>
              </div>
            </div>
            <div v-if="currentValues.length === 0" class="empty-state-sm" style="grid-column: 1/-1">
              <p>Chưa có giá trị nào. Hãy thêm mới!</p>
            </div>
          </div>
        </template>
        <div v-else class="no-selection-state">
          <p class="no-selection-text">Chọn một thuộc tính bên trái để quản lý giá trị</p>
        </div>
      </div>
    </div>

    <Transition name="modal">
      <div class="modal-backdrop" v-if="modalAttr.show" @click.self="modalAttr.show = false">
        <div class="modal-box">
          <div class="modal-header">
            <h4>{{ modalAttr.isEdit ? 'Sửa Thuộc tính' : 'Thêm Thuộc tính mới' }}</h4>
            <button class="modal-close" @click="modalAttr.show = false">✕</button>
          </div>
          <div class="modal-body">
            <label class="form-label">Tên thuộc tính</label>
            <input
              v-model="modalAttr.name"
              class="form-input"
              placeholder="VD: Màu sắc..."
              @keyup.enter="saveAttribute"
              autofocus
            />
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="modalAttr.show = false">Hủy</button>
            <button class="btn-primary" @click="saveAttribute" :disabled="!modalAttr.name.trim()">
              {{ modalAttr.isEdit ? 'Lưu thay đổi' : 'Tạo thuộc tính' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="modal">
      <div class="modal-backdrop" v-if="modalVal.show" @click.self="modalVal.show = false">
        <div class="modal-box">
          <div class="modal-header">
            <h4>{{ modalVal.isEdit ? 'Sửa Giá trị' : 'Thêm Giá trị mới' }}</h4>
            <button class="modal-close" @click="modalVal.show = false">✕</button>
          </div>
          <div class="modal-body">
            <label class="form-label"
              >Giá trị cho: <strong>{{ selectedAttribute?.attributeName }}</strong></label
            >
            <input
              v-model="modalVal.value"
              class="form-input"
              placeholder="VD: Đỏ, 256GB..."
              @keyup.enter="saveValue"
            />
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="modalVal.show = false">Hủy</button>
            <button
              v-if="!modalVal.isEdit"
              class="btn-secondary"
              @click="saveValueAndContinue"
              :disabled="!modalVal.value.trim()"
            >
              Lưu & thêm tiếp
            </button>
            <button class="btn-primary" @click="saveValue" :disabled="!modalVal.value.trim()">
              Lưu
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="modal">
      <div class="modal-backdrop" v-if="alertModal.show" @click.self="alertModal.show = false">
        <div class="modal-box alert-modal-box">
          <div class="modal-body text-center">
            <div class="alert-icon" :class="alertModal.type">
              <span v-if="alertModal.type === 'success'">✓</span>
              <span v-else>✕</span>
            </div>
            <h4 class="alert-title">
              {{ alertModal.type === 'success' ? 'Thành công' : 'Thông báo lỗi' }}
            </h4>
            <p class="alert-message">{{ alertModal.message }}</p>
          </div>
          <div class="modal-footer justify-center">
            <button class="btn-primary" @click="alertModal.show = false">Đóng</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
  <Transition name="modal">
    <div class="modal-backdrop" v-if="confirmModal.show" @click.self="confirmModal.show = false">
      <div class="modal-box alert-modal-box">
        <div class="modal-body text-center">
          <h4 class="alert-title">Xác nhận xóa</h4>
          <p class="alert-message">{{ confirmModal.message }}</p>
        </div>
        <div class="modal-footer justify-center">
          <button class="btn-cancel" @click="confirmModal.show = false">Hủy</button>
          <button class="btn-primary" style="background: #ef4444" @click="confirmDelete">
            Xóa
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/utils/api'
import '@/assets/css/attributemanager.css'

// ── STATE ──
const attributes = ref([])

const selectedAttribute = ref(null)

const modalAttr = ref({ show: false, isEdit: false, editId: null, name: '' })
const modalVal = ref({ show: false, isEdit: false, editId: null, value: '' })
const alertModal = ref({ show: false, message: '', type: 'success' })

const currentValues = computed(() => {
  if (!selectedAttribute.value) return []
  return attributeValues.value[selectedAttribute.value.attributeId] || []
})

const getValueCount = (attrId) => {
  return (attributeValues.value[attrId] || []).length
}

// ── API FETCH ──
const fetchAttributes = async () => {
  try {
    const res = await api.get('/admin/attributes')
    attributes.value = res.data?.data || []
  } catch (error) {
    showAlert(error.response?.data?.message || 'Lỗi tải danh sách thuộc tính', 'error')
  }
}

const attributeValues = ref({})
const fetchValuesForAttribute = async (attrId) => {
  try {
    const res = await api.get(`/admin/attribute-values/attribute/${attrId}`)
    attributeValues.value[attrId] = res.data.data || []
  } catch (err) {
    showAlert('Lỗi tải giá trị: ' + (err.response?.data?.message || err.message), 'error')
  }
}
onMounted(async () => {
  await fetchAttributes()
  await Promise.all(attributes.value.map((attr) => fetchValuesForAttribute(attr.attributeId)))
})
const selectAttribute = async (attr) => {
  selectedAttribute.value = attr
  console.log('Đang chọn thuộc tính:', attr)
  const id = attr.attributeId || attr.attribute_id
  await fetchValuesForAttribute(id)
}

const openAddAttributeModal = () => {
  modalAttr.value = { show: true, isEdit: false, editId: null, name: '' }
}
const openEditAttributeModal = (attr) => {
  modalAttr.value = { show: true, isEdit: true, editId: attr.attributeId, name: attr.attributeName }
}

const saveAttribute = async () => {
  const name = modalAttr.value.name.trim()
  if (!name) return
  try {
    if (modalAttr.value.isEdit)
      await api.put(`/admin/attributes/${modalAttr.value.editId}`, { name: name })
    else await api.post('/admin/attributes', { name: name })

    await fetchAttributes()
    modalAttr.value.show = false
    showAlert(
      modalAttr.value.isEdit ? 'Đã cập nhật thuộc tính!' : 'Đã thêm thuộc tính mới!',
      'success',
    )
  } catch (err) {
    showAlert(err.response?.data?.message || 'Có lỗi xảy ra', 'error')
  }
}

const confirmModal = ref({ show: false, message: '', action: null })

const deleteAttribute = (attr) => {
  confirmModal.value = {
    show: true,
    message: `Xóa thuộc tính "${attr.attributeName}"?`,
    action: async () => {
      await api.delete(`/admin/attributes/${attr.attributeId}`)
      attributes.value = attributes.value.filter((a) => a.attributeId !== attr.attributeId)
      confirmModal.value.show = false
    },
  }
}

const confirmDelete = async () => {
  try {
    await confirmModal.value.action()
  } catch (err) {
    showAlert(err.response?.data?.message || 'Lỗi xóa', 'error')
  }
}

const openAddValueModal = () => {
  modalVal.value = { show: true, isEdit: false, editId: null, value: '' }
}
const openEditValueModal = (val) => {
  modalVal.value = { show: true, isEdit: true, editId: val.valueId, value: val.valueString }
}

const saveValue = async () => {
  const vs = modalVal.value.value.trim()
  if (!vs) return
  try {
    if (modalVal.value.isEdit)
      await api.put(`/admin/attribute-values/${modalVal.value.editId}`, { valueString: vs })
    else
      await api.post('/admin/attribute-values', {
        attributeId: selectedAttribute.value.attributeId,
        valueString: vs,
      })

    await fetchValuesForAttribute(selectedAttribute.value.attributeId)
    modalVal.value.show = false
    showAlert('Đã lưu giá trị thành công!', 'success')
  } catch (err) {
    showAlert(err.response?.data?.message || 'Lưu thất bại', 'error')
  }
}

const saveValueAndContinue = async () => {
  await saveValue()
  modalVal.value = { show: true, isEdit: false, editId: null, value: '' }
}

const deleteValue = async (val) => {
  if (!confirm(`Xóa giá trị "${val.valueString}"?`)) return
  try {
    await api.delete(`/admin/attribute-values/${val.valueId}`)
    await fetchValuesForAttribute(selectedAttribute.value.attributeId)
    showAlert('Đã xóa giá trị thành công!', 'success')
  } catch (err) {
    showAlert(err.response?.data?.message || 'Xóa thất bại!', 'error')
  }
}

// ── UTILS ──
const showAlert = (msg, type = 'success') => {
  alertModal.value = { show: true, message: msg, type }
}

const colorKeywords = {
  đen: '#1a1a2e',
  black: '#1a1a2e',
  trắng: '#f0f0f0',
  white: '#f0f0f0',
  hồng: '#f472b6',
  pink: '#f472b6',
  đỏ: '#ef4444',
  red: '#ef4444',
  'xanh lam': '#3b82f6',
  blue: '#3b82f6',
  'xanh lá': '#22c55e',
  green: '#22c55e',
  vàng: '#eab308',
  gold: '#eab308',
  titan: '#8b8fa8',
  titanium: '#8b8fa8',
  xám: '#6b7280',
  gray: '#6b7280',
  tím: '#a855f7',
  purple: '#a855f7',
}
const getColorStyle = (valueStr) => {
  if (!valueStr) return {}
  const lower = valueStr.toLowerCase()
  for (const [key, color] of Object.entries(colorKeywords)) {
    if (lower.includes(key)) return { background: color }
  }
  return { background: 'linear-gradient(135deg, #fce7f3, #fbcfe8)' }
}
</script>
<style scoped></style>
