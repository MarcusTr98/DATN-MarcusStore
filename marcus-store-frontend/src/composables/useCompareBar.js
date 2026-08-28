import { reactive, computed } from 'vue'

const state = reactive({
  items: [],     // [{ productId, productName, thumbnailUrl, slug, price }]
  result: null,   // response.result từ BE
  resultProducts: [], // response.products từ BE (khi đã compare)
  loading: false,
  error: null,
})

const MAX_ITEMS = 3
const MIN_ITEMS = 2

function isInCompare(productId) {
  return state.items.some((p) => p.productId === productId)
}

function addToCompare(product) {
  if (!product || product.productId == null) return
  if (isInCompare(product.productId)) return
  if (state.items.length >= MAX_ITEMS) return
  state.items.push({
    productId: product.productId,
    productName: product.productName,
    thumbnailUrl: product.thumbnailUrl || null,
    slug: product.slug || null,
    price: product.price ?? null,
  })
  // Reset kết quả cũ khi thay đổi danh sách
  state.result = null
  state.resultProducts = []
  state.error = null
}

function removeFromCompare(productId) {
  state.items = state.items.filter((p) => p.productId !== productId)
  state.result = null
  state.resultProducts = []
  state.error = null
}

function toggleCompare(product) {
  if (isInCompare(product.productId)) {
    removeFromCompare(product.productId)
  } else {
    addToCompare(product)
  }
}

function clearCompare() {
  state.items = []
  state.result = null
  state.resultProducts = []
  state.error = null
}

const count = computed(() => state.items.length)
const canCompare = computed(() => state.items.length >= MIN_ITEMS)

export function useCompareBar() {
  return {
    state,
    count,
    canCompare,
    isInCompare,
    addToCompare,
    removeFromCompare,
    toggleCompare,
    clearCompare,
    MIN_ITEMS,
    MAX_ITEMS,
  }
}