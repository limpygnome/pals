package pals.plugins.handlers.defaultqch.criterias;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import pals.base.Logging;
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
import pals.plugins.handlers.defaultqch.DefaultQC;
import pals.plugins.handlers.defaultqch.data.ClassExists_Criteria;

/**
 * Handles java class existence criteria marking.
 */
public class JavaClassExists
{
    // Enums *******************************************************************
    public enum MarkingStatus
    {
        Incorrect_NotFound,
        Incorrect_Modifiers,
        Correct
    }
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
        String  critTitle =     req.getField("crit_title");
        String  critWeight =    req.getField("crit_weight");
        String  critClassName = req.getField("crit_class_name");
        String  critClassOnly = req.getField("crit_class_only");
        // -- Optional
        String  critMod =       req.getField("crit_mod");
        String  critAbstract =  req.getField("crit_mod_abstract"),
                critFinal =     req.getField("crit_mod_final"),
                critInterface = req.getField("crit_mod_interface"),
                critNative =    req.getField("crit_mod_native"),
                critPrivate =   req.getField("crit_mod_private"),
                critProtected = req.getField("crit_mod_protected"),
                critPublic =    req.getField("crit_mod_public"),
                critStatic =    req.getField("crit_mod_static"),
                critStrict =    req.getField("crit_mod_strict");
        if(critTitle != null && critWeight != null && critClassName != null && critClassOnly != null)
        {
            // Update data-model
            // -- Class-name
            cdata.setClassName(critClassName);
            // -- Modifiers
            int modifiers;
            if(critMod != null && critMod.equals("1"))
            {
                modifiers = 0;
                if(critAbstract != null && critAbstract.equals("1"))
                    modifiers |= Modifier.ABSTRACT;
                if(critFinal != null && critFinal.equals("1"))
                    modifiers |= Modifier.FINAL;
                if(critInterface != null && critInterface.equals("1"))
                    modifiers |= Modifier.INTERFACE;
                if(critNative != null && critNative.equals("1"))
                    modifiers |= Modifier.NATIVE;
                if(critPrivate != null && critPrivate.equals("1"))
                    modifiers |= Modifier.PRIVATE;
                if(critProtected != null && critProtected.equals("1"))
                    modifiers |= Modifier.PROTECTED;
                if(critPublic != null && critPublic.equals("1"))
                    modifiers |= Modifier.PUBLIC;
                if(critStatic != null && critStatic.equals("1"))
                    modifiers |= Modifier.STATIC;
                if(critStrict != null && critStrict.equals("1"))
                    modifiers |= Modifier.STRICT;
            }
            else
                modifiers = -1;
            cdata.setModifiers(modifiers);
            // -- Class-only mark
            int classOnly;
            try
            {
                classOnly = Integer.parseInt(critClassOnly);
                cdata.setMarkClassOnly(classOnly);
            }
            catch(NumberFormatException ex)
            {
                classOnly = -1;
            }
            if(classOnly < 0 || classOnly > 100)
                data.setTemplateData("error", "Invalid value for incorrect-modifiers-value; must be numeric and between 0 to 100.");
            else
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
        data.setTemplateData("crit_class_only", cdata.getMarkClassOnly());
        // -- -- Optional
        int tModifiers = cdata.getModifiers();
        data.setTemplateData("crit_mod", (critMod != null && critMod.equals("1"))                       || (critTitle == null && tModifiers != -1));
        data.setTemplateData("crit_mod_abstract", (critAbstract != null && critAbstract.equals("1"))    || (critTitle == null && tModifiers != -1 && (tModifiers & Modifier.ABSTRACT) == Modifier.ABSTRACT));
        data.setTemplateData("crit_mod_final", (critFinal != null && critFinal.equals("1"))             || (critTitle == null && tModifiers != -1 && (tModifiers & Modifier.FINAL) == Modifier.FINAL));
        data.setTemplateData("crit_mod_interface", (critInterface != null && critInterface.equals("1")) || (critTitle == null && tModifiers != -1 && (tModifiers & Modifier.INTERFACE) == Modifier.INTERFACE));
        data.setTemplateData("crit_mod_native", (critNative != null && critNative.equals("1"))          || (critTitle == null && tModifiers != -1 && (tModifiers & Modifier.NATIVE) == Modifier.NATIVE));
        data.setTemplateData("crit_mod_private", (critPrivate != null && critPrivate.equals("1"))       || (critTitle == null && tModifiers != -1 && (tModifiers & Modifier.PRIVATE) == Modifier.PRIVATE));
        data.setTemplateData("crit_mod_protected", (critProtected != null && critProtected.equals("1")) || (critTitle == null && tModifiers != -1 && (tModifiers & Modifier.PROTECTED) == Modifier.PROTECTED));
        data.setTemplateData("crit_mod_public", (critPublic != null && critPublic.equals("1"))          || (critTitle == null && tModifiers != -1 && (tModifiers & Modifier.PUBLIC) == Modifier.PUBLIC));
        data.setTemplateData("crit_mod_static", (critStatic != null && critStatic.equals("1"))          || (critTitle == null && tModifiers != -1 && (tModifiers & Modifier.STATIC) == Modifier.STATIC));
        data.setTemplateData("crit_mod_strict", (critStrict != null && critStrict.equals("1"))          || (critTitle == null && tModifiers != -1 && (tModifiers & Modifier.STRICT) == Modifier.STRICT));
        return true;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
            iac.setMark(0);
        else
        {
            // Load criteria-data
            ClassExists_Criteria cdata = (ClassExists_Criteria)iac.getQC().getData();
            if(cdata == null)
            {
                iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
            }
            // Fetch path for assignment
            String path = Storage.getPath_tempIAQ(core.getPathShared(), iac.getIAQ());
            // Check the path exists
            File f = new File(path);
            MarkingStatus ms = MarkingStatus.Incorrect_NotFound;
            if(f.exists())
            {
                try
                {
                    // Create class-loader at the path
                    URLClassLoader cl = new URLClassLoader(new URL[]{f.toURI().toURL()});
                    // Check if the class exists
                    try
                    {
                        int modifiers = cdata.getModifiers();
                        Class c = cl.loadClass(cdata.getClassName());
                        // Check modifiers
                        if(modifiers == -1 || modifiers == c.getModifiers())
                            ms = MarkingStatus.Correct;
                        else
                            ms = MarkingStatus.Incorrect_Modifiers;
                    }
                    catch(ClassNotFoundException ex)
                    {
                    }
                }
                catch(MalformedURLException ex)
                {
                    core.getLogging().logEx(DefaultQC.LOGGING_ALIAS, "IAC ~ aiqid "+iac.getIAQ().getAIQID()+", qcid "+iac.getQC().getQCID(), ex, Logging.EntryType.Warning);
                    iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                    return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
                }
            }
            // Update model
            switch(ms)
            {
                case Correct:
                    iac.setMark(100);
                    break;
                case Incorrect_Modifiers:
                    iac.setMark(cdata.getMarkClassOnly());
                    break;
                default:
                    iac.setMark(0);
                    break;
            }
            iac.setData(ms);
            iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        }
        iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        Object fdata = iac.getData();
        ClassExists_Criteria cdata = (ClassExists_Criteria)iac.getQC().getData();
        if(fdata != null && (fdata instanceof MarkingStatus) && cdata != null)
        {
            MarkingStatus status = (MarkingStatus)fdata;
            HashMap<String,Object> kvs = new HashMap<>();
            switch(status)
            {
                case Incorrect_NotFound:
                    kvs.put("error", "Class '"+cdata.getClassName()+"' not found.");
                    break;
                case Incorrect_Modifiers:
                    kvs.put("error", "Class '"+cdata.getClassName()+"' found with incorrect modifiers - expected: '"+Modifier.toString(cdata.getModifiers())+"'.");
                    break;
                case Correct:
                    if(cdata.getModifiers() == -1)
                        kvs.put("success", "Class '"+cdata.getClassName()+"' found.");
                    else
                        kvs.put("success", "Class '"+cdata.getClassName()+"' found with correct modifiers.");
                    break;
            }
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/feedback_display"));
            return true;
        }
        return false;
    }
}
