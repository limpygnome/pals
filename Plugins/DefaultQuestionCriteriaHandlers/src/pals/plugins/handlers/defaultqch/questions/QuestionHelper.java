package pals.plugins.handlers.defaultqch.questions;

import java.io.File;
import java.io.Serializable;
import pals.base.Storage;
import pals.base.assessment.Question;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;

/**
 * A helper class for question classes, intended to reduce repetitive code.
 */
public class QuestionHelper
{
    protected static <T extends Serializable> void handle_questionEditPostback(WebRequestData data, Question q, String title, String description, T qdata)
    {
        // Validate request
        if(!CSRF.isSecure(data))
            data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
        else
        {
            // Update model
            q.setTitle(title);
            q.setDescription(description);
            // Persist the model
            q.setData(qdata);
            Question.PersistStatus psq = q.persist(data.getConnector());
            switch(psq)
            {
                default:
                    data.setTemplateData("error", psq.getText(q));
                    break;
                case Success:
                    data.setTemplateData("success", psq.getText(q));
                    break;
            }
        }
    }
}
