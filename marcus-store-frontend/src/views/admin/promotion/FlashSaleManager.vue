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
              <option value="1">Đã lên lịch</option>
              <option value="2">Đang diễn ra</option>
              <option value="3">Đã kết thúc</option>
              <option value="4">Đã hủy</option>
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
              <th>Banner</th>
              <th>Thời gian</th>
              <th class="text-center">Số SP</th>
              <th class="text-center">Đã sử dụng</th>
              <th>Trạng thái</th>
              <th class="text-center">Thao tác</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="(slot, index) in slotsWithStatus" :key="slot.slotId">
              <td><span class="id-text">#{{ currentPage * pageSize + index + 1 }}</span></td>
              <td><span class="slot-name">{{ slot.name }}</span></td>
              <td>
                <div
                  v-if="slot.bannerImageUrl"
                  class="banner-thumb"
                  @click="openBannerPreview(slot.bannerImageUrl)"
                  title="Bấm để xem ảnh lớn"
                >
                  <img :src="slot.bannerImageUrl" alt="banner" />
                </div>
                <span v-else class="text-muted small">—</span>
              </td>
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
              <td class="text-center">
                <span
                  class="item-count"
                  :class="{ 'item-count-used': isSlotExhausted(slot) }"
                  :title="getUsedTitle(slot)"
                >
                  {{ slot.usedQuantity ?? 0 }}
                </span>
              </td>
              <td>
                <span class="status-badge" :class="statusBadgeClass(slot)">
                  {{ statusBadgeLabel(slot) }}
                </span>
              </td>
              <td class="text-center">
                <button
                  class="icon-button"
                  :class="{ 'icon-button-locked': isSlotLocked(slot) }"
                  :title="isSlotLocked(slot) ? 'Xem chi tiết (không thể chỉnh sửa)' : 'Sửa'"
                  @click="openEditModal(slot)"
                >
                  <i :class="isSlotLocked(slot) ? 'bi bi-eye-fill' : 'bi bi-pencil-square'"></i>
                </button>
                <!-- Slot đã ACTIVE/ENDED/CANCELLED/PENDING: đổi nút Xóa thành Xem chi tiết thống kê -->
                <button
                  v-if="isSlotLocked(slot)"
                  class="icon-button icon-button-detail ms-1"
                  title="Xem chi tiết thống kê"
                  @click="openDetailModal(slot)"
                >
                  <i class="bi bi-bar-chart-line-fill"></i>
                </button>
                <!-- Slot SCHEDULED/UPCOMING: nút Hủy chiến dịch (SCHEDULED sẽ chuyển sang CANCELLED, các trạng thái khác sẽ xóa) -->
                <button
                  v-else-if="Number(slot.status) === 1 || Number(slot.status) === 2"
                  class="icon-button danger ms-1"
                  :title="Number(slot.status) === 1 ? 'Hủy chiến dịch' : 'Hủy chiến dịch đang chạy'"
                  @click="openDelModal(slot)"
                >
                  <i class="bi bi-trash3"></i>
                </button>
                <!-- Slot CANCELLED: nút Khôi phục -->
                <button
                  v-if="Number(slot.status) === 4"
                  class="icon-button restore ms-1"
                  :class="{ 'icon-button-disabled': !canRestoreSlot(slot) }"
                  :title="restoreReason(slot)"
                  :disabled="!canRestoreSlot(slot)"
                  @click="openRestoreModal(slot)"
                >
                  <i class="bi bi-arrow-counterclockwise"></i>
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

        <!-- Banner cảnh báo khi slot bị khóa (chỉ hiện khi edit slot đã ACTIVE/ENDED/CANCELLED/PENDING) -->
        <div v-if="formLocked" class="fs-locked-banner">
          <i class="bi bi-eye-fill"></i>
          <div>
            <strong>Chế độ chỉ xem</strong>
            <span>{{ lockReason(editSlot) }}. Bạn có thể xem chi tiết nhưng không thể thay đổi bất kỳ trường nào.</span>
          </div>
        </div>

        <form
          class="voucher-form"
          :class="{ 'form-locked-view': formLocked }"
          novalidate
          @submit.prevent="saveSlot"
        >
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
                  :readonly="formLocked"
                  placeholder="VD: Flash Sale Thứ 6 - iPhone Series"
                  maxlength="100"
                />
                <button
                  type="button"
                  class="generate-code-btn"
                  :disabled="formLocked"
                  @click="generateSlotName"
                >
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
                  :readonly="formLocked"
                  :min="minStartDate"
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
                  :readonly="formLocked"
                  :min="form.startDate || minStartDate"
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
            <div v-if="overlappingSlots.length > 0" class="fs-overlap-warn mt-2">
              <i class="bi bi-exclamation-triangle-fill"></i>
              <div>
                <strong>Khung giờ bị trùng với {{ overlappingSlots.length }} flash sale khác:</strong>
                <ul class="mb-0 mt-1">
                  <li v-for="s in overlappingSlots" :key="s.slotId">
                    #{{ s.slotId }} "{{ s.name }}" —
                    {{ formatDateTime(s.startDate) }} → {{ formatDateTime(s.endDate) }}
                  </li>
                </ul>
                <small>Vui lòng chọn khung giờ khác để tránh chồng chéo.</small>
              </div>
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
                    :disabled="formLocked"
                    @click="switchBannerMode('file')"
                  >
                    <i class="bi bi-upload"></i> Tải ảnh lên
                  </button>
                  <button
                    type="button"
                    class="fs-banner-mode-btn"
                    :class="{ active: bannerInputMode === 'url' }"
                    :disabled="formLocked"
                    @click="switchBannerMode('url')"
                  >
                    <i class="bi bi-link-45deg"></i> Dán URL
                  </button>
                </div>

                <!-- MODE: FILE UPLOAD -->
                <div v-show="bannerInputMode === 'file'" class="fs-banner-file-zone">
                  <label class="fs-banner-file-label" :class="{ 'fs-banner-file-locked': formLocked }">
                    <i class="bi bi-cloud-arrow-up"></i>
                    <span v-if="!bannerFileName">Chọn ảnh từ thiết bị</span>
                    <span v-else class="fs-banner-file-name">{{ bannerFileName }}</span>
                    <small>PNG, JPG, WEBP — tối đa 2MB</small>
                    <input
                      type="file"
                      accept="image/png,image/jpeg,image/jpg,image/webp"
                      hidden
                      :disabled="formLocked"
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
                      :readonly="formLocked"
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
                    v-if="getBannerPreviewSrc && !bannerImageError"
                    :src="getBannerPreviewSrc"
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
                  :disabled="formLocked"
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
              type="button"
              class="fs-tab-btn"
              :class="{ active: activeTab === 0 }"
              :disabled="formLocked"
              @click="switchTab(0)"
            >
              <i class="bi bi-box-seam"></i> Chọn sản phẩm
            </button>
            <button
              type="button"
              class="fs-tab-btn"
              :class="{ active: activeTab === 1 }"
              :disabled="formLocked"
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
                :class="{ active: cascadeOpen, has: selectedItemPids.length > 0, locked: formLocked }"
                :disabled="formLocked"
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
                  <th class="text-end">Giá gốc</th>
                  <th class="text-center">Giá Flash Sale</th>
                  <th class="text-center">Chiết khấu (%)</th>
                  <th class="text-center">Số lượng</th>
                  <th></th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="pid in selectedItemPids" :key="pid">
                  <td>
                    <div style="display: flex; align-items: center; gap: 10px;">
                      <div class="fs-sel-thumb">
                        <img
                          v-if="getSku(pid)?.thumbnailUrl"
                          :src="getSku(pid).thumbnailUrl"
                          :alt="getSku(pid)?.productName"
                        />
                        <i v-else class="bi bi-image"></i>
                      </div>
                      <div class="fs-sel-name-wrap">
                        <div class="fs-sel-name">{{ getSku(pid)?.productName || `Sản phẩm #${pid}` }}</div>
                        <div class="fs-sel-sku">SKU: {{ getSku(pid)?.skuCode || pid }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="text-end">{{ formatVND(getProductPrice(pid)) }}</td>
                  <td class="text-center">
                    <input
                      type="text"
                      inputmode="numeric"
                      class="fs-input"
                      :class="{ 'is-invalid': submitted && errors[`item_${pid}_price`] }"
                      :readonly="formLocked"
                      :value="formatNumber(selItems[pid]?.flashSalePrice)"
                      @input="onPriceChange(pid, $event.target.value)"
                      placeholder="Nhập giá"
                    />
                    <div v-if="submitted && errors[`item_${pid}_price`]" class="invalid-feedback">
                      {{ errors[`item_${pid}_price`] }}
                    </div>
                  </td>
                  <td class="text-center">
                    <input
                      type="number"
                      class="fs-input"
                      :readonly="formLocked"
                      :value="selItems[pid]?.discountPercent || 0"
                      @input="onDiscountChange(pid, $event.target.value)"
                      min="0"
                      max="100"
                    />
                  </td>
                  <td class="text-center">
                    <input
                      type="number"
                      class="fs-input"
                      :class="{ 'is-invalid': (submitted && errors[`item_${pid}_quantity`]) || qtyError[pid] }"
                      :readonly="formLocked"
                      :value="selItems[pid]?.flashSaleQuantity || 0"
                      @input="onQtyChange(pid, $event.target.value)"
                      min="1"
                    />
                    <div v-if="qtyError[pid]" class="invalid-feedback">
                      Vượt tồn kho (còn {{ getProductStock(pid) }})
                    </div>
                    <div v-else-if="submitted && errors[`item_${pid}_quantity`]" class="invalid-feedback">
                      {{ errors[`item_${pid}_quantity`] }}
                    </div>
                  </td>
                  <td>
                    <button
                      class="fs-rm-btn"
                      :disabled="formLocked"
                      @click="removeProduct(pid)"
                    >
                      <i class="bi bi-x"></i>
                    </button>
                  </td>
                </tr>
                </tbody>
              </table>
            </div>

            <!-- Lỗi items chung (khi chưa chọn sản phẩm nào) -->
            <div v-if="submitted && errors.items" class="invalid-feedback d-block mt-2">
              {{ errors.items }}
            </div>
          </div>

          <!-- TAB 1: BY CATEGORY -->
          <div v-show="activeTab === 1" class="fs-tab-content">
            <div class="fs-cat-row">
              <div class="fs-cat-wrap">
                <button
                  type="button"
                  class="fs-cat-trigger"
                  :class="{ active: catOpen, has: activeCategoryId !== null, locked: formLocked }"
                  :disabled="formLocked"
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
                :disabled="formLocked"
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
                  <th class="text-end">Giá gốc</th>
                  <th class="text-center">Giá Flash Sale</th>
                  <th class="text-center">Chiết khấu (%)</th>
                  <th class="text-center">Số lượng</th>
                  <th></th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="pid in selectedItemPids" :key="pid">
                  <td>
                    <div style="display: flex; align-items: center; gap: 10px;">
                      <div class="fs-sel-thumb">
                        <img
                          v-if="getSku(pid)?.thumbnailUrl"
                          :src="getSku(pid).thumbnailUrl"
                          :alt="getSku(pid)?.productName"
                        />
                        <i v-else class="bi bi-image"></i>
                      </div>
                      <div class="fs-sel-name-wrap">
                        <div class="fs-sel-name">{{ getSku(pid)?.productName || `Sản phẩm #${pid}` }}</div>
                        <div class="fs-sel-sku">SKU: {{ getSku(pid)?.skuCode || pid }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="text-end">{{ formatVND(getProductPrice(pid)) }}</td>
                  <td class="text-center">
                    <input
                      type="text"
                      inputmode="numeric"
                      class="fs-input"
                      :class="{ 'is-invalid': submitted && errors[`item_${pid}_price`] }"
                      :readonly="formLocked"
                      :value="formatNumber(selItems[pid]?.flashSalePrice)"
                      @input="onPriceChange(pid, $event.target.value)"
                      placeholder="Nhập giá"
                    />
                    <div v-if="submitted && errors[`item_${pid}_price`]" class="invalid-feedback">
                      {{ errors[`item_${pid}_price`] }}
                    </div>
                  </td>
                  <td class="text-center">
                    <input
                      type="number"
                      class="fs-input"
                      :readonly="formLocked"
                      :value="selItems[pid]?.discountPercent || 0"
                      @input="onDiscountChange(pid, $event.target.value)"
                      min="0"
                      max="100"
                    />
                  </td>
                  <td class="text-center">
                    <input
                      type="number"
                      class="fs-input"
                      :class="{ 'is-invalid': (submitted && errors[`item_${pid}_quantity`]) || qtyError[pid] }"
                      :readonly="formLocked"
                      :value="selItems[pid]?.flashSaleQuantity || 0"
                      @input="onQtyChange(pid, $event.target.value)"
                      min="1"
                    />
                    <div v-if="qtyError[pid]" class="invalid-feedback">
                      Vượt tồn kho (còn {{ getProductStock(pid) }})
                    </div>
                    <div v-else-if="submitted && errors[`item_${pid}_quantity`]" class="invalid-feedback">
                      {{ errors[`item_${pid}_quantity`] }}
                    </div>
                  </td>
                  <td>
                    <button
                      class="fs-rm-btn"
                      :disabled="formLocked"
                      @click="removeProduct(pid)"
                    >
                      <i class="bi bi-x"></i>
                    </button>
                  </td>
                </tr>
                </tbody>
              </table>
            </div>

            <!-- Lỗi items chung -->
            <div v-if="submitted && errors.items" class="invalid-feedback d-block mt-2">
              {{ errors.items }}
            </div>
          </div>

          <div class="form-actions">
            <button
              type="button"
              class="btn btn-soft"
              :disabled="formLocked"
              @click="resetForm"
            >
              <i class="bi bi-arrow-counterclockwise"></i>Làm mới
            </button>

            <!-- Khi slot ACTIVE đang mở (formLocked=true): hiện nút "Hủy Flash Sale" thay vì "Đóng" -->
            <button
              v-if="formLocked && editSlot && Number(editSlot.status) === 2"
              type="button"
              class="btn btn-primary-action"
              style="background:#dc3545"
              @click="openCancelFromModal"
            >
              <i class="bi bi-x-circle-fill"></i> Hủy Flash Sale
            </button>
            <!-- Khi slot không phải ACTIVE (formLocked=true): hiện nút Đóng -->
            <button
              v-else-if="formLocked"
              type="button"
              class="btn btn-primary-action"
              @click="closeModal"
            >
              <i class="bi bi-x-circle"></i> Đóng
            </button>
            <!-- Khi slot SCHEDULED: hiện nút Lưu -->
            <button
              v-else
              type="submit"
              class="btn btn-primary-action"
              :disabled="saving || bannerUploading"
            >
              <i class="bi bi-check2-circle"></i>
              {{ saving || bannerUploading ? 'Đang xử lý...' : 'Lưu chiến dịch' }}
            </button>
          </div>

          <!-- Lỗi trả về từ server (fieldErrors + error message) -->
          <div v-if="submitted && (flashSaleStore.error || Object.keys(flashSaleStore.fieldErrors || {}).length > 0)"
               class="fs-server-errors mt-3">
            <div v-if="flashSaleStore.error" class="alert alert-danger mb-2">
              <i class="bi bi-exclamation-triangle-fill me-2"></i>
              {{ flashSaleStore.error }}
            </div>
            <ul v-if="Object.keys(flashSaleStore.fieldErrors || {}).length > 0" class="mb-0">
              <li v-for="(msg, field) in flashSaleStore.fieldErrors" :key="field">
                <strong>{{ field }}:</strong> {{ msg }}
              </li>
            </ul>
          </div>
        </form>
      </div>
    </div>

    <!-- DELETE / CANCEL CONFIRM -->
    <div
      class="modal-backdrop-custom"
      v-if="showDelModal"
    >
      <div class="flashsale-modal flashsale-modal--sm">
        <div class="modal-head">
          <div>
            <h2>{{ isCancellingSlot ? 'Hủy chiến dịch này?' : 'Xóa chiến dịch này?' }}</h2>
            <p>{{ isCancellingSlot
              ? (Number(delTarget?.status) === 2
                ? 'Chiến dịch đang diễn ra sẽ bị hủy và chuyển sang trạng thái "Đã hủy".'
                : 'Chiến dịch sẽ chuyển sang trạng thái "Đã hủy" và được đẩy xuống cuối bảng.')
              : 'Hành động này không thể hoàn tác.' }}
            </p>
          </div>
          <button class="modal-close-btn" @click="closeDelModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="voucher-form">
          <div class="form-section">
            <div class="section-title">
              <span><i :class="isCancellingSlot ? 'bi bi-x-circle' : 'bi bi-trash3'"></i></span>
              <div>
                <h3>{{ delTarget?.name }}</h3>
                <p>{{ isCancellingSlot
                  ? (Number(delTarget?.status) === 2
                    ? 'Chiến dịch đang diễn ra sẽ bị hủy ngay lập tức và có thể khôi phục lại sau.'
                    : 'Chiến dịch sẽ bị hủy và có thể xem lại ở trạng thái "Đã hủy".')
                  : 'Chiến dịch sẽ bị xóa vĩnh viễn khỏi hệ thống.' }}
                </p>
              </div>
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="btn-soft" @click="closeDelModal">
              <i class="bi bi-x-circle"></i>Hủy bỏ
            </button>
            <button type="button" class="btn-primary-action"
                    :style="isCancellingSlot ? 'background:#f59e0b' : 'background:#dc3545'"
                    @click="confirmDel">
              <i :class="isCancellingSlot ? 'bi bi-x-circle-fill' : 'bi bi-trash3'"></i>
              {{ isCancellingSlot ? 'Hủy chiến dịch' : 'Xóa ngay' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- BANNER LIGHTBOX -->
    <Transition name="fade">
      <div
        v-if="bannerLightboxUrl"
        class="banner-lightbox"
        @click.self="closeBannerPreview"
        @keydown.esc="closeBannerPreview"
        tabindex="0"
        ref="lightboxEl"
      >
        <button
          type="button"
          class="banner-lightbox-close"
          @click="closeBannerPreview"
          title="Đóng (Esc)"
        >
          <i class="bi bi-x-lg"></i>
        </button>
        <img
          :src="bannerLightboxUrl"
          alt="banner preview"
          class="banner-lightbox-img"
          @click.stop
        />
      </div>
    </Transition>

    <!-- Modal Xem chi tiết thống kê Flash Sale (cho slot đã ACTIVE/ENDED/CANCELLED/PENDING) -->
    <FlashSaleDetailModal
      :visible="detailVisible"
      :slot-id="detailSlotId"
      @close="closeDetailModal"
    />

    <!-- RESTORE CONFIRM MODAL -->
    <div
      class="modal-backdrop-custom"
      v-if="showRestoreModal"
    >
      <div class="flashsale-modal flashsale-modal--sm">
        <div class="modal-head">
          <div>
            <h2>Khôi phục chiến dịch này?</h2>
            <p>Chiến dịch sẽ tiếp tục chạy cho đến khi kết thúc.</p>
          </div>
          <button class="modal-close-btn" @click="closeRestoreModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="voucher-form">
          <div class="form-section">
            <div class="section-title">
              <span><i class="bi bi-arrow-counterclockwise"></i></span>
              <div>
                <h3>{{ restoreTarget?.name }}</h3>
                <p>Flash Sale sẽ chuyển sang trạng thái "Đang diễn ra" và tiếp tục hoạt động.</p>
              </div>
            </div>
            <div v-if="restoreTarget?.endDate" class="alert alert-info mt-2">
              <i class="bi bi-info-circle-fill me-2"></i>
              <strong>Lưu ý:</strong> Flash Sale kết thúc lúc {{ formatDateTime(restoreTarget.endDate) }}.
              Bạn chỉ có thể khôi phục trước thời điểm này ít nhất 1 tiếng.
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="btn-soft" @click="closeRestoreModal">
              <i class="bi bi-x-circle"></i>Hủy bỏ
            </button>
            <button type="button" class="btn-primary-action" style="background:#198754" @click="confirmRestore">
              <i class="bi bi-arrow-counterclockwise"></i>
              Khôi phục
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
import flashSaleApi from '@/api/FlashSaleApi'
import FlashSaleDetailModal from '@/views/admin/promotion/FlashSaleDetailModal.vue'
import '@/assets/css/FlashSale.css'

const flashSaleStore = useFlashSaleStore()
const {
  slots, loading, error, fieldErrors, pagination, stats,
  cascadeTree, cascadeLoading, cascadeError,
} = storeToRefs(flashSaleStore)



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
/**
 * Map status code từ DB sang label hiển thị.
 * Chỉ có 4 trạng thái thật trong hệ thống:
 *   1 = SCHEDULED  (Đã lên lịch)
 *   2 = ACTIVE     (Đang diễn ra)
 *   3 = ENDED      (Đã kết thúc)
 *   4 = CANCELLED  (Đã hủy)
 *
 * Scheduler BE tự động chuyển trạng thái theo thời gian (cron mỗi phút),
 * nên FE không cần tự tính "UPCOMING/PENDING" từ ngày nữa.
 */
function resolveStatus(slot) {
  switch (Number(slot.status)) {
    case 1: return 'SCHEDULED'
    case 2: return 'ACTIVE'
    case 3: return 'ENDED'
    case 4: return 'CANCELLED'
    default: return 'SCHEDULED' // fallback an toàn
  }
}

const statusPriority = {
  SCHEDULED: 1,
  ACTIVE: 2,
  ENDED: 3,
  // CANCELLED đẩy xuống cuối bảng (lớn hơn mọi status khác).
  // Slot bị hủy qua nút Xóa sẽ chuyển sang status = 4 → rơi xuống đây.
  CANCELLED: 99,
}

const slotsWithStatus = computed(() =>
  [...slots.value]
    .map(s => ({ ...s, resolvedStatus: resolveStatus(s) }))
    .sort((a, b) => {
      const pa = statusPriority[a.resolvedStatus] ?? 50
      const pb = statusPriority[b.resolvedStatus] ?? 50
      if (pa !== pb) return pa - pb
      // Nhóm CANCELLED: slot hủy sau cùng (updatedAt mới nhất) nằm cuối cùng.
      if (a.resolvedStatus === 'CANCELLED' && b.resolvedStatus === 'CANCELLED') {
        const ua = a.updatedAt ? new Date(a.updatedAt).getTime() : null
        const ub = b.updatedAt ? new Date(b.updatedAt).getTime() : null
        if (ua != null && ub != null) return ua - ub
        if (ua != null) return 1
        if (ub != null) return -1
        return 0
      }
      // Cùng nhóm (khác CANCELLED): ưu tiên flash sale mới tạo (createdAt mới nhất) lên đầu.
      // Nếu thiếu createdAt thì fallback theo startDate sớm nhất.
      const ca = a.createdAt ? new Date(a.createdAt).getTime() : null
      const cb = b.createdAt ? new Date(b.createdAt).getTime() : null
      if (ca != null && cb != null) return cb - ca
      if (ca != null) return -1
      if (cb != null) return 1
      return new Date(a.startDate) - new Date(b.startDate)
    })
)

function formatVND(value) {
  if (value === null || value === undefined || value === '') return '-'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(Number(value))
}

/**
 * Format số nguyên có dấu . phân cách hàng nghìn: 1234567 -> "1.234.567".
 * Dùng cho ô nhập giá Flash Sale để admin nhìn cho dễ.
 */
function formatNumber(value) {
  if (value === null || value === undefined || value === '') return ''
  const digits = String(value).replace(/\D/g, '')
  if (!digits) return ''
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(Number(digits))
}

/**
 * Parse chuỗi có dấu . về số: "1.234.567" -> 1234567.
 * Trả về 0 nếu không parse được.
 */
function parseNumber(value) {
  if (value === null || value === undefined) return 0
  const digits = String(value).replace(/\D/g, '')
  return digits ? Number(digits) : 0
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
    SCHEDULED: 'Đã lên lịch',
    ACTIVE: 'Đang diễn ra',
    ENDED: 'Đã kết thúc',
    CANCELLED: 'Đã hủy',
  }
  return map[slot.resolvedStatus] || slot.resolvedStatus
}

function statusBadgeClass(slot) {
  return {
    ACTIVE: '',
    SCHEDULED: 'warning',
    ENDED: 'inactive',
    CANCELLED: 'danger',
  }[slot.resolvedStatus] || 'info'
}

/**
 * Quy tắc khóa form edit:
 *   - status = 1 (SCHEDULED / Đã lên lịch): cho sửa
 *   - status = 2 (ACTIVE / Đang diễn ra): khóa
 *   - status = 3 (ENDED / Đã kết thúc): khóa
 *   - status = 4 (CANCELLED / Đã hủy): khóa
 *
 * Scheduler BE tự động chuyển status theo thời gian (cron mỗi phút),
 * nên FE không cần check thời gian để tự tính "UPCOMING" nữa.
 */
function isSlotLocked(slot) {
  if (!slot) return true
  // Chỉ status = 1 (SCHEDULED) là cho sửa
  return Number(slot.status) !== 1
}

/**
 * Lý do khóa — dùng hiển thị trong tooltip nút Sửa + toast thông báo.
 */
function lockReason(slot) {
  const map = {
    ACTIVE: 'Flash Sale đang diễn ra, không thể chỉnh sửa',
    ENDED: 'Flash Sale đã kết thúc, không thể chỉnh sửa',
    CANCELLED: 'Flash Sale đã bị hủy, không thể chỉnh sửa',
  }
  return map[slot?.resolvedStatus] || 'Flash Sale đã bị khóa, không thể chỉnh sửa'
}

// Cột 'Đã sử dụng': trả về true khi slot đã bán hết (used >= total)
function isSlotExhausted(slot) {
  const used = slot.usedQuantity ?? 0
  const total = slot.quantityFlashSaleSlot ?? 0
  return total > 0 && used >= total
}

// Kiểm tra slot có thể khôi phục hay không (chỉ áp dụng cho CANCELLED)
function canRestoreSlot(slot) {
  if (Number(slot.status) !== 4) return false
  if (!slot.endDate) return false
  const endDate = new Date(slot.endDate)
  const now = new Date()
  const deadline = new Date(endDate.getTime() - 60 * 60 * 1000) // endDate - 1 tiếng
  return now < deadline
}

// Lý do không thể khôi phục (tooltip)
function restoreReason(slot) {
  if (Number(slot.status) !== 4) return 'Slot không ở trạng thái đã hủy'
  if (!slot.endDate) return 'Slot không có thông tin thời gian kết thúc'
  const endDate = new Date(slot.endDate)
  const now = new Date()
  const deadline = new Date(endDate.getTime() - 60 * 60 * 1000)
  if (now >= deadline) {
    return `Đã quá thời hạn khôi phục (phải trước ${deadline.toLocaleString('vi-VN')})`
  }
  return 'Có thể khôi phục'
}

// Tooltip hiển thị thông tin sử dụng
function getUsedTitle(slot) {
  const used = slot.usedQuantity ?? 0
  const total = slot.quantityFlashSaleSlot ?? 0
  return `Đã sử dụng: ${used} / ${total}`
}

const isModalOpen = ref(false)
const isEditing = ref(false)
const activeTab = ref(0)
const editSlotId = ref(null)
const editSlot = ref(null) // Lưu slot đang edit để check locked trong form
const formLocked = ref(false) // true khi mở edit slot đã ACTIVE/ENDED/CANCELLED

// === Modal Xem chi tiết thống kê (chỉ hiện cho slot đã locked) ===
const detailVisible = ref(false)
const detailSlotId = ref(null)
const overlappingSlots = ref([])   // PHẢI khai báo trước watcher overlap

// === Banner preview lightbox (xem ảnh lớn khi click thumbnail trong bảng) ===
const bannerLightboxUrl = ref('')
const lightboxEl = ref(null)

function openBannerPreview(url) {
  if (!url) return
  bannerLightboxUrl.value = url
  nextTick(() => {
    lightboxEl.value?.focus()
  })
}

function closeBannerPreview() {
  bannerLightboxUrl.value = ''
}

const defaultForm = {
  name: '',
  status: 1,
  startDate: '',
  endDate: '',
  bannerUrl: '',
}

const form = reactive({...defaultForm})

// Debounced overlap check khi admin nhập startDate/endDate
// PHẢI đặt sau `const form = reactive(...)` để tránh TDZ (Cannot access 'form' before initialization)
let overlapTimer = null
watch(
  () => [form.startDate, form.endDate],
  () => {
    clearTimeout(overlapTimer)
    overlapTimer = setTimeout(checkOverlap, 400)
  }
)

async function checkOverlap() {
  if (!form.startDate || !form.endDate) {
    overlappingSlots.value = []
    return
  }
  // Đảm bảo endDate > startDate trước khi gọi API
  const start = new Date(form.startDate)
  const end = new Date(form.endDate)
  if (isNaN(start.getTime()) || isNaN(end.getTime()) || end <= start) {
    overlappingSlots.value = []
    return
  }
  const toIso = (d) => {
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`
  }
  try {
    const list = await flashSaleStore.fetchOverlap({
      startDate: toIso(start),
      endDate: toIso(end),
      excludeSlotId: isEditing.value ? editSlotId.value : null,
    })
    overlappingSlots.value = list
  } catch (e) {
    overlappingSlots.value = []
  }
}

const nameLength = computed(() => (form.name || '').length)

const minStartDate = computed(() => {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
})

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
  } else if (form.name.trim().length > 100) {
    result.name = 'Tên chiến dịch tối đa 100 ký tự'
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
    const now = new Date()
    if (s < now) {
      result.startDate = 'Thời gian bắt đầu không được ở trong quá khứ'
    } else if (e <= s) {
      result.time = 'Thời gian kết thúc phải sau thời gian bắt đầu'
    }
  }
  // Mode FILE: nếu đã chọn file thì OK, sẽ upload sau khi submit hợp lệ
  const hasFile = bannerInputMode.value === 'file' && bannerFile.value
  const hasUrl = bannerInputMode.value === 'url' && form.bannerUrl.trim()

  if (!hasFile && !hasUrl) {
    result.bannerUrl = 'Vui long upload anh hoac dan URL'
  } else if (hasUrl && !isValidUrl(form.bannerUrl)) {
    result.bannerUrl = 'URL anh khong hop le'
  }

  // Validate items
  const pids = selectedItemPids.value
  if (pids.length === 0) {
    result.items = 'Vui lòng chọn ít nhất 1 sản phẩm cho Flash Sale'
  } else {
    pids.forEach((skuId) => {
      const it = selItems[skuId]
      if (!it || it.flashSaleQuantity == null || it.flashSaleQuantity < 1) {
        result[`item_${skuId}_quantity`] = 'Số lượng phải >= 1'
      }
      const orig = getProductPrice(skuId)
      if (it && (it.flashSalePrice == null || it.flashSalePrice <= 0)) {
        result[`item_${skuId}_price`] = 'Giá Flash Sale phải > 0'
      } else if (it && orig > 0 && it.flashSalePrice >= orig) {
        result[`item_${skuId}_price`] = 'Giá Flash Sale phải nhỏ hơn giá gốc'
      }
      if (it && (it.discountPercent == null || it.discountPercent < 0 || it.discountPercent > 100)) {
        result[`item_${skuId}_discount`] = 'Chiết khấu phải từ 0–100%'
      }
      const stock = getProductStock(skuId)
      if (it && stock > 0 && it.flashSaleQuantity > stock) {
        result[`item_${skuId}_stock`] = `Vượt tồn kho (${stock})`
      }
    })
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

const getBannerPreviewSrc = computed(() => {
  if (bannerInputMode.value === 'file') return bannerPreviewUrl.value
  return form.bannerUrl || ''
})

const bannerImageError = ref(false)
const bannerInputMode = ref('file') // 'file' | 'url'
const bannerFileName = ref('')
const bannerFile = ref(null)         // File object chưa upload
const bannerPreviewUrl = ref('')     // data URL để preview
const bannerUploading = ref(false)   // disable nút Lưu khi đang upload

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
  bannerFile.value = null
  bannerPreviewUrl.value = ''
}

function switchBannerMode(mode) {
  bannerInputMode.value = mode
  // Reset trạng thái khi đổi mode
  bannerImageError.value = false
  bannerFileName.value = ''
  bannerFile.value = null
  bannerPreviewUrl.value = ''
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
  bannerFile.value = file

  // Preview bằng data URL (chỉ để xem trước, KHÔNG gán vào form.bannerUrl)
  const reader = new FileReader()
  reader.onload = (e) => {
    bannerPreviewUrl.value = e.target.result
  }
  reader.onerror = () => {
    showToast({
      type: 'error',
      title: 'Lỗi',
      message: 'Không thể đọc file ảnh.',
    })
    bannerFileName.value = ''
    bannerFile.value = null
    bannerPreviewUrl.value = ''
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
  // value có thể là "1.234.567" -> parseNumber ra số nguyên 1234567
  const fp = parseNumber(value)
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
  editSlot.value = null
  formLocked.value = false
  isEditing.value = false
  resetForm()
  isModalOpen.value = true
  activeTab.value = 0
  flashSaleStore.fetchCascade({ includeOutOfStock: false })
}


/**
 * Mở modal Xem chi tiết thống kê cho slot đã ACTIVE/ENDED/CANCELLED/PENDING.
 * Component con (FlashSaleDetailModal) tự gọi API lấy items + soldQuantity.
 */
function openDetailModal(slot) {
  detailSlotId.value = slot.slotId
  detailVisible.value = true
}

function closeDetailModal() {
  detailVisible.value = false
  // Reset slotId sau khi đóng để lần mở sau luôn fetch mới
  setTimeout(() => {
    detailSlotId.value = null
  }, 200)
}

async function openEditModal(slot) {
  // Hướng view-only: vẫn cho mở form để admin xem chi tiết
  // Khi slot đã ACTIVE/ENDED/CANCELLED/PENDING → formLocked=true → input readonly + mờ
  editSlotId.value = slot.slotId
  editSlot.value = slot
  isEditing.value = true
  formLocked.value = isSlotLocked(slot)
  resetForm(false)

  // Lấy chi tiết từ BE để có items đã chọn
  let detail = null
  try {
    detail = await flashSaleStore.fetchOneSlot(slot.slotId)
  } catch (e) {
    detail = null
  }
  const data = detail || slot

  form.name = data.name || ''
  form.startDate = toLocalDatetime(data.startDate)
  form.endDate = toLocalDatetime(data.endDate)
  form.status = data.status ?? 1
  form.bannerUrl = data.bannerImageUrl || ''
  bannerInputMode.value = 'url'
  bannerFile.value = null
  bannerFileName.value = ''
  bannerPreviewUrl.value = ''
  bannerUploading.value = false

  // Map items đã chọn sẵn vào selectedItemPids và selItems
  selectedItemPids.value = []
  Object.keys(selItems).forEach((k) => delete selItems[k])
  if (Array.isArray(data.items) && data.items.length > 0) {
    data.items.forEach((it) => {
      if (!it || it.skuId == null) return
      selectedItemPids.value.push(it.skuId)
      const orig = Number(it.originalPrice ?? 0)
      const fp = Number(it.flashSalePrice ?? 0)
      const disc = orig > 0 ? parseFloat(((1 - fp / orig) * 100).toFixed(1)) : 0
      selItems[it.skuId] = {
        flashSalePrice: fp,
        flashSaleQuantity: Number(it.flashSaleQuantity ?? 1),
        discountPercent: it.discountPercent != null ? Number(it.discountPercent) : disc,
      }
    })
  }

  isModalOpen.value = true
  activeTab.value = 0
  // include hết hàng để hiển thị lại SKU cũ nếu stock đã về 0
  flashSaleStore.fetchCascade({ includeOutOfStock: true })
}

function closeModal() {
  isModalOpen.value = false
  formLocked.value = false
  editSlot.value = null
  editSlotId.value = null
  isEditing.value = false
}

function switchTab(i) {
  activeTab.value = i
}

function resetForm(clearStatus = true) {
  submitted.value = false
  overlappingSlots.value = []   // clear trạng thái overlap khi reset form
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
  bannerFile.value = null
  bannerPreviewUrl.value = ''
  bannerUploading.value = false
}

function toLocalDatetime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * Convert giá trị datetime-local ("YYYY-MM-DDTHH:mm") sang ISO 8601
 * mà BE có thể parse bằng LocalDateTime.parse.
 * Ví dụ: "2026-07-04T20:30" -> "2026-07-04T20:30:00"
 */
function toIsoString(localStr) {
  if (!localStr) return null
  if (localStr.length === 16) return localStr + ':00'
  return localStr
}

async function saveSlot() {
  // Defense in depth: chặn gọi update API ngay cả khi ai đó bypass UI
  if (isEditing.value && formLocked.value) {
    showToast({
      type: 'warning',
      title: 'Không thể chỉnh sửa',
      message: lockReason(editSlot.value),
    })
    return
  }

  submitted.value = true
  if (Object.keys(errors.value).length > 0) {
    showToast({
      type: 'error',
      title: 'Lỗi',
      message: 'Vui lòng kiểm tra lại các trường được đánh dấu đỏ.',
    })
    return
  }

  // Chặn submit nếu đang bị overlap (BE vẫn validate lại lần cuối)
  if (overlappingSlots.value.length > 0) {
    showToast({
      type: 'error',
      title: 'Lỗi',
      message: 'Khung giờ bị trùng với flash sale khác, vui lòng chọn khung giờ khác.',
    })
    return
  }

  const pids = selectedItemPids.value
  if (pids.length === 0) {
    showToast({type: 'error', title: 'Lỗi', message: 'Vui lòng chọn ít nhất 1 sản phẩm.'})
    return
  }

  saving.value = true
  try {
    // Bước 1: Nếu user đang ở mode "file" và đã chọn file mới → upload lên Cloudinary
    if (bannerInputMode.value === 'file' && bannerFile.value) {
      bannerUploading.value = true
      try {
        const uploadRes = await flashSaleApi.uploadBanner(bannerFile.value)
        const uploadedUrl = uploadRes?.data?.data?.imageUrl
        if (!uploadedUrl) {
          showToast({
            type: 'error',
            title: 'Lỗi',
            message: 'Upload ảnh banner thất bại, vui lòng thử lại.',
          })
          return
        }
        form.bannerUrl = uploadedUrl
      } catch (err) {
        console.error('[FlashSale] Upload banner failed:', err)
        showToast({
          type: 'error',
          title: 'Lỗi',
          message:
            err?.response?.data?.message ||
            'Upload ảnh banner thất bại, vui lòng thử lại.',
        })
        return
      } finally {
        bannerUploading.value = false
      }
    }

    // Bước 2: Build payload & submit slot
    const items = pids.map((skuId) => {
      const skuIdNum = Number(skuId)
      // Guard: bỏ qua item có id không hợp lệ để tránh 400 toàn batch
      if (!Number.isFinite(skuIdNum) || skuIdNum <= 0) {
        console.warn('[FlashSale] Bỏ qua SKU không hợp lệ:', skuId)
        return null
      }
      const it = selItems[skuId]
      const sku = getSku(skuId)
      // BE DTO FlashSaleItemRequest dùng field "skuId" viết thường (xem FlashSaleItemRequest.java)
      return {
        skuId: skuIdNum,
        originalPrice: Number(sku?.originalPrice ?? 0),
        flashSalePrice: Number(it?.flashSalePrice ?? 0),
        flashSaleQuantity: Number(it?.flashSaleQuantity ?? 0),
      }
    }).filter(Boolean)

    const payload = {
      name: form.name.trim(),
      startDate: toIsoString(form.startDate),
      endDate: toIsoString(form.endDate),
      status: Number(form.status) || 1,
      // BE expect tên field "bannerImageUrl" (xem FlashSaleSlotRequest.java)
      bannerImageUrl: form.bannerUrl.trim(),
      items,
    }

    const ok = isEditing.value
      ? await flashSaleStore.updateSlot(editSlotId.value, payload)
      : await flashSaleStore.addSlot(payload)

    if (!ok) {
      showToast({
        type: 'error',
        title: 'Lỗi',
        message: flashSaleStore.error || 'Không thể lưu Flash Sale.',
      })
      return
    }

    // Reload lại danh sách từ BE để đảm bảo đồng bộ
    await loadSlots()
    showToast({
      type: 'success',
      title: 'Thành công',
      message: isEditing.value
        ? 'Cập nhật Flash Sale thành công!'
        : 'Tạo Flash Sale thành công!',
    })
    closeModal()
  } finally {
    saving.value = false
  }
}



const showDelModal = ref(false)
const delTarget = ref(null)

// Slot ở trạng thái SCHEDULED (status = 1) khi bấm Xóa sẽ được hủy (đổi sang CANCELLED = 4)
// chứ không xóa vĩnh viễn. Cờ này dùng để đổi text/tooltip của modal xác nhận.
const isCancellingSlot = computed(() => Number(delTarget.value?.status) === 1 || Number(delTarget.value?.status) === 2)

function openDelModal(slot) {
  delTarget.value = slot
  showDelModal.value = true
}

function closeDelModal() {
  showDelModal.value = false
  delTarget.value = null
}

// === Khôi phục Flash Sale đã hủy ===
const showRestoreModal = ref(false)
const restoreTarget = ref(null)

function openRestoreModal(slot) {
  restoreTarget.value = slot
  showRestoreModal.value = true
}

function closeRestoreModal() {
  showRestoreModal.value = false
  restoreTarget.value = null
}

// Mở modal hủy từ bên trong modal xem chi tiết (khi slot ACTIVE)
function openCancelFromModal() {
  if (!editSlot.value) return
  delTarget.value = { ...editSlot.value }
  showDelModal.value = true
}

async function confirmRestore() {
  if (!restoreTarget.value) return
  const target = restoreTarget.value

  const ok = await flashSaleStore.restoreFlashSale(target.slotId)
  if (ok) {
    await loadSlots()
    showToast({
      type: 'success',
      title: 'Thành công',
      message: 'Đã khôi phục chiến dịch Flash Sale.',
    })
    closeRestoreModal()
  } else {
    showToast({
      type: 'error',
      title: 'Lỗi',
      message: flashSaleStore.error || 'Không thể khôi phục Flash Sale.',
    })
    closeRestoreModal()
  }
}

async function confirmDel() {
  if (!delTarget.value) return
  const target = delTarget.value
  const currentStatus = Number(target.status)

  // Slot SCHEDULED (1) hoặc ACTIVE (2): hủy bằng cách đổi sang CANCELLED (4)
  if (currentStatus === 1 || currentStatus === 2) {
    const ok = await flashSaleStore.toggleSlotStatus(target.slotId, 4)
    if (ok) {
      // Đóng modal xem chi tiết nếu đang mở
      closeModal()
      await loadSlots()
      showToast({
        type: 'success',
        title: 'Thành công',
        message: currentStatus === 2
          ? 'Đã hủy chiến dịch đang diễn ra.'
          : 'Đã hủy chiến dịch Flash Sale.',
      })
      closeDelModal()
    } else {
      showToast({
        type: 'error',
        title: 'Lỗi',
        message: flashSaleStore.error || 'Không thể hủy Flash Sale.',
      })
      closeDelModal()
    }
    return
  }

  // Trạng thái khác (ENDED/CANCELLED): không cho xóa
  showToast({
    type: 'warning',
    title: 'Không thể thực hiện',
    message: 'Flash Sale ở trạng thái này không thể xóa.',
  })
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
    if (bannerLightboxUrl.value) closeBannerPreview()
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
