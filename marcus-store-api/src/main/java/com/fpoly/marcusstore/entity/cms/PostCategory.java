package com.fpoly.marcusstore.entity.cms;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "Post_Categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_category_id")
    private Integer postCategoryId;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    private Boolean status = true;

    @OneToMany(mappedBy = "postCategory")
    private List<Post> posts;
}