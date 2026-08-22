<template>
  <div class="ai-advisor">
    <button
      type="button"
      class="ai-trigger"
      :class="{ 'is-hidden': isOpen }"
      aria-label="Mở Marcus AI"
      @click="openAdvisor"
    >
      <span class="ai-spark"><i class="fas fa-sparkles"></i></span>
      <i class="fas fa-robot"></i>
      <span class="ai-badge">AI</span>
    </button>

    <Transition name="ai-window">
      <section v-show="isOpen" class="ai-panel" aria-label="Marcus AI tư vấn bán hàng">
        <header class="ai-header">
          <div class="ai-identity">
            <div class="ai-avatar"><i class="fas fa-robot"></i></div>
            <div>
              <h6>Marcus AI</h6>
              <span><i class="fas fa-circle"></i> Tư vấn tự động 24/7</span>
            </div>
          </div>
          <button type="button" class="ai-close" aria-label="Đóng" @click="isOpen = false">
            <i class="fas fa-times"></i>
          </button>
        </header>

        <div ref="messageBody" class="ai-body">
          <div class="ai-intro">
            <strong>Xin chào, mình là Marcus AI!</strong>
            <p>Mình có thể tìm và so sánh sản phẩm đang có trên Marcus Store.</p>
          </div>

          <div class="ai-suggestions">
            <span>Gợi ý nhanh</span>
            <div class="ai-suggestion-list">
              <button
                v-for="suggestion in suggestions"
                :key="suggestion"
                type="button"
                @click="selectSuggestion(suggestion)"
              >
                {{ suggestion }}
              </button>
            </div>
          </div>

          <div
            v-if="contextChips.length"
            class="ai-context"
            aria-label="Điều kiện tư vấn đang áp dụng"
          >
            <span>AI đang hiểu</span>
            <button
              v-for="chip in contextChips"
              :key="chip.key"
              type="button"
              :title="`Bỏ điều kiện ${chip.label}`"
              @click="removeContextChip(chip)"
            >
              {{ chip.label }} <i class="fas fa-times"></i>
            </button>
          </div>

          <article
            v-for="message in messages"
            :key="message.id"
            class="ai-message-row"
            :class="`is-${message.role}`"
          >
            <div class="ai-message">
              <span v-if="message.role === 'assistant'" class="message-author">Marcus AI</span>
              <!-- Marcus thêm: render Markdown giới hạn bằng Vue, không dùng v-html để tránh XSS. -->
              <div v-if="message.role === 'assistant' && message.sections" class="message-sections">
                <p><strong>Nhu cầu:</strong> {{ message.sections.needSummary }}</p>
                <template v-if="message.sections.suggestions?.length">
                  <strong>Gợi ý:</strong>
                  <ul>
                    <li v-for="item in message.sections.suggestions" :key="item">{{ item }}</li>
                  </ul>
                </template>
                <template v-if="message.sections.considerations?.length">
                  <strong>Điểm cần cân nhắc:</strong>
                  <ul>
                    <li v-for="item in message.sections.considerations" :key="item">{{ item }}</li>
                  </ul>
                </template>
                <p>
                  <strong>Nên chọn:</strong> {{ bestProductName(message)
                  }}{{ message.sections.bestReason }}
                </p>
                <p class="follow-up">
                  <em>Hỏi thêm: {{ message.sections.followUpQuestion }}</em>
                </p>
              </div>
              <div v-else-if="message.role === 'assistant'" class="message-content">
                <p
                  v-for="(line, lineIndex) in formatAssistantMessage(message.content)"
                  :key="`${message.id}-${lineIndex}`"
                  :class="{ 'is-bullet': line.bullet }"
                >
                  <span v-if="line.bullet" aria-hidden="true" class="message-bullet">•</span>
                  <template v-for="(token, tokenIndex) in line.tokens" :key="tokenIndex">
                    <strong v-if="token.type === 'bold'">{{ token.text }}</strong>
                    <em v-else-if="token.type === 'italic'">{{ token.text }}</em>
                    <span v-else>{{ token.text }}</span>
                  </template>
                </p>
              </div>
              <p v-else>{{ message.content }}</p>

              <div v-if="message.products?.length" class="product-suggestions">
                <router-link
                  v-for="product in message.products"
                  :key="product.productId"
                  :to="`/product/${product.slug}`"
                  class="ai-product"
                  @click="handleProductClick(product, message)"
                >
                  <img
                    :src="product.thumbnailUrl || '/images/product-placeholder.png'"
                    :alt="product.productName"
                  />
                  <div>
                    <strong>{{ product.productName }}</strong>
                    <!-- Marcus sửa: hiển thị khoảng giá SKU còn hàng, tránh hiểu
                    nhầm giá thấp nhất là giá của mọi phiên bản. -->
                    <span class="product-price">{{ formatProductPrice(product) }}</span>
                    <span v-if="product.compatibilityScore != null" class="match-score">
                      Phù hợp {{ product.compatibilityScore }}%
                    </span>
                    <span
                      v-if="product.matchReasons?.length"
                      class="match-reason"
                      :title="product.matchReasons.join(' · ')"
                    >
                      {{ product.matchReasons.join(' · ') }}
                    </span>
                    <small :class="{ 'out-of-stock': !product.inStock }">
                      {{ product.inStock ? 'Còn hàng' : 'Tạm hết hàng' }}
                    </small>
                    <span v-if="product.skuOptions?.length" class="sku-summary">
                      {{ formatSkuSummary(product) }}
                    </span>
                  </div>
                  <i class="fas fa-chevron-right"></i>
                </router-link>
              </div>
              <div
                v-if="message.role === 'assistant' && message.adviceId && !message.isError"
                class="ai-feedback"
              >
                <span>Câu trả lời này có hữu ích?</span>
                <button
                  type="button"
                  :class="{ active: message.feedback === true }"
                  :disabled="message.feedback !== undefined"
                  @click="submitFeedback(message, true)"
                >
                  <i class="far fa-thumbs-up"></i> Hữu ích
                </button>
                <button
                  type="button"
                  :class="{ active: message.feedback === false }"
                  :disabled="message.feedback !== undefined"
                  @click="submitFeedback(message, false)"
                >
                  <i class="far fa-thumbs-down"></i> Chưa hữu ích
                </button>
              </div>
            </div>
          </article>

          <div v-if="isLoading && !isStreamingText" class="ai-message-row is-assistant">
            <div class="ai-message typing"><span></span><span></span><span></span></div>
          </div>
        </div>

        <div class="ai-note">
          Marcus AI có thể nhầm lẫn. Giá và tồn kho được đối chiếu tại thời điểm tư vấn.
        </div>

        <footer class="ai-footer">
          <textarea
            ref="messageInput"
            v-model="inputMessage"
            rows="1"
            maxlength="500"
            placeholder="Bạn đang tìm sản phẩm nào?"
            :disabled="isLoading"
            @keydown.enter.exact.prevent="sendMessage"
          ></textarea>
          <button
            type="button"
            :disabled="!inputMessage.trim() || isLoading"
            aria-label="Gửi câu hỏi"
            @click="sendMessage"
          >
            <i class="fas fa-paper-plane"></i>
          </button>
        </footer>
      </section>
    </Transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  getAiServerSession,
  sendAiAdvisorFeedback,
  streamAiAdvisor,
  trackAiProductClick,
} from '@/api/aiAdvisorApi'
import { getBehaviorSessionId } from '@/api/behaviorApi'
import {
  clearFloatingContactPanel,
  setFloatingContactPanelOpen,
} from '@/utils/floatingContactVisibility'

