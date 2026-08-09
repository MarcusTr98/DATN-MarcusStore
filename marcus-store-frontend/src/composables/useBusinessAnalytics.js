import { computed, onMounted, ref } from 'vue'
import analyticsApi from '@/api/analyticsApi'

const DAY_IN_MILLISECONDS = 86_400_000

function toDateInput(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function dateDaysAgo(numberOfDays) {
  const date = new Date()
  date.setDate(date.getDate() - numberOfDays)
  return toDateInput(date)
}

function unwrap(response) {
  return response?.data?.data
}

export function useBusinessAnalytics() {
  const today = toDateInput(new Date())
  const activePreset = ref('30d')
  const fromDate = ref(dateDaysAgo(29))
  const toDate = ref(today)
  const overview = ref(null)
  const dailyTrend = ref([])
  const products = ref([])
  const cancellationReasons = ref([])
  const warrantyQuality = ref(null)
  const behaviorFunnel = ref(null)
  const loading = ref(false)
  const errorMessage = ref('')
  const aiReport = ref(null)
  const aiUsage = ref(null)
  const aiLoading = ref(false)
  const aiError = ref('')
  let requestVersion = 0

  const presets = [
    { key: '30d', label: '30 ngày' },
    { key: '90d', label: '90 ngày' },
    { key: '6m', label: '6 tháng' },
    { key: 'year', label: 'Năm nay' },
    { key: 'all', label: 'Toàn bộ' },
  ]

  const numberOfDays = computed(() => {
    if (!fromDate.value || !toDate.value) return 0
    return (
      Math.floor(
        (new Date(`${toDate.value}T00:00:00`) - new Date(`${fromDate.value}T00:00:00`)) /
          DAY_IN_MILLISECONDS,
      ) + 1
    )
  })

  // Marcus thêm: khoảng dài gom theo tháng; khoảng ngắn giữ từng ngày.
  const trend = computed(() => {
    if (numberOfDays.value <= 120) {
      return dailyTrend.value.map((point) => ({ ...point, label: point.date }))
    }

    const monthly = new Map()
    dailyTrend.value.forEach((point) => {
      const monthKey = point.date.slice(0, 7)
      const current = monthly.get(monthKey) || {
        date: `${monthKey}-01`,
        label: monthKey,
        completedSales: 0,
        completedOrders: 0,
        unitsSold: 0,
      }
      current.completedSales += Number(point.completedSales || 0)
      current.completedOrders += Number(point.completedOrders || 0)
      current.unitsSold += Number(point.unitsSold || 0)
      monthly.set(monthKey, current)
    })
    return [...monthly.values()]
  })

  const periodLabel = computed(() => {
    const period = overview.value?.period
    if (!period) return ''
    return `${formatDate(period.fromDate)} – ${formatDate(period.toDate)}`
  })

  /*
   * Marcus thêm: dự báo tuyến tính là lớp phân tích thống kê minh bạch.
   * Chỉ dùng chuỗi doanh thu tổng hợp đang hiển thị, không gửi dữ liệu khách hàng.
   */
  const forecast = computed(() => {
    const points = trend.value
    const sampleSize = numberOfDays.value > 120 ? 6 : Math.min(30, points.length)
    const horizon = numberOfDays.value > 120 ? 3 : 7
    const sample = points.slice(-sampleSize)
    if (sample.length < 3) return null

    const values = sample.map((point) => Number(point.completedSales || 0))
    const regression = linearRegression(values)
    const future = Array.from({ length: horizon }, (_, index) => {
      const position = values.length + index
      const predicted = Math.max(0, regression.intercept + regression.slope * position)
      return {
        label: nextPeriodLabel(sample.at(-1).date, index + 1, numberOfDays.value > 120),
        predictedSales: Math.round(predicted),
        lowerBound: Math.max(0, Math.round(predicted - regression.rmse)),
        upperBound: Math.round(predicted + regression.rmse),
      }
    })

    const recentAverage = average(values.slice(-Math.min(3, values.length)))
    const forecastAverage = average(future.map((point) => point.predictedSales))
    return {
      future,
      confidence:
        regression.rSquared >= 0.65 ? 'Khá' : regression.rSquared >= 0.35 ? 'Trung bình' : 'Thấp',
      rSquared: regression.rSquared,
      changePercent:
        recentAverage > 0 ? ((forecastAverage - recentAverage) / recentAverage) * 100 : null,
      forecastTotal: future.reduce((sum, point) => sum + point.predictedSales, 0),
      unit: numberOfDays.value > 120 ? 'tháng' : 'ngày',
    }
  })

  const analysis = computed(() => {
    if (!overview.value) return null
    const salesChange = overview.value.completedSales?.changePercent
    const orderChange = overview.value.completedOrders?.changePercent
    const cancellationChange = overview.value.cancellationRate?.percentagePointChange || 0
    const aovChange = overview.value.averageOrderValue?.changePercent
    const refund = Number(overview.value.successfulRefundAmount?.currentValue || 0)
    const sales = Number(overview.value.completedSales?.currentValue || 0)
    const refundRate = sales > 0 ? (refund / sales) * 100 : 0

    const score =
      directionScore(salesChange) +
      directionScore(orderChange) -
      (cancellationChange > 1 ? 1 : cancellationChange < -1 ? -1 : 0)
    const status =
      score >= 2
        ? { key: 'growth', label: 'Đang tăng trưởng', icon: 'bi bi-graph-up-arrow' }
        : score <= -2
          ? { key: 'decline', label: 'Có dấu hiệu suy giảm', icon: 'bi bi-graph-down-arrow' }
          : { key: 'steady', label: 'Đang đi ngang', icon: 'bi bi-activity' }

    const risingProducts = products.value
      .filter((product) => product.unitsChangePercent > 0)
      .sort((a, b) => b.unitsChangePercent - a.unitsChangePercent)
    const decliningProducts = products.value
      .filter((product) => product.unitsChangePercent < 0)
      .sort((a, b) => a.unitsChangePercent - b.unitsChangePercent)

    const insights = [
      {
        tone: changeTone(salesChange),
        icon: salesChange >= 0 ? 'bi bi-arrow-up-right' : 'bi bi-arrow-down-right',
        title: 'Đà doanh thu',
        text: metricNarrative('Doanh thu đã thu của đơn hoàn tất', salesChange),
      },
      {
        tone: cancellationChange > 1 ? 'warning' : 'positive',
        icon: cancellationChange > 1 ? 'bi bi-exclamation-triangle' : 'bi bi-check2-circle',
        title: 'Chất lượng đơn hàng',
        text:
          cancellationChange > 1
            ? `Tỷ lệ hủy tăng ${formatNumber(Math.abs(cancellationChange))} điểm phần trăm; cần kiểm tra thanh toán và lý do hủy.`
            : `Tỷ lệ hủy ${cancellationChange < 0 ? 'giảm' : 'ổn định'} so với kỳ trước.`,
      },
      {
        tone: changeTone(aovChange),
        icon: 'bi bi-receipt',
        title: 'Giá trị đơn',
        text: metricNarrative('Giá trị đơn trung bình', aovChange),
      },
      {
        tone: refundRate > 5 ? 'warning' : 'neutral',
        icon: 'bi bi-arrow-counterclockwise',
        title: 'Áp lực hoàn tiền',
        text:
          refund > 0
            ? `Tiền hoàn tương đương ${formatNumber(refundRate)}% doanh thu hoàn tất trong kỳ.`
            : 'Chưa phát sinh giao dịch hoàn tiền thành công trong kỳ.',
      },
    ]

    return {
      status,
      insights,
      risingProduct: risingProducts[0] || null,
      decliningProduct: decliningProducts[0] || null,
      salesChange,
      orderChange,
    }
  })

  function applyPreset(key) {
    const now = new Date()
    activePreset.value = key
    clearAiReport()
    toDate.value = toDateInput(now)

    if (key === '30d') fromDate.value = dateDaysAgo(29)
    if (key === '90d') fromDate.value = dateDaysAgo(89)
    if (key === '6m') {
      const date = new Date(now)
      date.setMonth(date.getMonth() - 6)
      date.setDate(date.getDate() + 1)
      fromDate.value = toDateInput(date)
    }
    if (key === 'year') fromDate.value = `${now.getFullYear()}-01-01`
    if (key === 'all') fromDate.value = '2025-01-01'

    loadAnalytics()
  }

  function applyCustomRange() {
    activePreset.value = 'custom'
    clearAiReport()
    loadAnalytics()
  }

  async function generateAiReport() {
    if (!overview.value || aiLoading.value) return
    aiLoading.value = true
    aiError.value = ''
    try {
      const response = await analyticsApi.generateAiReport({
        fromDate: fromDate.value,
        toDate: toDate.value,
      })
      aiReport.value = unwrap(response)
    } catch (error) {
      aiError.value =
        error?.response?.data?.message ||
        error?.message ||
        'Marcus AI chưa thể tạo bản phân tích. Vui lòng thử lại sau.'
    } finally {
      aiLoading.value = false
    }
  }

  function clearAiReport() {
    aiReport.value = null
    aiError.value = ''
  }

  async function loadAnalytics() {
    if (!fromDate.value || !toDate.value) {
      errorMessage.value = 'Vui lòng chọn đầy đủ từ ngày và đến ngày.'
      return
    }
    if (fromDate.value > toDate.value) {
      errorMessage.value = 'Từ ngày không được lớn hơn đến ngày.'
      return
    }
    if (toDate.value > today) {
      errorMessage.value = 'Không thể phân tích dữ liệu ở tương lai.'
      return
    }

    const currentRequest = ++requestVersion
    loading.value = true
    errorMessage.value = ''
    const params = { fromDate: fromDate.value, toDate: toDate.value }

    try {
      const [
        overviewResponse,
        trendResponse,
        productsResponse,
        cancellationResponse,
        warrantyResponse,
        aiUsageResponse,
        behaviorFunnelResponse,
      ] = await Promise.all([
        analyticsApi.getOverview(params),
        analyticsApi.getSalesTrend(params),
        analyticsApi.getProductTrends({ ...params, limit: 12 }),
        analyticsApi.getCancellationReasons(params),
        analyticsApi.getWarrantyQuality({ ...params, limit: 10 }),
        // Marcus sửa: telemetry là phần bổ sung; chưa chạy migration không được
        // làm hỏng toàn bộ trang phân tích kinh doanh.
        analyticsApi.getAiUsageSummary(params).catch(() => null),
        analyticsApi.getBehaviorFunnel(params).catch(() => null),
      ])
      if (currentRequest !== requestVersion) return

      overview.value = unwrap(overviewResponse)
      dailyTrend.value = unwrap(trendResponse) || []
      products.value = unwrap(productsResponse) || []
      cancellationReasons.value = unwrap(cancellationResponse) || []
      warrantyQuality.value = unwrap(warrantyResponse) || null
      aiUsage.value = unwrap(aiUsageResponse) || null
      behaviorFunnel.value = unwrap(behaviorFunnelResponse) || null
      await loadSavedAiReport(currentRequest)
    } catch (error) {
      if (currentRequest !== requestVersion) return
      errorMessage.value =
        error?.response?.data?.message || 'Không thể tải dữ liệu phân tích. Vui lòng thử lại.'
    } finally {
      if (currentRequest === requestVersion) loading.value = false
    }
  }

  async function loadSavedAiReport(expectedRequestVersion = requestVersion) {
    try {
      const response = await analyticsApi.getSavedAiReport({
        fromDate: fromDate.value,
        toDate: toDate.value,
      })
      if (expectedRequestVersion === requestVersion) {
        // Marcus thêm: mở lại trang chỉ đọc báo cáo đã lưu, không gọi Gemini.
        aiReport.value = unwrap(response) || null
      }
    } catch {
      // Báo cáo AI là phần bổ sung; lỗi đọc báo cáo cũ không được làm hỏng số liệu chính.
      if (expectedRequestVersion === requestVersion) aiReport.value = null
    }
  }

  function formatDate(value) {
    if (!value) return '—'
    return new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`))
  }

  onMounted(loadAnalytics)

  return {
    activePreset,
    analysis,
    aiError,
    aiLoading,
    aiReport,
    aiUsage,
    cancellationReasons,
    errorMessage,
    fromDate,
    forecast,
    loading,
    numberOfDays,
    overview,
    periodLabel,
    presets,
    products,
    toDate,
    today,
    trend,
    warrantyQuality,
    behaviorFunnel,
    applyCustomRange,
    applyPreset,
    generateAiReport,
    loadAnalytics,
  }
}

function linearRegression(values) {
  const count = values.length
  const xMean = (count - 1) / 2
  const yMean = average(values)
  let numerator = 0
  let denominator = 0
  values.forEach((value, index) => {
    numerator += (index - xMean) * (value - yMean)
    denominator += (index - xMean) ** 2
  })
  const slope = denominator ? numerator / denominator : 0
  const intercept = yMean - slope * xMean
  const residuals = values.map((value, index) => value - (intercept + slope * index))
  const residualSum = residuals.reduce((sum, value) => sum + value ** 2, 0)
  const totalSum = values.reduce((sum, value) => sum + (value - yMean) ** 2, 0)
  return {
    slope,
    intercept,
    rmse: Math.sqrt(residualSum / count),
    rSquared: totalSum ? Math.max(0, 1 - residualSum / totalSum) : 0,
  }
}

function nextPeriodLabel(lastDate, offset, monthly) {
  const date = new Date(`${lastDate}T00:00:00`)
  if (monthly) date.setMonth(date.getMonth() + offset)
  else date.setDate(date.getDate() + offset)
  return toDateInput(date)
}

function average(values) {
  return values.length ? values.reduce((sum, value) => sum + value, 0) / values.length : 0
}

function directionScore(value) {
  if (value === null || value === undefined) return 0
  if (value >= 5) return 1
  if (value <= -5) return -1
  return 0
}

function changeTone(value) {
  if (value === null || value === undefined || Math.abs(value) < 3) return 'neutral'
  return value > 0 ? 'positive' : 'warning'
}

function metricNarrative(label, value) {
  if (value === null || value === undefined) {
    return `${label} có phát sinh mới nhưng kỳ trước chưa có dữ liệu để so sánh.`
  }
  if (Math.abs(value) < 3) return `${label} gần như đi ngang so với kỳ trước.`
  return `${label} ${value > 0 ? 'tăng' : 'giảm'} ${formatNumber(Math.abs(value))}% so với kỳ trước.`
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 })
}
