import java.nio.ByteBuffer;

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
}
