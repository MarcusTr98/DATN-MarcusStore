<template>
  <div class="checkout-page pt-4">
    <BaseModal
      :visible="modal.show"
      :title="modal.title"
      :message="modal.message"
      @close="handleModalConfirm"
    />

    <!-- Modal thông báo Flash Sale đã bị admin hủy -->
    <CancelledFlashSaleModal
      :visible="showCancelledModal"
      @close="showCancelledModal = false"
      @confirm="handleCancelledConfirm"
    />

    <!-- Modal thông báo voucher không khả dụng -->
    <BaseModal
      :visible="isVoucherInvalidModalOpen"
      type="error"
      title="Voucher không khả dụng"
      :message="voucherInvalidMessage"
      @close="closeVoucherInvalidModal"
      @confirm="handleVoucherInvalidConfirm"
    />

    <!-- Modal chọn lại voucher (từ Cart) -->
    <VoucherModal
      :visible="isReSelectVoucherModalOpen"
      :vouchers="availableVouchers"
      :cart-total="cartData.totalAmount"
      :is-loading="isAvailableVouchersLoading"
      :pre-selected-id="null"
      :title="reSelectVoucherMessage ? 'Voucher không khả dụng' : 'Chọn 1 Voucher Áp Dụng'"
      :subtitle="
        reSelectVoucherMessage || 'Hệ thống tự động chọn mã có giá trị giảm cao nhất cho bạn'
      "
      @close="closeReSelectVoucherModal"
      @confirm="handleVoucherModalConfirm"
    />

    <!-- Modal chọn voucher từ checkout -->
    <VoucherModal
      :visible="isCheckoutVoucherModalOpen"
      :vouchers="availableVouchers"
      :cart-total="cartData.totalAmount"
      :is-loading="isAvailableVouchersLoading"
      :pre-selected-id="v2SelectedId"
      title="Chọn Voucher"
      subtitle="Chọn voucher để giảm giá đơn hàng"
      @close="closeCheckoutVoucherModal"
      @confirm="handleCheckoutVoucherConfirm"
    />

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
        <router-link to="/cart" class="btn-back-to-cart">
          <i class="fas fa-arrow-left"></i> Quay lại giỏ hàng
        </router-link>

        <!-- Danh sách địa chỉ đã lưu -->
        <div class="checkout-card mb-3" v-if="savedAddresses.length > 0">
          <div class="checkout-card__title">
            <span class="checkout-card__icon"><i class="fas fa-address-book"></i></span>
            Sổ địa chỉ nhận hàng
            <span class="address-count-badge">{{ savedAddresses.length }}</span>
          </div>

          <div class="saved-address-list">
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
              <div class="payment-option__icon payment-option__icon--plain">
                <i class="fas fa-shipping-fast text-danger shipping-method-icon"></i>
              </div>
              <div class="payment-option__body">
                <span class="shipping-option__name">Giao Hàng Nhanh (GHN)</span>
                <span class="payment-option__desc text-success" v-if="estimatedDelivery">
                  <i class="fas fa-calendar-check me-1"></i>{{ estimatedDelivery }}
                </span>
                <span class="payment-option__desc" v-else>Giao hàng tận nơi toàn quốc</span>
              </div>

              <div class="shipping-fee-display">
                <i class="fas fa-spinner fa-spin text-muted" v-if="isFeeLoading"></i>
                <template v-else-if="toWardCode">
                  <span v-if="hasFreeshipVoucher" class="text-success">Miễn phí</span>
                  <template v-else>
                    <!-- Hiển thị phí gốc bị gạch ngang nếu có Freeship hoặc giảm giá ship từ hệ thống -->
                    <del
                      v-if="shippingData.standardShippingFee > shippingData.discountedShippingFee"
                      class="shipping-fee-original"
                    >
                      {{ shippingData.standardShippingFee?.toLocaleString('vi-VN') }}₫
                    </del>

                    <span v-if="shippingData.isFreeship" class="text-success">Miễn phí</span>
                    <span v-else
                      >+{{ shippingData.discountedShippingFee?.toLocaleString('vi-VN') }}₫</span
                    >
                  </template>
                </template>
                <span class="text-muted shipping-fee-undetermined" v-else> Chưa xác định </span>
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
              <!-- Cột trái: Ảnh + Badge số lượng -->
              <div class="order-item__img-wrap">
                <img :src="item.thumbnailUrl" :alt="item.productName" class="order-item__img" />
                <span class="order-item__qty">{{ item.quantity }}</span>
              </div>

              <!-- Cột giữa: Thông tin sản phẩm -->
              <div class="order-item__info">
                <div class="order-item__name">{{ item.productName }}</div>
                <div class="order-item__variant" v-if="item.variantName">
                  {{ expandColorName(item.variantName) }}
                </div>
                <div class="order-item__sku">SKU: {{ item.skuCode }}</div>
                <!-- Badge Flash Sale -->
                <span v-if="item.isFlashSale" class="order-item__flash-sale-badge">
                  ⚡ {{ item.flashSaleSlotName || 'Flash Sale' }}
                </span>
              </div>

              <!-- Cột phải: Giá tiền -->
              <div class="order-item__price-col">
                <div class="order-item__current-price">
                  {{ item.totalPrice?.toLocaleString('vi-VN') }}₫
                </div>
                <s v-if="item.originalPrice && item.originalPrice > item.price" class="order-item__original-price">
                  {{ item.originalPrice?.toLocaleString('vi-VN') }}₫
                </s>
              </div>
            </div>
          </div>

          <div class="order-totals mt-3">
            <div class="order-totals__row">
              <span>Tạm tính</span>
              <span>{{ cartData.totalAmount?.toLocaleString('vi-VN') }}₫</span>
            </div>

            <!-- Voucher đã chọn -->
            <div class="order-totals__row order-totals__row--voucher" v-if="appliedVoucherCode">
              <div class="voucher-selected">
                <div class="voucher-selected__left">
                  <i class="fas fa-ticket-alt text-danger me-1"></i>
                  <span class="voucher-selected__code">{{ appliedVoucherCode }}</span>
                </div>
                <button
                  type="button"
                  class="voucher-selected__change"
                  @click="openVoucherModal"
                  title="Đổi voucher"
                >
                  Đổi mã
                  <i class="fas fa-chevron-right"></i>
                </button>
              </div>
              <span v-if="voucherError" class="voucher-error-text">{{ voucherError }}</span>
              <span v-else-if="isVoucherLoading" class="voucher-loading-text">
                <i class="fas fa-spinner fa-spin"></i>
              </span>
            </div>

            <!-- Nút chọn voucher -->
            <div class="order-totals__row" v-else>
              <button type="button" class="btn-voucher-select" @click="openVoucherModal">
                <i class="fas fa-ticket-alt me-1"></i> Chọn Voucher
              </button>
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
                <!-- Trường hợp 1: Freeship toàn phần (do voucher hoặc trợ giá >= phí ship) -->
                <span
                  v-if="hasFreeshipVoucher || shippingData.isFreeship"
                  class="text-success fw-bold"
                >
                  <i class="fas fa-truck-fast me-1"></i>Miễn phí
                </span>

                <!-- Trường hợp 2: Có tính phí (bao gồm cả trường hợp được trợ giá 1 phần) -->
                <template v-else>
                  <!-- Hiển thị giá gốc bị gạch ngang nếu có trợ giá -->
                  <del
                    v-if="shippingData.standardShippingFee > shippingData.discountedShippingFee"
                    style="font-size: 12px; color: #9ca3af; font-weight: 500"
                  >
                    {{ shippingData.standardShippingFee?.toLocaleString('vi-VN') }}₫
                  </del>

                  <!-- Nhãn Hỗ trợ phí nếu được trợ giá 1 phần -->
                  <span
                    v-if="shippingData.standardShippingFee > shippingData.discountedShippingFee"
                    class="badge-subsidy"
                  >
                    <i class="fas fa-hand-holding-usd"></i> Đã hỗ trợ
                  </span>

                  <!-- Phí khách phải trả cuối cùng -->
                  <span class="text-danger fw-bold">
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
import { ref, computed, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import BaseModal from '@/components/BaseModal.vue'
import CancelledFlashSaleModal from '@/components/CancelledFlashSaleModal.vue'
import VoucherModal from '@/components/VoucherModal.vue'
import { expandColorName } from '@/utils/colorUtils'

import { useCartStore } from '@/stores/cartStore'
import { useFlashSaleStore } from '@/stores/FlashSaleStore'
const cartStore = useCartStore()
const flashSaleStore = useFlashSaleStore()

import LoginRequiredModal from '@/components/LoginRequiredModal.vue'

import addressApi from '@/api/addressApi'
import userApi from '@/api/userApi'
import cartApi from '@/api/cartApi'
import ghnApi from '@/api/ghnApi'
import api from '@/utils/api'
import '@/assets/css/CheckOut.css'
import '@/assets/css/cart.css'

const router = useRouter()
const isProcessing = ref(false)
const isFeeLoading = ref(false)
const feeError = ref('')

// ==== Modal thông báo Flash Sale bị admin hủy ====
// Khi user vào trang thanh toán mà đơn hàng có chứa SP FS từ slot CANCELLED
// (admin vừa hủy sau khi user checkout từ giỏ) → chặn + hiện modal.
const showCancelledModal = ref(false)

// Cờ chặn vòng lặp modal bật liên tục. Sau khi user bấm "Đồng ý" 1 lần,
// không tự động mở lại modal trong cùng phiên. Reset khi component unmount
// (user rời trang) để lần sau quay lại modal vẫn hoạt động nếu vẫn còn lỗi.
const hasHandledCancelled = ref(false)

function openCancelledModal() {
  if (hasHandledCancelled.value) return
  showCancelledModal.value = true
}

async function handleCancelledConfirm() {
  if (hasHandledCancelled.value) return
  hasHandledCancelled.value = true

  showCancelledModal.value = false
  // Điều hướng về trang chủ thay vì reload toàn trang.
  // Lý do: reload sẽ trigger lại onMounted → watch(clientSlots) → lại mở modal nếu
  // cartData trong localStorage vẫn chứa SP CANCELLED. router.replace('/') thì an toàn.
  await router.replace({ path: '/' }).catch(() => {
    window.location.href = '/'
  })
}

// Kiểm tra trong cartData có SP Flash Sale thuộc slot đã bị admin hủy không.
// cartData lấy từ localStorage 'selectedCartItems' (đã có isFlashSale + flashSaleSlotId).
function findCancelledFlashSaleItem() {
  const items = Array.isArray(cartData.value.items) ? cartData.value.items : []
  const slots = flashSaleStore.clientSlots
  if (!Array.isArray(slots)) return null
  return (
    items.find(
      (item) =>
        item.isFlashSale &&
        item.flashSaleSlotId &&
        flashSaleStore.isSlotCancelled(item.flashSaleSlotId),
    ) || null
  )
}

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
        // Đảm bảo mỗi item có price/totalPrice hợp lệ — nếu không thì TÍNH LẠI.
        // Trường hợp totalPrice = 0 (do bug "Mua ngay" từ Flash Sale) → fallback về price * quantity.
        const safeItems = items.map((i) => {
          const safePrice = (i.price || 0) > 0 ? i.price : 0
          const computedTotal =
            (i.totalPrice || 0) > 0 ? i.totalPrice : safePrice * (i.quantity || 1)
          return {
            ...i,
            price: safePrice,
            totalPrice: computedTotal,
          }
        })
        return {
          items: safeItems,
          totalQuantity: safeItems.reduce((sum, i) => sum + (i.quantity || 0), 0),
          totalAmount: safeItems.reduce((sum, i) => sum + (i.totalPrice || 0), 0),
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
const getDefaultShippingData = () => ({
  standardShippingFee: 0,
  discountedShippingFee: 0,
  isFreeship: false,
  isAllowedToOrder: true,
  blockMessage: '',
  amountUntilFreeship: 0,
  suggestionMessage: '',
})

const estimatedDelivery = ref('')
const shippingData = ref(getDefaultShippingData())

// Reset toàn bộ state phí ship + ngày giao dự kiến (dùng khi đổi địa chỉ/tỉnh/huyện/xã)
const resetShippingState = () => {
  shippingData.value = getDefaultShippingData()
  estimatedDelivery.value = ''
}

const discountAmount = ref(0)
const hasFreeshipVoucher = ref(false)
const voucherError = ref('')
const isVoucherLoading = ref(false)

// ─── Modal chọn lại voucher (sau khi backend re-validate fail)
//      Hiển thị danh sách voucher khả dụng để user chọn cái khác thay thế.
const isReSelectVoucherModalOpen = ref(false)

const reSelectVoucherMessage = ref('')
const availableVouchers = ref([])
const isAvailableVouchersLoading = ref(false)
const v2SelectedId = ref(null)

// ─── Modal thông báo voucher không khả dụng (Modal 1 - bước 1)
//      Hiện khi backend re-validate fail khi user bấm Đặt hàng.
//      Bấm "Đồng ý" mới mở Modal 2 (chọn lại voucher).
const isVoucherInvalidModalOpen = ref(false)
const voucherInvalidMessage = ref('')

const openVoucherInvalidModal = (message) => {
  voucherInvalidMessage.value =
    message || 'Voucher hiện không còn khả dụng. Vui lòng chọn voucher khác để tiếp tục thanh toán.'
  isVoucherInvalidModalOpen.value = true
}

const closeVoucherInvalidModal = () => {
  isVoucherInvalidModalOpen.value = false
  voucherInvalidMessage.value = ''
}

const handleVoucherInvalidConfirm = () => {
  const message = voucherInvalidMessage.value
  isVoucherInvalidModalOpen.value = false
  voucherInvalidMessage.value = ''
  openReSelectVoucherModal(message)
}

const VOUCHER_RE_SELECT_CODES = new Set([
  'VOUCHER_INACTIVE',
  'VOUCHER_EXPIRED',
  'VOUCHER_QUOTA_EXHAUSTED',
  'VOUCHER_NOT_FOUND',
])

const isVoucherReSelectCode = (code) => code && VOUCHER_RE_SELECT_CODES.has(code)

// Mã lỗi từ CheckoutService khi Flash Sale bị admin hủy/hết hạn/hết hàng.
// Khi nhận các mã này → hiện modal "Flash Sale bị admin hủy" thay vì toast lỗi.
// Định dạng mã: "FLASH_SALE_CANCELLED|message..." → split('|')[0] để lấy code.
const FLASH_SALE_CANCELLED_CODES = new Set([
  'FLASH_SALE_CANCELLED',
  'FLASH_SALE_ENDED',
  'FLASH_SALE_NOT_STARTED',
  'FLASH_SALE_UNAVAILABLE',
  'FLASH_SALE_INVALID',
  'FLASH_SALE_NOT_FOUND',
  'FLASH_SALE_OUT_OF_STOCK',
])

function isFlashSaleCancelledCode(errorCode) {
  if (!errorCode) return false
  // errorCode có thể là "FLASH_SALE_CANCELLED|message..." → tách lấy phần đầu
  const codeOnly = String(errorCode).split('|')[0].trim()
  return FLASH_SALE_CANCELLED_CODES.has(codeOnly)
}

// Modal thông báo áp dụng voucher thành công
// ─── Re-select voucher modal (mở khi backend re-validate fail) ───
const fetchAvailableVouchers = async () => {
  isAvailableVouchersLoading.value = true
  try {
    const res = await api.get('/client/vouchers/available')
    availableVouchers.value = deduplicateVouchers(res.data?.data ?? res.data ?? [])
  } catch (e) {
    console.error('Lỗi tải danh sách voucher khả dụng:', e)
    availableVouchers.value = []
  } finally {
    isAvailableVouchersLoading.value = false
  }
}

const deduplicateVouchers = (vouchers) => {
  const uniqueVouchers = new Map()
  for (const voucher of Array.isArray(vouchers) ? vouchers : []) {
    const key = voucher.voucherId ?? voucher.voucherCode?.trim().toUpperCase()
    if (key != null && !uniqueVouchers.has(key)) uniqueVouchers.set(key, voucher)
  }
  return [...uniqueVouchers.values()]
}

const openReSelectVoucherModal = (message) => {
  reSelectVoucherMessage.value =
    message || 'Voucher bạn chọn không còn khả dụng. Vui lòng chọn voucher khác.'
  v2SelectedId.value = null
  isReSelectVoucherModalOpen.value = true
  fetchAvailableVouchers()
}

const closeReSelectVoucherModal = () => {
  isReSelectVoucherModalOpen.value = false
  reSelectVoucherMessage.value = ''
  v2SelectedId.value = null
}

const clearAppliedVoucher = () => {
  appliedVoucherCode.value = ''
  discountAmount.value = 0
  hasFreeshipVoucher.value = false
  voucherError.value = ''
  localStorage.removeItem('selectedVoucher')
}

// ─── Modal chọn voucher từ checkout
const isCheckoutVoucherModalOpen = ref(false)

const openVoucherModal = async () => {
  await fetchAvailableVouchers()
  // Pre-select current voucher nếu có
  if (appliedVoucherCode.value) {
    const current = availableVouchers.value.find(
      (v) => v.voucherCode?.toUpperCase() === appliedVoucherCode.value.toUpperCase(),
    )
    if (current) {
      v2SelectedId.value = current.voucherId
    }
  }
  isCheckoutVoucherModalOpen.value = true
}

const closeCheckoutVoucherModal = () => {
  isCheckoutVoucherModalOpen.value = false
  v2SelectedId.value = null
}

// Lưu voucher được chọn vào state + localStorage (dùng chung cho cả 2 modal voucher)
const persistSelectedVoucher = (picked) => {
  appliedVoucherCode.value = picked.voucherCode
  localStorage.setItem(
    'selectedVoucher',
    JSON.stringify({ code: picked.voucherCode, type: picked.discountType }),
  )
}

// Handle voucher confirmation từ VoucherModal (re-select)
const handleVoucherModalConfirm = (picked) => {
  clearAppliedVoucher()
  if (picked) persistSelectedVoucher(picked)
  closeReSelectVoucherModal()
}

// Handle voucher confirmation từ VoucherModal (checkout)
const handleCheckoutVoucherConfirm = (picked) => {
  clearAppliedVoucher()
  if (picked) {
    persistSelectedVoucher(picked)
    previewVoucher() // Preview voucher ngay sau khi chọn
  }
  closeCheckoutVoucherModal()
}

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
  } catch {
    return ''
  }
}

