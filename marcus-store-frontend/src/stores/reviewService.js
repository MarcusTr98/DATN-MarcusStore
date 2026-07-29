import api from "@/utils/api";

export default {

    create(orderItemId, data) {
        return api.post(`/reviews/order-items/${orderItemId}`, data);
    },

    update(reviewId, data) {
        return api.put(`/reviews/${reviewId}`, data);
    },

    remove(reviewId) {
        return api.delete(`/reviews/${reviewId}`);
    },

    getProductReviews(productId) {
        return api.get(`/reviews/product/${productId}`);
    },
    getMyReview(orderItemId){

    return api.get(`/reviews/order-items/${orderItemId}`)

},
}