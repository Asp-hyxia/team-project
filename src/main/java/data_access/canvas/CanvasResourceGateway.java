package data_access.canvas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.Course;
import entity.CourseResource;
import use_case.find_resources.DiscoveryException;
import use_case.find_resources.ResourceGateway;
import use_case.find_resources.ResourceListing;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static use_case.find_resources.DiscoveryException.Code.*;

/** Canvas GET-only adapter. Tokens never leave the configured origin or enter output DTOs. */
public final class CanvasResourceGateway implements ResourceGateway {
    private static final Pattern NEXT = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    private static final int MAX_PAGES = 20;
    private final URI origin;
    private final String token;
    private final HttpClient client;
    private final ObjectMapper json = new ObjectMapper();

    public CanvasResourceGateway(URI origin, String token) {
        this(origin, token, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }
    public CanvasResourceGateway(URI origin, String token, HttpClient client) {
        boolean local = "http".equals(origin.getScheme()) && "127.0.0.1".equals(origin.getHost());
        if ((!"https".equals(origin.getScheme()) && !local) || origin.getHost() == null
                || origin.getUserInfo() != null || origin.getQuery() != null || origin.getFragment() != null
                || !(origin.getPath().isEmpty() || "/".equals(origin.getPath()))
                || token == null || token.isBlank() || token.contains("\n") || token.contains("\r")
                || client.followRedirects() != HttpClient.Redirect.NEVER) {
            throw new IllegalArgumentException("Canvas requires an HTTPS origin and a local credential.");
        }
        this.origin = origin.resolve("/"); this.token = token; this.client = client;
    }
    @Override
    public List<Course> listCourses() throws DiscoveryException {
        List<String> warnings = new ArrayList<>();
        Set<Long> active = new HashSet<>();
        for (JsonNode item : getAll("/api/v1/courses?enrollment_state=active&per_page=100", warnings)) {
            active.add(positiveId(item, "id"));
        }
        List<Course> courses = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (JsonNode item : getAll("/api/v1/users/self/favorites/courses?per_page=100", warnings)) {
            long id = positiveId(item, "id");
            if (active.contains(id) && "available".equals(item.path("workflow_state").asText()) && seen.add(id)) {
                courses.add(new Course(id, required(item, "name"), item.path("course_code").asText("")));
            }
        }
        if (!warnings.isEmpty()) { throw new DiscoveryException(UNAVAILABLE); }
        return List.copyOf(courses);
    }
    @Override
    public ResourceListing listResources(long courseId) throws DiscoveryException {
        if (listCourses().stream().noneMatch(c -> c.getId() == courseId)) {
            throw new DiscoveryException(PERMISSION_DENIED);
        }
        List<CourseResource> resources = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int successfulSections = 0;
        DiscoveryException firstFailure = null;
        for (CourseResource.Type type : CourseResource.Type.values()) {
            String section = type == CourseResource.Type.PAGE ? "pages" : "files";
            try {
                List<JsonNode> items = getAll("/api/v1/courses/" + courseId + "/" + section + "?per_page=100", warnings);
                for (JsonNode item : items) {
                    if (item.path("locked_for_user").asBoolean() || item.path("hidden_for_user").asBoolean()
                            || (type == CourseResource.Type.PAGE && !item.path("published").asBoolean(true))) { continue; }
                    String id = type == CourseResource.Type.PAGE ? required(item, "url")
                            : Long.toString(positiveId(item, "id"));
                    if (id.contains("/") || id.contains("?") || id.contains("#") || id.equals(".") || id.equals("..")) {
                        throw new DiscoveryException(INVALID_RESPONSE);
                    }
                    String title = required(item, type == CourseResource.Type.PAGE ? "title" : "display_name");
                    resources.add(new CourseResource(id, courseId, title, type,
                            source("/courses/" + courseId + "/" + section + "/" + id)));
                }
                successfulSections++;
            }
            catch (DiscoveryException exception) {
                if (exception.getCode() == INVALID_RESPONSE) { throw exception; }
                if (firstFailure == null) { firstFailure = exception; }
                warnings.add(section + " could not be listed (" + exception.getCode() + "). Results are incomplete.");
            }
        }
        if (successfulSections == 0) { throw firstFailure; }
        return new ResourceListing(resources, warnings);
    }
    private URI source(String path) throws DiscoveryException {
        try {
            // In production this is HTTPS. Local fixture origins use a synthetic HTTPS source.
            return new URI("https", null, origin.getHost(), -1, path, null, null);
        }
        catch (URISyntaxException exception) { throw new DiscoveryException(INVALID_RESPONSE); }
    }
    private List<JsonNode> getAll(String path, List<String> warnings) throws DiscoveryException {
        URI next = origin.resolve(path);
        String allowedPath = next.getPath();
        List<JsonNode> items = new ArrayList<>();
        Set<URI> visited = new HashSet<>();
        for (int page = 0; next != null && page < MAX_PAGES; page++) {
            if (!origin.getScheme().equals(next.getScheme()) || !origin.getAuthority().equals(next.getAuthority())
                    || !allowedPath.equals(next.getPath()) || next.getUserInfo() != null || next.getFragment() != null
                    || !visited.add(next)) { throw new DiscoveryException(INVALID_RESPONSE); }
            try {
                HttpRequest request = HttpRequest.newBuilder(next).timeout(Duration.ofSeconds(20))
                        .header("Authorization", "Bearer " + token).header("Accept", "application/json").GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 401 || status == 403) { throw new DiscoveryException(PERMISSION_DENIED); }
                if (status != 200) { throw new DiscoveryException(UNAVAILABLE); }
                JsonNode body;
                try { body = json.readTree(response.body()); }
                catch (IOException exception) { throw new DiscoveryException(INVALID_RESPONSE); }
                if (body == null || !body.isArray()) { throw new DiscoveryException(INVALID_RESPONSE); }
                body.forEach(items::add);
                Matcher matcher = NEXT.matcher(String.join(",", response.headers().allValues("Link")));
                next = matcher.find() ? origin.resolve(matcher.group(1)) : null;
            }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt(); throw new DiscoveryException(DISCONNECTED);
            }
            catch (IOException exception) { throw new DiscoveryException(DISCONNECTED); }
            catch (IllegalArgumentException exception) { throw new DiscoveryException(INVALID_RESPONSE); }
        }
        if (next != null) { warnings.add("Listing reached the 20-page limit. Results are incomplete."); }
        return items;
    }
    private static long positiveId(JsonNode item, String key) throws DiscoveryException {
        if (!item.path(key).isIntegralNumber() || !item.path(key).canConvertToLong() || item.path(key).asLong() <= 0) {
            throw new DiscoveryException(INVALID_RESPONSE);
        }
        return item.path(key).asLong();
    }
    private static String required(JsonNode item, String key) throws DiscoveryException {
        if (!item.path(key).isTextual() || item.path(key).asText().isBlank()) {
            throw new DiscoveryException(INVALID_RESPONSE);
        }
        return item.path(key).asText();
    }
}
