<template>
  <transition name="fade">
    <div
      v-if="modelValue"
      class="modal-overlay"
      @click.self="close"
    >
      <div class="review-modal">
      <div class="modal-scroll">

        <!-- ================= HEADER ================= -->

        <div class="modal-header">

          <div class="header-left">

            <div class="header-icon">
              <i class="fa-solid fa-star"></i>
            </div>

            <div>

              <h3>
                {{
                  props.viewOnly
                    ? "Đánh giá của bạn"
                    : props.editMode
                      ? "Chỉnh sửa đánh giá"
                      : "Đánh giá sản phẩm"
                }}
              </h3>

              <p>

                {{
                  props.viewOnly
                    ? "Xem đánh giá đã gửi"
                    : "Chia sẻ trải nghiệm của bạn"

                }}

              </p>

            </div>

          </div>

          <button
            class="close-btn"
            @click="close"
          >
            <i class="fa-solid fa-xmark"></i>
          </button>

        </div>

        <!-- ================= PRODUCT ================= -->

        <div
          class="product-box"
          role="button"
          tabindex="0"
          @click="goToProductDetail"
          @keydown.enter="goToProductDetail"
        >

          <img
            :src="orderItem?.thumbnail"
            class="product-image"
          >

          <div class="product-info">

            <h4>{{ orderItem?.productName }}</h4>

            <small>

              Cảm nhận của bạn sẽ giúp khách hàng khác lựa chọn tốt hơn.

            </small>

            <span class="view-detail-link">
              Xem chi tiết sản phẩm
              <i class="fa-solid fa-chevron-right"></i>
            </span>

          </div>

        </div>

        <!-- ================= STAR ================= -->

        <div
          class="rating-section"
          :class="{ readonly: props.viewOnly }"
        >

          <div class="rating-box">

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

          <div
            v-if="rating"
            class="rating-text"
          >

            {{ ratingText }}

          </div>

        </div>

        <!-- ================= COMMENT ================= -->

        <div class="comment-box">

          <label>

            <i class="fa-regular fa-comment"></i>

            Nội dung đánh giá

          </label>

          <textarea

            v-model="commentText"

            rows="5"

            :readonly="props.viewOnly"

            :class="{ readonly: props.viewOnly }"

            placeholder="Hãy chia sẻ trải nghiệm của bạn về sản phẩm..."

          ></textarea>

        </div>

        <!-- ================= UPLOAD ================= -->

        <div
          v-if="!props.viewOnly"
          class="image-upload"
        >

          <label>

            <i class="fa-regular fa-image"></i>

            Hình ảnh đánh giá

            <span>

              ({{ previewImages.length }}/5)

            </span>

          </label>

          <input

            ref="fileInput"

            type="file"

            accept="image/*"

            multiple

            @change="handleImageChange"

            :disabled="loading || uploading"

          >

          <div class="upload-note">

            <div>

              ✓ Tối đa 5 ảnh

            </div>

            <div>

              ✓ Hỗ trợ JPG, PNG, WEBP
            </div>
            <div>

              ✓ Dung lượng tối đa 5MB/ảnh
          </div>

        </div>
