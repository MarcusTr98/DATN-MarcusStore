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
      :reverted-items="priceRevertedItems"
      @close="handleCancelledClose"
      @confirm="handleCancelledConfirm"
      @remove="handleCancelledRemove"
    />

    <!-- Toast cảnh báo giá Flash Sale vừa bị revert do admin hủy -->
    <Transition name="toast-slide">
      <div v-if="showPriceRevertedToast" class="fs-reverted-toast" role="alert">
        <div class="fs-reverted-toast__icon">
          <i class="fas fa-exclamation-triangle"></i>
        </div>
        <div class="fs-reverted-toast__body">
          <strong>Flash Sale đã bị admin hủy</strong>
          <p v-if="priceRevertedItems.length === 1">
            <strong>{{ priceRevertedItems[0].productName }}</strong> đã chuyển từ
            <s>{{ formatPrice(priceRevertedItems[0].oldPrice) }}₫</s>
            → <strong>{{ formatPrice(priceRevertedItems[0].newPrice) }}₫</strong>
          </p>
          <p v-else>
            {{ priceRevertedItems.length }} sản phẩm đã chuyển về giá gốc.
          </p>
        </div>
        <button class="fs-reverted-toast__close" @click="dismissPriceRevertedToast" type="button">
          <i class="fas fa-times"></i>
        </button>
      </div>
    </Transition>

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

        <!-- Marcus thêm: lựa chọn cách khách nhận đơn trước khi nhập địa chỉ. -->
        <div class="checkout-card fulfillment-card mb-3">
          <div class="checkout-card__title fulfillment-card__heading">
            <span class="checkout-card__icon"><i class="fas fa-box-open"></i></span>
            <span>
              <strong>Cách thức nhận hàng</strong>
              <small>Chọn phương án thuận tiện nhất cho bạn</small>
            </span>
          </div>
          <div class="fulfillment-selector" role="radiogroup" aria-label="Cách thức nhận hàng">
            <label
              class="fulfillment-option"
              :class="{ 'fulfillment-option--active': !isStorePickup }"
            >
              <input v-model="fulfillmentMethod" type="radio" value="DELIVERY" />
              <span class="fulfillment-option__icon"><i class="fas fa-truck-fast"></i></span>
              <span class="fulfillment-option__content">
                <strong>Giao tận nơi</strong>
                <small>Nhận hàng tại địa chỉ của bạn</small>
              </span>
              <span class="fulfillment-option__meta">Tính phí GHN</span>
              <i class="fas fa-circle-check fulfillment-option__check"></i>
            </label>
            <label
              class="fulfillment-option"
              :class="{ 'fulfillment-option--active': isStorePickup }"
            >
              <input v-model="fulfillmentMethod" type="radio" value="STORE_PICKUP" />
              <span class="fulfillment-option__icon"><i class="fas fa-store"></i></span>
              <span class="fulfillment-option__content">
                <strong>Nhận tại cửa hàng</strong>
                <small>Đến lấy sau khi cửa hàng xác nhận</small>
              </span>
              <span class="fulfillment-option__meta fulfillment-option__meta--free">Miễn phí</span>
              <i class="fas fa-circle-check fulfillment-option__check"></i>
            </label>
          </div>
        </div>

        <!-- Danh sách địa chỉ đã lưu -->
        <div class="checkout-card mb-3" v-if="!isStorePickup && savedAddresses.length > 0">
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
            {{ isStorePickup ? 'Thông tin người nhận' : 'Thông tin giao hàng' }}
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

            <div class="address-display-box" v-if="!isStorePickup && selectedAddress">
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

            <template v-else-if="!isStorePickup">
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

            <div v-else class="store-pickup-panel">
              <div class="store-pickup-panel__top">
                <span class="store-pickup-panel__icon"><i class="fas fa-store"></i></span>
                <div>
                  <span class="store-pickup-panel__eyebrow">ĐỊA ĐIỂM NHẬN HÀNG</span>
                  <strong>Marcus Store</strong>
                </div>
                <span class="store-pickup-panel__free">0₫</span>
              </div>
              <div class="store-pickup-panel__details">
                <p>
                  <i class="fas fa-location-dot"></i><span>{{ storeInfo.ADDRESS }}</span>
                </p>
                <p v-if="storeInfo.WORKING_HOURS">
                  <i class="far fa-clock"></i><span>{{ storeInfo.WORKING_HOURS }}</span>
                </p>
                <p v-if="storeInfo.HOTLINE">
                  <i class="fas fa-phone"></i
                  ><a :href="`tel:${storeInfo.HOTLINE}`">{{ storeInfo.HOTLINE }}</a>
                </p>
              </div>
              <div class="store-pickup-panel__notice">
                <i class="fas fa-circle-info"></i>
                <span
                  >Cửa hàng sẽ liên hệ khi đơn sẵn sàng. Bạn chỉ cần đến nhận sau khi có thông
                  báo.</span
                >
              </div>
            </div>

            <div class="form-row">
              <div class="form-group form-group--full">
                <label class="form-label"
                  >{{ isStorePickup ? 'Ghi chú cho cửa hàng' : 'Ghi chú cho shipper' }}
                  <span class="optional">(Tùy chọn)</span></label
                >
                <textarea v-model="orderForm.note" class="form-textarea" rows="2"></textarea>
              </div>
            </div>
          </form>
        </div>

        <!-- Phương thức vận chuyển -->
        <div v-if="!isStorePickup" class="checkout-card mt-3">
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

          <!-- Marcus tách: Checkout chỉ giữ state; component con trình bày hai phương thức hợp lệ. -->
          <CheckoutPaymentMethods
            v-model="orderForm.paymentMethod"
            :is-store-pickup="isStorePickup"
          />
        </div>
      </div>

      <!-- Cột hiển thị hóa đơn -->
      <div class="checkout-right">
        <div class="order-summary">
          <div class="order-summary__header">
            <span>Đơn hàng của bạn</span>
            <span class="order-summary__badge">{{ cartData.totalQuantity }} sản phẩm</span>
          </div>

          <!-- Marcus thêm: nhắc lại lựa chọn giao nhận ngay trong phần tổng kết. -->
          <div
            class="fulfillment-summary"
            :class="{ 'fulfillment-summary--pickup': isStorePickup }"
          >
            <span class="fulfillment-summary__icon">
              <i :class="isStorePickup ? 'fas fa-store' : 'fas fa-truck-fast'"></i>
            </span>
            <span>
              <small>Phương thức nhận hàng</small>
              <strong>{{ isStorePickup ? 'Nhận tại Marcus Store' : 'Giao hàng tận nơi' }}</strong>
            </span>
            <button
              type="button"
              @click="fulfillmentMethod = isStorePickup ? 'DELIVERY' : 'STORE_PICKUP'"
            >
              Đổi
            </button>
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

                <!-- Badge cảnh báo giá vừa bị revert do admin hủy FS -->
                <span v-if="item.priceReverted" class="order-item__reverted-badge">
                  <i class="fas fa-exclamation-circle"></i>
                  Giá đã về giá gốc do admin hủy Flash Sale
                  <small v-if="item.priceRevertedInfo?.slotName">
                    ({{ item.priceRevertedInfo.slotName }})
                  </small>
                </span>
              </div>

              <!-- Cột phải: Giá tiền -->
              <div class="order-item__price-col">
                <div class="order-item__current-price">
                  {{ item.totalPrice?.toLocaleString('vi-VN') }}₫
                </div>
                <s
                  v-if="item.originalPrice && item.originalPrice > item.price"
                  class="order-item__original-price"
                >
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
              <span>{{ isStorePickup ? 'Phí nhận tại cửa hàng' : 'Phí vận chuyển (GHN)' }}</span>
              <span v-if="isStorePickup" class="text-success fw-bold">
                <i class="fas fa-store me-1"></i>Miễn phí
              </span>
              <span v-else-if="isFeeLoading" class="text-muted" style="font-size: 12px">
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
              (!isStorePickup && isFeeLoading) ||
              (!isStorePickup && !shippingData.isAllowedToOrder && !!toWardCode)
            "
          >
            <span v-if="!isProcessing">
              <i
                :class="
                  orderForm.paymentMethod === 'VNPAY'
                    ? 'fas fa-arrow-up-right-from-square'
                    : 'fas fa-lock'
                "
                class="me-2"
              ></i>
              {{ orderForm.paymentMethod === 'VNPAY' ? 'Tiếp tục đến VNPAY' : 'Đặt hàng ngay' }}
            </span>
            <span v-else><i class="fas fa-spinner fa-spin me-2"></i>Đang xử lý...</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import BaseModal from '@/components/BaseModal.vue'
