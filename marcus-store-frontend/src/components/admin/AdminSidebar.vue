<template>
  <aside class="sidebar">
    <div class="logo" title="Khu vực quản trị hệ thống">
      <div class="logo-icon" :class="{ 'has-site-logo': siteLogoUrl }">
        <img v-if="siteLogoUrl" :src="siteLogoUrl" :alt="siteName" class="site-logo-image" />
        <i v-else class="fa-solid fa-mobile-screen-button"></i>
      </div>
      <div class="logo-text">
        <h2 :title="siteName">{{ siteName }}</h2>
        <span>Admin Panel</span>
      </div>
    </div>

    <!-- Marcus sắp xếp lại menu theo đúng luồng vận hành cửa hàng; route và quyền giữ nguyên. -->
    <section class="menu-section">
      <p class="menu-title">OVERVIEW</p>
      <router-link
        v-if="canAccessRoute('/admin/dashboard')"
        to="/admin/dashboard"
        class="menu-item"
        active-class="active"
      >
        <img :src="pieChartIcon" class="menu-icon" alt="" />
        <span>Dashboard</span>
      </router-link>
    </section>

    <section v-if="showSales" class="menu-section">
      <p class="menu-title">BÁN HÀNG</p>
      <router-link
        v-if="canAccessRoute('/admin/order')"
        to="/admin/order"
        class="menu-item"
        active-class="active"
      >
        <img :src="cartIcon" class="menu-icon" alt="" />
        <span>Đơn hàng</span>
      </router-link>
      <router-link
        v-if="canAccessRoute('/admin/order-assignment')"
        to="/admin/order-assignment"
        class="menu-item"
        active-class="active"
      >
        <i class="bi bi-diagram-3 menu-icon"></i>
        <span>Chia đơn hàng</span>
      </router-link>
      <!-- Marcus giữ nguyên module thành viên: chỉ ghép route bảo hành vào
           cấu trúc Sidebar mới, không thay đổi quyền hay nghiệp vụ bảo hành. -->
      <router-link
        v-if="canAccessRoute('/admin/warranty')"
        to="/admin/warranty"
        class="menu-item"
        active-class="active"
      >
        <i class="bi bi-arrow-repeat menu-icon"></i>
        <span>Đổi trả & Bảo hành</span>
      </router-link>
    </section>

    <section v-if="showSanPhamKho" class="menu-section">
      <p class="menu-title">SẢN PHẨM & KHO</p>
      <router-link
        v-if="canAccessRoute('/admin/category')"
        to="/admin/category"
        class="menu-item"
        active-class="active"
      >
        <img :src="layersIcon" class="menu-icon" alt="" />
        <span>Danh mục</span>
      </router-link>
      <template v-if="showProductParent">
        <button
          class="menu-item menu-parent"
          :class="{ active: isProductMenuActive }"
          type="button"
          @click="toggleProductMenu"
        >
          <img :src="boxIcon" class="menu-icon" alt="" />
          <span>Catalog sản phẩm</span>
          <img
            :src="chevronDownIcon"
            class="submenu-arrow"
            :class="{ open: isProductMenuOpen }"
            alt=""
          />
        </button>
        <div v-if="isProductMenuOpen" class="submenu">
          <router-link
            v-if="canAccessRoute('/admin/product')"
            to="/admin/product"
            class="submenu-item"
            active-class="active"
            >Sản phẩm</router-link
          >
          <router-link
            v-if="canAccessRoute('/admin/attribute')"
            to="/admin/attribute"
            class="submenu-item"
            active-class="active"
            >Thuộc tính sản phẩm</router-link
          >
          <!-- Marcus thêm: quản lý tập trung cấu trúc thông số theo danh mục. -->
          <router-link
            v-if="canAccessRoute('/admin/specification-sets')"
            to="/admin/specification-sets"
            class="submenu-item"
            active-class="active"
            >Bộ thông số kỹ thuật</router-link
          >
          <router-link
            v-if="canAccessRoute('/admin/skugenerator')"
            to="/admin/skugenerator"
            class="submenu-item"
            active-class="active"
            >SKU Builder</router-link
          >
        </div>
      </template>

      <template v-if="showInventoryParent">
        <button
          class="menu-item menu-parent"
          :class="{ active: isInventoryMenuActive }"
          type="button"
          @click="toggleInventoryMenu"
        >
          <i class="bi bi-box-seam menu-icon"></i>
          <span>Kho & IMEI</span>
          <img
            :src="chevronDownIcon"
            class="submenu-arrow"
            :class="{ open: isInventoryMenuOpen }"
            alt=""
          />
        </button>
        <div v-if="isInventoryMenuOpen" class="submenu">
          <router-link
            v-if="canAccessRoute('/admin/inventoryManager/with-imei')"
            to="/admin/inventoryManager/with-imei"
            class="submenu-item"
            active-class="active"
          >
            Kho có IMEI
          </router-link>
          <router-link
            v-if="canAccessRoute('/admin/inventoryManager/no-imei')"
            to="/admin/inventoryManager/no-imei"
            class="submenu-item"
            active-class="active"
          >
            Kho không IMEI
          </router-link>
        </div>
      </template>
    </section>

    <section v-if="showMarketing" class="menu-section">
      <p class="menu-title">MARKETING</p>
      <router-link
        v-if="canAccessRoute('/admin/voucher')"
        to="/admin/voucher"
        class="menu-item"
        active-class="active"
      >
        <img :src="tagsIcon" class="menu-icon" alt="" />
        <span>Voucher</span>
      </router-link>
      <router-link
        v-if="canAccessRoute('/admin/flash-sale')"
        to="/admin/flash-sale"
        class="menu-item"
        active-class="active"
      >
        <i class="bi bi-lightning-charge menu-icon"></i>
        <span>Flash Sale</span>
      </router-link>
      <router-link
        v-if="canAccessRoute('/admin/banner')"
        to="/admin/banner"
        class="menu-item"
        active-class="active"
      >
        <img :src="barChartIcon" class="menu-icon" alt="" />
        <span>Banner</span>
      </router-link>
    </section>

    <section v-if="showCustomerCare" class="menu-section">
      <p class="menu-title">CUSTOMER CARE</p>
      <router-link
        v-if="canAccessRoute('/admin/customer')"
        to="/admin/customer"
        class="menu-item"
        active-class="active"
      >
        <i class="bi bi-person-heart menu-icon"></i>
        <span>Khách hàng</span>
      </router-link>
      <router-link
        v-if="canAccessRoute('/admin/contact-management')"
        to="/admin/contact-management"
        class="menu-item"
        active-class="active"
      >
        <i class="fa-solid fa-headset menu-icon"></i>
        <span>Liên hệ hỗ trợ</span>
      </router-link>
      <router-link
        v-if="canAccessRoute('/admin/reviews')"
        to="/admin/reviews"
        class="menu-item"
        active-class="active"
      >
        <i class="bi bi-star menu-icon"></i>
        <span>Đánh giá & bình luận</span>
      </router-link>
    </section>

    <section v-if="canAccessRoute('/admin/post')" class="menu-section">
      <p class="menu-title">CONTENT</p>
      <router-link to="/admin/post" class="menu-item" active-class="active">
        <img :src="newspaperIcon" class="menu-icon" alt="" />
        <span>Bài viết</span>
      </router-link>
    </section>

    <section v-if="showBaoCao" class="menu-section">
      <p class="menu-title">ANALYTICS & FINANCE</p>
      <router-link
        v-if="canAccessRoute('/admin/analytics')"
        to="/admin/analytics"
        class="menu-item"
        active-class="active"
      >
        <i class="bi bi-graph-up-arrow menu-icon"></i>
        <span>AI Business Analytics</span>
      </router-link>
      <router-link
        v-if="canAccessRoute('/admin/finance-reports')"
        to="/admin/finance-reports"
        class="menu-item"
        active-class="active"
      >
        <i class="bi bi-wallet2 menu-icon"></i>
        <span>Đối soát tài chính</span>
      </router-link>
    </section>

    <section v-if="showHumanResources" class="menu-section">
      <p class="menu-title">NHÂN SỰ & QUYỀN</p>
      <router-link
        v-if="canAccessRoute('/admin/employee')"
        to="/admin/employee"
        class="menu-item"
        active-class="active"
      >
        <img :src="peopleIcon" class="menu-icon" alt="" />
        <span>Nhân viên</span>
      </router-link>
      <router-link
        v-if="canAccessRoute('/admin/role')"
        to="/admin/role"
        class="menu-item"
        active-class="active"
      >
        <i class="fa-solid fa-shield-halved menu-icon"></i>
        <span>Access Control</span>
      </router-link>
    </section>

    <section v-if="showHeThong" class="menu-section">
      <p class="menu-title">SYSTEM</p>
      <router-link
        v-if="canAccessRoute('/admin/settings')"
        to="/admin/settings"
        class="menu-item"
        active-class="active"
      >
        <img :src="gearIcon" class="menu-icon" alt="" />
        <span>Website Settings</span>
      </router-link>
      <router-link
        v-if="canAccessRoute('/admin/data-backup')"
        to="/admin/data-backup"
        class="menu-item"
        active-class="active"
      >
        <i class="bi bi-database-down menu-icon"></i>
        <span>Data Backup</span>
      </router-link>
      <router-link
        v-if="canAccessRoute('/admin/activity-log')"
        to="/admin/activity-log"
        class="menu-item"
        active-class="active"
      >
        <i class="bi bi-clock-history menu-icon"></i>
        <span>Audit Log</span>
      </router-link>
    </section>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSettings } from '@/composables/useSettings'

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
const router = useRouter()
const { siteName, siteLogoUrl, fetchSettings } = useSettings()

