<template>

    <div class="modal-overlay">

        <div class="modal-container">

            <!-- ================= Header ================= -->

            <div class="modal-header">

                <div>

                    <h3>

                        {{ mode === "add"
                            ? "Thêm chức danh"
                            : "Cập nhật chức danh" }}

                    </h3>

                    <p>

                        Thiết lập quyền mặc định cho chức danh

                    </p>

                </div>

                <button
                    class="btn-close"
                    @click="$emit('close')"
                >

                    <i class="bi bi-x-lg"></i>

                </button>

            </div>

            <!-- ================= Body ================= -->

            <div class="modal-body">

                <!-- LEFT -->

                <div class="left-panel">

                    <div class="form-group">

                        <label>

                            Tên chức danh
                            <span>*</span>

                        </label>

                        <input
                            v-model="form.positionName"
                            type="text"
                            placeholder="Ví dụ: Marketing"
                        >

                    </div>

                    <div class="form-group">

                        <label>Mô tả</label>

                        <textarea
                            rows="6"
                            v-model="form.description"
                            placeholder="Nhập mô tả..."
                        />

                    </div>

                </div>

                <!-- RIGHT -->

                <div class="right-panel">

                    <div class="permission-header">

                        <h4>

                            Quyền mặc định

                        </h4>

                        <button
                            class="btn-select-all"
                            @click="toggleAll"
                        >

                            {{ isAllSelected
                                ? "Bỏ chọn tất cả"
                                : "Chọn tất cả" }}

                        </button>

                    </div>

                    <!-- SECTION -->

                    <div

                        v-for="section in permissionTree"

                        :key="section.section"

                        class="permission-section"

                    >

                        <!-- SECTION TITLE -->

                        <div

                            class="section-title"

                            @click="toggleSection(section.section)"

                        >

                            <div class="section-left">

                                <i

                                    class="bi"

                                    :class="

                                        isSectionOpen(section.section)

                                            ? 'bi-chevron-down'

                                            : 'bi-chevron-right'

                                    "

                                ></i>

                                {{ section.section }}

                            </div>

                        </div>

                        <!-- MODULES -->

                        <transition name="fade">

                            <div

                                v-if="isSectionOpen(section.section)"

                            >

                                <div

                                    v-for="module in section.modules"

                                    :key="module.key"

                                    class="permission-card"

                                >

                                    <!-- Module -->

                                    <div class="module-header">

                                        <div class="module-info">

                                            <i

                                                class="bi"

                                                :class="module.icon"

                                            ></i>

                                            <span>

                                                {{ module.name }}

                                            </span>

                                        </div>

                                        <input

                                            type="checkbox"

                                            :checked="isModuleSelected(module)"

                                            @change="toggleModule(module)"

                                        >

                                    </div>

                                    <!-- Permission -->

                                    <div

                                        class="permission-list"

                                        v-if="module.permissions?.length"

                                    >

                                        <label

                                            v-for="permission in module.permissions"

                                            :key="permission.id"

                                            :class="{ 'perm-locked': !isPermissionCheckable(permission, module.permissions) }"

                                        >

                                            <input

                                                type="checkbox"

                                                :value="permission.id"

                                                :disabled="!isPermissionCheckable(permission, module.permissions)"

                                                v-model="selectedPermissions"

                                            >

                                            {{ permission.description }}

                                        </label>

                                    </div>

                                    <!-- Sub Module -->

                                    <template

                                        v-if="module.subs"

                                    >

                                        <div

                                            v-for="sub in module.subs"

                                            :key="sub.key"

                                            class="sub-module"

                                        >

                                            <div class="sub-title">

                                                {{ sub.label }}

                                            </div>

                                            <div

                                                class="permission-list"

                                                v-if="sub.permissions?.length"

                                            >

                                                <label

                                                    v-for="permission in sub.permissions"

                                                    :key="permission.id"

                                                    :class="{ 'perm-locked': !isPermissionCheckable(permission, sub.permissions) }"

                                                >

                                                    <input

                                                        type="checkbox"

                                                        :value="permission.id"

                                                        :disabled="!isPermissionCheckable(permission, sub.permissions)"

                                                        v-model="selectedPermissions"

                                                    >

                                                    {{ permission.description }}

                                                </label>

                                            </div>

                                        </div>

                                    </template>

                                </div>

                            </div>

                        </transition>

                    </div>

                </div>

            </div>

            <!-- ================= Footer ================= -->

            <div class="modal-footer">

                <button

                    class="btn-cancel"

                    @click="$emit('close')"

                >

                    Hủy

                </button>

                <button

                    class="btn-save"

                    @click="save"

                >

                    {{ mode === "add"

                        ? "Thêm chức danh"

                        : "Lưu thay đổi"

                    }}

                </button>

            </div>

        </div>

    </div>

    <BaseModal
        :visible="notice.visible"
        :type="notice.type"
        :title="notice.title"
        :message="notice.message"
        @close="closeNotice"
    />

