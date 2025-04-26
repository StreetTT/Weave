import java.nio.ByteBuffer;

//defines the interface for interacting with the native (c/c++) backend
public interface WeaveNative {

    //cleans up native resources
    public void DeInit();


    //releases the mutex/signal for a specific process id
    public void ReleaseProcess(int pid);

    //waits for (acquires) the mutex/signal for a specific process id
    public void WaitForProcess(int pid);

    //gets the combined stdout/stderr output from all run processes
    public ByteBuffer GetProcessesOutput();

    //clears the stored output buffer in the native layer
    public void ClearProcessOutput();

    //starts the background thread that reads process output pipes
    public void ReaderThreadStart();

    //stops the background output reader thread
    public void ReaderThreadStop();

    //coordinates running multiple pids: releases waiting ones, waits for signals
    public void RunPidsAndWait(int[] pids);

    //creates a new python process associated with a pid and runs the specified script
    public void CreatePythonProcess(int pid, String process);

    //resets the status signals for the given pids in the shared memory array
    public void resetSignalArray(int[] pids);

    //gets the current status signals for the given pids from shared memory
    public byte[] GetSignalArray(int[] pids);

    //checks if all the given pids have signaled finished or error status
    public boolean allProcessesFinished(int[] pids);
}
