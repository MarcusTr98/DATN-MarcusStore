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
            <tr v-for="(slot, index) in slotsWithStatus" :key="slot.slotId">
              <td><span class="id-text">#{{ currentPage * pageSize + index + 1 }}</span></td>
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
                <span class="item-count">{{ slot.quantityFlashSaleSlot ?? 0 }}</span>
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
        <div v-if="!loading && slots.length === 0" class="empty-state">
          <i class="bi bi-lightning"></i>
          <p>Chưa có Flash Sale nào</p>
          <span class="fs-hint-text">Tạo chiến dịch Flash Sale đầu tiên để bắt đầu.</span>
        </div>

        <!-- LOADING -->
        <div v-if="loading" class="empty-state">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="mt-2">Đang tải...</p>
        </div>

        <!-- PAGINATION -->
        <div v-if="pagination.totalPages > 0" class="flashsale-pagination">
          <div class="pagination-summary">
            Tổng <strong>{{ pagination.totalElements }}</strong> chiến dịch
          </div>
          <div class="pagination-controls">
            <label class="page-size-control">
              <span>Hiển thị</span>
              <select v-model.number="pageSize" class="form-select form-select-sm">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
            </label>
            <button
              type="button"
              class="pagination-button"
              :disabled="currentPage === 0"
              @click="goToPage(currentPage - 1)"
            >
              Trước
            </button>
            <span class="page-indicator">
              Trang <strong>{{ currentPage + 1 }}</strong> / {{ pagination.totalPages }}
            </span>
            <button
              type="button"
              class="pagination-button"
              :disabled="currentPage + 1 >= pagination.totalPages"
              @click="goToPage(currentPage + 1)"
            >
              Sau
            </button>
          </div>
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
              <div class="voucher-code-control">
                <input
                  v-model.trim="form.name"
                  type="text"
                  class="form-control voucher-code-input"
                  :class="{ 'is-invalid': submitted && errors.name }"
                  placeholder="VD: Flash Sale Thứ 6 - iPhone Series"
                  maxlength="100"
                />
                <button type="button" class="generate-code-btn" @click="generateSlotName">
                  <i class="bi bi-stars"></i>
                  Tạo tự động
                </button>
              </div>
              <div class="voucher-code-footer">
                <span
                  class="voucher-char-count"
                  :class="{ 'near-limit': nameLength >= 90, 'at-limit': nameLength >= 100 }"
                >
                  {{ nameLength }}/100 ký tự
                </span>
                <span v-if="nameLength >= 100" class="voucher-char-warning">
                  <i class="bi bi-exclamation-circle"></i>
                  Tên chiến dịch tối đa 100 ký tự
                </span>
              </div>
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


          <!-- ẢNH BANNER SLOT -->
          <div class="form-section">
            <div class="section-title">
              <span>2</span>
              <div>
                <h3>Ảnh banner Flash Sale</h3>
                <p>Ảnh sẽ được overlay lên ảnh sản phẩm khi hiển thị ở trang chủ.</p>
              </div>
            </div>

            <div class="row g-3">
              <!-- Cột trái: chọn file + toggle URL -->
              <div class="col-12 col-md-6">
                <!-- TAB SWITCH: chọn file / paste URL -->
                <div class="fs-banner-input-mode">
                  <button
                    type="button"
                    class="fs-banner-mode-btn"
                    :class="{ active: bannerInputMode === 'file' }"
                    @click="switchBannerMode('file')"
                  >
                    <i class="bi bi-upload"></i> Tải ảnh lên
                  </button>
                  <button
                    type="button"
                    class="fs-banner-mode-btn"
                    :class="{ active: bannerInputMode === 'url' }"
                    @click="switchBannerMode('url')"
                  >
                    <i class="bi bi-link-45deg"></i> Dán URL
                  </button>
                </div>

                <!-- MODE: FILE UPLOAD -->
                <div v-show="bannerInputMode === 'file'" class="fs-banner-file-zone">
                  <label class="fs-banner-file-label">
                    <i class="bi bi-cloud-arrow-up"></i>
                    <span v-if="!bannerFileName">Chọn ảnh từ thiết bị</span>
                    <span v-else class="fs-banner-file-name">{{ bannerFileName }}</span>
                    <small>PNG, JPG, WEBP — tối đa 2MB</small>
                    <input
                      type="file"
                      accept="image/png,image/jpeg,image/jpg,image/webp"
                      hidden
                      @change="onBannerFileChange"
                    />
                  </label>
                </div>

                <!-- MODE: PASTE URL -->
                <div v-show="bannerInputMode === 'url'">
                  <label class="form-label">URL ảnh banner <span class="text-danger">*</span></label>
                  <div class="input-group">
                    <span class="input-group-text">
                      <i class="bi bi-image"></i>
                    </span>
                    <input
                      v-model.trim="form.bannerUrl"
                      type="text"
                      class="form-control"
                      :class="{ 'is-invalid': submitted && errors.bannerUrl }"
                      placeholder="https://example.com/banner-flashsale.png"
                      @input="onBannerUrlChange"
                    />
                  </div>
                  <div v-if="submitted && errors.bannerUrl" class="invalid-feedback d-block">
                    {{ errors.bannerUrl }}
                  </div>
                </div>

                <small class="form-text text-muted mt-2 d-block">
                  Khuyến nghị: ảnh vuông hoặc PNG trong suốt, kích thước 400×400 px trở lên.
                </small>
              </div>

              <!-- Cột phải: preview ảnh -->
              <div class="col-12 col-md-6">
                <label class="form-label">Xem trước</label>
                <div class="fs-banner-preview-box">
                  <img
                    v-if="form.bannerUrl && !bannerImageError"
                    :src="form.bannerUrl"
                    alt="Banner preview"
                    class="fs-banner-preview-img"
                    @error="onBannerImageError"
                    @load="onBannerImageLoad"
                  />
                  <div v-else class="fs-banner-preview-empty">
                    <i class="bi" :class="bannerImageError ? 'bi-exclamation-triangle' : 'bi-image-alt'"></i>
                    <span>{{ bannerImageError ? 'Không thể tải ảnh từ URL này' : 'Chưa có ảnh banner' }}</span>
                  </div>
                </div>
                <button
                  v-if="form.bannerUrl"
                  type="button"
                  class="fs-banner-clear-btn mt-2"
                  @click="clearBanner"
                >
                  <i class="bi bi-x-circle"></i> Xóa ảnh
                </button>
              </div>
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
                  <!-- LOADING -->
                  <div v-if="cascadeLoading" class="fs-cascade-loading">
                    <div class="spinner-border text-primary" role="status"></div>
                    <p class="mt-2 mb-0">Đang tải cây sản phẩm...</p>
                  </div>

                  <!-- LỖI -->
                  <div v-else-if="cascadeError" class="fs-cascade-empty">
                    <i class="bi bi-exclamation-triangle text-warning"></i>
                    <p class="mt-2 mb-2">{{ cascadeError }}</p>
                    <button
                      type="button"
                      class="btn btn-sm btn-soft"
                      @click="flashSaleStore.fetchCascade({ includeOutOfStock: false })"
                    >
                      <i class="bi bi-arrow-clockwise"></i> Thử lại
                    </button>
                  </div>

                  <!-- EMPTY -->
                  <div v-else-if="cascadeTree.length === 0" class="fs-cascade-empty">
                    Không có sản phẩm nào đang bán.
                  </div>

                  <!-- DATA -->
                  <div v-else class="fs-cascade-cols">
                    <!-- LEFT COLUMN: Brands (~40%) -->
                    <ul class="fs-cascade-left">
                      <li
                        v-for="b in cascadeTree"
                        :key="b.brand"
                        class="fs-cascade-brand"
                        :class="{ active: cascadeBrand === b.brand }"
                        @mouseenter="setCascadeBrand(b.brand)"
                        @click="setCascadeBrand(b.brand)"
                      >
                        <i class="bi bi-phone"></i>
                        <span>{{ b.brand }}</span>
                        <i class="bi bi-chevron-right fs-cascade-arrow"></i>
                      </li>
                    </ul>

                    <!-- RIGHT COLUMN: Categories + SKUs (~60%) -->
                    <div class="fs-cascade-right">
                      <template v-if="cascadeBrand">
                        <div class="fs-cascade-right-hd">
                          <strong>{{ cascadeBrand }}</strong>
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
                            v-for="cat in (cascadeTree.find(x => x.brand === cascadeBrand)?.categories || [])"
                            :key="cat.categoryId"
                            class="fs-cascade-series"
                          >
                            <button
                              type="button"
                              class="fs-cascade-series-btn"
                              :class="{ active: cascadeCategoryId === cat.categoryId }"
                              @click.stop="toggleCategory(cat.categoryId)"
                            >
                              <input
                                type="checkbox"
                                class="fs-cascade-addall-cb"
                                :checked="categoryFullySelected(cascadeBrand, cat.categoryId)"
                                @click.stop="toggleAllInCategory(cascadeBrand, cat.categoryId)"
                              />
                              <span class="fs-cascade-series-name">{{ cat.categoryName }}</span>
                              <i
                                class="bi"
                                :class="cascadeCategoryId === cat.categoryId ? 'bi-chevron-up' : 'bi-chevron-down'"
                              ></i>
                            </button>

                            <div v-if="cascadeCategoryId === cat.categoryId" class="fs-cascade-items">
                              <div
                                v-for="sku in cat.skus"
                                :key="sku.skuId"
                                class="fs-cascade-item"
                                :class="{ checked: selectedItemPids.includes(sku.skuId) }"
                                @click.stop="toggleProduct(sku.skuId)"
                              >
                                <div class="fs-cascade-check">
                                  <i v-if="selectedItemPids.includes(sku.skuId)"
                                     class="bi bi-check-lg"></i>
                                </div>
                                <div class="fs-cascade-thumb">📦</div>
                                <div class="fs-cascade-info">
                                  <strong>{{ sku.productName }}</strong>
                                  <small v-if="sku.attributes"> · {{ sku.attributes }}</small>
                                  <small>Kho: {{ sku.stockQuantity }} | {{ formatVND(sku.originalPrice) }}</small>
                                </div>
                              </div>
                              <div
                                v-if="cat.skus.length === 0"
                                class="fs-cascade-empty"
                              >
                                Danh mục này hiện chưa có SKU nào còn hàng.
                              </div>
                            </div>
                          </div>
                        </div>
                      </template>

                      <div v-else class="fs-cascade-right-empty">
                        <i class="bi bi-arrow-left-circle"></i>
                        <span>Chọn thương hiệu bên trái để xem danh mục.</span>
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
                      v-for="c in uniqueCategories"
                      :key="c.categoryId"
                      class="fs-cat-item"
                      :class="{ active: activeCategoryId === c.categoryId }"
                      @click="selectCategory(c.categoryId)"
                    >
                      <i class="bi bi-grid-3x3-gap"></i>
                      <span>{{ c.categoryName }}</span>
                      <small>{{ c.count }} SP</small>
                      <i v-if="activeCategoryId === c.categoryId" class="bi bi-check2 fs-cat-check"></i>
                    </li>
                    <li v-if="uniqueCategories.length === 0" class="fs-cat-empty">
                      Chưa tải được danh mục nào.
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
import { storeToRefs } from 'pinia'
import { useFlashSaleStore } from '@/stores/flashSaleStore'
import '@/assets/css/FlashSale.css'

