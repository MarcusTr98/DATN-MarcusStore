package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.SpecAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecAttributeRepository extends JpaRepository<SpecAttribute, Integer> {

        List<SpecAttribute> findByCategoryCategoryIdOrderByDisplayOrderAsc(Integer categoryId);

        boolean existsByCategoryCategoryIdAndName(Integer categoryId, String name);

        boolean existsByCategoryCategoryIdAndNameAndSpecAttributeIdNot(
                        Integer categoryId, String name, Integer specAttributeId);

        @Query("SELECT sa FROM SpecAttribute sa " +
                        "JOIN FETCH sa.category " +
                        "WHERE sa.category.categoryId IN :categoryIds " +
                        "ORDER BY sa.category.categoryId ASC, sa.displayOrder ASC, sa.specAttributeId ASC")
        List<SpecAttribute> findByCategoryIdsWithCategory(@Param("categoryIds") List<Integer> categoryIds);

        @Query("SELECT sa FROM SpecAttribute sa " +
                        "JOIN FETCH sa.category " +
                        "WHERE sa.specAttributeId = :id")
        java.util.Optional<SpecAttribute> findByIdWithCategory(@Param("id") Integer id);
}
