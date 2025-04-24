import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Optional;
import javafx.scene.control.TextInputDialog;

public class Scheduler {
    private static Scheduler singleton_ref = null;
    private static final String PROCESS_FILE_HEADER = "import weave_shared as __WEAVE\n";
    private static final String FUNCTION_HEADER = "process_func_block_";
    private static final String INDENT = "        ";
    private static final String DIRECTORY_SEPERATOR = FileSystems.getDefault().getSeparator();

    public String projectName;
    public String projectDir;

    public static final int WEAVE_FILE_IDENTIFIER  = (byte)('W') | (((byte)('E')) << 8) | (((byte)'V') << 16) | (((byte)'E') << 24);
    public static final int PROCESS_IDENTIFIER = (byte)('P') | (((byte)'O') << 8) | (((byte)'C') << 16) | (((byte)'B') << 24);

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


        String dirpath = this.projectDir + "/" + folder;


        try {
            Files.createDirectories(Paths.get(dirpath)); // create one if it doens't already exist
        }  catch (IOException e) {
            System.err.println("Creating process directory failed");
            e.printStackTrace();
            return false;
        }

        String libpath = dirpath + "/lib";
        try {
        Files.createDirectories(Paths.get(libpath)); // create one if it doens't already exist
        } catch (IOException e) {
            System.err.println("Error creating lib directory");
            e.printStackTrace();
            return false;
        }

        try {
            Files.copy(Paths.get("./weave_shared.py"), Paths.get(dirpath + "/weave_shared.py"), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get("./lib/weave_native.dll"), Paths.get(libpath + "/weave_native.dll"), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get("./lib/weave_native.so"), Paths.get(libpath + "/weave_native.so"), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        }  catch (IOException e) {
            System.err.println("Failed to copy over weave runtime files");
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

            String fullFilepath = dirpath + "/" + filename;
            Path path = Paths.get(fullFilepath);

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
                fullFileString.append("    except Exception as e:\n" + INDENT + "__WEAVE.__WEAVE_PROCESS_END()\n" + INDENT + "raise e\n\n");
                if (blockIdx != process.largestIndex) {
                    fullFileString.append("    __WEAVE.__WEAVE_WAIT_TILL_SCHEDULED()\n");
                } else {
                    fullFileString.append("    __WEAVE.__WEAVE_PROCESS_END()\n");
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

        return true;
    }


    // TODO(Ray): Can test but probably not using JUnit
    public void runProcesses(ArrayList<WeaveProcess> processes) {
        final String outputDir = "outFiles";
        this.writeProcessesToDisk(processes, outputDir);
        WeaveNative wn = WeaveNativeFactory.get();
        int[] pids = new int[processes.size()];

        for (int i = 0; i < processes.size(); ++i) {
            int pid = getPIDFromIDX(i);
            wn.WaitForProcess(pid);
            pids[i] = pid;
        }

        //NOTE(Ray): Reader Thread sits in the background just polling to see if processes have written anything to stdout.
        // On windows, the stdout pipe will block all processes if it becomes full. The thread allows us to keep removing data
        // from the pipe without introducing any extra waiting latency for the main thread or python processes.

        wn.ReaderThreadStart();
        for (int i = 0; i < processes.size(); ++i) {
            // don't need to use default file seperator here, the native C code will do the conversion for us
            String pythonFile = "\"" + this.projectDir + "/" + outputDir + "/" + this.getFilenameFromIdx(i) + "\"";
            wn.CreatePythonProcess(pids[i], pythonFile);
        }

        while (!wn.allProcessesFinished(pids)) {
            wn.RunPidsAndWait(pids);
        }


        wn.ReaderThreadStop(); // Call this otherwise thread is just wasting system resources

        // NOTE(Ray): Need to release all mutexes here, since on when we enter the function again we will try to acquire
        //  a mutex that we already own and we will deadlock

        for (int i = 0; i < processes.size(); ++i) {
            wn.ReleaseProcess(getPIDFromIDX(i));
        }

        wn.resetSignalArray(pids);

    }

    //TODO(Ray): 100% can unit test this function
    public boolean saveProjectFile(ArrayList<WeaveProcess> processes) {
        Path path = Paths.get(this.projectDir + "/" + this.projectName + ".wve");
        // should be enough for all proceses and the headers
        ByteBuffer bytesToWrite = ByteBuffer.allocate(256 * (processes.size() + 1));

        bytesToWrite.order(ByteOrder.LITTLE_ENDIAN); // little endian on every architecture that matters
        bytesToWrite.putInt(WEAVE_FILE_IDENTIFIER);
        bytesToWrite.putInt(1); // version
        bytesToWrite.putInt(this.projectName.toCharArray().length * Character.BYTES);

        for (char c: this.projectName.toCharArray()) {
            bytesToWrite.putChar(c);
        }

        bytesToWrite.putInt(processes.size());
        for (int i = 0; i < processes.size(); ++i) {
            bytesToWrite.putInt(PROCESS_IDENTIFIER); // 4 bytes
            WeaveProcess process = processes.get(i);
            for (int j = 0; j < 256 / 8; ++j) {
                byte blocksByte = 0;
                for (int k = 0; k < 8; ++k) {
                    Block block = process.blocks[(j * 8) + k];

                    if (block != null) {
                        blocksByte |= (1 << k);
                    }
                }

                bytesToWrite.put(blocksByte);
            }
        }

        //TODO(Ray): CHKSUM

        try {
            Files.write(path, bytesToWrite.array());
        } catch (IOException e) {
            System.err.println("COULDN'T WRITE PROJECT FILE");
            e.printStackTrace();
            return false;
        }

        return true;
    }
}