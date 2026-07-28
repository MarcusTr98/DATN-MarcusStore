<template>
  <div class="review-page">

    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <div class="icon-box">
          <i class="fas fa-star"></i>
        </div>

        <div>
          <h2>Quản lý đánh giá</h2>
          <p>
            Quản lý đánh giá và phản hồi của khách hàng.
          </p>
        </div>
      </div>

      <button class="btn-refresh" @click="loadReviews">
        <i class="fas fa-rotate-right"></i>
        Làm mới
      </button>
    </div>

    <!-- Statistic -->
    <div class="stats-grid">

      <div class="stat-card">
        <span>Tổng đánh giá</span>
        <strong>{{ totalReviews }}</strong>
      </div>

      <div class="stat-card">
        <span>Đã trả lời</span>
        <strong class="text-accent">{{ repliedReviews }}</strong>
      </div>

      <div class="stat-card">
        <span>Chưa trả lời</span>
        <strong>{{ notReplyReviews }}</strong>
      </div>

    </div>
    <ReviewStatisticByProduct
    @changeProduct="changeProductFilter"
/>
    <!-- Filter -->
    <div class="filter-card">

      <div class="filter-item">
        <label>TÌM KIẾM</label>

        <div class="search-box">
          <i class="fas fa-search"></i>

          <input
              v-model="filters.keyword"
              placeholder="Tìm theo tên khách hàng">
        </div>
      </div>

      <div class="filter-item">

        <label>SỐ SAO</label>

        <select v-model="filters.rating">

          <option value="">Tất cả</option>

          <option :value="5">5 Sao</option>

          <option :value="4">4 Sao</option>

          <option :value="3">3 Sao</option>

          <option :value="2">2 Sao</option>

          <option :value="1">1 Sao</option>

        </select>

      </div>

      <div class="filter-item">

        <label>TRẠNG THÁI</label>

        <select v-model="filters.replied">

          <option value="">Tất cả</option>

          <option :value="true">Đã trả lời</option>

          <option :value="false">Chưa trả lời</option>

        </select>

      </div>

      <div class="filter-action">

        <button class="btn-refresh" @click="search">

          <i class="fas fa-search"></i>

        </button>

      </div>

    </div>

    <!-- Table + Pagination (dính liền thành 1 khối) -->
    <div class="table-card">
      <ReviewTable
          :reviews="reviews"
          @detail="openDetail"
          @reply="openReply"
          @edit="openEditReply"
          @delete="deleteReview"
      />
      <div class="pagination">

          <div class="left">

              Tổng <b>{{ totalElements }}</b> đánh giá

          </div>

          <div class="right">

              <span>Hiển thị</span>

              <select
                  v-model="size"
                  @change="changeSize"
              >

                  <option :value="5">5</option>

                  <option :value="10">10</option>

                  <option :value="20">20</option>

              </select>

              <button
                  @click="previousPage"
                  :disabled="page===0"
              >
                  Trước
              </button>

              <span>

                  Trang {{ page+1 }} / {{ totalPages }}

              </span>

              <button
                  @click="nextPage"
                  :disabled="page>=totalPages-1"
              >
                  Sau
              </button>

          </div>

      </div>
    </div>

    <ReviewDetailModal
        :visible="detailVisible"
        :review="selectedReview"
        @close="onCloseModal"
        @saved="loadReviews"
    />
  </div>
</template>

<script setup>

import {ref, computed, onMounted, nextTick} from "vue";
import ReviewTable from "@/views/admin/cms/ReviewTable.vue";
import api from "@/utils/api";
import ReviewDetailModal from "./ReviewDetailModal.vue";
import ReviewStatisticByProduct from "./ReviewStatisticByProduct.vue";
const page = ref(0)
const size = ref(5)

const totalPages = ref(0)
const totalElements = ref(0)
// ID ngẫu nhiên để phát hiện trường hợp component bị mount 2 lần (route/layout lỗi)
const instanceId = Math.random().toString(36).slice(2, 8);
console.log("[ReviewManagement] mounted instance:", instanceId);

const detailVisible = ref(false)

