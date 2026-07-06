<template>
  <div class="client-layout">
    <ClientHeader />
    <FaqOffcanvas />
    <main class="min-vh-100">
      <router-view></router-view>
    </main>

    <ClientFooter />
    <ChatWidgetRouter />
  </div>
</template>

<script setup>
import ClientHeader from '@/components/client/ClientHeader.vue'
import ClientFooter from '@/components/client/ClientFooter.vue'
import FaqOffcanvas from '@/components/client/FaqOffcanvas.vue'
import ChatWidgetRouter from '@/components/chat/ChatWidgetRouter.vue'
</script>
