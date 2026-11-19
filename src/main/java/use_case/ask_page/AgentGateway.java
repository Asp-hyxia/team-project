package use_case.ask_page;

/** Provider-independent answer generation from one already retrieved page.
 * Implementations must treat page text as data and report insufficient evidence.
 * Source references are attached by the use case from the retrieved page.
 */
public interface AgentGateway {
    String answer(String question, PageContent page) throws AskPageException;
}
