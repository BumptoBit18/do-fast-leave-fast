package controller;

import app.model.AuctionLot;
import app.model.BidRecord;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import support.JavaFxTestSupport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BidChartControllerTest {
    @BeforeAll
    static void setupFx() throws Exception {
        JavaFxTestSupport.ensureStarted();
    }

    @Test
    void shouldRenderLineChartFromBidHistory() throws Exception {
        AuctionLot lot = new AuctionLot(
                "AUC-CHART",
                "seller",
                "Camera",
                "Electronics",
                "Mirrorless",
                8_000_000,
                LocalDateTime.now().plusHours(2),
                ""
        );
        lot.placeBid(new BidRecord("bidderA", 8_500_000, LocalDateTime.of(2026, 6, 4, 14, 0)));
        lot.placeBid(new BidRecord("bidderB", 9_000_000, LocalDateTime.of(2026, 6, 4, 14, 5)));

        AtomicReference<Parent> viewRef = new AtomicReference<>();
        JavaFxTestSupport.runOnFxThreadAndWait(() -> viewRef.set(new BidChartController(lot).getView()));

        LineChart<?, ?> chart = assertInstanceOf(LineChart.class, viewRef.get());
        assertEquals("Realtime Price Curve", chart.getTitle());
        assertEquals(1, chart.getData().size());

        @SuppressWarnings("unchecked")
        XYChart.Series<String, Number> series = (XYChart.Series<String, Number>) chart.getData().getFirst();
        List<XYChart.Data<String, Number>> points = series.getData();
        assertEquals(2, points.size());
        assertEquals(8_500_000d, points.get(0).getYValue().doubleValue());
        assertEquals(9_000_000d, points.get(1).getYValue().doubleValue());
    }
}
