package pals.plugins.handlers.defaultqch;

import java.io.Serializable;

/**
 * Stores the settings data for a multiple-choice question.
 */
public class Data_Question_MultipleChoice implements Serializable
{
    public String text;
    public boolean singleAnswer;
    public String[] answers;
    
    public Data_Question_MultipleChoice()
    {
        this.text = "Undefined question text...";
        this.singleAnswer = false;
        this.answers = new String[]{};
    }
    
    public String getAnswersWebFormat()
    {
        StringBuilder sb = new StringBuilder();
        for(String s : answers)
            sb.append(s).append("\n");
        return sb.toString();
    }
}
