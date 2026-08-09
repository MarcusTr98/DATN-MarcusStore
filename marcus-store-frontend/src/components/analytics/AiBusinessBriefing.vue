<template>
  <section class="ai-briefing analysis-source-host" :class="{ 'ai-briefing--ready': report }">
    <!-- Marcus thêm: nhãn nguồn giúp người dùng không nhầm nội dung Gemini với thuật toán. -->
    <AnalysisSourceBadge :source="report?.source === 'ALGORITHM' ? 'algorithm' : 'ai'" />
    <div class="ai-briefing__hero">
      <div class="ai-briefing__identity">
        <span class="ai-briefing__robot"><i class="bi bi-robot"></i></span>
        <div>
          <div class="ai-briefing__eyebrow">
            <span></span>
            Marcus AI Business Analyst
            <em>Gemini</em>
          </div>
          <h2 v-if="!report">Biến số liệu thành quyết định kinh doanh</h2>
          <h2 v-else>{{ report.headline }}</h2>
          <p v-if="!report">
            AI đọc dữ liệu tổng hợp trong kỳ, tìm tín hiệu bất thường và đề xuất việc nên ưu tiên.
            Chỉ chạy khi bạn yêu cầu để tiết kiệm quota miễn phí.
          </p>
          <p v-else>{{ report.executiveSummary }}</p>
        </div>
      </div>

      <div class="ai-briefing__action">
        <button type="button" :disabled="loading" @click="$emit('generate')">
          <i v-if="loading" class="bi bi-stars ai-spark-loading"></i>
          <i v-else class="bi bi-stars"></i>
          {{ loading ? 'AI đang phân tích…' : report ? 'Phân tích lại' : 'Nhờ AI phân tích' }}
        </button>
        <small v-if="!report">Kết quả sẽ được lưu để xem lại miễn phí</small>
        <small v-else>
          <i :class="report.cached ? 'bi bi-database-check' : 'bi bi-clock'"></i>
          {{
            report.cached
              ? `Báo cáo đã lưu · ${formatTime(report.generatedAt)}`
              : `Vừa tạo lúc ${formatTime(report.generatedAt)}`
          }}
        </small>
      </div>
    </div>

    <div v-if="error" class="ai-briefing__error">
      <i class="bi bi-exclamation-circle"></i>
      <span>{{ error }}</span>
    </div>

    <template v-if="report">
      <div class="ai-briefing__meta">
        <span :class="`outlook-${report.outlook.toLowerCase()}`">
          <i :class="outlookIcon(report.outlook)"></i>
          Triển vọng: {{ outlookLabel(report.outlook) }}
        </span>
        <span>
          <i class="bi bi-shield-check"></i>
          Độ tin cậy: {{ confidenceLabel(report.confidence) }}
        </span>
        <span>
          <i class="bi bi-database-check"></i>
          Chỉ dùng dữ liệu tổng hợp
        </span>
      </div>

      <div v-if="report.productOutlooks.length" class="ai-product-outlooks">
        <div class="ai-product-outlooks__title">
          <i class="bi bi-stars"></i>
          <div>
            <strong>Dự báo xu hướng sản phẩm từ AI</strong>
            <small>AI đối chiếu biến động bán hàng giữa kỳ hiện tại và kỳ liền trước</small>
          </div>
        </div>
        <article v-for="product in report.productOutlooks" :key="product.productId">
          <span :class="`direction-${product.direction.toLowerCase()}`">
            <i :class="directionIcon(product.direction)"></i>
          </span>
          <div>
            <strong>{{ product.productName }}</strong>
            <p>{{ product.reason }}</p>
          </div>
          <em>{{ directionLabel(product.direction) }}</em>
        </article>
      </div>

      <div class="ai-briefing__content">
        <div class="ai-briefing__column">
          <h3><i class="bi bi-radar"></i> Tín hiệu AI phát hiện</h3>
          <article
            v-for="signal in report.signals"
            :key="`${signal.title}-${signal.evidence}`"
            class="ai-signal"
            :class="`ai-signal--${signal.severity.toLowerCase()}`"
          >
            <span><i :class="signalIcon(signal.severity)"></i></span>
            <div>
              <strong>{{ signal.title }}</strong>
              <small>{{ signal.evidence }}</small>
              <p>{{ signal.interpretation }}</p>
              <p v-if="signal.action"><strong>Hành động:</strong> {{ signal.action }}</p>
              <small v-if="signal.verification"
                ><strong>Kiểm chứng:</strong> {{ signal.verification }}</small
              >
            </div>
          </article>
        </div>

        <div class="ai-briefing__column">
          <h3><i class="bi bi-list-check"></i> AI đề xuất ưu tiên</h3>
          <article
            v-for="(action, index) in report.actions"
            :key="`${action.title}-${index}`"
            class="ai-action"
          >
            <span>{{ index + 1 }}</span>
            <div>
              <strong>{{ action.title }}</strong>
              <p>{{ action.reason }}</p>
            </div>
            <em :class="`priority-${action.priority.toLowerCase()}`">
              {{ priorityLabel(action.priority) }}
            </em>
          </article>
        </div>
      </div>

      <footer class="ai-briefing__footer">
        <i class="bi bi-shield-lock"></i>
        {{ report.disclaimer }}
      </footer>
    </template>

    <div v-if="usage" class="ai-briefing__usage">
      <article>
        <span><i class="bi bi-chat-dots"></i></span>
        <div>
          <strong>{{ formatNumber(usage.successfulChats) }}</strong
          ><small title="Phiên có ít nhất một đánh giá Hữu ích hoặc click từ AI sang sản phẩm"
            >Lượt tư vấn thành công</small
          >
        </div>
      </article>
      <article>
        <span><i class="bi bi-people"></i></span>
        <div>
          <strong>{{ formatNumber(usage.uniqueSessions) }}</strong
          ><small>Phiên AI ẩn danh</small>
        </div>
      </article>
      <article>
        <span><i class="bi bi-cursor"></i></span>
        <div>
          <strong>{{ formatNumber(usage.productClicks) }}</strong
          ><small>Click sang sản phẩm</small>
        </div>
      </article>
      <article>
        <span><i class="bi bi-bullseye"></i></span>
        <div>
          <strong>{{ formatPercent(usage.clickThroughRate) }}</strong
          ><small>Tỷ lệ click từ AI</small>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import AnalysisSourceBadge from '@/components/analytics/AnalysisSourceBadge.vue'

