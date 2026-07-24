import api from '@/utils/api'

const cartApi = {
  getCart() {
    return api.get('/cart')
  },

  getCartSuggestions(limit = 12) {
    return api.get('/cart/suggestions', { params: { limit } })
  },

  addToCart(data) {
    return api.post('/cart/items', data)
  },

    updateItemQuantity(skuId, quantity){
    return api.put(`/cart/items/${skuId}`, {quantity})
    },
  removeItemFromCart(skuId) {
    return api.delete(`/cart/items/${skuId}`)
  },

  removeManyItemFromCart(skuIds) {
    return api.delete('/cart/items/selected', {
      data: {
        skuIds,
      },
    })
  },

  removeAll() {
    return api.delete('/cart/items')
  },
}

export default cartApi
