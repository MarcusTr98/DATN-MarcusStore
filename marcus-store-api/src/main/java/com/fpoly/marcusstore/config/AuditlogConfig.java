package com.fpoly.marcusstore.config;

import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.contact.ContactRequest;
import com.fpoly.marcusstore.entity.interaction.AuditLog;
import com.fpoly.marcusstore.entity.interaction.CommentEvaluation;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.cms.AuditLogRepository;
import com.fpoly.marcusstore.repository.contact.ContactRequestRepository;
import com.fpoly.marcusstore.repository.statistics.CommentEvaluationRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Aspect
@Component
public class AuditlogConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuditlogConfig.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRequestRepository contactRequestRepository;

    @Autowired
    private CommentEvaluationRepository commentEvaluationRepository;

    private static final Map<String, String> AUDITED_SERVICES = Map.ofEntries(
            // Sản phẩm & kho
            Map.entry("CategoriesService", "Categories"),
            Map.entry("ProductsService", "Products"),
            Map.entry("ProductImgService", "Product_Images"),
            Map.entry("AttributeService", "Attributes"),
            Map.entry("AttributeValueService", "Attribute_Values"),
            Map.entry("ProductConfigService", "Product_Skus"),
            Map.entry("InventoryService", "Product_Skus"),
            Map.entry("ProductItemService", "Product_Items"),

            // Kênh bán hàng
            Map.entry("OrderService", "Orders"),
            Map.entry("OrderTransactionService", "Order_Transactions"),
            Map.entry("VoucherService", "Vouchers"),
            Map.entry("UserVoucherService", "User_Vouchers"),
            Map.entry("FlashSaleService", "Flash_Sale_Slots"),

            // Nội dung & Tương tác
            Map.entry("PostService", "Posts"),
            Map.entry("PostCategoryService", "Post_Categories"),
            Map.entry("BannerService", "Banners"),
            Map.entry("ReviewAdminService", "Product_Reviews"),

            // Liên hệ
            Map.entry("ContactService", "Contact_Requests"),

            // Hệ thống & tài khoản
            Map.entry("UserService", "Users"),
            Map.entry("UserAddressService", "User_Addresses"),
            Map.entry("PermissionService", "User_Permissions"),
            Map.entry("SystemSettingService", "System_Settings"),
            Map.entry("ShippingService", "Shipping_Config")
    );

    @Pointcut("execution(public * com.fpoly.marcusstore.service..*(..))")
    public void anyServiceMethod() {}

    @AfterReturning(pointcut = "anyServiceMethod()", returning = "result")
    public void logAfterSuccess(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();

            // Bỏ qua không ghi log khi khách vãng lai tự gửi form liên hệ
            if ("submitContact".equals(methodName)) return;

            String action = resolveAction(methodName);
            if (action == null) return;

            // Lấy tên class Service
            String rawName = joinPoint.getSignature().getDeclaringType().getSimpleName();
            String className = rawName.endsWith("Impl")
                    ? rawName.substring(0, rawName.length() - 4)
                    : rawName;

            String tableName = AUDITED_SERVICES.get(className);
            if (tableName == null) return;

            // Query User 1 lần duy nhất
            Integer currentUserId = safeGetCurrentUserId();
            User currentUser = (currentUserId != null) ? userRepository.findById(currentUserId).orElse(null) : null;
            String actorName = resolveActorName(currentUser);

            AuditLog log = new AuditLog();
            log.setActionType(action);
            log.setTableName(tableName);
            log.setUser(currentUser);
            log.setDescription(buildDescription(action, tableName, methodName, joinPoint, result, actorName));
            log.setIpAddress(resolveClientIp());

            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Ghi Audit_Logs thất bại (không ảnh hưởng tới thao tác chính): {}", e.getMessage(), e);
        }
    }

    private String resolveAction(String methodName) {
        String m = methodName.toLowerCase();

        // Tách riêng hành động Phản hồi đánh giá
        if (m.startsWith("reply"))
            return "REVIEW_REPLIED";

        if (m.startsWith("add") || m.startsWith("create") || m.startsWith("insert")
                || m.startsWith("batch") || m.startsWith("import"))
            return "CREATE";

        if (m.startsWith("remove") || m.startsWith("delete") || m.startsWith("destroy")
                || m.startsWith("hidden") || m.startsWith("hide"))
            return "DELETE";

        if (m.startsWith("update") || m.startsWith("edit") || m.startsWith("modify")
                || m.startsWith("process") || m.startsWith("approve") || m.startsWith("reject")
                || m.startsWith("lock") || m.startsWith("unlock") || m.startsWith("toggle")
                || m.startsWith("publish") || m.startsWith("changestatus") || m.startsWith("resolve")
                || m.startsWith("cancel") || m.startsWith("verify") || m.startsWith("bulk")
                || m.startsWith("adjust") || m.startsWith("restore"))
            return "UPDATE";

        return null;
    }

    private String buildDescription(String action, String tableName, String methodName,
                                    JoinPoint joinPoint, Object result, String actorName) {

        if ("resolveContact".equals(methodName)) {
            return buildResolveContactDescription(joinPoint, actorName);
        }

        if ("submitContact".equals(methodName)) {
            return buildSubmitContactDescription(joinPoint, actorName);
        }

        if ("Product_Reviews".equals(tableName)) {
            return buildReviewDescription(action, methodName, joinPoint, actorName);
        }

        String actionText = switch (action) {
            case "CREATE" -> "đã tạo mới";
            case "UPDATE" -> "đã cập nhật";
            case "DELETE" -> "đã xoá";
            case "REVIEW_REPLIED" -> "đã phản hồi";
            default -> "đã thao tác trên";
        };

        String label = extractLabel(result);
        if (label == null) {
            for (Object arg : joinPoint.getArgs()) {
                label = extractLabel(arg);
                if (label != null) break;
            }
        }

        return label != null
                ? actorName + " " + actionText + " " + tableName + ": " + label
                : actorName + " " + actionText + " " + tableName;
    }

    private String buildReviewDescription(String action, String methodName, JoinPoint joinPoint, String actorName) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length == 0) return actorName + " đã thao tác trên Đánh giá & Bình luận";

            Integer reviewId = (args[0] instanceof Integer id) ? id : null;
            String productName = "";

            if (reviewId != null) {
                CommentEvaluation review = commentEvaluationRepository.findById(reviewId).orElse(null);
                if (review != null && review.getProduct() != null) {
                    productName = " (Sản phẩm: " + review.getProduct().getProductName() + ")";
                }
            }

            if ("deleteReview".equals(methodName)) {
                return actorName + " đã xóa đánh giá #" + reviewId + productName;
            }

            if ("replyReview".equals(methodName)) {
                return actorName + " đã gửi phản hồi cho đánh giá #" + reviewId + productName;
            }

            if ("updateReply".equals(methodName)) {
                return actorName + " đã cập nhật phản hồi cho đánh giá #" + reviewId + productName;
            }

            return actorName + " đã thao tác trên đánh giá #" + reviewId;
        } catch (Exception e) {
            logger.warn("Không build được description cho ReviewAdminService: {}", e.getMessage());
            return actorName + " đã thao tác trên Đánh giá & Bình luận";
        }
    }

    private String buildResolveContactDescription(JoinPoint joinPoint, String actorName) {
        try {
            Object arg0 = joinPoint.getArgs().length > 0 ? joinPoint.getArgs()[0] : null;
            if (!(arg0 instanceof Integer contactId)) {
                return actorName + " đã xác nhận liên hệ/khiếu nại";
            }

            ContactRequest contact = contactRequestRepository.findById(contactId).orElse(null);
            if (contact == null) {
                return actorName + " đã xác nhận liên hệ/khiếu nại #" + contactId;
            }

            String customerName = contact.getCustomerName() != null
                    ? contact.getCustomerName() : "Khách vãng lai";
            String phone = contact.getPhoneNumber() != null
                    ? " (" + contact.getPhoneNumber() + ")" : "";

            return actorName + " đã xác nhận liên hệ/khiếu nại của khách: "
                    + customerName + phone;

        } catch (Exception e) {
            logger.warn("Không build được description cho resolveContact: {}", e.getMessage());
            return actorName + " đã xác nhận liên hệ/khiếu nại";
        }
    }

    private String buildSubmitContactDescription(JoinPoint joinPoint, String actorName) {
        try {
            for (Object arg : joinPoint.getArgs()) {
                String label = extractLabel(arg);
                if (label != null) {
                    return label + " đã gửi yêu cầu liên hệ/khiếu nại";
                }
            }
            return actorName + " đã gửi yêu cầu liên hệ/khiếu nại";
        } catch (Exception e) {
            return "Khách hàng đã gửi yêu cầu liên hệ/khiếu nại";
        }
    }

    private String resolveActorName(User user) {
        if (user == null) return "Hệ thống";
        return (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName()
                : user.getUsername();
    }

    private static final String[] LABEL_GETTERS = {
            "getTitle", "getName", "getProductName", "getFullName",
            "getVoucherCode", "getOrderCode", "getUsername", "getSlug",
            "getCustomerName", "getPhoneNumber", "getEmail", "getMessage"
    };

    private String extractLabel(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        if (obj instanceof Integer || obj instanceof Long) return "#" + obj;

        if (obj instanceof Optional<?> opt) return opt.map(this::extractLabel).orElse(null);
        if (obj instanceof Collection<?> col && !col.isEmpty()) {
            return extractLabel(col.iterator().next());
        }

        for (String getterName : LABEL_GETTERS) {
            try {
                Method m = obj.getClass().getMethod(getterName);
                Object value = m.invoke(obj);
                if (value != null && !value.toString().isBlank()) {
                    return value.toString();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String resolveClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;

        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private Integer safeGetCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}