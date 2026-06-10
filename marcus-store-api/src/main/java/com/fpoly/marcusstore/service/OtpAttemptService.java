package com.fpoly.marcusstore.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtpAttemptService {

    private final EntityManager entityManager;


    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public int incrementAttempt(Integer otpId) {
        entityManager.createNativeQuery(
            "UPDATE EmailOtps SET attempt_count = attempt_count + 1 WHERE otp_id = :id"
        ).setParameter("id", otpId).executeUpdate();

        // Đọc lại giá trị mới từ DB
        Object result = entityManager.createNativeQuery(
            "SELECT attempt_count FROM EmailOtps WHERE otp_id = :id"
        ).setParameter("id", otpId).getSingleResult();

        return ((Number) result).intValue();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void deleteOtp(Integer otpId) {
        entityManager.createNativeQuery(
            "DELETE FROM EmailOtps WHERE otp_id = :id"
        ).setParameter("id", otpId).executeUpdate();
    }
}