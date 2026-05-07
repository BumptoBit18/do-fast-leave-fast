import javafx.application.Application;
import javafx.stage.Stage;
import network.ServerConnection;
import util.SceneManager;

public class ClientMain extends Application {
    @Override
    public void start(Stage stage) {
        stage.setMinWidth(1120);
        stage.setMinHeight(760);
        SceneManager sceneManager = new SceneManager(stage, new ServerConnection());
        sceneManager.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
