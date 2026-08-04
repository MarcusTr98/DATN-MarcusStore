package com.fpoly.marcusstore.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fpoly.marcusstore.entity.auth.EmployeePosition;

@Repository
public interface EmployeePositionRepository
        extends JpaRepository<EmployeePosition, Integer> {

    boolean existsByPositionName(String positionName);

}