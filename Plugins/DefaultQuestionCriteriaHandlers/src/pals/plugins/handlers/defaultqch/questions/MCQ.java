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
package pals.plugins.handlers.defaultqch.questions;

import java.util.HashMap;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Question;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.data.MultipleChoice_Instance;
import pals.plugins.handlers.defaultqch.data.MultipleChoice_Question;

/**
 * Handles multiple-choice questions.
 */
public class MCQ
{
    // Constants ***************************************************************
    public static final UUID    UUID_QTYPE = UUID.parse("f38c3f28-4f63-49b5-994c-fd618c654de0");
    public static final String  TITLE = "Multiple Choice/Response";
    public static final String  DESCRIPTION = "Allows students to pick an answer, or multiple answers, from a set of possible choices.";
    // Methods *****************************************************************
    public static boolean pageQuestionEdit(WebRequestData data, Question q)
    {
        // Load question data
        MultipleChoice_Question qdata = (MultipleChoice_Question)q.getData();
        if(qdata == null)
            qdata = new MultipleChoice_Question();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String mcText = req.getField("mc_text");
        String mcSingleAnswer = req.getField("mc_single_answer");
        String mcAnswers = req.getField("mc_answers");
        if(mcText != null && mcAnswers != null)
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Update question data
                qdata.setText(mcText);
                qdata.setSingleAnswer(mcSingleAnswer != null && mcSingleAnswer.equals("1"));
                qdata.setAnswers(mcAnswers.replace("\r", "").split("\n"));
                // Persist the model
                q.setData(qdata);
                Question.PersistStatus psq = q.persist(data.getConnector());
                switch(psq)
                {
                    default:
                        data.setTemplateData("error", "Failed to persist question data; error '"+psq.name()+"'!");
                    case Success:
                        data.setTemplateData("success", "Successfully updated question.");
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit");
        data.setTemplateData("pals_content", "defaultqch/questions/multiplechoice_edit");
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("mc_text", qdata.getText());
        if((mcSingleAnswer != null && mcSingleAnswer.equals("1")) || (mcText == null && qdata.isSingleAnswer()))
            data.setTemplateData("mc_single_answer", true);
        data.setTemplateData("mc_answers", qdata.getAnswersWebFormat());
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    public static boolean pageQuestionCapture(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure)
    {
        // Load question data
        MultipleChoice_Question qdata = (MultipleChoice_Question)iaq.getAssignmentQuestion().getQuestion().getData();
        if(qdata == null)
            return false;
        // Load answer data
        MultipleChoice_Instance adata = (MultipleChoice_Instance)iaq.getData();
        // -- New attempt; create random indexes and persist...
        if(adata == null)
        {
            adata = new MultipleChoice_Instance(data.getCore().getRNG(), qdata);
            iaq.setData(adata);
            iaq.persist(data.getConnector());
        }
        // Check postback
        int aqid = iaq.getAssignmentQuestion().getAQID();
        String pb = data.getRequestData().getField("multiple_choice_pb_"+aqid);
        HashMap<String,Object> kvs = new HashMap<>();
        if(secure && pb != null)
        {
            // Process answers
            adata.processAnswers(aqid, data.getRequestData(), qdata);
            // Update the iaq model and persist
            iaq.setData(adata);
            iaq.setAnswered(true);
            InstanceAssignmentQuestion.PersistStatus iaqps = iaq.persist(data.getConnector());
            switch(iaqps)
            {
                case Failed:
                case Failed_Serialize:
                case Invalid_AssignmentQuestion:
                case Invalid_InstanceAssignment:
                    kvs.put("error", "Failed to update question ('"+iaqps.name()+"')!");
                    break;
                case Success:
                    kvs.put("success", "Saved answer.");
                    break;
            }
        }
        // Render the template
        kvs.put("text", qdata != null ? qdata.getText() : "No question defined...");
        kvs.put("choices", adata.getViewModels(aqid, data.getRequestData(), qdata, pb != null));
        if(qdata.isSingleAnswer())
            kvs.put("single_choice", true);
        kvs.put("aqid", aqid);
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/multiplechoice_capture"));
        return true;
    }
    public static boolean pageQuestionDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure, boolean editMode)
    {
        // Load question data
        MultipleChoice_Question qdata = (MultipleChoice_Question)iaq.getAssignmentQuestion().getQuestion().getData();
        // Load answer data
        MultipleChoice_Instance adata = (MultipleChoice_Instance)iaq.getData();
        // Render the template
        HashMap<String,Object> kvs = new HashMap<>();
        kvs.put("text", qdata.getText());
        kvs.put("answers", adata != null ? adata.getAnswers(qdata) : new String[0]);
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/multiplechoice_display"));
        return true;
    }
}
