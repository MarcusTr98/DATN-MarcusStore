import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import VueApexCharts from 'vue3-apexcharts'

// Import CSS
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import 'bootstrap-icons/font/bootstrap-icons.css'
import './assets/css/main.css'
import './assets/css/variables.css'

const pinia = createPinia()
const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(VueApexCharts)

// Init Flash Sale WebSocket real-time notifications (CANCELLED/EXPIRED events)
// Phải gọi SAU khi pinia đã được app.use để store hoạt động
import { useFlashSaleStore } from '@/stores/FlashSaleStore'
const flashSaleStore = useFlashSaleStore()
flashSaleStore.initWebSocket()

app.mount('#app')
