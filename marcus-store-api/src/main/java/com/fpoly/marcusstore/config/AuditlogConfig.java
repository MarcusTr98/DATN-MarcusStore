package com.fpoly.marcusstore.config;

import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.interaction.AuditLog;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.cms.AuditLogRepository;
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
import java.util.Map;

@Aspect
@Component
public class AuditlogConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuditlogConfig.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Map<String, String> AUDITED_SERVICES = Map.ofEntries(
            // Sản phẩm & kho
            Map.entry("CategoriesService", "Categories"),
            Map.entry("ProductsService", "Products"),
            Map.entry("ProductImgService", "Product_Images"),
            Map.entry("AttributeService", "Attributes"),
            Map.entry("AttributeValueService", "Attribute_Values"),
            Map.entry("ProductConfigService", "Product_Skus"), // ← FIX: thêm SKU service

            // Kênh bán hàng
            Map.entry("OrderService", "Orders"),
            Map.entry("OrderTransactionService", "Order_Transactions"),
            Map.entry("VoucherService", "Vouchers"),
            Map.entry("UserVoucherService", "User_Vouchers"),
            Map.entry("FlashSaleService", "Flash_Sale_Slots"),

            // Nội dung
            Map.entry("PostService", "Posts"),
            Map.entry("PostCategoryService", "Post_Categories"),
            Map.entry("BannerService", "Banners"),

            // Hệ thống & tài khoản
            Map.entry("UserService", "Users"),
            Map.entry("UserAddressService", "User_Addresses"),
            Map.entry("PermissionService", "User_Permissions"),
            Map.entry("SystemSettingService", "System_Settings"),
            Map.entry("ShippingService", "Shipping_Config"),
            Map.entry("AdminNotificationService", "Admin_Notifications")
    );

    // FIX: bỏ "*Service" ở cuối để bắt được cả *ServiceImpl
    @Pointcut("execution(public * com.fpoly.marcusstore.service..*(..))")
    public void anyServiceMethod() {}

    @AfterReturning(pointcut = "anyServiceMethod()", returning = "result")
    public void logAfterSuccess(JoinPoint joinPoint, Object result) {
        try {
            String rawName = joinPoint.getSignature().getDeclaringType().getSimpleName();
            String className = rawName.endsWith("Impl")
                    ? rawName.substring(0, rawName.length() - 4)
                    : rawName;

            String tableName = AUDITED_SERVICES.get(className);
            if (tableName == null) return; // Service này chưa đăng ký theo dõi -> bỏ qua

            String methodName = joinPoint.getSignature().getName();
            String action = resolveAction(methodName);
            if (action == null) return; // không phải thao tác ghi dữ liệu -> bỏ qua (getAll, getOne...)

            AuditLog log = new AuditLog();
            log.setActionType(action);
            log.setTableName(tableName);
            log.setDescription(buildDescription(action, tableName, joinPoint, result));
            log.setIpAddress(resolveClientIp());

            Integer currentUserId = safeGetCurrentUserId();
            if (currentUserId != null) {
                userRepository.findById(currentUserId).ifPresent(log::setUser);
            }

            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Ghi Audit_Logs thất bại (không ảnh hưởng tới thao tác chính): {}", e.getMessage(), e);
        }
    }

    // Nhận diện hành động dựa trên tiền tố tên method.
    // FIX: thêm "batch" (batchCreateSkus) vào CREATE, thêm "bulk" (bulkUpdateSkus) vào UPDATE
    private String resolveAction(String methodName) {
        String m = methodName.toLowerCase();

        // TẠO MỚI
        if (m.startsWith("add") || m.startsWith("create") || m.startsWith("insert")
                || m.startsWith("import") || m.startsWith("batch")) return "CREATE";

        // XOÁ (kể cả xoá mềm/ẩn — hiddenCategory, hiddenProduct)
        if (m.startsWith("remove") || m.startsWith("delete") || m.startsWith("destroy")
                || m.startsWith("hidden") || m.startsWith("hide")) return "DELETE";

        // CẬP NHẬT (mọi thao tác thay đổi trạng thái khác đều gộp vào UPDATE)
        if (m.startsWith("update") || m.startsWith("edit") || m.startsWith("modify")
                || m.startsWith("process") || m.startsWith("approve") || m.startsWith("reject")
                || m.startsWith("lock") || m.startsWith("unlock") || m.startsWith("toggle")
                || m.startsWith("publish") || m.startsWith("changestatus") || m.startsWith("resolve")
                || m.startsWith("cancel") || m.startsWith("verify")
                || m.startsWith("bulk")) return "UPDATE";

        return null;
    }

    private String buildDescription(String action, String tableName, JoinPoint joinPoint, Object result) {
        String actionText = switch (action) {
            case "CREATE" -> "đã tạo mới";
            case "UPDATE" -> "đã cập nhật";
            case "DELETE" -> "đã xoá";
            default -> "đã thao tác trên";
        };

        String label = extractLabel(result);
        if (label == null) {
            for (Object arg : joinPoint.getArgs()) {
                label = extractLabel(arg);
                if (label != null) break;
            }
        }
        return label != null ? actionText + " " + tableName + ": " + label : actionText + " " + tableName;
    }

    private static final String[] LABEL_GETTERS = {
            "getTitle", "getName", "getProductName", "getFullName",
            "getVoucherCode", "getOrderCode", "getUsername", "getSlug"
    };

    private String extractLabel(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        if (obj instanceof Integer || obj instanceof Long) return "#" + obj;

        for (String getterName : LABEL_GETTERS) {
            try {
                Method m = obj.getClass().getMethod(getterName);
                Object value = m.invoke(obj);
                if (value != null && !value.toString().isBlank()) {
                    return value.toString();
                }
            } catch (Exception ignored) {
                // Không có getter này -> thử getter tiếp theo
            }
        }
        return null;
    }

    private String resolveClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;

        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
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