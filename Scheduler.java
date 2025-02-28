import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Scheduler {
    private static Scheduler singleton_ref = null;
    private static final String PROCESS_FILE_HEADER = "import weave_shared as __WEAVE\n";
    private static final String PROCESS_FILE_FOOTER = "__WEAVE.__WEAVE_PROCESS_END()";
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

    public void writeProcessesToDisk(ArrayList<WeaveProcess> processes) {
        for (int processIdx = 0; processIdx < processes.size(); ++processIdx) {
            WeaveProcess process = processes.get(processIdx);
            StringBuilder fullFileString = new StringBuilder();
            fullFileString.append(PROCESS_FILE_HEADER);
            fullFileString.append("__WEAVE.__WEAVE_PROCESS_START(" + processIdx + ")\n");

            String filename = this.projectName + "_PROCESS_" + processIdx + ".py";
            String fullFilepath = this.projectDir + FileSystems.getDefault().getSeparator() + filename;
            Path path = Paths.get(fullFilepath);
            for (int blockIdx = 0; blockIdx < process.largestIndex + 1; ++blockIdx) {
                Block block = process.blocks[blockIdx];

                fullFileString.append("def process_func_block_" + (blockIdx) + "():\n    "); if (block != null) {
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

            fullFileString.append(PROCESS_FILE_FOOTER);
            try {
                Files.write(path, fullFileString.toString().getBytes());
            } catch (IOException e) {
                System.err.println("FAILED TO SAVE A PROCESS FILE TO DISK!!");
            }
        }
    }
}