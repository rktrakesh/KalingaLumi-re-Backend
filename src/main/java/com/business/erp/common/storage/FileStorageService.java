package com.business.erp.common.storage;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;
public interface FileStorageService {
    StoredFile store(MultipartFile file, String namespace, Set<StorageFileType> allowedTypes, long maxBytes);
    Resource load(String storageKey);
    void delete(String storageKey);

    String storeLogo(MultipartFile file);
    Resource loadLogo(String filename);
    void deleteManagedLogo(String publicUrl);
}
