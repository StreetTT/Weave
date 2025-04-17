import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

//NOTE:(Ray) Maybe take the new classes for PBlockRect and PopupEditor and move them into jxml styles
// and port the existing classes to controllers????

public class FrontendController {
    @FXML
    private VBox processContainer;
    @FXML
    private Region spacer;
    private ArrayList<WeaveProcess> selectedProcesses = new ArrayList<>();

    public void initialize() {
        addRow();
        addRow();
    }

    public ProcessRow addRow() {
        // Create a new row
        WeaveProcess process = new WeaveProcess();
        Frontend.processes.add(process);

        ProcessRow newRow = new ProcessRow(process);

        newRow.deleteButton.setOnAction(e -> {
                    processContainer.getChildren().remove(newRow);
                    Frontend.processes.remove(process);
        });
        newRow.deleteButton.setTooltip(new Tooltip("Delete this process"));
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
        newRow.selectButton.setTooltip(new Tooltip("Select this process"));
        processContainer.getChildren().add(newRow);
        System.out.println("ADDED PROCESS " + processContainer.getChildren().size());

        return newRow;
    }

    public void runProcesses() {
        Scheduler.Scheduler().runProcesses(Frontend.processes);
    }

    public void runSelectedProcesses() {
        Scheduler.Scheduler().runProcesses(selectedProcesses);
    }

    public void saveProjectAs(){
        File folder = this.showSaveDialogBox();
        if (folder != null) {
            Scheduler.Scheduler().projectDir = folder.toString();
            Scheduler.Scheduler().writeProcessesToDisk(Frontend.processes, "sourceFiles");
        }
    }

    public boolean saveProject(){
        return Scheduler.Scheduler().writeProcessesToDisk(Frontend.processes, "sourceFiles");
    }

    public void openProject() {
        File file = this.showOpenDialogBox();
        if (file == null) {
            return;
        }

        Scheduler.Scheduler().projectDir = file.getParent().toString();

        //adds the first row by defult
        ByteBuffer contents = ByteBuffer.allocate(0);
        try {
            contents = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            System.out.println("FAILED TO READ FROM PROJECT DATA FILE");
            e.printStackTrace();
        }

        contents.order(ByteOrder.LITTLE_ENDIAN);
        int fileIdentifier = contents.getInt();
        if (fileIdentifier == Scheduler.WEAVE_FILE_IDENTIFIER) {
            processContainer.getChildren().clear();
            Frontend.processes.clear();
            int version = contents.getInt(); // ignore the version we literally only have one
            StringBuilder projectName = new StringBuilder();
            int projectNameLength = contents.getInt() / Character.BYTES;

            for (int i = 0; i < projectNameLength; ++i) {
                projectName.append(contents.getChar());
            }

            Scheduler.Scheduler().projectName = projectName.toString();
            int processes = contents.getInt();
            //TODO(Ray): Extract and unit test
            // Deserialise proceess data

            int currentProcess = 0;

            while (contents.hasRemaining()) {
                boolean validProcess = false;
                int remaining = contents.remaining();
                int identifierShiftMul = 0;
                for (int i = 0; i < remaining; ++i) {
                    int shift = (identifierShiftMul * 8);
                    if (contents.get() == ((Scheduler.PROCESS_IDENTIFIER >> shift) & 0xFF)) {
                        ++identifierShiftMul;
                    } else {
                        identifierShiftMul = 0;
                    }

                    if (identifierShiftMul == 4) {
                        validProcess = true;
                        break;
                    }
                }

                if (validProcess) {
                    if (contents.remaining() > (256 / 8)) {
                        ProcessRow row = addRow();
                        // read in entire file and parse
                        Path processFile = Paths.get(Scheduler.Scheduler().projectDir + "/sourceFiles/" + projectName + "_PROCESS_" + (currentProcess + 1) + ".py");
                        currentProcess++;
                        byte[] processFileContents = new byte[0];
                        try {
                            processFileContents = Files.readAllBytes(processFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        String processFileString = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(processFileContents)).toString();
                        for (int j = 0; j < 256 / 8; ++j) {
                            byte block = contents.get();
                            for (int k = 0; k < 8; ++k) {
                                int currentBlockIdx = j * 8 + k;
                                int currentBit = (1 << k);
                                if ((block & (currentBit)) == currentBit) {
                                    PBlockRect uiBlock = row.gridPaneRow.findRectFromCol(currentBlockIdx);
                                    int functionIdx = processFileString.indexOf("process_func_block_" + currentBlockIdx + "():\n    try:");
                                    int blockStartIdx = processFileString.indexOf("        ", functionIdx);
                                    int blockEndIdx = processFileString.indexOf("\n        pass", blockStartIdx);
                                    String blockString = processFileString.substring(blockStartIdx, blockEndIdx);
                                    blockString = blockString.indent(-8); // remove indents
                                    uiBlock.activateBlock();
                                    uiBlock.block.fileContents = new StringBuilder(blockString);
                                }
                            }
                        }

                        if (contents.hasRemaining() && contents.get() != 'P' && currentProcess != processes) {
                            System.err.println("Corrupted project file detected");
                        }

                    } else {
                        System.err.println("Corrupted project file detected");
                    }
                    //go back one byte
                    contents.position(contents.position() - 1);
                }
            }
        }
    }

    private File showOpenDialogBox() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Weave Project");

        // Only allow .wve files
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Weave Files", "*.wve")
        );

        // Set initial directory
        String defaultPath = Scheduler.Scheduler().projectDir;
        fileChooser.setInitialDirectory(new File(defaultPath));

        // Show open dialog
        File selectedFile = fileChooser.showOpenDialog(new Stage());

        return selectedFile;
    }

    private File showSaveDialogBox() {
        // Show save dialog box
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose Project Directory");

        // Give the dialog a starting point
        String defaultPath = Scheduler.Scheduler().projectDir;
        dirChooser.setInitialDirectory(new File(defaultPath));

        // Show dialog and get selected directory
        return dirChooser.showDialog(new Stage());
    }
}
