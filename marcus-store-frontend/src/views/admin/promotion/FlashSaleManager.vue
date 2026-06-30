<template>
  <div class="flashsale-page">
    <Transition name="fade">
      <div v-if="toast.show" class="toast-alert" :class="toast.type">
        <strong>{{ toast.title }}</strong>
        <span>{{ toast.message }}</span>
      </div>
    </Transition>

    <div class="flashsale-shell">
      <!-- HERO -->
      <section class="flashsale-hero">
        <div class="hero-title">
          <div class="hero-icon">
            <i class="bi bi-lightning-charge-fill"></i>
          </div>
          <div>
            <h1>Quản lý Flash Sale</h1>
            <p>Tạo và quản lý các đợt giảm giá nhanh theo khung giờ.</p>
          </div>
        </div>
        <button type="button" class="btn-primary-action" @click="openCreateModal">
          <i class="bi bi-plus-lg"></i>
          Tạo Flash Sale
        </button>
      </section>

      <!-- STATS -->
      <section class="stats-grid">
        <article class="stat-card">
          <span>Tổng chiến dịch</span>
          <strong>{{ stats.total }}</strong>
        </article>
        <article class="stat-card">
          <span>Đang diễn ra</span>
          <strong class="accent">{{ stats.active }}</strong>
        </article>
        <article class="stat-card">
          <span>Sắp diễn ra</span>
          <strong>{{ stats.upcoming }}</strong>
        </article>
        <article class="stat-card">
          <span>Tổng SP sale</span>
          <strong>{{ stats.totalProducts }}</strong>
        </article>
      </section>

      <!-- TOOLBAR -->
      <section class="toolbar-panel">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-md-6 col-lg-5">
            <label class="form-label">Tìm kiếm</label>
            <div class="input-group">
              <span class="input-group-text">
                <i class="bi bi-search"></i>
              </span>
              <input
                v-model.trim="filters.keyword"
                type="text"
                class="form-control"
                placeholder="Tìm theo tên chiến dịch..."
              />
            </div>
          </div>

          <div class="col-12 col-md-6 col-lg">
            <label class="form-label">Trạng thái</label>
            <select v-model="filters.status" class="form-select">
              <option value="ALL">Tất cả</option>
              <option value="ACTIVE">Đang diễn ra</option>
              <option value="SCHEDULED">Đã lên lịch</option>
              <option value="UPCOMING">Sắp diễn ra</option>
              <option value="ENDED">Đã kết thúc</option>
              <option value="CANCELLED">Đã hủy</option>
              <option value="PENDING">Chờ xử lý</option>
            </select>
          </div>

          <div class="col-12 col-md-6 col-lg-auto">
            <button type="button" class="btn-soft w-100" title="Xóa lọc" @click="resetFilters">
              <i class="bi bi-arrow-counterclockwise"></i>
            </button>
          </div>


        </div>
      </section>

      <!-- TABLE -->
      <section class="table-panel">
        <div class="table-responsive">
          <table class="table align-middle flashsale-table mb-0">
            <thead>
            <tr>
              <th>ID</th>
              <th>Tên chiến dịch</th>
              <th>Thời gian</th>
              <th class="text-center">Số SP</th>
              <th>Trạng thái</th>
              <th class="text-center">Kích hoạt</th>
              <th class="text-center">Thao tác</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="slot in filteredSlots" :key="slot.slotId">
              <td><span class="id-text">#{{ String(slot.slotId).padStart(3, '0') }}</span></td>
              <td><span class="slot-name">{{ slot.name }}</span></td>
              <td>
                <div class="time-line">
                  <div><span class="time-label">Từ</span><span>{{
                      formatDateTime(slot.startDate)
                    }}</span></div>
                  <div><span class="time-label">Đến</span><span>{{
                      formatDateTime(slot.endDate)
                    }}</span></div>
                </div>
              </td>
              <td class="text-center">
                <span class="item-count">{{ slot.items.length }}</span>
              </td>
              <td>
                <span class="status-badge" :class="statusBadgeClass(slot)">
                  {{ statusBadgeLabel(slot) }}
                </span>
              </td>
              <td class="text-center">
                <label class="fs-tog">
                  <input
                    type="checkbox"
                    :checked="slot.status === 2"
                    @change="toggleActive(slot, $event.target.checked)"
                  />
                  <span class="fs-tog-track"><span class="fs-tog-thumb"></span></span>
                </label>
              </td>
              <td class="text-center">
                <button class="icon-button" title="Sửa" @click="openEditModal(slot)">
                  <i class="bi bi-pencil-square"></i>
                </button>
                <button class="icon-button danger ms-1" title="Xóa" @click="openDelModal(slot)">
                  <i class="bi bi-trash3"></i>
                </button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <!-- EMPTY -->
        <div v-if="!loading && localSlots.length === 0" class="empty-state">
          <i class="bi bi-lightning"></i>
          <p>Chưa có Flash Sale nào</p>
          <span class="fs-hint-text">Tạo chiến dịch Flash Sale đầu tiên để bắt đầu.</span>
        </div>

        <!-- LOADING -->
        <div v-if="loading" class="empty-state">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="mt-2">Đang tải...</p>
        </div>
      </section>
    </div>

    <!-- CRUD MODAL -->
    <div
      class="modal-backdrop-custom"
      v-if="isModalOpen"
      @click.self="closeModal"
    >
      <div class="flashsale-modal">
        <div class="modal-head">
          <div>
            <h2>{{ isEditing ? 'Chỉnh sửa Flash Sale' : 'Tạo Flash Sale mới' }}</h2>
            <p>Thiết lập thông tin chiến dịch và các sản phẩm áp dụng.</p>
          </div>
          <button type="button" class="icon-button" title="Đóng" @click="closeModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <form class="voucher-form" novalidate @submit.prevent="saveSlot">
          <!-- BASIC INFO -->
          <div class="form-section">
            <div class="section-title">
              <span>1</span>
              <div>
                <h3>Thông tin chiến dịch</h3>
                <p>Đặt tên và khoảng thời gian diễn ra Flash Sale.</p>
              </div>
            </div>

            <div class="full-width-field">
              <label class="form-label">Tên chiến dịch <span class="text-danger">*</span></label>
              <input
                v-model="form.name"
                type="text"
                class="form-control"
                :class="{ 'is-invalid': submitted && errors.name }"
                placeholder="VD: Flash Sale Thứ 6 - iPhone Series"
              />
              <div v-if="submitted && errors.name" class="invalid-feedback">{{ errors.name }}</div>
            </div>

            <div class="row g-3 mt-1">
              <div class="col-12 col-md-6">
                <label class="form-label">Thời gian bắt đầu <span
                  class="text-danger">*</span></label>
                <input
                  v-model="form.startDate"
                  type="datetime-local"
                  class="form-control"
                  :class="{ 'is-invalid': submitted && errors.startDate }"
                />
                <div v-if="submitted && errors.startDate" class="invalid-feedback">
                  {{ errors.startDate }}
                </div>
              </div>
              <div class="col-12 col-md-6">
                <label class="form-label">Thời gian kết thúc <span
                  class="text-danger">*</span></label>
                <input
                  v-model="form.endDate"
                  type="datetime-local"
                  class="form-control"
                  :class="{ 'is-invalid': submitted && errors.endDate }"
                />
                <div v-if="submitted && errors.endDate" class="invalid-feedback">{{
                    errors.endDate
                  }}
                </div>
              </div>
            </div>
            <div v-if="submitted && errors.time" class="invalid-feedback mt-2">{{
                errors.time
              }}
            </div>
          </div>


          <!-- TABS -->
          <div class="fs-tabs">
            <button
              class="fs-tab-btn"
              :class="{ active: activeTab === 0 }"
              @click="switchTab(0)"
            >
              <i class="bi bi-box-seam"></i> Chọn sản phẩm
            </button>
            <button
              class="fs-tab-btn"
              :class="{ active: activeTab === 1 }"
              @click="switchTab(1)"
            >
              <i class="bi bi-grid-3x3-gap"></i> Theo danh mục
            </button>
          </div>

          <!-- TAB 0: SELECT PRODUCTS -->
          <div v-show="activeTab === 0" class="fs-tab-content">
            <!-- CASCADING PICKER -->
            <div class="fs-cascade-wrap">
              <button
                type="button"
                class="fs-cascade-trigger"
                :class="{ active: cascadeOpen, has: selectedItemPids.length > 0 }"
                @click.stop="cascadeOpen = !cascadeOpen"
              >
                <i class="bi bi-diagram-3"></i>
                <span class="fs-cascade-trigger-text">
                  <template v-if="selectedItemPids.length > 0">
                    <span class="fs-cascade-dot"></span>
                    Đã chọn {{ selectedItemPids.length }} sản phẩm
                  </template>
                  <template v-else>
                    Chọn thương hiệu / dòng sản phẩm...
                  </template>
                </span>
                <i class="bi bi-chevron-down fs-cascade-chevron"></i>
              </button>

              <Transition name="fs-panel">
                <div v-if="cascadeOpen" class="fs-cascade-panel" @click.stop>
                  <div class="fs-cascade-cols">
                    <!-- LEFT COLUMN: Brands (~40%) -->
                    <ul class="fs-cascade-left">
                      <li
                        v-for="b in BRAND_SERIES"
                        :key="b.key"
                        class="fs-cascade-brand"
                        :class="{ active: cascadeBrand === b.key }"
                        @mouseenter="setCascadeBrand(b.key)"
                        @click="setCascadeBrand(b.key)"
                      >
                        <i class="bi" :class="b.icon"></i>
                        <span>{{ b.label }}</span>
                        <i class="bi bi-chevron-right fs-cascade-arrow"></i>
                      </li>
                    </ul>

                    <!-- RIGHT COLUMN: Series + Products (~60%) -->
                    <div class="fs-cascade-right">
                      <template v-if="cascadeBrand">
                        <div class="fs-cascade-right-hd">
                          <strong>{{
                              BRAND_SERIES.find(x => x.key === cascadeBrand)?.label
                            }}</strong>
                          <button
                            type="button"
                            class="fs-cascade-addall"
                            @click.stop="selectAllInBrand(cascadeBrand)"
                          >
                            <input
                              type="checkbox"
                              class="fs-cascade-addall-cb"
                              :checked="brandFullySelected(cascadeBrand)"
                              @click.stop="toggleAllInBrand(cascadeBrand)"
                            />
                            <span>Chọn tất cả</span>
                          </button>
                        </div>

                        <div class="fs-cascade-right-body">
                          <div
                            v-for="s in (BRAND_SERIES.find(x => x.key === cascadeBrand)?.series || [])"
                            :key="s"
                            class="fs-cascade-series"
                          >
                            <button
                              type="button"
                              class="fs-cascade-series-btn"
                              :class="{ active: cascadeSeries === s }"
                              @click.stop="cascadeSeries = cascadeSeries === s ? null : s"
                            >
                              <input
                                type="checkbox"
                                class="fs-cascade-addall-cb"
                                :checked="seriesFullySelected(cascadeBrand, s)"
                                @click.stop="toggleAllInSeries(cascadeBrand, s)"
                              />
                              <span class="fs-cascade-series-name">{{ s }}</span>
                              <i
                                class="bi"
                                :class="cascadeSeries === s ? 'bi-chevron-up' : 'bi-chevron-down'"
                              ></i>
                            </button>

                            <div v-if="cascadeSeries === s" class="fs-cascade-items">
                              <div
                                v-for="p in PRODUCTS.filter(x => x.brand === cascadeBrand && matchSeries(x.name, s))"
                                :key="p.id"
                                class="fs-cascade-item"
                                :class="{ checked: selectedItemPids.includes(p.id) }"
                                @click.stop="toggleProduct(p.id)"
                              >
                                <div class="fs-cascade-check">
                                  <i v-if="selectedItemPids.includes(p.id)"
                                     class="bi bi-check-lg"></i>
                                </div>
                                <div class="fs-cascade-thumb">{{ p.emoji }}</div>
                                <div class="fs-cascade-info">
                                  <strong>{{ p.name }}</strong>
                                  <small>Tồn kho: {{ p.stock }} | {{ formatVND(p.price) }}</small>
                                </div>
                              </div>
                              <div
                                v-if="PRODUCTS.filter(x => x.brand === cascadeBrand && matchSeries(x.name, s)).length === 0"
                                class="fs-cascade-empty"
                              >
                                Không có sản phẩm nào trong dòng này.
                              </div>
                            </div>
                          </div>
                        </div>
                      </template>

                      <div v-else class="fs-cascade-right-empty">
                        <i class="bi bi-arrow-left-circle"></i>
                        <span>Chọn thương hiệu bên trái để xem dòng sản phẩm.</span>
                      </div>
                    </div>
                  </div>

                  <!-- BOTTOM FOOTER -->
                  <div class="fs-cascade-footer">
                    <span class="fs-cascade-footer-count">
                      Đã chọn <strong>{{ selectedItemPids.length }}</strong> sản phẩm
                    </span>
                    <div class="fs-cascade-footer-actions">
                      <button
                        type="button"
                        class="fs-link-btn"
                        :disabled="selectedItemPids.length === 0"
                        @click="removeAllSelected"
                      >
                        <i class="bi bi-x-circle"></i> Xóa chọn
                      </button>
                      <button type="button" class="fs-link-btn primary" @click="closeCascade">
                        Áp dụng
                      </button>
                    </div>
                  </div>
                </div>
              </Transition>
            </div>

            <!-- SELECTED ITEMS -->
            <div v-if="selectedItemPids.length > 0" class="fs-sel-table-wrap mt-3">
              <table class="table fs-sel-table mb-0">
                <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Giá gốc</th>
                  <th>Giá Flash Sale</th>
                  <th>Chiết khấu (%)</th>
                  <th>Số lượng</th>
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
                    <div v-if="qtyError[pid]" class="invalid-feedback">Quá tồn kho</div>
                  </td>
                  <td>
                    <button class="fs-rm-btn" @click="removeProduct(pid)">
                      <i class="bi bi-x"></i>
                    </button>
                  </td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- TAB 1: BY CATEGORY -->
          <div v-show="activeTab === 1" class="fs-tab-content">
            <div class="fs-cat-row">
              <div class="fs-cat-wrap">
                <button
                  type="button"
                  class="fs-cat-trigger"
                  :class="{ active: catOpen, has: activeCategoryId !== null }"
                  @click.stop="catOpen = !catOpen"
                >
                  <i class="bi bi-grid-3x3-gap"></i>
                  <span class="fs-cat-trigger-text">
                    <template v-if="activeCategoryId !== null">
                      <i class="bi bi-tag-fill fs-cat-tag"></i>
                      {{ getCategoryName(activeCategoryId) }}
                    </template>
                    <template v-else>
                      Chọn danh mục toàn bộ...
                    </template>
                  </span>
                  <i class="bi bi-chevron-down fs-cat-chevron"></i>
                </button>
                <Transition name="fs-panel">
                  <ul v-if="catOpen" class="fs-cat-panel" @click.stop>
                    <li
                      v-for="c in CATEGORIES"
                      :key="c.id"
                      class="fs-cat-item"
                      :class="{ active: activeCategoryId === c.id }"
                      @click="selectCategory(c.id)"
                    >
                      <i class="bi bi-grid-3x3-gap"></i>
                      <span>{{ c.name }}</span>
                      <small>{{ PRODUCTS.filter(p => p.categoryId === c.id).length }} SP</small>
                      <i v-if="activeCategoryId === c.id" class="bi bi-check2 fs-cat-check"></i>
                    </li>
                  </ul>
                </Transition>
              </div>
              <button
                v-if="selectedItemPids.length > 0"
                type="button"
                class="fs-link-btn"
                @click="removeAllSelected"
              >
                <i class="bi bi-x-circle"></i> Xóa chọn
              </button>
            </div>
            <div v-if="activeCategoryId !== null" class="fs-cat-summary">
              Đã thêm <strong>{{ lastCategoryCount }}</strong> sản phẩm từ danh mục
              <strong>{{ getCategoryName(activeCategoryId) }}</strong>.
            </div>

            <!-- SELECTED ITEMS (same table as Tab 0) -->
            <div v-if="selectedItemPids.length > 0" class="fs-sel-table-wrap mt-3">
              <table class="table fs-sel-table mb-0">
                <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Giá gốc</th>
                  <th>Giá Flash Sale</th>
                  <th>Chiết khấu (%)</th>
                  <th>Số lượng</th>
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
                    <div v-if="qtyError[pid]" class="invalid-feedback">Quá tồn kho</div>
                  </td>
                  <td>
                    <button class="fs-rm-btn" @click="removeProduct(pid)">
                      <i class="bi bi-x"></i>
                    </button>
                  </td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-soft" @click="resetForm">
              <i class="bi bi-arrow-counterclockwise"></i>Làm mới
            </button>

            <button type="submit" class="btn btn-primary-action" :disabled="saving">
              <i class="bi bi-check2-circle"></i>
              {{ saving ? 'Đang lưu...' : 'Lưu chiến dịch' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- DELETE CONFIRM -->
    <div
      class="modal-backdrop-custom"
      v-if="showDelModal"
      @click.self="closeDelModal"
    >
      <div class="flashsale-modal flashsale-modal--sm">
        <div class="modal-head">
          <div>
            <h2>Xóa chiến dịch này?</h2>
            <p>Hành động này không thể hoàn tác.</p>
          </div>
          <button class="modal-close-btn" @click="closeDelModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="voucher-form">
          <div class="form-section">
            <div class="section-title">
              <span><i class="bi bi-trash3"></i></span>
              <div>
                <h3>{{ delTarget?.name }}</h3>
                <p>Chiến dịch sẽ bị xóa vĩnh viễn khỏi hệ thống.</p>
              </div>
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="btn-soft" @click="closeDelModal">
              <i class="bi bi-x-circle"></i>Hủy bỏ
            </button>
            <button type="button" class="btn-primary-action" style="background:#dc3545"
                    @click="confirmDel">
              <i class="bi bi-trash3"></i>Xóa ngay
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import {computed, reactive, ref, watch, onMounted, onUnmounted, nextTick} from 'vue'
import '@/assets/css/FlashSale.css'

/* ── MOCK DATA MODE ── */
// eslint-disable-next-line no-unused-vars
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
      {
        skuId: 1,
        productName: 'iPhone 15 Pro Max 256GB',
        originalPrice: 34990000,
        flashSalePrice: 29990000,
        flashSaleQuantity: 50,
        soldQuantity: 23
      },
      {
        skuId: 2,
        productName: 'iPhone 15 Pro 128GB',
        originalPrice: 27990000,
        flashSalePrice: 23990000,
        flashSaleQuantity: 30,
        soldQuantity: 18
      },
    ]
  },
  {
    slotId: 2,
    name: 'Flash Sale Cuoi Tuan - Samsung Galaxy',
    startDate: d(2),
    endDate: d(3),
    status: 1, // SCHEDULED
    items: [
      {
        skuId: 3,
        productName: 'Samsung Galaxy S24 Ultra',
        originalPrice: 29990000,
        flashSalePrice: 25990000,
        flashSaleQuantity: 25,
        soldQuantity: 0
      },
      {
        skuId: 4,
        productName: 'Samsung Galaxy S24+',
        originalPrice: 21990000,
        flashSalePrice: 18990000,
        flashSaleQuantity: 40,
        soldQuantity: 0
      },
    ]
  },
  {
    slotId: 3,
    name: 'Flash Sale Sang Thu 2 - Xiaomi',
    startDate: d(5),
    endDate: d(6),
    status: 1, // UPCOMING (startDate > now)
    items: [
      {
        skuId: 5,
        productName: 'Xiaomi 14 Ultra',
        originalPrice: 22990000,
        flashSalePrice: 19990000,
        flashSaleQuantity: 35,
        soldQuantity: 0
      },
    ]
  },
  {
    slotId: 4,
    name: 'Flash Sale Tuan Truoc - OPPO',
    startDate: d(-3),
    endDate: d(-2),
    status: 3, // ENDED
    items: [
      {
        skuId: 6,
        productName: 'OPPO Find X7 Pro',
        originalPrice: 19990000,
        flashSalePrice: 16990000,
        flashSaleQuantity: 60,
        soldQuantity: 60
      },
    ]
  },
  {
    slotId: 5,
    name: 'Flash Sale Da Huy - Vivo',
    startDate: d(-1),
    endDate: d(1),
    status: 4, // CANCELLED
    items: [
      {
        skuId: 7,
        productName: 'Vivo X100 Pro',
        originalPrice: 17990000,
        flashSalePrice: 14990000,
        flashSaleQuantity: 20,
        soldQuantity: 0
      },
    ]
  },
  {
    slotId: 6,
    name: 'Flash Sale Cho Xu Ly - Google Pixel',
    startDate: null,
    endDate: null,
    status: 0, // PENDING
    items: [
      {
        skuId: 8,
        productName: 'Google Pixel 8 Pro',
        originalPrice: 22990000,
        flashSalePrice: 19990000,
        flashSaleQuantity: 15,
        soldQuantity: 0
      },
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
  localSlots.value.map(s => ({...s, resolvedStatus: resolveStatus(s)}))
)

/* ── FILTERS ── */
const filters = reactive({
  keyword: '',
  status: 'ALL',
})

watch(
  () => [filters.keyword, filters.status],
  () => {
  }
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
    ACTIVE: 'Đang diễn ra',
    SCHEDULED: 'Đã lên lịch',
    UPCOMING: 'Sắp diễn ra',
    ENDED: 'Đã kết thúc',
    CANCELLED: 'Đã hủy',
    PENDING: 'Chờ xử lý',
  }
  return map[slot.resolvedStatus] || slot.resolvedStatus
}

