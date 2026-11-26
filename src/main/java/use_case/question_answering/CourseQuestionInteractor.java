package use_case.question_answering;

import entity.CourseResource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongFunction;
import use_case.find_resources.ResourceListing;
import use_case.question_answering.CourseQuestionException.Code;
import use_case.question_answering.CourseQuestionOutputBoundary.Reference;

public final class CourseQuestionInteractor implements CourseQuestionInputBoundary {
    private static final int MAX_REFERENCES = 3;

    private final CourseContentGateway content;
    private final QuestionModelGateway model;
    private final LongFunction<List<String>> defaultReferences;
    private final CourseQuestionOutputBoundary output;

    public CourseQuestionInteractor(CourseContentGateway content, QuestionModelGateway model,
            LongFunction<List<String>> defaultReferences, CourseQuestionOutputBoundary output) {
        this.content = content;
        this.model = model;
        this.defaultReferences = defaultReferences;
        this.output = output;
    }

    @Override
    public void loadCatalog(long courseId) {
        if (courseId <= 0) {
            output.presentFailure(Code.INVALID_INPUT);
            return;
        }
        try {
            ResourceListing listing = content.listContent(courseId);
            List<Reference> pages = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (CourseResource resource : listing.getResources()) {
                if (resource.getCourseId() != courseId) {
                    throw new CourseQuestionException(Code.INVALID_RESPONSE);
                }
                if (resource.getType() == CourseResource.Type.PAGE && seen.add(resource.getId())) {
                    pages.add(referenceOf(resource));
                }
            }
            output.presentCatalog(List.copyOf(pages), listing.getWarnings());
        }
        catch (CourseQuestionException failure) {
            output.presentFailure(failure.getCode());
        }
    }

    @Override
    public void ask(CourseQuestionInputData input) {
        if (input == null || input.courseId <= 0 || input.question == null || input.question.isBlank()) {
            output.presentFailure(Code.INVALID_INPUT);
            return;
        }

        Set<String> ids = new LinkedHashSet<>(defaultReferences.apply(input.courseId));
        ids.addAll(input.selectedContentIds);
        try {
            validateReferences(ids);
            List<String> referenceText = new ArrayList<>();
            List<Reference> sources = new ArrayList<>();
            for (String id : ids) {
                CourseContentGateway.Page page = content.readContent(input.courseId, id);
                if (page == null || page.resource.getCourseId() != input.courseId
                        || !page.resource.getId().equals(id) || page.resource.getType() != CourseResource.Type.PAGE
                        || page.text.isBlank()) {
                    throw new CourseQuestionException(Code.INVALID_RESPONSE);
                }
                referenceText.add(page.resource.getTitle() + "\n" + page.text);
                sources.add(referenceOf(page.resource));
            }
            String answer = model.answer(new AnswerPrompt(input.question.strip(), referenceText));
            if (answer == null || answer.isBlank()) {
                throw new CourseQuestionException(Code.INVALID_RESPONSE);
            }
            output.presentAnswer(answer.strip(), List.copyOf(sources));
        }
        catch (CourseQuestionException failure) {
            output.presentFailure(failure.getCode());
        }
    }

    private static void validateReferences(Set<String> ids) throws CourseQuestionException {
        if (ids.isEmpty()) {
            throw new CourseQuestionException(Code.NO_REFERENCES);
        }
        if (ids.size() > MAX_REFERENCES) {
            throw new CourseQuestionException(Code.TOO_MANY_REFERENCES);
        }
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                throw new CourseQuestionException(Code.INVALID_INPUT);
            }
        }
    }

    private static Reference referenceOf(CourseResource resource) {
        return new Reference(resource.getId(), resource.getTitle(), resource.getSourceUrl().toString());
    }
}
