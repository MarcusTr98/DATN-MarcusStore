import api from "@/utils/api";

export const getReviews = () => {
  return api.get("/admin/reviews");
};

export const searchReviews = (params) => {
  return api.get("/admin/reviews/search", {
    params,
  });
};

export const replyReview = (reviewId, data) => {
  return api.post(`/admin/reviews/${reviewId}/reply`, data);
};

export const updateReply = (reviewId, data) => {
  return api.put(`/admin/reviews/${reviewId}/reply`, data);
};

export const deleteReview = (reviewId) => {
  return api.delete(`/admin/reviews/${reviewId}`);
};