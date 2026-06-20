<template>
  <aside class="sidebar">
    <div class="logo">
      <div class="logo-icon">
        <img :src="boltIcon" alt="Logo" class="logo-img" />
      </div>
      <h2>MarcusStore Admin</h2>
    </div>

    <div class="menu-section">
      <p class="menu-title">TỔNG QUAN</p>
      <router-link to="/admin/dashboard" class="menu-item" active-class="active">
        <img :src="pieChartIcon" class="menu-icon" />
        <span>Admin Dashboard</span>
      </router-link>
    </div>

    <div class="menu-section">
      <p class="menu-title">SẢN PHẨM & KHO</p>

      <router-link to="/admin/category" class="menu-item" active-class="active">
        <img :src="layersIcon" class="menu-icon" />
        <span>Quản lý danh mục</span>
      </router-link>

      <div
        class="menu-item menu-parent"
        :class="{
          active:
            $route.path.includes('/admin/product') ||
            $route.path.includes('/admin/sku') ||
            $route.path.includes('/admin/attribute'),
        }"
        @click="toggleProductMenu"
      >
        <img :src="boxIcon" class="menu-icon" />
        <span>Quản lý sản phẩm</span>
        <img :src="chevronDownIcon" class="submenu-arrow" :class="{ open: isProductMenuOpen }" />
      </div>

      <div v-if="isProductMenuOpen" class="submenu">
        <router-link to="/admin/product" class="submenu-item" active-class="active"
          >Sản phẩm gốc</router-link
        >
        <router-link to="/admin/attribute" class="submenu-item" active-class="active"
          >Quản lý Thuộc tính</router-link
        >
        <router-link to="/admin/skugenerator" class="submenu-item" active-class="active"
          >Tạo biến thể (SKU)</router-link
        >
      </div>
    </div>

    <div class="menu-section">
      <p class="menu-title">BÁN HÀNG & MARKETING</p>

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
        <span>Quản lý Flash Sale</span>
      </router-link>

      <router-link to="/admin/post" class="menu-item" active-class="active">
        <img :src="newspaperIcon" class="menu-icon" />
        <span>Quản lý bài viết</span>
      </router-link>

      <router-link to="/admin/banner" class="menu-item" active-class="active">
        <img :src="barChartIcon" class="menu-icon" />
        <span>Quản lý Banner</span>
      </router-link>
    </div>

    <div class="menu-section">
      <p class="menu-title">CHĂM SÓC KHÁCH HÀNG</p>

      <router-link to="/admin/contact-management" class="menu-item" active-class="active">
        <i
          class="fa-solid fa-headset menu-icon d-flex align-items-center justify-content-center fs-5 text-secondary"
        ></i>
        <span>Quản lý khiếu nại</span>
      </router-link>
    </div>

    <div class="menu-section">
      <p class="menu-title">HỆ THỐNG</p>

      <router-link to="/admin/account" class="menu-item" active-class="active">
        <img :src="peopleIcon" class="menu-icon" />
        <span>Quản lý tài khoản</span>
      </router-link>

      <router-link to="/admin/role" class="menu-item" active-class="active">
        <img :src="clockHistoryIcon" class="menu-icon" />
        <span>Phân quyền (Role)</span>
      </router-link>

      <router-link to="/admin/settings" class="menu-item" active-class="active">
        <img :src="gearIcon" class="menu-icon" />
        <span>Cài đặt hệ thống</span>
      </router-link>
    </div>
  </aside>
</template>

<script setup>
// Giữ nguyên phần Script và Style của ông
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

import boltIcon from '/src/assets/icons/lightning.svg'
import pieChartIcon from '/src/assets/icons/pie-chart.svg'
import layersIcon from '/src/assets/icons/layers.svg'
import boxIcon from '/src/assets/icons/box.svg'
import cartIcon from '/src/assets/icons/cart.svg'
import peopleIcon from '/src/assets/icons/people.svg'
import tagsIcon from '/src/assets/icons/tags.svg'
import newspaperIcon from '/src/assets/icons/newspaper.svg'
import barChartIcon from '/src/assets/icons/bar-chart-line.svg'
import chevronDownIcon from '/src/assets/icons/chevron-down.svg'
import clockHistoryIcon from '/src/assets/icons/clock-history.svg'
import gearIcon from '/src/assets/icons/gear.svg'

const route = useRoute()
const isProductMenuOpen = ref(false)

const toggleProductMenu = () => {
  isProductMenuOpen.value = !isProductMenuOpen.value
}

onMounted(() => {
  if (
    route.path.includes('/admin/product') ||
    route.path.includes('/admin/sku') ||
    route.path.includes('/admin/attribute')
  ) {
    isProductMenuOpen.value = true
  }
})
</script>

<style scoped>
/* Giữ nguyên toàn bộ Style của ông (Không thay đổi) */
.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #5e4a54;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
}
.sidebar {
  width: 250px;
  height: 100vh;
  flex: 0 0 250px;
  background: white;
  border-right: 1px solid #ffdce9;
  padding: 20px 14px;
  overflow-y: auto;
  overflow-x: hidden;
}
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 36px;
}
.logo-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff7eb3, #ff4d94);
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-img {
  width: 20px;
  height: 20px;
}
.logo h2 {
  font-size: 22px;
  color: #ff4d94;
}
.menu-section {
  margin-bottom: 28px;
}
.menu-title {
  font-size: 11px;
  font-weight: 700;
  color: #c06b8d;
  margin-bottom: 12px;
  letter-spacing: 2px;
}
.menu-item:hover {
  background: #fff0f6;
}
.menu-item.active {
  background: linear-gradient(135deg, #ff8fb7, #ff5d99);
  color: white;
  box-shadow: 0 8px 18px rgba(255, 105, 160, 0.25);
}
.menu-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}
.menu-parent {
  position: relative;
}
.menu-parent span {
  flex: 1;
}
.submenu-arrow {
  width: 14px;
  height: 14px;
  transition: transform 0.3s ease;
}
.submenu-arrow.open {
  transform: rotate(180deg);
}
.submenu {
  margin: 4px 0 10px 16px;
  padding-left: 20px;
  border-left: 2px solid #ffd4e4;
  display: flex;
  flex-direction: column;
}
.submenu-item {
  padding: 10px 12px;
  margin-bottom: 6px;
  border-radius: 10px;
  color: #6b5660;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
  text-decoration: none;
  display: block;
}
.submenu-item:hover {
  background: #fff0f6;
  color: #ff4d94;
}
.submenu-item.active {
  background: #ffe3ef;
  color: #ff4d94;
  font-weight: 700;
}
@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #ffdce9;
  }
}
</style>
