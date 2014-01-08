package pals.plugins.handlers.defaultqch;

import java.io.Serializable;

/**
 * A model for holding data for a text-match criteria type.
 */
public class Data_Criteria_TextMatch implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String text;
    private boolean caseSensitive;
    // Methods - Constructors **************************************************
    public Data_Criteria_TextMatch()
    {
        this("unspecified text", false);
    }
    public Data_Criteria_TextMatch(String text, boolean caseSensitive)
    {
        this.text = text;
        this.caseSensitive = false;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param text The exact text to be matched.
     */
    public void setText(String text)
    {
        this.text = text;
    }
    /**
     * @param caseSensitive Indicates if matching should be case-sensitive
     * (true) or insensitive (false).
     */
    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The text to match.
     */
    public String getText()
    {
        return text;
    }
    /**
     * @return Indicates if the case should be sensitive (true) or
     * insensitive (false).
     */
    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }
}
