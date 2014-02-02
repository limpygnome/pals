package pals.plugins.handlers.defaultqch.criterias;

import pals.base.NodeCore;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.QuestionCriteria;
import pals.base.database.Connector;
import pals.base.web.WebRequestData;

/**
 * Handles Java method existence criteria marking.
 */
public class JavaMethodExists
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("1746eb7b-eda5-45ee-8c45-699f7d0a6d0c");
    public static final String  TITLE = "Java: Method Exists";
    public static final String  DESCRIPTION = "Checks the existence and modifiers of a method.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        return JavaExistsShared.pageCriteriaEdit(data, qc, JavaExistsShared.CriteriaType.Method);
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        return JavaExistsShared.criteriaMarking(conn, core, iac, JavaExistsShared.CriteriaType.Method);
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        return JavaExistsShared.criteriaDisplay(data, ia, iaq, iac, html, JavaExistsShared.CriteriaType.Method);
    }
}
