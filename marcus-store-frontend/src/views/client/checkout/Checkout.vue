<template>
  <div class="checkout-page pt-4">
    <BaseModal v-if="modal.show" :title="modal.title" @close="modal.show = false">
      <div class="p-3 text-center">
        <p class="mb-4" style="font-size: 15px; color: #374151">{{ modal.message }}</p>
        <button class="btn btn-danger px-4 py-2 rounded-pill" @click="handleModalConfirm">
          Đồng ý
        </button>
      </div>
    </BaseModal>

    <div class="checkout-header">
      <div class="checkout-header__inner">
        <router-link to="/" class="checkout-header__brand">
          <i class="fas fa-shopping-bag me-2"></i> Marcus Store
        </router-link>
        <div class="checkout-header__steps">
          <div class="step step--done">
            <span class="step__dot"><i class="fas fa-check"></i></span>
            <span class="step__label">Giỏ hàng</span>
          </div>
          <div class="step__line step__line--done"></div>
          <div class="step step--active">
            <span class="step__dot">2</span><span class="step__label">Thanh toán</span>
          </div>
          <div class="step__line"></div>
          <div class="step">
            <span class="step__dot">3</span><span class="step__label">Xác nhận</span>
          </div>
        </div>
        <div class="checkout-header__secure">
          <i class="fas fa-shield-alt"></i><span>Thanh toán bảo mật SSL</span>
        </div>
      </div>
    </div>

    <div class="checkout-body">
      <div class="checkout-left">
        <div class="checkout-card mb-3" v-if="savedAddresses.length > 0">
          <div class="checkout-card__title">
            <span class="checkout-card__icon"><i class="fas fa-address-book"></i></span>
            Sổ địa chỉ nhận hàng
            <span class="address-count-badge">{{ savedAddresses.length }}</span>
          </div>

          <div
            class="saved-address-list"
            style="
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
              gap: 12px;
            "
          >
            <button
              v-for="addr in savedAddresses"
              :key="addr.addressId"
              type="button"
              class="saved-address-chip"
              :class="{ 'saved-address-chip--active': activeAddressId === addr.addressId }"
              @click="applySavedAddress(addr)"
            >
              <span class="saved-address-chip__icon"><i class="fas fa-map-marker-alt"></i></span>
              <span class="saved-address-chip__body">
                <strong>{{ addr.recipientName }}</strong> · {{ addr.phoneNumber }}
                <small>{{ addr.districtName }}, {{ addr.provinceName }}</small>
              </span>
              <span class="saved-address-chip__default" v-if="addr.isDefault">
                <i class="fas fa-star"></i>
              </span>
            </button>
          </div>
        </div>

        <div class="checkout-card">
          <div class="checkout-card__title">
            <span class="checkout-card__icon"><i class="fas fa-map-marker-alt"></i></span>
            Địa chỉ giao hàng
          </div>

          <form @submit.prevent="handleCheckout" id="checkoutForm">
            <div class="form-row">
              <div class="form-group form-group--full">
                <label class="form-label">Họ và tên người nhận <span class="req">*</span></label>
                <div class="input-icon-wrap">
                  <i class="fas fa-user input-icon"></i>
                  <input
                    v-model="orderForm.recipientName"
                    type="text"
                    class="form-input form-input--icon"
                    required
                  />
                </div>
              </div>
            </div>

            <div class="form-row form-row--2col">
              <div class="form-group">
                <label class="form-label">Số điện thoại <span class="req">*</span></label>
                <div class="input-prefix">
                  <span class="input-prefix__badge">🇻🇳 +84</span>
                  <input
                    v-model="orderForm.recipientPhone"
                    type="tel"
                    class="form-input form-input--prefixed"
                    required
                  />
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">Email <span class="req">*</span></label>
                <div class="input-icon-wrap">
                  <i class="fas fa-envelope input-icon"></i>
                  <input
                    v-model="orderForm.email"
                    type="email"
                    class="form-input form-input--icon"
                    required
                  />
                </div>
              </div>
            </div>

            <div class="address-display-box" v-if="selectedAddress">
              <div class="address-display-box__row">
                <i class="fas fa-map-pin text-danger me-2" style="margin-top: 3px"></i>
                <span>
                  <strong>Giao đến:</strong> {{ selectedAddress.detailAddress }},
                  {{ selectedAddress.wardName }}, {{ selectedAddress.districtName }},
                  {{ selectedAddress.provinceName }}
                </span>
              </div>
              <div class="address-display-box__actions">
                <button type="button" class="btn-change-addr" @click="clearSelectedAddress">
                  <i class="fas fa-pencil-alt me-1"></i> Nhập địa chỉ khác
                </button>
              </div>
            </div>

            <template v-else>
              <div class="form-row form-row--3col">
                <div class="form-group">
                  <label class="form-label">Tỉnh / Thành phố <span class="req">*</span></label>
                  <div class="select-wrapper">
                    <select
                      class="form-select"
                      v-model="manualProvinceId"
                      @change="onManualProvinceChange"
                      required
                    >
                      <option :value="null" disabled>-- Chọn Tỉnh/Thành --</option>
                      <option v-for="p in ghnProvinces" :key="p.ProvinceID" :value="p.ProvinceID">
                        {{ p.ProvinceName }}
                      </option>
                    </select>
                    <i class="fas fa-chevron-down select-arrow"></i>
                  </div>
                </div>

                <div class="form-group">
                  <label class="form-label">Quận / Huyện <span class="req">*</span></label>
                  <div class="select-wrapper">
                    <select
                      class="form-select"
                      v-model="manualDistrictId"
                      @change="onManualDistrictChange"
                      :disabled="!manualProvinceId"
                      required
                    >
                      <option :value="null" disabled>-- Chọn Quận/Huyện --</option>
                      <option v-for="d in ghnDistricts" :key="d.DistrictID" :value="d.DistrictID">
                        {{ d.DistrictName }}
                      </option>
                    </select>
                    <i class="fas fa-chevron-down select-arrow"></i>
                  </div>
                </div>

                <div class="form-group">
                  <label class="form-label">Phường / Xã <span class="req">*</span></label>
                  <div class="select-wrapper">
                    <select
                      class="form-select"
                      v-model="manualWardCode"
                      @change="onManualWardChange"
                      :disabled="!manualDistrictId"
                      required
                    >
                      <option value="" disabled>-- Chọn Phường/Xã --</option>
                      <option v-for="w in ghnWards" :key="w.WardCode" :value="w.WardCode">
                        {{ w.WardName }}
                      </option>
                    </select>
                    <i class="fas fa-chevron-down select-arrow"></i>
                  </div>
                </div>
              </div>

              <div class="form-row">
                <div class="form-group form-group--full">
                  <label class="form-label">Số nhà, tên đường <span class="req">*</span></label>
                  <div class="input-icon-wrap">
                    <i class="fas fa-home input-icon"></i>
                    <input
                      v-model="detailAddress"
                      type="text"
                      class="form-input form-input--icon"
                      placeholder="VD: 118 Đường Cát Bi..."
                      required
                    />
                  </div>
                </div>
              </div>
            </template>

            <div class="form-row">
              <div class="form-group form-group--full">
                <label class="form-label"
                  >Ghi chú cho shipper <span class="optional">(Tùy chọn)</span></label
                >
                <textarea v-model="orderForm.note" class="form-textarea" rows="2"></textarea>
              </div>
            </div>
          </form>
        </div>

        <div class="checkout-card mt-3">
          <div class="checkout-card__title">
            <span class="checkout-card__icon"><i class="fas fa-truck"></i></span>
            Phương thức vận chuyển
          </div>

          <div class="payment-options">
            <label class="payment-option payment-option--active">
              <input type="radio" checked class="payment-option__radio" />
              <div
                class="payment-option__icon"
                style="background: transparent; width: auto; padding: 0 10px"
              >
                <i class="fas fa-shipping-fast text-danger" style="font-size: 28px"></i>
              </div>
              <div class="payment-option__body">
                <span class="shipping-option__name">Giao Hàng Nhanh (GHN)</span>
                <span class="payment-option__desc text-success" v-if="estimatedDelivery"
                  ><i class="fas fa-calendar-check me-1"></i>{{ estimatedDelivery }}</span
                >
                <span class="payment-option__desc" v-else>Giao hàng tận nơi toàn quốc</span>
              </div>

              <div style="font-size: 15px; font-weight: 800; color: #d92d20; padding-right: 15px">
                <i class="fas fa-spinner fa-spin text-muted" v-if="isFeeLoading"></i>
                <template v-else-if="shippingFee > 0"
                  >+{{ shippingFee.toLocaleString('vi-VN') }}₫</template
                >
                <template v-else-if="shippingFee === 0 && !isFeeLoading && toWardCode"
                  >Miễn phí</template
                >
                <span class="text-muted" style="font-size: 12px; font-weight: 500" v-else
                  >Chưa xác định</span
                >
              </div>

              <div class="payment-option__check"><i class="fas fa-check-circle"></i></div>
            </label>
          </div>
        </div>

        <div class="checkout-card mt-3">
          <div class="checkout-card__title">
            <span class="checkout-card__icon"><i class="fas fa-wallet"></i></span>
            Phương thức thanh toán
          </div>

          <div class="payment-options">
            <label
              class="payment-option"
              :class="{ 'payment-option--active': orderForm.paymentMethod === 'COD' }"
            >
              <input
                type="radio"
                v-model="orderForm.paymentMethod"
                value="COD"
                class="payment-option__radio"
              />
              <div class="payment-option__icon payment-option__icon--cod">
                <i class="fas fa-hand-holding-usd"></i>
              </div>
              <div class="payment-option__body">
                <span class="payment-option__name">Thanh toán khi nhận hàng (COD)</span>
                <span class="payment-option__desc">Trả tiền mặt khi shipper giao đến tay bạn</span>
              </div>
              <div class="payment-option__check"><i class="fas fa-check-circle"></i></div>
            </label>

            <label
              class="payment-option flex-column align-items-stretch"
              :class="{ 'payment-option--active': orderForm.paymentMethod === 'BANKING' }"
            >
              <div class="d-flex align-items-center gap-3 w-100">
                <input
                  type="radio"
                  v-model="orderForm.paymentMethod"
                  value="BANKING"
                  class="payment-option__radio"
                />
                <div class="payment-option__icon payment-option__icon--qr">
                  <i class="fas fa-qrcode"></i>
                </div>
                <div class="payment-option__body">
                  <span class="payment-option__name">Chuyển khoản mã QR</span>
                  <span class="payment-option__desc"
                    >Quét mã QR bằng ngân hàng (Tự động duyệt)</span
                  >
                </div>
                <div class="payment-option__check"><i class="fas fa-check-circle"></i></div>
              </div>
              <div
                class="qr-code-box mt-3 pt-3 border-top"
                v-if="orderForm.paymentMethod === 'BANKING'"
              >
                <div class="qr-bank-info">
                  <img
                    :src="`https://img.vietqr.io/image/mbbank-0901234567-compact2.png?amount=${finalAmount}&addInfo=DH${orderForm.recipientPhone}&accountName=MARCUS%20TRAN`"
                    class="qr-bank-info__img"
                  />
                  <div class="qr-bank-info__details">
                    <div class="qr-bank-info__bank">
                      <i class="fas fa-university me-1"></i>MB Bank
                    </div>
                    <div>STK: <strong>0901234567</strong></div>
                    <div>Chủ TK: <strong>MARCUS TRAN</strong></div>
                    <div class="qr-bank-info__amount">
                      {{ finalAmount.toLocaleString('vi-VN') }}₫
                    </div>
                    <div class="qr-bank-info__note">
                      ND: <strong>DH{{ orderForm.recipientPhone || 'SDT' }}</strong>
                    </div>
                  </div>
                </div>
              </div>
            </label>

            <label
              class="payment-option"
              :class="{ 'payment-option--active': orderForm.paymentMethod === 'VNPAY' }"
            >
              <input
                type="radio"
                v-model="orderForm.paymentMethod"
                value="VNPAY"
                class="payment-option__radio"
              />
              <div class="payment-option__icon payment-option__icon--vnpay">
                <i class="fas fa-credit-card"></i>
              </div>
              <div class="payment-option__body">
                <span class="payment-option__name">Ví điện tử / Thẻ quốc tế (VNPAY)</span>
                <span class="payment-option__desc">Thanh toán qua cổng VNPAY an toàn</span>
              </div>
              <div class="payment-option__check"><i class="fas fa-check-circle"></i></div>
            </label>
          </div>
        </div>
      </div>

      <div class="checkout-right">
        <div class="order-summary">
          <div class="order-summary__header">
            <span>Đơn hàng của bạn</span>
            <span class="order-summary__badge">{{ cartData.totalQuantity }} sản phẩm</span>
          </div>

          <div class="order-items">
            <div v-for="item in cartData.items" :key="item.cartItemId" class="order-item">
              <div class="order-item__img-wrap">
                <img :src="item.imageUrl" :alt="item.productName" class="order-item__img" />
                <span class="order-item__qty">{{ item.quantity }}</span>
              </div>
              <div class="order-item__info">
                <div class="order-item__name">{{ item.productName }}</div>
                <div class="order-item__variant" v-if="item.variantName">
                  {{ item.variantName }}
                </div>
                <div class="order-item__sku">SKU: {{ item.skuCode }}</div>
              </div>
              <div class="order-item__price">{{ item.totalPrice?.toLocaleString('vi-VN') }}₫</div>
            </div>
          </div>

          <div class="order-totals mt-3">
            <div class="order-totals__row">
              <span>Tạm tính</span>
              <span>{{ cartData.totalAmount?.toLocaleString('vi-VN') }}₫</span>
            </div>
            <div class="order-totals__row order-totals__row--discount" v-if="discountAmount > 0">
              <span><i class="fas fa-tag me-1"></i>Giảm giá Voucher</span>
              <span>-{{ discountAmount.toLocaleString('vi-VN') }}₫</span>
            </div>
            <div class="order-totals__row">
              <span>Phí vận chuyển (GHN)</span>
              <span v-if="isFeeLoading" class="text-muted" style="font-size: 12px"
                ><i class="fas fa-spinner fa-spin"></i
              ></span>
              <span v-else-if="shippingFee > 0" class="text-danger"
                >+{{ shippingFee.toLocaleString('vi-VN') }}₫</span
              >
              <span
                v-else-if="shippingFee === 0 && !isFeeLoading && toWardCode"
                class="text-success"
                >Miễn phí</span
              >
              <span v-else class="text-muted" style="font-size: 12px">Chưa xác định</span>
            </div>
          </div>

          <div class="order-final">
            <span class="order-final__label">Tổng thanh toán</span>
            <span class="order-final__amount"
              >{{ finalAmount.toLocaleString('vi-VN') }}<sup>₫</sup></span
            >
          </div>

          <button
            form="checkoutForm"
            type="submit"
            class="btn-checkout"
            :disabled="isProcessing || !cartData.items?.length || isFeeLoading"
          >
            <span v-if="!isProcessing"><i class="fas fa-lock me-2"></i>Đặt hàng ngay</span>
            <span v-else><i class="fas fa-spinner fa-spin me-2"></i>Đang xử lý...</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BaseModal from '@/components/BaseModal.vue'
