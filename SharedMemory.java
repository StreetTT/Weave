import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

public class SharedMemory {
    static public native void AlocWeaveSharedBuffer();
    static private native ByteBuffer GetMutexByteArray(int ammount);
    static public LongBuffer GetSharedMutexBuffer(int ammount) {
        return GetMutexByteArray(ammount).order(ByteOrder.nativeOrder()).asLongBuffer();
    }

    static public native ByteBuffer GetSignalArray();
    static public native void FreeWeaveSharedBuffer();
    static public native void ReleaseMutex(long mutex);
    static public native void WaitForMutex(long mutex);
    static public native long CreateProcess(String process);
    static public native long CreateMutex();
    static public native void WaitForMultipleMutex(long mutexArr[], int count); 

    //Make class impossible to instantiate
    private SharedMemory() {};

    static {
        System.loadLibrary("lib/shared_map");
    }

    public static void main(String args[]) {
        SharedMemory.AlocWeaveSharedBuffer();

        LongBuffer sharedMutexBuff = SharedMemory.GetSharedMutexBuffer(2);
        ByteBuffer signalArray = SharedMemory.GetSignalArray();

        System.out.println(sharedMutexBuff.get(0));

        SharedMemory.WaitForMutex(sharedMutexBuff.get(0));
        SharedMemory.WaitForMutex(sharedMutexBuff.get(1));

        long p1 = SharedMemory.CreateProcess("python program1.py");
        long p2 = SharedMemory.CreateProcess("python program2.py");

        SharedMemory.ReleaseMutex(sharedMutexBuff.get(0));
        SharedMemory.ReleaseMutex(sharedMutexBuff.get(1));

        //SPINLOCK SO WE DON't steal the mutex
        while (signalArray.get(0) == 0 || signalArray.get(1) == 0) {
            continue;
        }

        SharedMemory.WaitForMultipleMutex(new long[] {sharedMutexBuff.get(0), 
                sharedMutexBuff.get(1)}, 2);
        signalArray.put(0, (byte)0);
        signalArray.put(1, (byte)0);

        System.out.println("Process 1 & 2 ran first func");

        SharedMemory.ReleaseMutex(sharedMutexBuff.get(0));
        while (signalArray.get(0) == 0) {
            continue;
        }

        SharedMemory.WaitForMultipleMutex(new long[] {sharedMutexBuff.get(0)}, 1);
        signalArray.put(0, (byte)0);
        System.out.println("Process 1 ran func2");

        SharedMemory.ReleaseMutex(sharedMutexBuff.get(1));
        while (signalArray.get(1) == 0) {
            continue;
        }

        SharedMemory.WaitForMultipleMutex(new long[] {sharedMutexBuff.get(1)}, 1);
        signalArray.put(1, (byte)0);
        System.out.println("Process 2 ran func2");
        WaitForMutex(p1);
        WaitForMutex(p2);
        System.out.println("finished");

        SharedMemory.FreeWeaveSharedBuffer();
    }
}
