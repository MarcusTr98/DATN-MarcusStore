<template>
  <transition name="fade">
    <div
      v-if="modelValue"
      class="modal-overlay"
      @click.self="close"
    >
      <div class="review-modal">

        <!-- Header -->
        <div class="modal-header">
          <h3>
            {{ props.viewOnly ? "Đánh giá của bạn" : props.editMode ? "Sửa đánh giá" : "Đánh giá sản phẩm" }}
          </h3>
          <button class="close-btn" @click="close">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </div>

        <!-- Product -->
        <div class="product-box">
          <img
            :src="orderItem?.thumbnail"
            class="product-image"
          >
          <div class="product-info">
            <h4>{{ orderItem?.productName }}</h4>
          </div>
        </div>

        <!-- Rating -->
        <div
          class="rating-box"
          :class="{ readonly: props.viewOnly }"
        >
          <span
            v-for="star in 5"
            :key="star"
            class="star"
            :class="{ active: star <= hoverStar || star <= rating }"
            @mouseenter="!props.viewOnly && (hoverStar = star)"
            @mouseleave="!props.viewOnly && (hoverStar = 0)"
            @click="!props.viewOnly && (rating = star)"
          >
            ★
          </span>
        </div>

        <p
          class="rating-text"
          v-if="rating"
        >
          {{ ratingText }}
        </p>

        <!-- Comment -->
        <textarea
          v-model="commentText"
          rows="4"
          :readonly="props.viewOnly"
          :class="{ readonly: props.viewOnly }"
          placeholder="Hãy chia sẻ cảm nhận của bạn về sản phẩm..."
        ></textarea>
         <div
    class="image-upload"
    v-if="!props.viewOnly"
>

    <label>Hình ảnh đánh giá</label>

    <input
        type="file"
        multiple
        accept="image/*"
        @change="handleImageChange"
    />

</div>

<div class="preview-list">

    <div
        class="preview-item"
        v-for="(image,index) in previewImages"
        :key="index"
    >

        <img
            :src="image"
            class="preview-image"
        >

        <button
            class="remove-image"
            @click="removeImage(index)"
        >
            ×
        </button>

    </div>

</div>
        <p
          v-if="error"
          class="error"
        >
          {{ error }}
        </p>

        <!-- Xác nhận xóa đánh giá -->
        <div
          v-if="confirmingDelete"
          class="delete-confirm-box"
        >
          <p>Bạn chắc chắn muốn xóa đánh giá này?</p>
          <div class="delete-confirm-actions">
            <button
              class="btn-ghost-sm"
              :disabled="deleting"
              @click="confirmingDelete = false"
            >
              Không
            </button>
            <button
              class="btn-danger-sm"
              :disabled="deleting"
              @click="deleteReview"
            >
              <i
                v-if="deleting"
                class="fa-solid fa-spinner fa-spin"
              ></i>
              {{ deleting ? "Đang xóa..." : "Xóa" }}
            </button>
          </div>
        </div>

        <!-- Footer -->
        <div
          class="footer"
          v-else
        >
          <button
            v-if="props.editMode && !props.viewOnly"
            type="button"
            class="delete-link"
            :disabled="loading"
            @click="confirmingDelete = true"
          >
            <i class="fa-solid fa-trash-can"></i>
            Xóa đánh giá
          </button>
          <div class="footer-right">
            <button
              class="cancel-btn"
              @click="close"
            >
              {{ props.viewOnly ? "Đóng" : "Hủy" }}
            </button>

            <button
              v-if="!props.viewOnly"
              class="submit-btn"
              :disabled="loading"
              @click="submitReview"
            >
              <i
                v-if="loading"
                class="fa-solid fa-spinner fa-spin"
              ></i>
              {{
                loading
                  ? "Đang lưu..."
                  : props.editMode
                    ? "Cập nhật đánh giá"
                    : "Gửi đánh giá"
              }}
            </button>
          </div>
        </div>

      </div>
    </div>
  </transition>
</template>

<script setup>
import reviewService from "@/stores/reviewService"
import cloudinaryService from "@/stores/cloudinaryService"
import { computed, ref, watch } from "vue"

const props = defineProps({

  modelValue:Boolean,

  orderItem:Object,

  editMode:Boolean,

  viewOnly:{ type: Boolean, default: false }   // true = chỉ xem, không cho sửa/gửi

})

const emit = defineEmits([

  "update:modelValue",

  "success"

])

const rating = ref(0)

const hoverStar = ref(0)

const commentText = ref("")

const loading = ref(false)

const error = ref("")

const confirmingDelete = ref(false)   // đang hỏi xác nhận xóa

const deleting = ref(false)           // đang gọi API xóa
const existingImages = ref([])     // ảnh đã có trên Cloudinary

const selectedImages = ref([])     // File mới

const previewImages = ref([])

