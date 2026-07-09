package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.PostRequestDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.PostResponseDTO;
import com.fpoly.marcusstore.service.PostService;
import com.fpoly.marcusstore.service.CloudinaryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasAuthority('POST_VIEW')")
public class AdminPostController {

    @Autowired
    private PostService postService;

    @Autowired
    private CloudinaryService cloudinaryService;

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

   
    @GetMapping("/check-slug")
    public ApiResponse<Map<String, Boolean>> checkSlug(
            @RequestParam String slug,
            @RequestParam(required = false) Integer excludeId) {
        boolean exists = postService.checkSlugExists(slug, excludeId);
        return ApiResponse.success(Map.of("exists", exists));
    }

        // POST /api/admin/posts/upload-image
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('POST_CREATE')")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadImage(file);
            return ApiResponse.success(Map.of("url", url));
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh thất bại: " + e.getMessage());
        }
    }


     // POST /api/admin/posts
    @PostMapping
    @PreAuthorize("hasAuthority('POST_CREATE')")
    public ApiResponse<PostResponseDTO> add(@Valid @RequestBody PostRequestDTO req) {
        return ApiResponse.success(postService.add(req));
    }

    // PUT /api/admin/posts/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('POST_UPDATE')")
    public ApiResponse<PostResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody PostRequestDTO req) {
        return ApiResponse.success(postService.update(id, req));
    }

    // DELETE /api/admin/posts/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('POST_DELETE')")
    public ApiResponse<Void> remove(@PathVariable Integer id) {
        postService.remove(id);
        return ApiResponse.success("Xóa post thành công");
    }
}