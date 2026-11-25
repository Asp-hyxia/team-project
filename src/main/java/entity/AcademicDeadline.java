package entity;
import java.time.Instant;
import java.net.URI;
import java.util.Objects;

public final class AcademicDeadline {
    private final long id;
    private final long courseId;
    private final String title;
    private final Instant dueAt;
    private final URI sourceUrl;
    public AcademicDeadline(
            long id,
            long courseId,
            String title,
            Instant dueAt,
            URI sourceUrl){
        if (id <= 0 || courseId <= 0 || Objects.requireNonNull(title).isBlank()){
            throw new IllegalArgumentException(
                    "Deadline needs a positive ID,course ID or title."
            );
        }
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.dueAt = Objects.requireNonNull(dueAt);
        this.sourceUrl = Objects.requireNonNull(sourceUrl);
        if (!"https".equals(sourceUrl.getScheme())
        || sourceUrl.getHost() == null
        || sourceUrl.getUserInfo() != null
        || sourceUrl.getQuery() != null
        || sourceUrl.getFragment() != null){
            throw new IllegalArgumentException(
                    "Source must be a canonical HTTPS URL.");
        }
    }
    public long getId(){
        return id;
    }
    public long getCourseid(){
        return courseId;
    }
    public String getTitle(){
        return title;
    }
    public Instant getDueAt(){
        return dueAt;
    }
    public URI getSourceUrl(){
        return sourceUrl;
    }
}
