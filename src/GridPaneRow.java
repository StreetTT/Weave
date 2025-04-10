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

public class GridPaneRow extends GridPane {
    private static final int CELL_SIZE_WITH_PADDING = 100; // rects are 50 wide we add an extra 25 on each side to make 100
    private static final int COLS = 10;
    private final WeaveProcess process;

    // for drag and drop
    private int initialCol;
    private PBlockRect dragRect; // for drag and drop
    private PBlockRect selectedRect; // for copy and past
    private PBlockRect copyRect; // for copy and past
    private long clickStartTime;

    public GridPaneRow(WeaveProcess process) {
        super();
        this.process = process;
        setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < COLS; ++i) {
            ColumnConstraints col = new ColumnConstraints(CELL_SIZE_WITH_PADDING);  // column size includes padding in between
            this.getColumnConstraints().add(col);
        }

        RowConstraints row = new RowConstraints();
        this.getRowConstraints().add(row);

        for (int i = 0; i < COLS; ++i) {
            Rectangle blockRect = new PBlockRect(this.process, i);
            blockRect.setFill(new ImagePattern(PBlockRect.fillImage));
            this.add(blockRect, i, 0);
        }

        this.setOnMousePressed(this::mouseOnPressHandler);
        this.setOnMouseReleased(this::dragAndDropHandler);
        this.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressHandler);
    }

    private PBlockRect findRectFromCol(int col) {
        //NOTE:(Ray) for some reason javafx requires you to loop over every single child just to find out if one
        // is at a specified index....
        for (Node node : this.getChildren()) {
            if (GridPane.getColumnIndex(node) == col) {
                return (PBlockRect) node; // can just cast since we know what the object will be
            }
        }

        return null;
    }

    private boolean isInsideGrid(double x, double y) {
        boolean inXBounds = x < this.getWidth() || x >= 0;
        boolean inYBounds = y < this.getHeight() || y >= 0;

        return inXBounds && inYBounds;
    }

    private void resetSelectedRect() {
    }

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

    private void dragAndDropHandler(MouseEvent event) {
        long duration = System.currentTimeMillis() - clickStartTime;
        if (dragRect != null && dragRect.block != null) {
            if (isInsideGrid(event.getX(), event.getY())) {
                final int newCol = (int) (event.getX() / CELL_SIZE_WITH_PADDING);
                PBlockRect rect = findRectFromCol(newCol);
                if (newCol != initialCol) {
                    // click and hold
                    if (duration > 100) {
                        this.getChildren().removeAll(dragRect, rect);
                        this.add(dragRect, newCol, 0);
                        this.add(rect, initialCol, 0);
                        this.process.swapBlocks(initialCol, newCol);
                    }
                }

                // change the selected rect or deselect the current selectedRect

                //TODO(Ray): Fix weird slection stuff
                PBlockRect oldSelectedRect = selectedRect;
                if (oldSelectedRect != dragRect) {
                    selectedRect = dragRect;
                    // magical css style that will give our rectangle a border
                    selectedRect.setStyle("-fx-stroke: black; -fx-stroke-width: 2;");
                } else if (oldSelectedRect != null) {
                    selectedRect = null;
                    oldSelectedRect.setStyle("");
                }
            }
        }

        dragRect = null;
    }

    private void onKeyPressHandler(KeyEvent event) {
        if (event.isControlDown()) {
            if (event.getCode() == KeyCode.C) {
                copyRect = selectedRect;
            }
            if (event.getCode() == KeyCode.V)  {
                selectedRect = copyRect;
            }
        }

        // copy
    }
}