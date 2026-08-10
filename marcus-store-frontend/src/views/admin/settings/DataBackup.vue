<template>
  <main class="backup-page">
    <AdminPageHeader
      eyebrow="Quản trị dữ liệu"
      eyebrow-icon="bi bi-shield-lock-fill"
      title="Trung tâm sao lưu dữ liệu"
      description="Xuất dữ liệu kinh doanh để lưu hồ sơ hoặc tạo bản BAK phục hồi toàn bộ SQL Server."
      icon="bi bi-database-check"
    >
      <template #actions>
        <button type="button" :disabled="loading" @click="loadPage">
          <i class="bi bi-arrow-clockwise" :class="{ spin: loading }"></i>
          Làm mới dữ liệu
        </button>
      </template>
    </AdminPageHeader>

    <section v-if="errorMessage" class="alert-error">
      <i class="bi bi-exclamation-triangle-fill"></i>
      <span>{{ errorMessage }}</span>
      <button type="button" @click="loadPage">Thử lại</button>
    </section>

    <section v-if="overview.storageWarning" class="storage-warning">
      <i class="bi bi-device-hdd-fill"></i>
      Ổ lưu backup chỉ còn {{ formatBytes(overview.availableStorageBytes) }}. Hãy tải file về ổ cứng và dọn bản cũ.
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <span class="stat-icon blue"><i class="bi bi-database"></i></span>
        <div>
          <small>Database</small><strong>{{ overview.databaseName || '—' }}</strong>
        </div>
      </article>
      <article class="stat-card">
        <span class="stat-icon violet"><i class="bi bi-table"></i></span>
        <div>
          <small>Bảng dữ liệu</small><strong>{{ formatNumber(overview.tableCount) }}</strong>
        </div>
      </article>
      <article class="stat-card">
        <span class="stat-icon green"><i class="bi bi-collection"></i></span>
        <div>
          <small>Tổng bản ghi</small><strong>{{ formatNumber(overview.totalRecords) }}</strong>
        </div>
      </article>
      <article class="stat-card">
        <span class="stat-icon amber"><i class="bi bi-hdd"></i></span>
        <div>
          <small>Dung lượng đang lưu</small
          ><strong>{{ formatBytes(overview.storageBytes) }}</strong>
        </div>
      </article>
    </section>

    <section class="backup-options">
      <article class="backup-option excel">
        <div class="option-heading">
          <span class="option-icon"><i class="bi bi-file-earmark-spreadsheet-fill"></i></span>
          <span class="option-tag">Dễ đọc</span>
        </div>
        <h2>Xuất dữ liệu đọc (Excel)</h2>
        <p>Mỗi bảng SQL là một sheet để đọc/tra cứu. Đây không phải bản phục hồi toàn bộ hệ thống.</p>
        <ul>
          <li><i class="bi bi-check-circle-fill"></i> Mở bằng Excel để tra cứu và in</li>
          <li><i class="bi bi-check-circle-fill"></i> Có tiêu đề, bộ lọc và cố định hàng đầu</li>
          <li><i class="bi bi-check-circle-fill"></i> Không chứa dữ liệu xác thực nhạy cảm</li>
        </ul>
        <button type="button" :disabled="hasProcessing" @click="openCreate('EXCEL')">
          <i class="bi bi-file-earmark-arrow-down"></i> Tạo bản Excel
        </button>
      </article>

      <article class="backup-option bak">
        <div class="option-heading">
          <span class="option-icon"><i class="bi bi-server"></i></span>
          <span class="option-tag">Phục hồi hệ thống</span>
        </div>
        <h2>Sao lưu SQL Server (.bak)</h2>
        <p>Bản sao chính xác của database, bao gồm dữ liệu, khóa, index và cấu trúc bảng.</p>
        <ul>
          <li>
            <i class="bi bi-check-circle-fill"></i> COPY_ONLY không ảnh hưởng chuỗi backup khác
          </li>
          <li><i class="bi bi-check-circle-fill"></i> CHECKSUM kiểm tra toàn vẹn</li>
          <li><i class="bi bi-check-circle-fill"></i> RESTORE VERIFYONLY trước khi cho tải</li>
        </ul>
        <button type="button" :disabled="hasProcessing" @click="openCreate('BAK')">
          <i class="bi bi-database-down"></i> Tạo bản BAK
        </button>
      </article>
    </section>

    <section class="history-panel">
      <div class="panel-heading">
        <div>
          <span class="panel-kicker">Kho lưu trữ tạm</span>
          <h2>Lịch sử sao lưu</h2>
        </div>
        <span v-if="hasProcessing" class="processing-note">
          <i class="bi bi-arrow-repeat spin"></i> Hệ thống đang tạo file…
        </span>
      </div>

      <div v-if="loading && !history.length" class="loading-state">
        <i class="bi bi-arrow-repeat spin"></i> Đang đọc lịch sử…
      </div>
      <div v-else-if="!history.length" class="empty-state">
        <i class="bi bi-archive"></i>
        <strong>Chưa có bản sao lưu</strong>
        <span>Chọn Excel hoặc BAK ở phía trên để tạo bản đầu tiên.</span>
      </div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>File sao lưu</th>
              <th>Loại</th>
              <th>Người tạo</th>
              <th>Thời gian</th>
              <th>Dung lượng</th>
              <th>Trạng thái</th>
              <th class="actions-col">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in history" :key="item.id">
              <td>
                <div class="file-cell">
                  <span :class="['file-icon', item.type.toLowerCase()]">
                    <i
                      :class="
                        item.type === 'BAK'
                          ? 'bi bi-database-fill'
                          : 'bi bi-file-earmark-excel-fill'
                      "
                    ></i>
                  </span>
                  <div>
                    <strong>{{ item.fileName }}</strong>
                    <small :title="item.checksum">{{ shortChecksum(item.checksum) }}</small>
                    <small>Nguồn: {{ item.sourceDatabase || overview.databaseName || '—' }}</small>
                    <small v-if="item.restoreTestStatus" :class="['restore-result', item.restoreTestStatus.toLowerCase()]">
                      {{ item.restoreTestMessage }} · {{ formatDateTime(item.restoreTestedAt) }}
                    </small>
                    <small v-if="item.note">{{ item.note }}</small>
                  </div>
                </div>
              </td>
              <td>
                <span class="type-badge">{{ item.type }}</span>
              </td>
              <td>{{ item.createdBy }}</td>
              <td>{{ formatDateTime(item.createdAt) }}</td>
              <td>{{ formatBytes(item.fileSize) }}</td>
              <td>
                <span :class="['status-badge', item.status.toLowerCase()]">
                  <i v-if="item.status === 'PROCESSING'" class="bi bi-arrow-repeat spin"></i>
                  {{ statusLabel(item.status) }}
                </span>
                <small v-if="item.errorMessage" class="row-error">{{ item.errorMessage }}</small>
              </td>
              <td class="actions">
                <button
                  v-if="item.type === 'BAK' && item.status === 'SUCCESS'"
                  class="icon-btn restore"
                  type="button"
                  title="Phục hồi thử vào database tạm"
                  :disabled="restoreTestingId === item.id"
                  @click="testRestore(item)"
                >
                  <i :class="restoreTestingId === item.id ? 'bi bi-arrow-repeat spin' : 'bi bi-database-check'"></i>
                </button>
                <button
                  class="icon-btn download"
                  type="button"
                  title="Tải file"
                  :disabled="item.status !== 'SUCCESS' || downloadingId === item.id"
                  @click="downloadBackup(item)"
                >
                  <i
                    :class="
                      downloadingId === item.id ? 'bi bi-arrow-repeat spin' : 'bi bi-download'
                    "
                  ></i>
                </button>
                <button
                  class="icon-btn delete"
                  type="button"
                  title="Xóa khỏi máy chủ"
                  :disabled="item.status === 'PROCESSING' || deletingId === item.id"
                  @click="deleteBackup(item)"
                >
                  <i class="bi bi-trash3"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <details class="restore-guide">
      <summary><i class="bi bi-life-preserver"></i> Hướng dẫn phục hồi file BAK</summary>
      <ol>
        <li>Tải file BAK và kiểm tra SHA-256 hiển thị trong lịch sử.</li>
        <li>Trong SSMS chọn <strong>Databases → Restore Database → Device</strong>.</li>
        <li>Chọn file BAK, phục hồi sang tên database mới; không ghi đè database đang chạy.</li>
        <li>Đổi cấu hình <code>spring.datasource.url</code> sang database vừa phục hồi và chạy kiểm tra.</li>
      </ol>
      <p>Nút hình database ở lịch sử sẽ tự phục hồi vào database test tên ngẫu nhiên, đếm bảng rồi xóa database test.</p>
    </details>

    <details class="data-detail">
      <summary>
        <span><i class="bi bi-table"></i> Dữ liệu sẽ được đưa vào Excel</span>
        <strong>{{ formatNumber(overview.tableCount) }} bảng</strong>
      </summary>
      <div class="data-grid">
        <article v-for="table in overview.tables || []" :key="`${table.schema}.${table.table}`">
          <span>{{ table.table }}</span>
          <strong>{{ formatNumber(table.columnCount) }} cột</strong>
          <small>
            <span><i class="bi bi-collection"></i> {{ formatNumber(table.records) }} bản ghi</span>
          </small>
        </article>
      </div>
    </details>

    <div v-if="createModal.open" class="modal-backdrop" @click.self="closeCreate">
      <form class="create-modal" @submit.prevent="createBackup">
        <button class="modal-close" type="button" @click="closeCreate">
          <i class="bi bi-x-lg"></i>
        </button>
        <span :class="['modal-icon', createModal.type.toLowerCase()]">
          <i
            :class="
              createModal.type === 'BAK' ? 'bi bi-database-check' : 'bi bi-file-earmark-spreadsheet'
            "
          ></i>
        </span>
        <h2>Tạo bản sao lưu {{ createModal.type }}</h2>
        <p>
          {{
            createModal.type === 'BAK'
              ? 'File chứa toàn bộ database. Hãy cất giữ ở ổ cứng an toàn.'
              : 'File dùng để xem và lưu trữ dữ liệu nghiệp vụ.'
          }}
        </p>
        <label>
          Ghi chú
          <textarea
            v-model.trim="createModal.note"
            maxlength="250"
            placeholder="Ví dụ: Sao lưu trước buổi demo"
          ></textarea>
          <small>{{ createModal.note.length }}/250</small>
        </label>
        <div class="modal-warning">
          <i class="bi bi-shield-exclamation"></i>
          Đây là dữ liệu nội bộ. Không gửi file cho người không có thẩm quyền.
        </div>
        <button class="confirm-create" type="submit" :disabled="creating">
          <i :class="creating ? 'bi bi-arrow-repeat spin' : 'bi bi-shield-check'"></i>
          {{ creating ? 'Đang khởi tạo…' : 'Xác nhận tạo file' }}
        </button>
      </form>
    </div>

    <!-- Marcus sửa: thay confirm() của trình duyệt bằng modal dùng chung của hệ thống. -->
    <BaseModal
      :visible="deleteModal.visible"
      type="confirm"
      title="Xóa bản sao lưu?"
      :message="deleteModal.message"
      @close="closeDeleteModal"
      @confirm="confirmDeleteBackup"
    />
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { backupApi } from '@/api/backupApi'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import BaseModal from '@/components/BaseModal.vue'
import '@/assets/css/DataBackup.css'

