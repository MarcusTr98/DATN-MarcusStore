<template>
  <div class="row g-3 mb-5">
    <!-- Cột trái: giới thiệu Điện thoại thông minh (hiện full nội dung, có nút xem thêm/thu gọn) -->
    <div class="col-lg-8">
      <div class="phone-tabs d-flex flex-wrap gap-2 mb-3">
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

      <p class="phone-intro">
        Trong kỷ nguyên công nghệ số, điện thoại di động đã trở thành một phần không thể thiếu
        trong cuộc sống của mỗi người. Từ liên lạc, tra cứu thông tin đến chụp ảnh, quay phim, giải
        trí, điện thoại ngày càng chứng minh vai trò quan trọng và hữu dụng của mình. Với thiết kế
        nhỏ gọn, điện thoại đã trở thành vật mang theo bên mình không thể thiếu.
      </p>
      <p class="phone-intro">
        Vậy làm thế nào để lựa chọn được một chiếc điện thoại phù hợp giữa vô vàn sản phẩm trên thị
        trường? Bài viết này sẽ cung cấp những thông tin hữu ích, giúp bạn trở thành người mua hàng
        thông thái.
      </p>

      <div class="phone-toc">
        <button
          type="button"
          class="phone-toc-header d-flex align-items-center justify-content-between w-100"
          @click="tocOpen = !tocOpen"
        >
          <span>Nội dung chính</span>
          <span class="toc-toggle-icon">{{ tocOpen ? '▲' : '▼' }}</span>
        </button>

        <ul v-show="tocOpen" class="phone-toc-list">
          <li v-for="item in tocItems" :key="item.text">
            <a :href="'#' + item.anchor">{{ item.text }}</a>
            <ul v-if="item.children?.length">
              <li v-for="child in item.children" :key="child">
                <a href="#">{{ child }}</a>
              </li>
            </ul>
          </li>
        </ul>
      </div>

      <!-- Nội dung chi tiết: luôn hiện, nhưng bị giới hạn chiều cao khi chưa "Xem thêm" -->
      <div class="phone-detail-wrapper" :class="{ collapsed: !detailOpen }">
        <div class="phone-detail">
          <h5 id="muc1" class="detail-heading">
            1. Những chức năng chính biến điện thoại thành trợ thủ đắc lực
          </h5>
          <p>
            <strong>Giao tiếp và kết nối:</strong> Điện thoại cho phép bạn liên lạc với mọi người
            qua gọi điện, nhắn tin, email và video call, đồng thời kết nối với các thiết bị thông
            minh như đồng hồ thông minh và mắt kính thông minh, giúp việc liên lạc trở nên thuận
            tiện hơn bao giờ hết.
          </p>
          <p>
            <strong>Truy cập thông tin:</strong> Chỉ với vài thao tác chạm, bạn có thể dễ dàng truy
            cập internet để tìm kiếm mọi thông tin, cập nhật tin tức và xu hướng.
          </p>
          <p>
            <strong>Giải trí đa năng:</strong> Điện thoại là một trung tâm giải trí di động với khả
            năng nghe nhạc, xem phim, đọc sách, chơi game và sử dụng vô vàn ứng dụng giải trí khác.
          </p>
          <p>
            <strong>Quản lý cuộc sống:</strong> Các ứng dụng trên điện thoại giúp bạn lên kế hoạch,
            tổ chức công việc, nhắc nhở sự kiện quan trọng và quản lý cuộc sống hàng ngày hiệu quả.
          </p>
          <p>
            <strong>Bảo vệ người sử dụng:</strong> Công nghệ trên smartphone ngày càng được ứng
            dụng để bảo vệ sự an toàn của người dùng, ví dụ như chức năng SOS khi phát hiện va
            chạm.
          </p>

          <h5 id="muc2" class="detail-heading">2. Phân loại điện thoại</h5>
          <p>
            <strong>Theo tính năng:</strong> Điện thoại phổ thông là loại cơ bản, tập trung vào
            chức năng nghe gọi, nhắn tin, ưu điểm giá rẻ, pin trâu, dễ sử dụng. Điện thoại thông
            minh được trang bị hệ điều hành, kết nối internet, cài đặt ứng dụng, chụp ảnh chất
            lượng cao, ưu điểm là đa chức năng, khả năng tùy biến cao, đáp ứng mọi nhu cầu.
          </p>
          <p>
            <strong>Theo hệ điều hành:</strong> Android là hệ điều hành phổ biến nhất thế giới, mã
            nguồn mở, nhiều tùy biến, được dùng trên Samsung, Xiaomi, OPPO, Realme. iOS là hệ điều
            hành độc quyền của Apple, giao diện đơn giản, dễ sử dụng, bảo mật cao, hệ sinh thái
            đồng bộ. Ngoài ra một số điện thoại phổ thông còn dùng hệ điều hành khác như KaiOS.
          </p>
          <p>
            <strong>Theo nhu cầu/tính năng đặc biệt:</strong> Điện thoại gaming có cấu hình mạnh
            mẽ, tản nhiệt tốt, màn hình tần số quét cao, nhiều tính năng hỗ trợ game thủ. Điện
            thoại chụp ảnh đẹp có camera độ phân giải cao, nhiều ống kính, công nghệ xử lý ảnh tiên
            tiến. Điện thoại siêu bền chống nước, chống bụi, chống va đập, phù hợp người dùng hoạt
            động ngoài trời. Điện thoại AI trang bị các tính năng được hỗ trợ bởi trí tuệ nhân tạo.
            Điện thoại gập có thiết kế độc đáo, màn hình lớn có thể gập lại, mang đến trải nghiệm
            mới lạ.
          </p>

          <h5 id="muc3" class="detail-heading">3. TOP thương hiệu được ưa chuộng hiện nay</h5>
          <p>
            <strong>iPhone</strong> nổi tiếng với thiết kế sang trọng, hiệu năng mạnh mẽ, hệ điều
            hành iOS bảo mật và hệ sinh thái đồng bộ. <strong>Samsung</strong> đa dạng mẫu mã, từ
            tầm trung đến cao cấp, camera ấn tượng, màn hình đẹp, nhiều tính năng độc đáo.
            <strong>Xiaomi</strong> giá cả phải chăng, cấu hình tốt, thiết kế trẻ trung, nhiều tính
            năng hữu ích. <strong>OPPO</strong> chuyên về camera, thiết kế thời trang, sạc nhanh,
            nhiều tính năng làm đẹp. <strong>Realme</strong> tập trung vào hiệu năng, giá rẻ, thiết
            kế trẻ trung, phù hợp giới trẻ. <strong>Vivo</strong> camera selfie đẹp, thiết kế ấn
            tượng, nhiều tính năng độc đáo.
          </p>

          <h5 id="muc4" class="detail-heading">4. Kinh nghiệm chọn mua điện thoại</h5>
          <p>
            Trước tiên hãy xác định rõ <strong>nhu cầu sử dụng</strong>: bạn cần điện thoại để nghe
            gọi, nhắn tin, lướt web, xem phim, chơi game hay chụp ảnh? Tiếp theo, xác định
            <strong>ngân sách</strong> — số tiền bạn sẵn sàng chi cho chiếc điện thoại mới. Nên
            <strong>tìm hiểu kỹ thông tin</strong> qua các bài đánh giá, so sánh sản phẩm, tham
            khảo ý kiến người thân bạn bè, đồng thời <strong>đến trực tiếp cửa hàng</strong> để
            trải nghiệm thực tế màn hình, camera, tốc độ xử lý. Luôn <strong>kiểm tra kỹ</strong>
            tình trạng máy và phụ kiện đi kèm trước khi mua, và <strong>chọn cửa hàng uy tín</strong>
            để đảm bảo chất lượng sản phẩm cùng chế độ bảo hành tốt.
          </p>

          <h5 id="muc5" class="detail-heading">
            5. Mua điện thoại giá tốt, chính hãng tại Marcus Store
          </h5>
          <p>
            Bên cạnh các mẫu điện thoại phổ thông và smartphone quen thuộc, Marcus Store còn liên
            tục cập nhật những dòng sản phẩm công nghệ mới nhất như iPhone 17, iPhone 17 Pro,
            Samsung Z Flip 7, Samsung Z Fold 7, Samsung Galaxy S26, OPPO Find X9 Ultra, OPPO Find
            X9s, Xiaomi 17T, Xiaomi 17T Pro chính hãng, cùng nhiều lựa chọn khác như laptop, PC, tai
            nghe, bàn phím và các phụ kiện công nghệ — đáp ứng nhu cầu đa dạng từ người dùng cá
            nhân đến doanh nghiệp.
          </p>
          <p>
            Khi chọn mua điện thoại, bạn nên ưu tiên các cửa hàng uy tín, chất lượng như Marcus
            Store. Tại đây luôn có sẵn sản phẩm chính hãng 100% đi kèm chính sách bảo hành tốt cùng
            nhiều ưu đãi hấp dẫn: bảo hành cam kết 12 tháng, bảo hành chính hãng tại các trung tâm,
            giao hàng tận nhà nhanh chóng. Bạn có thể mua hàng theo hai hình thức: online trên
            website hoặc offline tại các cửa hàng trên toàn quốc — hoàn toàn không cần lo lắng về
            vấn đề bảo hành hay mua phải hàng kém chất lượng.
          </p>
        </div>

        <div v-if="!detailOpen" class="detail-fade"></div>
      </div>

      <button type="button" class="phone-read-more" @click="detailOpen = !detailOpen">
        {{ detailOpen ? 'Thu gọn ▲' : 'Xem thêm →' }}
      </button>
    </div>

    <!-- Cột phải: Tin tức sản phẩm (data tĩnh, click -> blog detail) -->
    <div class="col-lg-4">
      <div class="news-block">
        <div class="news-header d-flex align-items-center justify-content-between mb-3">
          <h5 class="news-title mb-0">Tin tức sản phẩm</h5>
          <router-link to="/blog" class="news-view-all">Xem tất cả →</router-link>
        </div>

        <div class="news-list">
          <router-link
            v-for="post in newsPosts"
            :key="post.title"
            to="/blog/detail-slug"
            class="news-item d-flex gap-2 mb-3"
          >
            <div class="news-thumb" :style="{ background: post.bg }">
              <span class="news-thumb-emoji">{{ post.emoji }}</span>
            </div>
            <p class="news-item-title mb-0">{{ post.title }}</p>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// ---- INFO SECTION: Điện thoại thông minh (data tĩnh, hiện full ngay tại Home) ----
