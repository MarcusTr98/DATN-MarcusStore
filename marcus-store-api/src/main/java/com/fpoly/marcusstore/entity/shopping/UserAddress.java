package com.fpoly.marcusstore.entity.shopping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fpoly.marcusstore.entity.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "User_Addresses")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Integer addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "phone_number", nullable = false, length = 10)
    private String phoneNumber;

    @Column(name = "province_name", nullable = false, length = 100)
    private String provinceName;

    @Column(name = "district_name", nullable = false, length = 100)
    private String districtName;

    @Column(name = "ward_name", nullable = false, length = 100)
    private String wardName;

    @Column(name = "detail_address", nullable = false, length = 300)
    private String detailAddress;

    @Column(name = "note", length = 300)
    private String note;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}