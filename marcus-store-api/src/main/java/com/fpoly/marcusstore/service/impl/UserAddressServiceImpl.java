package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.AddressRequestDTO;
import com.fpoly.marcusstore.dto.response.AddressResponseDTO;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.UserAddress;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.UserAddressRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
    }

    private AddressResponseDTO toResponse(UserAddress address) {
        return AddressResponseDTO.builder()
                .addressId(address.getAddressId())
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .provinceId(address.getProvinceId())
                .provinceName(address.getProvinceName())
                .districtId(address.getDistrictId())
                .districtName(address.getDistrictName())
                .wardCode(address.getWardCode())
                .wardName(address.getWardName())
                .detailAddress(address.getDetailAddress())
                .note(address.getNote())
                .isDefault(address.getIsDefault())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponseDTO> getMyAddresses() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return addressRepository.findByUser_UserIdOrderByIsDefaultDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponseDTO getAddressById(Integer addressId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        UserAddress address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hợp lệ"));
        return toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponseDTO addAddress(AddressRequestDTO request) {
        User user = getCurrentUser();
        UserAddress address = new UserAddress();

        long currentCount = addressRepository.countByUser_UserId(user.getUserId());

        // Nếu đây là địa chỉ ĐẦU TIÊN, bắt buộc phải là Mặc định
        boolean isDefault = (currentCount == 0) || Boolean.TRUE.equals(request.getIsDefault());

        if (isDefault && currentCount > 0) {
            addressRepository.clearDefaultAddress(user.getUserId());
        }

        address.setUser(user);
        address.setRecipientName(request.getRecipientName());
        address.setPhoneNumber(request.getPhoneNumber());

        // GHN Master Data
        address.setProvinceId(request.getProvinceId());
        address.setProvinceName(request.getProvinceName());
        address.setDistrictId(request.getDistrictId());
        address.setDistrictName(request.getDistrictName());
        address.setWardCode(request.getWardCode());
        address.setWardName(request.getWardName());

        address.setDetailAddress(request.getDetailAddress());
        address.setNote(request.getNote());
        address.setIsDefault(isDefault);
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponseDTO updateAddress(Integer addressId, AddressRequestDTO request) {
        User user = getCurrentUser();

        UserAddress address = addressRepository.findByAddressIdAndUser_UserId(addressId, user.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hợp lệ để sửa"));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            addressRepository.clearDefaultAddress(user.getUserId());
            address.setIsDefault(true);
        } else if (addressRepository.countByUser_UserId(user.getUserId()) == 1) {
            address.setIsDefault(true);
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhoneNumber(request.getPhoneNumber());

        // GHN Master Data
        address.setProvinceId(request.getProvinceId());
        address.setProvinceName(request.getProvinceName());
        address.setDistrictId(request.getDistrictId());
        address.setDistrictName(request.getDistrictName());
        address.setWardCode(request.getWardCode());
        address.setWardName(request.getWardName());

        address.setDetailAddress(request.getDetailAddress());
        address.setNote(request.getNote());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Integer addressId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        UserAddress address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hợp lệ để xóa"));

        if (address.getIsDefault()) {
            throw new RuntimeException(
                    "Không thể xóa địa chỉ Mặc định. Vui lòng thiết lập địa chỉ khác làm mặc định trước.");
        }

        addressRepository.delete(address);
    }

    @Override
    @Transactional
    public AddressResponseDTO setAddressAsDefault(Integer addressId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        UserAddress address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hợp lệ"));

        if (!address.getIsDefault()) {
            addressRepository.clearDefaultAddress(userId);
            address.setIsDefault(true);
            addressRepository.save(address);
        }

        return toResponse(address);
    }
}