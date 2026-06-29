<template>
  <div class="checkout-page pt-4">
    <BaseModal
      :show="modal.show"
      :title="modal.title"
      :message="modal.message"
      @close="handleModalConfirm"
    />

    <!-- Modal thông báo voucher hết hạn / hết lượt -->
    <a-modal
      v-model:open="showVoucherExpiredModal"
      title="Thông báo"
      :footer="null"
      centered
      :mask-closable="true"
      @cancel="closeVoucherExpiredModal"
    >
      <p>{{ voucherExpiredMessage }}</p>
    </a-modal>

    <!-- Modal thông báo áp dụng voucher thành công -->
    <div
      class="v-overlay"
      :class="{ active: isVoucherSuccessModalOpen }"
      @click.self="closeVoucherSuccessModal"
    >
      <div
        class="v-card alert-card voucher-success-modal"
        :class="`voucher-success-modal--${voucherSuccessType.toLowerCase()}`"
      >
        <div class="alert-icon voucher-success-modal__icon">
          <svg
            width="48"
            height="48"
            viewBox="0 0 48 48"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <circle cx="24" cy="24" r="20" stroke="#16A34A" stroke-width="2.5" fill="none" />
            <path
              d="M15 24L21 30L33 18"
              stroke="#16A34A"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
              fill="none"
            />
          </svg>
        </div>
        <div class="alert-body">
          <h3 class="alert-title">Áp dụng voucher thành công</h3>
          <p class="alert-message">{{ voucherSuccessMessage }}</p>

          <div class="voucher-success-modal__highlight" v-if="voucherSuccessType === 'FREESHIP'">
            <i class="fas fa-truck"></i>
            <span
              >Tiền được trừ:
              <strong>−{{ formatCurrency(voucherSuccessDiscountAmount) }}</strong></span
            >
          </div>

          <div
            class="voucher-success-modal__highlight"
            v-else-if="voucherSuccessType === 'PERCENT'"
          >
            <i class="fas fa-percent"></i>
            <span
              >Tiền được trừ:
              <strong>−{{ formatCurrency(voucherSuccessDiscountAmount) }}</strong></span
            >
          </div>

          <div class="voucher-success-modal__highlight" v-else-if="voucherSuccessType === 'AMOUNT'">
            <i class="fas fa-tag"></i>
            <span
              >Tiền được trừ:
              <strong>−{{ formatCurrency(voucherSuccessDiscountAmount) }}</strong></span
            >
          </div>

          <p class="voucher-success-modal__note">
            <i class="fas fa-info-circle"></i>
            {{ voucherSuccessNote }}
          </p>
        </div>
        <div class="alert-footer">
          <button
            class="alert-confirm-btn voucher-success-modal__btn"
            type="button"
            @click="closeVoucherSuccessModal"
          >
            Đồng ý
          </button>
        </div>
      </div>
    </div>

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
        <!-- Danh sách địa chỉ đã lưu -->
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

        <!-- Form địa chỉ mới -->
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

        <!-- Phương thức vận chuyển -->
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
                <span class="payment-option__desc text-success" v-if="estimatedDelivery">
                  <i class="fas fa-calendar-check me-1"></i>{{ estimatedDelivery }}
                </span>
                <span class="payment-option__desc" v-else>Giao hàng tận nơi toàn quốc</span>
              </div>

              <div
                style="
                  font-size: 15px;
                  font-weight: 800;
                  color: #d92d20;
                  padding-right: 15px;
                  text-align: right;
                "
              >
                <i class="fas fa-spinner fa-spin text-muted" v-if="isFeeLoading"></i>
                <template v-else-if="toWardCode">
                  <span v-if="hasFreeshipVoucher" class="text-success">Miễn phí</span>
                  <template v-else>
                    <!-- Hiển thị phí gốc bị gạch ngang nếu có Freeship hoặc giảm giá ship từ hệ thống -->
                    <del
                      v-if="shippingData.standardShippingFee > shippingData.discountedShippingFee"
                      style="
                        font-size: 12px;
                        color: #9ca3af;
                        font-weight: 500;
                        display: block;
                        line-height: 1;
                      "
                    >
                      {{ shippingData.standardShippingFee?.toLocaleString('vi-VN') }}₫
                    </del>

                    <span v-if="shippingData.isFreeship" class="text-success">Miễn phí</span>
                    <span v-else
                      >+{{ shippingData.discountedShippingFee?.toLocaleString('vi-VN') }}₫</span
                    >
                  </template>
                </template>
                <span class="text-muted" style="font-size: 12px; font-weight: 500" v-else>
                  Chưa xác định
                </span>
              </div>

              <div class="payment-option__check"><i class="fas fa-check-circle"></i></div>
            </label>
          </div>
        </div>

        <!-- Phương thức thanh toán -->
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

      <!-- Cột hiển thị hóa đơn -->
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
            <div class="order-totals__row align-items-center">
              <span>Phí vận chuyển (GHN)</span>
              <span v-if="isFeeLoading" class="text-muted" style="font-size: 12px">
                <i class="fas fa-spinner fa-spin"></i>
              </span>
              <div v-else-if="toWardCode" class="d-flex align-items-center gap-2">
                <span v-if="hasFreeshipVoucher" class="text-success fw-bold">
                  <i class="fas fa-truck-fast me-1"></i>Miễn phí
                </span>
                <template v-else>
                  <del
                    v-if="shippingData.standardShippingFee > shippingData.discountedShippingFee"
                    style="font-size: 12px; color: #9ca3af; font-weight: 500"
                  >
                    {{ shippingData.standardShippingFee?.toLocaleString('vi-VN') }}₫
                  </del>
                  <span v-if="shippingData.isFreeship" class="text-success fw-bold">
                    Miễn phí
                  </span>
                  <span v-else class="text-danger fw-bold">
                    +{{ shippingData.discountedShippingFee?.toLocaleString('vi-VN') }}₫
                  </span>
                </template>
              </div>
              <span v-else class="text-muted" style="font-size: 12px">Chưa xác định</span>
            </div>
          </div>

          <div class="order-final">
            <span class="order-final__label">Tổng thanh toán</span>
            <span class="order-final__amount"
              >{{ finalAmount.toLocaleString('vi-VN') }}<sup>₫</sup></span
            >
          </div>

          <!-- Thông báo Upsell / Freeship -->
          <div
            v-if="
              shippingData.suggestionMessage &&
              shippingData.isAllowedToOrder &&
              toWardCode &&
              !isFeeLoading
            "
            class="upsell-box"
          >
            <i class="fas fa-shipping-fast upsell-box__icon"></i>
            <span class="upsell-box__text">{{ shippingData.suggestionMessage }}</span>
          </div>

          <!-- Thông báo chặn đơn hàng khi dưới giá trị tối thiểu -->
          <div
            v-if="!shippingData.isAllowedToOrder && toWardCode && !isFeeLoading"
            class="block-box"
          >
            <i class="fas fa-exclamation-triangle block-box__icon"></i>
            <span class="block-box__text">{{ shippingData.blockMessage }}</span>
          </div>

          <button
            form="checkoutForm"
            type="submit"
            class="btn-checkout mt-3"
            :disabled="
              isProcessing ||
              !cartData.items?.length ||
              isFeeLoading ||
              (!shippingData.isAllowedToOrder && !!toWardCode)
            "
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import BaseModal from '@/components/BaseModal.vue'

