import { createRouter, createWebHistory } from 'vue-router'
import ClientLayout from '@/layouts/ClientLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import BlankLayout from '@/layouts/BlankLayout.vue'
import Profile from '@/views/client/account/Profile.vue'
const routes = [
  // 1. LUỒNG XÁC THỰC
  {
    path: '/auth',
    component: BlankLayout,
    children: [
      { path: 'login', name: 'Login', component: () => import('@/views/auth/Login.vue') },
      { path: 'register', name: 'Register', component: () => import('@/views/auth/Register.vue') },
      {
        path: 'forgot-password',
        name: 'ForgotPassword',
        component: () => import('@/views/auth/ForgotPassword.vue'),
      },
      {
        path: 'reset-password',
        name: 'ResetPassword',
        component: () => import('@/views/auth/ResetPassword.vue'),
      },
      {
        path: '/auth/verify-otp',
        name: 'VerifyOtp',
        component: () => import('@/views/auth/VerifyOtp.vue'),
      },
    ],
  },

  // 2. LUỒNG KHÁCH HÀNG
  {
    path: '/',
    component: ClientLayout,
    children: [
      { path: '', name: 'Home', component: () => import('@/views/client/shop/Home.vue') },
      { path: 'search', name: 'Search', component: () => import('@/views/client/shop/Search.vue') },
      {
        path: 'category/:slug',
        name: 'ProductList',
        component: () => import('@/views/client/shop/ProductList.vue'),
      },
      {
        path: 'product/:slug',
        name: 'ProductDetail',
        component: () => import('@/views/client/shop/ProductDetail.vue'),
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/client/checkout/Cart.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('@/views/client/checkout/Checkout.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'order-success',
        name: 'OrderSuccess',
        component: () => import('@/views/client/checkout/OrderSuccess.vue'),
      },
      {
        path: 'blog',
        name: 'BlogList',
        component: () => import('@/views/client/cms/BlogList.vue'),
      },
      {
        path: 'blog/:slug',
        name: 'BlogDetail',
        component: () => import('@/views/client/cms/BlogDetail.vue'),
      },
      {
        path: 'chinh-sach',
        name: 'Policy',
        component: () => import('@/views/client/cms/Policy.vue'),
      },
      {
        path: 'contact-store',
        name: 'ConTactStore',
        component: () => import('@/views/client/shop/Contact.vue'),
      },

      //PROFILE
      {
        path: 'profile',
        component: () => import('@/views/client/account/ProfileLayout.vue'),
        children: [
          {
            path: '',
            name: 'ClientProfile',
            component: () => import('@/views/client/account/UserInfo.vue'),
          },
          {
            path: 'addresses',
            name: 'UserAddresses',
            component: () => import('@/views/client/account/AddressBook.vue'),
          },
          {
            path: 'orders',
            name: 'MyOrders',
            component: () => import('@/views/client/account/MyOrders.vue'),
            meta: { requiresAuth: true },
          },
          {
            path: 'orders/:id',
            name: 'OrderDetail',
            component: () => import('@/views/client/account/OrderDetailView.vue'),
          },
          {
            path: 'wishlist',
            name: 'Wishlist',
            component: () => import('@/views/client/account/Wishlist.vue'),
            meta: { requiresAuth: true },
          },
          {
            path: '/change-password',
            name: 'change-password',
            component: () => import('@/views/client/account/ChangePassword.vue'),
          },
        ],
      },
      {
        path: 'khuyen-mai',
        name: 'FlashSalePage',
        component: () => import('@/views/client/shop/FlashSalePage.vue'),
      },
      {
        path: 'about-us',
        name: 'AboutUs',
        component: () => import('@/views/client/cms/AboutUs.vue'),
      },
    ],
  },

  // 3. LUỒNG QUẢN TRỊ ADMIN
  {
    path: '/admin',
    component: AdminLayout,
    meta: {
      requiresAuth: true,
      roles: ['ROLE_ADMIN', 'ROLE_STAFF'],
    },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        // Marcus sửa: Dashboard có quyền riêng để đồng nhất route và API thống kê.
        meta: { permission: 'DASHBOARD_VIEW' },
      },
      {
        path: 'analytics',
        name: 'BusinessAnalytics',
        component: () => import('@/views/admin/analytics/BusinessAnalytics.vue'),
        // Marcus thêm: tab Phân tích độc lập, dùng cùng quyền xem số liệu quản trị.
        meta: { permission: 'DASHBOARD_VIEW' },
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/admin/auth/Profile.vue'),
      },
      {
        path: 'category',
        name: 'CategoryManager',
        component: () => import('@/views/admin/product/CategoryManager.vue'),
        meta: { permission: 'CATEGORY_VIEW' },
      },
      {
        path: 'product',
        name: 'ProductManager',
        component: () => import('@/views/admin/product/ProductManager.vue'),
        meta: { permission: 'PRODUCT_VIEW' },
      },
      {
        path: 'attribute',
        name: 'AttributeManager',
        component: () => import('@/views/admin/product/Attributemanager.vue'),
        meta: { permission: 'ATTRIBUTE_VIEW' },
      },
      {
        path: 'skugenerator',
        name: 'Skugeneratorview',
        component: () => import('@/views/admin/product/Skugeneratorview.vue'),
        // Marcus sửa: đồng bộ route với quyền SKU_CREATE do thành viên định nghĩa.
        meta: { permission: 'SKU_CREATE' },
      },
      {
        path: 'contact-management',
        name: 'ContactManagement',
        component: () => import('@/views/admin/contact/ContactManagement.vue'),
        meta: { permission: 'CONTACT_VIEW' },
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/admin/order/OrderList.vue'),
        meta: { permission: 'ORDER_VIEW' },
      },
      {
        path: 'order/:id',
        name: 'AdminOrderDetail',
        component: () => import('@/views/admin/order/OrderDetail.vue'),
        meta: { permission: 'ORDER_VIEW' },
      },
      {
        path: 'voucher',
        name: 'VoucherManager',
        component: () => import('@/views/admin/promotion/VoucherManager.vue'),
        meta: { permission: 'VOUCHER_VIEW' },
      },
      {
        path: 'flash-sale',
        name: 'FlashSaleManager',
        component: () => import('@/views/admin/promotion/FlashSaleManager.vue'),
        meta: { permission: 'FLASHSALE_VIEW' },
      },
      {
        path: 'banner',
        name: 'BannerManager',
        component: () => import('@/views/admin/cms/BannerManager.vue'),
        meta: { permission: 'BANNER_VIEW' },
      },
      {
        path: 'post',
        name: 'PostManager',
        component: () => import('@/views/admin/cms/PostManager.vue'),
        meta: { permission: 'POST_VIEW' },
      },
      {
        path: 'settings',
        name: 'SystemSettings',
        component: () => import('@/views/admin/settings/SystemSettings.vue'),
        meta: { permission: 'SYSTEM_VIEW' },
      },
      {
        path: 'data-backup',
        name: 'DataBackup',
        component: () => import('@/views/admin/settings/DataBackup.vue'),
        // Marcus thêm: chỉ ADMIN được vào trang tải toàn bộ dữ liệu.
        meta: { roles: ['ROLE_ADMIN'] },
      },
      {
        path: 'employee',
        name: 'EmployeeManagement',
        component: () => import('@/views/admin/auth/EmployeeManagement.vue'),
        meta: { roles: ['ROLE_ADMIN'] },
      },
      {
        path: 'customer',
        name: 'CustomerManagement',
        component: () => import('@/views/admin/auth/CustomerManagement.vue'),
        meta: { permission: 'USER_VIEW' },
      },
      {
        path: 'role',
        name: 'RoleManager',
        component: () => import('@/views/admin/auth/RoleManager.vue'),
        meta: { roles: ['ROLE_ADMIN'] },
      },
      {
        path: 'finance-reports',
        name: 'FinancialReport',
        component: () => import('@/views/client/report/FinancialReport.vue'),
        meta: { permission: 'DONGTIEN_VIEW' },
      },
      {
        path: 'inventoryManager',
        name: 'InventoryManager',
        component: () => import('@/views/admin/auth/InventoryManager.vue'),
      },
      {
        path: 'activity-log',
        name: 'ActivityLog',
        component: () => import('@/views/admin/auth/ActivityLog.vue'),
        // Marcus sửa: API nhật ký chỉ dành cho ADMIN nên route phải đồng nhất.
        meta: { roles: ['ROLE_ADMIN'] },
      },
      {
        path: '/oauth-success',
        name: 'OAuthSuccess',
        component: () => import('@/views/admin/auth/OAuthSuccess.vue'),
      },
      {
        path: '/admin/reviews',
        name: 'ReviewManagement',
        component: () => import('@/views/admin/cms/ReviewManagement.vue'),
      },
    ],
  },

  // Kích hoạt cơ chế bắt URL rác điều hướng về trang chủ
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0, behavior: 'smooth' }
  },
})

