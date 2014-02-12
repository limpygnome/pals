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
 * Handles Java field existence criteria marking.
 */
public class JavaExistsField
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("69078967-3604-4a6f-a4db-80453ad4c747");
    public static final String  TITLE = "Java: Field Exists";
    public static final String  DESCRIPTION = "Checks the existence and modifiers of a field.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        return JavaExistsShared.pageCriteriaEdit(data, qc, JavaExistsShared.CriteriaType.Field);
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        return JavaExistsShared.criteriaMarking(conn, core, iac, JavaExistsShared.CriteriaType.Field);
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        return JavaExistsShared.criteriaDisplay(data, ia, iaq, iac, html, JavaExistsShared.CriteriaType.Field);
    }
}
