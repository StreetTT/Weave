import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Optional;
import javafx.scene.control.TextInputDialog;



//manages the saving, loading, writing, and running weave processes
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


    //reads the content of a specific python process file from disk
    public StringBuilder getProcessFileContent(int process) {
        //constructs the expected filename
        String filename = this.projectName + "_PROCESS_" + process + ".py";
        String fileSeperator = FileSystems.getDefault().getSeparator();


        //constructs full path
        String fullFilepath = this.projectDir + fileSeperator + filename;

        File file = new File(fullFilepath);
        Path path = Paths.get(fullFilepath);

        try {
            //reads the entire file content
            return new StringBuilder(Files.readString(path));
        } catch (IOException e) {
            //returns empty if file not found or error occours
            return new StringBuilder();
        }
    }

    //generates the python filename based on the process index
    private String getFilenameFromIdx(int idx) {
        return this.projectName + "_PROCESS_" + (idx + 1) + ".py";
    }



    //generates the python function call string based on the block index
    private String getBlockFuctionStringFromIdx(int i) {
        return FUNCTION_HEADER + (i) + "()";
    }

    //gets the process id used by native code, basied on the list index
    private int getPIDFromIDX(int i) {
        return i + 1;
    }


    //forces the user to enter a project name using a dialog box
    private String forceProjectName() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Project Name");
        dialog.setHeaderText("You must name your project first.");
        dialog.setContentText("Project name:");
        dialog.getEditor().setText("untitledProject");

        //shows the dialog and waits for user input
        Optional<String> result = dialog.showAndWait();

        //updates the project name if the user entered something valid
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            Scheduler.Scheduler().projectName = result.get().trim();
            return Scheduler.Scheduler().projectName;
        }
        
        return null;
    }


    //writes the python code for all processes to disk in the specified subfolde
    public boolean writeProcessesToDisk(ArrayList<WeaveProcess> processes, String folder) {
        //makes sure a project name is set, prompting if necessary
        while (Scheduler.Scheduler().projectName == null) {
            forceProjectName();
        }

        //constructs the full directory path for the output files
        String dirpath = this.projectDir + "/" + folder;


        try {
            //creates the output directory if it doesn't exist
            Files.createDirectories(Paths.get(dirpath));
        }  catch (IOException e) {
            System.err.println("Creating process directory failed");
            e.printStackTrace();
            return false;
        }
        //creates the lib subdirectory for native libraries
        String libpath = dirpath + "/lib";
        try {
        Files.createDirectories(Paths.get(libpath));
        } catch (IOException e) {
            System.err.println("Error creating lib directory");
            e.printStackTrace();
            return false;
        }

        try {
            //copies the necessary runtime files (shared python module, native libs)
            Files.copy(Paths.get("./weave_shared.py"), Paths.get(dirpath + "/weave_shared.py"), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get("./lib/weave_native.dll"), Paths.get(libpath + "/weave_native.dll"), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get("./lib/weave_native.so"), Paths.get(libpath + "/weave_native.so"), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        }  catch (IOException e) {
            System.err.println("Failed to copy over weave runtime files");
            e.printStackTrace();
            return false;
        }


        //loops through each process to generate its python file
        for (int processIdx = 0; processIdx < processes.size(); ++processIdx) {
            //output the headers
            WeaveProcess process = processes.get(processIdx);
            StringBuilder fullFileString = new StringBuilder();

            //adds the standard header and the process start call

            fullFileString.append(PROCESS_FILE_HEADER);
            fullFileString.append("__WEAVE.__WEAVE_PROCESS_START(" + getPIDFromIDX(processIdx) + ")\n");
            String filename = this.getFilenameFromIdx(processIdx);


            //constructs the full path for this process file
            String fullFilepath = dirpath + "/" + filename;
            Path path = Paths.get(fullFilepath);

            //generates one python function for each potential block slot
            for (int blockIdx = 0; blockIdx < process.largestIndex+1; ++blockIdx) {
                Block block = process.blocks[blockIdx];

                //starts the function definition with a try block for error handling

                fullFileString.append("def " + getBlockFuctionStringFromIdx(blockIdx) + ":\n    try:\n        ");
                if (block != null) {
                    StringBuilder blockContentStr = block.fileContents;
                    //iterates through the block's code characters
                    for (int charIdx = 0; charIdx < blockContentStr.length(); ++charIdx) {
                        char c = blockContentStr.charAt(charIdx);
                        //converts tabs to standard indentation
                        if (c == '\t') {
                            fullFileString.append(INDENT);
                            continue;
                        }

                        //ignores carriage return and form feed characters
                        if (c == '\r' || c == '\f') {
                            continue;
                        }
                        //appends the character
                        fullFileString.append(c);
                        //adds indentation after newlines
                        if (c == '\n') {
                            fullFileString.append(INDENT);
                        }
                    }
                }

                //adds 'pass' if the block was empty or null to make python happy
                fullFileString.append("\n" + INDENT + "pass");
                fullFileString.append("\n\n");

                //adds the except block to catch errors, signal error state, and re-raise
                fullFileString.append("    except Exception as e:\n" + INDENT + "__WEAVE.__WEAVE_PROCESS_END_ERROR()\n" + INDENT + "raise e\n\n");
                if (blockIdx != process.largestIndex) {
                    fullFileString.append("    __WEAVE.__WEAVE_WAIT_TILL_SCHEDULED()\n");
                    //adds the process end call for the last block
                } else {
                    fullFileString.append("    __WEAVE.__WEAVE_PROCESS_END()\n");
                }
            }

            //writes the function calls in sequence
            for (int blockIdx = 0; blockIdx < process.largestIndex; ++blockIdx) {
                fullFileString.append(getBlockFuctionStringFromIdx(blockIdx) + "\n");
            }
            //writes the last function call
            fullFileString.append(getBlockFuctionStringFromIdx(process.largestIndex) + "\n");
            try {
                //write the generated python code to the file
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


    //runs the specific process using native weave layer
    public byte[] runProcesses(ArrayList<WeaveProcess> processes) {
        final String outputDir = "outFiles";
        this.writeProcessesToDisk(processes, outputDir);
        WeaveNative wn = WeaveNativeFactory.get();
        int[] pids = new int[processes.size()];

        //prepares the native layer to wait for each process
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


        byte[] results = wn.GetSignalArray(pids); // happens before reset

        wn.resetSignalArray(pids);
        return results;
    }

    //saves the project structure (process names, block layout) to a .wve file
    public boolean saveProjectFile(ArrayList<WeaveProcess> processes) {
        Path path = Paths.get(this.projectDir + "/" + this.projectName + ".wve");
        // should be enough for all proceses and the headers
        ByteBuffer bytesToWrite = ByteBuffer.allocate(512 * (processes.size() + 1) + projectName.length()*Character.BYTES);

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
            bytesToWrite.putInt(Math.min(process.name.length(), 80));

            // cap it at 80 chars max
            for (int c = 0; c < process.name.length() && c < 80; ++c) {
                bytesToWrite.putChar(process.name.charAt(c));
            }

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