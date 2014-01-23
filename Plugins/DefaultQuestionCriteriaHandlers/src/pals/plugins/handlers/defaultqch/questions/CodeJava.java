package pals.plugins.handlers.defaultqch.questions;

import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Question;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.data.CodeJava_Question;

/**
 * Handles code-fragment questions.
 */
public class CodeJava
{
    // Constants ***************************************************************
    public static final UUID UUID_QTYPE = UUID.parse("3b452432-d939-4e39-a450-3867655412a3");
    // Methods *****************************************************************
    public static boolean pageQuestionEdit(WebRequestData data, Question q)
    {
        // Load question data
        CodeJava_Question qdata;
        if(q.getData() != null)
            qdata = q.getData();
        else
            qdata = new CodeJava_Question();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String mcText = req.getField("mc_text");
        String mcType = req.getField("mc_type");
        String mcWhitelist = req.getField("mc_whitelist");
        if(mcText != null && mcType != null && mcWhitelist != null)
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Update question data
                qdata.setText(mcText);
                qdata.setType(CodeJava_Question.QuestionType.parse(mcType));
                qdata.setWhitelist(mcWhitelist.replace("\r", "").split("\n"));
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
        data.setTemplateData("pals_content", "defaultqch/questions/codejava_edit");
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("mc_text", qdata.getText());
        data.setTemplateData("mc_type", qdata.getType().getFormValue());
        data.setTemplateData("mc_whitelist", qdata.getWhitelistWeb());
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    public static boolean pageQuestionCapture(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure)
    {
//        // Load question data
//        Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iaq.getAssignmentQuestion().getQuestion().getData();
//        if(qdata == null)
//            return false;
//        // Load answer data
//        Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iaq.getData();
//        // -- New attempt; create random indexes and persist...
//        if(adata == null)
//        {
//            adata = new Data_Answer_MultipleChoice(data.getCore().getRNG(), qdata);
//            iaq.setData(adata);
//            iaq.persist(data.getConnector());
//        }
//        // Check postback
//        int aqid = iaq.getAssignmentQuestion().getAQID();
//        String pb = data.getRequestData().getField("multiple_choice_pb_"+aqid);
//        HashMap<String,Object> kvs = new HashMap<>();
//        if(secure && pb != null)
//        {
//            // Process answers
//            adata.processAnswers(aqid, data.getRequestData(), qdata);
//            // Update the iaq model and persist
//            iaq.setData(adata);
//            iaq.setAnswered(true);
//            InstanceAssignmentQuestion.PersistStatus iaqps = iaq.persist(data.getConnector());
//            switch(iaqps)
//            {
//                case Failed:
//                case Failed_Serialize:
//                case Invalid_AssignmentQuestion:
//                case Invalid_InstanceAssignment:
//                    kvs.put("error", "Failed to update question ('"+iaqps.name()+"')!");
//                    break;
//                case Success:
//                    kvs.put("success", "Saved answer.");
//                    break;
//            }
//        }
//        // Render the template
//        kvs.put("text", qdata != null ? qdata.getText() : "No question defined...");
//        kvs.put("choices", adata.getViewModels(aqid, data.getRequestData(), qdata, pb != null));
//        if(qdata.isSingleAnswer())
//            kvs.put("single_choice", true);
//        kvs.put("aqid", aqid);
//        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/multiplechoice_capture"));
        return true;
    }
    public static boolean pageQuestionDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure, boolean editMode)
    {
//        // Load question data
//        Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iaq.getAssignmentQuestion().getQuestion().getData();
//        // Load answer data
//        Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iaq.getData();
//        // Render the template
//        HashMap<String,Object> kvs = new HashMap<>();
//        kvs.put("text", qdata.getText());
//        kvs.put("answers", adata != null ? adata.getAnswers(qdata) : new String[0]);
//        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/multiplechoice_display"));
        return true;
    }
}