const isOpen = ref(false)
const isLoading = ref(false)
const isStreamingText = ref(false)
const inputMessage = ref('')
const messageBody = ref(null)
const messageInput = ref(null)
const messages = ref([])
const advisorContext = ref(null)
let messageId = 0
const AI_HISTORY_STORAGE_KEY = 'MARCUS_AI_CONVERSATION'
const AI_SERVER_SESSION_KEY = 'MARCUS_AI_SERVER_SESSION'
const AI_HISTORY_TTL_MS = 4 * 60 * 60 * 1000
const AI_HISTORY_LIMIT = 16

// Marcus thêm: giữ hội thoại trong đúng tab trình duyệt để khách mua xong vẫn
// có thể quay lại cảm ơn AI. Không gửi lịch sử này vào database/localStorage.
const restoreConversation = () => {
  try {
    const stored = JSON.parse(sessionStorage.getItem(AI_HISTORY_STORAGE_KEY) || 'null')
    if (!stored || Date.now() - Number(stored.savedAt) > AI_HISTORY_TTL_MS) {
      sessionStorage.removeItem(AI_HISTORY_STORAGE_KEY)
      return { messages: [], context: null }
    }
    return {
      messages: Array.isArray(stored.messages) ? stored.messages.slice(-AI_HISTORY_LIMIT) : [],
      context: stored.context || null,
    }
  } catch {
    sessionStorage.removeItem(AI_HISTORY_STORAGE_KEY)
    return { messages: [], context: null }
  }
}