const uploading = ref(false)
// Đổ dữ liệu đánh giá đã có (dùng chung cho cả chế độ Xem và Sửa)
watch(
    () => props.orderItem,
    (item) => {

        if (!item) return

        if (item.review) {

            rating.value = item.review.rating

            commentText.value = item.review.commentText

            existingImages.value = [...(item.review.images || [])]

            selectedImages.value = []

            previewImages.value = [...existingImages.value]

        } else {

            rating.value = 0

            commentText.value = ""

            existingImages.value = []

            selectedImages.value = []

            previewImages.value = []

        }

        confirmingDelete.value = false

    },
    { immediate: true }
)

const ratingText = computed(()=>{

  switch(rating.value){

    case 1:return "Rất tệ"

    case 2:return "Tệ"

    case 3:return "Bình thường"

    case 4:return "Tốt"

    case 5:return "Rất tốt"

    default:return ""

  }

})
const handleImageChange = (event) => {

    const files = [...event.target.files]

    files.forEach(file => {

        selectedImages.value.push(file)

        previewImages.value.push(URL.createObjectURL(file))

    })

}
const removeImage = (index) => {

    // Nếu là ảnh cũ

    if (index < existingImages.value.length) {

        existingImages.value.splice(index,1)

        previewImages.value.splice(index,1)

        return

    }

    // Nếu là ảnh mới

    const newIndex = index - existingImages.value.length

    selectedImages.value.splice(newIndex,1)

    previewImages.value.splice(index,1)

}

function close(){

  emit("update:modelValue",false)

  rating.value=0

  hoverStar.value=0

  commentText.value=""

  error.value=""

  confirmingDelete.value=false

}

async function submitReview(){

    // Chặn an toàn: chế độ chỉ xem thì không được submit
    if(props.viewOnly) return

    error.value=""

    if(rating.value===0){

        error.value="Vui lòng chọn số sao."

        return

    }

    loading.value=true
    uploading.value = true

const uploadedUrls = []

uploading.value = true

for (const file of selectedImages.value) {

    const url = await cloudinaryService.upload(file)

    uploadedUrls.push(url)

}

uploading.value = false

const imageUrls = [

    ...existingImages.value,

    ...uploadedUrls

]

uploading.value = false
    try{

        if(props.editMode){
console.log("existingImages =", existingImages.value)

console.log("selectedImages =", selectedImages.value)

console.log("imageUrls =", imageUrls)
await reviewService.update(

    props.orderItem.review.reviewId,

    {

        rating: rating.value,

        commentText: commentText.value,

        imageUrls: imageUrls

    }

)

        }else{

await reviewService.create(

    props.orderItem.orderItemId,

    {

        rating: rating.value,

        commentText: commentText.value,

        imageUrls: imageUrls

    }

)

        }

        emit("success")

        close()

    }

    catch(e){

        error.value=e.response?.data?.message || "Có lỗi xảy ra."

    }

    finally{

        loading.value=false

    }

}

// Xóa đánh giá hiện tại (chỉ dùng khi đang ở chế độ Sửa)
// LƯU Ý: đổi tên hàm reviewService.remove(...) thành đúng tên hàm xóa
// thực tế trong file @/stores/reviewService.js của bạn nếu khác (vd: .delete(), .deleteReview()...)
async function deleteReview(){

    if(!props.orderItem?.review?.reviewId) return

    deleting.value = true

    error.value = ""

    try{

        await reviewService.remove(props.orderItem.review.reviewId)

        emit("success")

        close()

        existingImages.value = []

selectedImages.value = []

previewImages.value = []

    }

    catch(e){

        error.value = e.response?.data?.message || "Xóa đánh giá thất bại."

        confirmingDelete.value = false

    }

    finally{

        deleting.value = false

    }

}

</script>

<style scoped>

.modal-overlay{

position:fixed;

inset:0;

background:rgba(0,0,0,.5);

display:flex;

justify-content:center;

align-items:center;

z-index:9999;

padding:16px;

}

.review-modal{

width:100%;

max-width:400px;

background:#fff;

border-radius:16px;

padding:20px 22px 22px;

animation:show .22s ease;

box-shadow:0 20px 50px rgba(0,0,0,.25);

}

@keyframes show{

from{

transform:translateY(16px);

opacity:0;

}

to{

transform:translateY(0);

opacity:1;

}

}

.modal-header{

display:flex;

justify-content:space-between;

align-items:center;

margin-bottom:14px;

padding-bottom:12px;

border-bottom:1px solid #f0f0f0;

}

.modal-header h3{

font-size:17px;

font-weight:800;

color:#1f1f1f;

margin:0;

}

.close-btn{

background:#f5f5f5;

border:none;

width:28px;

height:28px;

border-radius:50%;

font-size:13px;

color:#666;

cursor:pointer;

display:flex;

align-items:center;

justify-content:center;

transition:.15s;

}

.close-btn:hover{

background:#ffe3e3;

color:#e60012;

}

.product-box{

display:flex;

gap:12px;

align-items:center;

margin-bottom:16px;

}

