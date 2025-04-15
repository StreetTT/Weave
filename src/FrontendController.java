import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Region;
import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Optional;

//NOTE:(Ray) Maybe take the new classes for PBlockRect and PopupEditor and move them into jxml styles
// and port the existing classes to controllers????

public class FrontendController {
    @FXML
    private VBox processContainer;
    @FXML
    private Region spacer;
    private ArrayList<WeaveProcess> selectedProcesses = new ArrayList<>();

    public void initialize(){
        //adds the first row by defult
        addRow();
    }

    public void addRow() {
        // Create a new row
        WeaveProcess process = new WeaveProcess();
        Frontend.processes.add(process);

        ProcessRow newRow = new ProcessRow(process);

        newRow.deleteButton.setOnAction(e -> {
                    processContainer.getChildren().remove(newRow);
                    Frontend.processes.remove(process);
        });

        newRow.selectButton.setOnAction(e -> {
            newRow.handleSelect();
            if (newRow.selected) {
                selectedProcesses.add(newRow.process); // Add to selected processes
                System.out.println("SELECTED PROCESS");
            } else {
                selectedProcesses.remove(newRow.process); // Remove from selected processes
                System.out.println("DESELECTED PROCESS");
            }
        });

        processContainer.getChildren().add(newRow);
        System.out.println("ADDED PROCESS " + processContainer.getChildren().size());
    }

    public void runProcesses() {
        Scheduler.Scheduler().runProcesses(Frontend.processes);
    }

    public void runSelectedProcesses() {
        Scheduler.Scheduler().runProcesses(selectedProcesses);
    }

    public boolean saveProjectAs(){
        File file = Scheduler.Scheduler().showSaveDialogBox();
        return Scheduler.Scheduler().writeProcessesToDisk(Frontend.processes, file);
    }

    public boolean saveProject(){
        return Scheduler.Scheduler().writeProcessesToDisk(Frontend.processes);
    }

    public boolean openProject(){
        File file = Scheduler.Scheduler().showOpenDialogBox();
        return Scheduler.Scheduler().writeProcessesFromDisk(file);
    }
}
