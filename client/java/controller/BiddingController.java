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

        Label title = new Label("Dat gia ngay");
        title.getStyleClass().add("section-title");

        TextField amountField = new TextField(String.valueOf((long) suggestedValue));
        amountField.setPromptText("Nhap so tien ban muon dat");

        Button placeBid = new Button("Xac nhan dat gia");
        placeBid.getStyleClass().add("primary-button");
        placeBid.setMaxWidth(Double.MAX_VALUE);
        placeBid.setOnAction(event -> {
            try {
                bidHandler.accept(Double.parseDouble(amountField.getText().trim()));
            } catch (NumberFormatException ex) {
                AlertUtil.error("Gia khong hop le", "Hay nhap mot so tien hop le.");
            }
        });

        box.getChildren().addAll(
                title,
                AppUi.fieldGroup("Gia muon dat", "He thong da goi y muc toi thieu hop le. Ban co the nhap cao hon de tang co hoi dan dau.", amountField),
                placeBid
        );
        return box;
    }
}
