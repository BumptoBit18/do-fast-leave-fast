package controller;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ui.AppUi;
import util.AlertUtil;

import java.util.function.Consumer;

public class BiddingController {
    private final double suggestedValue;
    private final Consumer<Double> bidHandler;

    public BiddingController(double suggestedValue, Consumer<Double> bidHandler) {
        this.suggestedValue = suggestedValue;
        this.bidHandler = bidHandler;
    }

    public Parent getView() {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));

        Label title = new Label("Đặt giá ngay");
        title.getStyleClass().add("section-title");

        TextField amountField = new TextField(String.valueOf((long) suggestedValue));
        amountField.setPromptText("Nhập số tiền bạn muốn đặt");

        Button placeBid = new Button("Xác nhận đặt giá");
        placeBid.getStyleClass().add("primary-button");
        placeBid.setMaxWidth(Double.MAX_VALUE);
        placeBid.setOnAction(event -> {
            try {
                bidHandler.accept(Double.parseDouble(amountField.getText().trim()));
            } catch (NumberFormatException ex) {
                AlertUtil.error("Giá không hợp lệ", "Hãy nhập một số tiền hợp lệ.");
            }
        });

        box.getChildren().addAll(
                title,
                AppUi.fieldGroup("Giá muốn đặt", "Hệ thống đã gợi ý mức tối thiểu hợp lệ. Bạn có thể nhập cao hơn để tăng cơ hội dẫn đầu.", amountField),
                placeBid
        );
        return box;
    }
}
