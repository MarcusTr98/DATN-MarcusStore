<template>
  <div class="admin-layout" :class="`admin-accent-${adminAccent}`">
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AdminHeader from '@/components/admin/AdminHeader.vue'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import AdminChatInbox from '@/components/chat/AdminChatInbox.vue'
import { useAdminTheme } from '@/composables/useAdminTheme'

const route = useRoute()
useAdminTheme()
const sidebarCollapsed = ref(localStorage.getItem('ADMIN_SIDEBAR_COLLAPSED') === 'true')
const mobileSidebarOpen = ref(false)

// Marcus thêm: màu nhấn theo nhóm nghiệp vụ, tự động dựa trên route hiện tại.
const blueAccentRoutes = [
  '/admin/analytics',
  '/admin/finance-reports',
  '/admin/settings',
  '/admin/data-backup',
  '/admin/activity-log',
]

const adminAccent = computed(() =>
  blueAccentRoutes.some((path) => route.path.startsWith(path)) ? 'blue' : 'pink',
)

// Marcus thêm: đánh dấu phạm vi Admin để modal Teleport cũng nhận đúng Dark Mode.
onMounted(() => document.body.classList.add('admin-theme-scope'))
onUnmounted(() => document.body.classList.remove('admin-theme-scope'))

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

<style>
/* Marcus thêm: theme token chỉ áp dụng trong Admin, không tác động Client. */
.admin-layout {
  --admin-bg: #f4f8fd;
  --admin-surface: #ffffff;
  --admin-surface-soft: #f8fbff;
  --admin-border: #dce7f3;
  --admin-text: #10233f;
  --admin-text-soft: #64748b;
  --admin-primary: #1769ca;
  --admin-shadow: 0 10px 28px rgba(30, 91, 156, 0.08);
  color: var(--admin-text);
  background: var(--admin-bg);
  transition: color 0.25s ease, background 0.25s ease;
}

