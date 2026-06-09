package com.fpoly.marcusstore.service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtp(String email, String otp) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(email);

        message.setSubject(
                "MarcusStore - OTP xác thực"
        );

        message.setText(
            "Xin chào,\n\n" +
            "Cảm ơn bạn đã sử dụng MarcusStore.\n\n" +
            "Mã xác thực (OTP) của bạn là: " + otp + "\n\n" +
            "Mã OTP này có hiệu lực trong vòng 5 phút.\n" +
            "Vui lòng không chia sẻ mã này với bất kỳ ai để đảm bảo an toàn cho tài khoản của bạn.\n\n" +
            "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n" +
            "Trân trọng,\n" +
            "Đội ngũ MarcusStore"
        );

        mailSender.send(message);
    }
}