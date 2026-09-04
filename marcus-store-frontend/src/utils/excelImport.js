import * as XLSX from 'xlsx'

const IMEI_HEADER_KEYS = ['imei', 'imeicode', 'imei_code', 'serial', 'mã imei', 'ma imei']

function detectHeader(rows) {
  if (!rows.length || !Array.isArray(rows[0])) return { imeiCol: 0, hasHeader: false }
  const header = rows[0].map((c) => String(c || '').trim().toLowerCase())
  const idx = header.findIndex((h) => IMEI_HEADER_KEYS.includes(h))
  if (idx >= 0) return { imeiCol: idx, hasHeader: true }
  return { imeiCol: 0, hasHeader: false }
}

function extractUniqueImeis(rows, imeiCol, hasHeader) {
  const data = hasHeader ? rows.slice(1) : rows
  const seen = new Set()
  const unique = []
  for (const row of data) {
    if (!row || !Array.isArray(row)) continue
    const code = String(row[imeiCol] == null ? '' : row[imeiCol]).trim()
    if (!code || seen.has(code)) continue
    seen.add(code)
    unique.push(code)
  }
  return unique
}

export function setupGlobalExcelImporter(opts) {
  const { getFormImport, getParsingExcel, setParsingExcel, onSuccess, showToast } = opts

  async function handleFile(file) {
    setParsingExcel(true)
    try {
      const buffer = await file.arrayBuffer()
      const wb = XLSX.read(buffer, { type: 'array' })
      const sheetName = wb.SheetNames[0]
      if (!sheetName) {
        showToast('File Excel không có sheet nào')
        return
      }

      const rows = XLSX.utils.sheet_to_json(wb.Sheets[sheetName], {
        header: 1,
        defval: '',
        blankrows: false,
      })

      const { imeiCol, hasHeader } = detectHeader(rows)
      const unique = extractUniqueImeis(rows, imeiCol, hasHeader)

      if (!unique.length) {
        showToast('Không tìm thấy mã IMEI nào trong file')
        return
      }

      getFormImport().imeiText = unique.join('\n')
      if (onSuccess) onSuccess(unique.length, file.name)
    } catch (e) {
      showToast('Không đọc được file Excel: ' + (e?.message || 'lỗi không xác định'))
    } finally {
      setParsingExcel(false)
    }
  }

  function trigger() {
    if (getParsingExcel()) return
    const tempInput = document.createElement('input')
    tempInput.type = 'file'
    tempInput.accept = '.xlsx,.xls,.csv'
    tempInput.style.position = 'fixed'
    tempInput.style.left = '-9999px'
    tempInput.style.top = '0'
    tempInput.style.opacity = '0'
    document.body.appendChild(tempInput)
    tempInput.addEventListener('change', async (e) => {
      if (tempInput.parentNode) document.body.removeChild(tempInput)
      const file = e.target.files?.[0]
      if (file) await handleFile(file)
    })
    setTimeout(() => {
      try {
        tempInput.click()
      } catch (err) {
        if (tempInput.parentNode) document.body.removeChild(tempInput)
        showToast('Không mở được hộp thoại: ' + (err?.message || err))
      }
    }, 0)
  }

  window.__importImeiFromExcel = trigger
  return trigger
}
