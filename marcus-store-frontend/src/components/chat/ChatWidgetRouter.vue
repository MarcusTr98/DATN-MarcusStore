<script setup>
import { onMounted, onBeforeUnmount, computed } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import { injectFallbackScript } from '@/utils/chatFallback'
import ChatWidget from './ChatWidget.vue'

const chatStore = useChatStore()

// Biến reactive để kiểm tra token
const isLoggedIn = computed(() => !!localStorage.getItem('ACCESS_TOKEN'))
const token = localStorage.getItem('ACCESS_TOKEN')
const username = localStorage.getItem('USERNAME')

onMounted(async () => {
  injectFallbackScript()

  // Hỏi trạng thái Admin
  await chatStore.checkPresence()

  // Nếu là User => Kết nối STOMP để hóng Admin
  if (isLoggedIn.value) {
    chatStore.connectSocket(token, username)
  }
})

onBeforeUnmount(() => {
  chatStore.disconnectSocket()
})
</script>

<template>
  <!-- Render Khung chat xịn kế bên Zalo nếu có Admin online -->
  <ChatWidget v-if="isLoggedIn && chatStore.isAdminOnline" />
</template>