const overview = ref({ tables: [], tableCount: 0, totalRecords: 0, storageBytes: 0 })
const history = ref([])
const loading = ref(false)
const creating = ref(false)
const downloadingId = ref('')
const deletingId = ref('')
const restoreTestingId = ref('')
const errorMessage = ref('')
const createModal = reactive({ open: false, type: 'EXCEL', note: '' })
const deleteModal = reactive({ visible: false, item: null, message: '' })
let pollTimer = null

const hasProcessing = computed(() => history.value.some((item) => item.status === 'PROCESSING'))

const loadPage = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [overviewResponse, historyResponse] = await Promise.all([
      backupApi.getOverview(),
      backupApi.getHistory(),
    ])
    overview.value = overviewResponse.data?.data ?? overview.value
    history.value = historyResponse.data?.data ?? []
    syncPolling()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Không thể tải trung tâm sao lưu.'
  } finally {
    loading.value = false
  }
}

const refreshHistory = async () => {
  try {
    const response = await backupApi.getHistory()
    history.value = response.data?.data ?? []
    syncPolling()
  } catch {
    stopPolling()
  }
}

const syncPolling = () => {
  if (hasProcessing.value && !pollTimer) {
    // Marcus thêm: chỉ kiểm tra trạng thái khi thật sự có job đang chạy, không polling thường trực.
    pollTimer = window.setInterval(refreshHistory, 2500)
  } else if (!hasProcessing.value) {
    stopPolling()
  }
}

