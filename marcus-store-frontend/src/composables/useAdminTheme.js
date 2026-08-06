import { computed, ref } from 'vue'

const STORAGE_KEY = 'ADMIN_COLOR_THEME'
const savedTheme = localStorage.getItem(STORAGE_KEY)
const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
const theme = ref(savedTheme === 'dark' || savedTheme === 'light' ? savedTheme : prefersDark ? 'dark' : 'light')

const applyTheme = () => {
  document.documentElement.dataset.adminTheme = theme.value
}

applyTheme()

export function useAdminTheme() {
  const isDark = computed(() => theme.value === 'dark')

  const toggleTheme = () => {
    theme.value = isDark.value ? 'light' : 'dark'
    localStorage.setItem(STORAGE_KEY, theme.value)
    applyTheme()
  }

  return { theme, isDark, toggleTheme }
}