const flashSaleStore = useFlashSaleStore()
const {
  slots, loading, error, fieldErrors, pagination, stats,
  cascadeTree, cascadeLoading, cascadeError,
} = storeToRefs(flashSaleStore)

const now = new Date()

/* ── FILTERS ── */
const filters = reactive({
  keyword: '',
  status: 'ALL',
})

function resetFilters() {
  filters.keyword = ''
  filters.status = 'ALL'
}

/* ── PAGINATION ── */
const currentPage = ref(0)
const pageSize = ref(5)

function buildSlotQuery() {
  return {
    page: currentPage.value,
    size: pageSize.value,
    keyword: filters.keyword || undefined,
    status: filters.status === 'ALL' ? undefined : filters.status,
  }
}

function loadSlots() {
  return flashSaleStore.fetchSlots(buildSlotQuery())
}

function goToPage(page) {
  if (page < 0 || page >= pagination.value.totalPages) {
    return
  }
  currentPage.value = page
  loadSlots()
}

watch(
  () => [filters.keyword, filters.status],
  () => {
    currentPage.value = 0
    loadSlots()
  }
)

watch(pageSize, () => {
  currentPage.value = 0
  loadSlots()
})
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

const statusPriority = {
  ACTIVE: 1,
  UPCOMING: 2,
  SCHEDULED: 3,
  ENDED: 4,
  CANCELLED: 5,
  PENDING: 6,
}

