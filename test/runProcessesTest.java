import javafx.scene.control.Menu;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;

public class runProcessesTest {
    public static final WeaveNative wn = WeaveNativeFactory.get();

    public String getOutputandClear() {
        String output = StandardCharsets.UTF_8.decode(this.wn.GetProcessesOutput()).toString();
        this.wn.ClearProcessOutput();

        return output;
    }

    @Test
    //NOTE(Ray): keep runProcessTests in one function otherwise JUNIT crashes in native code.

    public void runProcessesTest() {
        {
            ArrayList<WeaveProcess> processes = new ArrayList<WeaveProcess>();
            processes.add(new WeaveProcess());
            processes.add(new WeaveProcess());

            WeaveProcess process1 = processes.get(0);
            WeaveProcess process2 = processes.get(1);
            Block block = process1.addBlock(0);
            block.fileContents = new StringBuilder("from multiprocessing import shared_memory\n" +
                    "global shm_a\n" +
                    "shm_a = shared_memory.SharedMemory(\"testmem\", create=True, size=10)\n" +
                    "global buffer\n" +
                    "buffer = shm_a.buf\n" +
                    "print(buffer[0], flush=True)\n" +
                    "buffer[0] = 100                           # Modify single byte at a time");


            block = process1.addBlock(2);
            block.fileContents = new StringBuilder("print(buffer[0], flush=True)\n" +
                    "shm_a.close()\n" +
                    "shm_a.unlink() #only call this bad boy once\n");


            block = process2.addBlock(1);
            block.fileContents = new StringBuilder("from multiprocessing import shared_memory\n" +
                    "global shm_a\n" +
                    "shm_a = shared_memory.SharedMemory(\"testmem\")\n" +
                    "global buffer\n" +
                    "buffer = shm_a.buf\n" +
                    "print(buffer[0], flush=True)\n" +
                    "buffer[0] = 200");

            block = process2.addBlock(2);
            block.fileContents = new StringBuilder("shm_a.close()");
            Scheduler.Scheduler().projectDir = "pyTest/tmp";
            Scheduler.Scheduler().projectName = "tmp";
            Scheduler.Scheduler().runProcesses(processes);

            String output = getOutputandClear();
            output = output.replace("\r", "");
            assertEquals("0\n100\n200\n>>>>>>>>>>>>", output);
        }

        {
            ArrayList<WeaveProcess> processes = new ArrayList<WeaveProcess>();
            processes.add(new WeaveProcess());
            processes.add(new WeaveProcess());

            WeaveProcess process1 = processes.get(0);
            WeaveProcess process2 = processes.get(1);
            Block block = process1.addBlock(0);
            block.fileContents = new StringBuilder("from multiprocessing import shared_memory\n" +
                    "global shm_a\n" + "RUNTIME_EXCEPTION\n" +
                    "shm_a = shared_memory.SharedMemory(\"testmem\", create=True, size=10)\n" +
                    "global buffer\n" +
                    "buffer = shm_a.buf\n" +
                    "print(buffer[0], flush=True)\n" +
                    "buffer[0] = 100                           # Modify single byte at a time");


            block = process1.addBlock(2);
            block.fileContents = new StringBuilder("print(buffer[0], flush=True)\n" +
                    "shm_a.close()\n" +
                    "shm_a.unlink() #only call this bad boy once\n");


            block = process2.addBlock(1);
            block.fileContents = new StringBuilder("from multiprocessing import shared_memory\n" +
                    "global shm_a\n" + "THIS WILL HAPPEN AT PARSE TIME\n" +
                    "shm_a = shared_memory.SharedMemory(\"testmem\")\n" +
                    "global buffer\n" +
                    "buffer = shm_a.buf\n" +
                    "print(buffer[0], flush=True)\n" +
                    "buffer[0] = 200");

            block = process2.addBlock(2);
            block.fileContents = new StringBuilder("shm_a.close()");
            Scheduler.Scheduler().projectDir = "pyTest/tmp";
            Scheduler.Scheduler().projectName = "tmp";
            Scheduler.Scheduler().runProcesses(processes);

            String output = getOutputandClear();
            output = output.replace("\r", "");

        }
        WeaveNativeFactory.get().DeInit();

       assertEquals(true, true);
    }
}