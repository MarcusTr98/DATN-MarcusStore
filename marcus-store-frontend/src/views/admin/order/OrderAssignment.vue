<template>
  <section class="assignment-page">
    <AdminPageHeader
      eyebrow="Điều phối vận hành"
      title="Chia đơn hàng"
      :description="
        isAdmin
          ? 'Điều phối và theo dõi khối lượng xử lý đơn.'
          : 'Chủ động nhận đơn tiếp theo theo thứ tự công bằng.'
      "
      icon="bi bi-diagram-3-fill"
    >
      <template #actions>
        <button
          type="button"
          class="assignment-refresh-btn"
          :disabled="loading || staffLoading"
          @click="refreshPage"
        >
          <i class="bi bi-arrow-clockwise" :class="{ spinning: loading }"></i>
          {{ loading ? 'Đang tải...' : 'Làm mới' }}
        </button>
      </template>
    </AdminPageHeader>

    <nav v-if="isAdmin" class="assignment-tabs">
      <button :class="{ active: activeTab === 'dispatch' }" @click="activeTab = 'dispatch'">
        <i class="bi bi-diagram-3"></i> Điều phối đơn
      </button>
      <button :class="{ active: activeTab === 'staff' }" @click="activeTab = 'staff'">
        <i class="bi bi-bar-chart-line"></i> KPI nhân viên
      </button>
    </nav>

    <section v-if="!isAdmin" class="staff-self-panel">
      <div class="staff-ready-state" :class="{ paused: !staffStatus.acceptingOrders }">
        <span class="state-icon"><i class="bi bi-person-check-fill"></i></span>
        <div>
          <small>Trạng thái nhận đơn</small>
          <strong>{{ staffStatus.acceptingOrders ? 'Đang sẵn sàng' : 'Đang tạm dừng' }}</strong>
          <span
            >{{ staffStatus.activeOrderCount }} / {{ staffStatus.maxActiveOrders }} đơn đang phụ
            trách</span
          >
        </div>
        <label class="ready-switch">
          <input
            v-model="staffStatus.acceptingOrders"
            type="checkbox"
            @change="updateAvailability"
          />
          <span></span>
        </label>
      </div>
      <div class="claim-card">
        <span class="queue-number">{{ staffStatus.pendingOrderCount }}</span>
        <h2>{{ staffStatus.pendingOrderCount ? 'đơn đang chờ nhận' : 'Hiện chưa có đơn chờ' }}</h2>
        <p>Hệ thống sẽ giao đơn cũ nhất; thông tin đơn chỉ hiện sau khi nhận thành công.</p>
        <button
          type="button"
          :disabled="staffLoading || !staffStatus.canClaim || staffStatus.pendingOrderCount === 0"
          @click="claimNextOrder"
        >
          <i class="bi bi-hand-index-thumb"></i>
          {{ staffLoading ? 'Đang nhận đơn...' : 'Nhận đơn tiếp theo' }}
        </button>
        <small v-if="staffStatus.unavailableReason">{{ staffStatus.unavailableReason }}</small>
        <small v-else-if="staffStatus.cooldownRemainingSeconds">
          Có thể nhận tiếp sau {{ staffStatus.cooldownRemainingSeconds }} giây
        </small>
      </div>
      <div class="my-load-card">
        <strong>Khối lượng hiện tại: {{ staffStatus.workloadScore }} điểm</strong>
        <span>{{
          loadLabel(
            staffStatus.activeOrderCount,
            staffStatus.maxActiveOrders,
            staffStatus.acceptingOrders,
          )
        }}</span>
        <RouterLink to="/admin/order">Xem các đơn tôi đang phụ trách</RouterLink>
      </div>
      <section class="my-kpi-panel">
        <div><strong>KPI 30 ngày của tôi</strong><span>{{ staffStatus.assignedInPeriodCount }} đơn đã nhận</span></div>
        <article><small>Tỷ lệ tự nhận</small><strong>{{ staffStatus.selfAssignmentRate }}%</strong><span>{{ staffStatus.selfAssignedInPeriodCount }} / {{ staffStatus.assignedInPeriodCount }} đơn</span></article>
        <article><small>Tỷ lệ hoàn thành</small><strong>{{ staffStatus.periodCompletionRate }}%</strong><span>{{ staffStatus.completedInPeriodCount }} / {{ staffStatus.assignedInPeriodCount }} đơn</span></article>
      </section>
    </section>

    <template v-if="isAdmin && activeTab === 'dispatch'">
      <section class="overview-grid">
        <article class="overview-card overview-card--primary">
          <span class="overview-icon"><i class="bi bi-hourglass-split"></i></span>
          <div>
            <span class="overview-label">Đơn đang chờ</span><strong>{{ pendingCount }}</strong
            ><small>Chờ giao thủ công hoặc tự động</small>
          </div>
        </article>
        <article class="overview-card">
          <span class="overview-icon"><i class="bi bi-people-fill"></i></span>
          <div>
            <span class="overview-label">Nhân viên sẵn sàng</span
            ><strong>{{ dashboard.staffLoads.length }}</strong
            ><small>Có quyền xử lý đơn hàng</small>
          </div>
        </article>
        <article class="overview-card">
          <span class="overview-icon"><i class="bi bi-box-seam-fill"></i></span>
          <div>
            <span class="overview-label">Đơn đang xử lý</span><strong>{{ activeOrderTotal }}</strong
            ><small>Trên toàn bộ nhân viên</small>
          </div>
        </article>
      </section>
    </template>

    <section v-if="isAdmin && activeTab === 'staff'" class="kpi-dashboard">
      <div class="kpi-heading">
        <div>
          <h2>KPI nhận và hoàn thành đơn</h2>
          <p>Dữ liệu của các đơn được phân công trong 30 ngày gần nhất.</p>
        </div>
        <span><i class="bi bi-calendar3"></i> 30 ngày</span>
      </div>
      <div class="kpi-overview">
        <article><small>Tổng lượt nhận</small><strong>{{ kpiTotals.total }}</strong></article>
        <article><small>Tự nhận đơn</small><strong>{{ kpiTotals.selfRate }}%</strong><span>{{ kpiTotals.self }} lượt</span></article>
        <article><small>Hoàn thành</small><strong>{{ kpiTotals.completionRate }}%</strong><span>{{ kpiTotals.completed }} đơn</span></article>
        <article><small>Nhân viên</small><strong>{{ dashboard.staffLoads.length }}</strong></article>
      </div>
      <div class="panel kpi-table-wrap">
        <table class="kpi-table">
          <thead><tr><th>Nhân viên</th><th>Tổng nhận</th><th>Tự nhận</th><th>Tự động</th><th>Admin giao</th><th>Tỷ lệ tự nhận</th><th>Hoàn thành</th><th>Tỷ lệ hoàn thành</th></tr></thead>
          <tbody>
            <tr v-for="staff in dashboard.staffLoads" :key="staff.staffId">
              <td><span class="staff-avatar">{{ initials(staff.staffName) }}</span><strong>{{ staff.staffName }}</strong></td>
              <td>{{ staff.totalAssignedCount }}</td><td>{{ staff.selfAssignedCount }}</td><td>{{ staff.autoAssignedCount }}</td><td>{{ staff.manualAssignedCount }}</td>
              <td><b class="kpi-rate">{{ staff.selfAssignmentRate }}%</b></td><td>{{ staff.completedInPeriodCount }}</td><td><b class="kpi-rate kpi-rate--green">{{ staff.periodCompletionRate }}%</b></td>
            </tr>
            <tr v-if="dashboard.staffLoads.length === 0"><td colspan="8" class="kpi-empty">Chưa có nhân viên để thống kê.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="isAdmin && activeTab === 'dispatch'" class="workspace-grid">
      <article class="panel pending-panel">
        <div class="panel-heading">
          <div>
            <h2>Hàng chờ phân công</h2>
            <p>Hệ thống sẽ chọn người có ít đơn mở nhất sau 5 phút.</p>
          </div>
          <span class="count-badge">{{ pendingCount }}</span>
        </div>

        <div v-if="loading" class="loading-state">
          <i class="bi bi-arrow-repeat spinning"></i> Đang tải dữ liệu...
        </div>
        <div v-else-if="dashboard.pendingOrders.length === 0" class="empty-state">
          <i class="bi bi-check2-circle"></i><strong>Không có đơn cần chia</strong
          ><span>Tất cả đơn hiện tại đã có người phụ trách.</span>
        </div>
        <div v-else class="pending-list">
          <article
            v-for="order in dashboard.pendingOrders"
            :key="order.orderCode"
            class="pending-order"
          >
            <div class="order-main">
              <RouterLink :to="`/admin/order/${order.orderCode}`" class="order-code"
                >#{{ order.orderCode }}</RouterLink
              >
              <span class="customer-name">{{ order.recipientName }}</span>
              <strong>{{ currency(order.finalAmount) }}</strong>
            </div>
            <div class="assignment-plan">
              <span class="plan-label">Dự định tự chia cho</span>
              <strong
                ><i class="bi bi-person-check"></i>
                {{ order.plannedStaffName || 'Chưa có nhân viên phù hợp' }}</strong
              >
              <span class="countdown" :class="{ due: countdown(order.autoAssignAt).due }"
                ><i class="bi bi-clock"></i> {{ countdown(order.autoAssignAt).label }}</span
              >
            </div>
            <div class="manual-actions">
              <label :for="`staff-${order.orderCode}`">Giao ngay</label>
              <div class="manual-control">
                <select
                  :id="`staff-${order.orderCode}`"
                  v-model="selectedStaff[order.orderCode]"
                  :disabled="saving === order.orderCode"
                >
                  <option value="">Chọn nhân viên</option>
                  <option
                    v-for="staff in dashboard.staffLoads"
                    :key="staff.staffId"
                    :value="staff.staffId"
                  >
                    {{ staff.staffName }} · {{ staff.activeOrderCount }} đơn
                  </option>
                </select>
                <button
                  type="button"
                  :disabled="!selectedStaff[order.orderCode] || saving === order.orderCode"
                  @click="assign(order.orderCode)"
                >
                  {{ saving === order.orderCode ? 'Đang giao' : 'Giao đơn' }}
                </button>
              </div>
            </div>
          </article>
        </div>
      </article>

      <aside class="panel load-panel">
        <div class="panel-heading">
          <div>
            <h2>Hiệu suất phân công</h2>
            <p>Theo dõi đơn đang giao và tỷ lệ hoàn thành của từng nhân viên.</p>
          </div>
        </div>
        <div v-if="dashboard.staffLoads.length === 0" class="empty-staff">
          Chưa có nhân viên đủ quyền xử lý đơn.
        </div>
        <div v-else class="staff-list">
          <article v-for="staff in dashboard.staffLoads" :key="staff.staffId" class="staff-load">
            <div class="staff-top">
              <span class="staff-avatar">{{ initials(staff.staffName) }}</span>
              <div>
                <strong>{{ staff.staffName }}</strong
                ><small
                  >{{ staff.activeOrderCount }} đang giao · {{ staff.completedOrderCount }} hoàn
                  thành</small
                >
              </div>
              <b>{{ staff.completionRate }}%</b>
            </div>
            <div class="staff-metrics">
              <span
                ><i class="bi bi-inboxes-fill"></i>{{ staff.activeOrderCount }} đang phụ trách</span
              >
              <span
                ><i class="bi bi-check2-circle"></i>{{ staff.completedOrderCount }} hoàn thành</span
              >
              <span><i class="bi bi-speedometer2"></i>{{ staff.workloadScore }} điểm tải</span>
            </div>
            <div class="staff-settings">
              <label>
                <input v-model="staff.acceptingOrders" type="checkbox" /> Sẵn sàng nhận đơn
              </label>
              <label>
                Tối đa
                <input v-model.number="staff.maxActiveOrders" type="number" min="1" max="50" />
              </label>
              <button type="button" @click="saveStaffSettings(staff)">Lưu</button>
            </div>
            <div class="load-track" title="Tỷ lệ hoàn thành">
              <span
                :style="{
                  width: `${Math.max(staff.completionRate, staff.completedOrderCount ? 8 : 0)}%`,
                }"
              ></span>
            </div>
            <small class="completion-label">Tỷ lệ hoàn thành {{ staff.completionRate }}%</small>
          </article>
        </div>
        <div class="automation-note">
          <i class="bi bi-lightning-charge-fill"></i
          ><span
            ><strong>Quy tắc tự động</strong> Chọn nhân viên sẵn sàng có điểm tải thấp nhất; nếu
            bằng nhau, ưu tiên người lâu chưa được giao đơn.</span
          >
        </div>
        <div class="score-guide">
          <strong><i class="bi bi-calculator"></i> Điểm tải được tính thế nào?</strong>
          <p>Cộng điểm của tất cả đơn đang phụ trách theo trạng thái:</p>
          <div>
            <span v-for="(weight, status) in statusWeights" :key="status">
              {{ statusLabel(status) }} <b>{{ weight }} điểm/đơn</b>
            </span>
          </div>
          <small>Ví dụ: 2 đơn đang chuẩn bị = 2 × 2 = 4 điểm. Điểm thấp hơn được ưu tiên.</small>
        </div>
      </aside>
    </section>

    <transition name="toast"
      ><div v-if="message" class="toast-message" :class="message.type">
        <i
          :class="
            message.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'
          "
        ></i
        >{{ message.text }}
      </div></transition
    >
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import OrderAssignmentApi from '@/api/orderAssignmentApi.js'
import StaffOrderAssignmentApi from '@/api/staffOrderAssignmentApi.js'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const roles = JSON.parse(localStorage.getItem('USER_ROLE') || '[]')
const isAdmin = roles.includes('ROLE_ADMIN')
const activeTab = ref('dispatch')
const loading = ref(false)
const staffLoading = ref(false)
const saving = ref('')
const selectedStaff = reactive({})
const message = ref(null)
const now = ref(Date.now())
const dashboard = reactive({ staffLoads: [], pendingOrders: [] })
const staffStatus = reactive({
  acceptingOrders: false,
  maxActiveOrders: 5,
  activeOrderCount: 0,
  workloadScore: 0,
  canClaim: false,
  pendingOrderCount: 0,
  cooldownRemainingSeconds: 0,
  unavailableReason: '',
  assignedInPeriodCount: 0,
  selfAssignedInPeriodCount: 0,
  selfAssignmentRate: 0,
  completedInPeriodCount: 0,
  periodCompletionRate: 0,
})
const pendingCount = computed(() => dashboard.pendingOrders.length)
const activeOrderTotal = computed(() =>
  dashboard.staffLoads.reduce((total, staff) => total + staff.activeOrderCount, 0),
)
const kpiTotals = computed(() => {
  const total = dashboard.staffLoads.reduce((sum, staff) => sum + (staff.totalAssignedCount || 0), 0)
  const self = dashboard.staffLoads.reduce((sum, staff) => sum + (staff.selfAssignedCount || 0), 0)
  const completed = dashboard.staffLoads.reduce(
    (sum, staff) => sum + (staff.completedInPeriodCount || 0),
    0,
  )
  return {
    total,
    self,
    completed,
    selfRate: total ? Math.round((self / total) * 1000) / 10 : 0,
    completionRate: total ? Math.round((Math.min(completed, total) / total) * 1000) / 10 : 0,
  }
})
let timer
let messageTimer