</div>
        <!-- ================= LOADING ================= -->

        <div
          v-if="uploading"
          class="upload-loading"
        >

          <i class="fa-solid fa-spinner fa-spin"></i>

          Đang tải ảnh...

        </div>

        <!-- ================= PREVIEW ================= -->

        <div class="preview-list">

          <template v-if="previewImages.length">

            <div

              class="preview-item"

              v-for="(image,index) in previewImages"

              :key="index"

            >

              <img

                :src="image"

                class="preview-image"

                @click="previewImage(image)"

              >

              <button

                v-if="!props.viewOnly"

                class="remove-image"

                @click="removeImage(index)"

              >

                <i class="fa-solid fa-xmark"></i>

              </button>

            </div>

          </template>

          <div

            v-else

            class="empty-image"

          >

            <i class="fa-regular fa-images"></i>

            <p>

              Chưa có hình ảnh nào

            </p>

          </div>

        </div>

        <!-- ================= ERROR ================= -->

        <transition name="fade">

          <p
            v-if="error"
            class="error"
          >

            <i class="fa-solid fa-circle-exclamation"></i>

            {{ error }}

          </p>

        </transition>

        <!-- ================= DELETE ================= -->

        <div
          v-if="confirmingDelete"
          class="delete-confirm-box"
        >

          <i class="fa-solid fa-triangle-exclamation"></i>

          <p>

            Bạn chắc chắn muốn xóa đánh giá này?

          </p>

          <div class="delete-confirm-actions">

            <button

              class="btn-ghost-sm"

              @click="confirmingDelete=false"

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

              {{ deleting ? "Đang xóa..." : "Xóa đánh giá" }}

            </button>

          </div>

        </div>

        <!-- ================= FOOTER ================= -->

        <div
          v-else
          class="footer"
        >

          <button

            v-if="props.editMode && !props.viewOnly"

            class="delete-link"

            @click="confirmingDelete=true"

            :disabled="loading"

          >

            <i class="fa-solid fa-trash"></i>

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

              @click="submitReview"

              :disabled="loading || uploading"

            >

              <i

                v-if="loading"

                class="fa-solid fa-spinner fa-spin"

              ></i>

              {{

                loading

                  ? "Đang lưu..."

                  : props.editMode

                    ? "Cập nhật"

                    : "Gửi đánh giá"

              }}

            </button>

          </div>

        </div>

      </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import reviewService from "@/stores/reviewService"
import cloudinaryService from "@/stores/cloudinaryService"

import {
    computed,
    ref,
    watch,
    onBeforeUnmount
} from "vue"
import { useRouter } from "vue-router"

const props = defineProps({

    modelValue:Boolean,

    orderItem:Object,

    editMode:Boolean,

    viewOnly:{
        type:Boolean,
        default:false
    }

})

const emit = defineEmits([

    "update:modelValue",

    "success"

])

const router = useRouter()

/* ===========================
        CONSTANT
=========================== */

const MAX_IMAGES = 5

const MAX_FILE_SIZE = 5 * 1024 * 1024

const ACCEPT_TYPES = [

    "image/jpeg",

    "image/png",

    "image/webp"

]

/* ===========================
        STATE
=========================== */

const rating = ref(0)

const hoverStar = ref(0)

const commentText = ref("")

const loading = ref(false)

const uploading = ref(false)

const deleting = ref(false)

const error = ref("")

const confirmingDelete = ref(false)

const existingImages = ref([])

const selectedImages = ref([])

const previewImages = ref([])

const fileInput = ref(null)

/* ===========================
        WATCH
=========================== */

watch(

    () => props.orderItem,

    (item)=>{

        clearPreview()

        if(item?.review){

            rating.value = item.review.rating

            commentText.value = item.review.commentText

            existingImages.value = [...(item.review.images || [])]

            previewImages.value = [...existingImages.value]

        }

        else{

            rating.value = 0

            commentText.value = ""

            existingImages.value = []

            previewImages.value = []

        }

        selectedImages.value=[]

        confirmingDelete.value=false

        error.value=""

    },

    {immediate:true}

)

/* ===========================
        COMPUTED
=========================== */

const ratingText = computed(()=>{

    switch(rating.value){

        case 1:return "Rất tệ 😠"

        case 2:return "Chưa hài lòng 😕"

        case 3:return "Bình thường 🙂"

        case 4:return "Tốt 😊"

        case 5:return "Tuyệt vời 🤩"

        default:return ""

    }

})

/* ===========================
        NAVIGATE TO PRODUCT DETAIL
=========================== */

// LƯU Ý: item ở OrderDetailView hiện chưa có productId, chỉ có skuId/skuCode.
// Đổi query bên dưới cho khớp với route thật của bạn khi đã bổ sung field.
function goToProductDetail() {

    const slug = props.orderItem?.productSlug

    if (!slug) return

    close()

    router.push({
        name: "ProductDetail",
        params: {
            slug
        }
    })

}

/* ===========================
        IMAGE
=========================== */