const phoneTabs = ref(['iPhone 17', 'iPhone 17 256gb', 'iPhone 17 Pro', 'iPhone 17 Pro Max', 'iPhone Air'])
const activePhoneTab = ref('iPhone 17')

const tocOpen = ref(true)
const detailOpen = ref(false) // mặc định thu gọn, bấm "Xem thêm" mới hiện full

const tocItems = ref([
  { text: '1. Những chức năng chính biến điện thoại thành trợ thủ đắc lực', anchor: 'muc1' },
  {
    text: '2. Phân loại điện thoại',
    anchor: 'muc2',
    children: ['Theo tính năng', 'Theo hệ điều hành', 'Theo nhu cầu/Tính năng đặc biệt'],
  },
  { text: '3. TOP thương hiệu được ưa chuộng hiện nay', anchor: 'muc3' },
  { text: '4. Kinh nghiệm chọn mua điện thoại', anchor: 'muc4' },
  { text: '5. Mua điện thoại giá tốt, chính hãng tại Marcus Store', anchor: 'muc5' },
])

// ---- NEWS SECTION: Tin tức sản phẩm (data tĩnh, click -> /blog/detail-slug) ----
const newsPosts = ref([
  {
    title: 'Danh sách smartphone được lên đời Android 17: Bạn có thấy thiết bị của mình?',
    emoji: '🤖',
    bg: 'linear-gradient(135deg, #1a1a2e, #16213e)',
  },
  {
    title: 'OPPO Find N7 Wide sẽ có cụm camera sau ngang, màn hình không nếp gấp',
    emoji: '📷',
    bg: 'linear-gradient(135deg, #2d1b3d, #4a2c5e)',
  },
  {
    title: 'Xiaomi 18 có thể nâng cấp mạnh trải nghiệm màn hình phụ ở mặt sau với HyperOS 4',
    emoji: '📱',
    bg: 'linear-gradient(135deg, #0f3d2e, #1a5c45)',
  },
  {
    title: 'Cập nhật Xiaomi HyperOS 4: Chi tiết danh sách thiết bị hỗ trợ, ngày ra mắt và loạt tính năng',
    emoji: '4️⃣',
    bg: 'linear-gradient(135deg, #1a2a4a, #2c4a7a)',
  },
  {
    title: 'Samsung mở rộng thử nghiệm One UI 9 cho Galaxy Z Fold7, S24, S23 và A56',
    emoji: '🔄',
    bg: 'linear-gradient(135deg, #3d1a1a, #5e2c2c)',
  },
])
</script>

