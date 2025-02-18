public class Scheduler {
    private static Scheduler singleton_ref = null;
    private Scheduler() {
    }


    public static Scheduler Scheduler() {
        if (singleton_ref != null) {
            singleton_ref = new Scheduler();
        }

        return singleton_ref;
    };

    public void runProcesses() {
        //TODO(Ray) implement scheduling algorithm
        //
        // 0 is invalid process id and should be skipped all process blocks will be time orderd and 
        // tagged with their position along the time axis
        // [0, 0, 1, 2] would be 1 process that is invalid, and a second process with ID 1 at position 2 on the time axis
        //
        //
        //
        //
        //  TIME INDEXED ARRAY WHERE EACH OF THE 1024 TIME SLOTS HAS 256 PROCESS SLOTS all vailid pids at index 0-256 
        //  run first and then must wait for the next time block so on and so on
        
    }




    
}
