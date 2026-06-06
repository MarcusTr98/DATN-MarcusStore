import api from '@/utils/api'

const cartApi = {
  getCart() {
    return api.get('/cart')
  },

  addToCart(data) {
    return api.post('/cart/items', data)
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
