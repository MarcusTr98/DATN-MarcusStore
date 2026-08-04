<template>

    <div class="position-page">

        <div class="position-shell">

        <!-- Header -->

        <div class="page-header">

            <div class="header-left">

                <div class="header-icon">

                    <i class="bi bi-person-badge-fill"></i>

                </div>

                <div>

                    <h2>

                        Quản lý chức danh

                    </h2>

                    <p>

                        Tạo và quản lý chức danh mặc định cho nhân viên

                    </p>

                </div>

            </div>

            <button
                class="btn-add"
                @click="moThem"
            >

                <i class="bi bi-plus-circle"></i>

                Thêm chức danh

            </button>

        </div>

        <!-- Stats -->

        <div class="stats-row">

            <div class="stat-card">

                <p class="stat-label">

                    Tổng chức danh

                </p>

                <p class="stat-value">

                    {{ danhSachChucDanh.length }}

                </p>

            </div>

            <div class="stat-card">

                <p class="stat-label">

                    Đang hoạt động

                </p>

                <p class="stat-value active-text">

                    {{ soLuongHoatDong }}

                </p>

            </div>

            <div class="stat-card">

                <p class="stat-label">

                    Đã khóa

                </p>

                <p class="stat-value">

                    {{ soLuongKhoa }}

                </p>

            </div>

        </div>

        <!-- Filter -->

        <div class="filter-card">

            <div class="filter-group search-group">

                <label>TÌM KIẾM</label>

                <div class="search-box">

                    <i class="bi bi-search"></i>

                    <input
                        v-model="keyword"
                        type="text"
                        placeholder="Tìm theo tên chức danh..."
                    >

                </div>

            </div>

            <div class="filter-group">

                <label>TRẠNG THÁI</label>

                <select v-model="statusFilter">

                    <option value="all">

                        Tất cả

                    </option>

                    <option value="active">

                        Hoạt động

                    </option>

                    <option value="inactive">

                        Đã khóa

                    </option>

                </select>

            </div>

            <button
                class="reset-btn"
                title="Đặt lại bộ lọc"
                @click="datLaiBoLoc"
            >

                <i class="bi bi-arrow-clockwise"></i>

            </button>

        </div>

        <!-- Table -->

        <div class="table-card">

            <table>

                <thead>

                    <tr>

                        <th width="70">

                            STT

                        </th>

                        <th>

                            TÊN CHỨC DANH

                        </th>

                        <th>

                            MÔ TẢ

                        </th>

                        <th width="140">

                            TRẠNG THÁI

                        </th>

                        <th width="140">

                            THAO TÁC

                        </th>

                    </tr>

                </thead>

                <tbody>

                    <tr
                        v-for="(item,index) in filteredPositions"
                        :key="item.positionId"
                    >

                        <td class="id-cell">

                            #{{ index + 1 }}

                        </td>

                        <td class="name-cell">

                            {{ item.positionName }}

                        </td>

                        <td class="desc-cell">

                            {{ item.description }}

                        </td>

                        <td>

                            <span
                                class="status"
                                :class="item.isActive ? 'active' : 'inactive'"
                            >

                                {{ item.isActive
                                    ? "Hoạt động"
                                    : "Đã khóa"
                                }}

                            </span>

                        </td>

                        <td class="actions-cell">

                            <button
                                class="icon-btn edit"
                                title="Sửa"
                                @click="moSua(item)"
                            >

                                <i class="bi bi-pencil-square"></i>

                            </button>

                            <button
                                v-if="item.isActive"
                                class="icon-btn lock"
                                title="Khóa"
                                @click="openLockConfirm(item)"
                            >

                                <i class="bi bi-lock"></i>

                            </button>

                            <button
                                v-else
                                class="icon-btn unlock"
                                title="Mở khóa"
                                @click="openLockConfirm(item)"
                            >

                                <i class="bi bi-unlock"></i>

                            </button>

                        </td>

                    </tr>

                    <tr
                        v-if="filteredPositions.length === 0"
                    >

                        <td
                            colspan="5"
                            class="empty"
                        >

                            <i class="bi bi-inbox"></i>

                            <p>

                                Không có dữ liệu

                            </p>

                        </td>

                    </tr>

                </tbody>

            </table>

        </div>

        <!-- Modal -->

        <PositionModal

            v-if="showModal"

            :mode="mode"

            :data="currentPosition"

            @close="showModal = false"

            @success="reloadData"

        />

        </div>

    </div>

    <BaseModal
        :visible="lockConfirm.visible"
        type="confirm"
        title="Xác nhận thay đổi trạng thái"
        :message="lockConfirm.message"
        @close="closeLockConfirm"
        @confirm="confirmDoiTrangThai"
    />

