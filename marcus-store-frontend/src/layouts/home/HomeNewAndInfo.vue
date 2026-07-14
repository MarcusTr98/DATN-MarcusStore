<template>
  <div class="row g-4 mb-5">
    <!-- Cột trái: Nội dung Điện thoại thông minh -->
    <div class="col-lg-8">
      <div class="phone-tabs mb-4">
        <button
          v-for="tab in phoneTabs"
          :key="tab"
          type="button"
          class="phone-tab-btn"
          :class="{ active: activePhoneTab === tab }"
          @click="activePhoneTab = tab"
        >
          {{ tab }}
        </button>
      </div>

      <div class="content-card">
        <p class="phone-intro lead-text">
          Trong kỷ nguyên công nghệ số, điện thoại di động đã trở thành vật bất ly thân, là trung
          tâm điều khiển cuộc sống số, giải trí và sáng tạo của mỗi cá nhân .
        </p>

        <!-- MỤC LỤC ĐỒNG BỘ -->
        <div class="toc-container">
          <button class="toc-header" @click="tocOpen = !tocOpen">
            <span><i class="fa-solid fa-list-ul"></i> Mục lục bài viết</span>
            <i class="fa-solid" :class="tocOpen ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
          </button>
          <Transition name="slide">
            <ul v-if="tocOpen" class="toc-list">
              <li v-for="item in tocItems" :key="item.anchor">
                <a :href="'#' + item.anchor">{{ item.text }}</a>
              </li>
            </ul>
          </Transition>
        </div>

        <!-- NỘI DUNG CHI TIẾT (ĐÃ ĐỒNG BỘ ID VỚI MỤC LỤC) -->
        <div class="phone-detail-wrapper" :class="{ collapsed: !detailOpen }">
          <div class="phone-detail">
            <h5 id="chuc-nang">1. Chức năng biến điện thoại thành trợ thủ</h5>
            <p>
              Smartphone hiện đại tích hợp AI để tối ưu hóa hiệu năng, xử lý hình ảnh thời gian thực
              và quản lý pin thông minh, biến mọi thao tác của bạn trở nên đơn giản hơn.
            </p>

            <h5 id="phan-loai">2. Phân loại điện thoại</h5>
            <p>
              Từ dòng phổ thông cho đến các mẫu flagship cao cấp hay điện thoại gập, mỗi loại hình
              đều phục vụ một mục đích chuyên biệt, từ chơi game đồ họa cao đến làm việc đa nhiệm.
            </p>

            <h5 id="thuong-hieu">3. TOP thương hiệu hàng đầu</h5>
            <p>
              iPhone với hệ sinh thái iOS, Samsung với màn hình đỉnh cao và Xiaomi với cấu hình
              "khủng" là ba cái tên sáng giá nhất tại thị trường Việt Nam hiện nay .
            </p>

            <h5 id="kinh-nghiem">4. Kinh nghiệm chọn mua</h5>
            <p>
              Xác định ngân sách, ưu tiên trải nghiệm thực tế tại cửa hàng và kiểm tra kỹ chế độ bảo
              hành là những bước không thể thiếu để tránh sai lầm không đáng có .
            </p>

            <h5 id="mua-hang">5. Mua sắm tại Marcus Store</h5>
            <p>
              Marcus Store cam kết cung cấp sản phẩm chính hãng 100%, hỗ trợ thu cũ đổi mới với giá
              trợ giá tốt nhất thị trường và chính sách bảo hành 1 đổi 1 cực kỳ ưu việt .
            </p>
          </div>
          <div v-if="!detailOpen" class="detail-fade"></div>
        </div>

        <button class="read-more-btn" @click="detailOpen = !detailOpen">
          {{ detailOpen ? 'Thu gọn nội dung ▲' : 'Xem chi tiết nội dung →' }}
        </button>
      </div>
    </div>

    <!-- Cột phải: Tin tức sản phẩm (Sticky Sidebar) -->
    <div class="col-lg-4">
      <div class="news-side-card sticky-sidebar">
        <h5 class="news-header"><i class="fa-regular fa-newspaper"></i> Tin tức công nghệ</h5>

        <div v-if="newsLoading" class="text-muted small py-3 text-center">
          <i class="fas fa-spinner fa-spin me-2"></i>Đang tải tin tức...
        </div>

        <div v-else-if="newsPosts.length === 0" class="text-muted small py-3 text-center">
          Chưa có bài viết nào được xuất bản .
        </div>

        <div v-else class="news-list">
          <router-link
            v-for="post in newsPosts"
            :key="post.id"
            :to="{ name: 'BlogDetail', params: { slug: post.slug } }"
            class="news-item"
          >
            <div v-if="post.thumbnailUrl" class="news-thumb">
              <img :src="post.thumbnailUrl" :alt="post.title" />
            </div>
            <div v-else class="news-thumb" :style="{ background: fallbackBg(post.id) }">
              <span class="news-thumb-emoji">📰</span>
            </div>
            <div class="news-content">
              <p class="news-title">{{ post.title }}</p>
              <small class="text-muted-action">Đọc ngay</small>
            </div>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue' // SỬA LỖI 1: Import onMounted từ 'vue'
import { postPublicApi } from '@/api/PostApi' // SỬA LỖI 2: Đảm bảo import đúng đường dẫn API bài viết

