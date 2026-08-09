package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BackupOverviewResponse {
    private String databaseName;
    private int tableCount;
    private long totalRecords;
    private long successfulBackups;
    private long storageBytes;
    private long availableStorageBytes;
    private boolean storageWarning;
    private int retentionLimit;
    private List<TableOverview> tables;

    @Getter
    @Builder
    public static class TableOverview {
        private String schema;
        private String table;
        // Marcus thêm: giúp Admin biết quy mô cấu trúc, không chỉ số bản ghi.
        private int columnCount;
        private long records;
    }
}
