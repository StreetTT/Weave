import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PBlockRect extends Rectangle {
    private int pid;
    private int block;
    private PopupEditor editor;

    public PBlockRect(int pid, int block, double v, double v1) {
        super(v, v1);
        this.pid = pid;

        setFill(Color.LIGHTGRAY);
        setOnMousePressed(this::handleClick);
        Scheduler scheduler = Scheduler.Scheduler();
    }

    private void handleClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            if (this.editor == null) {
                Scheduler s = Scheduler.Scheduler();
                int block = s.addProcessBlock(this.pid);
                this.editor = new PopupEditor(s.getBlockInitialContents(block));
            }

            editor.showPopup();
        }
    }
}