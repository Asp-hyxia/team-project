package entity;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/** A dated course announcement with its original Canvas source. */
public final class Announcement {
    private final String id;
    private final long courseId;
    private final String courseName;
    private final Instant time;
    private final String type;
    private final String title;
    private final String content;
    private final URI sourceUrl;

    public Announcement(
            String id,
            long courseId,
            String courseName,
            Instant time,
            String type,
            String title,
            String content,
            URI sourceUrl) {
        if (Objects.requireNonNull(id).isBlank() || courseId <= 0
                || Objects.requireNonNull(courseName).isBlank()
                || Objects.requireNonNull(type).isBlank()
                || Objects.requireNonNull(title).isBlank()
                || Objects.requireNonNull(content).isBlank()) {
            throw new IllegalArgumentException("Announcement needs an ID, course, type, title and content.");
        }
        this.time = Objects.requireNonNull(time);
        this.sourceUrl = Objects.requireNonNull(sourceUrl);
        if (!"https".equals(sourceUrl.getScheme()) || sourceUrl.getHost() == null
                || sourceUrl.getUserInfo() != null || sourceUrl.getQuery() != null
                || sourceUrl.getFragment() != null) {
            throw new IllegalArgumentException("Source must be a canonical HTTPS URL.");
        }
        this.id = id;
        this.courseId = courseId;
        this.courseName = courseName;
        this.type = type;
        this.title = title;
        this.content = content;
    }

    public String getId() { return id; }
    public long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public Instant getTime() { return time; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public URI getSourceUrl() { return sourceUrl; }
}
