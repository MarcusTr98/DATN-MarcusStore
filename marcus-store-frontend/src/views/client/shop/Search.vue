<template>
  <div class="container py-4 search-page">
    <!-- Breadcrumb -->
    <nav aria-label="breadcrumb" class="mb-3">
      <ol class="breadcrumb small mb-0">
        <li class="breadcrumb-item"><router-link to="/">Trang chủ</router-link></li>
        <li class="breadcrumb-item active">Tìm kiếm</li>
      </ol>
    </nav>

    <!-- Title -->
    <div class="mb-3">
      <h4 class="fw-bold mb-1">
        Kết quả tìm kiếm
        <span class="text-muted fs-6 fw-normal">cho "{{ keyword }}"</span>
      </h4>
      <p class="text-muted small mb-0">{{ totalCount }} sản phẩm</p>
    </div>

    <!-- Filter bar: Loại + Hãng + Sắp xếp -->
    <div class="filter-bar d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
      <div class="filter-chips d-flex align-items-center gap-3 flex-wrap">
        <div class="d-flex align-items-center gap-2 flex-wrap">
          <span class="filter-label">Loại:</span>
          <button
            v-for="opt in typeOptions"
            :key="opt.slug ?? 'all'"
            type="button"
            class="filter-chip"
            :class="{ active: parentCategorySlug === opt.slug }"
            @click="onTypeChange(opt.slug)"
          >
            {{ opt.label }}
          </button>
        </div>

        <div v-if="availableBrands.length" class="d-flex align-items-center gap-2 flex-wrap">
          <span class="filter-label">Hãng:</span>
          <button
            type="button"
            class="filter-chip"
            :class="{ active: brandSlug == null }"
            @click="onBrandChange(null)"
          >
            Tất cả
          </button>
          <button
            v-for="b in availableBrands"
            :key="b.slug"
            type="button"
            class="filter-chip"
            :class="{ active: brandSlug === b.slug }"
            @click="onBrandChange(b.slug)"
          >
            {{ b.categoryName }}
          </button>
        </div>
      </div>

      <div class="sort-chips d-flex align-items-center gap-2 flex-wrap">
        <span class="filter-label">Sắp xếp:</span>
        <button
          v-for="opt in sortOptions"
          :key="opt.value"
          type="button"
          class="filter-chip"
          :class="{ active: sortBy === opt.value }"
          @click="onSortChange(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- Grid sản phẩm -->
    <ProductCard
      ref="productCardRef"
      mode="list"
      :keyword="keyword"
      :parent-category-slug="parentCategorySlug"
      :brand-slug="brandSlug"
      :sort-by="sortBy"
      :page="page"
      :size="12"
    />

    <!-- Pagination -->
    <nav
      v-if="totalPages > 1"
      class="d-flex justify-content-center mt-4"
      aria-label="Search pagination"
    >
      <ul class="pagination mb-0">
        <li class="page-item" :class="{ disabled: page <= 0 }">
          <button class="page-link" @click="goPage(page - 1)" :disabled="page <= 0">
            &laquo;
          </button>
        </li>
        <li
          v-for="p in totalPages"
          :key="p"
          class="page-item"
          :class="{ active: p - 1 === page }"
        >
          <button class="page-link" @click="goPage(p - 1)">{{ p }}</button>
        </li>
        <li class="page-item" :class="{ disabled: page >= totalPages - 1 }">
          <button
            class="page-link"
            @click="goPage(page + 1)"
            :disabled="page >= totalPages - 1"
          >
            &raquo;
          </button>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductCard from '@/components/client/ProductCard.vue'

const route = useRoute()
const router = useRouter()

// Marcus: chip Loại dùng slug thay vì id để URL dễ đọc (BE sẽ lookup id).
// 4 chip tương ứng 4 cây cha trong DB: Điện thoại (1), Phụ kiện (6), Máy tính bảng (10).
// Nếu admin đổi slug trong DB thì cần cập nhật lại bảng này.
const TYPE_OPTIONS = [
  { slug: null, label: 'Tất cả' },
  { slug: 'dien-thoai', label: 'Sản phẩm' },
  { slug: 'phu-kien', label: 'Phụ kiện' },
]
const SORT_OPTIONS = [
  { value: 'price_desc', label: 'Giá cao → thấp' },
  { value: 'price_asc', label: 'Giá thấp → cao' },
]
const typeOptions = TYPE_OPTIONS
const sortOptions = SORT_OPTIONS