function statusBadgeClass(slot) {
  return {
    ACTIVE: '',
    SCHEDULED: 'warning',
    UPCOMING: 'warning',
    ENDED: 'inactive',
    CANCELLED: 'danger',
    PENDING: 'info',
  }[slot.resolvedStatus] || 'info'
}

/* ── TOGGLE ACTIVE ── */
function toggleActive(slot, checked) {
  const idx = localSlots.value.findIndex(s => s.slotId === slot.slotId)
  if (idx !== -1) {
    localSlots.value[idx] = {
      ...localSlots.value[idx],
      status: checked ? 2 : 3
    }
    showToast({
      type: 'success',
      title: 'Thành công',
      message: checked ? 'Đã kích hoạt chiến dịch.' : 'Đã tắt chiến dịch.'
    })
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

const form = reactive({...defaultForm})

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
      result.time = 'Thời gian kết thúc phải sau thời gian bắt đầu'
    }
  }
  return result
})

/* ── PRODUCT SELECTION ── */
// categoryId: 1 = Điện thoại, 2 = Phụ kiện, 3 = Laptop
const PRODUCTS = [
  {
    id: 1,
    name: 'iPhone 15 Pro Max 256GB',
    emoji: '📱',
    price: 34990000,
    stock: 50,
    brand: 'iphone',
    categoryId: 1
  },
  {
    id: 2,
    name: 'iPhone 15 Pro 128GB',
    emoji: '📱',
    price: 27990000,
    stock: 30,
    brand: 'iphone',
    categoryId: 1
  },
  {
    id: 3,
    name: 'iPhone 14 Pro Max 256GB',
    emoji: '📱',
    price: 28990000,
    stock: 25,
    brand: 'iphone',
    categoryId: 1
  },
  {
    id: 4,
    name: 'iPhone 14 128GB',
    emoji: '📱',
    price: 19990000,
    stock: 40,
    brand: 'iphone',
    categoryId: 1
  },
  {
    id: 5,
    name: 'Samsung Galaxy S24 Ultra',
    emoji: '📲',
    price: 29990000,
    stock: 30,
    brand: 'samsung',
    categoryId: 1
  },
  {
    id: 6,
    name: 'Samsung Galaxy S24+',
    emoji: '📲',
    price: 21990000,
    stock: 40,
    brand: 'samsung',
    categoryId: 1
  },
  {
    id: 7,
    name: 'Samsung Galaxy A55',
    emoji: '📲',
    price: 9990000,
    stock: 60,
    brand: 'samsung',
    categoryId: 1
  },
  {
    id: 8,
    name: 'Xiaomi 14 Ultra',
    emoji: '🖤',
    price: 22990000,
    stock: 45,
    brand: 'xiaomi',
    categoryId: 1
  },
  {
    id: 9,
    name: 'OPPO Find X7 Pro',
    emoji: '🟢',
    price: 19990000,
    stock: 25,
    brand: 'oppo',
    categoryId: 1
  },
  {
    id: 10,
    name: 'Vivo X100 Pro',
    emoji: '🔵',
    price: 17990000,
    stock: 60,
    brand: 'vivo',
    categoryId: 1
  },
  {
    id: 11,
    name: 'Google Pixel 8 Pro',
    emoji: '🌈',
    price: 22990000,
    stock: 20,
    brand: 'google',
    categoryId: 1
  },
  {
    id: 12,
    name: 'Tai nghe Bluetooth AirPro',
    emoji: '🎧',
    price: 1490000,
    stock: 100,
    brand: 'iphone',
    categoryId: 2
  },
  {
    id: 13,
    name: 'Sac nhanh 65W Type-C',
    emoji: '🔌',
    price: 590000,
    stock: 80,
    brand: 'samsung',
    categoryId: 2
  },
  {
    id: 14,
    name: 'MacBook Air M3 13"',
    emoji: '💻',
    price: 27990000,
    stock: 15,
    brand: 'iphone',
    categoryId: 3
  },
]

