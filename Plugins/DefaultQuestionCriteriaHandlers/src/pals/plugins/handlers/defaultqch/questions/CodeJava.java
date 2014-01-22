package pals.plugins.handlers.defaultqch;

import java.util.HashMap;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Question;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;

/**
 * Handles multiple-choice questions.
 */
public class Handler_Question_MCQ
{
    // Constants ***************************************************************
    public static final UUID UUID_QTYPE = UUID.parse("f38c3f28-4f63-49b5-994c-fd618c654de0");
    // Methods *****************************************************************
    static boolean pageQuestionEdit_multipleChoice(WebRequestData data, Question q)
    {
        // Load question data
        Data_Question_MultipleChoice qdata;
        if(q.getData() != null)
            qdata = q.getData();
        else
            qdata = new Data_Question_MultipleChoice();
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
        data.setTemplateData("mc_text", mcText != null ? mcText : qdata.getText());
        if((mcSingleAnswer != null && mcSingleAnswer.equals("1")) || (mcText == null && qdata.isSingleAnswer()))
            data.setTemplateData("mc_single_answer", true);
        data.setTemplateData("mc_answers", mcAnswers != null ? mcAnswers : qdata.getAnswersWebFormat());
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    static boolean pageQuestionCapture_multipleChoice(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure)
    {
        // Load question data
        Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iaq.getAssignmentQuestion().getQuestion().getData();
        if(qdata == null)
            return false;
        // Load answer data
        Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iaq.getData();
        // -- New attempt; create random indexes and persist...
        if(adata == null)
        {
            adata = new Data_Answer_MultipleChoice(data.getCore().getRNG(), qdata);
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
    static boolean pagepageQuestionDisplay_multipleChoice(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure, boolean editMode)
    {
        // Load question data
        Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iaq.getAssignmentQuestion().getQuestion().getData();
        // Load answer data
        Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iaq.getData();
        // Render the template
        HashMap<String,Object> kvs = new HashMap<>();
        kvs.put("text", qdata.getText());
        kvs.put("answers", adata != null ? adata.getAnswers(qdata) : new String[0]);
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/multiplechoice_display"));
        return true;
    }
}
