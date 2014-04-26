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
package pals.plugins.handlers.defaultqch.criterias;

import java.util.ArrayList;
import java.util.HashMap;
import pals.base.NodeCore;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.QuestionCriteria;
import pals.base.database.Connector;
import pals.base.utils.Misc;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.data.MultipleChoice_Instance;
import pals.plugins.handlers.defaultqch.data.MultipleChoice_InstanceCriteria;
import pals.plugins.handlers.defaultqch.data.MultipleChoice_Question;
import pals.plugins.handlers.defaultqch.questions.MCQ;

/**
 * Handles a multiple-choice criteria.
 */
public class MultipleChoice
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("a45bf985-5b0e-4643-bce7-d55b9e5f24f8");
    public static final String  TITLE = "Multiple Choice/Response Choices";
    public static final String  DESCRIPTION = "Gives marks for the same choices selected.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load question data
        MultipleChoice_Question qdata;
        if(qc.getQuestion().getData() != null)
            qdata = (MultipleChoice_Question)qc.getQuestion().getData();
        else
            qdata = null;
        // Load criteria data
        MultipleChoice_InstanceCriteria cdata;
        if(qc.getData() != null)
            cdata = (MultipleChoice_InstanceCriteria)qc.getData();
        else
            cdata = new MultipleChoice_InstanceCriteria();
        // Fetch the possible answers
        String[] answers;
        if(qdata != null)
            answers = qdata.getAnswers();
        else
        {
            answers = new String[0];
            data.setTemplateData("error", "The question's multiple-choice data has not been defined!");
        }
        int totalAnswers = answers.length;
        boolean[] itemsSelected = new boolean[totalAnswers];
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        if(qdata != null && critTitle != null && critWeight != null)
        {
            // Fetch the items selected
            ArrayList<Integer> buffer = new ArrayList<>();
            for(int i = 0; i < totalAnswers; i++)
            {
                if(req.getField("crit_index_"+i) != null)
                {
                    buffer.add(i);
                    itemsSelected[i] = true;
                }
            }
            // Update the data model
            cdata.setIndexesCorrect(buffer.toArray(new Integer[buffer.size()]));
            // Handle entire process
            CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
        }
        else
        {
            Integer[] inds = cdata.getIndexesCorrect();
            // Update the items selected previously
            // -- Potentially O(n^2), however we want to reduce storage on the database
            // -- since a MCQ could have a lot of items; a large array, instead, would be a lot more wasteful.
            // -- -- This will only occur when editing or marking the criteria, both rare events.
            for(int i = 0; i < totalAnswers; i++)
                itemsSelected[i] = Misc.arrayContains(inds, i);
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/multiplechoice_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : String.valueOf(qc.getWeight()));
        data.setTemplateData("crit_items_selected", itemsSelected);
        data.setTemplateData("crit_items_total", totalAnswers);
        data.setTemplateData("crit_items", qdata != null ? qdata.getAnswers() : new String[0]);
        return true;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
            iac.setMark(0);
        else
        {
            UUID qtype = iac.getIAQ().getAssignmentQuestion().getQuestion().getQtype().getUuidQType();
            if(qtype == null || !qtype.equals(MCQ.UUID_QTYPE))
                return false;
            // Load question, answer and criteria data
            MultipleChoice_Question qdata = (MultipleChoice_Question)iac.getQC().getQuestion().getData();
            MultipleChoice_Instance adata = (MultipleChoice_Instance)iac.getIAQ().getData();
            MultipleChoice_InstanceCriteria cdata = (MultipleChoice_InstanceCriteria)iac.getQC().getData();
            // Check the indexes selected match the correct indexes
            boolean correct = true;
            Integer[] answers = adata.getAnswers();
            Integer[] correctAnswers = cdata.getIndexesCorrect();
            if(answers.length == correctAnswers.length)
            {
                for(int i = 0; i < answers.length; i++)
                {
                    if(!answers[i].equals(correctAnswers[i]))
                    {
                        correct = false;
                        break;
                    }
                }
            }
            else
                correct = false;
            // Update and persist the mark
            iac.setMark(correct ? 100 : 0);
            iac.setData(correct);
        }
        iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        Object fdata = iac.getData();
        MultipleChoice_Question qdata = (MultipleChoice_Question)iac.getQC().getQuestion().getData();
        MultipleChoice_InstanceCriteria cdata = (MultipleChoice_InstanceCriteria)iac.getQC().getData();
        if(fdata != null && (fdata instanceof Boolean) && qdata != null && cdata != null)
        {
            boolean correct = (Boolean)fdata;
            HashMap<String,Object> kvs = new HashMap<>();
            if(correct)
                kvs.put("success", "Correct answer.");
            else
            {
                kvs.put("error_correct", cdata.getCorrect(qdata));
            }
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/mcq_display"));
            return true;
        }
        return false;
    }
}
