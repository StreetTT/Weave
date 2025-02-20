import java.nio.ByteBuffer;
import java.nio.LongBuffer;

public class Scheduler {
    private static Scheduler singleton_ref = null;
    public static final int MAX_BLOCKS = 256*1024;

    //TODO(Ray) maybe change to dynamic arrays
    private int[] block_pids;
    private int[] block_times;
    private String[] process_filenames;

    private int numProcesses = 0;

    private LongBuffer mutex_buffer;
    private ByteBuffer signal_buffer;

    private Scheduler() {
        this.mutex_buffer = SharedMemory.GetSharedMutexBuffer(256);
        this.signal_buffer = SharedMemory.GetSignalArray();

        this.block_pids  = new int[MAX_BLOCKS];
        this.block_times  = new int[MAX_BLOCKS];
        this.process_filenames = new String[256];

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
        StringBuilder processString = new StringBuilder();
        for (int i = 0; i < numProcesses; ++i) {

        }
        
    }

    public int schedulerAddProcess(String filename) {
        int pid = this.numProcesses++;  // get the PID
        this.process_filenames[pid] = filename;
        return pid;
    }
}
