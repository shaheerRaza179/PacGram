package com.pacgram.moderation.service;

import com.pacgram.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ReportEvidenceStorageService {
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm");
    private final Path storageDirectory;

    public ReportEvidenceStorageService(
            @Value("${pacgram.storage.report-evidence-location:./private-report-evidence}") String location) {
        storageDirectory = Path.of(location).toAbsolutePath().normalize();
    }

    public StoredEvidence store(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw invalidEvidence();
        }
        String contentType = file.getContentType();
        if (contentType == null) throw invalidEvidence();
        contentType = contentType.toLowerCase(Locale.ROOT);
        String extension = validateAndGetExtension(contentType, bytes(file));
        String generatedName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(storageDirectory);
            Files.write(storageDirectory.resolve(generatedName), file.getBytes());
            String originalName = Path.of(file.getOriginalFilename() == null
                    ? "evidence" : file.getOriginalFilename()).getFileName().toString();
            return new StoredEvidence(generatedName, originalName, contentType, file.getSize());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "EVIDENCE_STORAGE_ERROR", "Report evidence could not be stored");
        }
    }

    public Resource load(String fileName) {
        if (fileName == null || !fileName.matches("[0-9a-fA-F-]{36}\\.(jpg|png|gif|mp4|webm)")) {
            throw invalidEvidence();
        }
        Path path = storageDirectory.resolve(fileName).normalize();
        if (!path.startsWith(storageDirectory) || !Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "EVIDENCE_NOT_FOUND", "Report evidence was not found");
        }
        return new FileSystemResource(path);
    }

    public void delete(String fileName) {
        if (fileName == null || !fileName.matches("[0-9a-fA-F-]{36}\\.(jpg|png|gif|mp4|webm)")) return;
        try {
            Files.deleteIfExists(storageDirectory.resolve(fileName).normalize());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "EVIDENCE_STORAGE_ERROR", "Report evidence could not be removed");
        }
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw invalidEvidence();
        }
    }

    private String validateAndGetExtension(String type, byte[] bytes) {
        if (IMAGE_TYPES.contains(type)) {
            try (ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                 ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
                if (imageInput == null) throw invalidEvidence();
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) throw invalidEvidence();
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInput, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || (long) width * height > 40_000_000L) {
                        throw invalidEvidence();
                    }
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    String actualType = switch (format) {
                        case "jpeg", "jpg" -> "image/jpeg";
                        case "png" -> "image/png";
                        case "gif" -> "image/gif";
                        default -> throw invalidEvidence();
                    };
                    if (!actualType.equals(type)) throw invalidEvidence();
                    reader.read(0);
                    return "image/jpeg".equals(type) ? ".jpg" : "image/png".equals(type) ? ".png" : ".gif";
                } finally {
                    reader.dispose();
                }
            } catch (IOException exception) {
                throw invalidEvidence();
            }
        }
        if ("video/mp4".equals(type) && bytes.length >= 12
                && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p') {
            return ".mp4";
        }
        if ("video/webm".equals(type) && bytes.length >= 4
                && bytes[0] == 0x1A && bytes[1] == 0x45 && bytes[2] == (byte) 0xDF
                && bytes[3] == (byte) 0xA3) {
            return ".webm";
        }
        throw invalidEvidence();
    }

    private ApiException invalidEvidence() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REPORT_EVIDENCE",
                "Attach up to 3 valid JPEG, PNG, GIF, MP4, or WebM files, each no larger than 20 MB");
    }

    public record StoredEvidence(String fileName, String originalName, String contentType, long fileSize) {
    }
}
