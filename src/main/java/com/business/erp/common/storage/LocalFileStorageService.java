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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path root;
    private final long maxLogoBytes;

    public LocalFileStorageService(
            @Value("${app.storage.path:uploads}") String storagePath,
            @Value("${app.storage.max-logo-bytes:2097152}") long maxLogoBytes) {
        this.root = Path.of(storagePath).toAbsolutePath().normalize();
        Path brandingRoot = root.resolve("branding").normalize();
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
        StoredFile stored = store(file, "branding",
                Set.of(StorageFileType.PNG, StorageFileType.JPEG, StorageFileType.WEBP), maxLogoBytes);
        return "/api/v1/public/branding/company-logo/" + Path.of(stored.storageKey()).getFileName();
    }

    @Override
    public StoredFile store(MultipartFile file, String namespace,
                            Set<StorageFileType> allowedTypes, long maxBytes) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BusinessException("A file is required");
            }
            byte[] bytes = file.getBytes();
            if (maxBytes <= 0) {
                throw new IllegalStateException("Configured maximum file size must be greater than zero");
            }
            if (bytes.length == 0 || bytes.length > maxBytes) {
                throw new BusinessException("File exceeds the maximum allowed size of " + maxBytes + " bytes");
            }

            String originalFilename = safeBasename(file.getOriginalFilename());
            String extension = extensionOf(originalFilename);
            StorageFileType extensionType = StorageFileType.fromExtension(extension);
            StorageFileType declaredType = StorageFileType.fromMimeType(file.getContentType());
            StorageFileType signatureType = StorageFileType.fromSignature(bytes);
            if (extensionType == null || declaredType == null || signatureType == null
                    || extensionType != declaredType || extensionType != signatureType
                    || allowedTypes == null || !allowedTypes.contains(extensionType)) {
                throw new BusinessException("File extension, MIME type, and file bytes must describe the same allowed format");
            }
            if ((signatureType == StorageFileType.PNG || signatureType == StorageFileType.JPEG)
                    && ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
                throw new BusinessException("Image data is invalid");
            }

            Path namespaceRoot = resolveNamespace(namespace);
            Files.createDirectories(namespaceRoot);
            Path target = namespaceRoot.resolve(UUID.randomUUID() + "." + extensionType.canonicalExtension()).normalize();
            if (!target.startsWith(namespaceRoot) || !target.startsWith(root)) {
                throw new BusinessException("Invalid storage path");
            }
            Files.write(target, bytes);
            String storageKey = root.relativize(target).toString().replace('\\', '/');
            return new StoredFile(storageKey, originalFilename, extensionType.mimeType(), bytes.length);
        } catch (IOException ex) {
            throw new BusinessException("Unable to store file");
        }
    }

    @Override
    public Resource loadLogo(String filename) {
        if (!isManagedFilename(filename)) {
            throw new BusinessException("Invalid logo filename");
        }
        return load("branding/" + filename);
    }

    @Override
    public Resource load(String storageKey) {
        try {
            Path file = resolveStorageKey(storageKey);
            if (!Files.isRegularFile(file)) {
                throw new ResourceNotFoundException("Stored file not found");
            }
            return new UrlResource(file.toUri());
        } catch (java.net.MalformedURLException ex) {
            throw new ResourceNotFoundException("Stored file not found");
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveStorageKey(storageKey));
        } catch (IOException | RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Unable to delete managed file {}", storageKey, ex);
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
            delete("branding/" + filename);
        } catch (RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Unable to delete managed logo {}", filename, ex);
        }
    }

    private String safeBasename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("File name is required");
        }
        String normalizedFilename = originalFilename.replace('\\', '/');
        if (normalizedFilename.startsWith("/") || normalizedFilename.contains("../")) {
            throw new BusinessException("File name is unsafe");
        }
        String basename = normalizedFilename.substring(normalizedFilename.lastIndexOf('/') + 1);
        if (basename.isBlank() || basename.equals(".") || basename.equals("..")
                || basename.indexOf('\u0000') >= 0 || basename.contains("..")
                || !basename.matches("[A-Za-z0-9][A-Za-z0-9._ -]*")) {
            throw new BusinessException("File name is unsafe");
        }
        return basename;
    }

    private String extensionOf(String filename) {
        int separator = filename.lastIndexOf('.');
        return separator <= 0 || separator == filename.length() - 1
                ? "" : filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private Path resolveNamespace(String namespace) {
        if (namespace == null || namespace.isBlank() || namespace.contains("..")
                || namespace.startsWith("/") || namespace.startsWith("\\")
                || !namespace.matches("[A-Za-z0-9][A-Za-z0-9/_-]*")) {
            throw new BusinessException("Storage namespace is unsafe");
        }
        Path resolved = root.resolve(namespace.replace('/', java.io.File.separatorChar)).normalize();
        if (!resolved.startsWith(root)) throw new BusinessException("Storage namespace is unsafe");
        return resolved;
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("..")
                || storageKey.startsWith("/") || storageKey.startsWith("\\")
                || !storageKey.matches("[A-Za-z0-9][A-Za-z0-9/_.-]*")) {
            throw new BusinessException("Storage key is unsafe");
        }
        Path resolved = root.resolve(storageKey.replace('/', java.io.File.separatorChar)).normalize();
        if (!resolved.startsWith(root)) throw new BusinessException("Storage key is unsafe");
        return resolved;
    }

    private boolean isManagedLogoUrl(String publicUrl) {
        return publicUrl.startsWith("/api/v1/public/branding/company-logo/")
                || publicUrl.startsWith("/api/v1/public/company-logo/");
    }

    private boolean isManagedFilename(String filename) {
        return filename != null && filename.matches("[0-9a-fA-F-]{36}\\.(png|jpg|webp)");
    }

}