const slotsWithStatus = computed(() =>
  [...slots.value]
    .map(s => ({ ...s, resolvedStatus: resolveStatus(s) }))
    .sort((a, b) => {
      const pa = statusPriority[a.resolvedStatus] ?? 99
      const pb = statusPriority[b.resolvedStatus] ?? 99
      if (pa !== pb) return pa - pb
      // cùng nhóm thì slot bắt đầu sớm hơn lên trước
      return new Date(a.startDate) - new Date(b.startDate)
    })
)

const filteredSlots = computed(() => {
  return slotsWithStatus.value
})

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


function toggleActive(slot, checked) {
  const idx = slots.value.findIndex(s => s.slotId === slot.slotId)
  if (idx !== -1) {
    slots.value[idx] = {
      ...slots.value[idx],
      status: checked ? 2 : 3
    }
    showToast({
      type: 'success',
      title: 'Thành công',
      message: checked ? 'Đã kích hoạt chiến dịch.' : 'Đã tắt chiến dịch.'
    })
  }
}

const isModalOpen = ref(false)
const isEditing = ref(false)
const activeTab = ref(0)
const editSlotId = ref(null)

const defaultForm = {
  name: '',
  status: 1,
  startDate: '',
  endDate: '',
  bannerUrl: '',
}

const form = reactive({...defaultForm})

