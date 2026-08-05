import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'

const MAX = 10
const MAX_BEST_SELLERS = 8
const GUEST_KEY = 'marcus:search_history:guest'

function currentKey() {
  if (typeof window === 'undefined') return GUEST_KEY
  const username = localStorage.getItem('USERNAME')
  return username ? `marcus:search_history:user:${username}` : GUEST_KEY
}

function readHistory() {
  try {
    return JSON.parse(localStorage.getItem(currentKey())) || []
  } catch {
    return []
  }
}

function writeHistory(list) {
  localStorage.setItem(currentKey(), JSON.stringify(list.slice(0, MAX)))
}

export const searchApi = {
  bestSellers(limit = 8) {
    return api.get('/client/products/best-sellers', {
      params: { limit },
      skipGlobalLoading: true,
    })
  },
  suggest(q, limit = 8) {
    return api.get('/client/products/search/suggest', {
      params: { q, limit },
      skipGlobalLoading: true,
    })
  },
  search(q, opts = {}) {
    const {
      parentCategorySlug = null,
      brandSlug = null,
      sortBy = 'price_desc',
      page = 0,
      size = 12,
    } = opts
    return api.get('/client/products/search', {
      params: {
        q,
        parentCategorySlug: parentCategorySlug == null ? undefined : parentCategorySlug,
        brandSlug: brandSlug == null ? undefined : brandSlug,
        sortBy,
        page,
        size,
      },
    })
  },
}

export function useSearchBox() {
  const router = useRouter()
  const query = ref('')
  const showPanel = ref(false)
  const isLoading = ref(false)
  const suggestions = ref([])
  const history = ref([])
  const isTyping = computed(() => !!query.value.trim())

  function loadHistory() {
    history.value = readHistory()
  }

  function pushHistory(keyword) {
    const k = (keyword || '').trim()
    if (!k) return
    const next = [k, ...history.value.filter((x) => x !== k)].slice(0, MAX)
    writeHistory(next)
    history.value = next
  }

  function removeHistory(keyword) {
    writeHistory(history.value.filter((x) => x !== keyword))
    history.value = history.value.filter((x) => x !== keyword)
  }

  function clearHistory() {
    writeHistory([])
    history.value = []
  }

  async function fetchBestSellers() {
    isLoading.value = true
    try {
      const { data } = await searchApi.bestSellers(MAX_BEST_SELLERS)
      const list = data?.data || []
      suggestions.value = Array.isArray(list) ? list.slice(0, MAX_BEST_SELLERS) : []
    } catch (e) {
      console.warn('bestSellers error', e)
      suggestions.value = []
    } finally {
      isLoading.value = false
    }
  }

  async function fetchSuggest(q) {
    isLoading.value = true
    try {
      const { data } = await searchApi.suggest(q, 8)
      suggestions.value = data?.data || []
    } catch (e) {
      console.warn('suggest error', e)
      suggestions.value = []
    } finally {
      isLoading.value = false
    }
  }

  let debounceId = null
  watch(query, (val) => {
    if (debounceId) clearTimeout(debounceId)
    if (!val.trim()) {
      fetchBestSellers()
      return
    }
    debounceId = setTimeout(() => fetchSuggest(val), 300)
  })

  async function openPanel() {
    showPanel.value = true
    if (history.value.length === 0) loadHistory()
    if (!isTyping.value) {
      suggestions.value = []
      await fetchBestSellers()
    }
  }

  function closePanel() {
    showPanel.value = false
  }

  async function submit(keyword) {
    const k = (keyword ?? query.value).trim()
    if (!k) return
    pushHistory(k)
    closePanel()
    router.push({ name: 'Search', query: { q: k } })
  }

  function refreshOnAuth() {
    function reload() {
      history.value = readHistory()
    }
    window.addEventListener('auth-changed', reload)
    return () => window.removeEventListener('auth-changed', reload)
  }

  return {
    query,
    showPanel,
    isLoading,
    suggestions,
    history,
    isTyping,
    openPanel,
    closePanel,
    loadHistory,
    pushHistory,
    removeHistory,
    clearHistory,
    submit,
    fetchBestSellers,
    refreshOnAuth,
  }
}