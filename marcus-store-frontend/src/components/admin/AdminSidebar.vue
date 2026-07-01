<template>
  <aside class="sidebar">
    <div class="logo">
      <div class="logo-icon">
        <i class="fa-solid fa-mobile-screen-button"></i>
      </div>
      <div class="logo-text">
        <h2>Marcus Store</h2>
        <span>Admin Panel</span>
      </div>
    </div>

    <div class="menu-section">
      <p class="menu-title">TỔNG QUAN</p>
      <router-link to="/admin/dashboard" class="menu-item" active-class="active">
        <img :src="pieChartIcon" class="menu-icon" />
        <span>Bảng điều khiển</span>
      </router-link>
    </div>

    <div class="menu-section">
      <p class="menu-title">SẢN PHẨM & KHO</p>
      <router-link to="/admin/category" class="menu-item" active-class="active">
        <img :src="layersIcon" class="menu-icon" />
        <span>Quản lý danh mục</span>
      </router-link>

      <div class="menu-item menu-parent" :class="{ active: isProductMenuActive }" @click="toggleProductMenu">
        <img :src="boxIcon" class="menu-icon" />
        <span>Sản phẩm</span>
        <img :src="chevronDownIcon" class="submenu-arrow" :class="{ open: isProductMenuOpen }" />
      </div>

      <div v-if="isProductMenuOpen" class="submenu">
        <router-link to="/admin/product" class="submenu-item" active-class="active">Sản phẩm gốc</router-link>
        <router-link to="/admin/attribute" class="submenu-item" active-class="active">Thuộc tính</router-link>
        <router-link to="/admin/skugenerator" class="submenu-item" active-class="active">Tạo SKU</router-link>
      </div>
    </div>

    <div class="menu-section">
      <p class="menu-title">KÊNH BÁN HÀNG</p>
      <router-link to="/admin/order" class="menu-item" active-class="active">
        <img :src="cartIcon" class="menu-icon" />
        <span>Quản lý đơn hàng</span>
      </router-link>
      <router-link to="/admin/voucher" class="menu-item" active-class="active">
        <img :src="tagsIcon" class="menu-icon" />
        <span>Quản lý voucher</span>
      </router-link>
      <router-link to="/admin/flash-sale" class="menu-item" active-class="active">
        <img :src="tagsIcon" class="menu-icon" />
        <span>Flash Sale</span>
      </router-link>
    </div>

    <div class="menu-section">
      <p class="menu-title">NỘI DUNG</p>
      <router-link to="/admin/post" class="menu-item" active-class="active">
        <img :src="newspaperIcon" class="menu-icon" />
        <span>Bài viết</span>
      </router-link>
      <router-link to="/admin/banner" class="menu-item" active-class="active">
        <img :src="barChartIcon" class="menu-icon" />
        <span>Quản lý Banner</span>
      </router-link>
      <router-link to="/admin/contact-management" class="menu-item" active-class="active">
        <i class="fa-solid fa-headset menu-icon" style="font-size: 16px"></i>
        <span>Quản lý liên hệ</span>
      </router-link>
    </div>
    <div class="menu-section">
      <p class="menu-title">BÁO CÁO</p>
      <router-link to="/admin/finance-reports" class="menu-item" active-class="active">
        <i class="bi bi-wallet2 menu-icon"></i>
        <span>Quản lý đối soát</span>
      </router-link>
    </div>

    <div class="menu-section">
      <p class="menu-title">HỆ THỐNG</p>
      <div class="menu-item menu-parent" :class="{
        active:
          $route.path.includes('/admin/employee') || $route.path.includes('/admin/customer'),
      }" @click="toggleAccountMenu">
        <img :src="peopleIcon" class="menu-icon" />
        <span>Quản lý tài khoản</span>
        <img :src="chevronDownIcon" class="submenu-arrow" :class="{ open: isAccountMenuOpen }" />
      </div>
      <div v-if="isAccountMenuOpen" class="submenu">
        <router-link to="/admin/employee" class="submenu-item" active-class="active">Tài khoản nhân viên</router-link>
        <router-link to="/admin/customer" class="submenu-item" active-class="active">Tài khoản khách hàng</router-link>
        <router-link to="role" class="submenu-item" active-class="active">
          Phân quyền nhân viên
        </router-link>
      </div>
      <router-link to="/admin/settings" class="menu-item" active-class="active">
        <img :src="gearIcon" class="menu-icon" />
        <span>Cấu hình chung</span>
      </router-link>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import pieChartIcon from '/src/assets/icons/pie-chart.svg'