</template>
<script setup>

import { ref, reactive, computed, watch, onMounted } from "vue";
import PositionApi from "@/api/PositionApi";
import permissionMenu from "@/api/permissionMenu";
import permissionService from "@/api/permissionService";
import BaseModal from "@/components/BaseModal.vue";

const openSections = ref([]);

const openModules = ref([]);
/* ===========================
        Props - Emit
=========================== */
const isSectionOpen = (section) => {

    return openSections.value.includes(section);

};

const toggleSection = (section) => {

    if (isSectionOpen(section)) {

        openSections.value =
            openSections.value.filter(item => item !== section);

    } else {

        openSections.value.push(section);

    }

};



const props = defineProps({

    mode:{

        type:String,

        default:"add"

    },

    data:{

        type:Object,

        default:null

    }

});

const emit = defineEmits([

    "close",

    "success"

]);

/* ===========================
        Notice Modal
=========================== */

const notice = reactive({

    visible:false,

    type:"error",

    title:"",

    message:""

});

const showNotice = ({ type = "error", title = "Thông báo", message = "" }) => {

    notice.type = type;

    notice.title = title;

    notice.message = message;

    notice.visible = true;

};

const closeNotice = () => {

    notice.visible = false;

};

/* ===========================
        Form
=========================== */

const form = reactive({

    positionName:"",

    description:""

});

/* ===========================
        Permission
=========================== */

const permissionList = ref([]);

const selectedPermissions = ref([]);

const loading = ref(false);

/* ===========================
      Load Permission API
=========================== */

const loadPermissions = async()=>{

    try{

        loading.value=true;

        const response=await permissionService.getAllPermissions();

        permissionList.value=response.data.data;

    }

    catch(error){

        console.error(error);

    }

    finally{

        loading.value=false;

    }

};
/* ===========================
      Permission Tree
=========================== */

const permissionTree = computed(() => {

    return permissionMenu.map(section => ({

        ...section,

        modules: section.modules.map(module => {

            // Module có submenu
            if (module.subs) {

                return {

                    ...module,

                    subs: module.subs.map(sub => ({

                        ...sub,

                        permissions: permissionList.value.filter(

                            permission =>

                                permission.module === sub.key

                        )

                    }))

                };

            }

            // Module thường
            return {

                ...module,

                permissions: permissionList.value.filter(

                    permission =>

                        permission.module === module.key

                )

            };

        })

    }));

});

/* ===========================
      View Gate (phải tick "Xem" mới được tick quyền khác)
=========================== */

const getViewPermissionId = (permissions) => {

    if (!permissions || permissions.length === 0) {

        return null;

    }

    const viewPermission = permissions.find(permission =>

        (permission.description || "")

            .trim()

            .toLowerCase()

            .startsWith("xem")

    );

    return viewPermission ? viewPermission.id : null;

};

