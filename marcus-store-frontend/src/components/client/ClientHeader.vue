<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const totalMoney = ref(0)
const totalQuantity = ref(0)
const isLoggedIn = ref(false)
const userName = ref('')
const searchQuery = ref('')
//const isMobileMenuOpen = ref(false)

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

const handleLogout = () => {
  if (!confirm('Bạn có chắc chắn muốn đăng xuất?')) return
  localStorage.removeItem('ACCESS_TOKEN')
  localStorage.removeItem('USER_ROLE')
  localStorage.removeItem('USERNAME')
  isLoggedIn.value = false
  router.push('/login')
}

const updateCartHeader = () => {
  const cart = JSON.parse(localStorage.getItem('marcus_cart')) || []
  totalMoney.value = cart.reduce((total, item) => total + item.price * item.quantity, 0)
  totalQuantity.value = cart.reduce((total, item) => total + item.quantity, 0)
}

onMounted(() => {
  checkAuth()
  updateCartHeader()
  window.addEventListener('cart-updated', updateCartHeader)
  window.addEventListener('auth-changed', checkAuth)
})

onUnmounted(() => {
  window.removeEventListener('cart-updated', updateCartHeader)
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
              <span>Khuyến mãi hôm nay: Giảm đến <strong>50%</strong> phụ kiện chính hãng</span>
            </span>
          </div>
          <div class="topbar-right">
            <a href="tel:0907640098" class="topbar-item topbar-link">
              <i class="fas fa-headset"></i>
              <span>Hotline: <strong>(+84) 907 640 098</strong></span>
            </a>
            <span class="topbar-divider">|</span>
            <router-link to="/he-thong-cua-hang" class="topbar-item topbar-link">
              <i class="fas fa-map-marker-alt"></i>
              <span>Hệ thống cửa hàng</span>
            </router-link>
            <span class="topbar-divider">|</span>
            <router-link to="/theo-doi-don-hang" class="topbar-item topbar-link">
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
                <router-link to="/login" class="h-action-btn">
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
                    <li>
                      <router-link class="dropdown-item" to="/my-orders">
                        <i class="fas fa-box-open me-2"></i>Đơn mua
                      </router-link>
                    </li>
                    <li>
                      <router-link class="dropdown-item" to="/wishlist">
                        <i class="far fa-heart me-2"></i>Sản phẩm yêu thích
                      </router-link>
                    </li>
                    <li><hr class="dropdown-divider mx-3 my-1" /></li>
                    <li>
                      <a class="dropdown-item text-danger" href="#" @click.prevent="handleLogout">
                        <i class="fas fa-sign-out-alt me-2"></i>Đăng xuất
                      </a>
                    </li>
                  </ul>
                </div>
              </template>
            </div>

            <!-- Wishlist -->
            <router-link to="/wishlist" class="h-action-btn">
              <div class="h-action-icon"><i class="far fa-heart"></i></div>
              <div class="h-action-text d-none d-xl-block">
                <span class="h-action-sub">Yêu thích</span>
                <span class="h-action-main">Sản phẩm</span>
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
    <nav class="cat-nav">
      <div class="container">
        <div class="cat-nav-inner">
          <router-link to="/category/dien-thoai" class="cat-nav-item">
            <i class="fas fa-mobile-alt"></i>
            <span>Điện thoại</span>
          </router-link>
          <router-link to="/category/may-tinh-bang" class="cat-nav-item">
            <i class="fas fa-tablet-alt"></i>
            <span>Máy tính bảng</span>
          </router-link>
          <router-link to="/category/am-thanh" class="cat-nav-item">
            <i class="fas fa-headphones-alt"></i>
            <span>Âm thanh</span>
          </router-link>
          <router-link to="/category/dong-ho-thong-minh" class="cat-nav-item">
            <i class="fas fa-clock"></i>
            <span>Đồng hồ thông minh</span>
          </router-link>
          <router-link to="/category/sac-pin" class="cat-nav-item">
            <i class="fas fa-battery-full"></i>
            <span>Sạc & Pin</span>
          </router-link>
          <router-link to="/category/op-lung" class="cat-nav-item">
            <i class="fas fa-shield-alt"></i>
            <span>Ốp lưng & Bảo vệ</span>
          </router-link>
          <router-link to="/category/phu-kien" class="cat-nav-item">
            <i class="fas fa-plug"></i>
            <span>Phụ kiện khác</span>
          </router-link>
          <router-link to="/khuyen-mai" class="cat-nav-item cat-nav-sale">
            <i class="fas fa-fire"></i>
            <span>Khuyến mãi</span>
          </router-link>
        </div>
      </div>
    </nav>
  </header>
</template>
