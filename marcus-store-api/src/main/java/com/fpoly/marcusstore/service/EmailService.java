package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

  // TODO: đổi thành domain thật khi lên production
  private static final String HOMEPAGE_URL = "http://localhost:8080";

  // Thứ tự các bước trong vòng đời đơn hàng (dùng để vẽ timeline)
  private static final String[] STATUS_FLOW = { "PENDING", "CONFIRMED", "PROCESSING", "PACKED", "SHIPPING", "DELIVERED",
      "COMPLETED" };

  // ============================================================
  // 1. GỬI OTP
  // ============================================================

  public void sendOtp(String email, String otp) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      helper.setTo(email);
      helper.setSubject("MarcusStore - Mã xác thực OTP của bạn");
      helper.setText(buildOtpHtml(otp), true);

      mailSender.send(mimeMessage);
    } catch (Exception e) {
      throw new RuntimeException("Gửi email OTP thất bại: " + e.getMessage(), e);
    }
  }

  private String buildOtpHtml(String otp) {
    return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="margin:0;padding:0;background-color:#eef0f4;font-family:'Segoe UI',Helvetica,Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#eef0f4;padding:40px 0;">
            <tr>
              <td align="center">
                <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);border:1px solid #f5d9d9;">

                  <!-- Header -->
                  <tr>
                    <td style="background-color:#d70018;padding:36px 40px 26px 40px;text-align:center;">
                      <div style="display:inline-block;width:52px;height:52px;line-height:52px;background-color:#ffffff;border-radius:50%%;font-size:24px;margin-bottom:14px;">
                        🔐
                      </div>
                      <div style="color:#ffffff;font-size:19px;font-weight:600;">
                        Xác thực tài khoản
                      </div>
                      <div style="color:#ffd4d4;font-size:12.5px;margin-top:6px;letter-spacing:0.5px;">
                        MARCUSSTORE SECURITY
                      </div>
                    </td>
                  </tr>

                  <!-- Body -->
                  <tr>
                    <td style="padding:34px 40px 8px 40px;text-align:center;">
                      <p style="font-size:15px;color:#333333;margin:0 0 6px 0;">
                        Xin chào,
                      </p>
                      <p style="font-size:14px;color:#666666;line-height:1.6;margin:0 auto;max-width:360px;">
                        Cảm ơn bạn đã sử dụng MarcusStore. Vui lòng dùng mã xác thực bên dưới để hoàn tất yêu cầu của bạn.
                      </p>
                    </td>
                  </tr>

                  <!-- OTP block -->
                  <tr>
                    <td style="padding:22px 40px 8px 40px;">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#fff5f5;border-radius:12px;border:1px solid #f5d9d9;">
                        <tr>
                          <td style="padding:26px;text-align:center;">
                            <div style="font-size:11px;color:#b3554f;letter-spacing:1.5px;text-transform:uppercase;margin-bottom:12px;">
                              Mã xác thực
                            </div>
                            <div style="display:inline-block;background-color:#d70018;color:#ffffff;font-size:28px;font-weight:700;letter-spacing:8px;padding:14px 26px;border-radius:8px;font-family:'Courier New',monospace;">
                              %s
                            </div>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <!-- Warning -->
                  <tr>
                    <td style="padding:20px 40px 8px 40px;text-align:center;">
                      <p style="font-size:13px;color:#d70018;background-color:#fff5f5;border-radius:8px;padding:12px 16px;margin:0;">
                        ⏳ Mã có hiệu lực trong <b>5 phút</b>. Vui lòng không chia sẻ mã này với bất kỳ ai.
                      </p>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding:12px 40px 32px 40px;text-align:center;">
                      <p style="font-size:12.5px;color:#888888;margin:0;">
                        Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.
                      </p>
                    </td>
                  </tr>

                  <!-- Footer -->
                  <tr>
                    <td style="background-color:#fff5f5;padding:22px 40px;text-align:center;border-top:1px solid #f5d9d9;">
                      <p style="font-size:11.5px;color:#997d7d;margin:0;line-height:1.6;">
                        Đây là email tự động, vui lòng không trả lời trực tiếp.<br/>
                        © 2026 MarcusStore. Trân trọng cảm ơn bạn đã đồng hành cùng chúng tôi.
                      </p>
                    </td>
                  </tr>

                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """
        .formatted(otp);
  }

  // ============================================================
  // 2. GỬI VOUCHER RIÊNG CHO KHÁCH (tông đỏ)
  // ============================================================

  public void sendVoucherAssigned(String email, String customerName, Voucher voucher) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      helper.setTo(email);
      helper.setSubject("🎁 Bạn vừa nhận được voucher ưu đãi từ MarcusStore!");
      helper.setText(buildVoucherHtml(customerName, voucher), true);

      mailSender.send(mimeMessage);
    } catch (Exception e) {
      throw new RuntimeException("Gửi email voucher thất bại: " + e.getMessage(), e);
    }
  }

  private String discountLabel(Voucher voucher) {
    return switch (voucher.getDiscountType()) {
      case "PERCENT" -> "Giảm " + voucher.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
      case "AMOUNT" -> "Giảm " + CURRENCY_FORMAT.format(voucher.getDiscountValue()) + "đ";
      case "FREESHIP" -> "Miễn phí vận chuyển";
      case "GIFT" -> "Tặng quà kèm đơn hàng";
      default -> "Ưu đãi đặc biệt";
    };
  }

  private String buildVoucherHtml(String customerName, Voucher voucher) {
    String maxDiscountRow = voucher.getMaxDiscountAmount() != null
        ? metaRow("Giảm tối đa", CURRENCY_FORMAT.format(voucher.getMaxDiscountAmount()) + "đ")
        : "";

    String minOrderRow = (voucher.getMinOrderValue() != null && voucher.getMinOrderValue().signum() > 0)
        ? metaRow("Đơn hàng từ", CURRENCY_FORMAT.format(voucher.getMinOrderValue()) + "đ")
        : "";

    return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="margin:0;padding:0;background-color:#f4f4f7;font-family:'Segoe UI',Helvetica,Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f7;padding:40px 0;">
            <tr>
              <td align="center">
                <table width="520" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);border:1px solid #f5d9d9;">

                  <!-- Header -->
                  <tr>
                    <td style="background-color:#d70018;padding:40px 40px 30px 40px;text-align:center;">
                      <div style="display:inline-block;width:56px;height:56px;line-height:56px;background-color:#ffffff;border-radius:50%%;font-size:26px;margin-bottom:16px;">
                        🎁
                      </div>
                      <div style="color:#ffffff;font-size:20px;font-weight:600;letter-spacing:0.3px;">
                        Voucher ưu đãi dành riêng cho bạn
                      </div>
                      <div style="color:#ffd4d4;font-size:13px;margin-top:6px;letter-spacing:0.5px;">
                        MARCUSSTORE MEMBERSHIP REWARD
                      </div>
                    </td>
                  </tr>

                  <!-- Body -->
                  <tr>
                    <td style="padding:36px 40px 8px 40px;text-align:center;">
                      <p style="font-size:15px;color:#333333;margin:0 0 6px 0;">
                        Xin chào <b>%s</b>,
                      </p>
                      <p style="font-size:14px;color:#666666;line-height:1.6;margin:0 auto 14px auto;max-width:400px;">
                        Cảm ơn bạn đã luôn tin tưởng và đồng hành cùng MarcusStore. Sự ủng hộ của bạn chính là động lực để chúng tôi không ngừng hoàn thiện.
                      </p>
                      <p style="font-size:14px;color:#666666;line-height:1.6;margin:0 auto 14px auto;max-width:380px;">
                        Đây là một phần quà nhỏ dành riêng cho bạn.
                      </p>
                      <p style="font-size:14px;color:#d70018;line-height:1.6;margin:0 auto;max-width:400px;font-weight:600;">
                        Chúng tôi đã thêm mã này vào kho voucher của bạn, hãy kiểm tra và sử dụng ngay tại trang thanh toán nhé!
                      </p>
                    </td>
                  </tr>

                  <!-- Voucher code block -->
                  <tr>
                    <td style="padding:24px 40px 8px 40px;">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#fff5f5;border-radius:12px;border:1px solid #f5d9d9;">
                        <tr>
                          <td style="padding:28px;text-align:center;">
                            <div style="font-size:11px;color:#b3554f;letter-spacing:1.5px;text-transform:uppercase;margin-bottom:10px;">
                              Mã ưu đãi
                            </div>
                            <div style="display:inline-block;background-color:#d70018;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:3px;padding:12px 28px;border-radius:8px;font-family:'Courier New',monospace;">
                              %s
                            </div>

                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top:22px;border-top:1px dashed #e0b3b3;">
                              <tr><td style="height:22px;"></td></tr>
                            </table>

                            <table width="100%%" cellpadding="0" cellspacing="0">
                              %s
                              %s
                              %s
                            </table>
                          </td>
                        </tr>
                      </table>

                      <p style="font-size:12.5px;color:#888888;margin:20px 0 0 0;text-align:center;">
                        Hiệu lực từ <b style="color:#555555;">%s</b> đến <b style="color:#555555;">%s</b>
                      </p>
                    </td>
                  </tr>

                  <!-- CTA: link to homepage -->
                  <tr>
                    <td style="padding:28px 40px 8px 40px;text-align:center;">
                      <a href="%s" target="_blank"
                         style="display:inline-block;background-color:#d70018;color:#ffffff;font-size:14px;font-weight:700;text-decoration:none;padding:13px 36px;border-radius:8px;letter-spacing:0.3px;">
                        Mua sắm ngay
                      </a>
                    </td>
                  </tr>

                  <!-- Footer -->
                  <tr>
                    <td style="background-color:#fff5f5;padding:22px 40px;text-align:center;border-top:1px solid #f5d9d9;margin-top:12px;">
                      <p style="font-size:11.5px;color:#997d7d;margin:0;line-height:1.6;">
                        Đây là email tự động, vui lòng không trả lời trực tiếp.<br/>
                        © 2026 MarcusStore. Trân trọng cảm ơn bạn đã đồng hành cùng chúng tôi.
                      </p>
                    </td>
                  </tr>

                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """
        .formatted(
            customerName,
            voucher.getVoucherCode(),
            metaRow("Ưu đãi", discountLabel(voucher)),
            maxDiscountRow,
            minOrderRow,
            voucher.getStartDate().format(DATE_FORMAT),
            voucher.getEndDate().format(DATE_FORMAT),
            HOMEPAGE_URL);
  }

  private String metaRow(String label, String value) {
    return """
        <tr>
          <td style="padding:5px 0;text-align:center;font-size:13.5px;color:#666666;">
            %s: <b style="color:#d70018;">%s</b>
          </td>
        </tr>
        """.formatted(label, value);
  }

  // ============================================================
  // 3. GỬI CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG (kèm chi tiết đơn hàng)
  // ============================================================

  public void sendOrderStatusUpdate(String email, String customerName, Order order, String status) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      helper.setTo(email);
      helper.setSubject("MarcusStore - Cập nhật đơn hàng #" + order.getOrderCode());
      helper.setText(buildOrderStatusHtml(customerName, order, status), true);

      mailSender.send(mimeMessage);
    } catch (Exception e) {
      throw new RuntimeException("Gửi email cập nhật đơn hàng thất bại: " + e.getMessage(), e);
    }
  }

  // Marcus lam them refund
  public void sendRefundStatusUpdate(
      String email, String customerName, Order order, BigDecimal amount, String status) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      String safeName = customerName == null || customerName.isBlank() ? "Quý khách" : customerName;
      String statusText = switch (status) {
        case "PENDING_APPROVAL" -> "Yêu cầu hoàn tiền đang chờ duyệt";
        case "PROCESSING", "RETRY_PENDING" -> "Yêu cầu hoàn tiền đang được xử lý";
        case "SUCCESS" -> "Hoàn tiền thành công";
        case "FAILED" -> "Hoàn tiền chưa thành công";
        default -> "Trạng thái hoàn tiền được cập nhật";
      };
      helper.setTo(email);
      helper.setSubject("MarcusStore - " + statusText + " #" + order.getOrderCode());
      helper.setText("""
          <html><body style="font-family:Arial,sans-serif;color:#333">
          <h2 style="color:#d70018">%s</h2>
          <p>Xin chào <b>%s</b>,</p>
          <p>Đơn hàng <b>%s</b> có số tiền hoàn dự kiến/thực tế là
          <b>%s đ</b>.</p>
          <p>MarcusStore hoàn toàn bộ số tiền đã thanh toán, bao gồm phí vận chuyển.</p>
          <p>Đây là email tự động từ MarcusStore.</p>
          </body></html>
          """.formatted(
          statusText,
          safeName,
          order.getOrderCode(),
          CURRENCY_FORMAT.format(amount)), true);
      mailSender.send(mimeMessage);
    } catch (Exception e) {
      throw new RuntimeException("Gửi email refund thất bại: " + e.getMessage(), e);
    }
  }

  private String statusLabel(String status) {
    return switch (status) {
      case "PENDING" -> "Lấy hàng thành công";
      case "SHIPPING" -> "Đang vận chuyển";
      case "DELIVERED" -> "Giao hàng thành công";
      case "READY_FOR_PICKUP" -> "Sẵn sàng nhận tại cửa hàng";
      default -> status;
    };
  }

  private String statusMessage(String status) {
    return switch (status) {
      case "PENDING" -> "Đơn vị vận chuyển đã lấy hàng thành công, đơn hàng của bạn sẽ sớm được giao đi.";
      case "SHIPPING" -> "Đơn hàng đang trên đường đến với bạn. Vui lòng để ý điện thoại nhé!";
      case "DELIVERED" -> "Đơn hàng đã được giao thành công. Cảm ơn bạn đã mua sắm cùng MarcusStore!";
      // Marcus thêm: nội dung riêng, tránh bảo khách đến cửa hàng trước khi hàng sẵn
      // sàng.
      case "READY_FOR_PICKUP" -> "Đơn hàng đã sẵn sàng. Bạn có thể đến Marcus Store để nhận hàng.";
      default -> "Trạng thái đơn hàng của bạn vừa được cập nhật.";
    };
  }

  private String buildOrderStatusHtml(String customerName, Order order, String status) {
    boolean isNegative = "CANCELLED".equals(status) || "FAILED".equals(status);
    String timelineHtml = isNegative
        ? buildNegativeTimeline(status)
        : buildTimeline(status, "STORE_PICKUP".equalsIgnoreCase(order.getFulfillmentMethod()));

    BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
    BigDecimal discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;

    String trackingRow = (order.getTrackingCode() != null && !order.getTrackingCode().isBlank())
        ? "<tr><td style=\"font-size:13.5px;color:#555555;padding:3px 0;\">Mã vận đơn: <b style=\"color:#222222;\">"
            + order.getTrackingCode() + "</b></td></tr>"
        : "";

    String discountRow = discountAmount.signum() > 0
        ? "<tr><td style=\"font-size:13px;color:#666666;padding:3px 0;\">Giảm giá</td>"
            + "<td style=\"font-size:13px;color:#d70018;text-align:right;\">-"
            + CURRENCY_FORMAT.format(discountAmount) + " đ</td></tr>"
        : "";

    List<OrderItem> items = order.getOrderItems();
    int itemCount = items != null ? items.size() : 0;

    // Width tăng gấp rưỡi so với bản cũ (560 -> 840) để bớt cảm giác chữ nhật đứng
    // / phải lăn chuột
    return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="margin:0;padding:0;background-color:#eef0f4;font-family:'Segoe UI',Helvetica,Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#eef0f4;padding:40px 0;">
            <tr>
              <td align="center">
                <table width="840" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);border:1px solid #f5d9d9;">

                  <!-- Header -->
                  <tr>
                    <td style="background-color:#d70018;padding:36px 56px 26px 56px;text-align:center;">
                      <div style="display:inline-block;width:52px;height:52px;line-height:52px;background-color:#ffffff;border-radius:50%%;font-size:24px;margin-bottom:14px;">
                        📦
                      </div>
                      <div style="color:#ffffff;font-size:19px;font-weight:600;">
                        Cập nhật đơn hàng của bạn
                      </div>
                      <div style="color:#ffd4d4;font-size:12.5px;margin-top:6px;letter-spacing:0.5px;">
                        MÃ ĐƠN HÀNG: %s
                      </div>
                    </td>
                  </tr>

                  <!-- Body -->
                  <tr>
                    <td style="padding:34px 56px 8px 56px;text-align:center;">
                      <p style="font-size:15px;color:#333333;margin:0 0 6px 0;">
                        Xin chào <b>%s</b>,
                      </p>
                      <p style="font-size:14px;color:#666666;line-height:1.6;margin:0 auto;max-width:480px;">
                        %s
                      </p>
                    </td>
                  </tr>

                  <!-- Timeline -->
                  <tr>
                    <td style="padding:28px 56px 8px 56px;">
                      <table width="100%%" cellpadding="0" cellspacing="0">
                        %s
                      </table>
                    </td>
                  </tr>

                  <!-- Current status badge -->
                  <tr>
                    <td style="padding:24px 56px 8px 56px;text-align:center;">
                      <span style="display:inline-block;background-color:%s;color:%s;font-size:13px;font-weight:700;letter-spacing:0.5px;padding:9px 22px;border-radius:20px;">
                        %s
                      </span>
                    </td>
                  </tr>

                  <!-- Recipient info -->
                  <tr>
                    <td style="padding:28px 56px 0 56px;">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#fafafa;border-radius:12px;border:1px solid #ececec;">
                        <tr>
                          <td style="padding:20px 24px;">
                            <div style="font-size:11px;color:#777777;letter-spacing:1px;text-transform:uppercase;margin-bottom:10px;">
                              Thông tin giao hàng
                            </div>
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr><td style="font-size:13.5px;color:#555555;padding:3px 0;">Người nhận: <b style="color:#222222;">%s</b></td></tr>
                              <tr><td style="font-size:13.5px;color:#555555;padding:3px 0;">Điện thoại: <b style="color:#222222;">%s</b></td></tr>
                              <tr><td style="font-size:13.5px;color:#555555;padding:3px 0;">Địa chỉ: <b style="color:#222222;">%s</b></td></tr>
                              <tr><td style="font-size:13.5px;color:#555555;padding:3px 0;">Thanh toán: <b style="color:#222222;">%s</b></td></tr>
                              %s
                            </table>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <!-- Order items -->
                  <tr>
                    <td style="padding:20px 56px 0 56px;">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border:1px solid #ececec;border-radius:12px;">
                        <tr>
                          <td style="padding:18px 24px 6px 24px;">
                            <div style="font-size:11px;color:#777777;letter-spacing:1px;text-transform:uppercase;margin-bottom:8px;">
                              Sản phẩm (%d)
                            </div>
                          </td>
                        </tr>
                        %s
                        <tr><td style="padding:8px 24px;"><div style="border-top:1px dashed #dddddd;"></div></td></tr>
                        <tr>
                          <td style="padding:4px 24px 18px 24px;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td style="font-size:13px;color:#666666;padding:3px 0;">Tạm tính</td>
                                <td style="font-size:13px;color:#333333;text-align:right;">%s đ</td>
                              </tr>
                              %s
                              <tr>
                                <td style="font-size:13px;color:#666666;padding:3px 0;">Phí vận chuyển</td>
                                <td style="font-size:13px;color:#333333;text-align:right;">%s đ</td>
                              </tr>
                              <tr>
                                <td style="font-size:14.5px;color:#1f1f2e;font-weight:700;padding:8px 0 0 0;">Tổng thanh toán</td>
                                <td style="font-size:16px;color:#d70018;font-weight:700;text-align:right;padding:8px 0 0 0;">%s đ</td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <tr><td style="height:32px;"></td></tr>

                  <!-- Footer -->
                  <tr>
                    <td style="background-color:#fff5f5;padding:22px 56px;text-align:center;border-top:1px solid #f5d9d9;">
                      <p style="font-size:11.5px;color:#997d7d;margin:0;line-height:1.6;">
                        Đây là email tự động, vui lòng không trả lời trực tiếp.<br/>
                        © 2026 MarcusStore. Trân trọng cảm ơn bạn đã đồng hành cùng chúng tôi.
                      </p>
                    </td>
                  </tr>

                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """
        .formatted(
            order.getOrderCode(),
            customerName,
            statusMessage(status),
            timelineHtml,
            isNegative ? "#f0f0f0" : "#fff5f5",
            isNegative ? "#666666" : "#d70018",
            statusLabel(status),
            order.getRecipientName(),
            order.getRecipientPhone(),
            order.getShippingAddress(),
            order.getPaymentMethod(),
            trackingRow,
            itemCount,
            buildItemRows(items),
            CURRENCY_FORMAT.format(order.getTotalAmount()),
            discountRow,
            CURRENCY_FORMAT.format(shippingFee),
            CURRENCY_FORMAT.format(order.getFinalAmount()));
  }

  private String buildItemRows(List<OrderItem> items) {
    if (items == null || items.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (OrderItem item : items) {
      String productName = item.getSku().getProduct().getProductName();
      String skuInfo = item.getSku().getSkuCode();
      BigDecimal lineTotal = item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));

      sb.append("""
          <tr>
            <td style="padding:6px 24px;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="font-size:13.5px;color:#333333;">
                    %s <span style="color:#666666;">x%d</span>
                    <div style="font-size:11.5px;color:#777777;">%s</div>
                  </td>
                  <td style="font-size:13.5px;color:#333333;text-align:right;vertical-align:top;">%s đ</td>
                </tr>
              </table>
            </td>
          </tr>
          """.formatted(productName, item.getQuantity(), skuInfo, CURRENCY_FORMAT.format(lineTotal)));
    }
    return sb.toString();
  }

  private String buildTimeline(String currentStatus, boolean storePickup) {
    // Marcus thêm: email đơn tại quầy không hiển thị các bước vận chuyển.
    String[] flow = storePickup
        ? new String[] { "PENDING", "PROCESSING", "READY_FOR_PICKUP" }
        : new String[] { "PENDING", "SHIPPING", "DELIVERED" };

    String[] labels = storePickup
        ? new String[] { "Đã đặt", "Chuẩn bị", "Sẵn sàng nhận" }
        : new String[] { "Lấy hàng", "Vận chuyển", "Đã giao" };

    int currentIndex = Arrays.asList(flow).indexOf(currentStatus);

    if (currentIndex < 0) {
      currentIndex = 0;
    }

    StringBuilder sb = new StringBuilder("<tr>");

    for (int i = 0; i < flow.length; i++) {

      boolean done = i <= currentIndex;

      String dotColor = done ? "#d70018" : "#e0e0e0";
      String textColor = done ? "#1f1f2e" : "#888888";
      String lineColor = i < currentIndex ? "#d70018" : "#e0e0e0";

      sb.append("<td style=\"text-align:center;\">");
      sb.append("<div style=\"width:12px;height:12px;border-radius:50%;background:")
          .append(dotColor)
          .append(";margin:0 auto 6px auto;\"></div>");

      sb.append("<div style=\"font-size:11px;color:")
          .append(textColor)
          .append(";\">")
          .append(labels[i])
          .append("</div>");

      sb.append("</td>");

      if (i < flow.length - 1) {
        sb.append("<td style=\"width:80px;\">")
            .append("<div style=\"height:2px;background:")
            .append(lineColor)
            .append(";margin-bottom:18px;\"></div>")
            .append("</td>");
      }
    }

    sb.append("</tr>");

    return sb.toString();
  }

  private String buildNegativeTimeline(String status) {
    String label = "FAILED".equals(status) ? "Giao hàng không thành công" : "Đơn hàng đã bị hủy";
    return """
        <tr>
          <td style="text-align:center;padding:12px 0;">
            <div style="width:14px;height:14px;border-radius:50%%;background-color:#c0392b;margin:0 auto 8px auto;"></div>
            <div style="font-size:12.5px;color:#c0392b;">%s</div>
          </td>
        </tr>
        """
        .formatted(label);
  }
}
