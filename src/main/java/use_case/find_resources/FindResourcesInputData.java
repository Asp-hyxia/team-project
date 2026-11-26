package use_case.find_resources;

public final class FindResourcesInputData {
    private final Long courseId;
    private final String query;
    /** Null course loads the course selector. A blank query browses the selected course. */
    public FindResourcesInputData(Long courseId, String query) {
        this.courseId = courseId;
        this.query = query;
    }
    public Long getCourseId() { return courseId; }
    public String getQuery() { return query; }
}
