package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.AddressRequestDTO;
import com.fpoly.marcusstore.dto.response.AddressResponseDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponseDTO>>> getMyAddresses() {
        return ResponseEntity.ok(new ApiResponse<>(200, "Thành công", addressService.getMyAddresses()));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponseDTO>> getAddressById(@PathVariable Integer addressId) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Thành công", addressService.getAddressById(addressId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponseDTO>> addAddress(@Valid @RequestBody AddressRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(200, "Thêm địa chỉ thành công", addressService.addAddress(request)));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponseDTO>> updateAddress(
            @PathVariable Integer addressId,
            @Valid @RequestBody AddressRequestDTO request) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Cập nhật địa chỉ thành công",
                addressService.updateAddress(addressId, request)));
    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponseDTO>> setAsDefault(@PathVariable Integer addressId) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Đã thiết lập làm địa chỉ mặc định",
                addressService.setAddressAsDefault(addressId)));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Integer addressId) {
        addressService.deleteAddress(addressId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Xóa địa chỉ thành công", null));
    }
}