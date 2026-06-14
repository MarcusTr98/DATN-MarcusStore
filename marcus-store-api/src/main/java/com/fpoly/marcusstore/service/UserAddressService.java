package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.AddressRequestDTO;
import com.fpoly.marcusstore.dto.response.AddressResponseDTO;

import java.util.List;

public interface UserAddressService {

    // Lấy danh sách địa chỉ của User đang đăng nhập
    List<AddressResponseDTO> getMyAddresses();

    // Lấy chi tiết 1 địa chỉ
    AddressResponseDTO getAddressById(Integer addressId);

    // Thêm địa chỉ mới
    AddressResponseDTO addAddress(AddressRequestDTO request);

    // Cập nhật địa chỉ
    AddressResponseDTO updateAddress(Integer addressId, AddressRequestDTO request);

    // Xóa địa chỉ
    void deleteAddress(Integer addressId);

    // Set một địa chỉ làm mặc định
    AddressResponseDTO setAddressAsDefault(Integer addressId);
}