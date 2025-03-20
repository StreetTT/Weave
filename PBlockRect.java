import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PBlockRect extends Rectangle {
    // Links a block and its related variables in the scheduler to the Pane in the front end
    private static final int BLOCK_WIDTH = 50;
    private static final int BLOCK_HEIGHT = 50;
    private final WeaveProcess process;
    private Block block;
    private int pos;
    private PopupEditor editor;


    public PBlockRect(WeaveProcess process, int pos) {
        super(BLOCK_WIDTH, BLOCK_HEIGHT);
        this.pos = pos;
        this.process = process;
        setFill(Color.LIGHTGRAY);

        setOnMouseClicked(this::handleClick);
        setOnMousePressed(this::handlePressed);
        setOnMouseReleased(this::handleRelease);
    }

    private void handleClick(MouseEvent event) {
            System.out.println("MOUSE CLICKED");
            if (event.getClickCount() == 2) {
                if (this.block != null) {
                    if (this.editor == null) {
                        this.editor = new PopupEditor(this.block);
                    }

                    this.editor.showPopup();
                } else {
                    this.block = process.addBlock(this.pos);
                    setFill(Color.GREEN);
                }
            }
        }

    private void handlePressed(MouseEvent event) {
        setFill(Color.BLACK);
    }

    private void handleRelease(MouseEvent event) {
        if (this.block != null) {
            this.setFill(Color.GREEN);
        } else {
            this.setFill(Color.RED);
        }
    }


}