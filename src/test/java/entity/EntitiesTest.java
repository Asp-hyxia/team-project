package entity;

import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

class EntitiesTest {
    @Test void courseInvariantsAndAccessors() {
        Course course = new Course(1, "Design", "CSC207");
        assertEquals(1, course.getId()); assertEquals("Design", course.getName()); assertEquals("CSC207", course.getCode());
        assertThrows(IllegalArgumentException.class, () -> new Course(0, "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> new Course(1, " ", "b"));
        assertThrows(NullPointerException.class, () -> new Course(1, null, "b"));
        assertThrows(NullPointerException.class, () -> new Course(1, "a", null));
    }
    @Test void resourceInvariantsAndAccessors() {
        URI url = URI.create("https://canvas.example.edu/courses/1/files/2");
        CourseResource resource = new CourseResource("2", 1, "Notes", CourseResource.Type.FILE, url);
        assertEquals("2", resource.getId()); assertEquals(1, resource.getCourseId());
        assertEquals("Notes", resource.getTitle()); assertEquals(CourseResource.Type.FILE, resource.getType()); assertEquals(url, resource.getSourceUrl());
        assertThrows(IllegalArgumentException.class, () -> new CourseResource("", 1, "Notes", CourseResource.Type.FILE, url));
        assertThrows(IllegalArgumentException.class, () -> new CourseResource("2", 0, "Notes", CourseResource.Type.FILE, url));
        assertThrows(IllegalArgumentException.class, () -> new CourseResource("2", 1, " ", CourseResource.Type.FILE, url));
        for (String invalid : new String[]{"http://example.edu/a", "https:/a", "https://a@b/a", "https://b/a?token=secret", "https://b/a#x"}) {
            assertThrows(IllegalArgumentException.class, () -> new CourseResource("2", 1, "Notes", CourseResource.Type.FILE, URI.create(invalid)));
        }
        assertThrows(NullPointerException.class, () -> new CourseResource(null, 1, "Notes", CourseResource.Type.FILE, url));
        assertThrows(NullPointerException.class, () -> new CourseResource("2", 1, null, CourseResource.Type.FILE, url));
        assertThrows(NullPointerException.class, () -> new CourseResource("2", 1, "Notes", null, url));
        assertThrows(NullPointerException.class, () -> new CourseResource("2", 1, "Notes", CourseResource.Type.FILE, null));
    }
}
