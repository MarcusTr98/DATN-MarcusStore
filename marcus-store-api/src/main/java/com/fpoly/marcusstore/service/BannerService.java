package com.fpoly.marcusstore.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fpoly.marcusstore.entity.cms.Banner;
import com.fpoly.marcusstore.entity.cms.BannerPosition;
import com.fpoly.marcusstore.repository.cms.BannerPositionRepository;
import com.fpoly.marcusstore.repository.cms.BannerRepository;


@Service
public class BannerService {

    @Autowired
    BannerRepository repo;

    @Autowired
    BannerPositionRepository positionRepository;

    // GET ALL
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAll() {
        return repo.findAll().stream().map(banner -> {
            Map<String, Object> map = new HashMap<>();

            map.put("id", banner.getBannerId());
            map.put("title", banner.getTitle());
            map.put("imageUrl", banner.getImageUrl());
            map.put("targetUrl", banner.getTargetUrl());
            map.put("displayOrder", banner.getDisplayOrder());
            map.put("isActive", banner.getIsActive());
            map.put("startDate", banner.getStartDate());
            map.put("endDate", banner.getEndDate());

            if (banner.getBannerPosition() != null) {
                map.put("positionId", banner.getBannerPosition().getPositionId());
                map.put("positionCode", banner.getBannerPosition().getPositionCode());
                map.put("description", banner.getBannerPosition().getDescription());
            }

            return map;
        }).collect(Collectors.toList());
    }

    // GET ONE
    public Optional<Map<String, Object>> getOne(Integer id) {
        return repo.findById(id).map(banner -> {
            Map<String, Object> map = new HashMap<>();

            map.put("id", banner.getBannerId());
            map.put("title", banner.getTitle());
            map.put("imageUrl", banner.getImageUrl());
            map.put("targetUrl", banner.getTargetUrl());
            map.put("displayOrder", banner.getDisplayOrder());
            map.put("isActive", banner.getIsActive());
            map.put("startDate", banner.getStartDate());
            map.put("endDate", banner.getEndDate());

            if (banner.getBannerPosition() != null) {
                map.put("positionId", banner.getBannerPosition().getPositionId());
                map.put("positionCode", banner.getBannerPosition().getPositionCode());
                map.put("description", banner.getBannerPosition().getDescription());
            }

            return map;
        });
    }

    // thêm banner
    public Banner add(Map<String, Object> req) {

        Banner banner = new Banner();

        banner.setTitle((String) req.get("title"));
        banner.setImageUrl((String) req.get("imageUrl"));
        banner.setTargetUrl((String) req.get("targetUrl"));
        banner.setDisplayOrder(req.get("displayOrder") != null ? (Integer) req.get("displayOrder") : 0);
        banner.setIsActive(req.get("isActive") != null ? (Boolean) req.get("isActive") : true);

        Integer positionId = (Integer) req.get("positionId");

        BannerPosition pos = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy position"));

        banner.setBannerPosition(pos);

        return repo.save(banner);
    }

    // sửa banner
    public Banner update(Integer id, Map<String, Object> req) {

        Banner banner = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner"));

        banner.setTitle((String) req.get("title"));
        banner.setImageUrl((String) req.get("imageUrl"));
        banner.setTargetUrl((String) req.get("targetUrl"));
        banner.setDisplayOrder((Integer) req.get("displayOrder"));
        banner.setIsActive((Boolean) req.get("isActive"));

        Integer positionId = (Integer) req.get("positionId");

        BannerPosition pos = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy position"));

        banner.setBannerPosition(pos);

        return repo.save(banner);
    }

    // xóa mềm
    public void remove(Integer id) {
        Banner banner = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner"));

        banner.setIsActive(false);
        repo.save(banner);
    }
}