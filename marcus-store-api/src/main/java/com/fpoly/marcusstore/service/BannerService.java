package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.BannerRequestDTO;
import com.fpoly.marcusstore.dto.response.BannerResponseDTO;
import com.fpoly.marcusstore.entity.cms.Banner;
import com.fpoly.marcusstore.entity.cms.BannerPosition;
import com.fpoly.marcusstore.repository.cms.BannerPositionRepository;
import com.fpoly.marcusstore.repository.cms.BannerRepository;
import com.fpoly.marcusstore.utils.BannerPositionRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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

        if (banner.getBannerPosition() != null) {
            builder.positionId(banner.getBannerPosition().getPositionId())
                   .positionCode(banner.getBannerPosition().getPositionCode())
                   .positionDescription(banner.getBannerPosition().getDescription());
        }

        return builder.build();
    }

    // Lấy tất cả banner (Admin)
    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getAll() {
        return bannerRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Lấy chi tiết 1 banner (Admin)
    @Transactional(readOnly = true)
    public BannerResponseDTO getOne(Integer id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với id: " + id));
        return toResponse(banner);
    }

    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getPublicBanners(String positionCode) {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository.findAll().stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .filter(b -> b.getStartDate() == null || !b.getStartDate().isAfter(now))
                .filter(b -> b.getEndDate() == null || !b.getEndDate().isBefore(now))
                .filter(b -> positionCode == null || positionCode.isBlank()
                        || (b.getBannerPosition() != null
                            && positionCode.equalsIgnoreCase(b.getBannerPosition().getPositionCode())))
                .sorted(Comparator.comparing(Banner::getDisplayOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Thêm banner mới
    @Transactional
    public BannerResponseDTO add(BannerRequestDTO req) {
        BannerPosition pos = positionRepository.findById(req.getPositionId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy position với id: " + req.getPositionId()));

        // Validate trùng thứ tự — chỉ áp dụng cho vị trí cho phép sắp thứ tự (HOME_SLIDER)
        if (BannerPositionRules.allowsOrder(pos.getPositionCode())
                && req.getDisplayOrder() != null
                && bannerRepository.existsByPositionIdAndDisplayOrder(
                        pos.getPositionId(), req.getDisplayOrder())) {
            throw new RuntimeException(
                    "Thứ tự " + req.getDisplayOrder()
                    + " đã được dùng bởi banner khác trong cùng vị trí này. "
                    + "Vui lòng chọn thứ tự khác.");
        }

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
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy banner với id: " + id));

        BannerPosition pos = positionRepository.findById(req.getPositionId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy position với id: " + req.getPositionId()));

        // Chỉ validate trùng thứ tự khi displayOrder THỰC SỰ thay đổi so với giá trị hiện tại
        boolean orderChanged = !Objects.equals(banner.getDisplayOrder(), req.getDisplayOrder());

        if (orderChanged
                && BannerPositionRules.allowsOrder(pos.getPositionCode())
                && req.getDisplayOrder() != null
                && bannerRepository.existsByPositionIdAndDisplayOrderExcluding(
                        pos.getPositionId(), req.getDisplayOrder(), id)) {
            throw new RuntimeException(
                    "Thứ tự " + req.getDisplayOrder()
                    + " đã được dùng bởi banner khác trong cùng vị trí này. "
                    + "Vui lòng chọn thứ tự khác.");
        }

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
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy banner với id: " + id));
        banner.setIsActive(false);
        bannerRepository.save(banner);
    }
}