const nameLength = computed(() => (form.name || '').length)

/**
 * Sinh tên Flash Sale tự động theo format:
 *   FLASH-YYMMDD-HHMM-<RANDOM4>
 *   VD: FLASH-260703-2030-A8K2
 * - Bắt đầu bằng prefix FLASH
 * - Có ngày tạo (YYMMDD) để dễ tra cứu
 * - Có HHMM để tránh trùng khi tạo nhiều slot cùng ngày
 * - Random 4 ký tự alphanumeric để chắc chắn unique
 * Ngắn gọn, dễ nhìn, giới hạn < 100 ký tự.
 */
function generateSlotName() {
  const d = new Date()
  const yy = String(d.getFullYear()).slice(-2)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  const rand = Math.random().toString(36).slice(2, 6).toUpperCase()
  form.name = `FLASH-${yy}${mm}${dd}-${hh}${mi}-${rand}`
  submitted.value = false
}
const submitted = ref(false)
const saving = ref(false)
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
  if (!form.bannerUrl.trim()) {
    result.bannerUrl = 'Vui long nhap URL anh banner'
  } else if (!isValidUrl(form.bannerUrl)) {
    result.bannerUrl = 'URL anh khong hop le'
  }
  return result
})

function isValidUrl(value) {
  if (!value) return false
  try {
    const u = new URL(value)
    return u.protocol === 'http:' || u.protocol === 'https:'
  } catch {
    return false
  }
}

