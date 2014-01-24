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
    // Inner-Classes ***********************************************************
    public static class ModelView_CodeError
    {
        // Fields *************************************************************8
        public String message;
        public int line, col;
        // Methods - Constructors **********************************************
        public ModelView_CodeError(String message, int line, int col)
        {
            this.message = message;
            this.line = line;
            this.col = col;
        }
        // Methods - Accessors *************************************************
        public String getMessage()
        {
            return message;
        }
        public int getLine()
        {
            return line;
        }
        public int getCol()
        {
            return col;
        }
    }
    // Methods *****************************************************************
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
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("mc_text", qdata.getText());
        data.setTemplateData("mc_type", qdata.getType().getFormValue());
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
        if(secure && code != null)
        {
            String parsedClassName;
            // Parse the full class-name
            if((parsedClassName = Utils.parseFullClassName(code)) == null)
                kvs.put("error", "Could not parse class-name from your code; check it for syntax errors!");
            else
            {
                // Update the model
                adata.codeClear();
                adata.codeAdd(parsedClassName, code);
                // Attempt to compile
                // -- if fails, set answered to false.
                CompilerResult cr = Utils.compile(data.getCore(), iaq, adata);
                switch(cr.getStatus())
                {
                    case Failed_CompilerNotFound:
                        kvs.put("error", "Compiler not found; please try again or contact an administrator.");
                        break;
                    case Failed_TempDirectory:
                        kvs.put("error", "Could not establish temporary shared folder for instance of question; please try again or contact an administrator.");
                        break;
                    default:
                    case Failed:
                        kvs.put("error", "Failed to compile.");
                        DiagnosticCollector<JavaFileObject> dc = cr.getErrors();
                        if(dc != null)
                        {
                            List<Diagnostic<? extends JavaFileObject>> arr = dc.getDiagnostics();
                            ModelView_CodeError[] msgs = new ModelView_CodeError[arr.size()];
                            Diagnostic d;
                            for(int i = 0; i < arr.size(); i++)
                            {
                                d = arr.get(i);
                                msgs[i] = new ModelView_CodeError(d.getMessage(Locale.getDefault()), (int)d.getLineNumber(), (int)d.getColumnNumber());
                            }
                            kvs.put("error_messages", msgs);
                        }
                        break;
                    case Success:
                        kvs.put("info", "Compiled.");
                        break;
                }
                // Update the iaq model
                iaq.setAnswered(cr.getStatus() == CompilerResult.CompileStatus.Success);
                iaq.setData(adata);
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
        kvs.put("code", code != null ? code : (adata.codeSize() == 1 ? adata.codeGetFirst() : ""));
        // Render the question
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/codejava_capture"));
        if(!data.containsTemplateData("codejava_css"))
        {
            data.appendHeaderJS("/content/codemirror/lib/codemirror.js");
            data.appendHeaderJS("/content/codemirror/addon/edit/matchbrackets.js");
            data.appendHeaderJS("/content/codemirror/mode/clike/clike.js");
            data.appendHeaderCSS("/content/codemirror/lib/codemirror.css");
        }
        return true;
    }
    public static boolean pageQuestionDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure, boolean editMode)
    {
//        // Load question data
//        Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iaq.getAssignmentQuestion().getQuestion().getData();
//        // Load answer data
//        Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iaq.getData();
//        // Render the template
//        HashMap<String,Object> kvs = new HashMap<>();
//        kvs.put("text", qdata.getText());
//        kvs.put("answers", adata != null ? adata.getAnswers(qdata) : new String[0]);
//        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/multiplechoice_display"));
        return true;
    }
}