function handleImageChange(event){

    const files=[...event.target.files]

    if(!files.length){

        return

    }

    for(const file of files){

        if(existingImages.value.length+

            selectedImages.value.length>=MAX_IMAGES){

            error.value=`Chỉ được tải tối đa ${MAX_IMAGES} ảnh.`

            break

        }

        if(!ACCEPT_TYPES.includes(file.type)){

            error.value="Chỉ hỗ trợ JPG, PNG, WEBP."

            continue

        }

        if(file.size>MAX_FILE_SIZE){

            error.value=`${file.name} vượt quá 5MB.`

            continue

        }

        const duplicated=

            selectedImages.value.some(

                f=>

                    f.name===file.name &&

                    f.size===file.size

            )

        if(duplicated){

            continue

        }

        selectedImages.value.push(file)

        previewImages.value.push(

            URL.createObjectURL(file)

        )

    }

    event.target.value=""

}

function removeImage(index){

    if(props.viewOnly){

        return

    }

    if(index<existingImages.value.length){

        existingImages.value.splice(index,1)

        previewImages.value.splice(index,1)

        return

    }

    const newIndex=

        index-existingImages.value.length

    URL.revokeObjectURL(

        previewImages.value[index]

    )

    selectedImages.value.splice(newIndex,1)

    previewImages.value.splice(index,1)

}

function clearPreview(){

    previewImages.value.forEach(url=>{

        if(url.startsWith("blob:")){

            URL.revokeObjectURL(url)

        }

    })

}

function previewImage(url){

    window.open(url,"_blank")

}

/* ===========================
        CLOSE
=========================== */

function close(){

    clearPreview()

    emit("update:modelValue",false)

    rating.value=0

    hoverStar.value=0

    commentText.value=""

    existingImages.value=[]

    selectedImages.value=[]

    previewImages.value=[]

    confirmingDelete.value=false

    error.value=""

}

/* ===========================
        DESTROY
=========================== */

onBeforeUnmount(()=>{

    clearPreview()

})
/* ===========================
        SUBMIT REVIEW
=========================== */

async function submitReview(){

    if(props.viewOnly){

        return

    }

    error.value=""

    if(rating.value===0){

        error.value="Vui lòng chọn số sao."

        return

    }

    loading.value=true

    uploading.value=true

    try{

        /* Upload ảnh mới song song */

        const uploadedUrls = await Promise.all(

            selectedImages.value.map(file=>

                cloudinaryService.upload(file)

            )

        )

        /* Ghép ảnh cũ + ảnh mới */

        const imageUrls=[

            ...existingImages.value,

            ...uploadedUrls

        ]

        const payload={

            rating:rating.value,

            commentText:commentText.value.trim(),

            imageUrls:imageUrls

        }

        if(props.editMode){

            await reviewService.update(

                props.orderItem.review.reviewId,

                payload

            )

        }

        else{

            await reviewService.create(

                props.orderItem.orderItemId,

                payload

            )

        }

        emit("success")

        close()

    }

    catch(e){

        console.error(e)

        error.value=

            e.response?.data?.message ||

            "Không thể gửi đánh giá."

    }

    finally{

        loading.value=false

        uploading.value=false

    }

}

/* ===========================
        DELETE REVIEW
=========================== */

async function deleteReview(){

    if(!props.orderItem?.review?.reviewId){

        return

    }

    deleting.value=true

    error.value=""

    try{

        await reviewService.remove(

            props.orderItem.review.reviewId

        )

        emit("success")

        close()

    }

    catch(e){

        console.error(e)

        error.value=

            e.response?.data?.message ||

            "Xóa đánh giá thất bại."

        confirmingDelete.value=false

    }

    finally{

        deleting.value=false

    }

}
</script>
<style scoped>

/* ===========================
        Overlay
=========================== */

.modal-overlay{
    position:fixed;
    inset:0;
    background:rgba(15,23,42,.55);
    backdrop-filter:blur(6px);
    display:flex;
    justify-content:center;
    align-items:center;
    z-index:9999;
    padding:24px;
}

.fade-enter-active,
.fade-leave-active{
    transition:.25s;
}

.fade-enter-from,
.fade-leave-to{
    opacity:0;
}

.fade-enter-from .review-modal{
    transform:translateY(30px) scale(.96);
}

.fade-leave-to .review-modal{
    transform:translateY(20px);
}

/* ===========================
        Modal
=========================== */