const bannerImageError = ref(false)
const bannerInputMode = ref('file') // 'file' | 'url'
const bannerFileName = ref('')

const MAX_BANNER_SIZE = 2 * 1024 * 1024 // 2MB
const ALLOWED_BANNER_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp']

function onBannerUrlChange() {
  // Reset trạng thái lỗi ảnh khi URL thay đổi
  bannerImageError.value = false
}

function onBannerImageError() {
  bannerImageError.value = true
}

function onBannerImageLoad() {
  bannerImageError.value = false
}

function clearBanner() {
  form.bannerUrl = ''
  bannerImageError.value = false
  bannerFileName.value = ''
}

function switchBannerMode(mode) {
  bannerInputMode.value = mode
  // Reset trạng thái khi đổi mode
  bannerImageError.value = false
  bannerFileName.value = ''
  form.bannerUrl = ''
}

function onBannerFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  // Validate loại file
  if (!ALLOWED_BANNER_TYPES.includes(file.type)) {
    showToast({
      type: 'error',
      title: 'Lỗi',
      message: 'Chỉ chấp nhận file PNG, JPG, WEBP.',
    })
    event.target.value = ''
    return
  }

  // Validate dung lượng
  if (file.size > MAX_BANNER_SIZE) {
    showToast({
      type: 'error',
      title: 'Lỗi',
      message: 'Dung lượng ảnh tối đa 2MB.',
    })
    event.target.value = ''
    return
  }

  bannerFileName.value = file.name
  bannerImageError.value = false

  // Đọc file thành data URL (base64) để set vào form.bannerUrl
  const reader = new FileReader()
  reader.onload = (e) => {
    form.bannerUrl = e.target.result
  }
  reader.onerror = () => {
    showToast({
      type: 'error',
      title: 'Lỗi',
      message: 'Không thể đọc file ảnh.',
    })
    bannerFileName.value = ''
  }
  reader.readAsDataURL(file)
}



/* ── CASCADE TREE (load từ BE) ── */
// cascadeTree, cascadeLoading, cascadeError đã lấy từ store

/* Trải phẳng toàn bộ SKU trong cây thành mảng, gắn thêm brand & category. */
function flattenSkus(tree) {
  const result = []
  for (const brandNode of tree || []) {
    for (const catNode of brandNode.categories || []) {
      for (const sku of catNode.skus || []) {
        result.push({
          ...sku,
          brand: brandNode.brand,
          categoryId: catNode.categoryId,
          categoryName: catNode.categoryName,
        })
      }
    }
  }
  return result
}

/* Computed: SKU phẳng + map tra cứu nhanh theo skuId */
const flatSkus = computed(() => flattenSkus(cascadeTree.value))
const skuMap = computed(() => {
  const m = new Map()
  flatSkus.value.forEach((s) => m.set(s.skuId, s))
  return m
})

/* Computed: danh sách category L2 duy nhất + đếm SKU, dùng cho Tab "Theo danh mục". */
const uniqueCategories = computed(() => {
  const map = new Map()
  for (const s of flatSkus.value) {
    if (!map.has(s.categoryId)) {
      map.set(s.categoryId, { categoryId: s.categoryId, categoryName: s.categoryName, count: 0 })
    }
    map.get(s.categoryId).count += 1
  }
  return Array.from(map.values()).sort((a, b) => a.categoryName.localeCompare(b.categoryName))
})

function getSku(skuId) {
  return skuMap.value.get(skuId)
}
function skusInBrand(brandName) {
  return flatSkus.value.filter((s) => s.brand === brandName)
}
function skusInCategory(brandName, categoryId) {
  return flatSkus.value.filter(
    (s) => s.brand === brandName && s.categoryId === categoryId
  )
}

