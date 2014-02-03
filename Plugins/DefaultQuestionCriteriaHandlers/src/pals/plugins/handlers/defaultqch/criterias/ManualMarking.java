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

/**
 * Handles the manual-marking criteria.
 */
public class ManualMarking
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("03830aa9-39d7-4bfe-9ab5-a9c765e6e426");
    public static final String  TITLE = "Manual Marking/Feedback";
    public static final String  DESCRIPTION = "Allows a marker to provide manual feedback for a question.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        if(critTitle != null && critWeight != null)
        {
            // Handle entire process
            CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, null);
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/manual_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        return true;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        // Set the criteria to manual-marking status
        iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        boolean editMode = data.containsTemplateData("edit_mode") && (boolean)data.getTemplateData("edit_mode");
        boolean secure = data.containsTemplateData("secure") && (boolean)data.getTemplateData("secure");
        String fdata = (String)iac.getData();
        HashMap<String,Object> kvs = new HashMap<>();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String feedback = req.getField("feedback_"+iaq.getAIQID()+"_"+iac.getQC().getQCID());
        if(feedback != null)
        {
            fdata = feedback;
            if(!secure)
                kvs.put("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Update the criteria
                iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
                iac.setData(fdata);
                // Persist
                InstanceAssignmentCriteria.PersistStatus iacps = iac.persist(data.getConnector());
                switch(iacps)
                {
                    default:
                        kvs.put("error", "Failed to update feedback ('"+iacps.name()+"'); please try again or contact an administrator!");
                        break;
                    case Success:
                        // Check if to recompute the assignment's grade
                        if(ia.isMarkComputationNeeded(data.getConnector()))
                        {
                            if(ia.computeMark(data.getConnector()))
                            {
                                ia.setStatus(InstanceAssignment.Status.Marked);
                                InstanceAssignment.PersistStatus iaps = ia.persist(data.getConnector());
                                switch(iaps)
                                {
                                    default:
                                        kvs.put("error", "Failed to update the status of the assignment to marked.");
                                        break;
                                    case Success:
                                        kvs.put("success", "Updated feedback, computed assignment grade and set status to marked.");
                                        break;
                                }
                            }
                            else
                                kvs.put("error", "Could not compute the grade for this assignment.");
                        }
                        else
                            kvs.put("success", "Updated feedback.");
                        break;
                }
            }
        }
        // Render template
        if(fdata != null)
        {
            String text = (String)fdata;
            if(text.length() > 0)
                kvs.put("text", text);
        }
        kvs.put("edit_mode", editMode);
        kvs.put("aiqid", iaq.getAIQID());
        kvs.put("qcid", iac.getQC().getQCID());
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/manual_display"));
        return true;
    }
}
