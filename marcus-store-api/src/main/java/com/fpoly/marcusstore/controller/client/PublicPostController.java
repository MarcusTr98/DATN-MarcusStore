package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.PostResponseDTO;
import com.fpoly.marcusstore.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

//dành cho guest kh cần đăng nhập
@RestController
@RequestMapping("/api/public/posts")
public class PublicPostController {

    @Autowired
    private PostService postService;

    // GET /api/posts
    @GetMapping
    public ApiResponse<Page<PostResponseDTO>> getAll(Pageable pageable) {
        return ApiResponse.success(postService.getPublished(pageable));
    }

    // GET /api/posts/{slug}
    @GetMapping("/{slug}")
    public ApiResponse<PostResponseDTO> getBySlug(@PathVariable String slug) {
        return ApiResponse.success(postService.getPublishedBySlug(slug));
    }
}