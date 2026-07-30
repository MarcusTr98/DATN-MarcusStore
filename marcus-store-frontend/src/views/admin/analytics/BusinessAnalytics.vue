<template>
  <main class="business-analytics">
    <AdminPageHeader
      eyebrow="Báo cáo quản trị"
      eyebrow-icon="bi bi-bar-chart-line"
      title="Phân tích kinh doanh"
      description="Theo dõi tăng trưởng từ giao dịch đã thu tiền của đơn hoàn tất và xu hướng sản phẩm."
      icon="bi bi-graph-up-arrow"
    >
      <template #actions>
        <button type="button" :disabled="loading" @click="loadAnalytics">
          <i class="bi bi-arrow-clockwise" :class="{ 'spin-icon': loading }"></i>
          Làm mới
        </button>
      </template>
    </AdminPageHeader>

    <AnalyticsFilterBar
      v-model:from-date="fromDate"
      v-model:to-date="toDate"
      :active-preset="activePreset"
      :loading="loading"
      :presets="presets"
      :today="today"
      @apply-custom="applyCustomRange"
      @select-preset="applyPreset"
    />

    <div v-if="errorMessage" class="analytics-alert" role="alert">
      <i class="bi bi-exclamation-triangle"></i>
      <div>
        <strong>Chưa thể tải báo cáo</strong>
        <p>{{ errorMessage }}</p>
      </div>
      <button type="button" @click="loadAnalytics">Thử lại</button>
    </div>

    <template v-if="overview">
      <div class="analytics-period-note">
        <span
          ><i class="bi bi-calendar3"></i> Kỳ đang xem: <strong>{{ periodLabel }}</strong></span
        >
        <span>{{ numberOfDays.toLocaleString('vi-VN') }} ngày</span>
      </div>

      <AiBusinessBriefing
        :error="aiError"
        :loading="aiLoading"
        :report="aiReport"
        :usage="aiUsage"
        @generate="generateAiReport"
      />

      <AnalyticsKpiGrid :overview="overview" />

      <AnalyticsInsightPanel v-if="analysis" :analysis="analysis" :forecast="forecast" />

      <AnalyticsSalesChart :monthly="numberOfDays > 120" :trend="trend" />

      <AnalyticsForecastChart v-if="forecast" :forecast="forecast" />

      <AnalyticsProductTable :products="products" />

      <aside class="analytics-scope-note">
        <i class="bi bi-info-circle"></i>
        <p>
          <strong>Phạm vi số liệu:</strong>
          doanh thu chỉ tính giao dịch thu tiền <code>SUCCESS</code>, không phải
          <code>REFUND</code>, của đơn <code>COMPLETED</code> và lọc theo ngày giao dịch. Doanh số
          sản phẩm lấy giá tại lúc mua; báo cáo không gọi là lợi nhuận vì hệ thống chưa lưu giá
          nhập.
        </p>
      </aside>
    </template>

    <div v-if="loading" class="analytics-loading" aria-live="polite">
      <span class="analytics-loading__spinner"></span>
      <div>
        <strong>Đang tổng hợp dữ liệu</strong>
        <p>Marcus đang đối chiếu doanh thu, đơn hàng và sản phẩm…</p>
      </div>
    </div>
  </main>
</template>

<script setup>
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AiBusinessBriefing from '@/components/analytics/AiBusinessBriefing.vue'
import AnalyticsFilterBar from '@/components/analytics/AnalyticsFilterBar.vue'
import AnalyticsForecastChart from '@/components/analytics/AnalyticsForecastChart.vue'
import AnalyticsInsightPanel from '@/components/analytics/AnalyticsInsightPanel.vue'
import AnalyticsKpiGrid from '@/components/analytics/AnalyticsKpiGrid.vue'
import AnalyticsProductTable from '@/components/analytics/AnalyticsProductTable.vue'
import AnalyticsSalesChart from '@/components/analytics/AnalyticsSalesChart.vue'
import { useBusinessAnalytics } from '@/composables/useBusinessAnalytics'

const {
  activePreset,
  analysis,
  aiError,
  aiLoading,
  aiReport,
  aiUsage,
  errorMessage,
  fromDate,
  forecast,
  loading,
  numberOfDays,
  overview,
  periodLabel,
  presets,
  products,
  toDate,
  today,
  trend,
  applyCustomRange,
  applyPreset,
  generateAiReport,
  loadAnalytics,
} = useBusinessAnalytics()
</script>

<style src="@/assets/css/BusinessAnalytics.css"></style>
