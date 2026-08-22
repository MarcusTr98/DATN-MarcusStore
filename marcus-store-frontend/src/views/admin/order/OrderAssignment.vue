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

    <nav v-if="isAdmin" class="assignment-tabs" aria-label="Chức năng chia đơn">
      <button :class="{ active: activeTab === 'dispatch' }" @click="activeTab = 'dispatch'">
        <span class="tab-icon"><i class="bi bi-diagram-3-fill"></i></span>
        <strong>Điều phối đơn</strong>
      </button>
      <button :class="{ active: activeTab === 'staff' }" @click="activeTab = 'staff'">
        <span class="tab-icon"><i class="bi bi-bar-chart-line-fill"></i></span>
        <strong>KPI nhân viên</strong>
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
        <p>Hệ thống sẽ giao đơn cũ nhất, thông tin đơn chỉ hiện sau khi nhận thành công.</p>
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
        <strong>Khối lượng hiện tại: {{ integerScore(staffStatus.workloadScore) }} điểm</strong>
        <span>{{
          loadLabel(
            staffStatus.activeOrderCount,
            staffStatus.maxActiveOrders,
            staffStatus.acceptingOrders,
          )
        }}</span>
        <RouterLink to="/admin/order">Xem các đơn tôi đang phụ trách</RouterLink>
      </div>
      <section class="my-score-guide">
        <div class="my-score-guide__heading">
          <span><i class="bi bi-calculator"></i></span>
          <div>
            <strong>Cách tính điểm tải của tôi</strong
            ><small>Cộng điểm các đơn đang phụ trách theo trạng thái.</small>
          </div>
        </div>
        <div class="my-score-formula">
          <span v-for="(count, status) in staffStatus.workloadBreakdown" :key="status">
            {{ statusLabel(status) }}: {{ count }} đơn × <b>{{ statusWeights[status] || 1 }}</b> =
            <strong>{{ count * (statusWeights[status] || 1) }} điểm</strong>
          </span>
          <span v-if="Object.keys(staffStatus.workloadBreakdown || {}).length === 0"
            >Chưa có đơn đang phụ trách nên điểm tải bằng 0.</span
          >
        </div>
        <small
          >Đơn càng cần nhiều công xử lý thì có điểm càng cao. Tổng điểm thấp hơn sẽ được ưu tiên
          chia đơn.</small
        >
      </section>
      <section class="my-kpi-panel">
        <div>
          <strong>KPI 30 ngày của tôi</strong
          ><span>{{ staffStatus.assignedInPeriodCount }} đơn đã nhận</span>
        </div>
        <article>
          <small>Tỷ lệ tự nhận</small><strong>{{ staffStatus.selfAssignmentRate }}%</strong
          ><span
            >{{ staffStatus.selfAssignedInPeriodCount }} /
            {{ staffStatus.assignedInPeriodCount }} đơn</span
          >
        </article>
        <article>
          <small>Tỷ lệ hoàn thành</small><strong>{{ staffStatus.periodCompletionRate }}%</strong
          ><span
            >{{ staffStatus.completedInPeriodCount }} /
            {{ staffStatus.assignedInPeriodCount }} đơn</span
          >
        </article>
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
            ><strong>{{ readyStaffCount }}</strong
            ><small>Đang bật nhận đơn và chưa đầy tải</small>
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
        <article>
          <small>Tổng lượt nhận</small><strong>{{ kpiTotals.total }}</strong>
        </article>
        <article>
          <small>Tự nhận đơn</small><strong>{{ kpiTotals.selfRate }}%</strong
          ><span>{{ kpiTotals.self }} lượt</span>
        </article>
        <article>
          <small>Hoàn thành</small><strong>{{ kpiTotals.completionRate }}%</strong
          ><span>{{ kpiTotals.completed }} đơn</span>
        </article>
        <article>
          <small>Nhân viên</small><strong>{{ dashboard.staffLoads.length }}</strong>
        </article>
      </div>
      <article class="panel kpi-chart-card">
        <div class="panel-heading">
          <div>
            <h2>So sánh tỷ lệ theo nhân viên</h2>
            <p>Tỷ lệ tự nhận và hoàn thành trong 30 ngày gần nhất.</p>
          </div>
        </div>
        <apexchart height="340" type="bar" :options="kpiChartOptions" :series="kpiChartSeries" />
      </article>
      <div class="panel kpi-table-wrap">
        <table class="kpi-table">
          <thead>
            <tr>
              <th>Nhân viên</th>
              <th>Tổng nhận</th>
              <th>Tự nhận</th>
              <th>Tự động</th>
              <th>Admin giao</th>
              <th>Tỷ lệ tự nhận</th>
              <th>Hoàn thành</th>
              <th>Tỷ lệ hoàn thành</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="staff in pagedKpiStaff" :key="staff.staffId">
              <td>
                <span class="staff-avatar">{{ initials(staff.staffName) }}</span
                ><strong>{{ staff.staffName }}</strong>
              </td>
              <td>{{ staff.totalAssignedCount }}</td>
              <td>{{ staff.selfAssignedCount }}</td>
              <td>{{ staff.autoAssignedCount }}</td>
              <td>{{ staff.manualAssignedCount }}</td>
              <td>
                <b class="kpi-rate">{{ staff.selfAssignmentRate }}%</b>
              </td>
              <td>{{ staff.completedInPeriodCount }}</td>
              <td>
                <b class="kpi-rate kpi-rate--green">{{ staff.periodCompletionRate }}%</b>
              </td>
            </tr>
            <tr v-if="dashboard.staffLoads.length === 0">
              <td colspan="8" class="kpi-empty">Chưa có nhân viên để thống kê.</td>
            </tr>
          </tbody>
        </table>
        <AppPagination v-if="kpiTotalPages > 1" v-model="kpiPage" :total-pages="kpiTotalPages" />
      </div>
    </section>

    <section v-if="isAdmin && activeTab === 'dispatch'" class="workspace-grid">
      <article class="panel pending-panel">
        <div class="panel-heading">
          <div>
            <h2>Hàng chờ phân công</h2>
            <p>Hệ thống sẽ chọn người có điểm tải thấp nhất sau 5 phút.</p>
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
        <AppPagination
          v-if="pendingTotalPages > 1"
          :model-value="pendingPage"
          :total-pages="pendingTotalPages"
          @update:model-value="changePendingPage"
        />
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
          <article v-for="staff in pagedStaffLoads" :key="staff.staffId" class="staff-load">
            <div class="staff-top">
              <span class="staff-avatar">{{ initials(staff.staffName) }}</span>
              <div>
                <strong>{{ staff.staffName }}</strong
                ><small
                  >{{ staff.activeOrderCount }} đang giao · {{ staff.completedOrderCount }} hoàn
                  thành</small
                >
              </div>
              <div class="workload-score" title="Tổng điểm tải từ các đơn đang phụ trách">
                <b>{{ integerScore(staff.workloadScore) }}</b
                ><small>điểm tải</small>
              </div>
            </div>
            <div class="staff-metrics">
              <span
                ><i class="bi bi-inboxes-fill"></i>{{ staff.activeOrderCount }} đang phụ trách</span
              >
              <span
                ><i class="bi bi-check2-circle"></i>{{ staff.completedOrderCount }} hoàn thành</span
              >
            </div>
            <div class="staff-settings">
              <label
                title="Staff tự bật trạng thái này. Admin có thể khóa hoặc mở lại khi cần điều phối."
              >
                <input v-model="staff.acceptingOrders" type="checkbox" /> Cho phép nhận đơn
              </label>
              <label>
                Tối đa
                <input v-model.number="staff.maxActiveOrders" type="number" min="1" max="50" />
              </label>
              <button type="button" @click="saveStaffSettings(staff)">Lưu</button>
            </div>
            <small class="admin-override-hint"
              >Trạng thái do staff tự chọn · Admin có quyền ghi đè</small
            >
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
        <AppPagination
          v-if="staffTotalPages > 1"
          v-model="staffPage"
          :total-pages="staffTotalPages"
        />
        <div class="automation-note">
          <i class="bi bi-lightning-charge-fill"></i
          ><span
            ><strong>Quy tắc tự động</strong> Chọn nhân viên sẵn sàng có điểm tải thấp nhất, nếu
            bằng nhau, ưu tiên người lâu chưa được giao đơn.</span
          >
        </div>
        <div class="score-guide">
          <strong><i class="bi bi-calculator"></i> Điểm tải được tính thế nào?</strong>
          <p>Cộng điểm của tất cả đơn đang phụ trách theo trạng thái:</p>
          <div>
            <span v-for="(weight, status) in statusWeights" :key="status">
              {{ statusLabel(status) }} <b class="score-dot">{{ weight }}</b>
            </span>
          </div>
          <small>Ví dụ: 2 đơn đang chuẩn bị = 2 × 3 = 6 điểm. Điểm thấp hơn được ưu tiên.</small>
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
import AppPagination from '@/components/admin/AppPagination.vue'