const stopPolling = () => {
  if (pollTimer) window.clearInterval(pollTimer)
  pollTimer = null
}

const openCreate = (type) => {
  createModal.type = type
  createModal.note = ''
  createModal.open = true
}

const closeCreate = () => {
  if (!creating.value) createModal.open = false
}

const createBackup = async () => {
  creating.value = true
  errorMessage.value = ''
  try {
    const response = await backupApi.create(createModal.type, createModal.note)
    history.value.unshift(response.data.data)
    createModal.open = false
    syncPolling()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Không thể khởi tạo bản sao lưu.'
  } finally {
    creating.value = false
  }
}

const downloadBackup = async (item) => {
  downloadingId.value = item.id
  errorMessage.value = ''
  try {
    const response = await backupApi.download(item.id)
    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = item.fileName
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    // Marcus sửa: chờ trình duyệt nhận tác vụ tải rồi mới giải phóng Blob URL.
    window.setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Không thể tải file sao lưu.'
  } finally {
    downloadingId.value = ''
  }
}

const deleteBackup = async (item) => {
  deleteModal.item = item
  deleteModal.message = `Bạn sắp xóa “${item.fileName}” khỏi máy chủ. Hãy chắc chắn file cần lưu đã được tải về ổ cứng. Thao tác này không thể hoàn tác.`
  deleteModal.visible = true
}

