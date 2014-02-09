package pals.plugins.handlers.defaultqch.criterias;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Storage;
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
import pals.plugins.handlers.defaultqch.java.Utils;

/**
 * Common code used for checking the existence of entities within Java.
 */
public class JavaExistsShared
{
    // Enums *******************************************************************
    public enum CriteriaType
    {
        Class,
        Method
    }
    public enum MarkingStatus
    {
        Incorrect_NotFoundClass,
        Incorrect_NotFoundMethod,
        Incorrect_Modifiers,
        Incorrect_ReturnTypeMethod,
        Correct
    }
    // Methods - Shared ********************************************************
    static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc, CriteriaType ct)
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
        String  critMethod =    req.getField("crit_method");
        String  critMethodParams = req.getField("crit_method_params");
        String  critMethodRT =  req.getField("crit_method_rt");
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
                critStrict =    req.getField("crit_mod_strict"),
                critSynchronized = req.getField("crit_mod_sync");
        if(critTitle != null && critWeight != null && critClassName != null && critClassOnly != null && (ct != CriteriaType.Method || (critMethod != null && critMethodParams != null && critMethodRT != null)))
        {
            // Update data-model
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
                if(critSynchronized != null && critSynchronized.equals("1"))
                    modifiers |= Modifier.SYNCHRONIZED;
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
            // -- Class-name
            else if(!cdata.setClassName(critClassName))
                data.setTemplateData("error", "Invalid class-name.");
            // -- Method name
            else if(ct == CriteriaType.Method && !cdata.setMethod(critMethod))
                data.setTemplateData("error", "Invalid method name.");
            else
            {
                if(ct == CriteriaType.Method)
                {
                    cdata.setParameters(critMethodParams);
                    cdata.setReturnType(critMethodRT);
                    cdata.setCriteriaType(ct);
                }
                // Handle entire process
                CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/javaclassexists_edit");
        switch(ct)
        {
            case Class:
                data.setTemplateData("crit_mode", 1);
                break;
            case Method:
                data.setTemplateData("crit_mode", 2);
                break;
        }
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        data.setTemplateData("crit_class_name", critClassName != null ? critClassName : cdata.getClassName());
        data.setTemplateData("crit_class_only", cdata.getMarkClassOnly());
        if(ct == CriteriaType.Method)
        {
            data.setTemplateData("crit_method", critMethod != null ? critMethod : cdata.getMethod());
            data.setTemplateData("crit_method_params", critMethodParams != null ? critMethodParams : cdata.getParametersWeb());
            data.setTemplateData("crit_method_rt", critMethodRT != null ? critMethodRT : cdata.getReturnType());
        }
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
        data.setTemplateData("crit_mod_sync", (critSynchronized != null && critSynchronized.equals("1")) || (critTitle == null && tModifiers != -1 && (tModifiers & Modifier.SYNCHRONIZED) == Modifier.SYNCHRONIZED));
        return true;
    }
    static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac, CriteriaType ct)
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
            MarkingStatus ms = MarkingStatus.Incorrect_NotFoundClass;
            if(f.exists())
            {
                try
                {
                    // Create class-loader at the path
                    URLClassLoader cl = new URLClassLoader(new URL[]{f.toURI().toURL()});
                    // Check if the class exists
                    try
                    {
                        // Handle type of matching even further
                        int modifiers = cdata.getModifiers();
                        Class c = cl.loadClass(cdata.getClassName());
                        switch(cdata.getCriteriaType())
                        {
                            default:
                                iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                                return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
                            case Class:
                                // Check modifiers
                                ms = modifiers == -1 || c.getModifiers() == modifiers ? MarkingStatus.Correct : MarkingStatus.Incorrect_Modifiers;
                                break;
                            case Method:
                                // Build params
                                String[] tparams = cdata.getParameters();
                                String trt = cdata.getReturnType();
                                Class[] params = new Class[tparams.length];
                                for(int i = 0; i < tparams.length; i++)
                                {
                                    if((params[i] = Utils.parseClass(tparams[i], cl)) == null)
                                    {
                                        // We have failed to parse a class for a question
                                        core.getLogging().log(DefaultQC.LOGGING_ALIAS, "Could not parse param class '"+tparams[i]+"' during marking; aiqid '"+iac.getIAQ().getAIQID()+"', qcid '"+iac.getQC().getQCID()+"'.", Logging.EntryType.Warning);
                                        iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                                        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
                                    }
                                }
                                // Build return-type
                                Class rt;
                                if(trt == null || trt.length() == 0)
                                    rt = null;
                                else if((rt = Utils.parseClass(trt, cl)) == null)
                                {
                                    core.getLogging().log(DefaultQC.LOGGING_ALIAS, "Could not parse return-type class '"+trt+"' during marking; aiqid '"+iac.getIAQ().getAIQID()+"', qcid '"+iac.getQC().getQCID()+"'.", Logging.EntryType.Warning);
                                    iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                                    return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
                                }
                                // Locate the method
                                try
                                {
                                    Method m = c.getDeclaredMethod(cdata.getMethod(), params);
                                    if((rt == null && m.getReturnType().equals(Void.TYPE)) || m.getReturnType().equals(rt))
                                    {
                                        // Check modifiers
                                        ms = modifiers == -1 || m.getModifiers() == modifiers ? MarkingStatus.Correct : MarkingStatus.Incorrect_Modifiers;
                                    }
                                    else
                                        ms = MarkingStatus.Incorrect_ReturnTypeMethod;
                                }
                                catch(NoSuchMethodException | SecurityException ex)
                                {
                                    ms = MarkingStatus.Incorrect_NotFoundMethod;
                                }
                                break;
                        }
                    }
                    catch(ClassNotFoundException ex) { }
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
    static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html, CriteriaType ct)
    {
        Object fdata = iac.getData();
        ClassExists_Criteria cdata = (ClassExists_Criteria)iac.getQC().getData();
        if(fdata != null && (fdata instanceof MarkingStatus) && cdata != null)
        {
            MarkingStatus status = (MarkingStatus)fdata;
            HashMap<String,Object> kvs = new HashMap<>();
            switch(status)
            {
                case Incorrect_NotFoundClass:
                    kvs.put("error", "Class '"+cdata.getClassName()+"' not found.");
                    break;
                case Incorrect_NotFoundMethod:
                    kvs.put("error", "Method '"+cdata.getMethod()+"', in class '"+cdata.getClassName()+"', not found.");
                    break;
                case Incorrect_Modifiers:
                    switch(cdata.getCriteriaType())
                    {
                        case Class:
                            kvs.put("error", "Class '"+cdata.getClassName()+"' found with incorrect modifiers - expected: '"+Modifier.toString(cdata.getModifiers())+"'.");
                            break;
                        case Method:
                            kvs.put("error", "Method '"+cdata.getMethod()+"', in class '"+cdata.getClassName()+"', found with incorrect modifiers - expected: '"+Modifier.toString(cdata.getModifiers())+"'.");
                            break;
                    }
                    break;
                case Correct:
                    switch(cdata.getCriteriaType())
                    {
                        case Class:
                            kvs.put("success", cdata.getModifiers() == -1 ? "Class '"+cdata.getClassName()+"' found." : "Class '"+cdata.getClassName()+"' found with correct modifiers.");
                            break;
                        case Method:
                            kvs.put("success", cdata.getModifiers() == -1 ? "Method '"+cdata.getMethod()+"', in class '"+cdata.getClassName()+"', found." : "Method '"+cdata.getClassName()+"', in class '"+cdata.getClassName()+"', found with correct modifiers.");
                            break;
                    }
                    break;
            }
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/feedback_display"));
            return true;
        }
        return false;
    }
}