import { useCartStore } from '@/stores/cartStore'
const cartStore = useCartStore()

import addressApi from '@/api/addressApi'
import userApi from '@/api/userApi'
import cartApi from '@/api/cartApi'
import ghnApi from '@/api/ghnApi'
import api from '@/utils/api'
import '@/assets/css/CheckOut.css'

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
  modal.value.action = null
}

// ─── Order & Cart Data (Kết hợp LocalStorage từ HEAD)
const getInitialCartData = () => {
  const savedItems = localStorage.getItem('selectedCartItems')
  if (savedItems) {
    try {
      const items = JSON.parse(savedItems)
      if (Array.isArray(items) && items.length > 0) {
        return {
          items,
          totalQuantity: items.reduce((sum, i) => sum + (i.quantity || 0), 0),
          totalAmount: items.reduce((sum, i) => sum + (i.totalPrice || 0), 0),
        }
      }
    } catch (e) {
      console.warn('Lỗi parse selectedCartItems:', e)
    }
  }
  return { items: [], totalQuantity: 0, totalAmount: 0 }
}

const cartData = ref(getInitialCartData())

const savedVoucher = JSON.parse(localStorage.getItem('selectedVoucher') || 'null')
const appliedVoucherCode = ref(savedVoucher?.code || '')

// Tích hợp Shipping Data mới theo Backend (Cơ chế Upsell/Freeship của Marcus)
const estimatedDelivery = ref('')
const shippingData = ref({
  standardShippingFee: 0,
  discountedShippingFee: 0,
  isFreeship: false,
  isAllowedToOrder: true,
  blockMessage: '',
  amountUntilFreeship: 0,
  suggestionMessage: '',
})

