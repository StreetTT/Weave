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
        // Create a new row with a space then an "add block" button
        HBox rowContainer = (HBox) processContainer.getChildren().get(pid);
        PBlockRect blockButton = new PBlockRect((pid + 1), 
                                    rowContainer.getChildren().size(), 50, 50);
        // TODO: Add blocks currently adds to the left instead of the right
        rowContainer.getChildren().add(rowContainer.getChildren().size(), blockButton);
        System.out.println("Added block " + (rowContainer.getChildren().size() - 3) + " to process " + (pid + 1));
    }

    public void addRow() { 
        // Create a new row with a space then an "add block" button
        // HBoxRow newRow = new HBoxRow(processContainer.getChildren().size(), this.processContainer);

        HBox newRow = new HBox();
        newRow.setAlignment(Pos.CENTER_LEFT);
        newRow.setPrefHeight(100.0);
        newRow.setPrefWidth(Double.MAX_VALUE);
    
        Region spacer = new Region();
        spacer.setPrefHeight(12.0);
        spacer.setPrefWidth(9.0);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setMargin(spacer, new Insets(0, 20, 0, 0));
    
        Button addBlockButton = new Button("Add Block");
        addBlockButton.setOnAction(event -> 
                            addBlock(processContainer.getChildren().indexOf(newRow)));
       
        newRow.getChildren().addAll(addBlockButton, spacer); 
        processContainer.getChildren().add(newRow);
        int pid = Scheduler.Scheduler().addProcess();
        System.out.println("Added process " + processContainer.getChildren().size());
    }

}
