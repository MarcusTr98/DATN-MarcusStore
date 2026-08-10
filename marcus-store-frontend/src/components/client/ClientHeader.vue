<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useCartStore } from '@/stores/cartStore'
import { useRoute, useRouter } from 'vue-router'
import { useSettings } from '@/composables/useSettings'
import { useSearchBox } from '@/composables/useSearchBox'
import BaseModal from '../BaseModal.vue'
import wishlist from '@/composables/useWishlistShared'
import { useUserNotifications } from '@/composables/useUserNotifications'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const {
  query: searchQuery,
  showPanel,
  isLoading: suggestLoading,
  suggestions,
  history,
  isTyping,
  openPanel,
  closePanel,
  removeHistory,
  clearHistory,
  submit,
  refreshOnAuth,
} = useSearchBox()

const totalMoney = computed(() => cartStore.totalAmount)
const totalQuantity = computed(() => cartStore.totalQuantity)
const isLoggedIn = ref(false)
const userName = ref('')
const showNotifications = ref(false)
const showAccountMenu = ref(false)
const showCategoryMenu = ref(false)
let notificationCloseTimer = null
const {
  notifications,
  displayUnreadCount,
  isLoading: notificationsLoading,
  errorMessage: notificationError,
  fetchNotifications,
  markRead,
  markAllRead,
  disconnectRealtime,
} = useUserNotifications()

const wishlistCount = computed(() => (wishlist.isLoaded() ? wishlist.totalCount() : 0))

const { sysSettings, fetchSettings, siteName, siteLogoUrl, siteNameParts } = useSettings()

const checkAuth = () => {
  const token = localStorage.getItem('ACCESS_TOKEN')
  const name = localStorage.getItem('USERNAME')
  if (token) {
    isLoggedIn.value = true
    userName.value = name || 'Khách hàng'
  } else {
    isLoggedIn.value = false
    userName.value = ''
  }
}
const showLogoutModal = ref(false)
const handleLogout = () => {
  showLogoutModal.value = true
}

const confirmLogout = () => {
  disconnectRealtime()
  localStorage.removeItem('ACCESS_TOKEN')
  localStorage.removeItem('USER_ROLE')
  localStorage.removeItem('USERNAME')
  // Marcus thêm: logout phải kết thúc luôn phiên tư vấn ẩn danh trên thiết bị.
  sessionStorage.removeItem('MARCUS_AI_CONVERSATION')
  sessionStorage.removeItem('MARCUS_AI_TRACKING_SESSION')
  // Marcus sửa: đăng xuất kết thúc anonymous journey hiện tại; lần truy cập sau
  // không được nối hành vi của hai người dùng chung trình duyệt.
  sessionStorage.removeItem('MARCUS_BEHAVIOR_SESSION')
  window.dispatchEvent(new Event('marcus-ai-reset'))

  cartStore.cart = null
  cartStore.items = []
  cartStore.error = null

  isLoggedIn.value = false
  showLogoutModal.value = false
  router.push('/auth/login')
}

const cancelLogout = () => {
  showLogoutModal.value = false
}

// ---- Yêu cầu đăng nhập khi Guest bấm vào Tài khoản / Yêu thích / Giỏ hàng / Theo dõi đơn hàng ----
const showGuestModal = ref(false)
const guestModalMessage = ref(
  'Vui lòng đăng nhập để trải nghiệm đầy đủ các tiện ích mua sắm tại Marcus Store.',
)

const goOrPrompt = (path, message) => {
  if (isLoggedIn.value) {
    router.push(path)
  } else {
    if (message) guestModalMessage.value = message
    showGuestModal.value = true
  }
}

const closeGuestModal = () => {
  showGuestModal.value = false
}

// Marcus thêm: đóng các lớp nổi khi chuyển trang để header không giữ trạng thái cũ.
watch(
  () => route.fullPath,
  () => {
    showAccountMenu.value = false
    showCategoryMenu.value = false
    showNotifications.value = false
    closePanel()
  },
)

