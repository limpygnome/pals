package pals.plugins.handlers.defaultqch;

import java.io.Serializable;

/**
 * Stores the settings data for a multiple-choice question.
 */
public class Data_MultipleChoice_Question implements Serializable
{
    public String text = "Undefined question text...";
    public boolean singleAnswer = false;
    public String[] answers = new String[]{};
    
    public String getAnswersWebFormat()
    {
        StringBuilder sb = new StringBuilder();
        for(String s : answers)
            sb.append(s).append("\n");
        return sb.toString();
    }
}
