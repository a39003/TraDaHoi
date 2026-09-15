package com.trasua.service;

import com.trasua.support.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMediaStorageServiceTest {
    @TempDir
    Path storage;

    @Test
    void resizesLargeImageBeforeSaving() throws Exception {
        ChatMediaStorageService service = new ChatMediaStorageService(storage.toString(), 1024L * 1024 * 1024);
        service.initialize();
        MockMultipartFile upload = new MockMultipartFile("image", "large.png", "image/png", png(3000, 2000));

        ChatMediaStorageService.StoredChatMedia stored = service.store(upload);
        BufferedImage saved = ImageIO.read(service.load(stored.storageKey()).getInputStream());

        assertTrue(Math.max(saved.getWidth(), saved.getHeight()) <= 1600);
        assertTrue(stored.byteSize() > 0);
    }

    @Test
    void rejectsImageWhenTotalStorageLimitWouldBeExceeded() throws Exception {
        ChatMediaStorageService service = new ChatMediaStorageService(storage.toString(), 10);
        service.initialize();
        MockMultipartFile upload = new MockMultipartFile("image", "small.png", "image/png", png(20, 20));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.store(upload));
        assertTrue(error.getMessage().contains("giới hạn"));
        assertEquals(0, service.storageUsageBytes());
    }

    private byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(45, 119, 84));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
