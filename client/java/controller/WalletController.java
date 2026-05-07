package controller;

import app.model.AppUser;
import app.model.NotificationItem;
import app.service.AuctionPlatformService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import network.ServerConnection;
import ui.AppUi;
import util.AlertUtil;
import util.SceneManager;

import java.util.List;

public class WalletController {
    private static final String QR_IMAGE_URL = "https://i.postimg.cc/hvGBx7Zc/778942ef-f12e-4d94-8ef6-21f06a49242d.jpg";
    private static final String BANK_NAME = "MB Bank";
    private static final String ACCOUNT_NAME = "NGUYEN TRONG HUNG";
    private static final String ACCOUNT_NUMBER = "0399858007";
    private static final Duration AUTO_REFRESH_INTERVAL = Duration.seconds(6);

    private final SceneManager sceneManager;
    private final AuctionPlatformService service;
    private double lastKnownBalance = Double.NaN;

    public WalletController(SceneManager sceneManager, ServerConnection serverConnection) {
        this.sceneManager = sceneManager;
        this.service = serverConnection.getService();
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setPadding(new Insets(24));

        AppUser currentUser = service.getCurrentUser();

        Button marketButton = new Button("Chợ đấu giá");
        marketButton.setOnAction(event -> sceneManager.showAuctionList());

        Button sellerButton = new Button("Khu người bán");
        sellerButton.getStyleClass().add("secondary-button");
        boolean canOpenSeller = currentUser.getRole().name().equals("SELLER") || currentUser.getRole().name().equals("ADMIN");
        sellerButton.setVisible(canOpenSeller);
        sellerButton.setManaged(canOpenSeller);
        sellerButton.setOnAction(event -> sceneManager.showSellerDashboard());

        Button adminButton = new Button("Quản trị hệ thống");
        adminButton.getStyleClass().add("secondary-button");
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        adminButton.setVisible(isAdmin);
        adminButton.setManaged(isAdmin);
        adminButton.setOnAction(event -> sceneManager.showAdminPanel());

        Button refreshButton = new Button("Làm mới");
        refreshButton.getStyleClass().add("secondary-button");

        Button logoutButton = new Button("Đăng xuất");
        logoutButton.setOnAction(event -> sceneManager.logout());

        Label balanceValue = new Label();
        balanceValue.getStyleClass().add("stat-value");
        Label roleValue = new Label();
        roleValue.getStyleClass().add("muted-label");
        Label userValue = new Label();
        userValue.getStyleClass().add("muted-label");

        ListView<String> notificationList = new ListView<>();
        notificationList.setPrefHeight(180);

        VBox accountPanel = AppUi.panelCard(
                "Ví của bạn",
                "Số dư sẽ tự cập nhật mỗi 6 giây hoặc ngay khi bạn bấm Làm mới.",
                AppUi.fieldGroup("Số dư hiện tại", "Cập nhật sau khi admin xác nhận yêu cầu nạp tiền.", balanceValue),
                AppUi.fieldGroup("Vai trò", "Quyền truy cập hiện tại.", roleValue),
                AppUi.fieldGroup("Người dùng", "Chủ tài khoản đang đăng nhập.", userValue),
                new Label("Thông báo gần đây"),
                notificationList,
                refreshButton
        );
        accountPanel.setPrefWidth(360);

        ComboBox<String> paymentMethodBox = new ComboBox<>();
        paymentMethodBox.getItems().add("Chuyển khoản ngân hàng");
        paymentMethodBox.setValue("Chuyển khoản ngân hàng");
        paymentMethodBox.setMaxWidth(Double.MAX_VALUE);

        TextField amountField = new TextField();
        amountField.setPromptText("Ví dụ: 500000");

        Button confirmTopUpButton = new Button("Tôi đã chuyển khoản, gửi yêu cầu xác nhận");
        confirmTopUpButton.getStyleClass().add("primary-button");
        confirmTopUpButton.setMaxWidth(Double.MAX_VALUE);
        confirmTopUpButton.setOnAction(event -> {
            try {
                double amount = Double.parseDouble(amountField.getText().trim());
                service.submitTopUpRequest(amount, BANK_NAME, ACCOUNT_NAME, ACCOUNT_NUMBER);
                service.markCurrentNotificationsSeen();
                AlertUtil.info(
                        "Đã gửi yêu cầu nạp tiền",
                        "Yêu cầu của bạn đã được gửi tới quản trị viên. Khi admin duyệt, số dư sẽ tự cập nhật và hệ thống sẽ hiển thị thông báo."
                );
                refreshWalletView(balanceValue, roleValue, userValue, notificationList, false);
            } catch (NumberFormatException ex) {
                AlertUtil.error("Số tiền không hợp lệ", "Hãy nhập số tiền hợp lệ trước khi gửi yêu cầu nạp tiền.");
            } catch (Exception ex) {
                AlertUtil.error("Không thể gửi yêu cầu", ex.getMessage());
            }
        });

        VBox transferPanel = AppUi.panelCard(
                "Nạp tiền vào ví",
                "Chọn phương thức nạp, quét mã QR, chuyển khoản và gửi yêu cầu để admin xác nhận. Dữ liệu ví tự làm mới mỗi 6 giây.",
                AppUi.fieldGroup("Phương thức nạp", "Hiện tại hệ thống hỗ trợ nạp qua chuyển khoản ngân hàng.", paymentMethodBox),
                AppUi.fieldGroup("Số tiền muốn nạp", "Nhập đúng số tiền bạn đã hoặc sẽ chuyển khoản.", amountField),
                buildQrView(),
                buildBankInfo(),
                confirmTopUpButton
        );
        HBox.setHgrow(transferPanel, Priority.ALWAYS);

        HBox body = new HBox(20, accountPanel, transferPanel);
        body.setAlignment(Pos.TOP_LEFT);

        refreshButton.setOnAction(event -> refreshWalletView(balanceValue, roleValue, userValue, notificationList, true));
        refreshWalletView(balanceValue, roleValue, userValue, notificationList, false);

        root.setCenter(new VBox(
                18,
                AppUi.pageHeader(
                        "Ví tiền",
                        "Nạp tiền vào tài khoản",
                        "Sau khi bạn gửi yêu cầu, quản trị viên phải duyệt thì số dư mới được cộng. Hệ thống tự làm mới mỗi 6 giây.",
                        AppUi.badge("Chờ duyệt"),
                        AppUi.badge("QR ngân hàng"),
                        marketButton,
                        sellerButton,
                        adminButton,
                        refreshButton,
                        logoutButton
                ),
                body
        ));

        attachAutoRefresh(root, AUTO_REFRESH_INTERVAL, () -> refreshWalletView(balanceValue, roleValue, userValue, notificationList, true));
        return root;
    }

