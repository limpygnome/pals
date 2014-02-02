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
        Written_Question qdata;
        if(q.getData() != null)
            qdata = q.getData();
        else
            qdata = new Written_Question();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String questionText = req.getField("question_text");
        if(questionText != null)
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Update data model
                qdata.setText(questionText);
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
        data.setTemplateData("pals_content", "defaultqch/questions/written_response_edit");
        // -- Fields
        data.setTemplateData("question", q);
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
        kvs.put("answer", adata != null ? adata : "");
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/written_response_display"));
        return true;
    }
}
