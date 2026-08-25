import { defineStore } from 'pinia'
import voucherApi from '@/api/voucherApi.js'

function formatDateTimeLocal(value) {
  if (!value) {
    return ''
  }

  return String(value).slice(0, 16)
}

function mapVoucher(voucher) {
  return {
    voucherId: voucher.voucherId,
    voucherCode: voucher.voucherCode,
    discountValue: Number(voucher.discountValue || 0),
    discountType: voucher.discountType,
    maxDiscountAmount: voucher.maxDiscountAmount != null ? Number(voucher.maxDiscountAmount) : null,
    minOrderValue: voucher.minOrderValue != null ? Number(voucher.minOrderValue) : null,
    startDate: voucher.startDate || null,
    endDate: voucher.endDate || null,
    quantity: Number(voucher.quantity || 0),
    status: voucher.status || 'ACTIVE', // 'ACTIVE', 'INACTIVE', 'SCHEDULED'
    // Đối tượng sử dụng
    targetType: voucher.targetType || 'ALL',
    targetUserIds: voucher.targetUserIds || [],
    targetUserCount: voucher.targetUserCount || null,
  }
}

function mapFieldErrors(errors) {
  if (!errors || typeof errors !== 'object') {
    return {}
  }

  const mappedErrors = {
    voucher_code: errors.voucherCode,
    discount_value: errors.discountValue,
    discount_type: errors.discountType,
    max_discount_amount: errors.maxDiscountAmount,
    min_order_value: errors.minOrderValue,
    start_date: errors.startDate,
    end_date: errors.endDate,
  }

  return Object.fromEntries(
    Object.entries(mappedErrors).filter(([, message]) => Boolean(message))
  )
}

function mapMessageToFieldError(message = '') {
  if (!message) {
    return {}
  }

  if (message.includes('ngày bắt đầu')) {
    return { start_date: message }
  }

  if (message.includes('ngày kết thúc')) {
    return { end_date: message }
  }

  if (message.includes('giảm tối đa')) {
    return { max_discount_amount: message }
  }

  if (message.includes('Giá trị giảm') || message.includes('giá trị giảm')) {
    return { discount_value: message }
  }

  if (message.includes('Loại giảm giá') || message.includes('loại giảm giá')) {
    return { discount_type: message }
  }

  if (message.includes('Mã voucher') || message.includes('mã voucher')) {
    return { voucher_code: message }
  }

  return {}
}

function getErrorMessage(error) {
  const message = error.response?.data?.message || error.response?.data?.error

  if (typeof message === 'string') {
    return message.replace(/^\d+\s+BAD_REQUEST\s+"?/, '').replace(/"$/, '')
  }

  return ''
}

function buildFallbackStats(vouchers = [], totalElements = 0) {
  return {
    total: totalElements,
    active: vouchers.filter((voucher) => voucher.status === 'ACTIVE').length,
    percent: vouchers.filter((voucher) => voucher.discountType === 'PERCENT').length,
    amount: vouchers.filter((voucher) => voucher.discountType === 'AMOUNT').length,
    freeship: vouchers.filter((voucher) => voucher.discountType === 'FREESHIP').length,
  }
}

