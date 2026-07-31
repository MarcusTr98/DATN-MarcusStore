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

          <div class="blog-rich-content lh-base text-secondary" v-html="sanitizedContent"></div>

          <!-- Nút Mua ngay — Đã FIX thành nút Click Vue Router -->
          <div v-if="productLinks.length" class="product-links-section">
            <div v-if="hotBadgeText" class="hot-badge">
              <i class="bi bi-fire"></i> {{ hotBadgeText }}
            </div>
            <div class="product-links-wrap">
              <button
                v-for="(link, i) in productLinks"
                :key="i"
                type="button"
                @click="handleBuyNow(link)"
                class="btn-buy-now"
              >
                <i class="bi bi-cart2 btn-buy-icon"></i>
                <span class="btn-buy-divline"></span>
                <span class="btn-buy-text">Mua ngay</span>
                <span class="btn-buy-name">{{ link.label }}</span>
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postPublicApi } from '@/api/PostApi'

const route = useRoute()
const router = useRouter()

const post = ref(null)
const loading = ref(true)
const loadError = ref('')

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const productLinks = ref([])
const hotBadgeText = ref('')
const sanitizedContent = ref('')

function parsePostContent(html) {
  if (!html) {
    sanitizedContent.value = ''
    productLinks.value = []
    hotBadgeText.value = ''
    return
  }
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  const badgeEl = doc.querySelector('span[data-hot-badge]')
  hotBadgeText.value = badgeEl ? (badgeEl.getAttribute('data-hot-badge') || '') : ''
  badgeEl?.remove()

  const anchors = doc.querySelectorAll('a[data-product-link="true"]')
  const links = []
  anchors.forEach(a => {
    links.push({
      href: a.getAttribute('href') || '',
      label: a.textContent.trim() || 'Sản phẩm'
    })
    a.remove()
  })
  productLinks.value = links
  sanitizedContent.value = doc.body.innerHTML
}

function handleBuyNow(link) {
  let rawHref = link.href.trim()

  let slug = ''
  if (rawHref.includes('/product/')) {
    slug = rawHref.split('/product/')[1]
  } else if (rawHref && rawHref !== '#') {
    slug = rawHref.replace(/^\//, '') 
  }
  slug = slug.split('#')[0].split('?')[0]

  if (slug) {
    router.push({ name: 'ProductDetail', params: { slug } })
  } else {
    const fallbackSlug = link.label
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[đĐ]/g, 'd')
      .replace(/[^a-z0-9 -]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-')

    router.push({ name: 'ProductDetail', params: { slug: fallbackSlug } })
  }
}

watch(() => post.value?.content, (newContent) => {
  parsePostContent(newContent || '')
}, { immediate: true })

async function loadPost(slug) {
  loading.value = true
  loadError.value = ''
  post.value = null
  productLinks.value = []
  hotBadgeText.value = ''
  sanitizedContent.value = ''
  try {
    post.value = await postPublicApi.getBySlug(slug)
  } catch (err) {
    loadError.value = err?.response?.data?.message || 'Không tìm thấy bài viết này hoặc bài viết chưa được xuất bản.'
  } finally {
    loading.value = false
  }
}

onMounted(() => loadPost(route.params.slug))

// Hỗ trợ chuyển bài mà không unmount component
watch(() => route.params.slug, (newSlug) => {
  if (newSlug) loadPost(newSlug)
})
</script>

<style scoped>
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

/* ── Khu vực Mua ngay ── */
.product-links-section {
  margin-top: 2rem;
  padding-top: 1.25rem;
  border-top: 1px solid #dee2e6;
}
.hot-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fca5a5;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 11px;
  margin-bottom: 12px;
}
.product-links-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.btn-buy-now {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: #dc2626;
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 13px 22px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  box-shadow: 0 2px 0 #991b1b, 0 4px 12px rgba(220, 38, 38, 0.25);
  transition: background 0.18s, transform 0.15s, box-shadow 0.18s;
}
.btn-buy-now:hover {
  background: #b91c1c;
  color: #fff;
  text-decoration: none;
  transform: translateY(-2px);
  box-shadow: 0 4px 0 #7f1d1d, 0 8px 20px rgba(220, 38, 38, 0.35);
}
.btn-buy-now:active {
  transform: translateY(1px);
  box-shadow: 0 1px 0 #991b1b, 0 2px 6px rgba(220, 38, 38, 0.2);
}
.btn-buy-icon {
  font-size: 18px;
}
.btn-buy-divline {
  width: 1px;
  height: 16px;
  background: rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}
.btn-buy-text {
  font-weight: 400;
  opacity: 0.9;
}
.btn-buy-name {
  font-weight: 700;
}
</style>