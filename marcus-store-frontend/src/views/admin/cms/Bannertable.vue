<template>
  <div class="table-wrap">
    <table class="tbl">
      <thead>
        <tr>
          <th style="width: 44px">#</th>
          <th style="width: 84px">Ảnh</th>
          <th>Tiêu đề</th>
          <th style="width: 110px">Vị trí</th>
          <th style="width: 80px">Thứ tự</th>
          <th style="width: 130px">Thời gian chạy</th>
          <th style="width: 120px">Trạng thái</th>
          <th style="width: 110px">Hành động</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!banners.length">
          <td colspan="8" class="empty">
            <i class="ti ti-inbox empty-icon"></i>
            Không có banner nào
          </td>
        </tr>

        <tr v-for="(b, i) in banners" :key="b.id">
          <td class="idx">{{ i + 1 }}</td>

          <td>
            <div v-if="b.imageUrl" class="img-preview">
              <img
                :src="b.imageUrl"
                :alt="b.title"
                @error="onImgError"
              />
            </div>
            <div v-else class="img-placeholder">
              <i class="ti ti-photo"></i>
            </div>
          </td>

          <td>
            <div class="title-text">{{ b.title }}</div>
            <div v-if="b.linkUrl" class="link-text">
              <i class="ti ti-link"></i> {{ truncate(b.linkUrl, 30) }}
            </div>
          </td>

          <td>
            <span class="pos-tag">{{ positionLabel(b.positionId) }}</span>
          </td>

          <td>
            <span class="order-num">{{ b.displayOrder }}</span>
          </td>

          <td>
            <div class="date-range">
              <span class="date-txt">{{ fmtDate(b.startDate) }}</span>
              <span class="date-txt date-end">→ {{ fmtDate(b.endDate) }}</span>
            </div>
          </td>

          <td>
            <span class="badge" :class="statusOf(b).cls">
              <span class="dot" :class="statusOf(b).dot"></span>
              {{ statusOf(b).label }}
            </span>
          </td>

          <td>
            <div class="action-wrap">
              <button class="btn-edit" @click="$emit('edit', b)">
                <i class="ti ti-edit"></i>Sửa
              </button>
              <button class="btn-del" @click="$emit('delete', b)">
                <i class="ti ti-trash"></i>
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="pagination">
      <span class="page-info">Hiển thị 1–{{ banners.length }} / {{ banners.length }}</span>
      <div class="page-btns">
        <button class="pbtn active">1</button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  banners: {
    type: Array,
    default: () => [],
  },
  positions: {
    type: Array,
    default: () => [
      { value: 'homepage', label: 'Trang chủ' },
      { value: 'product', label: 'Trang sản phẩm' },
      { value: 'sidebar', label: 'Sidebar' },
      { value: 'popup', label: 'Popup' },
    ],
  },
});

defineEmits(['edit', 'delete']);

// ---- Helpers ----
function positionLabel(positionId) {
  const found = props.positions.find((p) => p.value === positionId);
  return found ? found.label : positionId;
}

function fmtDate(d) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

function truncate(str, len) {
  if (!str) return '';
  return str.length > len ? str.slice(0, len) + '...' : str;
}

function onImgError(e) {
  e.target.parentElement.innerHTML = '<i class="ti ti-photo-off img-broken"></i>';
}

// Tính trạng thái real-time dựa trên is_active + start_date + end_date
function statusOf(b) {
  if (!b.isActive) {
    return { label: 'Tạm ẩn', cls: 'badge-hidden', dot: 'dot-red' };
  }
  const now = new Date();
  const start = b.startDate ? new Date(b.startDate) : null;
  const end = b.endDate ? new Date(b.endDate) : null;

  if (end && now > end) {
    return { label: 'Hết hạn', cls: 'badge-expired', dot: 'dot-gray' };
  }
  if (start && now < start) {
    return { label: 'Lên lịch chạy', cls: 'badge-scheduled', dot: 'dot-yellow' };
  }
  return { label: 'Đang hiển thị', cls: 'badge-active', dot: 'dot-green' };
}

defineExpose({ statusOf });
</script>

<style scoped>
.table-wrap {
  background: #fff;
  border: 1px solid #f3d6e3;
  border-radius: 10px;
  overflow: hidden;
}
.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.tbl thead {
  background: #fff0f7;
}
.tbl th {
  padding: 11px 14px;
  text-align: left;
  color: #b4557d;
  font-weight: 500;
  font-size: 12px;
  letter-spacing: 0.03em;
  border-bottom: 1px solid #f3d6e3;
}
.tbl td {
  padding: 11px 14px;
  color: #202636;
  border-bottom: 1px solid #fff0f7;
  vertical-align: middle;
}
.tbl tr:last-child td {
  border-bottom: none;
}
.tbl tr:hover td {
  background: #fffafd;
}
.idx {
  color: #b4557d;
  font-size: 12px;
}
.img-preview {
  width: 72px;
  height: 40px;
  border-radius: 5px;
  background: #fff0f7;
  border: 1px solid #f3d6e3;
  overflow: hidden;
}
.img-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.img-placeholder,
.img-broken {
  width: 72px;
  height: 40px;
  border-radius: 5px;
  background: #fff0f7;
  border: 1px solid #f3d6e3;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #efbdd2;
  font-size: 18px;
}
.title-text {
  font-weight: 500;
  color: #202636;
  font-size: 13px;
}
.link-text {
  font-size: 11px;
  color: #b4557d;
  margin-top: 2px;
}
.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;
}
.badge-active {
  background: #f0fdf4;
  color: #15803d;
}
.badge-scheduled {
  background: #fff0d9;
  color: #9a5b00;
}
.badge-expired {
  background: #f1f5f9;
  color: #64748b;
}
.badge-hidden {
  background: #ffe4ef;
  color: #c72250;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}
.dot-green {
  background: #15803d;
}
.dot-yellow {
  background: #9a5b00;
}
.dot-gray {
  background: #94a3b8;
}
.dot-red {
  background: #c72250;
}
.pos-tag {
  background: #fff0f7;
  color: #d63384;
  border: 1px solid #f3d6e3;
  border-radius: 5px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
}
.order-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  background: #fff0f7;
  border: 1px solid #f3d6e3;
  border-radius: 6px;
  color: #d63384;
  font-size: 12px;
  font-weight: 500;
}
.date-range {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.date-txt {
  font-size: 12px;
  color: #6b7280;
}
.date-end {
  color: #b4557d;
}
.action-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}
.btn-edit {
  background: #fff0f7;
  border: 1px solid #f3d6e3;
  color: #d63384;
  border-radius: 6px;
  padding: 5px 10px;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s;
}
.btn-edit:hover {
  background: #ffe4ef;
  border-color: #efbdd2;
}
.btn-del {
  background: #fef2f2;
  border: 1px solid #f5c2c7;
  color: #b91c1c;
  border-radius: 6px;
  padding: 5px 10px;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s;
}
.btn-del:hover {
  background: #fee2e2;
}
.empty {
  text-align: center;
  padding: 40px 20px;
  color: #b4557d;
}
.empty-icon {
  font-size: 28px;
  display: block;
  margin: 0 auto 8px;
  color: #efbdd2;
}
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid #f3d6e3;
  background: #fffafd;
}
.page-info {
  font-size: 12px;
  color: #6b7280;
}
.page-btns {
  display: flex;
  gap: 4px;
}
.pbtn {
  border: 1px solid #f3d6e3;
  background: #fff;
  color: #6b7280;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}
.pbtn.active {
  background: #f55d9b;
  color: #fff;
  border-color: #f55d9b;
}
</style>