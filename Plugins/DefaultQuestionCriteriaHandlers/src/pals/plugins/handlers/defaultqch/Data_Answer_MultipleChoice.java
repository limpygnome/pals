package pals.plugins.handlers.defaultqch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import pals.base.utils.Misc;
import pals.base.web.RemoteRequest;

/**
 * Stores the answer data for a multiple-choice question.
 */
public class Data_Answer_MultipleChoice implements Serializable
{
    // Fields ******************************************************************
    private Integer[]                   answers;                // Original indexes stored as items
    private HashMap<Integer,Integer>    randomIndexMappings;    // Random index,original index
    // Methods - Constructors **************************************************
    public Data_Answer_MultipleChoice(Random rng, Data_Question_MultipleChoice qdata)
    {
        this.randomIndexMappings = new HashMap<>();
        this.answers = new Integer[0];
        // Create randomly shuffled indexes
        Integer[] items = new Integer[qdata.answers.length];
        for(int i = 0; i < items.length; i++)
            items[i] = i;
        Misc.arrayShuffle(rng, items);
        // Create mappings
        for(int i = 0; i < items.length; i++)
            randomIndexMappings.put(i, items[i]);
    }
    // Methods *****************************************************************
    /**
     * Processes the answers from a postback.
     * 
     * @param aqid Identifier of the assignment-question.
     * @param req Remote request data.
     * @param qdata The question data.
     */
    public void processAnswers(int aqid, RemoteRequest req, Data_Question_MultipleChoice qdata)
    {
        ArrayList<Integer> answers = new ArrayList<>();
        for(int i = 0; i < qdata.answers.length; i++)
        {
            if(req.getField("multiple_choice_"+aqid+"_"+i) != null)
                answers.add(randomIndexMappings.get(i));
        }
        this.answers = answers.toArray(new Integer[answers.size()]);
    }
    // Methods - Mutators ******************************************************
    /**
     * @param answers The indexes of the answers selected, corresponding to the
     * possible answers in the Data_Question_MultipleChoice model.
     */
    public void setAnswers(Integer[] answers)
    {
        this.answers = answers;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The indexes of the answers selected, corresponding to the
     * possible answers in the Data_Question_MultipleChoice model.
     */
    public Integer[] getAnswers()
    {
        return answers;
    }
    /**
     * @param aqid Identifier of the assignment-question.
     * @param req Remote request data.
     * @param qdata The question data.
     * @param postback Indicates if postback has occurred during the current
     * request.
     * @return Creates and returns models representing the possible choices.
     */
    public MultipleChoiceRenderHolder[] getViewModels(int aqid, RemoteRequest req, Data_Question_MultipleChoice qdata, boolean postback)
    {
        MultipleChoiceRenderHolder[] choices = new MultipleChoiceRenderHolder[qdata.answers.length];
        int originalIndex;
        for(int i = 0; i < qdata.answers.length; i++)
        {
            originalIndex = randomIndexMappings.get(i);
            choices[i] = new MultipleChoiceRenderHolder(i, qdata.answers[originalIndex], (postback && req.getField("multiple_choice_"+aqid+"_"+i)!=null)||Misc.arrayContains(answers, originalIndex));
        }
        return choices;
    }
}
