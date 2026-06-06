<template>
  <div class="checkout-page pt-4">
    <div class="checkout-header">
      <div class="checkout-header__inner">
        <div class="checkout-header__brand">
          <i class="fas fa-shopping-bag"></i>
          <span>Thanh toán an toàn</span>
        </div>
        <div class="checkout-header__steps">
          <div class="step step--done">
            <span class="step__dot"><i class="fas fa-check"></i></span>
            <span class="step__label">Giỏ hàng</span>
          </div>
          <div class="step__line step__line--done"></div>
          <div class="step step--active">
            <span class="step__dot">2</span>
            <span class="step__label">Thanh toán</span>
          </div>
          <div class="step__line"></div>
          <div class="step">
            <span class="step__dot">3</span>
            <span class="step__label">Xác nhận</span>
          </div>
        </div>
      </div>
    </div>

    <div class="checkout-body">
      <div class="checkout-left">
        <div class="checkout-card mb-3" v-if="savedAddresses.length > 0">
          <div class="checkout-card__title">
            <span class="checkout-card__icon"><i class="fas fa-address-book"></i></span>
            Sổ địa chỉ của bạn
          </div>
          <div class="d-flex flex-wrap gap-2">
            <button
              v-for="addr in savedAddresses"
              :key="addr.id"
              type="button"
              class="btn btn-outline-danger btn-sm rounded-pill"
              @click="applySavedAddress(addr)"
            >
              <i class="fas fa-home me-1"></i> {{ addr.recipientName }} - {{ addr.phone }} ({{
                addr.provinceName
              }})
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
                <input v-model="orderForm.recipientName" type="text" class="form-input" required />
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
                <input v-model="orderForm.email" type="email" class="form-input" required />
              </div>
            </div>

            <div class="form-row form-row--3col">
              <div class="form-group">
                <label class="form-label">Tỉnh / Thành phố <span class="req">*</span></label>
                <div class="select-wrapper">
                  <select
                    class="form-select"
                    v-model="selectedProvince"
                    @change="handleProvinceChange"
                    required
                  >
                    <option value="" disabled>Chọn Tỉnh/Thành</option>
                    <option v-for="p in provinces" :key="p.code" :value="p">{{ p.name }}</option>
                  </select>
                  <i class="fas fa-chevron-down select-arrow"></i>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">Quận / Huyện <span class="req">*</span></label>
                <div class="select-wrapper">
                  <select
                    class="form-select"
                    v-model="selectedDistrict"
                    @change="handleDistrictChange"
                    :disabled="!selectedProvince"
                    required
                  >
                    <option value="" disabled>Chọn Quận/Huyện</option>
                    <option v-for="d in districts" :key="d.code" :value="d">{{ d.name }}</option>
                  </select>
                  <i class="fas fa-chevron-down select-arrow"></i>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">Phường / Xã <span class="req">*</span></label>
                <div class="select-wrapper">
                  <select
                    class="form-select"
                    v-model="selectedWard"
                    :disabled="!selectedDistrict"
                    required
                  >
                    <option value="" disabled>Chọn Phường/Xã</option>
                    <option v-for="w in wards" :key="w.code" :value="w">{{ w.name }}</option>
                  </select>
                  <i class="fas fa-chevron-down select-arrow"></i>
                </div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group form-group--full">
                <label class="form-label">Số nhà, tên đường <span class="req">*</span></label>
                <input
                  v-model="detailAddress"
                  type="text"
                  class="form-input"
                  placeholder="VD: 118 Đường Cát Bi..."
                  required
                />
              </div>
            </div>
          </form>
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
                <div class="d-flex gap-3 align-items-center p-3 bg-light rounded-3">
                  <img
                    :src="`https://img.vietqr.io/image/mbbank-0901234567-compact2.png?amount=${finalAmount}&addInfo=DH${orderForm.recipientPhone}&accountName=MARCUS%20TRAN`"
                    alt="QR Code"
                    class="rounded border bg-white"
                    style="width: 100px; height: 100px"
                  />
                  <div class="small">
                    <div class="fw-bold">Ngân hàng MB Bank</div>
                    <div>STK: <strong>0901234567</strong></div>
                    <div>Chủ TK: <strong>MARCUS TRAN</strong></div>
                    <div class="text-danger fw-bold">
                      Số tiền: {{ finalAmount.toLocaleString('vi-VN') }}đ
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
            Đơn hàng <span class="order-summary__badge">{{ cartData.totalQuantity }} sản phẩm</span>
          </div>

          <div class="order-items">
            <div v-for="item in cartData.items" :key="item.cartItemId" class="order-item">
              <div class="order-item__img-wrap">
                <img :src="item.imageUrl" alt="Product" class="order-item__img" />
                <span class="order-item__qty">{{ item.quantity }}</span>
              </div>
              <div class="order-item__info">
                <div class="order-item__name">{{ item.productName }}</div>
                <div class="order-item__sku">SKU: {{ item.skuCode }}</div>
              </div>
              <div class="order-item__price">{{ item.totalPrice?.toLocaleString('vi-VN') }}₫</div>
            </div>
          </div>

          <div class="voucher-box" v-if="appliedVoucherCode">
            <div class="voucher-box__icon"><i class="fas fa-ticket-alt"></i></div>
            <div class="p-2 flex-grow-1">
              <div class="fw-bold text-dark" style="font-size: 13px">
                Đã áp dụng mã: {{ appliedVoucherCode }}
              </div>
              <div class="text-success" style="font-size: 12px">
                Giảm {{ discountAmount.toLocaleString('vi-VN') }}₫
              </div>
            </div>
            <button
              class="btn btn-sm btn-link text-danger text-decoration-none"
              @click="returnToCart"
            >
              Đổi mã
            </button>
          </div>

          <div class="order-totals">
            <div class="order-totals__row">
              <span>Tạm tính</span>
              <span>{{ cartData.totalAmount?.toLocaleString('vi-VN') }}₫</span>
            </div>
            <div class="order-totals__row order-totals__row--discount" v-if="discountAmount > 0">
              <span>Giảm giá Voucher</span>
              <span>-{{ discountAmount.toLocaleString('vi-VN') }}₫</span>
            </div>
            <div class="order-totals__row order-totals__row--shipping">
              <span>Phí vận chuyển</span>
              <span class="text-success" v-if="shippingFee === 0 && !selectedProvince"
                >Chưa xác định</span
              >
              <span class="text-danger" v-else>+{{ shippingFee.toLocaleString('vi-VN') }}₫</span>
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
            :disabled="isProcessing || cartData.items.length === 0"
          >
            <span v-if="!isProcessing"><i class="fas fa-lock"></i> Đặt hàng ngay</span>
            <span v-else><i class="fas fa-spinner fa-spin"></i> Đang xử lý...</span>
          </button>
        </div>
      </div>
      <!-- END .checkout-right -->
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import '@/assets/css/check-out.css'