<style scoped>
.phone-tab-btn {
  background: #fff;
  border: 1px solid var(--cps-border);
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--cps-text);
  cursor: pointer;
  transition: all 0.15s ease;
}
.phone-tab-btn:hover {
  border-color: var(--cps-red-light);
}
.phone-tab-btn.active {
  border-color: var(--cps-red);
  color: var(--cps-red);
  background: var(--cps-red-tint);
}

.phone-intro {
  font-size: 0.85rem;
  line-height: 1.7;
  color: #444;
  margin-bottom: 10px;
}

.phone-toc {
  border: 1px solid var(--cps-border);
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 16px;
}
.phone-toc-header {
  background: #fff;
  border: none;
  padding: 12px 16px;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--cps-dark);
  cursor: pointer;
}
.toc-toggle-icon {
  font-size: 0.7rem;
  color: #888;
}
.phone-toc-list {
  list-style: none;
  background: var(--cps-gray);
  padding: 14px 20px;
  margin: 0;
  font-size: 0.82rem;
}
.phone-toc-list > li {
  margin-bottom: 8px;
}
.phone-toc-list ul {
  list-style: none;
  padding-left: 18px;
  margin-top: 6px;
}
.phone-toc-list ul li {
  margin-bottom: 6px;
}
.phone-toc-list a {
  color: var(--cps-blue-text);
  text-decoration: none;
}
.phone-toc-list a:hover {
  text-decoration: underline;
}

