package studybuddy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudyBuddyAppArgsTest {

    @TempDir
    Path temp;

    @Test
    void parsesInputAndLlmApiFlags() throws Exception {
        Path task = temp.resolve("task.txt");
        Files.writeString(task, "help\nquit\n");
        StudyBuddyApp.ParsedArgs parsed = StudyBuddyApp.ParsedArgs.parse(new String[] {
                "-LLMAPI", "-input", task.toString()
        });
        assertEquals("cli", parsed.mode);
        assertTrue(parsed.llmApiFlag);
        assertNotNull(parsed.inputFile);
        assertEquals(task.toAbsolutePath().normalize(), parsed.inputFile.toAbsolutePath().normalize());
        assertFalse(parsed.help);
    }

    @Test
    void defaultsToGuiWithoutArgs() {
        StudyBuddyApp.ParsedArgs parsed = StudyBuddyApp.ParsedArgs.parse(new String[] {});
        assertEquals("gui", parsed.mode);
        assertNull(parsed.inputFile);
    }
}