const router = useRouter()

const isProcessing = ref(false)
const shippingFee = ref(0)
const appliedVoucherCode = ref('')
const discountAmount = ref(0)

const orderForm = ref({
  recipientName: '',
  recipientPhone: '',
  email: '',
  paymentMethod: 'COD',
})

const cartData = ref({ items: [], totalQuantity: 0, totalAmount: 0 })

const provinces = ref([])
const districts = ref([])
const wards = ref([])
const selectedProvince = ref('')
const selectedDistrict = ref('')
const selectedWard = ref('')
const detailAddress = ref('')

const savedAddresses = ref([])

const fetchMyAddresses = async () => {
  try {
    const res = await api.get('/user/addresses')
    savedAddresses.value = res.data.data // Lấy đúng mảng Data từ ApiResponse

    // Auto-fill luôn địa chỉ mặc định (thằng đầu tiên) vào form cho khách lười
    if (savedAddresses.value.length > 0) {
      applySavedAddress(savedAddresses.value[0])
    }
  } catch (error) {
    console.error('Không thể tải sổ địa chỉ', error)
  }
}

const finalAmount = computed(() => {
  const total = cartData.value.totalAmount - discountAmount.value + shippingFee.value
  return total > 0 ? total : 0
})

const calculateShippingFee = () => {
  if (!selectedProvince.value) {
    shippingFee.value = 0
    return
  }
  if (selectedProvince.value.name.includes('Hải Phòng')) {
    shippingFee.value = 20000
  } else {
    shippingFee.value = 40000
  }
}

const fetchProvinces = async () => {
  try {
    const res = await fetch('https://provinces.open-api.vn/api/p/')
    provinces.value = await res.json()
  } catch (error) {
    console.error(error)
  }
}

const handleProvinceChange = async () => {
  selectedDistrict.value = ''
  selectedWard.value = ''
  wards.value = []
  calculateShippingFee()
  if (!selectedProvince.value) return
  const res = await fetch(
    `https://provinces.open-api.vn/api/p/${selectedProvince.value.code}?depth=2`,
  )
  const data = await res.json()
  districts.value = data.districts
}

const handleDistrictChange = async () => {
  selectedWard.value = ''
  if (!selectedDistrict.value) return
  const res = await fetch(
    `https://provinces.open-api.vn/api/d/${selectedDistrict.value.code}?depth=2`,
  )
  const data = await res.json()
  wards.value = data.wards
}

const applySavedAddress = async (addr) => {
  orderForm.value.recipientName = addr.recipientName
  orderForm.value.recipientPhone = addr.phone
  detailAddress.value = addr.detail

  const matchedProv = provinces.value.find((p) => p.name === addr.provinceName)
  if (matchedProv) {
    selectedProvince.value = matchedProv
    await handleProvinceChange()

    const matchedDist = districts.value.find((d) => d.name === addr.districtName)
    if (matchedDist) {
      selectedDistrict.value = matchedDist
      await handleDistrictChange()

      const matchedWard = wards.value.find((w) => w.name === addr.wardName)
      if (matchedWard) selectedWard.value = matchedWard
    }
  }
}

const fetchCart = async () => {
  try {
    const res = await api.get('/cart')
    cartData.value = res.data.data || res.data
    if (!cartData.value.items || cartData.value.items.length === 0) {
      alert('Giỏ hàng trống!')
      router.push('/')
      return
    }
  } catch (error) {
    if (error.response?.status === 401) {
      alert('Vui lòng đăng nhập để thanh toán')
      router.push('/auth/login')
    }
  }
}

const returnToCart = () => {
  router.push('/cart')
}

const handleCheckout = async () => {
  if (cartData.value.items.length === 0) return
  const fullShippingAddress = `${detailAddress.value}, ${selectedWard.value.name}, ${selectedDistrict.value.name}, ${selectedProvince.value.name}`

  const payload = {
    ...orderForm.value,
    shippingAddress: fullShippingAddress,
    voucherCode: appliedVoucherCode.value,
    shippingFee: shippingFee.value,
  }

  isProcessing.value = true
  console.log('Chốt đơn - Gửi Server:', payload)

  setTimeout(() => {
    alert(
      `Chốt đơn thành công! Tổng tiền: ${finalAmount.value.toLocaleString()} ₫\nShip tới: ${fullShippingAddress}`,
    )
    isProcessing.value = false
    router.push('/order-success')
  }, 1000)
}

onMounted(async () => {
  await fetchProvinces()
  fetchCart()
  fetchMyAddresses()
})
</script>
