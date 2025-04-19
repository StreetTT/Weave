import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;

public class runProcessesTest {
    @Test
    public void runProcessesTest() {
        FrontendController controller = new FrontendController();
        File projectFile = Paths.get("../pyTest/TestingRunProcesses/TEST_PROJ.wve").toFile();
        controller.loadProject(projectFile);
        Scheduler.Scheduler().runProcesses(Frontend.processes);
    }
}