function parseNullableString(v) {
  if (v === undefined || v === null || v === '') return null
  return String(v)
}
function parsePage(v) {
  const n = Number(v)
  return Number.isFinite(n) && n >= 0 ? Math.floor(n) : 0
}
function parseSort(v) {
  return v === 'price_asc' || v === 'price_desc' ? v : 'price_desc'
}

const keyword = computed(() => String(route.query.q || ''))
const parentCategorySlug = ref(parseNullableString(route.query.parentCategorySlug))
const brandSlug = ref(parseNullableString(route.query.brandSlug))
const sortBy = ref(parseSort(route.query.sortBy))
const page = ref(parsePage(route.query.page))
const productCardRef = ref(null)
const availableBrands = ref([])
// Marcus: refs bị auto-unwrap khi expose qua defineExpose,
// nên đọc trực tiếp productCardRef.value.totalElements (không cần .value).
const totalCount = computed(
  () => productCardRef.value?.totalElements ?? 0,
)
const totalPages = computed(
  () => productCardRef.value?.totalPages ?? 0,
)

function syncToUrl({ resetPage = false } = {}) {
  if (resetPage) page.value = 0
  router.replace({
    query: {
      q: keyword.value || undefined,
      parentCategorySlug: parentCategorySlug.value ?? undefined,
      brandSlug: brandSlug.value ?? undefined,
      sortBy: sortBy.value !== 'price_desc' ? sortBy.value : undefined,
      page: page.value > 0 ? page.value : undefined,
    },
  })
}

function onTypeChange(slug) {
  parentCategorySlug.value = slug
  // Đổi Loại → reset Hãng (brandSlug không dùng trên UI nữa nhưng vẫn reset cho sạch)
  brandSlug.value = null
  syncToUrl({ resetPage: true })
}
function onBrandChange(slug) {
  brandSlug.value = brandSlug.value === slug ? null : slug
  syncToUrl({ resetPage: true })
}
function onSortChange(value) {
  sortBy.value = value
  syncToUrl({ resetPage: true })
}
function goPage(n) {
  if (n < 0) return
  page.value = n
  syncToUrl()
}

watch(
  () => route.query.q,
  (newQ, oldQ) => {
    // Khi keyword đổi (user gõ từ khóa mới qua ô search), reset filter
    // Loại + Hãng + Sort + Page. Lý do: nếu user đang chọn chip Phụ kiện
    // rồi gõ "ip" mới, BE nhánh Phụ kiện sẽ bỏ LIKE và trả tất cả PK
    // (Samsung, Xiaomi...) → user tưởng sai. Reset về "Tất cả" cho khớp intent.
    if (newQ !== oldQ) {
      parentCategorySlug.value = null
      brandSlug.value = null
      sortBy.value = 'price_desc'
      page.value = 0
      // Đồng bộ URL để chip Loại trở về "Tất cả" (tránh sticky filter)
      router.replace({
        query: {
          q: newQ || undefined,
          parentCategorySlug: undefined,
          brandSlug: undefined,
          sortBy: undefined,
          page: undefined,
        },
      })
      return
    }
    parentCategorySlug.value = parseNullableString(route.query.parentCategorySlug)
    brandSlug.value = parseNullableString(route.query.brandSlug)
    sortBy.value = parseSort(route.query.sortBy)
    page.value = parsePage(route.query.page)
  },
)

// Lần đầu mount: không fetch brands (chip Hãng đã ẩn trên UI)
</script>

<style scoped>
.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: #555;
  margin-right: 4px;
}
.filter-chip {
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 13px;
  color: #444;
  cursor: pointer;
  transition: all 0.15s ease;
}
.filter-chip:hover {
  border-color: #bbb;
}
.filter-chip.active {
  border-color: #d70018;
  color: #d70018;
  background: #fff5f6;
}
.pagination .page-item.active .page-link {
  background: #d70018;
  border-color: #d70018;
  color: #fff;
}
</style>
