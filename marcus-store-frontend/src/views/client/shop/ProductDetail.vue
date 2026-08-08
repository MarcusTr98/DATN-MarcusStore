<template>
  <div class="pd-page">
    <!-- Loading -->
    <div v-if="loading" class="pd-page__loading">
      <i class="ti ti-loader-2" aria-hidden="true" />
      <span>Đang tải sản phẩm...</span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="pd-page__error">
      <i class="ti ti-alert-triangle" aria-hidden="true" />
      <p>{{ error }}</p>
      <button type="button" class="pd-page__back-btn" @click="goHome">
        <i class="ti ti-arrow-left" aria-hidden="true" /> Về trang chủ
      </button>
    </div>

    <!-- Detail -->
    <div v-else-if="product" class="container-xxl py-3">
      <!-- Breadcrumb -->
      <nav class="pd-breadcrumb">
        <router-link to="/">Trang chủ</router-link>
        <i class="ti ti-chevron-right" aria-hidden="true" />
        <router-link
          v-if="product.parentCategorySlug"
          :to="`/category/${product.parentCategorySlug}`"
        >
          {{ product.parentCategoryName }}
        </router-link>
        <i v-if="product.parentCategorySlug" class="ti ti-chevron-right" aria-hidden="true" />
        <router-link v-if="product.categorySlug" :to="`/category/${product.categorySlug}`">
          {{ product.categoryName }}
        </router-link>
        <i v-if="product.categorySlug" class="ti ti-chevron-right" aria-hidden="true" />
        <span class="pd-breadcrumb__current">{{ product.productName }}</span>
      </nav>

      <!-- Top: gallery + info panel -->
      <div class="pd-top">
        <div class="pd-top__left">
          <ProductGallery
            :product-name="product.productName"
            :images="product.images"
            :thumbnail-url="product.thumbnailUrl"
            :discount-percent="currentSku?.discountPercent || 0"
          />

          <!-- Policy -->
          <div><PolicyStrip :total-stock="product.totalStock" /></div>
        </div>

        <div class="pd-top__right">
          <ProductInfo
            :product-name="product.productName"
            :brand="product.brand"
            :total-sold="product.totalSold"
            :rating="product.rating || 0"
            :review-count="product.reviewCount || 0"
            :min-price="product.minPrice"
            :max-price="product.maxPrice"
            :min-original-price="product.minOriginalPrice"
            :is-wished="isWished"
            :current-sku="currentSku"
            @toggle-wishlist="toggleWishlist"
          />

          <!-- Variants -->
          <VariantSelector
            v-if="product.skus?.length"
            :skus="product.skus"
            :selected-sku="currentSku"
            @change="onVariantChange"
          />

          <!-- Notification nhỏ khi tổ hợp SKU không tồn tại -->
          <Transition name="fade">
            <div v-if="skuUnavailable" class="pd-variants__unavailable">
              <i class="ti ti-alert-circle" />
              <span>Sản phẩm hiện hết hàng. Vui lòng chọn sản phẩm khác</span>
            </div>
          </Transition>

          <Voucher />

          <!-- Buy actions -->
          <BuyActions
            :max-stock="currentSku?.stockQuantity || 0"
            :in-stock="currentSku?.inStock || false"
            :is-adding-to-cart="isAddingToCart"
            :is-buying="isBuying"
            @add-to-cart="onAddToCart"
            @buy-now="onBuyNow"
          />
        </div>
      </div>

      <!-- Description + specs -->
      <div class="pd-bottom">
        <div class="pd-bottom__left">
          <ProductDescription
            :description="product.description"
            :specifications="product.specifications"
            :current-sku="currentSku"
            :product-name="product.productName"
            :brand="product.brand"
            :total-skus="product.totalSkus"
            :total-stock="product.totalStock"
          />
        </div>

        <div class="pd-bottom__right">
          <ProductSuggestions />
        </div>
      </div>

      <!-- Reviews (panel riêng bên dưới) -->
      <div class="pd-reviews-section">
        <ProductReviews
          :product-name="product.productName"
          :rating="product.rating || 0"
          :review-count="product.reviewCount || 0"
          :review-distribution="product.reviewDistribution || []"
        />
      </div>
    </div>

    <!-- Notify Modal -->
    <BaseModal
      :visible="notifyModal.visible"
      :type="notifyModal.type"
      :title="notifyModal.title"
      :message="notifyModal.message"
      @close="notifyModal.visible = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import { useCartStore } from '@/stores/cartStore'
import wishlist, { state as wishlistState } from '@/composables/useWishlistShared'
import BaseModal from '@/components/BaseModal.vue'

import ProductGallery from '@/layouts/product-detail/ProductGallery.vue'
import ProductInfo from '@/layouts/product-detail/ProductInfo.vue'
import VariantSelector from '@/layouts/product-detail/VariantSelector.vue'
import PolicyStrip from '@/layouts/product-detail/PolicyStrip.vue'
import ProductDescription from '@/layouts/product-detail/ProductDescription.vue'
import ProductReviews from '@/layouts/product-detail/ProductReviews.vue'
import BuyActions from '@/layouts/product-detail/BuyActions.vue'
import ProductSuggestions from '@/layouts/product-detail/ProductSuggestions.vue'
import Voucher from '@/layouts/product-detail/Voucher.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const product = ref(null)
const loading = ref(false)
const error = ref(null)

