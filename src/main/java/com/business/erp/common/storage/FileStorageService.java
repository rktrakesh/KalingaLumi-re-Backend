package com.business.erp.common.storage;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
public interface FileStorageService {
    String storeLogo(MultipartFile file);
    Resource loadLogo(String filename);
    void deleteManagedLogo(String publicUrl);
}