const selItems = reactive({})
const selectedItemPids = ref([])
const qtyError = reactive({})

/* ── CASCADING PICKER (Tab 0) ── */
const cascadeOpen = ref(false)
const cascadeBrand = ref(null)
const cascadeCategoryId = ref(null)

/* ── CATEGORY PICKER (Tab 1) ── */
const activeCategoryId = ref(null)
const catOpen = ref(false)
const lastCategoryCount = ref(0)

function getProductName(skuId) {
  return getSku(skuId)?.productName || `SKU #${skuId}`
}

function getProductPrice(skuId) {
  return getSku(skuId)?.originalPrice ?? 0
}

function getProductStock(skuId) {
  return getSku(skuId)?.stockQuantity ?? 0
}

function addProduct(skuId) {
  if (selectedItemPids.value.includes(skuId)) return
  selectedItemPids.value.push(skuId)
  const sku = getSku(skuId)
  selItems[skuId] = {
    discountPercent: 15,
    flashSalePrice: Math.round((sku?.originalPrice ?? 0) * 0.85),
    flashSaleQuantity: 1,
  }
}

function removeProduct(skuId) {
  const idx = selectedItemPids.value.indexOf(skuId)
  if (idx > -1) selectedItemPids.value.splice(idx, 1)
  delete selItems[skuId]
  delete qtyError[skuId]
}

