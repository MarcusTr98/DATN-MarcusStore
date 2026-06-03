<template>
  <div class="admin-layout">
    <admin-sidebar />

    <main class="main-content">
      <admin-header />
      <router-view></router-view>
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
  min-height: 100vh;
}
.main-content {
  flex: 1;
  background: #fff7fa;
}
</style>
