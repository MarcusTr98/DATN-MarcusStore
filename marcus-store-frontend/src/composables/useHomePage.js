import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '@/utils/api'
import { watch } from 'vue'

// Marcus refactor: tách settings, hero slider và đồng hồ khỏi Home.vue.
export function useHomePage() {
  // Khởi tạo state nội bộ để reactivity 100%
  const sysSettings = ref({})

  const fetchSettings = async () => {
    try {
      const res = await api.get('/public/settings')
      sysSettings.value = res.data
    } catch (error) {
      console.error('Lỗi khi tải cấu hình trang chủ:', error)
    }
  }

  // Marcus sửa: dữ liệu tĩnh không cần ref, giảm proxy reactivity không cần thiết.
  const categories = [
    { name: 'Điện thoại', icon: 'fas fa-mobile-alt', to: '/category/dien-thoai' },
    { name: 'iPad / Tablet', icon: 'fas fa-tablet-alt', to: '/category/may-tinh-bang' },
    { name: 'Âm thanh', icon: 'fas fa-headphones', to: '/category/am-thanh' },
    { name: 'Đồng hồ TM', icon: 'far fa-clock', to: '/category/dong-ho-thong-minh' },
    { name: 'Sạc & Pin', icon: 'fas fa-plug', to: '/category/sac-pin' },
    { name: 'Ốp lưng', icon: 'fas fa-shield-alt', to: '/category/op-lung' },
  ]

  const defaultHeroSlides = [
    {
      kicker: 'FLAGSHIP 2026',
      name: 'Samsung Galaxy S26 Ultra',
      price: '26.990.000đ',
      tag: 'Trả góp 0%',
    },
    {
      kicker: 'BÁN CHẠY NHẤT',
      name: 'iPhone 17 Pro Max',
      price: '32.990.000đ',
      tag: 'Thu cũ trợ giá 5.000.000đ',
    },
    { kicker: 'ĐÁNG MUA', name: 'iPad Air M3', price: '16.990.000đ', tag: 'Tặng bút Apple Pencil' },
  ]

  const heroSlides = computed(() => {
    const slidesData = sysSettings.value.HOME_HERO_SLIDES
    if (!slidesData) return defaultHeroSlides
    try {
      const parsed = typeof slidesData === 'string' ? JSON.parse(slidesData) : slidesData
      if (!Array.isArray(parsed) || !parsed.length) return defaultHeroSlides

      // Marcus thêm: chuẩn hóa dữ liệu CMS để slide thiếu field không làm vỡ hero.
      return parsed.map((slide, index) => {
        const fallback = defaultHeroSlides[index % defaultHeroSlides.length]
        return {
          kicker: String(slide?.kicker || fallback.kicker),
          name: String(slide?.name || fallback.name),
          price: String(slide?.price || fallback.price),
          tag: String(slide?.tag || fallback.tag),
        }
      })
    } catch {
      return defaultHeroSlides
    }
  })

  const activeHeroSlide = ref(0)
  let heroSliderTimer = null

  const heroBenefits = [
    { text: 'Giao nhanh 2h', icon: 'fas fa-bolt', position: 'chip-top-right' },
    { text: 'Chính hãng 100%', icon: 'fas fa-shield-alt', position: 'chip-bottom-left' },
    { text: 'Bảo hành uy tín', icon: 'fas fa-award', position: 'chip-top-left' },
    { text: 'Đổi trả dễ dàng', icon: 'fas fa-sync-alt', position: 'chip-bottom-right' },
  ]

  const currentSlide = computed(() => {
    const slides = heroSlides.value
    return slides[activeHeroSlide.value % slides.length] || slides[0]
  })

  const stopHeroSlider = () => {
    if (heroSliderTimer) {
      clearInterval(heroSliderTimer)
      heroSliderTimer = null
    }
  }

  const resumeHeroSlider = () => {
    stopHeroSlider()

    heroSliderTimer = setInterval(() => {
      if (!document.hidden && heroSlides.value.length > 1) {
        activeHeroSlide.value = (activeHeroSlide.value + 1) % heroSlides.value.length
      }
    }, 4000)
  }

  const pauseHeroSlider = () => stopHeroSlider()

  /*  Đồng hồ hiện tại trên màn hình điện thoại  */
  const now = ref(new Date())
  let clockTimer = null

  const currentTime = computed(() =>
    now.value.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false }),
  )

  const weekdays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']

  const currentDate = computed(() => {
    const d = now.value
    const weekday = weekdays[d.getDay()]
    const day = String(d.getDate()).padStart(2, '0')
    const month = String(d.getMonth() + 1).padStart(2, '0')
    return `${weekday}, ${day} tháng ${month}`
  })

  const handleVisibilityChange = () => {
    if (document.hidden) pauseHeroSlider()
    else resumeHeroSlider()
  }

  // Marcus thêm: nếu CMS đổi số slide thì luôn đưa index về miền hợp lệ.
  watch(
    () => heroSlides.value.length,
    (length) => {
      if (length > 0 && activeHeroSlide.value >= length) activeHeroSlide.value = 0
      // Marcus sửa: dữ liệu CMS tải bất đồng bộ vẫn phải khởi động lại slider.
      if (length > 1 && !document.hidden) resumeHeroSlider()
    },
  )

  // Marcus refactor: một lifecycle quản lý toàn bộ timer và listener của trang Home.
  onMounted(() => {
    fetchSettings()
    resumeHeroSlider()
    clockTimer = setInterval(() => {
      now.value = new Date()
    }, 1000)
    document.addEventListener('visibilitychange', handleVisibilityChange)
  })

  onUnmounted(() => {
    stopHeroSlider()
    clearInterval(clockTimer)
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  /*  Banner thương hiệu  */
  const brandBanners = [
    {
      name: 'iPhone chính hãng',
      kicker: 'APPLE VN/A',
      icon: 'fab fa-apple',
      to: '/category/dien-thoai',
      bg: 'linear-gradient(135deg,#14151a,#2a2b33)',
    },
    {
      name: 'Samsung Galaxy',
      kicker: 'GALAXY AI',
      icon: 'fas fa-mobile-alt',
      to: '/category/dien-thoai',
      bg: 'linear-gradient(135deg,#e1121c,#8c0e15)',
    },
  ]

  // Marcus refactor: chỉ public dữ liệu mà giao diện Home sử dụng.
  return {
    sysSettings,
    categories,
    heroSlides,
    activeHeroSlide,
    heroBenefits,
    currentSlide,
    currentTime,
    currentDate,
    brandBanners,
  }
}
