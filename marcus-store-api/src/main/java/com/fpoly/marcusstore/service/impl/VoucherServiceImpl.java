package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

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
    public List<VoucherResponse> getAllVouChers(){
        return voucherRepository.findAll().stream()
                .map(this::toResponse).toList();
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
        if (request.getVoucherCode() == null || request.getVoucherCode().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng nhập mã voucher"
            );
        }

        if (request.getDiscountType() == null ||
                (!"PERCENT".equals(request.getDiscountType()) && !"AMOUNT".equals(request.getDiscountType()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại giảm giá không hợp lệ"
            );
        }

        if (request.getDiscountValue() == null ||
                request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giá trị giảm phải lớn hơn 0"
            );
        }

        if ("PERCENT".equals(request.getDiscountType()) &&
                request.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giảm theo phần trăm không được vượt quá 100%"
            );
        }

        if ("PERCENT".equals(request.getDiscountType()) &&
                (request.getMaxDiscountAmount() == null ||
                        request.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng nhập số tiền giảm tối đa"
            );
        }

        if (request.getMinOrderValue() == null ||
                request.getMinOrderValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn tối thiểu không được âm"
            );
        }

        if (request.getQuantity() == null || request.getQuantity() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng phải lớn hơn hoặc bằng 0"
            );
        }

        if (request.getStartDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn ngày bắt đầu"
            );
        }

        if (request.getEndDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn ngày kết thúc"
            );
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc phải lớn hơn ngày bắt đầu"
            );
        }
    }
}
