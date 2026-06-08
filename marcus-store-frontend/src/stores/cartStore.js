import {defineStore} from 'pinia'
import cartApi from '@/api/cartApi'

// Convert dữ liệu backend trả về sang dữ liệu mà Cart.vue đang dùng
function mapCartItem(item) {
  // convert dữ liệu   từ BE sang FE
  return {
    id: item.skuId,
    cartItemId: item.cartItemId,
    skuId: item.skuId,
    skuCode: item.skuCode,
    name: item.productName,
    // nếu BE có sẵn variantText thì dùng còn không thì nối chuỗi giữa color và storage
    variant: item.variantText || [item.color, item.storage].filter(Boolean).join(' / '),
    imageUrl: item.imageUrl,
    price: Number(item.price || 0),
    originalPrice: Number(item.price || 0),
    quantity: Number(item.quantity || 1),
    totalPrice: Number(item.totalPrice || 0),
    stockQuantity: Number(item.stockQuantity || 0),
    // Dữ liệu phục vụ giao diện hiện tại
    checked: true,
    badge: 'Sản phẩm chính',
    isAccessory: false,
    // Nếu chưa có ảnh thì dùng tạm mã SKU để hiển thị
    icon: item.skuCode || 'SP',
  }
}

export const useCartStore = defineStore('cart', {
  // lưu dữ liệu của giỏ hàng
  state: () => ({
    // lưu toàn bộ response của giỏ hàng
    cart: null,
    // luưu danh sách của sản phẩm trong giỏ hàng
    items: [],
    //
    loading: false,
    // lưu lỗi nếu gọi APi thất bại
    error: null,
  }),

  getters: {
    // lấy tổng số lượng sản phẩm trong giỏ
    totalQuantity: (state) => {
      // ?. dùng để kiểm tra an toàn nếu cart trống thì không bị lỗi
      return state.cart?.totalQuantity || 0
    },
    // tổng tiền của giỏ hàng
    totalAmount: (state) => {
      return state.cart?.totalAmount || 0
    },
  },
// xử lý toàn bộ  nghiệp vụ CRUD
  actions: {
    // gọi giỏ hàng từ database
    async fetchCart() {
      try {
        // tải dữ liệu giỏ hàng và xóa lỗi
        this.loading = true
        this.error = null
        // gọi hàm getCart bên cartApi.js
        const res = await cartApi.getCart()
        // lưu dữ liệu của axios
        this.cart = res.data
        // lưu danh sách sản phẩm và duyệt từng phân tử qua Map
        this.items = (res.data.items || []).map(mapCartItem)
      } catch (error) {
        console.error('Lỗi lấy giỏ hàng:', error)

        this.cart = null
        this.items = []
        this.error = 'Không thể tải giỏ hàng'
        // luôn chạy dù lỗi hay không
      } finally {
        this.loading = false
      }
    },

    async addToCart(skuId, quantity = 1) {
      try {
        this.loading = true
        this.error = null

        const data = {
          skuId,
          quantity,
        }

        const res = await cartApi.addToCart(data)

        this.cart = res.data
        this.items = (res.data.items || []).map(mapCartItem)

        return true
      } catch (error) {
        console.error('Lỗi thêm giỏ hàng:', error)

        this.error = 'Thêm vào giỏ hàng thất bại'
        return false
      } finally {
        this.loading = false
      }
    },
     async updateItemQuantity(skuId, Quantity){
      try {

        this.error = null;
        const res = await cartApi.updateItemQuantity(skuId, Quantity)
        const  data = res.data
        this.cart = data
        this.items = (data.items || []).map(mapCartItem);
        return true
      }catch (error){
        console.error("lỗi cập nhật số lượng: ", error)
        this.error =
          error.response?.data?.message||
          error.response?.data?.data||
          "cập nhật số lượng không thành công"
        return false
      }finally {

      }
     },
    async removeItemFromCart(skuId) {
      try {
        this.loading = true
        this.error = null

        const res = await cartApi.removeItemFromCart(skuId)

        this.cart = res.data
        this.items = (res.data.items || []).map(mapCartItem)

        return true
      } catch (error) {
        console.error('Lỗi xóa sản phẩm:', error)

        this.error = 'Xóa sản phẩm thất bại'
        return false
      } finally {
        this.loading = false
      }
    },

    async removeManyItemFromCart(skuIds) {
      try {
        this.loading = true
        this.error = null

        const res = await cartApi.removeManyItemFromCart(skuIds)

        this.cart = res.data
        this.items = (res.data.items || []).map(mapCartItem)

        return true
      } catch (error) {
        console.error('Lỗi xóa sản phẩm đã chọn:', error)

        this.error = 'Xóa sản phẩm đã chọn thất bại'
        return false
      } finally {
        this.loading = false
      }
    },

    async removeAll() {
      try {
        this.loading = true
        this.error = null

        const res = await cartApi.removeAll()

        this.cart = res.data
        this.items = (res.data.items || []).map(mapCartItem)

        return true
      } catch (error) {
        console.error('Lỗi xóa giỏ hàng:', error)

        this.error = 'Xóa giỏ hàng thất bại'
        return false
      } finally {
        this.loading = false
      }
    },
  },
})
