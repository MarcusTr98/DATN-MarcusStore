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

      <AnalyticsSectionNav />

      <div id="ai-conclusion" class="analytics-anchor-section">
        <AiBusinessBriefing
          :error="aiError"
          :loading="aiLoading"
          :report="aiReport"
          :usage="aiUsage"
          :tracked-actions="analyticsActions"
          @generate="generateAiReport"
          @drill-down="drillDownEvidence"
          @accept-action="acceptAnalyticsAction"
          @update-action="updateAnalyticsAction"
        />
      </div>

      <AnalyticsInsightPanel v-if="analysis" :analysis="analysis" :forecast="forecast" />

      <div id="forecast" class="analytics-anchor-section">
        <AnalyticsForecastChart v-if="forecast" :forecast="forecast" />
      </div>

      <div
        id="ai-effectiveness"
        class="analytics-history-divider analytics-history-divider--ai analytics-anchor-section"
      >
        <div>
          <span>Hiệu quả hỗ trợ bán hàng</span>
          <h2>Khách tiếp tục làm gì sau khi hỏi Marcus AI?</h2>
          <p>Theo dõi hành trình ẩn danh từ lúc nhận tư vấn đến khi tạo đơn và thanh toán.</p>
        </div>
        <i class="bi bi-stars"></i>
      </div>

      <AnalyticsAiSalesFunnel :data="aiSalesFunnel" />

      <div id="business-evidence" class="analytics-history-divider analytics-anchor-section">
        <div>
          <span>Cơ sở kiểm chứng</span>
          <h2>Dữ liệu kinh doanh làm căn cứ phân tích</h2>
          <p>Doanh thu, đơn hàng và sản phẩm giúp kiểm tra lại các dự báo và đề xuất phía trên.</p>
        </div>
        <i class="bi bi-database-check"></i>
      </div>

      <div id="evidence-kpi"><AnalyticsKpiGrid :overview="overview" /></div>

      <div id="evidence-sales">
        <AnalyticsSalesChart :monthly="numberOfDays > 120" :trend="trend" />
      </div>

      <div id="evidence-products"><AnalyticsProductTable :products="products" /></div>

      <div id="product-quality" class="analytics-anchor-section">
        <AnalyticsCancellationReasons :reasons="cancellationReasons" />
        <AnalyticsWarrantyQuality :data="warrantyQuality" />
        <AnalyticsBehaviorFunnel :data="behaviorFunnel" />
      </div>

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
import AnalyticsCancellationReasons from '@/components/analytics/AnalyticsCancellationReasons.vue'
import AnalyticsSalesChart from '@/components/analytics/AnalyticsSalesChart.vue'
import AnalyticsWarrantyQuality from '@/components/analytics/AnalyticsWarrantyQuality.vue'
import AnalyticsBehaviorFunnel from '@/components/analytics/AnalyticsBehaviorFunnel.vue'
import AnalyticsAiSalesFunnel from '@/components/analytics/AnalyticsAiSalesFunnel.vue'
import AnalyticsSectionNav from '@/components/analytics/AnalyticsSectionNav.vue'
import { useBusinessAnalytics } from '@/composables/useBusinessAnalytics'

const {
  activePreset,
  analysis,
  aiError,
  aiLoading,
  aiReport,
  aiUsage,
  aiSalesFunnel,
  analyticsActions,
  cancellationReasons,
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
  warrantyQuality,
  behaviorFunnel,
  applyCustomRange,
  applyPreset,
  generateAiReport,
  loadAnalytics,
  acceptAnalyticsAction,
  updateAnalyticsAction,
} = useBusinessAnalytics()

const drillDownEvidence = (signal) => {
  const content =
    `${signal?.title || ''} ${signal?.evidence || ''} ${signal?.interpretation || ''}`.toLowerCase()
  const target = content.match(/hủy|bảo hành|lỗi|đổi trả/)
    ? 'product-quality'
    : content.match(/sản phẩm|mẫu|sku/)
      ? 'evidence-products'
      : content.match(/doanh thu|đơn hàng|tăng trưởng|giao dịch/)
        ? 'evidence-sales'
        : 'evidence-kpi'
  document.getElementById(target)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style src="@/assets/css/BusinessAnalytics.css"></style>
