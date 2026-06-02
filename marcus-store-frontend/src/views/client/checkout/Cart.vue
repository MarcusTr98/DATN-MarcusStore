<template>
  <div class="cart-wrapper">
    <div class="cart-header">
      <i class="ti ti-shopping-cart cart-title-icon" aria-hidden="true"></i>
      <h2>Giỏ hàng của bạn</h2>
      <span class="count">{{ selectedCount }} sản phẩm</span>
    </div>

    <div class="cart-layout" v-if="cartItems.length > 0">
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


          <div class="cart-item-list" >
            <div v-for="item in cartItems" :key="item.id" class="cart-item">
              <div class="item-check">
                <input v-model="item.checked" type="checkbox" />
              </div>
              <div class="item-info">
                <div class="item-img">
                  <span class="item-img-placeholder">{{ item.icon }}</span>
                </div>
                <div class="item-details">
                  <div class="item-name">{{ item.name }}</div>
                  <div class="item-variant">{{ item.variant }}</div>
                  <span class="item-badge" :class="{ accessory: item.isAccessory }">
                    {{ item.badge }}
                  </span>
                </div>
              </div>
              <div class="item-price">
                <s>{{ formatPrice(item.originalPrice) }}</s>
                {{ formatPrice(item.price) }}
              </div>
              <div class="qty-control">
                <button class="qty-btn" type="button" @click="changeQty(item, -1)">-</button>
                <input v-model.number="item.quantity" class="qty-num" min="1" type="number" @change="normalizeQty(item)" />
                <button class="qty-btn" type="button" @click="changeQty(item, 1)">+</button>
              </div>
              <div class="item-total">{{ formatPrice(item.price * item.quantity) }}</div>
              <button class="item-remove" type="button" aria-label="Xóa sản phẩm" @click="removeItem(item.id)">
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
            <button class="suggested-nav suggested-nav-prev" type="button" aria-label="Xem phụ kiện trước" @click="scrollAccessories(-1)">
              <i class="ti ti-chevron-left" aria-hidden="true"></i>
            </button>

            <div id="productTrack" ref="suggestedTrack" class="suggested-track product-track">
              <div v-for="accessory in accessories" :key="accessory.id" class="sug-card product-card">
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

            <button class="suggested-nav suggested-nav-next" type="button" aria-label="Xem thêm phụ kiện" @click="scrollAccessories(1)">
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
            <button class="open-voucher-btn" type="button" @click="isVoucherModalOpen = true">
              Chọn hoặc nhập mã
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
              <span class="label">Phí vận chuyển</span>
              <span class="value shipping-free">Miễn phí</span>
            </div>
            <div class="summary-row">
              <span class="label">Voucher Marcus Store</span>
              <span class="value discount">-{{ formatPrice(voucherDiscount) }}</span>
            </div>
          </div>
          <div class="summary-total">
            <div>
              <span class="total-label">Tổng thanh toán</span>
              <span class="vat-note">(Đã bao gồm VAT)</span>
            </div>
            <div class="total-price">{{ formatPrice(totalPayment) }}</div>
          </div>
          <button class="checkout-btn" type="button" @click="handleCheckout" >Tiến hành thanh toán</button>
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
            Giảm ngay <strong>500.000đ</strong> khi mở thẻ tín dụng VIB hoặc thanh toán qua VNPAY-QR (Nhập mã <code>MARCUS500</code>).
          </div>
          <div class="ms-promo-box">
            Hỗ trợ <strong>Trả góp 0%</strong> lãi suất qua thẻ tín dụng hoặc công ty tài chính duyệt nhanh 5 phút.
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
    <div class="v-modal-overlay" :class="{ active: isVoucherModalOpen }" @click.self="isVoucherModalOpen = false">
      <div class="v-modal-card">
        <div class="v-modal-header">
          <h3>Chọn Marcus Store Voucher</h3>
          <button class="close-btn" type="button" @click="isVoucherModalOpen = false">&times;</button>
        </div>
        <div class="v-modal-search">
          <div class="v-search-box">
            <input v-model="voucherCode" type="text" placeholder="Mã Marcus Store Voucher" />
            <button type="button" :disabled="!voucherCode.trim()">ÁP DỤNG</button>
          </div>
        </div>
        <div class="v-modal-body">
          <div class="v-section-title">
            Mã miễn phí vận chuyển
            <span>(Có thể chọn 1 Voucher)</span>
          </div>
          <div class="v-ticket-list">
            <div class="v-ticket">
              <div class="v-ticket-left">
                <div class="v-icon-box accent-type"><i class="ti ti-truck" aria-hidden="true"></i></div>
                <span class="v-tag-type accent-type">Freeship</span>
              </div>
              <div class="v-ticket-right">
                <div class="v-ticket-info">
                  <span class="v-title">Miễn phí vận chuyển</span>
                  <span class="v-min-order">Đơn tối thiểu 0đ</span>
                  <span class="v-expiry">Sắp hết hạn: Còn 1 giờ</span>
                </div>
                <div class="v-ticket-action">
                  <input v-model="shippingVoucher" type="checkbox" />
                </div>
              </div>
            </div>
          </div>

          <div class="v-section-title">Mã giảm giá / Toàn sàn</div>
          <div class="v-ticket-list">
            <div class="v-ticket">
              <div class="v-ticket-left">
                <div class="v-icon-box"><i class="ti ti-shopping-cart" aria-hidden="true"></i></div>
                <span class="v-tag-type">Marcus Store</span>
              </div>
              <div class="v-ticket-right">
                <div class="v-ticket-info">
                  <span class="v-title">Giảm 50.000đ toàn sàn</span>
                  <span class="v-min-order">Đơn tối thiểu 500.000đ</span>
                  <span class="v-expiry">Hạn dùng đến: 31/12</span>
                </div>
                <div class="v-ticket-action">
                  <input v-model.number="selectedVoucher" type="radio" name="shop-voucher" :value="50000" />
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="v-modal-footer">
          <button class="v-btn v-btn-primary" type="button" @click="isVoucherModalOpen = false">ĐỒNG Ý</button>
        </div>
      </div>
    </div>
    <div class="v-modal-overlay" :class="{ active: isAlertModalOpen }" @click.self="isAlertModalOpen = false">
      <div class="v-modal-card" style="width: 400px;">
        <div class="v-modal-header">
          <h3>Thông báo hệ thống</h3>
          <button class="close-btn" type="button" @click="isAlertModalOpen = false">&times;</button>
        </div>
        <div class="v-modal-body" style="text-align: center; padding: 30px 20px;">
          <i class="ti ti-alert-triangle" style="font-size: 48px; color: var(--ms-primary); display: block; margin-bottom: 12px;"></i>
          <p style="font-size: 14.5px; margin: 0; line-height: 1.5; font-weight: 500;">
            {{ alertModalMessage }}
          </p>
        </div>
        <div class="v-modal-footer" style="justify-content: center;">
          <button class="v-btn v-btn-primary" type="button" @click="isAlertModalOpen = false">ĐỒNG Ý</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const cartItems = ref([
  {
    id: 'item-iphone',
    name: 'iPhone 16 Pro Max 256GB',
    variant: 'Màu: Titan Sa Mạc | VN/A',
    badge: 'Chính hãng VN/A',
    icon: 'Điện thoại',
    price: 29990000,
    originalPrice: 33990000,
    quantity: 1,
    checked: true,
  },
  {
    id: 'item-ipad',
    name: 'iPad Pro M4 11 inch Wi-Fi 256GB',
    variant: 'Màu: Bạc | Siêu mỏng',
    badge: 'Hàng mới về',
    icon: 'iPad',
    price: 26490000,
    originalPrice: 28990000,
    quantity: 1,
    checked: true,
  },
  {
    id: 'item-macbook',
    name: 'MacBook Air M3 13 inch 16GB - 512GB',
    variant: 'Màu: Xanh Đen (Midnight)',
    badge: 'Giảm 10%',
    icon: 'Mac',
    price: 29990000,
    originalPrice: 32990000,
    quantity: 1,
    checked: true,
  },
])

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
    icon: 'Túi',
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