// Level-1 (parent) = Brand, Level-2 (child) = Series dẽ từ tên SP
const BRAND_SERIES = [
  {
    key: 'iphone',
    label: 'iPhone',
    icon: 'bi-apple',
    series: ['iPhone 15 Series', 'iPhone 14 Series']
  },
  {
    key: 'samsung',
    label: 'Samsung',
    icon: 'bi-phone',
    series: ['Galaxy S24 Series', 'Galaxy A Series']
  },
  {key: 'xiaomi', label: 'Xiaomi', icon: 'bi-phone-fill', series: ['Xiaomi 14 Series']},
  {key: 'oppo', label: 'OPPO', icon: 'bi-phone-vibrate', series: ['Find X Series']},
  {key: 'vivo', label: 'Vivo', icon: 'bi-phone', series: ['X Series']},
  {key: 'google', label: 'Google', icon: 'bi-google', series: ['Pixel Series']},
]

const CATEGORIES = [
  {id: 1, name: 'Điện thoại'},
  {id: 2, name: 'Phụ kiện'},
  {id: 3, name: 'Laptop'},
]

function matchSeries(productName, seriesLabel) {
  const slug = seriesLabel.toLowerCase().replace(/\s*series$/, '').trim()
  return productName.toLowerCase().includes(slug)
}

