package pals.plugins.handlers.defaultqch;

import java.util.HashMap;
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
public class Handler_Criteria_ManualMarking
{
    // Constants ***************************************************************
    public static final UUID UUID_CTYPE = UUID.parse("03830aa9-39d7-4bfe-9ab5-a9c765e6e426");
    // Methods *****************************************************************
    static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        if(critTitle != null && critWeight != null)
        {
            // Validate security
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                try
                {
                    int weight = Integer.parseInt(critWeight);
                    // Update qc model
                    qc.setTitle(critTitle);
                    qc.setWeight(weight);
                    // Persist qc model
                    QuestionCriteria.PersistStatus qcps = qc.persist(data.getConnector());
                    switch(qcps)
                    {
                        case Failed:
                        case Failed_Serialize:
                        case Invalid_Criteria:
                        case Invalid_Question:
                            data.setTemplateData("error", "Failed to update model due to an unknown error ('"+qcps.name()+"'); try again or contact an administrator!");
                            break;
                        case Invalid_Title:
                            data.setTemplateData("error", "Invalid title; must be "+qc.getTitleMin()+" to "+qc.getTitleMax()+" characters in length!");
                            break;
                        case Invalid_Weight:
                            data.setTemplateData("error", "Invalid weight; must be numeric and greater than zero!");
                            break;
                        case Success:
                            data.setTemplateData("success", "Updated criteria settings successfully.");
                            break;
                    }
                }
                catch(NumberFormatException ex)
                {
                    data.setTemplateData("error", "Weight must be numeric!");
                }
            }
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
    static boolean criteriaMarking(Connector conn, InstanceAssignmentCriteria iac)
    {
        // Set the criteria to manual-marking status
        iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        boolean editMode = data.containsTemplateData("edit_mode") && (boolean)data.getTemplateData("edit_mode");
        boolean secure = data.containsTemplateData("secure") && (boolean)data.getTemplateData("secure");
        Object fdata = iac.getData();
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
        if(fdata != null && (fdata instanceof String))
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
