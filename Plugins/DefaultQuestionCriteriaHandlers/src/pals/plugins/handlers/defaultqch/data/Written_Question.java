package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;

/**
 * A model for holding data for a written-response question-type.
 */
public class Written_Question implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String text;
    // Methods - Constructors **************************************************
    public Written_Question()
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
