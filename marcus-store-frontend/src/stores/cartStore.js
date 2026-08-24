import { defineStore } from 'pinia'
import cartApi from '@/api/cartApi'
import { trackBehavior } from '@/api/behaviorApi'

// Convert dữ liệu backend trả về sang dữ liệu mà Cart.vue đang dùng
function mapCartItem(item) {
  // Xác định có phải sản phẩm Flash Sale không
  const isFlashSale =
    item.isFlashSale === true || item.isFlashSale === 'true' || !!item.flashSaleSlotId

  return {
    id: item.skuId,
    cartItemId: item.cartItemId,
    skuId: item.skuId,
    skuCode: item.skuCode,
    name: item.productName,
    // nếu BE có sẵn variantText thì dùng còn không thì nối chuỗi giữa color và storage
    variant: item.variantText || [item.color, item.storage].filter(Boolean).join(' / '),

    thumbnailUrl: item.thumbnailUrl,

    // Nếu là Flash Sale: price = giá FS, originalPrice = giá gốc
    // Nếu không: price = giá bán, originalPrice = giá gốc
    price: Number(item.price || 0),
    originalPrice: Number(item.originalPrice || item.price || 0),
    quantity: Number(item.quantity || 1),
    totalPrice: Number(item.totalPrice || 0),
    stockQuantity: Number(item.stockQuantity || 0),
    // Dữ liệu Flash Sale
    isFlashSale: isFlashSale,
    flashSaleSlotId: item.flashSaleSlotId || null,
    flashSaleSlotName: item.flashSaleSlotName || null,
    flashSalePrice: Number(item.flashSalePrice || 0) || null,
    // Dữ liệu phục vụ giao diện hiện tại
    checked: false,
    badge: isFlashSale ? '⚡ Flash Sale' : 'Sản phẩm chính',
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
        trackBehavior('CART_ADDED').catch(() => {})

        return true
      } catch (error) {
        console.error('Lỗi thêm giỏ hàng:', error)

        this.error = 'Thêm vào giỏ hàng thất bại'
        return false
      } finally {
        this.loading = false
      }
    },

    // Action mới: Thêm sản phẩm Flash Sale vào giỏ hàng
    async addToCartWithFlashSale(skuId, quantity, flashSaleSlotId) {
      try {
        this.loading = true
        this.error = null

        // Marcus sửa tại biên Cart -> Checkout: frontend chỉ gửi định danh, giá
        // Flash Sale phải do backend đọc lại từ database.
        const data = {
          skuId,
          quantity,
          flashSaleSlotId,
        }

        const res = await cartApi.addToCart(data)

        this.cart = res.data
        this.items = (res.data.items || []).map(mapCartItem)
        trackBehavior('CART_ADDED').catch(() => {})

        return true
      } catch (error) {
        console.error('Lỗi thêm giỏ hàng Flash Sale:', error)

        this.error = error.response?.data?.message || 'Thêm vào giỏ hàng thất bại'
        return false
      } finally {
        this.loading = false
      }
    },
    async updateItemQuantity(skuId, Quantity) {
      try {
        this.error = null
        const res = await cartApi.updateItemQuantity(skuId, Quantity)
        const data = res.data
        this.cart = data
        this.items = (data.items || []).map(mapCartItem)
        return true
      } catch (error) {
        console.error('lỗi cập nhật số lượng: ', error)
        this.error =
          error.response?.data?.message ||
          error.response?.data?.data ||
          'cập nhật số lượng không thành công'
        return false
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
    // Marcus thêm hàm mới dùng để reset nhanh số tiền trong giỏ hàng
    clearCartState() {
      this.cart = null
      this.items = []
      this.error = null
    },
  },
})
