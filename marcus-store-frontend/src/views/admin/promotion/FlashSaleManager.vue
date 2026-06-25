<template>
  <div class="flashsale-page">
    <Transition name="fade">
      <div v-if="toast.show" class="fs-toast" :class="toast.type">
        <strong>{{ toast.title }}</strong>
        <span>{{ toast.message }}</span>
      </div>
    </Transition>

    <div class="fs-shell">
      <!-- HERO -->
      <section class="fs-hero">
        <div class="fs-hero-title">
          <div class="fs-hero-icon">
            <i class="bi bi-lightning-charge-fill"></i>
          </div>
          <div>
            <h1>Quan ly Flash Sale</h1>
            <p>Tao va quan ly cac dot giam gia nhanh theo khung gio.</p>
          </div>
        </div>
        <button type="button" class="fs-btn-primary" @click="openCreateModal">
          <i class="bi bi-plus-lg"></i>
          Tao Flash Sale
        </button>
      </section>

      <!-- STATS -->
      <section class="fs-stats">
        <article class="fs-stat">
          <span>Tong chien dich</span>
          <strong>{{ stats.total }}</strong>
        </article>
        <article class="fs-stat">
          <span>Dang dien ra</span>
          <strong class="accent">{{ stats.active }}</strong>
        </article>
        <article class="fs-stat">
          <span>Sap dien ra</span>
          <strong>{{ stats.upcoming }}</strong>
        </article>
        <article class="fs-stat">
          <span>Tong SP sale</span>
          <strong>{{ stats.totalProducts }}</strong>
        </article>
      </section>

      <!-- TOOLBAR -->
      <section class="fs-toolbar">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-md-6 col-lg-5">
            <label class="form-label">Tim kiem</label>
            <div class="input-group">
              <span class="input-group-text">
                <i class="bi bi-search"></i>
              </span>
              <input
                v-model.trim="filters.keyword"
                type="text"
                class="form-control"
                placeholder="Tim theo ten chien dich..."
              />
            </div>
          </div>

          <div class="col-12 col-md-6 col-lg">
            <label class="form-label">Trang thai</label>
            <select v-model="filters.status" class="form-select">
              <option value="ALL">Tat ca</option>
              <option value="ACTIVE">Dang dien ra</option>
              <option value="SCHEDULED">Da len lich</option>
              <option value="UPCOMING">Sap dien ra</option>
              <option value="ENDED">Da ket thuc</option>
              <option value="CANCELLED">Da huy</option>
              <option value="PENDING">Cho xu ly</option>
            </select>
          </div>

          <div class="col-12 col-md-6 col-lg-auto">
            <button type="button" class="btn btn-soft w-100" title="Xoa loc" @click="resetFilters">
              <i class="bi bi-arrow-counterclockwise"></i>
            </button>
          </div>
        </div>
      </section>

      <!-- TABLE -->
      <section class="fs-table-panel">
        <div class="table-responsive">
          <table class="table align-middle fs-table mb-0">
            <thead>
            <tr>
              <th class="fs-th-w60">ID</th>
              <th>Ten chien dich</th>
              <th>Thoi gian</th>
              <th class="text-center">So SP</th>
              <th>Trang thai</th>
              <th class="text-center">Kich hoat</th>
              <th class="text-center fs-th-w100">Thao tac</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="slot in filteredSlots" :key="slot.slotId">
              <td><span class="fs-id-text">#{{ String(slot.slotId).padStart(3,'0') }}</span></td>
              <td><span class="fs-slot-name">{{ slot.name }}</span></td>
              <td>
                <div class="fs-time-line">
                  <div><span class="fs-label">Tu</span><span>{{ formatDateTime(slot.startDate) }}</span></div>
                  <div><span class="fs-label">Den</span><span>{{ formatDateTime(slot.endDate) }}</span></div>
                </div>
              </td>
              <td class="text-center">
                <span class="fs-item-count">{{ slot.items.length }}</span>
              </td>
              <td>
                <span class="fs-badge" :class="statusBadgeClass(slot)">
                  <span class="fs-dot"></span>{{ statusBadgeLabel(slot) }}
                </span>
              </td>
              <td class="text-center">
                <label class="fs-tog">
                  <input
                    type="checkbox"
                    :checked="slot.status === 2"
                    @change="toggleActive(slot, $event.target.checked)"
                  />
                  <div class="fs-tog-track"><div class="fs-tog-thumb"></div></div>
                </label>
              </td>
              <td class="text-center">
                <button class="fs-icon-btn fs-icon-edit" title="Sua" @click="openEditModal(slot)">
                  <i class="bi bi-pencil-square"></i>
                </button>
                <button class="fs-icon-btn fs-icon-del ms-1" title="Xoa" @click="openDelModal(slot)">
                  <i class="bi bi-trash3"></i>
                </button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <!-- EMPTY -->
        <div v-if="!loading && localSlots.length === 0" class="fs-empty">
          <i class="bi bi-lightning"></i>
          <p>Chua co Flash Sale nao</p>
          <div class="fs-hint-text">Tao chien dich Flash Sale dau tien de bat dau.</div>
        </div>

        <!-- LOADING -->
        <div v-if="loading" class="fs-empty">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="mt-2">Dang tai...</p>
        </div>
      </section>
    </div>

    <!-- CRUD MODAL -->
    <div
      class="fs-modal-overlay"
      :class="{ show: isModalOpen }"
      @click.self="closeModal"
    >
      <div class="fs-modal-box">
        <div class="fs-modal-hd">
          <h3>{{ isEditing ? 'Chinh sua Flash Sale' : 'Tao Flash Sale moi' }}</h3>
          <button class="fs-modal-close" @click="closeModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <div class="fs-modal-body">
          <!-- BASIC INFO -->
          <div class="fs-form-group">
            <label class="fs-form-label">Ten chien dich <span class="text-danger">*</span></label>
            <input
              v-model="form.name"
              type="text"
              class="fs-input"
              :class="{ 'is-invalid': submitted && errors.name }"
              placeholder="VD: Flash Sale Thu 6 - iPhone Series"
            />
            <div v-if="submitted && errors.name" class="fs-form-error">{{ errors.name }}</div>
          </div>

          <div class="row g-3">
            <div class="col-6">
              <label class="fs-form-label">Thoi gian bat dau <span class="text-danger">*</span></label>
              <input
                v-model="form.startDate"
                type="datetime-local"
                class="fs-input fs-input-fix"
                :class="{ 'is-invalid': submitted && errors.startDate }"
              />
              <div v-if="submitted && errors.startDate" class="fs-form-error">{{ errors.startDate }}</div>
            </div>
            <div class="col-6">
              <label class="fs-form-label">Thoi gian ket thuc <span class="text-danger">*</span></label>
              <input
                v-model="form.endDate"
                type="datetime-local"
                class="fs-input fs-input-fix"
                :class="{ 'is-invalid': submitted && errors.endDate }"
              />
              <div v-if="submitted && errors.endDate" class="fs-form-error">{{ errors.endDate }}</div>
            </div>
          </div>
          <div v-if="submitted && errors.time" class="fs-form-error mb-3">{{ errors.time }}</div>

          <!-- TABS -->
          <div class="fs-tabs">
            <button
              class="fs-tab-btn"
              :class="{ active: activeTab === 0 }"
              @click="switchTab(0)"
            >
              <i class="bi bi-box-seam"></i> Chon san pham
            </button>
            <button
              class="fs-tab-btn"
              :class="{ active: activeTab === 1 }"
              @click="switchTab(1)"
            >
              <i class="bi bi-grid-3x3-gap"></i> Theo danh muc
            </button>
          </div>

          <!-- TAB 0: SELECT PRODUCTS -->
          <div v-show="activeTab === 0" class="fs-tab-content">
            <div class="fs-prod-search">
              <div class="input-group">
                <input
                  v-model="productSearchQuery"
                  type="text"
                  class="fs-input"
                  placeholder="Tim san pham..."
                  @focus="openProdDrop"
                />
                <button class="fs-btn-soft" type="button" @click="openProdDrop">
                  <i class="bi bi-search"></i>
                </button>
              </div>
              <div v-if="showProdDrop" class="fs-prod-dropdown" id="prodDropdown">
                <div
                  v-for="p in filteredProducts"
                  :key="p.id"
                  class="fs-prod-row"
                  @click="toggleProduct(p.id)"
                >
                  <input
                    type="checkbox"
                    :checked="selectedItemPids.includes(p.id)"
                    @click.stop
                    @change="toggleProduct(p.id)"
                  />
                  <div class="fs-prod-thumb">{{ p.emoji }}</div>
                  <div class="fs-prod-info">
                    <strong>{{ p.name }}</strong>
                    <small>Ton kho: {{ p.stock }} | Gia goc: {{ formatVND(p.price) }}</small>
                  </div>
                </div>
                <div v-if="filteredProducts.length === 0" class="fs-dropdown-empty">
                  Khong tim thay san pham
                </div>
              </div>
            </div>

            <!-- SELECTED ITEMS -->
            <div v-if="selectedItemPids.length > 0" class="fs-sel-table-wrap">
              <table class="table fs-sel-table mb-0">
                <thead>
                  <tr>
                    <th>San pham</th>
                    <th>Gia goc</th>
                    <th>Gia Flash Sale</th>
                    <th>Chiet khau (%)</th>
                    <th>So luong</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="pid in selectedItemPids" :key="pid">
                    <td><strong>{{ getProductName(pid) }}</strong></td>
                    <td>{{ formatVND(getProductPrice(pid)) }}</td>
                    <td>
                      <input
                        type="number"
                        class="fs-input"
                        :value="selItems[pid]?.flashSalePrice || 0"
                        @input="onPriceChange(pid, $event.target.value)"
                        min="0"
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        class="fs-input"
                        :value="selItems[pid]?.discountPercent || 0"
                        @input="onDiscountChange(pid, $event.target.value)"
                        min="0"
                        max="100"
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        class="fs-input"
                        :class="{ 'is-invalid': qtyError[pid] }"
                        :value="selItems[pid]?.flashSaleQuantity || 0"
                        @input="onQtyChange(pid, $event.target.value)"
                        min="0"
                      />
                      <div v-if="qtyError[pid]" class="fs-form-error">Qua ton kho</div>
                    </td>
                    <td>
                      <button class="fs-rm-btn" @click="removeItem(pid)">
                        <i class="bi bi-x"></i>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- TAB 1: BY CATEGORY -->
          <div v-show="activeTab === 1" class="fs-coming-soon">
            <i class="bi bi-grid-3x3-gap-fill fs-coming-soon-icon"></i>
            <h3>Flash Sale toan danh muc</h3>
            <p>Tinh nang nay dang duoc phat trien va se ra mat som.</p>
          </div>
        </div>

        <div class="fs-modal-footer">
          <button type="button" class="fs-btn-white" @click="resetForm">
            <i class="bi bi-arrow-counterclockwise"></i>Lam moi
          </button>
          <button type="button" class="fs-btn-soft" @click="previewSlot">
            <i class="bi bi-eye"></i>Xem truoc
          </button>
          <button type="button" class="fs-btn-primary" :disabled="saving" @click="saveSlot">
            {{ saving ? 'Dang luu...' : 'Luu chien dich' }}
          </button>
        </div>
      </div>
    </div>

    <!-- DELETE CONFIRM -->
    <div
      class="fs-del-overlay"
      :class="{ show: showDelModal }"
      @click.self="closeDelModal"
    >
      <div class="fs-del-box">
        <div class="fs-del-icon"><i class="bi bi-trash3-fill"></i></div>
        <h5>Xoa chien dich nay?</h5>
        <p>
          Chien dich <strong>{{ delTarget?.name }}</strong> se bi xoa vinh vien.
          Hanh dong nay khong the hoan tac.
        </p>
        <div class="fs-del-actions">
          <button type="button" class="fs-btn-white" @click="closeDelModal">Huy bo</button>
          <button type="button" class="fs-btn-primary fs-btn-danger" @click="confirmDel">
            <i class="bi bi-trash3"></i>Xoa ngay
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import '@/assets/css/FlashSale.css'

