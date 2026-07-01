import api from "@/utils/api"

const API_URL = "/admin/staff-permissions"

export default {

  // Danh sách nhân viên
  getAllStaff(params) {
    return api.get("/admin/user", {
      params: {
        ...params,
        role: "STAFF"
      }
    })
  },

  // Danh sách permission
  getAllPermissions() {
    return api.get(API_URL)
  },

  // Permission của staff
  getUserPermissions(userId) {
    return api.get(`${API_URL}/${userId}`)
  },

  // Lưu permission
  updateUserPermissions(userId, permissionIds) {
    return api.put(`${API_URL}/${userId}`, {
      permissionIds
    })
  }

}