const isVoucherModalOpen = ref(false)
const selectedVoucher = ref(0)
const shippingVoucher = ref(true)
const voucherCode = ref('')
const isAlertModalOpen = ref(false)
const alertModalMessage = ref('')
const suggestedTrack = ref(null)
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
const voucherDiscount = computed(() => selectedVoucher.value || 0)
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

function formatPrice(value) {
  return `${value.toLocaleString('vi-VN')}đ`
}

function normalizeQty(item) {
  item.quantity = Math.max(Number(item.quantity) || 1, 1)
}

function changeQty(item, delta) {
  item.quantity = Math.max(item.quantity + delta, 1)
}

function removeItem(id) {
  cartItems.value = cartItems.value.filter((item) => item.id !== id)
}

function deleteChecked() {
  cartItems.value = cartItems.value.filter((item) => !item.checked)
}

function addAccessoryToCart(accessory) {
  const existingItem = cartItems.value.find((item) => item.id === accessory.id)

  if (existingItem) {
    existingItem.quantity += 1
    existingItem.checked = true
    return
  }

  cartItems.value.push({
    ...accessory,
    badge: 'Mua kèm giá sốc',
    quantity: 1,
    checked: true,
    isAccessory: true,
  })
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

function handleCheckout(){
  if(selectedCount.value === 0){
    alertModalMessage.value = "Vui lòng chọn ít nhất một sản phẩm để thanh toán";
    isAlertModalOpen.value = true;
    return;
  }
  window.location.href='/checkout';
}
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/@tabler/icons-webfont@2.44.0/tabler-icons.min.css');

* {
  box-sizing: border-box;
}

.cart-wrapper {
  --ms-primary: #e11d1d;
  --ms-primary-hover: #c0392b;
  --ms-primary-light: #fdeaea;
  --ms-primary-border: #f5c0c0;
  --ms-accent: #ff6b00;
  --ms-accent-light: #fff0e6;
  --ms-accent-dark: #cc5500;
  --ms-accent-border: #ffd4b3;
  --ms-gray: #9aa3b5;
  --ms-gray-light: #f4f5f7;
  --ms-gray-border: #e2e5ec;
  --ms-white: #ffffff;
  --ms-text: #1a2233;
  max-width: 1240px;
  margin: 0 auto;
  padding: 1.5rem 1rem;
  color: var(--ms-text);
  font-family: 'Segoe UI', sans-serif;
}
/* --- EMPTY CART STATE --- */
.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  padding: 60px 20px;
  text-align: center;
  background: transparent;
}

