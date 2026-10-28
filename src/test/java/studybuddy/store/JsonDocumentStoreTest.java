package studybuddy.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import studybuddy.domain.Course;
import studybuddy.domain.Material;

class JsonDocumentStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsCoursesAndMaterialsAcrossReload() {
        JsonDocumentStore store = new JsonDocumentStore(tempDir);
        Course course = store.save(new Course("EECS 3311", "EECS3311", "FW26"));
        Material material = store.save(new Material(
                course.getId(), "notes.txt", "notes.txt", "Factory Method creates products."));

        JsonDocumentStore reloaded = new JsonDocumentStore(tempDir);
        assertEquals(1, reloaded.listCourses().size());
        assertEquals("EECS 3311", reloaded.findCourse(course.getId()).orElseThrow().getName());
        assertTrue(reloaded.findMaterial(material.getId()).isPresent());
        assertEquals(
                "Factory Method creates products.",
                reloaded.findMaterial(material.getId()).orElseThrow().getExtractedText());
    }
}