async function loadDashboard() {
  loading.value = true
  try {
    const { data } = await OrderAssignmentApi.getDashboard()
    dashboard.staffLoads = data.staffLoads || []
    dashboard.pendingOrders = data.pendingOrders || []
  } catch {
    showMessage('Không tải được dữ liệu chia đơn. Vui lòng thử lại.', 'error')
  } finally {
    loading.value = false
  }
}

async function loadStaffStatus() {
  staffLoading.value = true
  try {
    const { data } = await StaffOrderAssignmentApi.getStatus()
    Object.assign(staffStatus, data)
  } catch (error) {
    showMessage(error.response?.data?.message || 'Không tải được trạng thái nhận đơn.', 'error')
  } finally {
    staffLoading.value = false
  }
}

function refreshPage() {
  return isAdmin ? loadDashboard() : loadStaffStatus()
}

async function updateAvailability() {
  staffLoading.value = true
  try {
    const { data } = await StaffOrderAssignmentApi.setAvailability(staffStatus.acceptingOrders)
    Object.assign(staffStatus, data)
    showMessage(
      data.acceptingOrders ? 'Bạn đã sẵn sàng nhận đơn.' : 'Đã tạm dừng nhận đơn.',
      'success',
    )
  } catch (error) {
    await loadStaffStatus()
    showMessage(error.response?.data?.message || 'Không thể cập nhật trạng thái.', 'error')
  } finally {
    staffLoading.value = false
  }
}

