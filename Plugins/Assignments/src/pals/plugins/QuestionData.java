package pals.plugins;

import pals.base.assessment.AssignmentQuestion;

/**
 * The model passed to the main template for rendering questions.
 */
public class QuestionData
{
    // Fields ******************************************************************
    private int                 number;
    private AssignmentQuestion  question;
    private String              html;
    // Methods - Constructors **************************************************
    public QuestionData()
    {
        this(-1, null, null);
    }
    public QuestionData(int number, AssignmentQuestion question, String html)
    {
        this.number = number;
        this.question = question;
        this.html = html;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param number The question's number, as displayed on the page.
     */
    public void setNumber(int number)
    {
        this.number = number;
    }
    /**
     * @param question Sets the question being displayed.
     */
    public void setQuestion(AssignmentQuestion question)
    {
        this.question = question;
    }
    /**
     * @param html Sets the HTML of the question being displayed.
     */
    public void setHTML(String html)
    {
        this.html = html;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The question's (display) number.
     */
    public int getNumber()
    {
        return number;
    }
    /**
     * @return Retrieves the question being displayed.
     */
    public AssignmentQuestion getQuestion()
    {
        return question;
    }
    /**
     * @return Retrieves the HTML of the question being displayed.
     */
    public String getHTML()
    {
        return html;
    }
}