/* Khối nội dung chi tiết, bị giới hạn chiều cao khi collapsed */
.phone-detail-wrapper {
  position: relative;
  overflow: hidden;
  transition: max-height 0.3s ease;
}
.phone-detail-wrapper.collapsed {
  max-height: 180px;
}
.phone-detail-wrapper:not(.collapsed) {
  max-height: none;
}

.phone-detail {
  font-size: 0.85rem;
  line-height: 1.7;
  color: #444;
}
.detail-heading {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--cps-dark);
  margin-top: 18px;
  margin-bottom: 8px;
}
.phone-detail p {
  margin-bottom: 10px;
}

/* Lớp phủ mờ dần ở đáy khi đang thu gọn, gợi ý còn nội dung phía dưới */
.detail-fade {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 60px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0), #fff);
  pointer-events: none;
}

.phone-read-more {
  display: inline-block;
  margin-top: 14px;
  background: none;
  border: none;
  padding: 0;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--cps-red);
  cursor: pointer;
}
.phone-read-more:hover {
  text-decoration: underline;
}

.news-block {
  border: 1px solid var(--cps-border);
  border-radius: 12px;
  padding: 16px;
  height: 100%;
}
.news-title {
  font-size: 1rem;
  font-weight: 800;
  color: var(--cps-dark);
}
.news-view-all {
  font-size: 0.8rem;
  color: var(--cps-blue-text);
  text-decoration: none;
  font-weight: 600;
  white-space: nowrap;
}
.news-view-all:hover {
  text-decoration: underline;
}
.news-item {
  text-decoration: none;
  color: inherit;
}
.news-thumb {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.news-thumb-emoji {
  font-size: 1.6rem;
}
.news-item-title {
  font-size: 0.82rem;
  line-height: 1.4;
  color: var(--cps-text);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.news-item:hover .news-item-title {
  color: var(--cps-red);
}
</style>