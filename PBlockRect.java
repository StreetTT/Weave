import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PBlockRect extends Rectangle {
    private PopupEditor editor;

    public PBlockRect(double v, double v1) {
        super(v, v1);
        setFill(Color.LIGHTGRAY);

        setOnMousePressed(this::handleClick);
    }

    private void handleClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            if (this.editor == null) {
                this.editor = new PopupEditor();
            }

            editor.showPopup();
        }
    }
}