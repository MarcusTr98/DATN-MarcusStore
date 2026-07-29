<template>
  <div class="review-table-wrap">
    <table>
      <thead>
      <tr>
        <th>STT</th>
        <th>Khách hàng</th>
        <th>Sản phẩm</th>
        <th>Sao</th>
        <th>Bình luận</th>
        <th>Ngày</th>
        <th>Trạng thái</th>
        <th>Thao tác</th>
      </tr>
      </thead>
      <tbody>
      <tr
          v-for="(review,index) in reviews"
          :key="review.reviewId"
      >
        <td class="idx">#{{ index+1 }}</td>
        <td>
          <div class="customer">
            <strong>{{ review.fullName }}</strong>
            <small>{{ review.username }}</small>
          </div>
        </td>
        <td class="product">
          {{ review.productName }}
        </td>

        <td>
          <span
              class="star"
              :class="n <= review.rating ? 'star-active' : 'star-empty'"
              v-for="n in 5"
              :key="n"
          >
            {{ n<=review.rating ? "★":"☆" }}
          </span>
        </td>
        <td class="comment" :title="review.commentText">
          {{ review.commentText }}
        </td>
        <td class="date">
          {{ formatDate(review.createdAt) }}
        </td>

        <td>
          <span
              class="badge replied"
              v-if="review.reply"
          >
            <i class="fas fa-check"></i> Đã trả lời
          </span>
          <span
              class="badge waiting"
              v-else
          >
            <i class="fas fa-clock"></i> Chưa trả lời
          </span>
        </td>
        <td>
          <div class="actions">
            <button
                class="btn info"
                @click="showDetail(review)"
                title="Xem chi tiết"
            >
                <i class="fas fa-eye"></i>
            </button>

            <button
                class="btn danger"
                @click="$emit('delete',review)"
                title="Xóa"
            >
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </td>
      </tr>
      <tr v-if="reviews.length==0">
        <td colspan="8" class="empty">
          <i class="far fa-folder-open"></i>
          <span>Không có đánh giá nào</span>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>

const emit = defineEmits([
  "detail",
  "delete"
])

const showDetail = (review) => {
  emit("detail", review)
}

defineProps({
  reviews:{
    type:Array,
    default:()=>[]
  }
})

const formatDate=(date)=>{
  return new Date(date).toLocaleDateString("vi-VN",{
    day:"2-digit",
    month:"2-digit",
    year:"numeric"
  })
}

</script>

<style scoped>

.review-table-wrap{
    background:white;
    font-family: -apple-system, "Segoe UI", Roboto, Inter, sans-serif;
}
table{
    width:100%;
    table-layout:fixed;
    border-collapse:separate;
    border-spacing:0;
}

thead{
    background: linear-gradient(135deg, #fff0f6, #fff7fb);
}

th{
    padding:14px 18px;
    color:#c24f83;
    font-size:12.5px;
    font-weight:700;
    text-transform:uppercase;
    letter-spacing:.5px;
    text-align:left;
    white-space:nowrap;
}

th:nth-child(1){ width:6%; }
th:nth-child(2){ width:15%; }
th:nth-child(3){ width:16%; }
th:nth-child(4){ width:9%; }
th:nth-child(5){ width:24%; }
th:nth-child(6){ width:10%; }
th:nth-child(7){ width:12%; }
th:nth-child(8){ width:8%; }
tbody tr{
    transition: background .2s ease;
}

tbody tr:hover{
    background:#fffbfd;
}

td{
    padding:14px 18px;
    border-bottom:1px solid #f2f2f2;
    font-size:14px;
    color:#333;
}

.idx{
    color:#a6adb8;
    font-weight:700;
}

.customer{
    display:flex;
    flex-direction:column;
}

.customer strong{
    font-weight:700;
    color:#1f2430;
}

.customer small{
    color:#8b93a1;
    font-size:12px;
    margin-top:2px;
}

.product{
    font-weight:600;
    color:#333;
}

.comment{
    max-width:220px;
    white-space:nowrap;
    overflow:hidden;
    text-overflow:ellipsis;
    color:#555;
    font-style:italic;
}

.date{
    color:#5b6270;
    font-size:13px;
}

.star{
    font-size:16px;
}

.star-active{
    color:#ffb400;
    text-shadow: 0 1px 4px rgba(255,180,0,.3);
}

.star-empty{
    color:#E0E0E0;
}

.badge{
    display:inline-flex;
    align-items:center;
    gap:6px;
    padding:5px 12px;
    border-radius:20px;
    font-size:12px;
    font-weight:700;
    letter-spacing:.2px;
    white-space:nowrap;
}

.replied{
    background:#FCE7F3;
    color:#9D174D;
}

.waiting{
    background:#FEF3C7;
    color:#92400E;
}

.actions{
    display:flex;
    gap:8px;
}

.btn{
    width:34px;
    height:34px;
    border:none;
    border-radius:9px;
    cursor:pointer;
    display:flex;
    align-items:center;
    justify-content:center;
    font-size:13px;
    transition: transform .15s ease, box-shadow .15s ease;
}

.btn:hover{
    transform: translateY(-2px);
}

.info{
    background:#eef6ff;
    color:#3b82f6;
}

.info:hover{
    box-shadow: 0 6px 14px rgba(59,130,246,.25);
}

.danger{
    background:#ffe9e9;
    color:#ef4444;
}

.danger:hover{
    box-shadow: 0 6px 14px rgba(239,68,68,.25);
}

.empty{
    text-align:center;
    padding:50px;
    color:#a6adb8;
}

.empty i{
    display:block;
    font-size:30px;
    margin-bottom:10px;
    color:#c3c8d1;
}

.empty span{
    font-size:14px;
}

</style>