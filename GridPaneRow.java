import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class GridPaneRow extends GridPane {
    private static final int CELL_SIZE_PADDING = 60;
    private static final int COLS = 10;
    private final WeaveProcess process;

    // for drag and drop
    private int initialCol;
    private PBlockRect dragRect;
    private long clickStartTime;

    public GridPaneRow(WeaveProcess process) {
        super();
        this.process = process;
        setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < COLS; ++i) {
            ColumnConstraints col = new ColumnConstraints(CELL_SIZE_PADDING); // 10 padding
            this.getColumnConstraints().add(col);
        }

        RowConstraints row = new RowConstraints();
        this.getRowConstraints().add(row);

        for (int i = 0; i < COLS; ++i) {
            // initilise all block squares as red
            // TODO: make these into + icon buttons later
            Rectangle blockRect = new PBlockRect(this.process, i);
            blockRect.setFill(Color.RED);

            this.add(blockRect, i, 0);
        }

        this.setOnMousePressed(event -> {
            if (!event.getEventType().equals(MouseEvent.MOUSE_CLICKED)) {
                clickStartTime = System.currentTimeMillis();
                initialCol = (int) (event.getX() / CELL_SIZE_PADDING);
                if (isInsideGrid(event.getX(), event.getY())) {
                    dragRect = findRectFromCol(initialCol);
                    if (dragRect != null) {
                        dragRect.setFill(Color.BLACK);
                    }
                }
            }
        });

        this.setOnMouseReleased(this::dragAndDrop);
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

    private void dragAndDrop(MouseEvent event) {
        long duration = System.currentTimeMillis() - clickStartTime;
        if (dragRect != null) {
            if (duration > 100) {

                if (isInsideGrid(event.getX(), event.getY())) {
                    final int newCol = (int) (event.getX() / CELL_SIZE_PADDING);
                    if (newCol != initialCol) {
                        PBlockRect rect = findRectFromCol(newCol);
                        this.getChildren().removeAll(dragRect, rect);
                        this.add(dragRect, newCol, 0);
                        this.add(rect, initialCol, 0);
                    }

                }
            }

            dragRect = null;
        }
    }

}
