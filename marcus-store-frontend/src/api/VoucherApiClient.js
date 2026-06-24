import api from"@/utils/api.js";

const voucherApiClient = {
  getAllVoucherClient () {
    return api.get("/cart/vouchers")
  }
}
export default voucherApiClient;
