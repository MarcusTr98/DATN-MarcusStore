import api from "@/utils/api.js";
const OrderDetailApi ={
  getOrderDetail(orderCode){
return api.get(`/admin/order/${orderCode}`)
  },
}
export default OrderDetailApi
