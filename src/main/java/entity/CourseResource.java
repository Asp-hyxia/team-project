package entity;

import java.net.URI;
import java.util.Objects;

/** Metadata only. A source URL is a Canvas page, never a signed download URL. */
public final class CourseResource {
    public enum Type { PAGE, FILE }
    private final String id;
    private final long courseId;
    private final String title;
    private final Type type;
    private final URI sourceUrl;

    public CourseResource(String id, long courseId, String title, Type type, URI sourceUrl) {
        if (Objects.requireNonNull(id).isBlank() || courseId <= 0
                || Objects.requireNonNull(title).isBlank()) {
            throw new IllegalArgumentException("Resource needs an ID, course and title.");
        }
        this.sourceUrl = Objects.requireNonNull(sourceUrl);
        if (!"https".equals(sourceUrl.getScheme()) || sourceUrl.getHost() == null
                || sourceUrl.getUserInfo() != null || sourceUrl.getQuery() != null
                || sourceUrl.getFragment() != null) {
            throw new IllegalArgumentException("Source must be a canonical HTTPS URL.");
        }
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.type = Objects.requireNonNull(type);
    }
    public String getId() { return id; }
    public long getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public Type getType() { return type; }
    public URI getSourceUrl() { return sourceUrl; }
}
