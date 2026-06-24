<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon-box">
          <i class="fa-solid fa-tags"></i>
        </div>
        <div class="header-text-group">
          <p class="breadcrumb-text">Sản phẩm</p>
          <h1 class="page-title">
            Quản lý Thuộc tính
            <span class="header-badge"><span class="badge-dot"></span>Master Data</span>
          </h1>
          <p class="page-subtitle">
            Quản lý các thuộc tính và giá trị áp dụng cho biến thể sản phẩm
          </p>
        </div>
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
          <p class="panel-subtitle">Nhấn vào một thuộc tính để xem các giá trị của nó.</p>
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
              <div
                v-if="val.valueMeta && val.valueMeta.startsWith('#')"
                class="value-chip-preview"
                :style="{ backgroundColor: val.valueMeta }"
              ></div>
              <div
                v-else
                class="value-chip-preview"
                style="
                  background: #f1f5f9;
                  color: #64748b;
                  font-size: 10px;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                "
              >
                <i class="fa-solid fa-microchip"></i>
              </div>

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
              placeholder="VD: Màu sắc, Bộ nhớ..."
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
            <label class="form-label">
              Giá trị thuộc tính:
              <strong style="color: #db2777">{{ selectedAttribute?.attributeName }}</strong>
            </label>

            <div class="input-with-unit">
              <input
                v-model="modalVal.value"
                class="form-input"
                :placeholder="inputPlaceholder"
                @keyup.enter="saveValue"
              />
              <select v-model="modalVal.unit" class="form-input select-unit">
                <option value="">(Thường)</option>
                <option value="COLOR">Màu sắc</option>
                <option value="GB">GB</option>
                <option value="TB">TB</option>
                <option value="mAh">mAh</option>
                <option value="W">W</option>
              </select>
            </div>

            <div v-if="modalVal.unit === 'COLOR'" class="dynamic-section">
              <label class="form-label" style="font-size: 11px">Mã màu hiển thị trên Website</label>
              <div class="color-swatches">
                <button
                  v-for="(color, idx) in predefinedColors"
                  :key="idx"
                  class="color-swatch-btn"
                  :class="{ active: modalVal.colorHex === color.hex }"
                  :style="{ backgroundColor: color.hex }"
                  :title="color.name"
                  @click="modalVal.colorHex = color.hex"
                ></button>

                <div
                  class="color-custom-wrapper"
                  :style="{ borderColor: modalVal.colorHex ? modalVal.colorHex : '#d1d5db' }"
                >
                  <input type="color" v-model="modalVal.colorHex" class="color-custom-input" />
                  <span class="color-custom-label">Khác</span>
                </div>
              </div>
            </div>

            <div
              v-else-if="['GB', 'TB', 'mAh', 'W'].includes(modalVal.unit)"
              class="dynamic-section"
            >
              <label class="form-label" style="font-size: 11px">Gợi ý bấm nhanh</label>
              <div class="quick-suggest-row">
                <button
                  v-for="sug in quickSuggestions[modalVal.unit]"
                  :key="sug"
                  class="suggest-pill"
                  @click="modalVal.value = sug"
                >
                  {{ sug }}
                </button>
              </div>
            </div>
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
              <span v-if="alertModal.type === 'success'">✓</span><span v-else>✕</span>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/utils/api'
import '@/assets/css/AttributeManager.css'

// ── CẤU HÌNH GỢI Ý NHANH VÀ BẢNG MÀU ──
const predefinedColors = [
  { name: 'Đen', hex: '#1a1a2e' },
  { name: 'Trắng', hex: '#f0f0f0' },
  { name: 'Xám Titan', hex: '#8b8fa8' },
  { name: 'Bạc', hex: '#e2e8f0' },
  { name: 'Đỏ', hex: '#ef4444' },
  { name: 'Xanh dương', hex: '#3b82f6' },
  { name: 'Xanh lá', hex: '#22c55e' },
  { name: 'Vàng/Gold', hex: '#eab308' },
  { name: 'Hồng', hex: '#f472b6' },
  { name: 'Tím', hex: '#a855f7' },
]

const quickSuggestions = {
  GB: ['64', '128', '256', '512'],
  TB: ['1', '2', '4'],
  mAh: ['4000', '4500', '5000', '6000'],
  W: ['15', '20', '25', '45', '65', '120'],
}

const inputPlaceholder = computed(() => {
  if (modalVal.value.unit === 'COLOR') return 'Nhập tên màu (VD: Đen Phantom)'
  if (['GB', 'TB', 'mAh', 'W'].includes(modalVal.value.unit)) return 'Nhập số (VD: 256)'
  return 'Nhập giá trị (VD: Bản Tiêu Chuẩn)'
})

// ── STATE ──
const attributes = ref([])
const selectedAttribute = ref(null)
const attributeValues = ref({})

const modalAttr = ref({ show: false, isEdit: false, editId: null, name: '' })
const modalVal = ref({
  show: false,
  isEdit: false,
  editId: null,
  value: '',
  unit: '',
  colorHex: '',
})
const alertModal = ref({ show: false, message: '', type: 'success' })
const confirmModal = ref({ show: false, message: '', action: null })

const currentValues = computed(() => {
  if (!selectedAttribute.value) return []
  return attributeValues.value[selectedAttribute.value.attributeId] || []
})

const getValueCount = (attrId) => (attributeValues.value[attrId] || []).length

// ── API FETCH ──
const fetchAttributes = async () => {
  try {
    const res = await api.get('/admin/attributes')
    attributes.value = res.data?.data || []
  } catch (error) {
    showAlert(error.response?.data?.message || 'Lỗi tải danh sách', 'error')
  }
}

