package pals.plugins.handlers.defaultqch.criterias;

import pals.base.NodeCore;
import pals.base.Storage;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.QuestionCriteria;
import pals.base.database.Connector;
import pals.base.web.WebRequestData;

/**
 * Handles java class existence criteria marking.
 */
public class JavaClassExists
{
    // Constants ***************************************************************
    public static final UUID UUID_CTYPE = UUID.parse("0ce02a08-9d6d-4d1d-bd8d-536d60fc1b65");
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        return false;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
            iac.setMark(0);
        else
        {
            // Fetch path for assignment
            //String path = Storage.getPath_tempAssignmentInstanceQuestion(get, iac.getIAQ())
        }
        // Update the model
        iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        return false;
    }
}
