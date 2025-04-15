import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Optional;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Scheduler {
    private static Scheduler singleton_ref = null;
    private static final String PROCESS_FILE_HEADER = "import weave_shared as __WEAVE\n";
    private static final String PROCESS_FILE_FOOTER = "__WEAVE.__WEAVE_PROCESS_END()";
    private static final String FUNCTION_HEADER = "process_func_block_";
    private static final String DIRECTORY_SEPARATOR = FileSystems.getDefault().getSeparator();
    private static final String INDENT = "        ";
    private static final String DIRECTORY_SEPERATOR = FileSystems.getDefault().getSeparator();

    public String projectName;
    public String projectDir;
  
    private Scheduler() {
    }


    //NOTE(Ray) Must be a singleton never instantiate more than once
    public static Scheduler Scheduler() {
        if (singleton_ref == null) {
            singleton_ref = new Scheduler();
        }

        return singleton_ref;
    };

    public StringBuilder getProcessFileContent(int process) {
        String filename = this.projectName + "_PROCESS_" + process + ".py";
        String fileSeperator = FileSystems.getDefault().getSeparator();

        String fullFilepath = this.projectDir + fileSeperator + filename;

        File file = new File(fullFilepath);
        Path path = Paths.get(fullFilepath);

        try {
            return new StringBuilder(Files.readString(path)); // Read Entire File
        } catch (IOException e) {
            return new StringBuilder();
        }
    }
    private String getFilenameFromIdx(int idx) {
        return this.projectName + "_PROCESS_" + (idx + 1) + ".py";
    }

    private String getBlockFuctionStringFromIdx(int i) {
        return FUNCTION_HEADER + (i) + "()";
    }

    private int getPIDFromIDX(int i) {
        return i + 1;
    }

    public File showSaveDialogBox() {
        // Show save dialog box
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose Project Directory");

        // Give the dialog a starting point
        String defaultPath = 
            Scheduler.Scheduler().projectDir != null 
            ? Scheduler.Scheduler().projectDir 
            : System.getProperty("user.home") + FileSystems.getDefault().getSeparator() + "Documents";
        dirChooser.setInitialDirectory(new File(defaultPath));

        // Show dialog and get selected directory
        return dirChooser.showDialog(new Stage());
    }

    private String forceProjectName() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Project Name");
        dialog.setHeaderText("You must name your project first.");
        dialog.setContentText("Project name:");
        dialog.getEditor().setText("untitledProject");
        
        // Show dialog and wait for response
        Optional<String> result = dialog.showAndWait();
        
        // Update project name if user entered one
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            Scheduler.Scheduler().projectName = result.get().trim();
            return Scheduler.Scheduler().projectName;
        }
        
        return null;
    }
    
    public boolean writeProcessesToDisk(ArrayList<WeaveProcess> processes, String folder) {
        // Force the user to name the project if it hasn't been done already
        while (Scheduler.Scheduler().projectName == null) {
            forceProjectName();
        }


        for (int processIdx = 0; processIdx < processes.size(); ++processIdx) {
            //output the headers
            WeaveProcess process = processes.get(processIdx);
            StringBuilder fullFileString = new StringBuilder();
            fullFileString.append(PROCESS_FILE_HEADER);
            fullFileString.append("__WEAVE.__WEAVE_PROCESS_START(" + getPIDFromIDX(processIdx) + ")\n");
            String filename = this.getFilenameFromIdx(processIdx);

            String fullFilepath = this.projectDir + "/" + folder + "/" + filename;
            Path path = Paths.get(fullFilepath);

             //TODO:(Ray) Have exceptions throw up error windows in javafx
            // output one function per block
            for (int blockIdx = 0; blockIdx < process.largestIndex+1; ++blockIdx) {
                Block block = process.blocks[blockIdx];

                //NOTE(Ray): Must surround the entire code in a try finally block, without this, if it encounters an exception
                // it will terminate and not Release the mutex, creating a deadlock

                fullFileString.append("def " + getBlockFuctionStringFromIdx(blockIdx) + ":\n    try:\n        ");
                if (block != null) {
                    StringBuilder blockContentStr = block.fileContents;
                    for (int charIdx = 0; charIdx < blockContentStr.length(); ++charIdx) {
                        char c = blockContentStr.charAt(charIdx);
                        if (c == '\t') {
                            fullFileString.append(INDENT);  // convert tabs to spaces
                            continue;
                        }

                        // ignore whitespace
                        if (c == '\r' || c == '\f') {
                            continue;
                        }

                        fullFileString.append(c);
                        if (c == '\n') {
                            fullFileString.append(INDENT); // indent after newlines
                        }
                    }
                }

                // python doesn't allow empty functions appending 'pass' will keep python happy
                fullFileString.append("\n" + INDENT + "pass");
                fullFileString.append("\n\n");

                // release the mutex even if an exception occurs
                fullFileString.append("    finally:\n");
                if (blockIdx != process.largestIndex) {
                    fullFileString.append(INDENT + "__WEAVE.__WEAVE_WAIT_TILL_SCHEDULED()\n");
                } else {
                    fullFileString.append(INDENT + "__WEAVE.__WEAVE_PROCESS_END()\n");
                }
            }

            // output all function calls
            for (int blockIdx = 0; blockIdx < process.largestIndex; ++blockIdx) {
                fullFileString.append(getBlockFuctionStringFromIdx(blockIdx) + "\n");
            }
            fullFileString.append(getBlockFuctionStringFromIdx(process.largestIndex) + "\n");
            try {
                Files.write(path, fullFileString.toString().getBytes(),
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.SYNC);
            } catch (IOException e) {
                System.err.println("FAILED TO SAVE A PROCESS FILE TO DISK!!");
                e.printStackTrace();
                return false;
            }
        }

        saveProjectFile(processes, this.projectName);
        return true;
    }

    public File showOpenDialogBox() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Weave Project");
        
        // Only allow .wve files
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Weave Files", "*.wve")
        );
        
        // Set initial directory
        String defaultPath = System.getProperty("user.home") + DIRECTORY_SEPARATOR + "Documents";
        fileChooser.setInitialDirectory(new File(defaultPath));
        
        // Show open dialog
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        
        return selectedFile;
    }


    // TODO(Ray): Can test but probably not using JUnit
    public void runProcesses(ArrayList<WeaveProcess> processes) {
        final String outputDir = "outFiles";
        this.writeProcessesToDisk(processes, outputDir);
        SharedMemory s = SharedMemory.SharedMemory();
        int[] pids = new int[processes.size()];

        for (int i = 0; i < processes.size(); ++i) {
            int pid = getPIDFromIDX(i);
            SharedMemory.WaitForProcess(pid);
            pids[i] = pid;
        }

        //NOTE(Ray): Reader Thread sits in the background just polling to see if processes have written anything to stdout
        // On windows, the stdout pipe will block all processes if it becomes full. The thread allows us to keep removing data
        // from the pipe without introducing any extra waiting latency for the main thread or python processes.

        SharedMemory.ReaderThreadStart();
        for (int i = 0; i < processes.size(); ++i) {
            // don't need to use default file seperator here, the native C code will do the conversion for us
            String pythonFile = this.projectDir + "/" + outputDir + "/" + this.getFilenameFromIdx(i);
            s.CreatePythonProcess(pids[i], pythonFile);
        }

        while (!s.allProcessesFinished(pids)) {
            s.RunPidsAndWait(pids);
        }

        SharedMemory.ReaderThreadStop(); // Call this otherwise thread is just wasting system resources

        // NOTE(Ray): Need to release all mutexes here, since on when we enter the function again we will try to acquire
        //  a mutex that we already own and we will deadlock

        for (int i = 0; i < processes.size(); ++i) {
            SharedMemory.ReleaseProcess(getPIDFromIDX(i));
        }

        s.resetSignalArray(pids);

        ByteBuffer buffer = SharedMemory.GetProcessesOutput();
        System.out.println(StandardCharsets.UTF_8.decode(buffer));

    }

    //TODO(Ray): 100% can unit test this function
    public void saveProjectFile(ArrayList<WeaveProcess> processes, String filename) {
        Path path = Paths.get(this.projectDir + "/" + filename + ".wve");
        ByteBuffer bytesToWrite = ByteBuffer.allocate(255 * processes.size());
        bytesToWrite.order(ByteOrder.LITTLE_ENDIAN);

        bytesToWrite.putInt(this.projectName.toCharArray().length * Character.BYTES);

        for (char c: this.projectName.toCharArray()) {
            bytesToWrite.putChar(c);
        }

        bytesToWrite.putInt(processes.size());

        for (int i = 0; i < processes.size(); ++i) {
            WeaveProcess process = processes.get(i);
            bytesToWrite.putInt(process.largestIndex + 1);
            for (int j = 0; j <= process.largestIndex; ++j) {
                Block block = process.blocks[j];
                if (block == null) {
                    bytesToWrite.put((byte)0);
                } else {
                    bytesToWrite.put((byte)1);
                }
            }
        }

        try {
            Files.write(path, bytesToWrite.array());
        } catch (IOException e) {
            System.err.println("COULDN'T WRITE PROJECT FILE");
            e.printStackTrace();
        }
    }
}