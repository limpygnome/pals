package pals.base.assessment;

/**
 *
 * @author limpygnome
 */
public class AssignmentQuestions
{
    // hahsmap of question ID to questions
    // -- multiple questions for an actual question.
    // -- -- they end up as sub-questions
    // -- AssignmentQuestions
    // -- assignmentid   | question (index 1...n) | group (index 1...n) | weight
    // -- assignmentid   | group (index FK) | questionid | weight
    // -- -- random group is picked
    // -- -- all questions in that group are displayed
    // -- -- can be multiple of the same question for each group
    
    // hashmap<question (index), AssignmentQuestionsGroup>
    // 
}
