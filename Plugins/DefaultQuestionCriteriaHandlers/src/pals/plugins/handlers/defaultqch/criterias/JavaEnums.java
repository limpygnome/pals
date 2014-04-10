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
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.plugins.handlers.defaultqch.criterias;

import java.io.File;
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
import pals.plugins.handlers.defaultqch.data.JavaEnums_Criteria;
import pals.plugins.handlers.defaultqch.data.JavaEnums_InstanceCriteria;

/**
 * Handles Java class existence criteria marking.
 * 
 * @version 1.0
 */
public class JavaEnums
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("8ce5bca4-2309-4877-af64-286d49acbc2c");
    public static final String  TITLE = "Java: Enum Exists";
    public static final String  DESCRIPTION = "Checks an enum exists with specified values.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load criteria data
        JavaEnums_Criteria cdata = (JavaEnums_Criteria)qc.getData();
        if(cdata == null)
            cdata = new JavaEnums_Criteria();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String  critTitle       = req.getField("crit_title"),
                critWeight      = req.getField("crit_weight"),
                critClassName   = req.getField("crit_class_name"),
                critValues      = req.getField("crit_values");
        // -- Optional
        String  critCaseSensitive   = req.getField("crit_case_sensitive"),
                critAllowExtra      = req.getField("crit_allow_extra");
        // Process postback
        if(critTitle != null && critWeight != null && critClassName != null && critValues != null)
        {
            // Validate and set fields
            if(!cdata.setClassName(critClassName))
                data.setTemplateData("error", "Invalid class-name.");
            else if(!cdata.setValues(critValues.replace("\r", "").split("\n")))
                data.setTemplateData("error", "Invalid values.");
            else
            {
                // Update fields of model
                cdata.setCaseSensitive(critCaseSensitive != null && critCaseSensitive.equals("1"));
                cdata.setAllowExtraValues(critAllowExtra != null && critAllowExtra.equals("1"));
                // Handle entire process
                CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/javaenum_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        data.setTemplateData("crit_class_name", critClassName != null ? critClassName : cdata.getClassName());
        data.setTemplateData("crit_values", cdata.getValuesWeb());
        if((critClassName != null && critCaseSensitive != null) || (critClassName == null && cdata.isCaseSensitive()))
            data.setTemplateData("crit_case_sensitive", true);
        if((critClassName != null && critAllowExtra != null) || (critClassName == null && cdata.isAllowExtraValues()))
            data.setTemplateData("crit_allow_extra", true);
        
        return true;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        // Load criteria data
        JavaEnums_Criteria cdata = (JavaEnums_Criteria)iac.getQC().getData();
        if(cdata == null)
        {
            iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
            return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
        }
        // Load path with class files into URL loader, find desired class, inspect
        JavaEnums_InstanceCriteria icdata = new JavaEnums_InstanceCriteria();
        String path = Storage.getPath_tempIAQ(core.getPathShared(), iac.getIAQ());
        File f = new File(path);
        // -- Check class files exist
        if(f.exists())
        {
            try
            {
                // Create loader for class-files
                URLClassLoader cl = new URLClassLoader(new URL[]{f.toURI().toURL()});
                // Fetch enum class
                Class c = cl.loadClass(cdata.getClassName());
                if(c != null)
                {
                    if(c.isEnum())
                    {
                        Object[] consts = c.getEnumConstants();
                        // Check the enum has the values required
                        {
                            ArrayList<String> valuesNotFound = new ArrayList<>();
                            boolean found;
                            String coS;
                            for(String s : cdata.getValues())
                            {
                                found = false;
                                for(Object co : consts)
                                {
                                    coS = co.toString();
                                    if(
                                            (cdata.isCaseSensitive() && coS.equals(s)) ||
                                            (!cdata.isCaseSensitive() && coS.toLowerCase().equals(s.toLowerCase()))
                                    )
                                    {
                                        found = true;
                                        break;
                                    }
                                }
                                if(!found)
                                    valuesNotFound.add(s);
                            }
                            // Save values not found
                            icdata.setValuesMissing(valuesNotFound.toArray(new String[valuesNotFound.size()]));
                        }
                        // Check for disallowed extra values
                        if(!cdata.isAllowExtraValues())
                        {
                            ArrayList<String> valuesExtra = new ArrayList<>();
                            // Iterate each constant and check it's allowed
                            String coS;
                            boolean found;
                            for(Object co : consts)
                            {
                                found = false;
                                coS = co.toString();
                                for(String s : cdata.getValues())
                                {
                                    if(
                                            (cdata.isCaseSensitive() && coS.equals(s)) ||
                                            (!cdata.isCaseSensitive() && coS.toLowerCase().equals(s.toLowerCase()))
                                    )
                                    {
                                        found = true;
                                        break;
                                    }
                                }
                                if(!found)
                                    valuesExtra.add(coS);
                            }
                            icdata.setValuesExtra(valuesExtra.toArray(new String[valuesExtra.size()]));
                        }
                        // Compute mark
                        double tconsts = cdata.getValues().length;
                        double mark = ((double)(tconsts-icdata.getValuesMissing().length) / tconsts) * 100.0;
                        if(!cdata.isAllowExtraValues())
                            mark -= ( (double)icdata.getValuesExtra().length / tconsts ) * 100.0;
                        // Check mark is valid
                        if(mark < 0.0)
                            mark = 0;
                        else if(mark > 100.0)
                            throw new IllegalStateException("Mark is above 100% somehow, possibly overflow. Fail-safe thrown.");
                        icdata.setStatus(mark == 100.0 ? JavaEnums_InstanceCriteria.Status.Correct : JavaEnums_InstanceCriteria.Status.Incorrect);
                        iac.setMark((int)mark);
                    }
                    else
                    {
                        icdata.setStatus(JavaEnums_InstanceCriteria.Status.NotEnum);
                        iac.setMark(0);
                    }
                }
                else
                {
                    icdata.setStatus(JavaEnums_InstanceCriteria.Status.NotFound);
                    iac.setMark(0);
                }
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
        JavaEnums_Criteria cdata = (JavaEnums_Criteria)iac.getQC().getData();
        // Load instance data
        JavaEnums_InstanceCriteria icdata = (JavaEnums_InstanceCriteria)iac.getData();
        if(cdata == null || icdata == null)
            return false;
        // Prepare to render template
        HashMap<String,Object> kvs = new HashMap<>();
        switch(icdata.getStatus())
        {
            case Correct:
                kvs.put("success", "Enum '"+cdata.getClassName()+"' is correct.");
                break;
            case Incorrect:
                StringBuilder sb;
                if(icdata.isValuesMissing())
                {
                    sb = new StringBuilder("The following constants, for enum '"+cdata.getClassName()+"', are missing: ");
                    for(String s : icdata.getValuesMissing())
                        sb.append(s).append(",");
                    sb.deleteCharAt(sb.length()-1);
                    kvs.put("error", sb.toString());
                }
                if(icdata.isValuesExtra())
                {
                    sb = new StringBuilder("The following enum constants, for enum '"+cdata.getClassName()+"', are not required: ");
                    for(String s : icdata.getValuesExtra())
                        sb.append(s).append(",");
                    sb.deleteCharAt(sb.length()-1);
                    kvs.put("error2", sb.toString());
                }
                break;
            case NotEnum:
                kvs.put("error", "Class at '"+cdata.getClassName()+"' is not an enum.");
                break;
            case NotFound:
                kvs.put("error", "Enum '"+cdata.getClassName()+"' could not be found.");
                break;
        }
        // Render template
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/feedback_display"));
        return true;
    }
}
