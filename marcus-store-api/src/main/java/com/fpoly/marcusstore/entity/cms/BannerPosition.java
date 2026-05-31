package com.fpoly.marcusstore.entity.cms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Banner_Positions")
@Getter
@Setter
public class BannerPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Integer positionId;

    @Column(name = "position_code", nullable = false, unique = true, length = 50)
    private String positionCode;

    @Column(name = "description", length = 255)
    private String description;

    @OneToMany(mappedBy = "bannerPosition", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Banner> banners = new ArrayList<>();
}