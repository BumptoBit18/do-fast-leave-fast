package server.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.model.BidTransaction;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectFileStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldWriteAndReadSerializedList() {
        Path file = tempDir.resolve("nested").resolve("transactions.dat");
        LocalDateTime time = LocalDateTime.of(2026, 6, 1, 10, 30);
        BidTransaction transaction = new BidTransaction("BID", "bidder", "AUC-1", "Manual bid", 1_500_000, time);

        ObjectFileStore.writeList(file, List.of(transaction));
        List<BidTransaction> restored = ObjectFileStore.readList(file);

        assertEquals(1, restored.size());
        assertEquals("BID", restored.get(0).getType());
        assertEquals("bidder", restored.get(0).getActorUsername());
        assertEquals("AUC-1", restored.get(0).getReferenceId());
        assertEquals("Manual bid", restored.get(0).getDescription());
        assertEquals(1_500_000, restored.get(0).getAmount());
        assertEquals(time, restored.get(0).getTime());
    }

    @Test
    void shouldReturnEmptyListForMissingOrUnreadableData() throws Exception {
        assertTrue(ObjectFileStore.readList(tempDir.resolve("missing.dat")).isEmpty());
        Path invalidFile = tempDir.resolve("invalid.dat");
        Files.writeString(invalidFile, "not a serialized object");
        assertTrue(ObjectFileStore.readList(invalidFile).isEmpty());
    }
}
