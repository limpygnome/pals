package pals.plugins.handlers.defaultqch;

import java.util.ArrayList;
import java.util.HashMap;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.QuestionCriteria;
import pals.base.database.Connector;
import pals.base.utils.Misc;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;

/**
 * Handles a multiple-choice criteria.
 */
public class Handler_Criteria_MultipleChoice
{
    // Constants ***************************************************************
    public static final UUID UUID_CTYPE = UUID.parse("a45bf985-5b0e-4643-bce7-d55b9e5f24f8");
    // Methods *****************************************************************
    static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load question data
        Data_Question_MultipleChoice qdata;
        if(qc.getQuestion().getData() != null)
            qdata = (Data_Question_MultipleChoice)qc.getQuestion().getData();
        else
            qdata = null;
        // Load criteria data
        Data_Criteria_MultipleChoice cdata;
        if(qc.getData() != null)
            cdata = (Data_Criteria_MultipleChoice)qc.getData();
        else
            cdata = new Data_Criteria_MultipleChoice();
        // Fetch the possible answers
        String[] answers;
        if(qdata != null)
            answers = qdata.getAnswers();
        else
        {
            answers = new String[0];
            data.setTemplateData("error", "The question's multiple-choice data has not been defined!");
        }
        int totalAnswers = answers.length;
        boolean[] itemsSelected = new boolean[totalAnswers];
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        if(qdata != null && critTitle != null && critWeight != null)
        {
            // Validate security
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                int weight;
                try
                {
                    weight = Integer.parseInt(critWeight);
                }
                catch(NumberFormatException ex)
                {
                    weight = -1;
                }
                // Fetch the items selected
                ArrayList<Integer> buffer = new ArrayList<>();
                for(int i = 0; i < totalAnswers; i++)
                {
                    if(req.getField("crit_index_"+i) != null)
                    {
                        buffer.add(i);
                        itemsSelected[i] = true;
                    }
                }
                // Update the data model
                cdata.setIndexesCorrect(buffer.toArray(new Integer[buffer.size()]));
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
        }
        else
        {
            Integer[] inds = cdata.getIndexesCorrect();
            // Update the items selected previously
            // -- Potentially O(n^2), however we want to reduce storage on the database
            // -- since a MCQ could have a lot of items; a large array, instead, would be a lot more wasteful.
            // -- -- This will only occur when editing or marking the criteria, both rare events.
            for(int i = 0; i < totalAnswers; i++)
                itemsSelected[i] = Misc.arrayContains(inds, i);
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/multiplechoice_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        data.setTemplateData("crit_items_selected", itemsSelected);
        data.setTemplateData("crit_items_total", totalAnswers);
        data.setTemplateData("crit_items", qdata != null ? qdata.getAnswers() : new String[0]);
        return true;
    }
    static boolean criteriaMarking(Connector conn, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
            iac.setMark(0);
        else
        {
            UUID qtype = iac.getIAQ().getAssignmentQuestion().getQuestion().getQtype().getUuidQType();
            if(qtype == null || !qtype.equals(Handler_Question_MCQ.UUID_QTYPE))
                return false;
            // Load question, answer and criteria data
            Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iac.getQC().getQuestion().getData();
            Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iac.getIAQ().getData();
            Data_Criteria_MultipleChoice cdata = (Data_Criteria_MultipleChoice)iac.getQC().getData();
            // Check the indexes selected match the correct indexes
            boolean correct = true;
            Integer[] answers = adata.getAnswers();
            Integer[] correctAnswers = cdata.getIndexesCorrect();
            if(answers.length == correctAnswers.length)
            {
                for(int i = 0; i < answers.length; i++)
                {
                    if(!answers[i].equals(correctAnswers[i]))
                    {
                        correct = false;
                        break;
                    }
                }
            }
            else
                correct = false;
            // Update and persist the mark
            iac.setMark(correct ? 100 : 0);
            iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
            iac.setData(correct);
        }
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        Object fdata = iac.getData();
        Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iac.getQC().getQuestion().getData();
        Data_Criteria_MultipleChoice cdata = (Data_Criteria_MultipleChoice)iac.getQC().getData();
        if(fdata != null && (fdata instanceof Boolean) && qdata != null && cdata != null)
        {
            boolean correct = (Boolean)fdata;
            HashMap<String,Object> kvs = new HashMap<>();
            if(correct)
                kvs.put("success", "Correct answer.");
            else
            {
                kvs.put("error_correct", cdata.getCorrect(qdata));
            }
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/mcq_display"));
            return true;
        }
        return false;
    }
}
