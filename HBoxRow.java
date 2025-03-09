import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;

public class HBoxRow extends HBox {
    // Links a row and its related blocks to the HBox in the front end
    private WeaveProcess process;
    private int numBlocks;

    public HBoxRow(int process) {
        // Create a new row
        super();
        this.process = process;
        this.numBlocks = 0;

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
        addBlockButton.setOnAction(event -> this.addBlock(this.numBlocks++));
     
        // Add both spacer and button to the row
        getChildren().addAll(addBlockButton, spacer);
    }

    private void addBlock(int pos) {
        // Create a new block
        Block block = process.addBlock(pos);
        PBlockRect blockButton = new PBlockRect(block);
        // TODO: Add blocks currently adds to the left instead of the right
        this.getChildren().add(this.getChildren().size(), blockButton);
        System.out.println("Added block " + (this.getChildren().size() - 3) + " to process " + this.process);
    }

}