package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.AddressRequestDTO;
import com.fpoly.marcusstore.dto.response.AddressResponseDTO;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.shopping.UserAddress;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.UserAddressRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserAddressService {

    @Autowired
    private UserAddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    private AddressResponseDTO toResponse(UserAddress entity) {
        return AddressResponseDTO.builder()
                .id(entity.getAddressId())
                .recipientName(entity.getRecipientName())
                .phone(entity.getPhoneNumber())
                .provinceName(entity.getProvinceName())
                .districtName(entity.getDistrictName())
                .wardName(entity.getWardName())
                .detail(entity.getDetailAddress())
                .isDefault(entity.getIsDefault())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AddressResponseDTO> getMyAddresses() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return addressRepository.findByUserIdOrderByIsDefaultDesc(userId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AddressResponseDTO addAddress(AddressRequestDTO req) {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Nếu là địa chỉ đầu tiên, tự động ép thành mặc định luôn
        List<UserAddress> existing = addressRepository.findByUserIdOrderByIsDefaultDesc(userId);
        if (existing.isEmpty()) {
            req.setIsDefault(true);
        }

        // user tick chọn "Đặt làm mặc định", phải reset các địa chỉ cũ
        if (req.getIsDefault()) {
            addressRepository.resetDefaultAddressForUser(userId);
        }

        UserAddress address = new UserAddress();
        address.setUser(user);
        address.setRecipientName(req.getRecipientName());
        address.setPhoneNumber(req.getPhoneNumber());
        address.setProvinceName(req.getProvinceName());
        address.setDistrictName(req.getDistrictName());
        address.setWardName(req.getWardName());
        address.setDetailAddress(req.getDetailAddress());
        address.setNote(req.getNote());
        address.setIsDefault(req.getIsDefault());

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(Integer addressId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        UserAddress address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hoặc bạn không có quyền xóa"));

        if (address.getIsDefault()) {
            throw new RuntimeException("Không thể xóa địa chỉ mặc định. Vui lòng set mặc định cho địa chỉ khác trước.");
        }

        addressRepository.delete(address);
    }
}