package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.AddressRequestDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.AddressResponseDTO;
import com.fpoly.marcusstore.service.UserAddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
public class UserAddressController {

    @Autowired
    private UserAddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponseDTO>>> getMyAddresses() {
        return ResponseEntity.ok(ApiResponse.success(addressService.getMyAddresses()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponseDTO>> addAddress(@Valid @RequestBody AddressRequestDTO req) {
        return ResponseEntity.ok(ApiResponse.success(addressService.addAddress(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAddress(@PathVariable Integer id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa địa chỉ thành công"));
    }
}