const validatePhone = (phone) => /^(03|05|07|08|09)\d{8}$/.test(phone)

const validateEmail = (email) => {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return re.test(email)
}

// ─── Voucher preview
// Chỉ dùng để hiển thị số tiền giảm trong checkout summary.
// KHÔNG clear voucher code khi fail — voucher chỉ bị reject khi backend validate lúc "Đặt hàng".
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
    } else {
      discountAmount.value = 0
      hasFreeshipVoucher.value = false
      voucherError.value = result?.message || ''
    }
  } catch (e) {
    discountAmount.value = 0
    hasFreeshipVoucher.value = false
    voucherError.value = e.response?.data?.message || 'Không thể xác minh voucher'
  } finally {
    isVoucherLoading.value = false
  }
}

// ─── 1. Phí vận chuyển GHN & Tính toán Freeship
const calculateShippingFee = async () => {
  if (!toDistrictId.value || !toWardCode.value) {
    resetShippingState()
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
  } catch {
    resetShippingState()
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
  resetShippingState()
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
  resetShippingState()
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
  resetShippingState()
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

  // Chặn submit nếu đơn hàng có SP Flash Sale thuộc slot đã bị admin hủy.
  // Hiện modal — user bấm "Đồng ý" sẽ reload trang, khi đó Cart.vue sẽ tự dọn các SP này.
  if (findCancelledFlashSaleItem()) {
    openCancelledModal()
    return
  }

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
    const responseData = error.response?.data
    const errorCode = responseData?.data // ResponseStatusException -> data = errorCode string
    const errorMessage = responseData?.message ?? 'Hệ thống gián đoạn. Vui lòng thử lại.'

    // Nếu backend báo lỗi liên quan đến Flash Sale (slot đã hủy/hết hạn/hết hàng) →
    // mở modal thông báo để user hiểu vấn đề, đồng thời reload để Cart.vue tự dọn SP hỏng.
    // Đây là tuyến phòng thủ cuối cùng phía client — backend đã chặn ngay từ đầu,
    // nhưng FE cần phản hồi thân thiện thay vì hiện toast lỗi khô khan.
    if (isFlashSaleCancelledCode(errorCode)) {
      showCancelledModal.value = true
      return
    }

    // Nếu backend báo voucher không khả dụng -> clear state + mở Modal 1 (thông báo)
    // User bấm "Đồng ý" ở Modal 1 mới mở Modal 2 (chọn voucher khác).
    if (isVoucherReSelectCode(errorCode) && appliedVoucherCode.value) {
      clearAppliedVoucher()
      openVoucherInvalidModal(errorMessage)
      return
    }

    showModal('Lỗi đặt hàng', errorMessage)
  } finally {
    isProcessing.value = false
  }
}