const fetchValuesForAttribute = async (attrId) => {
  try {
    const res = await api.get(`/admin/attribute-values/attribute/${attrId}`)
    attributeValues.value[attrId] = res.data.data || []
  } catch {
    showAlert('Lỗi tải giá trị', 'error')
  }
}

onMounted(async () => {
  await fetchAttributes()
  await Promise.all(attributes.value.map((attr) => fetchValuesForAttribute(attr.attributeId)))
})

const selectAttribute = async (attr) => {
  selectedAttribute.value = attr
  await fetchValuesForAttribute(attr.attributeId)
}

// ── THUỘC TÍNH CRUD ──
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
      await api.put(`/admin/attributes/${modalAttr.value.editId}`, { name })
    else await api.post('/admin/attributes', { name })
    await fetchAttributes()
    modalAttr.value.show = false
    showAlert(modalAttr.value.isEdit ? 'Đã cập nhật!' : 'Đã thêm thuộc tính!', 'success')
  } catch (err) {
    showAlert(err.response?.data?.message || 'Có lỗi xảy ra', 'error')
  }
}

const deleteAttribute = (attr) => {
  confirmModal.value = {
    show: true,
    message: `Xóa thuộc tính "${attr.attributeName}"?`,
    action: async () => {
      await api.delete(`/admin/attributes/${attr.attributeId}`)
      attributes.value = attributes.value.filter((a) => a.attributeId !== attr.attributeId)
      if (selectedAttribute.value?.attributeId === attr.attributeId) selectedAttribute.value = null
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

// ── GIÁ TRỊ CRUD ──
const openAddValueModal = () => {
  // Logic Auto-Detect thông minh dựa vào tên Thuộc tính
  let autoUnit = ''
  const attrName = selectedAttribute.value?.attributeName?.toLowerCase() || ''

  if (attrName.includes('màu') || attrName.includes('color')) autoUnit = 'COLOR'
  else if (attrName.includes('nhớ') || attrName.includes('lượng')) autoUnit = 'GB'
  else if (attrName.includes('pin')) autoUnit = 'mAh'
  else if (attrName.includes('sạc') || attrName.includes('công suất')) autoUnit = 'W'

  modalVal.value = {
    show: true,
    isEdit: false,
    editId: null,
    value: '',
    unit: autoUnit,
    colorHex: '#1a1a2e',
  }
}

const openEditValueModal = (val) => {
  let sValue = val.valueString
  let sUnit = ''

  if (val.valueMeta && val.valueMeta.startsWith('#')) {
    sUnit = 'COLOR'
  } else {
    // Regex bóc tách chữ số và đơn vị (nếu có đuôi GB, TB, mAh, W)
    const match = val.valueString.match(/^(.*?)(GB|TB|mAh|W)$/i)
    if (match) {
      sValue = match[1].trim()
      sUnit = match[2].toUpperCase()
    }
  }

  modalVal.value = {
    show: true,
    isEdit: true,
    editId: val.valueId,
    value: sValue,
    unit: sUnit,
    colorHex: val.valueMeta || '#1a1a2e',
  }
}

const saveValue = async () => {
  let finalValueString = modalVal.value.value.trim()
  let finalValueMeta = null

  // Gộp chuỗi nếu là dạng Đơn vị (GB, mAh...)
  if (modalVal.value.unit === 'COLOR') {
    finalValueMeta = modalVal.value.colorHex || null
  } else if (modalVal.value.unit !== '') {
    finalValueString = `${finalValueString}${modalVal.value.unit}`
  }

  try {
    const payload = {
      attributeId: selectedAttribute.value.attributeId,
      valueString: finalValueString,
      valueMeta: finalValueMeta,
    }

    if (modalVal.value.isEdit)
      await api.put(`/admin/attribute-values/${modalVal.value.editId}`, payload)
    else await api.post('/admin/attribute-values', payload)

    await fetchValuesForAttribute(selectedAttribute.value.attributeId)
    modalVal.value.show = false
    showAlert('Đã lưu giá trị thành công!', 'success')
  } catch (err) {
    showAlert(err.response?.data?.message || 'Lưu thất bại', 'error')
  }
}

const saveValueAndContinue = async () => {
  await saveValue()
  openAddValueModal()
}

const deleteValue = async (val) => {
  confirmModal.value = {
    show: true,
    message: `Xóa giá trị "${val.valueString}"?`,
    action: async () => {
      await api.delete(`/admin/attribute-values/${val.valueId}`)
      await fetchValuesForAttribute(selectedAttribute.value.attributeId)
      confirmModal.value.show = false
    },
  }
}

const showAlert = (msg, type = 'success') => {
  alertModal.value = { show: true, message: msg, type }
}
</script>

<style scoped>
/* Khối Nhập liệu + Đơn vị */
.input-with-unit {
  display: flex;
  gap: 12px;
  align-items: center;
}

.select-unit {
  width: 110px;
  flex-shrink: 0;
  cursor: pointer;
  background-color: #fff9fc;
  color: #db2777;
  font-weight: 700;
}

/* Khu vực hiển thị Động (Dynamic Section) */
.dynamic-section {
  margin-top: 14px;
  padding: 12px;
  background: #fdf6f9;
  border: 1px dashed #fce7f3;
  border-radius: 12px;
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Gợi ý Bấm nhanh (Quick Suggest Pills) */
.quick-suggest-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.suggest-pill {
  background: #fff;
  border: 1.5px solid #fce7f3;
  color: #db2777;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.15s;
}

.suggest-pill:hover {
  background: #fdf2f8;
  border-color: #f9a8d4;
  transform: translateY(-1px);
}
</style>
