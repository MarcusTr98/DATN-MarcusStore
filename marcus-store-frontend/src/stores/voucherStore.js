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
    maxDiscountAmount: voucher.maxDiscountAmount,
    minOrderValue: Number(voucher.minOrderValue || 0),
    startDate: formatDateTimeLocal(voucher.startDate),
    endDate: formatDateTimeLocal(voucher.endDate),
    quantity: Number(voucher.quantity || 0),
    isActive: Boolean(voucher.isActive),
  }
}

function mapFieldErrors(errors = {}) {
  const mappedErrors = {
    voucher_code: errors.voucherCode,
    discount_value: errors.discountValue,
    discount_type: errors.discountType,
    max_discount_amount: errors.maxDiscountAmount,
    min_order_value: errors.minOrderValue,
    start_date: errors.startDate,
    end_date: errors.endDate,
    quantity: errors.quantity,
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
    active: vouchers.filter((voucher) => voucher.isActive).length,
    percent: vouchers.filter((voucher) => voucher.discountType === 'PERCENT').length,
    amount: vouchers.filter((voucher) => voucher.discountType === 'AMOUNT').length,
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

        await voucherApi.deleteVoucherById(voucherId)

        this.vouchers = this.vouchers.filter(
          (voucher) => voucher.voucherId !== voucherId
        )

        return true
      } catch (error) {
        console.error('lỗi deleteVoucherById:', error)
        this.error =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'Không thể xóa voucher'

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

        const response = await voucherApi.createVoucher(payload)
        this.vouchers.unshift(mapVoucher(response.data))

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
