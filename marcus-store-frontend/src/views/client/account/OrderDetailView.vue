<template>
  <main class="client-order-page">
    <section class="client-order-shell">
      <div class="main-card">
        <div class="main-header-card">
          <div>
            <h2 class="main-title">
              <i class="fa-solid fa-box-open"></i>
              Chi tiết đơn hàng
            </h2>
            <p class="main-note">Theo dõi trạng thái, sản phẩm và thông tin giao hàng.</p>
          </div>
          <router-link class="back-link" to="/profile/orders">
            <i class="fa-solid fa-arrow-left"></i>
            Danh sách đơn
          </router-link>
        </div>

        <div class="main-body">
          <div v-if="loading" class="empty-state">Đang tải chi tiết đơn hàng...</div>
          <div v-else-if="error" class="empty-state text-danger">{{ error }}</div>

          <section v-else-if="selectedOrder" class="detail-stack">
            <div class="detail-header">
              <div class="detail-heading">
                <div>
                  <h3 class="detail-title">{{ selectedOrder.orderCode }}</h3>
                  <p class="detail-subtitle">
                    Ngày tạo: {{ formatDateTime(selectedOrder.createdAt) }}
                  </p>
                </div>
                <span
                  class="status-pill"
                  :class="statusConfig[selectedOrder.orderStatus]?.className"
                >
                  <i class="fa-solid" :class="statusConfig[selectedOrder.orderStatus]?.icon"></i>
                  {{ statusConfig[selectedOrder.orderStatus]?.label || selectedOrder.orderStatus }}
                </span>
              </div>

              <div class="meta-grid">
                <div class="meta-item">
                  <span class="meta-label"><i class="fa-solid fa-hashtag"></i>Mã đơn</span>
                  <strong class="meta-value">{{ selectedOrder.orderCode }}</strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"
                    ><i class="fa-solid fa-credit-card"></i>Phương thức thanh toán</span
                  >
                  <strong class="meta-value">{{
                    getPaymentMethodLabel(selectedOrder.paymentMethod)
                  }}</strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"
                    ><i class="fa-solid fa-circle-check"></i>Trạng thái TT</span
                  >
                  <strong class="meta-value">
                    {{ getPaymentStatusLabel(selectedOrder.paymentStatus) }}
                  </strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"><i class="fa-solid fa-truck-fast"></i>Mã vận đơn</span>
                  <strong class="meta-value">{{ selectedOrder.trackingCode || 'Chưa có' }}</strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"><i class="fa-solid fa-clock"></i>Cập nhật</span>
                  <strong class="meta-value">{{ selectedOrder.updatedAt || '---' }}</strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"
                    ><i class="fa-solid fa-calendar-check"></i>Thời gian TT</span
                  >
                  <strong class="meta-value">{{ displayPaymentDate || '---' }}</strong>
                </div>
              </div>

              <div class="timeline-body">
                <div
                  class="timeline"
                  :style="{
                    '--progress-width': timelineProgress,
                    '--timeline-columns': visibleTimelineSteps.length,
                  }"
                >
                  <div class="timeline-progress"></div>
                  <div
                    v-for="(step, index) in visibleTimelineSteps"
                    :key="step.key"
                    class="step"
                    :class="getStepClass(step, index)"
                  >
                    <div class="step-icon">
                      <i class="fa-solid" :class="step.icon"></i>
                    </div>

                    <div class="step-title">{{ step.title }}</div>
                    <div class="step-time">
                      {{ step.isCurrent ? 'Hiện tại' : formatDateTime(step.createdAt) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Marcus thêm thẻ theo dõi refund cho khách, không hiển thị dữ liệu kỹ thuật VNPAY. -->
            <section v-if="refund" class="refund-tracking-card">
              <div class="refund-icon"><i class="fa-solid fa-rotate-left"></i></div>
              <div class="refund-content">
                <div class="refund-heading">
                  <div>
                    <h3>Tiến trình hoàn tiền</h3>
                    <p>{{ refundStatusConfig[refund.status]?.description }}</p>
                  </div>
                  <span class="refund-pill" :class="refundStatusConfig[refund.status]?.className">
                    {{ refundStatusConfig[refund.status]?.label || refund.status }}
                  </span>
                </div>
                <div class="refund-values">
                  <div>
                    <span>Số tiền hoàn</span><strong>{{ formatMoney(refund.amount) }}</strong>
                  </div>
                  <div>
                    <span>Phí vận chuyển không hoàn</span
                    ><strong>{{ formatMoney(refund.shippingDeducted) }}</strong>
                  </div>
                  <div>
                    <span>Ngày yêu cầu</span><strong>{{ formatDateTime(refund.createdAt) }}</strong>
                  </div>
                </div>
                <p class="refund-reason"><strong>Lý do:</strong> {{ refund.reason }}</p>
              </div>
            </section>

            <div class="detail-grid">
              <div class="inner-stack">
                <section class="panel">
                  <div class="panel-header">
                    <h3 class="panel-title">
                      <i class="fa-solid fa-mobile-screen-button"></i>
                      Sản phẩm trong đơn
                    </h3>
                  </div>

                  <div class="product-list">
                    <article
                      v-for="item in selectedOrder.items || []"
                      :key="item.skuId || item.skuCode"
                      class="product-item"
                    >
                      <div class="product-thumb">
                        <img
                          v-if="item.productImage"
                          class="product-image"
                          :src="item.productImage"
                          :alt="item.productName"
                        />
                        <i v-else class="fa-solid fa-mobile-screen"></i>
                      </div>
                      <div>
                        <h4 class="product-name">{{ item.productName }}</h4>
                        <div v-if="getVariantText(item)" class="product-variants">
                          <i class="fa-solid fa-tags"></i>
                          <span>{{ getVariantText(item) }}</span>
                        </div>
                        <div class="product-meta">
                          <span
                            >SKU: <strong>{{ item.skuCode }}</strong></span
                          >
                          <span
                            >Số lượng: <strong>{{ item.quantity }}</strong></span
                          >
                          <span v-if="item.isFlashSale && item.originalPrice">
                            <span class="price-original">{{
                              formatMoney(item.originalPrice)
                            }}</span>
                            <span class="price-flashsale">{{
                              formatMoney(item.priceAtPurchase)
                            }}</span>
                            <span v-if="item.flashSaleSlotName" class="flash-badge">{{
                              item.flashSaleSlotName
                            }}</span>
                          </span>
                          <span v-else
                            >Đơn giá: <strong>{{ formatMoney(item.priceAtPurchase) }}</strong></span
                          >
                        </div>
                      </div>
                      <strong class="product-total">{{ formatMoney(item.lineTotal) }}</strong>
                      <!-- ===== Vùng thao tác đánh giá: tạo mới / xem / sửa ===== -->
                      <div v-if="selectedOrder.orderStatus === 'COMPLETED'" class="review-action">
                        <!-- chưa đánh giá -->
                        <button v-if="!item.reviewed" class="review-btn" @click="openReview(item)">
                          Đánh giá
                        </button>

                        <!-- đã đánh giá -->
                        <template v-else>
                          <button type="button" class="review-view-btn" @click="goToProduct(item)">
                            <i class="fa-solid fa-arrow-up-right-from-square"></i>
                            Xem đánh giá
                          </button>

                          <button class="review-edit-btn" @click="editReview(item)">
                            Sửa đánh giá
                          </button>
                        </template>

                        <!-- ===== Nút Yêu cầu bảo hành ===== -->
                        <button
                          v-if="canRequestWarranty(item)"
                          class="warranty-btn"
                          @click="openWarrantyModal(item)"
                        >
                          <i class="fa-solid fa-shield-halved"></i>
                          Yêu cầu bảo hành
                        </button>

                        <!-- Nút Xem bảo hành (khi đã có yêu cầu) -->
                        <button
                          v-else-if="item.warrantyStatus"
                          class="warranty-view-btn"
                          @click="openWarrantyModal(item)"
                        >
                          <i class="fa-solid fa-eye"></i>
                          Xem bảo hành
                        </button>

                        <!-- Hiển thị trạng thái bảo hành nếu đã có yêu cầu -->
                        <template v-if="item.warrantyStatus">
                          <span
                            class="warranty-status-pill"
                            :class="getWarrantyStatus(item).className"
                          >
                            <i :class="getWarrantyStatus(item).icon"></i>
                            {{ getWarrantyStatus(item).label }}
                          </span>
                        </template>
                      </div>
                      <!-- ===== Hết vùng thao tác đánh giá ===== -->
                    </article>
                  </div>
                </section>

                <section class="panel">
                  <div class="panel-header">
                    <h3 class="panel-title">
                      <i class="fa-solid fa-clock-rotate-left"></i>
                      Lịch sử trạng thái
                    </h3>
                  </div>

                  <div class="history-list">
                    <div
                      v-for="item in displayHistory"
                      :key="`${item.status}-${item.title}-${item.createdAt}`"
                      class="history-item"
                    >
                      <div class="history-icon">
                        <i
                          class="fa-solid"
                          :class="statusConfig[item.status]?.icon || 'fa-circle-info'"
                        ></i>
                      </div>
                      <div>
                        <div class="history-title">{{ item.title }}</div>
                        <div class="history-meta">
                          {{ statusConfig[item.status]?.label || item.status }} ·
                          {{ formatDateTime(item.createdAt) }}
                        </div>
                        <div v-if="item.note" class="history-note">
                          <span>Lý do:</span> {{ item.note }}
                        </div>
                      </div>
                    </div>
                  </div>
                </section>
              </div>

              <div class="inner-stack">
                <section class="panel">
                  <div class="panel-header">
                    <h3 class="panel-title">
                      <i class="fa-solid fa-location-dot"></i>
                      Thông tin giao hàng
                    </h3>
                  </div>

                  <div class="shipping-body">
                    <div class="receiver-card">
                      <div class="receiver-icon">
                        <i class="fa-solid fa-user"></i>
                      </div>
                      <div>
                        <div class="receiver-name">{{ selectedOrder.recipientName }}</div>
                        <div class="receiver-phone">
                          <i class="fa-solid fa-phone-volume"></i>
                          {{ selectedOrder.recipientPhone }}
                        </div>
                      </div>
                    </div>

                    <div class="info-line">
                      <i class="fa-solid fa-map-location-dot"></i>
                      <div>
                        <div class="info-label">
                          {{
                            selectedOrder.fulfillmentMethod === 'STORE_PICKUP'
                              ? 'Nhận tại cửa hàng'
                              : 'Địa chỉ nhận hàng'
                          }}
                        </div>
                        <div class="info-value">{{ selectedOrder.shippingAddress }}</div>
                      </div>
                    </div>

                    <div
                      class="info-line"
                      v-if="selectedOrder.fulfillmentMethod !== 'STORE_PICKUP'"
                    >
                      <i class="fa-solid fa-truck-ramp-box"></i>
                      <div>
                        <div class="info-label">Mã vận đơn</div>
                        <div class="info-value">
                          {{ selectedOrder.trackingCode || 'Chưa có mã vận đơn' }}
                        </div>
                      </div>
                    </div>
                    <div class="info-line" v-if="selectedOrder.deliveryNote">
                      <i class="fa-solid fa-clipboard-user"></i>
                      <div>
                        <div class="info-label">Ghi chú của bạn</div>
                        <div class="info-value" style="font-style: italic">
                          "{{ selectedOrder.deliveryNote }}"
                        </div>
                      </div>
                    </div>
                  </div>
                </section>

                <section class="panel">
                  <div class="panel-header">
                    <h3 class="panel-title">
                      <i class="fa-solid fa-file-invoice-dollar"></i>
                      Tổng thanh toán
                    </h3>
                  </div>

                  <div class="summary-body">
                    <!-- NÂNG CẤP: Bảng tóm tắt tài chính chuẩn xác cho Client -->
                    <div class="summary-row">
                      <span>Tạm tính</span>
                      <strong>{{ formatMoney(selectedOrder.totalAmount) }}</strong>
                    </div>

                    <div class="summary-row discount" v-if="selectedOrder.discountAmount > 0">
                      <span>Giảm giá Voucher</span>
                      <strong>-{{ formatMoney(selectedOrder.discountAmount) }}</strong>
                    </div>

                    <div class="summary-row">
                      <span>Phí vận chuyển</span>
                      <strong>{{ formatMoney(selectedOrder.shippingFee) }}</strong>
                    </div>

                    <div
                      class="summary-row discount"
                      v-if="selectedOrder.shippingSubsidy > 0"
                      style="color: #10b981"
                    >
                      <span>Trợ giá vận chuyển</span>
                      <strong>-{{ formatMoney(selectedOrder.shippingSubsidy) }}</strong>
                    </div>

                    <div class="summary-total">
                      <div class="summary-row">
                        <span>Thanh toán</span>
                        <strong>{{ formatMoney(selectedOrder.finalAmount) }}</strong>
                      </div>
                    </div>

                    <div class="detail-actions">
                      <button
                        v-if="canConfirmReceipt"
                        type="button"
                        class="btn-dark btn-confirm-received"
                        :disabled="confirmingReceipt"
                        @click="openReceiptConfirmation"
                      >
                        <i
                          class="fa-solid"
                          :class="confirmingReceipt ? 'fa-spinner fa-spin' : 'fa-circle-check'"
                        ></i>
                        {{
                          confirmingReceipt
                            ? 'Đang xác nhận...'
                            : selectedOrder.fulfillmentMethod === 'STORE_PICKUP'
                              ? 'Tôi đã nhận hàng tại cửa hàng'
                              : 'Tôi đã nhận được hàng'
                        }}
                      </button>
                      <button
                        type="button"
                        class="btn-dark btn-cancel-order"
                        :disabled="cancelling || !canCancelOrder"
                        @click="handleCancelOrder"
                      >
                        <i v-if="cancelling" class="fa-solid fa-spinner fa-spin"></i>
                        <i v-else class="fa-solid fa-ban"></i>
                        {{ cancelling ? 'Đang hủy đơn...' : 'Hủy đơn hàng' }}
                      </button>
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </section>
        </div>
      </div>
      <div v-if="cancelModal.open" class="modal-backdrop" @click.self="closeCancelModal">
        <div class="modal-card" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h4 class="modal-title">
              <i class="fa-solid fa-ban"></i>
              Hủy đơn hàng
            </h4>
            <button
              type="button"
              class="modal-close"
              :disabled="cancelling"
              @click="closeCancelModal"
            >
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>

          <div class="modal-body">
            <p class="modal-text">
              Vui lòng cho Marcus Store biết lý do bạn muốn hủy đơn

              <strong>{{ cancelModal.orderCode }}</strong
              >.
            </p>
            <!-- Marcus thêm danh sách lý do chuẩn để phục vụ thống kê/AI; chỉ dùng
                 textarea khi khách chọn "Lý do khác". -->
            <div class="cancel-reason-grid">
              <button
                v-for="reason in CUSTOMER_CANCEL_REASONS"
                :key="reason"
                type="button"
                class="cancel-reason-option"
                :class="{ active: cancelModal.selectedReason === reason }"
                :disabled="cancelling"
                @click="selectCancelReason(reason)"
              >
                {{ reason }}
              </button>
            </div>
            <textarea
              v-if="cancelModal.selectedReason === OTHER_CANCEL_REASON"
              v-model="cancelModal.reason"
              class="modal-input"
              rows="3"
              maxlength="500"
              placeholder="Ví dụ: Đặt nhầm size, đổi ý không muốn mua nữa..."
              :disabled="cancelling"
            ></textarea>

            <div class="modal-counter">{{ cancelModal.reason.length }}/500</div>

            <div
              v-if="cancelModal.feedback.message"
              class="modal-feedback"
              :class="`modal-feedback-${cancelModal.feedback.type}`"
              role="alert"
            >
              <i
                class="fa-solid"
                :class="
                  cancelModal.feedback.type === 'error'
                    ? 'fa-circle-exclamation'
                    : 'fa-circle-check'
                "
              ></i>
              <span>{{ cancelModal.feedback.message }}</span>
            </div>
          </div>

          <div class="modal-footer">
            <button
              type="button"
              class="btn-ghost"
              :disabled="cancelling"
              @click="closeCancelModal"
            >
              Quay lại
            </button>
            <button
              type="button"
              class="btn-danger"
              :disabled="cancelling || !cancelModal.reason.trim()"
              @click="confirmCancelOrder"
            >
              <i v-if="cancelling" class="fa-solid fa-spinner fa-spin"></i>
              <i v-else class="fa-solid fa-check"></i>
              {{ cancelling ? 'Đang xử lý...' : 'Xác nhận hủy' }}
            </button>
          </div>
        </div>
      </div>

      <!-- ===== Modal Yêu cầu / Xem bảo hành ===== -->
      <div v-if="warrantyModal.visible" class="modal-backdrop" @click.self="closeWarrantyModal">
        <div class="modal-card warranty-modal" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h4 class="modal-title">
              <i class="fa-solid fa-shield-halved"></i>
              <!-- VIEW mode: Hiển thị chi tiết -->
              <template v-if="warrantyModal.mode === 'view'">
                Chi tiết bảo hành
              </template>
              <!-- CREATE mode: Tạo mới -->
              <template v-else-if="warrantyModal.mode === 'create'">
                Yêu cầu đổi trả bảo hành
              </template>
              <!-- LOADING mode -->
              <template v-else>
                Đang tải...
              </template>
            </h4>
            <button
              type="button"
              class="modal-close"
              :disabled="warrantySubmitting"
              @click="closeWarrantyModal"
            >
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>

          <div class="modal-body" v-if="warrantyModal.mode === 'loading'">
            <div class="warranty-loading">
              <i class="fa-solid fa-spinner fa-spin"></i>
              <span>Đang kiểm tra thông tin bảo hành...</span>
            </div>
          </div>

          <template v-else>
            <!-- Thông tin sản phẩm -->
            <div class="warranty-product-card">
              <div class="warranty-product-thumb">
                <img
                  v-if="warrantyModal.selectedItem?.productImage"
                  :src="warrantyModal.selectedItem.productImage"
                  :alt="warrantyModal.selectedItem?.productName"
                  @error="handleImageError"
                />
                <div v-else class="warranty-product-thumb-placeholder">
                  <i class="fa-solid fa-mobile-screen-button"></i>
                </div>
              </div>
              <div class="warranty-product-info-content">
                <h5 class="warranty-product-title">
                  {{ warrantyModal.selectedItem?.productName }}
                </h5>
                <div class="warranty-product-specs">
                  <span class="spec-item">
                    <i class="fa-solid fa-barcode"></i>
                    SKU: {{ warrantyModal.selectedItem?.skuCode }}
                  </span>
                  <span class="spec-item">
                    <i class="fa-solid fa-cube"></i>
                    SL: {{ warrantyModal.selectedItem?.quantity }}
                  </span>
                </div>
                <div class="warranty-product-price-row">
                  <span class="price-label">Đơn giá:</span>
                  <span class="price-value">{{ formatMoney(warrantyModal.selectedItem?.priceAtPurchase) }}</span>
                </div>
              </div>
            </div>

            <!-- ===== VIEW MODE: Hiển thị thông tin đã gửi ===== -->
            <template v-if="warrantyModal.mode === 'view' && existingWarranty">
              <!-- Trạng thái bảo hành -->
              <div class="warranty-status-card" :class="getWarrantyStatusClass(existingWarranty.status)">
                <div class="warranty-status-header">
                  <i :class="getWarrantyStatusIcon(existingWarranty.status)"></i>
                  <span class="warranty-status-label">{{ existingWarranty.statusLabel }}</span>
                </div>
                <div class="warranty-status-meta">
                  <span><i class="fa-solid fa-calendar"></i> Ngày gửi: {{ formatDateTime(existingWarranty.createdAt) }}</span>
                  <span v-if="existingWarranty.processedAt"><i class="fa-solid fa-clock"></i> Xử lý: {{ formatDateTime(existingWarranty.processedAt) }}</span>
                </div>
                <div v-if="existingWarranty.processedByName" class="warranty-status-admin">
                  <i class="fa-solid fa-user-shield"></i>
                  {{ existingWarranty.processedByName }}
                </div>
              </div>

              <!-- Thông tin đã gửi -->
              <div class="warranty-view-section">
                <div class="warranty-view-item">
                  <label class="warranty-view-label">Lý do:</label>
                  <span class="warranty-view-value">{{ existingWarranty.reasonLabel }}</span>
                </div>

                <div class="warranty-view-item">
                  <label class="warranty-view-label">Mô tả chi tiết:</label>
                  <p class="warranty-view-description">{{ existingWarranty.description }}</p>
                </div>

                <!-- Ảnh/Video đã gửi -->
                <div v-if="existingWarranty.attachments?.length > 0" class="warranty-view-item">
                  <label class="warranty-view-label">Ảnh/Video đã gửi:</label>
                  <div class="warranty-view-attachments">
                    <div
                      v-for="(att, index) in existingWarranty.attachments"
                      :key="index"
                      class="warranty-view-attachment"
                    >
                      <img v-if="att.fileType === 'IMAGE'" :src="att.fileUrl" :alt="att.fileName" />
                      <video v-else :src="att.fileUrl" controls></video>
                    </div>
                  </div>
                </div>

                <!-- Ghi chú từ admin -->
                <div v-if="existingWarranty.adminNote" class="warranty-view-item">
                  <label class="warranty-view-label"><i class="fa-solid fa-comment-dots"></i> Phản hồi từ cửa hàng:</label>
                  <div class="warranty-admin-note">{{ existingWarranty.adminNote }}</div>
                </div>
              </div>

              <!-- Thông báo VIEW mode -->
              <div class="warranty-notice">
                <div class="warranty-notice-icon">
                  <i class="fa-solid fa-circle-info"></i>
                </div>
                <div class="warranty-notice-text">
                  <strong>Đã gửi yêu cầu.</strong> Bạn chỉ có thể xem lại thông tin đã gửi. Nếu cần hỗ trợ thêm, vui lòng liên hệ cửa hàng.
                </div>
              </div>
            </template>

            <!-- ===== CREATE MODE: Form nhập liệu ===== -->
            <template v-else-if="warrantyModal.mode === 'create'">
              <!-- Loại yêu cầu -->
              <div class="warranty-form-group">
                <label class="warranty-label">
                  Loại yêu cầu <span class="required">*</span>
                </label>
                <div class="warranty-type-options">
                  <label
                    class="warranty-type-option"
                    :class="{ active: warrantyForm.type === 'EXCHANGE' }"
                  >
                    <input
                      v-model="warrantyForm.type"
                      type="radio"
                      value="EXCHANGE"
                      class="warranty-type-radio"
                    />
                    <div class="warranty-type-content">
                      <div class="warranty-type-icon">
                        <i class="fa-solid fa-repeat"></i>
                      </div>
                      <span class="warranty-type-title">Đổi sản phẩm mới</span>
                      <span class="warranty-type-desc">Nhận sản phẩm thay thế</span>
                      <div v-if="warrantyForm.type === 'EXCHANGE'" class="warranty-type-check">
                        <i class="fa-solid fa-check"></i>
                      </div>
                    </div>
                  </label>
                  <label
                    class="warranty-type-option"
                    :class="{ active: warrantyForm.type === 'REFUND' }"
                  >
                    <input
                      v-model="warrantyForm.type"
                      type="radio"
                      value="REFUND"
                      class="warranty-type-radio"
                    />
                    <div class="warranty-type-content">
                      <div class="warranty-type-icon">
                        <i class="fa-solid fa-coins"></i>
                      </div>
                      <span class="warranty-type-title">Bảo hành</span>
                      <span class="warranty-type-desc">Gửi yêu cầu bảo hành sản phẩm</span>
                      <div v-if="warrantyForm.type === 'REFUND'" class="warranty-type-check">
                        <i class="fa-solid fa-check"></i>
                      </div>
                    </div>
                  </label>
                </div>
              </div>

              <!-- Lý do -->
              <div class="warranty-form-group">
                <label class="warranty-label">
                  Lý do <span class="required">*</span>
                </label>
                <div class="warranty-reason-grid">
                  <button
                    v-for="reason in WARRANTY_REASONS"
                    :key="reason.value"
                    type="button"
                    class="warranty-reason-option"
                    :class="{ active: warrantyForm.reason === reason.value }"
                    :disabled="warrantySubmitting"
                    @click="warrantyForm.reason = reason.value"
                  >
                    <i :class="reason.icon"></i>
                    <span>{{ reason.label }}</span>
                    <div v-if="warrantyForm.reason === reason.value" class="reason-check">
                      <i class="fa-solid fa-check"></i>
                    </div>
                  </button>
                </div>
              </div>

              <!-- Mô tả chi tiết -->
              <div class="warranty-form-group">
                <label class="warranty-label">
                  Mô tả chi tiết vấn đề <span class="required">*</span>
                </label>
                <textarea
                  v-model="warrantyForm.description"
                  class="warranty-textarea"
                  rows="4"
                  maxlength="500"
                  placeholder="Mô tả chi tiết vấn đề bạn gặp phải với sản phẩm (ví dụ: màn hình bị chết pixel, loa có tiếng ồn lạ...)"
                  :disabled="warrantySubmitting"
                ></textarea>
                <div class="warranty-textarea-footer">
                  <span class="warranty-char-count">{{ warrantyForm.description.length }}/500</span>
                  <button
                    type="button"
                    class="warranty-attach-btn"
                    :disabled="warrantySubmitting"
                    @click="triggerFileInput"
                  >
                    <i class="fa-solid fa-camera"></i>
                    Đính kèm ảnh/video
                  </button>
                  <input
                    ref="warrantyFileInput"
                    type="file"
                    accept="image/*,video/*"
                    multiple
                    class="warranty-file-input"
                    @change="handleFileSelect"
                  />
                </div>
                <p class="warranty-attach-hint">
                  Vui lòng cập nhật ít nhất một video và một ảnh
                </p>
                <!-- Preview files đã chọn -->
                <div v-if="warrantyForm.attachments.length > 0" class="warranty-attachments-preview">
                  <div
                    v-for="(file, index) in warrantyForm.attachments"
                    :key="index"
                    class="attachment-item"
                  >
                    <img v-if="file.preview" :src="file.preview" :alt="file.name" />
                    <video v-else-if="file.type.startsWith('video')" :src="file.preview" />
                    <i v-else class="fa-solid fa-file"></i>
                    <button
                      type="button"
                      class="attachment-remove"
                      @click="removeAttachment(index)"
                    >
                      <i class="fa-solid fa-xmark"></i>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Lưu ý -->
              <div class="warranty-notice">
                <div class="warranty-notice-icon">
                  <i class="fa-solid fa-circle-info"></i>
                </div>
                <div class="warranty-notice-text">
                  <strong>Lưu ý:</strong> Chúng tôi chỉ chấp nhận đổi trả bảo hành cho các sản phẩm còn trong thời gian bảo hành và lỗi phải thuộc về nhà sản xuất. Chúng tôi không chịu trách nhiệm nếu lỗi sản phẩm do người dùng gây ra.
                </div>
              </div>
            </template>

            <!-- Feedback -->
            <div
              v-if="warrantyModal.feedback.message"
              class="warranty-feedback"
              :class="`warranty-feedback-${warrantyModal.feedback.type}`"
              role="alert"
            >
              <i
                class="fa-solid"
                :class="
                  warrantyModal.feedback.type === 'error'
                    ? 'fa-circle-exclamation'
                    : 'fa-circle-check'
                "
              ></i>
              <span>{{ warrantyModal.feedback.message }}</span>
            </div>
          </template>

          <div class="modal-footer">
            <button
              type="button"
              class="btn-ghost"
              :disabled="warrantySubmitting"
              @click="closeWarrantyModal"
            >
              <i class="fa-solid fa-xmark"></i>
              {{ warrantyModal.mode === 'view' ? 'Đóng' : 'Hủy' }}
            </button>
            <!-- Nút gửi chỉ hiển thị ở CREATE mode -->
            <button
              v-if="warrantyModal.mode === 'create'"
              type="button"
              class="btn-primary"
              :disabled="!canSubmitWarranty || warrantySubmitting"
              @click="submitWarranty"
            >
              <i v-if="warrantySubmitting" class="fa-solid fa-spinner fa-spin"></i>
              <i v-else class="fa-solid fa-paper-plane"></i>
              {{ warrantySubmitting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
            </button>
          </div>
        </div>
      </div>
      <!-- ===== Hết Modal Yêu cầu bảo hành ===== -->

      <BaseModal
        :visible="receiptModal.visible"
        :type="receiptModal.type"
        :title="receiptModal.title"
        :message="receiptModal.message"
        @close="closeReceiptModal"
        @confirm="confirmReceivedOrder"
      />
    </section>
    <!-- ===== Modal đánh giá: tạo mới / xem / sửa ===== -->
    <review-modal
      v-model="showReviewModal"
      :order-item="selectedOrderItem"
      :edit-mode="editMode"
      :view-only="viewOnly"
      @success="reviewSuccess"
    />
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import UserOrderApi from '@/api/userOrder.js'
import WarrantyApi from '@/api/warrantyApi.js'
import '@/assets/css/OrderDetailView.css'
import ReviewModal from './ReviewModal.vue'
import reviewService from '@/stores/reviewService'
import cloudinaryService from '@/stores/cloudinaryService'
import BaseModal from '@/components/BaseModal.vue'

// ===== Marcus thêm state bảo hành =====
const selectedOrder = ref(null)
const showReviewModal = ref(false)
const editMode = ref(false)
const viewOnly = ref(false)
const selectedOrderItem = ref(null)
const refund = ref(null)
const loading = ref(false)
const error = ref(null)
const route = useRoute()
let refundPollingTimer = null

// ===== State modal bảo hành =====
const warrantyModal = ref({
  visible: false,
  selectedItem: null,
  mode: 'create', // 'create' | 'view'
  feedback: { type: '', message: '' },
})

const existingWarranty = ref(null) // Lưu thông tin warranty đã gửi

const warrantyForm = ref({
  type: 'EXCHANGE',
  reason: '',
  description: '',
  attachments: [], // Array of { file, preview, name, type }
})

const warrantySubmitting = ref(false)
const warrantyFileInput = ref(null)
const warrantyDescriptionMinLength = 10

// Các lý do bảo hành
const WARRANTY_REASONS = [
  { value: 'DEFECTIVE', label: 'Sản phẩm lỗi', icon: 'fa-solid fa-triangle-exclamation' },
  { value: 'DAMAGED', label: 'Bị hư hỏng', icon: 'fa-solid fa-kit-medical' },
  { value: 'WRONG_ITEM', label: 'Giao sai sản phẩm', icon: 'fa-solid fa-right-left' },
  { value: 'NOT_AS_DESCRIBED', label: 'Không đúng mô tả', icon: 'fa-solid fa-circle-question' },
  { value: 'ACCESSORY_MISSING', label: 'Thiếu phụ kiện', icon: 'fa-solid fa-box-open' },
  { value: 'OTHER', label: 'Lý do khác', icon: 'fa-solid fa-ellipsis' },
]

// Kiểm tra điều kiện hiển thị nút yêu cầu bảo hành
function canRequestWarranty(item) {
  const order = selectedOrder.value
  if (!order) return false
  
  // Chỉ đơn COMPLETED mới được yêu cầu bảo hành
  if (order.orderStatus !== 'COMPLETED') return false
  
  // Kiểm tra trong thời hạn bảo hành (6 tháng)
  const warrantyPeriodMonths = 6
  const orderDate = new Date(order.createdAt)
  const expiryDate = new Date()
  expiryDate.setMonth(expiryDate.getMonth() - warrantyPeriodMonths)
  if (orderDate < expiryDate) return false
  
  // Kiểm tra sản phẩm chưa có yêu cầu BH đang xử lý
  if (item.hasActiveWarrantyRequest) return false
  
  return true
}

// Lấy trạng thái bảo hành của sản phẩm (nếu có)
function getWarrantyStatus(item) {
  if (!item.warrantyStatus) return null
  
  const statusConfig = {
    PENDING: {
      label: 'Chờ xử lý BH',
      className: 'warranty-pending',
      icon: 'fa-solid fa-clock',
    },
    APPROVED: {
      label: 'Đã duyệt BH',
      className: 'warranty-approved',
      icon: 'fa-solid fa-circle-check',
    },
    REJECTED: {
      label: 'Từ chối BH',
      className: 'warranty-rejected',
      icon: 'fa-solid fa-circle-xmark',
    },
  }
  
  return statusConfig[item.warrantyStatus] || null
}

// Lấy CSS class cho trạng thái bảo hành (VIEW mode)
function getWarrantyStatusClass(status) {
  const classMap = {
    PENDING: 'warranty-status-pending',
    APPROVED: 'warranty-status-approved',
    REJECTED: 'warranty-status-rejected',
    COMPLETED: 'warranty-status-completed',
  }
  return classMap[status] || 'warranty-status-pending'
}

// Lấy icon cho trạng thái bảo hành (VIEW mode)
function getWarrantyStatusIcon(status) {
  const iconMap = {
    PENDING: 'fa-solid fa-clock',
    APPROVED: 'fa-solid fa-circle-check',
    REJECTED: 'fa-solid fa-circle-xmark',
    COMPLETED: 'fa-solid fa-flag-checkered',
  }
  return iconMap[status] || 'fa-solid fa-question-circle'
}

// Mở modal bảo hành - tự detect mode
async function openWarrantyModal(item) {
  warrantyModal.value = {
    visible: true,
    selectedItem: item,
    mode: 'loading', // đang kiểm tra
    feedback: { type: '', message: '' },
  }
  existingWarranty.value = null
  warrantyForm.value = {
    type: 'EXCHANGE',
    reason: '',
    description: '',
    attachments: [],
  }
  
  // Kiểm tra xem đã có yêu cầu BH chưa
  try {
    const orderItemId = item.orderItemId || item.skuId
    const response = await WarrantyApi.getWarrantyByOrderItem(orderItemId)
    existingWarranty.value = response.data.data
    warrantyModal.value.mode = 'view'
    
    // Điền thông tin đã gửi vào form (chỉ để hiển thị, không cho sửa)
    warrantyForm.value.reason = existingWarranty.value.reason
    warrantyForm.value.description = existingWarranty.value.description
  } catch (error) {
    // 404 = chưa có yêu cầu BH, cho phép tạo mới
    if (error.response?.status === 404) {
      warrantyModal.value.mode = 'create'
    } else {
      warrantyModal.value.mode = 'create'
      console.error('Lỗi khi kiểm tra bảo hành:', error)
    }
  }
}

// Đóng modal bảo hành
function closeWarrantyModal() {
  if (warrantySubmitting.value) return
  warrantyModal.value.visible = false
}

// Xử lý lỗi ảnh fallback
function handleImageError(event) {
  event.target.style.display = 'none'
  event.target.parentElement.querySelector('.warranty-product-thumb-placeholder')?.classList.add('show')
}

// Trigger file input
function triggerFileInput() {
  warrantyFileInput.value?.click()
}

// Xử lý chọn file
function handleFileSelect(event) {
  const files = Array.from(event.target.files)
  files.forEach((file) => {
    if (warrantyForm.value.attachments.length >= 5) return // Max 5 files
    const reader = new FileReader()
    reader.onload = (e) => {
      warrantyForm.value.attachments.push({
        file,
        preview: e.target.result,
        name: file.name,
        type: file.type,
      })
    }
    reader.readAsDataURL(file)
  })
  // Reset input
  event.target.value = ''
}

// Xóa attachment
function removeAttachment(index) {
  warrantyForm.value.attachments.splice(index, 1)
}

// Kiểm tra form có thể submit
const canSubmitWarranty = computed(() => {
  return (
    warrantyForm.value.reason &&
    warrantyForm.value.description.trim().length >= warrantyDescriptionMinLength
  )
})

// Submit yêu cầu bảo hành
async function submitWarranty() {
  if (!canSubmitWarranty.value || warrantySubmitting.value) return
  
  warrantySubmitting.value = true
  warrantyModal.value.feedback = { type: '', message: '' }
  
  try {
    // 1. Upload ảnh/video lên Cloudinary trước
    const attachmentUrls = []
    for (const attachment of warrantyForm.value.attachments) {
      if (attachment.file) {
        try {
          const url = await cloudinaryService.upload(attachment.file)
          attachmentUrls.push(url)
        } catch (uploadError) {
          console.error('Upload failed for:', attachment.name, uploadError)
          throw new Error(`Không thể tải lên file ${attachment.name}. Vui lòng thử lại.`)
        }
      }
    }
    
    // 2. Gửi request tạo bảo hành với URLs đã upload
    const payload = {
      orderItemId: warrantyModal.value.selectedItem.orderItemId || warrantyModal.value.selectedItem.skuId,
      reason: warrantyForm.value.reason,
      description: warrantyForm.value.description,
      attachmentUrls: attachmentUrls,
    }
    
    const response = await WarrantyApi.createWarranty(payload)
    
    warrantyModal.value.feedback = {
      type: 'success',
      message: 'Yêu cầu bảo hành đã được gửi thành công! Chúng tôi sẽ phản hồi trong thời gian sớm nhất.',
    }
    
    // Đóng modal sau 2 giây
    setTimeout(() => {
      closeWarrantyModal()
      // Refresh lại trang để cập nhật trạng thái
      quietRefreshOrder()
    }, 2000)
  } catch (e) {
    warrantyModal.value.feedback = {
      type: 'error',
      message: e?.response?.data?.message || e?.message || 'Gửi yêu cầu thất bại. Vui lòng thử lại.',
    }
  } finally {
    warrantySubmitting.value = false
  }
}

// ===== Mở modal để TẠO MỚI đánh giá =====
// ===== Mở modal để TẠO MỚI đánh giá =====
function openReview(item) {
  editMode.value = false
  viewOnly.value = false
  selectedOrderItem.value = item

  // Trì hoãn 1 tick để tránh hiện tượng "click xuyên" khi modal
  // được chèn vào DOM ngay dưới vị trí vừa chạm/click.
  setTimeout(() => {
    showReviewModal.value = true
  }, 0)
}

// ===== Mở modal để SỬA đánh giá đã có =====
async function editReview(item) {
  try {
    const res = await reviewService.getMyReview(item.orderItemId)

    selectedOrderItem.value = {
      ...item,
      review: res.data.data,
    }

    editMode.value = true
    viewOnly.value = false

    setTimeout(() => {
      showReviewModal.value = true
    }, 0)
  } catch (e) {
    console.log(e)
  }
}

// ===== Mở modal chỉ để XEM đánh giá đã có (không cho chỉnh sửa) =====
const router = useRouter()
function goToProduct(item) {
  if (!item.productSlug) {
    console.warn('Thiếu productSlug', item)
    return
  }

  router.push({
    name: 'ProductDetail',
    params: {
      slug: item.productSlug,
    },
    query: {
      review: true,
    },
  })
}
async function reviewSuccess() {
  // Chỉ đóng modal và âm thầm cập nhật lại dữ liệu đơn hàng (vd: item.reviewed),
  // không bật loading.value để tránh cả trang phía sau modal bị che lại
  // bởi màn hình "Đang tải chi tiết đơn hàng...".
  showReviewModal.value = false
  await quietRefreshOrder()
}

// Cập nhật lại chi tiết đơn hàng mà không hiện trạng thái loading toàn trang,
// dùng sau khi tạo/sửa/xóa đánh giá trong modal.
async function quietRefreshOrder() {
  const orderCode = route.params.id
  if (!orderCode) return
  try {
    const response = await UserOrderApi.userOrderDetail(orderCode)
    selectedOrder.value = response.data
  } catch (e) {
    console.error(e)
  }
}

// Marcus thêm hàm đồng bộ nhẹ để trạng thái khách cập nhật sau khi admin refund,
// không bật lại loading toàn trang và không làm nháy giao diện.
async function refreshRefundStatus() {
  const orderCode = route.params.id
  if (!orderCode) return
  try {
    const [orderResponse, refundResponse] = await Promise.all([
      UserOrderApi.userOrderDetail(orderCode),
      UserOrderApi.userRefund(orderCode),
    ])
    selectedOrder.value = orderResponse.data
    refund.value = refundResponse.status === 204 ? null : refundResponse.data
  } catch (refreshError) {
    console.error(refreshError)
  }
}

async function fetchOrderDetail() {
  try {
    loading.value = true
    error.value = null
    const orderCode = route.params.id

    const response = await UserOrderApi.userOrderDetail(orderCode)

    selectedOrder.value = response.data
    // Marcus sửa: lỗi tải refund không được làm mất toàn bộ trang chi tiết đơn.
    try {
      const refundResponse = await UserOrderApi.userRefund(orderCode)
      refund.value = refundResponse.status === 204 ? null : refundResponse.data
    } catch (refundError) {
      refund.value = null
      console.error(refundError)
    }
  } catch (e) {
    error.value = 'Không thể lấy chi tiết đơn hàng'
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOrderDetail()
  // Marcus thêm polling khi refund chưa kết thúc và refresh ngay khi khách quay lại tab.
  refundPollingTimer = window.setInterval(() => {
    if (refund.value && !['SUCCESS', 'FAILED', 'MANUAL_REVIEW'].includes(refund.value.status)) {
      refreshRefundStatus()
    }
  }, 10000)
  window.addEventListener('focus', refreshRefundStatus)
})

onBeforeUnmount(() => {
  if (refundPollingTimer) window.clearInterval(refundPollingTimer)
  window.removeEventListener('focus', refreshRefundStatus)
})

// Marcus sửa: đơn tại quầy vẫn có thể hủy trước khi nhân viên xác nhận đã giao.
const USER_CANCELLABLE_STATUSES = ['PENDING', 'PROCESSING', 'PACKED', 'READY_FOR_PICKUP']

// Marcus thêm nội dung thân thiện với khách thay cho response code kỹ thuật của VNPAY.
const refundStatusConfig = {
  PENDING_APPROVAL: {
    label: 'Chờ cửa hàng duyệt',
    className: 'pending',
    description: 'Cửa hàng đã tiếp nhận và đang kiểm tra yêu cầu hoàn tiền.',
  },
  PROCESSING: {
    label: 'Chờ VNPAY xác nhận',
    className: 'processing',
    description: 'Yêu cầu đã được gửi đến VNPAY và đang chờ xác nhận hoàn trả.',
  },
  SUBMITTING: {
    label: 'Đang gửi yêu cầu',
    className: 'processing',
    description: 'MarcusStore đang gửi yêu cầu hoàn tiền sang VNPAY.',
  },
  RETRY_PENDING: {
    label: 'Đang gửi lại yêu cầu',
    className: 'processing',
    description: 'Kết nối VNPAY chưa thành công; hệ thống sẽ tự động gửi lại.',
  },
  SUCCESS: {
    label: 'Đã hoàn tiền',
    className: 'success',
    description: 'VNPAY đã xác nhận hoàn tiền thành công.',
  },
  FAILED: {
    label: 'Chưa hoàn tất',
    className: 'failed',
    description: 'Yêu cầu hoàn tiền chưa hoàn tất. Cửa hàng sẽ kiểm tra và hỗ trợ bạn.',
  },
  MANUAL_REVIEW: {
    label: 'Cửa hàng đang kiểm tra',
    className: 'processing',
    description: 'Chưa có kết quả cuối từ VNPAY; cửa hàng đang kiểm tra giao dịch.',
  },
}

// Marcus sửa nhãn thanh toán để client không còn hiển thị "---" khi đang refund.
const getPaymentStatusLabel = (status) =>
  ({
    PAID: 'Đã thanh toán',
    UNPAID: 'Chưa thanh toán',
    FAILED: 'Thanh toán thất bại',
    REFUND_PENDING: 'Đang chờ hoàn tiền',
    REFUND_FAILED: 'Hoàn tiền cần hỗ trợ',
    REFUNDED: 'Đã hoàn tiền',
  })[status] ||
  status ||
  '---'

const canCancelOrder = computed(() => {
  const order = selectedOrder.value
  if (!order) return false
  // Marcus sửa: khách được hủy cả COD và VNPAY khi đơn chưa bước vào giao hàng.
  if (!['COD', 'VNPAY'].includes((order.paymentMethod || '').toUpperCase())) return false
  const status = (order.orderStatus || '').toUpperCase()
  return USER_CANCELLABLE_STATUSES.includes(status)
})

// Marcus thêm: nút nhận hàng bám đúng mốc nghiệp vụ của từng phương thức giao.
const canConfirmReceipt = computed(() => {
  const order = selectedOrder.value
  if (!order) return false
  const status = (order.orderStatus || '').toUpperCase()
  return order.fulfillmentMethod === 'STORE_PICKUP'
    ? status === 'READY_FOR_PICKUP'
    : status === 'DELIVERED'
})

const confirmingReceipt = ref(false)
const receiptModal = ref({
  visible: false,
  type: 'confirm',
  title: '',
  message: '',
})

function openReceiptConfirmation() {
  if (!canConfirmReceipt.value || confirmingReceipt.value) return
  const storePickup = selectedOrder.value?.fulfillmentMethod === 'STORE_PICKUP'
  receiptModal.value = {
    visible: true,
    type: 'confirm',
    title: storePickup ? 'Xác nhận đã nhận tại cửa hàng?' : 'Xác nhận đã nhận được hàng?',
    message: storePickup
      ? 'Sau khi xác nhận, đơn hàng sẽ hoàn thành và ghi nhận thanh toán tại cửa hàng.'
      : 'Sau khi xác nhận, đơn hàng sẽ chuyển sang hoàn thành.',
  }
}

function closeReceiptModal() {
  if (confirmingReceipt.value) return
  receiptModal.value.visible = false
}

async function confirmReceivedOrder() {
  if (!canConfirmReceipt.value || confirmingReceipt.value) return
  confirmingReceipt.value = true
  receiptModal.value.visible = false
  try {
    const response = await UserOrderApi.confirmReceived(selectedOrder.value.orderCode)
    selectedOrder.value = response?.data || selectedOrder.value
    receiptModal.value = {
      visible: true,
      type: 'success',
      title: 'Xác nhận thành công',
      message: 'Cảm ơn bạn. Đơn hàng đã được chuyển sang hoàn thành.',
    }
  } catch (e) {
    receiptModal.value = {
      visible: true,
      type: 'error',
      title: 'Chưa thể xác nhận',
      message:
        e?.response?.data?.message ||
        e?.message ||
        'Không thể xác nhận đã nhận hàng. Vui lòng thử lại.',
    }
  } finally {
    confirmingReceipt.value = false
  }
}

const cancelling = ref(false)
const OTHER_CANCEL_REASON = 'Lý do khác'
const CUSTOMER_CANCEL_REASONS = [
  'Đặt nhầm sản phẩm hoặc số lượng',
  'Muốn thay đổi địa chỉ nhận hàng',
  'Tìm được sản phẩm hoặc giá phù hợp hơn',
  'Thời gian giao hàng không phù hợp',
  'Không còn nhu cầu mua',
  OTHER_CANCEL_REASON,
]

const cancelModal = ref({
  open: false,
  orderCode: '',
  reason: '',
  selectedReason: '',
  feedback: { type: '', message: '' },
})

function openCancelModal() {
  const order = selectedOrder.value
  if (!order || !canCancelOrder.value || cancelling.value) return
  cancelModal.value = {
    open: true,
    orderCode: order.orderCode,
    reason: '',
    selectedReason: '',
    feedback: { type: '', message: '' },
  }
}

function closeCancelModal() {
  if (cancelling.value) return
  cancelModal.value = {
    open: false,
    orderCode: '',
    reason: '',
    selectedReason: '',
    feedback: { type: '', message: '' },
  }
}

async function handleCancelOrder() {
  if (!canCancelOrder.value || cancelling.value) return
  openCancelModal()
}

function selectCancelReason(reason) {
  cancelModal.value.selectedReason = reason
  cancelModal.value.reason = reason === OTHER_CANCEL_REASON ? '' : reason
  cancelModal.value.feedback = { type: '', message: '' }
}

async function confirmCancelOrder() {
  const modal = cancelModal.value
  if (!modal.open || cancelling.value) return

  const reason = modal.reason.trim()
  if (!reason) return

  cancelling.value = true
  modal.feedback = { type: '', message: '' }
  try {
    const response = await UserOrderApi.cancelOrder(modal.orderCode, { note: reason })
    if (response?.data) {
      selectedOrder.value = response.data
    } else {
      await fetchOrderDetail()
    }
    cancelling.value = false
    closeCancelModal()
  } catch (e) {
    modal.feedback = {
      type: 'error',
      message: e?.response?.data?.message || e?.message || 'Hủy đơn thất bại, vui lòng thử lại.',
    }
    cancelling.value = false
  }
}

const statusConfig = {
  CREATED: { label: 'Tạo đơn', className: 'pending', icon: 'fa-file-circle-plus' },
  PENDING: { label: 'Chờ xác nhận', className: 'pending', icon: 'fa-clock' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'confirmed', icon: 'fa-circle-check' },
  PROCESSING: { label: 'Đang chuẩn bị', className: 'processing', icon: 'fa-boxes-packing' },
  PACKED: { label: 'Đã đóng gói', className: 'processing', icon: 'fa-box' },
  READY_FOR_PICKUP: {
    label: 'Sẵn sàng nhận tại cửa hàng',
    className: 'delivered',
    icon: 'fa-store',
  },
  SHIPPING: { label: 'Đang giao', className: 'shipping', icon: 'fa-truck-fast' },
  DELIVERED: { label: 'Giao thành công', className: 'delivered', icon: 'fa-circle-check' },
  CANCELLED: { label: 'Đã hủy', className: 'cancelled', icon: 'fa-ban' },
  FAILED: { label: 'Giao thất bại', className: 'failed', icon: 'fa-triangle-exclamation' },
}

const defaultTimelineSteps = [
  { status: 'PENDING' },
  { status: 'CONFIRMED' },
  { status: 'PROCESSING' },
  { status: 'PACKED' },
  { status: 'SHIPPING' },
  { status: 'DELIVERED' },
]

const pickupTimelineSteps = [
  { status: 'PENDING' },
  { status: 'CONFIRMED' },
  { status: 'PROCESSING' },
  { status: 'READY_FOR_PICKUP' },
]

const visibleTimelineSteps = computed(() => {
  if (!selectedOrder.value) return []

  const currentStatus = selectedOrder.value.orderStatus
  const historyByStatus = new Map(
    (selectedOrder.value.history || []).map((item) => [item.status, item]),
  )

  // Marcus thêm: timeline tại quầy không hiển thị đóng gói, giao hàng và GHN.
  const timelineSteps =
    selectedOrder.value.fulfillmentMethod === 'STORE_PICKUP'
      ? pickupTimelineSteps
      : defaultTimelineSteps
  const currentIndex = timelineSteps.findIndex((step) => step.status === currentStatus)
  const isTerminalStatus = currentStatus === 'CANCELLED' || currentStatus === 'FAILED'
  const isCompletedStatus = currentStatus === 'COMPLETED'

  let flowStatuses
  if (isTerminalStatus) {
    flowStatuses = timelineSteps
      .filter((step) => historyByStatus.has(step.status))
      .map((step) => step.status)
    flowStatuses.push(currentStatus)
  } else if (currentIndex >= 0 || isCompletedStatus) {
    // COMPLETED: hiển thị đầy đủ flow như DELIVERED (không hiện COMPLETED trên timeline)
    flowStatuses = timelineSteps.map((step) => step.status)
  } else {
    flowStatuses = ['PENDING', currentStatus]
  }

  const statuses = ['CREATED', ...new Set(flowStatuses)]

  return statuses.map((status, index) => {
    const historyItem = historyByStatus.get(status)
    const timelineStep = createTimelineStep(
      status,
      index,
      status === 'CREATED' ? selectedOrder.value.createdAt : historyItem?.createdAt,
      historyItem?.note,
    )

    timelineStep.isCurrent =
      currentStatus === status ||
      (currentStatus === 'COMPLETED' &&
        status ===
          (selectedOrder.value.fulfillmentMethod === 'STORE_PICKUP'
            ? 'READY_FOR_PICKUP'
            : 'DELIVERED'))
    return timelineStep
  })
})

const displayHistory = computed(() => {
  if (!selectedOrder.value) return []

  // Lọc bỏ COMPLETED cho UI client (vẫn giữ ở admin)
  const history = (selectedOrder.value.history || []).filter((item) => item.status !== 'COMPLETED')
  const hasCreated = history.some((item) => item.status === 'CREATED')

  if (hasCreated) return history

  const createdItem = {
    status: 'CREATED',
    title: 'Tạo đơn',
    createdAt: selectedOrder.value.createdAt,
    note: null,
  }

  return [createdItem, ...history]
})

const displayPaymentDate = computed(() => {
  if (!selectedOrder.value) return ''

  const order = selectedOrder.value
  const isCOD = order.paymentMethod === 'COD'
  const hasPaymentDate = order.paymentDate

  // Nếu đã có paymentDate thì hiển thị paymentDate
  if (hasPaymentDate) {
    return formatDateTime(hasPaymentDate)
  }

  // Nếu là COD và chưa có paymentDate, lấy thời gian DELIVERED từ history
  if (isCOD) {
    const history = order.history || []
    const deliveredItem = history.find((item) => item.status === 'DELIVERED')
    if (deliveredItem?.createdAt) {
      return formatDateTime(deliveredItem.createdAt)
    }
  }

  return '---'
})

const timelineProgress = computed(() => {
  const steps = visibleTimelineSteps.value
  if (steps.length <= 1) return '0%'

  const currentIndex = steps.findIndex((step) => step.isCurrent)
  const safeIndex = currentIndex >= 0 ? currentIndex : steps.length - 1

  return `${(safeIndex / (steps.length - 1)) * 80}%`
})

function createTimelineStep(status, index, createdAt = null, note = null) {
  const config = statusConfig[status] || {}
  return {
    key: `${status}-${index}`,
    status,
    title: config.label || status,
    icon: config.icon || 'fa-circle-info',
    createdAt,
    note,
    isCurrent: false,
  }
}

function getStepClass(step, index) {
  if (step.status === 'CANCELLED' || step.status === 'FAILED') return 'cancel'
  if (step.isCurrent) return 'active'
  const currentIndex = visibleTimelineSteps.value.findIndex((item) => item.isCurrent)
  if (currentIndex === -1) {
    return index < visibleTimelineSteps.value.length - 1 ? 'done' : ''
  }
  if (index < currentIndex) return 'done'
  return ''
}

function formatMoney(value) {
  return `${new Intl.NumberFormat('vi-VN').format(value || 0)}đ`
}

function formatDateTime(value) {
  if (!value) return ''
  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})[T\s-]+(\d{2}):(\d{2})/)
  if (!match) return String(value).split('.')[0].replace('T', ' ')

  const [, year, month, day, hour, minute] = match
  return `${day}/${month}/${year} ${hour}:${minute}`
}

function getPaymentMethodLabel(method) {
  if (method === 'VNPay' || method === 'VNPAY') return 'VNPAY'
  if (method === 'BankTransfer') return 'Chuyển khoản'
  return method || '---'
}

function getVariantText(item) {
  if (!item || !Array.isArray(item.variants) || item.variants.length === 0) return ''
  return item.variants
    .filter((v) => v && v.valueString)
    .map((v) => {
      if (v.attributeName) return `${v.attributeName}: ${v.valueString}`
      return v.valueString
    })
    .join(' | ')
}
</script>

<style scoped>
.cancel-reason-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 16px 0;
}

