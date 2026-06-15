package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.dto.response.VoucherStatsResponse;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {
    private final VoucherRepository voucherRepository;

    private VoucherResponse toResponse(Voucher voucher){
    return VoucherResponse.builder()
            .voucherId(voucher.getVoucherId())
            .voucherCode((voucher.getVoucherCode()))
            .discountValue(voucher.getDiscountValue())
            .discountType(voucher.getDiscountType())
            .maxDiscountAmount((voucher.getMaxDiscountAmount()))
            .minOrderValue(voucher.getMinOrderValue())
            .startDate(voucher.getStartDate())
            .endDate(voucher.getEndDate())
            .quantity(voucher.getQuantity())
            .isActive(voucher.getIsActive())
            .build();
    }
    @Override
    public Page<VoucherResponse> getVouchersPage(String keyword, String discountType, Boolean isActive, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedDiscountType = normalizeDiscountType(discountType);

        return voucherRepository
                .searchVouchers(normalizedKeyword, normalizedDiscountType, isActive, pageable)
                .map(this::toResponse);
    }

    @Override
    public VoucherStatsResponse getVoucherStats(String keyword, String discountType, Boolean isActive) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedDiscountType = normalizeDiscountType(discountType);

        return new VoucherStatsResponse(
                voucherRepository.countVouchers(normalizedKeyword, normalizedDiscountType, isActive),
                voucherRepository.countActiveVouchers(normalizedKeyword, normalizedDiscountType, isActive),
                voucherRepository.countPercentVouchers(normalizedKeyword, normalizedDiscountType, isActive),
                voucherRepository.countAmountVouchers(normalizedKeyword, normalizedDiscountType, isActive)
        );
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
    }

    private String normalizeDiscountType(String discountType) {
        return discountType == null ||
                discountType.isBlank() ||
                "ALL".equalsIgnoreCase(discountType)
                ? null
                : discountType.trim().toUpperCase();
    }

    @Override
    public VoucherResponse getVoucherById(Integer voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với id: " + voucherId));

        return toResponse(voucher);
    }
    @Override
    public void deleteVoucherById(Integer id){
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("không tìm thấy voucher hợp lệ để xóa"+ id) );
        voucherRepository.delete(voucher);
    }
    @Override
    @Transactional
    public VoucherResponse addVoucher(AddVoucherRequest request){
        String voucherCode = request.getVoucherCode().trim().toUpperCase();
        String discountType = request.getDiscountType().trim().toUpperCase();
        validateVoucherRequest(request);
        if (voucherRepository.existsByVoucherCode(voucherCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã voucher đã tồn tại"
            );
        }

        Voucher voucher = new Voucher();

        voucher.setVoucherCode(voucherCode);
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setDiscountType(discountType);

        if ("AMOUNT".equals(discountType)) {
            voucher.setMaxDiscountAmount(null);
        } else {
            voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setQuantity(request.getQuantity());
        voucher.setIsActive(
                request.getQuantity() > 0 && Boolean.TRUE.equals(request.getIsActive())
        );
        Voucher saveVoucher = voucherRepository.save(voucher);
        return toResponse(saveVoucher);
    }
    private void validateVoucherRequest(AddVoucherRequest request) {
        String discountType = request.getDiscountType() == null
                ? null
                : request.getDiscountType().trim().toUpperCase();

        if (discountType == null ||
                (!"PERCENT".equals(discountType) && !"AMOUNT".equals(discountType))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại giảm giá không hợp lệ"
            );
        }

        if ("PERCENT".equals(discountType)) {
            if (request.getMaxDiscountAmount() == null ||
                    request.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Số tiền giảm tối đa phải lớn hơn 0"
                );
            }
        }
        if (request.getDiscountValue() == null ||
                request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giá trị giảm phải lớn hơn 0"
            );
        }
        if ("PERCENT".equals(discountType) &&
                request.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giảm theo phần trăm không được vượt quá 100%"
            );
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc phải lớn hơn ngày bắt đầu"
            );
        }
    }
     @Override
    @Transactional
    public VoucherResponse updateVoucher(Integer voucherId, AddVoucherRequest request){
        Voucher voucher = voucherRepository.findById(voucherId).orElseThrow(
                ()-> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "không tìm thấy voucher"
                ));
         String voucherCode = request.getVoucherCode().trim().toUpperCase();
         String discountType = request.getDiscountType().trim().toUpperCase();
         validateVoucherRequest(request);
         if (
                 voucherRepository.existsByVoucherCode(voucherCode)
                         && !voucher.getVoucherCode().equalsIgnoreCase(voucherCode)
         ) {
             throw new ResponseStatusException(
                     HttpStatus.BAD_REQUEST,
                     "Mã voucher đã tồn tại"
             );
         }
         voucher.setVoucherCode(voucherCode);
         voucher.setDiscountValue(request.getDiscountValue());
         voucher.setDiscountType(discountType);

         if ("AMOUNT".equals(discountType)) {
             voucher.setMaxDiscountAmount(null);
         } else {
             voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
         }
         voucher.setMinOrderValue(request.getMinOrderValue());
         voucher.setStartDate(request.getStartDate());
         voucher.setEndDate(request.getEndDate());
         voucher.setQuantity(request.getQuantity());
         voucher.setIsActive(
                 request.getQuantity() > 0 && Boolean.TRUE.equals(request.getIsActive())
         );
         return toResponse(voucherRepository.save(voucher));
     }
}
