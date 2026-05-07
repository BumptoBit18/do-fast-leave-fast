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
        xAxis.setLabel("Lượt mua");
        yAxis.setLabel("Giá ");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setTitle("Tiến trình đấu giá");
        chart.getStyleClass().add("chart-card");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int index = 1;
        for (BidRecord bidRecord : FXCollections.observableArrayList(auctionLot.getBidHistory())) {
            series.getData().add(new XYChart.Data<>(
                    "#" + index + " - " + bidRecord.getBidderUsername(),
                    bidRecord.getAmount()
            ));
            index++;
        }
        chart.getData().add(series);
        return chart;
    }
}
