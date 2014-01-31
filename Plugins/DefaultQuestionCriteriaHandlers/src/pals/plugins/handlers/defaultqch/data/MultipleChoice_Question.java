package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import pals.base.utils.Misc;

/**
 * Stores the settings data for a multiple-choice question.
 */
public class MultipleChoice_Question implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String      text;
    private boolean     singleAnswer;
    private String[]    answers;
    // Methods - Constructors **************************************************
    public MultipleChoice_Question()
    {
        this.text = "Undefined question text...";
        this.singleAnswer = false;
        this.answers = new String[]{};
    }
    // Methods - Mutators ******************************************************
    /**
     * @param text Sets the question's text.
     */
    public void setText(String text)
    {
        this.text = text;
    }
    /**
     * @param singleAnswer Sets if the user should only be able to provide a
     * single answer.
     */
    public void setSingleAnswer(boolean singleAnswer)
    {
        this.singleAnswer = singleAnswer;
    }
    /**
     * @param answers Sets the possible answers; includes filtering.
     */
    public void setAnswers(String[] answers)
    {
        this.answers = Misc.arrayStringUnique(answers);
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Compiles all of the answers into a single string, for display
     * in a text-box.
     */
    public String getAnswersWebFormat()
    {
        StringBuilder sb = new StringBuilder();
        for(String s : answers)
            sb.append(s).append("\n");
        if(sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
    /**
     * @return The question's text.
     */
    public String getText()
    {
        return text;
    }
    /**
     * @return Indicates if the user should only be able to provide a single
     * answer.
     */
    public boolean isSingleAnswer()
    {
        return singleAnswer;
    }
    /**
     * @return Array of possible answers, from which the user can select.
     */
    public String[] getAnswers()
    {
        return answers;
    }
}