import '@/assets/css/check-out.css'

import addressApi from '@/api/addressApi'
import userApi from '@/api/userApi'
import cartApi from '@/api/cartApi'
import ghnApi from '@/api/ghnApi'
import api from '@/utils/api'

const router = useRouter()
const isProcessing = ref(false)
const isFeeLoading = ref(false)
const feeError = ref('')

const modal = ref({ show: false, title: 'Thông báo', message: '', action: null })

const showModal = (title, message, action = null) => {
  modal.value = { show: true, title, message, action }
}

const handleModalConfirm = () => {
  modal.value.show = false
  if (modal.value.action === 'redirect_cart') router.push('/cart')
  if (modal.value.action === 'redirect_login') router.push('/auth/login')
  // Đã gỡ bỏ hành động redirect_success vì giờ ta redirect tự động
}

// ─── Order & Cart Data
const cartData = ref({ items: [], totalQuantity: 0, totalAmount: 0 })
const appliedVoucherCode = ref('')
const discountAmount = ref(0)
const shippingFee = ref(0)
const estimatedDelivery = ref('')

const orderForm = ref({
  recipientName: '',
  recipientPhone: '',
  email: '',
  paymentMethod: 'COD',
  note: '',
})

// ─── GHN
const toDistrictId = ref(null)
const toWardCode = ref('')

