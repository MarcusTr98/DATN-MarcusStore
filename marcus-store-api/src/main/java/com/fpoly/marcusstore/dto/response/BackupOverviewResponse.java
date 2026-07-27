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
    private List<TableOverview> tables;

    @Getter
    @Builder
    public static class TableOverview {
        private String schema;
        private String table;
        private long records;
    }
}
