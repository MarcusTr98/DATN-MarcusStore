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
.client-layout {
  min-width: 0;
  min-height: 100vh;
  overflow-x: clip;
  background: #f8fafc;
}

.client-layout > main {
  width: 100%;
  min-width: 0;
}

@media (max-width: 767px) {
  .client-layout > main {
    min-height: calc(100vh - 150px) !important;
  }

  /* Marcus nâng cấp: khoảng thở và vùng chạm thống nhất cho các trang Client mobile. */
  .client-layout .container,
  .client-layout .container-fluid {
    max-width: 100%;
    padding-inline: 12px;
  }

  .client-layout img,
  .client-layout video,
  .client-layout canvas,
  .client-layout svg {
    max-width: 100%;
  }

  .client-layout input,
  .client-layout select,
  .client-layout textarea {
    max-width: 100%;
    font-size: 16px;
  }

  .client-layout button,
  .client-layout .btn,
  .client-layout a[role='button'] {
    min-height: 42px;
  }

  .client-layout .table-responsive,
  .client-layout .table-wrapper,
  .client-layout .table-container {
    width: 100%;
    max-width: calc(100vw - 24px);
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .client-layout .modal-dialog,
  .client-layout .modal-content,
  .client-layout [class*='modal-card'] {
    width: calc(100vw - 24px) !important;
    max-width: calc(100vw - 24px) !important;
    max-height: calc(100dvh - 24px);
    margin: 12px auto !important;
  }
}

@media (max-width: 420px) {
  .client-layout .container,
  .client-layout .container-fluid {
    padding-inline: 10px;
  }
}

body.marcus-contact-panel-open .ai-trigger,
body.marcus-contact-panel-open .chat-trigger-btn,
body.marcus-contact-panel-open #marcus-floating-actions,
body.marcus-contact-panel-open .btn-floating-faq {
  opacity: 0 !important;
  visibility: hidden !important;
  pointer-events: none !important;
}
</style>
