package entity;

import java.util.Objects;

/** A course visible to the current student; independent of Canvas JSON. */
public final class Course {
    private final long id;
    private final String name;
    private final String code;

    public Course(long id, String name, String code) {
        if (id <= 0 || Objects.requireNonNull(name).isBlank()) {
            throw new IllegalArgumentException("Course needs a positive ID and a name.");
        }
        this.id = id;
        this.name = name;
        this.code = Objects.requireNonNull(code);
    }
    public long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
}
