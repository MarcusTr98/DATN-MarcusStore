<template>
  <div class="voucher-page">
    <Transition name="fade">
      <div v-if="toast.show" class="toast-alert" :class="toast.type">
        <strong>{{ toast.title }}</strong>
        <span>{{ toast.message }}</span>
      </div>
    </Transition>

    <div class="voucher-shell">
      <section class="voucher-hero">
        <div class="hero-title">
          <div class="hero-icon">
            <i class="bi bi-tags-fill"></i>
          </div>
          <div>
            <h1>Quản lý Voucher</h1>
            <p>Quản lý mã giảm giá, thời gian áp dụng và đối tượng sử dụng.</p>
          </div>
        </div>

        <button type="button" class="btn btn-primary-action" @click="openCreateModal">
          <i class="bi bi-plus-lg"></i>
          Thêm Voucher
        </button>
      </section>

      <section class="stats-grid">
        <article class="stat-card">
          <span>Tổng voucher</span>
          <strong>{{ stats.total }}</strong>
        </article>

        <article class="stat-card">
          <span>Đang sử dụng</span>
          <strong class="text-accent">{{ stats.active }}</strong>
        </article>

        <article class="stat-card">
          <span>Giảm theo %</span>
          <strong>{{ stats.percent }}</strong>
        </article>

        <article class="stat-card">
          <span>Giảm tiền</span>
          <strong>{{ stats.amount }}</strong>
        </article>
      </section>

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
                placeholder="Tìm theo mã voucher"
              />
            </div>
          </div>

          <div class="col-12 col-md-6 col-lg">
            <label class="form-label">Loại giảm giá</label>
            <select v-model="filters.discountType" class="form-select">
              <option value="ALL">Tất cả</option>
              <option value="PERCENT">Giảm theo %</option>
              <option value="AMOUNT">Giảm tiền</option>
            </select>
          </div>

          <div class="col-12 col-md-6 col-lg">
            <label class="form-label">Trạng thái</label>
            <select v-model="filters.status" class="form-select">
              <option value="ALL">Tất cả</option>
              <option value="ACTIVE">Đang sử dụng</option>
              <option value="INACTIVE">Ngừng sử dụng</option>
            </select>
          </div>

          <div class="col-12 col-md-6 col-lg-auto">
            <button type="button" class="btn btn-soft w-100" title="Xóa lọc" @click="resetFilters">
              <i class="bi bi-arrow-counterclockwise"></i>
            </button>
          </div>
        </div>
      </section>

      <section class="table-panel">
        <div class="table-responsive">
          <table class="table align-middle voucher-table mb-0">
            <thead>
            <tr>
              <th>ID</th>
              <th>Mã voucher</th>
              <th>Loại</th>
              <th>Giá trị</th>
              <th>Giảm tối đa</th>
              <th>Đơn tối thiểu</th>
              <th>Số lượng</th>
              <th>Thời gian</th>
              <th>Đối tượng</th>
              <th>Trạng thái</th>
              <th class="text-end">Thao tác</th>
            </tr>
            </thead>

            <tbody>
            <tr v-for="(voucher, index) in filteredVouchers" :key="voucher.voucherId">
              <td class="fw-bold">#{{ currentPage * pageSize + index + 1 }}</td>
              <td>
                <div class="voucher-code">{{ voucher.voucherCode }}</div>
              </td>
              <td>
                <span class="type-badge" :class="voucher.discountType.toLowerCase()">
                  {{ formatDiscountType(voucher.discountType) }}
                </span>
              </td>
              <td class="fw-semibold">{{ formatDiscountValue(voucher) }}</td>
              <td>{{ formatCurrency(voucher.maxDiscountAmount) }}</td>
              <td>{{ formatCurrency(voucher.minOrderValue) }}</td>
              <td>{{ voucher.quantity || '-' }}</td>
              <td>
                <div class="date-line">Từ: {{ formatDateTime(voucher.startDate) }}</div>
                <div class="date-line">Đến: {{ formatDateTime(voucher.endDate) }}</div>
              </td>
              <td>
                <span class="scope-badge target-badge">
                  <i :class="voucher.targetType === 'ALL' ? 'bi bi-globe' : 'bi bi-people'"></i>
                  {{ formatTargetType(voucher) }}
                </span>
              </td>
              <td>
                <span
                  class="status-badge"
                  :class="{
                    inactive: !voucher.isActive || (voucher.endDate && new Date(voucher.endDate) < new Date())
                  }"
                >
                  {{ voucher.isActive && (!voucher.endDate || new Date(voucher.endDate) >= new Date())
                      ? 'Đang sử dụng'
                      : 'Ngừng sử dụng' }}
                </span>
              </td>
              <td>
                <div class="d-flex justify-content-end gap-2">
                  <button
                    type="button"
                    class="icon-button"
                    title="Sửa voucher"
                    @click="openEditModal(voucher)"
                  >
                    <i class="bi bi-pencil-square"></i>
                  </button>
                  <button
                    type="button"
                    class="icon-button danger"
                    title="Xóa voucher"
                    @click="deleteVoucher(voucher)"
                  >
                    <i class="bi bi-trash3"></i>
                  </button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div v-if="filteredVouchers.length === 0" class="empty-state">
          <i class="bi bi-ticket-perforated"></i>
          <h3>Không có voucher nào</h3>
          <p>Hãy thêm voucher mới hoặc thay đổi bộ lọc.</p>
        </div>

        <div v-if="pagination.totalPages > 0" class="voucher-pagination">
          <div class="pagination-summary">
            Tổng <strong>{{ pagination.totalElements }}</strong> voucher
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
            <button type="button" class="pagination-button" :disabled="currentPage === 0" @click="goToPage(currentPage - 1)">
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

    <!-- Modal Thêm/Sửa Voucher -->
    <div v-if="isModalOpen" class="modal-backdrop-custom">
      <div class="voucher-modal">
        <div class="modal-head">
          <div>
            <h2>{{ isEditing ? 'Sửa Voucher' : 'Thêm Voucher' }}</h2>
            <p>Voucher mới mặc định ở trạng thái đang sử dụng. Mỗi tài khoản chỉ dùng được 1 lần.</p>
          </div>
          <button type="button" class="icon-button" title="Đóng" @click="closeModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <form class="voucher-form" novalidate @submit.prevent="saveVoucher">
          <!-- Segmented Control chọn loại voucher -->
          <div class="voucher-type-selector">
            <label
              class="voucher-type-option"
              :class="{ active: form.voucher_type === 'DISCOUNT' }"
            >
              <input
                v-model="form.voucher_type"
                type="radio"
                value="DISCOUNT"
                name="voucher_type"
              />
              <i class="bi bi-percent"></i>
              <span>Voucher thường</span>
            </label>

            <label
              class="voucher-type-option"
              :class="{ active: form.voucher_type === 'FREESHIP' }"
            >
              <input
                v-model="form.voucher_type"
                type="radio"
                value="FREESHIP"
                name="voucher_type"
              />
              <i class="bi bi-truck"></i>
              <span>Free Ship</span>
            </label>
          </div>

          <!-- ==================== DISCOUNT FORM ==================== -->
          <template v-if="form.voucher_type === 'DISCOUNT'">
            <section class="form-section">
              <div class="section-title">
                <span>1</span>
                <div>
                  <h3>Thông tin Voucher chính</h3>
                  <p>Mã Voucher và trạng thái hiển thị của voucher.</p>
                </div>
              </div>
              <div class="modal-body-grid compact voucher-main-grid">
                <div class="voucher-code-field">
                  <label class="form-label">Mã voucher <span>*</span></label>
                    <div class="voucher-code-control">
                    <input
                      v-model.trim="form.voucher_code"
                      type="text"
                      class="form-control text-uppercase voucher-code-input"
                      :class="{ 'is-invalid': isSubmitted && errors.voucher_code }"
                      placeholder="VD: SUMMER2026"
                      maxlength="12"
                    />
                    <button type="button" class="generate-code-btn" @click="generateVoucherCode">
                      <i class="bi bi-stars"></i>
                      Tạo mã tự động
                    </button>
                  </div>
                  <div class="voucher-code-footer">
                  </div>
                  <div v-if="errors.voucher_code" class="invalid-feedback d-block mt-1">
                    {{ errors.voucher_code }}
                  </div>
                </div>

                <div>
                  <label class="form-label">Trạng thái</label>
                  <select v-model="form.is_active" class="form-select">
                    <option :value="true">Đang sử dụng</option>
                    <option :value="false">Ngừng sử dụng</option>
                  </select>
                </div>

              </div>
            </section>

            <section class="form-section">
              <div class="section-title">
                <span>2</span>
                <div>
                  <h3>Chi tiết Giảm giá</h3>
                  <p>Chọn loại giảm giá theo % hoặc theo giá cố định.</p>
                </div>
              </div>

              <div class="wide-field">
                <label class="form-label">Loại giảm giá <span>*</span></label>
                <div class="discount-choice-grid">
                  <label class="discount-choice"
                         :class="{ active: form.discount_type === 'PERCENT' }">
                    <input v-model="form.discount_type" type="radio" value="PERCENT"/>
                    <span class="discount-choice-text">
                    <strong>Giảm theo phần trăm</strong>
                  </span>
                  </label>

                  <label class="discount-choice" :class="{ active: form.discount_type === 'AMOUNT' }">
                    <input v-model="form.discount_type" type="radio" value="AMOUNT"/>
                    <span class="discount-choice-text">
                    <strong>Giảm tiền trực tiếp</strong>
                  </span>
                  </label>
                </div>
              </div>

              <div>
                <label class="form-label">
                  {{
                    form.discount_type === 'PERCENT' ? 'Giá trị giảm (%)' : 'Số tiền giảm trực tiếp'
                  }}
                  <span>*</span>
                </label>
                <div class="input-group">
                  <input
                    :value="formatNumberInput(form.discount_value)"
                    @input="form.discount_value = parseNumberInput($event.target.value)"
                    type="text"
                    inputmode="numeric"
                    class="form-control"
                    :class="{ 'is-invalid': isSubmitted && errors.discount_value }"
                    :placeholder="form.discount_type === 'PERCENT' ? 'Nhập %, VD: 10' : 'Nhập số tiền, VD: 50.000'"
                  />
                  <span class="input-group-text">{{
                      form.discount_type === 'PERCENT' ? '%' : 'đ'
                    }}</span>
                  <div v-if="errors.discount_value" class="invalid-feedback">
                    {{ errors.discount_value }}
                  </div>
                </div>
              </div>

              <div v-if="form.discount_type === 'PERCENT'">
                <label class="form-label">Giảm tối đa <span>*</span></label>
                <div class="input-group">
                  <input
                    :value="formatNumberInput(form.max_discount_amount)"
                    @input="form.max_discount_amount = parseNumberInput($event.target.value)"
                    type="text"
                    inputmode="numeric"
                    class="form-control"
                    :class="{ 'is-invalid': isSubmitted && errors.max_discount_amount }"
                    placeholder="Nhập số tiền giảm tối đa"
                  />
                  <span class="input-group-text">đ</span>
                  <div v-if="isSubmitted && errors.max_discount_amount" class="invalid-feedback">
                    {{ errors.max_discount_amount }}
                  </div>
                </div>
              </div>

              <div>
                <label class="form-label with-help">
                  Đơn tối thiểu
                  <i class="bi bi-question-circle"
                     title="Áp dụng cho đơn hàng có giá trị từ số tiền này trở lên."></i>
                </label>
                <div class="input-group">
                  <input
                    :value="formatNumberInput(form.min_order_value)"
                    @input="form.min_order_value = parseNumberInput($event.target.value)"
                    type="text"
                    inputmode="numeric"
                    class="form-control"
                    :class="{ 'is-invalid': isSubmitted && errors.min_order_value }"
                    placeholder="Nhập giá trị đơn tối thiểu"
                  />
                  <span class="input-group-text">đ</span>
                  <div v-if="errors.min_order_value" class="invalid-feedback">
                    {{ errors.min_order_value }}
                  </div>
                </div>
              </div>

              <div>
                <label class="form-label">Số lượng voucher <span>*</span></label>
                <div class="input-group">
                  <input
                    v-model.number="form.quantity"
                    type="number"
                    min="1"
                    class="form-control"
                    :class="{ 'is-invalid': isSubmitted && errors.quantity }"
                    placeholder="Nhập số lượng voucher"
                  />
                  <span class="input-group-text">lần</span>
                  <div v-if="errors.quantity" class="invalid-feedback">
                    {{ errors.quantity }}
                  </div>
                </div>
              </div>
            </section>

            <section class="form-section">
              <div class="section-title">
                <span>3</span>
                <div>
                  <h3>Đối tượng sử dụng</h3>
                  <p>Chọn voucher áp dụng cho tất cả khách hoặc khách hàng cụ thể.</p>
                </div>
              </div>

              <div class="modal-body-grid compact">
                <div class="target-scope-field">
                  <label class="form-label">Áp dụng cho <span>*</span></label>
                  <div class="target-choice-grid">
                    <label class="target-choice" :class="{ active: form.target_type === 'ALL' }">
                      <input v-model="form.target_type" type="radio" value="ALL"/>
                      <span class="target-choice-icon">
                        <i class="bi bi-globe"></i>
                      </span>
                      <span class="target-choice-text">
                        <strong>Tất cả khách hàng</strong>
                        <small>Mọi tài khoản đều có thể dùng</small>
                      </span>
                    </label>

                    <label class="target-choice" :class="{ active: form.target_type === 'SPECIFIC' }">
                      <input v-model="form.target_type" type="radio" value="SPECIFIC"/>
                      <span class="target-choice-icon specific">
                        <i class="bi bi-people"></i>
                      </span>
                      <span class="target-choice-text">
                        <strong>Khách hàng cụ thể</strong>
                        <small>Chỉ những người được chọn mới dùng được</small>
                      </span>
                    </label>
                  </div>
                </div>

                <div v-if="form.target_type === 'SPECIFIC'" class="specific-users-field">
                  <label class="form-label">
                    Chọn khách hàng <span>*</span>
                    <span class="selected-count" v-if="form.selected_user_ids.length > 0">
                      ({{ form.selected_user_ids.length }} khách đã chọn)
                    </span>
                  </label>
                  <div class="user-search-wrapper">
                    <input
                      v-model="userSearchQuery"
                      type="text"
                      class="form-control"
                      placeholder="Tìm kiếm khách hàng..."
                      @focus="showUserDropdown = true"
                    />
                    <div v-if="showUserDropdown" class="user-dropdown">
                      <div v-if="loadingUsers" class="user-dropdown-loading">
                        <i class="bi bi-hourglass-split"></i> Đang tải...
                      </div>
                      <div v-else-if="filteredUsers.length === 0" class="user-dropdown-empty">
                        Không tìm thấy khách hàng
                      </div>
                      <div v-else class="user-dropdown-list">
                        <label
                          v-for="user in filteredUsers"
                          :key="user.userId"
                          class="user-option"
                          :class="{ selected: form.selected_user_ids.includes(user.userId) }"
                        >
                          <input
                            type="checkbox"
                            :value="user.userId"
                            v-model="form.selected_user_ids"
                          />
                          <div class="user-option-info">
                            <span class="user-name">{{ user.fullName || user.username }}</span>
                            <span class="user-email">{{ user.email }}</span>
                          </div>
                          <i v-if="form.selected_user_ids.includes(user.userId)" class="bi bi-check-circle-fill"></i>
                        </label>
                      </div>
                    </div>
                  </div>
                  <div v-if="form.selected_user_ids.length > 0" class="selected-users-preview">
                    <span
                      v-for="userId in form.selected_user_ids.slice(0, 3)"
                      :key="userId"
                      class="selected-user-tag"
                    >
                      {{ getUserDisplayName(userId) }}
                      <button type="button" @click="removeUser(userId)" class="remove-user-btn">
                        <i class="bi bi-x"></i>
                      </button>
                    </span>
                    <span v-if="form.selected_user_ids.length > 3" class="more-users-tag">
                      +{{ form.selected_user_ids.length - 3 }} khách khác
                    </span>
                  </div>
                  <div v-if="isSubmitted && errors.selected_user_ids" class="invalid-feedback d-block">
                    {{ errors.selected_user_ids }}
                  </div>
                </div>
              </div>
            </section>

            <section class="form-section">
              <div class="section-title">
                <span>4</span>
                <div>
                  <h3>Thời gian Sử dụng</h3>
                  <p>Ngày kết thúc phải lớn hơn ngày bắt đầu.</p>
                </div>
              </div>

              <div class="modal-body-grid compact">
                <div>
                  <label class="form-label">Ngày bắt đầu <span>*</span></label>
                  <input
                    v-model="form.start_date"
                    type="datetime-local"
                    class="form-control"
                    :min="todayDate"
                    :class="{ 'is-invalid': isSubmitted && errors.start_date }"
                  />
                  <small class="form-help">Gợi ý: chọn từ hôm nay.</small>
                  <div v-if="errors.start_date" class="invalid-feedback">
                    {{ errors.start_date }}
                  </div>
                </div>

                <div>
                  <label class="form-label">Ngày kết thúc <span>*</span></label>
                  <input
                    v-model="form.end_date"
                    type="datetime-local"
                    class="form-control"
                    :min="form.start_date || todayDate"
                    :class="{ 'is-invalid': isSubmitted && (errors.end_date || errors.time) }"
                  />
                  <small class="form-help">Không được trước ngày bắt đầu.</small>
                  <div v-if="errors.end_date" class="invalid-feedback">
                    {{ errors.end_date }}
                  </div>
                  <div v-if="errors.time" class="invalid-feedback d-block">
                    {{ errors.time }}
                  </div>
                </div>
              </div>
            </section>
          </template>

          <!-- ==================== FREESHIP FORM ==================== -->
          <template v-if="form.voucher_type === 'FREESHIP'">
            <section class="form-section">
              <div class="section-title">
                <span>1</span>
                <div>
                  <h3>Thông tin Voucher Free Ship</h3>
                  <p>Mã Voucher và trạng thái hiển thị.</p>
                </div>
              </div>
              <div class="modal-body-grid compact voucher-main-grid">
                <div class="voucher-code-field">
                  <label class="form-label">Mã voucher <span>*</span></label>
                  <div class="voucher-code-control">
                    <input
                      v-model.trim="form.voucher_code"
                      type="text"
                      class="form-control text-uppercase voucher-code-input"
                      :class="{ 'is-invalid': isSubmitted && errors.voucher_code }"
                      placeholder="VD: FREESHIP50K"
                      maxlength="12"
                    />
                    <button type="button" class="generate-code-btn" @click="generateVoucherCode">
                      <i class="bi bi-stars"></i>
                      Tạo mã tự động
                    </button>
                  </div>
                  <div class="voucher-code-footer">
                    <span class="voucher-char-count" :class="{ 'near-limit': codeLength >= 10, 'at-limit': codeLength >= 12 }">
                      {{ codeLength }}/12 ký tự
                    </span>
                    <span v-if="codeLength >= 12" class="voucher-char-warning">
                      <i class="bi bi-exclamation-circle"></i>
                      Mã voucher tối đa 12 ký tự
                    </span>
                  </div>
                  <div v-if="errors.voucher_code" class="invalid-feedback d-block mt-1">
                    {{ errors.voucher_code }}
                  </div>
                </div>

                <div>
                  <label class="form-label">Trạng thái</label>
                  <select v-model="form.is_active" class="form-select">
                    <option :value="true">Đang sử dụng</option>
                    <option :value="false">Ngừng sử dụng</option>
                  </select>
                </div>

              </div>
            </section>

            <section class="form-section">
              <div class="section-title">
                <span>2</span>
                <div>
                  <h3>Chi tiết Free Ship</h3>
                  <p>Cấu hình phí ship được miễn phí và đơn hàng tối thiểu.</p>
                </div>
              </div>

              <div>
                <label class="form-label">Phí ship được miễn phí <span>*</span></label>
                <div class="input-group">
                  <input
                    :value="formatNumberInput(form.freeship_value)"
                    @input="form.freeship_value = parseNumberInput($event.target.value)"
                    type="text"
                    inputmode="numeric"
                    class="form-control"
                    :class="{ 'is-invalid': isSubmitted && errors.freeship_value }"
                    placeholder="Nhập số tiền ship được miễn phí, VD: 25.000"
                  />
                  <span class="input-group-text">đ</span>
                  <div v-if="errors.freeship_value" class="invalid-feedback">
                    {{ errors.freeship_value }}
                  </div>
                </div>
                <small class="form-help">
                  Khách hàng sẽ được miễn phí vận chuyển với đơn hàng có phí ship từ số tiền này trở xuống.
                </small>
              </div>

              <div>
                <label class="form-label with-help">
                  Đơn tối thiểu
                  <i class="bi bi-question-circle" style="cursor:pointer;"
                     title="Áp dụng cho đơn hàng có giá trị từ số tiền này trở lên."></i>
                </label>
                <div class="input-group">
                  <input
                    :value="formatNumberInput(form.min_order_value)"
                    @input="form.min_order_value = parseNumberInput($event.target.value)"
                    type="text"
                    inputmode="numeric"
                    class="form-control"
                    :class="{ 'is-invalid': isSubmitted && errors.min_order_value }"
                    placeholder="Nhập giá trị đơn tối thiểu"
                  />
                  <span class="input-group-text">đ</span>
                  <div v-if="errors.min_order_value" class="invalid-feedback">
                    {{ errors.min_order_value }}
                  </div>
                </div>
              </div>

              <div>
                <label class="form-label">Số lượng voucher <span>*</span></label>
                <div class="input-group">
                  <input
                    v-model.number="form.quantity"
                    type="number"
                    min="1"
                    class="form-control"
                    :class="{ 'is-invalid': isSubmitted && errors.quantity }"
                    placeholder="Nhập số lượng voucher"
                  />
                  <span class="input-group-text">lần</span>
                  <div v-if="errors.quantity" class="invalid-feedback">
                    {{ errors.quantity }}
                  </div>
                </div>
              </div>
            </section>

            <section class="form-section">
              <div class="section-title">
                <span>3</span>
                <div>
                  <h3>Đối tượng sử dụng</h3>
                  <p>Chọn voucher áp dụng cho tất cả khách hoặc khách hàng cụ thể.</p>
                </div>
              </div>

              <div class="modal-body-grid compact">
                <div class="target-scope-field">
                  <label class="form-label">Áp dụng cho <span>*</span></label>
                  <div class="target-choice-grid">
                    <label class="target-choice" :class="{ active: form.target_type === 'ALL' }">
                      <input v-model="form.target_type" type="radio" value="ALL"/>
                      <span class="target-choice-icon">
                        <i class="bi bi-globe"></i>
                      </span>
                      <span class="target-choice-text">
                        <strong>Tất cả khách hàng</strong>
                        <small>Mọi tài khoản đều có thể dùng</small>
                      </span>
                    </label>

                    <label class="target-choice" :class="{ active: form.target_type === 'SPECIFIC' }">
                      <input v-model="form.target_type" type="radio" value="SPECIFIC"/>
                      <span class="target-choice-icon specific">
                        <i class="bi bi-people"></i>
                      </span>
                      <span class="target-choice-text">
                        <strong>Khách hàng cụ thể</strong>
                        <small>Chỉ những người được chọn mới dùng được</small>
                      </span>
                    </label>
                  </div>
                </div>

                <div v-if="form.target_type === 'SPECIFIC'" class="specific-users-field">
                  <label class="form-label">
                    Chọn khách hàng <span>*</span>
                    <span class="selected-count" v-if="form.selected_user_ids.length > 0">
                      ({{ form.selected_user_ids.length }} khách đã chọn)
                    </span>
                  </label>
                  <div class="user-search-wrapper">
                    <input
                      v-model="userSearchQuery"
                      type="text"
                      class="form-control"
                      placeholder="Tìm kiếm khách hàng..."
                      @focus="showUserDropdown = true"
                    />
                    <div v-if="showUserDropdown" class="user-dropdown">
                      <div v-if="loadingUsers" class="user-dropdown-loading">
                        <i class="bi bi-hourglass-split"></i> Đang tải...
                      </div>
                      <div v-else-if="filteredUsers.length === 0" class="user-dropdown-empty">
                        Không tìm thấy khách hàng
                      </div>
                      <div v-else class="user-dropdown-list">
                        <label
                          v-for="user in filteredUsers"
                          :key="user.userId"
                          class="user-option"
                          :class="{ selected: form.selected_user_ids.includes(user.userId) }"
                        >
                          <input
                            type="checkbox"
                            :value="user.userId"
                            v-model="form.selected_user_ids"
                          />
                          <div class="user-option-info">
                            <span class="user-name">{{ user.fullName || user.username }}</span>
                            <span class="user-email">{{ user.email }}</span>
                          </div>
                          <i v-if="form.selected_user_ids.includes(user.userId)" class="bi bi-check-circle-fill"></i>
                        </label>
                      </div>
                    </div>
                  </div>
                  <div v-if="form.selected_user_ids.length > 0" class="selected-users-preview">
                    <span
                      v-for="userId in form.selected_user_ids.slice(0, 3)"
                      :key="userId"
                      class="selected-user-tag"
                    >
                      {{ getUserDisplayName(userId) }}
                      <button type="button" @click="removeUser(userId)" class="remove-user-btn">
                        <i class="bi bi-x"></i>
                      </button>
                    </span>
                    <span v-if="form.selected_user_ids.length > 3" class="more-users-tag">
                      +{{ form.selected_user_ids.length - 3 }} khách khác
                    </span>
                  </div>
                  <div v-if="isSubmitted && errors.selected_user_ids" class="invalid-feedback d-block">
                    {{ errors.selected_user_ids }}
                  </div>
                </div>
              </div>
            </section>

            <section class="form-section">
              <div class="section-title">
                <span>4</span>
                <div>
                  <h3>Thời gian Sử dụng</h3>
                  <p>Ngày kết thúc phải lớn hơn ngày bắt đầu.</p>
                </div>
              </div>

              <div class="modal-body-grid compact">
                <div>
                  <label class="form-label">Ngày bắt đầu <span>*</span></label>
                  <input
                    v-model="form.start_date"
                    type="datetime-local"
                    class="form-control"
                    :min="todayDate"
                    :class="{ 'is-invalid': isSubmitted && errors.start_date }"
                  />
                  <small class="form-help">Gợi ý: chọn từ hôm nay.</small>
                  <div v-if="errors.start_date" class="invalid-feedback">
                    {{ errors.start_date }}
                  </div>
                </div>

                <div>
                  <label class="form-label">Ngày kết thúc <span>*</span></label>
                  <input
                    v-model="form.end_date"
                    type="datetime-local"
                    class="form-control"
                    :min="form.start_date || todayDate"
                    :class="{ 'is-invalid': isSubmitted && (errors.end_date || errors.time) }"
                  />
                  <small class="form-help">Không được trước ngày bắt đầu.</small>
                  <div v-if="errors.end_date" class="invalid-feedback">
                    {{ errors.end_date }}
                  </div>
                  <div v-if="errors.time" class="invalid-feedback d-block">
                    {{ errors.time }}
                  </div>
                </div>
              </div>
            </section>
          </template>

          <!-- Preview Section -->
          <section v-if="isPreviewVisible" class="voucher-preview">
            <div>
              <span class="preview-eyebrow">Xem trước</span>
              <strong>{{ previewVoucher.code }}</strong>
              <p>{{ previewVoucher.discountText }}</p>
              <small>{{ previewVoucher.conditionText }}</small>
            </div>
          </section>

          <div class="form-actions">
            <button type="button" class="btn btn-soft" @click="resetForm">
              Làm mới
            </button>
            <button
              type="button"
              class="btn btn-preview"
              @click="isPreviewVisible = !isPreviewVisible"
            >
              Xem trước
            </button>
            <button
              type="submit"
              class="btn btn-primary-action"
              :disabled="loading"
            >
              {{ loading ? 'Đang lưu...' : 'Lưu Voucher' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <BaseModal
      :visible="deleteConfirm.visible"
      type="confirm"
      title="Ngừng hoạt động voucher"
      :message="deleteConfirm.message"
      @close="closeDeleteConfirm"
      @confirm="confirmDeleteVoucher"
    />

    <BaseModal
      :visible="successModal.visible"
      type="success"
      :title="successModal.title"
      :message="successModal.message"
      @close="closeSuccessModal"
    />
  </div>
</template>

<script setup>
import {computed, reactive, ref, watch, onMounted, onUnmounted} from 'vue'
import {storeToRefs} from 'pinia'
import {useVoucherStore} from '@/stores/voucherStore'
import api from '@/utils/api'
import BaseModal from '@/components/BaseModal.vue'
import '@/assets/css/Voucher.css'

const voucherStore = useVoucherStore()

const {vouchers, loading, error, fieldErrors, pagination, stats} = storeToRefs(voucherStore)

const currentPage = ref(0)
const pageSize = ref(5)

// User list for dropdown
const allUsers = ref([])
const loadingUsers = ref(false)
const userSearchQuery = ref('')
const showUserDropdown = ref(false)

onMounted(() => {
  loadVouchers()
  loadUsers()

  // Close dropdown when clicking outside
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

function handleClickOutside(event) {
  const dropdown = document.querySelector('.user-search-wrapper')
  if (dropdown && !dropdown.contains(event.target)) {
    showUserDropdown.value = false
  }
}

const isModalOpen = ref(false)
const isEditing = ref(false)
const isSubmitted = ref(false)
const isPreviewVisible = ref(false)

const toast = reactive({
  show: false,
  type: 'success',
  title: '',
  message: '',
})

const deleteConfirm = reactive({
  visible: false,
  voucher: null,
  message: '',
})

const successModal = reactive({
  visible: false,
  title: '',
  message: '',
})

const filters = reactive({
  keyword: '',
  discountType: 'ALL',
  status: 'ALL',
})

const defaultForm = {
  voucher_id: null,
  voucher_type: 'DISCOUNT',
  voucher_code: '',
  discount_value: null,
  discount_type: 'PERCENT',
  max_discount_amount: null,
  min_order_value: 0,
  quantity: 100,
  start_date: '',
  end_date: '',
  is_active: true,
  freeship_value: null,
  // Đối tượng sử dụng
  target_type: 'ALL', // 'ALL' hoặc 'SPECIFIC'
  selected_user_ids: [], // Danh sách user IDs khi chọn cụ thể
}

const form = reactive({...defaultForm})
const codeLength = computed(() => (form.voucher_code || '').length)

const todayDate = computed(() => {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 10)
})

const previewVoucher = computed(() => {
  const code = form.voucher_code.trim().toUpperCase() || 'SUMMER2026'
  const discountValue = Number(form.discount_value || 0)
  const maxDiscount = Number(form.max_discount_amount || 0)
  const minOrder = Number(form.min_order_value || 0)

  if (form.voucher_type === 'FREESHIP') {
    return {
      code,
      discountText: `Miễn phí vận chuyển lên đến ${formatCurrency(form.freeship_value || 0)}`,
      conditionText: minOrder > 0 ? `Áp dụng cho đơn từ ${formatCurrency(minOrder)}` : 'Áp dụng cho mọi đơn hàng',
    }
  }

  return {
    code,
    discountText:
      form.discount_type === 'PERCENT'
        ? `Giảm ${discountValue || 0}%${maxDiscount > 0 ? `, tối đa ${formatCurrency(maxDiscount)}` : ''}`
        : `Giảm trực tiếp ${formatCurrency(discountValue)}`,
    conditionText: minOrder > 0 ? `Áp dụng cho đơn từ ${formatCurrency(minOrder)}` : 'Áp dụng cho mọi đơn hàng',
  }
})

const filteredVouchers = computed(() => {
  // Helper: voucher coi như ngừng sử dụng nếu isActive=false HOẶC đã quá hạn (endDate < now)
  const isConsideredInactive = (voucher) => {
    if (voucher.isActive === false) return true
    if (voucher.endDate && new Date(voucher.endDate) < new Date()) return true
    return false
  }

  // Tôn trọng bộ lọc status của UI - không double-filter cứng isActive=true
  // BE đã trả về đúng dữ liệu theo filters.status rồi
  let result
  if (filters.status === 'ACTIVE') {
    result = vouchers.value.filter((voucher) => !isConsideredInactive(voucher))
  } else if (filters.status === 'INACTIVE') {
    result = vouchers.value.filter(isConsideredInactive)
  } else {
    // ALL: hiển thị tất cả
    result = vouchers.value
  }

  // Đẩy các voucher ngừng sử dụng / hết hạn xuống cuối danh sách
  return [...result].sort((a, b) => {
    const aInactive = isConsideredInactive(a) ? 1 : 0
    const bInactive = isConsideredInactive(b) ? 1 : 0
    return aInactive - bInactive
  })
})
const filteredUsers = computed(() => {
  if (!userSearchQuery.value) {
    return allUsers.value
  }
  const query = userSearchQuery.value.toLowerCase()
  return allUsers.value.filter(user => {
    const fullName = (user.fullName || '').toLowerCase()
    const username = (user.username || '').toLowerCase()
    const email = (user.email || '').toLowerCase()
    return fullName.includes(query) || username.includes(query) || email.includes(query)
  })
})

const errors = computed(() => {
  if (!isSubmitted.value) {
    return {}
  }

  const result = {}
  const voucherCode = form.voucher_code.trim().toUpperCase()

  if (!voucherCode) {
    result.voucher_code = 'Vui lòng nhập mã voucher'
  }

  const duplicated = vouchers.value.some((voucher) => {
    return (
      voucher.voucherCode?.toLowerCase() === voucherCode.toLowerCase() &&
      voucher.voucherId !== form.voucher_id
    )
  })

  if (voucherCode && duplicated) {
    result.voucher_code = 'Mã voucher đã tồn tại'
  }

  // Validation cho DISCOUNT
  if (form.voucher_type === 'DISCOUNT') {
    if (form.discount_value === null || form.discount_value === '' || Number(form.discount_value) <= 0) {
      result.discount_value = 'Giá trị giảm phải lớn hơn 0'
    }

    if (form.discount_type === 'PERCENT' && Number(form.discount_value) > 100) {
      result.discount_value = 'Giảm theo phần trăm không nên vượt quá 100%'
    }

    if (form.discount_type === 'PERCENT' &&
        (form.max_discount_amount === null || form.max_discount_amount === '' || Number(form.max_discount_amount) <= 0)) {
      result.max_discount_amount = 'Bắt buộc nhập số tiền giảm tối đa'
    }

    if (form.discount_type === 'AMOUNT' && form.max_discount_amount !== null) {
      result.max_discount_amount = 'Giảm tiền cố định không cần giới hạn tối đa, hãy để trống'
    }
  }

  // Validation cho FREESHIP
  if (form.voucher_type === 'FREESHIP') {
    if (form.freeship_value === null || form.freeship_value === '' || Number(form.freeship_value) <= 0) {
      result.freeship_value = 'Phí ship được miễn phí phải lớn hơn 0'
    }
  }

  // Validation cho đối tượng sử dụng
  if (form.target_type === 'SPECIFIC' && form.selected_user_ids.length === 0) {
    result.selected_user_ids = 'Vui lòng chọn ít nhất 1 khách hàng'
  }

  // Validation cho số lượng voucher
  if (!form.quantity || form.quantity < 1) {
    result.quantity = 'Số lượng phải lớn hơn 0'
  }

  if (Number(form.min_order_value) < 0) {
    result.min_order_value = 'Đơn tối thiểu không được âm'
  }

  if (!form.start_date) {
    result.start_date = 'Vui lòng chọn ngày bắt đầu'
  } else {
    const start = new Date(form.start_date)
    start.setHours(0, 0, 0, 0)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    if (start > today) {
      result.start_date = 'Ngày bắt đầu không được là ngày trong tương lai'
    }
  }

  if (!form.end_date) {
    result.end_date = 'Vui lòng chọn ngày kết thúc'
  }

  if (form.start_date && form.end_date) {
    const start = new Date(form.start_date)
    const end = new Date(form.end_date)

    if (end <= start) {
      result.time = 'Ngày kết thúc phải lớn hơn ngày bắt đầu'
    }
  }

  return {
    ...result,
    ...fieldErrors.value,
  }
})

// Load users for dropdown
async function loadUsers() {
  try {
    loadingUsers.value = true
    const res = await api.get('/admin/user/customers')
    // ApiResponse<Page<UserResponse>> -> res.data.data.content
    allUsers.value = res.data?.data?.content || []
  } catch (error) {
    console.error('Lỗi khi tải danh sách users:', error)
    allUsers.value = []
  } finally {
    loadingUsers.value = false
  }
}

function getUserDisplayName(userId) {
  const user = allUsers.value.find(u => u.userId === userId)
  return user ? (user.fullName || user.username) : `User #${userId}`
}

function removeUser(userId) {
  form.selected_user_ids = form.selected_user_ids.filter(id => id !== userId)
}

function formatTargetType(voucher) {
  if (voucher.targetType === 'ALL') {
    return 'Tất cả'
  }
  if (voucher.targetUserCount) {
    return `${voucher.targetUserCount} khách`
  }
  return 'Cụ thể'
}

watch(
  () => form.discount_type,
  (newType) => {
    if (newType === 'AMOUNT') {
      form.max_discount_amount = null
    }
  },
)

watch(
  () => form.voucher_type,
  () => {
    isSubmitted.value = false
    voucherStore.fieldErrors = {}
  }
)

watch(
  () => form.target_type,
  (newType) => {
    if (newType === 'ALL') {
      form.selected_user_ids = []
    }
  }
)

watch(
  () => [filters.keyword, filters.discountType, filters.status],
  () => {
    currentPage.value = 0
    loadVouchers()
  },
)

watch(pageSize, () => {
  currentPage.value = 0
  loadVouchers()
})

function generateVoucherCode() {
  const prefixes = {
    DISCOUNT: 'VC',
    FREESHIP: 'FS',
  }
  const prefix = prefixes[form.voucher_type] || 'VC'
  const datePart = new Date().toISOString().slice(2, 10).replaceAll('-', '')
  const randomPart = Math.random().toString(36).slice(2, 6).toUpperCase()
  form.voucher_code = `${prefix}${datePart}${randomPart}`
}

function showToast({type = 'success', title, message}) {
  toast.show = true
  toast.type = type
  toast.title = title
  toast.message = message

  window.setTimeout(() => {
    toast.show = false
  }, 2500)
}

function showSuccessModal({title, message}) {
  successModal.visible = true
  successModal.title = title
  successModal.message = message
}

function closeSuccessModal() {
  successModal.visible = false
  successModal.title = ''
  successModal.message = ''
}

function resetFilters() {
  filters.keyword = ''
  filters.discountType = 'ALL'
  filters.status = 'ALL'
}

function buildVoucherQuery() {
  return {
    page: currentPage.value,
    size: pageSize.value,
    keyword: filters.keyword || undefined,
    discountType: filters.discountType === 'ALL' ? undefined : filters.discountType,
    isActive:
      filters.status === 'ALL'
        ? undefined
        : filters.status === 'ACTIVE',
  }
}

function loadVouchers() {
  return voucherStore.fetchVouchers(buildVoucherQuery())
}

function goToPage(page) {
  if (page < 0 || page >= pagination.value.totalPages) {
    return
  }

  currentPage.value = page
  loadVouchers()
}

function resetForm() {
  isSubmitted.value = false
  isPreviewVisible.value = false
  voucherStore.fieldErrors = {}
  Object.assign(form, {...defaultForm})
  isEditing.value = false
  userSearchQuery.value = ''
  showUserDropdown.value = false
}

function openCreateModal() {
  resetForm()
  isModalOpen.value = true
}

function openEditModal(voucher) {
  isSubmitted.value = false
  voucherStore.fieldErrors = {}

  let voucherType = 'DISCOUNT'
  if (voucher.discountType === 'FREESHIP') {
    voucherType = 'FREESHIP'
  }

  Object.assign(form, {
    voucher_id: voucher.voucherId,
    voucher_type: voucherType,
    voucher_code: voucher.voucherCode,
    discount_value: voucher.discountValue,
    discount_type: voucher.discountType,
    max_discount_amount: voucher.maxDiscountAmount,
    min_order_value: voucher.minOrderValue,
    quantity: voucher.quantity || 1,
    start_date: voucher.startDate,
    end_date: voucher.endDate,
    is_active: voucher.isActive,
    freeship_value: voucher.freeshipValue || null,
    // Đối tượng sử dụng
    target_type: voucher.targetType || 'ALL',
    selected_user_ids: voucher.targetUserIds || [],
  })

  isEditing.value = true
  isModalOpen.value = true
}


function closeModal() {
  isModalOpen.value = false
}

function buildPayload() {
  const basePayload = {
    voucherCode: form.voucher_code.trim().toUpperCase(),
    minOrderValue: Number(form.min_order_value || 0),
    startDate: form.start_date,
    endDate: form.end_date,
    isActive: Boolean(form.is_active),
    targetType: form.target_type,
    quantity: Number(form.quantity),
  }

  if (form.target_type === 'SPECIFIC') {
    basePayload.targetUserIds = form.selected_user_ids
  }

  if (form.voucher_type === 'DISCOUNT') {
    return {
      ...basePayload,
      discountValue: Number(form.discount_value),
      discountType: form.discount_type,
      maxDiscountAmount: form.discount_type === 'AMOUNT' ? null : Number(form.max_discount_amount),
    }
  }

  if (form.voucher_type === 'FREESHIP') {
    return {
      ...basePayload,
      discountType: 'FREESHIP',
      discountValue: Number(form.freeship_value),
    }
  }
}

async function saveVoucher() {
  isSubmitted.value = true
  voucherStore.fieldErrors = {}

  if (Object.keys(errors.value).length > 0) {
    return
  }

  const voucherData = buildPayload()

  if (isEditing.value) {
    const success = await voucherStore.updateVoucher(form.voucher_id, voucherData)

    if (!success) {
      if (Object.keys(voucherStore.fieldErrors).length > 0) {
        return
      }

      showToast({
        type: 'error',
        title: 'Cập nhật voucher thất bại',
        message: voucherStore.error || 'Vui lòng kiểm tra lại dữ liệu.',
      })
      return
    }

    closeModal()
    resetForm()
    loadVouchers()

    showSuccessModal({
      title: 'Cập nhật voucher thành công',
      message: `Voucher ${voucherData.voucherCode} đã được cập nhật.`,
    })

    return
  }

  const success = await voucherStore.addVoucher(voucherData)

  if (!success) {
    if (Object.keys(voucherStore.fieldErrors).length > 0) {
      return
    }

    showToast({
      type: 'error',
      title: 'Thêm voucher thất bại',
      message: voucherStore.error || 'Vui lòng kiểm tra lại dữ liệu.',
    })
    return
  }

  closeModal()
  resetForm()
  loadVouchers()

  showSuccessModal({
    title: 'Thêm voucher thành công',
    message: `Voucher ${voucherData.voucherCode} đã được thêm.`,
  })
}

function deleteVoucher(voucher) {
  deleteConfirm.voucher = voucher
  deleteConfirm.message = `Bạn có chắc muốn ngừng hoạt động voucher ${voucher.voucherCode} không?`
  deleteConfirm.visible = true
}

function closeDeleteConfirm() {
  deleteConfirm.visible = false
  deleteConfirm.voucher = null
  deleteConfirm.message = ''
}

async function confirmDeleteVoucher() {
  const voucher = deleteConfirm.voucher

  if (!voucher) return

  const success = await voucherStore.deleteVoucherById(voucher.voucherId)

  if (!success) {
    closeDeleteConfirm()
    showToast({
      type: 'error',
      title: 'Ngừng hoạt động voucher thất bại',
      message: voucherStore.error || 'Vui lòng thử lại.',
    })
    return
  }

  closeDeleteConfirm()

  // Load lại để có totalElements / totalPages mới nhất từ server
  await loadVouchers()

  // Sau khi load: nếu currentPage vượt quá totalPages (do xóa hết các item ở trang cuối)
  // → lùi currentPage về trang cuối còn data
  if (currentPage.value >= pagination.value.totalPages) {
    currentPage.value = Math.max(0, pagination.value.totalPages - 1)
  }

  // Nếu vẫn còn data, load lại với currentPage mới (nếu có thay đổi)
  // để đảm bảo FE hiển thị đúng các item ở trang hiện tại
  if (pagination.value.totalElements > 0) {
    await loadVouchers()
  } else {
    // Hết sạch voucher → reset về trang 0
    currentPage.value = 0
    await loadVouchers()
  }

  showSuccessModal({
    title: 'Ngừng hoạt động voucher thành công',
    message: `Voucher ${voucher.voucherCode} đã được chuyển sang trạng thái ngừng hoạt động.`,
  })
}

function formatDiscountValue(voucher) {
  if (voucher.discountType === 'PERCENT') {
    return `${voucher.discountValue}%`
  }

  return formatCurrency(voucher.discountValue)
}

function formatDiscountType(discountType) {
  if (discountType === 'PERCENT') {
    return 'Giảm theo %'
  }

  if (discountType === 'AMOUNT') {
    return 'Giảm tiền'
  }

  if (discountType === 'FREESHIP') {
    return 'Free Ship'
  }

  return discountType || '-'
}

function formatCurrency(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(Number(value))
}

function formatNumberInput(value) {
  if (value === null || value === undefined || value === '') return ''
  const raw = String(value).replace(/[^\d]/g, '')
  if (!raw) return ''
  return Number(raw).toLocaleString('vi-VN')
}

function parseNumberInput(value) {
  if (typeof value === 'number') return value
  if (typeof value === 'string') return Number(value.replace(/[^\d]/g, '')) || 0
  return 0
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<style scoped>

</style>
