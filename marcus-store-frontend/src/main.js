import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// 1. Import Bootstrap 5 CSS & Bundle JS
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'

// 2. Import Bootstrap Icons
import 'bootstrap-icons/font/bootstrap-icons.css'

// 3. Import File CSS
import './assets/css/main.css'

const app = createApp(App)

// Gắn Pinia (quản lý state Giỏ hàng/Token)
app.use(createPinia())

// Gắn Vue Router
app.use(router)

// Mount ứng dụng vào thẻ <div id="app"> trong index.html
app.mount('#app')
