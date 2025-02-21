import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Frontend extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("frontend.fxml"));
        Scheduler scheduler = Scheduler.Scheduler();
        scheduler.projectDir = "testproj";
        scheduler.projectName = "TEST_PROJ";
        int process1 = scheduler.addProcess();
        primaryStage.setTitle("Weave");
        Scene scene = new Scene(root, 1280, 720);

        // Window resizing listeners
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) ->
                scene.getWindow().setWidth(newVal.doubleValue()));

        primaryStage.heightProperty().addListener((obs, oldVal, newVal) ->
                scene.getWindow().setHeight(newVal.doubleValue()));

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

