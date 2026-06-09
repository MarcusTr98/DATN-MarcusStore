<template>
  <div class="cart-wrapper">
    <div class="cart-header">
      <i class="ti ti-shopping-cart cart-title-icon" aria-hidden="true"></i>
      <h2>Giỏ hàng của bạn</h2>
      <span class="count">{{ selectedCount }} sản phẩm</span>
    </div>

    <div v-if="isLoadingCart" class="cart-loading">
      Đang tải giỏ hàng...
    </div>

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


          <div class="cart-item-list" >
            <div v-for="item in cartItems" :key="item.id" class="cart-item">
              <div class="item-check">
                <input v-model="item.checked" type="checkbox" />
              </div>
              <div class="item-info">
                <div class="item-img">
                  <img
                    v-if="item.imageUrl"
                    :src="item.imageUrl"
                    :alt="item.name"
                    class="cart-product-img"
                  />

                  <span v-else class="item-img-placeholder">
    {{ item.icon }}
  </span>
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
                  <input
                    :checked="selectedVoucher === 50000"
                    type="checkbox"
                    @change="selectedVoucher = selectedVoucher === 50000 ? 0 : 50000"
                  />
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
    <div class="cart-toast" :class="{ active: isToastVisible }">
      <i class="ti ti-circle-check" aria-hidden="true"></i>
      <span>{{ toastMessage }}</span>
      <button class="cart-toast-close" type="button" aria-label="Đóng thông báo" @click="hideToast">&times;</button>
      <div :key="toastKey" class="cart-toast-progress"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useCartStore } from '@/stores/cartStore'
import '@/assets/css/cart.css'
const cartStore = useCartStore()

onMounted(() => {
  cartStore.fetchCart()
})

const cartItems = computed(() => cartStore.items)
const isLoadingCart = computed(() => cartStore.loading)
const cartError = computed(() => cartStore.error)

// function addAccessoryToCart(accessory) {
//   showAlert('Chức năng thêm phụ kiện vào giỏ sẽ làm sau khi có skuId thật')
// }
const isVoucherModalOpen = ref(false)
const selectedVoucher = ref(0)
const shippingVoucher = ref(true)
const voucherCode = ref('')
const isAlertModalOpen = ref(false)
const alertModalMessage = ref('')
const isToastVisible = ref(false)
const toastMessage = ref('')
const toastKey = ref(0)
let toastTimer = null
const suggestedTrack = ref(null)
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
  return `${Number(value || 0).toLocaleString('vi-VN')}đ`
}
async function updateItemQuantity(item, newQuantity) {
  const quantity = Math.max(Number(newQuantity) || 1, 1)
  if (item.stockQuantity && quantity > item.stockQuantity) {
    showAlert("số lượng đã vượt quá trong kho")
    await cartStore.fetchCart()
    return
  }

  const success = await cartStore.updateItemQuantity(item.skuId, quantity)
  if (!success) {
    showAlert(cartError || "cập nhật số lượng thất bại")
    await cartStore.fetchCart()
  }
}
async  function normalizeQty(item) {
 await  updateItemQuantity(item, item.quantity)
}
async function changeQty(item, delta) {
  const newQuantity = item.quantity + delta
  if(newQuantity < 1){
    return
  }
  if (item.stockQuantity && newQuantity > item.stockQuantity) {
    showAlert('Số lượng nhập vượt quá số lượng trong kho')
    return
  }
  await updateItemQuantity(item, newQuantity)
}


async  function removeItem(skuId){
  const success = await cartStore.removeItemFromCart(skuId)
  if(success){
    showToast("xóa thành công")
  }else{
    showAlert(cartStore.error || "xóa sản phẩm thất bại")
  }
}

 async function deleteChecked() {
  const checkedItems = cartItems.value.filter((item) => item.checked)
  if(checkedItems.length === 0){
    showAlert("vui lòng chọn 1 sản phẩm để xóa")
    return
  }
  const skuIds = checkedItems.map((item) => item.skuId)
  const success = await cartStore.removeManyItemFromCart(skuIds)
  if(success){
    showToast("xóa các sản phẩm thành công")
  }else{
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

function handleCheckout(){
  if(selectedCount.value === 0){
    showAlert("Vui lòng chọn ít nhất một sản phẩm để thanh toán");
    return;
  }
  window.location.href='/checkout';
}
</script>

<style scoped>

</style>