export const useVoucherStore = defineStore('voucher', {
  state: () => ({
    vouchers: [],
    selectedVoucher: null,
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
      percent: 0,
      amount: 0,
      freeship: 0,
    },
  }),

  actions: {
    async fetchVouchers(params = {}) {
      try {
        this.loading = true
        this.error = null

        const res = await voucherApi.getAllVoucher(params)
        const pageData = res.data

        this.vouchers = (pageData.content || []).map(mapVoucher)
        this.pagination = {
          page: pageData.number || 0,
          size: pageData.size || params.size || 10,
          totalPages: pageData.totalPages || 0,
          totalElements: pageData.totalElements || 0,
        }

        try {
          const statsRes = await voucherApi.getVoucherStats({})
          this.stats = statsRes.data || buildFallbackStats(this.vouchers, pageData.totalElements || 0)
        } catch (statsError) {
          this.stats = buildFallbackStats(this.vouchers, pageData.totalElements || 0)
        }

        return true
      } catch (error) {
        console.error('có lỗi ở getAllVoucher: ', error)
        this.vouchers = []
        this.error =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'Không thể tải danh sách voucher'

        return false
      } finally {
        this.loading = false
      }
    },

    async fetchGetOneVoucher(voucherId) {
      try {
        this.loading = true
        this.error = null

        const res = await voucherApi.getOneVoucher(voucherId)
        this.selectedVoucher = mapVoucher(res.data)

        return this.selectedVoucher
      } catch (error) {
        console.error('lỗi getOneVoucher: ', error)
        this.selectedVoucher = null
        this.error =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'Không thể tải chi tiết voucher'

        return null
      } finally {
        this.loading = false
      }
    },

    async deleteVoucherById(voucherId) {
      try {
        this.loading = true
        this.error = null

        // Gọi API DELETE - chỉ cần voucherId, không cần body đầy đủ
        // → Tránh được lỗi validation @NotNull khi DB có field null
        await voucherApi.deleteVoucherById(voucherId)

        // Ghi nhận trạng thái status trước khi xóa để tính totalElements đúng
        const deletedVoucher = this.vouchers.find((v) => v.voucherId === voucherId)
        const wasActive = deletedVoucher ? deletedVoucher.status === 'ACTIVE' : true

        // Cập nhật local state - loại bỏ voucher khỏi danh sách hiển thị
        this.vouchers = this.vouchers.filter(
          (voucher) => voucher.voucherId !== voucherId
        )

        // Cập nhật totalElements / totalPages ngay để UI phản ánh đúng
        // (chỉ giảm nếu voucher bị xóa đang được đếm trong tổng hiện tại)
        const size = this.pagination.size || 10
        const currentTotal = this.pagination.totalElements || 0
        const newTotal = wasActive ? Math.max(0, currentTotal - 1) : currentTotal

        this.pagination = {
          ...this.pagination,
          totalElements: newTotal,
          totalPages: Math.ceil(newTotal / size),
        }

        this.stats = buildFallbackStats(this.vouchers, newTotal)

        return true
      } catch (error) {
        console.error('lỗi deleteVoucherById:', error)
        this.error =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'Không thể ngừng hoạt động voucher'

        return false
      } finally {
        this.loading = false
      }
    },

    async addVoucher(payload) {
      try {
        this.loading = true
        this.error = null
        this.fieldErrors = {}

        await voucherApi.createVoucher(payload)
        // Reload danh sách để giữ đúng thứ tự sort từ BE
        await this.fetchVouchers()

        return true
      } catch (error) {
        console.error('lỗi addVoucher:', error)
        const message = getErrorMessage(error)

        this.fieldErrors = {
          ...mapFieldErrors(error.response?.data?.data),
          ...mapMessageToFieldError(message),
        }

        this.error =
          message ||
          error.response?.data?.error ||
          error.response?.data?.data ||
          'Không thể thêm voucher'

        return false
      } finally {
        this.loading = false
      }
    },

    async updateVoucher(voucherId, payload) {
      try {
        this.loading = true
        this.error = null
        this.fieldErrors = {}

        const response = await voucherApi.updateVoucher(voucherId, payload)
        const updatedVoucher = mapVoucher(response.data)
        const index = this.vouchers.findIndex(
          (voucher) => voucher.voucherId === voucherId
        )

        if (index !== -1) {
          this.vouchers[index] = updatedVoucher
        }

        return true
      } catch (error) {
        console.error('lỗi updateVoucher:', error)
        const message = getErrorMessage(error)

        this.fieldErrors = {
          ...mapFieldErrors(error.response?.data?.data),
          ...mapMessageToFieldError(message),
        }

        this.error =
          message ||
          error.response?.data?.error ||
          error.response?.data?.data ||
          'Không thể cập nhật voucher'

        return false
      } finally {
        this.loading = false
      }
    },
  },
})