async function claimNextOrder() {
  staffLoading.value = true
  try {
    const { data } = await StaffOrderAssignmentApi.claimNext()
    showMessage(`Đã nhận đơn ${data.orderCode}.`, 'success')
    window.setTimeout(() => window.location.assign(`/admin/order/${data.orderCode}`), 350)
  } catch (error) {
    showMessage(error.response?.data?.message || 'Không thể nhận đơn lúc này.', 'error')
    await loadStaffStatus()
  } finally {
    staffLoading.value = false
  }
}

async function assign(orderCode) {
  saving.value = orderCode
  try {
    await OrderAssignmentApi.assign(orderCode, {
      staffId: Number(selectedStaff[orderCode]),
      reason: 'Phân công thủ công từ màn chia đơn',
    })
    showMessage(`Đã giao đơn ${orderCode}.`, 'success')
    await loadDashboard()
  } catch (error) {
    showMessage(error.response?.data?.message || 'Không thể giao đơn.', 'error')
  } finally {
    saving.value = ''
  }
}

async function saveStaffSettings(staff) {
  try {
    await OrderAssignmentApi.updateStaffSettings(staff.staffId, {
      acceptingOrders: staff.acceptingOrders,
      maxActiveOrders: Number(staff.maxActiveOrders),
    })
    showMessage(`Đã cập nhật cấu hình của ${staff.staffName}.`, 'success')
    await loadDashboard()
  } catch (error) {
    showMessage(error.response?.data?.message || 'Không thể cập nhật nhân viên.', 'error')
  }
}

