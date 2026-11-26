package interface_adapter.course_question;

import java.util.ArrayList;
import java.util.List;
import use_case.question_answering.CourseQuestionException.Code;
import use_case.question_answering.CourseQuestionOutputBoundary;

public final class CourseQuestionPresenter implements CourseQuestionOutputBoundary {
    private final CourseQuestionViewModel viewModel;

    public CourseQuestionPresenter(CourseQuestionViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void presentCatalog(List<Reference> pages, List<String> warnings) {
        List<CourseQuestionViewModel.PageOption> options = new ArrayList<>();
        for (Reference page : pages) {
            options.add(new CourseQuestionViewModel.PageOption(page.id, page.title));
        }
        String result = pages.isEmpty() ? "No Page references are available." : "Enter a question about the course.";
        if (!warnings.isEmpty()) {
            result += "\n\n" + String.join("\n", warnings);
        }
        viewModel.setState(new CourseQuestionViewModel.State(options, result));
    }

    @Override
    public void presentAnswer(String answer, List<Reference> sources) {
        StringBuilder result = new StringBuilder(answer).append("\n\nSources:\n");
        for (Reference source : sources) {
            result.append(source.title).append('\n').append(source.url).append('\n');
        }
        showResult(result.toString().stripTrailing());
    }

    @Override
    public void presentFailure(Code code) {
        switch (code) {
            case INVALID_INPUT: showResult("Enter a valid question."); break;
            case NO_REFERENCES: showResult("Select a Page reference."); break;
            case TOO_MANY_REFERENCES: showResult("Use at most three Page references, including course defaults."); break;
            case PERMISSION_DENIED: showResult("This course or Page is not accessible."); break;
            case CONTENT_UNAVAILABLE: showResult("A requested Page is unavailable."); break;
            case MODEL_UNAVAILABLE: showResult("The model is unavailable. Please retry."); break;
            case INVALID_RESPONSE: showResult("Could not use the response from the content or model adapter."); break;
            default: showResult("The request could not be completed. Please retry.");
        }
    }

    private void showResult(String result) {
        viewModel.setState(new CourseQuestionViewModel.State(viewModel.getState().pages, result));
    }
}