defineProps({
  error: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  report: { type: Object, default: null },
  usage: { type: Object, default: null },
})

defineEmits(['generate'])

const labels = {
  outlook: {
    GROWTH: 'Tăng trưởng',
    STEADY: 'Đi ngang',
    DECLINE: 'Suy giảm',
    UNCERTAIN: 'Chưa chắc chắn',
  },
  confidence: { HIGH: 'Cao', MEDIUM: 'Trung bình', LOW: 'Thấp' },
  priority: { HIGH: 'Ưu tiên cao', MEDIUM: 'Nên làm', LOW: 'Theo dõi' },
  direction: {
    UP: 'Có thể tăng',
    STEADY: 'Đi ngang',
    DOWN: 'Có thể giảm',
    UNCERTAIN: 'Chưa đủ dữ liệu',
  },
}

function outlookLabel(value) {
  return labels.outlook[value] || labels.outlook.UNCERTAIN
}

function confidenceLabel(value) {
  return labels.confidence[value] || labels.confidence.LOW
}

function priorityLabel(value) {
  return labels.priority[value] || labels.priority.MEDIUM
}

function directionLabel(value) {
  return labels.direction[value] || labels.direction.UNCERTAIN
}

function outlookIcon(value) {
  if (value === 'GROWTH') return 'bi bi-graph-up-arrow'
  if (value === 'DECLINE') return 'bi bi-graph-down-arrow'
  return 'bi bi-activity'
}

function signalIcon(value) {
  if (value === 'POSITIVE') return 'bi bi-arrow-up-right'
  if (value === 'WARNING' || value === 'CRITICAL') return 'bi bi-exclamation-triangle'
  return 'bi bi-lightbulb'
}

function directionIcon(value) {
  if (value === 'UP') return 'bi bi-arrow-up-right'
  if (value === 'DOWN') return 'bi bi-arrow-down-right'
  if (value === 'STEADY') return 'bi bi-arrow-right'
  return 'bi bi-question'
}

function formatTime(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
  }).format(new Date(value))
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('vi-VN')
}

function formatPercent(value) {
  return `${Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%`
}
</script>

<style scoped>
.ai-briefing__usage {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 0 22px 18px;
}

.ai-briefing__usage article {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  border: 1px solid rgba(139, 92, 246, 0.18);
  border-radius: 14px;
  padding: 11px 12px;
  background: rgba(255, 255, 255, 0.72);
}

.ai-briefing__usage article > span {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 11px;
  background: #ede9fe;
  color: #6d28d9;
}

.ai-briefing__usage strong,
.ai-briefing__usage small {
  display: block;
}

.ai-briefing__usage strong {
  color: #312e81;
  font-size: 16px;
}

.ai-briefing__usage small {
  margin-top: 2px;
  color: #64748b;
  font-size: 10px;
}

@media (max-width: 900px) {
  .ai-briefing__usage {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
