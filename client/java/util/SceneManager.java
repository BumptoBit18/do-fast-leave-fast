package util;

import app.model.AuctionLot;
import controller.AdminController;
import controller.AuctionDetailController;
import controller.AuctionListController;
import controller.LoginController;
import controller.SellerController;
import controller.WalletController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import network.ServerConnection;

import java.io.IOException;

public class SceneManager {
    private static final double WIDTH = 1280;
    private static final double HEIGHT = 820;

    private final Stage stage;
    private final ServerConnection serverConnection;

    public SceneManager(Stage stage, ServerConnection serverConnection) {
        this.stage = stage;
        this.serverConnection = serverConnection;
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }

    public void showLogin() {
        setScene(loadView("/view/fxml/login.fxml", new LoginController(this, serverConnection)), "App Dau gia | Login");
    }

    public void showAuctionList() {
        setScene(loadView("/view/fxml/auction_list.fxml", new AuctionListController(this, serverConnection)), "App Dau gia | Marketplace");
    }

    public void showAuctionDetail(AuctionLot auctionLot) {
        setScene(loadView("/view/fxml/auction_detail.fxml", new AuctionDetailController(this, serverConnection, auctionLot)), "App Dau gia | Auction Detail");
    }

    public void showSellerDashboard() {
        setScene(loadView("/view/fxml/seller_dashboard.fxml", new SellerController(this, serverConnection)), "App Dau gia | Seller Studio");
    }

    public void showAdminPanel() {
        setScene(loadView("/view/fxml/admin_panel.fxml", new AdminController(this, serverConnection)), "App Dau gia | Admin Control");
    }

    public void showWallet() {
        setScene(loadView("/view/fxml/wallet.fxml", new WalletController(this, serverConnection)), "App Dau gia | Wallet");
    }

    public void logout() {
        serverConnection.getService().logout();
        showLogin();
    }

    private Parent loadView(String resourcePath, Object controller) {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(resourcePath));
        loader.setController(controller);
        try {
            return loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Khong the tai giao dien " + resourcePath, ex);
        }
    }

    private void setScene(Parent root, String title) {
        boolean wasShowing = stage.isShowing();
        boolean wasMaximized = stage.isMaximized();
        boolean wasFullScreen = stage.isFullScreen();
        double previousWidth = stage.getWidth();
        double previousHeight = stage.getHeight();

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("app-scroll");

        Scene scene;
        if (wasShowing && previousWidth > 0 && previousHeight > 0) {
            scene = new Scene(scrollPane, previousWidth, previousHeight);
        } else {
            scene = new Scene(scrollPane, WIDTH, HEIGHT);
        }

        String stylesheet = SceneManager.class.getResource("/view/css/style.css") == null
                ? null
                : SceneManager.class.getResource("/view/css/style.css").toExternalForm();
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet);
        }

        stage.setTitle(title);
        stage.setScene(scene);
        if (wasShowing && !wasMaximized && !wasFullScreen && previousWidth > 0 && previousHeight > 0) {
            stage.setWidth(previousWidth);
            stage.setHeight(previousHeight);
        }
        stage.setMaximized(wasMaximized);
        stage.setFullScreen(wasFullScreen);
        stage.show();
    }
}
