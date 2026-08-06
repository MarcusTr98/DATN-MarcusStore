<template>
  <div class="admin-layout">
    <button
      class="desktop-sidebar-toggle no-print"
      :class="{ 'is-collapsed': sidebarCollapsed }"
      type="button"
      :aria-label="sidebarCollapsed ? 'Hiện thanh điều hướng' : 'Ẩn thanh điều hướng'"
      :title="sidebarCollapsed ? 'Hiện sidebar' : 'Ẩn sidebar'"
      @click="toggleDesktopSidebar"
    >
      <i :class="sidebarCollapsed ? 'fa-solid fa-angles-right' : 'fa-solid fa-angles-left'"></i>
    </button>

    <button
      class="mobile-sidebar-toggle no-print"
      :class="{ 'is-open': mobileSidebarOpen }"
      type="button"
      :aria-label="mobileSidebarOpen ? 'Đóng menu quản trị' : 'Mở menu quản trị'"
      :aria-expanded="mobileSidebarOpen"
      @click="mobileSidebarOpen = !mobileSidebarOpen"
    >
      <i :class="mobileSidebarOpen ? 'fa-solid fa-xmark' : 'fa-solid fa-bars'"></i>
    </button>

    <div v-if="mobileSidebarOpen" class="sidebar-backdrop no-print" @click="mobileSidebarOpen = false"></div>
    <admin-sidebar class="admin-sidebar no-print"
      :class="{ 'is-open': mobileSidebarOpen, 'is-collapsed': sidebarCollapsed }" />

    <main class="main-content">
      <admin-header class="no-print" />
      <div class="admin-view">
        <router-view></router-view>
      </div>
    </main>
    <AdminChatInbox />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AdminHeader from '@/components/admin/AdminHeader.vue'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import AdminChatInbox from '@/components/chat/AdminChatInbox.vue'

const route = useRoute()
const sidebarCollapsed = ref(localStorage.getItem('ADMIN_SIDEBAR_COLLAPSED') === 'true')
const mobileSidebarOpen = ref(false)

const toggleDesktopSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('ADMIN_SIDEBAR_COLLAPSED', String(sidebarCollapsed.value))
}

