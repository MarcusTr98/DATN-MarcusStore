<template>
  <div class="fin-page">
    <div class="fin-shell">
      <!-- Marcus sửa: dùng chung header quản trị để đồng nhất với trang Phân tích kinh doanh. -->
      <AdminPageHeader
        eyebrow="Tài chính & giao dịch"
        eyebrow-icon="bi bi-cash-coin"
        title="Quản lý đối soát"
        description="Theo dõi dòng tiền thu vào, hoàn tiền và các khoản đang chờ xử lý trong hệ thống."
        icon="bi bi-graph-up-arrow"
      >
        <template #actions>
          <div class="hero-actions">
            <div class="dynamic-total" v-if="filteredTransactions.length > 0">
              Dòng tiền ròng theo bộ lọc:
              <strong :class="filteredTotal < 0 ? 'amount-outflow' : 'amount-inflow'">
                {{ formatCurrencyVnd(filteredTotal) }}
              </strong>
            </div>
            <button
              @click="handleExportFilteredExcel"
              class="btn-export-excel"
              :disabled="exporting"
            >
              <i
                class="bi"
                :class="exporting ? 'bi-arrow-repeat spin' : 'bi-file-earmark-excel'"
              ></i>
              {{ exporting ? 'Đang xuất...' : 'Xuất Báo Cáo Excel' }}
            </button>
          </div>
        </template>
      </AdminPageHeader>

      <!-- Stats -->
      <div class="stats-grid">
        <!-- Marcus sửa: ưu tiên nhóm KPI lượt giao dịch trước, sau đó mới tới tiền. -->
        <div class="stat-card">
          <div class="stat-icon stat-icon-blue"><i class="bi bi-receipt"></i></div>
          <div class="stat-body">
            <span>Tổng giao dịch</span>
            <strong>{{ stats.total }}</strong>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon-green"><i class="bi bi-check-circle"></i></div>
          <div class="stat-body">
            <span>Thành công</span>
            <strong class="fin-accent">{{ stats.success }}</strong>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon-amber"><i class="bi bi-hourglass-split"></i></div>
          <div class="stat-body">
            <span>Đang chờ xử lý</span>
            <strong>{{ stats.pending }}</strong>
          </div>
        </div>
        <div class="stat-card stat-card-money-in">
          <div class="stat-icon stat-icon-green"><i class="bi bi-arrow-down-left-circle"></i></div>
          <div class="stat-body">
            <span>Tiền vào thành công</span>
            <strong class="amount-inflow">{{ formatCurrencyVnd(stats.successfulInflow) }}</strong>
          </div>
        </div>
        <div class="stat-card stat-card-money-out">
          <div class="stat-icon stat-icon-red"><i class="bi bi-arrow-up-right-circle"></i></div>
          <div class="stat-body">
            <span>Đã hoàn thành công</span>
            <strong class="amount-outflow">-{{ formatCurrencyVnd(stats.successfulRefund) }}</strong>
          </div>
        </div>
        <div class="stat-card stat-card-money-in">
          <div class="stat-icon stat-icon-green"><i class="bi bi-bag-check"></i></div>
          <div class="stat-body">
            <span>Doanh thu đơn hoàn tất</span>
            <strong class="amount-inflow">{{ formatCurrencyVnd(stats.recognizedRevenue) }}</strong>
            <small>Đã thu tiền và đơn đã hoàn thành</small>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon-amber"><i class="bi bi-exclamation-circle"></i></div>
          <div class="stat-body">
            <span>Tiền đơn hủy chưa hoàn xong</span>
            <strong class="amount-outflow">
              {{ formatCurrencyVnd(stats.unsettledCancellationAmount) }}
            </strong>
            <small>Đang chờ refund thành công</small>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon-navy"><i class="bi bi-cash-stack"></i></div>
          <div class="stat-body">
            <span>Dòng tiền ròng</span>
            <strong :class="stats.totalAmount < 0 ? 'amount-outflow' : 'amount-inflow'">
              {{ formatCurrencyVnd(stats.totalAmount) }}
            </strong>
          </div>
        </div>
      </div>

      <!-- Marcus thêm: sáu nhóm tiền vận hành, không trộn tiền dự kiến với tiền đã thu. -->
      <div class="cash-breakdown-grid">
        <article><span>VNPAY đã thu</span><strong>{{ formatCurrencyVnd(stats.vnpayCollected) }}</strong></article>
        <article><span>COD dự kiến thu</span><strong>{{ formatCurrencyVnd(stats.codExpected) }}</strong></article>
        <article><span>COD đã đối soát</span><strong>{{ formatCurrencyVnd(stats.codReconciled) }}</strong></article>
        <article><span>Tiền nhận tại cửa hàng</span><strong>{{ formatCurrencyVnd(stats.storeCollected) }}</strong></article>
        <article class="cash-pending"><span>Refund dự kiến</span><strong>{{ formatCurrencyVnd(stats.refundExpected) }}</strong></article>
        <article class="cash-refund"><span>Refund thành công</span><strong>-{{ formatCurrencyVnd(stats.refundSuccessful) }}</strong></article>
      </div>

      <!-- Mini Chart (Stacked Bar) -->
      <div class="chart-panel" v-if="filteredTransactions.length > 0">
        <div class="chart-header">
          <div>
            <i class="bi bi-bar-chart-fill"></i> Phân bổ dòng tiền
            <span class="chart-subnote">{{ chartPeriodLabel }}</span>
          </div>
          <span class="chart-unit">Đơn vị: VND</span>
        </div>
        <apexchart
          type="bar"
          height="280"
          :options="chartOptions"
          :series="chartSeries"
        ></apexchart>
      </div>

      <!-- Filters -->
      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="field field-keyword">
            <label class="form-label">Tìm kiếm (Mã đơn, Ghi chú)</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-search"></i></span>
              <input
                :value="keywordInput"
                @input="onSearchInput"
                type="text"
                class="form-control"
                placeholder="Nhập từ khóa..."
              />
            </div>
          </div>

          <div class="field">
            <label class="form-label">Loại giao dịch</label>
            <select v-model="filters.type" class="form-select" @change="onFilterChange">
              <option value="">Tất cả</option>
              <option value="COD_COLLECTION">Thu hộ (COD)</option>
              <option value="STORE_PAYMENT">Thanh toán tại cửa hàng</option>
              <option value="VNPAY_PAYMENT">Thanh toán (VNPAY)</option>
              <option value="REFUND">Hoàn tiền</option>
            </select>
          </div>

          <div class="field">
            <label class="form-label">Kiểm tra đối soát</label>
            <select v-model="filters.attention" class="form-select" @change="onFilterChange">
              <option value="">Tất cả</option>
              <option value="UNRECONCILED">Chưa đối soát</option>
              <option value="ATTENTION">Chưa khớp / Cần xử lý</option>
            </select>
          </div>

          <div class="field">
            <label class="form-label">Trạng thái</label>
            <select v-model="filters.status" class="form-select" @change="onFilterChange">
              <option value="">Tất cả</option>
              <option value="SUCCESS">Thành công</option>
              <option value="PENDING">Chờ xử lý</option>
              <option value="FAILED">Thất bại</option>
            </select>
          </div>

          <div class="field field-dates">
            <div class="d-flex justify-content-between align-items-center mb-1">
              <label class="form-label mb-0">Thời gian</label>
              <div class="quick-dates">
                <button
                  type="button"
                  @click="applyDatePreset('yesterday')"
                  class="btn-quick-date"
                  :class="{ active: activeDatePreset === 'yesterday' }"
                >
                  Hôm qua
                </button>
                <button
                  type="button"
                  @click="applyDatePreset('today')"
                  class="btn-quick-date"
                  :class="{ active: activeDatePreset === 'today' }"
                >
                  Hôm nay
                </button>
                <button
                  type="button"
                  @click="applyDatePreset('7days')"
                  class="btn-quick-date"
                  :class="{ active: activeDatePreset === '7days' }"
                >
                  7 ngày qua
                </button>
                <button
                  type="button"
                  @click="applyDatePreset('30days')"
                  class="btn-quick-date"
                  :class="{ active: activeDatePreset === '30days' }"
                >
                  30 ngày qua
                </button>
                <button
                  type="button"
                  @click="applyDatePreset('thisMonth')"
                  class="btn-quick-date"
                  :class="{ active: activeDatePreset === 'thisMonth' }"
                >
                  Tháng này
                </button>
                <button
                  type="button"
                  @click="applyDatePreset('lastMonth')"
                  class="btn-quick-date"
                  :class="{ active: activeDatePreset === 'lastMonth' }"
                >
                  Tháng trước
                </button>
                <button
                  type="button"
                  @click="applyDatePreset('year')"
                  class="btn-quick-date"
                  :class="{ active: activeDatePreset === 'year' }"
                >
                  Năm nay
                </button>
              </div>
            </div>
            <div class="d-flex gap-2">
              <input
                v-model="filters.fromDate"
                type="date"
                class="form-control"
                @change="onDateFilterChange"
                title="Từ ngày"
              />
              <input
                v-model="filters.toDate"
                type="date"
                class="form-control"
                @change="onDateFilterChange"
                title="Đến ngày"
              />
              <button class="btn-soft" @click="resetFilters" title="Đặt lại bộ lọc">
                <i class="bi bi-arrow-counterclockwise"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Table -->
      <div class="table-panel">
        <div class="table-wrapper">
          <table class="financial-table">
            <thead>
              <tr>
                <th style="width: 56px">STT</th>
                <th>Đối soát</th>

                <th>Mã Đơn</th>
                <th>Loại Giao Dịch</th>
                <th class="text-end">Ảnh hưởng dòng tiền</th>
                <th>Trạng Thái</th>
                <th>Thời Gian</th>
                <th class="text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="8" class="text-center py-4">
                  <i class="bi bi-arrow-repeat spin"></i> Đang tải dữ liệu...
                </td>
              </tr>
              <tr v-else-if="pagedTransactions.length === 0">
                <td colspan="8" class="text-center py-4">
                  <i class="bi bi-inbox" style="font-size: 1.6rem; color: #9db8de"></i>
                  <div class="mt-2" style="color: #6b7280">Không có giao dịch nào phù hợp.</div>
                </td>
              </tr>
              <tr
                v-else
                v-for="(item, index) in pagedTransactions"
                :key="item.transactionId || index"
              >
                <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                <td class="text-center">
                  <div v-if="item.isReconciled" class="text-success">
                    <i class="bi bi-check-circle-fill" style="font-size: 1.3rem"></i>
                  </div>
                  <input
                    v-else-if="item.status === 'SUCCESS'"
                    type="checkbox"
                    @change="confirmReconciliation(item)"
                    class="form-check-input"
                    aria-label="Đánh dấu giao dịch thành công đã đối soát"
                  />
                  <span v-else class="text-muted" aria-label="Giao dịch chưa thành công, không thể đối soát">—</span>
                </td>

                <td class="fw-bold">{{ item.orderCode }}</td>
                <td>
                  <span :class="['badge', getTypeClass(item.type)]">
                    {{ formatType(item.type) }}
                  </span>
                </td>
                <td class="text-end">
                  <!-- Marcus sửa: chỉ SUCCESS mới tác động tiền thật; pending/failed
                       hiển thị số tiền yêu cầu nhưng không mang dấu cộng/trừ. -->
                  <strong :class="getAmountClass(item)">
                    {{ formatTransactionAmount(item) }}
                  </strong>
                  <small v-if="item.status !== 'SUCCESS'" class="amount-caption">
                    {{ getAmountCaption(item) }}
                  </small>
                </td>
                <td>
                  <span :class="['badge', getStatusClass(item.status)]">
                    {{ formatStatus(item.status) }}
                  </span>
                </td>
                <td>{{ formatDate(item.createdAt) }}</td>

                <td class="text-center">
                  <button class="btn-icon" @click="openDetailModal(item)" title="Xem chi tiết">
                    <i class="bi bi-eye"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div class="fin-pagination">
          <div class="pagination-summary">
            Tổng <strong>{{ filteredTransactions.length }}</strong> giao dịch
          </div>

          <div class="pagination-controls">
            <div class="page-size-group">
              <span class="page-size-label">Hiển thị</span>
              <select
                v-model.number="pageSize"
                class="form-select page-size-select"
                @change="currentPage = 1"
              >
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
              <span class="page-size-label page-size-suffix">/ trang</span>
            </div>

            <nav class="pager" aria-label="Phân trang">
              <button class="pager-arrow" :disabled="currentPage === 1" @click="currentPage = 1">
                <i class="bi bi-chevron-bar-left"></i>
              </button>
              <button class="pager-arrow" :disabled="currentPage === 1" @click="currentPage--">
                <i class="bi bi-chevron-left"></i>
              </button>
              <ul class="pager-list">
                <li v-for="(p, i) in pageItems" :key="i">
                  <span v-if="p === '...'" class="pager-ellipsis">…</span>
                  <button
                    v-else
                    class="pager-num"
                    :class="{ active: p === currentPage }"
                    @click="currentPage = p"
                  >
                    {{ p }}
                  </button>
                </li>
              </ul>
              <button
                class="pager-arrow"
                :disabled="currentPage === totalPages"
                @click="currentPage++"
              >
                <i class="bi bi-chevron-right"></i>
              </button>
              <button
                class="pager-arrow"
                :disabled="currentPage === totalPages"
                @click="currentPage = totalPages"
              >
                <i class="bi bi-chevron-bar-right"></i>
              </button>
            </nav>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Chi tiết Giao dịch -->
    <div v-if="isModalOpen" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-content">
        <div class="modal-header">
          <div class="modal-header-title">
            <span class="modal-header-icon"><i class="bi bi-file-earmark-text"></i></span>
            <h5 class="mb-0">Chi tiết Giao dịch</h5>
          </div>
          <button class="btn-close-modal" @click="closeDetailModal" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="id-row">
            <div class="id-block">
              <span class="detail-label">Mã giao dịch nội bộ</span>
              <strong class="font-monospace">
                {{ selectedTransaction?.transactionId || selectedTransaction?.id || '---' }}
              </strong>
            </div>
            <div class="id-block id-block-provider">
              <span class="detail-label">Mã giao dịch VNPAY</span>
              <strong class="font-monospace">
                {{ selectedTransaction?.providerTransactionId || '---' }}
              </strong>
            </div>
            <div class="id-block id-block-order">
              <span class="detail-label">Mã đơn hàng</span>
              <div class="d-flex align-items-center gap-2">
                <strong class="font-monospace text-primary">{{
                  selectedTransaction?.orderCode
                }}</strong>
                <button
                  class="btn-copy"
                  @click="copyToClipboard(selectedTransaction?.orderCode)"
                  title="Sao chép"
                >
                  <i class="bi bi-clipboard"></i>
                </button>
              </div>
            </div>
          </div>

          <div class="badge-row">
            <div class="detail-group">
              <span class="detail-label">Loại GD</span>
              <span :class="['badge', 'badge-lg', getTypeClass(selectedTransaction?.type)]">
                {{ formatType(selectedTransaction?.type) }}
              </span>
            </div>
            <div class="detail-group">
              <span class="detail-label">Trạng thái</span>
              <span :class="['badge', 'badge-lg', getStatusClass(selectedTransaction?.status)]">
                {{ formatStatus(selectedTransaction?.status) }}
              </span>
            </div>
          </div>

          <div class="amount-box">
            <span class="detail-label">Ảnh hưởng dòng tiền</span>
            <strong class="amount-value" :class="getAmountClass(selectedTransaction)">
              {{ formatTransactionAmount(selectedTransaction) }} VND
            </strong>
            <small v-if="selectedTransaction?.status !== 'SUCCESS'" class="amount-caption">
              {{ getAmountCaption(selectedTransaction) }} — chưa ảnh hưởng dòng tiền thực tế
            </small>
          </div>

          <div class="note-box">
            <span class="detail-label"><i class="bi bi-info-circle"></i> Ghi chú (Log)</span>
            <p class="note-text">{{ selectedTransaction?.note || 'Không có ghi chú' }}</p>
          </div>

          <div v-if="selectedTransaction?.reconciliationIssue" class="note-box reconciliation-warning">
            <span class="detail-label"><i class="bi bi-exclamation-triangle"></i> Cần xử lý</span>
            <p class="note-text">{{ selectedTransaction.reconciliationIssue }}</p>
          </div>

          <div v-if="selectedTransaction?.isReconciled" class="reconcile-audit">
            <i class="bi bi-person-check"></i>
            Đối soát bởi <strong>{{ selectedTransaction.reconciledBy || 'Không rõ' }}</strong>
            lúc {{ formatDate(selectedTransaction.reconciledAt) }}
          </div>

          <!-- Thông tin mở rộng Order -->
          <div v-if="selectedTransaction?.recipientName" class="recipient-box">
            <h6 class="recipient-title">
              <i class="bi bi-person-lines-fill"></i> Thông tin người nhận
            </h6>
            <div class="detail-group mb-2">
              <span class="detail-label">Tên &amp; SĐT</span>
              <strong
                >{{ selectedTransaction.recipientName || '---' }} ·
                {{ selectedTransaction.recipientPhone || '---' }}</strong
              >
            </div>
            <div class="detail-group mb-0">
              <span class="detail-label">Địa chỉ giao</span>
              <span class="text-muted address-text">
                {{ selectedTransaction.shippingAddress || '---' }}
              </span>
            </div>
          </div>

          <div class="modal-footer-note">
            <i class="bi bi-clock-history"></i>
            Tạo lúc: {{ formatDate(selectedTransaction?.createdAt) }}
          </div>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast.show" class="toast-alert" :class="{ error: toast.type === 'error' }">
        <i
          class="bi"
          :class="toast.type === 'error' ? 'bi-x-circle-fill' : 'bi-check-circle-fill'"
        ></i>
        <div>
          <strong>{{ toast.title }}</strong>
          <span>{{ toast.message }}</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import { useFinancialReport } from '@/composables/useFinancialReport'

// Marcus refactor: component chỉ còn nhiệm vụ render giao diện.
const {
  loading,
  exporting,
  filteredTotal,
  filteredTransactions,
  handleExportFilteredExcel,
  stats,
  chartOptions,
  chartSeries,
  keywordInput,
  onSearchInput,
  filters,
  onFilterChange,
  activeDatePreset,
  applyDatePreset,
  onDateFilterChange,
  resetFilters,
  pagedTransactions,
  currentPage,
  pageSize,
  totalPages,
  pageItems,
  isModalOpen,
  selectedTransaction,
  openDetailModal,
  closeDetailModal,
  copyToClipboard,
  formatCurrencyVnd,
  formatTransactionAmount,
  getAmountClass,
  getAmountCaption,
  formatType,
  formatStatus,
  getTypeClass,
  getStatusClass,
  formatDate,
  confirmReconciliation,
  toast,
  chartPeriodLabel,
} = useFinancialReport()
</script>

<style scoped src="@/assets/css/FinancialReport.css"></style>
