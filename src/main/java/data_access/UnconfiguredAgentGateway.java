package data_access;

import use_case.ask_page.AgentGateway;
import use_case.ask_page.AskPageException;
import use_case.ask_page.PageContent;

/** Used until the team selects and configures its LLM provider. */
public final class UnconfiguredAgentGateway implements AgentGateway {
    @Override
    public String answer(String question, PageContent page) throws AskPageException {
        throw new AskPageException(AskPageException.Code.PROVIDER_NOT_CONFIGURED);
    }
}
