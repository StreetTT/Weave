import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class savProjectFileTest {
    @Test
    public void test() {
        Scheduler.Scheduler().projectDir = "testproj";
        Scheduler.Scheduler().projectName = "testfile";
        ArrayList<WeaveProcess> processes = new ArrayList<>();
        WeaveProcess process = new WeaveProcess();
        process.blocks[4] = new Block(new StringBuilder(""));
        process.blocks[2] = new Block(new StringBuilder(""));

        processes.add(process);
        Scheduler.Scheduler().saveProjectFile(processes, "testfile");

        ByteBuffer contents = ByteBuffer.allocate(1);

        try {
            contents = ByteBuffer.wrap(Files.readAllBytes(Paths.get("./testproj/testfile.wve")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        contents.order(ByteOrder.LITTLE_ENDIAN); // little endian on every architecture that matters
        assertEquals(contents.getInt(), Scheduler.Scheduler().WEAVE_FILE_IDENTIFIER);
        assertEquals(contents.getInt(), 1); // version
        assertEquals(contents.getInt(), Scheduler.Scheduler().projectName.toCharArray().length * Character.BYTES);
        for (char c: Scheduler.Scheduler().projectName.toCharArray()) {
            assertEquals(c, contents.getChar());
        }

        assertEquals(contents.getInt(), processes.size());
        assertEquals(contents.getInt(), Scheduler.Scheduler().PROCESS_IDENTIFIER);
        assertEquals(0b00010100, contents.get());
    }
}
