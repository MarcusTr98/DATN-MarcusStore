package com.fpoly.marcusstore.entity.core;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Product_Spec_Values")
@Getter
@Setter
public class ProductSpecValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "value_text", nullable = false, length = 255)
    private String valueText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spec_attribute_id", nullable = false)
    @JsonBackReference
    private SpecAttribute specAttribute;
}