function countdown(value) {
  const seconds = Math.ceil(
    (new Date(String(value).replace(' ', 'T')).getTime() - now.value) / 1000,
  )
  if (!value || seconds <= 0) return { label: 'Đang chờ lượt tự động', due: true }
  return {
    label: `Còn ${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`,
    due: false,
  }
}
function currency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value || 0)
}
function initials(name) {
  return String(name || '?')
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()
}
const statusNames = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Đang chuẩn bị',
  READY_FOR_PICKUP: 'Sẵn sàng nhận',
  PACKED: 'Đã đóng gói',
  SHIPPING: 'Đang giao',
  DELIVERED: 'Đã giao',
  FAILED: 'Giao thất bại',
}
const statusWeights = {
  PENDING: 1,
  CONFIRMED: 1.2,
  PROCESSING: 2,
  READY_FOR_PICKUP: 0.7,
  PACKED: 0.8,
  SHIPPING: 0.4,
  DELIVERED: 0.2,
  FAILED: 1.5,
}
function statusLabel(status) {
  return statusNames[status] || status
}
function statusWeight(status) {
  return statusWeights[status] || 1
}
function loadLabel(active, max, accepting) {
  if (!accepting) return 'Đang tạm dừng'
  if (active >= max) return 'Đã đầy tải — Không chia thêm'
  if (active >= Math.max(1, max - 1)) return `Tải vừa — Còn ${max - active} suất`
  return `Tải thấp — Còn ${max - active} suất`
}
function showMessage(text, type) {
  window.clearTimeout(messageTimer)
  message.value = { text, type }
  messageTimer = window.setTimeout(() => {
    message.value = null
  }, 3000)
}

