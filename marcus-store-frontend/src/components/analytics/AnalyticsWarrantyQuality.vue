<template>
  <!-- Marcus thêm cho Analytics: hiển thị thống kê đọc từ module bảo hành của Đạt,
       không thực hiện cập nhật trạng thái hay thay đổi dữ liệu bảo hành. -->
  <section v-if="data" class="warranty-quality analysis-source-host">
    <AnalysisSourceBadge
      source="algorithm"
      detail="Tỷ lệ xử lý = yêu cầu đã kết thúc / tổng yêu cầu × 100%; tỷ lệ duyệt = số đã duyệt / (đã duyệt + từ chối) × 100%. Đây không phải tỷ lệ lỗi sản phẩm."
    />
    <header>
      <div>
        <span><i class="bi bi-shield-check"></i> Hậu mãi & chất lượng sản phẩm</span>
        <h2>Chỉ số yêu cầu bảo hành</h2>
        <p>
          Theo ngày khách gửi yêu cầu; dùng làm tín hiệu kiểm tra, không đồng nghĩa tỷ lệ lỗi tuyệt
          đối.
        </p>
      </div>
      <strong>{{ formatNumber(data.totalRequests?.currentValue) }} yêu cầu</strong>
    </header>

    <div class="warranty-quality__kpis">
      <article>
        <small>Chờ xác nhận</small><strong>{{ formatNumber(data.pendingRequests) }}</strong>
      </article>
      <article>
        <small>Đang xử lý</small><strong>{{ formatNumber(data.processingRequests) }}</strong>
      </article>
      <article>
        <small>Tỷ lệ đã xử lý</small
        ><strong>{{ formatPercent(data.resolutionRate?.currentPercent) }}</strong>
      </article>
      <article>
        <small>Tỷ lệ đồng ý bảo hành</small
        ><strong>{{ formatPercent(data.approvalRate?.currentPercent) }}</strong>
      </article>
    </div>

    <div class="warranty-quality__body">
      <div>
        <h3>Sản phẩm cần theo dõi</h3>
        <div v-if="data.productQuality?.length" class="warranty-quality__table-wrap">
          <table>
            <thead>
              <tr>
                <th>Sản phẩm</th>
                <th>Yêu cầu</th>
                <th>So kỳ trước</th>
                <th>Đã đồng ý</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="product in pagedProducts" :key="product.productId">
                <td>
                  <strong>{{ product.productName }}</strong
                  ><small>{{ product.brand || 'Chưa có hãng' }}</small>
                </td>
                <td>{{ formatNumber(product.currentRequests) }}</td>
                <td :class="changeClass(product.requestsChangePercent)">
                  {{ formatChange(product.requestsChangePercent) }}
                </td>
                <td>{{ formatPercent(product.approvalRate) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <nav
          v-if="totalPages > 1"
          class="warranty-pagination"
          aria-label="Phân trang sản phẩm bảo hành"
        >
          <button type="button" :disabled="page === 1" @click="page--">
            <i class="bi bi-chevron-left"></i>
          </button>
          <button
            v-for="number in totalPages"
            :key="number"
            type="button"
            :class="{ active: page === number }"
            @click="page = number"
          >
            {{ number }}
          </button>
          <button type="button" :disabled="page === totalPages" @click="page++">
            <i class="bi bi-chevron-right"></i>
          </button>
        </nav>
        <p v-else class="warranty-quality__empty">Chưa phát sinh yêu cầu bảo hành trong kỳ.</p>
      </div>

      <aside>
        <h3>Nhóm lý do</h3>
        <div v-for="reason in data.reasons" :key="reason.reason" class="warranty-reason">
          <div>
            <span>{{ reason.label }}</span
            ><strong>{{ formatNumber(reason.count) }}</strong>
          </div>
          <div class="warranty-reason__bar">
            <span :style="{ width: `${Math.min(100, reason.sharePercent)}%` }"></span>
          </div>
          <small>{{ formatPercent(reason.sharePercent) }} tổng yêu cầu</small>
        </div>
        <p v-if="!data.reasons?.length" class="warranty-quality__empty">Chưa có dữ liệu lý do.</p>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import AnalysisSourceBadge from '@/components/analytics/AnalysisSourceBadge.vue'

const props = defineProps({ data: { type: Object, default: null } })
const page = ref(1)
const pageSize = 5
const totalPages = computed(() =>
  Math.max(1, Math.ceil((props.data?.productQuality?.length || 0) / pageSize)),
)
const pagedProducts = computed(
  () => props.data?.productQuality?.slice((page.value - 1) * pageSize, page.value * pageSize) || [],
)
watch(
  () => props.data,
  () => {
    page.value = 1
  },
)

function formatNumber(value) {
  return Number(value || 0).toLocaleString('vi-VN')
}
function formatPercent(value) {
  return `${Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 })}%`
}
function formatChange(value) {
  if (value === null || value === undefined) return 'Mới phát sinh'
  if (Math.abs(value) < 0.01) return 'Không đổi'
  return `${value > 0 ? '+' : ''}${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 2 })}%`
}
function changeClass(value) {
  return Number(value || 0) > 0 ? 'is-warning' : 'is-positive'
}
</script>

<style scoped>
.warranty-quality {
  margin-top: 22px;
  border: 1px solid #cfe0f8;
  border-radius: 22px;
  padding: 22px;
  background: linear-gradient(145deg, #fff, #f4f8ff);
  box-shadow: 0 12px 34px rgba(23, 72, 135, 0.08);
}
.warranty-quality header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}
.warranty-quality header span {
  color: #2469c8;
  font-weight: 800;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.warranty-quality h2 {
  margin: 5px 0;
  font-size: 24px;
  color: #102a50;
}
.warranty-quality p {
  margin: 0;
  color: #64748b;
}
.warranty-quality header > strong {
  color: #1559b7;
  background: #e7f0ff;
  padding: 11px 16px;
  border-radius: 14px;
  white-space: nowrap;
}
.warranty-quality__kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 20px 0;
}
.warranty-quality__kpis article {
  padding: 15px;
  border: 1px solid #dbe7f7;
  border-radius: 16px;
  background: #fff;
}
.warranty-quality__kpis small {
  display: block;
  color: #64748b;
}
.warranty-quality__kpis strong {
  display: block;
  margin-top: 5px;
  font-size: 23px;
  color: #163762;
}
.warranty-quality__body {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(260px, 0.8fr);
  gap: 18px;
}
.warranty-quality h3 {
  font-size: 16px;
  color: #17345d;
  margin: 0 0 12px;
}
.warranty-quality__table-wrap {
  overflow: auto;
  border: 1px solid #dce7f5;
  border-radius: 15px;
  background: #fff;
}
.warranty-quality table {
  width: 100%;
  border-collapse: collapse;
}
.warranty-quality th,
.warranty-quality td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #edf2f8;
}
.warranty-quality th {
  font-size: 12px;
  color: #64748b;
  background: #f7faff;
}
.warranty-quality td strong,
.warranty-quality td small {
  display: block;
}
.warranty-quality td small {
  color: #8492a6;
  margin-top: 3px;
}
.warranty-quality tbody tr:last-child td {
  border-bottom: 0;
}
.is-warning {
  color: #dc2626;
  font-weight: 700;
}
.is-positive {
  color: #16865a;
  font-weight: 700;
}
.warranty-quality aside {
  border: 1px solid #dce7f5;
  border-radius: 15px;
  background: #fff;
  padding: 15px;
}
.warranty-reason {
  margin-bottom: 13px;
}
.warranty-reason > div:first-child {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
}
.warranty-reason__bar {
  height: 7px;
  background: #edf3fb;
  border-radius: 10px;
  overflow: hidden;
  margin: 7px 0;
}
.warranty-reason__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #3182e8, #74aaf0);
}
.warranty-reason small {
  color: #7b8ca4;
}
.warranty-quality__empty {
  padding: 18px;
  text-align: center;
  background: #fff;
  border-radius: 14px;
}
.warranty-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
}
.warranty-pagination button {
  display: grid;
  place-items: center;
  min-width: 34px;
  height: 34px;
  padding: 0 8px;
  border: 1px solid #d6e3f5;
  border-radius: 9px;
  background: #fff;
  color: #49617f;
  font-weight: 700;
}
.warranty-pagination button:hover:not(:disabled),
.warranty-pagination button.active {
  border-color: #2e75d3;
  background: #2e75d3;
  color: #fff;
}
.warranty-pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
@media (max-width: 900px) {
  .warranty-quality__kpis {
    grid-template-columns: repeat(2, 1fr);
  }
  .warranty-quality__body {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 560px) {
  .warranty-quality {
    padding: 16px;
  }
  .warranty-quality header {
    align-items: flex-start;
    flex-direction: column;
    padding-top: 34px;
  }
  .warranty-quality__kpis {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
