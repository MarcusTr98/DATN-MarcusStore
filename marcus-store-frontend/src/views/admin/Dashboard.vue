<template>
  <section class="dashboard-page">
    <div class="dashboard-shell">

      <DashboardHeader
        v-model:selectedTime="selectedTime"
        v-model:customDate="customDate"
      />

      <KpiGrid
        :kpiSummary="kpiSummary"
        :pendingOrdersCount="pendingOrdersCount"
        :lowStockData="lowStockData"
        :periodLabel="periodNoteLabel"
        @action="handleKpiAction"
      />

      <ChartsSection
        :selectedTime="selectedTime"
        :compareData="compareData"
        :weekdayStats="weekdayStats"
        :brandStats="brandStats"
        :newUsersData="newUsersData"
        :orderStats="orderStats"
      />

      <DataSection
        ref="dataSectionRef"
        :selectedTime="selectedTime"
        :customDate="customDate"
        :childCategories="childCategories"
        :brandList="brandNameList"
      />

    </div>
  </section>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import statisticsApi from '@/api/statisticsApi'
import DashboardHeader from '../../layouts/Dashboard/Dashboardheader.vue'
import KpiGrid         from '../../layouts/Dashboard/Kpigrid.vue'
import ChartsSection   from '../../layouts/Dashboard/Chartssection.vue'
import DataSection     from '../../layouts/Dashboard/Datasection.vue'

const selectedTime = ref('month')
const customDate   = ref('')

const kpiSummary         = ref({ totalRevenue: 0, totalOrders: 0, totalProductsSold: 0 })
const pendingOrdersCount = ref(0)
const weekdayStats       = ref([])
const brandStats         = ref([])
const compareData        = ref({ current: [], previous: [], currentLabel: '', previousLabel: '' })
const newUsersData       = ref([])
const lowStockData       = ref([])
const orderStats         = ref([])
const childCategories    = ref([])
const dataSectionRef     = ref(null)

const periodNoteLabel = computed(() => {
  if (customDate.value) {
    const [y, m, d] = customDate.value.split('-')
    return `ngày ${d}/${m}/${y}`
  }
  switch (selectedTime.value) {
    case 'today':     return 'hôm nay'
    case 'yesterday': return 'hôm qua'
    case '7days':     return '7 ngày qua'
    case '30days':    return '30 ngày qua'
    case 'week':      return 'tuần này'
    case 'year':      return 'năm nay'
    default:          return 'tháng này'
  }
})

const brandNameList = computed(() =>
  brandStats.value.map(b => b.brand).filter(Boolean)
)

async function fetchDashboardData(period = 'month', startDate = '', endDate = '') {
  try {
    const [
      kpiRes, weekdayRes, brandRes, lowStockRes,
      compareRes, pendingRes, newUserRes, orderStatsRes,
    ] = await Promise.all([
      statisticsApi.getKpiSummary(period, startDate, endDate),
      statisticsApi.getOrdersByWeekday(period, startDate, endDate),
      statisticsApi.getRevenueByBrand(period, startDate, endDate),
      statisticsApi.getLowStockProducts(),
      statisticsApi.getRevenueCompare(period, startDate, endDate),
      statisticsApi.getPendingOrdersCount(),
      statisticsApi.getNewUsers(period, startDate, endDate),
      statisticsApi.getRevenueByDay(period, startDate, endDate),
    ])

    kpiSummary.value         = kpiRes.data.data
    weekdayStats.value       = weekdayRes.data.data
    brandStats.value         = brandRes.data.data
    lowStockData.value       = lowStockRes.data.data
    compareData.value        = compareRes.data.data
    pendingOrdersCount.value = pendingRes.data.data
    newUsersData.value       = newUserRes.data.data
    orderStats.value         = orderStatsRes.data.data
  } catch (err) {
    console.error('Không thể tải dữ liệu thống kê:', err)
  }
}

watch(
  [selectedTime, customDate],
  ([time, custom]) => {
    if (custom) {
      // User đang chọn ngày cụ thể → luôn query 1 ngày đó
      fetchDashboardData('today', custom, custom)
    } else {
      // User chọn preset hoặc vừa xóa custom date
      fetchDashboardData(time)
    }
  },
)

function handleKpiAction(action) {
  if (action === 'lowStock') {
    dataSectionRef.value?.switchToLowStock()
    const el = document.querySelector('.data-card')
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

onMounted(async () => {
  fetchDashboardData(selectedTime.value)
  try {
    const catRes = await statisticsApi.getChildCategories()
    childCategories.value = catRes.data.data
  } catch {
    childCategories.value = []
  }
})
</script>

<style>
:root {
  --primary: #2563eb;
  --primary-light: #eff6ff;
  --primary-border: #bfdbfe;
  --success: #16a34a;
  --warning: #f59e0b;
  --danger: #dc2626;
  --text-main: #111827;
  --text-sub: #6b7280;
}
</style>

<style scoped>
.dashboard-page {
  min-height: calc(100vh - 74px);
  background: #fff7fb;
  color: #1f2937;
  padding: 28px;
}
.dashboard-shell {
  max-width: 1800px;
  margin: 0 auto;
  display: grid;
  gap: 24px;
}
@media (max-width: 992px) {
  .dashboard-page { padding: 18px; }
}
</style>