import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PBlockRect extends Rectangle {
    private boolean popup = false;

    public PBlockRect(double v, double v1) {
        super(v, v1);
        setFill(Color.LIGHTGRAY);

        setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2 && !popup) {
                    popup = true;
                    PopupEditor.showPopup();
                }
            }
        });
    }

    private void HandleClick(MouseEvent e) {
        setFill(Color.DARKGRAY);

    }
}