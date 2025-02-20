import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

// NOTE:(Ray) Maybe take the new classes for PBlockREct and PopupEditor and move them into jxml styles
//  and port the existing classes to controllers????

//controller class for frontend

public class FrontendController {
    @FXML
    private Button newBlockButton;
    @FXML
    private HBox processContainer;

    public void addBlock() {
        processContainer.getChildren().add(new PBlockRect(50, 50));
    }

}
