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
  // convert định dạng ngày backend trả về
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
      error: null
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
      async fetchVouchers(){
        try{
          this.loading = true
          this.error = null
          const res = await voucherApi.getAllVoucher()

          this.vouchers = (res.data || []).map(mapVoucher)
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

          const response = await voucherApi.createVoucher(payload)

          this.vouchers.unshift(response.data)

          return true
        } catch (error) {
          console.error('lỗi addVoucher:', error)

          this.error =
            error.response?.data?.message ||
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

          this.error =
            error.response?.data?.message ||
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

