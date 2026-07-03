<template>
  <div class="table-section">
    <table class="tbl">
      <thead>
        <tr>
          <th style="width:30px">ID</th>
          <th style="width:90px">ẢNH</th>
          <th>TIÊU ĐỀ</th>
          <th style="width:170px">VỊ TRÍ</th>
          <th style="width:50px">THỨ TỰ</th>
          <th style="width:110px">THỜI GIAN CHẠY</th>
          <th style="width:110px">TRẠNG THÁI</th>
          <th style="width:70px">THAO TÁC</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!banners.length">
          <td colspan="8" class="empty-row">
            <i class="bi bi-inbox empty-icon"></i>
            <span>Không có banner nào</span>
          </td>
        </tr>
        <tr v-for="(b, i) in banners" :key="b.id" :class="i % 2 === 1 ? 'row-alt' : ''">
          <td class="td-id">#{{ i + 1 }}</td>
          <td>
            <div class="img-cell">
              <img v-if="b.imageUrl" :src="b.imageUrl" :alt="b.title" class="thumb"
                @error="e => e.target.style.display='none'" />
              <div v-else class="thumb-placeholder"><i class="bi bi-image"></i></div>
            </div>
          </td>
          <td class="title-cell">
            <div class="title-main">{{ b.title }}</div>
            <div v-if="b.linkUrl" class="title-sub">
              <i class="bi bi-link-45deg" style="font-size:11px"></i> {{ b.linkUrl }}
            </div>
          </td>
          <td>
            <span class="pos-badge" :title="posLabel(b.positionId)">{{ posLabel(b.positionId) }}</span>
          </td>
          <td class="td-center">
            <span class="order-badge">{{ b.displayOrder }}</span>
          </td>
          <td>
            <div class="date-col">
              <span>{{ fmtDate(b.startDate) }}</span>
              <span class="date-sep">→</span>
              <span>{{ fmtDate(b.endDate) }}</span>
            </div>
          </td>
          <td>
            <span class="status-badge" :class="statusOf(b).cls">
              {{ statusOf(b).label }}
            </span>
          </td>
          <td>
            <div class="action-row">
              <button class="btn-action btn-edit" @click="$emit('edit', b)" title="Sửa">
                <i class="bi bi-pencil-square"></i>
              </button>
              <button class="btn-action btn-del" @click="$emit('delete', b)" title="Xóa">
                <i class="bi bi-trash"></i>
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
const props = defineProps({
  banners: { type: Array, default: () => [] },
  positions: { type: Array, default: () => [] },
});
defineEmits(['edit', 'delete']);

function posLabel(positionId) {
  const found = props.positions.find(p => String(p.value) === String(positionId));
  return found ? found.label : '—';
}
function fmtDate(d) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('vi-VN', { day:'2-digit', month:'2-digit', year:'numeric' });
}
function statusOf(b) {
  if (!b.isActive) return { label: 'Tạm ẩn', cls: 'st-hidden' };
  const now = new Date();
  const s = b.startDate ? new Date(b.startDate) : null;
  const e = b.endDate   ? new Date(b.endDate)   : null;
  if (e && now > e)  return { label: 'Hết hạn',      cls: 'st-expired' };
  if (s && now < s)  return { label: 'Lên lịch chạy', cls: 'st-scheduled' };
  return { label: 'Đang hiển thị', cls: 'st-active' };
}
</script>

<style scoped>
.table-section { overflow-x: auto; }
.tbl {
 width: 100%;
  min-width: 1250px;      /* thêm dòng này */
  border-collapse: collapse;
  font-size: 13px;
  table-layout: auto;
}
.tbl thead tr {
  border-bottom: 2px solid #f3e8ee;
}
.tbl th {
 padding: 14px 24px;
  text-align: left;
  font-size: 11px;
  font-weight: 700;
  color: #f55d9b;
  letter-spacing: 0.1em;
  white-space: nowrap;
  vertical-align: middle;
}
.tbl td {
  padding: 10px 12px;
  color: #111827;
  border-bottom: 1px solid #f9f0f5;
  vertical-align: middle;
}
.row-alt td { background: #fdf8fb; }
.tbl tr:hover td { background: #fff0f7; transition: background 0.1s; }
.td-id { color: #9ca3af; font-size: 11px; font-weight: 500; }
.td-center { text-align: center; }

/* Ảnh */
.img-cell { width:72px; min-width:72px; max-width:72px; height:42px; border-radius: 6px; overflow: hidden; background: #f9f0f5; display:flex; align-items:center; justify-content:center; }
.thumb { width: 100%; height: 100%; object-fit: cover; }
.thumb-placeholder { color: #e0b8cc; font-size: 20px; }

/* Tiêu đề */
.title-main {
  font-weight: 500;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.title-sub {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Vị trí — ellipsis + tooltip để tránh vỡ layout khi tên vị trí dài */
.pos-badge {
  display: inline-block;
  max-width: 100%;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  background: #fff0f7;
  color: #d63384;
  border: 1px solid #f3d6e3;
  white-space: normal;
  word-break: break-word;
  line-height: 1.4;
  box-sizing: border-box;
  cursor: default;
}

/* Thứ tự */
.order-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #f9f0f5;
  color: #d63384;
  font-size: 13px;
  font-weight: 600;
}

/* Thời gian */
.date-col { display: flex; flex-direction: column; gap: 1px; font-size: 12px; color: #4b5563; }
.date-sep { color: #d1d5db; font-size: 10px; }

/* Trạng thái */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}
.st-active    { background: #f0fdf4; color: #15803d; }
.st-scheduled { background: #fff7ed; color: #c2410c; }
.st-expired   { background: #f1f5f9; color: #64748b; }
.st-hidden    { background: #fff0f7; color: #c72250; }

/* Thao tác */
.action-row { display: flex; gap: 6px; align-items: center; }
.btn-action {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  transition: all 0.15s;
}
.btn-edit { background: #fff0f7; color: #d63384; }
.btn-edit:hover { background: #ffe4ef; }
.btn-del { background: #fff5f5; color: #dc2626; }
.btn-del:hover { background: #fee2e2; }


/* Sticky action column */
.tbl th:last-child,
.tbl td:last-child{
 position:sticky;
 right:0;
 background:#fff;
 z-index:3;
}
.row-alt td:last-child{background:#fdf8fb;}
.tbl tr:hover td:last-child{background:#fff0f7;}

/* Empty */
.empty-row { text-align: center; padding: 48px 20px; color: #9ca3af; }
.empty-icon { font-size: 32px; display: block; margin: 0 auto 8px; color: #e0b8cc; }
</style>

.title-cell{overflow:hidden;min-width:250px;}
