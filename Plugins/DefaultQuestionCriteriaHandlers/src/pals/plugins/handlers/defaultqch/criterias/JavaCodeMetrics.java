package pals.plugins.handlers.defaultqch.criterias;

import pals.base.Logging;
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
import pals.plugins.handlers.defaultqch.criterias.metrics.AverageIdentifiers;
import pals.plugins.handlers.defaultqch.criterias.metrics.BlankLines;
import pals.plugins.handlers.defaultqch.criterias.metrics.CommentLines;
import pals.plugins.handlers.defaultqch.criterias.metrics.LinesOfCode;
import pals.plugins.handlers.defaultqch.criterias.metrics.Metric;
import pals.plugins.handlers.defaultqch.data.CodeJava_Instance;
import pals.plugins.handlers.defaultqch.data.JavaCodeMetrics_Criteria;
import pals.plugins.handlers.defaultqch.java.CompilerResult;

/**
 * Handles code text-metrics criteria marking.
 * 
 * Partly based on ideas by Rees 1982 (Automatic Assessment Aids for Pascal
 * Programs). Marks are distributed between low, lotol, hitol and hi; these are
 * thresholds where marks are scaled between low to lo-tol or hi-tol to hi from
 * 0 to full-marks, with any value between lotol and hitol receiving full-marks.
 */
