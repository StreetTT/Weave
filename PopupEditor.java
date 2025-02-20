import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PopupEditor {
    private boolean showing;
    private Stage currStage;

    public PopupEditor() {
        this.showing = false;
    }

    public void showPopup() {
        if (!showing) {
            this.showing = true;
            this.currStage = new Stage();
            currStage.setTitle("Popup Edito");

            // make sure window doesn't block application events
            currStage.initModality(Modality.NONE);

            //TODO(Ray) implement loading of process file blocks into text field
            TextArea textarea = new TextArea("DEFAULT TEXT");
            Scene scene = new Scene(textarea, 300, 200);
            currStage.setScene(scene);

            // set showing to false when window closes
            currStage.setOnCloseRequest(event -> this.showing = false);
            currStage.showAndWait();
        } else {
            // just change window focus back to popup if already open
            currStage.requestFocus();
        }
    }
}