const selItems = reactive({})
const selectedItemPids = ref([])
const qtyError = reactive({})

/* ── CASCADING PICKER (Tab 0) ── */
const cascadeOpen = ref(false)
const cascadeBrand = ref(null)
const cascadeSeries = ref(null)

/* ── CATEGORY PICKER (Tab 1) ── */
const activeCategoryId = ref(null)
const catOpen = ref(false)
const lastCategoryCount = ref(0)

function getProductName(pid) {
  return PRODUCTS.find((p) => p.id === pid)?.name || `SP #${pid}`
}

function getProductPrice(pid) {
  return PRODUCTS.find((p) => p.id === pid)?.price ?? 0
}

function getProductStock(pid) {
  return PRODUCTS.find((p) => p.id === pid)?.stock ?? 0
}

function addProduct(pid) {
  if (selectedItemPids.value.includes(pid)) return
  selectedItemPids.value.push(pid)
  const p = PRODUCTS.find((x) => x.id === pid)
  selItems[pid] = {
    discountPercent: 15,
    flashSalePrice: Math.round((p?.price ?? 0) * 0.85),
    flashSaleQuantity: 10
  }
}

function removeProduct(pid) {
  const idx = selectedItemPids.value.indexOf(pid)
  if (idx > -1) selectedItemPids.value.splice(idx, 1)
  delete selItems[pid]
  delete qtyError[pid]
}

