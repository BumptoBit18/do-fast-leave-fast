package controller;

import app.model.AppUser;
import app.model.AuctionLot;
import app.model.BidRecord;
import app.model.UserRole;
import app.service.AuctionPlatformService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import network.ServerConnection;
import ui.AppUi;
import util.AlertUtil;
import util.SceneManager;

public class AuctionDetailController {
    private final SceneManager sceneManager;
    private final AuctionPlatformService service;
    private final AuctionLot auctionLot;
    private final AppUser currentUser;

    public AuctionDetailController(SceneManager sceneManager, ServerConnection serverConnection, AuctionLot auctionLot) {
        this.sceneManager = sceneManager;
        this.service = serverConnection.getService();
        this.currentUser = service.getCurrentUser();
        this.auctionLot = service.getAuctionById(auctionLot.getId());
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setPadding(new Insets(24));

        Button backButton = new Button("Về chợ đấu giá");
        backButton.setOnAction(event -> sceneManager.showAuctionList());

        Button sellerButton = new Button("Khu người bán");
        sellerButton.getStyleClass().add("secondary-button");
        sellerButton.setVisible(currentUser.getRole() != UserRole.BIDDER);
        sellerButton.setManaged(currentUser.getRole() != UserRole.BIDDER);
        sellerButton.setOnAction(event -> sceneManager.showSellerDashboard());

        Button walletButton = new Button("Nạp tiền");
        walletButton.getStyleClass().add("secondary-button");
        walletButton.setOnAction(event -> sceneManager.showWallet());

        Button logoutButton = new Button("Đăng xuất");
        logoutButton.setOnAction(event -> sceneManager.logout());

        VBox left = new VBox(18);
        left.setPrefWidth(420);
        left.getChildren().addAll(overviewCard(), biddingArea());

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Lịch sử đặt giá", buildHistoryTable()));
        tabs.getTabs().add(new Tab("Biểu đồ giá", new BidChartController(auctionLot).getView()));
        tabs.getTabs().add(new Tab("Thông báo", buildNotificationPanel()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox rightPanel = AppUi.panelCard(
                "Dòng thời gian và thông báo",
                "Theo dõi lịch sử đặt giá, biểu đồ giá và các sự kiện liên quan tới lô.",
                tabs
        );
        HBox.setHgrow(tabs, Priority.ALWAYS);

        root.setCenter(new VBox(
                18,
                AppUi.pageHeader(
                        "Chi tiết đấu giá",
                        auctionLot.getTitle() + " | Người bán: " + auctionLot.getSellerUsername(),
                        auctionLot.getCategory() + " | Mã phiên: " + auctionLot.getId(),
                        AppUi.badge(auctionLot.getStatusLabel()),
                        AppUi.badge(service.formatCurrency(auctionLot.getCurrentPrice())),
                        backButton,
                        walletButton,
                        sellerButton,
                        logoutButton
                ),
                new HBox(20, left, rightPanel)
        ));
        return root;
    }

    private VBox overviewCard() {
        VBox card = AppUi.panelCard("Tổng quan lô đấu giá", "Cập nhật trạng thái theo thời gian thực của phiên đang xem.");

        Label title = new Label(auctionLot.getTitle() + " | Người bán: " + auctionLot.getSellerUsername());
        title.getStyleClass().add("page-title");

        Label meta = new Label(auctionLot.getCategory() + " | Mã phiên: " + auctionLot.getId());
        meta.getStyleClass().add("muted-label");

        Label price = new Label("Giá hiện tại: " + service.formatCurrency(auctionLot.getCurrentPrice()));
        price.getStyleClass().add("stat-value");

        Label status = new Label("Trạng thái: " + auctionLot.getStatusLabel() + " | Còn lại: " + auctionLot.getTimeLeftLabel());
        Label leader = new Label("Người đang dẫn đầu: " + auctionLot.getHighestBidder());
        Label antiSnipe = new Label("Chống đặt giá sát giờ: " + (auctionLot.isAntiSnipeTriggered() ? "Đã kích hoạt" : "Chưa kích hoạt"));
        antiSnipe.getStyleClass().add("muted-label");

        Label description = new Label(auctionLot.getDescription());
        description.setWrapText(true);

        card.getChildren().addAll(title, meta, price, status, leader, antiSnipe, description);
        return card;
    }

    private Parent biddingArea() {
        if (currentUser.getRole() != UserRole.BIDDER) {
            VBox box = new VBox(10);
            box.getStyleClass().add("card");
            box.setPadding(new Insets(20));
            box.getChildren().add(new Label("Tài khoản hiện tại không phải người mua, nên chỉ có quyền xem thông tin lô."));
            return box;
        }

        VBox wrapper = new VBox(16);

        if (!auctionLot.isClosed()) {
            wrapper.getChildren().add(new BiddingController(auctionLot.getMinimumBid(), amount -> {
                try {
                    service.placeBid(auctionLot, amount);
                    AlertUtil.info("Đặt giá thành công", "Bạn đã đặt giá " + service.formatCurrency(amount));
                    sceneManager.showAuctionDetail(auctionLot);
                } catch (Exception ex) {
                    AlertUtil.error("Không thể đặt giá", ex.getMessage());
                }
            }).getView());

            VBox autoBidCard = new VBox(10);
            autoBidCard.getStyleClass().add("card");
            autoBidCard.setPadding(new Insets(20));

            Label autoTitle = new Label("Đặt giá tự động");
            autoTitle.getStyleClass().add("section-title");

            TextField maxAmountField = new TextField(String.valueOf((long) (auctionLot.getMinimumBid() + 1_000_000)));
            maxAmountField.setPromptText("Nhập mức trần tối đa");

            TextField stepField = new TextField("200000");
            stepField.setPromptText("Nhập bước tăng mỗi lần");

            Button enableAutoBid = new Button("Bật đặt giá tự động");
            enableAutoBid.getStyleClass().add("secondary-button");
            enableAutoBid.setMaxWidth(Double.MAX_VALUE);
            enableAutoBid.setOnAction(event -> {
                try {
                    service.enableAutoBid(
                            auctionLot,
                            Double.parseDouble(maxAmountField.getText().trim()),
                            Double.parseDouble(stepField.getText().trim())
                    );
                    AlertUtil.info("Đã bật đặt giá tự động", "Hệ thống sẽ tự động đặt giá cho bạn theo cấu hình vừa chọn.");
                    sceneManager.showAuctionDetail(auctionLot);
                } catch (Exception ex) {
                    AlertUtil.error("Không bật được đặt giá tự động", ex.getMessage());
                }
            });

            autoBidCard.getChildren().addAll(
                    autoTitle,
                    AppUi.fieldGroup("Mức trần tối đa", "Đây là số tiền cao nhất hệ thống được phép thay bạn đặt cho lô này.", maxAmountField),
                    AppUi.fieldGroup("Bước tăng", "Mỗi lần tự động đấu giá, hệ thống sẽ tăng theo đúng bước này nếu còn trong mức trần.", stepField),
                    enableAutoBid
            );
            wrapper.getChildren().add(autoBidCard);
        } else {
            VBox closedCard = new VBox(10);
            closedCard.getStyleClass().add("card");
            closedCard.setPadding(new Insets(20));
            closedCard.getChildren().add(new Label("Phiên này đã đóng nên bạn không thể đặt giá thêm."));

            if (auctionLot.getHighestBidder().equalsIgnoreCase(currentUser.getUsername()) && !auctionLot.isPaid()) {
                TextField topUpField = new TextField("5000000");
                topUpField.setPromptText("Nhập số tiền muốn nạp thêm");

                Button topUpButton = new Button("Nạp tiền vào ví");
                topUpButton.getStyleClass().add("secondary-button");
                topUpButton.setMaxWidth(Double.MAX_VALUE);
                topUpButton.setOnAction(event -> {
                    try {
                        service.topUpWallet(Double.parseDouble(topUpField.getText().trim()));
                        AlertUtil.info("Nạp tiền thành công", "Số dư mới: " + service.formatCurrency(service.getCurrentUser().getWalletBalance()));
                        sceneManager.showAuctionDetail(auctionLot);
                    } catch (Exception ex) {
                        AlertUtil.error("Không nạp được tiền", ex.getMessage());
                    }
                });

                Button payButton = new Button("Thanh toán lô này");
                payButton.getStyleClass().add("primary-button");
                payButton.setMaxWidth(Double.MAX_VALUE);
                payButton.setOnAction(event -> {
                    try {
                        service.payForAuction(auctionLot);
                        AlertUtil.info("Thanh toán thành công", "Bạn đã thanh toán " + service.formatCurrency(auctionLot.getCurrentPrice()));
                        sceneManager.showAuctionDetail(auctionLot);
                    } catch (Exception ex) {
                        AlertUtil.error("Thanh toán thất bại", ex.getMessage());
                    }
                });

                closedCard.getChildren().addAll(
                        new Label("Bạn đang là người thắng. Số dư hiện tại: " + service.formatCurrency(currentUser.getWalletBalance())),
                        AppUi.fieldGroup("Số tiền muốn nạp thêm", "Nếu ví chưa đủ để thanh toán, hãy nạp thêm tiền trước khi bấm thanh toán.", topUpField),
                        topUpButton,
                        payButton
                );
            }
            wrapper.getChildren().add(closedCard);
        }

        return wrapper;
    }

    private Parent buildHistoryTable() {
        TableView<BidRecord> table = new TableView<>();

        TableColumn<BidRecord, String> bidderColumn = new TableColumn<>("Người đặt");
        bidderColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBidderUsername()));
        bidderColumn.setPrefWidth(180);

        TableColumn<BidRecord, String> amountColumn = new TableColumn<>("Giá");
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(service.formatCurrency(data.getValue().getAmount())));
        amountColumn.setPrefWidth(180);

        TableColumn<BidRecord, String> timeColumn = new TableColumn<>("Thời gian");
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTime().toString().replace('T', ' ')));
        timeColumn.setPrefWidth(220);

        table.getColumns().addAll(bidderColumn, amountColumn, timeColumn);
        table.getItems().setAll(auctionLot.getBidHistory());
        return table;
    }

    private Parent buildNotificationPanel() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(16));

        Label hint = new Label("Thông báo liên quan đến lô này và các sự kiện hệ thống.");
        hint.getStyleClass().add("muted-label");

        ListView<String> detailList = new ListView<>();
        detailList.getItems().setAll(
                service.getNotificationsForCurrentUser().stream()
                        .filter(item -> item.getMessage().contains(auctionLot.getTitle()) || item.getTitle().contains("Anti-snipe"))
                        .map(item -> item.getTime().toString().replace('T', ' ') + " | " + item.getTitle() + " | " + item.getMessage())
                        .toList()
        );

        wrapper.getChildren().addAll(hint, detailList);
        return wrapper;
    }
}
