<template>
  <div class="statistic-card">

    <div class="title">
      <i class="fas fa-chart-bar"></i>
      <span>Thống kê đánh giá theo sản phẩm</span>
    </div>

    <div class="select-box">
      <select
          v-model="selectedProduct"
          @change="loadStatistic">

        <option value="">
          -- Chọn sản phẩm --
        </option>

        <option
            v-for="item in products"
            :key="item.productId"
            :value="item.productId">

          {{ item.productName }}

        </option>

      </select>
    </div>

    <div
        v-if="selectedProduct"
        class="star-grid">

      <div
          class="star-card"
          v-for="star in statistics"
          :key="star.star">

        <div class="star-title">

          {{ "⭐".repeat(star.star) }}

        </div>

        <div class="star-count">

          {{ star.count }}

        </div>

      </div>

    </div>

  </div>
</template>

<script setup>

import {ref,onMounted} from "vue";
import api from "@/utils/api";

const products = ref([]);
const statistics = ref([]);
const selectedProduct = ref("");

const emit = defineEmits([
    "changeProduct"
])

const loadProducts = async () => {

    const res = await api.get("/admin/reviews/products");

    products.value = res.data.data;

}

const loadStatistic = async () => {

    if(!selectedProduct.value){

        statistics.value = [];

        // Bỏ chọn sản phẩm => bỏ filter, bảng bên dưới hiện lại tất cả
        emit("changeProduct", null);

        return;

    }

    const res = await api.get(

        `/admin/reviews/statistics/${selectedProduct.value}`

    );

    statistics.value = res.data.data;

    // Bắn productId lên component cha để lọc bảng review theo đúng sản phẩm này
    emit("changeProduct", selectedProduct.value);

}

onMounted(loadProducts);

</script>

<style scoped>

.statistic-card{

    background:white;

    border:1px solid #ffd6e4;

    border-radius:14px;

    padding:18px 20px;

    margin-bottom:20px;

}

.title{

    display:flex;

    align-items:center;

    gap:8px;

    font-size:15px;

    font-weight:700;

    color:#f55d9b;

    margin-bottom:16px;

}

.select-box{

    margin-bottom:18px;

}

.select-box select{

    width:320px;

    max-width:100%;

    height:40px;

    border-radius:10px;

    border:1.5px solid #f3bfd3;

    padding:0 12px;

    font-size:14px;

    color:#333;

    cursor:pointer;

    transition: border-color .2s ease, box-shadow .2s ease;

}

.select-box select:focus{

    border-color:#f55d9b;

    box-shadow: 0 0 0 4px rgba(245,93,155,.12);

    outline:none;

}

.star-grid{

    display:grid;

    grid-template-columns:repeat(5,1fr);

    gap:14px;

}

.star-card{

    background:#fff7fb;

    border:1px solid #ffd6e4;

    border-radius:12px;

    text-align:center;

    padding:14px;

    transition:.25s;

}

.star-card:hover{

    transform:translateY(-3px);

    box-shadow:0 8px 18px rgba(245,93,155,.14);

}

.star-title{

    font-size:15px;

}

.star-count{

    margin-top:8px;

    font-size:22px;

    font-weight:800;

    color:#f55d9b;

}

</style>