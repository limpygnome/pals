package pals.plugins.handlers.defaultqch;

/**
 * A simple class for the template engine to render the possible choices of a
 * multiple choice question.
 */
public class MultipleChoiceRenderHolder
{
    private String   text;
    private boolean  selected;
    public MultipleChoiceRenderHolder(String text, boolean selected)
    {
        this.text = text;
        this.selected = selected;
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