.empty-cart-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--ms-gray-light);
  color: #c5cada;
  font-size: 64px;
  margin-bottom: 24px;
}

.empty-cart h3 {
  margin: 0 0 12px 0;
  color: var(--ms-text);
  font-size: 24px;
  font-weight: 700;
}

.empty-cart p {
  margin: 0 0 28px 0;
  color: var(--ms-gray);
  font-size: 16px;
}

.continue-shopping-btn {
  border: none;
  border-radius: 6px;
  background: var(--ms-primary);
  color: #fff;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  padding: 12px 30px;
  transition: all 0.2s;
}

.continue-shopping-btn:hover {
  background: var(--ms-primary-hover);
  color: #fff;
}
.cart-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 1.25rem;
}

.cart-title-icon {
  color: var(--ms-primary);
  font-size: 22px;
}

.cart-header h2 {
  margin: 0;
  color: var(--ms-text);
  font-size: 20px;
  font-weight: 600;
}

.cart-header .count {
  border-radius: 20px;
  background: var(--ms-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 9px;
}

.cart-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  align-items: start;
  gap: 1rem;
}

.cart-main-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-width: 0;
}

.cart-sidebar-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  position: sticky;
  top: 1rem;
}

.cart-items,
.cart-summary,
.ms-side-widget,
.suggested {
  overflow: hidden;
  border: 0.5px solid var(--ms-gray-border);
  border-radius: 10px;
  background: var(--ms-white);
}

