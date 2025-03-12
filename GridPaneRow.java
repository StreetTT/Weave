import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.awt.event.MouseEvent;

public class GridPaneRow extends GridPane {
    private static final int CELL_SIZE = 50;
    private static final int COLS = 10;
    private final WeaveProcess process;

    public GridPaneRow(WeaveProcess process) {
        super();
        this.process = process;
        setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < COLS; ++i) {
            ColumnConstraints col = new ColumnConstraints(CELL_SIZE + 10); // 10 padding
            this.getColumnConstraints().add(col);
        }

        RowConstraints row = new RowConstraints();
        this.getRowConstraints().add(row);

        for (int i = 0; i < COLS; ++i) {
            // initilise all block squares as red
            // TODO: make these into + icon buttons later
            Rectangle blockRect = new PBlockRect(this.process, i);
            blockRect.setFill(Color.RED);

            this.add(blockRect, i, 1);
        }
    }


}
