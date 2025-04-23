import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.paint.Color;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;

public class FrontendController {
    @FXML
    public VBox processContainer;
    @FXML
    public Pane columnOverlayPane;
    @FXML
    public Region spacer;
    @FXML
    public TextArea  outputTextArea;

    private ArrayList<WeaveProcess> selectedProcesses = new ArrayList<>();
    private static final int MAX_RECENT = 5;    // max amount of recent projects
    private final Preferences prefs = Preferences.userNodeForPackage(FrontendController.class);
    private static final String RECENT_PROJECT_KEY = "recentProject_";
    private List<String> recentProjects = new ArrayList<>();
    @FXML public Menu fileMenu;


    public void initialize() {
        showStartupDialog();
        loadRecentProjects();
        populateRecentProjectsMenu();


        //LISTENERS
        processContainer.getChildren().addListener((ListChangeListener<Node>) c -> {
            while (c.next()) {

            }
            // lines update when list changes
            updateColoumLines();
        });

        //listens for height changess
        processContainer.heightProperty().addListener((obs, oldH, newH) -> updateColoumLines());


        javafx.application.Platform.runLater(this::updateColoumLines);
    }


    @FXML
    public void handleAddProcessRow(){
        //adds a row, any other functionatilty can be added here for adding a row
        addRow();
    }



    //FIXME: force them to name a new project
    private void showStartupDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Welcome to Weave");
        dialog.setHeaderText("Choose an option to continue");

        // Make dialog modal (blocks input to other windows)
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Create buttons
        ButtonType newProjectBtn = new ButtonType("New Project", ButtonBar.ButtonData.OK_DONE);
        ButtonType openProjectBtn = new ButtonType("Open Project", ButtonBar.ButtonData.OTHER);
        
        // Create recent projects list
        ListView<String> recentList = new ListView<>();
        recentList.getItems().addAll(recentProjects);
        recentList.setPrefHeight(100);
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Recent Projects:"),
            recentList
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(newProjectBtn, openProjectBtn);

        // Handle button clicks
        dialog.setResultConverter(buttonType -> {
            if (buttonType == newProjectBtn) {
                return "new";
            } else if (buttonType == openProjectBtn) {
                return "open";
            }
            return null;
        });

