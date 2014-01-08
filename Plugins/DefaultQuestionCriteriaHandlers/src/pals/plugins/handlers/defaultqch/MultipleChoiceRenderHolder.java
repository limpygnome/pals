package pals.plugins.handlers.defaultqch;

/**
 * A simple class for the template engine to render the possible choices of a
 * multiple choice question.
 */
public class MultipleChoiceRenderHolder
{
    // Fields ******************************************************************
    private final int           number;
    private final String        text;
    private final boolean       selected;
    // Methods - Constructors **************************************************
    public MultipleChoiceRenderHolder(int number, String text, boolean selected)
    {
        this.number = number;
        this.text = text;
        this.selected = selected;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The random-index of the choice.
     */
    public int getNumber()
    {
        return number;
    }
    /**
     * @return Indicates if the choice is currently selected.
     */
    public boolean isSelected()
    {
        return selected;
    }
    /**
     * @return The text of the choice.
     */
    public String getText()
    {
        return text;
    }
}