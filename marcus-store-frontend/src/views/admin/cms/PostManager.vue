<template>
  <div class="bm-page">

    <!-- Danh sách -->
    <template v-if="view === 'list'">
      <div class="page-header">
        <div class="page-header-left">
          <div class="page-icon">
            <i class="bi bi-file-earmark-text"></i>
          </div>
          <div>
            <h2 class="page-title">Quản lý bài viết</h2>
            <p class="page-sub">Quản lý bài viết tin tức, thủ thuật, đánh giá sản phẩm trên website.</p>
          </div>
        </div>
        <button class="btn-add" @click="goCreate">
          <i class="bi bi-plus-lg"></i> Thêm bài viết
        </button>
      </div>

      <section class="stats-grid">
        <article class="stat-card">
          <span>Tổng bài viết</span>
          <strong>{{ stats.total }}</strong>
        </article>
        <article class="stat-card">
          <span>Đã xuất bản</span>
          <strong class="text-accent">{{ stats.published }}</strong>
        </article>
        <article class="stat-card">
          <span>Lên lịch đăng</span>
          <strong>{{ stats.scheduled }}</strong>
        </article>
        <article class="stat-card">
          <span>Bản nháp</span>
          <strong>{{ stats.draft }}</strong>
        </article>
      </section>

      <div class="page-card">
        <div v-if="loading" class="state-box">
          <i class="bi bi-arrow-repeat spin"></i> Đang tải dữ liệu...
        </div>

        <div v-else-if="loadError" class="state-box state-error">
          <i class="bi bi-exclamation-circle"></i> {{ loadError }}
          <button class="btn-retry" @click="loadAll">Thử lại</button>
        </div>

        <template v-else>
          <PostFilter
            :filters="filters"
            :categories="categoryOptions"
            @update:search="filters.search = $event"
            @update:categoryId="filters.categoryId = $event"
            @update:status="filters.status = $event"
          />

          <PostTable
            :posts="filteredPosts"
            :categories="categoryOptions"
            @edit="goEdit"
            @toggle="handleToggle"
          />
        </template>
      </div>
    </template>

    <PostForm
      v-else
      :post="editingPost"
      :categories="categoryOptions"
      @saved="onFormSaved"
      @cancel="onFormCancel"
    />

    <div class="toast-wrap">
      <div v-for="t in toasts" :key="t.id" class="toast" :class="t.type">{{ t.message }}</div>
    </div>

    <Teleport to="body">
      <div v-if="confirmHidePost" class="modal-overlay" @click.self="cancelHideConfirm">
        <div class="modal-box">
          <div class="modal-header">
            <i class="bi bi-eye-slash-fill warn-icon"></i>
            <span class="modal-title">Ẩn bài viết này?</span>
          </div>
          <p class="modal-body">
            Bài viết "<strong>{{ confirmHidePost.title }}</strong>" sẽ không còn hiển thị công khai trên website.
          </p>
          <div class="modal-actions">
            <button class="btn-cancel-modal" @click="cancelHideConfirm">Hủy</button>
            <button class="btn-confirm-hide" @click="confirmHide">
              <i class="bi bi-eye-slash"></i> Xác nhận ẩn
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue';
import PostFilter from './PostFilter.vue';
import PostTable from './PostTable.vue';
import PostForm from './PostForm.vue';
import { postApi } from '@/api/PostApi';

const view = ref('list'); 
const editingPost = ref(null); 
const confirmHidePost = ref(null);

const posts = ref([]);
const rawCategories = ref([]);
const loading = ref(true);
const loadError = ref('');
const toasts = ref([]);
function pushToast(type, message) {
  const id = Date.now() + Math.random();
  toasts.value.push({ id, type, message });
  setTimeout(() => { toasts.value = toasts.value.filter((t) => t.id !== id); }, 3200);
}

const categoryOptions = computed(() =>
  rawCategories.value.map((c) => ({
    value: c.id,
    label: c.name,
  }))
);

const filters = reactive({ search: '', categoryId: '', status: '' });

function computeStatus(p) {
  if (!p.isPublished) return 'draft';
  const now = new Date();
  const pub = p.publishedAt ? new Date(p.publishedAt) : null;
  if (pub && now < pub) return 'scheduled';
  return 'published';
}

const stats = computed(() => {
  let published = 0, scheduled = 0, draft = 0;
  posts.value.forEach((p) => {
    const s = computeStatus(p);
    if (s === 'published') published++;
    else if (s === 'scheduled') scheduled++;
    else draft++;
  });
  return { total: posts.value.length, published, scheduled, draft };
});

const filteredPosts = computed(() => {
  let list = [...posts.value];
  if (filters.search.trim()) {
    const q = filters.search.toLowerCase();
    list = list.filter((p) => p.title?.toLowerCase().includes(q));
  }
  if (filters.categoryId) {
    list = list.filter((p) => String(p.postCategoryId) === String(filters.categoryId));
  }
  if (filters.status) {
    list = list.filter((p) => computeStatus(p) === filters.status);
  }
  list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  return list;
});

function mapFromApi(p) {
  return {
    id: p.id,
    postCategoryId: p.postCategoryId,
    postCategoryName: p.postCategoryName,
    authorId: p.authorId,
    authorName: p.authorName,
    title: p.title,
    slug: p.slug,
    thumbnailUrl: p.thumbnailUrl,
    excerpt: p.excerpt,
    content: p.content,
    isPublished: !!p.isPublished,
    publishedAt: p.publishedAt,
    createdAt: p.createdAt,
    updatedAt: p.updatedAt,
  };
}

