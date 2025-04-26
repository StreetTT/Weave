import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import java.io.InputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.ArrayList;


//main class for the javafx frontend application
public class Frontend extends Application {

    //static list holding all the current weave processes in the project
    static public ArrayList<WeaveProcess> processes = new ArrayList<>();


    //entry point for the javafx application
    @Override
    public void start(Stage primaryStage) {

        Parent root;
        try {
            //loads the ui layout from the fxml file
            root = FXMLLoader.load(getClass().getResource("frontend.fxml"));
        } catch (IOException e) {
            System.err.println("Counln't find the fxml file");
            e.printStackTrace();
            return;
        }

        //sets the window title

        primaryStage.setTitle("Weave");
        Scene scene = new Scene(root, 1280, 720);

        //loads and sets the application icon
        Image appIcon = new Image(getClass().getResourceAsStream("/assets/W.png"));
        primaryStage.getIcons().add(appIcon);




        //loads the css stylesheet for syntax highlighting and other styles
        scene.getStylesheets().add(getClass().getResource("python-keywords.css").toExternalForm());


        primaryStage.setScene(scene);


        // Window resizing listeners
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) ->
                scene.getWindow().setWidth(newVal.doubleValue()));

        primaryStage.heightProperty().addListener((obs, oldVal, newVal) ->
                scene.getWindow().setHeight(newVal.doubleValue()));

        primaryStage.setOnCloseRequest(this::closeFunction);

        primaryStage.setScene(scene);
        primaryStage.show();

        Scheduler.Scheduler(); // init this stuff last
    }



    //called when the user attempts to close the application window
    public void closeFunction(WindowEvent e) {
        //prompts the user to save changes if necessary
        prematureExit(e);
        WeaveNativeFactory.get().DeInit();
    }




    //handles the save confirmation dialog when exiting
    private void prematureExit(WindowEvent e) {

        //flag to track save status
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
                        e.consume();
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




    //main entry point of the java program
    public static void main(String[] args) {
        //initializes the scheduler singleton early
        Scheduler scheduler = Scheduler.Scheduler();

        // init shared memory
        WeaveNativeFactory.get();

        scheduler.projectDir = "testproj";
        scheduler.projectName = "TEST_PROJ";
        launch(args);
    }

}

