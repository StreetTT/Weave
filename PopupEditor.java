import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class PopupEditor {
    private boolean showing;
    private Stage currStage;
    private StringBuilder contents;
    private TextArea textArea;

    public PopupEditor(String contents) {
        this.showing = false;
        this.contents = new StringBuilder(contents);
    }

    public void showPopup() {
        if (!showing) {
            this.showing = true;
            this.currStage = new Stage();
            currStage.setTitle("Popup Editor");

            // make sure window doesn't block application events
            currStage.initModality(Modality.NONE);

            //TODO(Ray) implement loading of process file blocks into text field
            this.textArea = new TextArea(this.contents.toString());
            Scene scene = new Scene(this.textArea, 300, 200);
            currStage.setScene(scene);

            // set showing to false when window closes
            // TODO(Ray): Save on close
            currStage.setOnCloseRequest(this::saveOnClose);
            currStage.showAndWait();
        } else {
            // just change window focus back to us if already open
            currStage.requestFocus();
        }
    }

    public void saveOnClose(WindowEvent event) {
        // save the into the contents
        this.contents = new StringBuilder(this.textArea.getText());
        this.showing = false;
    }
}
