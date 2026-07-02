import { defineStore } from 'pinia'
import flashSaleApi from '@/api/flashSaleApi.js'


 // map data từ BE sang FE

function mapSlot(slot) {
  return {
    slotId: slot.slotId,
    name: slot.name,
    startDate: slot.startDate || null,
    endDate: slot.endDate || null,
    status: Number(slot.status),
    items: (slot.items || []).map((it) => ({
      skuId: it.skuId,
      productName: it.productName,
      originalPrice: Number(it.originalPrice ?? 0),
      flashSalePrice: Number(it.flashSalePrice ?? 0),
      flashSaleQuantity: Number(it.flashSaleQuantity ?? 0),
      soldQuantity: Number(it.soldQuantity ?? 0),
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

    totalProducts += (slot.items || []).length
  })

  return {
    total: totalElements || slots.length,
    active,
    upcoming,
    totalProducts, // gồm cả sold
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
  }),

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
          // #region DEBUG_LOG
          console.log('[DEBUG_STATS] fetchSlots params:', params)
          fetch('http://127.0.0.1:7828/ingest/6683b584-65cf-4bee-bf05-7d2708749dc3', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Debug-Session-Id': '41c373' },
            body: JSON.stringify({
              sessionId: '41c373',
              location: 'FlashSaleStore.js:126',
              message: 'fetchSlots called with params',
              data: { params },
              timestamp: Date.now()
            })
          }).catch(() => {})
          // #endregion
          const statsRes = await flashSaleApi.getFlashSaleStats(params)
          // #region DEBUG_LOG
          console.log('[DEBUG_STATS] statsRes:', statsRes.data)
          fetch('http://127.0.0.1:7828/ingest/6683b584-65cf-4bee-bf05-7d2708749dc3', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Debug-Session-Id': '41c373' },
            body: JSON.stringify({
              sessionId: '41c373',
              location: 'FlashSaleStore.js:136',
              message: 'stats API response data',
              data: { rawResponse: statsRes.data },
              timestamp: Date.now()
            })
          }).catch(() => {})
          // #endregion
          const data = statsRes.data || {}
          // Map từ key BE sang key FE cho thống nhất
          // #region DEBUG_LOG
          console.log('[DEBUG_STATS] mapped stats:', {
            total: data.totalSlots ?? 0,
            active: data.activeSlots ?? 0,
            upcoming: data.upcomingSlots ?? 0,
            totalProducts: data.totalActiveProducts ?? 0,
          })
          // #endregion
          this.stats = {
            total: data.totalSlots ?? 0,
            active: data.activeSlots ?? 0,
            upcoming: data.upcomingSlots ?? 0,
            totalProducts: data.totalActiveProducts ?? 0,
          }
          // #region DEBUG_LOG
          console.log('[DEBUG_STATS] this.stats after set:', JSON.parse(JSON.stringify(this.stats)))
          // #endregion
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

        await flashSaleApi.toggleFlashSaleSlot(slotId, status)

        const index = this.slots.findIndex((s) => s.slotId === slotId)
        if (index !== -1) {
          this.slots[index] = { ...this.slots[index], status }
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
  },
})
