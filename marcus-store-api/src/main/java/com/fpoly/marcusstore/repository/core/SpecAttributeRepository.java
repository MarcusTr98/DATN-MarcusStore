package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.SpecAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecAttributeRepository extends JpaRepository<SpecAttribute, Integer> {

    List<SpecAttribute> findByCategoryCategoryIdOrderByDisplayOrderAsc(Integer categoryId);

    boolean existsByCategoryCategoryIdAndName(Integer categoryId, String name);
}