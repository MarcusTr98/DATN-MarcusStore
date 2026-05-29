package com.fpoly.marcusstore.entity.core;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Attribute_Values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttributeValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "value_id")
    private Integer valueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    @Column(name = "value_string", nullable = false, length = 100)
    private String valueString;

    @ManyToMany(mappedBy = "attributeValues")
    @JsonIgnore
    private Set<ProductSku> skus = new HashSet<>();
}