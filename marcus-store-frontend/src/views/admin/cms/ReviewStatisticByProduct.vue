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

const loadProducts = async () => {

    const res = await api.get("/admin/reviews/products");

    products.value = res.data.data;

}

const loadStatistic = async () => {

    if(!selectedProduct.value){

        statistics.value=[];

        return;

    }

    const res = await api.get(

        `/admin/reviews/statistics/${selectedProduct.value}`

    );

    statistics.value=res.data.data;

}

onMounted(loadProducts);

</script>

<style scoped>

.statistic-card{

    background:white;

    border:1px solid #ffd6e4;

    border-radius:18px;

    padding:24px;

    margin-bottom:25px;

}

.title{

    display:flex;

    align-items:center;

    gap:10px;

    font-size:20px;

    font-weight:700;

    color:#f55d9b;

    margin-bottom:20px;

}

.select-box{

    margin-bottom:25px;

}

.select-box select{

    width:420px;

    height:48px;

    border-radius:12px;

    border:1px solid #f3bfd3;

    padding:0 15px;

}

.star-grid{

    display:grid;

    grid-template-columns:repeat(5,1fr);

    gap:18px;

}

.star-card{

    background:#fff7fb;

    border:1px solid #ffd6e4;

    border-radius:15px;

    text-align:center;

    padding:20px;

    transition:.25s;

}

.star-card:hover{

    transform:translateY(-4px);

    box-shadow:0 8px 20px rgba(245,93,155,.15);

}

.star-title{

    font-size:22px;

}

.star-count{

    margin-top:12px;

    font-size:32px;

    font-weight:800;

    color:#f55d9b;

}

</style>