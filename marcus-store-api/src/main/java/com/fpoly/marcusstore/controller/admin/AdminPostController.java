package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.PostRequestDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.PostResponseDTO;
import com.fpoly.marcusstore.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    @Autowired
    private PostService postService;

    // GET /api/admin/posts
    @GetMapping
    public ApiResponse<Page<PostResponseDTO>> getAll(Pageable pageable) {
        return ApiResponse.success(postService.getAll(pageable));
    }

    // GET /api/admin/posts/{id}
    @GetMapping("/{id}")
    public ApiResponse<PostResponseDTO> getOne(@PathVariable Integer id) {
        return ApiResponse.success(postService.getOne(id));
    }

    // POST /api/admin/posts
    @PostMapping
    public ApiResponse<PostResponseDTO> add(@Valid @RequestBody PostRequestDTO req) {
        return ApiResponse.success(postService.add(req));
    }

    // PUT /api/admin/posts/{id}
    @PutMapping("/{id}")
    public ApiResponse<PostResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody PostRequestDTO req) {
        return ApiResponse.success(postService.update(id, req));
    }

    // DELETE /api/admin/posts/{id}
    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Integer id) {
        postService.remove(id);
        return ApiResponse.success("Xóa post thành công");
    }
}