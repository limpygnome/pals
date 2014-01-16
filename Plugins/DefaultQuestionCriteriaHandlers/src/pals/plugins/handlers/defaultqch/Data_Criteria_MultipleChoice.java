package pals.plugins.handlers.defaultqch;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Holds data for a multiple-choice criteria.
 */
public class Data_Criteria_MultipleChoice implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private Integer[] indexesCorrect;
    // Methods - Constructors **************************************************
    public Data_Criteria_MultipleChoice()
    {
        this.indexesCorrect = new Integer[0];
    }
    public Data_Criteria_MultipleChoice(Integer[] values)
    {
        this.indexesCorrect = values;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param indexesCorrect The indexes of the multiple choice answers which
     * are correct.
     */
    public void setIndexesCorrect(Integer[] indexesCorrect)
    {
        this.indexesCorrect = indexesCorrect;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The indexes of the choices which are correct.
     */
    public Integer[] getIndexesCorrect()
    {
        return indexesCorrect;
    }
    /**
     * @param qdata Question data.
     * @return Correct answers as text; can be empty.
     */
    public String[] getCorrect(Data_Question_MultipleChoice qdata)
    {
        ArrayList<String> buffer = new ArrayList<>();
        String[] text = qdata.getAnswers();
        for(Integer index : indexesCorrect)
        {
            if(index >= 0 && index < text.length)
                buffer.add(text[index]);
        }
        return buffer.toArray(new String[buffer.size()]);
    }
}