/* ── MOCK DATA MODE ── */
const USE_MOCK_DATA = true

/* ── MOCK SLOTS (6 trang thai khac nhau) ── */
const now = new Date()
const d = (offsetDays) => {
  const dt = new Date(now)
  dt.setDate(dt.getDate() + offsetDays)
  return dt.toISOString()
}

const MOCK_SLOTS = [
  {
    slotId: 1,
    name: 'Flash Sale Thu 6 - iPhone 15 Series',
    startDate: d(0),
    endDate: d(1),
    status: 2, // ACTIVE
    items: [
      { skuId: 1, productName: 'iPhone 15 Pro Max 256GB', originalPrice: 34990000, flashSalePrice: 29990000, flashSaleQuantity: 50, soldQuantity: 23 },
      { skuId: 2, productName: 'iPhone 15 Pro 128GB', originalPrice: 27990000, flashSalePrice: 23990000, flashSaleQuantity: 30, soldQuantity: 18 },
    ]
  },
  {
    slotId: 2,
    name: 'Flash Sale Cuoi Tuan - Samsung Galaxy',
    startDate: d(2),
    endDate: d(3),
    status: 1, // SCHEDULED
    items: [
      { skuId: 3, productName: 'Samsung Galaxy S24 Ultra', originalPrice: 29990000, flashSalePrice: 25990000, flashSaleQuantity: 25, soldQuantity: 0 },
      { skuId: 4, productName: 'Samsung Galaxy S24+', originalPrice: 21990000, flashSalePrice: 18990000, flashSaleQuantity: 40, soldQuantity: 0 },
    ]
  },
  {
    slotId: 3,
    name: 'Flash Sale Sang Thu 2 - Xiaomi',
    startDate: d(5),
    endDate: d(6),
    status: 1, // UPCOMING (startDate > now)
    items: [
      { skuId: 5, productName: 'Xiaomi 14 Ultra', originalPrice: 22990000, flashSalePrice: 19990000, flashSaleQuantity: 35, soldQuantity: 0 },
    ]
  },
  {
    slotId: 4,
    name: 'Flash Sale Tuan Truoc - OPPO',
    startDate: d(-3),
    endDate: d(-2),
    status: 3, // ENDED
    items: [
      { skuId: 6, productName: 'OPPO Find X7 Pro', originalPrice: 19990000, flashSalePrice: 16990000, flashSaleQuantity: 60, soldQuantity: 60 },
    ]
  },
  {
    slotId: 5,
    name: 'Flash Sale Da Huy - Vivo',
    startDate: d(-1),
    endDate: d(1),
    status: 4, // CANCELLED
    items: [
      { skuId: 7, productName: 'Vivo X100 Pro', originalPrice: 17990000, flashSalePrice: 14990000, flashSaleQuantity: 20, soldQuantity: 0 },
    ]
  },
  {
    slotId: 6,
    name: 'Flash Sale Cho Xu Ly - Google Pixel',
    startDate: null,
    endDate: null,
    status: 0, // PENDING
    items: [
      { skuId: 8, productName: 'Google Pixel 8 Pro', originalPrice: 22990000, flashSalePrice: 19990000, flashSaleQuantity: 15, soldQuantity: 0 },
    ]
  },
]