/* Marcus sửa: Light Theme thống nhất các màn do nhiều module khác nhau phát triển. */
html[data-admin-theme='light'] .admin-layout .main-content,
html[data-admin-theme='light'] .admin-layout .admin-view {
  color: var(--admin-text) !important;
  background: linear-gradient(180deg, #f5f9fe 0%, #eef5fc 100%) !important;
}

html[data-admin-theme='light'] .admin-layout .sidebar,
html[data-admin-theme='light'] .admin-layout .header {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: #ffffff !important;
  box-shadow: var(--admin-shadow) !important;
}

html[data-admin-theme='light'] .admin-layout .logo {
  border-color: #d9e7f5 !important;
  background: linear-gradient(135deg, #ffffff 0%, #edf6ff 100%) !important;
}

html[data-admin-theme='light'] .admin-layout .menu-title {
  color: #52677f !important;
}

html[data-admin-theme='light'] .admin-layout .menu-item,
html[data-admin-theme='light'] .admin-layout .submenu-item {
  color: #263b53 !important;
}

html[data-admin-theme='light'] .admin-layout .menu-item:hover,
html[data-admin-theme='light'] .admin-layout .submenu-item:hover {
  color: #1769ca !important;
  background: #edf6ff !important;
}

html[data-admin-theme='light'] .admin-layout .menu-item.active,
html[data-admin-theme='light'] .admin-layout .submenu-item.active {
  color: #ffffff !important;
  background: linear-gradient(135deg, #2478d4, #1769ca) !important;
  box-shadow: 0 7px 17px rgba(23, 105, 202, 0.2) !important;
}

html[data-admin-theme='light'] .admin-layout .voucher-page,
html[data-admin-theme='light'] .admin-layout .flashsale-page,
html[data-admin-theme='light'] .admin-layout .order-page,
html[data-admin-theme='light'] .admin-layout .cm-page,
html[data-admin-theme='light'] .admin-layout .amg-page,
html[data-admin-theme='light'] .admin-layout .skug-page,
html[data-admin-theme='light'] .admin-layout .bm-page,
html[data-admin-theme='light'] .admin-layout .review-page,
html[data-admin-theme='light'] .admin-layout .fin-page,
html[data-admin-theme='light'] .admin-layout .system-settings-page,
html[data-admin-theme='light'] .admin-layout .dashboard-page {
  background: transparent !important;
}

html[data-admin-theme='light'] .admin-layout .card,
html[data-admin-theme='light'] .admin-layout .stat-card,
html[data-admin-theme='light'] .admin-layout .page-card,
html[data-admin-theme='light'] .admin-layout .toolbar-panel,
html[data-admin-theme='light'] .admin-layout .filter-card,
html[data-admin-theme='light'] .admin-layout .section-card,
html[data-admin-theme='light'] .admin-layout .summary-card,
html[data-admin-theme='light'] .admin-layout .amg-panel,
html[data-admin-theme='light'] .admin-layout .skug-card,
html[data-admin-theme='light'] .admin-layout .chart-card,
html[data-admin-theme='light'] .admin-layout .data-card,
html[data-admin-theme='light'] .admin-layout .settings-card,
html[data-admin-theme='light'] .admin-layout .branding-settings,
html[data-admin-theme='light'] .admin-layout .table-wrapper {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface) !important;
  box-shadow: var(--admin-shadow) !important;
}

html[data-admin-theme='light'] .admin-layout input,
html[data-admin-theme='light'] .admin-layout select,
html[data-admin-theme='light'] .admin-layout textarea,
html[data-admin-theme='light'] .admin-layout .form-control,
html[data-admin-theme='light'] .admin-layout .form-select,
html[data-admin-theme='light'] .admin-layout .input-group-text {
  border-color: #cfdceb !important;
  color: #263b53 !important;
  background-color: #ffffff !important;
}

html[data-admin-theme='light'] .admin-layout input:focus,
html[data-admin-theme='light'] .admin-layout select:focus,
html[data-admin-theme='light'] .admin-layout textarea:focus,
html[data-admin-theme='light'] .admin-layout .form-control:focus,
html[data-admin-theme='light'] .admin-layout .form-select:focus {
  border-color: #60a5fa !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12) !important;
}

html[data-admin-theme='light'] .admin-layout table,
html[data-admin-theme='light'] .admin-layout thead,
html[data-admin-theme='light'] .admin-layout tbody,
html[data-admin-theme='light'] .admin-layout tr,
html[data-admin-theme='light'] .admin-layout th,
html[data-admin-theme='light'] .admin-layout td {
  border-color: #e2eaf3 !important;
  color: #263b53 !important;
  background-color: #ffffff !important;
}

html[data-admin-theme='light'] .admin-layout thead th {
  color: #174f8e !important;
  background: #eaf4ff !important;
}

html[data-admin-theme='light'] .admin-layout tbody tr:hover td {
  background: #f3f8fe !important;
}

html[data-admin-theme='light'] .admin-layout .btn-primary-action,
html[data-admin-theme='light'] .admin-layout .btn-add,
html[data-admin-theme='light'] .admin-layout .btn-primary,
html[data-admin-theme='light'] .admin-layout .btn-save,
html[data-admin-theme='light'] .admin-layout .btn-submit,
html[data-admin-theme='light'] .admin-layout .submit-btn {
  border-color: #1769ca !important;
  color: #ffffff !important;
  background: linear-gradient(135deg, #2478d4, #1769ca) !important;
  box-shadow: 0 7px 16px rgba(23, 105, 202, 0.18) !important;
}

html[data-admin-theme='light'] .admin-layout .pagination .active,
html[data-admin-theme='light'] .admin-layout .page-link.active,
html[data-admin-theme='light'] .admin-layout .nav-pills .active {
  border-color: #1769ca !important;
  color: #ffffff !important;
  background: #1769ca !important;
}

html[data-admin-theme='light'] .admin-layout .header-btn,
html[data-admin-theme='light'] .admin-layout .desktop-sidebar-toggle,
html[data-admin-theme='light'] .admin-layout .mobile-sidebar-toggle {
  border-color: #d6e2ef !important;
  color: #334b65 !important;
  background: #ffffff !important;
}

html[data-admin-theme='light'] .admin-layout .header-btn:hover,
html[data-admin-theme='light'] .admin-layout .desktop-sidebar-toggle:hover,
html[data-admin-theme='light'] .admin-layout .mobile-sidebar-toggle:hover {
  border-color: #93bce8 !important;
  color: #1769ca !important;
  background: #edf6ff !important;
}

html[data-admin-theme='light'] .admin-layout .notif-dropdown,
html[data-admin-theme='light'] .admin-layout .notif-header,
html[data-admin-theme='light'] .admin-layout .notif-tabs,
html[data-admin-theme='light'] .admin-layout .notif-item,
html[data-admin-theme='light'] .admin-layout .btn-load-more {
  border-color: var(--admin-border) !important;
  background: #ffffff !important;
}

html[data-admin-theme='light'] .admin-layout .notif-item.unread {
  background: #f0f7ff !important;
}

/* Marcus thêm: Light Mode đổi accent theo nhóm nghiệp vụ của route hiện tại. */
html[data-admin-theme='light'] .admin-layout.admin-accent-pink {
  --admin-bg: #fff8fb;
  --admin-surface-soft: #fff3f8;
  --admin-border: #f2d3e0;
  --admin-primary: #d93678;
  --admin-shadow: 0 10px 28px rgba(190, 49, 108, 0.08);
}

html[data-admin-theme='light'] .admin-layout.admin-accent-pink .main-content,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .admin-view {
  background: linear-gradient(180deg, #fffafd, #fff5f9) !important;
}

html[data-admin-theme='light'] .admin-layout.admin-accent-pink .admin-page-header,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .dashboard-heading,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .voucher-hero,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .flashsale-hero,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .order-hero,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .cm-hero,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .skug-header,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .amg-header,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .page-heading,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .page-header {
  border-color: #f2c7d9 !important;
  background:
    radial-gradient(circle at 88% 10%, rgba(244, 114, 182, 0.13), transparent 25%),
    linear-gradient(112deg, #fff0f6 0%, #fff7fb 54%, #ffffff 100%) !important;
  box-shadow: 0 10px 28px rgba(190, 49, 108, 0.09) !important;
}

html[data-admin-theme='light'] .admin-layout.admin-accent-pink .admin-page-header::after,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .dashboard-heading::after,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .voucher-hero::after,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .flashsale-hero::after,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .order-hero::after,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .cm-hero::after,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .skug-header::after,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .amg-header::after,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .page-heading::after,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .page-header::after {
  border-color: rgba(219, 63, 125, 0.055) !important;
}

html[data-admin-theme='light'] .admin-layout.admin-accent-pink .admin-page-header__icon,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .hero-icon,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .cm-hero-icon,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .skug-header-icon,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .amg-header-icon,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .header-icon {
  border-color: #f2c1d5 !important;
  color: #d93678 !important;
  background: #ffe8f2 !important;
  box-shadow: inset 0 0 0 5px rgba(255, 255, 255, 0.62) !important;
}

html[data-admin-theme='light'] .admin-layout.admin-accent-pink .admin-page-header__eyebrow,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .eyebrow,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .skug-breadcrumb,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .amg-breadcrumb,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .greeting-title span {
  color: #d93678 !important;
}

html[data-admin-theme='light'] .admin-layout.admin-accent-pink .admin-page-header__actions button,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .voucher-hero > button,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .flashsale-hero > button,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .page-header > button,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .btn-primary-action,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .btn-add,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .btn-primary,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .btn-save,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .btn-submit,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .submit-btn {
  border-color: #d93678 !important;
  color: #ffffff !important;
  background: linear-gradient(135deg, #e7548e, #d93678) !important;
  box-shadow: 0 7px 16px rgba(217, 54, 120, 0.2) !important;
}

html[data-admin-theme='light'] .admin-layout.admin-accent-pink input:focus,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink select:focus,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink textarea:focus,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .form-control:focus,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .form-select:focus {
  border-color: #e7548e !important;
  box-shadow: 0 0 0 3px rgba(231, 84, 142, 0.12) !important;
}

html[data-admin-theme='light'] .admin-layout.admin-accent-pink thead th {
  color: #9f285a !important;
  background: #fff0f6 !important;
}

html[data-admin-theme='light'] .admin-layout.admin-accent-pink tbody tr:hover td {
  background: #fff6fa !important;
}

html[data-admin-theme='light'] .admin-layout.admin-accent-pink .menu-item.active,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .submenu-item.active,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .pagination .active,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .page-link.active,
html[data-admin-theme='light'] .admin-layout.admin-accent-pink .nav-pills .active {
  border-color: #d93678 !important;
  color: #ffffff !important;
  background: linear-gradient(135deg, #e7548e, #d93678) !important;
  box-shadow: 0 7px 18px rgba(217, 54, 120, 0.2) !important;
}

html[data-admin-theme='light'] .admin-layout.admin-accent-blue {
  --admin-bg: #f4f8fd;
  --admin-surface-soft: #f3f8ff;
  --admin-border: #d6e5f5;
  --admin-primary: #1769ca;
}

html[data-admin-theme='dark'] .admin-layout {
  --admin-bg: #0b1220;
  --admin-surface: #111c2f;
  --admin-surface-soft: #16243a;
  --admin-border: #263a55;
  --admin-text: #e7eef8;
  --admin-text-soft: #9fb0c7;
  --admin-primary: #60a5fa;
  --admin-shadow: 0 14px 34px rgba(0, 0, 0, 0.24);
  color-scheme: dark;
}

html[data-admin-theme='dark'] .admin-layout .main-content,
html[data-admin-theme='dark'] .admin-layout .admin-view {
  background: var(--admin-bg) !important;
  color: var(--admin-text) !important;
}

html[data-admin-theme='dark'] .admin-layout .sidebar,
html[data-admin-theme='dark'] .admin-layout .header,
html[data-admin-theme='dark'] .admin-layout .card,
html[data-admin-theme='dark'] .admin-layout .stat-card,
html[data-admin-theme='dark'] .admin-layout .page-card,
html[data-admin-theme='dark'] .admin-layout .toolbar-panel,
html[data-admin-theme='dark'] .admin-layout .filter-card,
html[data-admin-theme='dark'] .admin-layout .section-card,
html[data-admin-theme='dark'] .admin-layout .summary-card,
html[data-admin-theme='dark'] .admin-layout .amg-panel,
html[data-admin-theme='dark'] .admin-layout .skug-card,
html[data-admin-theme='dark'] .admin-layout .chart-card,
html[data-admin-theme='dark'] .admin-layout .data-card,
html[data-admin-theme='dark'] .admin-layout .settings-card {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface) !important;
  box-shadow: var(--admin-shadow) !important;
}

html[data-admin-theme='dark'] .admin-layout .logo {
  border-color: var(--admin-border) !important;
  background: linear-gradient(135deg, #15243a, #111c2f) !important;
}

html[data-admin-theme='dark'] .admin-layout .logo-text h2,
html[data-admin-theme='dark'] .admin-layout .logo-text span,
html[data-admin-theme='dark'] .admin-layout .menu-title,
html[data-admin-theme='dark'] .admin-layout .menu-item,
html[data-admin-theme='dark'] .admin-layout .submenu-item,
html[data-admin-theme='dark'] .admin-layout h1,
html[data-admin-theme='dark'] .admin-layout h2,
html[data-admin-theme='dark'] .admin-layout h3,
html[data-admin-theme='dark'] .admin-layout h4,
html[data-admin-theme='dark'] .admin-layout h5,
html[data-admin-theme='dark'] .admin-layout strong,
html[data-admin-theme='dark'] .admin-layout label,
html[data-admin-theme='dark'] .admin-layout .greeting-title,
html[data-admin-theme='dark'] .admin-layout .notif-title,
html[data-admin-theme='dark'] .admin-layout .notif-desc {
  color: var(--admin-text) !important;
}

html[data-admin-theme='dark'] .admin-layout p,
html[data-admin-theme='dark'] .admin-layout small,
html[data-admin-theme='dark'] .admin-layout .text-muted,
html[data-admin-theme='dark'] .admin-layout .greeting-sub,
html[data-admin-theme='dark'] .admin-layout .summary-label,
html[data-admin-theme='dark'] .admin-layout .form-text {
  color: var(--admin-text-soft) !important;
}

html[data-admin-theme='dark'] .admin-layout .menu-item:hover,
html[data-admin-theme='dark'] .admin-layout .submenu-item:hover {
  color: #93c5fd !important;
  background: #1d304b !important;
}

html[data-admin-theme='dark'] .admin-layout .menu-item.active,
html[data-admin-theme='dark'] .admin-layout .submenu-item.active {
  color: #ffffff !important;
  background: linear-gradient(135deg, #2563eb, #1d4ed8) !important;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.24) !important;
}

html[data-admin-theme='dark'] .admin-layout .admin-page-header,
html[data-admin-theme='dark'] .admin-layout .dashboard-heading,
html[data-admin-theme='dark'] .admin-layout .voucher-hero,
html[data-admin-theme='dark'] .admin-layout .flashsale-hero,
html[data-admin-theme='dark'] .admin-layout .order-hero,
html[data-admin-theme='dark'] .admin-layout .cm-hero,
html[data-admin-theme='dark'] .admin-layout .skug-header,
html[data-admin-theme='dark'] .admin-layout .amg-header,
html[data-admin-theme='dark'] .admin-layout .page-heading,
html[data-admin-theme='dark'] .admin-layout .page-header {
  border-color: #2d4666 !important;
  background:
    radial-gradient(circle at 88% 10%, rgba(96, 165, 250, 0.13), transparent 26%),
    linear-gradient(115deg, #152741, #111c2f 62%, #17263c) !important;
  box-shadow: var(--admin-shadow) !important;
}

html[data-admin-theme='dark'] .admin-layout .admin-page-header__icon,
html[data-admin-theme='dark'] .admin-layout .hero-icon,
html[data-admin-theme='dark'] .admin-layout .cm-hero-icon,
html[data-admin-theme='dark'] .admin-layout .skug-header-icon,
html[data-admin-theme='dark'] .admin-layout .amg-header-icon,
html[data-admin-theme='dark'] .admin-layout .header-icon {
  border-color: #36577d !important;
  color: #93c5fd !important;
  background: #1d3554 !important;
  box-shadow: inset 0 0 0 5px rgba(15, 30, 50, 0.48) !important;
}

html[data-admin-theme='dark'] .admin-layout input,
html[data-admin-theme='dark'] .admin-layout select,
html[data-admin-theme='dark'] .admin-layout textarea,
html[data-admin-theme='dark'] .admin-layout .form-control,
html[data-admin-theme='dark'] .admin-layout .form-select,
html[data-admin-theme='dark'] .admin-layout .input-group-text {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background-color: #0e192a !important;
}

html[data-admin-theme='dark'] .admin-layout input::placeholder,
html[data-admin-theme='dark'] .admin-layout textarea::placeholder {
  color: #71839b !important;
}

html[data-admin-theme='dark'] .admin-layout table,
html[data-admin-theme='dark'] .admin-layout thead,
html[data-admin-theme='dark'] .admin-layout tbody,
html[data-admin-theme='dark'] .admin-layout tr,
html[data-admin-theme='dark'] .admin-layout th,
html[data-admin-theme='dark'] .admin-layout td {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background-color: var(--admin-surface) !important;
}

html[data-admin-theme='dark'] .admin-layout thead th {
  background-color: #172943 !important;
}

html[data-admin-theme='dark'] .admin-layout tbody tr:hover td {
  background-color: #182a43 !important;
}

html[data-admin-theme='dark'] .admin-layout .header-btn,
html[data-admin-theme='dark'] .admin-layout .desktop-sidebar-toggle,
html[data-admin-theme='dark'] .admin-layout .mobile-sidebar-toggle {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface-soft) !important;
}

html[data-admin-theme='dark'] .admin-layout .notif-dropdown,
html[data-admin-theme='dark'] .admin-layout .notif-header,
html[data-admin-theme='dark'] .admin-layout .notif-tabs,
html[data-admin-theme='dark'] .admin-layout .notif-item,
html[data-admin-theme='dark'] .admin-layout .btn-load-more {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface) !important;
}

html[data-admin-theme='dark'] .admin-layout .notif-item:hover,
html[data-admin-theme='dark'] .admin-layout .notif-item.unread {
  background: var(--admin-surface-soft) !important;
}

html[data-admin-theme='dark'] .admin-layout .modal-content,
html[data-admin-theme='dark'] .admin-layout .modal-card,
html[data-admin-theme='dark'] .admin-layout .dialog-card {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface) !important;
}

/* Marcus rà soát: loại các bề mặt Light còn sót trong từng module Admin. */
html[data-admin-theme='dark'] .admin-layout .dashboard-page,
html[data-admin-theme='dark'] .admin-layout .voucher-page,
html[data-admin-theme='dark'] .admin-layout .flashsale-page,
html[data-admin-theme='dark'] .admin-layout .order-page,
html[data-admin-theme='dark'] .admin-layout .cm-page,
html[data-admin-theme='dark'] .admin-layout .amg-page,
html[data-admin-theme='dark'] .admin-layout .skug-page,
html[data-admin-theme='dark'] .admin-layout .bm-page,
html[data-admin-theme='dark'] .admin-layout .review-page,
html[data-admin-theme='dark'] .admin-layout .fin-page,
html[data-admin-theme='dark'] .admin-layout .system-settings-page,
html[data-admin-theme='dark'] .admin-layout .data-backup-page,
html[data-admin-theme='dark'] .admin-layout .activity-log-page,
html[data-admin-theme='dark'] .admin-layout .customer-page,
html[data-admin-theme='dark'] .admin-layout .employee-page,
html[data-admin-theme='dark'] .admin-layout .role-page {
  color: var(--admin-text) !important;
  background: transparent !important;
}

html[data-admin-theme='dark'] .admin-layout .dashboard-card,
html[data-admin-theme='dark'] .admin-layout .filter-panel,
html[data-admin-theme='dark'] .admin-layout .chart-panel,
html[data-admin-theme='dark'] .admin-layout .table-panel,
html[data-admin-theme='dark'] .admin-layout .stats-panel,
html[data-admin-theme='dark'] .admin-layout .info-panel,
html[data-admin-theme='dark'] .admin-layout .table-wrap,
html[data-admin-theme='dark'] .admin-layout .table-wrapper,
html[data-admin-theme='dark'] .admin-layout .branding-settings,
html[data-admin-theme='dark'] .admin-layout .branding-preview,
html[data-admin-theme='dark'] .admin-layout .preview-card,
html[data-admin-theme='dark'] .admin-layout .toggle-row,
html[data-admin-theme='dark'] .admin-layout .author-row,
html[data-admin-theme='dark'] .admin-layout .person-section,
html[data-admin-theme='dark'] .admin-layout .fin-pagination,
html[data-admin-theme='dark'] .admin-layout .pager,
html[data-admin-theme='dark'] .admin-layout .pagination-bar,
html[data-admin-theme='dark'] .admin-layout .filter-bar,
html[data-admin-theme='dark'] .admin-layout .filter-wrapper,
html[data-admin-theme='dark'] .admin-layout .search-wrapper,
html[data-admin-theme='dark'] .admin-layout .upload-dropzone,
html[data-admin-theme='dark'] .admin-layout .product-dropdown,
html[data-admin-theme='dark'] .admin-layout .state-box {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface) !important;
  box-shadow: none !important;
}

html[data-admin-theme='dark'] .admin-layout .card-header,
html[data-admin-theme='dark'] .admin-layout .panel-header,
html[data-admin-theme='dark'] .admin-layout .amg-panel-header,
html[data-admin-theme='dark'] .admin-layout .skug-card-header,
html[data-admin-theme='dark'] .admin-layout .form-card-header,
html[data-admin-theme='dark'] .admin-layout .bn-modal-header,
html[data-admin-theme='dark'] .admin-layout .bn-modal-footer,
html[data-admin-theme='dark'] .admin-layout .modal-header,
html[data-admin-theme='dark'] .admin-layout .modal-footer {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface-soft) !important;
}

html[data-admin-theme='dark'] .admin-layout .pg-btn,
html[data-admin-theme='dark'] .admin-layout .page-btn,
html[data-admin-theme='dark'] .admin-layout .pager button,
html[data-admin-theme='dark'] .admin-layout .btn-quick-date,
html[data-admin-theme='dark'] .admin-layout .btn-cancel,
html[data-admin-theme='dark'] .admin-layout .btn-cancel-modal,
html[data-admin-theme='dark'] .admin-layout .btn-secondary,
html[data-admin-theme='dark'] .admin-layout .outline-btn,
html[data-admin-theme='dark'] .admin-layout .brand-remove-btn {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface-soft) !important;
}

html[data-admin-theme='dark'] .admin-layout .pg-btn:hover:not(:disabled),
html[data-admin-theme='dark'] .admin-layout .page-btn:hover:not(:disabled),
html[data-admin-theme='dark'] .admin-layout .btn-quick-date:hover,
html[data-admin-theme='dark'] .admin-layout .btn-cancel:hover,
html[data-admin-theme='dark'] .admin-layout .outline-btn:hover {
  border-color: #4d6f98 !important;
  color: #bfdbfe !important;
  background: #1b304b !important;
}

html[data-admin-theme='dark'] .admin-layout .dropdown-menu,
html[data-admin-theme='dark'] .admin-layout .dropdown-item,
html[data-admin-theme='dark'] .admin-layout .product-dropdown-list,
html[data-admin-theme='dark'] .admin-layout .autocomplete-list,
html[data-admin-theme='dark'] .admin-layout .select-menu {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface) !important;
}

html[data-admin-theme='dark'] .admin-layout .dropdown-item:hover,
html[data-admin-theme='dark'] .admin-layout .product-dropdown-item:hover,
html[data-admin-theme='dark'] .admin-layout .autocomplete-item:hover {
  color: var(--admin-text) !important;
  background: var(--admin-surface-soft) !important;
}

html[data-admin-theme='dark'] .admin-layout .page-size-select,
html[data-admin-theme='dark'] .admin-layout .date-picker,
html[data-admin-theme='dark'] .admin-layout .search-box,
html[data-admin-theme='dark'] .admin-layout .filter-input,
html[data-admin-theme='dark'] .admin-layout .form-input,
html[data-admin-theme='dark'] .admin-layout .content-area {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: #0e192a !important;
}

html[data-admin-theme='dark'] .admin-layout .apexcharts-canvas text,
html[data-admin-theme='dark'] .admin-layout .apexcharts-legend-text,
html[data-admin-theme='dark'] .admin-layout .apexcharts-title-text,
html[data-admin-theme='dark'] .admin-layout .apexcharts-xaxis-label,
html[data-admin-theme='dark'] .admin-layout .apexcharts-yaxis-label {
  fill: var(--admin-text-soft) !important;
  color: var(--admin-text-soft) !important;
}

html[data-admin-theme='dark'] .admin-layout .apexcharts-gridline,
html[data-admin-theme='dark'] .admin-layout .apexcharts-xaxis line,
html[data-admin-theme='dark'] .admin-layout .apexcharts-yaxis line {
  stroke: #2b405c !important;
}

html[data-admin-theme='dark'] .admin-layout .user-name,
html[data-admin-theme='dark'] .admin-layout .cell-main,
html[data-admin-theme='dark'] .admin-layout .money,
html[data-admin-theme='dark'] .admin-layout .preview-title,
html[data-admin-theme='dark'] .admin-layout .page-indicator strong,
html[data-admin-theme='dark'] .admin-layout .pagination-summary strong,
html[data-admin-theme='dark'] .admin-layout .legend-text b,
html[data-admin-theme='dark'] .admin-layout .chart-sub,
html[data-admin-theme='dark'] .admin-layout .table-title,
html[data-admin-theme='dark'] .admin-layout .card-title {
  color: var(--admin-text) !important;
}

html[data-admin-theme='dark'] .admin-layout .toast-alert {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface) !important;
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.3) !important;
}