import CancelledFlashSaleModal from '@/components/CancelledFlashSaleModal.vue'
import CheckoutPaymentMethods from '@/components/client/checkout/CheckoutPaymentMethods.vue'
import VoucherModal from '@/components/VoucherModal.vue'
import { expandColorName } from '@/utils/colorUtils'
import { useCheckoutPage } from '@/composables/useCheckoutPage'
import '@/assets/css/CheckOut.css'
import '@/assets/css/cart.css'

function formatPrice(value) {
  if (value == null) return '0'
  return Number(value).toLocaleString('vi-VN')
}

// Marcus refactor: Checkout.vue chỉ còn nhiệm vụ kết nối giao diện với luồng checkout.
const {
  modal,
  handleModalConfirm,
  showCancelledModal,
  handleCancelledConfirm,
  handleCancelledRemove,
  // Toast + danh sách cart item bị revert giá
  priceRevertedItems,
  showPriceRevertedToast,
  dismissPriceRevertedToast,
  fulfillmentMethod,
  isStorePickup,
  storeInfo,
  isVoucherInvalidModalOpen,
  voucherInvalidMessage,
  closeVoucherInvalidModal,
  handleVoucherInvalidConfirm,
  isReSelectVoucherModalOpen,
  availableVouchers,
  cartData,
  isAvailableVouchersLoading,
  reSelectVoucherMessage,
  closeReSelectVoucherModal,
  handleVoucherModalConfirm,
  isCheckoutVoucherModalOpen,
  v2SelectedId,
  closeCheckoutVoucherModal,
  handleCheckoutVoucherConfirm,
  savedAddresses,
  activeAddressId,
  applySavedAddress,
  orderForm,
  selectedAddress,
  clearSelectedAddress,
  manualProvinceId,
  onManualProvinceChange,
  ghnProvinces,
  manualDistrictId,
  onManualDistrictChange,
  ghnDistricts,
  manualWardCode,
  onManualWardChange,
  ghnWards,
  detailAddress,
  estimatedDelivery,
  isFeeLoading,
  toWardCode,
  hasFreeshipVoucher,
  shippingData,
  appliedVoucherCode,
  voucherError,
  isVoucherLoading,
  discountAmount,
  openVoucherModal,
  finalAmount,
  isProcessing,
  handleCheckout,
} = useCheckoutPage()
</script>