/* ── LOCAL STATE (mock) ── */
const localSlots = ref([...MOCK_SLOTS])
const loading = ref(false)
const submitted = ref(false)
const saving = ref(false)

function resolveStatus(slot) {
  if (slot.status === 2) return 'ACTIVE'
  if (slot.status === 3) return 'ENDED'
  if (slot.status === 4) return 'CANCELLED'
  if (slot.status === 1) return 'SCHEDULED'
  if (slot.startDate) {
    const start = new Date(slot.startDate)
    if (now < start) return 'UPCOMING'
  }
  return 'PENDING'
}

const slotsWithStatus = computed(() =>
  localSlots.value.map(s => ({ ...s, resolvedStatus: resolveStatus(s) }))
)

/* ── FILTERS ── */
const filters = reactive({
  keyword: '',
  status: 'ALL',
})

watch(
  () => [filters.keyword, filters.status],
  () => {}
)

const filteredSlots = computed(() => {
  return slotsWithStatus.value.filter((s) => {
    if (filters.keyword && !s.name.toLowerCase().includes(filters.keyword.toLowerCase())) {
      return false
    }
    if (filters.status !== 'ALL' && s.resolvedStatus !== filters.status) {
      return false
    }
    return true
  })
})

function resetFilters() {
  filters.keyword = ''
  filters.status = 'ALL'
}

