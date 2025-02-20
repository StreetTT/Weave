import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PopupEditor {
    public static void showPopup() {
        Stage stage = new Stage();
        stage.setTitle("Popup Edito");

        // make sure window doesn't block application events
        stage.initModality(Modality.NONE);

        //TODO(Ray) implement loading of processfile blocks into text field
        TextArea textarea = new TextArea("DEFAULT TEXT");
        Scene scene = new Scene(textarea, 300, 200);
        stage.setScene(scene);

        stage.showAndWait(); // Show popup and wait until it's closed
    }
}
