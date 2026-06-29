package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.ShippingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingConfigRepository extends JpaRepository<ShippingConfig, Integer> {
    Optional<ShippingConfig> findFirstByIsActiveTrueOrderByCreatedAtDesc();
}