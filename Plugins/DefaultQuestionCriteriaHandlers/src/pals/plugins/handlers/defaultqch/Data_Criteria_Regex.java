package pals.plugins.handlers.defaultqch;

import java.io.Serializable;

/**
 * Stores the settings for a regex-match criteria.
 */
public class Data_Criteria_Regex implements Serializable
{
    public String   regexPattern;   // The pattern applied to an answer.
    public int      mode;           // The mode of the matcher; refer to: 'http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html'.
    
    public Data_Criteria_Regex()
    {
        this.regexPattern = "";
        this.mode = 0;
    }
}
