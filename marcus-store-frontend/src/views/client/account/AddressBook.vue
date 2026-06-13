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
                <p class="fw-bold text-dark mb-1">
                  <i class="fas fa-user text-danger me-1"></i> 1. Thông tin liên hệ
                </p>
              </div>
              <div class="col-md-6">
                <label class="field-lbl field-lbl--req">Họ và tên người nhận</label>
                <input v-model.trim="form.recipientName" type="text" class="f-input" />
              </div>
              <div class="col-md-6">
                <label class="field-lbl field-lbl--req">Số điện thoại</label>
                <input v-model.trim="form.phoneNumber" type="tel" class="f-input" />
              </div>

              <div class="col-12 mt-4">
                <p class="fw-bold text-dark mb-1">
                  <i class="fas fa-truck text-danger me-1"></i> 2. Khu vực giao hàng (Tính phí ship)
                </p>
              </div>
              <div class="col-md-4">
                <label class="field-lbl field-lbl--req">Tỉnh / Thành phố</label>
                <select v-model="form.provinceId" @change="onProvinceChange" class="f-input">
                  <option value="" disabled>-- Chọn Tỉnh/TP --</option>
                  <option v-for="p in ghnData.provinces" :key="p.ProvinceID" :value="p.ProvinceID">
                    {{ p.ProvinceName }}
                  </option>
                </select>
              </div>
              <div class="col-md-4">
                <label class="field-lbl field-lbl--req">Quận / Huyện</label>
                <select
                  v-model="form.districtId"
                  @change="onDistrictChange"
                  class="f-input"
                  :disabled="!form.provinceId"
                >
                  <option value="" disabled>-- Chọn Quận/Huyện --</option>
                  <option v-for="d in ghnData.districts" :key="d.DistrictID" :value="d.DistrictID">
                    {{ d.DistrictName }}
                  </option>
                </select>
              </div>
              <div class="col-md-4">
                <label class="field-lbl field-lbl--req">Phường / Xã</label>
                <select
                  v-model="form.wardCode"
                  @change="onWardChange"
                  class="f-input"
                  :disabled="!form.districtId"
                >
                  <option value="" disabled>-- Chọn Phường/Xã --</option>
                  <option v-for="w in ghnData.wards" :key="w.WardCode" :value="w.WardCode">
                    {{ w.WardName }}
                  </option>
                </select>
              </div>

              <div class="col-12">
                <label class="field-lbl field-lbl--req">Số nhà, Tên đường (Chi tiết)</label>
                <input
                  v-model.trim="form.detailAddress"
                  type="text"
                  class="f-input"
                  placeholder="Ví dụ: Số 12, ngõ 34 Bạch Đằng..."
                />
              </div>

              <div class="col-12 mt-4">
                <p class="fw-bold text-dark mb-1">
                  <i class="fas fa-crosshairs text-danger me-1"></i> 3. Định vị tọa độ (Tùy chọn)
                </p>
                <p class="field-hint mb-2">
                  Giúp Shipper tìm đường dễ hơn. Bấm "Vị trí của tôi" hoặc tự kéo ghim trên bản đồ.
                </p>
                <MapLocator
                  :initialLat="form.latitude"
                  :initialLng="form.longitude"
                  @update:location="handleMapCoordinateUpdate"
                />
              </div>

              <div class="col-12 mt-3">
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
import ghnApi from '@/api/ghnApi'
import MapLocator from '@/components/common/MapLocator.vue'
import BaseModal from '@/components/common/BaseModal.vue'

const addresses = ref([])
const isLoading = ref(false)
const showModal = ref(false)
const isEditing = ref(false)
const isSaving = ref(false)

const ghnData = reactive({ provinces: [], districts: [], wards: [] })

