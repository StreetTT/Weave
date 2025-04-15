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
        prematureExit(e);
        SharedMemory.DeInit();
    }

    private void prematureExit(WindowEvent e) {
        boolean successfulSave = false;
        while (!successfulSave) {
            if (!successfulSave) {
                // Show a dialog box to confirm exit without saving
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("Do you want to save before exiting?");
                alert.setContentText("Choose your option.");

                ButtonType saveButton = new ButtonType("Save");
                ButtonType dontSaveButton = new ButtonType("Don't Save");
                ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == saveButton) {
                    successfulSave = Scheduler.Scheduler().writeProcessesToDisk(processes, "sourceFiles");
                    if (!successfulSave) {
                        e.consume(); // Don't close if save failed
                    }
                } else if (result.get() == dontSaveButton) {
                    successfulSave = true;
                } else {
                    e.consume();
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        Scheduler scheduler = Scheduler.Scheduler(); // Please always do this at application start

        // init shared memory
        SharedMemory.SharedMemory();

        scheduler.projectDir = "testproj";
        scheduler.projectName = "TEST_PROJ";
        launch(args);
    }

}

