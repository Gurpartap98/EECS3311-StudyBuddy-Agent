package studybuddy.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import studybuddy.domain.Course;
import studybuddy.domain.Material;

class DocumentStoreMaterialTest {

    @Test
    void listMaterialsReturnsUploadedNotes() {
        DocumentStore store = new DocumentStore();
        Course course = store.save(new Course("eecs 3311", "EECS3311", "FW26"));
        Material material = store.save(new Material(
                course.getId(),
                "os-scheduling.txt",
                "data/samples/os-scheduling.txt",
                "Round Robin uses a time quantum."));

        assertFalse(store.listMaterials(course.getId()).isEmpty());
        assertEquals(1, store.listMaterials(course.getId()).size());
        assertEquals("os-scheduling.txt", store.listMaterials(course.getId()).get(0).getTitle());
        assertEquals(material.getId(), course.getMaterialIds().get(0));
    }
}
