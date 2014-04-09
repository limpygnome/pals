/*
    The MIT License (MIT)

    Copyright (c) 2014 Marcus Craske <limpygnome@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    ----------------------------------------------------------------------------
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.plugins.handlers.defaultqch.criterias;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
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
import pals.plugins.handlers.defaultqch.data.JavaInheritance_Criteria;
import pals.plugins.handlers.defaultqch.data.JavaInheritance_InstanceCriteria;

/**
 * Handles Java class existence criteria marking.
 */
public class JavaInheritance
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("8b1bbb7f-af00-40de-bf7b-195ece42693d");
    public static final String  TITLE = "Java: Class Inheritance & Interfaces";
    public static final String  DESCRIPTION = "Checks the inheritance and implemented interfaces of a class.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load criteria data
        JavaInheritance_Criteria cdata = (JavaInheritance_Criteria)qc.getData();
        if(cdata == null)
            cdata = new JavaInheritance_Criteria();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String  critTitle               = req.getField("crit_title"),
                critWeight              = req.getField("crit_weight"),
                critClassName           = req.getField("crit_class_name"),
                critInheritedClassName  = req.getField("crit_inherited_class_name"),
                critInheritedGeneric    = req.getField("crit_inherited_generic"),
                critInterfaces          = req.getField("crit_interfaces");
        // Process postback
        if(critTitle != null && critWeight != null && critClassName != null)
        {
            // Validate
            if(critClassName.length() == 0)
                data.setTemplateData("error", "Invalid class-name.");
            else if((critInheritedClassName == null || critInheritedClassName.length() == 0) && (critInterfaces == null || critInterfaces.length() == 0))
                data.setTemplateData("error", "You must specify either a class to be extended or a set of interfaces to be implemented.");
            else
            {
                // Update fields of model
                cdata.setClassName(critClassName);
                cdata.setInheritedClassName(critInheritedClassName);
                cdata.setInheritedGenericType(critInheritedGeneric);
                cdata.setInterfaces(critInterfaces.replace("\r", "").split("\n"));
                // Handle entire process
                CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/javainheritance_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        data.setTemplateData("crit_class_name", critClassName != null ? critClassName : cdata.getClassName());
        data.setTemplateData("crit_inherited_class_name", critInheritedClassName != null ? critInheritedClassName : cdata.getInheritedClassName());
        data.setTemplateData("crit_inherited_generic", critInheritedGeneric != null ? critInheritedGeneric : cdata.getInheritedGenericType());
        data.setTemplateData("crit_interfaces", critInterfaces != null ? critInterfaces : cdata.getInterfacesWeb());
        return true;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        // Load criteria data
        JavaInheritance_Criteria cdata = (JavaInheritance_Criteria)iac.getQC().getData();
        if(cdata == null)
        {
            iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
            return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
        }
        // Load path with class files into URL loader, find desired class, inspect
        JavaInheritance_InstanceCriteria icdata = new JavaInheritance_InstanceCriteria();
        String path = Storage.getPath_tempIAQ(core.getPathShared(), iac.getIAQ());
        File f = new File(path);
        // -- Check class files exist
        if(f.exists())
        {
            try
            {
                // Create loader for class-files
                URLClassLoader cl = new URLClassLoader(new URL[]{f.toURI().toURL()});
                // Fetch desired class
                Class c = cl.loadClass(cdata.getClassName());
                if(c != null)
                {
                    // Mark extended class
                    if(cdata.isInheritedClassNameConsidered())
                    {
                        boolean correct = true;
                        // Check super-class
                        if(!c.getSuperclass().getName().equals(cdata.getInheritedClassName()))
                            correct = false;
                        // Check generic type
                        else if(cdata.isInheritedGenericTypeConsidered())
                        {
                            // Fetch parametrized type of class
                            ParameterizedType pt = (ParameterizedType)c.getGenericSuperclass();
                            // Check it has arguments
                            if(pt.getActualTypeArguments().length == 0)
                                correct = false;
                            // Check the type is correct
                            // -- Convert generic type argument to the class
                            else if(!((Class<?>)pt.getActualTypeArguments()[0]).getName().equals(cdata.getInheritedGenericType()))
                                correct = false;
                        }
                        icdata.setStatus(correct ? JavaInheritance_InstanceCriteria.Status.Correct : JavaInheritance_InstanceCriteria.Status.IncorrectExtend);
                    }
                    else
                        icdata.setStatus(JavaInheritance_InstanceCriteria.Status.Correct);
                    // Mark interfaces implemented
                    if(cdata.isInterfaces())
                    {
                        Class[] interfaces = c.getInterfaces();
                        ArrayList<String> interfacesMissing = new ArrayList<>();
                        boolean found = false;
                        for(String i : cdata.getInterfaces())
                        {
                            found = false;
                            // Search interfaces for required interface
                            for(Class i2 : interfaces)
                            {
                                if(i2.getName().equals(i))
                                {
                                    found = true;
                                    break;
                                }
                            }
                            // Log if not found
                            if(!found)
                                interfacesMissing.add(i);
                        }
                        // Set the interfaces not implemented
                        icdata.setClassesNotImplemented(interfacesMissing.toArray(new String[interfacesMissing.size()]));
                    }
                    // Compute mark
                    int totalInterfaces = cdata.getInterfaces().length;
                    if(cdata.isInterfaces() && cdata.isInheritedClassNameConsidered())
                        iac.setMark(
                                (int)
                                (
                                    (icdata.getStatus() == JavaInheritance_InstanceCriteria.Status.Correct ? 50.0 : 0.0) +
                                    (
                                        ((double)(totalInterfaces-icdata.getClassesNotImplemented().length) / (double)totalInterfaces)*50.0
                                    )
                                )
                        );
                    else if(cdata.isInterfaces())
                        iac.setMark(
                                (int)(((double)(totalInterfaces-icdata.getClassesNotImplemented().length)/(double)totalInterfaces)*100.0)
                        );
                    else if(icdata.getStatus() == JavaInheritance_InstanceCriteria.Status.Correct)
                        iac.setMark(100);
                    else
                        iac.setMark(0);
                }
                else
                    iac.setMark(0);
            }
            catch(Exception ex) // Broad, just in-case...
            {
                // Set to manual marking...
                core.getLogging().logEx(DefaultQC.LOGGING_ALIAS, "IAC ~ aiqid "+iac.getIAQ().getAIQID()+", qcid "+iac.getQC().getQCID(), ex, Logging.EntryType.Warning);
                iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
            }
        }
        else
            iac.setMark(0);
        // Persist mark
        iac.setData(icdata);
        iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        // Load criteria data
        JavaInheritance_Criteria cdata = (JavaInheritance_Criteria)iac.getQC().getData();
        // Load instance data
        JavaInheritance_InstanceCriteria icdata = (JavaInheritance_InstanceCriteria)iac.getData();
        if(cdata == null || icdata == null)
            return false;
        // Prepare to render template
        HashMap<String,Object> kvs = new HashMap<>();
        if(icdata.getStatus() == JavaInheritance_InstanceCriteria.Status.ClassNotFound)
            kvs.put("error", "Class '"+cdata.getClassName()+"' could not be found.");
        else
        {
            // Output status of extended class
            switch(icdata.getStatus())
            {
                case Correct:
                    kvs.put("success", "Class '"+cdata.getClassName()+"' correctly extends '"+cdata.getFullExtendedClassName()+"'.");
                    break;
                case IncorrectExtend:
                    kvs.put("error", "Class '"+cdata.getClassName()+"' does not extend class '"+cdata.getFullExtendedClassName()+"'.");
                    if(cdata.isInheritedGenericTypeConsidered())
                        kvs.put("warning", "The extended class must have the generic type '"+cdata.getInheritedGenericType()+"', ensure you have specified it correctly.");
                    break;
            }
            // Output status of interfaces
            if(cdata.isInterfaces())
            {
                String[] missedInterfaces = icdata.getClassesNotImplemented();
                if(missedInterfaces.length == 0)
                    kvs.put("success2", "You have correctly implemented all of the required interfaces.");
                else
                {
                    StringBuilder sb = new StringBuilder("You have not implemented the following interfaces:\n");
                    for(String i : missedInterfaces)
                        sb.append(i).append("\n");
                    sb.deleteCharAt(sb.length()-1);
                    kvs.put("error2", sb.toString());
                }
            }
        }
        // Render template
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/feedback_display"));
        return true;
    }
}
