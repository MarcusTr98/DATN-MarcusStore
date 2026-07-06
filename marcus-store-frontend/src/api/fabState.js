import { reactive } from 'vue'

// State dùng chung cho cụm nút liên hệ nổi (FAB)
// Được dùng bởi cả chatFallback.js (Zalo/Facebook) và ChatWidget.vue (Chat CSKH)
// để tất cả cùng thu vào/phóng ra đồng bộ với nhau.
export const fabState = reactive({
  open: false, // true = đang phóng ra, false = đã thu gọn
})

export function toggleFabMenu() {
  fabState.open = !fabState.open
}

export function openFabMenu() {
  fabState.open = true
}

export function closeFabMenu() {
  fabState.open = false
}
