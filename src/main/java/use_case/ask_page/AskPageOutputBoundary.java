package use_case.ask_page;

public interface AskPageOutputBoundary {
    void present(AskPageOutputData output);
    void presentFailure(AskPageException.Code code);
}