const persistConversation = () => {
  try {
    const safeMessages = messages.value.slice(-AI_HISTORY_LIMIT).map((message) => ({
      id: message.id,
      role: message.role,
      content: String(message.content || '').slice(0, 1500),
      products: Array.isArray(message.products) ? message.products.slice(0, 3) : [],
      adviceId: message.adviceId,
      fallbackUsed: Boolean(message.fallbackUsed),
      isError: Boolean(message.isError),
      feedback: message.feedback,
      sections: message.sections,
    }))
    sessionStorage.setItem(
      AI_HISTORY_STORAGE_KEY,
      JSON.stringify({
        savedAt: Date.now(),
        messages: safeMessages,
        context: advisorContext.value,
      }),
    )
  } catch {
    // sessionStorage đầy/bị chặn không được làm gián đoạn thao tác chat.
  }
}

const clearConversation = () => {
  messages.value = []
  advisorContext.value = null
  messageId = 0
  sessionStorage.removeItem(AI_HISTORY_STORAGE_KEY)
  sessionStorage.removeItem('MARCUS_AI_TRACKING_SESSION')
}

const syncBackendSession = async () => {
  try {
    const response = await getAiServerSession()
    const serverSessionId = response?.data?.serverSessionId
    if (!serverSessionId) return
    const previousServerSessionId = sessionStorage.getItem(AI_SERVER_SESSION_KEY)
    if (!previousServerSessionId || previousServerSessionId !== serverSessionId) {
      clearConversation()
    }
    sessionStorage.setItem(AI_SERVER_SESSION_KEY, serverSessionId)
  } catch {
    // Backend tạm thời chưa lên không được làm mất lịch sử đang có.
  }
}

const getTrackingSessionId = () => {
  // Marcus sửa: AI, Product View, Checkout, Order và Payment phải dùng chung
  // anonymous journey ID để đo chuyển đổi thật.
  return getBehaviorSessionId()
}

const contextChips = computed(() => {
  const context = advisorContext.value
  if (!context) return []
  const chips = []
  if (context.category === 'ACCESSORY') chips.push({ key: 'category', label: 'Phụ kiện' })
  if (context.category === 'PHONE') chips.push({ key: 'category', label: 'Điện thoại' })
  if (context.platform === 'ANDROID') chips.push({ key: 'platform', label: 'Android' })
  if (context.platform === 'IOS') chips.push({ key: 'platform', label: 'iOS' })
  const brands = context.brands || []
  brands.forEach((brand) =>
    chips.push({ key: `brand-${brand}`, type: 'brand', value: brand, label: brandLabel(brand) }),
  )
  if (context.maxBudget != null) {
    chips.push({ key: 'maxBudget', label: `≤ ${formatCompactPrice(context.maxBudget)}` })
  }
  const priorityLabels = {
    CAMERA: 'Camera',
    PERFORMANCE: 'Hiệu năng',
    BATTERY: 'Pin/sạc',
    DISPLAY: 'Màn hình',
    STORAGE: 'Lưu trữ',
    DURABILITY: 'Độ bền',
    CONNECTIVITY: 'Kết nối',
    EASY_TO_USE: 'Dễ sử dụng',
    BRAND: 'Thương hiệu',
  }
  const priorities = context.priorities || []
  priorities.forEach((priority) =>
    chips.push({
      key: `priority-${priority}`,
      type: 'priority',
      value: priority,
      label: priorityLabels[priority] || priority,
    }),
  )
  return chips
})

const brandLabel = (brand) =>
  ({
    apple: 'Apple',
    samsung: 'Samsung',
    xiaomi: 'Xiaomi',
    oppo: 'OPPO',
    vivo: 'Vivo',
    realme: 'Realme',
    honor: 'HONOR',
    nokia: 'Nokia',
  })[brand] || brand