</template>
<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import PositionApi from "@/api/PositionApi";
import PositionModal from "./PositionModal.vue";
import BaseModal from "@/components/BaseModal.vue";


/* ==========================
        STATE
========================== */

const danhSachChucDanh = ref([]);

const keyword = ref("");

const statusFilter = ref("all");

const showModal = ref(false);

const mode = ref("add");

const currentPosition = ref(null);

const loading = ref(false);

const lockConfirm = reactive({

    visible:false,

    item:null,

    message:""

});

/* ==========================
      LOAD DANH SÁCH
========================== */

const layDanhSach = async () => {

    loading.value = true;

    try {

        const response = await PositionApi.layDanhSach();

        danhSachChucDanh.value = response.data.data;

    } catch (error) {

        console.error("Lỗi lấy chức danh", error);

    } finally {

        loading.value = false;

    }

};

/* ==========================
        THỐNG KÊ
========================== */

const soLuongHoatDong = computed(() =>

    danhSachChucDanh.value.filter(item => item.isActive).length

);

const soLuongKhoa = computed(() =>

    danhSachChucDanh.value.filter(item => !item.isActive).length

);

/* ==========================
        SEARCH + FILTER
========================== */

const filteredPositions = computed(() => {

    let ds = danhSachChucDanh.value;

    if (statusFilter.value === "active") {

        ds = ds.filter(item => item.isActive);

    } else if (statusFilter.value === "inactive") {

        ds = ds.filter(item => !item.isActive);

    }

    if (!keyword.value.trim()) {

        return ds;

    }

    const key = keyword.value.toLowerCase();

    return ds.filter(item =>

        item.positionName.toLowerCase().includes(key)

        ||

        (item.description || "")

            .toLowerCase()

            .includes(key)

    );

});

const datLaiBoLoc = () => {

    keyword.value = "";

    statusFilter.value = "all";

};

/* ==========================
      MỞ MODAL THÊM
========================== */

const moThem = () => {

    mode.value = "add";

    currentPosition.value = null;

    showModal.value = true;

};

/* ==========================
      MỞ MODAL SỬA
========================== */

const moSua = async (item) => {

    try {

        const response = await PositionApi.layChiTiet(item.positionId);

        currentPosition.value = response.data.data;

        mode.value = "edit";

        showModal.value = true;

    } catch (error) {

        console.error(error);

    }

};

/* ==========================
      KHÓA / MỞ KHÓA
========================== */

const openLockConfirm = (item) => {

    lockConfirm.item = item;

    lockConfirm.message = item.isActive

        ? `Bạn có chắc muốn khóa chức danh "${item.positionName}" không?`

        : `Bạn có chắc muốn mở khóa chức danh "${item.positionName}" không?`;

    lockConfirm.visible = true;

};

const closeLockConfirm = () => {

    lockConfirm.visible = false;

    lockConfirm.item = null;

    lockConfirm.message = "";

};

const confirmDoiTrangThai = async () => {

    const item = lockConfirm.item;

    if (!item) {

        return;

    }

    try {

        await PositionApi.doiTrangThai(

            item.positionId,

            !item.isActive

        );

        await layDanhSach();

    } catch (error) {

        console.error(error);

    } finally {

        closeLockConfirm();

    }

};

/* ==========================
      SAU KHI LƯU
========================== */

