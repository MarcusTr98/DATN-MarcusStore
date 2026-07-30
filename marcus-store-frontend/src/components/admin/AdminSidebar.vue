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

    <div v-if="showSanPhamKho" class="menu-section">
      <p class="menu-title">SẢN PHẨM & KHO</p>

      <router-link v-if="canAccessRoute('/admin/category')" to="/admin/category" class="menu-item"
        active-class="active">
        <img :src="layersIcon" class="menu-icon" />
        <span>Quản lý danh mục</span>
      </router-link>

      <router-link v-if="canAccessRoute('/admin/inventoryManager')" to="/admin/inventoryManager" class="menu-item"
        active-class="active">
        <i class="bi bi-box-seam menu-icon" style="font-size: 16px"></i>
        <span>Quản lý kho</span>
      </router-link>

      <template v-if="showProductParent">
        <div class="menu-item menu-parent" :class="{ active: isProductMenuActive }" @click="toggleProductMenu">
          <img :src="boxIcon" class="menu-icon" />
          <span>Sản phẩm</span>
          <img :src="chevronDownIcon" class="submenu-arrow" :class="{ open: isProductMenuOpen }" />
        </div>

        <div v-if="isProductMenuOpen" class="submenu">
          <router-link v-if="canAccessRoute('/admin/product')" to="/admin/product" class="submenu-item"
            active-class="active">Sản phẩm gốc</router-link>
          <router-link v-if="canAccessRoute('/admin/attribute')" to="/admin/attribute" class="submenu-item"
            active-class="active">Thuộc tính</router-link>
          <router-link v-if="canAccessRoute('/admin/skugenerator')" to="/admin/skugenerator" class="submenu-item"
            active-class="active">Tạo SKU</router-link>
        </div>
      </template>
    </div>

    <div v-if="showKenhBanHang" class="menu-section">
      <p class="menu-title">KÊNH BÁN HÀNG</p>

      <router-link v-if="canAccessRoute('/admin/order')" to="/admin/order" class="menu-item" active-class="active">
        <img :src="cartIcon" class="menu-icon" />
        <span>Quản lý đơn hàng</span>
      </router-link>

      <router-link v-if="canAccessRoute('/admin/voucher')" to="/admin/voucher" class="menu-item" active-class="active">
        <img :src="tagsIcon" class="menu-icon" />
        <span>Quản lý voucher</span>
      </router-link>

      <router-link v-if="canAccessRoute('/admin/flash-sale')" to="/admin/flash-sale" class="menu-item"
        active-class="active">
        <img :src="tagsIcon" class="menu-icon" />
        <span>Flash Sale</span>
      </router-link>
    </div>

    <div v-if="showNoiDung" class="menu-section">
      <p class="menu-title">NỘI DUNG</p>

      <router-link v-if="canAccessRoute('/admin/post')" to="/admin/post" class="menu-item" active-class="active">
        <img :src="newspaperIcon" class="menu-icon" />
        <span>Bài viết</span>
      </router-link>

      <router-link v-if="canAccessRoute('/admin/banner')" to="/admin/banner" class="menu-item" active-class="active">
        <img :src="barChartIcon" class="menu-icon" />
        <span>Quản lý Banner</span>
      </router-link>

      <router-link v-if="canAccessRoute('/admin/contact-management')" to="/admin/contact-management" class="menu-item"
        active-class="active">
        <i class="fa-solid fa-headset menu-icon" style="font-size: 16px"></i>
        <span>Quản lý liên hệ</span>
      </router-link>
          <router-link
    v-if="canAccessRoute('/admin/reviews')"
    to="/admin/reviews"
    class="menu-item"
    active-class="active"
  >
    <i class="bi bi-star menu-icon"></i>
    <span>Quản lý đánh giá & bình luận</span>
  </router-link>
    </div>
    <div v-if="showBaoCao" class="menu-section">
      <p class="menu-title">BÁO CÁO</p>
      <router-link v-if="canAccessRoute('/admin/analytics')" to="/admin/analytics" class="menu-item"
        active-class="active">
        <i class="bi bi-graph-up-arrow menu-icon"></i>
        <span>Phân tích kinh doanh</span>
      </router-link>
      <router-link v-if="canAccessRoute('/admin/finance-reports')" to="/admin/finance-reports" class="menu-item"
        active-class="active">
        <i class="bi bi-wallet2 menu-icon"></i>
        <span>Quản lý đối soát</span>
      </router-link>
        <router-link
    v-if="canAccessRoute('/admin/activity-log')"
    to="/admin/activity-log"
    class="menu-item"
    active-class="active"
  >
    <i class="bi bi-clock-history menu-icon"></i>
    <span>Quản lý thao tác</span>
  </router-link>
    </div>

    <div v-if="showHeThong" class="menu-section">
      <p class="menu-title">HỆ THỐNG</p>

      <!-- Quản lý tài khoản: chỉ còn Tài khoản nhân viên + Tài khoản khách hàng -->
      <template v-if="showAccountParent">
        <div class="menu-item menu-parent" :class="{ active: isAccountMenuActive }" @click="toggleAccountMenu">
          <img :src="peopleIcon" class="menu-icon" />
          <span>Quản lý tài khoản</span>
          <img :src="chevronDownIcon" class="submenu-arrow" :class="{ open: isAccountMenuOpen }" />
        </div>
        <div v-if="isAccountMenuOpen" class="submenu">
          <router-link v-if="canAccessRoute('/admin/employee')" to="/admin/employee" class="submenu-item"
            active-class="active">Tài khoản nhân viên</router-link>
          <router-link v-if="canAccessRoute('/admin/customer')" to="/admin/customer" class="submenu-item"
            active-class="active">Tài khoản khách hàng</router-link>
        </div>
      </template>

      <!-- Quản lý phân quyền: route chỉ yêu cầu roles: ['ROLE_ADMIN'], không có permission -->
      <router-link v-if="canAccessRoute('/admin/role')" to="/admin/role" class="menu-item" active-class="active">
        <i class="fa-solid fa-shield-halved menu-icon" style="font-size: 16px"></i>
        <span>Quản lý phân quyền</span>
      </router-link>

      <router-link v-if="canAccessRoute('/admin/settings')" to="/admin/settings" class="menu-item"
        active-class="active">
        <img :src="gearIcon" class="menu-icon" />
        <span>Cấu hình chung</span>
      </router-link>

      <router-link v-if="canAccessRoute('/admin/data-backup')" to="/admin/data-backup" class="menu-item"
        active-class="active">
        <i class="bi bi-database-down menu-icon"></i>
        <span>Sao lưu dữ liệu</span>
      </router-link>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";

