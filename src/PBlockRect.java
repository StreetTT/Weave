import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;



//representing one visual "block" in the ui grid
public class PBlockRect extends Rectangle implements Cloneable {


    //width of blocks
    static final int BLOCK_WIDTH = 70;
    //height of blocks
    private static final int BLOCK_HEIGHT = 50;
    // holds the data assosicated with a weave process
    public final WeaveProcess process;
    //holds block data
    public Block block;
    //stores the position of blcok in grid
    private int pos;
    //holds the instance of pop up python editor
    private PopupEditor editor;


    //constructor for the visual blocks
    public PBlockRect(WeaveProcess process, int pos) {
        //creates the rectangle shape
        super(BLOCK_WIDTH, BLOCK_HEIGHT);
        this.pos = pos;
        this.process = process;

        //adds css stlyes for the block
        this.getStyleClass().addAll("p-block-rect", "p-block-rect-placeholder");


        //setus up what will happen when a block is clicked
        setOnMouseClicked(this::handleClick);
    }

    //turns a block "active", links it to actual block data
    public void activateBlock() {

        //checks if the block isnt already linked
        if (this.block == null) {
            //adds a new blcok of data to the process at the position
            this.block = process.addBlock(this.pos);
            //remove the placeholder dotted block
            this.getStyleClass().remove("p-block-rect-placeholder");
            //adds an "active" block style if activated
            if (!this.getStyleClass().contains("active-block")) {
                this.getStyleClass().add("active-block");
            }

            //clears any placeholder fill colour
            this.setFill(null);
        }
    }


    //handles clicks on blocks
    private void handleClick(MouseEvent event) {
        //checks for a quick double click
        if (event.getClickCount() == 2) {
            //if block has data
            if (this.block != null) {
                //creates the popup python editor if not open
                if (this.editor == null) {
                    this.editor = new PopupEditor(this.block);
                }

                //show editor
                this.editor.showPopup();
            //If its still a placeholder block
            } else {
                //activates the block first
                activateBlock();

                //if needed it creates the editor
                if (this.editor == null) {
                    this.editor = new PopupEditor(this.block);
                }

                //show editor
                this.editor.showPopup();
            }
        }

        //if alt is held during one click(delete function)
        if (event.isAltDown()) {
            //removes link to block data
            this.block = null;
            //removes editor object
            this.editor = null;
            //goes back to a placeholder
            this.process.removeBlock(this.pos);
            this.getStyleClass().remove("active-block");
            this.getStyleClass().add("p-block-rect-placeholder");
            //clears fill
            this.setFill(null);
        }
    }


    //copies the state of block to another
    public void duplicateState(PBlockRect other) {
        //copies visual stlye of block
        other.setFill(this.getFill());
        //additionally if it has data
        if (this.block != null) {
            //activates new block
            other.activateBlock();
            //copies the code content
            other.block.fileContents = new StringBuilder(this.block.fileContents);

            //creates a new editor for dupe block
            if (this.editor != null) {
                other.editor = new PopupEditor(this.block);
            }
        }
    }
}