.product-image{

width:56px;

height:56px;

object-fit:cover;

border-radius:8px;

border:1px solid #eee;

flex-shrink:0;

}

.product-info h4{

font-size:14px;

font-weight:700;

color:#1f1f1f;

margin:0;

}

.rating-box{

display:flex;

justify-content:center;

gap:4px;

margin:14px 0 4px;

}

.rating-box.readonly .star{

cursor:default;

}

.star{

font-size:30px;

line-height:1;

cursor:pointer;

color:#e2e2e2;

transition:.15s;

}

.star.active{

color:#e60012;

transform:scale(1.08);

}

.rating-text{

text-align:center;

font-weight:700;

font-size:13px;

margin:0 0 12px;

color:#e60012;

}

textarea{

width:100%;

resize:none;

padding:10px 12px;

border-radius:10px;

border:1px solid #e5e5e5;

font-size:13px;

font-family:inherit;

outline:none;

box-sizing:border-box;

transition:.15s;

}

textarea:focus{

border-color:#e60012;

box-shadow:0 0 0 3px rgba(230,0,18,.08);

}

textarea.readonly{

background:#fafafa;

color:#444;

cursor:default;

}

textarea.readonly:focus{

border-color:#e5e5e5;

box-shadow:none;

}

.footer{

display:flex;

justify-content:space-between;

align-items:center;

margin-top:18px;

gap:10px;

}

.footer-right{

display:flex;

gap:8px;

margin-left:auto;

}

.delete-link{

display:inline-flex;

align-items:center;

gap:6px;

background:none;

border:none;

color:#e60012;

font-size:12.5px;

font-weight:600;

cursor:pointer;

padding:6px 4px;

}

.delete-link:hover{

text-decoration:underline;

}

.delete-link:disabled{

opacity:.5;

cursor:not-allowed;

}

.cancel-btn{

padding:9px 16px;

border:1px solid #e5e5e5;

background:#fff;

color:#444;

cursor:pointer;

border-radius:8px;

font-size:13px;

font-weight:600;

transition:.15s;

}

.cancel-btn:hover{

background:#f7f7f7;

}

.submit-btn{

padding:9px 18px;

background:#e60012;

color:#fff;

border:none;

border-radius:8px;

cursor:pointer;

font-size:13px;

font-weight:700;

display:inline-flex;

align-items:center;

gap:6px;

transition:.15s;

}

.submit-btn:hover{

background:#c40010;

}

.submit-btn:disabled{

opacity:.6;

cursor:not-allowed;

}

.error{

color:#e60012;

margin-top:8px;

font-size:12.5px;

}

/* Khối xác nhận xóa đánh giá */
.delete-confirm-box{

margin-top:16px;

padding:12px 14px;

background:#fff5f5;

border:1px solid #ffd4d4;

border-radius:10px;

text-align:center;

}

.delete-confirm-box p{

margin:0 0 10px;

font-size:13px;

font-weight:600;

color:#7a0009;

}

.delete-confirm-actions{

display:flex;

justify-content:center;

gap:8px;

}

.btn-ghost-sm{

padding:7px 14px;

border:1px solid #e5e5e5;

background:#fff;

color:#444;

cursor:pointer;

border-radius:8px;

font-size:12.5px;

font-weight:600;

}

.btn-ghost-sm:hover{

background:#f7f7f7;

}

.btn-danger-sm{

padding:7px 14px;

border:none;

background:#e60012;

color:#fff;

cursor:pointer;

border-radius:8px;

font-size:12.5px;

font-weight:700;

display:inline-flex;

align-items:center;

gap:6px;

}

.btn-danger-sm:hover{

background:#c40010;

}

.btn-ghost-sm:disabled,
.btn-danger-sm:disabled{

opacity:.6;

cursor:not-allowed;

}

.fade-enter-active,

.fade-leave-active{

transition:.2s;

}

.fade-enter-from,

.fade-leave-to{

opacity:0;

}



.image-upload{

margin-top:16px;

}

.image-upload label{

display:block;

margin-bottom:8px;

font-weight:600;

font-size:13px;

color:#555;

}

.image-upload input{

width:100%;

padding:10px;

border:1px solid #ddd;

border-radius:8px;

background:white;

}

.preview-list{

display:flex;

flex-wrap:wrap;

gap:10px;

margin-top:14px;

}

.preview-image{

width:90px;

height:90px;

border-radius:10px;

object-fit:cover;

border:1px solid #ddd;

}
.preview-item{
    position: relative;
}

.remove-image{
    position: absolute;
    top: -6px;
    right: -6px;

    width: 22px;
    height: 22px;

    border: none;
    border-radius: 50%;

    background: #e60012;
    color: white;

    cursor: pointer;
    font-size: 14px;
    font-weight: bold;

    display: flex;
    justify-content: center;
    align-items: center;
}

.remove-image:hover{
    background: #b4000e;
}
</style>