.cart-item {
  display: grid;
  grid-template-columns: 40px minmax(180px, 1fr) 120px 110px 110px 36px;
  align-items: center;
  gap: 8px;
}

.cart-item {
  border-bottom: 0.5px solid var(--ms-gray-border);
  padding: 20px 16px;
  transition: background 0.15s;
}

.cart-item-list {
  max-height: 460px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--ms-primary) var(--ms-gray-light);
}

.cart-item-list::-webkit-scrollbar {
  width: 8px;
}

.cart-item-list::-webkit-scrollbar-track {
  background: var(--ms-gray-light);
  border-radius: 999px;
}

.cart-item-list::-webkit-scrollbar-thumb {
  background: var(--ms-primary);
  border-radius: 999px;
}

.cart-item-list::-webkit-scrollbar-thumb:hover {
  background: var(--ms-primary-hover);
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item:hover {
  background: #fafafa;
}

.item-check input,
.select-all input,
.v-ticket-action input {
  accent-color: var(--ms-primary);
  cursor: pointer;
}

.item-check input {
  width: 16px;
  height: 16px;
}

.item-info {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.item-img {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  overflow: hidden;
  border: 0.5px solid var(--ms-gray-border);
  border-radius: 8px;
  background: var(--ms-gray-light);
}

.item-img-placeholder {
  color: var(--ms-primary);
  font-size: 16px;
  font-weight: 700;
}

.item-details {
  min-width: 0;
}

.item-name {
  display: -webkit-box;
  overflow: hidden;
  color: var(--ms-text);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.4;
  margin-bottom: 3px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.item-variant {
  color: var(--ms-gray);
  font-size: 11.5px;
}

.item-badge {
  display: inline-block;
  border: 0.5px solid var(--ms-accent-border);
  border-radius: 4px;
  background: var(--ms-accent-light);
  color: var(--ms-accent-dark);
  font-size: 10px;
  font-weight: 700;
  margin-top: 4px;
  padding: 1px 6px;
}

.item-badge.accessory {
  border-color: var(--ms-primary-border);
  background: var(--ms-primary-light);
  color: var(--ms-primary);
}

.item-price {
  color: var(--ms-gray);
  font-size: 13.5px;
  text-align: center;
}

.item-price s {
  display: block;
  color: #c5cada;
  font-size: 11px;
}

.qty-control {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--ms-gray-border);
  border-radius: 6px;
}

.qty-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  background: var(--ms-gray-light);
  color: var(--ms-gray);
  cursor: pointer;
  font-size: 16px;
  transition: all 0.1s;
  user-select: none;
}

.qty-btn:hover {
  background: var(--ms-primary-light);
  color: var(--ms-primary);
}

.qty-num {
  width: 38px;
  height: 30px;
  border: none;
  outline: none;
  background: var(--ms-white);
  color: var(--ms-text);
  font-size: 13px;
  font-weight: 600;
  text-align: center;
}

.qty-num::-webkit-inner-spin-button,
.qty-num::-webkit-outer-spin-button {
  margin: 0;
  appearance: none;
}

.item-total {
  color: var(--ms-primary);
  font-size: 14px;
  font-weight: 700;
  text-align: center;
}

.item-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  background: none;
  color: var(--ms-gray);
  cursor: pointer;
  transition: all 0.15s;
}

.item-remove:hover {
  background: var(--ms-primary-light);
  color: var(--ms-primary);
}

.cart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 0.5px solid var(--ms-gray-border);
  background: var(--ms-gray-light);
  padding: 12px 16px;
}

.select-all {
  display: flex;
  align-items: center;
  color: var(--ms-gray);
  cursor: pointer;
  font-size: 13px;
  gap: 7px;
}

