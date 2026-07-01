package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.entity.contact.ContactRequest;
import com.fpoly.marcusstore.repository.contact.ContactRequestRepository;
import com.fpoly.marcusstore.service.impl.ContactServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/contacts")
@PreAuthorize("hasAuthority('CONTACT_VIEW')")
@RequiredArgsConstructor
public class AdminContactController {

    private final ContactServiceImpl contactService;
    private final ContactRequestRepository contactRepo;

    // lấy danh sách yêu cầu liên hệ, sắp xếp mới nhất lên đầu
    @GetMapping
    public ApiResponse<Page<ContactRequest>> getAllContacts(@PageableDefault(page = 0, size = 10) Pageable pageable) {
        Page<ContactRequest> contactPage = contactRepo.findAllByOrderByCreatedAtDesc(pageable);
        return ApiResponse.success(contactPage);
    }

    // Đánh dấu một yêu cầu liên hệ là ĐÃ RESOLVED
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('CONTACT_PROCESS')")
    public ApiResponse<String> resolveContact(@PathVariable("id") Integer id) {
        contactService.resolveContact(id);
        return ApiResponse.success("Đã xử lý xong yêu cầu liên hệ");
    }
}