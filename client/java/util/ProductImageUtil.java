package util;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

public final class ProductImageUtil {
    public static final long MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/bmp"
    );

    private ProductImageUtil() {
    }

    public static String toDataUri(Path path) throws IOException {
        long size = Files.size(path);
        if (size <= 0) {
            throw new IllegalArgumentException("File anh dang rong.");
        }
        if (size > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Anh san pham toi da 2 MB.");
        }

        String contentType = detectContentType(path);
        if (!SUPPORTED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Chi ho tro anh PNG, JPG, GIF hoac BMP.");
        }

        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
    }

    public static Image decode(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            String base64Data = payload.contains(",")
                    ? payload.substring(payload.indexOf(',') + 1)
                    : payload;
            Image image = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(base64Data)));
            return image.isError() ? null : image;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String detectContentType(Path path) throws IOException {
        String detected = Files.probeContentType(path);
        if (detected != null) {
            return detected.toLowerCase(Locale.ROOT);
        }

        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".png")) {
            return "image/png";
        }
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (filename.endsWith(".gif")) {
            return "image/gif";
        }
        if (filename.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "application/octet-stream";
    }
}
