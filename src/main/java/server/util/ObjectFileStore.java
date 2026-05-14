package server.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ObjectFileStore {
    private ObjectFileStore() {
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> List<T> readList(Path path) {
        try {
            ensureFile(path);
            if (Files.size(path) == 0) {
                return new ArrayList<>();
            }
            try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
                Object value = input.readObject();
                if (value instanceof List<?>) {
                    return new ArrayList<>((List<T>) value);
                }
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    public static synchronized <T> void writeList(Path path, List<T> values) {
        try {
            ensureFile(path);
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(path))) {
                output.writeObject(new ArrayList<>(values));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Khong the ghi du lieu vao " + path, ex);
        }
    }

    private static void ensureFile(Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }
}