const selectedReview = ref({})
const reviews = ref([]);
const selectedProductId = ref(null);
const filters = ref({

  keyword: "",

  rating: "",

  replied: ""

});
const openDetail = (review) => {
  console.log(`[${instanceId}] openDetail called, review =`, review)

  selectedReview.value = review
  detailVisible.value = true

  nextTick(() => {
    console.log(`[${instanceId}] after nextTick, detailVisible =`, detailVisible.value)
  })
}
const onCloseModal = () => {
  console.log(`[${instanceId}] onCloseModal (@close received from ReviewDetailModal)`)
  detailVisible.value = false
}
const openReply = (review) => {
  console.log("reply", review)
}

const openEditReply = (review) => {
  console.log("edit", review)
}

const deleteReview = async (review) => {

  const ok = confirm(
      `Bạn có chắc muốn xóa đánh giá của "${review.fullName}"?`
  )

  if (!ok) return

  try {

    await api.delete(`/admin/reviews/${review.reviewId}`)

    alert("Đã xóa đánh giá.")

    await loadReviews()

  } catch (e) {

    console.error(e)

    alert("Xóa thất bại.")

  }

}
const changeProductFilter = (productId) => {

    selectedProductId.value = productId

    page.value = 0

    search()

}
const totalReviews = computed(() => reviews.value.length);

const repliedReviews = computed(() =>
    reviews.value.filter(i => i.reply).length
);

const notReplyReviews = computed(() =>
    reviews.value.filter(i => !i.reply).length
);

const loadReviews = async () => {

  const res = await api.get("/admin/reviews", {
    params: {
      page: page.value,
      size: size.value
    }
  })

  reviews.value = res.data.data.content
  totalPages.value = res.data.data.totalPages
  totalElements.value = res.data.data.totalElements

}

const search = async () => {

  const res = await api.get("/admin/reviews/search", {

    params: {

      page: page.value,

      size: size.value,

      keyword: filters.value.keyword || null,

      productId: selectedProductId.value || null,

      rating: filters.value.rating || null,

      replied:
        filters.value.replied === ""
            ? null
            : filters.value.replied

    }

  })

  reviews.value = res.data.data.content
  totalPages.value = res.data.data.totalPages
  totalElements.value = res.data.data.totalElements

}
const previousPage = () => {

    if(page.value>0){

        page.value--

        loadReviews()

    }

}

const nextPage = () => {

    if(page.value<totalPages.value-1){

        page.value++

        loadReviews()

    }

}

const changeSize = () => {

    page.value=0

    loadReviews()

}
onMounted(loadReviews);

</script>

<style scoped>

.review-page{

    padding:20px;
    max-width:1300px;
    margin:0 auto;
    font-family: -apple-system, "Segoe UI", Roboto, Inter, sans-serif;

}

/* Header */

.page-header{

    display:flex;

    justify-content:space-between;

    align-items:center;

    background:white;

    border-radius:14px;

    padding:20px 24px;

    margin-bottom:20px;

    border:1px solid #ffd7e5;

    transition: box-shadow .25s ease;

}

.page-header:hover{
    box-shadow: 0 8px 24px rgba(245,93,155,.08);
}

.header-left{

    display:flex;

    align-items:center;

    gap:14px;

}

