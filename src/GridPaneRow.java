import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import java.security.Key;


//represents the grid of blocks within a single process row in the ui
public class GridPaneRow extends GridPane {
    static final int CELL_SIZE_WITH_PADDING = 120;
    private int cols = 10;
    private final WeaveProcess process;


    private int initialCol;
    private PBlockRect dragRect; // for drag and drop
    private long clickStartTime;



    //gets the current number of columns in this gridpane
    public int getColumnCountGridPlane() {
        return this.getColumnConstraints().size();
    }


    //creates an empty (placeholder) block visual at the specified column
    private void createEmptyBlockAtCol(int col) {
        Rectangle blockRect = new PBlockRect(this.process, col);

        this.add(blockRect, col, 0);
    }


    //constructor for the gridpane row
    public GridPaneRow(WeaveProcess process) {
        //calls the GridPane constructor
        super();
        this.process = process;

        //aligns content to the left within the grid
        setAlignment(Pos.CENTER_LEFT);


        //creates the initial set of column constraints
        for (int i = 0; i < cols; ++i) {
            //sets a fixed width for each column
            ColumnConstraints col = new ColumnConstraints(CELL_SIZE_WITH_PADDING);  // column size includes padding in between
            this.getColumnConstraints().add(col);
        }

        //creates a single row constraint
        RowConstraints row = new RowConstraints();
        this.getRowConstraints().add(row);


        //populates the initial columns with empty block placeholders
        for (int i = 0; i < cols; ++i) {
            createEmptyBlockAtCol(i);
        }


        //sets up event handlers for mouse press (start drag) and release (end drag)
        this.setOnMousePressed(this::mouseOnPressHandler);
        this.setOnMouseReleased(this::dragAndDropHandler);
    }


    //finds the PBlockRect ui element located at a specific column index
    public PBlockRect findRectFromCol(int col) {
        //NOTE:(Ray) for some reason javafx requires you to loop over every single child just to find out if one
        // is at a specified index....
        for (Node node : this.getChildren()) {
            if (GridPane.getColumnIndex(node) == col) {
                return (PBlockRect) node; // can just cast since we know what the object will be
            }
        }

        return null;
    }


    //extends the grid by adding new columns up to the specified column index
    public void extendGridToCol(int col) {
        int oldCols = this.cols;
        if (this.cols < col) {
            this.cols = col;
        }

        //adds new column constraints and empty blocks for the extended range
        for (int i = oldCols; i < col + 1; ++i) {
            ColumnConstraints column = new ColumnConstraints(CELL_SIZE_WITH_PADDING);  // column size includes padding in between
            this.getColumnConstraints().add(column);
            createEmptyBlockAtCol(i);
        }

    }


    //checks if the given x, y coordinates are within the bounds of the gridpane
    private boolean isInsideGrid(double x, double y) {
        boolean inXBounds = x < this.getWidth() || x >= 0;
        boolean inYBounds = y < this.getHeight() || y >= 0;

        return inXBounds && inYBounds;
    }

    //handles the mouse press event, initiating a potential drag operation
    private void mouseOnPressHandler(MouseEvent event) {
        if (!event.getEventType().equals(MouseEvent.MOUSE_CLICKED)) {
            // drag and drop
            clickStartTime = System.currentTimeMillis();
            initialCol = (int) (event.getX() / CELL_SIZE_WITH_PADDING);

            if (isInsideGrid(event.getX(), event.getY())) {
                PBlockRect rect = findRectFromCol(initialCol);
                dragRect = rect;
            }

        }
    }


    //handles the mouse release event, completing a drag and drop or click action
    private void dragAndDropHandler(MouseEvent event) {
        //calculates how long the mouse button was held down
        long duration = System.currentTimeMillis() - clickStartTime;
        //checks if a block was being dragged and if that block actually has data
        if (dragRect != null && dragRect.block != null) {
            //checks if the mouse was released inside the grid bounds
            if (isInsideGrid(event.getX(), event.getY())) {
                //calculates the column index where the mouse was released
                final int newCol = (int) (event.getX() / CELL_SIZE_WITH_PADDING);
                //finds the block rectangle at the destination column
                PBlockRect rect = findRectFromCol(newCol);
                //only performs action if dropped on a different column
                if (newCol != initialCol) {
                    // click and hold
                    if (duration > 100) {
                        if (event.isControlDown()) {
                            dragRect.duplicateState(rect);
                        } else {
                            this.getChildren().removeAll(dragRect, rect);
                            this.add(dragRect, newCol, 0);
                            this.add(rect, initialCol, 0);
                            this.process.swapBlocks(initialCol, newCol);
                        }
                    }
                }
            }
        }

        //resets the dragged rectangle reference
        dragRect = null;
    }


    //adds a new empty block to the end of the grid row

    public void addNewBlock() {
        //creates a new column constraint
        ColumnConstraints col = new ColumnConstraints(CELL_SIZE_WITH_PADDING);
        this.getColumnConstraints().add(col);

        //calculates the index for the new column
        int newCol = cols;
        //creates a new placeholder block rectangle for the new column
        Rectangle blockRect = new PBlockRect(this.process, newCol);
        //adds the new block rectangle to the grid
        this.add(blockRect, newCol, 0);

        cols++;
    }
}