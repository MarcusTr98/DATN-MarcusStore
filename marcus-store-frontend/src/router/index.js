import { createRouter, createWebHistory } from 'vue-router'
import ClientLayout from '@/layouts/ClientLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import BlankLayout from '@/layouts/BlankLayout.vue'

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
}
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
      { path: 'cart', name: 'Cart', component: () => import('@/views/client/checkout/Cart.vue') },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('@/views/client/checkout/Checkout.vue'),
      },
      {
        path: 'order-success',
        name: 'OrderSuccess',
        component: () => import('@/views/client/checkout/OrderSuccess.vue'),
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/client/account/Profile.vue'),
      },
      {
        path: 'my-orders',
        name: 'MyOrders',
        component: () => import('@/views/client/account/MyOrders.vue'),
      },
      {
        path: 'my-orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/client/account/OrderDetailView.vue'),
      },
      {
        path: 'wishlist',
        name: 'Wishlist',
        component: () => import('@/views/client/account/Wishlist.vue'),
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
      },
      {
        path: 'product',
        name: 'ProductManager',
        component: () => import('@/views/admin/product/ProductManager.vue'),
      },
      {
        path: 'attribute',
        name: 'AttributeManager',
        component: () => import('@/views/admin/product/Attributemanager.vue'),
        meta: { roles: ['ROLE_ADMIN'] },
      },
      {
        path: 'skugenerator',
        name: 'Skugeneratorview',
        component: () => import('@/views/admin/product/Skugeneratorview.vue'),
        meta: { roles: ['ROLE_ADMIN'] },
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/admin/order/OrderList.vue'),
      },
      {
        path: 'order/:id',
        name: 'AdminOrderDetail',
        component: () => import('@/views/admin/order/OrderDetail.vue'),
      },
      {
        path: 'voucher',
        name: 'VoucherManager',
        component: () => import('@/views/admin/promotion/VoucherManager.vue'),
      },
      {
        path: 'flash-sale',
        name: 'FlashSaleManager',
        component: () => import('@/views/admin/promotion/FlashSaleManager.vue'),
      },
      {
        path: 'banner',
        name: 'BannerManager',
        component: () => import('@/views/admin/cms/BannerManager.vue'),
      },
      {
        path: 'post',
        name: 'PostManager',
        component: () => import('@/views/admin/cms/PostManager.vue'),
      },
      {
        path: 'settings',
        name: 'SystemSettings',
        component: () => import('@/views/admin/settings/SystemSettings.vue'),
      },
      {
        path: 'account',
        name: 'AccountManager',
        component: () => import('@/views/admin/auth/AccountManager.vue'),
      },
      {
        path: 'role',
        name: 'RoleManager',
        component: () => import('@/views/admin/auth/RoleManager.vue'),
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
  const token = localStorage.getItem('ACCESS_TOKEN')
  const roles = JSON.parse(localStorage.getItem('USER_ROLE') || '[]')

  // Chưa login mà cố vào admin
  if (to.path.startsWith('/admin') && !token) {
    return '/auth/login'
  }

  // USER cố vào admin
  if (
    to.path.startsWith('/admin') &&
    !roles.includes('ROLE_ADMIN') &&
    !roles.includes('ROLE_STAFF')
  ) {
    return '/'
  }

  // Kiểm tra quyền riêng từng màn hình
  const requiredRoles = to.meta.roles
  if (requiredRoles) {
    const hasPermission = roles.some((role) => requiredRoles.includes(role))
    if (!hasPermission) {
      alert('Bạn không có quyền truy cập trang này')
      return '/admin/dashboard'
    }
  }
  return true
})

export default router
