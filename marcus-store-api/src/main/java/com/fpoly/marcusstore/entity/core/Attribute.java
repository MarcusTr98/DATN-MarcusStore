package com.fpoly.marcusstore.entity.core;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "Attributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Attribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attribute_id")
    private Integer attributeId;

    @Column(name = "attribute_name", nullable = false, unique = true, length = 50)
    private String attributeName;

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL)
    private List<AttributeValue> values;
}