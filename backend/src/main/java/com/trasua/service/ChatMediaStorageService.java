package com.trasua.service;

import com.trasua.support.BusinessRuleException;
import com.trasua.support.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ChatMediaStorageService {
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 8_000;
    private static final int TARGET_IMAGE_DIMENSION = 1_280;
    private static final long FAST_PATH_MAX_BYTES = 1_500_000L;
    private static final float JPEG_QUALITY = 0.76f;

    private final Path storageDirectory;
    private final long maxTotalStorageBytes;

    public ChatMediaStorageService(
            @Value("${app.chat-media.storage-path:uploads/chat}") String storagePath,
            @Value("${app.chat-media.max-total-storage-bytes:1073741824}") long maxTotalStorageBytes) {
        this.storageDirectory = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxTotalStorageBytes = maxTotalStorageBytes;
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException error) {
            throw new IllegalStateException("Không thể tạo nơi lưu ảnh chat", error);
        }
    }

    public synchronized StoredChatMedia store(MultipartFile source) {
        if (source == null || source.isEmpty()) throw new BusinessRuleException("Ảnh gửi lên đang trống");
        if (source.getSize() > MAX_IMAGE_BYTES) throw new BusinessRuleException("Mỗi ảnh chỉ được tối đa 5 MB");

        try {
            byte[] originalBytes = source.getBytes();
            ImageFormat originalFormat = ImageFormat.detect(originalBytes);
            if (originalFormat == null) throw new BusinessRuleException("Chỉ hỗ trợ ảnh JPG, PNG, GIF hoặc WEBP");

            ProcessedImage processed = compress(originalBytes, originalFormat);
            long currentUsage = storageUsageBytes();
            if (currentUsage + processed.bytes().length > maxTotalStorageBytes) {
                throw new BusinessRuleException("Kho ảnh đã đạt giới hạn " + humanSize(maxTotalStorageBytes)
                        + ". Hãy xóa bớt ảnh cũ trước khi gửi tiếp.");
            }

            String storageKey = UUID.randomUUID() + "." + processed.format().extension;
            Path destination = resolve(storageKey);
            Files.write(destination, processed.bytes(), StandardOpenOption.CREATE_NEW);
            return new StoredChatMedia(storageKey, safeFilename(source.getOriginalFilename(), processed.format().extension),
                    processed.format().contentType, processed.bytes().length);
        } catch (BusinessRuleException error) {
            throw error;
        } catch (IOException error) {
            throw new IllegalStateException("Không thể lưu ảnh chat", error);
        }
    }

    public Resource load(String storageKey) {
        Path file = resolve(storageKey);
        if (!Files.isRegularFile(file)) throw new ResourceNotFoundException("Không tìm thấy ảnh chat");
        return new FileSystemResource(file);
    }

    public String contentType(String storageKey) {
        String lower = storageKey == null ? "" : storageKey.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        throw new ResourceNotFoundException("Định dạng ảnh không hợp lệ");
    }

    public void deleteQuietly(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException | RuntimeException ignored) {
            // Cleanup is best effort and must not hide the original operation.
        }
    }

    public int deleteOrphans(Set<String> referencedKeys, Duration gracePeriod) {
        Set<String> retained = new HashSet<>(referencedKeys);
        Instant deleteBefore = Instant.now().minus(gracePeriod);
        int deleted = 0;
        try (Stream<Path> files = Files.list(storageDirectory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                if (retained.contains(file.getFileName().toString())) continue;
                if (Files.getLastModifiedTime(file).toInstant().isAfter(deleteBefore)) continue;
                if (Files.deleteIfExists(file)) deleted++;
            }
            return deleted;
        } catch (IOException error) {
            throw new IllegalStateException("Không thể dọn tệp ảnh không còn sử dụng", error);
        }
    }

    public long storageUsageBytes() {
        try (Stream<Path> files = Files.list(storageDirectory)) {
            return files.filter(Files::isRegularFile).mapToLong(file -> {
                try { return Files.size(file); }
                catch (IOException ignored) { return 0L; }
            }).sum();
        } catch (IOException error) {
            throw new IllegalStateException("Không thể tính dung lượng kho ảnh", error);
        }
    }

    private ProcessedImage compress(byte[] bytes, ImageFormat format) throws IOException {
        if (format == ImageFormat.WEBP) return new ProcessedImage(bytes, format);
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0
                || source.getWidth() > MAX_IMAGE_DIMENSION || source.getHeight() > MAX_IMAGE_DIMENSION) {
            throw new BusinessRuleException("Kích thước ảnh không hợp lệ");
        }

        // Ảnh đã được trình duyệt thu nhỏ trước khi tải lên không cần mã hóa
        // lại tại server. Điều này giúp gửi ảnh nhanh hơn trên gói máy chủ nhỏ.
        if (Math.max(source.getWidth(), source.getHeight()) <= TARGET_IMAGE_DIMENSION
                && bytes.length <= FAST_PATH_MAX_BYTES) {
            return new ProcessedImage(bytes, format);
        }

        double scale = Math.min(1d, (double) TARGET_IMAGE_DIMENSION / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        boolean transparent = source.getColorModel().hasAlpha() && format == ImageFormat.PNG;
        BufferedImage resized = new BufferedImage(width, height, transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (!transparent) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
        }
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();

        ImageFormat outputFormat = transparent ? ImageFormat.PNG : ImageFormat.JPEG;
        byte[] compressed = outputFormat == ImageFormat.PNG ? writePng(resized) : writeJpeg(resized);
        if (scale == 1d && compressed.length >= bytes.length && (format == ImageFormat.JPEG || format == ImageFormat.PNG)) {
            return new ProcessedImage(bytes, format);
        }
        return new ProcessedImage(compressed, outputFormat);
    }

    private byte[] writePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) throw new IOException("PNG writer unavailable");
        return output.toByteArray();
    }

    private byte[] writeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) throw new IOException("JPEG writer unavailable");
        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("/") || storageKey.contains("\\")) {
            throw new BusinessRuleException("Tên tệp ảnh không hợp lệ");
        }
        Path path = storageDirectory.resolve(storageKey).normalize();
        if (!path.startsWith(storageDirectory)) throw new BusinessRuleException("Tên tệp ảnh không hợp lệ");
        return path;
    }

    private String safeFilename(String originalFilename, String extension) {
        String name = originalFilename == null ? "anh-chat" : originalFilename.replaceAll("[\\r\\n]", " ").trim();
        if (name.isBlank()) name = "anh-chat";
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        if (name.length() > 230) name = name.substring(0, 230);
        return name + "." + extension;
    }

    private String humanSize(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) return (bytes / (1024L * 1024 * 1024)) + " GB";
        return (bytes / (1024L * 1024)) + " MB";
    }

    public record StoredChatMedia(String storageKey, String originalFilename, String contentType, long byteSize) {}
    private record ProcessedImage(byte[] bytes, ImageFormat format) {}

    private enum ImageFormat {
        JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), GIF("gif", "image/gif"), WEBP("webp", "image/webp");
        private final String extension;
        private final String contentType;
        ImageFormat(String extension, String contentType) { this.extension = extension; this.contentType = contentType; }

        private static ImageFormat detect(byte[] bytes) {
            if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) return JPEG;
            if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) return PNG;
            if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && ((bytes[3] == '8' && bytes[4] == '7' && bytes[5] == 'a') || (bytes[3] == '8' && bytes[4] == '9' && bytes[5] == 'a'))) return GIF;
            if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return WEBP;
            return null;
        }
    }
}
