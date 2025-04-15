import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
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
    public String projectName;
    public String projectDir;
  
    private Scheduler() {
        //TODO(Ray) eventually serialise block pids and times from file
    }


    //NOTE(Ray) Must be a singleton never instantiate more than once
    public static Scheduler Scheduler() {
        if (singleton_ref == null) {
            singleton_ref = new Scheduler();
        }

        return singleton_ref;
    };

    public void runProcesses() {
        //TODO(Ray) implement scheduling algorithm
    }

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

    public File showSaveDialogBox(){
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
    
    public byte[] createApplicationStateFile(){
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024); // 1MB initial size
        
            // Write magic number to identify file type
            buffer.putInt(0x57454156); // "WEAV" in ASCII
            buffer.putShort((short)1); // Version number
            //TODO(Ray) implement this function to save the current state of the application

            byte[] result = new byte[buffer.position()];
            buffer.rewind();
            buffer.get(result);
            return result;
            
        } catch (Exception e) {
            System.err.println("Failed to create binary data: " + e.getMessage());
            return null;
        }
    }

    public boolean writeProcessesToDisk(ArrayList<WeaveProcess> processes) {
        return writeProcessesToDisk(processes, null);
    }
        
    public boolean writeProcessesToDisk(ArrayList<WeaveProcess> processes, File fileDir) {
        // Force the user to name the project if it hasn't been done already
        while (Scheduler.Scheduler().projectName == null) {
            forceProjectName();
        }
        
        // If the project directory is not given, use the one already set if there is one
        if (fileDir == null && Scheduler.Scheduler().projectDir != null) {
            fileDir = new File(Scheduler.Scheduler().projectDir);
        }

        // If the project directory is still not set, show the save dialog box
        if (fileDir == null || !fileDir.exists()) {
            fileDir = showSaveDialogBox();           
            if (fileDir == null){ // If clicked cancel, then don't save
                return false;
            }
        } 
        this.projectDir = fileDir.getAbsolutePath();

        // Create the binary file
        File wveFile = new File(this.projectDir + DIRECTORY_SEPARATOR + this.projectName + ".wve");
        try {
            Files.write(wveFile.toPath(), createApplicationStateFile());
        } catch (IOException e) {
            System.err.println("FAILED TO WRITE WVE FILE TO DISK!!");
            e.printStackTrace();
            return false;
        }

        for (int processIdx = 0; processIdx < processes.size(); ++processIdx) {
            //output the headers
            WeaveProcess process = processes.get(processIdx);
            StringBuilder fullFileString = new StringBuilder();
            fullFileString.append(PROCESS_FILE_HEADER);
            fullFileString.append("__WEAVE.__WEAVE_PROCESS_START(" + getPIDFromIDX(processIdx) + ")\n");
            String filename = this.getFilenameFromIdx(processIdx);

            String fullFilepath = this.projectDir + DIRECTORY_SEPARATOR + filename;
            Path path = Paths.get(fullFilepath);

             //TODO:(Ray) Have exceptions throw up error windows in javafx
            // output one function per block
            for (int blockIdx = 0; blockIdx < process.largestIndex+1; ++blockIdx) {
                Block block = process.blocks[blockIdx];
                fullFileString.append("def " + getBlockFuctionStringFromIdx(blockIdx) + ":\n    ");
                if (block != null) {
                    StringBuilder blockContentStr = block.fileContents;
                    for (int charIdx = 0; charIdx < blockContentStr.length(); ++charIdx) {
                        char c = blockContentStr.charAt(charIdx);
                        if (c == '\t') {
                            fullFileString.append("    ");  // convert tabs to spaces
                            continue;
                        }

                        // ignore whitespace
                        if (c == '\r' || c == '\f') {
                            continue;
                        }

                        fullFileString.append(c);
                        if (c == '\n') {
                            fullFileString.append("    "); // indent after newlines
                        }
                    }
                }

                // python doesn't allow empty functions appending 'pass' will keep python happy
                fullFileString.append("\n    pass");
                fullFileString.append("\n\n");
            }

            // output all function calls
            for (int blockIdx = 0; blockIdx < process.largestIndex; ++blockIdx) {
                fullFileString.append(getBlockFuctionStringFromIdx(blockIdx) + "\n");
                fullFileString.append("__WEAVE.__WEAVE_WAIT_TILL_SCHEDULED()\n");
            }

            // the last one doesn't need to wait
            fullFileString.append(getBlockFuctionStringFromIdx(process.largestIndex) + "\n");
            fullFileString.append(PROCESS_FILE_FOOTER);
            try {
                Files.write(path, fullFileString.toString().getBytes(),
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.SYNC);
            } catch (IOException e) {
                System.err.println("FAILED TO SAVE A PROCESS FILE TO DISK!!");
                e.printStackTrace();
            }
        }
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

    private boolean readApplicationStateFile(byte[] data) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            
            // Verify magic number
            int magic = buffer.getInt();
            if (magic != 0x57454156) { // "WEAV"
                throw new IllegalArgumentException("Invalid file format: incorrect magic number");
            }
            
            // Check version
            short version = buffer.getShort();
            if (version != 1) {
                throw new IllegalArgumentException("Unsupported version: " + version);
            }
            //TODO(Ray): Read the binary file and load the application state
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to read application state: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean writeProcessesFromDisk(File file){
        this.projectDir = file.getParent();
        this.projectName = file.getName().substring(0, file.getName().length() - 4); // remove the .wve extension
        
        try {
            readApplicationStateFile(Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            System.err.println("FAILED TO READ WVE FILE TO DISK!!");
            e.printStackTrace();
            return false;
        }
        //TODO(Ray): Read the processes from the disk and load them into the application state
        
        return true; 
    }

    public void runProcesses(ArrayList<WeaveProcess> processes) {
        final String outputDir = "outFiles";
        this.writeProcessesToDisk(processes);
        SharedMemory s = SharedMemory.SharedMemory();
        int[] pids = new int[processes.size()];

        for (int i = 0; i < processes.size(); ++i) {
            int pid = getPIDFromIDX(i);
            SharedMemory.WaitForProcess(pid);
            pids[i] = pid;
        }

        //NOTE(Ray): Reader Thread sits in the background just polling to see if processes have written anything to stdout
        // On windows, the stdout pipe will block all processes if it becomes full. The thread allows us to keep removing data
        // from the pipe without introducing any extra waiting latency for the main thread or WEAVE processes.

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

        // NOTE(Ray): Need to release all mutexes here, since on when we enter the function again we will try to aquire
        //  a mutex that we already own and we will deadlock

        for (int i = 0; i < processes.size(); ++i) {
            SharedMemory.ReleaseProcess(getPIDFromIDX(i));
        }

        s.resetSignalArray(pids);

        ByteBuffer buffer = SharedMemory.GetProcessesOutput();
        System.out.println(StandardCharsets.UTF_8.decode(buffer));

    }
}