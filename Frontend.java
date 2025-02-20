import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.control.Separator;

public class Frontend extends Application {

    private static final int MAX_PROCESSES = 10;
    private static final int BLOCKS_PER_PROCESS = 20;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("WEAVE WINDOW");
        GridPane root = new GridPane();

        // Create the grid layout 

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);


        Scene scene = new Scene(new VBox(scrollPane), 1280, 720);

        for (int row = 0; row < MAX_PROCESSES; ++row) {
            for (int col = 0; col < BLOCKS_PER_PROCESS; ++col) {
                if (row % 2 == 0 && col % 2 == 0) {

                    Rectangle code_block = new Rectangle(200, 200);
                    code_block.setFill(Color.LIGHTGRAY);
                    code_block.setOnMouseClicked(event -> code_block.setFill(Color.BLACK));
                    root.add(code_block, row, col);
                } else if (row % 2 == 1 && col % 2 == 0) {
                    Separator separator = new Separator();
                    separator.setPrefWidth(200);
                    root.add(separator, col, row);
                } else if (row % 2 == 0 && col % 2 == 1) {
                    // Add vertical separator
                    Separator separator = new Separator();
                    separator.setOrientation(javafx.geometry.Orientation.VERTICAL);
                    separator.setPrefHeight(200);
                    root.add(separator, col, row);
            }
            // Change Color

            }
        }

        // listen for window resize
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) ->
                scene.getWindow().setWidth(newVal.doubleValue()));

        primaryStage.heightProperty().addListener((obs, oldVal, newVal) ->
            scene.getWindow().setHeight(newVal.doubleValue()));

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
 
}
