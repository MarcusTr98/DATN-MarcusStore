<template>
  <div class="card-promo">
    <span
      v-for="(item, idx) in items"
      :key="idx"
      class="card-promo__chip"
      :class="`card-promo__chip--${item.tone}`"
    >
      <i v-if="item.icon" :class="item.icon" class="card-promo__icon"></i>
      <span class="card-promo__text">{{ item.text }}</span>
    </span>
  </div>
</template>

<script setup>
// Dùng cho ProductCard ở trang Home.
// SỬA NỘI DUNG TẠI ĐÂY -> tất cả card SP trên Home tự đổi theo.
// Mỗi promotion có: text (nội dung hiển thị),
// icon (FontAwesome class, có thể bỏ trống), tone (màu nền: red|blue|orange|green|gray).
// Mỗi card sẽ hiển thị tất cả text trong list.
const PROMOTIONS = [
  {
    text: 'Học sinh/Sinh viên giảm đến 500.000đ',
    icon: 'fa-solid fa-graduation-cap',
    tone: 'blue',
  },
  {
    text: 'Giảm 250.000đ vào tháng 6 này',
    icon: 'fa-solid fa-gift',
    tone: 'red',
  },
  {
    text: 'Bảo hành đổi trả trong 12 tháng',
    icon: 'fa-solid fa-shield-halved',
    tone: 'gray',
  },
]

// Hiện tất cả text trong list.
const items = PROMOTIONS
</script>

<style scoped>
.card-promo {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin: 4px 0 6px;
  min-height: 22px;
}
.card-promo__chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
  line-height: 1.4;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-promo__chip--red {
  background: #fff1f0;
  color: #d70018;
  border: 1px solid #ffd6d6;
}
.card-promo__chip--blue {
  background: #e8f3ff;
  color: #0b66d6;
  border: 1px solid #cfe5ff;
}
.card-promo__chip--orange {
  background: #fff5e6;
  color: #d97706;
  border: 1px solid #ffe1b8;
}
.card-promo__chip--green {
  background: #eafbef;
  color: #1f9d55;
  border: 1px solid #cfeed8;
}
.card-promo__chip--gray {
  background: #f0f0f0;
  color: #555;
  border: 1px solid #dcdcdc;
}
.card-promo__icon {
  font-size: 11px;
}
.card-promo__text {
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>