// ---- INFO SECTION ----
const phoneTabs = ref([
  'iPhone 17',
  'iPhone 17 256GB',
  'iPhone 17 Pro',
  'iPhone 17 Pro Max',
  'iPhone Air',
])
const activePhoneTab = ref('iPhone 17')

const tocOpen = ref(true)
const detailOpen = ref(false)

const tocItems = ref([
  { text: '1. Chức năng biến điện thoại thành trợ thủ', anchor: 'chuc-nang' },
  { text: '2. Phân loại điện thoại', anchor: 'phan-loai' },
  { text: '3. TOP thương hiệu hàng đầu', anchor: 'thuong-hieu' },
  { text: '4. Kinh nghiệm chọn mua', anchor: 'kinh-nghiem' },
  { text: '5. Mua sắm tại Marcus Store', anchor: 'mua-hang' },
])

// ---- NEWS SECTION ----
const newsPosts = ref([])
const newsLoading = ref(true)

const fallbackGradients = [
  'linear-gradient(135deg, #1a1a2e, #16213e)',
  'linear-gradient(135deg, #2d1b3d, #4a2c5e)',
  'linear-gradient(135deg, #0f3d2e, #1a5c45)',
  'linear-gradient(135deg, #1a2a4a, #2c4a7a)',
  'linear-gradient(135deg, #3d1a1a, #5e2c2c)',
]

function fallbackBg(id) {
  return fallbackGradients[id % fallbackGradients.length]
}

// SỬA LỖI 3: Trích xuất đúng tầng dữ liệu từ API response (.data.data hoặc .data.content tùy API cấu trúc)
onMounted(async () => {
  try {
    const res = await postPublicApi.getAll({ size: 5 })
    // Kiểm tra cấu trúc API trả về để gán mảng dữ liệu sạch cho chính xác
    newsPosts.value = res.data?.data?.content || res.data?.data || res || []
  } catch (error) {
    console.error('Lỗi khi fetch tin tức trang chủ:', error)
    newsPosts.value = []
  } finally {
    newsLoading.value = false
  }
})
</script>

<style scoped>
/* Layout Sticky cho Sidebar */
.sticky-sidebar {
  position: sticky;
  top: 90px;
}

/* Card Style nâng cấp */
.content-card {
  background: #fff;
  padding: 2.5rem;
  border-radius: 24px;
  border: 1px solid #f3d6e3;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
}

/* Tabs */
.phone-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.phone-tab-btn {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  padding: 10px 20px;
  font-weight: 700;
  font-size: 0.88rem;
  cursor: pointer;
  transition: 0.3s;
}
.phone-tab-btn:hover {
  border-color: #efbdd2;
  background: #fff0f7;
}
.phone-tab-btn.active {
  background: #e1121c;
  color: #fff;
  border-color: #e1121c;
}

/* TOC Style */
.toc-container {
  background: #fffafd;
  border-left: 4px solid #e1121c;
  border-radius: 4px;
  margin: 24px 0;
  border-top: 1px solid #f3d6e3;
  border-right: 1px solid #f3d6e3;
  border-bottom: 1px solid #f3d6e3;
  overflow: hidden;
}
.toc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 14px 18px;
  background: #fff0f7;
  border: none;
  font-weight: 800;
  color: #202636;
  cursor: pointer;
}
.toc-list {
  list-style: none;
  padding: 14px 20px;
  margin: 0;
}
.toc-list li {
  margin-bottom: 10px;
}
.toc-list li:last-child {
  margin-bottom: 0;
}
.toc-list a {
  color: #b4557d;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 600;
}
.toc-list a:hover {
  color: #e1121c;
  text-decoration: underline;
}

/* Detail content wrapper */
.phone-detail-wrapper {
  position: relative;
  overflow: hidden;
  transition: max-height 0.4s ease-in-out;
}
.phone-detail-wrapper.collapsed {
  max-height: 180px;
}
.phone-detail-wrapper:not(.collapsed) {
  max-height: 2000px;
}
.phone-detail h5 {
  font-size: 1.1rem;
  font-weight: 800;
  color: #202636;
  margin-top: 24px;
  margin-bottom: 12px;
}
.phone-detail p {
  font-size: 0.92rem;
  line-height: 1.75;
  color: #4b5563;
  margin-bottom: 14px;
}
.detail-fade {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 80px;
  background: linear-gradient(transparent, #fff);
}

/* Read more button */
.read-more-btn {
  margin-top: 16px;
  color: #e1121c;
  font-weight: 700;
  font-size: 0.9rem;
  border: none;
  background: none;
  cursor: pointer;
}
.read-more-btn:hover {
  text-transform: scale(1.02);
}

/* News Card bên phải */
.news-side-card {
  background: #fff;
  padding: 1.5rem;
  border-radius: 20px;
  border: 1px solid #f3d6e3;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}
.news-header {
  font-weight: 800;
  color: #202636;
  border-bottom: 2px solid #e1121c;
  padding-bottom: 12px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.news-item {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  text-decoration: none;
  color: #333;
  padding: 10px;
  border-radius: 12px;
  transition: 0.2s;
}
.news-item:hover {
  background: #fff0f7;
}
.news-thumb {
  width: 80px;
  height: 60px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
}
.news-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.news-thumb-emoji {
  font-size: 1.5rem;
}
.news-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: #202636;
  margin: 0 0 4px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.text-muted-action {
  font-size: 0.75rem;
  color: #e1121c;
  font-weight: 700;
}

/* Animations */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease-out;
}
.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
