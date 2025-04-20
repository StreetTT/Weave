import java.nio.ByteBuffer;

//TODO(Ray): Obfuscate the sharedmemroy name

public class WeaveNativeImpl implements WeaveNative {
    private native void Init();
    private native ByteBuffer GetSignalArray();
    public native void DeInit();
    public native void ReleaseProcess(int pid);
    public native void WaitForProcess(int pid);
    public native ByteBuffer GetProcessesOutput();
    public native void ClearProcessOutput();
    private native long CreatePythonProcess(String process);
    private native boolean isProcessAlive(long pHandle);
    public native void ReaderThreadStart();
    public native void ReaderThreadStop();

    private static final int PROCESS_SLEEPING = 0;
    private static final int PROCESS_ACQUIRED = 1;
    private static final int PROCESS_FINISHED = 2;

    private static WeaveNativeImpl singletonRef;
    private ByteBuffer signalArray;
    private long[] processHandles;

    private WeaveNativeImpl() {
        this.Init();
        this.signalArray = this.GetSignalArray();
        this.processHandles = new long[256];
    };

    static public WeaveNativeImpl WeaveNativeImpl() {
        if (singletonRef == null) {
            singletonRef = new WeaveNativeImpl();
        }

        return singletonRef;
    }

    public void RunPidsAndWait(int[] pids) {
        int activeProcesses[] = new int[pids.length];
        int activeProcessesCount = 0;

        for (int i = 0; i < pids.length; ++i) {
            if (this.signalArray.get(pids[i]) != PROCESS_FINISHED) {
                activeProcesses[activeProcessesCount++] = pids[i];
            }
        }

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
                    this.signalArray.put(activeProcesses[i], (byte)PROCESS_FINISHED); // set process to finished
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
            if (this.signalArray.get(activeProcesses[i]) == PROCESS_FINISHED) {
                continue;
            }

            this.signalArray.put(activeProcesses[i], (byte)0);
        }
    }

    public void CreatePythonProcess(int pid, String process) {
        long procesHandle = this.CreatePythonProcess(process);
        processHandles[pid] = procesHandle;
    }

    public void resetSignalArray(int[] pids) {
        for (int i = 0; i < pids.length; ++i) {
            this.signalArray.put(pids[i], (byte)PROCESS_SLEEPING);
        }
    }

    public boolean allProcessesFinished(int[] pids) {
        for (int i = 0; i < pids.length; ++i) {
            if (this.signalArray.get(pids[i]) != PROCESS_FINISHED) {
                return false;
            }
        }

        return true;
    }

    static {
        String os = System.getProperty("os.name");
        if (os.contains("Windows")) {
            System.loadLibrary("./lib/weave_native.dll");
        } else if (os.contains("inux")) {
            System.load(System.getProperty("user.dir") + "/lib/weave_native.so");
        }
    }

    //TODO(Ray): Write Tests and delete this
    //TODO(Ray): Convert this into a more general Run function inside the scheduler
    //TODO(Ray): stop using stdio to debug and use sharedmemeory instead
    //TODO(Ray): Move testing code out of main
}