const openNotificationsOnHover = async () => {
  window.clearTimeout(notificationCloseTimer)
  if (!showNotifications.value) {
    showNotifications.value = true
    await fetchNotifications()
  }
}

const scheduleCloseNotifications = () => {
  window.clearTimeout(notificationCloseTimer)
  notificationCloseTimer = window.setTimeout(() => {
    showNotifications.value = false
  }, 180)
}

const openNotification = async (item) => {
  const target = await markRead(item)
  showNotifications.value = false
  if (!target) return
  // Marcus sửa: nếu đang ở chính URL đó (cùng orderCode) thì Vue Router không remount
  // OrderDetailView → warrantyStatus cũ vẫn hiển thị. Trick bằng query rác để ép route đổi,
  // watch(route.fullPath) ở OrderDetailView sẽ bắt được và gọi lại fetchOrderDetail.
  // Sau đó replace về URL gốc để không để lại query trên thanh địa chỉ.
  const currentPath = router.currentRoute.value.path
  if (currentPath === target) {
    await router.replace(target + '?refresh=' + Date.now())
    await router.replace(target)
  } else {
    router.push(target)
  }
}

const getUserNotificationIcon = (type) =>
  ({
    ORDER_PENDING: 'fas fa-receipt',
    ORDER_CONFIRMED: 'fas fa-circle-check',
    ORDER_PROCESSING: 'fas fa-box-open',
    ORDER_READY_FOR_PICKUP: 'fas fa-store',
    ORDER_PACKED: 'fas fa-box',
    ORDER_SHIPPING: 'fas fa-truck-fast',
    ORDER_DELIVERED: 'fas fa-house-circle-check',
    ORDER_COMPLETED: 'fas fa-medal',
    ORDER_CANCELLED: 'fas fa-ban',
    ORDER_FAILED: 'fas fa-triangle-exclamation',
    PAYMENT_SUCCESS: 'fas fa-credit-card',
    REFUND_PENDING: 'fas fa-hourglass-half',
    REFUND_SUCCESS: 'fas fa-circle-check',
    REFUND_FAILED: 'fas fa-triangle-exclamation',
  })[type] || 'far fa-bell'

onMounted(() => {
  checkAuth()

  if (localStorage.getItem('ACCESS_TOKEN')) {
    cartStore.fetchCart()
    wishlist.fetchIds()
    fetchNotifications()
  } else {
    wishlist.reset()
  }

  fetchSettings()
  window.addEventListener('auth-changed', checkAuth)
  const detachAuth = refreshOnAuth()
  window.addEventListener('mousedown', handleClickOutside)

  detachAuthListener = detachAuth
})

let detachAuthListener = null

// Marcus sửa: onMounted không nhận cleanup trả về; tách đúng lifecycle để tránh listener bị nhân đôi.
onUnmounted(() => {
  window.removeEventListener('auth-changed', checkAuth)
  detachAuthListener?.()
  window.removeEventListener('mousedown', handleClickOutside)
  window.clearTimeout(notificationCloseTimer)
})

function handleClickOutside(e) {
  const wrapper = document.querySelector('.search-bar-wrapper')
  if (wrapper && !wrapper.contains(e.target)) closePanel()
  if (!e.target.closest('.account-dropdown')) showAccountMenu.value = false
  if (!e.target.closest('.category-dropdown')) showCategoryMenu.value = false
}
</script>

