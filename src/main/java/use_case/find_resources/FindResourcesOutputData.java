package use_case.find_resources;

import java.util.List;

public final class FindResourcesOutputData {
    public static final class CourseData {
        public final long id;
        public final String name;
        public CourseData(long id, String name) { this.id = id; this.name = name; }
    }
    public static final class ResourceData {
        public final long courseId;
        public final String resourceId;
        public final String title;
        public final String type;
        public final String course;
        public final String sourceUrl;
        public ResourceData(String title, String type, String course, String sourceUrl) {
            this(0, "", title, type, course, sourceUrl);
        }
        public ResourceData(long courseId, String resourceId, String title, String type, String course, String sourceUrl) {
            this.courseId = courseId; this.resourceId = resourceId;
            this.title = title; this.type = type; this.course = course; this.sourceUrl = sourceUrl;
        }
    }
    public final List<CourseData> courses;
    public final List<ResourceData> resources;
    public final List<String> warnings;
    public final boolean courseSelectionOnly;
    public FindResourcesOutputData(List<CourseData> courses, List<ResourceData> resources,
                                   List<String> warnings, boolean courseSelectionOnly) {
        this.courses = List.copyOf(courses);
        this.resources = List.copyOf(resources);
        this.warnings = List.copyOf(warnings);
        this.courseSelectionOnly = courseSelectionOnly;
    }
}
