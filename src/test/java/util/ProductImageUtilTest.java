package util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductImageUtilTest {
    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @TempDir
    Path tempDir;

    @Test
    void shouldEncodeAndDecodeSupportedImage() throws Exception {
        Path imageFile = tempDir.resolve("product.png");
        Files.write(imageFile, Base64.getDecoder().decode(ONE_PIXEL_PNG));

        String payload = ProductImageUtil.toDataUri(imageFile);

        assertTrue(payload.startsWith("data:image/png;base64,"));
        assertNotNull(ProductImageUtil.decode(payload));
    }

    @Test
    void shouldRejectUnsupportedOrOversizedFile() throws Exception {
        Path textFile = tempDir.resolve("notes.txt");
        Files.writeString(textFile, "not an image");
        Path largeImage = tempDir.resolve("large.png");
        Files.write(largeImage, new byte[(int) ProductImageUtil.MAX_IMAGE_BYTES + 1]);

        assertThrows(IllegalArgumentException.class, () -> ProductImageUtil.toDataUri(textFile));
        assertThrows(IllegalArgumentException.class, () -> ProductImageUtil.toDataUri(largeImage));
    }
}
