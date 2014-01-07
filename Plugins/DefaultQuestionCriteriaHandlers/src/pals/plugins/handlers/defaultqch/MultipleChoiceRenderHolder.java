package pals.plugins.handlers.defaultqch;

/**
 * A simple class for the template engine to render the possible choices of a
 * multiple choice question.
 */
public class MultipleChoiceRenderHolder
{
    private int         number;
    private String      text;
    private boolean     selected;
    public MultipleChoiceRenderHolder(int number, String text, boolean selected)
    {
        this.number = number;
        this.text = text;
        this.selected = selected;
    }
    public int getNumber()
    {
        return number;
    }
    public boolean isSelected()
    {
        return selected;
    }
    public String getText()
    {
        return text;
    }
}