// Pinia store cho Flash Sale - cả Admin và Client dùng chung.
// Admin CRUD: fetchSlots / fetchOneSlot / addSlot / updateSlot / deleteSlotById / toggleSlotStatus
// Admin khác: fetchCascade / fetchOverlap
// Client storefront: fetchClientSlots + getter primarySlot / displaySlots

import { defineStore } from 'pinia'
import flashSaleApi from '@/api/FlashSaleApi.js'


 // map data từ BE sang FE

function formatHm(iso) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return ''
    const hh = String(d.getHours()).padStart(2, '0')
    const mm = String(d.getMinutes()).padStart(2, '0')
    return `${hh}:${mm}`
  } catch {
    return ''
  }
}

function mapSlot(slot) {
  return {
    slotId: slot.slotId,
    name: slot.name,
    startDate: slot.startDate || null,
    endDate: slot.endDate || null,
    status: Number(slot.status),
    quantityFlashSaleSlot: Number(slot.quantityFlashSaleSlot ?? 0),
    usedQuantity: Number(slot.usedQuantity ?? 0),
    bannerImageUrl: slot.bannerImageUrl || null,
    items: Array.isArray(slot.items) ? slot.items : [],
    createdAt: slot.createdAt || null,
    updatedAt: slot.updatedAt || null,
  }
}

// === Client side (public) — map cho storefront ===

// Chuẩn hoá URL ảnh:
//  - Nếu BE trả relative path dạng "/images/..." (ảnh sản phẩm đã được upload lên Cloudinary
//    nhưng DB đang giữ path cũ) → map sang URL Cloudinary tương ứng.
//  - Ngược lại, nếu là http/https → dùng luôn.
//  - Các relative path khác (vd "/uploads/x.jpg") → ghép domain BE như cũ.
// Lý do: BE không serve folder /images/ nên trả 404, browser block bằng ORB.
const CLOUDINARY_CLOUD_NAME = 'dyeb3lju6'
const CLOUDINARY_BASE = `https://res.cloudinary.com/${CLOUDINARY_CLOUD_NAME}/image/upload/marcus-store`

const BE_RESOURCE_ORIGIN = (() => {
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  // Cắt "/api" cuối nếu có để ra origin
  return base.replace(/\/api\/?$/, '')
})()

