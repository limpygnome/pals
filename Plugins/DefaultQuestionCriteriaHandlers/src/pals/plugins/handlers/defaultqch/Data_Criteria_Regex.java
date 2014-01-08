package pals.plugins.handlers.defaultqch;

import java.io.Serializable;

/**
 * Stores the settings for a regex-match criteria.
 */
public class Data_Criteria_Regex implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String   regexPattern;   // The pattern applied to an answer.
    private int      mode;           // The mode of the matcher; refer to: 'http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html'.
    // Methods - Constructors **************************************************
    public Data_Criteria_Regex()
    {
        this.regexPattern = "";
        this.mode = 0;
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
}