const formatCompactPrice = (value) => {
  const amount = Number(value || 0)
  return amount >= 1_000_000
    ? `${Number((amount / 1_000_000).toFixed(1))} triệu`
    : formatPrice(amount)
}

const removeContextChip = (chip) => {
  if (!advisorContext.value) return
  const next = { ...advisorContext.value }
  if (chip.type === 'brand')
    next.brands = (next.brands || []).filter((brand) => brand !== chip.value)
  else if (chip.type === 'priority')
    next.priorities = (next.priorities || []).filter((item) => item !== chip.value)
  else if (chip.key === 'category') next.category = null
  else if (chip.key === 'platform') next.platform = 'ANY'
  else next[chip.key] = null
  advisorContext.value = next
  persistConversation()
}

const bestProductName = (message) => {
  const product = message.products?.find(
    (item) => item.productId === message.sections?.bestProductId,
  )
  return product ? `${product.productName} — ` : ''
}

const handleProductClick = (product, message) => {
  // Marcus thêm: ghi nhớ thẻ khách vừa chọn trong phiên hiện tại. Những câu hỏi
  // tiếp nối không được tự coi sản phẩm “Nên chọn” cũ là lựa chọn của khách.
  advisorContext.value = {
    ...(advisorContext.value || {}),
    selectedProductIds: [product.productId],
    focusedProductId: product.productId,
  }
  persistConversation()
  isOpen.value = false
  // Marcus sửa: tracking không được cản trở thao tác mở sản phẩm nếu API thống kê
  // tạm thời lỗi.
  trackAiProductClick(product.productId, getTrackingSessionId(), message?.adviceId).catch(() => {})
}

// Marcus thêm: AI và Live Chat có bộ câu hỏi riêng, không trộn trạng thái hai kênh.
const suggestions = [
  'Tư vấn điện thoại phù hợp trong tầm giá 10 triệu',
  'So sánh iPhone và Samsung đang có tại cửa hàng',
  'Màn hình OLED, AMOLED và LTPO khác nhau thế nào?',
  'Chọn điện thoại ưu tiên camera hay hiệu năng?',
  'Địa chỉ Marcus Store ở đâu?',
  'Tìm điện thoại phù hợp để học tập và làm việc',
]

const openAdvisor = async () => {
  isOpen.value = true
  await nextTick()
  messageInput.value?.focus()
}

const selectSuggestion = async (suggestion) => {
  inputMessage.value = suggestion
  await nextTick()
  messageInput.value?.focus()
}

const buildHistory = () =>
  messages.value
    .filter((message) => !message.isError)
    .slice(-6)
    .map(({ role, content }) => ({ role, content: content.slice(0, 500) }))

// Marcus thêm: chỉ hỗ trợ in đậm, in nghiêng và gạch đầu dòng; mọi nội dung
// vẫn được Vue escape như text nên câu trả lời AI không thể chèn HTML/script.
const parseInlineFormatting = (text) => {
  const tokens = []
  const pattern = /(\*\*[^*\n]+\*\*|\*[^*\n]+\*)/g
  let cursor = 0
  let match

  while ((match = pattern.exec(text)) !== null) {
    if (match.index > cursor) tokens.push({ type: 'text', text: text.slice(cursor, match.index) })
    const value = match[0]
    tokens.push({
      type: value.startsWith('**') ? 'bold' : 'italic',
      text: value.startsWith('**') ? value.slice(2, -2) : value.slice(1, -1),
    })
    cursor = match.index + value.length
  }

  if (cursor < text.length) tokens.push({ type: 'text', text: text.slice(cursor) })
  return tokens.length ? tokens : [{ type: 'text', text }]
}

