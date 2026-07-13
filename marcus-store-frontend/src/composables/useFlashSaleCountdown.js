import { ref, computed, onUnmounted, watch } from 'vue'

/**
 * Composable quản lý bộ đếm ngược cho Flash Sale.
 *
 * Đầu vào:
 *   - slotsRef: Ref<Array<{ status, startDate, endDate }>> (vd. store.clientSlots)
 *
 * Hành vi:
 *   - Tự động chọn slot "đang diễn ra" (status=2) ưu tiên đầu tiên.
 *   - Nếu không có ACTIVE thì chọn slot "sắp diễn ra" (status=1) đầu tiên.
 *   - Trả về:
 *       + label: 'KẾT THÚC SAU' | 'BẮT ĐẦU SAU' | ''
 *       + timer: { hours: 'HH', minutes: 'MM', seconds: 'SS' }  (đã padStart(2, '0'))
 *       + targetSlot: ref tới slot đang được đếm ngược
 *   - Khi đồng hồ chạm 00:00:00, gọi callback onExpire() để FE refetch dữ liệu mới.
 *
 * Usage:
 *   const { label, timer, targetSlot } = useFlashSaleCountdown(
 *       () => flashSaleStore.clientSlots,
 *       () => flashSaleStore.fetchClientSlots(20),
 *   )
 */
export function useFlashSaleCountdown(getSlots, onExpire) {
    const now = ref(Date.now())
    let intervalId = null
    const hasFiredExpire = ref(false)

    // Chọn slot đang làm mục tiêu đếm ngược
    const targetSlot = computed(() => {
        const slots = getSlots()
        if (!Array.isArray(slots) || slots.length === 0) return null
        // Ưu tiên 1: slot đang diễn ra (status=2)
        const active = slots.find((s) => Number(s.status) === 2)
        if (active) return active
        // Ưu tiên 2: slot sắp diễn ra (status=1)
        const upcoming = slots.find((s) => Number(s.status) === 1)
        return upcoming || null
    })

    // Xác định target time & label dựa trên slot được chọn
    const countdownInfo = computed(() => {
        const slot = targetSlot.value
        if (!slot) {
            return { label: '', targetTs: null }
        }
        const status = Number(slot.status)
        const target = status === 2
            ? new Date(slot.endDate).getTime()
            : new Date(slot.startDate).getTime()
        const label = status === 2 ? 'KẾT THÚC SAU' : 'BẮT ĐẦU SAU'
        return { label, targetTs: target }
    })

    // Số giây còn lại (>=0)
    const remainingSeconds = computed(() => {
        const { targetTs } = countdownInfo.value
        if (!targetTs) return 0
        const diff = Math.floor((targetTs - now.value) / 1000)
        return diff > 0 ? diff : 0
    })

    // Tính HH:MM:SS
    const timer = computed(() => {
        const total = remainingSeconds.value
        const hours = Math.floor(total / 3600)
        const minutes = Math.floor((total % 3600) / 60)
        const seconds = total % 60
        return {
            hours: String(hours).padStart(2, '0'),
            minutes: String(minutes).padStart(2, '0'),
            seconds: String(seconds).padStart(2, '0'),
        }
    })

    const label = computed(() => countdownInfo.value.label)

    // Watcher: khi timer chạm 0 -> fire expire callback 1 lần, reset cờ khi slot thay đổi
    watch(remainingSeconds, (val) => {
        if (val === 0 && countdownInfo.value.targetTs && !hasFiredExpire.value) {
            hasFiredExpire.value = true
            if (typeof onExpire === 'function') {
                // Chạy async để không chặn reactivity
                Promise.resolve().then(() => {
                    try {
                        onExpire()
                    } catch (e) {
                        console.error('[useFlashSaleCountdown] onExpire error:', e)
                    }
                })
            }
        }
    })

    // Reset cờ expire khi target slot thay đổi (sau khi refetch)
    watch(targetSlot, () => {
        hasFiredExpire.value = false
    })

    // Bắt đầu interval
    function start() {
        stop()
        now.value = Date.now()
        intervalId = setInterval(() => {
            now.value = Date.now()
        }, 1000)
    }

    function stop() {
        if (intervalId != null) {
            clearInterval(intervalId)
            intervalId = null
        }
    }

    start()
    onUnmounted(stop)

    return {
        timer,
        label,
        targetSlot,
        remainingSeconds,
        start,
        stop,
    }
}