/* ── STATS ── */
const stats = computed(() => ({
  total: localSlots.value.length,
  active: slotsWithStatus.value.filter((s) => s.resolvedStatus === 'ACTIVE').length,
  upcoming: slotsWithStatus.value.filter((s) => s.resolvedStatus === 'UPCOMING' || s.resolvedStatus === 'SCHEDULED').length,
  totalProducts: localSlots.value.reduce((a, s) => a + s.items.length, 0),
}))

/* ── FORMAT ── */
function formatVND(value) {
  if (value === null || value === undefined || value === '') return '-'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(Number(value))
}

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (isNaN(d.getTime())) return String(value)
  return d.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function statusBadgeLabel(slot) {
  const map = {
    ACTIVE: 'Dang dien ra',
    SCHEDULED: 'Da len lich',
    UPCOMING: 'Sap dien ra',
    ENDED: 'Da ket thuc',
    CANCELLED: 'Da huy',
    PENDING: 'Cho xu ly',
  }
  return map[slot.resolvedStatus] || slot.resolvedStatus
}

function statusBadgeClass(slot) {
  return {
    ACTIVE: 'fs-badge-active',
    SCHEDULED: 'fs-badge-scheduled',
    UPCOMING: 'fs-badge-scheduled',
    ENDED: 'fs-badge-ended',
    CANCELLED: 'fs-badge-cancelled',
    PENDING: 'fs-badge-pending',
  }[slot.resolvedStatus] || 'fs-badge-pending'
}