const formatAssistantMessage = (content = '') =>
  content
    .split(/\r?\n/)
    .map((rawLine) => {
      const trimmed = rawLine.trim()
      const bullet = /^[-•]\s+/.test(trimmed)
      const text = bullet ? trimmed.replace(/^[-•]\s+/, '') : trimmed
      return { bullet, tokens: parseInlineFormatting(text) }
    })
    .filter((line) => line.tokens.some((token) => token.text.trim()))

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || isLoading.value) return

  const history = buildHistory()
  messages.value.push({ id: ++messageId, role: 'user', content })
  inputMessage.value = ''
  isLoading.value = true
  await scrollToBottom()

  try {
    const assistantMessage = {
      id: ++messageId,
      role: 'assistant',
      content: '',
      products: [],
    }
    messages.value.push(assistantMessage)
    await streamAiAdvisor(content, history, getTrackingSessionId(), advisorContext.value, {
      onToken: (token) => {
        isStreamingText.value = true
        assistantMessage.content += token
        scrollToBottom()
      },
      onDone: (data) => {
        assistantMessage.content =
          data?.answer || assistantMessage.content || 'Mình chưa tìm được câu trả lời phù hợp.'
        assistantMessage.products = data?.products ?? []
        assistantMessage.adviceId = data?.adviceId
        assistantMessage.fallbackUsed = Boolean(data?.fallbackUsed)
        assistantMessage.sections = data?.sections || null
        advisorContext.value = data?.context || advisorContext.value
      },
    })
  } catch (error) {
    messages.value.push({
      id: ++messageId,
      role: 'assistant',
      content:
        error.response?.data?.message ||
        error.message ||
        'Marcus AI đang gián đoạn. Bạn có thể thử lại hoặc dùng Live Chat với Admin.',
      isError: true,
    })
  } finally {
    isLoading.value = false
    isStreamingText.value = false
    await scrollToBottom()
  }
}

const submitFeedback = async (message, helpful) => {
  if (!message.adviceId || message.feedback !== undefined) return
  message.feedback = helpful
  try {
    await sendAiAdvisorFeedback(message.adviceId, getTrackingSessionId(), helpful)
  } catch {
    delete message.feedback
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageBody.value) messageBody.value.scrollTop = messageBody.value.scrollHeight
}

const formatPrice = (price) =>
  price == null ? 'Liên hệ' : `${Number(price).toLocaleString('vi-VN')} ₫`

const formatProductPrice = (product) => {
  const minPrice = Number(product?.price || 0)
  const maxPrice = Number(product?.maxPrice || 0)
  if (maxPrice > minPrice) return `${formatPrice(minPrice)} – ${formatPrice(maxPrice)}`
  return formatPrice(product?.price)
}

const formatSkuSummary = (product) => {
  const skuOptions = product?.skuOptions || []
  const first = skuOptions.find((item) => item.skuId === product?.matchedSkuId) || skuOptions[0]
  const label = first?.attributes || first?.skuCode
  if (!label) return `${skuOptions.length} phiên bản còn hàng`
  return skuOptions.length > 1 ? `${label} và ${skuOptions.length - 1} phiên bản khác` : label
}

watch(isOpen, (opened) => setFloatingContactPanelOpen('ai', opened))
watch(messages, persistConversation, { deep: true })
watch(advisorContext, persistConversation, { deep: true })
onMounted(async () => {
  await syncBackendSession()
  const restored = restoreConversation()
  messages.value = restored.messages
  advisorContext.value = restored.context
  messageId = messages.value.reduce(
    (highestId, message) => Math.max(highestId, Number(message.id) || 0),
    0,
  )
  window.addEventListener('marcus-ai-reset', clearConversation)
})
onBeforeUnmount(() => {
  clearFloatingContactPanel('ai')
  window.removeEventListener('marcus-ai-reset', clearConversation)
})
</script>

<style scoped>
.ai-advisor {
  position: fixed;
  right: 24px;
  bottom: 228px;
  /* Marcus sửa: AI trên Live Chat nhưng nằm dưới backdrop/offcanvas Trợ giúp. */
  z-index: 1052;
  font-family: 'Be Vietnam Pro', sans-serif;
}

.ai-trigger {
  position: relative;
  width: 56px;
  height: 56px;
  border: 3px solid #fff;
  border-radius: 18px;
  background: linear-gradient(145deg, #4f46e5, #7c3aed);
  color: #fff;
  font-size: 23px;
  cursor: pointer;
  box-shadow:
    0 10px 24px rgba(79, 70, 229, 0.3),
    0 2px 6px rgba(15, 23, 42, 0.12);
  transition: 0.22s ease;
}

.ai-trigger:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 32px rgba(79, 70, 229, 0.42);
}

.ai-trigger.is-hidden {
  opacity: 0;
  visibility: hidden;
  transform: scale(0.82);
}

