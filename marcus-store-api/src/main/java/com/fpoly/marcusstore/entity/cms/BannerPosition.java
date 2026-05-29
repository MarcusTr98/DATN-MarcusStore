package com.fpoly.marcusstore.entity.cms;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "Banner_Positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BannerPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Integer positionId;

    @Column(name = "position_code", nullable = false, unique = true, length = 50)
    private String positionCode;

    @Column(length = 255)
    private String description;

    @OneToMany(mappedBy = "bannerPosition", cascade = CascadeType.ALL)
    private List<Banner> banners;
}