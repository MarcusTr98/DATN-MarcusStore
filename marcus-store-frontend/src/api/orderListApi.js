import api from'@/utils/api.js'
const orderApi ={
  getAllOrder(params){
    return api.get('/admin/orders', {params})
  },
   getOrderStats(params){
    return api.get('/admin/orders/stats', {params})
   },
   getFilterOption(){
    return api.get('/admin/orders/filter-options')
   }
}
export default orderApi;
