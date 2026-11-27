package data_access;

import java.util.List;
import use_case.question_answering.AnswerPrompt;
import use_case.question_answering.QuestionModelGateway;

/** Echoes the supplied fixture references as an answer without contacting a model provider. */
public final class DemoQuestionModelGateway implements QuestionModelGateway {
    @Override
    public String answer(AnswerPrompt prompt) {
        List<String> parts = prompt.getUserParts();
        return "Simulated answer from the demo references:\n\n"
                + String.join("\n\n", parts.subList(1, parts.size()));
    }
}