public class JavaCodeMetrics
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("5855d34f-cf37-435f-bddd-0dbfcea72fd7");
    public static final String  TITLE = "Java: Code Metrics";
    public static final String  DESCRIPTION = "Performs static analysis on code using metrics.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load cdata
        JavaCodeMetrics_Criteria cdata = (JavaCodeMetrics_Criteria)qc.getData();
        if(cdata == null)
            cdata = new JavaCodeMetrics_Criteria();
        // Check for postback
        boolean error = false;
        RemoteRequest req = data.getRequestData();
        String  critTitle       = req.getField("crit_title"),
                critWeight      = req.getField("crit_weight"),
                critType        = req.getField("crit_type"),
                critClasses     = req.getField("crit_classes"),
                critLo          = req.getField("crit_lo"),
                critLotol       = req.getField("crit_lotol"),
                critHi          = req.getField("crit_hi"),
                critHitol       = req.getField("crit_hitol");
        if(critTitle != null && critWeight != null && critClasses != null && critLo != null && critLotol != null && critHi != null && critHitol != null)
            pageCriteriaEdit_postback(data, qc, cdata, critTitle, critWeight, critType, critClasses, critLo, critLotol, critHitol, critHi);
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/javacodemetrics_edit");
        // -- Fields
        data.setTemplateData("types", JavaCodeMetrics_Criteria.MetricType.getModels());
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        try
        {
            data.setTemplateData("crit_type", critType != null ? Double.parseDouble(critType) : cdata.getType().getFormValue());
        }
        catch(NumberFormatException ex0)
        {
            data.setTemplateData("crit_type", cdata.getType().getFormValue());
        }
        data.setTemplateData("crit_classes", critClasses != null ? critClasses : cdata.getClassesWeb());
        data.setTemplateData("crit_lo", critLo != null ? critLo : cdata.getLo());
        data.setTemplateData("crit_lotol", critLotol != null ? critLotol : cdata.getLotol());
        data.setTemplateData("crit_hi", critHi != null ? critHi : cdata.getHi());
        data.setTemplateData("crit_hitol", critHitol != null ? critHitol : cdata.getHitol());
        return true;
    }
    private static void pageCriteriaEdit_postback(WebRequestData data, QuestionCriteria qc, JavaCodeMetrics_Criteria cdata, String critTitle, String critWeight, String critType, String critClasses, String critLo, String critLotol, String critHitol, String critHi)
    {
        // Update type
        if(!cdata.setType(critType))
        {
            data.setTemplateData("error", "Invalid metric type.");
            return;
        }
        // Update classes
        cdata.setClasses(critClasses);
        // Parse lo,lotol,hi,hitol
        double lo, lotol, hi, hitol;
        try
        {
            lo = Double.parseDouble(critLo);
        }
        catch(NumberFormatException ex)
        {
            data.setTemplateData("error", "Invalid lo value; must be numeric.");
            return;
        }
        try
        {
            lotol = Double.parseDouble(critLotol);
        }
        catch(NumberFormatException ex)
        {
            data.setTemplateData("error", "Invalid lotol value; must be numeric.");
            return;
        }
        try
        {
            hi = Double.parseDouble(critHi);
        }
        catch(NumberFormatException ex)
        {
            data.setTemplateData("error", "Invalid hi value; must be numeric.");
            return;
        }
        try
        {
            hitol = Double.parseDouble(critHitol);
        }
        catch(NumberFormatException ex)
        {
            data.setTemplateData("error", "Invalid hitol value; must be numeric.");
            return;
        }
        // Update thresholds
        switch(cdata.setThresholds(lo, lotol, hitol, hi))
        {
            case InvalidOrder:
                data.setTemplateData("error", "Invalid order; hi must be greater than hitol, hitol must be greater than lotol and lotol must be greater than lo.");
                return;
            case InvalidMustBeRatio:
                data.setTemplateData("error", "Invalid values for metric type; the selected type is a ratio, therefore the value must be between 0.0 to 1.0.");
                return;
        }
        // Handle entire process
        CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
            iac.setMark(0);
        else
        {
            CodeJava_Instance           idata = (CodeJava_Instance)iac.getIAQ().getData();
            JavaCodeMetrics_Criteria    cdata = (JavaCodeMetrics_Criteria)iac.getQC().getData();
            if(idata.getStatus() != CompilerResult.CompileStatus.Success)
                iac.setMark(0);
            else
            {
                // Setup the metric
                Metric m;
                switch(cdata.getType())
                {
                    case AverageLengthIdentifiersClasses:
                    case AverageLengthIdentifiersFields:
                    case AverageLengthIdentifiersMethods:
                        m = new AverageIdentifiers();
                        break;
                    case BlankLines:
                    case BlankLines_Ratio:
                        m = new BlankLines();
                        break;
                    case CommentLines:
                    case CommentLines_Ratio:
                        m = new CommentLines();
                        break;
                    case LinesOfCode:
                    case LinesOfCode_Ratio:
                        m = new LinesOfCode();
                        break;
                    default:
                        core.getLogging().log("JavaCode Metrics", "Unhandled metric type '"+cdata.getType().name()+"'.", Logging.EntryType.Info);
                        return false;
                }
                // Init metric
                m.init(core, iac, idata, cdata);
                // Process code-files
                String[] classes = cdata.getClasses();
                if(classes == null || classes.length == 0)
                {
                    // Process all files
                    for(String c : idata.getCodeNames())
                        m.process(idata, cdata, c);
                }
                else
                {
                    // Process selected classes
                    for(String c : classes)
                        m.process(idata, cdata, c);
                }
                // Compute and update mark
                double  metric = m.metricComputeValue(idata, cdata, iac);
                int     mark = mark(cdata.getLo(), cdata.getLotol(), cdata.getHitol(), cdata.getHi(), metric);
                iac.setMark(mark);
                // Dispose
                m.dispose(idata, cdata);
            }
        }
        iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        return false;
    }
    // Methods - Marking *******************************************************
    private static int mark(double lo, double lotol, double hitol, double hi, double value)
    {
        // Check the value is within range
        if(value <= lo || value >= hi)
            return 0;
        // Check lo-lotol range
        if(value > lo && value < lotol)
        {
            double t = value-lo; // DBZ percaution
            return t <= 0.0 ? 0 : (int)( 100.0*((value-lo)/(lotol-lo)) );
        }
        // Check hi-hitol range
        if(value > hitol && value < hi)
        {
            double t = value-hitol; // DBZ percaution
            return t <= 0.0 ? 0 : (int)( 100.0*(1.0-((value-hitol)/(hi-hitol))) );
        }
        // Check lotol-hitol range
        if(value >= lotol && value <= hitol)
            return 100;
        return 0;
    }
}
