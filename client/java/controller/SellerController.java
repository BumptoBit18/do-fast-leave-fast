package controller;

import app.model.AppUser;
import app.model.AuctionLot;
import app.service.AuctionPlatformService;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.layout.VBox;
import network.ServerConnection;
import ui.AppUi;
import util.AlertUtil;
import util.SceneManager;

public class SellerController {
    private final SceneManager sceneManager;
    private final AuctionPlatformService service;

    public SellerController(SceneManager sceneManager, ServerConnection serverConnection) {
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

        Button adminButton = new Button("Hệ thống admin");
        adminButton.getStyleClass().add("secondary-button");
        adminButton.setVisible(currentUser.getRole().name().equals("ADMIN"));
        adminButton.setManaged(currentUser.getRole().name().equals("ADMIN"));
        adminButton.setOnAction(event -> sceneManager.showAdminPanel());

        Button logoutButton = new Button("Đăng xuất");
        logoutButton.setOnAction(event -> sceneManager.logout());

        TableView<AuctionLot> sellerTable = buildSellerTable();
        sellerTable.getItems().setAll(service.getAuctionsForSeller(currentUser.getUsername()));

        Button cancelButton = new Button("Hủy phiên đã chọn");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(event -> {
            AuctionLot selected = sellerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertUtil.error("Chưa chọn phiên", "Hãy chọn một phiên để hủy.");
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
                "Bảng quản lý gian hàng",
                "Theo dõi các lô đang bán, số dư ví và thông báo dành cho người bán.",
                AppUi.statCard("Tổng lô", String.valueOf(sellerTable.getItems().size()), "Danh sách hiện tại"),
                AppUi.statCard("Số dư ví", service.formatCurrency(currentUser.getWalletBalance()), "Tiền có thể nhận hoặc sử dụng"),
                sellerTable,
                new Label("Thông báo gần đây"),
                sellerNotifications,
                cancelButton
        );
        VBox.setVgrow(sellerTable, Priority.ALWAYS);

        VBox form = buildCreateForm();

        HBox content = new HBox(20, left, form);
        HBox.setHgrow(left, Priority.ALWAYS);

        root.setCenter(new VBox(
                18,
                AppUi.pageHeader(
                        "Người bán",
                        "Khu vực quản lý gian hàng",
                        "Tạo lô mới, theo dõi hàng đang bán và xử lý thông báo giao dịch.",
                        AppUi.badge("Gian hàng"),
                        AppUi.badge("Ví tiền"),
                        marketButton,
                        adminButton,
                        logoutButton
                ),
                content
        ));
        return root;
    }

    private TableView<AuctionLot> buildSellerTable() {
        TableView<AuctionLot> table = new TableView<>();

        TableColumn<AuctionLot, String> titleColumn = new TableColumn<>("Sản phẩm");
        titleColumn.setCellValueFactory(data -> data.getValue().titleProperty());
        titleColumn.setPrefWidth(220);

        TableColumn<AuctionLot, String> priceColumn = new TableColumn<>("Giá hiện tại");
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(service.formatCurrency(data.getValue().getCurrentPrice())));
        priceColumn.setPrefWidth(140);

        TableColumn<AuctionLot, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
        statusColumn.setPrefWidth(110);

        TableColumn<AuctionLot, String> endColumn = new TableColumn<>("Thời gian còn lại");
        endColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimeLeftLabel()));
        endColumn.setPrefWidth(140);

        table.getColumns().addAll(titleColumn, priceColumn, statusColumn, endColumn);
        return table;
    }

    private VBox buildCreateForm() {
        VBox form = AppUi.panelCard("Tạo phiên đấu giá mới", "Điền đầy đủ thông tin sản phẩm trước khi đăng lên hệ thống.");
        form.setPrefWidth(380);

        TextField itemName = new TextField();
        itemName.setPromptText("Ví dụ: MacBook Pro M3");

        ComboBox<String> category = new ComboBox<>();
        category.getItems().addAll("Electronics", "Vehicle", "Art", "Collectible", "Luxury");
        category.setValue("Electronics");
        category.setMaxWidth(Double.MAX_VALUE);

        TextField startPrice = new TextField();
        startPrice.setPromptText("Ví dụ: 25000000");

        Spinner<Integer> duration = new Spinner<>(6, 168, 24);
        duration.setEditable(true);

        TextField imageHint = new TextField();
        imageHint.setPromptText("Ví dụ: Laptop màu bạc, còn mới");

        TextArea description = new TextArea();
        description.setPromptText("Mô tả chi tiết tình trạng, phụ kiện, xuất xứ...");
        description.setPrefRowCount(6);

        Button createButton = new Button("Đăng phiên ngay");
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
                AlertUtil.info("Tạo thành công", "Phiên đấu giá mới đã được tạo.");
                sceneManager.showSellerDashboard();
            } catch (Exception ex) {
                AlertUtil.error("Không tạo được phiên", "Kiểm tra dữ liệu đầu vào. " + ex.getMessage());
            }
        });

        form.getChildren().addAll(
                AppUi.fieldGroup("Tên sản phẩm", "Đây là tên chính hiển thị cho người mua trong danh sách đấu giá.", itemName),
                AppUi.fieldGroup("Danh mục", "Chọn nhóm phù hợp để người mua lọc sản phẩm dễ hơn.", category),
                AppUi.fieldGroup("Giá khởi điểm", "Nhập mức giá ban đầu cho phiên đấu giá, chưa bao gồm các lần đặt tiếp theo.", startPrice),
                AppUi.fieldGroup("Thời lượng phiên (giờ)", "Chọn số giờ phiên đấu giá sẽ mở trước khi tự động kết thúc.", duration),
                AppUi.fieldGroup("Gợi ý hình ảnh", "Mô tả ngắn về hình ảnh hoặc diện mạo sản phẩm để hệ thống hiển thị ngữ cảnh tốt hơn.", imageHint),
                AppUi.fieldGroup("Mô tả chi tiết", "Ghi rõ tình trạng sản phẩm, phụ kiện kèm theo và các lưu ý cần thiết.", description),
                createButton
        );
        return form;
    }
}