function toggleProduct(skuId) {
  if (selectedItemPids.value.includes(skuId)) {
    removeProduct(skuId)
  } else {
    addProduct(skuId)
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
  cascadeCategoryId.value = null
}

function toggleCategory(catId) {
  cascadeCategoryId.value = cascadeCategoryId.value === catId ? null : catId
}

function selectAllInBrand(brandKey) {
  skusInBrand(brandKey).forEach((s) => addProduct(s.skuId))
}

function toggleAllInBrand(brandKey) {
  const list = skusInBrand(brandKey)
  if (brandFullySelected(brandKey)) {
    list.forEach((s) => removeProduct(s.skuId))
  } else {
    list.forEach((s) => addProduct(s.skuId))
  }
}

function toggleAllInCategory(brandKey, catId) {
  const list = skusInCategory(brandKey, catId)
  if (categoryFullySelected(brandKey, catId)) {
    list.forEach((s) => removeProduct(s.skuId))
  } else {
    list.forEach((s) => addProduct(s.skuId))
  }
}

function brandFullySelected(brandKey) {
  const list = skusInBrand(brandKey)
  return list.length > 0 && list.every((s) => selectedItemPids.value.includes(s.skuId))
}

function categoryFullySelected(brandKey, catId) {
  const list = skusInCategory(brandKey, catId)
  return list.length > 0 && list.every((s) => selectedItemPids.value.includes(s.skuId))
}

function selectCategory(catId) {
  activeCategoryId.value = catId
  catOpen.value = false
  const list = flatSkus.value.filter((s) => s.categoryId === catId)
  let added = 0
  list.forEach((s) => {
    if (!selectedItemPids.value.includes(s.skuId)) {
      addProduct(s.skuId)
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
  const found = flatSkus.value.find((s) => s.categoryId === catId)
  return found?.categoryName || `Danh mục #${catId}`
}

function onDiscountChange(skuId, value) {
  const disc = parseFloat(value) || 0
  const orig = getProductPrice(skuId)
  const fp = Math.round(orig * (1 - disc / 100))
  selItems[skuId] = {...selItems[skuId], discountPercent: disc, flashSalePrice: fp}
}

function onPriceChange(skuId, value) {
  const fp = parseFloat(value) || 0
  const orig = getProductPrice(skuId)
  const disc = orig > 0 ? parseFloat(((1 - fp / orig) * 100).toFixed(1)) : 0
  selItems[skuId] = {...selItems[skuId], flashSalePrice: fp, discountPercent: disc}
}

function onQtyChange(skuId, value) {
  const v = parseInt(value) || 0
  const stock = getProductStock(skuId)
  qtyError[skuId] = v > stock
  selItems[skuId] = {...selItems[skuId], flashSaleQuantity: v}
}


function openCreateModal() {
  editSlotId.value = null
  isEditing.value = false
  resetForm()
  isModalOpen.value = true
  activeTab.value = 0
  flashSaleStore.fetchCascade({ includeOutOfStock: false })
}

function openEditModal(slot) {
  editSlotId.value = slot.slotId
  isEditing.value = true
  resetForm(false)
  form.name = slot.name
  form.startDate = toLocalDatetime(slot.startDate)
  form.endDate = toLocalDatetime(slot.endDate)
  form.status = slot.status
  form.bannerUrl = slot.imageUrl || slot.bannerUrl || ''
  bannerInputMode.value = 'url'

  selectedItemPids.value = []
  Object.keys(selItems).forEach((k) => delete selItems[k])
  isModalOpen.value = true
  activeTab.value = 0
  flashSaleStore.fetchCascade({ includeOutOfStock: false })
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
  cascadeCategoryId.value = null
  catOpen.value = false
  activeCategoryId.value = null
  lastCategoryCount.value = 0
  bannerImageError.value = false
  bannerInputMode.value = 'file'
  bannerFileName.value = ''
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
  pids.forEach((skuId) => {
    const it = selItems[skuId]
    if (!it || it.flashSaleQuantity <= 0) {
      hasQtyErr = true
    }
    if ((it?.flashSaleQuantity ?? 0) > getProductStock(skuId)) {
      qtyError[skuId] = true
      hasQtyErr = true
    }
  })
  if (hasQtyErr) {
    showToast({type: 'error', title: 'Loi', message: 'Kiem tra lai so luong san pham.'})
    return
  }

  saving.value = true

  const items = pids.map((skuId) => {
    const it = selItems[skuId]
    const sku = getSku(skuId)
    return {
      skuId,
      productName: sku?.productName || `SKU #${skuId}`,
      originalPrice: sku?.originalPrice ?? it.flashSalePrice,
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
    bannerUrl: form.bannerUrl.trim(),
    items,
  }


  await new Promise(r => setTimeout(r, 300))

  if (isEditing.value) {
    const idx = slots.value.findIndex(s => s.slotId === editSlotId.value)
    if (idx !== -1) {
      slots.value[idx] = {...slots.value[idx], ...payload}
    }
    showToast({type: 'success', title: 'Thành công', message: 'Cập nhật Flash Sale thành công!'})
  } else {
    const newSlot = {
      ...payload,
      slotId: Math.max(...slots.value.map(s => s.slotId), 0) + 1,
    }
    slots.value.unshift(newSlot)
    showToast({type: 'success', title: 'Thành công', message: 'Tạo Flash Sale thành công!'})
  }

  saving.value = false
  closeModal()
}



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
  slots.value = slots.value.filter(s => s.slotId !== delTarget.value.slotId)
  showToast({type: 'success', title: 'Thành công', message: 'Đã xóa Flash Sale.'})
  closeDelModal()
}


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
function handleDocClick(e) {
  const inCascade = e.target.closest('.fs-cascade-wrap')
  const inCat = e.target.closest('.fs-cat-wrap')
  if (!inCascade) cascadeOpen.value = false
  if (!inCat) catOpen.value = false
}

function handleDocKey(e) {
  if (e.key === 'Escape') {
    cascadeOpen.value = false
    catOpen.value = false
  }
}

onMounted(async () => {
  await loadSlots()
  document.addEventListener('click', handleDocClick)
  document.addEventListener('keydown', handleDocKey)
})

onUnmounted(() => {
  clearTimeout(toastTimer)
  document.removeEventListener('click', handleDocClick)
  document.removeEventListener('keydown', handleDocKey)
})



</script>

<style scoped>
</style>
