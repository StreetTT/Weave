import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class ProcessRow extends HBox {
    public Button button;

    public ProcessRow(WeaveProcess process) {
        this.setSpacing(50);
        this.button = new Button("Delete Row");
        GridPaneRow newRow = new GridPaneRow(process);
        this.getChildren().add(this.button);
        this.getChildren().add(newRow);
    }
}