const discountAmount = ref(0)
const hasFreeshipVoucher = ref(false)
const voucherError = ref('')
const isVoucherLoading = ref(false)

const showVoucherExpiredModal = ref(false)
const voucherExpiredMessage = ref('')

const openVoucherExpiredModal = (message) => {
  voucherExpiredMessage.value =
    message || 'Voucher đã hết hạn hoặc hết lượt sử dụng, vui lòng chọn voucher khác.'
  showVoucherExpiredModal.value = true
}

const closeVoucherExpiredModal = () => {
  showVoucherExpiredModal.value = false
  voucherExpiredMessage.value = ''
}

// Modal thông báo áp dụng voucher thành công
const isVoucherSuccessModalOpen = ref(false)
const voucherSuccessMessage = ref('')
const voucherSuccessType = ref('AMOUNT')
const voucherSuccessAmount = ref(0)
const voucherSuccessPercent = ref(0)
const voucherSuccessMaxDiscount = ref(0)
const voucherSuccessDiscountAmount = ref(0)
const voucherSuccessNote = ref(
  'Số tiền giảm thực tế sẽ được hệ thống tính toán lại tại bước thanh toán.',
)
const lastShownSuccessCode = ref('')

const openVoucherSuccessModal = (voucherInfo) => {
  if (!voucherInfo) return
  const code = voucherInfo.voucherCode || appliedVoucherCode.value
  if (lastShownSuccessCode.value === code) return
  lastShownSuccessCode.value = code

  voucherSuccessType.value = (voucherInfo.discountType || 'AMOUNT').toUpperCase()
  voucherSuccessAmount.value = Number(voucherInfo.discountValue) || 0
  voucherSuccessPercent.value = Number(voucherInfo.discountPercent) || 0
  voucherSuccessMaxDiscount.value = Number(voucherInfo.maxDiscountAmount) || 0
  voucherSuccessDiscountAmount.value =
    Number(voucherInfo.actualDiscountAmount ?? voucherInfo.discountAmount) || 0
  voucherSuccessMessage.value = `Voucher ${code} đã được áp dụng thành công.`

  if (voucherSuccessType.value === 'FREESHIP') {
    voucherSuccessNote.value =
      'Số tiền freeship thực tế sẽ được tính dựa trên phí vận chuyển GHN tại bước thanh toán.'
  } else {
    voucherSuccessNote.value =
      'Vui lòng kiểm tra lại thông tin voucher trước khi tiến hành đặt hàng.'
  }

  isVoucherSuccessModalOpen.value = true
}

const closeVoucherSuccessModal = () => {
  isVoucherSuccessModalOpen.value = false
  voucherSuccessMessage.value = ''
  voucherSuccessAmount.value = 0
  voucherSuccessPercent.value = 0
  voucherSuccessMaxDiscount.value = 0
  voucherSuccessDiscountAmount.value = 0
  voucherSuccessType.value = 'AMOUNT'
}

const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')}₫`