const ghnProvinces = ref([])
const ghnDistricts = ref([])
const ghnWards = ref([])
const manualProvinceId = ref(null)
const manualDistrictId = ref(null)
const manualWardCode = ref('')
const detailAddress = ref('')

// ─── Saved Addresses
const savedAddresses = ref([])
const selectedAddress = ref(null)
const activeAddressId = ref(null)

// ─── Computed
const finalAmount = computed(() => {
  const total = (cartData.value.totalAmount ?? 0) - discountAmount.value + shippingFee.value
  return total > 0 ? total : 0
})

const isAddressReady = computed(() => !!toDistrictId.value && !!toWardCode.value)

// ─── Utils
const formatDeliveryDate = (isoDate) => {
  if (!isoDate) return ''
  try {
    const d = new Date(isoDate)
    return `Dự kiến giao ${d.toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'numeric' })}`
  } catch (e) {
    console.warn('Lỗi format date:', e)
    return ''
  }
}

const validatePhone = (phone) => /(03|05|07|08|09)\d{8}/.test(phone)

//1. Phí vận chuyển GHN
const calculateShippingFee = async () => {
  if (!toDistrictId.value || !toWardCode.value) {
    shippingFee.value = 0
    estimatedDelivery.value = ''
    return
  }

  isFeeLoading.value = true
  feeError.value = ''

  try {
    const res = await api.post('/checkout/calculate-fee', {
      toDistrictId: toDistrictId.value,
      toWardCode: toWardCode.value,
    })

    const feeValue = res.data?.data
    if (typeof feeValue === 'number' && feeValue >= 0) {
      shippingFee.value = feeValue
      estimatedDelivery.value = formatDeliveryDate(res.data?.expectedDelivery ?? null)
    } else {
      throw new Error('Phí ship không hợp lệ')
    }
  } catch (error) {
    console.error('Lỗi tính phí:', error)
    shippingFee.value = 0
    feeError.value = 'Không thể tính phí vận chuyển. Vui lòng kiểm tra địa chỉ hoặc thử lại!'
  } finally {
    isFeeLoading.value = false
  }
}