.cancel-reason-option {
  padding: 11px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  color: #334155;
  text-align: left;
  font-weight: 600;
  transition: 0.18s ease;
}

.cancel-reason-option:hover,
.cancel-reason-option.active {
  border-color: #dc2626;
  background: #fff1f2;
  color: #b91c1c;
}

@media (max-width: 640px) {
  .cancel-reason-grid {
    grid-template-columns: 1fr;
  }
}
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  padding: 8px 12px;
  background: #fff1f2;
  color: #e60012;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}

.product-thumb {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #ccc;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.price-original {
  text-decoration: line-through;
  color: #9ca3af;
  font-size: 12px;
  margin-right: 4px;
}

.product-variants {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding: 4px 10px;
  background: #f3f4f6;
  color: #374151;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.product-variants i {
  color: #e60012;
  font-size: 11px;
}

.price-flashsale {
  color: #ef4444;
  font-weight: 700;
}

.flash-badge {
  font-size: 10px;
  background: #fee2e2;
  color: #ef4444;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
  margin-left: 4px;
}

/* Marcus thêm giao diện theo dõi refund cho client. */
.refund-tracking-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  border: 1px solid #fde68a;
  border-radius: 14px;
  background: #fffbeb;
}
.refund-icon {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #fef3c7;
  color: #d97706;
}
.refund-content {
  flex: 1;
  min-width: 0;
}
.refund-heading {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}
.refund-heading h3 {
  margin: 0 0 4px;
  font-size: 18px;
}
.refund-heading p,
.refund-reason {
  margin: 0;
  color: #6b7280;
}
.refund-pill {
  padding: 6px 10px;
  border-radius: 999px;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 700;
  background: #e5e7eb;
}
.refund-pill.pending,
.refund-pill.processing {
  color: #92400e;
  background: #fef3c7;
}
.refund-pill.success {
  color: #166534;
  background: #dcfce7;
}
.refund-pill.failed {
  color: #991b1b;
  background: #fee2e2;
}
.refund-values {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0;
}
.refund-values div {
  padding: 12px;
  border-radius: 10px;
  background: #fff;
}
.refund-values span {
  display: block;
  margin-bottom: 4px;
  color: #6b7280;
  font-size: 12px;
}
@media (max-width: 768px) {
  .refund-heading {
    flex-direction: column;
  }
  .refund-values {
    grid-template-columns: 1fr;
  }
}

