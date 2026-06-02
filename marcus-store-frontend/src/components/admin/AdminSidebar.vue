<template>
  <aside class="sidebar">
    <!-- LOGO -->
    <div class="logo">
      <div class="logo-icon">
        <img :src="boltIcon" alt="" class="logo-img" />
      </div>
      <h2>MarcusStore Admin</h2>
    </div>

    <!-- TỔNG QUAN -->
    <div class="menu-section">
      <p class="menu-title">TỔNG QUAN</p>

      <div
        class="menu-item"
        :class="{ active: currentPage === 'dashboard' }"
        @click="$emit('changePage', 'dashboard')"
      >
        <img :src="pieChartIcon" class="menu-icon" />
        <span>Admin Dashboard</span>
      </div>
    </div>

    <!-- KINH DOANH -->
    <div class="menu-section">
      <p class="menu-title">KINH DOANH</p>

      <!-- Danh mục -->
      <div
        class="menu-item"
        :class="{ active: currentPage === 'category' }"
        @click="$emit('changePage', 'category')"
      >
        <img :src="layersIcon" class="menu-icon" />
        <span>Quản lý danh mục</span>
      </div>

      <!-- Sản phẩm -->
      <div
        class="menu-item menu-parent"
        :class="{ active: productPages.includes(currentPage) }"
        @click="toggleProductMenu"
      >
        <img :src="boxIcon" class="menu-icon" />
        <span>Quản lý sản phẩm</span>

        <img
          :src="chevronDownIcon"
          class="submenu-arrow"
          :class="{ open: isProductMenuOpen }"
        />
      </div>

      <div v-if="isProductMenuOpen" class="submenu">
        <div
          class="submenu-item"
          :class="{ active: currentPage === 'base-product' }"
          @click="$emit('changePage', 'base-product')"
        >
          Sản phẩm gốc
        </div>

        <div
          class="submenu-item"
          :class="{ active: currentPage === 'product-variant' }"
          @click="$emit('changePage', 'product-variant')"
        >
          Quản lý biến thể
        </div>

        <div
          class="submenu-item"
          :class="{ active: currentPage === 'inventory' }"
          @click="$emit('changePage', 'inventory')"
        >
          Quản lý tồn kho
        </div>
      </div>

      <!-- Đơn hàng -->
      <div
        class="menu-item"
        :class="{ active: currentPage === 'order' }"
        @click="$emit('changePage', 'order')"
      >
        <img :src="cartIcon" class="menu-icon" />
        <span>Quản lý đơn hàng</span>
      </div>

      <!-- Khách hàng -->
      <div
        class="menu-item"
        :class="{ active: currentPage === 'customer' }"
        @click="$emit('changePage', 'customer')"
      >
        <img :src="peopleIcon" class="menu-icon" />
        <span>Quản lý khách hàng</span>
      </div>

      <!-- Voucher -->
      <div
        class="menu-item"
        :class="{ active: currentPage === 'voucher' }"
        @click="$emit('changePage', 'voucher')"
      >
        <img :src="tagsIcon" class="menu-icon" />
        <span>Quản lý voucher</span>
      </div>
    </div>

    <!-- NỘI DUNG -->
    <div class="menu-section">
      <p class="menu-title">NỘI DUNG VÀ PHÂN TÍCH</p>

      <div
        class="menu-item"
        :class="{ active: currentPage === 'post' }"
        @click="$emit('changePage', 'post')"
      >
        <img :src="newspaperIcon" class="menu-icon" />
        <span>Quản lý bài viết</span>
      </div>

      <div
        class="menu-item"
        :class="{ active: currentPage === 'report' }"
        @click="$emit('changePage', 'report')"
      >
        <img :src="barChartIcon" class="menu-icon" />
        <span>Báo cáo doanh thu</span>
      </div>
    </div>

    <!-- HỆ THỐNG -->
    <div class="menu-section">
      <p class="menu-title">HỆ THỐNG</p>

      <div
        class="menu-item"
        :class="{ active: currentPage === 'change-history' }"
        @click="$emit('changePage', 'change-history')"
      >
        <img :src="clockHistoryIcon" class="menu-icon" />
        <span>Lịch sử thay đổi</span>
      </div>

      <div
        class="menu-item"
        :class="{ active: currentPage === 'system-setting' }"
        @click="$emit('changePage', 'system-setting')"
      >
        <img :src="gearIcon" class="menu-icon" />
        <span>Setting system</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  currentPage: {
    type: String,
    default: 'dashboard'
  }
})

defineEmits(['changePage'])

/* ICONS */
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


/* PRODUCT MENU */
const isProductMenuOpen = ref(false)

const productPages = [
  'base-product',
  'product-variant',
  'inventory'
]

const toggleProductMenu = () => {
  isProductMenuOpen.value = !isProductMenuOpen.value
}
</script>

<style scoped>
.sidebar {
  width: 250px;
  background: white;
  border-right: 1px solid #ffdce9;
  padding: 20px 14px;
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
  margin: -2px 0 10px 32px;
  padding-left: 14px;
  border-left: 2px solid #ffd4e4;
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