function toggleProduct(pid) {
  if (selectedItemPids.value.includes(pid)) {
    removeProduct(pid)
  } else {
    addProduct(pid)
  }
}

function removeAllSelected() {
  selectedItemPids.value = []
  Object.keys(selItems).forEach((k) => delete selItems[k])
  Object.keys(qtyError).forEach((k) => delete qtyError[k])
}

function closeCascade() {
  cascadeOpen.value = false
}

function setCascadeBrand(key) {
  cascadeBrand.value = key
  cascadeSeries.value = null
}

function selectAllInBrand(brandKey) {
  PRODUCTS.filter((p) => p.brand === brandKey).forEach((p) => addProduct(p.id))
}

function toggleAllInBrand(brandKey) {
  const list = PRODUCTS.filter((p) => p.brand === brandKey)
  if (brandFullySelected(brandKey)) {
    list.forEach((p) => removeProduct(p.id))
  } else {
    list.forEach((p) => addProduct(p.id))
  }
}

function toggleAllInSeries(brandKey, seriesLabel) {
  const list = PRODUCTS.filter(
    (p) => p.brand === brandKey && matchSeries(p.name, seriesLabel)
  )
  if (seriesFullySelected(brandKey, seriesLabel)) {
    list.forEach((p) => removeProduct(p.id))
  } else {
    list.forEach((p) => addProduct(p.id))
  }
}

