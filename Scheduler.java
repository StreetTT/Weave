import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scheduler {
    private static Scheduler singleton_ref = null;
    private static final String PROCESS_FILE_HEADER = "import weave_shared as __WEAVE\n__WEAVE.__WEAVE_PROCESS_START(1)\n";
    private static final String PROCESS_FILE_FOOTER = "__WEAVE.__WEAVE_PROCESS_END()";
    private static final int BLOCK_PER_PROCESS = 1024;
    private static final int MAX_PROCESSES = 256;

    public static final int MAX_BLOCKS = MAX_PROCESSES*BLOCK_PER_PROCESS;

    public String projectName;
    public String projectDir;

    private int[] blocksFileStartIdx;
    private int[] blocksFileEndIdx;
    private StringBuilder blocksFileContents[];

    //NOTE:(Ray) All attributes are allocated as an array to keep data together for bulk processing.
    // A pid (ProcessId) is just an index into anyone of these arrays
    // A block index is simply just the (blockid + pid * MAX_PROCESSES)
    // pid=0 is always reserved for invalid processes

    // For the frontend the above isn't relavent and it can just pass around PIDs and BlockIDs to the scheduler functions

    public String[] processFilenames;
    public StringBuilder[] processFileContents; //NOTE(Ray): Maybe this are redundant
    public int[] processBlockCount;

    private int numProcesses = 1;

    private Scheduler() {
        this.processFilenames = new String[256];
        this.processFileContents = new StringBuilder[256];

        // Process Block arrays
        this.processBlockCount = new int[256];
        this.blocksFileStartIdx = new int[MAX_BLOCKS];
        this.blocksFileEndIdx = new int[MAX_BLOCKS];

        this.blocksFileContents = new StringBuilder[MAX_BLOCKS];

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

    public int addProcess() {
        int pid = this.numProcesses++;  // get the PID
        String filename = this.projectName + "_PROCESS_" + pid + ".py";
        String fileSeperator = FileSystems.getDefault().getSeparator();

        String fullFilepath = this.projectDir + fileSeperator + filename;

        File file = new File(fullFilepath);
        Path path = Paths.get(fullFilepath);

        try {
            this.processFileContents[pid] = new StringBuilder(Files.readString(path)); // Read Entire File
            if (!file.createNewFile()) {
                this.processFileContents[pid].append(PROCESS_FILE_HEADER);
            }

        } catch (IOException e) {
            return 0;
        }


        return pid;
    }

    public int addProcessBlock(int pid) {
        // Use the pid to claim the variables and array space needed for a block
        int blockIdx = this.processBlockCount[pid] + (pid * BLOCK_PER_PROCESS);
        ++this.processBlockCount[pid];

        this.blocksFileContents[blockIdx] = new StringBuilder();

        //NOTE(Ray): Not too sure if should parse python file with regex or serilse the positions of block content
        this.blocksFileStartIdx[blockIdx] = 0;
        this.blocksFileEndIdx[blockIdx] = 0;

        return blockIdx;
    }

    public StringBuilder getBlockContents(int block) {
        return this.blocksFileContents[block];
    }

    public void writeProcessesToDisk() {
        for (int process = 1; process < this.numProcesses; ++process) {
            StringBuilder fullFileString = new StringBuilder();
            fullFileString.append(PROCESS_FILE_HEADER);

            int blockIDStart = process * BLOCK_PER_PROCESS;
            String filename = this.projectName + "_PROCESS_" + process + ".py";
            String fullFilepath = this.projectDir + FileSystems.getDefault().getSeparator() + filename;

            Path path = Paths.get(fullFilepath);

            for (int block = blockIDStart; block < blockIDStart + this.processBlockCount[process]; ++block) {
                StringBuilder blockContentStr = this.getBlockContents(block);
                fullFileString.append("def process_func_block_" + (block - blockIDStart) + "():\n    ");

                for (int i = 0; i < blockContentStr.length(); ++i) {
                    char c = blockContentStr.charAt(i);
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

                // python doesn't allow empty functions appending 'pass' will keep python happy
                fullFileString.append("\n    pass");
                fullFileString.append("\n\n");
            }

            fullFileString.append(PROCESS_FILE_FOOTER);
            try {
                Files.write(path, fullFileString.toString().getBytes());
            } catch (IOException e) {
                System.err.println("FAILED TO SAVE A PROCESS FILE TO DISK!!");
            }
        }
    }

    public static void main(String args[]) {
        Scheduler s = new Scheduler();
        s.projectName = "TEST_PROJ";
        s.projectDir = args[0];
        s.addProcess();
    }
}