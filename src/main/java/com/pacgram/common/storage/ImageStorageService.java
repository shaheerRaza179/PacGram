package com.pacgram.common.storage;

import com.pacgram.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
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
public class ImageStorageService {
    private static final long MAX_BYTES = 10 * 1024 * 1024;
    private static final long MAX_PIXELS = 40_000_000;
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "png", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif");
    private final Path storageDirectory;

    public ImageStorageService(@Value("${pacgram.storage.location}") String location) {
        storageDirectory = Path.of(location).toAbsolutePath().normalize();
    }

    public String storeImage(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw badFile();
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw badFile();
        }
        try {
            byte[] image = file.getBytes();
            String format = validateImage(image);
            String extension = "jpeg".equals(format) ? "jpg" : format;
            String fileName = UUID.randomUUID() + "." + extension;
            Files.createDirectories(storageDirectory);
            Files.write(storageDirectory.resolve(fileName), image);
            return "/media/" + fileName;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "FILE_STORAGE_ERROR", "The image could not be stored");
        }
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/media/")) {
            return;
        }
        String fileName = imageUrl.substring("/media/".length());
        if (!fileName.matches("[0-9a-fA-F-]{36}\\.(jpg|png|gif)")) {
            return;
        }
        Path file = storageDirectory.resolve(fileName).normalize();
        if (!file.startsWith(storageDirectory)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "FILE_STORAGE_ERROR", "The image could not be removed");
        }
    }

    private String validateImage(byte[] image) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(image);
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw badFile();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw badFile();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw badFile();
                }
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!ALLOWED_FORMATS.contains(format)) {
                    throw badFile();
                }
                reader.read(0);
                return format;
            } finally {
                reader.dispose();
            }
        }
    }

    private ApiException badFile() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE",
                "Upload a valid JPEG, PNG, or GIF image no larger than 10 MB");
    }
}
