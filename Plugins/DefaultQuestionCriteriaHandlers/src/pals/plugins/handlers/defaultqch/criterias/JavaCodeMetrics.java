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
 * Handles code text-metrics criteria marking.
 */
public class JavaCodeMetrics
{
    // lines of code
    // correctly named classes and methods - capitalisation ~ refer to java api
    // ratio of comment-lines to code-lines
    // style++ ~ use p1,p2,p3,p4 values idea (from former NY paper / rees 1982):
    // -- lines of code
    // -- blank lines
    // -- comment lines
    // -- number of global variables
    // -- number of local variables
    // -- number of numeric constants
    // -- number of classes
    // -- number of methods
    // -- average methods per class
    
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("5855d34f-cf37-435f-bddd-0dbfcea72fd7");
    public static final String  TITLE = "Java: Code Metrics";
    public static final String  DESCRIPTION = "Performs static analysis on code using metrics.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        return false;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        return false;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        return false;
    }
}
