package interface_adapter.ask_page;

import use_case.ask_page.AskPageException;
import use_case.ask_page.AskPageOutputBoundary;
import use_case.ask_page.AskPageOutputData;

public final class AskPagePresenter implements AskPageOutputBoundary {
    private final AskPageViewModel model;

    public AskPagePresenter(AskPageViewModel model) { this.model = model; }

    @Override
    public void present(AskPageOutputData output) {
        model.setState(new AskPageViewModel.State(output.answer, output.sourceTitle, output.sourceUrl));
    }

    @Override
    public void presentFailure(AskPageException.Code code) {
        String message;
        switch (code) {
            case INVALID_INPUT:
                message = "Select a valid course/page and enter a question of at most 4000 characters.";
                break;
            case PERMISSION_DENIED:
                message = "Canvas denied access to this page. Check your current course access.";
                break;
            case PAGE_UNAVAILABLE:
                message = "The page could not be retrieved. Please try again.";
                break;
            case PROVIDER_NOT_CONFIGURED:
                message = "The LLM provider is not configured yet. Configure an AgentGateway to enable answers.";
                break;
            case PROVIDER_AUTH_FAILED:
                message = "Gemini authentication or API access failed. Check the API key and project permissions.";
                break;
            case PROVIDER_MODEL_UNAVAILABLE:
                message = "The configured Gemini model is unavailable. Check GEMINI_MODEL.";
                break;
            case PROVIDER_RATE_LIMITED:
                message = "Gemini rate or quota limit reached. Check quota or retry later.";
                break;
            case PROVIDER_BUSY:
                message = "Gemini is temporarily busy. Please retry later.";
                break;
            case PROVIDER_TIMEOUT:
                message = "The Gemini request timed out. Please retry.";
                break;
            case PROVIDER_REQUEST_REJECTED:
                message = "Gemini rejected the request. Check the model configuration and input size.";
                break;
            case SERVICE_BUSY:
                message = "The application could not start this request. Please retry.";
                break;
            case UNEXPECTED_FAILURE:
                message = "The request failed unexpectedly. You can try again.";
                break;
            case PROVIDER_UNAVAILABLE:
                message = "The model service is unavailable. Please try again.";
                break;
            default:
                message = "The service returned an invalid response. Please try again.";
        }
        model.setState(new AskPageViewModel.State(message, "", ""));
    }
}
