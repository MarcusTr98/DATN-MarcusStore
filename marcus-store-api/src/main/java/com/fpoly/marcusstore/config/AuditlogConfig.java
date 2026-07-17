package com.fpoly.marcusstore.config;

import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.contact.ContactRequest;
import com.fpoly.marcusstore.entity.interaction.AuditLog;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.cms.AuditLogRepository;
import com.fpoly.marcusstore.repository.contact.ContactRequestRepository;
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

    @Autowired
    private ContactRequestRepository contactRequestRepository;

    private static final Map<String, String> AUDITED_SERVICES = Map.ofEntries(
            // Sản phẩm & kho
            Map.entry("CategoriesService", "Categories"),
            Map.entry("ProductsService", "Products"),
            Map.entry("ProductImgService", "Product_Images"),
            Map.entry("AttributeService", "Attributes"),
            Map.entry("AttributeValueService", "Attribute_Values"),
            Map.entry("ProductConfigService", "Product_Skus"),

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
            String rawName = joinPoint.getSignature().getDeclaringType().getSimpleName();
            String className = rawName.endsWith("Impl")
                    ? rawName.substring(0, rawName.length() - 4)
                    : rawName;

            String tableName = AUDITED_SERVICES.get(className);
            if (tableName == null) return;

            String methodName = joinPoint.getSignature().getName();
            String action = resolveAction(methodName);
            if (action == null) return;

            AuditLog log = new AuditLog();
            log.setActionType(action);
            log.setTableName(tableName);
            log.setDescription(buildDescription(action, tableName, methodName, joinPoint, result));
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

    private String resolveAction(String methodName) {
        String m = methodName.toLowerCase();

        if (m.startsWith("add") || m.startsWith("create") || m.startsWith("insert")
                || m.startsWith("import") || m.startsWith("batch"))
            return "CREATE";

        if (m.startsWith("remove") || m.startsWith("delete") || m.startsWith("destroy")
                || m.startsWith("hidden") || m.startsWith("hide"))
            return "DELETE";

        if (m.startsWith("update") || m.startsWith("edit") || m.startsWith("modify")
                || m.startsWith("process") || m.startsWith("approve") || m.startsWith("reject")
                || m.startsWith("lock") || m.startsWith("unlock") || m.startsWith("toggle")
                || m.startsWith("publish") || m.startsWith("changestatus") || m.startsWith("resolve")
                || m.startsWith("cancel") || m.startsWith("verify") || m.startsWith("bulk"))
            return "UPDATE";

        return null;
    }

    private String buildDescription(String action, String tableName, String methodName,
                                    JoinPoint joinPoint, Object result) {
        // Lấy tên người thực hiện để đưa vào đầu câu
        String actorName = resolveActorName();

        // Xử lý đặc biệt cho resolveContact
        if ("resolveContact".equals(methodName)) {
            return buildResolveContactDescription(joinPoint, actorName);
        }

        // Xử lý đặc biệt cho submitContact
        if ("submitContact".equals(methodName)) {
            return buildSubmitContactDescription(joinPoint, actorName);
        }

        // Xử lý chung — format: "[Tên] đã [hành động] [bảng]: [label]"
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

        String base = label != null
                ? actorName + " " + actionText + " " + tableName + ": " + label
                : actorName + " " + actionText + " " + tableName;
        return base;
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
            // Lấy tên khách từ request arg
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

    private String resolveActorName() {
        try {
            Integer userId = SecurityUtils.getCurrentUserId();
            if (userId == null) return "Hệ thống";
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return "Hệ thống";
            return user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName()
                    : user.getUsername();
        } catch (Exception e) {
            return "Hệ thống";
        }
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