router.beforeEach((to) => {
  if (to.path === '/oauth-success') {
    return true
  }
  const token = localStorage.getItem('ACCESS_TOKEN')
  const roles = JSON.parse(localStorage.getItem('USER_ROLE') || '[]')
  const permissions = JSON.parse(localStorage.getItem('USER_PERMISSIONS') || '[]')
  const isAdmin = roles.includes('ROLE_ADMIN')
  const isAdminOrStaff = isAdmin || roles.includes('ROLE_STAFF')

  // ĐÃ ĐĂNG NHẬP LÀ ADMIN/STAFF mà cố vào trang client hoặc trang login -> đẩy vào admin
  if (token && isAdminOrStaff && (to.path === '/' || to.path.startsWith('/auth/login'))) {
    return '/admin/dashboard'
  }

  // Chưa login mà cố vào admin
  if (to.path.startsWith('/admin') && !token) {
    return '/auth/login'
  }

  // USER cố vào admin
  if (to.path.startsWith('/admin') && !isAdminOrStaff) {
    return '/'
  }
  // Route client yêu cầu đăng nhập
  if (to.meta.requiresAuth && !token) {
    return {
      path: '/auth/login',
      query: {
        redirect: to.fullPath,
      },
    }
  }
  // Kiểm tra theo role (chặn thô, vd chỉ ADMIN mới được vào trang)
  const requiredRoles = to.meta.roles
  if (requiredRoles) {
    const hasRole = roles.some((role) => requiredRoles.includes(role))
    if (!hasRole) {
      alert('Bạn không có quyền truy cập trang này')
      return '/admin/dashboard'
    }
  }

  // Kiểm tra theo permission cụ thể (chặn chi tiết theo từng chức năng)
  // ADMIN luôn được bypass, không cần check permission lẻ
  const requiredPermission = to.meta.permission
  if (requiredPermission && !isAdmin) {
    if (!permissions.includes(requiredPermission)) {
      alert('Bạn không có quyền truy cập chức năng này')
      return '/admin/dashboard'
    }
  }

  return true
})

export default router
