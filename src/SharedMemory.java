import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

//TODO(Ray): Obfuscate the sharedmemroy name

public class SharedMemory {
    static private native void Init();
    static private native ByteBuffer GetSignalArray();
    static public native void DeInit();
    static public native void ReleaseProcess(int pid);
    static public native void WaitForProcess(int pid);
    static public native ByteBuffer GetProcessesOutput();
    static private native long CreatePythonProcess(String process);
    static private native boolean isProcessAlive(long pHandle);
    static public native void ReaderThreadStart();
    static public native void ReaderThreadStop();

    private static final int PROCESS_SLEEPING = 0;
    private static final int PROCESS_AQUIRED = 1;
    private static final int PROCESS_FINISHED = 2;

    private static SharedMemory singletonRef;
    private ByteBuffer signalArray;
    private long[] processHandles;

    private SharedMemory() {
        SharedMemory.Init();
        this.signalArray = SharedMemory.GetSignalArray();
        this.processHandles = new long[256];
    };

    static public SharedMemory SharedMemory() {
        if (singletonRef == null) {
            singletonRef = new SharedMemory();
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
            SharedMemory.ReleaseProcess(activeProcesses[i]);
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
        long procesHandle = SharedMemory.CreatePythonProcess(process);
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
        System.loadLibrary("./lib/weave_native");
    }

    //TODO(Ray): Convert this into a more general Run function inside the scheduler
    //TODO(Ray): stop using stdio to debug and use sharedmemeory instead
    //TODO(Ray): Move testing code out of main
    public static void main(String args[]) {
        SharedMemory s = SharedMemory.SharedMemory();
        ByteBuffer signalArray = s.signalArray;

        SharedMemory.WaitForProcess(1);
        SharedMemory.WaitForProcess(2);

        for (int i = 0; i < signalArray.capacity(); ++i) {
            signalArray.put(i, (byte)0);
        }

        for (int i = 0; i < signalArray.capacity(); ++i) {
            System.out.print(signalArray.get(i));
        }

        System.out.println();
        System.out.println("------------------------------------");

        long p1 = SharedMemory.CreatePythonProcess("pyTest/program1.py");
        long p2 = SharedMemory.CreatePythonProcess("pyTest/program2.py");

        s.RunPidsAndWait(new int[]{2});

        for (int i = 0; i < signalArray.capacity(); ++i) {
            System.out.print(signalArray.get(i));
        }

        System.out.println();
        System.out.println("------------------------------------");

        s.RunPidsAndWait(new int[]{1});

        for (int i = 0; i < signalArray.capacity(); ++i) {
            System.out.print(signalArray.get(i));
        }
        System.out.println();
        System.out.println("------------------------------------");

        s.RunPidsAndWait(new int[]{2, 1});

        for (int i = 0; i < signalArray.capacity(); ++i) {
            System.out.print(signalArray.get(i));
        }

        System.out.println();
        System.out.println("------------------------------------");

        SharedMemory.DeInit();
    }
}
