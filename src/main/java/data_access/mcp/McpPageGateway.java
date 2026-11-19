package data_access.mcp;

import java.net.URI;
import java.util.Map;
import use_case.ask_page.AskPageException;
import use_case.ask_page.PageContent;
import use_case.ask_page.PageGateway;
import use_case.find_resources.DiscoveryException;
import use_case.mcp.McpToolGateway;

/** Shares the UC2 MCP transport and converts read_page into the UC3 page contract. */
public final class McpPageGateway implements PageGateway {
    private final McpToolGateway tools;

    public McpPageGateway(McpToolGateway tools) { this.tools = tools; }

    @Override
    public PageContent readPage(long courseId, String pageSlug) throws AskPageException {
        try {
            Map<String, Object> data = tools.callTool("read_page", Map.of("course_id", courseId, "page_slug", pageSlug));
            Object returnedId = data.get("course_id");
            if (!(returnedId instanceof Integer || returnedId instanceof Long) || ((Number) returnedId).longValue() != courseId
                    || !pageSlug.equals(data.get("page_slug"))) { throw invalid(); }
            return new PageContent(courseId, pageSlug, text(data, "title"), text(data, "text"),
                    URI.create(text(data, "source_url")));
        }
        catch (DiscoveryException exception) {
            AskPageException.Code code;
            switch (exception.getCode()) {
                case PERMISSION_DENIED: code = AskPageException.Code.PERMISSION_DENIED; break;
                case INVALID_INPUT: code = AskPageException.Code.INVALID_INPUT; break;
                case INVALID_RESPONSE: code = AskPageException.Code.INVALID_RESPONSE; break;
                default: code = AskPageException.Code.PAGE_UNAVAILABLE;
            }
            throw new AskPageException(code);
        }
        catch (IllegalArgumentException exception) { throw invalid(); }
    }

    private static String text(Map<String, Object> data, String key) throws AskPageException {
        Object value = data.get(key);
        if (!(value instanceof String)) { throw invalid(); }
        return (String) value;
    }

    private static AskPageException invalid() {
        return new AskPageException(AskPageException.Code.INVALID_RESPONSE);
    }
}
