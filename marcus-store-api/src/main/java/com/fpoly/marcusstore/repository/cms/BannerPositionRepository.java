package com.fpoly.marcusstore.repository.cms;

import com.fpoly.marcusstore.entity.cms.BannerPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannerPositionRepository extends JpaRepository<BannerPosition, Integer> {
}