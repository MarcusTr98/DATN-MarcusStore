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

        <!-- ================= BODY: 2 COLUMN GRID ================= -->

        <div class="modal-body-grid">

          <!-- ---------- LEFT: PRODUCT + RATING ---------- -->
          <div class="modal-left">

            <div
              class="product-box"
              role="button"
              tabindex="0"
              @click="goToProductDetail"
              @keydown.enter="goToProductDetail"
            >

              <img
                v-if="orderItem?.productImage"
                :src="orderItem.productImage"
                :alt="orderItem.productName"
                class="product-image"
              />

              <div v-else class="no-image">
                <i class="fa-solid fa-image"></i>
              </div>

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

          </div>

          <!-- ---------- RIGHT: COMMENT + UPLOAD ---------- -->
          <div class="modal-right">

            <div class="comment-box">

              <label>

                <i class="fa-regular fa-comment"></i>

                Nội dung đánh giá

              </label>

              <!-- ============ QUICK COMMENT CHIPS (đổi theo số sao) ============ -->
              <div
                v-if="!props.viewOnly"
                class="quick-comments"
              >

                <button
                  v-for="(text, idx) in quickComments"
                  :key="idx"
                  type="button"
                  class="chip"
                  :class="{ 'chip-active': selectedQuickComment === text }"
                  @click="applyQuickComment(text)"
                >
                  {{ text }}
                </button>

              </div>

              <textarea

                v-model="commentText"

                rows="5"

                :readonly="props.viewOnly"

                :class="{ readonly: props.viewOnly }"

                placeholder="Hãy chia sẻ trải nghiệm của bạn về sản phẩm..."

                @input="onCommentManualInput"

              ></textarea>

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

              <div class="upload-control">

                <button
                  type="button"
                  class="upload-choose-btn"
                  :disabled="loading || uploading"
                  @click="fileInput?.click()"
                >
                  <i class="fa-solid fa-image"></i>
                  Chọn ảnh
                </button>

                <span
                  v-if="selectedImages.length"
                  class="upload-status-text"
                >

                  Đã chọn {{ selectedImages.length }} ảnh mới

                </span>

                <input

                  ref="fileInput"

                  type="file"

                  accept="image/*"

                  multiple

                  class="upload-native-input"

                  @change="handleImageChange"

                  :disabled="loading || uploading"

                >

              </div>

              <div class="upload-note">

                Tối đa 5 ảnh · JPG, PNG, WEBP · ≤ 5MB/ảnh

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

                v-else-if="!props.viewOnly"

                class="empty-image"

              >

                <i class="fa-regular fa-images"></i>

                <p>

                  Chưa có hình ảnh nào

                </p>

              </div>

            </div>

          </div>

        </div>

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

// Gợi ý bình luận nhanh cho khách - đổi theo số sao để khớp cảm xúc thật của khách
const quickCommentsByRating = {

    1: [

        "Sản phẩm không như mô tả",

        "Chất lượng kém, rất thất vọng",

        "Giao hàng chậm, đóng gói sơ sài"

    ],

    2: [

        "Chưa hài lòng về chất lượng sản phẩm",

        "Sản phẩm chưa như kỳ vọng",

        "Cần cải thiện thêm về đóng gói/giao hàng"

    ],

    3: [

        "Sản phẩm tạm ổn, chưa có gì nổi bật",

        "Chất lượng ở mức trung bình",

        "Đóng gói bình thường, giao hàng đúng hẹn"

    ],

    4: [

        "Chất lượng tốt, đúng như mô tả sản phẩm",

        "Giao hàng nhanh, nhân viên tư vấn nhiệt tình",

        "Hài lòng với sản phẩm, sẽ cân nhắc mua lại"

    ],

    5: [

        "Hàng chính hãng, giao nhanh đảm bảo",

        "Hàng chuẩn, đóng gói cẩn thận, rất hài lòng",

        "Sẽ tiếp tục ủng hộ shop dài dài"

    ]

}

/* ===========================
        STATE
=========================== */

const rating = ref(0)

const hoverStar = ref(0)

const commentText = ref("")

