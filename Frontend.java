import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

//NOTE(Ray) *MAYBE IMPLEMENT* Javafx cannot render all process and blocks using gridPane may have to use canvas so
// we can dynamically render grid blocks on a scrolling pane
public class Frontend extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("frontend.fxml"));
        Scheduler scheduler = Scheduler.Scheduler(); // Please always do this at application start

        scheduler.projectDir = "testproj";
        scheduler.projectName = "TEST_PROJ";
        primaryStage.setTitle("Weave");
        Scene scene = new Scene(root, 1280, 720);

        // Window resizing listeners
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) ->
                scene.getWindow().setWidth(newVal.doubleValue()));

        primaryStage.heightProperty().addListener((obs, oldVal, newVal) ->
                scene.getWindow().setHeight(newVal.doubleValue()));

        primaryStage.setOnCloseRequest(this::closeFunction);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void closeFunction(WindowEvent e) {
        Scheduler.Scheduler().writeProcessesToDisk();
        SharedMemory.FreeWeaveSharedBuffer();
    }

    public static void main(String[] args) {
        launch(args);
    }

}