const orderForm = ref({
  recipientName: '',
  recipientPhone: '',
  email: '',
  paymentMethod: 'COD',
  note: '',
})

// ─── GHN Data
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
  const total = (cartData.value.totalAmount ?? 0) - discountAmount.value
  const finalShippingFee = hasFreeshipVoucher.value
    ? 0
    : shippingData.value.discountedShippingFee || 0
  const finalTotal = total + finalShippingFee
  return finalTotal > 0 ? finalTotal : 0
})

const isAddressReady = computed(() => !!toDistrictId.value && !!toWardCode.value)

// ─── Utils (Validation & Formatting)
const formatDeliveryDate = (isoDate) => {
  if (!isoDate) return ''
  try {
    const d = new Date(isoDate)
    return `Dự kiến giao ${d.toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'numeric' })}`
  } catch (e) {
    return ''
  }
}

const validatePhone = (phone) => /(03|05|07|08|09)\d{8}/.test(phone)

const validateEmail = (email) => {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return re.test(email)
}

// ─── Voucher preview
const previewVoucher = async () => {
  const code = appliedVoucherCode.value
  if (!code) {
    discountAmount.value = 0
    hasFreeshipVoucher.value = false
    voucherError.value = ''
    return
  }

  isVoucherLoading.value = true
  voucherError.value = ''

  try {
    const res = await api.post('/client/vouchers/preview', {
      voucherCode: code,
      orderAmount: cartData.value.totalAmount || 0,
      shippingFee: 0,
    })

    const result = res.data?.data ?? res.data
    if (result?.applied) {
      discountAmount.value = Number(result.discountAmount) || 0
      hasFreeshipVoucher.value = result.discountType === 'FREESHIP'
      voucherError.value = ''
      openVoucherSuccessModal({
        voucherCode: code,
        discountType: result.discountType,
        discountValue: result.discountValue,
        discountPercent: result.discountPercent,
        maxDiscountAmount: result.maxDiscountAmount,
        actualDiscountAmount: result.discountAmount,
      })
    } else {
      discountAmount.value = 0
      hasFreeshipVoucher.value = false
      voucherError.value = result?.message || 'Mã giảm giá không hợp lệ'
      localStorage.removeItem('selectedVoucher')
      appliedVoucherCode.value = ''
      openVoucherExpiredModal(
        result?.message || 'Voucher đã hết hạn hoặc hết lượt sử dụng, vui lòng chọn voucher khác.',
      )
    }
  } catch (e) {
    discountAmount.value = 0
    hasFreeshipVoucher.value = false
    const errorMsg = e.response?.data?.message || 'Không thể áp dụng mã giảm giá'
    voucherError.value = errorMsg
    localStorage.removeItem('selectedVoucher')
    appliedVoucherCode.value = ''
    openVoucherExpiredModal(errorMsg)
  } finally {
    isVoucherLoading.value = false
  }
}

// ─── 1. Phí vận chuyển GHN & Tính toán Freeship
const calculateShippingFee = async () => {
  if (!toDistrictId.value || !toWardCode.value) {
    shippingData.value = {
      standardShippingFee: 0,
      discountedShippingFee: 0,
      isFreeship: false,
      isAllowedToOrder: true,
      blockMessage: '',
      amountUntilFreeship: 0,
      suggestionMessage: '',
    }
    estimatedDelivery.value = ''
    return
  }

  isFeeLoading.value = true
  feeError.value = ''

  try {
    const res = await api.post('/client/shipping/calculate', {
      toDistrictId: toDistrictId.value,
      toWardCode: toWardCode.value,
      totalWeightGram: 500,
      cartTotal: cartData.value.totalAmount,
    })

    if (res.data?.code === 200 && res.data?.data) {
      shippingData.value = res.data.data
      const futureDate = new Date()
      futureDate.setDate(futureDate.getDate() + 3)
      estimatedDelivery.value = formatDeliveryDate(futureDate.toISOString())
    } else {
      throw new Error('Không thể lấy thông tin phí vận chuyển')
    }
  } catch (error) {
    shippingData.value = {
      standardShippingFee: 0,
      discountedShippingFee: 0,
      isFreeship: false,
      isAllowedToOrder: true,
      blockMessage: '',
      amountUntilFreeship: 0,
      suggestionMessage: '',
    }
    feeError.value = 'Không thể tính phí vận chuyển. Vui lòng kiểm tra địa chỉ hoặc thử lại!'
  } finally {
    isFeeLoading.value = false
  }
}

