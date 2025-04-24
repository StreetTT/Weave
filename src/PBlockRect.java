import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

public class PBlockRect extends Rectangle implements Cloneable {
    // Links a block and its related variables in the scheduler to the Pane in the front end
    static final int BLOCK_WIDTH = 50;
    private static final int BLOCK_HEIGHT = 50;
    public final WeaveProcess process;
    public Block block;
    private int pos;
    private PopupEditor editor;


    static final public Image fillImage = new Image("./assets/plus-icon.png",  50, 0, false, false);

    public PBlockRect(WeaveProcess process, int pos) {
        super(BLOCK_WIDTH, BLOCK_HEIGHT);
        this.pos = pos;
        this.process = process;

        //styling
        this.getStyleClass().add("p-block-rect");

        setFill(new ImagePattern(fillImage));
        setStroke(null);

        setOnMouseClicked(this::handleClick);
        setOnMousePressed(this::handlePressed);
        setOnMouseReleased(this::handleRelease);
    }

    public void activateBlock() {
        this.block = process.addBlock(this.pos);
        setFill(Color.WHITE);
        setStroke(Color.GRAY);
        setStrokeWidth(1.0);
        if (!this.getStyleClass().contains("active-block")) {
            this.getStyleClass().add("active-block");
        }
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
                activateBlock();
            }
        }
    }

    private void handlePressed(MouseEvent event) {
        if (this.block != null) {
            setFill(Color.web("#f0f0f0"));
            setStroke(Color.BLACK);
            setStrokeWidth(1.5);
        }
    }

    private void handleRelease(MouseEvent event) {
        if (this.block != null) {

            setFill(Color.WHITE);
            setStroke(Color.GRAY);
            setStrokeWidth(1.0);

            if (!this.getStyleClass().contains("active-block")) {
                this.getStyleClass().add("active-block");
            }
        } else {
            this.setFill(new ImagePattern(PBlockRect.fillImage));
            this.setStroke(null);
            this.getStyleClass().remove("active-block");
        }
    }


    public void duplicateState(PBlockRect other) {
        other.setFill(this.getFill());
        if (this.block != null) {
            other.activateBlock();
            other.block.fileContents = new StringBuilder(this.block.fileContents);

            if (this.editor != null) {
                other.editor = new PopupEditor(this.block);
            }
        }
    }
}