const isPermissionCheckable = (permission, permissions) => {

    const viewId = getViewPermissionId(permissions);

    if (!viewId || permission.id === viewId) {

        return true;

    }

    return selectedPermissions.value.includes(viewId);

};

const enforceViewGate = (permissions) => {

    const viewId = getViewPermissionId(permissions);

    if (!viewId) {

        return;

    }

    if (selectedPermissions.value.includes(viewId)) {

        return;

    }

    const hasOthersChecked = permissions.some(permission =>

        permission.id !== viewId &&

        selectedPermissions.value.includes(permission.id)

    );

    if (!hasOthersChecked) {

        return;

    }

    selectedPermissions.value = selectedPermissions.value.filter(id => {

        const belongsToGroup = permissions.some(permission => permission.id === id);

        return !belongsToGroup || id === viewId;

    });

};

watch(

    selectedPermissions,

    () => {

        permissionTree.value.forEach(section => {

            section.modules.forEach(module => {

                if (module.permissions) {

                    enforceViewGate(module.permissions);

                }

                if (module.subs) {

                    module.subs.forEach(sub => {

                        enforceViewGate(sub.permissions);

                    });

                }

            });

        });

    },

    { deep: true }

);

/* ===========================
      Check All
=========================== */

const isAllSelected = computed(() => {

    const total = permissionList.value.length;

    if (total === 0) {

        return false;

    }

    return selectedPermissions.value.length === total;

});

const toggleAll = () => {

    if (isAllSelected.value) {

        selectedPermissions.value = [];

        return;

    }

    selectedPermissions.value =

        permissionList.value.map(item => item.id);

};

/* ===========================
      Module
=========================== */

const getModulePermissionIds = (module) => {

    let ids = [];

    if (module.permissions) {

        ids.push(

            ...module.permissions.map(item => item.id)

        );

    }

    if (module.subs) {

        module.subs.forEach(sub => {

            ids.push(

                ...sub.permissions.map(item => item.id)

            );

        });

    }

    return ids;

};

const isModuleSelected = (module) => {

    const ids = getModulePermissionIds(module);

    if (ids.length === 0) {

        return false;

    }

    return ids.every(id =>

        selectedPermissions.value.includes(id)

    );

};

const toggleModule = (module) => {

    const ids = getModulePermissionIds(module);

    const checked = ids.every(id =>

        selectedPermissions.value.includes(id)

    );

    if (checked) {

        selectedPermissions.value =

            selectedPermissions.value.filter(id =>

                !ids.includes(id)

            );

    } else {

        ids.forEach(id => {

            if (

                !selectedPermissions.value.includes(id)

            ) {

                selectedPermissions.value.push(id);

            }

        });

    }

};
/* ===========================
        Reset Form
=========================== */

const resetForm = () => {

    form.positionName = "";

    form.description = "";

    selectedPermissions.value = [];

};

/* ===========================
            Save
=========================== */

const save = async () => {

    if (!form.positionName.trim()) {

        showNotice({

            type:"error",

            title:"Thiếu thông tin",

            message:"Vui lòng nhập tên chức danh"

        });

        return;

    }

    const payload = {

        positionName: form.positionName,

        description: form.description,

        permissions: selectedPermissions.value.map(id => ({
            id
        }))

    };

    try {

        loading.value = true;

        if (props.mode === "add") {

            await PositionApi.them(payload);

        } else {

            await PositionApi.capNhat(

                props.data.positionId,

                payload

            );

        }

        emit("success");

        emit("close");

    } catch (error) {

        console.error(error);

        showNotice({

            type:"error",

            title:"Có lỗi xảy ra",

            message:"Vui lòng kiểm tra lại thông tin và thử lại."

        });

    } finally {

        loading.value = false;

    }

};

/* ===========================
        Edit
=========================== */

