package com.fpoly.marcusstore.dto.response;

import com.fpoly.marcusstore.entity.shopping.WarrantyAttachment;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyReason;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyResponse {
    
    private Integer warrantyId;
    private Integer userId;
    private Integer orderItemId;
    private String orderCode;
    private String productName;
    private String productImage;
    private WarrantyReason reason;
    private String reasonLabel;
    private String description;
    private WarrantyStatus status;
    private String statusLabel;
    private String adminNote;
    private String processedByName;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AttachmentResponse> attachments;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentResponse {
        private Integer attachmentId;
        private String fileUrl;
        private String fileType;
        private String fileName;
        private Long fileSize;
    }
    
    public static String getReasonLabel(WarrantyReason reason) {
        return switch (reason) {
            case DEFECTIVE -> "Sản phẩm lỗi";
            case DAMAGED -> "Bị hư hỏng";
            case WRONG_ITEM -> "Giao sai sản phẩm";
            case NOT_AS_DESCRIBED -> "Không đúng mô tả";
            case ACCESSORY_MISSING -> "Thiếu phụ kiện";
            case OTHER -> "Lý do khác";
        };
    }
    
    public static String getStatusLabel(WarrantyStatus status) {
        return switch (status) {
            case PENDING -> "Chờ xử lý";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Từ chối";
            case COMPLETED -> "Hoàn thành";
        };
    }
}
