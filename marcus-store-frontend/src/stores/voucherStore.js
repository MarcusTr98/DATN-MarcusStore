import  {defineStore} from "pinia";
import voucherApi from '@/api/voucherApi.js'
// convert dữ liệu từ backend sang frontend
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
  // convert định dạng ngày backend trả về
function matchesVoucherParams(voucher, params = {}) {
  const keyword = params.keyword?.trim().toLowerCase()
  const discountType = params.discountType
  const isActive = params.isActive

  if (keyword && !voucher.voucherCode?.toLowerCase().includes(keyword)) {
    return false
  }

  if (discountType && voucher.discountType !== discountType) {
    return false
  }

  if (typeof isActive === 'boolean' && Boolean(voucher.isActive) !== isActive) {
    return false
  }

  return true
}
function buildStats(vouchers = []) {
  return {
    total: vouchers.length,
    active: vouchers.filter((voucher) => voucher.isActive).length,
    percent: vouchers.filter((voucher) => voucher.discountType === 'PERCENT').length,
    amount: vouchers.filter((voucher) => voucher.discountType === 'AMOUNT').length,
  }
}
  function formatDateTimeLocal(value){
    if(!value){
      return ''
    }
    return String(value).slice(0,16)
  }
  export const useVoucherStore = defineStore('voucher', {
    state: () => ({
      //lấy danh sách voucher từ API
      vouchers: [],
      // Voucher đang được chọn để xem chi tiết
      selectedVoucher: null,
      // trạng thái loading khi gọi API
      loading: false,
      // lưu lỗi nếu API thất bại
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
      }
    }),
    getters: {
      totalVoucher: (state) =>{
        return state.vouchers.length
      },
      activeVoucher: (state) => {
        return state.vouchers.filter((voucher) => voucher.isActive).length
      },

      percentVoucher: (state) => {
        return state.vouchers.filter((voucher) => voucher.discountType === 'PERCENT').length
      },

      amountVoucher: (state) => {
        return state.vouchers.filter((voucher) => voucher.discountType === 'AMOUNT').length
      },
    },
    actions:{
      async fetchVouchers(params = {}){
        try{
          this.loading = true
          this.error = null
          const res = await voucherApi.getAllVoucher(params)
          const pageData = res.data
          const isArrayResponse = Array.isArray(pageData)
          const page = params.page || 0
          const size = params.size || 10
          const allVouchers = isArrayResponse
            ? pageData.map(mapVoucher).filter((voucher) => matchesVoucherParams(voucher, params))
            : []
          const vouchersData = isArrayResponse
            ? allVouchers.slice(page * size, page * size + size)
            : (pageData.content || [])

          this.vouchers = isArrayResponse ? vouchersData : vouchersData.map(mapVoucher)
          if (isArrayResponse) {
            this.stats = buildStats(allVouchers)
          } else {
            const statsParams = { ...params }
            delete statsParams.page
            delete statsParams.size
            const statsRes = await voucherApi.getVoucherStats(statsParams)
            this.stats = statsRes.data || {
              total: pageData.totalElements || 0,
              active: 0,
              percent: 0,
              amount: 0,
            }
          }
          this.pagination = isArrayResponse
            ? {
              page,
              size,
              totalPages: Math.ceil(allVouchers.length / size),
              totalElements: allVouchers.length,
            }
            : {
              page: pageData.number || 0,
              size: pageData.size || params.size || 10,
              totalPages: pageData.totalPages || 0,
              totalElements: pageData.totalElements || 0,
            }
          return true
        }catch (error){
          console.error("có lỗi ở getAllVoucher: ", error)
          this.vouchers = []
          this.error = error.response?.data?.message ||
            error.response?.data?.data ||
            'Không thể tải danh sách voucher'
          return false
        }finally {
          this.loading = false
        }
      },
      async fetchGetOneVoucher(voucherId){
        try{
          this.loading = true
          this.error = null
          const res = await  voucherApi.getOneVoucher(voucherId)
          this.selectedVoucher = mapVoucher(res.data)
          return this.selectedVoucher
        }catch (error){
          console.error("lỗi getOneVoucher: " , error)
          this.selectedVoucher = null
          this.error =
            error.response?.data?.message ||
            error.response?.data?.data ||
            'Không thể tải chi tiết voucher'

          return null
        }finally {
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

          this.vouchers.unshift(response.data)

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
      }
    }
  })