onMounted(() => {
  refreshPage()
  timer = window.setInterval(() => {
    now.value = Date.now()
    if (!isAdmin && staffStatus.cooldownRemainingSeconds > 0) {
      staffStatus.cooldownRemainingSeconds -= 1
      if (staffStatus.cooldownRemainingSeconds === 0) loadStaffStatus()
    }
  }, 1000)
})
onBeforeUnmount(() => {
  window.clearInterval(timer)
  window.clearTimeout(messageTimer)
})
</script>

<style scoped>
.assignment-page {
  max-width: 1440px;
  margin: 0 auto;
  padding: 8px 4px 36px;
  color: #183153;
}
.assignment-refresh-btn {
  border: 1px solid #1d4ed8;
  border-radius: 10px;
  padding: 10px 14px;
  background: #1d4ed8;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 4px 12px #2563eb33;
}
.assignment-refresh-btn:disabled {
  opacity: 0.65;
}
.assignment-tabs {
  display: flex;
  gap: 8px;
  margin: 18px 0;
  border-bottom: 1px solid #dbeafe;
}
.assignment-tabs button {
  border: 0;
  border-bottom: 3px solid transparent;
  padding: 11px 16px;
  background: transparent;
  color: #64748b;
  font-weight: 800;
}
.assignment-tabs button.active {
  border-bottom-color: #2563eb;
  color: #1d4ed8;
}
.staff-self-panel {
  display: grid;
  gap: 18px;
  max-width: 760px;
  margin: 24px auto;
}
.staff-ready-state,
.claim-card,
.my-load-card {
  border: 1px solid #bfdbfe;
  border-radius: 16px;
  background: #fff;
  padding: 20px;
  box-shadow: 0 8px 24px #1d4ed814;
}
.staff-ready-state {
  display: flex;
  align-items: center;
  gap: 14px;
}
.staff-ready-state > div {
  display: grid;
  gap: 3px;
  margin-right: auto;
}
.staff-ready-state strong {
  color: #15803d;
  font-size: 18px;
}
.staff-ready-state.paused strong {
  color: #b45309;
}
.state-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: #dcfce7;
  color: #15803d;
  font-size: 23px;
}
.ready-switch input {
  display: none;
}
.ready-switch span {
  display: block;
  width: 48px;
  height: 26px;
  border-radius: 99px;
  background: #cbd5e1;
  padding: 3px;
  cursor: pointer;
}
.ready-switch span::after {
  content: '';
  display: block;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: 0.2s;
}
.ready-switch input:checked + span {
  background: #22c55e;
}
.ready-switch input:checked + span::after {
  transform: translateX(22px);
}
.claim-card {
  text-align: center;
  background: linear-gradient(145deg, #eff6ff, #fff);
}
.queue-number {
  display: block;
  color: #1d4ed8;
  font-size: 56px;
  font-weight: 900;
  line-height: 1;
}
.claim-card h2 {
  margin: 8px 0;
}
.claim-card p {
  color: #64748b;
}
.claim-card button {
  margin: 12px 0 5px;
  border: 0;
  border-radius: 12px;
  padding: 13px 24px;
  background: #1d4ed8;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
}
.claim-card button:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}
.claim-card small {
  display: block;
  color: #b45309;
}
.my-load-card {
  display: flex;
  align-items: center;
  gap: 12px;
}
.my-load-card span {
  color: #64748b;
  margin-right: auto;
}
.my-kpi-panel { display: grid; grid-template-columns: 1.3fr 1fr 1fr; gap: 12px; }
.my-kpi-panel > div, .my-kpi-panel article { padding: 17px; border: 1px solid #bfdbfe; border-radius: 13px; background: #fff; }
.my-kpi-panel > div { display: flex; flex-direction: column; justify-content: center; background: #eff6ff; }
.my-kpi-panel small, .my-kpi-panel span { display: block; color: #64748b; font-size: 12px; }
.my-kpi-panel article strong { display: block; margin: 4px 0; color: #1d4ed8; font-size: 25px; }
.kpi-dashboard {
  display: grid;
  gap: 18px;
}
.kpi-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.kpi-heading h2 { margin: 0; font-size: 20px; }
.kpi-heading p { margin: 5px 0 0; color: #64748b; }
.kpi-heading > span { border-radius: 9px; padding: 8px 11px; background: #eff6ff; color: #1d4ed8; font-weight: 700; }
.kpi-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.kpi-overview article { padding: 17px; border: 1px solid #bfdbfe; border-radius: 13px; background: #fff; }
.kpi-overview small, .kpi-overview span { display: block; color: #64748b; }
.kpi-overview strong { display: block; margin: 4px 0; color: #1d4ed8; font-size: 27px; }
.kpi-table-wrap { overflow-x: auto; }
.kpi-table { width: 100%; border-collapse: collapse; white-space: nowrap; }
.kpi-table th { padding: 13px 14px; background: #f8fbff; color: #64748b; font-size: 12px; text-align: right; }
.kpi-table th:first-child { text-align: left; }
.kpi-table td { padding: 14px; border-top: 1px solid #dbeafe; text-align: right; }
.kpi-table td:first-child { display: flex; align-items: center; gap: 9px; text-align: left; }
.kpi-rate { color: #1d4ed8; }
.kpi-rate--green { color: #15803d; }
.kpi-empty { display: table-cell !important; padding: 35px !important; color: #64748b; text-align: center !important; }
.spinning {
  animation: spin 0.9s linear infinite;
}
.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin: 20px 0;
}
.overview-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border: 1px solid #b9d2ff;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 3px 13px #1d4ed812;
}
.overview-card--primary {
  border-color: #2563eb;
  background: #eff6ff;
}
.overview-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 19px;
}
.overview-label,
.overview-card small {
  display: block;
  color: #708096;
  font-size: 12px;
}
.overview-card strong {
  display: block;
  margin: 1px 0;
  font-size: 26px;
  line-height: 1.15;
}
.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(300px, 0.8fr);
  gap: 20px;
  align-items: start;
}
.panel {
  overflow: hidden;
  border: 1px solid #9fc5ff;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 6px 20px #1d4ed814;
}
.panel-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 20px 16px;
  border-bottom: 1px solid #bfdbfe;
  background: #f8fbff;
}
.panel-heading h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 800;
}
.panel-heading p {
  margin: 5px 0 0;
  color: #738197;
  font-size: 13px;
}
.count-badge {
  display: grid;
  place-items: center;
  min-width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #fff2cc;
  color: #9a6700;
  font-size: 13px;
  font-weight: 800;
}
.pending-list {
  padding: 12px;
}
.pending-order {
  display: grid;
  grid-template-columns: 1fr 1.22fr 1.5fr;
  gap: 18px;
  align-items: center;
  padding: 16px;
  margin-bottom: 10px;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 8px #2563eb0a;
}
.pending-order:last-child {
  margin-bottom: 0;
}
.order-main {
  display: grid;
  gap: 4px;
}
.order-code {
  font-weight: 800;
  color: #1d4ed8;
  text-decoration: none;
}
.customer-name {
  font-size: 13px;
  color: #607086;
}
.assignment-plan {
  display: grid;
  gap: 4px;
}
.plan-label,
.manual-actions label {
  font-size: 11px;
  font-weight: 800;
  color: #8190a5;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.assignment-plan strong {
  font-size: 14px;
}
.assignment-plan strong i {
  color: #2563eb;
}
.countdown {
  width: max-content;
  font-size: 12px;
  color: #64748b;
}
.countdown.due {
  color: #c2410c;
  font-weight: 700;
}
.manual-actions {
  display: grid;
  gap: 6px;
}
.manual-control {
  display: flex;
  gap: 7px;
}
.manual-control select {
  min-width: 0;
  flex: 1;
  border: 1px solid #93c5fd;
  border-radius: 8px;
  padding: 8px;
  background: #fff;
  font-size: 12px;
}
.manual-control button {
  border: 1px solid #1d4ed8;
  border-radius: 8px;
  padding: 8px 10px;
  background: #1d4ed8;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}
.manual-control button:disabled {
  background: #9aa8bb;
}
.load-panel {
  position: sticky;
  top: 16px;
}
.staff-list {
  padding: 5px 20px;
}
.staff-load {
  padding: 15px 0;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  margin: 10px 0;
  padding: 14px;
  background: #fbfdff;
}
.staff-load:last-child {
  margin-bottom: 10px;
}
.staff-top {
  display: grid;
  grid-template-columns: 34px 1fr auto;
  align-items: center;
  gap: 9px;
}
.staff-avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 800;
}
.staff-top strong,
.staff-top small {
  display: block;
}
.staff-top small {
  margin-top: 2px;
  color: #77869a;
  font-size: 12px;
}
.staff-top b {
  color: #15803d;
  font-size: 13px;
}
.staff-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.staff-metrics span {
  border: 1px solid #bfdbfe;
  border-radius: 7px;
  padding: 4px 7px;
  color: #43617f;
  font-size: 11px;
  font-weight: 700;
}
.staff-metrics i {
  margin-right: 4px;
  color: #2563eb;
}
.staff-settings {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  font-size: 12px;
}
.staff-settings label {
  display: flex;
  align-items: center;
  gap: 5px;
}
.staff-settings input[type='number'] {
  width: 52px;
  padding: 3px;
}
.staff-settings button {
  border: 0;
  border-radius: 6px;
  padding: 5px 9px;
  background: #2563eb;
  color: #fff;
}
.load-track {
  height: 7px;
  margin-top: 10px;
  overflow: hidden;
  border-radius: 99px;
  border: 1px solid #dbeafe;
  background: #eff6ff;
}
.load-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #4ade80, #15803d);
}
.completion-label {
  display: block;
  margin-top: 6px;
  color: #4b6684;
  font-size: 11px;
  font-weight: 700;
}
.automation-note {
  display: flex;
  gap: 10px;
  margin: 16px;
  padding: 13px;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  background: #eff6ff;
  color: #52657f;
  font-size: 12px;
  line-height: 1.45;
}
.automation-note i {
  color: #e69b13;
  font-size: 17px;
}
.automation-note strong {
  display: block;
  color: #1f3a63;
}
.score-guide {
  margin: 16px;
  padding: 14px;
  border: 1px solid #f0c36a;
  border-radius: 10px;
  background: #fffbeb;
}
.score-guide > strong { color: #92400e; }
.score-guide p, .score-guide small { color: #64748b; font-size: 12px; }
.score-guide > div { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin: 10px 0; }
.score-guide span { display: flex; justify-content: space-between; gap: 8px; padding: 5px 7px; border-radius: 6px; background: #fff; font-size: 11px; }
.loading-state,
.empty-state {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 240px;
  color: #738197;
}
.empty-state i {
  font-size: 38px;
  color: #22a06b;
}
.empty-state strong {
  color: #334155;
}
.empty-state span {
  font-size: 13px;
}
.empty-staff {
  padding: 30px 20px;
  color: #738197;
  font-size: 13px;
}
.toast-message {
  position: fixed;
  right: 25px;
  bottom: 25px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 13px 16px;
  border-radius: 10px;
  color: #fff;
  box-shadow: 0 10px 28px #17203340;
  z-index: 50;
}
.toast-message.success {
  background: #16834a;
}
.toast-message.error {
  background: #c0362c;
}
.toast-enter-active,
.toast-leave-active {
  transition: 0.2s;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 1000px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
  .load-panel {
    position: static;
  }
  .pending-order {
    grid-template-columns: 1fr 1fr;
  }
  .manual-actions {
    grid-column: 1/-1;
  }
}
@media (max-width: 680px) {
  .assignment-page {
    padding: 0;
  }
  .overview-grid {
    grid-template-columns: 1fr;
  }
  .kpi-overview { grid-template-columns: 1fr 1fr; }
  .my-kpi-panel { grid-template-columns: 1fr; }
  .pending-order {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .manual-control {
    flex-direction: column;
  }
  .manual-control button {
    width: 100%;
  }
}
</style>