<template>
  <header class="ms-header">
    <!-- Top Bar -->
    <div class="topbar">
      <div class="container">
        <div class="topbar-inner">
          <div class="topbar-left">
            <span class="topbar-item">
              <i class="fas fa-tag"></i>
              <span
                >Khuyến mãi hôm nay:
                <strong>{{ sysSettings.PROMO_TEXT || 'Đang cập nhật' }}</strong></span
              >
            </span>
          </div>
          <div class="topbar-right">
            <a :href="'tel:' + sysSettings.HOTLINE" class="topbar-item topbar-link">
              <i class="fas fa-headset"></i>
              <span
                >Hotline: <strong>(+84) {{ sysSettings.HOTLINE || 'Đang cập nhật' }}</strong></span
              >
            </a>
            <span class="topbar-divider">|</span>
            <router-link to="/contact-store" class="topbar-item topbar-link">
              <i class="fas fa-map-marker-alt"></i>
              <span>Liên hệ cửa hàng</span>
            </router-link>
            <span class="topbar-divider">|</span>
            <a
              href="#"
              class="topbar-item topbar-link"
              @click.prevent="
                goOrPrompt(
                  '/profile/orders',
                  'Vui lòng đăng nhập để theo dõi tình trạng đơn hàng của bạn.',
                )
              "
            >
              <i class="fas fa-truck"></i>
              <span>Theo dõi đơn hàng</span>
            </a>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Header -->
    <div class="main-header">
      <div class="container">
        <div class="main-header-inner">
          <!-- Logo -->
          <router-link to="/" class="ms-logo">
            <div class="logo-icon" :class="{ 'has-site-logo': siteLogoUrl }">
              <img v-if="siteLogoUrl" :src="siteLogoUrl" :alt="siteName" class="site-logo-image" />
              <i v-else class="fas fa-mobile-alt"></i>
            </div>
            <div class="logo-text">
              <span class="logo-brand">{{ siteNameParts.primary }}</span>
              <span v-if="siteNameParts.secondary" class="logo-store">{{
                siteNameParts.secondary
              }}</span>
            </div>
          </router-link>

          <!-- Search Bar -->
          <div class="search-bar-wrapper" @keydown.esc="closePanel">
            <div class="search-bar">
              <i class="fas fa-search search-icon"></i>
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="Tìm kiếm điện thoại, phụ kiện..."
                @focus="openPanel"
                @keydown.enter.prevent="submit()"
              />
              <button class="search-btn" @click="submit()">Tìm kiếm</button>
            </div>

            <!-- Suggest panel -->
            <div v-if="showPanel" class="search-suggest-panel shadow">
              <!-- Lịch sử tìm kiếm (chỉ khi chưa gõ) -->
              <div v-if="!isTyping && history.length" class="ss-section">
                <div class="ss-head">
                  <span>Lịch sử tìm kiếm</span>
                  <button class="ss-clear" @click="clearHistory">Xóa tất cả</button>
                </div>
                <ul class="ss-history">
                  <li v-for="h in history" :key="h">
                    <button class="ss-history-item" @click="submit(h)">
                      <i class="far fa-clock"></i>
                      <span>{{ h }}</span>
                    </button>
                    <button class="ss-remove" @click.stop="removeHistory(h)" aria-label="Xóa">
                      <i class="fas fa-times"></i>
                    </button>
                  </li>
                </ul>
              </div>

              <!-- Xu hướng / Gợi ý: dữ liệu bán chạy đến từ AnalyticsRepository.findBestSellers -->
              <div class="ss-section">
                <div class="ss-head">
                  <span>{{ isTyping ? 'Kết quả gợi ý' : 'Sản phẩm bán chạy' }}</span>
                  <small v-if="!isTyping" class="text-muted">Cập nhật liên tục</small>
                </div>
                <div v-if="suggestLoading" class="ss-loading">Đang tải...</div>
                <ul v-else-if="suggestions.length" class="ss-products">
                  <li v-for="p in suggestions" :key="p.productId">
                    <router-link
                      :to="`/product/${p.slug}`"
                      class="ss-product-item"
                      @click="closePanel"
                    >
                      <img :src="p.thumbnailUrl" :alt="p.productName" />
                      <div class="ss-product-info">
                        <p class="ss-name">{{ p.productName }}</p>
                        <p class="ss-price">{{ Number(p.price).toLocaleString('vi-VN') }}₫</p>
                      </div>
                    </router-link>
                  </li>
                </ul>
                <div v-else class="ss-empty">Không có gợi ý phù hợp</div>
              </div>
            </div>
          </div>

          <!-- Header Actions -->
          <div class="header-actions">
            <!-- Marcus thêm chuông khách: tách dữ liệu theo user đăng nhập và cập
                 nhật realtime khi hủy đơn/refund đổi trạng thái. -->
            <div
              v-if="isLoggedIn"
              class="h-action client-notification"
              @mouseenter="openNotificationsOnHover"
              @mouseleave="scheduleCloseNotifications"
            >
              <button
                type="button"
                class="h-action-btn notification-button"
                @click="openNotificationsOnHover"
              >
                <div class="h-action-icon position-relative">
                  <i class="far fa-bell"></i>
                  <span
                    v-if="Number(displayUnreadCount) > 0 || displayUnreadCount === '99+'"
                    class="cart-count"
                  >
                    {{ displayUnreadCount }}
                  </span>
                </div>
                <div class="h-action-text d-none d-xl-block">
                  <span class="h-action-main">Thông báo</span>
                </div>
              </button>
              <div v-if="showNotifications" class="client-notification-panel">
                <div class="client-notification-head">
                  <div><strong>Thông báo của bạn</strong><small>Đơn hàng và hoàn tiền</small></div>
                  <button v-if="notifications.length" type="button" @click="markAllRead">
                    Đọc tất cả
                  </button>
                </div>
                <div v-if="notificationsLoading" class="notification-empty">
                  Đang tải thông báo...
                </div>
                <div v-else-if="notificationError" class="notification-empty error">
                  {{ notificationError }}
                </div>
                <div v-else-if="!notifications.length" class="notification-empty">
                  Bạn chưa có thông báo.
                </div>
                <template v-else>
                  <button
                    v-for="item in notifications"
                    :key="item.id"
                    type="button"
                    class="client-notification-item"
                    :class="{ unread: !item.isRead }"
                    @click="openNotification(item)"
                  >
                    <span class="notification-item-icon" :class="item.type">
                      <i :class="item.icon || getUserNotificationIcon(item.type)"></i>
                    </span>
                    <span
                      ><strong>{{ item.title }}</strong
                      ><small>{{ item.message }}</small></span
                    >
                  </button>
                </template>
              </div>
            </div>
            <!-- Account -->
            <div class="h-action">
              <template v-if="!isLoggedIn">
                <a
                  href="#"
                  class="h-action-btn"
                  @click.prevent="
                    goOrPrompt('/profile', 'Vui lòng đăng nhập để quản lý tài khoản của bạn.')
                  "
                >
                  <div class="h-action-icon"><i class="far fa-user"></i></div>
                  <div class="h-action-text">
                    <span class="h-action-sub">Đăng nhập</span>
                    <span class="h-action-main">Tài khoản</span>
                  </div>
                </a>
              </template>
              <template v-else>
                <div
                  class="dropdown dropdown-hover account-dropdown"
                  :class="{ show: showAccountMenu }"
                >
                  <a
                    href="#"
                    class="h-action-btn"
                    @click.prevent="showAccountMenu = !showAccountMenu"
                  >
                    <div class="h-action-icon active"><i class="far fa-user"></i></div>
                    <div class="h-action-text">
                      <span class="h-action-sub">Xin chào,</span>
                      <span class="h-action-main">{{ userName }}</span>
                    </div>
                  </a>
                  <ul
                    class="dropdown-menu dropdown-menu-end ms-dropdown shadow"
                    :class="{ show: showAccountMenu }"
                  >
                    <li class="dropdown-user-header">
                      <i class="fas fa-user-circle me-2"></i>{{ userName }}
                    </li>
                    <li><hr class="dropdown-divider mx-3 my-1" /></li>
                    <li>
                      <router-link class="dropdown-item" to="/profile">
                        <i class="far fa-id-badge me-2"></i>Tài khoản của tôi
                      </router-link>
                    </li>
                    <li></li>
                    <li>
                      <router-link class="dropdown-item" to="/profile/wishlist">
                        <i class="far fa-heart me-2"></i>Sản phẩm yêu thích
                      </router-link>
                    </li>
                    <li><hr class="dropdown-divider mx-3 my-1" /></li>
                    <li>
                      <button class="dropdown-item text-danger" @click.prevent="handleLogout">
                        <i class="fas fa-sign-out-alt me-2"></i>Đăng xuất
                      </button>
                    </li>
                  </ul>
                </div>
              </template>
            </div>

            <!-- Wishlist -->
            <a
              href="#"
              class="h-action-btn"
              @click.prevent="
                goOrPrompt(
                  '/profile/wishlist',
                  'Vui lòng đăng nhập để xem danh sách sản phẩm yêu thích của bạn.',
                )
              "
            >
              <div class="h-action-icon position-relative">
                <i class="far fa-heart"></i>
                <span v-if="wishlistCount > 0" class="cart-count">{{ wishlistCount }}</span>
              </div>
              <div class="h-action-text d-none d-xl-block">
                <span class="h-action-sub d-block">Yêu thích</span>
                <span class="h-action-main d-block">Sản phẩm</span>
              </div>
            </a>

            <!-- Cart -->
            <a
              href="#"
              class="h-action-btn cart-action"
              @click.prevent="
                goOrPrompt('/cart', 'Vui lòng đăng nhập để xem và thanh toán giỏ hàng của bạn.')
              "
            >
              <div class="h-action-icon position-relative">
                <i class="fas fa-shopping-cart"></i>
                <span v-if="totalQuantity > 0" class="cart-count">{{ totalQuantity }}</span>
              </div>
              <div class="h-action-text">
                <span class="h-action-sub">Giỏ hàng</span>
                <span class="h-action-main cart-price"
                  >{{ totalMoney.toLocaleString('vi-VN') }}₫</span
                >
              </div>
            </a>
          </div>
        </div>
      </div>
    </div>

    <!-- Category Nav -->
    <nav class="main-nav bg-white border-bottom shadow-sm">
      <div class="container">
        <ul
          class="nav-list d-flex align-items-center justify-content-center list-unstyled mb-0 gap-1 gap-md-1 py-1"
        >
          <!-- 1. Trang chủ -->
          <li class="nav-item">
            <router-link to="/" class="nav-link fw-semibold text-dark px-1 py-1 rounded-pill">
              <i class="fas fa-home me-1"></i> Trang chủ
            </router-link>
          </li>

          <!-- 2. Danh mục (Dropdown Hover) -->
          <li
            class="nav-item dropdown dropdown-hover category-dropdown"
            :class="{ show: showCategoryMenu }"
          >
            <a
              href="#"
              class="nav-link fw-semibold text-dark px-1 py-2 rounded d-flex align-items-center"
              @click.prevent="showCategoryMenu = !showCategoryMenu"
            >
              <i class="fas fa-bars me-2"></i> Danh mục
              <i class="fas fa-chevron-down ms-2" style="font-size: 10px"></i>
            </a>

            <!-- Menu xổ xuống -->
            <ul
              class="dropdown-menu border-0 shadow-lg mt-0 rounded-3 p-2"
              :class="{ show: showCategoryMenu }"
            >
              <li>
                <router-link to="/category/dien-thoai" class="dropdown-item rounded py-2">
                  <i class="fas fa-mobile-alt fa-fw text-danger me-2"></i> Điện thoại
                </router-link>
              </li>
              <li>
                <router-link to="/category/may-tinh-bang" class="dropdown-item rounded py-2">
                  <i class="fas fa-tablet-alt fa-fw text-danger me-2"></i> Máy tính bảng
                </router-link>
              </li>
              <li>
                <router-link to="/category/am-thanh" class="dropdown-item rounded py-2">
                  <i class="fas fa-headphones-alt fa-fw text-danger me-2"></i> Âm thanh
                </router-link>
              </li>
              <li>
                <router-link to="/category/dong-ho-thong-minh" class="dropdown-item rounded py-2">
                  <i class="fas fa-clock fa-fw text-danger me-2"></i> Đồng hồ thông minh
                </router-link>
              </li>
              <li>
                <router-link to="/category/sac-pin" class="dropdown-item rounded py-2">
                  <i class="fas fa-battery-full fa-fw text-danger me-2"></i> Sạc & Pin
                </router-link>
              </li>
              <li>
                <router-link to="/category/op-lung" class="dropdown-item rounded py-2">
                  <i class="fas fa-shield-alt fa-fw text-danger me-2"></i> Ốp lưng & Bảo vệ
                </router-link>
              </li>
            </ul>
          </li>

          <!-- 3. Flash Sale -->
          <li class="nav-item">
            <router-link to="/khuyen-mai" class="nav-link nav-sale fw-bold px-1 py-1 rounded">
              <i class="fas fa-bolt text-warning me-1 flash-icon"></i> Flash Sale
            </router-link>
          </li>

          <!-- 4. Giới thiệu -->
          <li class="nav-item">
            <router-link to="/about-us" class="nav-link fw-semibold text-dark px-1 py-1 rounded">
              <i class="fas fa-users me-1"></i> Giới thiệu
            </router-link>
          </li>

          <!-- 5. Tin công nghệ -->
          <li class="nav-item">
            <router-link to="/blog" class="nav-link fw-semibold text-dark px-1 py-1 rounded">
              <i class="fas fa-newspaper me-1"></i> Tin công nghệ
            </router-link>
          </li>
        </ul>
      </div>
    </nav>
  </header>

  <!-- Modal xác nhận đăng xuất -->
  <BaseModal
    :visible="showLogoutModal"
    type="confirm"
    title="Đăng xuất"
    message="Bạn có chắc chắn muốn đăng xuất?"
    :show-confirm="true"
    @close="cancelLogout"
    @confirm="confirmLogout"
  />

  <!-- Modal yêu cầu đăng nhập khi Guest bấm Tài khoản / Yêu thích / Giỏ hàng / Theo dõi đơn hàng -->
  <!-- Viết inline trực tiếp tại đây, không tách file riêng -->
  <Teleport to="body">
    <transition name="guest-fade">
      <div v-if="showGuestModal" class="guest-prompt-overlay" @click="closeGuestModal">
        <div class="guest-prompt-modal shadow-lg" @click.stop>
          <div class="modal-top-action">
            <button class="close-btn-modal" @click="closeGuestModal">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="modal-body text-center pt-0">
            <div class="brand-logo-wrapper mb-4">
              <div class="logo-icon-box shadow-sm" :class="{ 'has-site-logo': siteLogoUrl }">
                <img
                  v-if="siteLogoUrl"
                  :src="siteLogoUrl"
                  :alt="siteName"
                  class="site-logo-image"
                />
                <i v-else class="fas fa-mobile-alt"></i>
              </div>
              <div class="logo-text-box">
                <span class="text-marcus">{{ siteNameParts.primary }}</span>
                <span v-if="siteNameParts.secondary" class="text-store">{{
                  siteNameParts.secondary
                }}</span>
              </div>
            </div>

            <h5 class="fw-bold mb-2 text-dark">Trải nghiệm tiện ích</h5>
            <p class="text-muted mb-4 px-2" style="font-size: 14px; line-height: 1.5">
              {{ guestModalMessage }}
            </p>

            <div class="action-buttons px-3 pb-2">
              <router-link
                to="/auth/login"
                class="btn btn-primary login-btn w-100 mb-3 py-2 fw-bold shadow-sm"
                @click="closeGuestModal"
              >
                Đăng nhập ngay
              </router-link>
              <div class="register-hint text-muted" style="font-size: 13px">
                Chưa có tài khoản?
                <router-link
                  to="/auth/register"
                  class="text-danger fw-bold text-decoration-none ms-1"
                  @click="closeGuestModal"
                  >Đăng ký</router-link
                >
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>