/* ── TOGGLE ACTIVE ── */
function toggleActive(slot, checked) {
  const idx = localSlots.value.findIndex(s => s.slotId === slot.slotId)
  if (idx !== -1) {
    localSlots.value[idx] = {
      ...localSlots.value[idx],
      status: checked ? 2 : 3
    }
    showToast({ type: 'success', title: 'Thanh cong', message: checked ? 'Da kich hoat chien dich.' : 'Da tat chien dich.' })
  }
}

/* ════════════════════════════════════
   MODAL STATE
═══════════════════════════════════ */
const isModalOpen = ref(false)
const isEditing = ref(false)
const activeTab = ref(0)
const editSlotId = ref(null)

const defaultForm = {
  name: '',
  status: 1,
  startDate: '',
  endDate: '',
}

const form = reactive({ ...defaultForm })

const errors = computed(() => {
  if (!submitted.value) return {}
  const result = {}
  if (!form.name.trim()) {
    result.name = 'Vui long nhap ten chien dich'
  }
  if (!form.startDate) {
    result.startDate = 'Vui long chon thoi gian bat dau'
  }
  if (!form.endDate) {
    result.endDate = 'Vui long chon thoi gian ket thuc'
  }
  if (form.startDate && form.endDate) {
    const s = new Date(form.startDate)
    const e = new Date(form.endDate)
    if (e <= s) {
      result.time = 'Thoi gian ket thuc phai sau thoi gian bat dau'
    }
  }
  return result
})

/* ── PRODUCT SELECTION ── */
const PRODUCTS = [
  { id: 1, name: 'iPhone 15 Pro Max 256GB', emoji: '📱', price: 34990000, stock: 50 },
  { id: 2, name: 'iPhone 15 Pro 128GB', emoji: '📱', price: 27990000, stock: 30 },
  { id: 3, name: 'Samsung Galaxy S24 Ultra', emoji: '📲', price: 29990000, stock: 30 },
  { id: 4, name: 'Samsung Galaxy S24+', emoji: '📲', price: 21990000, stock: 40 },
  { id: 5, name: 'Xiaomi 14 Ultra', emoji: '🖤', price: 22990000, stock: 45 },
  { id: 6, name: 'OPPO Find X7 Pro', emoji: '🟢', price: 19990000, stock: 25 },
  { id: 7, name: 'Vivo X100 Pro', emoji: '🔵', price: 17990000, stock: 60 },
  { id: 8, name: 'Google Pixel 8 Pro', emoji: '🌈', price: 22990000, stock: 20 },
]

