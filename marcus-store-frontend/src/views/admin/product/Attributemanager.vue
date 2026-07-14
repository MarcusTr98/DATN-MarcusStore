<template>
  <div class="amg-page">
    <div class="amg-header">
      <div class="amg-header-left">
        <div class="amg-header-icon">
          <i class="fa-solid fa-tags"></i>
        </div>
        <div>
          <p class="amg-breadcrumb">Sản phẩm</p>
          <h1 class="amg-title">
            Quản lý Thuộc tính
            <span class="amg-badge"><span class="amg-badge-dot"></span>Master Data</span>
          </h1>
          <p class="amg-subtitle">
            Quản lý các thuộc tính và giá trị áp dụng cho biến thể sản phẩm
          </p>
        </div>
      </div>
    </div>

    <div class="amg-layout">
      <div class="amg-panel">
        <div class="amg-panel-header">
          <div class="amg-panel-title-row">
            <h3 class="amg-panel-title">
              Thuộc tính <span class="amg-count-pill">{{ attributes.length }}</span>
            </h3>
            <button class="amg-btn-add" @click="openAddAttributeModal">Thêm mới</button>
          </div>
          <p class="amg-panel-subtitle">Nhấn vào một thuộc tính để xem các giá trị của nó.</p>
        </div>

        <div class="amg-attr-list">
          <div
            v-for="attr in attributes"
            :key="attr.attributeId"
            class="amg-attr-item"
            :class="{ 'is-active': selectedAttribute?.attributeId === attr.attributeId }"
            @click="selectAttribute(attr)"
          >
            <div class="amg-attr-content">
              <div class="amg-attr-icon">A</div>
              <div>
                <p class="amg-attr-name">{{ attr.attributeName }}</p>
                <p class="amg-attr-meta">{{ getValueCount(attr.attributeId) }} giá trị</p>
              </div>
            </div>
            <div class="amg-attr-actions">
              <button class="amg-btn-ghost amg-btn-edit" @click.stop="openEditAttributeModal(attr)">
                Sửa
              </button>
              <button class="amg-btn-ghost amg-btn-del" @click.stop="deleteAttribute(attr)">
                Xóa
              </button>
            </div>
          </div>
          <div v-if="attributes.length === 0" class="amg-empty-sm">
            <p>Chưa có thuộc tính nào.</p>
          </div>
        </div>
      </div>

      <div class="amg-panel">
        <template v-if="selectedAttribute">
          <div class="amg-panel-header">
            <div class="amg-panel-title-row">
              <div>
                <h3 class="amg-panel-title">
                  Giá trị của:
                  <span class="amg-highlight">{{ selectedAttribute.attributeName }}</span>
                  <span class="amg-count-pill">{{ currentValues.length }}</span>
                </h3>
                <p class="amg-panel-subtitle">Thêm, chỉnh sửa hoặc xóa các giá trị cụ thể.</p>
              </div>
              <button class="amg-btn-add" @click="openAddValueModal">Thêm giá trị</button>
            </div>
          </div>

          <div class="amg-values-grid">
            <div v-for="val in currentValues" :key="val.valueId" class="amg-value-card">
              <div
                v-if="val.valueMeta && val.valueMeta.startsWith('#')"
                class="amg-value-preview"
                :style="{ backgroundColor: val.valueMeta }"
              ></div>
              <div v-else class="amg-value-preview-fallback">
                <i class="fa-solid fa-microchip"></i>
              </div>

              <span class="amg-value-name">{{ val.valueString }}</span>
              <div class="amg-value-actions">
                <button class="amg-btn-ghost amg-btn-edit" @click="openEditValueModal(val)">
                  Sửa
                </button>
                <button class="amg-btn-ghost amg-btn-del" @click="deleteValue(val)">Xóa</button>
              </div>
            </div>
            <div v-if="currentValues.length === 0" class="amg-empty-sm" style="grid-column: 1/-1">
              <p>Chưa có giá trị nào. Hãy thêm mới!</p>
            </div>
          </div>
        </template>
        <div v-else class="amg-empty-panel">
          <p class="amg-empty-panel-text">Chọn một thuộc tính bên trái để quản lý giá trị</p>
        </div>
      </div>
    </div>

    <!-- MODAL: THÊM / SỬA THUỘC TÍNH -->
    <Transition name="modal">
      <div class="amg-modal-backdrop" v-if="modalAttr.show" @click.self="modalAttr.show = false">
        <div class="amg-modal">
          <div class="amg-modal-header">
            <h4>{{ modalAttr.isEdit ? 'Sửa Thuộc tính' : 'Thêm Thuộc tính mới' }}</h4>
            <button class="amg-modal-close" @click="modalAttr.show = false">✕</button>
          </div>
          <div class="amg-modal-body">
            <label class="amg-label">Tên thuộc tính</label>
            <input
              v-model="modalAttr.name"
              class="amg-input"
              placeholder="VD: Màu sắc, Bộ nhớ..."
              @keyup.enter="saveAttribute"
              autofocus
            />
          </div>
          <div class="amg-modal-footer">
            <button class="amg-btn-cancel" @click="modalAttr.show = false">Hủy</button>
            <button
              class="amg-btn-primary"
              @click="saveAttribute"
              :disabled="!modalAttr.name.trim()"
            >
              {{ modalAttr.isEdit ? 'Lưu thay đổi' : 'Tạo thuộc tính' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- MODAL: THÊM / SỬA GIÁ TRỊ -->
    <Transition name="modal">
      <div class="amg-modal-backdrop" v-if="modalVal.show" @click.self="modalVal.show = false">
        <div class="amg-modal">
          <div class="amg-modal-header">
            <h4>{{ modalVal.isEdit ? 'Sửa Giá trị' : 'Thêm Giá trị mới' }}</h4>
            <button class="amg-modal-close" @click="modalVal.show = false">✕</button>
          </div>

          <div class="amg-modal-body">
            <label class="amg-label">
              Giá trị thuộc tính:
              <strong class="amg-highlight">{{ selectedAttribute?.attributeName }}</strong>
            </label>

            <div class="amg-input-unit-row">
              <input
                v-model="modalVal.value"
                class="amg-input"
                :placeholder="inputPlaceholder"
                @keyup.enter="saveValue"
              />
              <select v-model="modalVal.unit" class="amg-input amg-select-unit">
                <option value="">(Thường)</option>
                <option value="COLOR">Màu sắc</option>
                <option value="GB">GB</option>
                <option value="TB">TB</option>
                <option value="mAh">mAh</option>
                <option value="W">W</option>
              </select>
            </div>

            <div v-if="modalVal.unit === 'COLOR'" class="amg-dynamic">
              <label class="amg-label" style="font-size: 11px">Mã màu hiển thị trên Website</label>
              <div class="amg-color-swatches">
                <button
                  v-for="(color, idx) in predefinedColors"
                  :key="idx"
                  class="amg-color-swatch"
                  :class="{ 'is-active': modalVal.colorHex === color.hex }"
                  :style="{ backgroundColor: color.hex }"
                  :title="color.name"
                  @click="modalVal.colorHex = color.hex"
                ></button>

                <div
                  class="amg-color-custom"
                  :style="{ borderColor: modalVal.colorHex ? modalVal.colorHex : '#d1d5db' }"
                >
                  <input type="color" v-model="modalVal.colorHex" class="amg-color-custom-input" />
                  <span class="amg-color-custom-label">Khác</span>
                </div>
              </div>
            </div>

            <div v-else-if="['GB', 'TB', 'mAh', 'W'].includes(modalVal.unit)" class="amg-dynamic">
              <label class="amg-label" style="font-size: 11px">Gợi ý bấm nhanh</label>
              <div class="amg-suggest-row">
                <button
                  v-for="sug in quickSuggestions[modalVal.unit]"
                  :key="sug"
                  class="amg-suggest-pill"
                  @click="modalVal.value = sug"
                >
                  {{ sug }}
                </button>
              </div>
            </div>
          </div>
          <div class="amg-modal-footer">
            <button class="amg-btn-cancel" @click="modalVal.show = false">Hủy</button>
            <button
              v-if="!modalVal.isEdit"
              class="amg-btn-secondary"
              @click="saveValueAndContinue"
              :disabled="!modalVal.value.trim()"
            >
              Lưu & thêm tiếp
            </button>
            <button class="amg-btn-primary" @click="saveValue" :disabled="!modalVal.value.trim()">
              Lưu
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- MODAL: ALERT -->
    <Transition name="modal">
      <div class="amg-modal-backdrop" v-if="alertModal.show" @click.self="alertModal.show = false">
        <div class="amg-modal amg-alert-modal">
          <div class="amg-modal-body amg-text-center">
            <div
              class="amg-alert-icon"
              :class="alertModal.type === 'success' ? 'is-success' : 'is-error'"
            >
              <span v-if="alertModal.type === 'success'">✓</span><span v-else>✕</span>
            </div>
            <h4 class="amg-alert-title">
              {{ alertModal.type === 'success' ? 'Thành công' : 'Thông báo lỗi' }}
            </h4>
            <p class="amg-alert-message">{{ alertModal.message }}</p>
          </div>
          <div class="amg-modal-footer amg-justify-center">
            <button class="amg-btn-primary" @click="alertModal.show = false">Đóng</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- MODAL: XÁC NHẬN XÓA -->
    <Transition name="modal">
      <div
        class="amg-modal-backdrop"
        v-if="confirmModal.show"
        @click.self="confirmModal.show = false"
      >
        <div class="amg-modal amg-alert-modal">
          <div class="amg-modal-body amg-text-center">
            <h4 class="amg-alert-title">Xác nhận xóa</h4>
            <p class="amg-alert-message">{{ confirmModal.message }}</p>
          </div>
          <div class="amg-modal-footer amg-justify-center">
            <button class="amg-btn-cancel" @click="confirmModal.show = false">Hủy</button>
            <button class="amg-btn-primary amg-btn-danger" @click="confirmDelete">Xóa</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/utils/api'
import '@/assets/css/attributemanager.css'

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
