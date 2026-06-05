package com.fpoly.marcusstore.service;
import com.fpoly.marcusstore.dto.request.BannerRequestDTO;
import com.fpoly.marcusstore.dto.response.BannerResponseDTO;
import com.fpoly.marcusstore.entity.cms.Banner;
import com.fpoly.marcusstore.entity.cms.BannerPosition;
import com.fpoly.marcusstore.repository.cms.BannerPositionRepository;
import com.fpoly.marcusstore.repository.cms.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BannerService {

@Autowired
private BannerRepository bannerRepository;

    @Autowired
    private BannerPositionRepository positionRepository;
    private BannerResponseDTO toResponse(Banner banner) {
        BannerResponseDTO.BannerResponseDTOBuilder builder = BannerResponseDTO.builder()
                .id(banner.getBannerId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .targetUrl(banner.getTargetUrl())
                .displayOrder(banner.getDisplayOrder())
                .isActive(banner.getIsActive())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate());

        // Gắn thêm thông tin position nếu có
        if (banner.getBannerPosition() != null) {
            builder.positionId(banner.getBannerPosition().getPositionId())
                   .positionCode(banner.getBannerPosition().getPositionCode())
                   .positionDescription(banner.getBannerPosition().getDescription());
        }

        return builder.build();
    }

    // Lấy tất cả banner
    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getAll() {
        return bannerRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    // Lấy chi tiết 1 banner theo ID
    @Transactional(readOnly = true)
    public BannerResponseDTO getOne(Integer id) {
        Banner banner = bannerRepository.findById(id)
        // GlobalExceptionHandler bắt RuntimeException → trả 400 + message
        .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với id: " + id));
        return toResponse(banner);
    }

    // Thêm banner mới
    @Transactional
    public BannerResponseDTO add(BannerRequestDTO req) {
        // Kiểm tra position có tồn tại không
        BannerPosition pos = positionRepository.findById(req.getPositionId())
        .orElseThrow(() -> new RuntimeException("Không tìm thấy position với id: " + req.getPositionId()));

        Banner banner = new Banner();
        banner.setTitle(req.getTitle());
        banner.setImageUrl(req.getImageUrl());
        banner.setTargetUrl(req.getTargetUrl());
        banner.setDisplayOrder(req.getDisplayOrder());
        banner.setIsActive(req.getIsActive());
        banner.setStartDate(req.getStartDate());
        banner.setEndDate(req.getEndDate());
        banner.setBannerPosition(pos);

        return toResponse(bannerRepository.save(banner));
    }

    // Sửa banner theo ID
    @Transactional
    public BannerResponseDTO update(Integer id, BannerRequestDTO req) {
        // Kiểm tra banner tồn tại
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với id: " + id));

        // Kiểm tra position tồn tại
        BannerPosition pos = positionRepository.findById(req.getPositionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy position với id: " + req.getPositionId()));

        banner.setTitle(req.getTitle());
        banner.setImageUrl(req.getImageUrl());
        banner.setTargetUrl(req.getTargetUrl());
        banner.setDisplayOrder(req.getDisplayOrder());
        banner.setIsActive(req.getIsActive());
        banner.setStartDate(req.getStartDate());
        banner.setEndDate(req.getEndDate());
        banner.setBannerPosition(pos);

        return toResponse(bannerRepository.save(banner));
    }

    // Xóa mềm: chỉ set isActive = false, không xóa khỏi DB
    @Transactional
    public void remove(Integer id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với id: " + id));
        banner.setIsActive(false);
        bannerRepository.save(banner);
    }
}