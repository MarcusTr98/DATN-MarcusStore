package com.fpoly.marcusstore.dto.request;

import com.fpoly.marcusstore.entity.shopping.WarrantyAttachment.FileType;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyReason;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateWarrantyRequest {

    @NotNull(message = "Order item ID is required")
    private Integer orderItemId;

    @NotNull(message = "Reason is required")
    private WarrantyReason reason;

    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @Size(min = 2, max = 10, message = "Phải đính kèm ít nhất 1 ảnh và 1 video (tối đa 10 file)")
    private List<String> attachmentUrls;

    /**
     * Validate nghiêm ngặt: phải có ít nhất 1 ảnh VÀ 1 video.
     * Trigger tự động bởi @AssertTrue trong quá trình @Valid ở controller.
     */
    @AssertTrue(message = "Yêu cầu bảo hành phải có ít nhất 1 ảnh và 1 video")
    public boolean isHasImageAndVideo() {
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            return false;
        }
        boolean hasImage = false;
        boolean hasVideo = false;
        for (String url : attachmentUrls) {
            FileType type = detectFromUrl(url);
            if (type == FileType.IMAGE) hasImage = true;
            if (type == FileType.VIDEO) hasVideo = true;
            if (hasImage && hasVideo) return true;
        }
        return false;
    }

    /**
     * Suy luận loại file (ẢNH / VIDEO) dựa trên URL Cloudinary hoặc đuôi file.
     * Ưu tiên nhận diện qua path "/video/upload/" hoặc "/image/upload/" (chuẩn Cloudinary),
     * sau đó fallback qua đuôi file thực tế trong URL.
     */
    public static FileType detectFromUrl(String url) {
        if (url == null) return FileType.IMAGE;
        String lower = url.toLowerCase();

        if (lower.contains("/video/upload/")) {
            return FileType.VIDEO;
        }
        if (lower.contains("/image/upload/")) {
            return FileType.IMAGE;
        }
        if (lower.matches(".*\\.(mp4|mov|webm|mkv|avi|m4v|3gp|ogv)(\\?.*)?$")) {
            return FileType.VIDEO;
        }
        if (lower.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|svg|heic|heif)(\\?.*)?$")) {
            return FileType.IMAGE;
        }
        return FileType.IMAGE;
    }
}