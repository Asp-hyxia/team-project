package data_access.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.Course;
import entity.CourseResource;
import use_case.find_resources.*;
import use_case.mcp.McpToolGateway;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Converts our documented MCP tool payloads into application models. */
public final class McpResourceGateway implements ResourceGateway {
    private final McpToolGateway tools;
    private final ObjectMapper json = new ObjectMapper();
    public McpResourceGateway(McpToolGateway tools) { this.tools = tools; }
    @Override public List<Course> listCourses() throws DiscoveryException {
        JsonNode result = json.valueToTree(tools.callTool("list_courses", Map.of()));
        try {
            List<Course> courses = new ArrayList<>();
            for (JsonNode item : array(result, "items")) {
                courses.add(new Course(id(item, "id"), text(item, "name"), text(item, "code")));
            }
            return List.copyOf(courses);
        }
        catch (IllegalArgumentException exception) { throw invalid(); }
    }
    @Override public ResourceListing listResources(long courseId) throws DiscoveryException {
        JsonNode result = json.valueToTree(tools.callTool("list_resources", Map.of("course_id", courseId)));
        try {
            List<CourseResource> resources = new ArrayList<>();
            for (JsonNode item : array(result, "items")) {
                long returnedCourseId = id(item, "course_id");
                if (returnedCourseId != courseId) { throw invalid(); }
                resources.add(new CourseResource(text(item, "id"), returnedCourseId, text(item, "title"),
                        CourseResource.Type.valueOf(text(item, "type")), URI.create(text(item, "source_url"))));
            }
            List<String> warnings = new ArrayList<>();
            for (JsonNode warning : array(result, "warnings")) {
                if (!warning.isTextual()) { throw invalid(); }
                warnings.add(warning.asText());
            }
            return new ResourceListing(resources, warnings);
        }
        catch (IllegalArgumentException exception) { throw invalid(); }
    }
    private static JsonNode array(JsonNode node, String name) throws DiscoveryException {
        if (!node.path(name).isArray()) { throw invalid(); } return node.path(name);
    }
    private static String text(JsonNode node, String name) throws DiscoveryException {
        if (!node.path(name).isTextual()) { throw invalid(); } return node.path(name).asText();
    }
    private static long id(JsonNode node, String name) throws DiscoveryException {
        JsonNode value = node.path(name);
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() <= 0) { throw invalid(); }
        return value.asLong();
    }
    private static DiscoveryException invalid() { return new DiscoveryException(DiscoveryException.Code.INVALID_RESPONSE); }
}
