package pals.plugins.handlers.defaultqch.criterias;

import java.io.Serializable;
import pals.base.assessment.QuestionCriteria;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;

/**
 * A helper class for criteria classes, intended to reduce repetitive code.
 */
public abstract class CriteriaHelper
{
    protected static <T extends Serializable> void handle_criteriaEditPostback(WebRequestData data, QuestionCriteria qc, String critTitle, String critWeight, T cdata)
    {
        // Validate the request
        if(!CSRF.isSecure(data))
            data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
        else
        {
            // Parse weight
            int weight;
            try
            {
                weight = Integer.parseInt(critWeight);
            }
            catch(NumberFormatException ex)
            {
                weight = -1;
            }
            // Update model
            qc.setData(cdata);
            qc.setTitle(critTitle);
            qc.setWeight(weight);
            // Persist QC model
            QuestionCriteria.PersistStatus qcps = qc.persist(data.getConnector());
            switch(qcps)
            {
                default:
                    data.setTemplateData("error", qcps.getText(qc));
                    break;
                case Success:
                    data.setTemplateData("success", qcps.getText(qc));
                    break;
            }
        }
    }
}