watch(

    () => props.data,

    (value) => {

        resetForm();

        if (!value) {

            return;

        }

        form.positionName = value.positionName;

        form.description = value.description;

selectedPermissions.value = value.permissions
    ? value.permissions.map(item => item.id)
    : [];

    },

    {

        immediate: true

    }

);

/* ===========================
        Mounted
=========================== */

onMounted(async () => {

    await loadPermissions();

});
 </script>
 <style scoped>

/* ================= Overlay ================= */

.modal-overlay{
    position:fixed;
    inset:0;
    background:rgba(15,23,42,.46);
    display:flex;
    justify-content:center;
    align-items:center;
    z-index:9999;
    animation:fade .25s;
}

@keyframes fade{
    from{opacity:0;}
    to{opacity:1;}
}

/* ================= Container ================= */

.modal-container{
    width:1200px;
    max-width:95%;
    height:90vh;
    background:#fff;
    border:1px solid #f3d6e3;
    border-radius:8px;
    display:flex;
    flex-direction:column;
    overflow:hidden;
    box-shadow:0 18px 45px rgba(15,23,42,.16);
}

/* ================= Header ================= */

.modal-header{
    padding:18px 24px;
    display:flex;
    justify-content:space-between;
    align-items:flex-start;
    border-bottom:1px solid #f3d6e3;
    background:#fff;
}

.modal-header h3{
    margin:0;
    font-size:1.3rem;
    font-weight:800;
    color:#f55d9b;
}

.modal-header p{
    margin-top:4px;
    color:#6b7280;
    font-size:.85rem;
}

.btn-close{
    width:36px;
    height:36px;
    border:1px solid #f3d6e3;
    border-radius:8px;
    background:#fff;
    color:#202636;
    cursor:pointer;
    font-size:14px;
    transition:.2s;
}

.btn-close:hover{
    background:#fff0f7;
    color:#d63384;
}

/* ================= Body ================= */

.modal-body{
    flex:1;
    display:grid;
    grid-template-columns:300px 1fr;
    overflow:hidden;
}

/* ================= Left ================= */

.left-panel{
    padding:22px;
    border-right:1px solid #f3d6e3;
    overflow:auto;
}

.form-group{
    margin-bottom:20px;
}

.form-group label{
    display:block;
    font-weight:800;
    font-size:.76rem;
    letter-spacing:0;
    text-transform:uppercase;
    color:#b4557d;
    margin-bottom:7px;
}

.form-group span{
    color:#dc3545;
}

.form-group input,
.form-group textarea{
    width:100%;
    border:1px solid #f3d6e3;
    background:#fffafd;
    border-radius:8px;
    padding:10px 12px;
    outline:none;
    transition:.18s;
    font-size:.88rem;
    color:#202636;
}

.form-group textarea{
    resize:none;
}

.form-group input:hover,
.form-group textarea:hover{
    border-color:#efbdd2;
    background:#fff;
    box-shadow:0 0 0 .12rem rgba(245,93,155,.08);
}

.form-group input:focus,
.form-group textarea:focus{
    border-color:#f55d9b;
    background:#fff;
    box-shadow:0 0 0 .18rem rgba(245,93,155,.12);
}

/* ================= Right ================= */

.right-panel{
    padding:22px;
    overflow:auto;
}

.permission-header{
    display:flex;
    justify-content:space-between;
    align-items:center;
    margin-bottom:18px;
}

.permission-header h4{
    margin:0;
    font-size:1rem;
    font-weight:800;
    color:#202636;
}

.btn-select-all{
    border:none;
    background:#f55d9b;
    color:#fff;
    padding:8px 16px;
    border-radius:8px;
    cursor:pointer;
    font-size:.82rem;
    font-weight:700;
    transition:.2s;
}

.btn-select-all:hover{
    background:#ec4d8d;
}

/* ================= Section ================= */

.permission-section{
    margin-bottom:24px;
}