    private void refreshWalletView(
            Label balanceValue,
            Label roleValue,
            Label userValue,
            ListView<String> notificationList,
            boolean showPopupOnApproval
    ) {
        AppUser refreshedUser = service.getCurrentUser();
        double previousBalance = lastKnownBalance;
        double currentBalance = refreshedUser.getWalletBalance();

        balanceValue.setText(service.formatCurrency(currentBalance));
        roleValue.setText(refreshedUser.getRole().name());
        userValue.setText(refreshedUser.getFullName());

        List<NotificationItem> notifications = service.getNotificationsForCurrentUser();
        notificationList.getItems().setAll(
                notifications.stream()
                        .limit(6)
                        .map(item -> item.getTitle() + " - " + item.getMessage())
                        .toList()
        );

        List<NotificationItem> newNotifications = service.getNewNotificationsForCurrentUser();
        if (showPopupOnApproval) {
            newNotifications.stream()
                    .filter(this::isApprovalNotification)
                    .findFirst()
                    .ifPresentOrElse(
                            item -> AlertUtil.info("Nạp tiền thành công", item.getMessage()),
                            () -> {
                                if (!Double.isNaN(previousBalance) && currentBalance > previousBalance) {
                                    AlertUtil.info(
                                            "Nạp tiền thành công",
                                            "Số dư ví của bạn đã tăng lên " + service.formatCurrency(currentBalance) + "."
                                    );
                                }
                            }
                    );
        }

        lastKnownBalance = currentBalance;
    }

    private Parent buildQrView() {
        Image qrImage = new Image(QR_IMAGE_URL, true);
        if (qrImage.isError()) {
            return AppUi.panelCard(
                    "Ảnh QR chưa sẵn sàng",
                    "Không thể tải ảnh QR từ đường link đã cung cấp. Hãy kiểm tra lại link công khai của ảnh."
            );
        }

        ImageView imageView = new ImageView(qrImage);
        imageView.setFitWidth(340);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("qr-image");

        Label qrLabel = new Label("Quét mã QR để chuyển khoản");
        qrLabel.getStyleClass().add("section-title");

        VBox qrBox = new VBox(14, qrLabel, imageView);
        qrBox.getStyleClass().add("qr-panel");
        qrBox.setAlignment(Pos.CENTER);
        return qrBox;
    }

    private VBox buildBankInfo() {
        VBox infoPanel = AppUi.panelCard(
                "Thông tin tài khoản nhận tiền",
                "Bạn có thể sao chép nhanh thông tin để dán vào ứng dụng ngân hàng nếu không dùng mã QR."
        );
        infoPanel.getChildren().addAll(
                copyField("Ngân hàng", BANK_NAME),
                copyField("Chủ tài khoản", ACCOUNT_NAME),
                copyField("Số tài khoản", ACCOUNT_NUMBER)
        );
        return infoPanel;
    }

    private HBox copyField(String labelText, String value) {
        Label titleLabel = new Label(labelText);
        titleLabel.getStyleClass().add("field-label");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("muted-label");

        VBox textBox = new VBox(4, titleLabel, valueLabel);

        Button copyButton = new Button("Sao chép");
        copyButton.getStyleClass().add("secondary-button");
        copyButton.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(value);
            Clipboard.getSystemClipboard().setContent(content);
            AlertUtil.info("Đã sao chép", labelText + " đã được sao chép vào bộ nhớ tạm.");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, textBox, spacer, copyButton);
        row.getStyleClass().add("copy-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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

    private boolean isApprovalNotification(NotificationItem item) {
        String title = item.getTitle() == null ? "" : item.getTitle().toLowerCase();
        String message = item.getMessage() == null ? "" : item.getMessage().toLowerCase();
        return title.contains("duyệt")
                || title.contains("nạp tiền")
                || message.contains("duyệt")
                || message.contains("số dư")
                || message.contains("cộng");
    }
}
