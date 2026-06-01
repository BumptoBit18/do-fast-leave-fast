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

import java.io.File;
import java.util.List;
import network.MessageListener;
import network.ServerConnection;
import shared.socket.RealtimeEvent;
import ui.AppUi;
import util.AlertUtil;
import util.ProductImageUtil;
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

        Button fillFormButton = new Button("Nap vao form");
        fillFormButton.getStyleClass().add("secondary-button");
        fillFormButton.setOnAction(event -> {
            if (sellerTable.getSelectionModel().getSelectedItem() == null) {
                AlertUtil.error("Chua chon phien", "Hay chon mot phien de sua.");
            }
        });

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

        Button deleteButton = new Button("Xoa han phien da chon");
        deleteButton.getStyleClass().add("secondary-button");
        deleteButton.setOnAction(event -> {
            AuctionLot selected = sellerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.error("Chua chon phien", "Hay chon mot phien de xoa.");
                return;
            }
            try {
                service.deleteAuction(selected);
                AlertUtil.info("Da xoa phien", "Phien dau gia da duoc xoa khoi he thong.");
                sceneManager.showSellerDashboard();
            } catch (Exception ex) {
                AlertUtil.error("Khong the xoa phien", ex.getMessage());
            }
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
                fillFormButton,
                cancelButton,
                deleteButton
        );
        VBox.setVgrow(sellerTable, Priority.ALWAYS);

        VBox form = buildCreateForm(sellerTable, fillFormButton);

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

        table.getColumns().setAll(List.of(titleColumn, priceColumn, statusColumn, endColumn));
        return table;
    }

    private VBox buildCreateForm(TableView<AuctionLot> sellerTable, Button fillFormButton) {
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

        final String[] selectedImagePayload = {""};
        Label imageState = new Label("Chua chon anh san pham");
        imageState.getStyleClass().add("muted-label");

        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(320);
        imagePreview.setFitHeight(180);
        imagePreview.setPreserveRatio(true);
        imagePreview.setSmooth(true);
        imagePreview.setVisible(false);
        imagePreview.setManaged(false);

        Button chooseImageButton = new Button("Chon anh tu may");
        chooseImageButton.getStyleClass().add("secondary-button");
        chooseImageButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Chon anh san pham");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Anh san pham (*.png, *.jpg, *.jpeg, *.gif, *.bmp)",
                    "*.png",
                    "*.jpg",
                    "*.jpeg",
                    "*.gif",
                    "*.bmp"
            ));
            File selectedFile = chooser.showOpenDialog(root.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }
            try {
                selectedImagePayload[0] = ProductImageUtil.toDataUri(selectedFile.toPath());
                imageState.setText("Da chon: " + selectedFile.getName());
                updateImagePreview(imagePreview, selectedImagePayload[0]);
            } catch (Exception ex) {
                AlertUtil.error("Khong doc duoc anh", ex.getMessage());
            }
        });

        Button removeImageButton = new Button("Xoa anh da chon");
        removeImageButton.getStyleClass().add("secondary-button");
        removeImageButton.setOnAction(event -> {
            selectedImagePayload[0] = "";
            imageState.setText("Chua chon anh san pham");
            updateImagePreview(imagePreview, "");
        });

        VBox imagePicker = new VBox(8, imagePreview, imageState, new HBox(8, chooseImageButton, removeImageButton));

        TextArea description = new TextArea();
        description.setPromptText("Mo ta chi tiet tinh trang, phu kien, xuat xu...");
        description.setPrefRowCount(6);

        Label editorState = new Label("Dang tao phien moi");
        editorState.getStyleClass().add("muted-label");

        final AuctionLot[] editingAuction = {null};

        Runnable resetForm = () -> {
            editingAuction[0] = null;
            editorState.setText("Dang tao phien moi");
            itemName.clear();
            category.setValue("Electronics");
            startPrice.clear();
            duration.getValueFactory().setValue(24);
            description.clear();
            selectedImagePayload[0] = "";
            imageState.setText("Chua chon anh san pham");
            updateImagePreview(imagePreview, "");
        };

        Runnable loadSelectedAuction = () -> {
            AuctionLot selected = sellerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.error("Chua chon phien", "Hay chon mot phien de dua vao form.");
                return;
            }
            editingAuction[0] = selected;
            editorState.setText("Dang sua phien: " + selected.getId());
            itemName.setText(selected.getTitle());
            category.setValue(selected.getCategory());
            startPrice.setText(String.valueOf((long) selected.getStartPrice()));
            long hoursLeft = java.time.Duration.between(java.time.LocalDateTime.now(), selected.getEndTime()).toHours();
            duration.getValueFactory().setValue((int) Math.max(6, Math.min(168, hoursLeft <= 0 ? 24 : hoursLeft)));
            description.setText(selected.getDescription());
            Image existingImage = ProductImageUtil.decode(selected.getImageHint());
            selectedImagePayload[0] = existingImage == null ? "" : selected.getImageHint();
            imageState.setText(existingImage == null ? "Phien nay chua co anh san pham" : "Dang dung anh san pham hien tai");
            updateImagePreview(imagePreview, selectedImagePayload[0]);
        };

        fillFormButton.setOnAction(event -> loadSelectedAuction.run());

        Button createButton = new Button("Dang phien ngay");
        createButton.getStyleClass().add("primary-button");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> {
            try {
                if (editingAuction[0] == null) {
                    service.createAuction(
                            service.getCurrentUser().getUsername(),
                            itemName.getText().trim(),
                            category.getValue(),
                            description.getText().trim(),
                            Double.parseDouble(startPrice.getText().trim()),
                            duration.getValue(),
                            selectedImagePayload[0]
                    );
                    AlertUtil.info("Tao thanh cong", "Phien dau gia moi da duoc tao.");
                } else {
                    service.updateAuction(
                            editingAuction[0],
                            itemName.getText().trim(),
                            category.getValue(),
                            description.getText().trim(),
                            Double.parseDouble(startPrice.getText().trim()),
                            duration.getValue(),
                            selectedImagePayload[0]
                    );
                    AlertUtil.info("Cap nhat thanh cong", "Phien dau gia da duoc cap nhat.");
                }
                resetForm.run();
                sceneManager.showSellerDashboard();
            } catch (Exception ex) {
                AlertUtil.error("Khong luu duoc phien", "Kiem tra du lieu dau vao. " + ex.getMessage());
            }
        });

        Button resetButton = new Button("Tao phien moi");
        resetButton.getStyleClass().add("secondary-button");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.setOnAction(event -> resetForm.run());

        form.getChildren().addAll(
                editorState,
                AppUi.fieldGroup("Ten san pham", "Day la ten chinh hien thi cho nguoi mua trong danh sach dau gia.", itemName),
                AppUi.fieldGroup("Danh muc", "Chon nhom phu hop de nguoi mua loc san pham de hon.", category),
                AppUi.fieldGroup("Gia khoi diem", "Nhap muc gia ban dau cho phien dau gia, chua bao gom cac lan dat tiep theo.", startPrice),
                AppUi.fieldGroup("Thoi luong phien (gio)", "Chon so gio phien dau gia se mo truoc khi tu dong ket thuc.", duration),
                AppUi.fieldGroup("Anh san pham", "Chon file PNG, JPG, GIF hoac BMP tu may. Kich thuoc toi da 2 MB.", imagePicker),
                AppUi.fieldGroup("Mo ta chi tiet", "Ghi ro tinh trang san pham, phu kien kem theo va cac luu y can thiet.", description),
                createButton,
                resetButton
        );
        return form;
    }

    private void updateImagePreview(ImageView preview, String payload) {
        Image image = ProductImageUtil.decode(payload);
        preview.setImage(image);
        preview.setVisible(image != null);
        preview.setManaged(image != null);
    }

    @Override
    public void onMessage(RealtimeEvent event) {
        if (root == null) {
            return;
        }
        Platform.runLater(() -> root.getChildren().setAll(buildView()));
    }
}
