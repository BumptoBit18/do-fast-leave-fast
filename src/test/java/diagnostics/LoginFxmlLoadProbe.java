package diagnostics;

import controller.LoginController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class LoginFxmlLoadProbe {
    private LoginFxmlLoadProbe() {
    }

    public static void main(String[] args) throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.startup(() -> Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(LoginFxmlLoadProbe.class.getResource("/view/fxml/login.fxml"));
                loader.setController(new LoginController(null, null));
                Parent root = loader.load();
                System.out.println("LOGIN_FXML_OK=" + root.getClass().getSimpleName());
            } catch (Throwable ex) {
                failure.set(ex);
            } finally {
                Platform.exit();
                completed.countDown();
            }
        }));

        completed.await();
        if (failure.get() != null) {
            throw new IllegalStateException("Login FXML load failed.", failure.get());
        }
    }
}
