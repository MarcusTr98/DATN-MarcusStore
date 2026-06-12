<template>
  <div class="profile-page">
    <div class="container py-5">
      <div class="row g-4">
        <div class="col-lg-3">
          <div class="sidebar-card">
            <div class="avt-ring">
              <div class="avt-inner">
                <span class="avt-letters">{{ initials }}</span>
              </div>
            </div>

            <h5 class="user-name">{{ user.fullName || 'Khách hàng' }}</h5>
            <p class="user-handle">@{{ user.username || 'user' }}</p>
            <span class="badge-silver"><i class="fas fa-crown me-1"></i> Thành viên Bạc</span>

            <hr class="divider" />

            <nav class="sidebar-nav">
              <router-link to="/profile" exact-active-class="active" class="nav-item">
                <i class="fas fa-user"></i> Hồ sơ cá nhân
              </router-link>
              <router-link to="/profile/addresses" active-class="active" class="nav-item">
                <i class="fas fa-map-marker-alt"></i> Sổ địa chỉ
              </router-link>
              <router-link to="/profile/orders" active-class="active" class="nav-item">
                <i class="fas fa-box"></i> Đơn hàng của tôi
              </router-link>
              <router-link to="/profile/wishlist" active-class="active" class="nav-item">
                <i class="fas fa-heart"></i> Sản phẩm yêu thích
              </router-link>
            </nav>

            <hr class="divider" />
            <button class="btn-logout" @click="handleLogout">
              <i class="fas fa-sign-out-alt me-2"></i> Đăng xuất
            </button>
          </div>
        </div>

        <div class="col-lg-9">
          <router-view />
        </div>
      </div>
    </div>

    <BaseModal
      :visible="globalModal.visible"
      :type="globalModal.type"
      :title="globalModal.title"
      :message="globalModal.message"
      @close="globalModal.visible = false"
      @confirm="executeModalAction"
    />
  </div>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import userApi from '@/api/userApi'
import BaseModal from '@/components/common/BaseModal.vue'
import '@/assets/css/profile.css'

const router = useRouter()

const user = reactive({ username: '', fullName: '' })

const initials = computed(() =>
  (user.fullName || 'U')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join(''),
)

onMounted(() => {
  fetchSidebarProfile()
})

const fetchSidebarProfile = async () => {
  try {
    const res = await userApi.getMyProfile()
    if (res.data && res.data.data) {
      user.username = res.data.data.username
      user.fullName = res.data.data.fullName
    }
  } catch (error) {
    console.error('Lỗi lấy sidebar profile', error)
  }
}

// Global Modal Setup
const globalModal = reactive({
  visible: false,
  type: 'info',
  title: '',
  message: '',
  actionCallback: null,
})
const showConfirm = (title, message, callback) => {
  globalModal.type = 'confirm'
  globalModal.title = title
  globalModal.message = message
  globalModal.actionCallback = callback
  globalModal.visible = true
}
const executeModalAction = () => {
  if (globalModal.actionCallback) globalModal.actionCallback()
}

const handleLogout = () => {
  showConfirm('Xác nhận đăng xuất', 'Bạn có chắc chắn muốn thoát khỏi hệ thống?', () => {
    localStorage.removeItem('ACCESS_TOKEN')
    localStorage.removeItem('USER_ROLE')
    localStorage.removeItem('USERNAME')
    router.push('/auth/login')
  })
}
</script>
