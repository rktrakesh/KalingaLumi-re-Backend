package com.business.erp.common.storage;

import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Map<String, ImageFormat> EXTENSIONS = Map.of(
            "png", ImageFormat.PNG, "jpg", ImageFormat.JPEG,
            "jpeg", ImageFormat.JPEG, "webp", ImageFormat.WEBP);

    private final Path root;
    private final Path brandingRoot;
    private final long maxLogoBytes;

    public LocalFileStorageService(
            @Value("${app.storage.path:uploads}") String storagePath,
            @Value("${app.storage.max-logo-bytes:2097152}") long maxLogoBytes) {
        this.root = Path.of(storagePath).toAbsolutePath().normalize();
        this.brandingRoot = root.resolve("branding").normalize();
        if (!brandingRoot.startsWith(root)) {
            throw new IllegalStateException("Configured logo storage path is outside app.storage.path");
        }
        if (maxLogoBytes <= 0) {
            throw new IllegalStateException("app.storage.max-logo-bytes must be greater than zero");
        }
        this.maxLogoBytes = maxLogoBytes;
    }

    @Override
    public String storeLogo(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BusinessException("A company logo file is required");
            }
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > maxLogoBytes) {
                throw new BusinessException("Logo must be a PNG, JPEG, or WebP image up to " + maxLogoBytes + " bytes");
            }

            String extension = extensionOf(safeBasename(file.getOriginalFilename()));
            ImageFormat extensionFormat = EXTENSIONS.get(extension);
            ImageFormat declaredFormat = ImageFormat.fromMimeType(file.getContentType());
            ImageFormat signatureFormat = ImageFormat.fromSignature(bytes);
            if (extensionFormat == null || declaredFormat == null || signatureFormat == null
                    || declaredFormat != extensionFormat || signatureFormat != extensionFormat) {
                throw new BusinessException("Logo extension, MIME type, and image bytes must describe the same image format");
            }
            if ((signatureFormat == ImageFormat.PNG || signatureFormat == ImageFormat.JPEG)
                    && ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
                throw new BusinessException("Logo image data is invalid");
            }

            Files.createDirectories(brandingRoot);
            String storedExtension = extensionFormat == ImageFormat.JPEG ? "jpg" : extension;
            Path target = brandingRoot.resolve(UUID.randomUUID() + "." + storedExtension).normalize();
            if (!target.startsWith(brandingRoot)) {
                throw new BusinessException("Invalid logo storage path");
            }
            Files.write(target, bytes);
            return "/api/v1/public/branding/company-logo/" + target.getFileName();
        } catch (IOException ex) {
            throw new BusinessException("Unable to store company logo");
        }
    }

    @Override
    public Resource loadLogo(String filename) {
        if (!isManagedFilename(filename)) {
            throw new BusinessException("Invalid logo filename");
        }
        try {
            Path file = brandingRoot.resolve(filename).normalize();
            if (!file.startsWith(brandingRoot) || !Files.isRegularFile(file)) {
                throw new ResourceNotFoundException("Company logo not found");
            }
            return new UrlResource(file.toUri());
        } catch (java.net.MalformedURLException ex) {
            throw new ResourceNotFoundException("Company logo not found");
        }
    }

    @Override
    public void deleteManagedLogo(String publicUrl) {
        if (publicUrl == null || !isManagedLogoUrl(publicUrl)) {
            return;
        }
        String filename = publicUrl.substring(publicUrl.lastIndexOf('/') + 1);
        if (!isManagedFilename(filename)) {
            return;
        }
        try {
            Path target = brandingRoot.resolve(filename).normalize();
            if (!target.startsWith(brandingRoot)) {
                throw new IOException("Logo cleanup path escaped the branding storage root");
            }
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Unable to delete managed logo {}", filename, ex);
        }
    }

    private String safeBasename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("Logo filename is required");
        }
        String normalizedFilename = originalFilename.replace('\\', '/');
        if (normalizedFilename.startsWith("/") || normalizedFilename.contains("../")) {
            throw new BusinessException("Logo filename is unsafe");
        }
        String basename = normalizedFilename.substring(normalizedFilename.lastIndexOf('/') + 1);
        if (basename.isBlank() || basename.equals(".") || basename.equals("..")
                || basename.indexOf('\u0000') >= 0 || basename.contains("..")
                || !basename.matches("[A-Za-z0-9][A-Za-z0-9._ -]*")) {
            throw new BusinessException("Logo filename is unsafe");
        }
        return basename;
    }

    private String extensionOf(String filename) {
        int separator = filename.lastIndexOf('.');
        return separator <= 0 || separator == filename.length() - 1
                ? "" : filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isManagedLogoUrl(String publicUrl) {
        return publicUrl.startsWith("/api/v1/public/branding/company-logo/")
                || publicUrl.startsWith("/api/v1/public/company-logo/");
    }

    private boolean isManagedFilename(String filename) {
        return filename != null && filename.matches("[0-9a-fA-F-]{36}\\.(png|jpg|webp)");
    }

    private enum ImageFormat {
        PNG("image/png"), JPEG("image/jpeg"), WEBP("image/webp");
        private final String mimeType;
        ImageFormat(String mimeType) { this.mimeType = mimeType; }

        private static ImageFormat fromMimeType(String mimeType) {
            if (mimeType == null) return null;
            String normalized = mimeType.toLowerCase(Locale.ROOT).trim();
            for (ImageFormat format : values()) if (format.mimeType.equals(normalized)) return format;
            return null;
        }

        private static ImageFormat fromSignature(byte[] bytes) {
            if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E
                    && bytes[3] == 0x47 && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) return PNG;
            if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return JPEG;
            if (bytes.length >= 12 && "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
                    && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) return WEBP;
            return null;
        }
    }
}