const selItems = reactive({})
const selectedItemPids = ref([])
const productSearchQuery = ref('')
const showProdDrop = ref(false)
const qtyError = reactive({})

const filteredProducts = computed(() => {
  const q = productSearchQuery.value.toLowerCase()
  return PRODUCTS.filter(p => p.name.toLowerCase().includes(q))
})

function getProductName(pid) {
  return PRODUCTS.find((p) => p.id === pid)?.name || `SP #${pid}`
}
function getProductPrice(pid) {
  return PRODUCTS.find((p) => p.id === pid)?.price ?? 0
}
function getProductStock(pid) {
  return PRODUCTS.find((p) => p.id === pid)?.stock ?? 0
}

function openProdDrop() {
  showProdDrop.value = true
}
function closeProdDrop() {
  showProdDrop.value = false
}
function toggleProduct(pid) {
  const idx = selectedItemPids.value.indexOf(pid)
  if (idx > -1) {
    selectedItemPids.value.splice(idx, 1)
    delete selItems[pid]
    delete qtyError[pid]
  } else {
    selectedItemPids.value.push(pid)
    const p = PRODUCTS.find((x) => x.id === pid)
    selItems[pid] = { discountPercent: 15, flashSalePrice: Math.round((p?.price ?? 0) * 0.85), flashSaleQuantity: 10 }
  }
}
function onDiscountChange(pid, value) {
  const disc = parseFloat(value) || 0
  const orig = getProductPrice(pid)
  const fp = Math.round(orig * (1 - disc / 100))
  selItems[pid] = { ...selItems[pid], discountPercent: disc, flashSalePrice: fp }
}
function onPriceChange(pid, value) {
  const fp = parseFloat(value) || 0
  const orig = getProductPrice(pid)
  const disc = orig > 0 ? parseFloat(((1 - fp / orig) * 100).toFixed(1)) : 0
  selItems[pid] = { ...selItems[pid], flashSalePrice: fp, discountPercent: disc }
}
function onQtyChange(pid, value) {
  const v = parseInt(value) || 0
  const stock = getProductStock(pid)
  qtyError[pid] = v > stock
  selItems[pid] = { ...selItems[pid], flashSaleQuantity: v }
}
function removeItem(pid) {
  const idx = selectedItemPids.value.indexOf(pid)
  if (idx > -1) selectedItemPids.value.splice(idx, 1)
  delete selItems[pid]
  delete qtyError[pid]
}

/* ════════════════════════════════════
   CRUD
═══════════════════════════════════ */
function openCreateModal() {
  editSlotId.value = null
  isEditing.value = false
  resetForm()
  isModalOpen.value = true
  activeTab.value = 0
}

function openEditModal(slot) {
  editSlotId.value = slot.slotId
  isEditing.value = true
  resetForm(false)
  form.name = slot.name
  form.startDate = toLocalDatetime(slot.startDate)
  form.endDate = toLocalDatetime(slot.endDate)
  form.status = slot.status

  selectedItemPids.value = []
  Object.keys(selItems).forEach((k) => delete selItems[k])
  slot.items.forEach((item) => {
    const pid = item.skuId
    if (pid) {
      selectedItemPids.value.push(pid)
      selItems[pid] = {
        discountPercent: item.originalPrice > 0 ? parseFloat(((1 - item.flashSalePrice / item.originalPrice) * 100).toFixed(1)) : 0,
        flashSalePrice: item.flashSalePrice,
        flashSaleQuantity: item.flashSaleQuantity,
      }
    }
  })
  isModalOpen.value = true
  activeTab.value = 0
}

function closeModal() {
  isModalOpen.value = false
}

function switchTab(i) {
  activeTab.value = i
}

