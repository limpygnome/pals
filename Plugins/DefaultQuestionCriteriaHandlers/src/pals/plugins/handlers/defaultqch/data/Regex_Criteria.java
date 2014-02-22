package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;

/**
 * Stores the settings for a regex-match criteria.
 */
public class Regex_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String      regexPattern;       // The pattern applied to an answer.
    private int         mode;               // The mode of the matcher; refer to: 'http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html'.
    private boolean     hidePattern;        // Indicates if to hide the pattern from users.
    private boolean     invert;             // Indicates if to invert marks for matching.
    // Methods - Constructors **************************************************
    public Regex_Criteria()
    {
        this.regexPattern = "";
        this.mode = 0;
        this.hidePattern = false;
        this.invert = false;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param regexPattern Sets the regex (regular expressions) pattern used for
     * matching.
     */
    public void setRegexPattern(String regexPattern)
    {
        this.regexPattern = regexPattern;
    }
    /**
     * @param mode The mode of the Java Regex Pattern; refer to the following
     * for documentation:
     * http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
     */
    public void setMode(int mode)
    {
        this.mode = mode;
    }
    /**
     * @param hidePattern Sets if to hide the matching-pattern from the
     * end-users.
     */
    public void setHide(boolean hidePattern)
    {
        this.hidePattern = hidePattern;
    }
    /**
     * @param invert Sets if to invert marking; this will only give marks
     * if no match occurs.
     */
    public void setInvert(boolean invert)
    {
        this.invert = invert;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The regular expression pattern used for matching.
     */
    public String getRegexPattern()
    {
        return regexPattern;
    }
    /**
     * @return The mode used for the Java Pattern object, for matching.
     */
    public int getMode()
    {
        return mode;
    }
    /**
     * @return Indicates if to hide the pattern.
     */
    public boolean getHide()
    {
        return hidePattern;
    }
    /**
     * @return Indicates if to invert marking; if true, this will only reward
     * marks if no match occurs.
     */
    public boolean getInvert()
    {
        return invert;
    }
}
