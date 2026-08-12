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

            <!-- Hạng thành viên (đồng bộ ngưỡng với admin) -->
            <span class="tier-badge" :class="tier.cls">
              <span class="tier-icon">{{ tier.icon }}</span>
              <span>Thành viên {{ tier.label }}</span>
            </span>
            <p v-if="totalSpent > 0" class="tier-spent">
              Tổng chi tiêu: <b>{{ formattedSpent }}</b>
            </p>

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
              <router-link to="/profile/change-password" active-class="active" class="nav-item">
                <i class="fas fa-shield-alt"></i>Đổi mật khẩu
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
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import userApi from '@/api/userApi'
import BaseModal from '@/components/BaseModal.vue'
import { getTier, formatVND } from '@/composables/useMembershipTier'
import '@/assets/css/profile.css'

const router = useRouter()

const user = reactive({
  username: '',
  fullName: '',
  totalSpent: 0,
})

// Hạng & tổng chi tiêu lấy từ BE (endpoint /client/profile/tier)
const totalSpent = ref(0)
const tier = ref(getTier(0))

// Map label tiếng Việt (BE) → cls trong FE
const RANK_CLS = {
  'Đồng': 'bronze',
  'Bạc': 'silver',
  'Vàng': 'gold',
  'Kim Cương': 'diamond',
}

const formattedSpent = computed(() => formatVND(totalSpent.value))

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
  fetchMyTier()
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

const fetchMyTier = async () => {
  try {
    const res = await userApi.getMyTier()
    if (res.data && res.data.data) {
      const data = res.data.data
      const rank = data.rank || 'Đồng'
      const spent = Number(data.totalSpent ?? 0)

      totalSpent.value = spent

      const cls = RANK_CLS[rank] || getTier(spent).cls
      tier.value = {
        label: rank,
        icon: getTier(spent).icon,
        cls,
        rank: getTier(spent).rank,
      }
    }
  } catch (error) {
    // BE lỗi → fallback dùng totalSpent = 0, rank = 'Đồng'
    console.error('Lỗi lấy hạng thành viên', error)
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

<style scoped>
/* ── Hạng thành viên — đồng bộ style với admin/CustomerTable ── */
.tier-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  border-radius: 999px;
  padding: 5px 14px;
  font-size: 0.78rem;
  font-weight: 800;
  white-space: nowrap;
  margin-top: 4px;
}

.tier-badge .tier-icon {
  font-size: 0.95rem;
  line-height: 1;
}

.tier-badge.bronze {
  background: #fdf3e7;
  color: #a0522d;
  border: 1px solid #f5cfa0;
}
.tier-badge.silver {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
}
.tier-badge.gold {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fcd34d;
}
.tier-badge.diamond {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #93c5fd;
}

.tier-spent {
  margin: 6px 0 0;
  font-size: 0.78rem;
  color: #6b7280;
  font-weight: 500;
}
.tier-spent b {
  color: #111827;
}
</style>
