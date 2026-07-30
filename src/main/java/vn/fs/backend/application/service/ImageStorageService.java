package vn.fs.backend.application.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class ImageStorageService {

    @Value("${upload.path:${user.dir}/upload/images}")
    private String uploadDir;

    public static final String TMP_PREFIX = "__tmp__";

    public boolean isValidUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return true;
        }
        String original = StringUtils.defaultString(file.getOriginalFilename());
        return isAllowed(getExt(original));
    }

    /** Lưu file tạm để không mất khi form lỗi; trả về tên file tạm hoặc null nếu invalid */
    public String saveTemp(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) return null;
            Files.createDirectories(Path.of(uploadDir));
            String original = StringUtils.defaultString(file.getOriginalFilename());
            String ext = getExt(original);
            if (!isAllowed(ext)) return null;

            String newName = TMP_PREFIX + UUID.randomUUID().toString().replace("-", "") + "." + ext;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, Path.of(uploadDir, newName), StandardCopyOption.REPLACE_EXISTING);
            }
            return newName;
        } catch (Exception e) {
            log.error("saveTemp error", e);
            return null;
        }
    }

    /** Nếu là file tạm thì đổi sang tên chính thức, nếu không thì trả nguyên */
    public String promoteIfTempOrKeep(String name, String prefix) {
        try {
            if (StringUtils.isBlank(name)) return null;
            if (!isTemp(name)) return name;
            Files.createDirectories(Path.of(uploadDir));
            String ext = getExt(name);
            String finalName = (StringUtils.defaultString(prefix) + UUID.randomUUID().toString().replace("-", "") + "." + ext);
            Files.move(Path.of(uploadDir, name), Path.of(uploadDir, finalName), StandardCopyOption.REPLACE_EXISTING);
            return finalName;
        } catch (Exception e) {
            log.error("promoteIfTempOrKeep error: {}", name, e);
            return null;
        }
    }

    public void deleteIfPermanent(String name) {
        try {
            if (StringUtils.isBlank(name) || isTemp(name)) return;
            Files.deleteIfExists(Path.of(uploadDir, name));
        } catch (Exception e) {
            log.warn("deleteIfPermanent error: {}", name, e);
        }
    }

    public boolean isTemp(String name) {
        return StringUtils.startsWith(name, TMP_PREFIX);
    }

    private boolean isAllowed(String ext) {
        ext = StringUtils.lowerCase(ext);
        return "jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext) || "webp".equals(ext);
    }
    private String getExt(String filename) {
        int i = StringUtils.lastIndexOf(filename, '.');
        return (i >= 0 && i < filename.length() - 1) ? filename.substring(i + 1).toLowerCase(Locale.ROOT) : "";
    }
}