const form = reactive({
  addressId: null,
  recipientName: '',
  phoneNumber: '',
  provinceId: '',
  provinceName: '',
  districtId: '',
  districtName: '',
  wardCode: '',
  wardName: '',
  detailAddress: '',
  isDefault: false,
  latitude: null,
  longitude: null,
})

const localModal = reactive({
  visible: false,
  type: 'info',
  title: '',
  message: '',
  actionCallback: null,
})
const showAlert = (type, title, msg) => {
  Object.assign(localModal, { type, title, message: msg, actionCallback: null, visible: true })
}
const showConfirm = (title, msg, cb) => {
  Object.assign(localModal, {
    type: 'confirm',
    title,
    message: msg,
    actionCallback: cb,
    visible: true,
  })
}
const execConfirm = () => {
  if (localModal.actionCallback) localModal.actionCallback()
}

onMounted(() => {
  fetchAddresses()
  loadProvinces()
})

const fetchAddresses = async () => {
  isLoading.value = true
  try {
    const res = await addressApi.getMyAddresses()
    addresses.value = res.data.data
  } finally {
    isLoading.value = false
  }
}

// LOAD DATA TỪ GIAO HÀNG NHANH
const loadProvinces = async () => {
  try {
    const res = await ghnApi.getProvinces()
    ghnData.provinces = res.data.data
  } catch (e) {
    console.error(e)
  }
}

const onProvinceChange = async () => {
  const selected = ghnData.provinces.find((p) => p.ProvinceID === form.provinceId)
  form.provinceName = selected?.ProvinceName || ''
  form.districtId = ''
  form.districtName = ''
  form.wardCode = ''
  form.wardName = ''
  ghnData.districts = []
  ghnData.wards = []

  if (form.provinceId) {
    const res = await ghnApi.getDistricts(form.provinceId)
    ghnData.districts = res.data.data
  }
}

const onDistrictChange = async () => {
  const selected = ghnData.districts.find((d) => d.DistrictID === form.districtId)
  form.districtName = selected?.DistrictName || ''
  form.wardCode = ''
  form.wardName = ''
  ghnData.wards = []

  if (form.districtId) {
    const res = await ghnApi.getWards(form.districtId)
    ghnData.wards = res.data.data
  }
}

const onWardChange = () => {
  const selected = ghnData.wards.find((w) => w.WardCode === form.wardCode)
  form.wardName = selected?.WardName || ''
}

// BẢN ĐỒ CHỈ ĐẨY TỌA ĐỘ VÀO FORM
const handleMapCoordinateUpdate = (locData) => {
  form.latitude = locData.lat
  form.longitude = locData.lng
}

// ĐÓNG MỞ MODAL & CRUD
const openModal = async (addr = null) => {
  if (addr) {
    isEditing.value = true
    Object.assign(form, addr)
    if (form.provinceId) {
      const res1 = await ghnApi.getDistricts(form.provinceId)
      ghnData.districts = res1.data.data
    }
    if (form.districtId) {
      const res2 = await ghnApi.getWards(form.districtId)
      ghnData.wards = res2.data.data
    }
  } else {
    isEditing.value = false
    Object.assign(form, {
      addressId: null,
      recipientName: localStorage.getItem('USERNAME') || '',
      phoneNumber: '',
      provinceId: '',
      provinceName: '',
      districtId: '',
      districtName: '',
      wardCode: '',
      wardName: '',
      detailAddress: '',
      isDefault: false,
      latitude: null,
      longitude: null,
    })
    ghnData.districts = []
    ghnData.wards = []
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const saveAddress = async () => {
  // Validate chặt chẽ
  if (
    !form.recipientName ||
    !form.phoneNumber ||
    !form.provinceId ||
    !form.districtId ||
    !form.wardCode ||
    !form.detailAddress
  ) {
    showAlert(
      'error',
      'Thiếu thông tin',
      'Vui lòng điền đầy đủ Thông tin liên hệ và Khu vực giao hàng!',
    )
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

const deleteAddress = (id) => {
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
