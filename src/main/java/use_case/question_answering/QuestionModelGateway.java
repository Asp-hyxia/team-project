package use_case.question_answering;

import java.util.function.Consumer;

/** Answers from supplied references using a replaceable model adapter. */
public interface QuestionModelGateway {
    String answer(AnswerPrompt prompt) throws CourseQuestionException;

    /** Emits answer text in order on the calling thread when the model supports streaming. */
    default String answer(AnswerPrompt prompt, Consumer<String> onAnswerDelta) throws CourseQuestionException {
        return answer(prompt);
    }
}
