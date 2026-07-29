<template>
  <div class="client-layout">
    <ClientHeader />
    <FaqOffcanvas />
    <main class="min-vh-100">
      <router-view></router-view>
    </main>

    <ClientFooter />
    <ChatWidgetRouter />
    <WelcomeVoucherModal />
  </div>
</template>

<script setup>
import ClientHeader from '@/components/client/ClientHeader.vue'
import ClientFooter from '@/components/client/ClientFooter.vue'
import FaqOffcanvas from '@/components/client/FaqOffcanvas.vue'
import ChatWidgetRouter from '@/components/chat/ChatWidgetRouter.vue'
import WelcomeVoucherModal from '@/components/WelcomeVoucherModal.vue'
</script>

<style>
body.marcus-contact-panel-open .ai-trigger,
body.marcus-contact-panel-open .chat-trigger-btn,
body.marcus-contact-panel-open #marcus-floating-actions,
body.marcus-contact-panel-open .btn-floating-faq {
  opacity: 0 !important;
  visibility: hidden !important;
  pointer-events: none !important;
}
</style>
