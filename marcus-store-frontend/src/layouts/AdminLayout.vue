<template>
  <div class="admin-layout">
    <admin-sidebar class="no-print" />

    <main class="main-content">
      <admin-header class="no-print" />
      <div class="admin-view">
        <router-view></router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import AdminHeader from '@/components/admin/AdminHeader.vue'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}
.main-content {
  flex: 1;
  min-width: 0;
  height: 100vh;
  background: #fff7fa;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.admin-view {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}
</style>
