<template>
  <div class="client-layout">
    <ClientHeader />

    <main class="min-vh-100">
      <router-view></router-view>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import ClientHeader from '@/components/client/ClientHeader.vue'
import ClientFooter from '@/components/client/ClientFooter.vue'
</script>
