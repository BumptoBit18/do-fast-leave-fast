package controller;

import app.model.AppUser;
import app.model.AuctionLot;
import app.model.BidRecord;
import app.model.UserRole;
import app.service.AuctionPlatformService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.MessageListener;

import java.util.List;
import network.ServerConnection;
import shared.socket.RealtimeEvent;
import ui.AppUi;
import util.AlertUtil;
import util.ProductImageUtil;
import util.SceneManager;

public class AuctionDetailController implements MessageListener {
    private final SceneManager sceneManager;
    private final ServerConnection serverConnection;
    private final AuctionPlatformService service;
    private final String auctionId;
    @FXML
    private StackPane root;

    public AuctionDetailController(SceneManager sceneManager, ServerConnection serverConnection, AuctionLot auctionLot) {
        this.sceneManager = sceneManager;
        this.serverConnection = serverConnection;
        this.service = serverConnection.getService();
        this.auctionId = auctionLot.getId();
    }

    @FXML
    private void initialize() {
        serverConnection.addMessageListener(this);
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                serverConnection.removeMessageListener(this);
            }
        });
        root.getChildren().setAll(buildView());
    }

    private Parent buildView() {
        AuctionLot auctionLot = service.getAuctionById(auctionId);
        AppUser currentUser = service.getCurrentUser();

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setPadding(new Insets(24));

        Button backButton = new Button("Ve cho dau gia");
        backButton.setOnAction(event -> sceneManager.showAuctionList());

        Button sellerButton = new Button("Khu nguoi ban");
        sellerButton.getStyleClass().add("secondary-button");
        sellerButton.setVisible(currentUser.getRole() != UserRole.BIDDER);
        sellerButton.setManaged(currentUser.getRole() != UserRole.BIDDER);
        sellerButton.setOnAction(event -> sceneManager.showSellerDashboard());

        Button walletButton = new Button("Nap tien");
        walletButton.getStyleClass().add("secondary-button");
        walletButton.setOnAction(event -> sceneManager.showWallet());

        Button logoutButton = new Button("Dang xuat");
        logoutButton.setOnAction(event -> sceneManager.logout());

        VBox left = new VBox(18);
        left.setPrefWidth(420);
        left.getChildren().addAll(overviewCard(auctionLot), biddingArea(auctionLot, currentUser));

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Lich su dat gia", buildHistoryTable(auctionLot)));
        tabs.getTabs().add(new Tab("Bieu do gia", new BidChartController(auctionLot).getView()));
        tabs.getTabs().add(new Tab("Thong bao", buildNotificationPanel(auctionLot)));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox rightPanel = AppUi.panelCard(
                "Dong thoi gian va thong bao",
                "Theo doi lich su dat gia, bieu do gia va cac su kien lien quan toi lo.",
                tabs
        );
        HBox.setHgrow(tabs, Priority.ALWAYS);

        shell.setCenter(new VBox(
                18,
                AppUi.pageHeader(
                        "Chi tiet dau gia",
                        auctionLot.getTitle() + " | Nguoi ban: " + auctionLot.getSellerUsername(),
                        auctionLot.getCategory() + " | Ma phien: " + auctionLot.getId(),
                        AppUi.badge(auctionLot.getStatusLabel()),
                        AppUi.badge(service.formatCurrency(auctionLot.getCurrentPrice())),
                        backButton,
                        walletButton,
                        sellerButton,
                        logoutButton
                ),
                new HBox(20, left, rightPanel)
        ));
        return shell;
    }

    private VBox overviewCard(AuctionLot auctionLot) {
        VBox card = AppUi.panelCard("Tong quan lo dau gia", "Cap nhat trang thai theo thoi gian thuc cua phien dang xem.");

        Label title = new Label(auctionLot.getTitle() + " | Nguoi ban: " + auctionLot.getSellerUsername());
        title.getStyleClass().add("page-title");

        Label meta = new Label(auctionLot.getCategory() + " | Ma phien: " + auctionLot.getId());
        meta.getStyleClass().add("muted-label");

        Label price = new Label("Gia hien tai: " + service.formatCurrency(auctionLot.getCurrentPrice()));
        price.getStyleClass().add("stat-value");

        Label status = new Label("Trang thai: " + auctionLot.getStatusLabel() + " | Con lai: " + auctionLot.getTimeLeftLabel());
        Label leader = new Label("Nguoi dang dan dau: " + auctionLot.getHighestBidder());
        Label antiSnipe = new Label("Chong dat gia sat gio: " + (auctionLot.isAntiSnipeTriggered() ? "Da kich hoat" : "Chua kich hoat"));
        antiSnipe.getStyleClass().add("muted-label");

        Label description = new Label(auctionLot.getDescription());
        description.setWrapText(true);

        Image image = ProductImageUtil.decode(auctionLot.getImageHint());
        if (image != null) {
            ImageView productImage = new ImageView(image);
            productImage.setFitWidth(360);
            productImage.setFitHeight(240);
            productImage.setPreserveRatio(true);
            productImage.setSmooth(true);
            card.getChildren().add(productImage);
        }

        card.getChildren().addAll(title, meta, price, status, leader, antiSnipe, description);
        return card;
    }

    private Parent biddingArea(AuctionLot auctionLot, AppUser currentUser) {
        if (currentUser.getRole() != UserRole.BIDDER) {
            VBox box = new VBox(10);
            box.getStyleClass().add("card");
            box.setPadding(new Insets(20));
            box.getChildren().add(new Label("Tai khoan hien tai khong phai nguoi mua, nen chi co quyen xem thong tin lo."));
            return box;
        }

        VBox wrapper = new VBox(16);

        if (!auctionLot.isClosed()) {
            wrapper.getChildren().add(new BiddingController(auctionLot.getMinimumBid(), amount -> {
                try {
                    service.placeBid(auctionLot, amount);
                    AlertUtil.info("Dat gia thanh cong", "Ban da dat gia " + service.formatCurrency(amount));
                    root.getChildren().setAll(buildView());
                } catch (Exception ex) {
                    AlertUtil.error("Khong the dat gia", ex.getMessage());
                }
            }).getView());

            VBox autoBidCard = new VBox(10);
            autoBidCard.getStyleClass().add("card");
            autoBidCard.setPadding(new Insets(20));

            Label autoTitle = new Label("Dat gia tu dong");
            autoTitle.getStyleClass().add("section-title");

            TextField maxAmountField = new TextField(String.valueOf((long) (auctionLot.getMinimumBid() + 1_000_000)));
            maxAmountField.setPromptText("Nhap muc tran toi da");

            TextField stepField = new TextField("200000");
            stepField.setPromptText("Nhap buoc tang moi lan");

            Button enableAutoBid = new Button("Bat dat gia tu dong");
            enableAutoBid.getStyleClass().add("secondary-button");
            enableAutoBid.setMaxWidth(Double.MAX_VALUE);
            enableAutoBid.setOnAction(event -> {
                try {
                    service.enableAutoBid(
                            auctionLot,
                            Double.parseDouble(maxAmountField.getText().trim()),
                            Double.parseDouble(stepField.getText().trim())
                    );
                    AlertUtil.info("Da bat dat gia tu dong", "He thong se tu dong dat gia cho ban theo cau hinh vua chon.");
                    root.getChildren().setAll(buildView());
                } catch (Exception ex) {
                    AlertUtil.error("Khong bat duoc dat gia tu dong", ex.getMessage());
                }
            });

            autoBidCard.getChildren().addAll(
                    autoTitle,
                    AppUi.fieldGroup("Muc tran toi da", "Day la so tien cao nhat he thong duoc phep thay ban dat cho lo nay.", maxAmountField),
                    AppUi.fieldGroup("Buoc tang", "Moi lan tu dong dau gia, he thong se tang theo dung buoc nay neu con trong muc tran.", stepField),
                    enableAutoBid
            );
            wrapper.getChildren().add(autoBidCard);
        } else {
            VBox closedCard = new VBox(10);
            closedCard.getStyleClass().add("card");
            closedCard.setPadding(new Insets(20));
            closedCard.getChildren().add(new Label("Phien nay da dong nen ban khong the dat gia them."));

            if (auctionLot.getHighestBidder().equalsIgnoreCase(currentUser.getUsername()) && !auctionLot.isPaid()) {
                TextField topUpField = new TextField("5000000");
                topUpField.setPromptText("Nhap so tien muon nap them");

                Button topUpButton = new Button("Nap tien vao vi");
                topUpButton.getStyleClass().add("secondary-button");
                topUpButton.setMaxWidth(Double.MAX_VALUE);
                topUpButton.setOnAction(event -> {
                    try {
                        service.topUpWallet(Double.parseDouble(topUpField.getText().trim()));
                        AlertUtil.info("Nap tien thanh cong", "So du moi: " + service.formatCurrency(service.getCurrentUser().getWalletBalance()));
                        root.getChildren().setAll(buildView());
                    } catch (Exception ex) {
                        AlertUtil.error("Khong nap duoc tien", ex.getMessage());
                    }
                });

                Button payButton = new Button("Thanh toan lo nay");
                payButton.getStyleClass().add("primary-button");
                payButton.setMaxWidth(Double.MAX_VALUE);
                payButton.setOnAction(event -> {
                    try {
                        service.payForAuction(auctionLot);
                        AlertUtil.info("Thanh toan thanh cong", "Ban da thanh toan " + service.formatCurrency(auctionLot.getCurrentPrice()));
                        root.getChildren().setAll(buildView());
                    } catch (Exception ex) {
                        AlertUtil.error("Thanh toan that bai", ex.getMessage());
                    }
                });

                closedCard.getChildren().addAll(
                        new Label("Ban dang la nguoi thang. So du hien tai: " + service.formatCurrency(currentUser.getWalletBalance())),
                        AppUi.fieldGroup("So tien muon nap them", "Neu vi chua du de thanh toan, hay nap them tien truoc khi bam thanh toan.", topUpField),
                        topUpButton,
                        payButton
                );
            }
            wrapper.getChildren().add(closedCard);
        }

        return wrapper;
    }

    private Parent buildHistoryTable(AuctionLot auctionLot) {
        TableView<BidRecord> table = new TableView<>();

        TableColumn<BidRecord, String> bidderColumn = new TableColumn<>("Nguoi dat");
        bidderColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBidderUsername()));
        bidderColumn.setPrefWidth(180);

        TableColumn<BidRecord, String> amountColumn = new TableColumn<>("Gia");
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(service.formatCurrency(data.getValue().getAmount())));
        amountColumn.setPrefWidth(180);

        TableColumn<BidRecord, String> timeColumn = new TableColumn<>("Thoi gian");
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTime().toString().replace('T', ' ')));
        timeColumn.setPrefWidth(220);

        table.getColumns().setAll(List.of(bidderColumn, amountColumn, timeColumn));
        table.getItems().setAll(auctionLot.getBidHistory());
        return table;
    }

    private Parent buildNotificationPanel(AuctionLot auctionLot) {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(16));

        Label hint = new Label("Thong bao lien quan den lo nay va cac su kien he thong.");
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

    @Override
    public void onMessage(RealtimeEvent event) {
        if (event == null) {
            return;
        }
        if (event.auctionId() != null && !event.auctionId().equalsIgnoreCase(auctionId)) {
            return;
        }
        if (root == null) {
            return;
        }
        Platform.runLater(() -> root.getChildren().setAll(buildView()));
    }
}