// Bộ chọn variant hiện tại
const currentSku = ref(null)
const isAddingToCart = ref(false)
const isBuying = ref(false)

// Notification nhỏ khi tổ hợp SKU không tồn tại
const skuUnavailable = ref(false)
let skuUnavailableTimer = null

const notifyModal = ref({ visible: false, type: 'info', title: '', message: '' })

function showNotify(type, title, message) {
  notifyModal.value = { visible: true, type, title, message }
}

function showSkuUnavailable() {
  skuUnavailable.value = true
  if (skuUnavailableTimer) clearTimeout(skuUnavailableTimer)
  skuUnavailableTimer = setTimeout(() => {
    skuUnavailable.value = false
  }, 2500)
}

// ===== Tìm SKU đầu tiên =====
const firstSku = computed(() => {
  if (!product.value?.skus?.length) return null
  const skus = product.value.skus
  return skus.find((s) => s.isActive && s.inStock) || skus.find((s) => s.isActive) || skus[0] || null
})

// ===== Fetch =====
async function fetchProduct() {
  const slug = route.params.slug
  if (!slug) return

  loading.value = true
  error.value = null
  product.value = null
  currentSku.value = null

  // Đảm bảo share state đã có wishlist ids (nếu user đã đăng nhập) trước khi bind UI
  if (localStorage.getItem('ACCESS_TOKEN')) {
    await wishlist.fetchIds()
  }

  try {
    const res = await api.get(`/client/products/${slug}`)
    product.value = res.data?.data ?? null

    // Set SKU mặc định
    currentSku.value = firstSku.value

    // Đồng bộ isWished từ BE vào share state
    if (product.value?.productId != null && product.value.isWished === true) {
      wishlistState.productIds.add(product.value.productId)
    }
  } catch (err) {
    console.error('Lỗi tải chi tiết sản phẩm:', err)
    error.value =
      err.response?.data?.message ||
      err.response?.data?.data ||
      'Không thể tải thông tin sản phẩm. Vui lòng thử lại sau.'
  } finally {
    loading.value = false
  }
}

// ===== Variants =====
function onVariantChange({ attributeId, valueId }) {
  if (!product.value?.skus?.length || !currentSku.value) return

  // Lấy tất cả attributeIds của SKU hiện tại
  const currentAttrIds = (currentSku.value.attributeValues || []).map((av) => av.attributeId)

  // Nếu user click vào attribute đã có trong SKU hiện tại → thay đổi trực tiếp
  const existingAttr = (currentSku.value.attributeValues || []).find((av) => av.attributeId === attributeId)

  if (existingAttr) {
    // Tạo bản đồ attributeValue mới
    const newAttrValues = currentSku.value.attributeValues.map((av) =>
      av.attributeId === attributeId ? { ...av, valueId, valueString: existingAttr.valueString } : av,
    )
    // Tìm SKU khớp với attributeValues mới
    const match = product.value.skus.find((sku) =>
      sku.isActive && (sku.attributeValues || []).length === newAttrValues.length &&
      newAttrValues.every((nav) =>
        sku.attributeValues.some((sav) => sav.attributeId === nav.attributeId && sav.valueId === nav.valueId),
      ),
    )

    if (match) {
      currentSku.value = match
    } else {
      showSkuUnavailable()
    }
  } else {
    // User click attribute mới (chưa có trong SKU hiện tại)
    // Tạo attributeValues mới bao gồm attribute cũ + attribute mới
    const newAttrValues = [
      ...(currentSku.value.attributeValues || []).map((av) => ({ attributeId: av.attributeId, valueId: av.valueId })),
      { attributeId, valueId },
    ]

    // Tìm SKU khớp với TẤT CẢ attributeValues mới
    const match = product.value.skus.find((sku) => {
      if (!sku.isActive) return false
      if ((sku.attributeValues || []).length !== newAttrValues.length) return false
      return newAttrValues.every((nav) =>
        sku.attributeValues.some((sav) => sav.attributeId === nav.attributeId && sav.valueId === nav.valueId),
      )
    })

    if (match) {
      currentSku.value = match
    } else {
      showSkuUnavailable()
    }
  }
}

// ===== Wishlist =====
const isWished = computed(() =>
  product.value?.productId != null ? wishlist.isWished(product.value.productId) : false,
)

async function toggleWishlist() {
  if (!product.value?.productId) return
  const token = localStorage.getItem('ACCESS_TOKEN')
  if (!token) {
    showNotify('info', 'Yêu cầu đăng nhập', 'Vui lòng đăng nhập để sử dụng tính năng yêu thích.')
    return
  }
  const result = await wishlist.toggle(product.value.productId)
  if (!result.success) {
    showNotify('error', 'Thao tác thất bại', result.message || 'Không thể cập nhật yêu thích.')
    return
  }
  showNotify(
    'success',
    isWished.value ? 'Đã thêm vào yêu thích' : 'Đã bỏ yêu thích',
    isWished.value
      ? `Sản phẩm "${product.value.productName}" đã được thêm vào danh sách yêu thích.`
      : `Sản phẩm "${product.value.productName}" đã được bỏ khỏi yêu thích.`,
  )
}

