package controller;

import app.model.AppUser;
import app.model.AuctionLot;
import app.model.NotificationItem;
import app.model.UserRole;
import app.service.AuctionPlatformService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import network.ServerConnection;
import ui.AppUi;
import util.SceneManager;

import java.util.List;

public class AuctionListController {
    private final SceneManager sceneManager;
    private final AuctionPlatformService service;
    private final ObservableList<AuctionLot> visibleAuctions = FXCollections.observableArrayList();

    public AuctionListController(SceneManager sceneManager, ServerConnection serverConnection) {
        this.sceneManager = sceneManager;
        this.service = serverConnection.getService();
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setPadding(new Insets(24));

        AppUser currentUser = service.getCurrentUser();

        TextField searchField = new TextField();
        searchField.setPromptText("Tim theo ten, mo ta hoac ten nguoi ban...");
        searchField.setPrefWidth(320);

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(service.getCategories());
        categoryBox.setValue(service.getCategories().get(0));
        categoryBox.setPrefWidth(180);

        Button applyFilter = new Button("Loc");
        applyFilter.getStyleClass().add("secondary-button");

        Button sellerButton = new Button("Khu nguoi ban");
        sellerButton.getStyleClass().add("secondary-button");
        sellerButton.setVisible(currentUser.getRole() != UserRole.BIDDER);
        sellerButton.setManaged(currentUser.getRole() != UserRole.BIDDER);
        sellerButton.setOnAction(event -> sceneManager.showSellerDashboard());

        Button adminButton = new Button("Quan tri he thong");
        adminButton.getStyleClass().add("secondary-button");
        adminButton.setVisible(currentUser.getRole() == UserRole.ADMIN);
        adminButton.setManaged(currentUser.getRole() == UserRole.ADMIN);
        adminButton.setOnAction(event -> sceneManager.showAdminPanel());

        Button walletButton = new Button("Nap tien");
        walletButton.getStyleClass().add("secondary-button");
        walletButton.setOnAction(event -> sceneManager.showWallet());

        Button refreshButton = new Button("Lam moi");
        refreshButton.getStyleClass().add("secondary-button");

        Button logoutButton = new Button("Dang xuat");
        logoutButton.setOnAction(event -> sceneManager.logout());

        Label toolbarSubtitle = new Label();
        toolbarSubtitle.getStyleClass().add("muted-label");

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);

        HBox toolbarActions = new HBox(
                12,
                searchField,
                categoryBox,
                applyFilter,
                toolbarSpacer,
                refreshButton,
                logoutButton
        );
        toolbarActions.setAlignment(Pos.CENTER_LEFT);

        VBox toolbarCard = AppUi.panelCard(
                "Bo loc marketplace",
                "",
                toolbarSubtitle,
                toolbarActions
        );

        TableView<AuctionLot> table = buildTable();
        table.setItems(visibleAuctions);

        Label previewTitle = new Label("Chon mot phien dau gia de xem truoc");
        previewTitle.getStyleClass().add("card-title");

        Label previewMeta = new Label();
        previewMeta.getStyleClass().add("muted-label");

        TextArea previewDescription = new TextArea();
        previewDescription.setEditable(false);
        previewDescription.setWrapText(true);
        previewDescription.setPrefRowCount(8);

        ListView<String> notificationList = new ListView<>();
        notificationList.setPrefHeight(180);

        Label wonLabel = new Label();
        wonLabel.getStyleClass().add("muted-label");
        wonLabel.setManaged(currentUser.getRole() == UserRole.BIDDER);
        wonLabel.setVisible(currentUser.getRole() == UserRole.BIDDER);

