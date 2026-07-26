<script setup>
import { onMounted, onBeforeUnmount, computed } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import { injectFallbackScript, removeFallbackScript } from '@/utils/chatFallback'
import ChatWidget from './ChatWidget.vue'

const chatStore = useChatStore()

// Biến reactive để kiểm tra token
const isLoggedIn = computed(() => !!localStorage.getItem('ACCESS_TOKEN'))
const token = localStorage.getItem('ACCESS_TOKEN')

onMounted(async () => {
  injectFallbackScript()

  // Hỏi trạng thái Admin
  await chatStore.checkPresence()

  // Chỉ Kết nối STOMP khi đã đăng nhập (Guest không cần socket)
  if (isLoggedIn.value) {
    chatStore.connectSocket(token)
  }
})

onBeforeUnmount(() => {
  chatStore.disconnectSocket()
  removeFallbackScript()
})
</script>

<template>
  <ChatWidget
    v-if="chatStore.isAdminOnline || chatStore.hasActiveSession"
    :is-logged-in="isLoggedIn"
  />
</template>
