import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;

//NOTE:(Ray) Maybe take the new classes for PBlockRect and PopupEditor and move them into jxml styles
// and port the existing classes to controllers????

public class FrontendController {
    @FXML
    private Button newBlockButton;
    @FXML
    private VBox processContainer;
    @FXML
    private Region spacer;

    public void addRow() {
        // Create a new row
        WeaveProcess process = new WeaveProcess();
        Frontend.processes.add(process);

        GridPaneRow newRow = new GridPaneRow(process);
        processContainer.getChildren().add(newRow);
        System.out.println("Added process " + processContainer.getChildren().size());
    }

    public void runProcesses() {
        Scheduler.Scheduler().runProcesses(Frontend.processes);
    }
}