import pieChartIcon from "/src/assets/icons/pie-chart.svg";
import layersIcon from "/src/assets/icons/layers.svg";
import boxIcon from "/src/assets/icons/box.svg";
import cartIcon from "/src/assets/icons/cart.svg";
import peopleIcon from "/src/assets/icons/people.svg";
import tagsIcon from "/src/assets/icons/tags.svg";
import newspaperIcon from "/src/assets/icons/newspaper.svg";
import barChartIcon from "/src/assets/icons/bar-chart-line.svg";
import chevronDownIcon from "/src/assets/icons/chevron-down.svg";
import gearIcon from "/src/assets/icons/gear.svg";

const route = useRoute();
const router = useRouter();

/* ===========================
   USER INFO
=========================== */

const roles = JSON.parse(localStorage.getItem("USER_ROLE") || "[]");

const permissions = JSON.parse(
  localStorage.getItem("USER_PERMISSIONS") || "[]"
);

const userRoles = ref(Array.isArray(roles) ? roles : [roles]);

const myPermissions = ref(
  Array.isArray(permissions) ? permissions : [permissions]
);

const isAdmin = computed(() =>
  userRoles.value.includes("ROLE_ADMIN")
);

/* ===========================
   CHECK ROUTE PERMISSION
=========================== */

function canAccessRoute(path) {
  if (isAdmin.value) return true;

  const resolved = router.resolve(path);

  if (!resolved.matched.length) {
    return false;
  }

  const requiredRoles = resolved.meta?.roles || [];
  const requiredPermission = resolved.meta?.permission;

  if (
    requiredRoles.length &&
    !userRoles.value.some(role =>
      requiredRoles.includes(role)
    )
  ) {
    return false;
  }

  if (
    requiredPermission &&
    !myPermissions.value.includes(requiredPermission)
  ) {
    return false;
  }

  return true;
}

/* ===========================
   SHOW MENU
=========================== */

const showProductParent = computed(() => {
  return (
    canAccessRoute("/admin/product") ||
    canAccessRoute("/admin/attribute") ||
    canAccessRoute("/admin/skugenerator")
  );
});

const showAccountParent = computed(() => {
  return (
    canAccessRoute("/admin/employee") ||
    canAccessRoute("/admin/customer")
  );
});

const showSanPhamKho = computed(() => {
  return (
    canAccessRoute("/admin/category") ||
    canAccessRoute("/admin/inventoryManager") ||
    showProductParent.value
  );
});

const showKenhBanHang = computed(() => {
  return (
    canAccessRoute("/admin/order") ||
    canAccessRoute("/admin/voucher") ||
    canAccessRoute("/admin/flash-sale")
  );
});

const showNoiDung = computed(() => {
  return (
    canAccessRoute("/admin/post") ||
    canAccessRoute("/admin/banner") ||
    canAccessRoute("/admin/contact-management")||
     canAccessRoute("/admin/reviews")
  );
});

const showBaoCao = computed(() => {
  return (
    canAccessRoute("/admin/analytics") ||
    canAccessRoute("/admin/finance-reports") ||
    canAccessRoute("/admin/activity-log")
  );
});

const showHeThong = computed(() => {
  return (
    showAccountParent.value ||
    canAccessRoute("/admin/role") ||
    canAccessRoute("/admin/settings") ||
    canAccessRoute("/admin/data-backup")
  );
});

/* ===========================
   SUBMENU
=========================== */

const isProductMenuOpen = ref(false);

const isAccountMenuOpen = ref(false);

const isProductMenuActive = computed(() => {
  return [
    "/admin/product",
    "/admin/attribute",
    "/admin/skugenerator",
  ].some(path => route.path.startsWith(path));
});

const isAccountMenuActive = computed(() => {
  return [
    "/admin/employee",
    "/admin/customer",
  ].some(path => route.path.startsWith(path));
});
/* ===========================
   TOGGLE MENU
=========================== */

const toggleProductMenu = () => {
  isProductMenuOpen.value = !isProductMenuOpen.value;
};

const toggleAccountMenu = () => {
  isAccountMenuOpen.value = !isAccountMenuOpen.value;
};

/* ===========================
   INIT
=========================== */

onMounted(() => {
  // Tự động mở submenu Sản phẩm nếu đang ở các trang liên quan
  if (isProductMenuActive.value) {
    isProductMenuOpen.value = true;
  }

  // Tự động mở submenu Quản lý tài khoản nếu đang ở các trang liên quan
  if (isAccountMenuActive.value) {
    isAccountMenuOpen.value = true;
  }
});
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
