package use_case.ask_page;

/** Coordinates selected-page Q&A without depending on HTTP, Swing or an LLM SDK. */
public final class AskPageInteractor implements AskPageInputBoundary {
    private final PageGateway pages;
    private final AgentGateway agent;
    private final AskPageOutputBoundary presenter;

    public AskPageInteractor(PageGateway pages, AgentGateway agent, AskPageOutputBoundary presenter) {
        this.pages = pages;
        this.agent = agent;
        this.presenter = presenter;
    }

    @Override
    public void execute(AskPageInputData input) {
        if (input == null || input.courseId <= 0 || input.pageSlug == null
                || !PageContent.isValidSlug(input.pageSlug.strip()) || input.question == null || input.question.isBlank()
                || input.question.length() > 4000) {
            presenter.presentFailure(AskPageException.Code.INVALID_INPUT);
            return;
        }
        try {
            String slug = input.pageSlug.strip();
            PageContent page = pages.readPage(input.courseId, slug);
            if (page == null || page.courseId != input.courseId || !page.slug.equals(slug)) {
                throw new AskPageException(AskPageException.Code.INVALID_RESPONSE);
            }
            String answer = agent.answer(input.question.strip(), page);
            if (answer == null || answer.isBlank()) {
                throw new AskPageException(AskPageException.Code.INVALID_RESPONSE);
            }
            presenter.present(new AskPageOutputData(answer.strip(), page.title, page.source.toString()));
        }
        catch (AskPageException exception) {
            presenter.presentFailure(exception.getCode());
        }
    }
}
