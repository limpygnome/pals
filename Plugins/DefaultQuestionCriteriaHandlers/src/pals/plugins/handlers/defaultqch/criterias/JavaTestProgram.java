package pals.plugins.handlers.defaultqch.criterias;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Storage;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.QuestionCriteria;
import pals.base.database.Connector;
import pals.base.utils.Misc;
import pals.base.utils.PalsProcess;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.DefaultQC;
import pals.plugins.handlers.defaultqch.data.CodeJava_Instance;
import pals.plugins.handlers.defaultqch.data.CodeJava_Question;
import pals.plugins.handlers.defaultqch.data.JavaTestProgram_Criteria;
import pals.plugins.handlers.defaultqch.data.JavaTestProgram_InstanceCriteria;
import pals.plugins.handlers.defaultqch.java.CompilerResult;
import pals.plugins.handlers.defaultqch.java.Utils;
import pals.plugins.handlers.defaultqch.logging.ModelException;

/**
 * A criteria for testing a program.
 */
public class JavaTestProgram
{
    // Classes *****************************************************************
    private static class Line
    {
        boolean input;
        String line;
    }
    private static Line parseLine(String[] data)
    {
        Line l = new Line();
        l.input = data[0].equals("in");
        l.line = data[1];
        return l;
    }
    // Constants ***************************************************************
    public static final UUID    UUID_CTYPE = UUID.parse("5ed0135a-f438-4487-ba24-6c7acd4c48ae");
    public static final String  TITLE = "Java: Test Program I/O";
    public static final String  DESCRIPTION = "Performs dynamic analysis of a program using standard input/output.";
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load qcdata
        JavaTestProgram_Criteria cdata = (JavaTestProgram_Criteria)qc.getData();
        if(cdata == null)
            cdata = new JavaTestProgram_Criteria();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle            = req.getField("crit_title");
        String critWeight           = req.getField("crit_weight");
        String critClass            = req.getField("crit_class");
        String critMethod           = req.getField("crit_method");
        String critArgs             = req.getField("crit_args");
        String critIO               = req.getField("crit_io");
        String critErrorThreshold   = req.getField("crit_error_threshold");
        // -- Optional
        String critMerge = req.getField("crit_merge");
        String critHide = req.getField("crit_hide");
        if(critTitle != null && critWeight != null && critClass != null && critMethod != null && critArgs != null && critIO != null)
        {
            int errorThreshold;
            try
            {
                errorThreshold = Integer.parseInt(critErrorThreshold);
            }
            catch(NumberFormatException ex)
            {
                errorThreshold = -1;
            }
            if(errorThreshold < 0)
                data.setTemplateData("error", "Error threshold must be numeric and zero or greater.");
            else if(!cdata.setEpClass(critClass))
                data.setTemplateData("error", "Invalid entry-point class.");
            else if(!cdata.setEpMethod(critMethod))
                data.setTemplateData("error", "Invalid entry-point method.");
            else if(!cdata.setIO(critIO))
                data.setTemplateData("error", "Invalid I/O; must contain at least one item.");
            else
            {
                // Update model
                cdata.setEpArgs(critArgs);
                cdata.setMergeStdErr(critMerge != null && critMerge.equals("1"));
                cdata.setHideSolution(critHide != null && critHide.equals("1"));
                cdata.setErrorThreshold(errorThreshold);
                // Handle entire process
                CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/javatestprogram_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        data.setTemplateData("crit_class", critClass != null ? critClass : cdata.getEpClass());
        data.setTemplateData("crit_method", critMethod != null ? critMethod : cdata.getEpMethod());
        data.setTemplateData("crit_args", critArgs != null ? critArgs : cdata.getEpArgsWeb());
        data.setTemplateData("crit_io", critIO != null ? critIO : cdata.getIOWeb());
        data.setTemplateData("crit_error_threshold", critErrorThreshold != null ? critErrorThreshold : cdata.getErrorThreshold());
        data.setTemplateData("crit_merge", (critMerge != null && critMerge.equals("1")) || (critTitle == null && cdata.getMergeStdErr()));
        return true;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
            iac.setMark(0);
        else
        {
            // Load qcdata and idata
            CodeJava_Question           qdata = (CodeJava_Question)iac.getQC().getQuestion().getData();
            JavaTestProgram_Criteria    qcdata = (JavaTestProgram_Criteria)iac.getQC().getData();
            CodeJava_Instance           idata = (CodeJava_Instance)iac.getIAQ().getData();
            if(idata == null || idata.getStatus() != CompilerResult.CompileStatus.Success)
                iac.setMark(0);
            else if(qcdata == null || qdata == null)
                return false;
            else
            {
                // Fetch script to follow
                String[][] IO = qcdata.getIO();
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
                String[] sandboxArgs;
                {
                    String[] args = qcdata.getEpArgs();
                    Utils.formatInputs(core, args);
                    sandboxArgs = Utils.buildJavaSandboxArgs(core, javaSandbox, pathIAQ, qcdata.getEpClass(), qcdata.getEpMethod(), qdata.getWhitelist(), false, args);
                }
                // Create process for code
                PalsProcess proc = PalsProcess.create(core, pathIAQ, "java", sandboxArgs);
                // Define process parameters
                proc.getProcessBuilder().redirectErrorStream(qcdata.getMergeStdErr());
                // Start process
                if(!proc.start())
                {
                    iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                    return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
                }
                // Interface with process
                long timeout = System.currentTimeMillis()+core.getSettings().getInt("tools/windows_user_tool/timeout_ms", 12000);
                int errorThreshold = qcdata.getErrorThreshold();
                int errors = 0;
                int correct = 0;
                int total = 0;
                // -- Hook I/O
                BufferedReader br = new BufferedReader(new InputStreamReader(proc.getProcess().getInputStream()));
                PrintWriter pw = new PrintWriter(proc.getProcess().getOutputStream());
                // -- Prepare first item
                int ioIndex = 0;
                int ioLimit = IO.length;
                Line l = parseLine(IO[0]);
                String line;
                // -- Flags to indicate if to switch line.
                boolean nextLine = false;
                // -- Capture output
                JavaTestProgram_InstanceCriteria cdata = new JavaTestProgram_InstanceCriteria();
                boolean keepRunning = true;
                boolean exceptionLogged = false; // Prevent multiple exceptions being logged
                while(keepRunning)
                {
                    try
                    {
                        // Check if to kill the process
                        if(System.currentTimeMillis() > timeout)
                        {
                            cdata.addLine("Terminated - program exceeded period to execute.", JavaTestProgram_InstanceCriteria.Status.Info);
                            proc.getProcess().destroy();
                            break;
                        }
                        try
                        {
                            // Check if to input data
                            if(l != null && l.input)
                            {
                                pw.println(l.line);
                                pw.flush();
                                cdata.addLine(l.line, JavaTestProgram_InstanceCriteria.Status.Input);
                                nextLine = true;
                            }
                            // Check for output
                            else
                            {
                                // Read the next line
                                line = br.readLine();
                                if(line == null || line.length() == 0)
                                {
                                    if(proc.hasExited())
                                        keepRunning = false;
                                }
                                // Check if the line is correct
                                else if(l != null && line.equals(l.line))
                                {
                                    total++;
                                    cdata.addLine(line, JavaTestProgram_InstanceCriteria.Status.Correct);
                                    correct++;
                                    errors = 0; // Reset errors.
                                }
                                else
                                {
                                    total++;
                                    cdata.addLine(line, JavaTestProgram_InstanceCriteria.Status.Incorrect);
                                    errors++;
                                    // Check if this is an exception line
                                    if(!exceptionLogged && line.startsWith("Exception: "))
                                    {
                                        Matcher m = DefaultQC.pattMatchNodeException.matcher(line);
                                        if(m.matches() && m.groupCount() >= 2)
                                        {
                                            ModelException e = new ModelException(m.group(1), m.group(2), iac.getIAQ(), true);
                                            e.persist(conn);
                                            exceptionLogged = true;
                                        }
                                    }
                                    // Check if the error threshold has been surpassed
                                    if(errors >= errorThreshold)
                                    {
                                        proc.getProcess().destroy();
                                        cdata.addLine("Terminated - too many I/O errors.", JavaTestProgram_InstanceCriteria.Status.Info);
                                        break;
                                    }
                                }
                                nextLine = true;
                            }
                            // Check if to switch line
                            if(nextLine)
                                l = ++ioIndex >= ioLimit ? null : parseLine(IO[ioIndex]);
                        }
                        catch(IOException ex){}
                        Thread.sleep(10);
                    }
                    catch(InterruptedException ex){}
                }
                try
                {
                    br.close();
                    pw.close();
                }
                catch(IOException ex){}
                // Update model
                iac.setMark(total > 0 & correct > 0 ? (int)(((double)correct/(double)total)*100.0) : 0);
                iac.setData(cdata);
            }
        }
        iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        JavaTestProgram_Criteria            qcdata = (JavaTestProgram_Criteria)iac.getQC().getData();
        JavaTestProgram_InstanceCriteria    idata = (JavaTestProgram_InstanceCriteria)iac.getData();
        if(qcdata != null && idata != null)
        {
            HashMap<String,Object> kvs = new HashMap<>();
            if(!qcdata.getHideSolution())
            {
                String solution = qcdata.getIOWeb();
                kvs.put("solution", solution);
                kvs.put("solution_lines", Misc.countOccurrences(solution, '\n'));
            }
            kvs.put("result", idata);
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/javatestprogram_display"));
            return true;
        }
        return false;
    }
}
