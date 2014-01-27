package pals.plugins.handlers.defaultqch.criterias;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import pals.base.NodeCore;
import pals.base.Storage;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.QuestionCriteria;
import pals.base.database.Connector;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.data.ClassExists_Criteria;

/**
 * Handles java class existence criteria marking.
 */
public class JavaClassExists
{
    // Constants ***************************************************************
    public static final UUID UUID_CTYPE = UUID.parse("0ce02a08-9d6d-4d1d-bd8d-536d60fc1b65");
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load criteria data
        ClassExists_Criteria cdata = (ClassExists_Criteria)qc.getData();
        if(cdata == null)
            cdata = new ClassExists_Criteria();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        String critClassName = req.getField("crit_class_name");
        if(critClassName != null)
        {
            // Update data-model
            cdata.setClassName(critClassName);
            // Handle entire process
            CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/javaclassexists_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        data.setTemplateData("crit_class_name", critClassName != null ? critClassName : cdata.getClassName());
        return true;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
        {
            iac.setMark(0);
            iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        }
        else
        {
            // Load criteria-data
            ClassExists_Criteria cdata = (ClassExists_Criteria)iac.getQC().getData();
            // Fetch path for assignment
            String path = Storage.getPath_tempIAQ(core.getPathShared(), iac.getIAQ());
            // Check the path exists
            File f = new File(path);
            if(f.exists())
            {
                try
                {
                    // Create class-loader at the path
                    URLClassLoader cl = new URLClassLoader(new URL[]{f.toURI().toURL()});
                    // Check if the class exists
                    boolean exists;
                    try
                    {
                        cl.loadClass(cdata.getClassName());
                        exists = true;
                    }
                    catch(ClassNotFoundException ex)
                    {
                        exists = false;
                    }
                    // Update model
                    iac.setMark(exists ? 100 : 0);
                    iac.setData(exists);
                    iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
                }
                catch(MalformedURLException ex)
                {
                    iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                }
            }
        }
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        Object fdata = iac.getData();
        ClassExists_Criteria cdata = (ClassExists_Criteria)iac.getQC().getData();
        if(fdata != null && (fdata instanceof Boolean) && cdata != null)
        {
            boolean exists = (Boolean)fdata;
            HashMap<String,Object> kvs = new HashMap<>();
            kvs.put(exists ? "success" : "error", exists ? "Class '"+cdata.getClassName()+"' found." : "Class '"+cdata.getClassName()+"' not found.");
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/feedback_display"));
            return true;
        }
        return false;
    }
}
