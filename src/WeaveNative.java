import java.nio.ByteBuffer;

//TODO(Ray): Actually find out what methods should be static and which ones should be instance methods

public interface WeaveNative {
    public void DeInit();
    public void ReleaseProcess(int pid);
    public void WaitForProcess(int pid);
    public ByteBuffer GetProcessesOutput();
    public void ClearProcessOutput();
    public void ReaderThreadStart();
    public void ReaderThreadStop();
    public void RunPidsAndWait(int[] pids);
    public void CreatePythonProcess(int pid, String process);
    public void resetSignalArray(int[] pids);
    public boolean allProcessesFinished(int[] pids);
}
