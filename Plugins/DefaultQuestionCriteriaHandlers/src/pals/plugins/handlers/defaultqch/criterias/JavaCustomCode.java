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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
import pals.base.utils.PalsProcess;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.DefaultQC;
import pals.plugins.handlers.defaultqch.data.CodeJava_Instance;
import pals.plugins.handlers.defaultqch.data.CodeJava_Question;
import pals.plugins.handlers.defaultqch.data.JavaCustom_Criteria;
import pals.plugins.handlers.defaultqch.data.JavaCustom_InstanceCriteria;
import pals.plugins.handlers.defaultqch.data.JavaTestProgram_InstanceCriteria;
import pals.plugins.handlers.defaultqch.java.CompilerResult;
import pals.plugins.handlers.defaultqch.java.Utils;

/**
 * A criteria for testing with custom code.
 * 
 * @version 1.0
 */
public class JavaCustomCode
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("07b83079-db06-4422-a9ec-780a2c502c06");
    public static final String  TITLE = "Java: Custom Code";
    public static final String  DESCRIPTION = "Invokes custom code to perform marking.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load qcdata
        JavaCustom_Criteria cdata = (JavaCustom_Criteria)qc.getData();
        if(cdata == null)
            cdata = new JavaCustom_Criteria();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle            = req.getField("crit_title");
        String critWeight           = req.getField("crit_weight");
        String critClass            = req.getField("crit_class");
        String critMethod           = req.getField("crit_method");
        String critMessageThreshold = req.getField("crit_messages");
        if(critTitle != null && critWeight != null && critClass != null && critMethod != null && critMessageThreshold != null)
        {
            int messages;
            try
            {
                messages = Integer.parseInt(critMessageThreshold);
            }
            catch(NumberFormatException ex)
            {
                messages = -1;
            }
            if(messages < 1)
                data.setTemplateData("error", "Invalid message threshold value, must be numeric and greater than zero.");
            else if(!cdata.setClassName(critClass))
                data.setTemplateData("error", "Invalid entry-point class.");
            else if(!cdata.setMethod(critMethod))
                data.setTemplateData("error", "Invalid entry-point method.");
            else
            {
                // Update model
                cdata.setMessageThreshold(messages);
                // Handle entire process
                CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/javacustom_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : String.valueOf(qc.getWeight()));
        data.setTemplateData("crit_class", critClass != null ? critClass : cdata.getClassName());
        data.setTemplateData("crit_method", critMethod != null ? critMethod : cdata.getMethod());
        data.setTemplateData("crit_messages", critMessageThreshold != null ? critMessageThreshold : cdata.getMessageThreshold());
        return true;
    }
    
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
            iac.setMark(0);
        else
        {
            // Load question, question instance and critera data
            CodeJava_Question           qdata = (CodeJava_Question)iac.getQC().getQuestion().getData();
            CodeJava_Instance           qidata = (CodeJava_Instance)iac.getIAQ().getData();
            JavaCustom_Criteria         cdata = (JavaCustom_Criteria)iac.getQC().getData();
            if(qidata == null || qidata.getStatus() != CompilerResult.CompileStatus.Success)
                iac.setMark(0);
            else if(cdata == null || qdata == null)
            {
                core.getLogging().log(DefaultQC.LOGGING_ALIAS, "Invalid/null question or criteria data for IAC '"+iac.getIAQ().getAIQID()+","+iac.getQC().getQCID()+"' (aqid,qcid).", Logging.EntryType.Error);
                return false;       // Question or criteria has not been setup properly.
            }
            else if(!qidata.prepare(core, conn, iac.getIAQ()))
            {
                core.getLogging().log(DefaultQC.LOGGING_ALIAS, "Failed to prepare instance for assessment - IAC '"+iac.getIAQ().getAIQID()+","+iac.getQC().getQCID()+"' (aqid,qcid).", Logging.EntryType.Warning);
                return false;
            }
            else
            {
                // Create instance data for feedback
                JavaCustom_InstanceCriteria icdata = new JavaCustom_InstanceCriteria();
                // Fetch sandbox and IAQ path
                String javaSandbox, pathIAQ;
                try
                {
                    javaSandbox = new File(core.getSettings().getStr("tools/javasandbox/path")).getCanonicalPath();
                    pathIAQ = new File(Storage.getPath_tempIAQ(core.getPathShared(), iac.getIAQ())).getCanonicalPath();
                }
                catch(IOException ex)
                {
                    core.getLogging().log(DefaultQC.LOGGING_ALIAS, "Failed to create paths for Java-Sandbox/pathQC/pathIAQ ~ aiqid "+iac.getIAQ().getAIQID()+", qcid "+iac.getQC().getQCID(), Logging.EntryType.Warning);
                    iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                    return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
                }
                // Build sandbox arguments
                String[] sandboxArgs = Utils.buildJavaSandboxArgs(core, javaSandbox, pathIAQ, cdata.getClassName(), cdata.getMethod(), qdata.getWhitelist(), true, new String[0]);
                // Create process for code
                PalsProcess proc = PalsProcess.create(core, pathIAQ, "java", sandboxArgs);
                proc.getProcessBuilder().redirectErrorStream(true);
                // Start process
                if(!proc.start())
                {
                    core.getLogging().log(DefaultQC.LOGGING_ALIAS, "Failed to start Java Sandbox ~ aiqid "+iac.getIAQ().getAIQID()+", qcid "+iac.getQC().getQCID(), Logging.EntryType.Warning);
                    iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                    return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
                }
                // Fetch the timeout value for the process, to manually kill the process
                long timeout = System.currentTimeMillis()+core.getSettings().getInt("tools/windows_user_tool/timeout_ms", 12000);
                BufferedReader br = new BufferedReader(new InputStreamReader(proc.getProcess().getInputStream()));
                PrintWriter pw = new PrintWriter(proc.getProcess().getOutputStream());
                boolean keepRunning = true;
                String line, lastLine = null;
                int msgTotal = 0;
                long started = System.currentTimeMillis();
                while(keepRunning)
                {
                    // Check if the process has exceeded the timeout
                    if(System.currentTimeMillis()-started > timeout)
                    {
                        icdata.add(JavaCustom_InstanceCriteria.FeedbackType.Error, "Program exceeded time limit allowed; forcibly terminated.");
                        proc.getProcess().destroy();
                        keepRunning = false;
                        // Avoid spoofing cheat possibility...
                        lastLine = null;
                    }
                    else
                    {
                        // Read the next available line, parse as possible feedback or
                        // info
                        try
                        {
                            line = br.readLine();
                            if(line == null || line.length() == 0)
                            {
                                if(proc.hasExited())
                                    keepRunning = false;
                            }
                            // Check if this is an exit line
                            else if(line.endsWith("javasandbox-end-of-program"))
                            {
                                // Inform sandbox it can terminate
                                pw.println("0"); // Input does not matter
                                pw.flush();
                                // Kill the process and end
                                proc.getProcess().destroy();
                                keepRunning = false;
                            }
                            else if(msgTotal == cdata.getMessageThreshold())
                            {
                                icdata.add(JavaCustom_InstanceCriteria.FeedbackType.Error, "Terminated due to message threshold being exceeded.");
                            }
                            // Check if error message
                            else if(line.startsWith("error ") && line.length() > 6)
                                icdata.add(JavaCustom_InstanceCriteria.FeedbackType.Error, line.substring(6));
                            // Check if warning message
                            else if(line.startsWith("warning ") && line.length() > 8)
                                icdata.add(JavaCustom_InstanceCriteria.FeedbackType.Warning, line.substring(8));
                            // Check if success message
                            else if(line.startsWith("success ") && line.length() > 8)
                                icdata.add(JavaCustom_InstanceCriteria.FeedbackType.Success, line.substring(8));
                            // Queue as last-line -> which could become an info message on next iteration
                            else
                            {
                                // Place previous message as info
                                if(lastLine != null)
                                    icdata.add(JavaCustom_InstanceCriteria.FeedbackType.Info, lastLine);
                                lastLine = line;
                            }
                        }
                        catch(IOException ex)
                        {
                        }
                        // Sleep to avoid excessive CPU usage
                        try
                        {
                            Thread.sleep(5);
                        }
                        catch(InterruptedException ex){}
                    }
                }
                // Dispose I/O
                try
                {
                    br.close();
                    pw.close();
                }
                catch(IOException ex){}
                // Check the last line received - this will be the output
                // -- Cannot be cheated/spoofed since exiting the environment will
                // -- print that exit has been called.
                if(lastLine != null && lastLine.length() > 0)
                {
                    try
                    {
                        int t = Integer.parseInt(lastLine);
                        if(t < 0 || t > 100)
                        {
                            // Invalid mark value - log and set for manual marking
                            core.getLogging().log(DefaultQC.LOGGING_ALIAS, "Invalid mark value of "+t+" returned for Java: Custom criteria ("+cdata.getClassName()+"."+cdata.getMethod()+") ~ aiqid "+iac.getIAQ().getAIQID()+", qcid "+iac.getQC().getQCID(), Logging.EntryType.Warning);
                            iac.setData(icdata);
                            iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                            return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
                        }
                        else
                            iac.setMark(t);
                    }
                    catch(NumberFormatException ex)
                    {
                        iac.setMark(0);
                        // Log as info message
                        icdata.add(JavaCustom_InstanceCriteria.FeedbackType.Info, lastLine);
                    }
                }
                else
                    iac.setMark(0);
                // Set model
                iac.setData(icdata);
            }
        }
        iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        // Load criteria and instance criteria data
        JavaCustom_Criteria cdata = (JavaCustom_Criteria)iac.getQC().getData();
        JavaCustom_InstanceCriteria icdata = (JavaCustom_InstanceCriteria)iac.getData();
        if(cdata != null && icdata != null)
        {
            // Setup feedback template values
            HashMap<String,Object> kvs = new HashMap<>();
            JavaCustom_InstanceCriteria.FeedbackMessage[] msgs = icdata.getMessages();
            kvs.put("messages", msgs);
            // Render and output template
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/javacustom_display"));
            return true;
        }
        return false;
    }
}
