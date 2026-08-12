import { computed } from 'vue'

/**
 * Hạng thành viên khách hàng — dùng chung cho mọi nơi cần render tier.
 * Ngưỡng & tên hạng bám theo CustomerTable.vue (admin) để đồng bộ:
 *  - Đồng     : <  50.000.000
 *  - Bạc      : ≥  50.000.000
 *  - Vàng     : ≥ 150.000.000
 *  - Kim Cương : ≥ 300.000.000
 */
export const TIER_THRESHOLDS = {
  BRONZE: 0,
  SILVER: 50_000_000,
  GOLD: 150_000_000,
  DIAMOND: 300_000_000,
}

export function getTier(totalSpent) {
  const amount = Number(totalSpent) || 0

  if (amount >= TIER_THRESHOLDS.DIAMOND) {
    return { label: 'Kim Cương', icon: '💎', cls: 'diamond', rank: 4 }
  }
  if (amount >= TIER_THRESHOLDS.GOLD) {
    return { label: 'Vàng', icon: '🥇', cls: 'gold', rank: 3 }
  }
  if (amount >= TIER_THRESHOLDS.SILVER) {
    return { label: 'Bạc', icon: '🥈', cls: 'silver', rank: 2 }
  }
  return { label: 'Đồng', icon: '🥉', cls: 'bronze', rank: 1 }
}

export function formatVND(amount) {
  const num = Number(amount) || 0
  return num.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })
}

/**
 * Composable tiện dụng cho component:
 *  const { tier, spent, formattedSpent } = useMembershipTier(() => user.totalSpent)
 */
export function useMembershipTier(spentRef) {
  const tier = computed(() => getTier(spentRef.value))
  const formattedSpent = computed(() => formatVND(spentRef.value))

  // Số tiền còn thiếu để lên hạng kế tiếp (0 nếu đã cao nhất)
  const nextTierGap = computed(() => {
    const amount = Number(spentRef.value) || 0
    if (amount >= TIER_THRESHOLDS.DIAMOND) return 0
    if (amount >= TIER_THRESHOLDS.GOLD) return TIER_THRESHOLDS.DIAMOND - amount
    if (amount >= TIER_THRESHOLDS.SILVER) return TIER_THRESHOLDS.GOLD - amount
    return TIER_THRESHOLDS.SILVER - amount
  })

  return { tier, formattedSpent, nextTierGap }
}
