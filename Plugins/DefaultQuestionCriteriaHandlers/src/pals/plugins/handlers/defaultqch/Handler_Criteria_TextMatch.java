package pals.plugins.handlers.defaultqch;

import pals.base.UUID;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.QuestionCriteria;
import pals.base.database.Connector;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;

/**
 * Handles the text-matching criteria.
 */
public class Handler_Criteria_TextMatch
{
    // Constants ***************************************************************
    public static final UUID UUID_CTYPE = UUID.parse("b9a1143c-98cb-446b-9b39-42addac71f4f");
    // Methods *****************************************************************
    static boolean pageCriteriaEdit_textMatch(WebRequestData data, QuestionCriteria qc)
    {
        // Load criteria data
        Data_Criteria_TextMatch cdata;
        if(qc.getData() != null)
            cdata = (Data_Criteria_TextMatch)qc.getData();
        else
            cdata = new Data_Criteria_TextMatch();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        String critMatch = req.getField("crit_match");
        // -- Optional
        String critSensitive = req.getField("crit_sensitive");
        if(critTitle != null && critWeight != null && critMatch != null)
        {
            // Validate security
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                try
                {
                    int weight = Integer.parseInt(critWeight);
                    // Update the data model
                    cdata.setText(critMatch);
                    cdata.setCaseSensitive(critSensitive != null && critSensitive.equals("1"));
                    // Update qc model
                    qc.setData(cdata);
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
        data.setTemplateData("pals_content", "defaultqch/criteria/textmatch_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_match", critMatch != null ? critMatch : cdata.getText());
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        if((critMatch == null && cdata.isCaseSensitive()) || (critSensitive != null && critSensitive.equals("1")))
            data.setTemplateData("crit_sensitive", cdata.isCaseSensitive());
        
        return true;
    }
    static boolean criteriaMarking_textMatch(Connector conn, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
        {
            iac.setMark(0);
            return true;
        }
        else
        {
            UUID qtype = iac.getIAQ().getAssignmentQuestion().getQuestion().getQtype().getUuidQType();
            Data_Criteria_TextMatch cdata = (Data_Criteria_TextMatch)iac.getQC().getData();
            String match = cdata.isCaseSensitive() ? cdata.getText() : cdata.getText().toLowerCase();
            boolean matched = false;
            if(qtype.equals(Handler_Question_WrittenResponse.UUID_QTYPE))
            {
                
                String text = (String)iac.getIAQ().getData();
                if(!cdata.isCaseSensitive())
                    text = text.toLowerCase();
                matched = text.equals(match);
            }
            else if(qtype.equals(Handler_Question_MCQ.UUID_QTYPE))
            {
                Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iac.getQC().getQuestion().getData();
                Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iac.getIAQ().getData();
                String[] text = adata.getAnswers(qdata);
                for(String t : text)
                {
                    if((cdata.isCaseSensitive() ? t : t.toLowerCase()).equals(match))
                    {
                        matched = true;
                        break;
                    }
                }
            }
            else
                return false;
            // Update and persist the mark
            iac.setMark(matched ? 100 : 0);
            iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
            return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
        }
    }
}