.product-item {
  display: grid;
  grid-template-columns: 64px 1fr auto;
  grid-template-areas:
    'thumb name total'
    'action action action';
  align-items: center;
  gap: 12px 16px;
  padding: 16px;
  border: 1px solid #f1f1f1;
  border-radius: 12px;
  position: relative;
}

.product-thumb {
  grid-area: thumb;
  width: 64px;
  height: 64px;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  flex-shrink: 0;
}

.product-thumb i {
  font-size: 24px;
  color: #cbd5e1;
}

.product-name {
  grid-area: name;
  margin: 0 0 4px;
  font-weight: 700;
}

.product-meta {
  grid-area: name;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #6b7280;
  margin-top: 22px;
}

.product-total {
  grid-area: total;
  white-space: nowrap;
  font-weight: 700;
}

/* ===== Vùng nút Đánh giá / Xem đánh giá / Sửa đánh giá ===== */
.review-action {
  grid-area: action;
  margin-top: 8px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
}

.review-btn,
.review-view-btn,
.review-edit-btn {
  min-width: 140px;
  height: 38px;
  padding: 0 16px;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: 0.2s;
}

.review-btn {
  background: #df062d;
  color: #fff;
}

.review-btn:hover {
  background: #c00526;
}

.review-view-btn {
  background: #3b82f6;
  color: #fff;
}