function brandFullySelected(brandKey) {
  const list = PRODUCTS.filter((p) => p.brand === brandKey)
  return list.length > 0 && list.every((p) => selectedItemPids.value.includes(p.id))
}

function seriesFullySelected(brandKey, seriesLabel) {
  const list = PRODUCTS.filter(
    (p) => p.brand === brandKey && matchSeries(p.name, seriesLabel)
  )
  return list.length > 0 && list.every((p) => selectedItemPids.value.includes(p.id))
}

function selectCategory(catId) {
  activeCategoryId.value = catId
  catOpen.value = false
  const list = PRODUCTS.filter((p) => p.categoryId === catId)
  let added = 0
  list.forEach((p) => {
    if (!selectedItemPids.value.includes(p.id)) {
      addProduct(p.id)
      added++
    }
  })
  lastCategoryCount.value = added
  if (added > 0) {
    showToast({
      type: 'success',
      title: 'Thành công',
      message: `Đã thêm ${added} sản phẩm từ danh mục.`
    })
  }
}

function getCategoryName(catId) {
  return CATEGORIES.find((c) => c.id === catId)?.name || ''
}

function onDiscountChange(pid, value) {
  const disc = parseFloat(value) || 0
  const orig = getProductPrice(pid)
  const fp = Math.round(orig * (1 - disc / 100))
  selItems[pid] = {...selItems[pid], discountPercent: disc, flashSalePrice: fp}
}

