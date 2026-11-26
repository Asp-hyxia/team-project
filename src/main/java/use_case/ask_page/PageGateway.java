package use_case.ask_page;

/** Reads an authorized page body. The adapter must check current course/page access. */
public interface PageGateway {
    PageContent readPage(long courseId, String pageSlug) throws AskPageException;
}
