import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PBlockRect extends Rectangle {
    // Links a block and its related variables in the scheduler to the Pane in the front end
    private static final int BLOCK_WIDTH = 50;
    private static final int BLOCK_HEIGHT = 50;

    private Block block;
    private PopupEditor editor;


    public PBlockRect(Block block) {
        super(BLOCK_WIDTH, BLOCK_HEIGHT);
        this.block = block;

        setFill(Color.LIGHTGRAY);
        setOnMousePressed(this::handleClick);
    }

    private void handleClick(MouseEvent mouseEvent) {
        // Open block's editor when double clicked
        if (mouseEvent.getClickCount() == 2) {
            if (this.editor == null) {
                this.editor = new PopupEditor(this.block);
            }

            editor.showPopup();
        }
    }
}