.review-modal{

    width:100%;
    max-width:620px;
    max-height:90vh;

    /* overflow:hidden ở đây (thay vì auto) để 4 góc luôn bo đều,
       phần cuộn thật sự nằm ở .modal-scroll bên trong */
    overflow:hidden;

    background:white;

    border-radius:22px;

    box-shadow:
        0 30px 80px rgba(0,0,0,.18);

    animation:popup .25s;
}

.modal-scroll{

    max-height:90vh;

    overflow-y:auto;

}

.modal-scroll::-webkit-scrollbar{

    width:6px;

}

.modal-scroll::-webkit-scrollbar-track{

    background:transparent;

}

.modal-scroll::-webkit-scrollbar-thumb{

    background:#f1a9c2;

    border-radius:10px;

}

.modal-scroll::-webkit-scrollbar-thumb:hover{

    background:#e60012;

}

@keyframes popup{

    from{

        opacity:0;
        transform:translateY(25px) scale(.95);

    }

    to{

        opacity:1;
        transform:none;

    }

}

/* ===========================
        Header
=========================== */

.modal-header{

    display:flex;
    justify-content:space-between;
    align-items:center;

    padding:22px 26px;

    border-bottom:1px solid #f1f1f1;

    background:linear-gradient(135deg,#fff,#fff5f5);

}

.modal-header h3{

    font-size:22px;
    font-weight:800;

    color:#e60012;

}

.close-btn{

    width:40px;
    height:40px;

    border:none;
    border-radius:50%;

    background:#fff0f5;

    cursor:pointer;

    transition:.25s;
}

.close-btn:hover{

    transform:rotate(90deg);

    background:#e60012;

    color:white;

}

/* ===========================
        Product
=========================== */

.product-box{

    display:flex;

    gap:18px;

    padding:16px 26px;

    margin:0 26px 0;

    align-items:center;

    border-radius:18px;

    cursor:pointer;

    transition:.25s;

    border:1px solid transparent;

}

.product-box:hover{

    background:#fff5f5;

    border-color:#ffd4d4;

    transform:translateY(-2px);

    box-shadow:0 10px 24px rgba(230,0,18,.12);

}

.product-image{

    width:85px;
    height:85px;

    border-radius:16px;

    object-fit:cover;

    border:1px solid #eee;

    flex-shrink:0;

}

.product-info{

    flex:1;

    min-width:0;

}

.product-info h4{

    font-size:18px;

    font-weight:700;

    line-height:1.5;

    color:#000;

    margin:0;

}

.product-info small{

    color:#000;

    opacity:.6;

}

.view-detail-link{

    display:inline-flex;

    align-items:center;

    gap:6px;

    margin-top:6px;

    font-size:13px;

    font-weight:700;

    color:#000;

    transition:.2s;

}

.view-detail-link i{

    font-size:11px;

}

.product-box:hover .view-detail-link{

    color:#e60012;

    gap:8px;

}

/* ===========================
        Rating
=========================== */

.rating-box{

    display:flex;

    justify-content:center;

    gap:10px;

    margin-top:5px;

}

.star{

    font-size:42px;

    color:#ddd;

    cursor:pointer;

    transition:.25s;

}

.star:hover{

    transform:scale(1.25) rotate(-10deg);

}

.star.active{

    color:#FFC107;

    text-shadow:

        0 0 12px rgba(255,193,7,.45);

}

.rating-text{

    text-align:center;

    font-size:15px;

    color:#e60012;

    font-weight:700;

    margin:12px 0 22px;

}

/* ===========================
        Textarea
=========================== */

.comment-box{

    padding:20px 26px 0;

}

.comment-box label{

    display:flex;

    align-items:center;

    gap:8px;

    font-weight:700;

    color:#555;

    margin-bottom:10px;

}

textarea{

    width:100%;

    min-height:130px;

    resize:none;

    border:2px solid #ececec;

    border-radius:16px;

    padding:18px;

    font-size:15px;

    transition:.25s;

    font-family:inherit;

}

textarea:focus{

    outline:none;

    border-color:#e60012;

    box-shadow:

        0 0 0 5px rgba(230,0,18,.12);

}

textarea.readonly{

    background:#fafafa;

    cursor:default;

}

/* ===========================
        Upload
=========================== */

.image-upload{

    margin:22px 26px 10px;

}

.image-upload label{

    display:flex;

    justify-content:space-between;

    align-items:center;

    font-weight:700;

    margin-bottom:12px;

    color:#555;

}

.image-upload input{

    width:100%;

    padding:12px;

    border-radius:12px;

    border:2px dashed #e60012;

    background:#fff5f5;

    cursor:pointer;

}

/* ===========================
        Preview
=========================== */

.preview-list{

    display:flex;

    flex-wrap:wrap;

    gap:14px;

    margin:16px 26px;

}

.preview-item{

    position:relative;

}

.preview-image{

    width:110px;

    height:110px;

    object-fit:cover;

    border-radius:16px;

    transition:.25s;

    border:2px solid #eee;

}

.preview-image:hover{

    transform:scale(1.06);

    box-shadow:

        0 12px 28px rgba(0,0,0,.18);

}

.remove-image{

    position:absolute;

    top:-8px;

    right:-8px;

    width:28px;

    height:28px;

    border:none;

    border-radius:50%;

    background:#ff4d4f;

    color:white;

    cursor:pointer;

    font-size:15px;

    transition:.25s;

    display:flex;
    justify-content:center;
    align-items:center;

}

.remove-image:hover{

    transform:scale(1.15);

    background:#e53935;

}

/* ===========================
        Error
=========================== */

.error{

    margin:0 26px;

    color:#ff4d4f;

    font-size:14px;

    font-weight:600;

}

/* ===========================
        Delete confirm
=========================== */

.delete-confirm-box{

    margin:22px 26px;

    padding:18px;

    border-radius:16px;

    background:#fff5f5;

    border:1px solid #ffd5d5;

    text-align:center;

}

.delete-confirm-box p{

    font-weight:700;

    color:#d63031;

    margin-bottom:15px;

}

.delete-confirm-actions{

    display:flex;

    justify-content:center;

    gap:12px;

}

.btn-ghost-sm,
.btn-danger-sm{

    padding:11px 22px;

    border-radius:10px;

    cursor:pointer;

    font-weight:700;

}

.btn-ghost-sm{

    background:white;

    border:1px solid #ddd;

}

.btn-danger-sm{

    background:#ff4d4f;

    color:white;

    border:none;

}

/* ===========================
        Footer
=========================== */

.footer{

    display:flex;

    justify-content:space-between;

    align-items:center;

    padding:22px 26px;

    border-top:1px solid #f2f2f2;

}

.footer-right{

    display:flex;

    gap:12px;

}

.delete-link{

    background:none;

    border:none;

    color:#ff4d4f;

    font-weight:700;

    cursor:pointer;

}

.cancel-btn{

    padding:12px 24px;

    border-radius:12px;

    border:1px solid #ddd;

    background:white;

    cursor:pointer;

    transition:.25s;

}

.cancel-btn:hover{

    background:#f6f6f6;

}

.submit-btn{

    padding:12px 28px;

    border:none;

    border-radius:12px;

    background:linear-gradient(135deg,#e60012,#ff4d4f);

    color:white;

    font-weight:700;

    cursor:pointer;

    transition:.25s;

}

.submit-btn:hover{

    transform:translateY(-2px);

    box-shadow:

        0 12px 28px rgba(230,0,18,.35);

}

.submit-btn:disabled{

    opacity:.65;

    cursor:not-allowed;

}

/* ===========================
        Responsive
=========================== */

@media(max-width:640px){

.review-modal{

    max-width:100%;

    border-radius:18px;

}

.product-box{

    flex-direction:column;

    text-align:center;

}

.product-image{

    width:100px;
    height:100px;

}

.preview-image{

    width:90px;
    height:90px;

}

.star{

    font-size:36px;

}

.comment-box{

    padding:16px 18px 0;

}

.preview-list{

    margin:16px 18px;

}

.image-upload{

    margin:20px 18px;

}

.footer{

    flex-direction:column;

    gap:14px;

}

.footer-right{

    width:100%;

}

.cancel-btn,
.submit-btn{

    flex:1;

}

}

</style>