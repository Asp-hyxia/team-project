package use_case.ask_page;

import java.net.URI;

/** A retrieved page. The gateway supplies its canonical Canvas source. */
public final class PageContent {
    public final long courseId;
    public final String slug;
    public final String title;
    public final String text;
    public final URI source;

    public PageContent(long courseId, String slug, String title, String text, URI source) {
        if (courseId <= 0 || !isValidSlug(slug) || title == null || title.isBlank()
                || text == null || text.isBlank() || source == null
                || !"https".equalsIgnoreCase(source.getScheme()) || source.getHost() == null
                || source.getUserInfo() != null) {
            throw new IllegalArgumentException("A complete page with an HTTPS source is required.");
        }
        this.courseId = courseId;
        this.slug = slug;
        this.title = title;
        this.text = text;
        this.source = source;
    }

    /** Allows page identifiers while excluding path traversal and URL delimiters. */
    public static boolean isValidSlug(String slug) {
        return slug != null && !slug.isBlank() && !slug.equals(".") && !slug.equals("..")
                && slug.matches("[^/\\\\?#\\p{Cntrl}]+");
    }

}
