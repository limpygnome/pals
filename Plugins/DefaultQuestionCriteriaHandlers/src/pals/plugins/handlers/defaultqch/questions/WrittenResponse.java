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
import pals.plugins.handlers.defaultqch.data.Written_Question;
import static pals.plugins.handlers.defaultqch.questions.QuestionHelper.handle_questionEditPostback;

/**
 * Handles multiple-choice questions.
 */
public class WrittenResponse
{
    // Constants ***************************************************************
    public static final UUID    UUID_QTYPE = UUID.parse("fcfec4ec-3347-4219-a988-f69b632b657e");
    public static final String  TITLE = "Written Response";
    public static final String  DESCRIPTION = "Allows students to provide a written response.";
    // Methods *****************************************************************
    public static boolean pageQuestionEdit(WebRequestData data, Question q)
    {
        // Load question data
        Written_Question qdata = (Written_Question)q.getData();
        if(qdata == null)
            qdata = new Written_Question();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String  qTitle = req.getField("q_title"),
                qDesc = req.getField("q_desc");
        String  questionText = req.getField("question_text");
        if(questionText != null)
        {
            // Update data model
            qdata.setText(questionText);
            // Handle the rest of the request
            handle_questionEditPostback(data, q, qTitle, qDesc, qdata);
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit");
        data.setTemplateData("pals_content", "defaultqch/questions/written_response_edit");
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("q_title", qTitle != null ? qTitle : q.getTitle());
        data.setTemplateData("q_desc", qDesc != null ? qDesc : q.getDescription());
        data.setTemplateData("question_text", questionText != null ? questionText : qdata.getText());
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    public static boolean pageQuestionCapture(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure)
    {
        // Load question data
        Written_Question qdata = (Written_Question)iaq.getAssignmentQuestion().getQuestion().getData();
        // Load answer data
        String adata = (String)iaq.getData();
        // Check postback
        int aqid = iaq.getAssignmentQuestion().getAQID();
        String answer = data.getRequestData().getField("written_response_"+aqid);
        HashMap<String,Object> kvs = new HashMap<>();
        if(secure && answer != null)
        {
            // Update the iaq model and persist
            iaq.setData(answer);
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
        kvs.put("answer", answer != null ? answer : adata);
        kvs.put("aqid", aqid);
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/written_response_capture"));
        return true;
    }
    public static boolean pageQuestionDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure, boolean editMode)
    {
        // Load question data
        Written_Question qdata = (Written_Question)iaq.getAssignmentQuestion().getQuestion().getData();
        // Load answer data
        String adata = (String)iaq.getData();
        // Render the template
        HashMap<String,Object> kvs = new HashMap<>();
        kvs.put("text", qdata.getText());
        kvs.put("answer", adata);
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/written_response_display"));
        return true;
    }
}
