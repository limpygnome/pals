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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.io.FileUtils;
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
import pals.plugins.handlers.defaultqch.data.CodeJava_Shared;
import pals.plugins.handlers.defaultqch.data.JavaTestInputs_Criteria;
import pals.plugins.handlers.defaultqch.data.JavaTestInputs_InstanceCriteria;
import pals.plugins.handlers.defaultqch.java.CompilerResult;
import pals.plugins.handlers.defaultqch.java.Utils;
import pals.plugins.stats.ModelException;
import pals.plugins.stats.ModelExceptionClass;

/**
 * Handles text inputs criteria marking.
 */
public class JavaTestInputs
{
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("49d5b5fa-e0b9-427a-8171-04e7ae33fe64");
    public static final String  TITLE = "Java: Test Inputs";
    public static final String  DESCRIPTION = "Performs dynamic analysis of individual methods using varied inputs.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load criteria data
        JavaTestInputs_Criteria cdata = (JavaTestInputs_Criteria)qc.getData();
        if(cdata == null)
            cdata = new JavaTestInputs_Criteria();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        String critClassName = req.getField("crit_class_name");
        String critMethod = req.getField("crit_method");
        String critTestCode = req.getField("crit_test_code");
        String critInputTypes = req.getField("crit_input_types");
        String critInputs = req.getField("crit_inputs");
        // -- Optional
        String critForceCompile = req.getField("crit_force_compile");
        String critHidden = req.getField("crit_hidden");
        if(critTitle != null && critWeight != null && critClassName != null && critMethod != null && critTestCode != null && critInputTypes != null && critInputs != null)
        {
            boolean compile = !critTestCode.equals(cdata.getTestCode()) || (critForceCompile != null && critForceCompile.equals("1"));
            if(compile)
            {
                String className = Utils.parseFullClassName(critTestCode);
                if(className == null)
                    data.setTemplateData("error", "Cannot compile test-code, unable to determine the full class-name.");
                // Check disabled for now - there are cases where we do not need this.
                //else if(!className.equals(critClassName))
                //    data.setTemplateData("error", "Class-name of test-code ('"+className+"') does not match the provided class-name ('"+critClassName+"'); these must be the same.");
                else
                {
                    String outputPath = Storage.getPath_tempQC(data.getCore().getPathShared(), qc);
                    HashMap<String,String> codeMap = new HashMap<>();
                    // Copy support files
                    {
                        File op = new File(outputPath);
                        // Delete old files
                        if(op.isDirectory() && op.exists())
                        {
                            try
                            {
                                FileUtils.deleteDirectory(op);
                            }
                            catch(IOException ex){}
                        }
                        // Remake dir
                        if(!op.exists())
                            op.mkdir();
                        // Copy files
                        CodeJava_Shared.copyQuestionFiles(data.getCore(), qc.getQuestion(), TITLE);
                    }
                    // Copy any code from the question
                    {
                        CodeJava_Question qdata = (CodeJava_Question)qc.getQuestion().getData();
                        if(qdata != null && qdata.getCodeMap() != null)
                            codeMap.putAll(qdata.getCodeMap());
                    }
                    // Add test code
                    codeMap.put(className, critTestCode);
                    // Attempt to compile the code
                    CompilerResult cr = Utils.compile(data.getCore(), outputPath, codeMap);
                    CompilerResult.CompileStatus cs = cr.getStatus();
                    switch(cs)
                    {
                        case Unknown:
                            data.setTemplateData("warning", cs.getText());
                            break;
                        case Failed_CompilerNotFound:
                        case Failed_TempDirectory:
                        case Failed:
                            data.setTemplateData("error", cs.getText());
                            data.setTemplateData("error_messages", cr.getCodeErrors());
                            break;
                        case Success:
                            data.setTemplateData("info", cs.getText());
                            break;
                    }
                }
            }
            // Update data model
            cdata.setClassName(critClassName);
            cdata.setMethod(critMethod);
            cdata.setTestCode(critTestCode);
            cdata.setHideSolution(critHidden != null && critHidden.equals("1"));
            if(!cdata.setInputTypes(critInputTypes))
                data.setTemplateData("error", "Invalid input-types.");
            else if(!cdata.setInputs(critInputs))
                data.setTemplateData("error", "Invalid inputs.");
            else
            {
                // Handle entire process
                CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/javatestinputs_edit");
        Utils.pageHookCodeMirror_Java(data);
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : String.valueOf(qc.getWeight()));
        data.setTemplateData("crit_class_name", critClassName != null ? critClassName : cdata.getClassName());
        data.setTemplateData("crit_method", critMethod != null ? critMethod : cdata.getMethod());
        data.setTemplateData("crit_test_code", critTestCode != null ? critTestCode : cdata.getTestCode());
        data.setTemplateData("crit_input_types", critInputTypes != null ? critInputTypes : cdata.getInputTypesWeb());
        data.setTemplateData("crit_inputs", critInputs != null ? critInputs : cdata.getInputsWeb());
        data.setTemplateData("crit_hidden", critTitle != null ? (critHidden != null && critHidden.equals("1")) : cdata.getHideSolution());
        return true;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
            iac.setMark(0);
        else
        {
            // Load idata, qdata and cdata
            CodeJava_Instance       idata = (CodeJava_Instance)iac.getIAQ().getData();
            CodeJava_Question       qdata = (CodeJava_Question)iac.getQC().getQuestion().getData();
            JavaTestInputs_Criteria cdata = (JavaTestInputs_Criteria)iac.getQC().getData();
            if(idata == null || idata.getStatus() != CompilerResult.CompileStatus.Success)
                iac.setMark(0);     // No answer data; no need to mark.
            else if(qdata == null || cdata == null || cdata.getInputs().length == 0)
            {
                core.getLogging().log(DefaultQC.LOGGING_ALIAS, "Invalid/null question or criteria data for IAC '"+iac.getIAQ().getAIQID()+","+iac.getQC().getQCID()+"' (aqid,qcid).", Logging.EntryType.Error);
                return false;       // Question or criteria has not been setup properly.
            }
            else if(!idata.prepare(core, conn, iac.getIAQ()))
            {
                core.getLogging().log(DefaultQC.LOGGING_ALIAS, "Failed to prepare instance for assessment - IAC '"+iac.getIAQ().getAIQID()+","+iac.getQC().getQCID()+"' (aqid,qcid).", Logging.EntryType.Warning);
                return false;
            }
            else
            {
                // Fetch path of compiled classes
                String      pathQC = Storage.getPath_tempQC(core.getPathShared(), iac.getQC());
                String      pathIAQ = Storage.getPath_tempIAQ(core.getPathShared(), iac.getIAQ());
                // Fetch data ready for tests
                String[]    types = cdata.getInputTypes();
                String[][]  inputs = cdata.getInputs();
                String[]    whiteList = qdata.getWhitelist();
                String      className = cdata.getClassName();
                String      method = cdata.getMethod();
                int         timeout = core.getSettings().getInt("tools/windows_user_tool/timeout_ms", 12000);
                String      javaSandbox;
                try
                {
                    javaSandbox = new File(core.getSettings().getStr("tools/javasandbox/path")).getCanonicalPath();
                    pathQC = new File(pathQC).getCanonicalPath();
                    pathIAQ = new File(pathIAQ).getCanonicalPath();
                }
                catch(IOException ex)
                {
                    core.getLogging().log(DefaultQC.LOGGING_ALIAS, "Failed to create paths for Java-Sandbox/pathQC/pathIAQ ~ aiqid "+iac.getIAQ().getAIQID()+", qcid "+iac.getQC().getQCID(), Logging.EntryType.Warning);
                    iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                    return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
                }
                // Iterate each test input; test with student's and lecturer's code
                JavaTestInputs_InstanceCriteria icdata = new JavaTestInputs_InstanceCriteria(inputs.length);
                String[] argsQC, argsIAQ;
                String valQC, valIAQ;
                int correct = 0;
                String[] formattedArgs;
                boolean exceptionLogged = false;
                for(int row = 0; row < inputs.length; row++)
                {
                    formattedArgs = inputs[row];
                    // Append types to args
                    for(int i = 0; i < formattedArgs.length; i++)
                        formattedArgs[i] = types[i]+"="+formattedArgs[i];
                    // Format inputs
                    Utils.formatInputs(core, formattedArgs);
                    // Build args for both
                    argsQC = Utils.buildJavaSandboxArgs(core, javaSandbox, pathQC, className, method, whiteList, true, formattedArgs);
                    argsIAQ = Utils.buildJavaSandboxArgs(core, javaSandbox, pathIAQ, className, method, whiteList, true, formattedArgs);
                    // Execute each process and capture output
                    valQC = run(PalsProcess.create(core, pathQC, "java", argsQC), timeout);
                    valIAQ = run(PalsProcess.create(core, pathIAQ, "java", argsIAQ), timeout);
                    // Compare values
                    if(valIAQ == null || valQC == null)
                    {
                        // Something has gone wrong, set to manual marking
                        iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
                    }
                    else
                    {
                        // Check if the student and lecturer's solution match
                        if(valQC.equals(valIAQ))
                            correct++;
                        else if(!exceptionLogged && valIAQ.contains("Exception: "))
                        {
                            // Check for exception
                            String[] e = DefaultQC.matchException(valIAQ);
                            if(e != null)
                            {
                                ModelException ex = new ModelException(e[0], e[1], iac.getIAQ(), true);
                                ex.persist(conn);
                                exceptionLogged = true;
                            }
                        }
                    }
                    // Update result model
                    icdata.setInput(row, inputsToStr(formattedArgs));
                    icdata.setOutputCorrect(row, valQC);
                    icdata.setOutputStudent(row, valIAQ);
                }
                // Update data model for feedback
                iac.setData(icdata);
                // Calculate score
                iac.setMark( (int)(((double)correct/(double)inputs.length)*100.0) );
            }
        }
        iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    private static String inputsToStr(String[] inputs)
    {
        if(inputs.length == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        int ind;
        for(String s : inputs)
        {
            ind = s.indexOf('=');
            sb.append(ind == s.length()-1 ? "" : s.substring(ind+1)).append(';');
        }
        return sb.deleteCharAt(sb.length()-1).toString();
    }
    private static String run(PalsProcess proc, int timeout)
    {
        // Start the process
        if(!proc.start())
            return null;
        // Begin reading standard output
        StringBuilder buffer = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getProcess().getInputStream()));
        // Wait for the process to terminate
        long timeoutL = System.currentTimeMillis()+(long)timeout;
        char[] cbuffer = new char[1024];
        String line;
        int cbufferRead;
        while(!proc.hasExited())
        {
            try
            {
                // Check if to kill the process
                if(System.currentTimeMillis() > timeoutL)
                {
                    proc.getProcess().destroy();
                    break;
                }
                // Read more output
                try
                {
                    while((line = br.readLine()) != null)
                    {
                        if(line.endsWith("javasandbox-end-of-program"))
                        {
                            // Inform the sandbox we're happy for it to end - this can be anything
                            proc.getProcess().getOutputStream().write(0);
                            // As a fail-safe...we no longer need the process...
                            proc.getProcess().destroy();
                            break;
                        }
                        else
                            buffer.append(line).append('\n');
                    }
                }
                catch(IOException ex)
                {
                }
                // Sleep...
                Thread.sleep(1);
            }
            catch(InterruptedException ex) {}
        }
        return buffer.toString().trim();
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        // Load cdata
        JavaTestInputs_Criteria cdata = (JavaTestInputs_Criteria)iac.getQC().getData();
        // Load icdata
        JavaTestInputs_InstanceCriteria icdata = (JavaTestInputs_InstanceCriteria)iac.getData();
        if(icdata != null && cdata != null)
        {
            HashMap<String,Object> kvs = new HashMap<>();
            kvs.put("result", icdata);
            kvs.put("mark", iac.getMark());
            kvs.put("input_mark", (1.0/icdata.getTests())*100.0);
            kvs.put("hide_solution", cdata.getHideSolution());
            // Find runtime errors and build feedback
            String[] e;
            String t;
            HashSet<String> hints = new HashSet<>();
            for(int i = 0; i < icdata.getTests(); i++)
            {
                if((e = DefaultQC.matchException(icdata.getOutputStudent(i))) != null && (t = ModelExceptionClass.fetchHint(data.getConnector(), e[0], true)) != null)
                    hints.add(t);
            }
            kvs.put("hints", hints.toArray(new String[hints.size()]));
            // Render template
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/javatestinputs_display"));
            return true;
        }
        return false;
    }
}
