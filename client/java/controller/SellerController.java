package controller;

import app.model.AppUser;
import app.model.AuctionLot;
import app.service.AuctionPlatformService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

public class SellerController implements MessageListener {
    private final SceneManager sceneManager;
    private final ServerConnection serverConnection;
    private final AuctionPlatformService service;
    @FXML
    private StackPane root;

    public SellerController(SceneManager sceneManager, ServerConnection serverConnection) {
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
        AppUser currentUser = service.getCurrentUser();

        Button marketButton = new Button("Cho dau gia");
        marketButton.setOnAction(event -> sceneManager.showAuctionList());

        Button adminButton = new Button("He thong admin");
        adminButton.getStyleClass().add("secondary-button");
        adminButton.setVisible(currentUser.getRole().name().equals("ADMIN"));
        adminButton.setManaged(currentUser.getRole().name().equals("ADMIN"));
        adminButton.setOnAction(event -> sceneManager.showAdminPanel());

        Button logoutButton = new Button("Dang xuat");
        logoutButton.setOnAction(event -> sceneManager.logout());

        TableView<AuctionLot> sellerTable = buildSellerTable();
        sellerTable.getItems().setAll(service.getAuctionsForSeller(currentUser.getUsername()));

        Button cancelButton = new Button("Huy phien da chon");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(event -> {
            AuctionLot selected = sellerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.error("Chua chon phien", "Hay chon mot phien de huy.");
                return;
            }
            service.cancelAuction(selected);
            sceneManager.showSellerDashboard();
        });

        ListView<String> sellerNotifications = new ListView<>();
        sellerNotifications.setPrefHeight(170);
        sellerNotifications.getItems().setAll(
                service.getNotificationsForCurrentUser().stream()
                        .limit(6)
                        .map(item -> item.getTitle() + " - " + item.getMessage())
                        .toList()
        );

        VBox left = AppUi.panelCard(
                "Bang quan ly gian hang",
                "Theo doi cac lo dang ban, so du vi va thong bao danh cho nguoi ban.",
                AppUi.statCard("Tong lo", String.valueOf(sellerTable.getItems().size()), "Danh sach hien tai"),
                AppUi.statCard("So du vi", service.formatCurrency(currentUser.getWalletBalance()), "Tien co the nhan hoac su dung"),
                sellerTable,
                new Label("Thong bao gan day"),
                sellerNotifications,
                cancelButton
        );
        VBox.setVgrow(sellerTable, Priority.ALWAYS);

        VBox form = buildCreateForm();

        HBox content = new HBox(20, left, form);
        HBox.setHgrow(left, Priority.ALWAYS);

        shell.setCenter(new VBox(
                18,
                AppUi.pageHeader(
                        "Nguoi ban",
                        "Khu vuc quan ly gian hang",
                        "Tao lo moi, theo doi hang dang ban va xu ly thong bao giao dich.",
                        AppUi.badge("Gian hang"),
                        AppUi.badge("Vi tien"),
                        marketButton,
                        adminButton,
                        logoutButton
                ),
                content
        ));
        return shell;
    }

    private TableView<AuctionLot> buildSellerTable() {
        TableView<AuctionLot> table = new TableView<>();

        TableColumn<AuctionLot, String> titleColumn = new TableColumn<>("San pham");
        titleColumn.setCellValueFactory(data -> data.getValue().titleProperty());
        titleColumn.setPrefWidth(220);

        TableColumn<AuctionLot, String> priceColumn = new TableColumn<>("Gia hien tai");
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(service.formatCurrency(data.getValue().getCurrentPrice())));
        priceColumn.setPrefWidth(140);

        TableColumn<AuctionLot, String> statusColumn = new TableColumn<>("Trang thai");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
        statusColumn.setPrefWidth(110);

        TableColumn<AuctionLot, String> endColumn = new TableColumn<>("Thoi gian con lai");
        endColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimeLeftLabel()));
        endColumn.setPrefWidth(140);

        table.getColumns().addAll(titleColumn, priceColumn, statusColumn, endColumn);
        return table;
    }

    private VBox buildCreateForm() {
        VBox form = AppUi.panelCard("Tao phien dau gia moi", "Dien day du thong tin san pham truoc khi dang len he thong.");
        form.setPrefWidth(380);

        TextField itemName = new TextField();
        itemName.setPromptText("Vi du: MacBook Pro M3");

        ComboBox<String> category = new ComboBox<>();
        category.getItems().addAll("Electronics", "Vehicle", "Art", "Collectible", "Luxury");
        category.setValue("Electronics");
        category.setMaxWidth(Double.MAX_VALUE);

        TextField startPrice = new TextField();
        startPrice.setPromptText("Vi du: 25000000");

        Spinner<Integer> duration = new Spinner<>(6, 168, 24);
        duration.setEditable(true);

        TextField imageHint = new TextField();
        imageHint.setPromptText("Vi du: Laptop mau bac, con moi");

        TextArea description = new TextArea();
        description.setPromptText("Mo ta chi tiet tinh trang, phu kien, xuat xu...");
        description.setPrefRowCount(6);

        Button createButton = new Button("Dang phien ngay");
        createButton.getStyleClass().add("primary-button");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> {
            try {
                service.createAuction(
                        service.getCurrentUser().getUsername(),
                        itemName.getText().trim(),
                        category.getValue(),
                        description.getText().trim(),
                        Double.parseDouble(startPrice.getText().trim()),
                        duration.getValue(),
                        imageHint.getText().trim()
                );
                AlertUtil.info("Tao thanh cong", "Phien dau gia moi da duoc tao.");
                sceneManager.showSellerDashboard();
            } catch (Exception ex) {
                AlertUtil.error("Khong tao duoc phien", "Kiem tra du lieu dau vao. " + ex.getMessage());
            }
        });

        form.getChildren().addAll(
                AppUi.fieldGroup("Ten san pham", "Day la ten chinh hien thi cho nguoi mua trong danh sach dau gia.", itemName),
                AppUi.fieldGroup("Danh muc", "Chon nhom phu hop de nguoi mua loc san pham de hon.", category),
                AppUi.fieldGroup("Gia khoi diem", "Nhap muc gia ban dau cho phien dau gia, chua bao gom cac lan dat tiep theo.", startPrice),
                AppUi.fieldGroup("Thoi luong phien (gio)", "Chon so gio phien dau gia se mo truoc khi tu dong ket thuc.", duration),
                AppUi.fieldGroup("Goi y hinh anh", "Mo ta ngan ve hinh anh hoac dien mao san pham de he thong hien thi ngu canh tot hon.", imageHint),
                AppUi.fieldGroup("Mo ta chi tiet", "Ghi ro tinh trang san pham, phu kien kem theo va cac luu y can thiet.", description),
                createButton
        );
        return form;
    }

    @Override
    public void onMessage(RealtimeEvent event) {
        if (root == null) {
            return;
        }
        Platform.runLater(() -> root.getChildren().setAll(buildView()));
    }
}
