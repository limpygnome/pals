package pals.plugins.handlers.defaultqch;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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

/**
 * Handles the regex-matching criteria.
 */
public class Handler_Criteria_RegexMatch
{
    // Constants ***************************************************************
    public static final UUID UUID_CTYPE = UUID.parse("3e6518e8-bb13-4878-bb4c-c0d687ad2e6e");
    // Methods *****************************************************************
    static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load criteria data
        Data_Criteria_Regex cdata;
        if(qc.getData() != null)
            cdata = (Data_Criteria_Regex)qc.getData();
        else
            cdata = new Data_Criteria_Regex();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        String critRegex = req.getField("crit_regex");
        // -- Optional
        String critMultiline = req.getField("crit_multiline");
        String critCase = req.getField("crit_case");
        String critDotall = req.getField("crit_dotall");
        if(critRegex != null && critTitle != null && critWeight != null)
        {
            try
            {
                int weight = Integer.parseInt(critWeight);
                // Validate security
                if(!CSRF.isSecure(data))
                    data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
                else
                {
                    // Test compiling the regex pattern
                    // -- Exception is caught by try-catch
                    Pattern.compile(critRegex);
                    // Update data model
                    cdata.setMode(
                            (critMultiline != null && critMultiline.equals("1") ? Pattern.MULTILINE : 0) | (critCase != null && critCase.equals("1") ? Pattern.CASE_INSENSITIVE : 0) | (critDotall != null && critDotall.equals("1") ? Pattern.DOTALL : 0)
                    );
                    cdata.setRegexPattern(critRegex);
                    // Update question criteria model
                    qc.setData(cdata);
                    qc.setWeight(weight);
                    qc.setTitle(critTitle);
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
            }
            catch(NumberFormatException ex)
            {
                data.setTemplateData("error", "Weight must be numeric!");
            }
            catch(PatternSyntaxException ex)
            {
                data.setTemplateData("error", "Regex pattern cannot compile: '"+ex.getMessage()+"'!");
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/regex_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_regex", critRegex != null ? critRegex : cdata.getRegexPattern());
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        
        // -- Note: critRegex is used to test for postback; if a box was previously selected, unselecting and posting-back may cause it to change state
        if( (critRegex == null && ((cdata.getMode() & Pattern.MULTILINE) == Pattern.MULTILINE)) || (critMultiline != null && critMultiline.equals("1")))
            data.setTemplateData("crit_multiline", data);
        if( (critRegex == null && ((cdata.getMode() & Pattern.CASE_INSENSITIVE) == Pattern.CASE_INSENSITIVE)) || (critCase != null && critCase.contains("1")))
            data.setTemplateData("crit_case", data);
        if( (critRegex == null && ((cdata.getMode() & Pattern.DOTALL) == Pattern.DOTALL)) || (critDotall != null && critDotall.equals("1")))
            data.setTemplateData("crit_dotall", data);
        
        return true;
    }
    static boolean criteriaMarking(NodeCore core, Connector conn, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
            iac.setMark(0);
        else
        {
            Data_Criteria_Regex cdata = (Data_Criteria_Regex)iac.getQC().getData();
            // Compile regex pattern
            Pattern p;
            try
            {
                p = Pattern.compile(cdata.getRegexPattern(), cdata.getMode());
            }
            catch(PatternSyntaxException ex)
            {
                core.getLogging().logEx("Default QC", "Could not compile regex pattern for question-criteria '"+iac.getQC().getQCID()+"' (QCID).", ex, Logging.EntryType.Warning);
                iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
            }
            // Perform matching
            UUID qtype = iac.getIAQ().getAssignmentQuestion().getQuestion().getQtype().getUuidQType();
            boolean matched = false;
            if(qtype.equals(Handler_Question_MCQ.UUID_QTYPE))
            {
                Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iac.getQC().getQuestion().getData();
                Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iac.getIAQ().getData();
                String[] text = adata.getAnswers(qdata);
                for(String t : text)
                {
                    if(p.matcher(t).matches())
                    {
                        matched = true;
                        break;
                    }
                }
            }
            else if(qtype.equals(Handler_Question_WrittenResponse.UUID_QTYPE))
            {
                String text = (String)iac.getIAQ().getData();
                matched = p.matcher(text).matches();
            }
            else
                return false;
            // Update and persist the mark
            iac.setMark(matched ? 100 : 0);
            iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
            iac.setData(matched);
        }
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        Object fdata = iac.getData();
        Data_Criteria_Regex cdata = (Data_Criteria_Regex)iac.getQC().getData();
        if(fdata != null && (fdata instanceof Boolean) && cdata != null)
        {
            boolean matched = (Boolean)fdata;
            HashMap<String,Object> kvs = new HashMap<>();
            kvs.put(matched ? "success" : "error", matched ? "Correct answer." : "Your answer was not matched by the regular-expressions pattern '"+cdata.getRegexPattern()+"'.");
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/feedback_display"));
            return true;
        }
        return false;
    }
}