.delete-selected {
  border: none;
  border-radius: 5px;
  background: none;
  color: var(--ms-primary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  padding: 5px 10px;
  transition: background 0.12s;
}

.delete-selected:hover {
  background: var(--ms-primary-light);
}

.summary-title {
  border-bottom: 0.5px solid var(--ms-gray-border);
  color: var(--ms-text);
  font-size: 15px;
  font-weight: 700;
  padding: 14px 16px;
}

.summary-title i {
  color: var(--ms-primary);
  font-size: 16px;
  margin-right: 6px;
  vertical-align: -2px;
}

.voucher-row {
  display: flex;
  align-items: center;
  border-bottom: 0.5px solid var(--ms-gray-border);
  gap: 8px;
  padding: 12px 16px;
}

.voucher-label-trigger {
  flex: 1;
  color: var(--ms-gray);
  font-size: 13px;
}

.open-voucher-btn {
  display: flex;
  align-items: center;
  border: none;
  background: none;
  color: #4a90e2;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  gap: 4px;
}

.open-voucher-btn:hover {
  color: var(--ms-primary);
}

.summary-rows {
  display: flex;
  flex-direction: column;
  border-bottom: 0.5px solid var(--ms-gray-border);
  gap: 9px;
  padding: 12px 16px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
}

.summary-row .label {
  color: var(--ms-gray);
}

.summary-row .value {
  color: var(--ms-text);
  font-weight: 500;
}

.summary-row .discount,
.summary-row .shipping-free {
  color: #1a8c3c;
}

.summary-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 0.5px solid var(--ms-gray-border);
  padding: 14px 16px;
}

.total-label {
  color: var(--ms-text);
  font-size: 14px;
  font-weight: 600;
}

.total-price {
  color: var(--ms-primary);
  font-size: 20px;
  font-weight: 800;
}

.vat-note {
  display: block;
  color: var(--ms-gray);
  font-size: 10.5px;
  margin-top: 1px;
}

.checkout-btn {
  display: block;
  width: calc(100% - 32px);
  height: 46px;
  border: none;
  border-radius: 8px;
  margin: 14px 16px;
  background: var(--ms-primary);
  color: #fff;
  cursor: pointer;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.3px;
  transition: background 0.15s;
}

.checkout-btn:hover {
  background: var(--ms-primary-hover);
}

.guarantee-strip {
  display: flex;
  justify-content: space-around;
  padding: 10px 16px 14px;
}

.guarantee-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.guarantee-item i {
  color: var(--ms-primary);
  font-size: 20px;
}

.guarantee-item span {
  color: var(--ms-gray);
  font-size: 10.5px;
  text-align: center;
}

.ms-side-widget {
  padding: 14px 16px;
}

.ms-widget-title {
  display: flex;
  align-items: center;
  color: var(--ms-text);
  font-size: 13.5px;
  font-weight: 700;
  gap: 6px;
  margin-bottom: 10px;
}

.ms-widget-title i {
  color: var(--ms-primary);
  font-size: 16px;
}

.ms-promo-box {
  border: 1px dashed var(--ms-accent-border);
  border-radius: 6px;
  background: linear-gradient(135deg, #fffcf6 0%, #fff5e5 100%);
  font-size: 12px;
  line-height: 1.5;
  margin-bottom: 8px;
  padding: 10px;
}

.ms-promo-box strong {
  color: var(--ms-accent-dark);
}

.ms-support-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 13px;
}

.ms-support-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--ms-primary-light);
  color: var(--ms-primary);
  font-size: 16px;
}

.ms-support-chat {
  background: #eef4fa;
  color: #4a90e2;
}

.ms-support-info span {
  display: block;
  color: var(--ms-gray);
  font-size: 11px;
}

.ms-support-info strong {
  color: var(--ms-primary);
}

.ms-support-link {
  color: #4a90e2;
  cursor: pointer;
}

.suggested {
  padding: 14px 16px;
  min-width: 0;
}

.suggested-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.suggested-title {
  display: flex;
  align-items: center;
  color: var(--ms-text);
  font-size: 14px;
  font-weight: 700;
  gap: 6px;
}

.suggested-title i {
  color: var(--ms-accent);
}

