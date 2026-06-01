package server.controller;

import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.BidTransaction;
import server.model.item.Item;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentBidTest {
    private final ItemController itemController = new ItemController();

    @Test
    void shouldAcceptConcurrentBidsWithoutLosingEntries() throws Exception {
        Item item = itemController.createItem(
                "Electronics",
                "IT-5",
                "Console",
                "Brand new",
                7_000_000,
                LocalDateTime.now().plusHours(3),
                ""
        );
        Auction auction = new Auction("AUC-5", "seller", item);

        int bidCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(bidCount);
        CountDownLatch ready = new CountDownLatch(bidCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(bidCount);
        List<Throwable> failures = new ArrayList<>();

        for (int index = 0; index < bidCount; index++) {
            final int bidIndex = index;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    auction.addBid(new BidTransaction(
                            "BID",
                            "bidder-" + bidIndex,
                            auction.getId(),
                            "Concurrent bid",
                            7_100_000 + (bidIndex * 100_000L),
                            LocalDateTime.now()
                    ));
                } catch (Throwable ex) {
                    synchronized (failures) {
                        failures.add(ex);
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        executor.shutdownNow();

        assertTrue(failures.isEmpty(), "Concurrent bidding should not fail");
        assertEquals(bidCount, auction.getBidHistory().size());
        assertEquals(7_100_000 + ((bidCount - 1) * 100_000L), auction.getCurrentPrice());
    }
}
