import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class savProjectFileTest {
    @Test
    public void test() {
        Scheduler.Scheduler().projectDir = "./testproject";
        Scheduler.Scheduler().projectName = "TEST_PROJ";
        ArrayList<WeaveProcess> processes = new ArrayList<>();
        WeaveProcess process = new WeaveProcess();
        process.largestIndex = 4;
        process.blocks[4] = new Block(new StringBuilder(""));
        process.blocks[2] = new Block(new StringBuilder(""));

        processes.add(process);
        Scheduler.Scheduler().saveProjectFile(processes, "testfile");

        ByteBuffer contents = ByteBuffer.allocate(1);

        try {
            contents = ByteBuffer.wrap(Files.readAllBytes(Paths.get("./testproject/testfile.wve")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(contents.getInt(), Scheduler.Scheduler().projectDir.getBytes().length);
        for (char c: Scheduler.Scheduler().projectDir.toCharArray()) {
            assertEquals(contents.getChar(), c);
        }

        assertEquals(contents.getInt(), 5);
        assertEquals(contents.get(), 0);
        assertEquals(contents.get(), 0);
        assertEquals(contents.get(), 1);
        assertEquals(contents.get(), 0);
        assertEquals(contents.get(), 1);
    }
}
