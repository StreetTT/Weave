import java.io.File;
import java.net.URISyntaxException;
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
    private static final int PROCESS_ERROR = 3;

    public String platformLib;

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
            if (this.signalArray.get(pids[i]) == PROCESS_SLEEPING) {
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

    public void CreatePythonProcess(int pid, String process) {
        long procesHandle = this.CreatePythonProcess(process);
        processHandles[pid] = procesHandle;
    }

    public byte[] GetSignalArray(int[] pids) {
        byte[] result = new byte[pids.length];
        for (int i = 0; i < pids.length; ++i) {
            result[i] = this.signalArray.get(pids[i]);
        }

        return result;
    }

    public void resetSignalArray(int[] pids) {
        for (int i = 0; i < pids.length; ++i) {
            this.signalArray.put(pids[i], (byte)PROCESS_SLEEPING);
        }
    }

    public boolean allProcessesFinished(int[] pids) {
        for (int i = 0; i < pids.length; ++i) {
            int processSignal = this.signalArray.get(pids[i]);
            if (processSignal != PROCESS_FINISHED && processSignal != PROCESS_ERROR) {
                return false;
            }
        }

        return true;
    }

    static {
        String os = System.getProperty("os.name");
        if (os.contains("Windows")) {
            try {
                File exeDir = new File(WeaveNativeImpl.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
                System.load(exeDir.getAbsolutePath() + "/weave_native.dll");
            } catch (URISyntaxException e) {
                System.err.println("Unable to find executable directory");
                e.printStackTrace();
            }
        } else if (os.contains("inux")) {
            try {
                File exeDir = new File(WeaveNativeImpl.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
                System.load(exeDir + "/weave_native.so");
            } catch (URISyntaxException e) {
                System.err.println("Unable to find executable directory");
                e.printStackTrace();
            }

        }
    }
}
