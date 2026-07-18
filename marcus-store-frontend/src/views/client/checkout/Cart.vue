<template>
  <div class="cart-wrapper">
    <div class="cart-header">
      <i class="ti ti-shopping-cart cart-title-icon" aria-hidden="true"></i>
      <h2>Giỏ hàng của bạn</h2>
      <span class="count">{{ selectedCount }} sản phẩm</span>
      <div class="cart-header__steps">
        <div class="step step--active">
          <span class="step__dot">1</span>
          <span class="step__label">Giỏ hàng</span>
        </div>
        <div class="step__line"></div>
        <div class="step">
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

    <div v-if="isLoadingCart" class="cart-loading">Đang tải giỏ hàng...</div>

    <div v-else-if="cartError" class="cart-error">
      {{ cartError }}
    </div>

    <div v-else-if="cartItems.length > 0" class="cart-layout">
      <div class="cart-main-content">
        <div class="cart-items">
          <div class="cart-footer">
            <label class="select-all">
              <input v-model="allSelected" type="checkbox" />
              Chọn tất cả
            </label>
            <button class="delete-selected" type="button" @click="deleteChecked">
              <i class="ti ti-trash" aria-hidden="true"></i>
              Xóa đã chọn
            </button>
          </div>

          <div class="cart-item-list">
            <div
              v-for="item in cartItems"
              :key="item.id"
              class="cart-item"
              @click="toggleItem(item)"
            >
              <div class="item-check">
                <input v-model="item.checked" type="checkbox" @click.stop />
              </div>
              <div class="item-info">
                <div class="item-img">
                  <img
                    v-if="item.thumbnailUrl"
                    :src="item.thumbnailUrl"
                    :alt="item.name"
                    class="cart-product-img"
                  />
                  <span v-else class="item-img-placeholder">
                    {{ item.icon }}
                  </span>
                </div>
                <div class="item-details">
                  <div class="item-name">{{ item.name }}</div>
                  <div class="item-variant">{{ expandColorName(item.variant) }}</div>
                  <!-- Badge Flash Sale cho sản phẩm FS -->
                  <span v-if="item.isFlashSale" class="item-badge flash-sale-badge">
                    ⚡ {{ item.flashSaleSlotName || 'Flash Sale' }}
                  </span>
                  <span v-else class="item-badge" :class="{ accessory: item.isAccessory }">
                    {{ item.badge }}
                  </span>
                </div>
              </div>
              <div class="item-price">
                <s>{{ formatPrice(item.originalPrice) }}</s>
                {{ formatPrice(item.price) }}
              </div>
              <div class="qty-control" @click.stop>
                <button
                  class="qty-btn"
                  type="button"
                  :disabled="item.quantity <= 1 || isLoadingCart"
                  @click="changeQty(item, -1)"
                >
                  -
                </button>

                <input
                  v-model.number="item.quantity"
                  class="qty-num"
                  min="1"
                  :max="item.stockQuantity"
                  type="number"
                  :disabled="isLoadingCart"
                  @change="normalizeQty(item)"
                  @click.stop
                />

                <button
                  class="qty-btn"
                  type="button"
                  :disabled="isLoadingCart"
                  @click="changeQty(item, 1)"
                >
                  +
                </button>
              </div>
              <div class="item-total">{{ formatPrice(item.price * item.quantity) }}</div>
              <button
                class="item-remove"
                type="button"
                aria-label="Xóa sản phẩm"
                @click.stop="removeItem(item.skuId)"
              >
                <i class="ti ti-trash" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>

        <div class="suggested">
          <div class="suggested-header">
            <div class="suggested-title">
              <i class="ti ti-gift" aria-hidden="true"></i>
              Phụ kiện nên mua kèm ưu đãi giá sốc
            </div>
            <button class="suggested-more" type="button" @click="goToAccessories">
              Xem thêm
              <i class="ti ti-chevron-right" aria-hidden="true"></i>
            </button>
          </div>

          <div class="suggested-carousel">
            <button
              class="suggested-nav suggested-nav-prev"
              type="button"
              aria-label="Xem phụ kiện trước"
              @click="scrollAccessories(-1)"
            >
              <i class="ti ti-chevron-left" aria-hidden="true"></i>
            </button>

            <div id="productTrack" ref="suggestedTrack" class="suggested-track product-track">
              <div
                v-for="accessory in accessories"
                :key="accessory.id"
                class="sug-card product-card"
              >
                <div>
                  <div class="sug-img">{{ accessory.icon }}</div>
                  <div class="sug-name">{{ accessory.name }}</div>
                  <div class="sug-price">{{ formatPrice(accessory.price) }}</div>
                </div>
                <button class="sug-add" type="button" @click="addAccessoryToCart(accessory)">
                  <i class="ti ti-plus" aria-hidden="true"></i>
                  Thêm vào giỏ
                </button>
              </div>
            </div>

            <button
              class="suggested-nav suggested-nav-next"
              type="button"
              aria-label="Xem thêm phụ kiện"
              @click="scrollAccessories(1)"
            >
              <i class="ti ti-chevron-right" aria-hidden="true"></i>
            </button>
          </div>
        </div>
      </div>

      <div class="cart-sidebar-content">
        <div class="cart-summary">
          <div class="summary-title">
            <i class="ti ti-receipt" aria-hidden="true"></i>
            Tóm tắt đơn hàng
          </div>

          <div class="voucher-row">
            <span class="voucher-label-trigger">
              <i class="ti ti-ticket" aria-hidden="true"></i>
              Marcus Store Voucher
            </span>
            <div v-if="voucherCode" class="voucher-selected-info">
              <div class="voucher-selected-code">
                <span class="voucher-selected-code__text">ID voucher: {{ voucherCode }}</span>
                <span
                  v-if="selectedVoucherType === 'FREESHIP'"
                  class="voucher-selected-badge voucher-selected-badge--freeship"
                  title="Voucher miễn phí vận chuyển"
                >
                  <i class="ti ti-truck-delivery" aria-hidden="true"></i>
                  FREESHIP
                </span>
                <span
                  v-else-if="selectedVoucherType === 'PERCENT'"
                  class="voucher-selected-badge voucher-selected-badge--percent"
                >
                  <i class="ti ti-percentage" aria-hidden="true"></i>
                  PERCENT
                </span>
              </div>
              <button class="open-voucher-btn" type="button" @click="isVoucherModalOpen = true">
                Đổi mã
                <i class="ti ti-chevron-right" aria-hidden="true"></i>
              </button>
            </div>
            <button
              v-else
              class="open-voucher-btn"
              type="button"
              @click="isVoucherModalOpen = true"
            >
              Chọn mã giảm giá
              <i class="ti ti-chevron-right" aria-hidden="true"></i>
            </button>
          </div>

          <div class="summary-rows">
            <div class="summary-row">
              <span class="label">Tạm tính</span>
              <span class="value">{{ formatPrice(originalTotal) }}</span>
            </div>
            <div class="summary-row">
              <span class="label">Giảm giá sản phẩm</span>
              <span class="value discount">-{{ formatPrice(productDiscount) }}</span>
            </div>
            <div class="summary-row">
              <span class="label">
                Voucher Marcus Store
                <span
                  v-if="selectedVoucherType === 'FREESHIP' && voucherCode"
                  class="voucher-selected-badge voucher-selected-badge--freeship voucher-selected-badge--inline"
                  title="Voucher miễn phí vận chuyển"
                >
                  <i class="ti ti-truck-delivery" aria-hidden="true"></i>
                  FREESHIP
                </span>
              </span>
              <span class="value discount"> -{{ formatPrice(voucherDiscount) }} </span>
            </div>
          </div>
          <div class="summary-total">
            <div>
              <span class="total-label">Tổng thanh toán</span>
              <span class="vat-note">(Đã bao gồm VAT)</span>
            </div>
            <div class="total-price">{{ formatPrice(totalPayment) }}</div>
          </div>
          <button class="checkout-btn" type="button" @click="handleCheckout">
            Tiến hành thanh toán
          </button>
          <div class="guarantee-strip">
            <div class="guarantee-item">
              <i class="ti ti-shield-check" aria-hidden="true"></i>
              <span>Bảo hành<br />chính hãng</span>
            </div>
            <div class="guarantee-item">
              <i class="ti ti-refresh" aria-hidden="true"></i>
              <span>Đổi trả<br />30 ngày</span>
            </div>
            <div class="guarantee-item">
              <i class="ti ti-truck" aria-hidden="true"></i>
              <span>Giao hàng<br />nhanh 2h</span>
            </div>
            <div class="guarantee-item">
              <i class="ti ti-credit-card" aria-hidden="true"></i>
              <span>Thanh toán<br />an toàn</span>
            </div>
          </div>
        </div>

        <div class="ms-side-widget">
          <div class="ms-widget-title">
            <i class="ti ti-credit-card" aria-hidden="true"></i>
            Ưu đãi thanh toán đối tác
          </div>
          <div class="ms-promo-box">
            Giảm ngay <strong>500.000đ</strong> khi mở thẻ tín dụng VIB hoặc thanh toán qua VNPAY-QR
            (Nhập mã <code>MARCUS500</code>).
          </div>
          <div class="ms-promo-box">
            Hỗ trợ <strong>Trả góp 0%</strong> lãi suất qua thẻ tín dụng hoặc công ty tài chính
            duyệt nhanh 5 phút.
          </div>
        </div>

        <div class="ms-side-widget">
          <div class="ms-widget-title">
            <i class="ti ti-headset" aria-hidden="true"></i>
            Bạn cần hỗ trợ tư vấn?
          </div>
          <div class="ms-support-item">
            <div class="ms-support-icon">
              <i class="ti ti-phone-call" aria-hidden="true"></i>
            </div>
            <div class="ms-support-info">
              <span>Tư vấn mua hàng (Miễn phí)</span>
              <strong>1900 6098</strong> (8:00 - 22:00)
            </div>
          </div>
          <div class="ms-support-item">
            <div class="ms-support-icon ms-support-chat">
              <i class="ti ti-brand-hipchat" aria-hidden="true"></i>
            </div>
            <div class="ms-support-info">
              <span>Chat hỗ trợ trực tuyến</span>
              <strong class="ms-support-link">Liên hệ qua Zalo / Messenger</strong>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="empty-cart" v-else>
      <div class="empty-cart-icon">
        <i class="ti ti-shopping-cart-x"></i>
      </div>
      <h3>Giỏ hàng của bạn đang trống</h3>
      <p>Hãy tìm thêm những sản phẩm yêu thích và thêm vào giỏ hàng nhé!</p>
      <button class="continue-shopping-btn" type="button" @click="goToProducts">
        Tiếp tục mua sắm
      </button>
    </div>

    <!-- Voucher Modal V2 (Dat's update) -->
    <div
      class="v-modal-overlay"
      :class="{ active: isVoucherModalOpen }"
      @click.self="isVoucherModalOpen = false"
    >
      <div class="v-modal-card">
        <div class="v-modal-header">
          <div class="v2-header-text">
            <h3>Chọn 1 Voucher Áp Dụng</h3>
            <p class="v2-subtitle">Hệ thống tự động chọn mã có giá trị giảm cao nhất cho bạn</p>
          </div>
          <button class="close-btn" type="button" @click="isVoucherModalOpen = false">
            &times;
          </button>
        </div>

        <div class="v-modal-body v2-modal-body">
          <div class="v2-voucher-list">
            <div
              v-for="voucher in v2ActiveVouchers"
              :key="voucher.id"
              class="v2-voucher-card"
              :class="{ selected: v2SelectedId === voucher.id }"
              @click="selectVoucher(voucher.id)"
            >
              <div class="v2-badge-best" v-if="voucher.isBest">Tốt Nhất</div>

              <div class="v2-voucher-left">
                <div class="v2-icon-box" :class="voucher.iconClass">
                  <i :class="voucher.icon" aria-hidden="true"></i>
                </div>
              </div>

              <div class="v2-voucher-info">
                <span class="v2-code">ID voucher: {{ voucher.voucherCode }}</span>
                <span class="v2-title">{{ voucher.title }}</span>
                <span class="v2-min-order"
                  >Đơn tối thiểu {{ formatPriceVnd(voucher.minOrder) }}</span
                >
                <span class="v2-expiry" :class="{ urgent: voucher.expiryUrgent }">
                  {{ voucher.expiryLabel }}
                </span>
              </div>

              <div class="v2-voucher-action" @click.stop>
                <input
                  type="radio"
                  name="voucher-radio"
                  :checked="v2SelectedId === voucher.id"
                  @change="selectVoucher(voucher.id)"
                />
              </div>
            </div>

            <div
              v-for="voucher in v2DisabledVouchers"
              :key="voucher.id"
              class="v2-voucher-card v2-voucher-card--disabled"
            >
              <div class="v2-voucher-left">
                <div class="v2-icon-box" :class="voucher.iconClass">
                  <i :class="voucher.icon" aria-hidden="true"></i>
                </div>
              </div>

              <div class="v2-voucher-info">
                <span class="v2-code">ID voucher: {{ voucher.voucherCode }}</span>
                <span class="v2-title">{{ voucher.title }}</span>
                <span class="v2-min-order"
                  >Đơn tối thiểu {{ formatPriceVnd(voucher.minOrder) }}</span
                >
                <span class="v2-expiry" :class="{ urgent: voucher.expiryUrgent }">
                  {{ voucher.expiryLabel }}
                </span>
              </div>

              <div class="v2-voucher-action v2-voucher-action--disabled">
                <input type="radio" name="voucher-radio" disabled />
                <span class="v2-disabled-reason">{{ voucher.disabledReason }}</span>
              </div>
            </div>

            <div v-if="v2ActiveVouchers.length === 0" class="v2-voucher-empty">
              <i class="ti ti-ticket-off" aria-hidden="true"></i>
              <p>Không có voucher khả dụng</p>
            </div>
          </div>
        </div>

        <div class="v2-footer-bar">
          <button
            class="v-btn v-btn-primary v2-confirm-btn"
            type="button"
            @click="applySelectedVoucher"
          >
            ĐỒNG Ý
          </button>
        </div>
      </div>
    </div>

    <!-- Alert Modal (Dat's UI) -->
    <div
      class="v-modal-overlay"
      :class="{ active: isAlertModalOpen }"
      @click.self="isAlertModalOpen = false"
    >
      <div class="alert-modal-card">
        <div class="alert-modal-icon">
          <svg
            width="48"
            height="48"
            viewBox="0 0 48 48"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M22.314 5.286L4.286 35.286C3.428 36.8 4.514 38.628 6.228 38.628H41.772C43.486 38.628 44.572 36.8 43.714 35.286L25.686 5.286C24.828 3.772 23.172 3.772 22.314 5.286Z"
              stroke="#E11D1D"
              stroke-width="2.5"
              stroke-linejoin="round"
              fill="none"
            />
            <path d="M24 17V25" stroke="#E11D1D" stroke-width="2.8" stroke-linecap="round" />
            <circle cx="24" cy="31" r="1.8" fill="#E11D1D" />
          </svg>
        </div>
        <div class="alert-modal-body">
          <h3 class="alert-modal-title">Thông báo</h3>
          <p class="alert-modal-message">{{ alertModalMessage }}</p>
        </div>
        <div class="alert-modal-footer">
          <button class="alert-modal-confirm-btn" type="button" @click="isAlertModalOpen = false">
            Đồng ý
          </button>
        </div>
      </div>
    </div>

    <!-- Modal thông báo Flash Sale đã bị admin hủy (dùng chung component) -->
    <CancelledFlashSaleModal
      :visible="showCancelledModal"
      @close="showCancelledModal = false"
      @confirm="handleCancelledConfirm"
    />

    <!-- Modal yêu cầu đăng nhập -->
    <LoginRequiredModal
      :visible="showLoginRequiredModal"
      :title="loginRequiredTitle"
      :message="loginRequiredMessage"
      @close="showLoginRequiredModal = false"
    />

    <!-- Modal thông báo áp dụng voucher thành công -->
    <div
      class="v-modal-overlay"
      :class="{ active: isVoucherSuccessModalOpen }"
      @click.self="closeVoucherSuccessModal"
    >
      <div
        class="alert-modal-card voucher-success-modal"
        :class="`voucher-success-modal--${voucherSuccessType.toLowerCase()}`"
      >
        <div class="alert-modal-icon voucher-success-modal__icon">
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
        <div class="alert-modal-body">
          <h3 class="alert-modal-title">Áp dụng voucher thành công</h3>
          <p class="alert-modal-message">{{ voucherSuccessMessage }}</p>
          <div class="voucher-success-modal__highlight" v-if="voucherSuccessType === 'FREESHIP'">
            <i class="ti ti-truck-delivery" aria-hidden="true"></i>
            <span
              >Miễn phí vận chuyển tối đa
              <strong>{{ formatPrice(voucherSuccessAmount) }}</strong></span
            >
          </div>
          <div
            class="voucher-success-modal__highlight"
            v-else-if="voucherSuccessType === 'PERCENT'"
          >
            <i class="ti ti-percentage" aria-hidden="true"></i>
            <span
              >Giảm <strong>{{ voucherSuccessPercent }}%</strong>
              <template v-if="voucherSuccessMaxDiscount > 0">
                (tối đa {{ formatPrice(voucherSuccessMaxDiscount) }})
              </template>
              cho đơn hàng
            </span>
          </div>
          <p class="voucher-success-modal__note">
            <i class="ti ti-info-circle" aria-hidden="true"></i>
            Vui lòng kiểm tra lại thông tin voucher trước khi tiến hành thanh toán.
          </p>
        </div>
        <div class="alert-modal-footer">
          <button
            class="alert-modal-confirm-btn voucher-success-modal__btn"
            type="button"
            @click="closeVoucherSuccessModal"
          >
            Đồng ý
          </button>
        </div>
      </div>
    </div>

    <div class="cart-toast" :class="{ active: isToastVisible }">
      <i class="ti ti-circle-check" aria-hidden="true"></i>
      <span>{{ toastMessage }}</span>
      <button class="cart-toast-close" type="button" aria-label="Đóng thông báo" @click="hideToast">
        &times;
      </button>
      <div :key="toastKey" class="cart-toast-progress"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cartStore'
import { useFlashSaleStore } from '@/stores/FlashSaleStore'
import CancelledFlashSaleModal from '@/components/CancelledFlashSaleModal.vue'
import LoginRequiredModal from '@/components/LoginRequiredModal.vue'
import { expandColorName } from '@/utils/colorUtils'
import '@/assets/css/cart.css'
import voucherApiClient from '@/api/voucherApiClient.js'
const router = useRouter()
const cartStore = useCartStore()
const flashSaleStore = useFlashSaleStore()

// ==== Flash Sale bị admin hủy ====
// Khi giỏ hàng chứa SP FS từ slot đã CANCELLED → hiện modal thông báo + reload trang.
// Khi user bấm "Đồng ý" sẽ xóa luôn các SP FS bị hủy khỏi giỏ rồi reload.
const showCancelledModal = ref(false)

// Cờ chặn vòng lặp modal bật liên tục: khi user đã xác nhận xử lý FS bị hủy,
// không tự động mở lại modal trong cùng 1 phiên trang. Reset khi F5 hoặc đổi route.
const hasHandledCancelled = ref(false)

// ==== Modal yêu cầu đăng nhập (khi guest nhận 401) ====
const showLoginRequiredModal = ref(false)
const loginRequiredTitle = ref('Đăng nhập để tiếp tục')
const loginRequiredMessage = ref('Vui lòng đăng nhập để truy cập giỏ hàng và thanh toán.')

function findCancelledFlashSaleItem() {
  const slots = flashSaleStore.clientSlots
  if (!Array.isArray(slots)) return null
  return cartItems.value.find(
    (item) =>
      item.isFlashSale &&
      item.flashSaleSlotId &&
      flashSaleStore.isSlotCancelled(item.flashSaleSlotId),
  ) || null
}

async function handleCancelledConfirm() {
  // Đánh dấu "đã xử lý" NGAY LẬP TỨC để chặn vòng lặp vô tận.
  // Nếu để nguyên thì:
  //   1. User bấm "Đồng ý" → modal đóng
  //   2. reload() chạy → onMounted chạy lại → tìm lại thấy SP CANCELLED → modal mở lại
  //   3. Vòng lặp → modal bật/tắt liên tục.
  // Cờ hasHandledCancelled chỉ reset khi F5 thật sự hoặc đổi route, không bị trigger
  // bởi onMounted của chính lần redirect này.
  if (hasHandledCancelled.value) return
  hasHandledCancelled.value = true

  // Thu gom SKU id của các SP FS thuộc slot bị hủy để xóa khỏi giỏ.
  const cancelledSkuIds = cartItems.value
    .filter(
      (item) =>
        item.isFlashSale &&
        item.flashSaleSlotId &&
        flashSaleStore.isSlotCancelled(item.flashSaleSlotId),
    )
    .map((item) => item.skuId)
  if (cancelledSkuIds.length) {
    try {
      await cartStore.removeManyItemFromCart(cancelledSkuIds)
    } catch (e) {
      console.warn('Không thể xóa SP FS bị hủy khỏi giỏ:', e)
    }
  }

  // Đóng modal trước, rồi chuyển về trang chủ.
  // Dùng router.push thay vì window.location.reload() vì:
  //   - Tránh reload toàn trang (UX mượt hơn, giữ SPA state)
  //   - User rời khỏi /cart → / nên không trigger lại onMounted của Cart.vue
  //   - Ngay cả khi user back lại /cart, modal không tự bật vì hasHandledCancelled = true
  //     và cartItems không còn SP CANCELLED (đã xóa ở trên).
  showCancelledModal.value = false
  await router.replace({ path: '/' }).catch(() => {
    // Fallback nếu router có vấn đề → reload trang chủ
    window.location.href = '/'
  })
}

onMounted(async () => {
  // Xóa dữ liệu checkout cũ nếu có
  localStorage.removeItem('selectedCartItems')
  localStorage.removeItem('selectedSubtotal')
  // Giữ lại selectedVoucherCode để user vẫn thấy voucher đã chọn

  // Lắng nghe event auth-required (khi guest nhận 401 từ API)
  window.addEventListener('auth-required', handleAuthRequired)

  // Tải song song cả cart và danh sách slot FS để check slot CANCELLED sớm nhất có thể.
  await Promise.all([cartStore.fetchCart(), flashSaleStore.fetchClientSlots(20)])
  await fetchAvailableVouchers()

  // Khôi phục trạng thái checkbox từ localStorage (nếu có)
  restoreSelectedItems()

  // Nếu giỏ đã có SP FS thuộc slot bị hủy (do admin hủy sau khi user thêm vào giỏ) → hiện modal.
  // Guard: bỏ qua nếu user đã xử lý trong phiên này rồi.
  if (!hasHandledCancelled.value && findCancelledFlashSaleItem()) {
    showCancelledModal.value = true
  }
})

// Theo dõi khi clientSlots thay đổi (vd: scheduler reload, refresh thủ công...) để phát hiện
// slot vừa bị admin hủy trong khi user đang ở trang giỏ hàng.
watch(
  () => flashSaleStore.clientSlots,
  () => {
    if (
      !showCancelledModal.value &&
      !hasHandledCancelled.value &&
      findCancelledFlashSaleItem()
    ) {
      showCancelledModal.value = true
    }
  },
  { deep: true },
)

// Khi component bị huỷ (user rời /cart hoặc SPA remount), reset cờ để lần sau
// quay lại /cart mà vẫn còn SP CANCELLED thì modal có thể hiện lại.
onBeforeUnmount(() => {
  hasHandledCancelled.value = false
  // Cleanup event listener
  window.removeEventListener('auth-required', handleAuthRequired)
})

// ==== Event listener cho auth-required (từ api interceptor khi guest nhận 401) ====
function handleAuthRequired(event) {
  loginRequiredTitle.value = event.detail?.title || 'Đăng nhập để tiếp tục'
  loginRequiredMessage.value = event.detail?.message || 'Vui lòng đăng nhập để truy cập giỏ hàng và thanh toán.'
  showLoginRequiredModal.value = true
}

// Khôi phục trạng thái checkbox từ localStorage
function restoreSelectedItems() {
  const savedItems = localStorage.getItem('selectedCartItems')
  if (!savedItems) return

  try {
    const selectedItems = JSON.parse(savedItems)
    const selectedIds = new Set(selectedItems.map((i) => i.cartItemId))

    // Uncheck các sản phẩm không có trong danh sách đã chọn
    cartStore.items.forEach((item) => {
      if (item.cartItemId && !selectedIds.has(item.cartItemId)) {
        item.checked = false
      }
    })
  } catch (e) {
    console.warn('Lỗi restoreSelectedItems:', e)
  }
}

const loading = ref(false)
const error = ref(null)
const availableVouchers = ref([])
async function fetchAvailableVouchers() {
  try {
    loading.value = true
    error.value = null
    const response = await voucherApiClient.getAllVoucherClient()
    availableVouchers.value = deduplicateVouchers(response.data)
  } catch (e) {
    error.value = 'không thể lấy voucher'
    console.error(e)
  } finally {
    loading.value = false
  }
}

function deduplicateVouchers(vouchers) {
  const uniqueVouchers = new Map()
  for (const voucher of Array.isArray(vouchers) ? vouchers : []) {
    const key = voucher.voucherId ?? voucher.voucherCode?.trim().toUpperCase()
    if (key != null && !uniqueVouchers.has(key)) uniqueVouchers.set(key, voucher)
  }
  return [...uniqueVouchers.values()]
}

const cartItems = computed(() => cartStore.items)
const isLoadingCart = computed(() => cartStore.loading)
const cartError = computed(() => cartStore.error)

const isVoucherModalOpen = ref(false)
const selectedVoucher = ref(0)
const selectedVoucherType = ref('AMOUNT') // 'AMOUNT', 'PERCENT', 'FREESHIP'
const selectedVoucherMaxDiscount = ref(0) // Số tiền giảm tối đa cho voucher PERCENT
const voucherCode = ref('')
const isAlertModalOpen = ref(false)
const alertModalMessage = ref('')
// Modal thông báo áp dụng voucher thành công (áp dụng cho tất cả loại voucher)
const isVoucherSuccessModalOpen = ref(false)
const voucherSuccessMessage = ref('')
const voucherSuccessType = ref('AMOUNT')
const voucherSuccessAmount = ref(0)
const voucherSuccessPercent = ref(0)
const voucherSuccessMaxDiscount = ref(0)
const isToastVisible = ref(false)
const toastMessage = ref('')
const toastKey = ref(0)

function formatPrice(value) {
  return `${Number(value || 0).toLocaleString('vi-VN')}đ`
}

async function updateItemQuantity(item, newQuantity) {
  const quantity = Math.max(Number(newQuantity) || 1, 1)
  if (item.stockQuantity && quantity > item.stockQuantity) {
    showAlert('số lượng đã vượt quá trong kho')
    await cartStore.fetchCart()
    return
  }

  const success = await cartStore.updateItemQuantity(item.skuId, quantity)
  if (!success) {
    showAlert(cartError.value || 'cập nhật số lượng thất bại')
    await cartStore.fetchCart()
  }
}

async function normalizeQty(item) {
  await updateItemQuantity(item, item.quantity)
}

async function changeQty(item, delta) {
  const newQuantity = item.quantity + delta
  if (newQuantity < 1) {
    return
  }
  if (item.stockQuantity && newQuantity > item.stockQuantity) {
    showAlert('Số lượng nhập vượt quá số lượng trong kho')
    return
  }
  await updateItemQuantity(item, newQuantity)
}

let toastTimer = null
const suggestedTrack = ref(null)

const v2SelectedId = ref(null)

// Thêm computed cho voucher từ API
const v2Vouchers = computed(() => {
  return availableVouchers.value.map((v) => {
    // Xử lý discountType - đảm bảo so sánh đúng
    const discountType = (v.discountType || '').toUpperCase()
    const isAmount = discountType === 'AMOUNT'
    const isPercent = discountType === 'PERCENT'
    const isFreeship = discountType === 'FREESHIP'

    // Tính giá trị giảm dựa trên loại
    let title = ''
    let discountValue = 0
    let discountPercent = 0

    if (isAmount) {
      title = `Giảm ${formatPriceVnd(v.discountValue)} toàn sàn`
      discountValue = v.discountValue
    } else if (isPercent) {
      title = `Giảm ${v.discountValue}% toàn sàn`
      discountPercent = v.discountValue
    } else if (isFreeship) {
      title = `Miễn phí vận chuyển ${formatPriceVnd(v.discountValue)}`
      discountValue = v.discountValue
    } else {
      // Fallback - coi như AMOUNT nếu không xác định được
      title = `Giảm ${formatPriceVnd(v.discountValue)} toàn sàn`
      discountValue = v.discountValue
    }

    return {
      id: v.voucherId,
      voucherCode: v.voucherCode,
      title,
      discountType,
      minOrder: v.minOrderValue,
      discountValue,
      discountPercent,
      maxDiscountAmount: v.maxDiscountAmount || 0,
      expiryLabel: `Hạn dùng đến: ${new Date(v.endDate).toLocaleDateString('vi-VN')}`,
      expiryUrgent: false,
      icon: isFreeship ? 'bi bi-truck' : isPercent ? 'bi bi-cash' : 'bi bi-cash',
      iconClass: isFreeship
        ? 'v2-icon-box--freeship'
        : isPercent
          ? 'v2-icon-box--percent'
          : 'v2-icon-box--amount',
      tag: 'Marcus Store',
      active: isVoucherActive(v),
      disabledReason: getDisabledReason(v),
      isBest: false,
    }
  })
})

// Kiểm tra voucher có thể dùng không
function isVoucherActive(voucher) {
  const cartTotal = subtotal.value
  return voucher.isActive && !voucher.isUsed && cartTotal >= (voucher.minOrderValue || 0)
}

// Lý do voucher bị disable
function getDisabledReason(voucher) {
  const cartTotal = subtotal.value
  if (!voucher.isActive) return 'Voucher không còn hoạt động'
  if (voucher.isUsed) return 'Voucher đã được sử dụng'
  if (cartTotal < (voucher.minOrderValue || 0)) {
    const needed = (voucher.minOrderValue || 0) - cartTotal
    return `Chưa đủ điều kiện: Mua thêm ${formatPriceVnd(needed)}`
  }
  return ''
}

const v2ActiveVouchers = computed(() =>
  v2Vouchers.value.filter((v) => v.active).sort((a, b) => b.discountValue - a.discountValue),
)

const v2DisabledVouchers = computed(() => v2Vouchers.value.filter((v) => !v.active))

function selectVoucher(id) {
  v2SelectedId.value = v2SelectedId.value === id ? null : id
}

function applySelectedVoucher() {
  const picked = v2Vouchers.value.find((v) => v.id === v2SelectedId.value)
  if (picked) {
    selectedVoucher.value =
      picked.discountPercent > 0 ? picked.discountPercent : picked.discountValue
    selectedVoucherType.value = picked.discountType
    voucherCode.value = picked.voucherCode
    selectedVoucherMaxDiscount.value = picked.maxDiscountAmount || 0
  } else if (!voucherCode.value.trim()) {
    selectedVoucher.value = 0
    selectedVoucherType.value = 'AMOUNT'
    voucherCode.value = ''
    selectedVoucherMaxDiscount.value = 0
  }
  isVoucherModalOpen.value = false

  // Hiển thị modal thông báo áp dụng thành công cho MỌI loại voucher
  if (picked) {
    openVoucherSuccessModal(picked)
  }
}

function openVoucherSuccessModal(picked) {
  voucherSuccessType.value = picked.discountType || 'AMOUNT'
  voucherSuccessAmount.value = Number(picked.discountValue) || 0
  voucherSuccessPercent.value = Number(picked.discountPercent) || 0
  voucherSuccessMaxDiscount.value = Number(picked.maxDiscountAmount) || 0
  voucherSuccessMessage.value = `Voucher ${picked.voucherCode} đã được áp dụng thành công vào đơn hàng của bạn.`
  isVoucherSuccessModalOpen.value = true
}

function closeVoucherSuccessModal() {
  isVoucherSuccessModalOpen.value = false
  voucherSuccessMessage.value = ''
  voucherSuccessAmount.value = 0
  voucherSuccessPercent.value = 0
  voucherSuccessMaxDiscount.value = 0
  voucherSuccessType.value = 'AMOUNT'
}

function formatPriceVnd(value) {
  return `${Number(value || 0).toLocaleString('vi-VN')}đ`
}

const accessories = [
  {
    id: 'item-sac-anker',
    name: 'Củ sạc nhanh Anker Nano GaN 30W',
    variant: 'Trắng | Bảo hành 18 tháng',
    icon: '30W',
    price: 350000,
    originalPrice: 450000,
  },
  {
    id: 'item-cl-iphone-12',
    name: 'Kính cường lực iPhone 12 Pro Max Kingkong',
    variant: 'Hộp sắt | Chống vân tay',
    icon: 'Glass',
    price: 180000,
    originalPrice: 250000,
  },
  {
    id: 'item-cl-iphone-13',
    name: 'Kính cường lực iPhone 13 Pro Max Kingkong',
    variant: 'Hộp sắt | Chống vân tay',
    icon: 'Glass',
    price: 200000,
    originalPrice: 250000,
  },
  {
    id: 'item-cl-iphone-14',
    name: 'Kính cường lực iPhone 14 Pro Max Kingkong',
    variant: 'Hộp sắt | Chống vân tay',
    icon: 'Glass',
    price: 5000000,
    originalPrice: 250000,
  },
  {
    id: 'item-tui-tomtoc',
    name: 'Túi chống sốc Laptop/Macbook Tomtoc 13 inch',
    variant: 'Màu Xám | Kháng nước CornerArmor',
    icon: 'Tui',
    price: 790000,
    originalPrice: 950000,
  },
  {
    id: 'item-chuot-logi',
    name: 'Chuột không dây Silent Logitech M220',
    variant: 'Đen | Kết nối USB receiver',
    icon: 'Mouse',
    price: 299000,
    originalPrice: 390000,
  },
]

const selectedItems = computed(() => cartItems.value.filter((item) => item.checked))

const selectedCount = computed(() =>
  selectedItems.value.reduce((total, item) => total + item.quantity, 0),
)

const originalTotal = computed(() =>
  selectedItems.value.reduce((total, item) => total + item.originalPrice * item.quantity, 0),
)

const subtotal = computed(() =>
  selectedItems.value.reduce((total, item) => total + item.price * item.quantity, 0),
)

const productDiscount = computed(() => Math.max(originalTotal.value - subtotal.value, 0))

// Tính giảm giá voucher dựa trên loại
const voucherDiscount = computed(() => {
  if (!selectedVoucher.value || !selectedVoucherType.value) return 0

  if (selectedVoucherType.value === 'PERCENT') {
    // PERCENT: giảm theo % của subtotal
    let discount = subtotal.value * (selectedVoucher.value / 100)
    // Áp dụng cap maxDiscountAmount nếu có
    if (selectedVoucherMaxDiscount.value > 0 && discount > selectedVoucherMaxDiscount.value) {
      discount = selectedVoucherMaxDiscount.value
    }
    return Math.floor(discount)
  }

  // AMOUNT hoặc FREESHIP: giảm trực tiếp số tiền
  return selectedVoucher.value || 0
})

const totalPayment = computed(() => Math.max(subtotal.value - voucherDiscount.value, 0))

const allSelected = computed({
  get() {
    return cartItems.value.length > 0 && cartItems.value.every((item) => item.checked)
  },
  set(value) {
    cartItems.value.forEach((item) => {
      item.checked = value
    })
  },
})

function toggleItem(item) {
  item.checked = !item.checked
}

async function removeItem(skuId) {
  const success = await cartStore.removeItemFromCart(skuId)
  if (success) {
    showToast('xóa thành công')
  } else {
    showAlert(cartStore.error || 'xóa sản phẩm thất bại')
  }
}

async function deleteChecked() {
  const checkedItems = cartItems.value.filter((item) => item.checked)
  if (checkedItems.length === 0) {
    showAlert('vui lòng chọn 1 sản phẩm để xóa')
    return
  }
  const skuIds = checkedItems.map((item) => item.skuId)
  const success = await cartStore.removeManyItemFromCart(skuIds)
  if (success) {
    showToast('xóa các sản phẩm thành công')
  } else {
    showAlert(cartStore.error || 'xóa thất bại')
  }
}

function showAlert(message) {
  alertModalMessage.value = message
  isAlertModalOpen.value = true
}

function showToast(message) {
  toastMessage.value = message
  isToastVisible.value = true
  toastKey.value += 1

  if (toastTimer) {
    clearTimeout(toastTimer)
  }

  toastTimer = setTimeout(() => {
    hideToast()
  }, 3000)
}

function hideToast() {
  isToastVisible.value = false

  if (toastTimer) {
    clearTimeout(toastTimer)
    toastTimer = null
  }
}

function addAccessoryToCart(accessory) {
  const existingItem = cartItems.value.find((item) => item.id === accessory.id)

  if (existingItem) {
    existingItem.quantity += 1
    existingItem.checked = true
    showToast('Thêm vào giỏ hàng thành công')
    return
  }

  cartItems.value.push({
    ...accessory,
    badge: 'Mua kèm giá sốc',
    quantity: 1,
    checked: true,
    isAccessory: true,
  })
  showToast('Thêm vào giỏ hàng thành công')
}

function goToProducts() {
  window.location.href = '/category/dien-thoai'
}

function goToAccessories() {
  window.location.href = '/category/phu-kien'
}

function scrollAccessories(direction) {
  if (!suggestedTrack.value) return

  const card = suggestedTrack.value.querySelector('.sug-card')
  const cardWidth = card ? card.offsetWidth : 240
  const scrollAmount = cardWidth + 20

  suggestedTrack.value.scrollBy({
    left: direction * scrollAmount,
    behavior: 'smooth',
  })
}

function handleCheckout() {
  if (selectedCount.value === 0) {
    showAlert('Vui lòng chọn ít nhất một sản phẩm để thanh toán')
    return
  }

  // Chặn checkout nếu giỏ có SP thuộc Flash Sale đã bị admin hủy.
  // (User phải bấm "Đồng ý" ở modal để hệ thống xóa các SP đó khỏi giỏ + reload)
  if (findCancelledFlashSaleItem()) {
    showCancelledModal.value = true
    return
  }

  // Lưu voucher đã chọn - CHỈ lưu code và type, KHÔNG lưu value
  // (để FE không bị trust data sai từ localStorage, BE sẽ tự tính lại khi checkout)
  if (voucherCode.value) {
    localStorage.setItem(
      'selectedVoucher',
      JSON.stringify({
        code: voucherCode.value,
        type: selectedVoucherType.value,
      }),
    )
  } else {
    localStorage.removeItem('selectedVoucher')
  }

  // Lưu danh sách sản phẩm đã chọn để checkout hiển thị đúng
  const selectedItemsData = selectedItems.value.map((item) => ({
    cartItemId: item.cartItemId,
    productName: item.name,
    variantName: item.variant,
    skuCode: item.skuCode,
    thumbnailUrl: item.thumbnailUrl,
    quantity: item.quantity,
    price: item.price,
    originalPrice: item.originalPrice,
    totalPrice: (item.isFlashSale ? item.flashSalePrice : item.price) * item.quantity,
    // Thông tin Flash Sale
    isFlashSale: item.isFlashSale || false,
    flashSaleSlotId: item.flashSaleSlotId || null,
    flashSaleSlotName: item.flashSaleSlotName || null,
  }))
  localStorage.setItem('selectedCartItems', JSON.stringify(selectedItemsData))

  // Lưu thông tin tổng tiền
  localStorage.setItem('selectedSubtotal', subtotal.value.toString())

  window.location.href = '/checkout'
}
</script>
