package pals.plugins.handlers.defaultqch.criterias;

import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.QuestionCriteria;
import pals.base.database.Connector;
import pals.base.web.WebRequestData;

/**
 * Handles code text-metrics criteria marking.
 */
public class JavaCodeMetrics
{
    // Constants ***************************************************************
    public static final UUID UUID_CTYPE = UUID.parse("5855d34f-cf37-435f-bddd-0dbfcea72fd7");
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        return false;
    }
    public static boolean criteriaMarking(Connector conn, InstanceAssignmentCriteria iac)
    {
        return false;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        return false;
    }
}
