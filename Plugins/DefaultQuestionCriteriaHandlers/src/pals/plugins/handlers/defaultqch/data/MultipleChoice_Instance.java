/*
    The MIT License (MIT)

    Copyright (c) 2014 Marcus Craske <limpygnome@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    ----------------------------------------------------------------------------
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import pals.base.utils.Misc;
import pals.base.web.RemoteRequest;
import pals.plugins.handlers.defaultqch.MultipleChoiceRenderHolder;

/**
 * Stores the answer data for a multiple-choice question. This is the instance
 * question data holder.
 */
public class MultipleChoice_Instance implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private Integer[]                   answers;                // Original indexes stored as items
    private HashMap<Integer,Integer>    randomIndexMappings;    // New index,original index
    // Methods - Constructors **************************************************
    public MultipleChoice_Instance(Random rng, MultipleChoice_Question qdata)
    {
        this.randomIndexMappings = new HashMap<>();
        this.answers = new Integer[0];
        // Create randomly shuffled indexes
        Integer[] items = new Integer[qdata != null ? qdata.getAnswers().length : 0];
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
    public void processAnswers(int aqid, RemoteRequest req, MultipleChoice_Question qdata)
    {
        ArrayList<Integer> buffer = new ArrayList<>();
        String[] answers = req.getFields("multiple_choice_"+aqid);
        if(answers == null)
            return;
        int value;
        for(String answer : answers)
        {
            try
            {
                value = Integer.parseInt(answer);
                if(value < 0 || value >= randomIndexMappings.size())
                    return;
                buffer.add(randomIndexMappings.get(value));
            }
            catch(NumberFormatException ex)
            {
                // Invalid request...
                return;
            }
        }
        // Sort indexes
        Collections.sort(buffer);
        // Check we do not capture more than one answer for single-answer mode
        // -- Else we will ignore the request.
        // -- -- Most likely the user attempting to trick the system.
        if(!(qdata.isSingleAnswer() && buffer.size() > 1))
            this.answers = buffer.toArray(new Integer[buffer.size()]);
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
     * @param qdata The question data.
     * @return String-array of answers.
     */
    public String[] getAnswers(MultipleChoice_Question qdata)
    {
        String[] buffer = new String[answers.length];
        String[] txtAnswers = qdata.getAnswers();
        for(int i = 0; i < answers.length; i++)
            buffer[i] = txtAnswers[answers[i]];
        return buffer;
    }
    /**
     * @param aqid Identifier of the assignment-question.
     * @param req Remote request data.
     * @param qdata The question data.
     * @param postback Indicates if postback has occurred during the current
     * request.
     * @return Creates and returns models representing the possible choices.
     */
    public MultipleChoiceRenderHolder[] getViewModels(int aqid, RemoteRequest req, MultipleChoice_Question qdata, boolean postback)
    {
        String[] txtAnswers = qdata.getAnswers();
        MultipleChoiceRenderHolder[] choices = new MultipleChoiceRenderHolder[txtAnswers.length];
        int originalIndex;
        for(int i = 0; i < txtAnswers.length; i++)
        {
            originalIndex = randomIndexMappings.get(i);
            choices[i] = new MultipleChoiceRenderHolder(i, txtAnswers[originalIndex], (postback && req.getField("multiple_choice_"+aqid+"_"+i)!=null)||Misc.arrayContains(answers, originalIndex));
        }
        return choices;
    }
}
