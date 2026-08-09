import { ref, reactive, computed, onMounted, watch } from 'vue'
import financialApi from '@/api/financialApi'

// Marcus refactor: gom toàn bộ nghiệp vụ đối soát khỏi component giao diện.
export function useFinancialReport() {
  // State dữ liệu
  const transactions = ref([])
  const loading = ref(false)
  const exporting = ref(false)

  // State Modal
  const isModalOpen = ref(false)
  const selectedTransaction = ref(null)

  // Bộ lọc
  const keywordInput = ref('')
  let searchTimeout = null
  const filters = reactive({ keyword: '', type: '', status: '', attention: '', fromDate: '', toDate: '' })
  // Marcus thêm: mặc định trang đối soát mở ở 7 ngày gần nhất.
  const activeDatePreset = ref('7days')

  // Phân trang
  const currentPage = ref(1)
  const pageSize = ref(10)

  // Cảnh báo Toast
  const toast = reactive({ show: false, type: 'success', title: '', message: '' })
  let toastTimer = null
  const showToast = (type, title, message) => {
    toast.show = true
    toast.type = type
    toast.title = title
    toast.message = message
    clearTimeout(toastTimer)
    toastTimer = setTimeout(() => (toast.show = false), 3000)
  }

  const fetchTransactions = async () => {
    loading.value = true
    try {
      const res = await financialApi.getTransactions(filters.fromDate, filters.toDate)
      // Lấy đúng mảng transactions từ response
      const data = res.data?.transactions || res.data || []

      transactions.value = data
        .map((t) => ({
          ...t,
          // Marcus thêm lớp phòng vệ UI cho dữ liệu cũ: nhận tại quầy không
          // được gắn nhãn thu hộ GHN.
          type:
            t.type === 'COD_COLLECTION' && t.fulfillmentMethod === 'STORE_PICKUP'
              ? 'STORE_PAYMENT'
              : t.type,
          // Ưu tiên lấy orderCode trực tiếp, nếu không có mới tìm trong order
          orderCode: t.orderCode || t.order?.orderCode || '---',
        }))
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    } catch {
      showToast('error', 'Lỗi', 'Không thể tải dữ liệu')
    } finally {
      loading.value = false
    }
  }

  //TỐI ƯU UX/UI: DEBOUNCE & QUICK DATES
  const onSearchInput = (e) => {
    keywordInput.value = e.target.value
    clearTimeout(searchTimeout)
    searchTimeout = setTimeout(() => {
      filters.keyword = keywordInput.value
      currentPage.value = 1
    }, 300) // 300ms debounce
  }

  const applyDatePreset = (preset) => {
    const today = new Date()
    activeDatePreset.value = preset
    // Marcus sửa: input type=date phải dùng ngày local, tránh lệch ngày do UTC+7.
    filters.toDate = formatLocalDateInput(today)

    if (preset === 'yesterday') {
      const yesterday = new Date(today)
      yesterday.setDate(today.getDate() - 1)
      filters.fromDate = formatLocalDateInput(yesterday)
      filters.toDate = filters.fromDate
    } else if (preset === 'today') {
      filters.fromDate = filters.toDate
    } else if (preset === '7days') {
      const past7 = new Date(today)
      past7.setDate(today.getDate() - 6)
      filters.fromDate = formatLocalDateInput(past7)
    } else if (preset === '30days') {
      const past30 = new Date(today)
      past30.setDate(today.getDate() - 29)
      filters.fromDate = formatLocalDateInput(past30)
    } else if (preset === 'thisMonth') {
      // Marcus sửa: tháng này tính từ ngày 1 đến hôm nay, không bao gồm ngày tương lai.
      const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
      filters.fromDate = formatLocalDateInput(firstDay)
    } else if (preset === 'lastMonth') {
      const firstDayLastMonth = new Date(today.getFullYear(), today.getMonth() - 1, 1)
      const lastDayLastMonth = new Date(today.getFullYear(), today.getMonth(), 0)
      filters.fromDate = formatLocalDateInput(firstDayLastMonth)
      filters.toDate = formatLocalDateInput(lastDayLastMonth)
    } else if (preset === 'year') {
      filters.fromDate = formatLocalDateInput(new Date(today.getFullYear(), 0, 1))
    }
    currentPage.value = 1
    fetchTransactions()
  }

  const onFilterChange = () => {
    currentPage.value = 1
  }

  const onDateFilterChange = () => {
    // Marcus thêm: nhập ngày thủ công vẫn giữ nguyên và gọi lại query backend.
    activeDatePreset.value = ''
    currentPage.value = 1
    fetchTransactions()
  }

  const resetFilters = () => {
    keywordInput.value = ''
    filters.keyword = ''
    filters.type = ''
    filters.status = ''
    filters.attention = ''
    currentPage.value = 1
    applyDatePreset('7days')
  }

  // COMPUTED: LỌC & TỔNG TIỀN ĐỘNG
  const filteredTransactions = computed(() => {
    return transactions.value.filter((item) => {
      const kw = filters.keyword.trim().toLowerCase()
      if (kw) {
        const matchKw =
          item.orderCode.toLowerCase().includes(kw) || item.note?.toLowerCase().includes(kw)
        if (!matchKw) return false
      }
      if (filters.type && item.type !== filters.type) return false
      if (filters.status && item.status !== filters.status) return false
      if (filters.attention === 'UNRECONCILED' && !(item.status === 'SUCCESS' && !item.isReconciled)) return false
      if (filters.attention === 'ATTENTION' && !item.needsAttention) return false
      if (filters.fromDate) {
        // Marcus sửa: parse ngày bắt đầu theo local để không bỏ sót giao dịch 00:00–06:59.
        if (new Date(item.createdAt) < parseLocalDate(filters.fromDate)) return false
      }
      if (filters.toDate) {
        const to = parseLocalDate(filters.toDate)
        to.setHours(23, 59, 59, 999)
        if (new Date(item.createdAt) > to) return false
      }
      return true
    })
  })

  const filteredTotal = computed(() => {
    return (
      filteredTransactions.value
        .filter((t) => t.status === 'SUCCESS')
        // Marcus sửa: chỉ SUCCESS mới phát sinh tiền; payment/COD là tiền vào,
        // REFUND là tiền ra.
        .reduce((sum, t) => sum + getSuccessfulCashFlowAmount(t), 0)
    )
  })

  // MODAL & UTILS
  const openDetailModal = (item) => {
    console.log('Dữ liệu dòng được chọn:', item) //log ktra lỗi
    selectedTransaction.value = item
    isModalOpen.value = true
  }
  const closeDetailModal = () => {
    isModalOpen.value = false
    selectedTransaction.value = null
  }

  const copyToClipboard = async (text) => {
    if (!text) return
    try {
      await navigator.clipboard.writeText(text)
      showToast('success', 'Đã sao chép', `Copied: ${text}`)
    } catch (err) {
      console.error('Copy failed', err)
    }
  }

  // XUẤT EXCEL CHỈ XUẤT DATA ĐANG LỌC
  const handleExportFilteredExcel = () => {
    exporting.value = true
    try {
      if (filteredTransactions.value.length === 0) {
        showToast('error', 'Lỗi', 'Không có dữ liệu để xuất!')
        exporting.value = false
        return
      }

      const BOM = '\uFEFF'
      // Marcus sửa: tách số tiền yêu cầu và ảnh hưởng dòng tiền để pending/failed
      // không bị hiểu nhầm là tiền đã cộng hoặc trừ.
      let csvContent =
        BOM +
        'STT,Mã Đơn,Loại Giao Dịch,Số Tiền Yêu Cầu,Ảnh Hưởng Dòng Tiền,Trạng Thái,Ghi Chú,Thời Gian\n'

      filteredTransactions.value.forEach((item, index) => {
        const row = [
          index + 1,
          item.orderCode,
          formatType(item.type),
          Number(item.amount) || 0,
          getSuccessfulCashFlowAmount(item),
          formatStatus(item.status),
          `"${item.note ? item.note.replace(/"/g, '""') : ''}"`,
          formatDate(item.createdAt),
        ]
        csvContent += row.join(',') + '\n'
      })

      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      link.setAttribute('href', url)
      link.setAttribute('download', `DoiSoat_Filtered_${new Date().getTime()}.csv`)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      showToast('success', 'Thành công', 'Xuất báo cáo theo bộ lọc hoàn tất!')
    } catch {
      showToast('error', 'Lỗi xuất file', 'Đã xảy ra lỗi trong quá trình tạo file.')
    } finally {
      exporting.value = false
    }
  }

  // BIỂU ĐỒ APEXCHARTS
  // Parse chuỗi 'YYYY-MM-DD' thành Date theo giờ local (tránh lệch ngày do quy đổi UTC)
  const parseLocalDate = (str) => {
    const [y, m, d] = str.split('-').map(Number)
    return new Date(y, m - 1, d)
  }

  // Marcus thêm: định dạng YYYY-MM-DD theo múi giờ máy người dùng, không qua UTC.
  const formatLocalDateInput = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  // Marcus thêm: chỉ giao dịch thành công mới ảnh hưởng dòng tiền thực tế.
  const getSuccessfulCashFlowAmount = (transaction) => {
    if (transaction?.status !== 'SUCCESS') return 0
    const amount = Number(transaction?.amount) || 0
    return transaction?.type === 'REFUND' ? -amount : amount
  }

  // Marcus thêm: phân biệt tiền thật với số tiền đang chờ hoặc đã xử lý thất bại.
  const formatTransactionAmount = (transaction) => {
    const amount = Number(transaction?.amount) || 0
    if (transaction?.status !== 'SUCCESS') return formatCurrency(amount)
    const sign = transaction?.type === 'REFUND' ? '-' : '+'
    return `${sign}${formatCurrency(amount)}`
  }

  const getAmountClass = (transaction) => {
    if (transaction?.status !== 'SUCCESS') return 'amount-neutral'
    return transaction?.type === 'REFUND' ? 'amount-outflow' : 'amount-inflow'
  }

  const getAmountCaption = (transaction) => {
    if (transaction?.status === 'PENDING') {
      return transaction?.type === 'REFUND' ? 'Dự kiến hoàn' : 'Chờ thu tiền'
    }
    if (transaction?.status === 'FAILED') {
      return transaction?.type === 'REFUND' ? 'Không hoàn được' : 'Không thu được'
    }
    return ''
  }

  const MAX_CONTINUOUS_CHART_DAYS = 31 // đủ cho preset "Tháng này"

  // Gom toàn bộ logic group-by-ngày vào MỘT computed duy nhất, dùng chung
  // cho cả chartSeries và chartOptions => tránh lệch dữ liệu giữa 2 bên,
  // và luôn bám sát filteredTransactions (đúng bộ lọc đang chọn).
  const chartData = computed(() => {
    const grouped = {}
    const isYearView = activeDatePreset.value === 'year'

    filteredTransactions.value.forEach((t) => {
      const transactionDate = new Date(t.createdAt)
      // Marcus sửa: preset Năm nay gom theo tháng; các preset khác vẫn gom theo ngày.
      const groupKey = isYearView
        ? `T${transactionDate.getMonth() + 1}`
        : transactionDate.toLocaleDateString('vi-VN')
      if (!grouped[groupKey]) {
        grouped[groupKey] = { INFLOW: 0, REFUND: 0, PENDING: 0, FAILED: 0 }
      }
      const amount = Number(t.amount) || 0
      if (t.status === 'SUCCESS') {
        if (t.type === 'REFUND') grouped[groupKey].REFUND += amount
        else grouped[groupKey].INFLOW += amount
      } else if (grouped[groupKey][t.status] !== undefined) {
        grouped[groupKey][t.status] += amount
      }
    })

    // Marcus thêm: biểu đồ năm luôn hiện đủ T1–T12, kể cả tháng chưa có giao dịch.
    let categories = isYearView ? Array.from({ length: 12 }, (_, index) => `T${index + 1}`) : null

    // Khi có bộ lọc khoảng ngày (kể cả preset Hôm nay / 7 Ngày / Tháng này), hiển thị
    // ĐỦ các ngày trong khoảng đó => ngày không có giao dịch vẫn hiện với giá trị 0
    if (!isYearView && filters.fromDate && filters.toDate) {
      const start = parseLocalDate(filters.fromDate)
      const end = parseLocalDate(filters.toDate)
      const dayCount = Math.round((end - start) / 86400000) + 1

      if (dayCount > 0 && dayCount <= MAX_CONTINUOUS_CHART_DAYS) {
        categories = []
        const cursor = new Date(start)
        for (let i = 0; i < dayCount; i++) {
          categories.push(cursor.toLocaleDateString('vi-VN'))
          cursor.setDate(cursor.getDate() + 1)
        }
      }
    }

    // Không lọc theo ngày (hoặc khoảng quá dài) => chỉ hiện các ngày có dữ liệu,
    // theo thứ tự cũ => mới, tối đa 7 ngày gần nhất.
    if (!categories) {
      categories = Object.keys(grouped).reverse().slice(-7)
    }

    return {
      categories,
      inflowData: categories.map((d) => grouped[d]?.INFLOW || 0),
      refundData: categories.map((d) => grouped[d]?.REFUND || 0),
      pendingData: categories.map((d) => grouped[d]?.PENDING || 0),
      failedData: categories.map((d) => grouped[d]?.FAILED || 0),
    }
  })

  const chartSeries = computed(() => [
    { name: 'Tiền vào thành công', data: chartData.value.inflowData },
    { name: 'Hoàn tiền thành công', data: chartData.value.refundData },
    { name: 'Đang treo', data: chartData.value.pendingData },
    { name: 'Thất bại/Hủy', data: chartData.value.failedData },
  ])

  // Marcus thêm: mô tả đúng cấp thời gian đang hiển thị trên biểu đồ.
  const chartPeriodLabel = computed(() =>
    activeDatePreset.value === 'year'
      ? `(theo tháng trong năm ${new Date().getFullYear()})`
      : `(từ ${formatDisplayDate(filters.fromDate)} đến ${formatDisplayDate(filters.toDate)})`,
  )

  // Marcus sửa: trục tung luôn dùng dấu chấm phân cách và ghi rõ đơn vị VND.
  const formatAxisVnd = (value) =>
    `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(Math.round(value || 0))} VND`

  const chartOptions = computed(() => {
    const categories = chartData.value.categories
    const todayLabel = new Date().toLocaleDateString('vi-VN')
    const isToday = categories.includes(todayLabel)

    return {
      chart: {
        type: 'bar',
        stacked: true,
        toolbar: { show: false },
        fontFamily: { family: 'Be Vietnam Pro', fallback: ['sans-serif'] },
      },
      plotOptions: { bar: { columnWidth: '40%', borderRadius: 4 } },
      colors: ['#08783e', '#dc2626', '#f59e0b', '#64748b'],
      xaxis: {
        categories,
        labels: {
          style: {
            colors: categories.map((c) => (c === todayLabel ? '#0b3d91' : '#6b7c93')),
          },
        },
      },
      yaxis: {
        labels: {
          minWidth: 110,
          maxWidth: 160,
          formatter: formatAxisVnd,
          style: { colors: ['#64748b'], fontSize: '11px', fontWeight: 600 },
        },
      },
      tooltip: {
        y: {
          formatter: formatAxisVnd,
        },
      },
      legend: { position: 'top', horizontalAlign: 'right' },
      dataLabels: { enabled: false },
      fill: { opacity: 1 },
      annotations: {
        xaxis: isToday
          ? [
              {
                x: todayLabel,
                borderColor: '#0b3d91',
                fillColor: 'rgba(11, 61, 145, 0.14)',
                label: {
                  text: 'Hôm nay',
                  orientation: 'horizontal',
                  style: {
                    color: '#ffffff',
                    background: '#0b3d91',
                    fontSize: '10px',
                    fontWeight: 700,
                  },
                },
              },
            ]
          : [],
      },
    }
  })

  // PHÂN TRANG & THỐNG KÊ GỐC
  const totalPages = computed(() =>
    Math.max(1, Math.ceil(filteredTransactions.value.length / pageSize.value)),
  )
  const pagedTransactions = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return filteredTransactions.value.slice(start, start + pageSize.value)
  })

  const pageItems = computed(() => {
    const total = totalPages.value
    const current = currentPage.value
    const delta = 1
    const items = []
    const range = []
    for (let i = Math.max(2, current - delta); i <= Math.min(total - 1, current + delta); i++)
      range.push(i)
    items.push(1)
    if (range.length && range[0] > 2) items.push('...')
    items.push(...range)
    if (range.length && range[range.length - 1] < total - 1) items.push('...')
    if (total > 1) items.push(total)
    return items
  })

  watch(totalPages, (val) => {
    if (currentPage.value > val) currentPage.value = val
  })

  // Thống kê tổng quan
  const stats = computed(() => {
    const dataset = filteredTransactions.value
    const successfulInflow = dataset
      .filter((t) => t.status === 'SUCCESS' && t.type !== 'REFUND')
      .reduce((sum, t) => sum + (Number(t.amount) || 0), 0)
    const successfulRefund = dataset
      .filter((t) => t.status === 'SUCCESS' && t.type === 'REFUND')
      .reduce((sum, t) => sum + (Number(t.amount) || 0), 0)

    // Marcus thêm: cùng quy tắc nguồn tiền với Analytics/AI.
    const recognizedRevenue = dataset
      .filter((t) => t.status === 'SUCCESS' && t.type !== 'REFUND' && t.orderStatus === 'COMPLETED')
      .reduce((sum, t) => sum + (Number(t.amount) || 0), 0)

    // Marcus thêm: tách tiền đã thu của đơn hủy nhưng refund chưa SUCCESS.
    const cancelledBalances = new Map()
    dataset
      .filter((t) => t.status === 'SUCCESS' && t.orderStatus === 'CANCELLED')
      .forEach((t) => {
        const current = cancelledBalances.get(t.orderCode) || 0
        const effect = t.type === 'REFUND' ? -(Number(t.amount) || 0) : Number(t.amount) || 0
        cancelledBalances.set(t.orderCode, current + effect)
      })
    const unsettledCancellationAmount = [...cancelledBalances.values()]
      .filter((amount) => amount > 0)
      .reduce((sum, amount) => sum + amount, 0)

    return {
      total: dataset.length,
      success: dataset.filter((t) => t.status === 'SUCCESS').length,
      pending: dataset.filter((t) => t.status === 'PENDING').length,
      successfulInflow,
      successfulRefund,
      recognizedRevenue,
      unsettledCancellationAmount,
      vnpayCollected: sumCashCategory(dataset, 'VNPAY_COLLECTED'),
      codExpected: sumCashCategory(dataset, 'COD_EXPECTED'),
      codReconciled: sumCashCategory(dataset, 'COD_RECONCILED'),
      storeCollected: sumCashCategory(dataset, 'STORE_COLLECTED'),
      refundExpected: sumCashCategory(dataset, 'REFUND_EXPECTED'),
      refundSuccessful: sumCashCategory(dataset, 'REFUND_SUCCESSFUL'),
      attentionCount: dataset.filter((t) => t.needsAttention).length,
      totalAmount: successfulInflow - successfulRefund,
    }
  })

  const sumCashCategory = (dataset, category) =>
    dataset
      .filter((item) => item.cashCategory === category)
      .reduce((sum, item) => sum + (Number(item.amount) || 0), 0)

  // UTILS FORMATTING
  const formatCurrency = (value) => new Intl.NumberFormat('vi-VN').format(value || 0)
  const formatCurrencyVnd = (value) => `${formatCurrency(value)} VND`
  const formatDisplayDate = (date) => {
    if (!date) return '—'
    const [year, month, day] = date.split('-')
    return `${day}/${month}/${year}`
  }
  const formatDate = (dateString) =>
    dateString ? new Date(dateString).toLocaleString('vi-VN') : ''
  const formatType = (type) => {
    if (type === 'COD_COLLECTION') return 'Thu hộ (COD)'
    if (type === 'STORE_PAYMENT') return 'Thanh toán tại cửa hàng'
    if (type === 'VNPAY_PAYMENT') return 'Thanh toán (VNPAY)'
    if (type === 'REFUND') return 'Hoàn tiền'
    return type
  }

  const formatStatus = (status) => {
    if (status === 'SUCCESS') return 'Thành công'
    if (status === 'PENDING') return 'Chờ xử lý'
    if (status === 'FAILED') return 'Thất bại'
    return status
  }

  const getTypeClass = (type) =>
    type === 'VNPAY_PAYMENT'
      ? 'bg-primary'
      : type === 'REFUND'
        ? 'bg-warning'
        : type === 'STORE_PAYMENT'
          ? 'bg-success'
          : 'bg-info'

  const getStatusClass = (status) =>
    status === 'SUCCESS' ? 'bg-success' : status === 'PENDING' ? 'bg-warning' : 'bg-danger'

  const confirmReconciliation = async (item) => {
    // Marcus sửa: backend/UI chỉ cho đối soát giao dịch đã phát sinh dòng tiền thật.
    if (item?.status !== 'SUCCESS') {
      showToast('warning', 'Chưa thể đối soát', 'Chỉ giao dịch thành công mới được đối soát.')
      return
    }
    const originalStatus = item.isReconciled
    item.isReconciled = true // Tích V ngay trên UI

    try {
      // Gọi API cập nhật DB
      await financialApi.reconcile(item.transactionId, true)
      showToast('success', 'Thành công', 'Đã đối soát đơn ' + item.orderCode)
    } catch {
      item.isReconciled = originalStatus // Hoàn tác nếu lỗi
      showToast('error', 'Lỗi', 'Không thể cập nhật trạng thái đối soát.')
    }
  }

  onMounted(() => {
    // Marcus thêm: vào trang luôn hiển thị đúng 7 ngày tính cả hôm nay.
    applyDatePreset('7days')
  })

  // Marcus refactor: chỉ public các state/hàm mà template FinancialReport sử dụng.
  return {
    loading,
    exporting,
    filteredTotal,
    filteredTransactions,
    handleExportFilteredExcel,
    stats,
    chartOptions,
    chartSeries,
    keywordInput,
    onSearchInput,
    filters,
    onFilterChange,
    activeDatePreset,
    applyDatePreset,
    onDateFilterChange,
    resetFilters,
    pagedTransactions,
    currentPage,
    pageSize,
    totalPages,
    pageItems,
    isModalOpen,
    selectedTransaction,
    openDetailModal,
    closeDetailModal,
    copyToClipboard,
    formatCurrencyVnd,
    formatTransactionAmount,
    getAmountClass,
    getAmountCaption,
    formatType,
    formatStatus,
    getTypeClass,
    getStatusClass,
    formatDate,
    confirmReconciliation,
    toast,
    chartPeriodLabel,
  }
}
