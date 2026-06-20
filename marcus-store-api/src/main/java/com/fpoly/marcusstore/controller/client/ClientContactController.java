package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.CreateContactRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.impl.ContactServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/contact")
@RequiredArgsConstructor
public class ClientContactController {

    private final ContactServiceImpl contactService;

    @PostMapping
    public ApiResponse<String> submitContactForm(@Valid @RequestBody CreateContactRequest request) {
        System.out.println("Đã nhận được request tại ClientContactController!"); // log lỗi
        contactService.submitContact(request);
        return ApiResponse.success("Gửi yêu cầu thành công");
    }
}