onMounted(fetchSettings)

/* ===========================
   USER INFO
=========================== */

const roles = JSON.parse(localStorage.getItem('USER_ROLE') || '[]')

const permissions = JSON.parse(localStorage.getItem('USER_PERMISSIONS') || '[]')

const userRoles = ref(Array.isArray(roles) ? roles : [roles])

const myPermissions = ref(Array.isArray(permissions) ? permissions : [permissions])

const isAdmin = computed(() => userRoles.value.includes('ROLE_ADMIN'))

/* ===========================
   CHECK ROUTE PERMISSION
=========================== */

function canAccessRoute(path) {
  if (isAdmin.value) return true

  const resolved = router.resolve(path)

  if (!resolved.matched.length) {
    return false
  }

  const requiredRoles = resolved.meta?.roles || []
  const requiredPermission = resolved.meta?.permission

  if (requiredRoles.length && !userRoles.value.some((role) => requiredRoles.includes(role))) {
    return false
  }

  if (requiredPermission && !myPermissions.value.includes(requiredPermission)) {
    return false
  }

  return true
}

/* ===========================
   SHOW MENU
=========================== */

const showProductParent = computed(() => {
  return (
    canAccessRoute('/admin/product') ||
    canAccessRoute('/admin/attribute') ||
    canAccessRoute('/admin/specification-sets') ||
    canAccessRoute('/admin/skugenerator')
  )
})