const reloadData = async () => {

    showModal.value = false;

    await layDanhSach();

};

/* ==========================
        INIT
========================== */

onMounted(() => {

    layDanhSach();

});
</script>
<style scoped>

/* ==========================
        PAGE
========================== */

.position-page{
    position:relative;
    min-height:calc(100vh - 64px);
    background:#fff7fa;
    color:#202636;
}

.position-shell{
    width:min(100%,1280px);
    margin:0 auto;
    padding:24px;
}

/* ==========================
        HEADER
========================== */

.page-header{
    display:flex;
    justify-content:space-between;
    align-items:center;
    gap:20px;
    margin-bottom:24px;
    padding:20px 24px;
    border:1px solid #f3d6e3;
    border-radius:8px;
    background:#fff;
    box-shadow:0 4px 18px rgba(15,23,42,.06);
}

.header-left{
    display:flex;
    align-items:center;
    gap:14px;
}

.header-icon{
    width:52px;
    height:52px;
    border-radius:8px;
    background:#f55d9b;
    color:#fff;
    display:flex;
    justify-content:center;
    align-items:center;
    font-size:1.45rem;
}

.header-left h2{
    margin:0;
    font-size:1.45rem;
    font-weight:800;
    color:#f55d9b;
}

.header-left p{
    margin-top:4px;
    color:#6b7280;
    font-size:.9rem;
}

/* ==========================
        BUTTON
========================== */

.btn-add{
    display:inline-flex;
    align-items:center;
    justify-content:center;
    gap:8px;
    border:none;
    border-radius:8px;
    background:#f55d9b;
    color:#fff;
    padding:10px 16px;
    cursor:pointer;
    font-weight:700;
    font-size:14px;
    transition:.2s;
}

.btn-add:hover{
    background:#ec4d8d;
}

/* ==========================
        STATS
========================== */

.stats-row{
    display:grid;
    grid-template-columns:repeat(3,1fr);
    gap:16px;
    margin-bottom:20px;
}

.stat-card{
    border:1px solid #f3d6e3;
    background:#fff;
    box-shadow:0 4px 18px rgba(15,23,42,.06);
    border-radius:8px;
    padding:20px 18px;
    min-height:100px;
    display:flex;
    flex-direction:column;
    justify-content:space-between;
}

.stat-label{
    margin:0;
    color:#6b7280;
    font-size:.86rem;
    font-weight:700;
}

.stat-value{
    margin:6px 0 0;
    font-size:1.65rem;
    font-weight:800;
    line-height:1;
    color:#202636;
}

.active-text{
    color:#f55d9b;
}

/* ==========================
        FILTER
========================== */

.filter-card{
    border:1px solid #f3d6e3;
    background:#fff;
    box-shadow:0 4px 18px rgba(15,23,42,.06);
    border-radius:8px;
    padding:18px 20px;
    display:flex;
    align-items:flex-end;
    gap:16px;
    margin-bottom:18px;
}

.filter-group{
    display:flex;
    flex-direction:column;
    gap:7px;
}

.filter-group label{
    font-size:.76rem;
    font-weight:800;
    color:#b4557d;
    letter-spacing:0;
    text-transform:uppercase;
}

.search-group{
    flex:1;
}

.search-box{
    display:flex;
    align-items:center;
    gap:9px;
    background:#fffafd;
    padding:0 14px;
    border:1px solid #f3d6e3;
    border-radius:8px;
    transition:.18s;
}

.search-box i{
    color:#b4557d;
    font-size:14px;
}

.search-box input{
    flex:1;
    border:none;
    outline:none;
    padding:11px 0;
    background:none;
    font-size:14px;
    color:#202636;
}

.search-box:hover{
    border-color:#efbdd2;
    background:#fff;
    box-shadow:0 0 0 .12rem rgba(245,93,155,.08);
}

.search-box:focus-within{
    border-color:#f55d9b;
    box-shadow:0 0 0 .18rem rgba(245,93,155,.12);
}