//2. Sổ địa chỉ
const fetchMyAddresses = async () => {
  try {
    const res = await addressApi.getMyAddresses()
    savedAddresses.value = (res.data?.data ?? []).slice(0, 4)

    const defaultAddr = savedAddresses.value.find((a) => a.isDefault) ?? savedAddresses.value[0]
    if (defaultAddr) await applySavedAddress(defaultAddr)
  } catch (error) {
    console.error('Lỗi tải sổ địa chỉ:', error)
  }
}

const applySavedAddress = async (addr) => {
  activeAddressId.value = addr.addressId
  selectedAddress.value = addr
  orderForm.value.recipientName = addr.recipientName
  orderForm.value.recipientPhone = addr.phoneNumber
  toDistrictId.value = addr.districtId
  toWardCode.value = addr.wardCode
  await calculateShippingFee()
}

const clearSelectedAddress = () => {
  selectedAddress.value = null
  activeAddressId.value = null
  toDistrictId.value = null
  toWardCode.value = ''
  shippingFee.value = 0
  estimatedDelivery.value = ''
  manualProvinceId.value = null
  manualDistrictId.value = null
  manualWardCode.value = ''
  detailAddress.value = ''
}

//3. Dropdown GHN
const fetchGhnProvinces = async () => {
  try {
    const res = await ghnApi.getProvinces()
    ghnProvinces.value = res.data?.data ?? []
  } catch (error) {
    console.error('Lỗi tải Tỉnh GHN:', error)
  }
}