.review-view-btn:hover {
  background: #2563eb;
}

.review-edit-btn {
  background: #22d0ee;
  color: #fff;
}

.review-edit-btn:hover {
  background: #12b4d1;
}
/* ===== Hết vùng nút đánh giá ===== */

/* ===== Marcus thêm CSS cho Bảo hành ===== */

/* Nút yêu cầu bảo hành */
.warranty-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 160px;
  height: 38px;
  padding: 0 16px;
  border: 2px solid #e60012;
  border-radius: 8px;
  background: #fff;
  color: #e60012;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.warranty-btn:hover {
  background: #e60012;
  color: #fff;
}

.warranty-btn i {
  font-size: 14px;
}

/* Pill trạng thái bảo hành */
.warranty-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.warranty-status-pill i {
  font-size: 11px;
}

.warranty-pending {
  background: #fff1f2;
  color: #e60012;
}

.warranty-approved {
  background: #dcfce7;
  color: #166534;
}

.warranty-rejected {
  background: #fee2e2;
  color: #991b1b;
}

/* Modal bảo hành */
.warranty-modal {
  max-width: 760px;
  width: 96%;
  max-height: 90vh;
  overflow-y: auto;
  padding: 28px;
}

/* Căn lại header và footer của modal bảo hành */
.warranty-modal .modal-header {
  padding: 0 0 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid #f1f5f9;
}

