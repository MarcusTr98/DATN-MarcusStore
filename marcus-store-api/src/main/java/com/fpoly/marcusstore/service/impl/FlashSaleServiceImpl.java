package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.response.FlashSaleResponse;
import com.fpoly.marcusstore.entity.promotion.FlashSaleSlot;
import com.fpoly.marcusstore.repository.promotion.FlashSaleSlotRepository;
import com.fpoly.marcusstore.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class FlashSaleServiceImpl implements FlashSaleService {
     private final FlashSaleSlotRepository flashSaleSlotRepository;
     private FlashSaleResponse toResponses(FlashSaleSlot flashSaleSlot){
          Integer quantityFlashSaleSlot = flashSaleSlotRepository.countTotalQuantityBySlotId(flashSaleSlot.getSlotId());
      return FlashSaleResponse.builder()
              .name(flashSaleSlot.getName())
              .startDate(flashSaleSlot.getStartDate())
              .status(flashSaleSlot.getStatus())
              .endDate(flashSaleSlot.getEndDate())
              .quantityFlashSaleSlot(quantityFlashSaleSlot)
              .build();
     }
@Override
     public List<FlashSaleResponse> getAllFlashSale(){
          List<FlashSaleSlot> slots = flashSaleSlotRepository.findAll();
          return slots.stream().map(this::toResponses).collect(Collectors.toList());
     }
}
