import java.nio.ByteBuffer;


//concrete implementation of the WeaveNative interface using java native interface
public class WeaveNativeImpl implements WeaveNative {
    //native method declarations - implemented in c/c++
    private native void Init();
    //gets a direct byte buffer reference to the shared signal array
    private native ByteBuffer GetSignalArray();
    //cleans up native resources
    public native void DeInit();
    //releases the native mutex for a given pid
    public native void ReleaseProcess(int pid);
    //acquires the native mutex for a given pid (blocks until available)
    public native void WaitForProcess(int pid);
    //gets a direct byte buffer reference to the shared output buffer
    public native ByteBuffer GetProcessesOutput();
    //clears the shared output buffer
    public native void ClearProcessOutput();
    //creates a python process using the system's python interpreter
    private native long CreatePythonProcess(String process);
    //checks if the process with the given handle is still running
    private native boolean isProcessAlive(long pHandle);
    //starts the background thread in native code for reading process output
    public native void ReaderThreadStart();
    //stops the native output reader thread
    public native void ReaderThreadStop();

    //constants defining process states in the shared signal array
    private static final int PROCESS_SLEEPING = 0;
    private static final int PROCESS_ACQUIRED = 1;
    private static final int PROCESS_FINISHED = 2;
    private static final int PROCESS_ERROR = 3;

    //stores the name of the platform-specific native library file
    public String platformLib;


    private static WeaveNativeImpl singletonRef;
    //java reference to the shared memory signal array
    private ByteBuffer signalArray;
    //array to store native process handles (e.g., pids) returned by CreatePythonProcess
    private long[] processHandles;


    //private constructor for singleton pattern
    private WeaveNativeImpl() {
        //calls the native Init function
        this.Init();
        //gets the reference to the shared signal array from native code
        this.signalArray = this.GetSignalArray();
        //initializes the array to store process handles (size 256 assumes max pids)
        this.processHandles = new long[256];
    };


    //gets the singleton instance of the native implementation
    static public WeaveNativeImpl WeaveNativeImpl() {
        if (singletonRef == null) {
            singletonRef = new WeaveNativeImpl();
        }

        return singletonRef;
    }

    //coordinates running a set of processes, waiting for them to complete their current block
    public void RunPidsAndWait(int[] pids) {
        //creates a list to hold pids that are currently sleeping (ready to run)
        int activeProcesses[] = new int[pids.length];
        int activeProcessesCount = 0;

        //identifies processes in the sleeping state
        for (int i = 0; i < pids.length; ++i) {
            if (this.signalArray.get(pids[i]) == PROCESS_SLEEPING) {
                activeProcesses[activeProcessesCount++] = pids[i];
            }
        }
        //releases the mutexes for the sleeping processes, allowing them to run
        for (int i = 0; i < activeProcessesCount; ++i) {
            WeaveNativeFactory.get().ReleaseProcess(activeProcesses[i]);
        }

        boolean allSignaled = false;
        // spinlock until all mutexes have been aqquired on the python side
        while (!allSignaled) {
            allSignaled = true;
            for (int i = 0; i < activeProcessesCount; ++i) {
                // check if process ungracefully terminated
                if (!isProcessAlive(this.processHandles[activeProcesses[i]])) {
                    int processSignal = this.signalArray.get(activeProcesses[i]);

                    // if terminated without notice
                    if (processSignal != PROCESS_FINISHED && processSignal != PROCESS_ERROR) {
                        this.signalArray.put(activeProcesses[i], (byte)PROCESS_ERROR); // set process to error
                    }

                    continue;
                }

                if (this.signalArray.get(activeProcesses[i]) == PROCESS_SLEEPING) {
                    allSignaled = false;
                }
            }
        }

        for (int i = 0; i < activeProcessesCount; ++i) {
            WaitForProcess(activeProcesses[i]);
        }

        // reset the signal
        for (int i = 0; i < activeProcessesCount; ++i) {
            int processSignal = this.signalArray.get(activeProcesses[i]);
            if (processSignal == PROCESS_FINISHED || processSignal == PROCESS_ERROR) {
                continue;
            }

            this.signalArray.put(activeProcesses[i], (byte)0);
        }
    }


    //creates a python process using the native method and stores its handle
    public void CreatePythonProcess(int pid, String process) {
        long procesHandle = this.CreatePythonProcess(process);
        processHandles[pid] = procesHandle;
    }


    //retrieves the current signal states for the given pids from the shared buffer
    public byte[] GetSignalArray(int[] pids) {
        byte[] result = new byte[pids.length];
        for (int i = 0; i < pids.length; ++i) {
            result[i] = this.signalArray.get(pids[i]);
        }

        return result;
    }

    //resets the signal states for the given pids back to sleeping
    public void resetSignalArray(int[] pids) {
        for (int i = 0; i < pids.length; ++i) {
            this.signalArray.put(pids[i], (byte)PROCESS_SLEEPING);
        }
    }

    //checks if all the specified processes have reached a finished or error state
    public boolean allProcessesFinished(int[] pids) {
        for (int i = 0; i < pids.length; ++i) {
            int processSignal = this.signalArray.get(pids[i]);
            if (processSignal != PROCESS_FINISHED && processSignal != PROCESS_ERROR) {
                return false;
            }
        }

        return true;
    }
    //static initializer block to load the native library when the class is loaded
    static {
        String os = System.getProperty("os.name");
        if (os.contains("Windows")) {
            System.loadLibrary("./lib/weave_native");
        } else if (os.contains("inux")) {
            System.load(System.getProperty("user.dir") + "/lib/weave_native.so");
        }
    }
}