.suggested-more {
  display: inline-flex;
  align-items: center;
  border: none;
  background: none;
  color: var(--ms-primary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  gap: 3px;
  padding: 4px 0;
  white-space: nowrap;
}

.suggested-more:hover {
  color: var(--ms-primary-hover);
}

.suggested-carousel {
  position: relative;
  overflow: hidden;
  min-width: 0;
  width: 100%;
}

.suggested-track {
  display: flex;
  gap: 20px;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 0 2px 10px;
  scroll-behavior: auto;
  scroll-snap-type: x mandatory;
}

.suggested-track {
  scrollbar-width: thin;
  scrollbar-color: var(--ms-primary) var(--ms-gray-light);
}

.suggested-track::-webkit-scrollbar {
  height: 8px;
}

.suggested-track::-webkit-scrollbar-track {
  background: var(--ms-gray-light);
  border-radius: 999px;
}

.suggested-track::-webkit-scrollbar-thumb {
  background: var(--ms-primary);
  border-radius: 999px;
}

.suggested-track::-webkit-scrollbar-thumb:hover {
  background: var(--ms-primary-hover);
}

.product-carousel {
  width: 100%;
  overflow: hidden;
}

.product-track {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  scroll-behavior: auto;

}

.product-track::-webkit-scrollbar {
  display: none;
}

.product-card {
  flex-shrink: 0;
  font-weight: 600;
}

.suggested-nav {
  position: absolute;
  z-index: 2;
  top: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--ms-gray-border);
  border-radius: 50%;
  background: var(--ms-white);
  color: var(--ms-primary);
  cursor: pointer;
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(26, 34, 51, 0.14);
  transform: translateY(-50%);
  transition: all 0.15s;
}

.suggested-nav:hover {
  border-color: var(--ms-primary);
  background: var(--ms-primary);
  color: #fff;
}

.suggested-nav-prev {
  left: -10px;
}

.suggested-nav-next {
  right: -10px;
}

.sug-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  border: 0.5px solid var(--ms-gray-border);
  border-radius: 8px;
  cursor: pointer;
  flex: 0 0 calc((100% - 48px) / 4);
  min-width: 0;
  max-width: calc((100% - 48px) / 4);
  height: 355px;
  padding: 10px;
  scroll-snap-align: start;
  text-align: center;
  transition: border-color 0.15s;
}

.sug-card:hover {
  border-color: var(--ms-primary);
}

.sug-img {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  border-radius: 6px;
  height: 210px;
  aspect-ratio: auto;
  background: var(--ms-gray-light);
  color: var(--ms-primary);
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 6px;
}

.sug-name {
  display: -webkit-box;
  height: 40px;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: var(--ms-text);
  font-size: 11.5px;
  font-weight: 500;
  line-height: 1.3;
  margin-bottom: 3px;
}

.sug-price {
  color: var(--ms-primary);
  font-size: 12px;
  font-weight: 700;
}

.sug-add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 28px;
  border: none;
  border-radius: 5px;
  background: var(--ms-primary-light);
  color: var(--ms-primary);
  cursor: pointer;
  font-size: 11.5px;
  font-weight: 600;
  gap: 4px;
  margin-top: 6px;
  flex-shrink: 0;
  transition: background 0.12s;
}

.sug-add:hover {
  background: var(--ms-primary);
  color: #fff;
}

.v-modal-overlay {
  position: fixed;
  z-index: 1000;
  top: 0;
  left: 0;
  display: flex;
  visibility: hidden;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  opacity: 0;
  transition: opacity 0.2s, visibility 0.2s;
}

.v-modal-overlay.active {
  visibility: visible;
  opacity: 1;
}

.v-modal-card {
  display: flex;
  flex-direction: column;
  width: 550px;
  max-width: 90%;
  max-height: 80vh;
  border-radius: 12px;
  background: var(--ms-white);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  transform: translateY(20px);
  transition: transform 0.2s;
}

.v-modal-overlay.active .v-modal-card {
  transform: translateY(0);
}

.v-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--ms-gray-border);
  padding: 16px 20px;
}

