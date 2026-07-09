<template>
  <div class="container py-5">
    <h2 class="fw-bold text-dark mb-4">
      <i class="far fa-newspaper me-2 text-danger"></i>Tin tức công nghệ
    </h2>

    <div v-if="loading" class="text-center text-muted py-5">
      <i class="fas fa-spinner fa-spin me-2"></i>Đang tải bài viết...
    </div>

    <div v-else-if="posts.length === 0" class="text-center text-muted py-5">
      Chưa có bài viết nào được đăng.
    </div>

    <template v-else>
      <div class="row g-4">
        <div class="col-md-4" v-for="post in posts" :key="post.id">
          <div class="card border-0 shadow-sm h-100 rounded-3 overflow-hidden bg-white position-relative">
            <div v-if="post.thumbnailUrl" class="post-thumb-wrap">
              <img :src="post.thumbnailUrl" aria-hidden="true" class="post-thumb-bg" />
              <img :src="post.thumbnailUrl" :alt="post.title" class="post-thumb-fg" />
            </div>
            <div v-else class="bg-secondary text-white text-center py-5 small" style="height: 180px">
              Chưa có ảnh đại diện
            </div>
            <div class="card-body p-3 d-flex flex-column">
              <span v-if="post.postCategoryName" class="badge bg-danger-subtle text-danger fw-semibold mb-2 align-self-start">
                {{ post.postCategoryName }}
              </span>
              <h6 class="fw-bold text-dark line-clamp-2">{{ post.title }}</h6>
              <p class="text-muted small line-clamp-3 mb-3">{{ post.excerpt }}</p>
              <router-link
                :to="{ name: 'BlogDetail', params: { slug: post.slug } }"
                class="btn btn-light btn-sm text-danger w-100 fw-bold border mt-auto stretched-link"
              >
                Đọc chi tiết bài viết
              </router-link>
            </div>
          </div>
        </div>
      </div>

      <!-- Phân trang thật (trang 1/2/3...) thay vì nút "Xem thêm" tải chồng vô hạn -->
      <nav v-if="totalPages > 1" class="mt-5 d-flex justify-content-center">
        <ul class="pagination">
          <li class="page-item" :class="{ disabled: page === 0 }">
            <button type="button" class="page-link" @click="goToPage(page - 1)">‹ Trước</button>
          </li>
          <li v-for="p in pageNumbers" :key="p" class="page-item" :class="{ active: p - 1 === page }">
            <button type="button" class="page-link" @click="goToPage(p - 1)">{{ p }}</button>
          </li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }">
            <button type="button" class="page-link" @click="goToPage(page + 1)">Sau ›</button>
          </li>
        </ul>
      </nav>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { postPublicApi } from '@/api/PostApi'

const posts = ref([])
const loading = ref(true)
const page = ref(0) // 0-indexed, khớp với Spring Data Pageable
const totalPages = ref(1)
const pageSize = 6

const pageNumbers = computed(() => Array.from({ length: totalPages.value }, (_, i) => i + 1))

async function loadPage(targetPage) {
  loading.value = true
  try {
    const res = await postPublicApi.getPage({ page: targetPage, size: pageSize })
    posts.value = res.content || []
    totalPages.value = res.totalPages || 1
    page.value = res.number ?? targetPage
  } catch (err) {
    posts.value = []
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

function goToPage(p) {
  if (p < 0 || p > totalPages.value - 1 || p === page.value) return
  loadPage(p)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => loadPage(0))
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.post-thumb-wrap {
  position: relative;
  height: 180px;
  width: 100%;
  overflow: hidden;
  background: #e9ecef;
}
.post-thumb-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(14px) brightness(0.85);
  transform: scale(1.3);
}
.post-thumb-fg {
  position: relative;
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.card.position-relative {
  cursor: pointer;
  transition: box-shadow 0.15s, transform 0.15s;
}
.card.position-relative:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1) !important;
  transform: translateY(-2px);
}
.pagination .page-link {
  color: #e1121c;
  border-color: #f1d6d6;
}
.pagination .page-item.active .page-link {
  background: #e1121c;
  border-color: #e1121c;
  color: #fff;
}
.pagination .page-item.disabled .page-link {
  color: #adb5bd;
}
</style>