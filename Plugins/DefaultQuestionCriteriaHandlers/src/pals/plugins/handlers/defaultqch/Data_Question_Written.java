package pals.plugins.handlers.defaultqch;

import java.io.Serializable;

/**
 * A model for holding data for a written-response question-type.
 */
public class Data_Question_Written implements Serializable
{
    private String text;
    // Methods - Constructors **************************************************
    public Data_Question_Written()
    {
        this.text = "Default question text...";
    }
    // Methods - Mutators ******************************************************
    /**
     * @param text Sets the new text of the question.
     */
    public void setText(String text)
    {
        this.text = text;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The text of the question.
     */
    public String getText()
    {
        return text;
    }
}