function resolveImageUrl(url) {
  if (!url || typeof url !== 'string') return null
  const trimmed = url.trim()
  if (!trimmed) return null
  // Đã là http/https → dùng luôn
  if (/^https?:\/\//i.test(trimmed)) return trimmed
  // Relative path "/images/..." → redirect sang Cloudinary (giữ nguyên tên file + đuôi).
  // BE không serve folder này; DB vẫn lưu path cũ nên ta rewrite phía FE.
  if (/^\/?images\//i.test(trimmed)) {
    const filename = trimmed.replace(/^\/?images\//i, '')
    return `${CLOUDINARY_BASE}/${filename}`
  }
  // Relative path khác → ghép domain BE
  const path = trimmed.startsWith('/') ? trimmed : `/${trimmed}`
  return `${BE_RESOURCE_ORIGIN}${path}`
}

function mapClientSlot(slot) {
  if (!slot) return null
  const items = Array.isArray(slot.items) ? slot.items : []
  return {
    slotId: slot.slotId,
    name: slot.name,
    bannerImageUrl: resolveImageUrl(slot.bannerImageUrl),
    startDate: slot.startDate || null,
    endDate: slot.endDate || null,
    status: Number(slot.status ?? 0),
    items: items.map((it) => ({
      skuId: it.skuId,
      productId: it.productId,
      productName: it.productName,
      skuCode: it.skuCode,
      skuImageUrl: resolveImageUrl(it.skuImageUrl),
      originalPrice: Number(it.originalPrice ?? 0),
      flashSalePrice: Number(it.flashSalePrice ?? 0),
      flashSaleQuantity: Number(it.flashSaleQuantity ?? 0),
      soldQuantity: Number(it.soldQuantity ?? 0),
      remainingQuantity: Number(it.remainingQuantity ?? 0),
      // Tính discountPercent ở FE, không cần BE trả thêm field
      discountPercent:
        Number(it.originalPrice) > 0
          ? Math.floor(
              ((Number(it.originalPrice) - Number(it.flashSalePrice)) /
                Number(it.originalPrice)) *
                100,
            )
          : 0,
    })),
  }
}

// Map lỗi theo từng field để hiển thị dưới input
function mapFieldErrors(errors = {}) {
  return Object.fromEntries(
    Object.entries({
      name: errors.name,
      startDate: errors.startDate,
      endDate: errors.endDate,
    }).filter(([, message]) => Boolean(message))
  )
}

function mapMessageToFieldError(message = '') {
  if (!message) return {}

  if (message.includes('tên chiến dịch') || message.includes('Tên chiến dịch')) {
    return { name: message }
  }
  if (message.includes('thời gian bắt đầu') || message.includes('Thời gian bắt đầu')) {
    return { startDate: message }
  }
  if (message.includes('thời gian kết thúc') || message.includes('Thời gian kết thúc')) {
    return { endDate: message }
  }
  return {}
}
// Rút error message từ axios error

function getErrorMessage(error) {
  const message = error.response?.data?.message || error.response?.data?.error
  if (typeof message === 'string') return message
  return ''
}

// Tự tính stats khi BE chưa có endpoint

function buildFallbackStats(slots = [], totalElements = 0) {
  const now = new Date()
  let active = 0
  let upcoming = 0
  let totalProducts = 0

  slots.forEach((slot) => {
    const start = slot.startDate ? new Date(slot.startDate) : null
    const end = slot.endDate ? new Date(slot.endDate) : null

    if (slot.status === 2 && start && end && start <= now && end >= now) {
      active++
    } else if (slot.status === 1 && start && start > now) {
      upcoming++
    }

    totalProducts += Number(slot.quantityFlashSaleSlot ?? 0)
  })

  return {
    total: totalElements || slots.length,
    active,
    upcoming,
    totalProducts,
  }
}

export const useFlashSaleStore = defineStore('flashSale', {
  state: () => ({
    slots: [],
    selectedSlot: null,
    loading: false,
    error: null,
    fieldErrors: {},
    pagination: {
      page: 0,
      size: 10,
      totalPages: 0,
      totalElements: 0,
    },
    stats: {
      total: 0,
      active: 0,
      upcoming: 0,
      totalProducts: 0,
    },
    // Cây sản phẩm dùng trong modal tạo/sửa Flash Sale:
    // [{ brand, categories:[{ categoryId, categoryName, skus:[...] }] }]
    cascadeTree: [],
    cascadeLoading: false,
    cascadeError: null,

    // === Client side (public storefront) ===
    clientSlots: [],           // danh sách slot ACTIVE + SCHEDULED cho /khuyen-mai và home
    clientLoading: false,
  }),

  getters: {
    // Slot featured cho trang chủ: ACTIVE đầu tiên (BE đã sort ACTIVE trước theo startDate ASC).
    // Fallback về slot đầu tiên nếu không có ACTIVE (ví dụ chỉ có SCHEDULED).
    featuredSlot(state) {
      if (!Array.isArray(state.clientSlots) || state.clientSlots.length === 0) return null
      return (
        state.clientSlots.find((s) => Number(s.status) === 2) ||
        state.clientSlots[0] ||
        null
      )
    },
    // Slot ưu tiên cho countdown: ưu tiên ACTIVE trước, rồi SCHEDULED.
    // Tương đương featuredSlot nhưng semantic rõ ràng cho bộ đếm ngược.
    primarySlot(state) {
      if (!Array.isArray(state.clientSlots) || state.clientSlots.length === 0) return null
      return (
        state.clientSlots.find((s) => Number(s.status) === 2) ||
        state.clientSlots.find((s) => Number(s.status) === 1) ||
        null
      )
    },
    // Chuẩn bị dữ liệu cho thanh timeline trên FlashSalePage.vue.
    // Trả về tối đa 4 entry: slot đang diễn ra (live) + các slot sắp diễn ra tiếp theo.
    displaySlots(state) {
      if (!Array.isArray(state.clientSlots) || state.clientSlots.length === 0) return []
      const list = []
      const live = state.clientSlots.find((s) => Number(s.status) === 2)
      if (live) {
        list.push({
          slotId: live.slotId,
          isLive: true,
          time: `Đang diễn ra · ${formatHm(live.startDate)}–${formatHm(live.endDate)}`,
          startDate: live.startDate,
          endDate: live.endDate,
        })
      }
      const upcomings = state.clientSlots
        .filter((s) => Number(s.status) === 1)
        .slice(0, 3)
      upcomings.forEach((s) => {
        list.push({
          slotId: s.slotId,
          isLive: false,
          time: `${formatHm(s.startDate)}`,
          startDate: s.startDate,
          endDate: s.endDate,
        })
      })
      return list.slice(0, 4)
    },
    // Thống kê nhanh cho banner: tổng SP, % giảm cao nhất, label trạng thái.
    bannerStats(state) {
      const slots = Array.isArray(state.clientSlots) ? state.clientSlots : []
      let totalProducts = 0
      let maxDiscount = 0
      let hasLive = false
      let hasUpcoming = false
      for (const slot of slots) {
        const items = Array.isArray(slot.items) ? slot.items : []
        for (const it of items) {
          totalProducts += 1
          const d = Number(it.discountPercent ?? 0)
          if (d > maxDiscount) maxDiscount = d
        }
        if (Number(slot.status) === 2) hasLive = true
        if (Number(slot.status) === 1) hasUpcoming = true
      }
      const liveLabel = hasLive ? 'ĐANG CHÁY' : (hasUpcoming ? 'SẮP DIỄN RA' : '24h')
      const liveSubLabel = hasLive ? 'Đến lúc mua' : (hasUpcoming ? 'Chuẩn bị sẵn' : 'Chỉ hôm nay')
      return {
        totalProducts: totalProducts > 0 ? `${totalProducts}+` : '200+',
        maxDiscount: maxDiscount > 0 ? `${maxDiscount}%` : '50%',
        liveLabel,
        liveSubLabel,
      }
    },
  },

  actions: {
    async fetchSlots(params = {}) {
      try {
        this.loading = true
        this.error = null

        const res = await flashSaleApi.getAllFlashSaleSlots(params)
        const pageData = res.data

        this.slots = (pageData.content || []).map(mapSlot)
        this.pagination = {
          page: pageData.number || 0,
          size: pageData.size || params.size || 10,
          totalPages: pageData.totalPages || 0,
          totalElements: pageData.totalElements || 0,
        }

        // Gọi thêm stats (giống pattern voucher)
        try {
          const statsRes = await flashSaleApi.getFlashSaleStats({
            keyword: params.keyword,
            status: params.status,
          })
          const data = statsRes.data
          this.stats = {
            total: data.totalSlots ?? 0,
            active: data.activeSlots ?? 0,
            upcoming: data.upcomingSlots ?? 0,
            totalProducts: data.totalActiveProducts ?? 0,
          }
        } catch (statsError) {
          console.warn('Stats endpoint lỗi, dùng fallback:', statsError)
          this.stats = buildFallbackStats(this.slots, pageData.totalElements || 0)
        }

        return true
      } catch (error) {
        console.error('có lỗi ở fetchSlots: ', error)
        this.slots = []
        this.error =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'Không thể tải danh sách flash sale'
        return false
      } finally {
        this.loading = false
      }
    },

    async fetchOneSlot(slotId) {
      try {
        this.loading = true
        this.error = null

        const res = await flashSaleApi.getOneFlashSaleSlot(slotId)
        this.selectedSlot = mapSlot(res.data)

        return this.selectedSlot
      } catch (error) {
        console.error('lỗi fetchOneSlot: ', error)
        this.selectedSlot = null
        this.error =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'Không thể tải chi tiết flash sale'
        return null
      } finally {
        this.loading = false
      }
    },

    async addSlot(payload) {
      try {
        this.loading = true
        this.error = null
        this.fieldErrors = {}

        const response = await flashSaleApi.createFlashSaleSlot(payload)
        this.slots.unshift(mapSlot(response.data))
        this.stats = buildFallbackStats(this.slots, this.pagination.totalElements + 1)

        return true
      } catch (error) {
        console.error('lỗi addSlot:', error)
        const message = getErrorMessage(error)

        this.fieldErrors = {
          ...mapFieldErrors(error.response?.data?.data),
          ...mapMessageToFieldError(message),
        }

        this.error = message || 'Không thể thêm flash sale'
        return false
      } finally {
        this.loading = false
      }
    },

    async updateSlot(slotId, payload) {
      try {
        this.loading = true
        this.error = null
        this.fieldErrors = {}

        const response = await flashSaleApi.updateFlashSaleSlot(slotId, payload)
        const updated = mapSlot(response.data)
        const index = this.slots.findIndex((s) => s.slotId === slotId)

        if (index !== -1) {
          this.slots[index] = updated
        }

        return true
      } catch (error) {
        console.error('lỗi updateSlot:', error)
        const message = getErrorMessage(error)

        this.fieldErrors = {
          ...mapFieldErrors(error.response?.data?.data),
          ...mapMessageToFieldError(message),
        }

        this.error = message || 'Không thể cập nhật flash sale'
        return false
      } finally {
        this.loading = false
      }
    },

    async deleteSlotById(slotId) {
      try {
        this.loading = true
        this.error = null

        await flashSaleApi.deleteFlashSaleSlot(slotId)

        this.slots = this.slots.filter((s) => s.slotId !== slotId)
        this.stats = buildFallbackStats(this.slots, this.pagination.totalElements - 1)

        return true
      } catch (error) {
        console.error('lỗi deleteSlotById:', error)
        this.error =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'Không thể xóa flash sale'
        return false
      } finally {
        this.loading = false
      }
    },

    async toggleSlotStatus(slotId, status) {
      try {
        this.loading = true
        this.error = null

        // BE trả về FlashSaleResponse sau khi đổi status, dùng để sync local state
        const res = await flashSaleApi.toggleFlashSaleSlot(slotId, status)
        const updated = mapSlot(res.data)

        const index = this.slots.findIndex((s) => s.slotId === slotId)
        if (index !== -1) {
          this.slots[index] = updated
        }

        return true
      } catch (error) {
        console.error('lỗi toggleSlotStatus:', error)
        this.error =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'Không thể thay đổi trạng thái flash sale'
        return false
      } finally {
        this.loading = false
      }
    },

    async restoreFlashSale(slotId) {
      try {
        this.loading = true
        this.error = null

        const res = await flashSaleApi.restoreFlashSale(slotId)
        const restored = mapSlot(res.data)

        const index = this.slots.findIndex((s) => s.slotId === slotId)
        if (index !== -1) {
          this.slots[index] = restored
        }

        return true
      } catch (error) {
        console.error('lỗi restoreFlashSale:', error)
        this.error =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'Không thể khôi phục flash sale'
        return false
      } finally {
        this.loading = false
      }
    },


     // Lấy cây brand -> categoryL2 -> sku từ BE để admin chọn sản phẩm

    async fetchCascade({ includeOutOfStock = false } = {}) {
      this.cascadeLoading = true
      this.cascadeError = null
      try {
        const res = await flashSaleApi.getProductCascade({ includeOutOfStock })
        // res.data là ApiResponse: { code, message, data: [...] }
        this.cascadeTree = res.data?.data || []
        return this.cascadeTree
      } catch (error) {
        console.error('lỗi fetchCascade:', error)
        this.cascadeError =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'Không thể tải cây sản phẩm'
        this.cascadeTree = []
        return []
      } finally {
        this.cascadeLoading = false
      }
    },

    // Kiểm tra khung giờ mới nhập có đang đụng flash sale khác không.
    // Trả về danh sách slot bị overlap (rỗng = OK để tạo).
    // excludeSlotId để bỏ qua chính nó khi đang sửa.
    async fetchOverlap({ startDate, endDate, excludeSlotId = null } = {}) {
      try {
        const res = await flashSaleApi.checkOverlap({
          startDate,
          endDate,
          excludeSlotId,
        })
        return res.data || []
      } catch (error) {
        console.error('lỗi fetchOverlap:', error)
        return []
      }
    },

    // === Client side (public storefront) ===

     // Tải danh sách slot ACTIVE + SCHEDULED còn hiệu lực cho trang client.


    async fetchClientSlots(limit = 20) {
      this.clientLoading = true
      try {
        const res = await flashSaleApi.getActiveAndUpcoming(limit)
        // Có 2 dạng response có thể gặp:
        //   1. Array trực tiếp: res.data = [...]
        //   2. ApiResponse envelope: res.data = { code, message, data: [...] }
        const raw = res.data?.data ?? res.data ?? []
        const list = Array.isArray(raw)
          ? raw.map(mapClientSlot).filter(Boolean)
          : []
        this.clientSlots = list
        if (list.length > 0) {
          const firstItem = list[0]?.items?.[0]
          console.log(
            `[FlashSaleStore] fetchClientSlots: ${list.length} slot(s). Featured items: ${list[0]?.items?.length || 0}. First item image URL =`,
            firstItem?.skuImageUrl || '(empty)',
          )
        } else {
          console.warn('[FlashSaleStore] fetchClientSlots: 0 slot(s) returned')
        }
        return this.clientSlots
      } catch (error) {
        console.error('[FlashSaleStore] lỗi fetchClientSlots:', error)
        this.clientSlots = []
        return []
      } finally {
        this.clientLoading = false
      }
    },
  },
})
