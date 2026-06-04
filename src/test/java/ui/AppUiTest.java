package ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import support.JavaFxTestSupport;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppUiTest {
    @BeforeAll
    static void setupFx() throws Exception {
        JavaFxTestSupport.ensureStarted();
    }

    @Test
    void shouldBuildReusableUiBlocks() throws Exception {
        AtomicReference<HBox> headerRef = new AtomicReference<>();
        AtomicReference<VBox> cardRef = new AtomicReference<>();
        AtomicReference<VBox> statRef = new AtomicReference<>();
        AtomicReference<Label> badgeRef = new AtomicReference<>();
        AtomicReference<VBox> fieldRef = new AtomicReference<>();

        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            headerRef.set(AppUi.pageHeader("Market", "Auction Hub", "Realtime bids", new Button("Refresh")));
            cardRef.set(AppUi.panelCard("Panel", "Helper text", new Label("Body")));
            statRef.set(AppUi.statCard("Open", "12", "Running now"));
            badgeRef.set(AppUi.badge("ADMIN"));
            fieldRef.set(AppUi.fieldGroup("Amount", "Nhap gia", new TextField("1000000")));
        });

        HBox header = headerRef.get();
        VBox card = cardRef.get();
        VBox stat = statRef.get();
        Label badge = badgeRef.get();
        VBox field = fieldRef.get();

        assertEquals(3, ((VBox) header.getChildren().get(0)).getChildren().size());
        assertEquals("Auction Hub", ((Label) ((VBox) header.getChildren().get(0)).getChildren().get(1)).getText());
        assertEquals("Panel", ((Label) card.getChildren().get(0)).getText());
        assertEquals("Helper text", ((Label) card.getChildren().get(1)).getText());
        assertEquals("Open", ((Label) stat.getChildren().get(0)).getText());
        assertEquals("12", ((Label) stat.getChildren().get(1)).getText());
        assertEquals("ADMIN", badge.getText());
        assertEquals("Amount", ((Label) field.getChildren().get(0)).getText());
        assertTrue(field.getChildren().get(2) instanceof TextField);
    }
}
