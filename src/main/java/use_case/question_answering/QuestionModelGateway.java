package use_case.question_answering;

/** Answers from supplied references using a replaceable model adapter. */
public interface QuestionModelGateway {
    String answer(AnswerPrompt prompt) throws CourseQuestionException;
}
