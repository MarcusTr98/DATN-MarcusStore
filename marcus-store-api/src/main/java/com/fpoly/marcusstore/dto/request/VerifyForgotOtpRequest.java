package com.fpoly.marcusstore.dto.request;
import lombok.Data;
 @Data
public class VerifyForgotOtpRequest {
    private String email;

    private String otp;
}
