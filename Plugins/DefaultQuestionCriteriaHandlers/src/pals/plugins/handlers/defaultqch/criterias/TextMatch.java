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

import java.util.HashMap;
import pals.base.NodeCore;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.QuestionCriteria;
import pals.base.database.Connector;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.data.MultipleChoice_Instance;
import pals.plugins.handlers.defaultqch.data.TextMatch_Criteria;
import pals.plugins.handlers.defaultqch.data.MultipleChoice_Question;
import pals.plugins.handlers.defaultqch.questions.MCQ;
import pals.plugins.handlers.defaultqch.questions.WrittenResponse;

/**
 * Handles the text-matching criteria.
 */
public class TextMatch
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("b9a1143c-98cb-446b-9b39-42addac71f4f");
    public static final String  TITLE = "Text Match";
    public static final String  DESCRIPTION = "Gives marks for an answer matching a piece of text.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load criteria data
        TextMatch_Criteria cdata = (TextMatch_Criteria)qc.getData();
        if(cdata == null)
            cdata = new TextMatch_Criteria();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        String critMatch = req.getField("crit_match");
        // -- Optional
        String critSensitive = req.getField("crit_sensitive");
        if(critTitle != null && critWeight != null && critMatch != null)
        {
            // Update model
            cdata.setText(critMatch);
            cdata.setCaseSensitive(critSensitive != null && critSensitive.equals("1"));
            // Handle entire process
            CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/textmatch_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_match", critMatch != null ? critMatch : cdata.getText());
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : String.valueOf(qc.getWeight()));
        if((critMatch == null && cdata.isCaseSensitive()) || (critSensitive != null && critSensitive.equals("1")))
            data.setTemplateData("crit_sensitive", cdata.isCaseSensitive());
        return true;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
            iac.setMark(0);
        else
        {
            UUID qtype = iac.getIAQ().getAssignmentQuestion().getQuestion().getQtype().getUuidQType();
            TextMatch_Criteria cdata = (TextMatch_Criteria)iac.getQC().getData();
            String match = cdata.isCaseSensitive() ? cdata.getText() : cdata.getText().toLowerCase();
            boolean matched = false;
            if(qtype.equals(WrittenResponse.UUID_QTYPE))
            {
                
                String text = (String)iac.getIAQ().getData();
                if(!cdata.isCaseSensitive())
                    text = text.toLowerCase();
                matched = text.equals(match);
            }
            else if(qtype.equals(MCQ.UUID_QTYPE))
            {
                MultipleChoice_Question qdata = (MultipleChoice_Question)iac.getQC().getQuestion().getData();
                MultipleChoice_Instance adata = (MultipleChoice_Instance)iac.getIAQ().getData();
                String[] text = adata.getAnswers(qdata);
                for(String t : text)
                {
                    if((cdata.isCaseSensitive() ? t : t.toLowerCase()).equals(match))
                    {
                        matched = true;
                        break;
                    }
                }
            }
            else
                return false;
            // Update and persist the mark
            iac.setMark(matched ? 100 : 0);
            iac.setData(matched);
        }
        iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        Object fdata = iac.getData();
        TextMatch_Criteria cdata = (TextMatch_Criteria)iac.getQC().getData();
        if(fdata != null && (fdata instanceof Boolean) && cdata != null)
        {
            boolean matched = (Boolean)fdata;
            HashMap<String,Object> kvs = new HashMap<>();
            kvs.put(matched ? "success" : "error", matched ? "Correct answer." : "The correct answer was '"+cdata.getText()+"'.");
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/feedback_display"));
            return true;
        }
        return false;
    }
}