.v-modal-header h3 {
  margin: 0;
  color: var(--ms-text);
  font-size: 16px;
  font-weight: 600;
}

.close-btn {
  border: none;
  border-radius: 6px;
  background: none;
  color: var(--ms-gray);
  cursor: pointer;
  font-size: 20px;
  transition: all 0.15s;
}

.close-btn:hover {
  background: var(--ms-primary-light);
  color: var(--ms-primary);
}

.v-modal-search {
  border-bottom: 1px solid var(--ms-gray-border);
  background: #fafdff;
  padding: 14px 20px;
}

.v-search-box {
  display: flex;
  border: 1px solid var(--ms-gray-border);
  border-radius: 6px;
  background: #fff;
  gap: 10px;
  padding: 4px 6px;
}

.v-search-box input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 13px;
  padding-left: 8px;
}

.v-search-box button {
  border: none;
  border-radius: 4px;
  background: var(--ms-primary);
  color: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 14px;
}

.v-search-box button:disabled {
  background: var(--ms-gray-light);
  color: var(--ms-gray);
  cursor: not-allowed;
}

.v-modal-body {
  flex: 1;
  overflow-y: auto;
  max-height: min(52vh, 420px);
  padding: 16px 20px;
}

.v-section-title {
  color: var(--ms-text);
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
}

.v-section-title span {
  color: var(--ms-gray);
  font-weight: 400;
}

.v-ticket-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.v-ticket {
  display: flex;
  min-height: 94px;
  overflow: hidden;
  position: relative;
  border: 1px solid var(--ms-primary-border);
  border-radius: 8px;
  background: #fff;
}

.v-ticket-left {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  width: 110px;
  position: relative;
  border-right: 1px dashed var(--ms-primary-border);
  background: #fffafa;
  padding: 10px;
  text-align: center;
}

.v-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--ms-primary);
  color: #fff;
  font-size: 20px;
  margin-bottom: 4px;
}

.v-icon-box.accent-type {
  background: var(--ms-accent);
}

.v-tag-type {
  color: var(--ms-primary);
  font-size: 10px;
  font-weight: 600;
}

.v-tag-type.accent-type {
  color: var(--ms-accent-dark);
}

.v-ticket-right {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
}

.v-ticket-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.v-title {
  color: var(--ms-text);
  font-size: 14px;
  font-weight: 700;
}

.v-min-order {
  color: var(--ms-gray);
  font-size: 11.5px;
}

.v-expiry {
  color: var(--ms-primary);
  font-size: 11px;
  font-weight: 500;
  margin-top: 4px;
}

.v-ticket-action input {
  width: 18px;
  height: 18px;
}

.v-modal-footer {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--ms-gray-border);
  background: #fafafa;
  gap: 10px;
  padding: 14px 20px;
}

.v-btn {
  min-width: 96px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  padding: 9px 14px;
}

.v-btn-secondary {
  background: var(--ms-gray-light);
  color: var(--ms-text);
}

.v-btn-primary {
  background: var(--ms-primary);
  color: #fff;
}

@media (max-width: 900px) {
  .cart-layout {
    grid-template-columns: 1fr;
  }

  .cart-sidebar-content {
    position: static;
  }

  .cart-summary {
    position: static;
  }

}

@media (max-width: 720px) {
  .cart-item {
    grid-template-columns: 28px 1fr 36px;
    align-items: center;
  }

  .item-info {
    grid-column: 2;
  }

  .item-price,
  .qty-control,
  .item-total {
    grid-column: 2;
    justify-self: start;
  }

  .item-remove {
    grid-column: 3;
    grid-row: 1;
  }

  .suggested-header {
    align-items: flex-start;
  }

  .sug-card {
    flex: 0 0 min(82vw, 260px);
    max-width: min(82vw, 260px);
    height: 340px;
  }

  .sug-img {
    height: 190px;
  }

  .v-modal-card {
    width: min(94vw, 550px);
    max-width: 94vw;
    max-height: 86vh;
  }

  .v-modal-body {
    max-height: 55vh;
    overflow-y: auto;
  }
}
</style>
