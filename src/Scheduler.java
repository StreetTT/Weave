import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

public class Scheduler {
    private static Scheduler singleton_ref = null;
    private static final String PROCESS_FILE_HEADER = "import weave_shared as __WEAVE\n";
    private static final String PROCESS_FILE_FOOTER = "__WEAVE.__WEAVE_PROCESS_END()";
    private static final String FUNCTION_HEADER = "process_func_block_";
    private static final String DIRECTORY_SEPERATOR = FileSystems.getDefault().getSeparator();
    public String projectName;
    public String projectDir;
  
    private Scheduler() {
        //TODO(Ray) eventually serialise block pids and times from file
    }


    //NOTE(Ray) Must be a singleton never instantiate more than once
    public static Scheduler Scheduler() {
        if (singleton_ref == null) {
            singleton_ref = new Scheduler();
        }

        return singleton_ref;
    };

    public void runProcesses() {
        //TODO(Ray) implement scheduling algorithm
    }

    public StringBuilder getProcessFileContent(int process) {
        String filename = this.projectName + "_PROCESS_" + process + ".py";
        String fileSeperator = FileSystems.getDefault().getSeparator();

        String fullFilepath = this.projectDir + fileSeperator + filename;

        File file = new File(fullFilepath);
        Path path = Paths.get(fullFilepath);

        try {
            return new StringBuilder(Files.readString(path)); // Read Entire File
        } catch (IOException e) {
            return new StringBuilder();
        }
    }
    private String getFilenameFromIdx(int idx) {
        return this.projectName + "_PROCESS_" + (idx + 1) + ".py";
    }

    private String getBlockFuctionStringFromIdx(int i) {
        return FUNCTION_HEADER + (i) + "()";
    }

    private int getPIDFromIDX(int i) {
        return i + 1;
    }

    public void writeProcessesToDisk(ArrayList<WeaveProcess> processes, String directoryName) {
        String directoryPath = this.projectDir + DIRECTORY_SEPERATOR + directoryName;

        try {
            Files.createDirectories(Paths.get(directoryPath));
        } catch (IOException e) {
            //NOTE:(Ray) Files.createDirectories will NOT throw if the directory already exists
            System.err.println("FAILED TO CREATE DIRECTORIES WHEN SAVING PROCESS TO DISK");
            e.printStackTrace();
        }

        for (int processIdx = 0; processIdx < processes.size(); ++processIdx) {
            //output the headers
            WeaveProcess process = processes.get(processIdx);
            StringBuilder fullFileString = new StringBuilder();
            fullFileString.append(PROCESS_FILE_HEADER);
            fullFileString.append("__WEAVE.__WEAVE_PROCESS_START(" + getPIDFromIDX(processIdx) + ")\n");
            String filename = this.getFilenameFromIdx(processIdx);

            String fullFilepath = directoryPath + DIRECTORY_SEPERATOR + filename;
            Path path = Paths.get(fullFilepath);

             //TODO:(Ray) Have exceptions throw up error windows in javafx
            // output one function per block
            for (int blockIdx = 0; blockIdx < process.largestIndex+1; ++blockIdx) {
                Block block = process.blocks[blockIdx];
                fullFileString.append("def " + getBlockFuctionStringFromIdx(blockIdx) + ":\n    ");
                if (block != null) {
                    StringBuilder blockContentStr = block.fileContents;
                    for (int charIdx = 0; charIdx < blockContentStr.length(); ++charIdx) {
                        char c = blockContentStr.charAt(charIdx);
                        if (c == '\t') {
                            fullFileString.append("    ");  // convert tabs to spaces
                            continue;
                        }

                        // ignore whitespace
                        if (c == '\r' || c == '\f') {
                            continue;
                        }

                        fullFileString.append(c);
                        if (c == '\n') {
                            fullFileString.append("    "); // indent after newlines
                        }
                    }
                }

                // python doesn't allow empty functions appending 'pass' will keep python happy
                fullFileString.append("\n    pass");
                fullFileString.append("\n\n");
            }

            // output all function calls
            for (int blockIdx = 0; blockIdx < process.largestIndex; ++blockIdx) {
                fullFileString.append(getBlockFuctionStringFromIdx(blockIdx) + "\n");
                fullFileString.append("__WEAVE.__WEAVE_WAIT_TILL_SCHEDULED()\n");
            }

            // the last one doesn't need to wait
            fullFileString.append(getBlockFuctionStringFromIdx(process.largestIndex) + "\n");
            fullFileString.append(PROCESS_FILE_FOOTER);
            try {
                Files.write(path, fullFileString.toString().getBytes(),
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.SYNC);
            } catch (IOException e) {
                System.err.println("FAILED TO SAVE A PROCESS FILE TO DISK!!");
                e.printStackTrace();
            }
        }
    }

    public void runProcesses(ArrayList<WeaveProcess> processes) {
        final String outputDir = "outFiles";
        this.writeProcessesToDisk(processes, outputDir);
        SharedMemory s = SharedMemory.SharedMemory();
        int[] pids = new int[processes.size()];
        for (int i = 0; i < processes.size(); ++i) {
            int pid = getPIDFromIDX(i);
            SharedMemory.WaitForProcess(pid);
            pids[i] = pid;
        }

        for (int i = 0; i < processes.size(); ++i) {
            // don't need to use default file seperator here, the native C code will do the conversion for us
            String pythonFile = this.projectDir + "/" + outputDir + "/" + this.getFilenameFromIdx(i);
            s.CreatePythonProcess(pids[i], pythonFile);
        }

        while (!s.allProcessesFinished(pids)) {
            s.RunPidsAndWait(pids);
        }

        // NOTE(Ray): Need to release all mutexes here, since on when we enter the function again we will try to aquire
        //  a mutex that we already own and we will deadlock

        for (int i = 0; i < processes.size(); ++i) {
            SharedMemory.ReleaseProcess(getPIDFromIDX(i));
        }
    }
}