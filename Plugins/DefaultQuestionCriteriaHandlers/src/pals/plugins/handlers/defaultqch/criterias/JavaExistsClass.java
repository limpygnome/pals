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
 * Handles Java class existence criteria marking.
 */
public class JavaExistsClass
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("0ce02a08-9d6d-4d1d-bd8d-536d60fc1b65");
    public static final String  TITLE = "Java: Class Exists";
    public static final String  DESCRIPTION = "Checks the existence and modifiers of a class.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        return JavaExistsShared.pageCriteriaEdit(data, qc, JavaExistsShared.CriteriaType.Class);
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        return JavaExistsShared.criteriaMarking(conn, core, iac, JavaExistsShared.CriteriaType.Class);
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        return JavaExistsShared.criteriaDisplay(data, ia, iaq, iac, html, JavaExistsShared.CriteriaType.Class);
    }
}