// ─── 2. Sổ địa chỉ
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
  shippingData.value = {
    standardShippingFee: 0,
    discountedShippingFee: 0,
    isFreeship: false,
    isAllowedToOrder: true,
    blockMessage: '',
    amountUntilFreeship: 0,
    suggestionMessage: '',
  }
  estimatedDelivery.value = ''
  manualProvinceId.value = null
  manualDistrictId.value = null
  manualWardCode.value = ''
  detailAddress.value = ''
}

// ─── 3. Dropdown GHN
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
  shippingData.value = {
    standardShippingFee: 0,
    discountedShippingFee: 0,
    isFreeship: false,
    isAllowedToOrder: true,
    blockMessage: '',
    amountUntilFreeship: 0,
    suggestionMessage: '',
  }
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
  shippingData.value = {
    standardShippingFee: 0,
    discountedShippingFee: 0,
    isFreeship: false,
    isAllowedToOrder: true,
    blockMessage: '',
    amountUntilFreeship: 0,
    suggestionMessage: '',
  }
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

// ─── 4. API phụ trợ
const fetchCart = async () => {
  try {
    const res = await cartApi.getCart()
    const data = res.data
    const fetchedCart = data?.data ?? data

    if (!cartData.value.items?.length && fetchedCart.items?.length) {
      cartData.value = fetchedCart
    }
  } catch (error) {
    if (error.response?.status === 401) {
      showModal('Yêu cầu đăng nhập', 'Vui lòng đăng nhập để tiếp tục thanh toán.', 'redirect_login')
    }
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

// ─── 5. Submit Checkout
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
      'Vui lòng nhập đúng số điện thoại Việt Nam (VD: 0901234567).',
    )
    return
  }

  if (!validateEmail(orderForm.value.email)) {
    showModal(
      'Email không hợp lệ',
      'Vui lòng nhập đúng định dạng email (VD: nguyenvan_a@gmail.com).',
    )
    return
  }

  if (!selectedAddress.value) {
    if (!manualProvinceId.value) {
      showModal('Thiếu địa chỉ', 'Vui lòng chọn <strong>Tỉnh / Thành phố</strong> giao hàng.')
      return
    }
    if (!manualDistrictId.value) {
      showModal('Thiếu địa chỉ', 'Vui lòng chọn <strong>Quận / Huyện</strong> giao hàng.')
      return
    }
    if (!manualWardCode.value) {
      showModal('Thiếu địa chỉ', 'Vui lòng chọn <strong>Phường / Xã</strong> giao hàng.')
      return
    }
    if (!detailAddress.value.trim()) {
      showModal('Thiếu địa chỉ', 'Vui lòng nhập <strong>Số nhà, tên đường</strong>.')
      return
    }
  }

  if (!isAddressReady.value) {
    showModal(
      'Lỗi hệ thống',
      'Chưa nhận diện được mã địa chỉ giao hàng. Vui lòng tải lại trang hoặc chọn lại địa chỉ.',
    )
    return
  }

  if (feeError.value || isFeeLoading.value) {
    showModal(
      'Lỗi phí vận chuyển',
      'Không thể tính phí giao hàng. Vui lòng đợi trong giây lát hoặc chọn địa chỉ khác.',
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
    // ÉP LẤY DỮ LIỆU: Ưu tiên địa chỉ sổ (toDistrictId) => nếu không có thì lấy địa chỉ chọn tay
    toDistrictId: toDistrictId.value || manualDistrictId.value,
    toWardCode: toWardCode.value || manualWardCode.value,
    voucherCode: appliedVoucherCode.value || null,
  }

  isProcessing.value = true
  try {
    const { data } = await api.post('/checkout', payload)

    const paidSkuIds = cartData.value.items.map((i) => i.skuId)
    await cartStore.removeManyItemFromCart(paidSkuIds)

    localStorage.removeItem('selectedCartItems')
    localStorage.removeItem('selectedSubtotal')
    localStorage.removeItem('selectedVoucher')

    if (orderForm.value.paymentMethod === 'VNPAY' && data?.data?.paymentUrl) {
      window.location.href = data.data.paymentUrl
      return
    }

    const savedOrderCode = data?.data?.orderCode || data?.orderCode || 'Không xác định'
    router.push({ path: '/order-success', query: { orderCode: savedOrderCode } })
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
  await previewVoucher()
})

