package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupRecordResponse {
    private String id;
    private String type;
    private String status;
    private String fileName;
    private Long fileSize;
    private String checksum;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}
