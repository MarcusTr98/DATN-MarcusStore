package com.fpoly.marcusstore.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderAssignmentResponse {
    private final Integer staffId;
    private final String staffName;
    private final String assignmentType;
    private final String assignedByName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime assignedAt;
}
