<template>
  <div class="container py-5">
    <h2 class="fw-bold text-dark mb-4">
      <i class="far fa-newspaper me-2 text-danger"></i>Tin tức công nghệ
    </h2>

    <div v-if="loading && posts.length === 0" class="text-center text-muted py-5">
      <i class="fas fa-spinner fa-spin me-2"></i>Đang tải bài viết...
    </div>

    <div v-else-if="posts.length === 0" class="text-center text-muted py-5">
      Chưa có bài viết nào được đăng.
    </div>

    <template v-else>
      <div class="row g-4">
        <div class="col-md-4" v-for="post in posts" :key="post.id">
          <div class="card border-0 shadow-sm h-100 rounded-3 overflow-hidden bg-white">
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
                class="btn btn-light btn-sm text-danger w-100 fw-bold border mt-auto"
              >
                Đọc chi tiết bài viết
              </router-link>
            </div>
          </div>
        </div>
      </div>

      <div v-if="hasMore" class="text-center mt-5">
        <button type="button" class="btn btn-outline-danger px-4" :disabled="loading" @click="loadMore">
          <i class="fas fa-spinner fa-spin me-2" v-if="loading"></i>
          {{ loading ? 'Đang tải...' : 'Xem thêm bài viết' }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { postPublicApi } from '@/api/PostApi'

const posts = ref([])
const loading = ref(true)
const page = ref(0)
const hasMore = ref(true)

async function loadPage() {
  loading.value = true
  try {
    const res = await postPublicApi.getPage({ page: page.value })
    posts.value = posts.value.concat(res.content || [])
    hasMore.value = !res.last
  } catch (err) {
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value += 1
  loadPage()
}

onMounted(loadPage)
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
.post-thumb {
  height: 180px;
  width: 100%;
  object-fit: cover;
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
</style>