const roles = JSON.parse(localStorage.getItem('USER_ROLE') || '[]')
const isAdmin = roles.includes('ROLE_ADMIN')
const activeTab = ref('dispatch')
const loading = ref(false)
const staffLoading = ref(false)
const saving = ref('')
const selectedStaff = reactive({})
const message = ref(null)
const now = ref(Date.now())
const dashboard = reactive({
  staffLoads: [],
  pendingOrders: [],
  pendingTotalElements: 0,
  pendingTotalPages: 0,
})
const staffStatus = reactive({
  acceptingOrders: false,
  maxActiveOrders: 5,
  activeOrderCount: 0,
  workloadScore: 0,
  workloadBreakdown: {},
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
const pendingCount = computed(() => dashboard.pendingTotalElements)
const PAGE_SIZE = 5
const pendingPage = ref(1)
const staffPage = ref(1)
const kpiPage = ref(1)
const pageCount = (items) => Math.max(1, Math.ceil(items.length / PAGE_SIZE))
const pageSlice = (items, page) => items.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
const pendingTotalPages = computed(() => dashboard.pendingTotalPages || 1)
const staffTotalPages = computed(() => pageCount(dashboard.staffLoads))
const kpiTotalPages = computed(() => pageCount(dashboard.staffLoads))
const pagedStaffLoads = computed(() => pageSlice(dashboard.staffLoads, staffPage.value))
const pagedKpiStaff = computed(() => pageSlice(dashboard.staffLoads, kpiPage.value))
const activeOrderTotal = computed(() =>
  dashboard.staffLoads.reduce((total, staff) => total + staff.activeOrderCount, 0),
)
const readyStaffCount = computed(
  () => dashboard.staffLoads.filter((staff) => staff.eligibleForAssignment).length,
)
const kpiTotals = computed(() => {
  const total = dashboard.staffLoads.reduce(
    (sum, staff) => sum + (staff.totalAssignedCount || 0),
    0,
  )
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
const kpiChartSeries = computed(() => [
  {
    name: 'Tỷ lệ tự nhận',
    data: dashboard.staffLoads.map((staff) => staff.selfAssignmentRate || 0),
  },
  {
    name: 'Tỷ lệ hoàn thành',
    data: dashboard.staffLoads.map((staff) => staff.periodCompletionRate || 0),
  },
])
const kpiChartOptions = computed(() => ({
  chart: {
    type: 'bar',
    toolbar: { show: false },
    fontFamily: 'inherit',
    animations: { enabled: true, easing: 'easeinout', speed: 500 },
  },
  colors: ['#2563eb', '#16a34a'],
  plotOptions: {
    bar: {
      borderRadius: 6,
      borderRadiusApplication: 'end',
      columnWidth: dashboard.staffLoads.length > 8 ? '70%' : '52%',
    },
  },
  dataLabels: {
    enabled: true,
    formatter: (value) => (value > 0 ? `${Math.round(value)}%` : ''),
    offsetY: -8,
    style: { fontSize: '11px', fontWeight: 700, colors: ['#334155'] },
    background: { enabled: false },
  },
  grid: { borderColor: '#dbeafe', strokeDashArray: 4, padding: { top: 16, right: 10 } },
  xaxis: {
    categories: dashboard.staffLoads.map((staff) => staff.staffName),
    labels: {
      trim: true,
      rotate: dashboard.staffLoads.length > 6 ? -25 : 0,
      hideOverlappingLabels: true,
      formatter: (value) => (value?.length > 16 ? `${value.slice(0, 15)}…` : value),
    },
  },
  yaxis: {
    min: 0,
    max: 100,
    tickAmount: 5,
    title: { text: 'Tỷ lệ (%)' },
    labels: { formatter: (value) => `${Math.round(value)}%` },
  },
  tooltip: {
    shared: true,
    intersect: false,
    y: { formatter: (value) => `${Math.round(value)}%` },
  },
  legend: { position: 'top', horizontalAlign: 'center', markers: { size: 6 } },
  noData: { text: 'Chưa có dữ liệu KPI' },
}))
let timer
let messageTimer

async function loadDashboard() {
  loading.value = true
  try {
    const { data } = await OrderAssignmentApi.getDashboard({
      pendingPage: pendingPage.value - 1,
      pendingSize: PAGE_SIZE,
    })
    dashboard.staffLoads = data.staffLoads || []
    dashboard.pendingOrders = data.pendingOrders || []
    dashboard.pendingTotalElements = Number(data.pendingTotalElements) || 0
    dashboard.pendingTotalPages = Number(data.pendingTotalPages) || 0
    const lastPendingPage = dashboard.pendingTotalPages || 1
    if (pendingPage.value > lastPendingPage) {
      pendingPage.value = lastPendingPage
      return await loadDashboard()
    }
    staffPage.value = Math.min(staffPage.value, pageCount(dashboard.staffLoads))
    kpiPage.value = Math.min(kpiPage.value, pageCount(dashboard.staffLoads))
  } catch {
    showMessage('Không tải được dữ liệu chia đơn. Vui lòng thử lại.', 'error')
  } finally {
    loading.value = false
  }
}

async function changePendingPage(page) {
  pendingPage.value = page
  await loadDashboard()
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
  CONFIRMED: 1,
  PROCESSING: 3,
  READY_FOR_PICKUP: 2,
  PACKED: 2,
  SHIPPING: 1,
  DELIVERED: 1,
  FAILED: 2,
}
function statusLabel(status) {
  return statusNames[status] || status
}
function loadLabel(active, max, accepting) {
  if (!accepting) return 'Đang tạm dừng'
  if (active >= max) return 'Đã đầy tải — Không chia thêm'
  if (active >= Math.max(1, max - 1)) return `Tải vừa — Còn ${max - active} suất`
  return `Tải thấp — Còn ${max - active} suất`
}
function integerScore(value) {
  return Math.round(Number(value) || 0)
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

<style scoped src="@/assets/css/OrderAssignment.css"></style>
