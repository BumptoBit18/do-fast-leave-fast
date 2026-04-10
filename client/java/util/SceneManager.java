package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class SceneManager {
    private static Stage primaryStage;

    // Hàm này cần được gọi 1 lần duy nhất tại file Main.java khi ứng dụng khởi chạy
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchScene(String fxmlPath) {
        try {
            // Lấy tài nguyên file FXML
            URL fxmlLocation = SceneManager.class.getResource(fxmlPath);
            if (fxmlLocation == null) {
                AlertUtil.showError("Lỗi hệ thống: Không tìm thấy file giao diện tại " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);
            Scene scene = new Scene(root);
            
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Lỗi không thể tải màn hình: " + e.getMessage());
        }
    }
}