<template>
  <div class="container py-5">
    <div class="row justify-content-center">
      <div class="col-lg-9 bg-white p-4 p-md-5 rounded-3 shadow-sm border">

        <div v-if="loading" class="text-center text-muted py-5">
          <i class="fas fa-spinner fa-spin me-2"></i>Đang tải bài viết...
        </div>

        <div v-else-if="loadError" class="text-center text-muted py-5">
          <i class="fas fa-exclamation-circle me-2 text-danger"></i>{{ loadError }}
        </div>

        <template v-else-if="post">
          <div v-if="post.thumbnailUrl" class="post-detail-thumb-wrap mb-4">
            <img :src="post.thumbnailUrl" aria-hidden="true" class="post-detail-thumb-bg" />
            <img :src="post.thumbnailUrl" :alt="post.title" class="post-detail-thumb-fg" />
          </div>

          <span v-if="post.postCategoryName" class="badge bg-danger-subtle text-danger fw-semibold mb-2">
            {{ post.postCategoryName }}
          </span>

          <h2 class="fw-bold text-dark mb-3">{{ post.title }}</h2>

          <div class="d-flex align-items-center gap-3 text-muted small mb-4 pb-3 border-bottom">
            <span><i class="far fa-user me-1"></i>Tác giả: {{ post.authorName || 'Marcus Store' }}</span>
            <span><i class="far fa-clock me-1"></i>Ngày đăng: {{ formatDate(post.publishedAt) }}</span>
          </div>

          <div class="blog-rich-content lh-base text-secondary" v-html="post.content"></div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { postPublicApi } from '@/api/PostApi'

const route = useRoute()
const post = ref(null)
const loading = ref(true)
const loadError = ref('')

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

async function loadPost(slug) {
  loading.value = true
  loadError.value = ''
  post.value = null
  try {
    post.value = await postPublicApi.getBySlug(slug)
  } catch (err) {
    loadError.value = err?.response?.data?.message || 'Không tìm thấy bài viết này hoặc bài viết chưa được xuất bản.'
  } finally {
    loading.value = false
  }
}

onMounted(() => loadPost(route.params.slug))

// Hỗ trợ trường hợp chuyển từ bài viết này sang bài viết khác (slug đổi) mà không unmount component
watch(() => route.params.slug, (newSlug) => {
  if (newSlug) loadPost(newSlug)
})
</script>

<style scoped>
.post-detail-thumb {
  width: 100%;
  max-height: 420px;
  object-fit: cover;
  border-radius: 12px;
}
.post-detail-thumb-wrap {
  position: relative;
  width: 100%;
  height: 340px;
  overflow: hidden;
  border-radius: 12px;
  background: #e9ecef;
}
.post-detail-thumb-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(18px) brightness(0.85);
  transform: scale(1.3);
}
.post-detail-thumb-fg {
  position: relative;
  width: 100%;
  height: 100%;
  object-fit: contain;
}
@media (max-width: 768px) {
  .post-detail-thumb-wrap {
    height: 220px;
  }
}
.blog-rich-content :deep(p) {
  margin-bottom: 1rem;
}
.blog-rich-content :deep(ul),
.blog-rich-content :deep(ol) {
  margin-bottom: 1rem;
  padding-left: 1.5rem;
}
.blog-rich-content :deep(a) {
  color: #e1121c;
}
</style>