.ai-trigger::before {
  content: 'Tư vấn với Marcus AI';
  position: absolute;
  right: 72px;
  top: 50%;
  transform: translateY(-50%);
  padding: 7px 12px;
  border-radius: 8px;
  background: #172033;
  color: #fff;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: 0.2s ease;
}

.ai-trigger:hover::before {
  opacity: 1;
  visibility: visible;
}

.ai-badge {
  position: absolute;
  right: -5px;
  top: -5px;
  min-width: 24px;
  height: 20px;
  padding: 0 5px;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #fbbf24;
  color: #4c1d95;
  font-size: 9px;
  font-weight: 800;
  line-height: 16px;
}

.ai-spark {
  position: absolute;
  top: 5px;
  left: 8px;
  color: #fde68a;
  font-size: 9px;
}

.ai-panel {
  position: absolute;
  right: 0;
  bottom: -204px;
  width: 390px;
  height: 540px;
  overflow: hidden;
  border: 1px solid #ddd6fe;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 22px 55px rgba(30, 41, 59, 0.22);
  display: flex;
  flex-direction: column;
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: linear-gradient(135deg, #312e81, #6d28d9);
  color: #fff;
}

.ai-identity {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.15);
}

.ai-identity h6 {
  margin: 0 0 2px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}

.ai-identity span {
  color: #ddd6fe;
  font-size: 10px;
}

.ai-identity span i {
  margin-right: 4px;
  color: #4ade80;
  font-size: 7px;
}

.ai-close {
  border: 0;
  background: transparent;
  color: #ddd6fe;
  font-size: 18px;
  cursor: pointer;
}

.ai-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  background: #f8fafc;
}

.ai-intro {
  margin-bottom: 12px;
  border: 1px solid #e9d5ff;
  border-radius: 14px;
  padding: 10px 12px;
  background: #faf5ff;
  color: #4c1d95;
  font-size: 12px;
}

.ai-intro p {
  margin: 3px 0 0;
  color: #6b7280;
}

.ai-suggestions > span {
  color: #64748b;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
}

.ai-suggestion-list {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  margin: 6px 0 14px;
  padding-bottom: 4px;
  scrollbar-width: thin;
}

.ai-suggestion-list button {
  flex: 0 0 auto;
  max-width: 260px;
  overflow: hidden;
  border: 1px solid #ddd6fe;
  border-radius: 999px;
  padding: 6px 10px;
  background: #fff;
  color: #5b21b6;
  font-size: 10px;
  white-space: nowrap;
  text-overflow: ellipsis;
  cursor: pointer;
}

.ai-context {
  display: flex;
  align-items: center;
  gap: 5px;
  overflow-x: auto;
  margin: -6px 0 10px;
  padding-bottom: 3px;
  scrollbar-width: thin;
}

.ai-context > span {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 9px;
  font-weight: 700;
}

.ai-context button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto;
  border: 1px solid #c4b5fd;
  border-radius: 999px;
  padding: 4px 7px;
  background: #f5f3ff;
  color: #5b21b6;
  font-size: 9px;
  cursor: pointer;
}

.ai-context button:hover {
  border-color: #8b5cf6;
  background: #ede9fe;
}

.ai-message-row {
  display: flex;
  margin: 8px 0;
}

.ai-message-row.is-user {
  justify-content: flex-end;
}

.ai-message {
  max-width: 88%;
  border: 1px solid #e2e8f0;
  border-radius: 15px 15px 15px 4px;
  padding: 9px 11px;
  background: #fff;
  color: #334155;
  font-size: 12px;
  line-height: 1.5;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.04);
}

.is-user .ai-message {
  border: 0;
  border-radius: 15px 15px 4px 15px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
}

.ai-message p {
  margin: 0;
  white-space: pre-line;
}

.message-content {
  display: grid;
  gap: 5px;
}

.message-content p {
  position: relative;
  white-space: normal;
}

.message-content p.is-bullet {
  padding-left: 13px;
}

.message-bullet {
  position: absolute;
  left: 1px;
  color: #7c3aed;
  font-weight: 800;
}

.message-content strong {
  color: #312e81;
  font-weight: 750;
}

.message-content em {
  color: #64748b;
}

.message-sections {
  display: grid;
  gap: 5px;
}