function onPriceChange(pid, value) {
  const fp = parseFloat(value) || 0
  const orig = getProductPrice(pid)
  const disc = orig > 0 ? parseFloat(((1 - fp / orig) * 100).toFixed(1)) : 0
  selItems[pid] = {...selItems[pid], flashSalePrice: fp, discountPercent: disc}
}

function onQtyChange(pid, value) {
  const v = parseInt(value) || 0
  const stock = getProductStock(pid)
  qtyError[pid] = v > stock
  selItems[pid] = {...selItems[pid], flashSaleQuantity: v}
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
  cascadeOpen.value = false
  cascadeBrand.value = null
  cascadeSeries.value = null
  catOpen.value = false
  activeCategoryId.value = null
  lastCategoryCount.value = 0
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
    showToast({type: 'error', title: 'Loi', message: 'Vui long chon it nhat 1 san pham.'})
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
    showToast({type: 'error', title: 'Loi', message: 'Kiem tra lai so luong san pham.'})
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
      localSlots.value[idx] = {...localSlots.value[idx], ...payload}
    }
    showToast({type: 'success', title: 'Thành công', message: 'Cập nhật Flash Sale thành công!'})
  } else {
    const newSlot = {
      ...payload,
      slotId: Math.max(...localSlots.value.map(s => s.slotId), 0) + 1,
    }
    localSlots.value.unshift(newSlot)
    showToast({type: 'success', title: 'Thành công', message: 'Tạo Flash Sale thành công!'})
  }

  saving.value = false
  closeModal()
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
  showToast({type: 'success', title: 'Thành công', message: 'Đã xóa Flash Sale.'})
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

function showToast({type = 'success', title, message}) {
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

// Close dropdowns on outside click / Escape
onMounted(() => {
  document.addEventListener('click', (e) => {
    const inCascade = e.target.closest('.fs-cascade-wrap')
    const inCat = e.target.closest('.fs-cat-wrap')
    if (!inCascade) cascadeOpen.value = false
    if (!inCat) catOpen.value = false
  })
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      cascadeOpen.value = false
      catOpen.value = false
    }
  })
})
</script>

<style scoped>
</style>