const showInventoryParent = computed(() => {
  return (
    canAccessRoute('/admin/inventoryManager/with-imei') ||
    canAccessRoute('/admin/inventoryManager/no-imei')
  )
})

const showSanPhamKho = computed(() => {
  return canAccessRoute('/admin/category') || showInventoryParent.value || showProductParent.value
})

// Marcus thêm: nhóm Bán hàng hiển thị nếu người dùng được xem đơn hàng hoặc
// module đổi trả/bảo hành vừa được tích hợp từ nhánh thành viên.
const showSales = computed(() => {
  return (
    canAccessRoute('/admin/order') ||
    canAccessRoute('/admin/order-assignment') ||
    canAccessRoute('/admin/warranty')
  )
})

const showMarketing = computed(() => {
  return (
    canAccessRoute('/admin/voucher') ||
    canAccessRoute('/admin/flash-sale') ||
    canAccessRoute('/admin/banner')
  )
})

const showCustomerCare = computed(() => {
  return (
    canAccessRoute('/admin/customer') ||
    canAccessRoute('/admin/contact-management') ||
    canAccessRoute('/admin/reviews')
  )
})

const showBaoCao = computed(() => {
  return canAccessRoute('/admin/analytics') || canAccessRoute('/admin/finance-reports')
})

const showHeThong = computed(() => {
  return (
    canAccessRoute('/admin/settings') ||
    canAccessRoute('/admin/data-backup') ||
    canAccessRoute('/admin/activity-log')
  )
})