.warranty-modal .modal-body {
  padding: 0 4px;
}

.warranty-modal .modal-footer {
  padding: 18px 0 0;
  margin-top: 20px;
  border-top: 1px solid #f1f5f9;
  gap: 12px;
}

/* Scrollbar tùy chỉnh cho modal */
.warranty-modal::-webkit-scrollbar {
  width: 6px;
}

.warranty-modal::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.warranty-modal::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.warranty-modal::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

/* Product Card trong modal */
.warranty-product-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  background: linear-gradient(135deg, #fff7f7 0%, #fff 100%);
  border: 1.5px solid #fee2e2;
  border-radius: 12px;
  margin-bottom: 20px;
}

.warranty-product-thumb {
  position: relative;
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f5f5;
}

.warranty-product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.warranty-product-thumb-placeholder {
  display: none;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #ccc;
  background: #fafafa;
}

.warranty-product-thumb-placeholder.show {
  display: flex;
}

.warranty-product-info-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.warranty-product-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.warranty-product-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.spec-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #64748b;
}

.spec-item i {
  font-size: 11px;
  color: #94a3b8;
}

.spec-item strong {
  color: #334155;
  font-weight: 600;
}

.warranty-product-price-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
}

.price-label {
  font-size: 12px;
  color: #64748b;
}

