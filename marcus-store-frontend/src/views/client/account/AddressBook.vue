<template>
  <div class="content-card">
    <div class="card-head">
      <div class="card-title-group">
        <span class="card-icon"><i class="fas fa-map-marker-alt"></i></span>
        <h5 class="card-title">Sổ địa chỉ nhận hàng</h5>
      </div>
      <button class="btn-red btn-red--sm" @click="openModal()">
        <i class="fas fa-plus me-1"></i> Thêm địa chỉ
      </button>
    </div>

    <div v-if="isLoading" class="state-loading">
      <span class="spinner-cps"></span><span>Đang tải địa chỉ...</span>
    </div>

    <div v-else-if="addresses.length === 0" class="state-empty">
      <i class="fas fa-map-marked-alt state-empty-icon"></i>
      <p class="state-empty-title">Chưa có địa chỉ nhận hàng</p>
      <button class="btn-red" @click="openModal()">
        <i class="fas fa-plus me-2"></i> Thêm ngay
      </button>
    </div>

    <div v-else class="address-list">
      <div
        v-for="addr in addresses"
        :key="addr.addressId"
        class="address-item"
        :class="{ 'address-item--default': addr.isDefault }"
      >
        <div class="address-item__left">
          <div class="address-item__row1">
            <span class="a-name">{{ addr.recipientName }}</span>
            <span class="a-phone"><i class="fas fa-phone-alt me-1"></i>{{ addr.phoneNumber }}</span>
            <span v-if="addr.isDefault" class="a-badge-default"
              ><i class="fas fa-check-circle me-1"></i>Mặc định</span
            >
          </div>
          <p class="a-detail"><i class="fas fa-home me-1"></i>{{ addr.detailAddress }}</p>
          <p class="a-region">
            <i class="fas fa-map-pin me-1"></i>{{ addr.wardName }}, {{ addr.districtName }},
            {{ addr.provinceName }}
          </p>
        </div>
        <div class="address-item__actions">
          <div class="actions-row">
            <button class="btn-link btn-link--blue" @click="openModal(addr)">
              <i class="fas fa-pen me-1"></i> Sửa
            </button>
            <button
              v-if="!addr.isDefault"
              class="btn-link btn-link--red"
              @click="deleteAddress(addr.addressId)"
            >
              <i class="fas fa-trash me-1"></i> Xóa
            </button>
          </div>
          <button v-if="!addr.isDefault" class="btn-outline-sm" @click="setDefault(addr.addressId)">
            Đặt mặc định
          </button>
        </div>
      </div>
    </div>

    <transition name="modal-fade">
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-box">
          <div class="modal-hd">
            <div class="card-title-group">
              <span class="card-icon card-icon--sm"><i class="fas fa-map-marker-alt"></i></span>
              <h5 class="modal-title">{{ isEditing ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới' }}</h5>
            </div>
            <button class="btn-close-x" @click="closeModal"><i class="fas fa-times"></i></button>
          </div>

          <div class="modal-bd custom-scrollbar">
            <form class="row g-3">
              <div class="col-12">
                <label class="field-lbl"
                  ><i class="fas fa-crosshairs me-1 text-cps"></i> Vị trí Bản đồ</label
                >
                <div class="map-wrap">
                  <MapLocator
                    :initialLat="form.latitude"
                    :initialLng="form.longitude"
                    @update:location="handleLocation"
                    @apply-address="applyAddressToForm"
                  />
                </div>
              </div>
              <div class="col-12"><hr class="divider-light" /></div>
              <div class="col-md-6">
                <label class="field-lbl field-lbl--req">Người nhận</label>
                <input v-model.trim="form.recipientName" type="text" class="f-input" />
              </div>
              <div class="col-md-6">
                <label class="field-lbl field-lbl--req">Số điện thoại</label>
                <input v-model.trim="form.phoneNumber" type="text" class="f-input" />
              </div>
              <div class="col-md-4">
                <label class="field-lbl field-lbl--req">Tỉnh / Thành phố</label>
                <input v-model.trim="form.provinceName" type="text" class="f-input" />
              </div>
              <div class="col-md-4">
                <label class="field-lbl">Quận / Huyện</label>
                <input v-model.trim="form.districtName" type="text" class="f-input" />
              </div>
              <div class="col-md-4">
                <label class="field-lbl">Phường / Xã</label>
                <input v-model.trim="form.wardName" type="text" class="f-input" />
              </div>
              <div class="col-12">
                <label class="field-lbl">Số nhà, tên đường</label>
                <input v-model.trim="form.detailAddress" type="text" class="f-input" />
              </div>
              <div class="col-12">
                <label class="checkbox-lbl">
                  <input v-model="form.isDefault" type="checkbox" class="chk-input" />
                  <span class="chk-box"></span
                  ><span class="chk-text">Đặt làm địa chỉ mặc định</span>
                </label>
              </div>
            </form>
          </div>

          <div class="modal-ft">
            <button class="btn-ghost" @click="closeModal">Trở lại</button>
            <button class="btn-red" @click="saveAddress" :disabled="isSaving">
              {{ isSaving ? 'Đang lưu...' : 'Hoàn thành' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <BaseModal
      :visible="localModal.visible"
      :type="localModal.type"
      :title="localModal.title"
      :message="localModal.message"
      @close="localModal.visible = false"
      @confirm="execConfirm"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import addressApi from '@/api/addressApi'
import MapLocator from '@/components/common/MapLocator.vue'
import BaseModal from '@/components/common/BaseModal.vue'

const addresses = ref([])
const isLoading = ref(false)
const showModal = ref(false)
const isEditing = ref(false)
const isSaving = ref(false)

const form = reactive({
  addressId: null,
  recipientName: '',
  phoneNumber: '',
  provinceName: '',
  districtName: '',
  wardName: '',
  detailAddress: '',
  isDefault: false,
  latitude: null,
  longitude: null,
})

// Local Alert & Confirm
const localModal = reactive({
  visible: false,
  type: 'info',
  title: '',
  message: '',
  actionCallback: null,
})
const showAlert = (type, title, msg) => {
  localModal.type = type
  localModal.title = title
  localModal.message = msg
  localModal.actionCallback = null
  localModal.visible = true
}
const showConfirm = (title, msg, cb) => {
  localModal.type = 'confirm'
  localModal.title = title
  localModal.message = msg
  localModal.actionCallback = cb
  localModal.visible = true
}
const execConfirm = () => {
  if (localModal.actionCallback) localModal.actionCallback()
}

onMounted(() => {
  fetchAddresses()
})

const fetchAddresses = async () => {
  isLoading.value = true
  try {
    const res = await addressApi.getMyAddresses()
    addresses.value = res.data.data
  } catch (error) {
    console.error(error)
  } finally {
    isLoading.value = false
  }
}

const openModal = (addr = null) => {
  if (addr) {
    isEditing.value = true
    Object.assign(form, addr)
  } else {
    isEditing.value = false
    Object.assign(form, {
      addressId: null,
      recipientName: localStorage.getItem('USERNAME') || '',
      phoneNumber: '',
      provinceName: '',
      districtName: '',
      wardName: '',
      detailAddress: '',
      isDefault: false,
      latitude: null,
      longitude: null,
    })
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

//chạy ngầm mỗi khi ghim bị di chuyển (Chỉ lấy tọa độ, tuyệt đối không chạm vào text)
const handleLocation = (locData) => {
  form.latitude = locData.lat
  form.longitude = locData.lng
}

//kHI bấm nút Điền xuống Form
const applyAddressToForm = (locData) => {
  if (locData.province) form.provinceName = locData.province
  if (locData.district) form.districtName = locData.district
  if (locData.ward) form.wardName = locData.ward

  if (locData.detail) {
    form.detailAddress = locData.detail
  } else if (locData.full) {
    form.detailAddress = locData.full.split(',')[0]
  }
}

const saveAddress = async () => {
  if (!form.recipientName || !form.phoneNumber || !form.provinceName) {
    showAlert('error', 'Thiếu dữ liệu', 'Vui lòng nhập đủ các trường bắt buộc!')
    return
  }
  isSaving.value = true
  try {
    if (isEditing.value) await addressApi.updateAddress(form.addressId, form)
    else await addressApi.addAddress(form)
    closeModal()
    fetchAddresses()
    showAlert('success', 'Thành công', 'Lưu địa chỉ thành công.')
  } catch (err) {
    showAlert('error', 'Lỗi', err.response?.data?.message || 'Có lỗi xảy ra')
  } finally {
    isSaving.value = false
  }
}

const setDefault = async (id) => {
  await addressApi.setAsDefault(id)
  fetchAddresses()
}
const deleteAddress = async (id) => {
  showConfirm('Xác nhận xóa', 'Bạn có chắc chắn muốn xóa địa chỉ này?', async () => {
    try {
      await addressApi.deleteAddress(id)
      showAlert('success', 'Đã xóa', 'Địa chỉ đã được xóa.')
      fetchAddresses()
    } catch (e) {
      showAlert('error', 'Lỗi', e.response?.data?.message || 'Không thể xóa')
    }
  })
}
</script>
