package com.fpoly.marcusstore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpoly.marcusstore.dto.response.BackupOverviewResponse;
import com.fpoly.marcusstore.dto.response.BackupRecordResponse;
import com.fpoly.marcusstore.entity.interaction.AuditLog;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.cms.AuditLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private static final String TYPE_BAK = "BAK";
    private static final String TYPE_EXCEL = "EXCEL";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final int EXCEL_MAX_ROWS = 1_048_576;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Set<String> SENSITIVE_COLUMN_MARKERS = Set.of(
            "password", "otp", "secret", "token", "credential", "hash");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Value("${backup.storage-path:D:/MarcusStoreBackups}")
    private String storagePath;

    @Value("${backup.sqlserver-directory:D:/MarcusStoreBackups}")
    private String sqlServerDirectory;

    @Value("${backup.retention.max-successful:10}")
    private int maxSuccessfulBackups;

    @Value("${backup.storage-warning-free-mb:1024}")
    private long storageWarningFreeMb;

    @PostConstruct
    void recoverInterruptedJobs() {
        // Marcus thêm: nếu backend tắt giữa chừng, job cũ không được treo PROCESSING
        // mãi.
        try {
            for (BackupRecordResponse record : listBackups()) {
                if (STATUS_PROCESSING.equals(record.getStatus())) {
                    record.setStatus(STATUS_FAILED);
                    record.setCompletedAt(LocalDateTime.now());
                    record.setErrorMessage("Tác vụ bị gián đoạn do backend đã dừng hoặc khởi động lại.");
                    writeMetadata(record);
                    deleteFileQuietly(record.getFileName());
                }
            }
        } catch (RuntimeException exception) {
            // Marcus sửa: thư mục backup cấu hình sai không được làm sập toàn bộ cửa hàng.
            log.warn("Trung tâm sao lưu chưa sẵn sàng: {}", exception.getMessage());
        }
    }

    public BackupOverviewResponse getOverview() {
        List<BackupOverviewResponse.TableOverview> tables = loadTables();
        List<BackupRecordResponse> records = listBackups();
        long storageBytes = records.stream()
                .filter(item -> item.getFileSize() != null)
                .mapToLong(BackupRecordResponse::getFileSize)
                .sum();
        Path storageRoot = ensureStorageDirectory();
        long availableStorageBytes;
        try {
            availableStorageBytes = Files.getFileStore(storageRoot).getUsableSpace();
        } catch (IOException exception) {
            availableStorageBytes = 0;
        }

        return BackupOverviewResponse.builder()
                .databaseName(currentDatabaseName())
                .tableCount(tables.size())
                .totalRecords(tables.stream().mapToLong(BackupOverviewResponse.TableOverview::getRecords).sum())
                .successfulBackups(records.stream().filter(item -> STATUS_SUCCESS.equals(item.getStatus())).count())
                .storageBytes(storageBytes)
                .availableStorageBytes(availableStorageBytes)
                .storageWarning(availableStorageBytes > 0
                        && availableStorageBytes < storageWarningFreeMb * 1024L * 1024L)
                .retentionLimit(Math.max(1, maxSuccessfulBackups))
                .tables(tables)
                .build();
    }

    public List<BackupRecordResponse> listBackups() {
        Path root = ensureStorageDirectory();
        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".meta.json"))
                    .map(this::readMetadataQuietly)
                    .filter(java.util.Objects::nonNull)
                    .sorted(Comparator.comparing(BackupRecordResponse::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc lịch sử sao lưu.", exception);
        }
    }

    public synchronized BackupRecordResponse initializeBackup(
            String type, String note, String username, String ipAddress) {
        // Marcus thêm: frontend đã khóa nút nhưng backend vẫn phải chặn request gọi
        // thẳng tạo nhiều file cùng lúc.
        boolean hasRunningJob = listBackups().stream()
                .anyMatch(item -> STATUS_PROCESSING.equals(item.getStatus()));
        if (hasRunningJob) {
            throw new IllegalStateException("Hệ thống đang tạo một bản sao lưu khác. Vui lòng chờ hoàn tất.");
        }

        String id = UUID.randomUUID().toString();
        String extension = TYPE_BAK.equals(type) ? ".bak" : ".xlsx";
        String fileName = "MarcusStore-" + type + "-" + LocalDateTime.now().format(FILE_TIME)
                + "-" + id.substring(0, 8) + extension;

        BackupRecordResponse record = BackupRecordResponse.builder()
                .id(id)
                .type(type)
                .status(STATUS_PROCESSING)
                .fileName(fileName)
                .fileSize(0L)
                .note(note == null ? "" : note.trim())
                .createdBy(username)
                .createdAt(LocalDateTime.now())
                .build();
        writeMetadata(record);
        writeAudit("BACKUP_REQUESTED", record, username, ipAddress);
        return record;
    }

    public void generateBackup(String id, String username, String ipAddress) {
        BackupRecordResponse record = getRecord(id);
        try {
            Path output = safeResolve(record.getFileName());
            // Marcus sửa: metadata bị chỉnh tay không được tự động rơi vào nhánh Excel.
            switch (record.getType()) {
                case TYPE_BAK -> generateSqlServerBackup(output);
                case TYPE_EXCEL -> generateExcelBackup(output);
                default -> throw new IllegalArgumentException("Loại bản sao lưu không hợp lệ.");
            }
            record.setStatus(STATUS_SUCCESS);
            record.setFileSize(Files.size(output));
            record.setChecksum(sha256(output));
            record.setSourceDatabase(currentDatabaseName());
            record.setIntegrityVerified(TYPE_BAK.equals(record.getType()));
            record.setCompletedAt(LocalDateTime.now());
            record.setErrorMessage(null);
            writeMetadata(record);
            writeAudit("BACKUP_COMPLETED", record, username, ipAddress);
            enforceRetention(record.getId());
        } catch (Exception exception) {
            log.error("Tạo backup {} thất bại", id, exception);
            record.setStatus(STATUS_FAILED);
            record.setCompletedAt(LocalDateTime.now());
            record.setErrorMessage(toSafeErrorMessage(record.getType()));
            writeMetadata(record);
            writeAudit("BACKUP_FAILED", record, username, ipAddress);
            deleteFileQuietly(record.getFileName());
        }
    }

    public Resource getDownload(String id, String username, String ipAddress) {
        BackupRecordResponse record = getRecord(id);
        if (!STATUS_SUCCESS.equals(record.getStatus())) {
            throw new IllegalStateException("Bản sao lưu chưa sẵn sàng để tải.");
        }
        try {
            Path file = safeResolve(record.getFileName());
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException("File sao lưu không còn tồn tại trên máy chủ.");
            }
            String actualChecksum = sha256(file);
            if (!actualChecksum.equalsIgnoreCase(record.getChecksum())) {
                throw new IllegalStateException("File sao lưu không vượt qua kiểm tra toàn vẹn.");
            }
            writeAudit("BACKUP_DOWNLOADED", record, username, ipAddress);
            return new UrlResource(file.toUri());
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể mở file sao lưu.", exception);
        }
    }

    public void deleteBackup(String id, String username, String ipAddress) {
        BackupRecordResponse record = getRecord(id);
        if (STATUS_PROCESSING.equals(record.getStatus())) {
            throw new IllegalStateException("Không thể xóa khi bản sao lưu đang được tạo.");
        }
        try {
            Files.deleteIfExists(safeResolve(record.getFileName()));
            Files.deleteIfExists(metadataPath(id));
            writeAudit("BACKUP_DELETED", record, username, ipAddress);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xóa bản sao lưu.", exception);
        }
    }

    public BackupRecordResponse getRecord(String id) {
        if (id == null || !id.matches("^[a-fA-F0-9-]{36}$")) {
            throw new IllegalArgumentException("Mã bản sao lưu không hợp lệ.");
        }
        Path metadata = metadataPath(id);
        if (!Files.isRegularFile(metadata)) {
            throw new IllegalArgumentException("Không tìm thấy bản sao lưu.");
        }
        try {
            return objectMapper.readValue(metadata.toFile(), BackupRecordResponse.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Metadata bản sao lưu bị lỗi.", exception);
        }
    }

    private void generateExcelBackup(Path output) throws IOException {
        List<BackupOverviewResponse.TableOverview> tables = loadTables();
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
                OutputStream stream = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW)) {
            workbook.setCompressTempFiles(true);
            CellStyle headerStyle = createHeaderStyle(workbook);
            Set<String> usedSheetNames = new HashSet<>();

            for (BackupOverviewResponse.TableOverview table : tables) {
                String sheetName = uniqueSheetName(table.getTable(), usedSheetNames);
                Sheet sheet = workbook.createSheet(sheetName);
                String sql = "SELECT * FROM " + quoteIdentifier(table.getSchema()) + "."
                        + quoteIdentifier(table.getTable());

                jdbcTemplate.query(sql, (ResultSetExtractor<Void>) resultSet -> {
                    ResultSetMetaData metadata = resultSet.getMetaData();
                    int columnCount = metadata.getColumnCount();
                    Row header = sheet.createRow(0);
                    for (int index = 1; index <= columnCount; index++) {
                        Cell cell = header.createCell(index - 1);
                        cell.setCellValue(metadata.getColumnLabel(index));
                        cell.setCellStyle(headerStyle);
                    }
                    sheet.createFreezePane(0, 1);
                    sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0,
                            Math.max(0, columnCount - 1)));

                    int rowIndex = 1;
                    while (resultSet.next()) {
                        if (rowIndex >= EXCEL_MAX_ROWS)
                            break;
                        Row row = sheet.createRow(rowIndex++);
                        for (int index = 1; index <= columnCount; index++) {
                            String columnName = metadata.getColumnLabel(index);
                            writeCell(row.createCell(index - 1), resultSet.getObject(index),
                                    isSensitiveColumn(columnName));
                        }
                    }
                    for (int index = 0; index < Math.min(columnCount, 20); index++) {
                        sheet.setColumnWidth(index, 18 * 256);
                    }
                    return null;
                });
            }
            workbook.write(stream);
        }
    }

    private void generateSqlServerBackup(Path backendOutput) {
        Path sqlDirectory = Path.of(sqlServerDirectory).toAbsolutePath().normalize();
        if (!sqlDirectory.equals(ensureStorageDirectory())) {
            throw new IllegalStateException(
                    "backup.storage-path và backup.sqlserver-directory phải trỏ cùng một thư mục dùng chung.");
        }
        String database = currentDatabaseName();
        if (!database.matches("^[A-Za-z0-9_-]+$")) {
            throw new IllegalStateException("Tên database không hợp lệ để sao lưu.");
        }
        String sqlPath = backendOutput.toAbsolutePath().toString().replace("'", "''");
        String databaseIdentifier = quoteIdentifier(database);
        String baseCommand = "BACKUP DATABASE " + databaseIdentifier + " TO DISK = N'" + sqlPath + "'";
        try {
            jdbcTemplate.execute(baseCommand + " WITH COPY_ONLY, COMPRESSION, CHECKSUM, INIT");
        } catch (RuntimeException compressionException) {
            // Marcus sửa: một số edition SQL Server không hỗ trợ COMPRESSION.
            log.warn("Backup compression không khả dụng, thử lại không nén: {}",
                    compressionException.getMessage());
            jdbcTemplate.execute(baseCommand + " WITH COPY_ONLY, CHECKSUM, INIT");
        }
        jdbcTemplate.execute("RESTORE VERIFYONLY FROM DISK = N'" + sqlPath + "' WITH CHECKSUM");
    }

    private List<BackupOverviewResponse.TableOverview> loadTables() {
        // Marcus sửa: lấy luôn số cột từ metadata SQL Server để Trung tâm sao lưu
        // mô tả đúng cả cấu trúc và số bản ghi của từng bảng.
        List<TableMetadata> tableMetadata = jdbcTemplate.query("""
                SELECT tables.TABLE_SCHEMA, tables.TABLE_NAME, COUNT(columns.COLUMN_NAME) AS column_count
                FROM INFORMATION_SCHEMA.TABLES tables
                INNER JOIN INFORMATION_SCHEMA.COLUMNS columns
                    ON columns.TABLE_SCHEMA = tables.TABLE_SCHEMA
                   AND columns.TABLE_NAME = tables.TABLE_NAME
                WHERE tables.TABLE_TYPE = 'BASE TABLE'
                GROUP BY tables.TABLE_SCHEMA, tables.TABLE_NAME
                ORDER BY tables.TABLE_SCHEMA, tables.TABLE_NAME
                """, (resultSet, rowNumber) -> new TableMetadata(
                        resultSet.getString("TABLE_SCHEMA"),
                        resultSet.getString("TABLE_NAME"),
                        resultSet.getInt("column_count")));
        List<BackupOverviewResponse.TableOverview> tables = new ArrayList<>();
        for (TableMetadata metadata : tableMetadata) {
            if (!isSafeIdentifier(metadata.schema()) || !isSafeIdentifier(metadata.table()))
                continue;
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT_BIG(*) FROM " + quoteIdentifier(metadata.schema()) + "."
                            + quoteIdentifier(metadata.table()),
                    Long.class);
            tables.add(BackupOverviewResponse.TableOverview.builder()
                    .schema(metadata.schema())
                    .table(metadata.table())
                    .columnCount(metadata.columnCount())
                    .records(count == null ? 0 : count)
                    .build());
        }
        return tables;
    }

    /**
     * Marcus thêm: phục hồi thật vào database test tên ngẫu nhiên, đếm bảng rồi
     * xóa ngay. Không bao giờ ghi đè MarcusStoreDB đang chạy.
     */
    public synchronized BackupRecordResponse testRestore(String id, String username, String ipAddress) {
        BackupRecordResponse record = getRecord(id);
        if (!TYPE_BAK.equals(record.getType()) || !STATUS_SUCCESS.equals(record.getStatus())) {
            throw new IllegalStateException("Chỉ bản BAK sẵn sàng mới có thể kiểm tra phục hồi.");
        }
        Path backup = safeResolve(record.getFileName());
        if (!Files.isRegularFile(backup) || !sha256(backup).equalsIgnoreCase(record.getChecksum())) {
            throw new IllegalStateException("File BAK không vượt qua kiểm tra checksum.");
        }
        String testDatabase = "MarcusRestoreTest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String escapedBackup = backup.toAbsolutePath().toString().replace("'", "''");
        Path restoreDirectory = Path.of(sqlServerDirectory).toAbsolutePath().normalize();
        try {
            List<java.util.Map<String, Object>> files = jdbcTemplate.queryForList(
                    "RESTORE FILELISTONLY FROM DISK = N'" + escapedBackup + "'");
            if (files.isEmpty()) throw new IllegalStateException("BAK không chứa file dữ liệu SQL Server.");
            StringBuilder moves = new StringBuilder();
            int dataIndex = 0;
            int logIndex = 0;
            for (java.util.Map<String, Object> file : files) {
                String logical = String.valueOf(file.get("LogicalName")).replace("'", "''");
                boolean logFile = "L".equalsIgnoreCase(String.valueOf(file.get("Type")));
                String suffix = logFile ? "_log_" + (++logIndex) + ".ldf" : "_data_" + (++dataIndex) + ".mdf";
                String physical = restoreDirectory.resolve(testDatabase + suffix).toString().replace("'", "''");
                moves.append(", MOVE N'").append(logical).append("' TO N'").append(physical).append("'");
            }
            jdbcTemplate.execute("RESTORE DATABASE " + quoteIdentifier(testDatabase)
                    + " FROM DISK = N'" + escapedBackup + "' WITH RECOVERY, REPLACE" + moves);
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + quoteIdentifier(testDatabase) + ".sys.tables", Integer.class);
            record.setRestoreTestStatus("SUCCESS");
            record.setRestoreTestMessage("Phục hồi thử thành công " + (tableCount == null ? 0 : tableCount) + " bảng.");
            record.setRestoreTestedAt(LocalDateTime.now());
            writeMetadata(record);
            writeAudit("BACKUP_RESTORE_TESTED", record, username, ipAddress);
            return record;
        } catch (Exception exception) {
            record.setRestoreTestStatus("FAILED");
            record.setRestoreTestMessage("Không thể phục hồi thử. Kiểm tra quyền RESTORE và thư mục SQL Server.");
            record.setRestoreTestedAt(LocalDateTime.now());
            writeMetadata(record);
            throw new IllegalStateException(record.getRestoreTestMessage(), exception);
        } finally {
            try {
                jdbcTemplate.execute("IF DB_ID(N'" + testDatabase + "') IS NOT NULL BEGIN "
                        + "ALTER DATABASE " + quoteIdentifier(testDatabase)
                        + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE "
                        + quoteIdentifier(testDatabase) + "; END");
            } catch (Exception cleanupException) {
                log.error("Không thể xóa database restore test {}", testDatabase, cleanupException);
            }
        }
    }

    private record TableMetadata(String schema, String table, int columnCount) {
    }

    private String currentDatabaseName() {
        return jdbcTemplate.execute((ConnectionCallback<String>) connection -> connection.getCatalog());
    }

    private void enforceRetention(String currentId) {
        int keep = Math.max(1, maxSuccessfulBackups);
        List<BackupRecordResponse> successful = listBackups().stream()
                .filter(item -> STATUS_SUCCESS.equals(item.getStatus()))
                .sorted(Comparator.comparing(BackupRecordResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        successful.stream().skip(keep).filter(item -> !currentId.equals(item.getId())).forEach(item -> {
            try {
                Files.deleteIfExists(safeResolve(item.getFileName()));
                Files.deleteIfExists(metadataPath(item.getId()));
            } catch (IOException exception) {
                log.warn("Không thể dọn backup cũ {}", item.getFileName());
            }
        });
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private void writeCell(Cell cell, Object value, boolean sensitive) {
        if (sensitive && value != null) {
            cell.setCellValue("[ĐÃ CHE]");
        } else if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof Timestamp timestamp) {
            cell.setCellValue(timestamp.toLocalDateTime().toString());
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.toString());
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date.toString());
        } else if (value instanceof byte[] bytes) {
            cell.setCellValue("[DỮ LIỆU NHỊ PHÂN " + bytes.length + " bytes]");
        } else {
            String text = String.valueOf(value);
            cell.setCellValue(text.length() > 32_767 ? text.substring(0, 32_767) : text);
        }
    }

    private boolean isSensitiveColumn(String columnName) {
        String normalized = columnName.toLowerCase(Locale.ROOT);
        return SENSITIVE_COLUMN_MARKERS.stream().anyMatch(normalized::contains);
    }

    private String uniqueSheetName(String rawName, Set<String> used) {
        String base = rawName.replaceAll("[\\\\/?*\\[\\]:]", "_");
        base = base.substring(0, Math.min(base.length(), 31));
        String candidate = base;
        int suffix = 2;
        while (!used.add(candidate.toLowerCase(Locale.ROOT))) {
            String tail = "-" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 31 - tail.length())) + tail;
        }
        return candidate;
    }

    private Path ensureStorageDirectory() {
        try {
            Path root = Path.of(storagePath).toAbsolutePath().normalize();
            Files.createDirectories(root);
            return root;
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo thư mục lưu backup.", exception);
        }
    }

    private Path safeResolve(String fileName) {
        if (fileName == null || !fileName.matches("^[A-Za-z0-9._-]+$")) {
            throw new IllegalArgumentException("Tên file backup không hợp lệ.");
        }
        Path root = ensureStorageDirectory();
        Path resolved = root.resolve(fileName).normalize();
        if (!resolved.getParent().equals(root)) {
            throw new IllegalArgumentException("Đường dẫn backup không hợp lệ.");
        }
        return resolved;
    }

    private Path metadataPath(String id) {
        return safeResolve(id + ".meta.json");
    }

    private void writeMetadata(BackupRecordResponse record) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath(record.getId()).toFile(), record);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu lịch sử sao lưu.", exception);
        }
    }

    private BackupRecordResponse readMetadataQuietly(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), BackupRecordResponse.class);
        } catch (Exception exception) {
            log.warn("Bỏ qua metadata backup không hợp lệ: {}", path.getFileName());
            return null;
        }
    }

    private String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tính checksum của file sao lưu.", exception);
        }
    }

    private void writeAudit(String action, BackupRecordResponse record, String username, String ipAddress) {
        try {
            AuditLog audit = new AuditLog();
            audit.setActionType(action);
            audit.setTableName("SYSTEM_BACKUP");
            audit.setDescription("%s %s (%s)".formatted(action, record.getFileName(), record.getId()));
            audit.setIpAddress(ipAddress);
            userRepository.findByUsername(username).ifPresent(audit::setUser);
            auditLogRepository.save(audit);
        } catch (Exception exception) {
            log.error("Không thể ghi audit cho thao tác backup {}", action, exception);
        }
    }

    private String toSafeErrorMessage(String type) {
        if (TYPE_BAK.equals(type)) {
            return "SQL Server không thể tạo hoặc xác minh file .bak. Kiểm tra quyền thư mục và quyền BACKUP DATABASE.";
        }
        return "Không thể tạo file Excel. Kiểm tra dung lượng ổ đĩa và quyền ghi thư mục backup.";
    }

    private void deleteFileQuietly(String fileName) {
        try {
            Files.deleteIfExists(safeResolve(fileName));
        } catch (Exception ignored) {
            // Marcus: metadata FAILED vẫn được giữ để admin biết nguyên nhân.
        }
    }

    private boolean isSafeIdentifier(String value) {
        return value != null && value.matches("^[A-Za-z0-9_]+$");
    }

    private String quoteIdentifier(String value) {
        if (!isSafeIdentifier(value))
            throw new IllegalArgumentException("Tên SQL không hợp lệ.");
        return "[" + value + "]";
    }
}
