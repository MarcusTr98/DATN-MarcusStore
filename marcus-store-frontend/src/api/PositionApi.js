import api from "@/utils/api";


export default {

    layDanhSach() {

        return api.get("/admin/positions");

    },

    layChiTiet(id) {

        return api.get(`/admin/positions/${id}`);

    },

    them(data) {

        return api.post("/admin/positions", data);

    },

    capNhat(id,data){

        return api.put(`/admin/positions/${id}`,data);

    },

    doiTrangThai(id,active){

        return api.put(`/admin/positions/${id}/status?active=${active}`);

    }

}