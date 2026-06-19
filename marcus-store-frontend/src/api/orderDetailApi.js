import api from "@/utils/api.js";
const OrderDetailApi ={
  getOrderDetail(orderCode){
return api.get(`/admin/order/${orderCode}`)
  },
  updateStatusOrder(orderCode, data){
    return api.put(`/admin/order/${orderCode}`, data)
  },
}
export default OrderDetailApi