        // Handle recent project selection
        recentList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {  // Double click
                String selectedPath = recentList.getSelectionModel().getSelectedItem();
                if (selectedPath != null) {
                    dialog.close();
                    openProjectFromPath(selectedPath);
                }
            }
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(action -> {
            if (action.equals("new")) {
                // Handle new project
                Optional<String> name;
                do {
                    TextInputDialog projectNameDialog = new TextInputDialog();
                    projectNameDialog.setTitle("New Project");
                    projectNameDialog.setHeaderText("Choose A Project Name");
                    name = projectNameDialog.showAndWait();
                } while (name.isEmpty());

                Scheduler.Scheduler().projectName = name.get();
                saveProjectAs();
                addRow();
                addRow();
            } else if (action.equals("open")) {
                openProject();
            }
        });
    }

    public ProcessRow addRow() {
        // Create a new row
        WeaveProcess process = new WeaveProcess();
        Frontend.processes.add(process);

        ProcessRow newRow = new ProcessRow(process);
        newRow.setOnBlockAdded(this::updateColoumLines);

        newRow.deleteButton.setOnAction(e -> {
                    processContainer.getChildren().remove(newRow);
                    Frontend.processes.remove(process);
                    //update lines
                    updateColoumLines();
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
        updateColoumLines();
        System.out.println("ADDED PROCESS " + processContainer.getChildren().size());

        return newRow;
    }



    //implementation of visualisation lines
    private void updateColoumLines(){
        //clear all lines first
        columnOverlayPane.getChildren().clear();

        //chech if there are any process rows we need to draw over
        if(processContainer.getChildren().isEmpty()){
            //no need for lines
            return;
        }

        //determine vertical length needed

        //gets the first process block
        Node firstChild = processContainer.getChildren().get(0);

        //work out the top of the first process to position the line
        Point2D firstRowTopLeftInScene = firstChild.localToScene(0, 0);
        if (firstRowTopLeftInScene == null || columnOverlayPane.getScene() == null) {
            return;
        }
        Point2D firstRowTopLeftInOverlay = columnOverlayPane.sceneToLocal(firstRowTopLeftInScene);

   

        // Start the line slightly below the absolute top of the first row
        double startY = firstRowTopLeftInOverlay.getY() + 10.5;


        Bounds containerBoundsInParent = processContainer.getBoundsInParent();
        double endY = containerBoundsInParent.getMaxY() + 5;

        //make sure its actually a valid height
        if(startY >= endY){
            return;
        }

        //now determine distance of horizontal nature
        ProcessRow firstRow = (ProcessRow) processContainer.getChildren().get(0);

        GridPaneRow gridPane = firstRow.getGridPaneRow();

        //check we actually have a gridpane
        if (gridPane == null){
            Platform.runLater(this::updateColoumLines);
            return;
        }

        //calculate x pos of overlay plane
        Bounds gridBoundsInScene = gridPane.localToScene(gridPane.getBoundsInLocal());
        if (gridBoundsInScene == null || columnOverlayPane.getScene() == null) {
            // Scene might not be ready yet during initial layout so we add it to the later running stuff
            Platform.runLater(this::updateColoumLines);
            return;
        }
        Point2D gridTopLeftInOverlay = columnOverlayPane.sceneToLocal(gridBoundsInScene.getMinX(), gridBoundsInScene.getMinY());

        //if a conversion failed
        if (gridTopLeftInOverlay == null) {
            return;
        }
        double gridPaneStartXInOverlay = gridTopLeftInOverlay.getX();

        //time to draw lines

        int cols = gridPane.getColumnCount(); // Use the dynamic column count
        final double cellWidth = GridPaneRow.CELL_SIZE_WITH_PADDING;

        for (int i = 1; i < cols; i++) {
            final double PADDING_OFFSET = (GridPaneRow.CELL_SIZE_WITH_PADDING - PBlockRect.BLOCK_WIDTH) / 2.0;
            double lineX = gridPaneStartXInOverlay + (i * cellWidth) - PADDING_OFFSET;

            Line line = new Line(lineX, startY, lineX, endY);
            line.setStroke(Color.GRAY);
            line.setStrokeWidth(1);
            line.getStrokeDashArray().addAll(5.0, 5.0);
            columnOverlayPane.getChildren().add(line);
        }


    }

    public void updateOutputTerminal() {
        //raw output string from the native layer
        String rawOutputString = StandardCharsets.UTF_8.decode(WeaveNativeFactory.get().GetProcessesOutput()).toString(); //

        if (outputTextArea != null) {
            //adds > to each line to look cooler
            String processedOutputString = Arrays.stream(rawOutputString.split("\\R")).map(line -> "> " + line).collect(Collectors.joining(System.lineSeparator()));

            outputTextArea.setText(processedOutputString);


            outputTextArea.setScrollTop(Double.MAX_VALUE); 
        } else {
            System.err.println("outputTextArea is null. Check FXML connection.");
        }
    }

    public void runProcesses() {
        //TOOD: Prompt the user to save before running processes
        Scheduler.Scheduler().saveProjectFile(Frontend.processes);
        Scheduler.Scheduler().runProcesses(Frontend.processes);
        updateOutputTerminal();
    }

    public void runSelectedProcesses() {
        Scheduler.Scheduler().runProcesses(selectedProcesses);
        updateOutputTerminal();
    }

    public void saveProjectAs(){
        File folder = this.showSaveDialogBox();
        if (folder != null) {
            Scheduler.Scheduler().projectDir = folder.toString();
            Scheduler.Scheduler().writeProcessesToDisk(Frontend.processes, "sourceFiles");
            Scheduler.Scheduler().saveProjectFile(Frontend.processes);
        }
    }

    public boolean saveProject(){
        boolean ok = Scheduler.Scheduler().writeProcessesToDisk(Frontend.processes, "sourceFiles");
        ok |= Scheduler.Scheduler().saveProjectFile(Frontend.processes);
        return ok;
    }

    public void loadProject(File file) {
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

            updateRecentProjects(file.getAbsolutePath());
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
                                    row.gridPaneRow.extendGridToCol(currentBlockIdx);
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

            Platform.runLater(this::updateColoumLines);
        }
    }

    public void openProject() {
        File file = this.showOpenDialogBox();
        if (file == null) {
            return;
        }
        loadProject(file);
    }

    private void populateRecentProjectsMenu() {
        fileMenu.getItems().clear();

        for (String path : recentProjects) {
            MenuItem item = new MenuItem(path);
            item.setOnAction(e -> openProjectFromPath(path));
            fileMenu.getItems().add(item);
        }
    }

    private void openProjectFromPath(String path) {
        File file = new File(path);
        if (file.exists()) {
            loadProject(file);
        } else {
            //gonna change all of this
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("The File was not found");
            alert.setContentText("The selected recent project no longer exists here.");
            alert.showAndWait();
            recentProjects.remove(path);
            saveRecentProjects();
            populateRecentProjectsMenu();
        }
    }

    private void updateRecentProjects(String path) {
        recentProjects.remove(path);
        recentProjects.add(0, path);

        if (recentProjects.size() > MAX_RECENT) {
            recentProjects = new ArrayList<>(recentProjects.subList(0, MAX_RECENT));
        }

        saveRecentProjects();
        populateRecentProjectsMenu();
    }

    private void saveRecentProjects() {
        for (int i = 0; i < MAX_RECENT; i++) {
            prefs.remove(RECENT_PROJECT_KEY + i);
        }

        for (int i = 0; i < recentProjects.size(); i++) {
            prefs.put(RECENT_PROJECT_KEY + i, recentProjects.get(i));
        }
    }

    private void loadRecentProjects() {
        recentProjects.clear();
        for (int i = 0; i < MAX_RECENT; i++) {
            String path = prefs.get(RECENT_PROJECT_KEY + i, null);
            if (path != null) {
                recentProjects.add(path);
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
