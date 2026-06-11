<template>
  <div class="profile-page">
    <div class="container py-5">
      <div class="row g-4">
        <div class="col-lg-4">
          <div class="sidebar-card">
            <div class="avt-ring">
              <div class="avt-inner">
                <span class="avt-letters">{{ initials }}</span>
              </div>
            </div>

            <h5 class="user-name">{{ user.fullName || 'Khách hàng' }}</h5>
            <p class="user-handle">@{{ user.username || 'user' }}</p>

            <span class="badge-silver"> <i class="fas fa-crown me-1"></i> Thành viên Bạc </span>

            <div class="stat-row">
              <div class="stat-box">
                <div class="stat-num">12</div>
                <div class="stat-lbl">Đơn hàng</div>
              </div>
              <div class="stat-box">
                <div class="stat-num">3</div>
                <div class="stat-lbl">Yêu thích</div>
              </div>
            </div>

            <hr class="divider" />

            <nav class="sidebar-nav">
              <a
                class="nav-item"
                :class="{ active: activeSection === 'section-profile' }"
                @click.prevent="scrollTo('section-profile')"
              >
                <i class="fas fa-user"></i> Hồ sơ cá nhân
              </a>
              <a
                class="nav-item"
                :class="{ active: activeSection === 'section-address' }"
                @click.prevent="scrollTo('section-address')"
              >
                <i class="fas fa-map-marker-alt"></i> Sổ địa chỉ
              </a>
              <a
                class="nav-item"
                href="#orders"
                @click.prevent="alert('Chức năng đang phát triển!')"
              >
                <i class="fas fa-box"></i> Đơn hàng của tôi
              </a>
              <a
                class="nav-item"
                href="#wishlist"
                @click.prevent="alert('Chức năng đang phát triển!')"
              >
                <i class="fas fa-heart"></i> Sản phẩm yêu thích
              </a>
            </nav>

            <hr class="divider" />

            <button class="btn-logout" @click="handleLogout">
              <i class="fas fa-sign-out-alt me-2"></i> Đăng xuất
            </button>
          </div>
        </div>

        <div class="col-lg-8 d-flex flex-column gap-4">
          <div id="section-profile" class="content-card">
            <div class="card-head">
              <div class="card-title-group">
                <span class="card-icon"><i class="fas fa-id-card"></i></span>
                <h5 class="card-title">Hồ sơ cá nhân</h5>
              </div>
            </div>

            <div v-if="isLoadingProfile" class="state-loading">
              <span class="spinner-cps"></span>
              <span>Đang tải thông tin...</span>
            </div>

            <form v-else @submit.prevent="updateProfile">
              <div class="row g-3">
                <div class="col-md-6">
                  <label class="field-lbl">Tài khoản đăng nhập</label>
                  <div class="input-wrap">
                    <input
                      type="text"
                      class="f-input f-input--locked"
                      :value="user.username"
                      disabled
                    />
                    <i class="fas fa-lock lock-icon"></i>
                  </div>
                </div>
                <div class="col-md-6">
                  <label class="field-lbl">Email liên hệ</label>
                  <div class="input-wrap">
                    <input
                      type="email"
                      class="f-input f-input--locked"
                      :value="user.email"
                      disabled
                    />
                    <i class="fas fa-lock lock-icon"></i>
                  </div>
                </div>
                <div class="col-md-6">
                  <label class="field-lbl field-lbl--req">Họ và tên</label>
                  <input
                    v-model="user.fullName"
                    type="text"
                    class="f-input"
                    placeholder="Nhập họ và tên"
                  />
                </div>
                <div class="col-md-6">
                  <label class="field-lbl field-lbl--req">Số điện thoại</label>
                  <input
                    v-model="user.phoneNumber"
                    type="tel"
                    class="f-input"
                    placeholder="Nhập số điện thoại"
                  />
                </div>
              </div>
              <div class="card-foot">
                <button type="submit" class="btn-red" :disabled="isSavingProfile">
                  <i class="fas fa-save me-2" v-if="!isSavingProfile"></i>
                  <i class="fas fa-spinner fa-spin me-2" v-else></i>
                  {{ isSavingProfile ? 'Đang lưu...' : 'Lưu thay đổi' }}
                </button>
              </div>
            </form>
          </div>

          <div id="section-address" class="content-card">
            <div class="card-head">
              <div class="card-title-group">
                <span class="card-icon"><i class="fas fa-map-marker-alt"></i></span>
                <h5 class="card-title">Sổ địa chỉ nhận hàng</h5>
              </div>
              <button class="btn-red btn-red--sm" @click="openAddressModal()">
                <i class="fas fa-plus me-1"></i> Thêm địa chỉ
              </button>
            </div>

            <div v-if="isLoadingAddresses" class="state-loading">
              <span class="spinner-cps"></span>
              <span>Đang tải địa chỉ...</span>
            </div>

            <div v-else-if="addresses.length === 0" class="state-empty">
              <i class="fas fa-map-marked-alt state-empty-icon"></i>
              <p class="state-empty-title">Chưa có địa chỉ nhận hàng</p>
              <p class="state-empty-sub">Thêm địa chỉ để đặt hàng nhanh hơn</p>
              <button class="btn-red" @click="openAddressModal()">
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
                    <span class="a-phone"
                      ><i class="fas fa-phone-alt me-1"></i>{{ addr.phoneNumber }}</span
                    >
                    <span v-if="addr.isDefault" class="a-badge-default">
                      <i class="fas fa-check-circle me-1"></i>Mặc định
                    </span>
                  </div>
                  <p class="a-detail"><i class="fas fa-home me-1"></i>{{ addr.detailAddress }}</p>
                  <p class="a-region">
                    <i class="fas fa-map-pin me-1"></i>{{ addr.wardName }}, {{ addr.districtName }},
                    {{ addr.provinceName }}
                  </p>
                </div>
                <div class="address-item__actions">
                  <div class="actions-row">
                    <button class="btn-link btn-link--blue" @click="openAddressModal(addr)">
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
                  <button
                    v-if="!addr.isDefault"
                    class="btn-outline-sm"
                    @click="setDefaultAddress(addr.addressId)"
                  >
                    Đặt mặc định
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <transition name="modal-fade">
      <div v-if="showAddressModal" class="modal-overlay" @click.self="closeAddressModal">
        <div class="modal-box">
          <div class="modal-hd">
            <div class="card-title-group">
              <span class="card-icon card-icon--sm"><i class="fas fa-map-marker-alt"></i></span>
              <h5 class="modal-title">
                {{ isEditingAddress ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới' }}
              </h5>
            </div>
            <button class="btn-close-x" @click="closeAddressModal" aria-label="Đóng">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="modal-bd custom-scrollbar">
            <form class="row g-3">
              <div class="col-12">
                <label class="field-lbl"
                  ><i class="fas fa-crosshairs me-1 text-cps"></i> Chọn vị trí trên bản đồ</label
                >
                <p class="field-hint">
                  Kéo ghim đỏ để điều chỉnh — thông tin bên dưới sẽ tự động điền.
                </p>
                <div class="map-wrap">
                  <MapLocator
                    :initialLat="addressForm.latitude"
                    :initialLng="addressForm.longitude"
                    @update:location="handleLocationUpdate"
                  />
                </div>
              </div>
              <div class="col-12"><hr class="divider-light" /></div>
              <div class="col-md-6">
                <label class="field-lbl field-lbl--req">Người nhận</label>
                <input
                  v-model.trim="addressForm.recipientName"
                  type="text"
                  class="f-input"
                  placeholder="Họ và tên người nhận"
                />
              </div>
              <div class="col-md-6">
                <label class="field-lbl field-lbl--req">Số điện thoại</label>
                <input
                  v-model.trim="addressForm.phoneNumber"
                  type="text"
                  class="f-input"
                  placeholder="09xxxxxxxxx"
                />
              </div>
              <div class="col-md-4">
                <label class="field-lbl field-lbl--req">Tỉnh / Thành phố</label>
                <input
                  v-model.trim="addressForm.provinceName"
                  type="text"
                  class="f-input"
                  placeholder="VD: Hà Nội"
                />
              </div>
              <div class="col-md-4">
                <label class="field-lbl">Quận / Huyện</label>
                <input
                  v-model.trim="addressForm.districtName"
                  type="text"
                  class="f-input"
                  placeholder="VD: Cầu Giấy"
                />
              </div>
              <div class="col-md-4">
                <label class="field-lbl">Phường / Xã</label>
                <input
                  v-model.trim="addressForm.wardName"
                  type="text"
                  class="f-input"
                  placeholder="VD: Dịch Vọng"
                />
              </div>
              <div class="col-12">
                <label class="field-lbl">Số nhà, tên đường</label>
                <input
                  v-model.trim="addressForm.detailAddress"
                  type="text"
                  class="f-input"
                  placeholder="VD: 12 Nguyễn Chí Thanh"
                />
              </div>
              <div class="col-12">
                <label class="checkbox-lbl">
                  <input
                    v-model="addressForm.isDefault"
                    type="checkbox"
                    class="chk-input"
                    id="setDefault"
                  />
                  <span class="chk-box"></span>
                  <span class="chk-text">Đặt làm địa chỉ mặc định</span>
                </label>
              </div>
            </form>
          </div>

          <div class="modal-ft">
            <button class="btn-ghost" @click="closeAddressModal">Trở lại</button>
            <button class="btn-red" @click="saveAddress" :disabled="isSavingAddress">
              <span v-if="isSavingAddress"
                ><i class="fas fa-spinner fa-spin me-2"></i>Đang lưu...</span
              >
              <span v-else><i class="fas fa-check me-2"></i>Hoàn thành</span>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import userApi from '@/api/userApi'
import addressApi from '@/api/addressApi'
import MapLocator from '@/components/common/MapLocator.vue'
import '@/assets/css/Profile.css'

const router = useRouter()

// ==========================================
// ĐIỀU HƯỚNG SIDEBAR (SCROLL MƯỢT)
// ==========================================
const activeSection = ref('section-profile')

const scrollTo = (id) => {
  activeSection.value = id
  const el = document.getElementById(id)
  if (el) {
    // Cuộn tới phần tử và trừ hao chiều cao của Header để không bị đè
    const yOffset = -90
    const y = el.getBoundingClientRect().top + window.scrollY + yOffset
    window.scrollTo({ top: y, behavior: 'smooth' })
  }
}

// ==========================================
// 1. HỒ SƠ CÁ NHÂN (Gọi từ API)
// ==========================================
const user = reactive({
  username: '',
  email: '',
  fullName: '',
  phoneNumber: '',
  roleName: '',
})
const isLoadingProfile = ref(true)
const isSavingProfile = ref(false)

const initials = computed(() =>
  (user.fullName || 'U')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join(''),
)

onMounted(() => {
  fetchProfile()
  fetchAddresses()
})

const fetchProfile = async () => {
  isLoadingProfile.value = true
  try {
    const res = await userApi.getMyProfile()
    // Gán dữ liệu trả về vào Object reactive
    if (res.data && res.data.data) {
      Object.assign(user, res.data.data)
    }
    console.log('Dữ liệu Profile trả về:', res.data.data) // Log để check data
  } catch (error) {
    console.error('Lỗi lấy hồ sơ:', error)
  } finally {
    isLoadingProfile.value = false
  }
}

const updateProfile = async () => {
  if (!user.fullName || !user.phoneNumber) {
    alert('Vui lòng điền đủ Họ tên và Số điện thoại')
    return
  }
  isSavingProfile.value = true
  try {
    const payload = {
      fullName: user.fullName,
      phoneNumber: user.phoneNumber,
    }
    await userApi.updateProfile(payload)
    alert('Cập nhật hồ sơ thành công')
    localStorage.setItem('USERNAME', user.fullName)
    window.dispatchEvent(new Event('auth-changed')) // Kích hoạt sự kiện để Header tự đổi tên
  } catch (error) {
    alert(error.response?.data?.message || 'Có lỗi xảy ra khi lưu hồ sơ')
  } finally {
    isSavingProfile.value = false
  }
}

const handleLogout = () => {
  if (!confirm('Bạn có chắc chắn muốn đăng xuất?')) return
  localStorage.removeItem('ACCESS_TOKEN')
  localStorage.removeItem('USER_ROLE')
  localStorage.removeItem('USERNAME')
  router.push('/login')
}

// ==========================================
// 2. SỔ ĐỊA CHỈ
// ==========================================
const addresses = ref([])
const isLoadingAddresses = ref(false)
const showAddressModal = ref(false)
const isEditingAddress = ref(false)
const isSavingAddress = ref(false)

const defaultAddressForm = {
  addressId: null,
  recipientName: '',
  phoneNumber: '',
  provinceName: '',
  districtName: '',
  wardName: '',
  detailAddress: '',
  note: '',
  isDefault: false,
  latitude: null,
  longitude: null,
}
const addressForm = reactive({ ...defaultAddressForm })

const fetchAddresses = async () => {
  isLoadingAddresses.value = true
  try {
    const res = await addressApi.getMyAddresses()
    if (res.data && res.data.data) {
      addresses.value = res.data.data
    }
  } catch (error) {
    console.error('Lỗi lấy sổ địa chỉ:', error)
  } finally {
    isLoadingAddresses.value = false
  }
}

const openAddressModal = (addr = null) => {
  if (addr) {
    isEditingAddress.value = true
    Object.assign(addressForm, addr)
  } else {
    isEditingAddress.value = false
    Object.assign(addressForm, { ...defaultAddressForm })
    addressForm.recipientName = user.fullName
    addressForm.phoneNumber = user.phoneNumber
  }
  showAddressModal.value = true
}

const closeAddressModal = () => {
  showAddressModal.value = false
}

const handleLocationUpdate = (locData) => {
  addressForm.latitude = locData.lat
  addressForm.longitude = locData.lng
  if (locData.province) addressForm.provinceName = locData.province
  if (locData.district) addressForm.districtName = locData.district
  if (locData.ward) addressForm.wardName = locData.ward

  if (!addressForm.detailAddress && locData.full) {
    addressForm.detailAddress = locData.full.split(',')[0]
  }
}

const saveAddress = async () => {
  if (!addressForm.recipientName || !addressForm.phoneNumber || !addressForm.provinceName) {
    alert('Vui lòng nhập đủ các trường bắt buộc (Người nhận, SĐT, Tỉnh/Thành phố)!')
    return
  }
  isSavingAddress.value = true
  try {
    if (isEditingAddress.value) {
      await addressApi.updateAddress(addressForm.addressId, addressForm)
    } else {
      await addressApi.addAddress(addressForm)
    }
    closeAddressModal()
    fetchAddresses()
  } catch (error) {
    alert(error.response?.data?.message || 'Có lỗi xảy ra khi lưu địa chỉ')
  } finally {
    isSavingAddress.value = false
  }
}

const setDefaultAddress = async (id) => {
  await addressApi.setAsDefault(id)
  fetchAddresses()
}

const deleteAddress = async (id) => {
  if (!confirm('Bạn có chắc muốn xóa địa chỉ này?')) return
  await addressApi.deleteAddress(id)
  fetchAddresses()
}
</script>