.message-sections p,
.message-sections ul {
  margin: 0;
}

.message-sections ul {
  display: grid;
  gap: 3px;
  padding-left: 17px;
}

.message-sections li::marker {
  color: #7c3aed;
}

.message-sections strong {
  color: #312e81;
}

.message-sections .follow-up {
  color: #64748b;
}

.message-author {
  display: block;
  margin-bottom: 3px;
  color: #6d28d9;
  font-size: 9px;
  font-weight: 700;
}

.product-suggestions {
  display: grid;
  gap: 6px;
  margin-top: 9px;
}

.ai-feedback {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  padding-top: 7px;
  border-top: 1px solid #eef2f7;
  font-size: 9px;
  color: #64748b;
}
.ai-feedback span {
  width: 100%;
}
.ai-feedback button {
  border: 1px solid #ddd6fe;
  border-radius: 999px;
  padding: 4px 7px;
  background: #fff;
  color: #5b21b6;
  font-size: 9px;
  cursor: pointer;
}
.ai-feedback button.active {
  border-color: #7c3aed;
  background: #ede9fe;
  font-weight: 700;
}
.ai-feedback button:disabled {
  cursor: default;
  opacity: 0.75;
}

.ai-product {
  display: grid;
  grid-template-columns: 45px 1fr 12px;
  align-items: center;
  gap: 8px;
  border: 1px solid #ede9fe;
  border-radius: 11px;
  padding: 7px;
  background: #fafafa;
  color: inherit;
  text-decoration: none;
}

.ai-product img {
  width: 45px;
  height: 45px;
  object-fit: contain;
  border-radius: 8px;
  background: #fff;
}

.ai-product strong,
.ai-product span,
.ai-product small {
  display: block;
}

.ai-product strong {
  display: -webkit-box;
  overflow: hidden;
  color: #1e293b;
  font-size: 10px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.product-price {
  color: #dc2626;
  font-size: 11px;
  font-weight: 700;
}

.ai-product .match-score {
  width: fit-content;
  margin-top: 2px;
  border-radius: 999px;
  padding: 1px 5px;
  background: #ede9fe;
  color: #6d28d9;
  font-size: 8px;
  font-weight: 700;
}

.ai-product .match-reason {
  overflow: hidden;
  margin-top: 2px;
  color: #64748b;
  font-size: 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-product small {
  color: #15803d;
  font-size: 9px;
}

.ai-product small.out-of-stock {
  color: #94a3b8;
}

.ai-product .sku-summary {
  overflow: hidden;
  margin-top: 2px;
  color: #64748b;
  font-size: 8px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-product > i {
  color: #8b5cf6;
  font-size: 9px;
}

.typing {
  display: flex;
  gap: 4px;
  padding: 12px 14px;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8b5cf6;
  animation: typing-dot 1s infinite alternate;
}

.typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing span:nth-child(3) {
  animation-delay: 0.4s;
}

.ai-note {
  padding: 6px 12px;
  border-top: 1px solid #ede9fe;
  background: #faf5ff;
  color: #7c3aed;
  font-size: 9px;
  text-align: center;
}

.ai-footer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.ai-footer textarea {
  flex: 1;
  min-height: 39px;
  max-height: 80px;
  resize: none;
  border: 1px solid #ddd6fe;
  border-radius: 14px;
  padding: 10px 12px;
  outline: none;
  color: #334155;
  font: inherit;
  font-size: 12px;
}

.ai-footer textarea:focus {
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

.ai-footer button {
  width: 39px;
  height: 39px;
  border: 0;
  border-radius: 13px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  cursor: pointer;
}

.ai-footer button:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.ai-window-enter-active,
.ai-window-leave-active {
  transition: 0.24s ease;
}

.ai-window-enter-from,
.ai-window-leave-to {
  opacity: 0;
  transform: translateY(15px) scale(0.94);
}

@keyframes typing-dot {
  to {
    opacity: 0.25;
    transform: translateY(-3px);
  }
}

@media (max-width: 520px) {
  .ai-advisor {
    right: 16px;
  }

  .ai-panel {
    position: fixed;
    right: 12px;
    bottom: 12px;
    width: calc(100vw - 24px);
    height: min(620px, calc(100vh - 24px));
  }
}
</style>