watch(
  () => cartData.value.totalAmount,
  () => {
    if (appliedVoucherCode.value) {
      previewVoucher()
    }
  },
)
</script>

<style scoped>
/* ── Voucher Success Modal (style tương thích với check-out.css) ── */
.v-overlay {
  position: fixed;
  z-index: 1100;
  top: 0;
  left: 0;
  display: flex;
  visibility: hidden;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(4px);
  opacity: 0;
  transition:
    opacity 0.2s ease,
    visibility 0.2s ease;
}

.v-overlay.active {
  visibility: visible;
  opacity: 1;
}

.v-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  transform: scale(0.92);
  opacity: 0;
  transition:
    transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1),
    opacity 0.2s;
}

.v-overlay.active .v-card {
  transform: scale(1);
  opacity: 1;
}

.alert-card {
  width: 420px;
  max-width: 92vw;
  border-radius: 20px;
  padding: 36px 32px 28px;
  text-align: center;
  align-items: center;
}

.alert-icon {
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #f0fdf4;
}

.alert-body {
  width: 100%;
  margin-bottom: 24px;
}

.alert-title {
  margin: 0 0 10px;
  color: #16a34a;
  font-size: 19px;
  font-weight: 700;
  line-height: 1.2;
}

.alert-message {
  margin: 0;
  color: #475569;
  font-size: 14.5px;
  line-height: 1.6;
}

.alert-footer {
  width: 100%;
}

.alert-confirm-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.3px;
  transition: background 0.15s ease;
}

.alert-confirm-btn:hover {
  background: linear-gradient(135deg, #15803d 0%, #166534 100%);
}

.voucher-success-modal .alert-title {
  color: #16a34a;
}

.voucher-success-modal__highlight {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 14px auto 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border: 1px solid #a7f3d0;
  border-radius: 10px;
  color: #065f46;
  font-size: 14px;
  flex-wrap: wrap;
  text-align: center;
}

.voucher-success-modal__highlight i {
  font-size: 18px;
  color: #16a34a;
  flex-shrink: 0;
}

.voucher-success-modal__highlight strong {
  color: #047857;
  font-size: 15.5px;
  font-weight: 800;
}

.voucher-success-modal__note {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 12px 0 0;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  color: #64748b;
  font-size: 12.5px;
  line-height: 1.5;
  text-align: left;
}

.voucher-success-modal__note i {
  font-size: 14px;
  color: #94a3b8;
  flex-shrink: 0;
  margin-top: 1px;
}

.voucher-success-modal--percent .voucher-success-modal__highlight {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-color: #fde68a;
  color: #92400e;
}

.voucher-success-modal--percent .voucher-success-modal__highlight i {
  color: #b45309;
}

.voucher-success-modal--percent .voucher-success-modal__highlight strong {
  color: #b45309;
}

.voucher-success-modal--amount .voucher-success-modal__highlight {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-color: #bfdbfe;
  color: #1e40af;
}

.voucher-success-modal--amount .voucher-success-modal__highlight i {
  color: #1d4ed8;
}

.voucher-success-modal--amount .voucher-success-modal__highlight strong {
  color: #1d4ed8;
}

@media (max-width: 480px) {
  .alert-card {
    width: 94vw;
    padding: 28px 22px 22px;
  }
}
</style>
