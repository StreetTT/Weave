import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.FileSystems;

import static java.lang.System.getProperty;

public class Scheduler {
    private static Scheduler singleton_ref = null;
    public static final int MAX_BLOCKS = 256*1024;

    //TODO(Ray) maybe change to dynamic arrays
    public String projectName;
    public String projectDir;
    private int[] blockPids;
    private int[] blockTimes;
    private String[] processFilenames;

    // zero is always reserved for invalid processes
    private int numProcesses = 1;

    private LongBuffer mutexBuffer;
    private ByteBuffer signalBuffer;

    private Scheduler() {
        SharedMemory.AlocWeaveSharedBuffer();
        // NOTE:(Ray) Never use more than 255 as the argument to this function
        // maybe make allocWeaveSharedBuffer take in paramters
        this.mutexBuffer = SharedMemory.GetSharedMutexBuffer(255);
        this.signalBuffer = SharedMemory.GetSignalArray();

        this.blockPids  = new int[MAX_BLOCKS];
        this.blockTimes  = new int[MAX_BLOCKS];
        this.processFilenames = new String[256];

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
        try {
            file.createNewFile();
        } catch (IOException e) {
            return 0;
        }

        return pid;
    }

    public static void main(String args[]) {
        Scheduler s = new Scheduler();
        s.projectName = "TEST_PROJ";
        s.projectDir = args[0];
        s.addProcess();
    }
}