// ===== Add to cart =====
async function onAddToCart(quantity) {
  const sku = currentSku.value
  if (!sku || !sku.inStock) {
    showNotify('error', 'Không thể thêm', 'Sản phẩm hiện đang hết hàng.')
    return
  }
  isAddingToCart.value = true
  try {
    const ok = await cartStore.addToCart(sku.skuId, quantity)
    if (ok) {
      showNotify(
        'success',
        'Đã thêm vào giỏ hàng',
        `${product.value.productName} (x${quantity}) đã được thêm vào giỏ.`,
      )
    } else {
      showNotify('error', 'Thêm thất bại', cartStore.error || 'Thêm vào giỏ hàng thất bại.')
    }
  } finally {
    isAddingToCart.value = false
  }
}

// ===== Buy now =====
async function onBuyNow(quantity) {
  const sku = currentSku.value
  if (!sku || !sku.inStock) {
    showNotify('error', 'Không thể mua', 'Sản phẩm hiện đang hết hàng.')
    return
  }
  isBuying.value = true
  try {
    // Thêm vào giỏ trước
    const ok = await cartStore.addToCart(sku.skuId, quantity)
    if (ok) {
      // Lấy cartItemId của item vừa thêm (cartItem là item có skuId khớp trong cart)
      const addedItem = cartStore.items.find((i) => i.skuId === sku.skuId)
      const cartItemId = addedItem?.cartItemId
      if (cartItemId) {
        // Marcus thêm: lưu quantity Buy Now riêng (đã chốt số lượng user chọn).
        // Nếu SP đã tồn tại trong giỏ, backend có thể cộng dồn quantity → cartItem
        // thật trong DB sẽ có quantity > quantity Buy Now. Checkout sẽ dùng quantity
        // Buy Now tại đây để KHÔNG lấy nhầm số lượng từ cart hiện tại.
        sessionStorage.setItem(
          'buyNowCartItemIds',
          JSON.stringify([cartItemId]),
        )
        sessionStorage.setItem(
          'buyNowCartItemQuantities',
          JSON.stringify({
            [cartItemId]: quantity,
          }),
        )
      }
      // Đi thẳng sang /checkout
      router.push('/checkout')
    } else {
      showNotify('error', 'Mua thất bại', cartStore.error || 'Không thể tiến hành mua ngay.')
    }
  } finally {
    isBuying.value = false
  }
}

function goHome() {
  router.push('/')
}

onMounted(fetchProduct)

// Khi đổi route -> load lại
watch(() => route.params.slug, fetchProduct)
</script>

<style scoped>
.pd-page {
  background: #f8f9fa;
  min-height: 60vh;
}

/* Loading + Error */
.pd-page__loading,
.pd-page__error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 50vh;
  font-size: 15px;
  color: #555;
  padding: 40px 20px;
}
.pd-page__loading i {
  font-size: 36px;
  color: #e11d1d;
  animation: pd-spin 1s linear infinite;
}
.pd-page__error i {
  font-size: 48px;
  color: #e11d1d;
}
.pd-page__error p {
  font-size: 15px;
  color: #333;
  text-align: center;
  max-width: 480px;
}
.pd-page__back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: #e11d1d;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.pd-page__back-btn:hover {
  background: #c0392b;
}

@keyframes pd-spin {
  to {
    transform: rotate(360deg);
  }
}

/* Breadcrumb */
.pd-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #555;
  margin-bottom: 16px;
  padding: 10px 0;
}
.pd-breadcrumb a {
  color: #555;
  text-decoration: none;
}
.pd-breadcrumb a:hover {
  color: #e11d1d;
}
.pd-breadcrumb i {
  font-size: 12px;
  color: #bbb;
}
.pd-breadcrumb__current {
  color: #1a1a1a;
  font-weight: 500;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Top section: gallery + info */
.pd-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.05fr);
  gap: 28px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 24px;
}

.pd-top__left,
.pd-top__right {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.pd-reviews-section {
  margin-top: 20px;
}

@media (max-width: 992px) {
  .pd-top {
    grid-template-columns: 1fr;
    padding: 16px;
  }
  .pd-breadcrumb {
    padding: 8px 4px;
  }
}

.pd-bottom {
  margin-top: 20px;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

@media (max-width: 992px) {
  .pd-bottom {
    grid-template-columns: 1fr;
  }
}

/* Notification nhỏ khi tổ hợp SKU không tồn tại */
.pd-variants__unavailable {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fff3cd;
  border: 1px solid #ffc107;
  border-radius: 6px;
  font-size: 13px;
  color: #856404;
  margin-top: -8px;
}
.pd-variants__unavailable i {
  font-size: 16px;
  flex-shrink: 0;
}

/* Transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
