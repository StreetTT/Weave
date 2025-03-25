import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

public class SharedMemory {
    static private native void AlocWeaveSharedBuffer();
    static private native ByteBuffer GetSignalArray();
    static public native void FreeWeaveSharedBuffer();
    static private native void ReleaseProcess(int pid);
    static private native void WaitForProcess(int pid);
    static public native long CreatePythonProcess(String process);

    private static SharedMemory singletonRef;
    private ByteBuffer signalArray;

    private SharedMemory() {
        SharedMemory.AlocWeaveSharedBuffer();
        this.signalArray = SharedMemory.GetSignalArray();
    };

    static public SharedMemory SharedMemory() {
        if (singletonRef == null) {
            singletonRef = new SharedMemory();
        }

        return singletonRef;
    }

    public void RunPidsAndWait(int[] pids) {
        for (int i = 0; i < pids.length; ++i) {
            SharedMemory.ReleaseProcess(pids[i]);
        }

        boolean allSignaled = false;
        // spinlock until all mutexes have been aqquired on the python side
        while (!allSignaled) {
            allSignaled = true;
            for (int i = 0; i < pids.length; ++i) {
                if (this.signalArray.get(pids[i]) == 0) {
                    allSignaled = false;
                }
            }
        }

        for (int i = 0; i < pids.length; ++i) {
            WaitForProcess(pids[i]);
        }

        // reset the signal
        for (int i = 0; i < pids.length; ++i) {
            this.signalArray.put(pids[i], (byte)0);
        }

    }

    static {
        System.loadLibrary("./lib/shared_map");
    }

    //TODO(Ray): Convert this into a more general Run function inside the scheduler
    //TODO(Ray): stop using stdio to debug and use sharedmemeory instead
    //TODO(Ray): Move testing code out of main
    public static void main(String args[]) {
        SharedMemory s = SharedMemory.SharedMemory();
        ByteBuffer signalArray = SharedMemory.GetSignalArray();

        SharedMemory.WaitForProcess(1);
        SharedMemory.WaitForProcess(2);

        for (int i = 0; i < signalArray.capacity(); ++i) {
            System.out.print(signalArray.get(i));
        }

        for (int i = 0; i < signalArray.capacity(); ++i) {
            System.out.print(signalArray.get(i));
        }

        System.out.println();
        System.out.println("------------------------------------");

        long p1 = SharedMemory.CreatePythonProcess("testproj/program1.py");
        long p2 = SharedMemory.CreatePythonProcess("testproj/program2.py");

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

        SharedMemory.FreeWeaveSharedBuffer();
    }
}