        Button detailButton = new Button("Mo chi tiet");
        detailButton.getStyleClass().add("primary-button");
        detailButton.setMaxWidth(Double.MAX_VALUE);
        detailButton.setOnAction(event -> {
            AuctionLot selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                sceneManager.showAuctionDetail(selected);
            }
        });

        VBox bidderSummary = new VBox(8, new Label("Thong bao va ket qua gan day"), notificationList, wonLabel);
        VBox detailPane = AppUi.panelCard(
                "Xem nhanh",
                "Tom tat phien da chon va thong bao gan day.",
                previewTitle,
                previewMeta,
                previewDescription,
                bidderSummary,
                detailButton
        );
        detailPane.setPrefWidth(360);

        HBox content = new HBox(20, table, detailPane);
        HBox.setHgrow(table, Priority.ALWAYS);

        HBox statsStrip = new HBox(14);
        statsStrip.getStyleClass().add("stats-strip");

        Runnable refreshData = () -> {
            AppUser refreshedUser = service.getCurrentUser();
            ObservableList<AuctionLot> auctions = service.getAuctions();
            List<NotificationItem> notifications = service.getNotificationsForCurrentUser();

            toolbarSubtitle.setText(
                    "Xin chao " + refreshedUser.getFullName()
                            + " | Vai tro: " + refreshedUser.getRole()
                            + " | So du: " + service.formatCurrency(refreshedUser.getWalletBalance())
            );

            String selectedId = table.getSelectionModel().getSelectedItem() == null
                    ? null
                    : table.getSelectionModel().getSelectedItem().getId();

            refreshAuctions(searchField.getText(), categoryBox.getValue());

            if (selectedId != null) {
                visibleAuctions.stream()
                        .filter(item -> item.getId().equalsIgnoreCase(selectedId))
                        .findFirst()
                        .ifPresent(item -> table.getSelectionModel().select(item));
            }
            if (!visibleAuctions.isEmpty() && table.getSelectionModel().getSelectedItem() == null) {
                table.getSelectionModel().selectFirst();
            }

            notificationList.getItems().setAll(
                    notifications.stream()
                            .limit(5)
                            .map(item -> item.getTitle() + " - " + item.getMessage())
                            .toList()
            );

            if (refreshedUser.getRole() == UserRole.BIDDER) {
                wonLabel.setText("So lo da thang: " + service.getWonAuctionsForBidder(refreshedUser.getUsername()).size());
            }

            long liveCount = auctions.stream().filter(auction -> !auction.isClosed()).count();
            long bidCount = auctions.stream().mapToLong(auction -> auction.getBidHistory().size()).sum();
            double hottest = auctions.stream().mapToDouble(AuctionLot::getCurrentPrice).max().orElse(0);
            statsStrip.getChildren().setAll(
                    AppUi.statCard("Phien dang mo", String.valueOf(liveCount), "So phien dau gia dang dien ra"),
                    AppUi.statCard("Tong luot gia", String.valueOf(bidCount), "Tong so lan dat gia"),
                    AppUi.statCard("Gia cao nhat", service.formatCurrency(hottest), "Phien dau gia dat nhat"),
                    AppUi.statCard("Thong bao", String.valueOf(notifications.size()), "Thong bao moi nhat")
            );

            updatePreview(table.getSelectionModel().getSelectedItem(), previewTitle, previewMeta, previewDescription);
        };

        applyFilter.setOnAction(event -> refreshData.run());
        refreshButton.setOnAction(event -> refreshData.run());
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                sceneManager.showAuctionDetail(table.getSelectionModel().getSelectedItem());
            }
        });
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) ->
                updatePreview(selected, previewTitle, previewMeta, previewDescription)
        );

        refreshData.run();

        VBox centerBox = new VBox(
                18,
                AppUi.pageHeader(
                        "Marketplace",
                        "Trung tam dau gia",
                        "Kham pha cac phien dau gia dang dien ra, tim kiem san pham yeu thich va xem ro nguoi ban cua tung lo.",
                        AppUi.badge(currentUser.getRole().name()),
                        AppUi.badge("Vi " + service.formatCurrency(currentUser.getWalletBalance())),
                        walletButton,
                        sellerButton,
                        adminButton
                ),
                statsStrip,
                toolbarCard,
                content
        );
        VBox.setVgrow(content, Priority.ALWAYS);

        root.setCenter(centerBox);
        attachAutoRefresh(root, Duration.seconds(5), refreshData);
        return root;
    }

    private void updatePreview(AuctionLot selected, Label previewTitle, Label previewMeta, TextArea previewDescription) {
        if (selected == null) {
            previewTitle.setText("Chon mot phien dau gia de xem truoc");
            previewMeta.setText("");
            previewDescription.clear();
            return;
        }

        previewTitle.setText(selected.getTitle() + " | Nguoi ban: " + selected.getSellerUsername());
        previewMeta.setText(
                selected.getCategory()
                        + " | " + service.formatCurrency(selected.getCurrentPrice())
                        + " | " + selected.getTimeLeftLabel()
                        + " | " + selected.getStatusLabel()
        );
        previewDescription.setText(selected.getDescription());
    }

    private TableView<AuctionLot> buildTable() {
        TableView<AuctionLot> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<AuctionLot, String> titleColumn = new TableColumn<>("San pham dau gia");
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTitle() + " | Nguoi ban: " + data.getValue().getSellerUsername()
        ));
        titleColumn.setPrefWidth(340);

        TableColumn<AuctionLot, String> categoryColumn = new TableColumn<>("Danh muc");
        categoryColumn.setCellValueFactory(data -> data.getValue().categoryProperty());
        categoryColumn.setPrefWidth(130);

        TableColumn<AuctionLot, String> sellerColumn = new TableColumn<>("Nguoi ban");
        sellerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSellerUsername()));
        sellerColumn.setPrefWidth(130);

        TableColumn<AuctionLot, String> priceColumn = new TableColumn<>("Gia hien tai");
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(service.formatCurrency(data.getValue().getCurrentPrice())));
        priceColumn.setPrefWidth(150);

        TableColumn<AuctionLot, String> timeColumn = new TableColumn<>("Thoi gian con lai");
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimeLeftLabel()));
        timeColumn.setPrefWidth(170);

        TableColumn<AuctionLot, String> statusColumn = new TableColumn<>("Trang thai");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
        statusColumn.setPrefWidth(120);

        table.getColumns().setAll(titleColumn, categoryColumn, sellerColumn, priceColumn, timeColumn, statusColumn);
        return table;
    }

    private void refreshAuctions(String keyword, String category) {
        visibleAuctions.setAll(service.searchAuctions(keyword, category));
    }

    private void attachAutoRefresh(Parent root, Duration interval, Runnable action) {
        Timeline timeline = new Timeline(new KeyFrame(interval, event -> action.run()));
        timeline.setCycleCount(Timeline.INDEFINITE);

        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null) {
                timeline.stop();
            }
            if (newScene != null) {
                Window window = newScene.getWindow();
                if (window != null && window.isShowing()) {
                    timeline.playFromStart();
                } else {
                    newScene.windowProperty().addListener((windowObs, oldWindow, newWindow) -> {
                        if (newWindow != null) {
                            newWindow.showingProperty().addListener((showingObs, wasShowing, isShowing) -> {
                                if (isShowing) {
                                    timeline.playFromStart();
                                } else {
                                    timeline.stop();
                                }
                            });
                            if (newWindow.isShowing()) {
                                timeline.playFromStart();
                            }
                        }
                    });
                }
            }
        });
    }
}
