import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

// NOTE:(Ray) Maybe take the new classes for PBlockRect and PopupEditor and move them into jxml styles
//  and port the existing classes to controllers????

//controller class for frontend

public class FrontendController {
    @FXML
    private Button newBlockButton;
    @FXML
    private VBox processContainer;
    @FXML
    private Region spacer;

    public void addBlock(int pid) {
        // Create a new block
        HBoxRow rowContainer = (HBoxRow) processContainer.getChildren().get(pid);
        PBlockRect blockButton = new PBlockRect((pid + 1), 
                                    rowContainer.getChildren().size(), 50, 50);
        // TODO: Add blocks currently adds to the left instead of the right
        rowContainer.getChildren().add(rowContainer.getChildren().size(), blockButton);
        System.out.println("Added block " + (rowContainer.getChildren().size() - 3) + " to process " + (pid + 1));
    }

        public void addRow() {
        // Create a new row
        int pid = Scheduler.Scheduler().addProcess();
        HBoxRow newRow = new HBoxRow(pid, this.processContainer, this);
        processContainer.getChildren().add(newRow);
        System.out.println("Added process " + processContainer.getChildren().size());
    }

}
