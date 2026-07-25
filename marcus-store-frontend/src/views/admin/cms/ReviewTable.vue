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
  console.log("emit detail", review)
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
    border-collapse:separate;
    border-spacing:0;
}

thead{
    background: linear-gradient(135deg, #fff0f6, #fff7fb);
}

th{
    padding:18px;
    color:#c24f83;
    font-size:13px;
    font-weight:700;
    text-transform:uppercase;
    letter-spacing:.6px;
    text-align:left;
}
tbody tr{
    transition: background .2s ease;
}

tbody tr:hover{
    background:#fffbfd;
}

td{
    padding:18px;
    border-bottom:1px solid #f2f2f2;
    font-size:14.5px;
    color:#333;
}

.idx{
    color:#bbb;
    font-weight:700;
}

.customer{
    display:flex;
    flex-direction:column;
}

.customer strong{
    font-weight:700;
}

.customer small{
    color:#999;
    font-size:12.5px;
    margin-top:2px;
}

.product{
    font-weight:600;
    color:#444;
}

.comment{
    max-width:260px;
    white-space:nowrap;
    overflow:hidden;
    text-overflow:ellipsis;
    color:#666;
    font-style:italic;
}

.date{
    color:#888;
    font-size:13.5px;
}

.star{
    color:#ffb400;
    font-size:18px;
    text-shadow: 0 1px 4px rgba(255,180,0,.3);
}

.badge{
    display:inline-flex;
    align-items:center;
    gap:6px;
    padding:6px 14px;
    border-radius:20px;
    font-size:12.5px;
    font-weight:700;
    letter-spacing:.2px;
}

.replied{
    background:#ffe3ef;
    color:#f55d9b;
}

.waiting{
    background:#fff4d8;
    color:#d08a00;
}

.actions{
    display:flex;
    gap:8px;
}

.btn{
    width:38px;
    height:38px;
    border:none;
    border-radius:10px;
    cursor:pointer;
    display:flex;
    align-items:center;
    justify-content:center;
    font-size:14px;
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
    padding:60px;
    color:#bbb;
}

.empty i{
    display:block;
    font-size:34px;
    margin-bottom:10px;
}

.empty span{
    font-size:14.5px;
}

</style>