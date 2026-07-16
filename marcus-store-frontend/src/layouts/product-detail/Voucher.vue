<template>
  <div class="promo-box">
    <div class="promo-header">
      <span class="promo-title">🎁 Khuyến mãi đi kèm</span>
      <a href="#" class="promo-viewall" @click.prevent>
        Xem tất cả voucher <span class="arrow">›</span>
      </a>
    </div>

    <!-- Voucher card -->
    <div class="voucher-card">
      <div class="voucher-discount">
        <span class="giam-label">Giảm</span>
        <span class="percent">{{ voucher.percent }}%</span>
      </div>
      <div class="voucher-info">
        <div class="voucher-desc">{{ voucher.description }}</div>
        <div class="voucher-expiry">Thời hạn: {{ voucher.expiry }}</div>
      </div>
      <button class="voucher-btn" @click="collectVoucher">
        {{ collected ? 'Đã thu thập' : 'Thu thập' }}
      </button>
    </div>

    <!-- Danh sách quà tặng / ưu đãi -->
    <ul class="promo-list">
      <li v-for="(item, idx) in promoItems" :key="idx" class="promo-item">
        <span class="promo-index">{{ idx + 1 }}</span>
        <span class="promo-text">
          {{ item.text }}
          <a v-if="item.hasDetail" href="#" class="promo-detail-link" @click.prevent>Xem chi tiết</a>
        </span>
      </li>
    </ul>
  </div>
</template>

<script setup>
// ⚠️ MOCK - dữ liệu fix cứng để test UI, thay bằng API thật khi backend có endpoint khuyến mãi
import { ref } from 'vue';

const collected = ref(false);

const voucher = ref({
  percent: 5,
  description: 'Tối đa 3 triệu cho sản phẩm có hiện tại',
  expiry: '22:00 16/07/2026',
});

const promoItems = ref([
  { text: 'Chi thêm 30K, nhận Sim/Esim 5G VNSKY, có ngay 3GB data/ngày+500 phút gọi Mobifone & VNSKY, miễn phí 30 ngày đầu - chỉ áp dụng tại cửa hàng', hasDetail: true },
  { text: 'Giảm thêm 10% cho Pin dự phòng - Camera giám sát - Đồng hồ trẻ em - Gia dụng - Sức khỏe Làm đẹp khi mua Điện thoại/Laptop', hasDetail: true },
  { text: 'Tặng PMH trị giá 500.000 đ cho AIRPODS MỚI - không áp dụng kèm ưu đãi khác/HSSV', hasDetail: true },
]);

function collectVoucher() {
  collected.value = true;
}
</script>

<style scoped>
.promo-box {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 14px 16px;
  background: #fff;
}

.promo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.promo-title {
  font-weight: 700;
  font-size: 15px;
}

.promo-viewall {
  font-size: 13px;
  color: #e02b2b;
  text-decoration: none;
  white-space: nowrap;
}

.arrow {
  font-weight: 700;
}

.voucher-card {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px dashed #f0a0a0;
  background: #fff8f8;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 12px;
}

.voucher-discount {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #e02b2b;
  color: #fff;
  border-radius: 6px;
  padding: 4px 10px;
  min-width: 54px;
  line-height: 1.1;
}

.giam-label {
  font-size: 11px;
}

.percent {
  font-size: 16px;
  font-weight: 700;
}

.voucher-info {
  flex: 1;
  min-width: 0;
}

.voucher-desc {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.voucher-expiry {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.voucher-btn {
  background: #e02b2b;
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.voucher-btn:hover {
  background: #c92222;
}

.promo-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.promo-item {
  display: flex;
  gap: 8px;
  font-size: 13px;
  color: #333;
  padding: 6px 0;
  border-bottom: 1px dashed #eee;
}

.promo-item:last-child {
  border-bottom: none;
}

.promo-index {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #666;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.promo-text {
  line-height: 1.5;
}

.promo-detail-link {
  color: #1a73e8;
  text-decoration: none;
  white-space: nowrap;
  margin-left: 4px;
}

.promo-detail-link:hover {
  text-decoration: underline;
}
</style>