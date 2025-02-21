import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

// NOTE:(Ray) Maybe take the new classes for PBlockRect and PopupEditor and move them into jxml styles
//  and port the existing classes to controllers????

//controller class for frontend

public class FrontendController {
    @FXML
    private Button newBlockButton;
    @FXML
    private HBox processContainer;
    @FXML
    private Region spacer;

    public void addBlock() {
        processContainer.getChildren().add(processContainer.getChildren().indexOf(spacer),
                                           new PBlockRect(1, processContainer.getChildren().size(), 50, 50));
    }

}
