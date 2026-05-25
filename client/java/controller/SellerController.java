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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;
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

        // --- Image picker ---
        final String[] selectedImageBase64 = {null};

        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(340);
        imagePreview.setFitHeight(180);
        imagePreview.setPreserveRatio(true);
        imagePreview.setStyle("-fx-background-color: #f0f0f0;");
        imagePreview.setVisible(false);
        imagePreview.setManaged(false);

        Label imageLabel = new Label("Chua chon anh");
        imageLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        Button chooseImageButton = new Button("Chon anh tu may tinh");
        chooseImageButton.setMaxWidth(Double.MAX_VALUE);
        chooseImageButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chon anh san pham");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Anh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
            );
            File file = fileChooser.showOpenDialog(root.getScene().getWindow());
            if (file != null) {
                try (FileInputStream fis = new FileInputStream(file);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        baos.write(buffer, 0, read);
                    }
                    String ext = file.getName().toLowerCase();
                    String mime = ext.endsWith(".png") ? "image/png"
                            : ext.endsWith(".gif") ? "image/gif"
                            : "image/jpeg";
                    selectedImageBase64[0] = "data:" + mime + ";base64,"
                            + Base64.getEncoder().encodeToString(baos.toByteArray());
                    Image img = new Image(file.toURI().toString(), true);
                    imagePreview.setImage(img);
                    imagePreview.setVisible(true);
                    imagePreview.setManaged(true);
                    imageLabel.setText(file.getName());
                } catch (Exception ex) {
                    AlertUtil.error("Loi doc anh", "Khong the doc file anh: " + ex.getMessage());
                }
            }
        });

        Button clearImageButton = new Button("Xoa anh");
        clearImageButton.setOnAction(event -> {
            selectedImageBase64[0] = null;
            imagePreview.setImage(null);
            imagePreview.setVisible(false);
            imagePreview.setManaged(false);
            imageLabel.setText("Chua chon anh");
        });

        HBox imageButtons = new HBox(8, chooseImageButton, clearImageButton);
        HBox.setHgrow(chooseImageButton, Priority.ALWAYS);

        VBox imageBox = new VBox(6, imageButtons, imageLabel, imagePreview);
        // --- End image picker ---

        TextArea description = new TextArea();
        description.setPromptText("Mo ta chi tiet tinh trang, phu kien, xuat xu...");
        description.setPrefRowCount(6);

        Button createButton = new Button("Dang phien ngay");
        createButton.getStyleClass().add("primary-button");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> {
            try {
                String imageData = selectedImageBase64[0] != null ? selectedImageBase64[0] : "";
                service.createAuction(
                        service.getCurrentUser().getUsername(),
                        itemName.getText().trim(),
                        category.getValue(),
                        description.getText().trim(),
                        Double.parseDouble(startPrice.getText().trim()),
                        duration.getValue(),
                        imageData
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
                AppUi.fieldGroup("Thoi luong phien ", "Chon so gio phien dau gia se mo truoc khi tu dong ket thuc.", duration),
                AppUi.fieldGroup("Hinh anh san pham", "Chon anh tu may tinh de nguoi mua thay duoc hinh anh truc quan.", imageBox),
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