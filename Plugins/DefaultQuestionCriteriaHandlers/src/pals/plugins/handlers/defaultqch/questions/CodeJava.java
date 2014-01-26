package pals.plugins.handlers.defaultqch.questions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Question;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.data.CodeError;
import pals.plugins.handlers.defaultqch.data.CodeJava_Instance;
import pals.plugins.handlers.defaultqch.data.CodeJava_Question;
import pals.plugins.handlers.defaultqch.java.CompilerResult;
import pals.plugins.handlers.defaultqch.java.Utils;

/**
 * Handles code-fragment questions.
 */
public class CodeJava
{
    // Constants ***************************************************************
    public static final UUID UUID_QTYPE = UUID.parse("3b452432-d939-4e39-a450-3867655412a3");
    // Methods - Pages *********************************************************
    public static boolean pageQuestionEdit(WebRequestData data, Question q)
    {
        // Load question data
        CodeJava_Question qdata;
        if(q.getData() != null)
            qdata = q.getData();
        else
            qdata = new CodeJava_Question();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String mcText = req.getField("mc_text");
        String mcType = req.getField("mc_type");
        String mcSkeleton = req.getField("mc_skeleton");
        String mcWhitelist = req.getField("mc_whitelist");
        if(mcText != null && mcType != null && mcWhitelist != null)
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Update question data
                qdata.setText(mcText);
                qdata.setType(CodeJava_Question.QuestionType.parse(mcType));
                qdata.setSkeleton(mcSkeleton);
                qdata.setWhitelist(mcWhitelist.replace("\r", "").split("\n"));
                // Persist the model
                q.setData(qdata);
                Question.PersistStatus psq = q.persist(data.getConnector());
                switch(psq)
                {
                    default:
                        data.setTemplateData("error", "Failed to persist question data; error '"+psq.name()+"'!");
                    case Success:
                        data.setTemplateData("success", "Successfully updated question.");
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit");
        data.setTemplateData("pals_content", "defaultqch/questions/codejava_edit");
        hookCodeMirror(data);
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("mc_text", mcText != null ? mcText : qdata.getText());
        data.setTemplateData("mc_type", qdata.getType().getFormValue());
        data.setTemplateData("mc_skeleton", mcSkeleton != null ? mcSkeleton : qdata.getSkeleton());
        data.setTemplateData("mc_whitelist", qdata.getWhitelistWeb());
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    public static boolean pageQuestionCapture(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure)
    {
        // Load question data
        CodeJava_Question qdata = (CodeJava_Question)iaq.getAssignmentQuestion().getQuestion().getData();
        if(qdata == null)
            return false;
        // Load instance data
        CodeJava_Instance adata = (CodeJava_Instance)iaq.getData();
        if(adata == null)
            adata = new CodeJava_Instance();
        // Delegate based on type
        switch(qdata.getType())
        {
            case Fragment:
                return pageQuestionCapture_fragment(data, ia, iaq, html, secure, qdata, adata);
            case Upload:
                return pageQuestionCapture_upload(data, ia, iaq, html, secure, qdata, adata);
            default:
                return false;
        }
    }
    public static boolean pageQuestionCapture_upload(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure, CodeJava_Question qdata, CodeJava_Instance adata)
    {
        return true;
    }
    public static boolean pageQuestionCapture_fragment(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure, CodeJava_Question qdata, CodeJava_Instance adata)
    {
        HashMap<String,Object> kvs = new HashMap<>();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        int aqid = iaq.getAssignmentQuestion().getAQID();
        String code = req.getField("codejava_"+aqid+"_code");
        String compile = req.getField("codejava_"+aqid+"_compile");
        String reset = req.getField("codejava_"+aqid+"_reset");
        boolean resetMode = reset != null && reset.equals("1");
        if(secure && code != null)
        {
            String parsedClassName;
            // Parse the full class-name
            if((parsedClassName = Utils.parseFullClassName(code)) == null)
                kvs.put("error", "Unable to save code; could not parse a class-name from your code. Check for syntax errors!");
            else
            {
                // Update the code in the model
                adata.codeClear();
                adata.errorClear();
                adata.setCompileStatus(CompilerResult.CompileStatus.Unknown);
                // Check we're not in reset-mode
                if(!resetMode)
                {
                    adata.codeAdd(parsedClassName, code);
                    // Check if to compile
                    if(compile != null && compile.equals("1"))
                    {
                        // -- If fails, set answered to false.
                        CompilerResult cr = Utils.compile(data.getCore(), iaq, adata);
                        // Update the model's status
                        adata.setCompileStatus(cr.getStatus());
                        // Handle the compile result
                        switch(cr.getStatus())
                        {
                            case Failed:
                                DiagnosticCollector<JavaFileObject> dc = cr.getErrors();
                                if(dc != null)
                                {
                                    // Iterate the errors from the compiler
                                    List<Diagnostic<? extends JavaFileObject>> arr = dc.getDiagnostics();
                                    CodeError[] msgs = new CodeError[arr.size()];
                                    Diagnostic d;
                                    for(int i = 0; i < arr.size(); i++)
                                    {
                                        d = arr.get(i);
                                        adata.errorsAdd(
                                                new CodeError(d.getMessage(Locale.getDefault()), (int)d.getLineNumber(), (int)d.getColumnNumber())
                                        );
                                    }
                                }
                                break;
                        }
                    }
                }
                // Update the iaq model
                iaq.setData(adata);
                iaq.setAnswered(true);
                // Attempt to persist
                InstanceAssignmentQuestion.PersistStatus iaqps = iaq.persist(data.getConnector());
                switch(iaqps)
                {
                    case Failed:
                    case Failed_Serialize:
                    case Invalid_AssignmentQuestion:
                    case Invalid_InstanceAssignment:
                        kvs.put("error", "Failed to update question ('"+iaqps.name()+"')!");
                        break;
                    case Success:
                        kvs.put("success", "Saved answer.");
                        break;
                }
            }
        }
        // Set fields
        kvs.put("aqid", aqid);
        kvs.put("text", qdata.getText());
        kvs.put("whitelist", qdata.getWhitelist());
        if(!resetMode)
            kvs.put("code", code != null ? code : (adata.codeSize() == 1 ? adata.codeGetFirst() : null));
        kvs.put("skeleton", qdata.getSkeleton());
        kvs.put("error_messages", adata.getErrors());
        // -- Compiler status
        CompilerResult.CompileStatus cs = adata.getStatus();
        switch(cs)
        {
            case Unknown:
                kvs.put("warning", cs.getText());
                break;
            case Failed_CompilerNotFound:
            case Failed_TempDirectory:
            case Failed:
                kvs.put("error", cs.getText());
                break;
            case Success:
                kvs.put("info", cs.getText());
                break;
        }
        // Render the question
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/codejava_capture"));
        hookCodeMirror(data);
        return true;
    }
    public static boolean pageQuestionDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure, boolean editMode)
    {
        // Load question data
        CodeJava_Question qdata = (CodeJava_Question)iaq.getAssignmentQuestion().getQuestion().getData();
        if(qdata == null)
            return false;
        // Load instance data
        CodeJava_Instance adata = (CodeJava_Instance)iaq.getData();
        if(adata == null)
            adata = new CodeJava_Instance();
        // Setup fields
        HashMap<String,Object> kvs = new HashMap<>();
        kvs.put("aqid", iaq.getAssignmentQuestion().getAQID());
        kvs.put("text", qdata.getText());
        kvs.put("errors", adata.getErrors());
        kvs.put("code", adata.getCode());
        // Render template
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/codejava_display"));
        // Setup code-mirror
        hookCodeMirror(data);
        return true;
    }
    // Methods *****************************************************************
    private static void hookCodeMirror(WebRequestData data)
    {
        if(!data.containsTemplateData("codemirror_clike"))
        {
            data.appendHeaderJS("/content/codemirror/lib/codemirror.js");
            data.appendHeaderJS("/content/codemirror/addon/edit/matchbrackets.js");
            data.appendHeaderJS("/content/codemirror/mode/clike/clike.js");
            data.appendHeaderCSS("/content/codemirror/lib/codemirror.css");
        }
    }
}
