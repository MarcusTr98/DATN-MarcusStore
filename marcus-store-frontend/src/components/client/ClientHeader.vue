<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useCartStore } from '@/stores/cartStore'
import { useRouter } from 'vue-router'
import { useSettings } from '@/composables/useSettings'
import BaseModal from '../BaseModal.vue'
import wishlist from '@/composables/useWishlistShared'

const router = useRouter()
const cartStore = useCartStore()

const totalMoney = computed(() => cartStore.totalAmount)
const totalQuantity = computed(() => cartStore.totalQuantity)
const isLoggedIn = ref(false)
const userName = ref('')
const searchQuery = ref('')

const wishlistCount = computed(() => (wishlist.isLoaded() ? wishlist.totalCount() : 0))

const { sysSettings, fetchSettings } = useSettings()

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
  localStorage.removeItem('ACCESS_TOKEN')
  localStorage.removeItem('USER_ROLE')
  localStorage.removeItem('USERNAME')

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

onMounted(() => {
  checkAuth()

  if (localStorage.getItem('ACCESS_TOKEN')) {
    cartStore.fetchCart()
    wishlist.fetchIds()
  } else {
    wishlist.reset()
  }

  fetchSettings()
  window.addEventListener('auth-changed', checkAuth)
})
onUnmounted(() => {
  window.removeEventListener('auth-changed', checkAuth)
})
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
            <router-link to="/profile/orders" class="topbar-item topbar-link">
              <i class="fas fa-truck"></i>
              <span>Theo dõi đơn hàng</span>
            </router-link>
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
            <div class="logo-icon"><i class="fas fa-mobile-alt"></i></div>
            <div class="logo-text">
              <span class="logo-brand">Marcus</span>
              <span class="logo-store">STORE</span>
            </div>
          </router-link>

          <!-- Search Bar -->
          <div class="search-bar-wrapper">
            <div class="search-bar">
              <i class="fas fa-search search-icon"></i>
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="Tìm kiếm điện thoại, phụ kiện..."
              />
              <button class="search-btn">Tìm kiếm</button>
            </div>
            <div class="search-tags">
              <span class="search-tag-label">Hot:</span>
              <router-link to="/search?q=iphone+16" class="search-tag">iPhone 16</router-link>
              <router-link to="/search?q=samsung+s25" class="search-tag">Samsung S25</router-link>
              <router-link to="/search?q=airpods" class="search-tag">AirPods</router-link>
              <router-link to="/search?q=sac-du-phong" class="search-tag">Sạc dự phòng</router-link>
            </div>
          </div>

          <!-- Header Actions -->
          <div class="header-actions">
            <!-- Account -->
            <div class="h-action">
              <template v-if="!isLoggedIn">
                <router-link to="/auth/login" class="h-action-btn">
                  <div class="h-action-icon"><i class="far fa-user"></i></div>
                  <div class="h-action-text">
                    <span class="h-action-sub">Đăng nhập</span>
                    <span class="h-action-main">Tài khoản</span>
                  </div>
                </router-link>
              </template>
              <template v-else>
                <div class="dropdown">
                  <a href="#" class="h-action-btn" data-bs-toggle="dropdown">
                    <div class="h-action-icon active"><i class="far fa-user"></i></div>
                    <div class="h-action-text">
                      <span class="h-action-sub">Xin chào,</span>
                      <span class="h-action-main">{{ userName }}</span>
                    </div>
                  </a>
                  <ul class="dropdown-menu dropdown-menu-end ms-dropdown shadow">
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
            <router-link to="/profile/wishlist" class="h-action-btn">
              <div class="h-action-icon position-relative">
                <i class="far fa-heart"></i>
                <span v-if="wishlistCount > 0" class="cart-count">{{ wishlistCount }}</span>
              </div>
              <div class="h-action-text d-none d-xl-block">
                <span class="h-action-sub d-block">Yêu thích</span>
                <span class="h-action-main d-block">Sản phẩm</span>
              </div>
            </router-link>

            <!-- Cart -->
            <router-link to="/cart" class="h-action-btn cart-action">
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
            </router-link>
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
          <li class="nav-item dropdown dropdown-hover">
            <a
              href="#"
              class="nav-link fw-semibold text-dark px-1 py-2 rounded d-flex align-items-center"
              @click.prevent
            >
              <i class="fas fa-bars me-2"></i> Danh mục
              <i class="fas fa-chevron-down ms-2" style="font-size: 10px"></i>
            </a>

            <!-- Menu xổ xuống -->
            <ul class="dropdown-menu border-0 shadow-lg mt-0 rounded-3 p-2">
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
  <BaseModal
    :visible="showLogoutModal"
    type="confirm"
    title="Đăng xuất"
    message="Bạn có chắc chắn muốn đăng xuất?"
    :show-confirm="true"
    @close="cancelLogout"
    @confirm="confirmLogout"
  />
</template>
