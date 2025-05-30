import java.nio.ByteBuffer;

//a placeholder implementation of WeaveNative for unsupported platforms or testing
public class WeaveNativeStub implements WeaveNative {
    public WeaveNativeStub() {}
    public void DeInit() {}
    public void ReleaseProcess(int pid) {}
    public void WaitForProcess(int pid) {}
    public void ReaderThreadStart() {}
    public void ReaderThreadStop() {}
    public void RunPidsAndWait(int[] pids) {}
    public void CreatePythonProcess(int pid, String process) {}
    public void resetSignalArray(int[] pids) {}
    public void ClearProcessOutput() {}

    public boolean allProcessesFinished(int[] pids) {
        return true;
    }
    public ByteBuffer GetProcessesOutput() {
        return ByteBuffer.allocate(1);
    }

    public byte[] GetSignalArray(int[] pids) {
        return new byte[pids.length];
    }
}