.price-value {
  font-size: 15px;
  font-weight: 700;
  color: #e60012;
}

/* Form groups */
.warranty-form-group {
  margin-bottom: 20px;
}

.warranty-label {
  display: block;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.required {
  color: #e60012;
}

/* Loại yêu cầu (radio cards) - Đỏ thương hiệu */
.warranty-type-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.warranty-type-option {
  position: relative;
  cursor: pointer;
}

.warranty-type-radio {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.warranty-type-content {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  text-align: center;
  transition: all 0.25s ease;
}

.warranty-type-option:hover .warranty-type-content {
  border-color: #f87171;
  background: #fff5f5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(230, 0, 18, 0.1);
}

.warranty-type-option.active .warranty-type-content {
  border-color: #e60012;
  background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
  box-shadow: 0 4px 16px rgba(230, 0, 18, 0.15);
}

.warranty-type-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f5f5f5;
  transition: all 0.25s ease;
}

.warranty-type-option:hover .warranty-type-icon {
  background: #fee2e2;
}

.warranty-type-option.active .warranty-type-icon {
  background: linear-gradient(135deg, #e60012, #ff1a1a);
}

.warranty-type-icon i {
  font-size: 22px;
  color: #94a3b8;
  transition: color 0.25s ease;
}

.warranty-type-option:hover .warranty-type-icon i {
  color: #e60012;
}

.warranty-type-option.active .warranty-type-icon i {
  color: #fff;
}

.warranty-type-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.warranty-type-option.active .warranty-type-title {
  color: #e60012;
}

.warranty-type-desc {
  font-size: 12px;
  color: #64748b;
}

.warranty-type-check {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e60012;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  box-shadow: 0 2px 8px rgba(230, 0, 18, 0.3);
  animation: scaleIn 0.2s ease;
}

@keyframes scaleIn {
  from {
    transform: scale(0);
  }
  to {
    transform: scale(1);
  }
}

/* Lý do bảo hành - Card bấm chọn với tick */
.warranty-reason-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.warranty-reason-option {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.warranty-reason-option i {
  font-size: 16px;
  color: #9ca3af;
  transition: color 0.2s ease;
}

.warranty-reason-option span {
  flex: 1;
}

.reason-check {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e60012;
  color: #fff;
  border-radius: 50%;
  font-size: 11px;
  box-shadow: 0 2px 6px rgba(230, 0, 18, 0.3);
  animation: scaleIn 0.2s ease;
}

/* Active state - Đỏ nhạt + viền đỏ */
.warranty-reason-option:hover {
  border-color: #f87171;
  background: #fff5f5;
  color: #e60012;
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(230, 0, 18, 0.1);
}

.warranty-reason-option:hover i {
  color: #e60012;
}

.warranty-reason-option.active {
  border-color: #e60012;
  background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
  color: #e60012;
  box-shadow: 0 4px 12px rgba(230, 0, 18, 0.12);
}

.warranty-reason-option.active i {
  color: #e60012;
}

/* Textarea */
.warranty-textarea {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 14px;
  font-family: inherit;
  color: #1e293b;
  resize: vertical;
  min-height: 100px;
  transition: all 0.2s ease;
}

.warranty-textarea:focus {
  outline: none;
  border-color: #e60012;
  box-shadow: 0 0 0 4px rgba(230, 0, 18, 0.1);
}

.warranty-textarea:disabled {
  background: #f8fafc;
  color: #94a3b8;
  cursor: not-allowed;
}

.warranty-textarea::placeholder {
  color: #94a3b8;
}

.warranty-textarea-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.warranty-char-count {
  font-size: 12px;
  color: #94a3b8;
}

.warranty-attach-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.warranty-attach-btn:hover {
  border-color: #e60012;
  color: #e60012;
  background: #fff5f5;
}

.warranty-file-input {
  display: none;
}

/* Preview attachments */
.warranty-attachments-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.warranty-attach-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #6b7280;
  font-style: italic;
}

.attachment-item {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  border: 1px solid #e5e7eb;
}

.attachment-item img,
.attachment-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.attachment-item i {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 24px;
  color: #94a3b8;
}

.attachment-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border: none;
  border-radius: 50%;
  font-size: 10px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.attachment-item:hover .attachment-remove {
  opacity: 1;
}

/* Lưu ý bảo hành */
.warranty-notice {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #fffbeb 0%, #fefce8 100%);
  border: 1px solid #fde68a;
  border-radius: 10px;
  margin-bottom: 16px;
}

.warranty-notice-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fef3c7;
  border-radius: 50%;
  color: #d97706;
  font-size: 14px;
}

.warranty-notice-text {
  flex: 1;
  font-size: 13px;
  color: #92400e;
  line-height: 1.5;
}

.warranty-notice-text strong {
  color: #b45309;
}

/* Feedback */
.warranty-feedback {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 12px;
  font-size: 14px;
  margin-top: 16px;
}

.warranty-feedback i {
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 1px;
}

.warranty-feedback-success {
  background: linear-gradient(135deg, #dcfce7 0%, #f0fdf4 100%);
  border: 1px solid #86efac;
  color: #166534;
}

.warranty-feedback-error {
  background: linear-gradient(135deg, #fee2e2 0%, #fff5f5 100%);
  border: 1px solid #fca5a5;
  color: #991b1b;
}

/* Nút ghost cho modal */
.btn-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 44px;
  padding: 0 28px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  color: #64748b;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 120px;
}

.btn-ghost:hover:not(:disabled) {
  border-color: #e60012;
  color: #e60012;
  background: #fff5f5;
}

.btn-ghost:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Nút primary cho modal - Đỏ thương hiệu */
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 44px;
  padding: 0 32px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #e60012 0%, #cc0000 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 14px rgba(230, 0, 18, 0.35);
  min-width: 140px;
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #cc0000 0%, #a80000 100%);
  box-shadow: 0 6px 20px rgba(230, 0, 18, 0.45);
  transform: translateY(-1px);
}

.btn-primary:disabled {
  background: #e5e7eb;
  color: #94a3b8;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* Responsive */
@media (max-width: 640px) {
  .warranty-type-options,
  .warranty-reason-grid {
    grid-template-columns: 1fr;
  }
  
  .warranty-product-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .warranty-product-specs {
    justify-content: center;
  }
  
  .warranty-product-price-row {
    justify-content: center;
  }
  
  .warranty-textarea-footer {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
}

/* ===== CSS cho VIEW mode ===== */

/* Loading state */
.warranty-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: #64748b;
  font-size: 15px;
}

.warranty-loading i {
  font-size: 24px;
  color: #e60012;
}

/* Trạng thái card trong VIEW mode */
.warranty-status-card {
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.warranty-status-card.warranty-status-pending {
  background: linear-gradient(135deg, #fff1f2 0%, #fff 100%);
  border: 1.5px solid #fecdd3;
}

.warranty-status-card.warranty-status-approved {
  background: linear-gradient(135deg, #dcfce7 0%, #fff 100%);
  border: 1.5px solid #bbf7d0;
}

.warranty-status-card.warranty-status-rejected {
  background: linear-gradient(135deg, #fee2e2 0%, #fff 100%);
  border: 1.5px solid #fecaca;
}

.warranty-status-card.warranty-status-completed {
  background: linear-gradient(135deg, #dbeafe 0%, #fff 100%);
  border: 1.5px solid #bfdbfe;
}

.warranty-status-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.warranty-status-header i {
  font-size: 20px;
}

.warranty-status-pending .warranty-status-header i { color: #e60012; }
.warranty-status-approved .warranty-status-header i { color: #16a34a; }
.warranty-status-rejected .warranty-status-header i { color: #dc2626; }
.warranty-status-completed .warranty-status-header i { color: #2563eb; }

.warranty-status-label {
  font-size: 16px;
  font-weight: 700;
}

.warranty-status-pending .warranty-status-label { color: #be123c; }
.warranty-status-approved .warranty-status-label { color: #166534; }
.warranty-status-rejected .warranty-status-label { color: #991b1b; }
.warranty-status-completed .warranty-status-label { color: #1e40af; }

.warranty-status-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #64748b;
}

.warranty-status-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.warranty-status-meta i {
  font-size: 12px;
  color: #94a3b8;
}

.warranty-status-admin {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 6px 12px;
  background: rgba(255,255,255,0.7);
  border-radius: 6px;
  font-size: 13px;
  color: #475569;
}

.warranty-status-admin i {
  color: #64748b;
}

/* Section xem thông tin đã gửi */
.warranty-view-section {
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 16px;
}

.warranty-view-item {
  margin-bottom: 16px;
}

.warranty-view-item:last-child {
  margin-bottom: 0;
}

.warranty-view-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.warranty-view-label i {
  margin-right: 4px;
}

.warranty-view-value {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.warranty-view-description {
  margin: 0;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* Ảnh/Video đã gửi */
.warranty-view-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.warranty-view-attachment {
  width: 100px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
  border: 1.5px solid #e5e7eb;
  background: #f5f5f5;
}

.warranty-view-attachment img,
.warranty-view-attachment video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Ghi chú từ admin */
.warranty-admin-note {
  padding: 14px;
  background: linear-gradient(135deg, #fef3c7 0%, #fff 100%);
  border: 1px solid #fde68a;
  border-radius: 10px;
  font-size: 14px;
  color: #92400e;
  line-height: 1.5;
}

/* Nút xem bảo hành */
.warranty-view-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 140px;
  height: 38px;
  padding: 0 16px;
  border: 2px solid #3b82f6;
  border-radius: 8px;
  background: #fff;
  color: #3b82f6;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.warranty-view-btn:hover {
  background: #3b82f6;
  color: #fff;
}

.warranty-view-btn i {
  font-size: 14px;
}
</style>