onMounted(async () => {
  await prefillUserEmail()
  await Promise.allSettled([
    fetchGhnProvinces(),
    fetchCart(),
    fetchMyAddresses(),
    // Tải slot FS để phát hiện slot CANCELLED trước khi user bấm Đặt hàng.
    flashSaleStore.fetchClientSlots(20),
  ])
  // Preview voucher nếu có voucher được chọn từ cart
  if (appliedVoucherCode.value) {
    await previewVoucher()
  }

  // Nếu đơn hàng hiện tại có SP Flash Sale thuộc slot bị admin hủy → hiện modal ngay.
  // Guard: bỏ qua nếu user đã xử lý trong phiên này rồi (tránh vòng lặp modal).
  if (findCancelledFlashSaleItem()) {
    openCancelledModal()
  }
})

onBeforeUnmount(() => {
  // Reset cờ khi rời trang để lần sau vào lại modal vẫn hoạt động nếu lỗi còn.
  hasHandledCancelled.value = false
})

// Theo dõi khi clientSlots thay đổi (vd: refresh, scheduler reload) để phát hiện
// slot vừa bị admin hủy trong khi user đang ở trang checkout.
watch(
  () => flashSaleStore.clientSlots,
  () => {
    // Chỉ mở modal khi: chưa xử lý + chưa mở + có item CANCELLED trong đơn
    if (
      !hasHandledCancelled.value &&
      !showCancelledModal.value &&
      findCancelledFlashSaleItem()
    ) {
      openCancelledModal()
    }
  },
  { deep: true },
)

// Watch appliedVoucherCode để tự động preview khi thay đổi
watch(appliedVoucherCode, async (newCode) => {
  if (newCode) {
    await previewVoucher()
  } else {
    discountAmount.value = 0
    hasFreeshipVoucher.value = false
    voucherError.value = ''
  }
})
</script>
