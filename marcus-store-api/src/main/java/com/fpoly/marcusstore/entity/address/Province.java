package com.fpoly.marcusstore.entity.address;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Provinces")
@Getter
@Setter
public class Province {

    @Id
    @Column(name = "province_code", length = 20)
    private String provinceCode; // Ví dụ: '01' cho Hà Nội

    @Column(name = "province_name", nullable = false, length = 100)
    private String provinceName;

    // TỐI ƯU 4: Vì đây là Master Data (ít khi gọi ngược từ Tỉnh ra danh sách
    // Huyện), @JsonIgnore để ngắt hoàn toàn đệ quy không cần thiết
    @OneToMany(mappedBy = "province")
    @JsonIgnore
    private List<District> districts = new ArrayList<>();
}