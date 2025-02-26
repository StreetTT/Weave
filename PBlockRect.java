import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PBlockRect extends Rectangle {
    // Links a block and its related variables in the scheduler to the Pane in the front end
    private int pid;
    private int block;
    private PopupEditor editor;

    public PBlockRect(int pid, int block, double v, double v1) {
        super(v, v1);
        this.pid = pid;

        setFill(Color.LIGHTGRAY);
        setOnMousePressed(this::handleClick);
        this.block = Scheduler.Scheduler().addProcessBlock(this.pid);
    }

    private void handleClick(MouseEvent mouseEvent) {
        // Open block's editor when double clicked
        if (mouseEvent.getClickCount() == 2) {
            if (this.editor == null) {
                Scheduler s = Scheduler.Scheduler();
                this.editor = new PopupEditor(this.block, s.getBlockContents(block));
            }

            editor.showPopup();
        }
    }
}