const onManualProvinceChange = async () => {
  manualDistrictId.value = null
  manualWardCode.value = ''
  toDistrictId.value = null
  toWardCode.value = ''
  shippingFee.value = 0
  estimatedDelivery.value = ''
  ghnDistricts.value = []
  ghnWards.value = []
  if (!manualProvinceId.value) return

  try {
    const res = await ghnApi.getDistricts(manualProvinceId.value)
    ghnDistricts.value = res.data?.data ?? []
  } catch (error) {
    console.error('Lỗi tải Quận GHN:', error)
  }
}

const onManualDistrictChange = async () => {
  manualWardCode.value = ''
  toWardCode.value = ''
  toDistrictId.value = manualDistrictId.value ?? null
  shippingFee.value = 0
  estimatedDelivery.value = ''
  ghnWards.value = []
  if (!manualDistrictId.value) return

  try {
    const res = await ghnApi.getWards(manualDistrictId.value)
    ghnWards.value = res.data?.data ?? []
  } catch (error) {
    console.error('Lỗi tải Phường GHN:', error)
  }
}

const onManualWardChange = async () => {
  toWardCode.value = manualWardCode.value
  await calculateShippingFee()
}

//4. API phụ trợ
const fetchCart = async () => {
  try {
    const res = await cartApi.getCart()
    const data = res.data
    cartData.value = data?.data ?? data
    if (!cartData.value.items?.length) showModal('Lỗi', 'Giỏ hàng trống!', 'redirect_cart')
  } catch (error) {
    if (error.response?.status === 401)
      showModal('Cảnh báo', 'Vui lòng đăng nhập để tiếp tục.', 'redirect_login')
  }
}

