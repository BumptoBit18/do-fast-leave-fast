package controller;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import support.JavaFxTestSupport;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BiddingControllerTest {
    @BeforeAll
    static void setupFx() throws Exception {
        JavaFxTestSupport.ensureStarted();
    }

    @Test
    void shouldBuildBidFormAndSubmitParsedAmount() throws Exception {
        AtomicReference<Double> submittedAmount = new AtomicReference<>();
        AtomicReference<Parent> viewRef = new AtomicReference<>();

        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            BiddingController controller = new BiddingController(1_250_000, submittedAmount::set);
            viewRef.set(controller.getView());
        });

        VBox view = assertInstanceOf(VBox.class, viewRef.get());
        Label title = assertInstanceOf(Label.class, view.getChildren().get(0));
        VBox fieldGroup = assertInstanceOf(VBox.class, view.getChildren().get(1));
        TextField amountField = assertInstanceOf(TextField.class, fieldGroup.getChildren().get(2));
        Button submitButton = assertInstanceOf(Button.class, view.getChildren().get(2));

        assertEquals("Dat gia ngay", title.getText());
        assertEquals("1250000", amountField.getText());
        assertEquals("Xac nhan dat gia", submitButton.getText());

        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            amountField.setText("1750000");
            submitButton.fire();
        });

        assertEquals(1_750_000, submittedAmount.get());
    }
}
