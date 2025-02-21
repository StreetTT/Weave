import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.System.getProperty;

public class Scheduler {
    private static Scheduler singleton_ref = null;
    private static final String PROCESS_FILE_HEADER = "import weave_shared as __WEAVE\n__WEAVE.__WEAVE_PROCESS_START(1)\n";
    private static final int BLOCK_PER_PROCESS = 1024;
    private static final int MAX_PROCESSES = 256;


    public static final int MAX_BLOCKS = MAX_PROCESSES*BLOCK_PER_PROCESS;

    public String projectName;
    public String projectDir;
    public int[] blocksFileStartIdx;
    public int[] blocksFileEndIdx;

    //NOTE:(Ray) All attributes are allocated as an array to keep data together for bulk processing.
    // A pid (ProcessId) is just an index into anyone of these arrays
    // A block index is simply just the (blockid + pid * MAX_PROCESSES)
    // pid=0 is always reserved for invalid processes

    public String[] processFilenames;
    public StringBuilder[] processFileContents;
    public int[] processBlockCount;

    private int numProcesses = 1;

    private LongBuffer mutexBuffer;
    private ByteBuffer signalBuffer;

    private Scheduler() {
        SharedMemory.AlocWeaveSharedBuffer();
        //  NOTE:(Ray) Never use more than 256 as the argument to this function
        //  maybe make allocWeaveSharedBuffer take in paramters

        this.mutexBuffer = SharedMemory.GetSharedMutexBuffer(256);
        this.signalBuffer = SharedMemory.GetSignalArray();

        this.processFilenames = new String[256];
        this.processFileContents = new StringBuilder[256]; //NOTE(Ray): Maybe These are redundant

        // Process Block arrays
        this.processBlockCount = new int[256];
        this.blocksFileStartIdx = new int[MAX_BLOCKS];
        this.blocksFileEndIdx = new int[MAX_BLOCKS];

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
        //
        // 0 is invalid process id and should be skipped all process blocks will be time orderd and
        // tagged with their position along the time axis
        // [0, 0, 1, 2] would be 1 process that is invalid, and a second process with ID 1 at position 2 on the time axis
        //
        //
        //
        //
        //  TIME INDEXED ARRAY WHERE EACH OF THE 1024 TIME SLOTS HAS 256 PROCESS SLOTS all vailid pids at index 0-256
        //  run first and then must wait for the next time block so on and so on
        //  MAYBE TREAT PROCESSES AS ENTITIES GIVE THEM UI_X AND UI_Y FOR RENDERING STORE THEIR IDS
        //
    }

    public int addProcess() {
        int pid = this.numProcesses++;  // get the PID
        StringBuilder filename = new StringBuilder(this.projectName);
        filename.append("_PROCESS_");
        filename.append(pid);
        filename.append(".py");
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
        StringBuilder blockString = new StringBuilder("def ");
        blockString.append(projectName);
        blockString.append("_block_1:\n");

        int blockIdx = this.processBlockCount[pid] + (pid * MAX_PROCESSES);
        this.blocksFileStartIdx[blockIdx] = blockString.length() - 1;
        this.blocksFileEndIdx[blockIdx] = blockString.length() - 1;

        return blockIdx;
    }

    public String getBlockInitialContents(int block) {
        int pid = block / MAX_PROCESSES;
        return this.processFileContents[pid].substring(this.blocksFileStartIdx[block], this.blocksFileEndIdx[block]);
    }

    public static void main(String args[]) {
        Scheduler s = new Scheduler();
        s.projectName = "TEST_PROJ";
        s.projectDir = args[0];
        s.addProcess();
    }
}