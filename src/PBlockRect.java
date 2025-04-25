import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class PBlockRect extends Rectangle implements Cloneable {
    // Links a block and its related variables in the scheduler to the Pane in the front end
    static final int BLOCK_WIDTH = 70;
    private static final int BLOCK_HEIGHT = 50;
    public final WeaveProcess process;
    public Block block;
    private int pos;
    private PopupEditor editor;


    public PBlockRect(WeaveProcess process, int pos) {
        super(BLOCK_WIDTH, BLOCK_HEIGHT);
        this.pos = pos;
        this.process = process;


        this.getStyleClass().addAll("p-block-rect", "p-block-rect-placeholder");



        setOnMouseClicked(this::handleClick);
    }


    public void activateBlock() {
        if (this.block == null) {
            this.block = process.addBlock(this.pos);
            this.getStyleClass().remove("p-block-rect-placeholder");
            if (!this.getStyleClass().contains("active-block")) {
                this.getStyleClass().add("active-block");
            }


            this.setFill(null);
        }
    }

    private void handleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            if (this.block != null) {
                if (this.editor == null) {
                    this.editor = new PopupEditor(this.block);
                }

                this.editor.showPopup();
            } else {
                activateBlock();
                if (this.editor == null) {
                    this.editor = new PopupEditor(this.block);
                }
                this.editor.showPopup();
            }
        }

        //DELET
        if (event.isAltDown()) {
            this.block = null;
            if (this.editor.showing) {
                this.editor.currStage.close();
            }

            this.editor = null;
            this.process.removeBlock(this.pos);
            this.getStyleClass().remove("active-block");
            this.getStyleClass().add("p-block-rect-placeholder");
            this.setFill(null);
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