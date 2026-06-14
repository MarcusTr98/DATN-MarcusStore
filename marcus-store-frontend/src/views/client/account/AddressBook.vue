<template>
  <div class="ab-page">
    <div class="ab-card">
      <div class="ab-card__head">
        <div class="ab-head-left">
          <div class="ab-icon-wrap">
            <i class="fas fa-map-marked-alt"></i>
          </div>
          <div>
            <h4 class="ab-card__title">Sổ địa chỉ nhận hàng</h4>
            <p class="ab-card__sub">Quản lý địa chỉ giao hàng của bạn</p>
          </div>
        </div>
        <button class="ab-btn-red" @click="openModal()">
          <i class="fas fa-plus"></i> Thêm địa chỉ
        </button>
      </div>

      <div v-if="isLoading" class="ab-state-loading">
        <span class="ab-spinner"></span>
        <span>Đang tải địa chỉ...</span>
      </div>

      <div v-else-if="addresses.length === 0" class="ab-state-empty">
        <div class="ab-empty-illus">
          <i class="fas fa-map-marked-alt"></i>
        </div>
        <p class="ab-empty-title">Chưa có địa chỉ nào</p>
        <p class="ab-empty-sub">Thêm địa chỉ để quá trình đặt hàng nhanh hơn</p>
        <button class="ab-btn-red" @click="openModal()">
          <i class="fas fa-plus"></i> Thêm địa chỉ đầu tiên
        </button>
      </div>

      <div v-else class="ab-list">
        <transition-group name="ab-list" tag="div" class="ab-list__inner">
          <div
            v-for="addr in addresses"
            :key="addr.addressId"
            class="ab-item"
            :class="{ 'ab-item--default': addr.isDefault }"
          >
            <div class="ab-item__left">
              <div class="ab-row1">
                <span class="ab-name">{{ addr.recipientName }}</span>
                <span class="ab-sep">·</span>
                <span class="ab-phone">
                  <i class="fas fa-phone-alt"></i> {{ addr.phoneNumber }}
                </span>
              </div>
              <p class="ab-detail"><i class="fas fa-home"></i> {{ addr.detailAddress }}</p>
              <p class="ab-region">
                <i class="fas fa-map-pin"></i>
                {{ addr.wardName }}, {{ addr.districtName }}, {{ addr.provinceName }}
              </p>
              <p v-if="addr.latitude && addr.longitude" class="ab-coords">
                <i class="fas fa-crosshairs"></i>
                {{ addr.latitude.toFixed(5) }}, {{ addr.longitude.toFixed(5) }}
              </p>
            </div>

            <div class="ab-item__right">
              <div class="ab-badge-wrap">
                <span v-if="addr.isDefault" class="ab-badge-default">
                  <i class="fas fa-check-circle"></i> Mặc định
                </span>
              </div>

              <div class="ab-actions-group">
                <button class="ab-action-btn ab-action-btn--edit" @click="openModal(addr)">
                  <i class="fas fa-pen"></i> Sửa
                </button>
                <button
                  v-if="!addr.isDefault"
                  class="ab-action-btn ab-action-btn--delete"
                  @click="deleteAddress(addr.addressId)"
                >
                  <i class="fas fa-trash"></i> Xóa
                </button>
                <button
                  v-if="!addr.isDefault"
                  class="ab-action-btn ab-action-btn--setdefault"
                  @click="setDefault(addr.addressId)"
                >
                  Đặt mặc định
                </button>
              </div>
            </div>
          </div>
        </transition-group>
      </div>
    </div>

    <transition name="ab-modal-fade">
      <div v-if="showModal" class="ab-modal-overlay" @click.self="closeModal">
        <div class="ab-modal-box">
          <div class="ab-modal-hd">
            <div class="ab-head-left">
              <div class="ab-icon-wrap ab-icon-wrap--sm">
                <i class="fas fa-map-marker-alt"></i>
              </div>
              <div>
                <h5 class="ab-modal-title">
                  {{ isEditing ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới' }}
                </h5>
                <p class="ab-modal-sub">
                  {{
                    isEditing ? 'Chỉnh sửa thông tin địa chỉ' : 'Điền đầy đủ để giao hàng chính xác'
                  }}
                </p>
              </div>
            </div>
            <button class="ab-btn-close" @click="closeModal" aria-label="Đóng modal">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="ab-modal-bd custom-scrollbar">
            <div class="ab-form-grid">
              <div class="ab-section-hd">
                <span class="ab-section-num">1</span>
                <div>
                  <p class="ab-section-title">Thông tin liên hệ</p>
                  <p class="ab-section-sub">Người nhận sẽ được liên hệ khi giao hàng</p>
                </div>
              </div>

              <div class="ab-field ab-field--half">
                <label class="ab-lbl ab-lbl--req">Họ và tên người nhận</label>
                <div class="ab-input-wrap">
                  <span class="ab-prefix"><i class="fas fa-user"></i></span>
                  <input
                    v-model.trim="form.recipientName"
                    type="text"
                    class="ab-input"
                    placeholder="Nguyễn Văn A"
                    autocomplete="name"
                  />
                </div>
              </div>

              <div class="ab-field ab-field--half">
                <label class="ab-lbl ab-lbl--req">Số điện thoại</label>
                <div class="ab-input-wrap">
                  <span class="ab-prefix"><i class="fas fa-phone-alt"></i></span>
                  <input
                    v-model.trim="form.phoneNumber"
                    type="tel"
                    class="ab-input"
                    placeholder="0912 345 678"
                    autocomplete="tel"
                  />
                </div>
              </div>

              <div class="ab-section-hd ab-section-hd--mt">
                <span class="ab-section-num">2</span>
                <div>
                  <p class="ab-section-title">Khu vực giao hàng</p>
                  <p class="ab-section-sub">Dữ liệu từ Giao Hàng Nhanh — dùng để tính phí ship</p>
                </div>
              </div>

              <div class="ab-field ab-field--third">
                <label class="ab-lbl ab-lbl--req">Tỉnh / Thành phố</label>
                <div class="ab-select-wrap">
                  <select
                    v-model="form.provinceId"
                    @change="onProvinceChange"
                    class="ab-select"
                    :disabled="ghnLoading.provinces"
                  >
                    <option value="" disabled>
                      {{ ghnLoading.provinces ? 'Đang tải...' : '-- Chọn Tỉnh/TP --' }}
                    </option>
                    <option
                      v-for="p in ghnData.provinces"
                      :key="p.ProvinceID"
                      :value="p.ProvinceID"
                    >
                      {{ p.ProvinceName }}
                    </option>
                  </select>
                  <i class="fas fa-chevron-down ab-select-arrow"></i>
                </div>
              </div>

              <div class="ab-field ab-field--third">
                <label class="ab-lbl ab-lbl--req">Quận / Huyện</label>
                <div class="ab-select-wrap">
                  <select
                    v-model="form.districtId"
                    @change="onDistrictChange"
                    class="ab-select"
                    :disabled="!form.provinceId || ghnLoading.districts"
                  >
                    <option value="" disabled>
                      {{ ghnLoading.districts ? 'Đang tải...' : '-- Chọn Quận/Huyện --' }}
                    </option>
                    <option
                      v-for="d in ghnData.districts"
                      :key="d.DistrictID"
                      :value="d.DistrictID"
                    >
                      {{ d.DistrictName }}
                    </option>
                  </select>
                  <i class="fas fa-chevron-down ab-select-arrow"></i>
                </div>
              </div>

              <div class="ab-field ab-field--third">
                <label class="ab-lbl ab-lbl--req">Phường / Xã</label>
                <div class="ab-select-wrap">
                  <select
                    v-model="form.wardCode"
                    @change="onWardChange"
                    class="ab-select"
                    :disabled="!form.districtId || ghnLoading.wards"
                  >
                    <option value="" disabled>
                      {{ ghnLoading.wards ? 'Đang tải...' : '-- Chọn Phường/Xã --' }}
                    </option>
                    <option v-for="w in ghnData.wards" :key="w.WardCode" :value="w.WardCode">
                      {{ w.WardName }}
                    </option>
                  </select>
                  <i class="fas fa-chevron-down ab-select-arrow"></i>
                </div>
              </div>

              <div class="ab-field ab-field--full">
                <label class="ab-lbl ab-lbl--req">Số nhà, Tên đường</label>
                <div class="ab-input-wrap">
                  <span class="ab-prefix"><i class="fas fa-road"></i></span>
                  <input
                    v-model.trim="form.detailAddress"
                    type="text"
                    class="ab-input"
                    placeholder="Ví dụ: Số 12, ngõ 34 đường Bạch Đằng..."
                  />
                </div>
              </div>

              <div class="ab-section-hd ab-section-hd--mt">
                <span class="ab-section-num">3</span>
                <div>
                  <p class="ab-section-title">
                    Định vị tọa độ <span class="ab-badge-optional">Tùy chọn</span>
                  </p>
                  <p class="ab-section-sub">
                    Chọn Phường/Xã — bản đồ tự bay về khu vực đó. Kéo ghim để chính xác hơn.
                  </p>
                </div>
              </div>

              <div class="ab-field ab-field--full">
                <MapLocator
                  ref="mapRef"
                  :initialLat="form.latitude"
                  :initialLng="form.longitude"
                  @update:location="handleMapCoordinateUpdate"
                />
              </div>

              <div class="ab-field ab-field--full">
                <label class="ab-checkbox">
                  <input v-model="form.isDefault" type="checkbox" class="ab-chk-native" />
                  <span class="ab-chk-box"></span>
                  <span class="ab-chk-label">Đặt làm địa chỉ mặc định</span>
                </label>
              </div>
            </div>
          </div>

          <div class="ab-modal-ft">
            <button class="ab-btn-ghost" @click="closeModal">
              <i class="fas fa-arrow-left"></i> Trở lại
            </button>
            <button class="ab-btn-red" @click="saveAddress" :disabled="isSaving">
              <i :class="isSaving ? 'fas fa-spinner fa-spin' : 'fas fa-check'"></i>
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

const mapRef = ref(null)

const ghnData = reactive({ provinces: [], districts: [], wards: [] })
const ghnLoading = reactive({ provinces: false, districts: false, wards: false })

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
const showAlert = (type, title, msg) =>
  Object.assign(localModal, { type, title, message: msg, actionCallback: null, visible: true })
const showConfirm = (title, msg, cb) =>
  Object.assign(localModal, {
    type: 'confirm',
    title,
    message: msg,
    actionCallback: cb,
    visible: true,
  })
const execConfirm = () => localModal.actionCallback?.()

onMounted(() => {
  fetchAddresses()
  loadProvinces()
})

const fetchAddresses = async () => {
  isLoading.value = true
  try {
    const res = await addressApi.getMyAddresses()
    addresses.value = res.data.data
  } catch (err) {
    showAlert(
      'error',
      'Lỗi tải dữ liệu',
      err.response?.data?.message || 'Không thể tải danh sách địa chỉ.',
    )
  } finally {
    isLoading.value = false
  }
}

const loadProvinces = async () => {
  ghnLoading.provinces = true
  try {
    const res = await ghnApi.getProvinces()
    ghnData.provinces = res.data.data
  } catch (err) {
    console.error('[AddressBook] Không load được tỉnh GHN:', err)
  } finally {
    ghnLoading.provinces = false
  }
}

const onProvinceChange = async () => {
  const selected = ghnData.provinces.find((p) => p.ProvinceID === form.provinceId)
  form.provinceName = selected?.ProvinceName ?? ''

  // Reset cấp dưới
  Object.assign(form, { districtId: '', districtName: '', wardCode: '', wardName: '' })
  ghnData.districts = []
  ghnData.wards = []

  // Kích hoạt bay map tỉnh
  syncMapPosition()

  if (!form.provinceId) return
  ghnLoading.districts = true
  try {
    const res = await ghnApi.getDistricts(form.provinceId)
    ghnData.districts = res.data.data
  } catch (err) {
    console.error('[AddressBook] Lỗi:', err)
  } finally {
    ghnLoading.districts = false
  }
}

const onDistrictChange = async () => {
  const selected = ghnData.districts.find((d) => d.DistrictID === form.districtId)
  form.districtName = selected?.DistrictName ?? ''

  // Reset cấp dưới
  Object.assign(form, { wardCode: '', wardName: '' })
  ghnData.wards = []

  // Kích hoạt bay map quận/huyện
  syncMapPosition()

  if (!form.districtId) return
  ghnLoading.wards = true
  try {
    const res = await ghnApi.getWards(form.districtId)
    ghnData.wards = res.data.data
  } catch (err) {
    console.error('[AddressBook] Lỗi:', err)
  } finally {
    ghnLoading.wards = false
  }
}

const onWardChange = () => {
  const selected = ghnData.wards.find((w) => w.WardCode === form.wardCode)
  form.wardName = selected?.WardName ?? ''

  // Kích hoạt bay map phường/xã
  syncMapPosition()
}

const syncMapPosition = async () => {
  if (!mapRef.value || !form.provinceName) return

  // 1. Nếu đã chọn đủ 3 cấp => Zoom 15 - phường
  if (form.wardName && form.districtName && form.provinceName) {
    const success = await mapRef.value.flyToAddress(
      `${form.wardName}, ${form.districtName}, ${form.provinceName}, Việt Nam`,
      15,
    )
    if (success) return // Nếu bay thành công thì dừng
  }

  // 2. Fallback 1: Nếu Phường tìm không ra, hoặc đang ở bước chọn Quận => Zoom 13 - quận/ huyện
  if (form.districtName && form.provinceName) {
    const success = await mapRef.value.flyToAddress(
      `${form.districtName}, ${form.provinceName}, Việt Nam`,
      13,
    )
    if (success) return // Nếu bay thành công thì dừng
  }

  // 3. Fallback 2: Nếu Quận cũng không ra, hoặc mới chọn ở bước Tỉnh => Zoom 11 - thành phố/ tỉnh
  if (form.provinceName) {
    await mapRef.value.flyToAddress(`${form.provinceName}, Việt Nam`, 11)
  }
}
const openModal = async (addr = null) => {
  if (addr) {
    isEditing.value = true
    Object.assign(form, addr)
    await Promise.all([
      form.provinceId
        ? ghnApi.getDistricts(form.provinceId).then((r) => {
            ghnData.districts = r.data.data
          })
        : Promise.resolve(),
      form.districtId
        ? ghnApi.getWards(form.districtId).then((r) => {
            ghnData.wards = r.data.data
          })
        : Promise.resolve(),
    ])
    showModal.value = true
    setTimeout(syncMapPosition, 500)
  } else {
    isEditing.value = false
    Object.assign(form, {
      addressId: null,
      recipientName: localStorage.getItem('USERNAME') ?? '',
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
    showModal.value = true
  }
}

const closeModal = () => {
  showModal.value = false
}

const handleMapCoordinateUpdate = ({ lat, lng }) => {
  form.latitude = lat
  form.longitude = lng
}

const saveAddress = async () => {
  // 1. Validate bắt buộc ở Frontend
  const missing =
    !form.recipientName ||
    !form.phoneNumber ||
    !form.provinceId ||
    !form.districtId ||
    !form.wardCode ||
    !form.detailAddress

  if (missing) {
    showAlert(
      'error',
      'Thiếu thông tin',
      'Vui lòng điền đầy đủ Thông tin liên hệ và Khu vực giao hàng!',
    )
    return
  }

  isSaving.value = true
  try {
    if (isEditing.value) {
      await addressApi.updateAddress(form.addressId, form)
    } else {
      await addressApi.addAddress(form)
    }
    closeModal()
    await fetchAddresses()
    showAlert('success', 'Thành công', 'Đã lưu địa chỉ thành công.')
  } catch (err) {
    //lấy từ GlobalExceptionHandler
    const resData = err.response?.data
    let errorMsg = resData?.message || 'Có lỗi xảy ra, vui lòng thử lại.'

    if (resData?.data && typeof resData.data === 'object' && Object.keys(resData.data).length > 0) {
      const detailErrors = Object.values(resData.data).join('; ')
      errorMsg = `Lỗi: ${detailErrors}`
    }

    showAlert('error', 'Lỗi nhập liệu', errorMsg)
  } finally {
    isSaving.value = false
  }
}

const setDefault = async (id) => {
  try {
    await addressApi.setAsDefault(id)
    await fetchAddresses()
  } catch (err) {
    showAlert('error', 'Lỗi', err.response?.data?.message || 'Không thể đặt mặc định.')
  }
}

const deleteAddress = (id) => {
  showConfirm('Xác nhận xóa', 'Bạn có chắc chắn muốn xóa địa chỉ này không?', async () => {
    try {
      await addressApi.deleteAddress(id)
      showAlert('success', 'Đã xóa', 'Địa chỉ đã được xóa thành công.')
      await fetchAddresses()
    } catch (err) {
      showAlert('error', 'Lỗi', err.response?.data?.message || 'Không thể xóa địa chỉ này.')
    }
  })
}
</script>

<style scoped src="@/assets/css/AddressBook.css"></style>