.icon-box{

    width:44px;

    height:44px;

    border-radius:12px;

    background: linear-gradient(135deg, #f55d9b, #ef3f89);

    color:white;

    display:flex;

    justify-content:center;

    align-items:center;

    font-size:18px;

    box-shadow: 0 6px 16px rgba(245,93,155,.3);

    transition: transform .25s ease;

    flex-shrink:0;

}

.icon-box:hover{
    transform: rotate(-8deg) scale(1.05);
}

.header-left h2{

    color:#1f2430;

    font-weight:800;

    font-size:19px;

    letter-spacing:-.2px;

}

.header-left p{

    color:#828a97;

    font-size:13px;

    margin-top:2px;

}

/* Statistic */

.stats-grid{

    display:grid;

    grid-template-columns:repeat(3, 1fr);

    gap:16px;

    margin-bottom:20px;

}

.stat-card{

    position:relative;

    background:white;

    border-radius:14px;

    padding:20px 24px;

    border:1px solid #ffd7e5;

    display:flex;

    flex-direction:column;

    gap:10px;

    transition: transform .25s ease, box-shadow .25s ease;

    min-width:0;

}

.stat-card:hover{

    transform: translateY(-3px);

    box-shadow: 0 12px 24px rgba(245,93,155,.12);

}

.stat-card span{

    color:#6b7280;

    font-weight:600;

    font-size:14px;

}

.stat-card strong{

    font-size:32px;

    font-weight:700;

    letter-spacing:-.3px;

    color:#1f2430;

}

.text-accent{

    color:#f55d9b;

}

/* Filter */

.filter-card{

    display:grid;

    grid-template-columns:2fr 1fr 1fr 56px;

    gap:16px;

    background:white;

    border-radius:14px;

    padding:18px 20px;

    border:1px solid #ffd7e5;

    margin-bottom:20px;

}

.filter-item{

    display:flex;

    flex-direction:column;

}

.filter-item label{

    font-size:12px;

    color:#d85d95;

    font-weight:700;

    letter-spacing:.5px;

    margin-bottom:8px;

}

.search-box{

    display:flex;

    align-items:center;

    border:1.5px solid #f3bfd3;

    border-radius:10px;

    padding:10px 12px;

    transition: border-color .2s ease, box-shadow .2s ease;

}

.search-box:focus-within{

    border-color:#f55d9b;

    box-shadow: 0 0 0 4px rgba(245,93,155,.12);

}

.search-box i{
    color:#e8779f;
}

.search-box input{

    border:none;

    width:100%;

    outline:none;

    margin-left:10px;

    font-size:14px;

    color:#333;

}

.search-box input::placeholder{
    color:#a6adb8;
}

select{

    height:40px;

    border-radius:10px;

    border:1.5px solid #f3bfd3;

    padding:0 12px;

    font-size:14px;

    color:#333;

    cursor:pointer;

    transition: border-color .2s ease, box-shadow .2s ease;

}

select:focus{

    border-color:#f55d9b;

    box-shadow: 0 0 0 4px rgba(245,93,155,.12);

    outline:none;

}

.btn-refresh{

    border:none;

    background: linear-gradient(135deg, #f55d9b, #ef3f89);

    color:white;

    border-radius:10px;

    padding:0 18px;

    height:40px;

    cursor:pointer;

    font-weight:700;

    font-size:14px;

    box-shadow: 0 6px 16px rgba(245,93,155,.28);

    transition: transform .15s ease, box-shadow .15s ease;

    display:flex;

    align-items:center;

    justify-content:center;

    gap:8px;

}

.btn-refresh:hover{

    transform: translateY(-2px);

    box-shadow: 0 10px 22px rgba(245,93,155,.38);

}

.btn-refresh:active{

    transform: translateY(0);

}

.filter-action{

    display:flex;

    align-items:flex-end;

}

.filter-action .btn-refresh{

    width:100%;

    padding:0;

}

/* Table + Pagination — dính liền thành 1 khối */
.table-card{
    background:white;
    border-radius:14px;
    border:1px solid #ffd6e4;
    overflow:hidden;
}
.pagination{

    padding:14px 20px;

    display:flex;

    justify-content:space-between;

    align-items:center;

    border-top:1px solid #ffd6e4;

}

.left{

    font-size:14px;

    color:#5b6270;

}

.left b{

    color:#f55d9b;

    font-weight:800;

}

.right{

    display:flex;

    align-items:center;

    gap:10px;

    font-size:13.5px;

    color:#5b6270;

}

.right select{

    width:62px;

    height:34px;

    border:1px solid #ffd6e4;

    border-radius:8px;

    padding:0 8px;

    font-size:13.5px;

    color:#333;

    cursor:pointer;

    transition: border-color .2s ease;

}

.right select:focus{

    border-color:#f55d9b;

    outline:none;

}

.right button{

    border:1px solid #ffd6e4;

    background:white;

    border-radius:8px;

    padding:7px 16px;

    cursor:pointer;

    font-weight:700;

    font-size:13.5px;

    color:#f55d9b;

    transition: background .2s ease, box-shadow .2s ease;

}

.right button:hover:not(:disabled){

    background:#fff0f6;

    box-shadow: 0 4px 10px rgba(245,93,155,.15);

}

.right button:disabled{

    color:#c3c8d1;

    border-color:#eee;

    cursor:not-allowed;

}

.right span{

    font-weight:600;

    color:#5b6270;

}
</style>