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
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AdminHeader from '@/components/admin/AdminHeader.vue'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import AdminChatInbox from '@/components/chat/AdminChatInbox.vue'

const route = useRoute()
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
.admin-layout .main-content,
.admin-layout .admin-view {
  color: var(--admin-text) !important;
  background: linear-gradient(180deg, #f5f9fe 0%, #eef5fc 100%) !important;
}

.admin-layout .sidebar,
.admin-layout .header {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: #ffffff !important;
  box-shadow: var(--admin-shadow) !important;
}

.admin-layout .logo {
  border-color: #d9e7f5 !important;
  background: linear-gradient(135deg, #ffffff 0%, #edf6ff 100%) !important;
}

.admin-layout .menu-title {
  color: #52677f !important;
}

.admin-layout .menu-item,
.admin-layout .submenu-item {
  color: #263b53 !important;
}

.admin-layout .menu-item:hover,
.admin-layout .submenu-item:hover {
  color: #1769ca !important;
  background: #edf6ff !important;
}

.admin-layout .menu-item.active,
.admin-layout .submenu-item.active {
  color: #ffffff !important;
  background: linear-gradient(135deg, #2478d4, #1769ca) !important;
  box-shadow: 0 7px 17px rgba(23, 105, 202, 0.2) !important;
}

.admin-layout .voucher-page,
.admin-layout .flashsale-page,
.admin-layout .order-page,
.admin-layout .cm-page,
.admin-layout .amg-page,
.admin-layout .skug-page,
.admin-layout .bm-page,
.admin-layout .review-page,
.admin-layout .fin-page,
.admin-layout .system-settings-page,
.admin-layout .dashboard-page {
  background: transparent !important;
}

.admin-layout .card,
.admin-layout .stat-card,
.admin-layout .page-card,
.admin-layout .toolbar-panel,
.admin-layout .filter-card,
.admin-layout .section-card,
.admin-layout .summary-card,
.admin-layout .amg-panel,
.admin-layout .skug-card,
.admin-layout .chart-card,
.admin-layout .data-card,
.admin-layout .settings-card,
.admin-layout .branding-settings,
.admin-layout .table-wrapper {
  border-color: var(--admin-border) !important;
  color: var(--admin-text) !important;
  background: var(--admin-surface) !important;
  box-shadow: var(--admin-shadow) !important;
}

.admin-layout input,
.admin-layout select,
.admin-layout textarea,
.admin-layout .form-control,
.admin-layout .form-select,
.admin-layout .input-group-text {
  border-color: #cfdceb !important;
  color: #263b53 !important;
  background-color: #ffffff !important;
}

.admin-layout input:focus,
.admin-layout select:focus,
.admin-layout textarea:focus,
.admin-layout .form-control:focus,
.admin-layout .form-select:focus {
  border-color: #60a5fa !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12) !important;
}

.admin-layout table,
.admin-layout thead,
.admin-layout tbody,
.admin-layout tr,
.admin-layout th,
.admin-layout td {
  border-color: #e2eaf3 !important;
  color: #263b53 !important;
  background-color: #ffffff !important;
}

.admin-layout thead th {
  color: #174f8e !important;
  background: #eaf4ff !important;
}

.admin-layout tbody tr:hover td {
  background: #f3f8fe !important;
}

.admin-layout .btn-primary-action,
.admin-layout .btn-add,
.admin-layout .btn-primary,
.admin-layout .btn-save,
.admin-layout .btn-submit,
.admin-layout .submit-btn {
  border-color: #1769ca !important;
  color: #ffffff !important;
  background: linear-gradient(135deg, #2478d4, #1769ca) !important;
  box-shadow: 0 7px 16px rgba(23, 105, 202, 0.18) !important;
}

.admin-layout .pagination .active,
.admin-layout .page-link.active,
.admin-layout .nav-pills .active {
  border-color: #1769ca !important;
  color: #ffffff !important;
  background: #1769ca !important;
}

.admin-layout .header-btn,
.admin-layout .desktop-sidebar-toggle,
.admin-layout .mobile-sidebar-toggle {
  border-color: #d6e2ef !important;
  color: #334b65 !important;
  background: #ffffff !important;
}

.admin-layout .header-btn:hover,
.admin-layout .desktop-sidebar-toggle:hover,
.admin-layout .mobile-sidebar-toggle:hover {
  border-color: #93bce8 !important;
  color: #1769ca !important;
  background: #edf6ff !important;
}

.admin-layout .notif-dropdown,
.admin-layout .notif-header,
.admin-layout .notif-tabs,
.admin-layout .notif-item,
.admin-layout .btn-load-more {
  border-color: var(--admin-border) !important;
  background: #ffffff !important;
}

.admin-layout .notif-item.unread {
  background: #f0f7ff !important;
}

/* Marcus thêm: Light Mode đổi accent theo nhóm nghiệp vụ của route hiện tại. */
.admin-layout.admin-accent-pink {
  --admin-bg: #fff8fb;
  --admin-surface-soft: #fff3f8;
  --admin-border: #f2d3e0;
  --admin-primary: #d93678;
  --admin-shadow: 0 10px 28px rgba(190, 49, 108, 0.08);
}

.admin-layout.admin-accent-pink .main-content,
.admin-layout.admin-accent-pink .admin-view {
  background: linear-gradient(180deg, #fffafd, #fff5f9) !important;
}

.admin-layout.admin-accent-pink .admin-page-header,
.admin-layout.admin-accent-pink .dashboard-heading,
.admin-layout.admin-accent-pink .voucher-hero,
.admin-layout.admin-accent-pink .flashsale-hero,
.admin-layout.admin-accent-pink .order-hero,
.admin-layout.admin-accent-pink .cm-hero,
.admin-layout.admin-accent-pink .skug-header,
.admin-layout.admin-accent-pink .amg-header,
.admin-layout.admin-accent-pink .page-heading,
.admin-layout.admin-accent-pink .page-header {
  border-color: #f2c7d9 !important;
  background:
    radial-gradient(circle at 88% 10%, rgba(244, 114, 182, 0.13), transparent 25%),
    linear-gradient(112deg, #fff0f6 0%, #fff7fb 54%, #ffffff 100%) !important;
  box-shadow: 0 10px 28px rgba(190, 49, 108, 0.09) !important;
}

.admin-layout.admin-accent-pink .admin-page-header::after,
.admin-layout.admin-accent-pink .dashboard-heading::after,
.admin-layout.admin-accent-pink .voucher-hero::after,
.admin-layout.admin-accent-pink .flashsale-hero::after,
.admin-layout.admin-accent-pink .order-hero::after,
.admin-layout.admin-accent-pink .cm-hero::after,
.admin-layout.admin-accent-pink .skug-header::after,
.admin-layout.admin-accent-pink .amg-header::after,
.admin-layout.admin-accent-pink .page-heading::after,
.admin-layout.admin-accent-pink .page-header::after {
  border-color: rgba(219, 63, 125, 0.055) !important;
}

.admin-layout.admin-accent-pink .admin-page-header__icon,
.admin-layout.admin-accent-pink .hero-icon,
.admin-layout.admin-accent-pink .cm-hero-icon,
.admin-layout.admin-accent-pink .skug-header-icon,
.admin-layout.admin-accent-pink .amg-header-icon,
.admin-layout.admin-accent-pink .header-icon {
  border-color: #f2c1d5 !important;
  color: #d93678 !important;
  background: #ffe8f2 !important;
  box-shadow: inset 0 0 0 5px rgba(255, 255, 255, 0.62) !important;
}

.admin-layout.admin-accent-pink .admin-page-header__eyebrow,
.admin-layout.admin-accent-pink .eyebrow,
.admin-layout.admin-accent-pink .skug-breadcrumb,
.admin-layout.admin-accent-pink .amg-breadcrumb,
.admin-layout.admin-accent-pink .greeting-title span {
  color: #d93678 !important;
}

.admin-layout.admin-accent-pink .admin-page-header__actions button,
.admin-layout.admin-accent-pink .voucher-hero > button,
.admin-layout.admin-accent-pink .flashsale-hero > button,
.admin-layout.admin-accent-pink .page-header > button,
.admin-layout.admin-accent-pink .btn-primary-action,
.admin-layout.admin-accent-pink .btn-add,
.admin-layout.admin-accent-pink .btn-primary,
.admin-layout.admin-accent-pink .btn-save,
.admin-layout.admin-accent-pink .btn-submit,
.admin-layout.admin-accent-pink .submit-btn {
  border-color: #d93678 !important;
  color: #ffffff !important;
  background: linear-gradient(135deg, #e7548e, #d93678) !important;
  box-shadow: 0 7px 16px rgba(217, 54, 120, 0.2) !important;
}

.admin-layout.admin-accent-pink input:focus,
.admin-layout.admin-accent-pink select:focus,
.admin-layout.admin-accent-pink textarea:focus,
.admin-layout.admin-accent-pink .form-control:focus,
.admin-layout.admin-accent-pink .form-select:focus {
  border-color: #e7548e !important;
  box-shadow: 0 0 0 3px rgba(231, 84, 142, 0.12) !important;
}

.admin-layout.admin-accent-pink thead th {
  color: #9f285a !important;
  background: #fff0f6 !important;
}

.admin-layout.admin-accent-pink tbody tr:hover td {
  background: #fff6fa !important;
}

.admin-layout.admin-accent-pink .menu-item.active,
.admin-layout.admin-accent-pink .submenu-item.active,
.admin-layout.admin-accent-pink .pagination .active,
.admin-layout.admin-accent-pink .page-link.active,
.admin-layout.admin-accent-pink .nav-pills .active {
  border-color: #d93678 !important;
  color: #ffffff !important;
  background: linear-gradient(135deg, #e7548e, #d93678) !important;
  box-shadow: 0 7px 18px rgba(217, 54, 120, 0.2) !important;
}

.admin-layout.admin-accent-blue {
  --admin-bg: #f4f8fd;
  --admin-surface-soft: #f3f8ff;
  --admin-border: #d6e5f5;
  --admin-primary: #1769ca;
}

@media (prefers-reduced-motion: reduce) {
  .admin-layout,
  .admin-layout * {
    transition-duration: 0.01ms !important;
  }
}

/* Marcus nâng cấp: lớp responsive dùng chung cho toàn bộ màn Admin trên điện thoại.
   Chỉ điều chỉnh cách trình bày, không can thiệp nghiệp vụ của các module thành viên. */
@media (max-width: 720px) {
  .admin-layout .admin-view {
    padding-bottom: max(20px, env(safe-area-inset-bottom));
    overscroll-behavior-y: contain;
  }

  .admin-layout .admin-view > * {
    min-width: 0;
  }

  .admin-layout .admin-view .container,
  .admin-layout .admin-view .container-fluid,
  .admin-layout .admin-view .page-container,
  .admin-layout .admin-view .page-content,
  .admin-layout .admin-view .content-wrapper {
    width: 100% !important;
    max-width: 100% !important;
    padding-inline: 12px !important;
  }

  .admin-layout .admin-page-header,
  .admin-layout .dashboard-heading,
  .admin-layout .voucher-hero,
  .admin-layout .flashsale-hero,
  .admin-layout .order-hero,
  .admin-layout .cm-hero,
  .admin-layout .skug-header,
  .admin-layout .amg-header,
  .admin-layout .page-heading,
  .admin-layout .page-header {
    margin: 12px 12px 18px !important;
    padding: 18px !important;
    border-radius: 16px !important;
  }

  .admin-layout .admin-page-header h1,
  .admin-layout .dashboard-heading h1,
  .admin-layout .voucher-hero h1,
  .admin-layout .flashsale-hero h1,
  .admin-layout .order-hero h1,
  .admin-layout .cm-hero h1,
  .admin-layout .skug-header h1,
  .admin-layout .amg-header h1,
  .admin-layout .page-heading h1,
  .admin-layout .page-header h1 {
    font-size: clamp(1.3rem, 6vw, 1.65rem) !important;
    line-height: 1.25 !important;
  }

  .admin-layout .admin-page-header__actions,
  .admin-layout .page-actions,
  .admin-layout .header-actions,
  .admin-layout .toolbar-actions {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .admin-layout .admin-page-header__actions > *,
  .admin-layout .page-actions > *,
  .admin-layout .header-actions > *,
  .admin-layout .toolbar-actions > * {
    min-height: 42px;
  }

  .admin-layout .filter-row,
  .admin-layout .filters-row,
  .admin-layout .filter-grid,
  .admin-layout .toolbar-panel,
  .admin-layout .search-filter {
    min-width: 0;
    grid-template-columns: 1fr !important;
    flex-direction: column !important;
    align-items: stretch !important;
  }

  .admin-layout input,
  .admin-layout select,
  .admin-layout textarea,
  .admin-layout button,
  .admin-layout .btn {
    min-height: 42px;
  }

  /* Bảng quản trị giữ đủ cột và cuộn trong khung, không kéo vỡ toàn trang. */
  .admin-layout .table-responsive,
  .admin-layout .table-wrapper,
  .admin-layout .data-table-wrapper,
  .admin-layout .table-container {
    width: 100%;
    max-width: calc(100vw - 24px);
    overflow-x: auto !important;
    overscroll-behavior-inline: contain;
    -webkit-overflow-scrolling: touch;
  }

  .admin-layout table {
    min-width: 720px;
  }

  .admin-layout .modal-dialog,
  .admin-layout .modal-content,
  .admin-layout [class*='modal-card'] {
    width: calc(100vw - 24px) !important;
    max-width: calc(100vw - 24px) !important;
    max-height: calc(100dvh - 24px);
    margin: 12px auto !important;
  }
}

@media (max-width: 420px) {
  .admin-layout .admin-page-header,
  .admin-layout .dashboard-heading,
  .admin-layout .voucher-hero,
  .admin-layout .flashsale-hero,
  .admin-layout .order-hero,
  .admin-layout .cm-hero,
  .admin-layout .skug-header,
  .admin-layout .amg-header,
  .admin-layout .page-heading,
  .admin-layout .page-header {
    margin-inline: 8px !important;
    padding: 16px !important;
  }

  .admin-layout .admin-view .container,
  .admin-layout .admin-view .container-fluid,
  .admin-layout .admin-view .page-container,
  .admin-layout .admin-view .page-content,
  .admin-layout .admin-view .content-wrapper {
    padding-inline: 8px !important;
  }
}
</style>
