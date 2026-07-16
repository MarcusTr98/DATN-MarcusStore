package com.fpoly.marcusstore.entity.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Spec_Attributes")
@Getter
@Setter
public class SpecAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spec_attribute_id")
    private Integer specAttributeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType = "text";

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore
    private Category category;

    @OneToMany(mappedBy = "specAttribute", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ProductSpecValue> productSpecValues = new ArrayList<>();
}
