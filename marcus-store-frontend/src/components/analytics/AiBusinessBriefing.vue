<template>
  <section class="ai-briefing analysis-source-host" :class="{ 'ai-briefing--ready': report }">
    <!-- Marcus thêm: nhãn nguồn giúp người dùng không nhầm nội dung Gemini với thuật toán. -->
    <AnalysisSourceBadge
      :source="report?.source === 'ALGORITHM' ? 'algorithm' : 'ai'"
      :detail="
        report?.source === 'ALGORITHM'
          ? 'Fallback so sánh Δ doanh thu, số đơn và sản lượng giữa hai kỳ; không gọi Gemini.'
          : 'Gemini tổng hợp bằng chứng, diễn giải rủi ro và đề xuất hành động. Product ID và số liệu đều được backend kiểm chứng trước khi hiển thị.'
      "
    />
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
          <p v-else>{{ conciseText(report.executiveSummary, 260) }}</p>
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

      <!-- Marcus thêm: biến báo cáo AI thành một quyết định đọc được trong 10 giây,
      thay vì bắt Admin tự ghép nhiều card giống Dashboard. -->
      <section class="ai-decision-board">
        <header>
          <span><i class="bi bi-stars"></i> Định hướng kỳ tiếp theo từ AI</span>
          <strong>3 ý quan trọng Admin cần nắm</strong>
          <p>Đọc nhanh xu hướng, căn cứ và việc ưu tiên trước khi xem phân tích chi tiết.</p>
        </header>
        <div class="ai-decision-board__grid">
          <article class="is-verdict">
            <small><i class="bi bi-graph-down-arrow"></i> Xu hướng cần chú ý</small>
            <strong>{{ report.headline }}</strong>
            <p>
              <template v-for="(part, index) in emphasizedParts(conciseText(report.executiveSummary, 210))" :key="index">
                <strong v-if="part.emphasis" class="ai-metric">{{ part.text }}</strong><template v-else>{{ part.text }}</template>
              </template>
            </p>
          </article>
          <article>
            <small><i class="bi bi-bar-chart-line"></i> Căn cứ chính</small>
            <strong>{{ report.signals?.[0]?.title || 'Chưa đủ tín hiệu nổi bật' }}</strong>
            <p>
              <template v-for="(part, index) in emphasizedParts(conciseText(report.signals?.[0]?.evidence || 'Cần thêm dữ liệu ở kỳ tiếp theo.', 160))" :key="index">
                <strong v-if="part.emphasis" class="ai-metric">{{ part.text }}</strong><template v-else>{{ part.text }}</template>
              </template>
            </p>
          </article>
          <article class="is-action">
            <small><i class="bi bi-lightning-charge-fill"></i> Hành động ưu tiên</small>
            <strong>{{ report.actions?.[0]?.title || 'Tiếp tục theo dõi' }}</strong>
            <p>
              <template v-for="(part, index) in emphasizedParts(conciseText(report.actions?.[0]?.reason || 'Kiểm chứng lại số liệu trong kỳ tiếp theo.', 160))" :key="index">
                <strong v-if="part.emphasis" class="ai-metric">{{ part.text }}</strong><template v-else>{{ part.text }}</template>
              </template>
            </p>
          </article>
        </div>
      </section>

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
            :key="signal.evidenceId || `${signal.title}-${signal.evidence}`"
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

// Marcus thêm: bảo vệ thị giác nếu báo cáo cache/provider trả câu dài hơn hợp đồng mới.
function conciseText(value, maxLength = 200) {
  const text = String(value || '')
    .replace(/\s+/g, ' ')
    .trim()
  if (text.length <= maxLength) return text
  const firstSentence = text.match(/^.{1,260}?[.!?](?:\s|$)/)?.[0]?.trim()
  if (firstSentence && firstSentence.length >= Math.min(90, maxLength * 0.55)) return firstSentence
  const shortened = text.slice(0, maxLength)
  const boundary = shortened.lastIndexOf(' ')
  return `${(boundary > maxLength * 0.7 ? shortened.slice(0, boundary) : shortened).trim()}…`
}

// Marcus thêm: nhấn số liệu an toàn bằng text node, không render HTML do AI sinh ra.
function emphasizedParts(value) {
  const metricPattern = /(\d+(?:[.,]\d+)*(?:\s*(?:%|VNĐ|tỷ đồng|triệu đồng))?)/gi
  return String(value || '').split(metricPattern).filter(Boolean).map((text) => ({
    text,
    emphasis: /\d/.test(text),
  }))
}
</script>

<style scoped>
.ai-briefing__usage {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 0 22px 18px;
}

.ai-decision-board {
  margin: 18px 22px;
  padding: 18px;
  border: 1px solid rgba(109, 40, 217, 0.22);
  border-radius: 18px;
  background: linear-gradient(135deg, #f7f2ff 0%, #fff 58%, #eef7ff 100%);
}
.ai-decision-board > header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 14px;
}
.ai-decision-board > header span {
  color: #6d28d9;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.ai-decision-board > header strong {
  color: #172554;
  font-size: 20px;
}
.ai-decision-board > header p {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}
.ai-decision-board__grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr 1fr;
  gap: 12px;
}
.ai-decision-board__grid article {
  position: relative;
  padding: 16px;
  border: 1.5px solid #bfdbfe;
  border-radius: 16px;
  background: linear-gradient(145deg, #f8fbff, #eff6ff);
  box-shadow: 0 8px 22px rgba(37, 99, 235, 0.06);
}
.ai-decision-board__grid article.is-verdict {
  border-color: #d8b4fe;
  background: linear-gradient(145deg, #fdfaff, #f3e8ff);
  box-shadow: 0 8px 22px rgba(124, 58, 237, 0.07);
}
.ai-decision-board__grid article.is-action {
  border-color: #a7f3d0;
  background: linear-gradient(145deg, #f7fffb, #e8fff5);
  box-shadow: 0 8px 22px rgba(5, 150, 105, 0.07);
}
.ai-decision-board__grid small,
.ai-decision-board__grid > article > strong {
  display: block;
}
.ai-decision-board__grid small {
  margin-bottom: 7px;
  color: #52627a;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.025em;
  text-transform: uppercase;
}
.ai-decision-board__grid small i {
  margin-right: 5px;
  color: #2563eb;
}
.ai-decision-board__grid .is-verdict small i {
  color: #7c3aed;
}
.ai-decision-board__grid .is-action small i {
  color: #059669;
}
.ai-decision-board__grid > article > strong {
  color: #1e1b4b;
  font-size: 14px;
  line-height: 1.4;
}
.ai-decision-board__grid .is-action > strong {
  color: #065f46;
}
.ai-decision-board__grid p {
  margin: 7px 0 0;
  color: #52627a;
  font-size: 12px;
  line-height: 1.5;
}
.ai-decision-board__grid .ai-metric {
  display: inline;
  color: #172554;
  font-weight: 900;
}
.ai-decision-board__grid .is-action .ai-metric {
  color: #047857;
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
  .ai-decision-board__grid {
    grid-template-columns: 1fr;
  }
  .ai-briefing__usage {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
