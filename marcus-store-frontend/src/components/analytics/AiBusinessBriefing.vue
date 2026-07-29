<template>
  <section class="ai-briefing" :class="{ 'ai-briefing--ready': report }">
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
          {{ report.cached ? `Báo cáo đã lưu · ${formatTime(report.generatedAt)}` : `Vừa tạo lúc ${formatTime(report.generatedAt)}` }}
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

      <div v-if="report.productOutlooks.length" class="ai-product-outlooks">
        <div class="ai-product-outlooks__title">
          <i class="bi bi-boxes"></i>
          <div>
            <strong>AI dự đoán hướng sản phẩm</strong>
            <small>Dựa trên biến động bán hàng giữa hai kỳ</small>
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

      <footer class="ai-briefing__footer">
        <i class="bi bi-shield-lock"></i>
        {{ report.disclaimer }}
      </footer>
    </template>
  </section>
</template>

<script setup>
defineProps({
  error: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  report: { type: Object, default: null },
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
  direction: { UP: 'Có thể tăng', STEADY: 'Đi ngang', DOWN: 'Có thể giảm', UNCERTAIN: 'Chưa đủ dữ liệu' },
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
</script>