.section-title{
    display:flex;
    align-items:center;
    font-size:.95rem;
    font-weight:800;
    color:#202636;
    margin-bottom:12px;
    cursor:pointer;
    user-select:none;
}

.section-left{
    display:flex;
    align-items:center;
    gap:8px;
}

.section-title i{
    font-size:.85rem;
    color:#b4557d;
}

/* ================= Card ================= */

.permission-card{
    border:1px solid #f3d6e3;
    border-radius:10px;
    margin-bottom:14px;
    overflow:hidden;
    transition:.18s;
}

.permission-card:hover{
    border-color:#f55d9b;
    box-shadow:0 8px 20px rgba(245,93,155,.1);
}

.module-header{
    display:flex;
    justify-content:space-between;
    align-items:center;
    padding:12px 14px;
    background:#fff0f7;
    border-bottom:1px solid #f3d6e3;
}

.module-info{
    display:flex;
    align-items:center;
    gap:10px;
    font-weight:700;
    font-size:.88rem;
    color:#202636;
}

.module-info i{
    color:#f55d9b;
    font-size:1rem;
}

.module-header input[type="checkbox"]{
    accent-color:#f55d9b;
    width:16px;
    height:16px;
    cursor:pointer;
}

/* ================= Permission ================= */

.permission-list{
    display:grid;
    grid-template-columns:repeat(2,1fr);
    gap:8px;
    padding:14px;
}

.permission-list label{
    display:flex;
    align-items:center;
    gap:8px;
    padding:7px 10px;
    border-radius:8px;
    cursor:pointer;
    font-size:.82rem;
    color:#374151;
    transition:.15s;
}

.permission-list label:hover{
    background:#fffafd;
}

.permission-list input{
    accent-color:#f55d9b;
    width:15px;
    height:15px;
}

/* ================= Sub ================= */

.sub-module{
    margin:0 14px 14px;
    border:1px dashed #f3d6e3;
    border-radius:10px;
    overflow:hidden;
}

.sub-title{
    padding:9px 14px;
    background:#fafafa;
    font-weight:700;
    font-size:.82rem;
    color:#4b5563;
    border-bottom:1px dashed #f3d6e3;
}

/* ================= Footer ================= */

.modal-footer{
    display:flex;
    justify-content:flex-end;
    gap:10px;
    padding:16px 22px;
    border-top:1px solid #f3d6e3;
    background:#fffafd;
}

.btn-cancel{
    border:1px solid #f3d6e3;
    background:#fff;
    color:#202636;
    padding:9px 18px;
    border-radius:8px;
    cursor:pointer;
    font-size:.85rem;
    font-weight:700;
    transition:.2s;
}

.btn-cancel:hover{
    border-color:#efbdd2;
    background:#fff0f7;
}

.btn-save{
    border:none;
    background:#f55d9b;
    color:#fff;
    padding:9px 20px;
    border-radius:8px;
    cursor:pointer;
    font-size:.85rem;
    font-weight:700;
    transition:.2s;
}

.btn-save:hover{
    background:#ec4d8d;
}

/* ================= Scroll ================= */

.left-panel::-webkit-scrollbar,
.right-panel::-webkit-scrollbar{
    width:8px;
}

.left-panel::-webkit-scrollbar-thumb,
.right-panel::-webkit-scrollbar-thumb{
    background:#f3d6e3;
    border-radius:10px;
}

.left-panel::-webkit-scrollbar-thumb:hover,
.right-panel::-webkit-scrollbar-thumb:hover{
    background:#efbdd2;
}

/* ================= Responsive ================= */

@media(max-width:992px){

    .modal-container{
        width:98%;
        height:95vh;
    }

    .modal-body{
        grid-template-columns:1fr;
    }

    .left-panel{
        border-right:none;
        border-bottom:1px solid #f3d6e3;
    }

    .permission-list{
        grid-template-columns:1fr;
    }

}

</style>