async function loadAll() {
  loading.value = true;
  loadError.value = '';
  try {
    const [postList, catRes] = await Promise.all([
      postApi.getAll(),
      postApi.getCategories(),
    ]);
    posts.value = postList.map(mapFromApi);
    rawCategories.value = catRes;
  } catch {
    loadError.value = 'Không tải được dữ liệu bài viết. Vui lòng thử lại.';
  } finally {
    loading.value = false;
  }
}

onMounted(loadAll);

function goCreate() {
  editingPost.value = null;
  view.value = 'form';
}
function goEdit(post) {
  editingPost.value = post;
  view.value = 'form';
}
function onFormCancel() {
  view.value = 'list';
  editingPost.value = null;
}
async function onFormSaved() {
  view.value = 'list';
  editingPost.value = null;
  await loadAll();
}

// Bật công khai -> làm ngay. Ẩn bài viết -> hỏi xác nhận trước rồi mới ẩn ngay lập tức.
function handleToggle(post) {
  const wantPublish = !post.isPublished;
  if (!wantPublish) {
    confirmHidePost.value = post;
    return;
  }
  executeToggle(post, true);
}

function cancelHideConfirm() {
  confirmHidePost.value = null;
}

function confirmHide() {
  const post = confirmHidePost.value;
  confirmHidePost.value = null;
  executeToggle(post, false);
}

async function executeToggle(post, newVal) {
  const idx = posts.value.findIndex((p) => p.id === post.id);
  if (idx === -1) return;
  posts.value[idx] = { ...posts.value[idx], isPublished: newVal };
  try {
    await postApi.togglePublish(post, newVal);
    pushToast('success', newVal ? 'Đã công khai bài viết.' : 'Đã ẩn bài viết.');
  } catch (err) {
    posts.value[idx] = { ...posts.value[idx], isPublished: !newVal };
    pushToast('error', err?.response?.data?.message || 'Cập nhật trạng thái thất bại.');
  }
}
</script>

<style scoped>
.bm-page { padding: 24px; background: #f9fafb; min-height: 100%; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.page-header-left { display: flex; align-items: center; gap: 16px; }
.page-icon {
  width: 48px; height: 48px; border-radius: 12px;
  background: linear-gradient(135deg, #f55d9b, #ec4d8d);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 22px; flex-shrink: 0;
}
.page-title { font-size: 22px; font-weight: 700; color: #f55d9b; margin: 0; }
.page-sub { font-size: 13px; color: #6b7280; margin: 2px 0 0; }
.btn-add {
  display: flex; align-items: center; gap: 6px;
  background: #f55d9b; color: #fff; border: none; border-radius: 10px;
  padding: 10px 20px; font-size: 14px; font-weight: 500; cursor: pointer;
  transition: background 0.15s;
}
.btn-add:hover { background: #ec4d8d; }

.page-card { background: #fff; border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); overflow: hidden; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
@media (max-width: 992px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
.stat-card {
  border: 1px solid #f3d6e3; background: #ffffff; box-shadow: 0 4px 18px rgba(15, 23, 42, 0.06);
  padding: 20px 18px; border-radius: 8px; display: flex; flex-direction: column;
  justify-content: space-between; min-height: 100px;
}
.stat-card span { display: block; color: #6b7280; font-size: 0.86rem; font-weight: 700; }
.stat-card strong { display: block; margin-top: 6px; font-size: 1.65rem; line-height: 1; }
.text-accent { color: #f55d9b; }

.state-box { padding: 48px; text-align: center; color: #9ca3af; font-size: 14px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.state-error { color: #dc2626; }
.spin { animation: spin 1s linear infinite; font-size: 24px; }
@keyframes spin { to { transform: rotate(360deg); } }
.btn-retry {
  margin-top: 8px; background: #f55d9b; color: #fff; border: none; border-radius: 8px;
  padding: 7px 18px; font-size: 13px; cursor: pointer;
}
.btn-retry:hover { background: #ec4d8d; }

.toast-wrap { position: fixed; right: 24px; bottom: 24px; z-index: 10000; display: flex; flex-direction: column; gap: 10px; }
.toast { min-width: 260px; max-width: 360px; padding: 13px 16px; border-radius: 12px; box-shadow: 0 10px 30px rgba(15,23,42,0.18); font-size: 13.5px; font-weight: 500; }
.toast.success { background: #f0fdf4; color: #15803d; border: 1px solid #bbf0cc; }
.toast.error { background: #fef2f2; color: #b91c1c; border: 1px solid #f5c2c7; }

.modal-overlay { position: fixed; inset: 0; background: rgba(15,23,42,0.46); display: flex; align-items: center; justify-content: center; z-index: 10001; padding: 20px; }
.modal-box { background: #fff; border-radius: 14px; padding: 22px 24px; max-width: 400px; width: 100%; box-shadow: 0 20px 60px rgba(15,23,42,0.18); }
.modal-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.warn-icon { color: #f59e0b; font-size: 18px; }
.modal-title { font-size: 16px; font-weight: 700; color: #111827; }
.modal-body { font-size: 13.5px; color: #4b5563; line-height: 1.6; margin: 0 0 20px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }
.btn-cancel-modal {
  background: #fff; border: 1px solid #f3d6e3; color: #6b7280; border-radius: 8px;
  padding: 8px 16px; font-size: 13px; cursor: pointer; transition: all 0.15s;
}
.btn-cancel-modal:hover { background: #fff0f7; color: #d63384; border-color: #efbdd2; }
.btn-confirm-hide {
  background: #fff5f6; color: #dc3545; border: 1px solid #f5c2c7; border-radius: 8px;
  padding: 8px 16px; font-size: 13px; font-weight: 500; cursor: pointer;
  display: flex; align-items: center; gap: 6px; transition: background 0.15s;
}
.btn-confirm-hide:hover { background: #f8d7da; }
</style>