import layersIcon from '/src/assets/icons/layers.svg'
import boxIcon from '/src/assets/icons/box.svg'
import cartIcon from '/src/assets/icons/cart.svg'
import peopleIcon from '/src/assets/icons/people.svg'
import tagsIcon from '/src/assets/icons/tags.svg'
import newspaperIcon from '/src/assets/icons/newspaper.svg'
import barChartIcon from '/src/assets/icons/bar-chart-line.svg'
import chevronDownIcon from '/src/assets/icons/chevron-down.svg'
import gearIcon from '/src/assets/icons/gear.svg'

const route = useRoute()
const isProductMenuOpen = ref(false)

const isProductMenuActive = computed(() =>
  ['/admin/product', '/admin/sku', '/admin/attribute'].some((path) => route.path.includes(path)),
)

const toggleProductMenu = () => {
  isProductMenuOpen.value = !isProductMenuOpen.value
}

onMounted(() => {
  if (isProductMenuActive.value) isProductMenuOpen.value = true
})

const isAccountMenuOpen = ref(false)

const toggleAccountMenu = () => {
  isAccountMenuOpen.value = !isAccountMenuOpen.value
}

onMounted(() => {
  if (route.path.includes('/admin/employee') || route.path.includes('/admin/customer')) {
    isAccountMenuOpen.value = true
  }
})
</script>

<style scoped>
/* Sidebar tổng thể: Nền trắng sạch, viền nhẹ */
.sidebar {
  width: 260px;
  height: 100vh;
  background: #ffffff;
  border-right: 1px solid #fee2e2;
  padding: 24px 16px;
  overflow-y: auto;
}

/* Logo mới: Chữ đậm, dễ nhìn */
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
  padding-left: 8px;
}

.logo-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #ff4d94;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.logo-text h2 {
  font-size: 20px;
  font-weight: 900;
  color: #111827;
  margin: 0;
}

.logo-text span {
  font-size: 13px;
  color: #111827;
  font-weight: 700;
  text-transform: uppercase;
}

/* Menu Group Title: Đổi sang đen, đậm rõ để dễ phân biệt */
.menu-title {
  font-size: 11px;
  font-weight: 900;
  color: #000000;
  margin: 24px 0 12px 14px;
  letter-spacing: 1px;
}

/* Menu Item: Chữ đen đậm, không còn ghi nhạt */
.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: 12px;
  transition: 0.3s;
  color: #000000;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
}

.menu-item:hover {
  background: #fff5f8;
  color: #ff4d94;
}

.menu-item.active {
  background: #ff4d94;
  color: #ffffff;
  font-weight: 900;
  box-shadow: 0 4px 12px rgba(255, 77, 148, 0.3);
}

/* Submenu */
.submenu {
  margin-left: 20px;
  padding-left: 10px;
  border-left: 2px solid #fee2e2;
}

.submenu-item {
  padding: 8px 12px;
  margin-bottom: 4px;
  border-radius: 8px;
  color: #000000;
  font-size: 13px;
  font-weight: 700;
  display: block;
  text-decoration: none;
}

.submenu-item:hover {
  color: #ff4d94;
}

.submenu-item.active {
  background: #fff0f5;
  color: #ff4d94;
  font-weight: 900;
}
</style>
