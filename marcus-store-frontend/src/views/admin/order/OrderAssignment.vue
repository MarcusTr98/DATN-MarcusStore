<template>
  <section class="assignment-page">
    <AdminPageHeader
      eyebrow="Điều phối vận hành"
      title="Chia đơn hàng"
      description="Cân bằng tải xử lý, giao đơn thủ công hoặc để hệ thống tự phân sau 5 phút."
      icon="bi bi-diagram-3-fill"
    >
      <template #actions>
        <button
          type="button"
          class="assignment-refresh-btn"
          :disabled="loading"
          @click="loadDashboard"
        >
          <i class="bi bi-arrow-clockwise" :class="{ spinning: loading }"></i>
          {{ loading ? 'Đang tải...' : 'Làm mới' }}
        </button>
      </template>
    </AdminPageHeader>

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

    <section class="workspace-grid">
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
            <h2>Tải xử lý</h2>
            <p>Tỷ lệ dựa trên số đơn đang mở.</p>
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
                ><small>{{ staff.activeOrderCount }} đơn đang xử lý</small>
              </div>
              <b>{{ staff.workloadRate }}%</b>
            </div>
            <div class="load-track">
              <span
                :style="{
                  width: `${Math.max(staff.workloadRate, staff.activeOrderCount ? 8 : 0)}%`,
                }"
              ></span>
            </div>
          </article>
        </div>
        <div class="automation-note">
          <i class="bi bi-lightning-charge-fill"></i
          ><span
            ><strong>Quy tắc tự động</strong> Chọn nhân viên có ít đơn mở nhất, nếu bằng nhau, ưu
            tiên mã nhân viên nhỏ hơn.</span
          >
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
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const loading = ref(false)
const saving = ref('')
const selectedStaff = reactive({})
const message = ref(null)
const now = ref(Date.now())
const dashboard = reactive({ staffLoads: [], pendingOrders: [] })
const pendingCount = computed(() => dashboard.pendingOrders.length)
const activeOrderTotal = computed(() =>
  dashboard.staffLoads.reduce((total, staff) => total + staff.activeOrderCount, 0),
)
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
function showMessage(text, type) {
  window.clearTimeout(messageTimer)
  message.value = { text, type }
  messageTimer = window.setTimeout(() => {
    message.value = null
  }, 3000)
}

onMounted(() => {
  loadDashboard()
  timer = window.setInterval(() => {
    now.value = Date.now()
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
  color: #1d4ed8;
  font-size: 13px;
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
  background: linear-gradient(90deg, #60a5fa, #1d4ed8);
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