const showHumanResources = computed(() => {
  return canAccessRoute('/admin/employee') || canAccessRoute('/admin/role')
})

/* ===========================
   SUBMENU
=========================== */

const isProductMenuOpen = ref(false)
const isInventoryMenuOpen = ref(false)

const isProductMenuActive = computed(() => {
  return [
    '/admin/product',
    '/admin/attribute',
    '/admin/specification-sets',
    '/admin/skugenerator',
  ].some((path) => route.path.startsWith(path))
})

const isInventoryMenuActive = computed(() => route.path.startsWith('/admin/inventoryManager'))

/* ===========================
   TOGGLE MENU
=========================== */

const toggleProductMenu = () => {
  isProductMenuOpen.value = !isProductMenuOpen.value
}

const toggleInventoryMenu = () => {
  isInventoryMenuOpen.value = !isInventoryMenuOpen.value
}

/* ===========================
   INIT
=========================== */

onMounted(() => {
  // Tự động mở submenu Sản phẩm nếu đang ở các trang liên quan
  if (isProductMenuActive.value) {
    isProductMenuOpen.value = true
  }

  if (isInventoryMenuActive.value) {
    isInventoryMenuOpen.value = true
  }
})
</script>
<style scoped>
/* Sidebar tổng thể: Nền trắng sạch, viền nhẹ */
.sidebar {
  width: clamp(260px, 20vw, 292px);
  flex: 0 0 clamp(260px, 20vw, 292px);
  height: 100vh;
  background: #ffffff;
  border-right: 1px solid #e8edf5;
  padding: 18px 14px 24px;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
  transition:
    width 0.25s ease,
    flex-basis 0.25s ease,
    padding 0.25s ease,
    transform 0.25s ease;
}

.sidebar::-webkit-scrollbar {
  width: 5px;
}

.sidebar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
}

/* Logo mới: Chữ đậm, dễ nhìn */
.logo {
  display: flex;
  align-items: center;
  gap: 13px;
  margin-bottom: 24px;
  padding: 10px;
  min-width: 0;
  border: 1px solid #edf1f7;
  border-radius: 16px;
  background: linear-gradient(135deg, #ffffff 0%, #fff7f8 100%);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.logo-icon {
  width: 58px;
  height: 58px;
  border-radius: 12px;
  background: #ff4d94;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex: 0 0 58px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

/* Marcus sửa: logo tải lên dùng nền trung tính để không bị chìm trên nền hồng/đỏ. */
.logo-icon.has-site-logo {
  padding: 4px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.09);
}

.logo-icon .site-logo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 7px;
}

.logo-text {
  min-width: 0;
  flex: 1;
}

.logo-text h2 {
  font-size: clamp(16px, 1.28vw, 19px);
  font-weight: 900;
  color: #111827;
  margin: 0;
  line-height: 1.12;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  overflow-wrap: anywhere;
}

.logo-text span {
  display: block;
  margin-top: 5px;
  font-size: 11px;
  color: #64748b;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
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
  width: 100%;
  border: 0;
  background: transparent;
  text-align: left;
  font-family: inherit;
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

@media (max-width: 1180px) and (min-width: 901px) {
  .sidebar {
    width: 250px;
    flex-basis: 250px;
    padding-inline: 11px;
  }

  .logo {
    gap: 10px;
    padding: 8px;
  }

  .logo-icon {
    width: 52px;
    height: 52px;
    flex-basis: 52px;
  }

  .menu-item {
    gap: 11px;
    padding-inline: 13px;
  }
}

@media (max-width: 900px) {
  .sidebar {
    width: min(86vw, 292px);
    flex-basis: min(86vw, 292px);
    border-right: 0;
  }
}
</style>
