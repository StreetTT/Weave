import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;

public class HBoxRow extends HBox {
    // Links a row and its related blocks to the HBox in the front end
    private int pid;

    public HBoxRow(int pid, VBox processContainer, FrontendController frontendController ) {
        
        // Create a new row
        super();
        this.pid = pid;
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(100.0);
        setPrefWidth(Double.MAX_VALUE);
        

        // Create space between add button and blocks
        Region spacer = new Region();
        spacer.setPrefHeight(12.0);
        spacer.setPrefWidth(9.0);
        setHgrow(spacer, Priority.ALWAYS);
        setMargin(spacer, new Insets(0, 20, 0, 0));
        
        // Create and configure add button
        Button addBlockButton = new Button("Add Block");
        addBlockButton.setOnAction(event -> 
            frontendController.addBlock(this.pid, this));
        
        // Add both spacer and button to the row
        getChildren().addAll(addBlockButton, spacer);
    }

    public int getPid() {
        return pid;
    }

}