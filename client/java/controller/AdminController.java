package controller;

import app.model.AppUser;
import app.model.AuctionLot;
import app.model.NotificationItem;
import app.model.PaymentRecord;
import app.model.TopUpRequestRecord;
import app.model.TransactionRecord;
import app.model.UserRole;
import app.service.AuctionPlatformService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.MessageListener;
import network.ServerConnection;
import shared.socket.RealtimeEvent;
import ui.AppUi;
import util.AlertUtil;
import util.SceneManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdminController implements MessageListener {
    private final SceneManager sceneManager;
    private final AuctionPlatformService service;
    private final ServerConnection serverConnection;
    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "admin-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean statsRefreshInProgress = new AtomicBoolean(false);
    private final AtomicBoolean tabRefreshInProgress = new AtomicBoolean(false);
    private Label subtitleLabel;
    private HBox statsRow;
    private TableView<TopUpRequestRecord> topUpTable;
    private TableView<AuctionLot> auctionTable;
    private TableView<AppUser> userTable;
    private TableView<PaymentRecord> paymentTable;
    private TableView<TransactionRecord> transactionTable;
    private TableView<NotificationItem> notificationTable;
    private TabPane tabs;
    @FXML
    private StackPane root;

    public AdminController(SceneManager sceneManager, ServerConnection serverConnection) {
        this.sceneManager = sceneManager;
        this.serverConnection = serverConnection;
        this.service = serverConnection.getService();
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
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setPadding(new Insets(24));

        Button marketButton = new Button("Cho dau gia");
        marketButton.setOnAction(event -> sceneManager.showAuctionList());

        Button sellerButton = new Button("Khu nguoi ban");
        sellerButton.getStyleClass().add("secondary-button");
        sellerButton.setOnAction(event -> sceneManager.showSellerDashboard());

        Button walletButton = new Button("Nap tien");
        walletButton.getStyleClass().add("secondary-button");
        walletButton.setOnAction(event -> sceneManager.showWallet());

        Button refreshButton = new Button("Lam moi");
        refreshButton.getStyleClass().add("secondary-button");

        Button logoutButton = new Button("Dang xuat");
        logoutButton.setOnAction(event -> sceneManager.logout());

        subtitleLabel = new Label("Dang tai du lieu admin...");
        subtitleLabel.getStyleClass().add("muted-label");

        statsRow = new HBox(14);
        statsRow.getStyleClass().add("stats-strip");

        topUpTable = buildTopUpRequestTable();
        auctionTable = buildAuctionTable();
        userTable = buildUserTable();
        paymentTable = buildPaymentTable();
        transactionTable = buildTransactionTable();
        notificationTable = buildNotificationTable();

        Label topUpHelper = new Label("Chon mot yeu cau dang PENDING roi bam xac nhan de cong tien vao vi nguoi dung.");
        topUpHelper.getStyleClass().add("muted-label");

        Button topUpRefreshButton = new Button("Lam moi danh sach");
        topUpRefreshButton.getStyleClass().add("secondary-button");

        Button approveButton = new Button("Xac nhan cong tien");
        approveButton.getStyleClass().add("primary-button");
        approveButton.setOnAction(event -> {
            TopUpRequestRecord selected = topUpTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.error("Chua chon yeu cau", "Hay chon mot yeu cau nap tien de xac nhan.");
                return;
            }
            try {
                service.approveTopUpRequest(selected.getId());
                AlertUtil.info("Xac nhan thanh cong", "So du nguoi dung da duoc cong sau khi admin xac nhan.");
                refreshStatsAsync(subtitleLabel, statsRow, true);
                refreshActiveTabAsync(null, topUpTable, auctionTable, userTable, paymentTable, transactionTable, notificationTable, false);
            } catch (Exception ex) {
                AlertUtil.error("Khong the xac nhan", ex.getMessage());
            }
        });

        VBox topUpPanel = new VBox(12, topUpHelper, topUpTable, new HBox(12, topUpRefreshButton, approveButton));

        Tab topUpTab = new Tab("Yeu cau nap tien", topUpPanel);
        topUpTab.setUserData(AdminTab.TOP_UP);
        Tab auctionTab = new Tab("Phien dau gia", auctionTable);
        auctionTab.setUserData(AdminTab.AUCTION);
        Tab userTab = new Tab("Nguoi dung", userTable);
        userTab.setUserData(AdminTab.USER);
        Tab paymentTab = new Tab("Thanh toan", paymentTable);
        paymentTab.setUserData(AdminTab.PAYMENT);
        Tab transactionTab = new Tab("Giao dich", transactionTable);
        transactionTab.setUserData(AdminTab.TRANSACTION);
        Tab notificationTab = new Tab("Thong bao", notificationTable);
        notificationTab.setUserData(AdminTab.NOTIFICATION);

        tabs = new TabPane(topUpTab, auctionTab, userTab, paymentTab, transactionTab, notificationTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) ->
                refreshActiveTabAsync(newTab, topUpTable, auctionTable, userTable, paymentTable, transactionTable, notificationTable, true)
        );

        refreshButton.setOnAction(event -> {
            refreshStatsAsync(subtitleLabel, statsRow, false);
            refreshActiveTabAsync(tabs.getSelectionModel().getSelectedItem(), topUpTable, auctionTable, userTable, paymentTable, transactionTable, notificationTable, false);
        });
        topUpRefreshButton.setOnAction(event ->
                refreshActiveTabAsync(topUpTab, topUpTable, auctionTable, userTable, paymentTable, transactionTable, notificationTable, false)
        );

        VBox center = new VBox(
                18,
                AppUi.pageHeader(
                        "Admin",
                        "Bang dieu hanh he thong",
                        "Theo doi user, thanh toan, giao dich va duyet yeu cau nap tien. Du lieu duoc cap nhat theo su kien server.",
                        marketButton,
                        sellerButton,
                        walletButton,
                        refreshButton,
                        logoutButton
                ),
                subtitleLabel,
                statsRow,
                AppUi.panelCard("Khu vuc van hanh", "Chi tai du lieu tab dang mo de giam do tre tren giao dien.", tabs)
        );
        VBox.setVgrow(tabs, Priority.ALWAYS);

        shell.setCenter(center);
        refreshStatsAsync(subtitleLabel, statsRow, false);
        refreshActiveTabAsync(topUpTab, topUpTable, auctionTable, userTable, paymentTable, transactionTable, notificationTable, false);
        return shell;
    }

    private void refreshStatsAsync(Label subtitleLabel, HBox statsRow, boolean silent) {
        if (!statsRefreshInProgress.compareAndSet(false, true)) {
            return;
        }

        refreshExecutor.submit(() -> {
            try {
                AdminStatsSnapshot snapshot = loadStatsSnapshot();
                Platform.runLater(() -> applyStatsSnapshot(subtitleLabel, statsRow, snapshot));
            } catch (Exception ex) {
                if (!silent) {
                    Platform.runLater(() -> AlertUtil.error("Khong the tai thong ke admin", ex.getMessage()));
                }
            } finally {
                statsRefreshInProgress.set(false);
            }
        });
    }

    private void refreshActiveTabAsync(
            Tab selectedTab,
            TableView<TopUpRequestRecord> topUpTable,
            TableView<AuctionLot> auctionTable,
            TableView<AppUser> userTable,
            TableView<PaymentRecord> paymentTable,
            TableView<TransactionRecord> transactionTable,
            TableView<NotificationItem> notificationTable,
            boolean silent
    ) {
        if (!tabRefreshInProgress.compareAndSet(false, true)) {
            return;
        }

        AdminTab tab = selectedTab == null ? AdminTab.TOP_UP : (AdminTab) selectedTab.getUserData();

        refreshExecutor.submit(() -> {
            try {
                switch (tab) {
                    case TOP_UP -> Platform.runLater(() -> topUpTable.getItems().setAll(List.copyOf(service.getTopUpRequests())));
                    case AUCTION -> Platform.runLater(() -> auctionTable.getItems().setAll(List.copyOf(service.getAuctions())));
                    case USER -> Platform.runLater(() -> userTable.getItems().setAll(List.copyOf(service.getUsers())));
                    case PAYMENT -> Platform.runLater(() -> paymentTable.getItems().setAll(List.copyOf(service.getPayments())));
                    case TRANSACTION -> Platform.runLater(() -> transactionTable.getItems().setAll(List.copyOf(service.getTransactions())));
                    case NOTIFICATION -> Platform.runLater(() -> notificationTable.getItems().setAll(List.copyOf(service.getNotifications())));
                }
            } catch (Exception ex) {
                if (!silent) {
                    Platform.runLater(() -> AlertUtil.error("Khong the tai du lieu tab admin", ex.getMessage()));
                }
            } finally {
                tabRefreshInProgress.set(false);
            }
        });
    }

    private AdminStatsSnapshot loadStatsSnapshot() {
        List<AuctionLot> auctions = List.copyOf(service.getAuctions());
        List<AppUser> users = List.copyOf(service.getUsers());
        List<NotificationItem> notifications = List.copyOf(service.getNotifications());

        long openCount = auctions.stream().filter(auction -> !auction.isClosed()).count();
        long finishedCount = auctions.stream().filter(AuctionLot::isClosed).count();
        long sellers = users.stream().filter(user -> user.getRole() == UserRole.SELLER).count();
        long bidders = users.stream().filter(user -> user.getRole() == UserRole.BIDDER).count();
        double totalVolume = auctions.stream()
                .map(AuctionLot::getHighestBid)
                .filter(Objects::nonNull)
                .mapToDouble(bid -> bid.getAmount())
                .sum();
        long paidCount = auctions.stream().filter(AuctionLot::isPaid).count();
        long autoBidCount = auctions.stream().mapToLong(auction -> auction.getAutoBidRules().size()).sum();

        return new AdminStatsSnapshot(
                openCount,
                finishedCount,
                sellers,
                bidders,
                totalVolume,
                paidCount,
                autoBidCount,
                notifications.size()
        );
    }

    private void applyStatsSnapshot(Label subtitleLabel, HBox statsRow, AdminStatsSnapshot snapshot) {
        subtitleLabel.setText("Dashboard admin dang nhan cap nhat realtime tu server. Chi tab dang mo moi tai lai du lieu.");
        statsRow.getChildren().setAll(
                AppUi.statCard("Phien mo", String.valueOf(snapshot.openCount()), "Phien dang hoat dong"),
                AppUi.statCard("Phien dong", String.valueOf(snapshot.finishedCount()), "Phien da ket thuc"),
                AppUi.statCard("Nguoi ban", String.valueOf(snapshot.sellers()), "Tai khoan seller"),
                AppUi.statCard("Nguoi mua", String.valueOf(snapshot.bidders()), "Tai khoan bidder"),
                AppUi.statCard("GMV", service.formatCurrency(snapshot.totalVolume()), "Tong gia tri giao dich"),
                AppUi.statCard("Da thanh toan", String.valueOf(snapshot.paidCount()), "Phien da tra tien"),
                AppUi.statCard("Auto bid", String.valueOf(snapshot.autoBidCount()), "Quy tac tu dong"),
                AppUi.statCard("Thong bao", String.valueOf(snapshot.notificationCount()), "Tong thong bao")
        );
    }

    private TableView<TopUpRequestRecord> buildTopUpRequestTable() {
        TableView<TopUpRequestRecord> table = new TableView<>();

        TableColumn<TopUpRequestRecord, String> idColumn = new TableColumn<>("Ma yeu cau");
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        idColumn.setPrefWidth(140);

        TableColumn<TopUpRequestRecord, String> userColumn = new TableColumn<>("Nguoi dung");
        userColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        userColumn.setPrefWidth(120);

        TableColumn<TopUpRequestRecord, String> amountColumn = new TableColumn<>("So tien");
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(service.formatCurrency(data.getValue().getAmount())));
        amountColumn.setPrefWidth(150);

        TableColumn<TopUpRequestRecord, String> bankColumn = new TableColumn<>("Ngan hang");
        bankColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBankName()));
        bankColumn.setPrefWidth(120);

        TableColumn<TopUpRequestRecord, String> accountColumn = new TableColumn<>("Tai khoan nhan");
        accountColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAccountNumber()));
        accountColumn.setPrefWidth(140);

        TableColumn<TopUpRequestRecord, String> statusColumn = new TableColumn<>("Trang thai");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusColumn.setPrefWidth(120);

        TableColumn<TopUpRequestRecord, String> requestedAtColumn = new TableColumn<>("Thoi gian gui");
        requestedAtColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRequestedAt().toString().replace('T', ' ')));
        requestedAtColumn.setPrefWidth(180);

        table.getColumns().setAll(idColumn, userColumn, amountColumn, bankColumn, accountColumn, statusColumn, requestedAtColumn);
        return table;
    }

    private TableView<AuctionLot> buildAuctionTable() {
        TableView<AuctionLot> table = new TableView<>();

        TableColumn<AuctionLot, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        idColumn.setPrefWidth(130);

        TableColumn<AuctionLot, String> titleColumn = new TableColumn<>("San pham");
        titleColumn.setCellValueFactory(data -> data.getValue().titleProperty());
        titleColumn.setPrefWidth(230);

        TableColumn<AuctionLot, String> sellerColumn = new TableColumn<>("Nguoi ban");
        sellerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSellerUsername()));
        sellerColumn.setPrefWidth(120);

        TableColumn<AuctionLot, String> priceColumn = new TableColumn<>("Gia");
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(service.formatCurrency(data.getValue().getCurrentPrice())));
        priceColumn.setPrefWidth(150);

        TableColumn<AuctionLot, String> statusColumn = new TableColumn<>("Trang thai");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
        statusColumn.setPrefWidth(120);

        table.getColumns().setAll(idColumn, titleColumn, sellerColumn, priceColumn, statusColumn);
        return table;
    }

    private TableView<AppUser> buildUserTable() {
        TableView<AppUser> table = new TableView<>();

        TableColumn<AppUser, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        idColumn.setPrefWidth(120);

        TableColumn<AppUser, String> usernameColumn = new TableColumn<>("Ten dang nhap");
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        usernameColumn.setPrefWidth(180);

        TableColumn<AppUser, String> passwordColumn = new TableColumn<>("Mat khau");
        passwordColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPassword()));
        passwordColumn.setPrefWidth(160);

        TableColumn<AppUser, String> nameColumn = new TableColumn<>("Ho va ten");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        nameColumn.setPrefWidth(220);

        TableColumn<AppUser, String> roleColumn = new TableColumn<>("Vai tro");
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole().name()));
        roleColumn.setPrefWidth(120);

        table.getColumns().setAll(idColumn, usernameColumn, passwordColumn, nameColumn, roleColumn);
        return table;
    }

    private TableView<PaymentRecord> buildPaymentTable() {
        TableView<PaymentRecord> table = new TableView<>();

        TableColumn<PaymentRecord, String> auctionColumn = new TableColumn<>("Auction");
        auctionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuctionId()));
        auctionColumn.setPrefWidth(140);

        TableColumn<PaymentRecord, String> buyerColumn = new TableColumn<>("Buyer");
        buyerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBuyerUsername()));
        buyerColumn.setPrefWidth(120);

        TableColumn<PaymentRecord, String> sellerColumn = new TableColumn<>("Seller");
        sellerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSellerUsername()));
        sellerColumn.setPrefWidth(120);

        TableColumn<PaymentRecord, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(service.formatCurrency(data.getValue().getAmount())));
        amountColumn.setPrefWidth(160);

        TableColumn<PaymentRecord, String> paidAtColumn = new TableColumn<>("Paid At");
        paidAtColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaidAt().toString().replace('T', ' ')));
        paidAtColumn.setPrefWidth(220);

        table.getColumns().setAll(auctionColumn, buyerColumn, sellerColumn, amountColumn, paidAtColumn);
        return table;
    }

    private TableView<TransactionRecord> buildTransactionTable() {
        TableView<TransactionRecord> table = new TableView<>();

        TableColumn<TransactionRecord, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        typeColumn.setPrefWidth(140);

        TableColumn<TransactionRecord, String> actorColumn = new TableColumn<>("Actor");
        actorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getActorUsername()));
        actorColumn.setPrefWidth(140);

        TableColumn<TransactionRecord, String> refColumn = new TableColumn<>("Ref");
        refColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReferenceId()));
        refColumn.setPrefWidth(140);

        TableColumn<TransactionRecord, String> descColumn = new TableColumn<>("Description");
        descColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        descColumn.setPrefWidth(360);

        TableColumn<TransactionRecord, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTime().toString().replace('T', ' ')));
        timeColumn.setPrefWidth(220);

        table.getColumns().setAll(typeColumn, actorColumn, refColumn, descColumn, timeColumn);
        return table;
    }

    private TableView<NotificationItem> buildNotificationTable() {
        TableView<NotificationItem> table = new TableView<>();

        TableColumn<NotificationItem, String> userColumn = new TableColumn<>("User");
        userColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        userColumn.setPrefWidth(120);

        TableColumn<NotificationItem, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        titleColumn.setPrefWidth(180);

        TableColumn<NotificationItem, String> messageColumn = new TableColumn<>("Message");
        messageColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMessage()));
        messageColumn.setPrefWidth(420);

        TableColumn<NotificationItem, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTime().toString().replace('T', ' ')));
        timeColumn.setPrefWidth(220);

        table.getColumns().setAll(userColumn, titleColumn, messageColumn, timeColumn);
        return table;
    }

    @Override
    public void onMessage(RealtimeEvent event) {
        if (root == null || root.getChildren().isEmpty() || tabs == null) {
            return;
        }
        refreshStatsAsync(subtitleLabel, statsRow, true);
        refreshActiveTabAsync(
                tabs.getSelectionModel().getSelectedItem(),
                topUpTable,
                auctionTable,
                userTable,
                paymentTable,
                transactionTable,
                notificationTable,
                true
        );
    }

    private enum AdminTab {
        TOP_UP,
        AUCTION,
        USER,
        PAYMENT,
        TRANSACTION,
        NOTIFICATION
    }

    private record AdminStatsSnapshot(
            long openCount,
            long finishedCount,
            long sellers,
            long bidders,
            double totalVolume,
            long paidCount,
            long autoBidCount,
            int notificationCount
    ) {
    }
}