/* Modal của Admin dùng Teleport ra body nên cần scope riêng ngoài admin-layout. */
html[data-admin-theme='dark'] body.admin-theme-scope .modal-content,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-card,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-box,
html[data-admin-theme='dark'] body.admin-theme-scope .banner-modal-box,
html[data-admin-theme='dark'] body.admin-theme-scope .bn-modal-box,
html[data-admin-theme='dark'] body.admin-theme-scope .rdm-modal,
html[data-admin-theme='dark'] body.admin-theme-scope .voucher-modal,
html[data-admin-theme='dark'] body.admin-theme-scope .fs-detail-modal {
  border-color: #263a55 !important;
  color: #e7eef8 !important;
  background: #111c2f !important;
}

html[data-admin-theme='dark'] body.admin-theme-scope .modal-header,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-footer,
html[data-admin-theme='dark'] body.admin-theme-scope .bn-modal-header,
html[data-admin-theme='dark'] body.admin-theme-scope .bn-modal-footer,
html[data-admin-theme='dark'] body.admin-theme-scope .rdm-header {
  border-color: #263a55 !important;
  color: #e7eef8 !important;
  background: #16243a !important;
}

html[data-admin-theme='dark'] body.admin-theme-scope .modal-content input,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-content select,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-content textarea,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-box input,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-box select,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-box textarea,
html[data-admin-theme='dark'] body.admin-theme-scope .banner-modal-box input,
html[data-admin-theme='dark'] body.admin-theme-scope .banner-modal-box select,
html[data-admin-theme='dark'] body.admin-theme-scope .banner-modal-box textarea {
  border-color: #263a55 !important;
  color: #e7eef8 !important;
  background: #0e192a !important;
}

html[data-admin-theme='dark'] body.admin-theme-scope .modal-content h1,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-content h2,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-content h3,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-content h4,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-content label,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-box h1,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-box h2,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-box h3,
html[data-admin-theme='dark'] body.admin-theme-scope .modal-box label {
  color: #e7eef8 !important;
}

@media (prefers-reduced-motion: reduce) {
  .admin-layout,
  .admin-layout * {
    transition-duration: 0.01ms !important;
  }
}
</style>