const prefillUserEmail = async () => {
  try {
    const res = await userApi.getMyProfile()
    orderForm.value.email = res.data?.data?.email ?? ''
  } catch (error) {
    console.warn('Không thể pre-fill email:', error)
  }
}

//5. Submit
const buildShippingAddress = () => {
  if (selectedAddress.value) {
    const a = selectedAddress.value
    return `${a.detailAddress}, ${a.wardName}, ${a.districtName}, ${a.provinceName}`
  }
  const wardName = ghnWards.value.find((w) => w.WardCode === manualWardCode.value)?.WardName ?? ''
  const districtName =
    ghnDistricts.value.find((d) => d.DistrictID === manualDistrictId.value)?.DistrictName ?? ''
  const provinceName =
    ghnProvinces.value.find((p) => p.ProvinceID === manualProvinceId.value)?.ProvinceName ?? ''
  return `${detailAddress.value}, ${wardName}, ${districtName}, ${provinceName}`
}

const handleCheckout = async () => {
  if (!cartData.value.items?.length) return

  if (!validatePhone(orderForm.value.recipientPhone)) {
    showModal(
      'Số điện thoại không hợp lệ',
      'Vui lòng nhập số điện thoại Việt Nam hợp lệ (VD: 0901234567).',
    )
    return
  }

  if (!selectedAddress.value && !detailAddress.value.trim()) {
    showModal('Thiếu thông tin', 'Vui lòng nhập số nhà, tên đường.')
    return
  }

  if (!isAddressReady.value) {
    showModal('Thiếu thông tin', 'Vui lòng chọn đầy đủ địa chỉ giao hàng.')
    return
  }

  if (feeError.value) {
    showModal(
      'Lỗi vận chuyển',
      'Không thể tính phí giao hàng. Vui lòng thử lại hoặc chọn địa chỉ khác.',
    )
    return
  }

  const payload = {
    cartItemIds: cartData.value.items.map((i) => i.cartItemId),
    recipientName: orderForm.value.recipientName,
    recipientPhone: orderForm.value.recipientPhone,
    email: orderForm.value.email,
    paymentMethod: orderForm.value.paymentMethod,
    note: orderForm.value.note,
    shippingAddress: buildShippingAddress(),
    toDistrictId: toDistrictId.value,
    toWardCode: toWardCode.value,
    voucherCode: appliedVoucherCode.value || null,
  }

  isProcessing.value = true
  try {
    const { data } = await api.post('/checkout', payload)

    // Nếu là thanh toán VNPAY, đợi trả URL rồi đá sang trang thanh toán
    if (orderForm.value.paymentMethod === 'VNPAY' && data?.data?.paymentUrl) {
      window.location.href = data.data.paymentUrl
      return
    }

    // ĐÃ SỬA: Bỏ gọi Modal phiền phức, Redirect thẳng sang trang Order Success!
    router.push('/order-success')
  } catch (error) {
    showModal(
      'Lỗi đặt hàng',
      error.response?.data?.message ?? 'Hệ thống gián đoạn. Vui lòng thử lại.',
    )
  } finally {
    isProcessing.value = false
  }
}

onMounted(async () => {
  await prefillUserEmail()
  await Promise.allSettled([fetchGhnProvinces(), fetchCart(), fetchMyAddresses()])
})
</script>
