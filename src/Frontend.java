import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import java.util.ArrayList;

public class Frontend extends Application {

    static public ArrayList<WeaveProcess> processes = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("frontend.fxml"));
        Scheduler scheduler = Scheduler.Scheduler(); // Please always do this at application start

        // init shared memory
        SharedMemory.SharedMemory();

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
        prematureExit();
        SharedMemory.DeInit();
    }

    private void prematureExit(){
        boolean successfulSave = false;
        while (!successfulSave){
            successfulSave = Scheduler.Scheduler().writeProcessesToDisk(processes);
            if (!successfulSave) {
                // Show a dialog box to confirm exit without saving
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("Project is not be saved.");
                alert.setContentText("Are you sure you want to exit without saving?");

                ButtonType yesButton = new ButtonType("Yes");
                ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(yesButton, noButton);

                // Wait for user response
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == yesButton) {
                    break; // Exit the loop and proceed with closing
                } else {
                    e.consume(); // Prevent the window from closing
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}

