package com.business.erp.common.storage;

import com.business.erp.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesValidatedPngUnderControlledPublicUrl() throws Exception {
        LocalFileStorageService service = new LocalFileStorageService(tempDirectory.toString(), 1024 * 1024);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", bytes);
        MockMultipartFile logo = new MockMultipartFile("file", "logo.png", "image/png", bytes.toByteArray());

        String url = service.storeLogo(logo);

        assertTrue(url.matches("/api/v1/public/branding/company-logo/[0-9a-f-]{36}\\.png"));
        assertTrue(service.loadLogo(url.substring(url.lastIndexOf('/') + 1)).exists());
    }

    @Test
    void rejectsUnsafeNameAndMismatchedMimeType() {
        LocalFileStorageService service = new LocalFileStorageService(tempDirectory.toString(), 1024 * 1024);
        MockMultipartFile unsafe = new MockMultipartFile("file", "../logo.png", "image/png", new byte[] {1, 2, 3});
        MockMultipartFile mismatch = new MockMultipartFile("file", "logo.png", "image/jpeg", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

        assertThrows(BusinessException.class, () -> service.storeLogo(unsafe));
        assertThrows(BusinessException.class, () -> service.storeLogo(mismatch));
    }
}