// Marcus thêm: đóng menu drawer sau khi chuyển trang trên màn hình nhỏ.
watch(
  () => route.fullPath,
  () => {
    mobileSidebarOpen.value = false
  },
)
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}
.admin-sidebar {
  position: relative;
  z-index: 30;
}
.admin-sidebar.is-collapsed {
  width: 0;
  flex-basis: 0;
  padding-inline: 0;
  border-right: 0;
  overflow: hidden;
}
.desktop-sidebar-toggle {
  position: fixed;
  top: 92px;
  left: clamp(238px, calc(20vw - 22px), 270px);
  z-index: 1000;
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e2e8f0;
  border-radius: 0 10px 10px 0;
  background: #ffffff;
  color: #64748b;
  box-shadow: 5px 2px 14px rgba(15, 23, 42, 0.1);
  cursor: pointer;
  transition: left 0.25s ease, color 0.2s ease, background 0.2s ease;
}
.desktop-sidebar-toggle:hover {
  color: #dc2626;
  background: #fff7f7;
}
.desktop-sidebar-toggle.is-collapsed {
  left: 0;
}
.mobile-sidebar-toggle,
.sidebar-backdrop {
  display: none;
}
.main-content {
  flex: 1;
  min-width: 0;
  height: 100vh;
  background: #fff7fa;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.admin-view {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

/* Marcus thêm: hệ giao diện header dùng chung cho toàn bộ màn hình Admin cũ và mới. */
:deep(.admin-view .dashboard-heading),
:deep(.admin-view .voucher-hero),
:deep(.admin-view .flashsale-hero),
:deep(.admin-view .order-hero),
:deep(.admin-view .cm-hero),
:deep(.admin-view .skug-header),
:deep(.admin-view .amg-header),
:deep(.admin-view .page-heading),
:deep(.admin-view .page-header) {
  position: relative;
  min-height: 132px;
  margin-bottom: 24px;
  padding: 24px 28px !important;
  overflow: hidden;
  border: 1px solid #c7dcef !important;
  border-radius: 20px !important;
  background:
    radial-gradient(circle at 88% 10%, rgba(96, 165, 250, 0.16), transparent 24%),
    linear-gradient(110deg, #eaf4ff 0%, #f4f9ff 52%, #ffffff 100%) !important;
  box-shadow: 0 10px 28px rgba(30, 91, 156, 0.09) !important;
}

:deep(.admin-view .dashboard-heading)::after,
:deep(.admin-view .voucher-hero)::after,
:deep(.admin-view .flashsale-hero)::after,
:deep(.admin-view .order-hero)::after,
:deep(.admin-view .cm-hero)::after,
:deep(.admin-view .skug-header)::after,
:deep(.admin-view .amg-header)::after,
:deep(.admin-view .page-heading)::after,
:deep(.admin-view .page-header)::after {
  content: '';
  position: absolute;
  z-index: 0;
  top: -88px;
  right: 7%;
  width: 190px;
  height: 190px;
  border: 34px solid rgba(23, 105, 202, 0.045);
  border-radius: 50%;
  pointer-events: none;
}

:deep(.admin-view .dashboard-heading > *),
:deep(.admin-view .voucher-hero > *),
:deep(.admin-view .flashsale-hero > *),
:deep(.admin-view .order-hero > *),
:deep(.admin-view .cm-hero > *),
:deep(.admin-view .skug-header > *),
:deep(.admin-view .amg-header > *),
:deep(.admin-view .page-heading > *),
:deep(.admin-view .page-header > *) {
  position: relative;
  z-index: 1;
}

:deep(.admin-view .hero-icon),
:deep(.admin-view .cm-hero-icon),
:deep(.admin-view .skug-header-icon),
:deep(.admin-view .amg-header-icon),
:deep(.admin-view .header-icon) {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  flex: 0 0 58px;
  border: 1px solid #cfe1f8 !important;
  border-radius: 17px !important;
  color: #1769ca !important;
  background: #eaf3ff !important;
  box-shadow: inset 0 0 0 5px rgba(255, 255, 255, 0.58) !important;
}

:deep(.admin-view .dashboard-heading h1),
:deep(.admin-view .voucher-hero h1),
:deep(.admin-view .flashsale-hero h1),
:deep(.admin-view .order-hero h1),
:deep(.admin-view .cm-hero h1),
:deep(.admin-view .skug-header h1),
:deep(.admin-view .amg-header h1),
:deep(.admin-view .page-heading h1),
:deep(.admin-view .page-heading h2),
:deep(.admin-view .page-heading h3),
:deep(.admin-view .page-header h1),
:deep(.admin-view .page-header h2),
:deep(.admin-view .page-header h3),
:deep(.admin-view .page-header h4) {
  color: #10233f !important;
  font-weight: 800 !important;
}

:deep(.admin-view .dashboard-heading p),
:deep(.admin-view .dashboard-heading span),
:deep(.admin-view .voucher-hero p),
:deep(.admin-view .flashsale-hero p),
:deep(.admin-view .order-hero p),
:deep(.admin-view .cm-hero p),
:deep(.admin-view .skug-header p),
:deep(.admin-view .amg-header p),
:deep(.admin-view .page-heading p),
:deep(.admin-view .page-header p) {
  color: #64748b;
}

/* Marcus sửa: nút hành động nằm trong header dùng duy nhất hệ màu xanh quản trị. */
:deep(.admin-view .voucher-hero > button),
:deep(.admin-view .flashsale-hero > button),
:deep(.admin-view .page-header > button),
:deep(.admin-view .page-header .btn-add),
:deep(.admin-view .page-header .btn-refresh),
:deep(.admin-view .page-heading .page-actions button),
:deep(.admin-view .page-heading .page-actions a) {
  border: 1px solid #1769ca !important;
  color: #ffffff !important;
  background: linear-gradient(135deg, #2478d4, #1769ca) !important;
  box-shadow: 0 7px 16px rgba(23, 105, 202, 0.2) !important;
}

:deep(.admin-view .voucher-hero > button:hover),
:deep(.admin-view .flashsale-hero > button:hover),
:deep(.admin-view .page-header > button:hover),
:deep(.admin-view .page-header .btn-add:hover),
:deep(.admin-view .page-header .btn-refresh:hover),
:deep(.admin-view .page-heading .page-actions button:hover),
:deep(.admin-view .page-heading .page-actions a:hover) {
  border-color: #1259ad !important;
  background: linear-gradient(135deg, #1d6ec7, #1259ad) !important;
  transform: translateY(-1px);
}

@media (max-width: 900px) {
  .desktop-sidebar-toggle {
    display: none;
  }

  .admin-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 1002;
    transform: translateX(-105%);
    transition: transform 0.25s ease;
    box-shadow: 16px 0 36px rgba(15, 23, 42, 0.18);
  }

  .admin-sidebar.is-collapsed {
    width: min(86vw, 292px);
    flex-basis: min(86vw, 292px);
    padding: 18px 14px 24px;
  }

  .admin-sidebar.is-open {
    transform: translateX(0);
  }

  .mobile-sidebar-toggle {
    position: fixed;
    top: 14px;
    left: 14px;
    z-index: 1004;
    width: 42px;
    height: 42px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #fecdd3;
    border-radius: 12px;
    background: #ffffff;
    color: #dc2626;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.14);
    cursor: pointer;
    transition: left 0.25s ease, background 0.2s ease, color 0.2s ease;
  }

  .mobile-sidebar-toggle.is-open {
    left: min(calc(86vw - 54px), 238px);
    background: #dc2626;
    color: #ffffff;
  }

  .sidebar-backdrop {
    position: fixed;
    inset: 0;
    z-index: 1001;
    display: block;
    background: rgba(15, 23, 42, 0.42);
    backdrop-filter: blur(2px);
  }

  .main-content {
    width: 100%;
  }
}

@media (max-width: 720px) {
  :deep(.admin-view .dashboard-heading),
  :deep(.admin-view .voucher-hero),
  :deep(.admin-view .flashsale-hero),
  :deep(.admin-view .order-hero),
  :deep(.admin-view .cm-hero),
  :deep(.admin-view .skug-header),
  :deep(.admin-view .amg-header),
  :deep(.admin-view .page-heading),
  :deep(.admin-view .page-header) {
    min-height: auto;
    padding: 20px !important;
    align-items: flex-start !important;
    flex-direction: column !important;
    gap: 16px !important;
  }
}
</style>
