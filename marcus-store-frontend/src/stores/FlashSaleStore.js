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
    quantityFlashSaleSlot: Number(slot.quantityFlashSaleSlot ?? 0),
    imageUrl: slot.imageUrl || slot.bannerUrl || null,
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

    /**
     * Lấy cây brand -> categoryL2 -> sku từ BE để admin chọn sản phẩm
     * cho Flash Sale.
     * @param {Object} options
     * @param {boolean} options.includeOutOfStock mặc định false.
     */
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
  },
})