function resetForm(clearStatus = true) {
  submitted.value = false
  Object.keys(form).forEach((k) => {
    if (k === 'status' && !clearStatus) return
    form[k] = defaultForm[k]
  })
  selectedItemPids.value = []
  Object.keys(selItems).forEach((k) => delete selItems[k])
  Object.keys(qtyError).forEach((k) => delete qtyError[k])
  productSearchQuery.value = ''
  showProdDrop.value = false
}

function toLocalDatetime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function saveSlot() {
  submitted.value = true
  if (Object.keys(errors.value).length > 0) return

  const pids = selectedItemPids.value
  if (pids.length === 0) {
    showToast({ type: 'error', title: 'Loi', message: 'Vui long chon it nhat 1 san pham.' })
    return
  }
  let hasQtyErr = false
  pids.forEach((pid) => {
    const it = selItems[pid]
    if (!it || it.flashSaleQuantity <= 0) {
      hasQtyErr = true
    }
    if ((it?.flashSaleQuantity ?? 0) > getProductStock(pid)) {
      qtyError[pid] = true
      hasQtyErr = true
    }
  })
  if (hasQtyErr) {
    showToast({ type: 'error', title: 'Loi', message: 'Kiem tra lai so luong san pham.' })
    return
  }

  saving.value = true

  const items = pids.map((pid) => {
    const it = selItems[pid]
    const p = PRODUCTS.find((x) => x.id === pid)
    return {
      skuId: pid,
      productName: p?.name || `SP #${pid}`,
      originalPrice: p?.price ?? it.flashSalePrice,
      flashSalePrice: it.flashSalePrice,
      flashSaleQuantity: it.flashSaleQuantity,
      soldQuantity: 0,
    }
  })

  const payload = {
    name: form.name.trim(),
    startDate: form.startDate || null,
    endDate: form.endDate || null,
    status: Number(form.status),
    items,
  }

  // Simulate async
  await new Promise(r => setTimeout(r, 300))

  if (isEditing.value) {
    const idx = localSlots.value.findIndex(s => s.slotId === editSlotId.value)
    if (idx !== -1) {
      localSlots.value[idx] = { ...localSlots.value[idx], ...payload }
    }
    showToast({ type: 'success', title: 'Thanh cong', message: 'Cap nhat Flash Sale thanh cong!' })
  } else {
    const newSlot = {
      ...payload,
      slotId: Math.max(...localSlots.value.map(s => s.slotId), 0) + 1,
    }
    localSlots.value.unshift(newSlot)
    showToast({ type: 'success', title: 'Thanh cong', message: 'Tao Flash Sale thanh cong!' })
  }

  saving.value = false
  closeModal()
}

function previewSlot() {
  const name = form.name || '(chua dat ten)'
  const count = selectedItemPids.value.length
  showToast({ type: 'info', title: 'Xem truoc', message: `"${name}" - ${count} san pham` })
}

/* ── DELETE ── */
const showDelModal = ref(false)
const delTarget = ref(null)

function openDelModal(slot) {
  delTarget.value = slot
  showDelModal.value = true
}
function closeDelModal() {
  showDelModal.value = false
  delTarget.value = null
}
async function confirmDel() {
  if (!delTarget.value) return
  await new Promise(r => setTimeout(r, 200))
  localSlots.value = localSlots.value.filter(s => s.slotId !== delTarget.value.slotId)
  showToast({ type: 'success', title: 'Thanh cong', message: 'Da xoa Flash Sale.' })
  closeDelModal()
}

/* ════════════════════════════════════
   TOAST
═══════════════════════════════════ */
const toast = reactive({
  show: false,
  type: 'success',
  title: '',
  message: '',
})

let toastTimer = null
function showToast({ type = 'success', title, message }) {
  toast.show = false
  clearTimeout(toastTimer)
  nextTick(() => {
    toast.type = type
    toast.title = title
    toast.message = message
    toast.show = true
    toastTimer = setTimeout(() => {
      toast.show = false
    }, 2800)
  })
}

onUnmounted(() => {
  clearTimeout(toastTimer)
})

// Close dropdown on outside click
onMounted(() => {
  document.addEventListener('click', (e) => {
    if (showProdDrop.value && !e.target.closest('.fs-prod-search')) {
      showProdDrop.value = false
    }
  })
})
</script>

<style scoped>
</style>
