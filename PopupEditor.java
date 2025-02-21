import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class PopupEditor {
    private boolean showing;
    private Stage currStage;
    private int block;
    private TextArea textArea;


    public PopupEditor(int block, StringBuilder contents) {
        this.showing = false;
        this.block = block;
    }

    public void showPopup() {
        if (!showing) {
            this.showing = true;
            this.currStage = new Stage();
            Scheduler s = Scheduler.Scheduler();
            currStage.setTitle("Popup Editor");

            // make sure window doesn't block application events
            currStage.initModality(Modality.NONE);

            //TODO(Ray) implement loading of process file blocks into text field
            this.textArea = new TextArea(s.getBlockContents(this.block).toString());
            Scene scene = new Scene(this.textArea, 300, 200);
            currStage.setScene(scene);

            // set showing to false when window closes
            // TODO(Ray): Save on close
            currStage.setOnCloseRequest(this::saveOnClose);
            currStage.showAndWait();
            currStage.requestFocus();
        } else {
            // just change window focus back to us if already open
            currStage.requestFocus();
        }
    }

    public void saveOnClose(WindowEvent event) {
        // save the into the contents
        Scheduler s = Scheduler.Scheduler();
        StringBuilder blockString = s.getBlockContents(this.block);

        blockString.setLength(0); // Clear the string
        blockString.append(this.textArea.getText()); // overwite with new string

        this.showing = false;
    }
}
