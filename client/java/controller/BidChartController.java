package controller;

import app.model.AuctionLot;
import app.model.BidRecord;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class BidChartController {
    private final AuctionLot auctionLot;

    public BidChartController(AuctionLot auctionLot) {
        this.auctionLot = auctionLot;
    }

    public Parent getView() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Timestamp");
        yAxis.setLabel("Gia hien tai");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setTitle("Realtime Price Curve");
        chart.getStyleClass().add("chart-card");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (BidRecord bidRecord : FXCollections.observableArrayList(auctionLot.getBidHistory())) {
            String timestamp = bidRecord.getTime().toString().replace('T', ' ');
            series.getData().add(new XYChart.Data<>(timestamp, bidRecord.getAmount()));
        }
        chart.getData().add(series);
        return chart;
    }
}
