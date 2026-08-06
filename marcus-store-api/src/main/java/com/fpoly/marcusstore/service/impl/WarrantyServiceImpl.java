package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.CreateWarrantyRequest;
import com.fpoly.marcusstore.dto.request.UpdateWarrantyStatusRequest;
import com.fpoly.marcusstore.dto.response.WarrantyResponse;
import com.fpoly.marcusstore.dto.response.WarrantyResponse.AttachmentResponse;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.entity.shopping.WarrantyAttachment;
import com.fpoly.marcusstore.entity.shopping.WarrantyAttachment.FileType;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyStatus;
import com.fpoly.marcusstore.repository.shopping.OrderItemRepository;
import com.fpoly.marcusstore.repository.shopping.WarrantyAttachmentRepository;
import com.fpoly.marcusstore.repository.shopping.WarrantyRepository;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.service.WarrantyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarrantyServiceImpl implements WarrantyService {

    private final WarrantyRepository warrantyRepository;
    private final WarrantyAttachmentRepository warrantyAttachmentRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private static final int WARRANTY_MONTHS = 6;

    @Override
    @Transactional
    public WarrantyResponse createWarranty(Integer userId, CreateWarrantyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        if (!canRequestWarranty(userId, request.getOrderItemId())) {
            throw new RuntimeException("Bạn không thể yêu cầu bảo hành cho sản phẩm này");
        }

        WarrantyReturn warranty = new WarrantyReturn();
        warranty.setUser(user);
        warranty.setOrderItem(orderItem);
        warranty.setReason(request.getReason());
        warranty.setDescription(request.getDescription());
        warranty.setStatus(WarrantyStatus.PENDING);

        warranty = warrantyRepository.save(warranty);

        if (request.getAttachmentUrls() != null && !request.getAttachmentUrls().isEmpty()) {
            for (String url : request.getAttachmentUrls()) {
                WarrantyAttachment attachment = new WarrantyAttachment();
                attachment.setWarrantyReturn(warranty);
                attachment.setFileUrl(url);
                attachment.setFileType(url.contains("video") ? FileType.VIDEO : FileType.IMAGE);
                warrantyAttachmentRepository.save(attachment);
            }
        }

        return mapToResponse(warranty);
    }

    @Override
    public List<WarrantyResponse> getWarrantiesByUser(Integer userId) {
        return warrantyRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WarrantyResponse> getAllWarranties() {
        return warrantyRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WarrantyResponse> getWarrantiesByStatus(String status) {
        WarrantyStatus warrantyStatus = WarrantyStatus.valueOf(status.toUpperCase());
        return warrantyRepository.findByStatusOrderByCreatedAtDesc(warrantyStatus)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WarrantyResponse getWarrantyById(Integer warrantyId) {
        WarrantyReturn warranty = warrantyRepository.findById(warrantyId)
                .orElseThrow(() -> new RuntimeException("Warranty request not found"));
        return mapToResponse(warranty);
    }

    @Override
    @Transactional
    public WarrantyResponse updateWarrantyStatus(Integer warrantyId, Integer adminId, UpdateWarrantyStatusRequest request) {
        WarrantyReturn warranty = warrantyRepository.findById(warrantyId)
                .orElseThrow(() -> new RuntimeException("Warranty request not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        warranty.setStatus(request.getStatus());
        warranty.setAdminNote(request.getAdminNote());
        warranty.setProcessedBy(admin);
        warranty.setProcessedAt(LocalDateTime.now());

        warranty = warrantyRepository.save(warranty);
        return mapToResponse(warranty);
    }

    @Override
    public boolean canRequestWarranty(Integer userId, Integer orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId).orElse(null);
        if (orderItem == null) return false;

        var order = orderItem.getOrder();
        if (order == null || !order.getUser().getUserId().equals(userId)) return false;

        if (!"COMPLETED".equals(order.getOrderStatus())) return false;

        LocalDateTime warrantyEndDate = order.getUpdatedAt().plusMonths(WARRANTY_MONTHS);

        if (LocalDateTime.now().isAfter(warrantyEndDate)) return false;

        List<WarrantyStatus> activeStatuses = Arrays.asList(
                WarrantyStatus.PENDING, WarrantyStatus.APPROVED);
        return !warrantyRepository.existsByOrderItemOrderItemIdAndUserUserIdAndStatusIn(
                orderItemId, userId, activeStatuses);
    }

    @Override
    @Transactional(readOnly = true)
    public WarrantyResponse getWarrantyByOrderItemId(Integer userId, Integer orderItemId) {
        WarrantyReturn warranty = warrantyRepository
                .findByOrderItemOrderItemIdAndUserUserId(orderItemId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu bảo hành cho sản phẩm này"));
        return mapToResponse(warranty);
    }

    private WarrantyResponse mapToResponse(WarrantyReturn warranty) {
        OrderItem orderItem = warranty.getOrderItem();
        var order = orderItem.getOrder();
        var sku = orderItem.getSku();
        var product = sku.getProduct();

        List<AttachmentResponse> attachments = warranty.getAttachments().stream()
                .map(a -> AttachmentResponse.builder()
                        .attachmentId(a.getAttachmentId())
                        .fileUrl(a.getFileUrl())
                        .fileType(a.getFileType().name())
                        .fileName(a.getFileName())
                        .fileSize(a.getFileSize())
                        .build())
                .collect(Collectors.toList());

        return WarrantyResponse.builder()
                .warrantyId(warranty.getWarrantyId())
                .userId(warranty.getUser().getUserId())
                .orderItemId(orderItem.getOrderItemId())
                .orderCode(order.getOrderCode())
                .productName(product.getProductName())
                .productImage(sku.getSkuImageUrl())
                .reason(warranty.getReason())
                .reasonLabel(WarrantyResponse.getReasonLabel(warranty.getReason()))
                .description(warranty.getDescription())
                .status(warranty.getStatus())
                .statusLabel(WarrantyResponse.getStatusLabel(warranty.getStatus()))
                .adminNote(warranty.getAdminNote())
                .processedByName(warranty.getProcessedBy() != null ? warranty.getProcessedBy().getFullName() : null)
                .processedAt(warranty.getProcessedAt())
                .createdAt(warranty.getCreatedAt())
                .updatedAt(warranty.getUpdatedAt())
                .attachments(attachments)
                .build();
    }

    private String extractPublicId(String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) return null;
        try {
            String[] parts = cloudinaryUrl.split("/");
            int uploadIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if ("upload".equals(parts[i])) {
                    uploadIndex = i;
                    break;
                }
            }
            if (uploadIndex == -1 || uploadIndex + 2 >= parts.length) return null;
            StringBuilder publicId = new StringBuilder();
            for (int i = uploadIndex + 2; i < parts.length; i++) {
                String part = parts[i];
                int extIndex = part.lastIndexOf('.');
                if (extIndex > 0) {
                    part = part.substring(0, extIndex);
                }
                if (publicId.length() > 0) publicId.append("/");
                publicId.append(part);
            }
            return "marcus-store/" + publicId;
        } catch (Exception e) {
            return null;
        }
    }
}