.filter-group select{
    border:1px solid #f3d6e3;
    background:#fffafd;
    border-radius:8px;
    padding:11px 14px;
    min-width:150px;
    outline:none;
    color:#202636;
    font-size:14px;
    cursor:pointer;
    transition:.18s;
}

.filter-group select:hover{
    border-color:#efbdd2;
    background:#fff;
    box-shadow:0 0 0 .12rem rgba(245,93,155,.08);
}

.filter-group select:focus{
    border-color:#f55d9b;
    box-shadow:0 0 0 .18rem rgba(245,93,155,.12);
}

.reset-btn{
    width:42px;
    height:42px;
    border:1px solid #f3d6e3;
    background:#fff;
    border-radius:8px;
    color:#202636;
    cursor:pointer;
    transition:.18s;
    display:flex;
    align-items:center;
    justify-content:center;
    font-size:15px;
}

.reset-btn:hover{
    border-color:#efbdd2;
    background:#fff0f7;
    color:#d63384;
}

/* ==========================
        TABLE
========================== */

.table-card{
    border:1px solid #f3d6e3;
    background:#fff;
    box-shadow:0 4px 18px rgba(15,23,42,.06);
    border-radius:8px;
    overflow:hidden;
}

table{
    width:100%;
    border-collapse:collapse;
}

thead{
    background:#fff0f7;
}

th{
    padding:14px 16px;
    text-align:left;
    color:#b4557d;
    font-weight:800;
    font-size:.74rem;
    letter-spacing:0;
    text-transform:uppercase;
    white-space:nowrap;
}

td{
    padding:16px;
    border-top:1px solid #f3d6e3;
    font-size:.9rem;
    color:#4b5563;
}

.id-cell{
    color:#9ca3af;
    font-weight:600;
}

.name-cell{
    font-weight:800;
    color:#202636;
}

.desc-cell{
    color:#6b7280;
}

tbody tr{
    transition:.15s;
}

tbody tr:hover{
    background:#fffafd;
}

/* ==========================
        STATUS
========================== */

.status{
    display:inline-flex;
    justify-content:center;
    align-items:center;
    min-height:26px;
    min-width:90px;
    padding:4px 10px;
    border-radius:999px;
    font-size:.76rem;
    font-weight:800;
}

.active{
    background:#ffe4ef;
    color:#d63384;
}

.inactive{
    background:#f1f5f9;
    color:#64748b;
}

/* ==========================
        ACTION
========================== */

.actions-cell{
    white-space:nowrap;
}

.icon-btn{
    width:36px;
    height:36px;
    border:1px solid #f3d6e3;
    background:#fff;
    border-radius:8px;
    cursor:pointer;
    margin-right:8px;
    font-size:14px;
    color:#202636;
    transition:.15s;
}

.icon-btn:hover{
    background:#fff0f7;
    color:#d63384;
}

.icon-btn.lock{
    border-color:#f5c2c7;
    background:#fff5f6;
    color:#dc3545;
}

.icon-btn.lock:hover{
    background:#f8d7da;
    color:#dc3545;
}

.icon-btn.unlock{
    border-color:#bbf7d0;
    background:#f0fdf4;
    color:#15803d;
}

.icon-btn.unlock:hover{
    background:#dcfce7;
    color:#15803d;
}

/* ==========================
        EMPTY
========================== */

.empty{
    text-align:center;
    padding:42px 16px;
    color:#6b7280;
}

.empty i{
    display:block;
    font-size:2.4rem;
    margin-bottom:10px;
    color:#f55d9b;
}

.empty p{
    margin:0;
    font-size:14px;
}

/* ==========================
        RESPONSIVE
========================== */

@media(max-width:900px){

    .page-header{
        flex-direction:column;
        align-items:flex-start;
        gap:18px;
    }

    .stats-row{
        grid-template-columns:1fr;
    }

    .filter-card{
        flex-direction:column;
        align-items:stretch;
    }

    .reset-btn{
        align-self:flex-end;
    }

    .table-card{
        overflow:auto;
    }

    table{
        min-width:850px;
    }

}

</style>