const closeDeleteModal = () => {
  if (deletingId.value) return
  deleteModal.visible = false
  deleteModal.item = null
  deleteModal.message = ''
}

const confirmDeleteBackup = async () => {
  const item = deleteModal.item
  if (!item || deletingId.value) return
  deletingId.value = item.id
  errorMessage.value = ''
  try {
    await backupApi.remove(item.id)
    history.value = history.value.filter((entry) => entry.id !== item.id)
    deleteModal.visible = false
    deleteModal.message = ''
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Không thể xóa bản sao lưu.'
    deleteModal.visible = false
  } finally {
    deletingId.value = ''
    deleteModal.item = null
  }
}

const testRestore = async (item) => {
  restoreTestingId.value = item.id
  errorMessage.value = ''
  try {
    const response = await backupApi.testRestore(item.id)
    Object.assign(item, response.data?.data || {})
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Không thể phục hồi thử file BAK.'
    await refreshHistory()
  } finally {
    restoreTestingId.value = ''
  }
}

const formatNumber = (value) => new Intl.NumberFormat('vi-VN').format(Number(value) || 0)
const formatBytes = (value) => {
  const bytes = Number(value) || 0
  if (!bytes) return '—'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / 1024 ** index).toLocaleString('vi-VN', { maximumFractionDigits: 2 })} ${units[index]}`
}
const formatDateTime = (value) =>
  value ? new Date(value).toLocaleString('vi-VN', { hour12: false }) : '—'
const shortChecksum = (value) => (value ? `SHA-256: ${value.slice(0, 12)}…` : 'Chưa có checksum')
const statusLabel = (value) =>
  ({ PROCESSING: 'Đang tạo', SUCCESS: 'Sẵn sàng', FAILED: 'Thất bại' })[value] || value

onMounted(loadPage)
onBeforeUnmount(stopPolling)
</script>
