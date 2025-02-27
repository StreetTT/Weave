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

//TODO:(Ray) Frontend needs to be in charge of the 256x1024 grid layout for all the blocks and processes
// Scheduler doesn't really care too much about what processes are where

//controller class for frontend

public class FrontendController {
    @FXML
    private Button newBlockButton;
    @FXML
    private VBox processContainer;
    @FXML
    private Region spacer;

    public void addBlock(int pid, HBoxRow parentRowContainer) {
        // Create a new block
        PBlockRect block = new PBlockRect(pid, parentRowContainer.getChildren().size(), 50, 50);
        // TODO: Add blocks currently adds to the left instead of the right
        parentRowContainer.getChildren().add(parentRowContainer.getChildren().size(), block);
        System.out.println("Added block " + (parentRowContainer.getChildren().size() - 3) + " to process " + (pid));
    }

        public void addRow() {
        // Create a new row
        int pid = Scheduler.Scheduler().addProcess();
        HBoxRow newRow = new HBoxRow(pid, this.processContainer, this);
        processContainer.getChildren().add(newRow);
        System.out.println("Added process " + processContainer.getChildren().size());
    }

}
