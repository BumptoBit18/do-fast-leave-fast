package util;

import app.model.AuctionLot;
import controller.AdminController;
import controller.AuctionDetailController;
import controller.AuctionListController;
import controller.LoginController;
import controller.SellerController;
import controller.WalletController;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import network.ServerConnection;

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
        setScene(new LoginController(this, serverConnection).getView(), "App Đấu giá | Login");
    }

    public void showAuctionList() {
        setScene(new AuctionListController(this, serverConnection).getView(), "App Đấu giá | Marketplace");
    }

    public void showAuctionDetail(AuctionLot auctionLot) {
        setScene(new AuctionDetailController(this, serverConnection, auctionLot).getView(), "App Đấu giá | Auction Detail");
    }

    public void showSellerDashboard() {
        setScene(new SellerController(this, serverConnection).getView(), "App Đấu giá | Seller Studio");
    }

    public void showAdminPanel() {
        setScene(new AdminController(this, serverConnection).getView(), "App Đấu giá | Admin Control");
    }

    public void showWallet() {
        setScene(new WalletController(this, serverConnection).getView(), "App Đấu giá | Wallet");
    }

    public void logout() {
        serverConnection.getService().logout();
        showLogin();
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

        String stylesheet = SceneManager.class.getResource("/style.css") == null
                ? null
                : SceneManager.class.getResource("/style.css").toExternalForm();
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
