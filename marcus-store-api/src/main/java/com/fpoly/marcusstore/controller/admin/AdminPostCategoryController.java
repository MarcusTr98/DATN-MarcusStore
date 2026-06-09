package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.PostCategoryRequestDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.PostCategoryResponseDTO;
import com.fpoly.marcusstore.service.PostCategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/post-categories")
public class AdminPostCategoryController {

    @Autowired
    private PostCategoryService postCategoryService;

    // GET /api/admin/post-categories
    @GetMapping
    public ApiResponse<List<PostCategoryResponseDTO>> getAll() {
        return ApiResponse.success(postCategoryService.getAll());
    }

    // GET /api/admin/post-categories/{id}
    @GetMapping("/{id}")
    public ApiResponse<PostCategoryResponseDTO> getOne(@PathVariable Integer id) {
        return ApiResponse.success(postCategoryService.getOne(id));
    }

    // POST /api/admin/post-categories
    @PostMapping
    public ApiResponse<PostCategoryResponseDTO> add(
            @Valid @RequestBody PostCategoryRequestDTO req) {
        return ApiResponse.success(postCategoryService.add(req));
    }

    // PUT /api/admin/post-categories/{id}
    @PutMapping("/{id}")
    public ApiResponse<PostCategoryResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody PostCategoryRequestDTO req) {
        return ApiResponse.success(postCategoryService.update(id, req));
    }

    // DELETE /api/admin/post-categories/{id}
    @DeleteMapping("/{id}")
    public ApiResponse<String> remove(@PathVariable Integer id) {
        postCategoryService.remove(id);
        return ApiResponse.success("Xóa danh mục thành công");
    }
}