// Mẫu đánh giá đang được áp dụng (để biết cần thay thế hay nối thêm)
const selectedQuickComment = ref("")

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

        selectedQuickComment.value=""

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

// Gợi ý hiển thị theo số sao đang chọn; chưa chọn sao thì tạm dùng bộ trung tính (3 sao)
const quickComments = computed(()=>{

    return quickCommentsByRating[rating.value] || quickCommentsByRating[3]

})

/* ===========================
        QUICK COMMENT
=========================== */

// Nếu khách tự gõ tay thì bỏ trạng thái "đang dùng mẫu",
// để lần bấm mẫu tiếp theo sẽ nối vào thay vì thay thế nhầm.
function onCommentManualInput(){

    if(

        selectedQuickComment.value &&

        commentText.value.trim() !== selectedQuickComment.value

    ){

        selectedQuickComment.value = ""

    }

}

function applyQuickComment(text){

    if(props.viewOnly){

        return

    }

    const current = commentText.value.trim()

    // Nếu nội dung hiện tại chính là mẫu vừa chọn trước đó
    // (khách chưa gõ thêm gì khác) -> THAY THẾ bằng mẫu mới.
    if(

        selectedQuickComment.value &&

        current === selectedQuickComment.value

    ){

        commentText.value = text

    }

    // Nếu khách có gõ thêm nội dung riêng trước/sau mẫu cũ,
    // thì bỏ phần mẫu cũ ở cuối, giữ lại phần khách tự viết, rồi nối mẫu mới.
    else if(

        selectedQuickComment.value &&

        current.endsWith(selectedQuickComment.value)

    ){

        const base = current

            .slice(0, current.length - selectedQuickComment.value.length)

            .trim()

        commentText.value = base ? base + " " + text : text

    }

    // Chưa từng chọn mẫu nào -> nối vào cuối nội dung hiện có (nếu có)
    else{

        commentText.value = current ? current + " " + text : text

    }

    selectedQuickComment.value = text

}

/* ===========================
        NAVIGATE TO PRODUCT DETAIL
=========================== */

// LƯU Ý: item ở OrderDetailView hiện chưa có productId, chỉ có skuId/skuCode.
// Đổi query bên dưới cho khớp với route thật của bạn khi đã bổ sung field.
function goToProductDetail() {

    const slug = props.orderItem?.productSlug

    if (!slug) {
        console.warn("[ReviewModal] Thiếu productSlug", props.orderItem)
        return
    }

    close()

    router.push({ name: "ProductDetail", params: { slug } })

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

    selectedQuickComment.value=""

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
    padding:20px;
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
    max-width:920px;
    max-height:92vh;

    overflow:hidden;

    background:white;

    border-radius:20px;

    box-shadow:
        0 30px 80px rgba(0,0,0,.18);

    animation:popup .25s;
}

