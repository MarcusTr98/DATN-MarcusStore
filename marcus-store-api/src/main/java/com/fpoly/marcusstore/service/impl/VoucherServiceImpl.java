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
    if(voucherRepository.existsByVoucherCode(voucherCode)){
        throw  new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Mã đã tồn tại"
        );

    }
        Voucher voucher = new Voucher();
        voucher.setVoucherCode(voucherCode);
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setMaxDiscountAmount(
                "AMOUNT".equals(voucher.getDiscountType()) ?
                        null : voucher.getMaxDiscountAmount());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setIsActive(
                request.getQuantity() > 0 && Boolean.TRUE.equals(request.getIsActive())
        );
        Voucher saveVoucher = voucherRepository.save(voucher);
        return toResponse(saveVoucher);
    }
     private void validateVoucherRequest(AddVoucherRequest request){
        if(request.getVoucherCode() == null || request.getVoucherCode().trim().isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Vui lòng nhập mã giảm giá"
            );
        }
         if (request.getDiscountType() == null ||
                 (!"PERCENT".equals(request.getDiscountType()) && !"AMOUNT".equals(request.getDiscountType()))) {
             throw new ResponseStatusException(
                     HttpStatus.BAD_REQUEST,
                     "Loại giảm giá không hợp lệ"
             );
         }
         if(request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO)<=0){
             throw new ResponseStatusException(
                     HttpStatus.BAD_REQUEST, "giá trị giảm phải lớn hơn 0"
             );
         }
     }
}
