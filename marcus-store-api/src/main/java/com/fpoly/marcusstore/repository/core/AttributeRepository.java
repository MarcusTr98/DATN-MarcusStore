package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Integer> {
    boolean existsByAttributeName(String attributeName);
}