.modal-scroll{

    max-height:92vh;

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

    padding:16px 22px;

    border-bottom:1px solid #f1f1f1;

    background:linear-gradient(135deg,#fff,#fff5f5);

}

.header-left{

    display:flex;

    align-items:center;

    gap:14px;

}

.header-icon{

    width:42px;

    height:42px;

    flex-shrink:0;

    border-radius:50%;

    display:flex;

    align-items:center;

    justify-content:center;

    background:#fff0f2;

    color:#e60012;

    font-size:17px;

}

.modal-header h3{

    font-size:20px;
    font-weight:800;

    color:#e60012;

    line-height:1.3;

}

.modal-header p{

    font-size:13px;

    color:#777;

    margin-top:2px;

}

.close-btn{

    width:38px;
    height:38px;

    flex-shrink:0;

    border:none;
    border-radius:50%;

    background:#fff0f5;

    cursor:pointer;

    display:flex;

    align-items:center;

    justify-content:center;

    transition:.25s;
}

.close-btn:hover{

    transform:rotate(90deg);

    background:#e60012;

    color:white;

}

/* ===========================
        Body grid (2 cột)
=========================== */

.modal-body-grid{

    display:flex;

    gap:22px;

    padding:14px 22px 0;

    align-items:flex-start;

}

/* Cột trái giờ chỉ còn sản phẩm + rating -> gọn, không còn chật chội */
.modal-left{

    flex:0 0 240px;

    display:flex;

    flex-direction:column;

    gap:14px;

}

/* Cột phải giờ gánh thêm phần upload/preview -> lấp đầy khoảng trống dọc */
.modal-right{

    flex:1;

    min-width:0;

    display:flex;

    flex-direction:column;

    gap:14px;

}

/* ===========================
        Product
=========================== */

.product-box{

    display:flex;

    gap:14px;

    padding:10px;

    align-items:center;

    border-radius:16px;

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

    width:68px;
    height:68px;

    border-radius:14px;

    object-fit:cover;

    border:1px solid #eee;

    flex-shrink:0;

}

.no-image{

    width:68px;

    height:68px;

    flex-shrink:0;

    border-radius:14px;

    border:1px solid #eee;

    background:#fafafa;

    display:flex;

    align-items:center;

    justify-content:center;

    color:#cbd5e1;

    font-size:22px;

}

.product-info{

    flex:1;

    min-width:0;

}

.product-info h4{

    font-size:15px;

    font-weight:700;

    line-height:1.35;

    color:#000;

    margin:0;

}

.product-info small{

    display:block;

    font-size:12px;

    color:#000;

    opacity:.6;

    margin-top:3px;

}

.view-detail-link{

    display:inline-flex;

    align-items:center;

    gap:5px;

    margin-top:5px;

    font-size:12px;

    font-weight:700;

    color:#000;

    transition:.2s;

}

.view-detail-link i{

    font-size:10px;

}

.product-box:hover .view-detail-link{

    color:#e60012;

    gap:7px;

}

/* ===========================
        Rating
=========================== */

.rating-section{

    padding:6px 4px;

}

.rating-box{

    display:flex;

    justify-content:center;

    gap:8px;

    margin-top:2px;

}

.star{

    font-size:32px;

    color:#ddd;

    cursor:pointer;

    transition:.25s;

    line-height:1;

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

    font-size:13px;

    color:#e60012;

    font-weight:700;

    margin:10px 0 0;

}

/* ===========================
        Textarea
=========================== */

.comment-box{

    padding:0;

}

.comment-box label{

    display:flex;

    align-items:center;

    gap:8px;

    font-weight:700;

    font-size:14px;

    color:#555;

    margin-bottom:8px;

}

/* ===========================
        Quick comments (tông trung tính, không dùng đỏ)
=========================== */

.quick-comments{

    display:flex;

    flex-wrap:wrap;

    gap:8px 8px;

    row-gap:10px;

    margin-bottom:12px;

}

.chip{

    padding:7px 13px;

    border-radius:20px;

    border:1px solid #e2e8f0;

    background:#f8fafc;

    color:#475569;

    font-size:12.5px;

    font-weight:600;

    cursor:pointer;

    transition:.2s;

    white-space:nowrap;

}

.chip:hover{

    background:#eef2ff;

    color:#3b4b6b;

    border-color:#c7d2fe;

}

.chip-active{

    background:#e0e7ff;

    color:#3730a3;

    border-color:#a5b4fc;

}

textarea{

    width:100%;

    min-height:150px;

    resize:none;

    border:2px solid #ececec;

    border-radius:14px;

    padding:14px;

    font-size:14px;

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

    margin:0;

}

.image-upload label{

    display:flex;

    justify-content:space-between;

    align-items:center;

    font-weight:700;

    font-size:14px;

    margin-bottom:8px;

    color:#555;

}

.upload-control{

    display:flex;

    align-items:center;

    gap:10px;

    padding:9px 12px;

    border-radius:10px;

    border:2px dashed #e60012;

    background:#fff5f5;

    position:relative;

}

.upload-native-input{

    /* Ẩn hoàn toàn input gốc của trình duyệt, chỉ dùng để mở hộp thoại chọn file */

    position:absolute;

    inset:0;

    width:100%;

    height:100%;

    opacity:0;

    cursor:pointer;

    z-index:-1;

}

.upload-choose-btn{

    flex-shrink:0;

    display:inline-flex;

    align-items:center;

    gap:6px;

    padding:7px 14px;

    border:none;

    border-radius:8px;

    background:#e60012;

    color:#fff;

    font-size:13px;

    font-weight:700;

    cursor:pointer;

    transition:.2s;

    white-space:nowrap;

}

.upload-choose-btn:hover{

    background:#c40010;

}

.upload-choose-btn:disabled{

    opacity:.6;

    cursor:not-allowed;

}

.upload-status-text{

    font-size:13px;

    color:#777;

    overflow:hidden;

    text-overflow:ellipsis;

    white-space:nowrap;

    min-width:0;

}

.upload-note{

    margin-top:6px;

    font-size:11.5px;

    color:#999;

}

.upload-loading{

    display:flex;

    align-items:center;

    gap:8px;

    font-size:13px;

    color:#e60012;

    font-weight:600;

}

/* ===========================
        Preview
=========================== */

.preview-list{

    display:flex;

    flex-wrap:wrap;

    gap:10px;

    margin:0;

}

.preview-item{

    position:relative;

}

.preview-image{

    width:72px;

    height:72px;

    object-fit:cover;

    border-radius:12px;

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

    top:-6px;

    right:-6px;

    width:20px;

    height:20px;

    border:none;

    border-radius:50%;

    background:#ff4d4f;

    color:white;

    cursor:pointer;

    font-size:11px;

    transition:.25s;

    display:flex;
    justify-content:center;
    align-items:center;

}

.remove-image:hover{

    transform:scale(1.15);

    background:#e53935;

}

.empty-image{

    display:flex;

    flex-direction:column;

    align-items:center;

    gap:4px;

    color:#bbb;

    font-size:12px;

    padding:10px 0;

}

.empty-image i{

    font-size:20px;

}

.empty-image p{

    margin:0;

}

/* ===========================
        Error
=========================== */

.error{

    margin:0;

    color:#ff4d4f;

    font-size:13px;

    font-weight:600;

}

/* ===========================
        Delete confirm
=========================== */

.delete-confirm-box{

    margin:16px 22px;

    padding:16px;

    border-radius:14px;

    background:#fff5f5;

    border:1px solid #ffd5d5;

    text-align:center;

}

.delete-confirm-box p{

    font-weight:700;

    font-size:14px;

    color:#d63031;

    margin-bottom:12px;

}

.delete-confirm-actions{

    display:flex;

    justify-content:center;

    gap:10px;

}

.btn-ghost-sm,
.btn-danger-sm{

    padding:10px 20px;

    border-radius:10px;

    cursor:pointer;

    font-weight:700;

    font-size:13px;

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

    padding:16px 22px;

    border-top:1px solid #f2f2f2;

}

.footer-right{

    display:flex;

    gap:10px;

}

.delete-link{

    background:none;

    border:none;

    color:#ff4d4f;

    font-weight:700;

    font-size:13px;

    cursor:pointer;

}

.cancel-btn{

    padding:11px 22px;

    border-radius:10px;

    border:1px solid #ddd;

    background:white;

    cursor:pointer;

    font-size:14px;

    transition:.25s;

}

.cancel-btn:hover{

    background:#f6f6f6;

}

.submit-btn{

    padding:11px 26px;

    border:none;

    border-radius:10px;

    background:linear-gradient(135deg,#e60012,#ff4d4f);

    color:white;

    font-weight:700;

    font-size:14px;

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

@media(max-width:768px){

.review-modal{

    max-width:100%;

    max-height:96vh;

    border-radius:16px;

}

.modal-scroll{

    max-height:96vh;

}

.modal-body-grid{

    flex-direction:column;

    padding:12px 16px 0;

    gap:16px;

}

.modal-left{

    flex:none;

}

.product-box{

    flex-direction:column;

    text-align:center;

    padding:10px 0;

}

.product-image,
.no-image{

    width:88px;
    height:88px;

}

.preview-image{

    width:70px;
    height:70px;

}

.star{

    font-size:32px;

}

.footer{

    flex-direction:column;

    gap:12px;

    padding:14px 16px;

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