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
package pals.plugins.handlers.defaultqch.questions;

import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import pals.base.Storage;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Question;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;
import pals.base.web.UploadedFile;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.data.CodeError;
import pals.plugins.handlers.defaultqch.data.CodeJava_Instance;
import pals.plugins.handlers.defaultqch.data.CodeJava_Question;
import pals.plugins.handlers.defaultqch.data.CodeJava_Shared;
import pals.plugins.handlers.defaultqch.java.CompilerResult;
import pals.plugins.handlers.defaultqch.java.Utils;
import pals.plugins.stats.ModelException;
import pals.plugins.stats.ModelExceptionClass;

/**
 * Handles code-fragment questions.
 */
public class CodeJava extends QuestionHelper
{
    // Constants ***************************************************************
    public static final UUID    UUID_QTYPE = UUID.parse("3b452432-d939-4e39-a450-3867655412a3");
    public static final String  TITLE = "Code: Java";
    public static final String  DESCRIPTION = "Allows students to upload or provide fragments of Java code.";
    public static final int     FILEUPLOAD_FILES_LIMIT = 64;
    public static final Pattern patt;
    static
    {
        patt = Pattern.compile("");
    }
    // Methods - Pages *********************************************************
    public static boolean pageQuestionEdit(WebRequestData data, Question q)
    {
        // Load question data
        CodeJava_Question qdata = (CodeJava_Question)q.getData();
        if(q.getData() == null)
            qdata = new CodeJava_Question();
        
        RemoteRequest req = data.getRequestData();
        String  download = req.getField("download"),
                remove = req.getField("remove");
        
        if(download != null || remove != null)
        {
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request, please try again!");
            else if(download != null && download.length() != 0)
            {
                // Download file
                boolean java = download.endsWith(".java");
                if(java && download.length() > 5)
                    download = download.substring(0,download.length()-5);
                byte[] bdata = qdata.fetch(new File(Storage.getPath_tempQuestion(data.getCore().getPathShared(), q)), download);
                if(bdata != null)
                {
                    RemoteResponse resp = data.getResponseData();
                    resp.setBuffer(bdata);
                    resp.setHeader("Content-Disposition", "attachment; filename="+new File(download).getName()+(java ? ".java":""));
                    resp.setResponseType("application/octet-stream");
                    return true;
                }
                else
                    data.setTemplateData("error", "Could not fetch file '"+download+".");
            }
            else if(remove != null && remove.length() != 0)
            {
                // Remove file
                if(!qdata.remove(new File(Storage.getPath_tempQuestion(data.getCore().getPathShared(), q)), remove))
                    data.setTemplateData("error", "Could not remove file '"+remove+"'.");
                else
                {
                    q.setData(qdata);
                    q.persist(data.getConnector());
                }
            }
        }
        // Check for postback
        String  qTitle = req.getField("q_title"),
                qDesc = req.getField("q_desc");
        String  mcText = req.getField("mc_text"),
                mcType = req.getField("mc_type"),
                mcSkeleton = req.getField("mc_skeleton"),
                mcWhitelist = req.getField("mc_whitelist"),
                mcUploadPath = req.getField("mc_upload_path");
        CodeJava_Question.QuestionType qt = CodeJava_Question.QuestionType.parse(mcType);
        // -- Optional
        String mcReset = req.getField("mc_reset");
        UploadedFile mcUpload = req.getFile("mc_upload");
        if(mcText != null && mcType != null && mcWhitelist != null)
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Update question data
                qdata.setText(mcText);
                qdata.setType(qt);
                qdata.setSkeleton(mcSkeleton);
                qdata.setWhitelist(mcWhitelist.replace("\r", "").split("\n"));
                // Check if to reset files
                if(mcReset != null && mcReset.equals("1"))
                    qdata.reset(new File(Storage.getPath_tempQuestion(data.getCore().getPathShared(), q)));
                // Handle the rest of the request
                handle_questionEditPostback(data, q, qTitle, qDesc, qdata);
            }
            // Check for upload
            if(!data.containsTemplateData("error") && mcUpload != null && mcUpload.getSize() > 0)
            {
                // Validate request
                if(!CSRF.isSecure(data))
                    data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
                else
                {
                    // Fetch Q path
                    String qPath = Storage.getPath_tempQuestion(data.getCore().getPathShared(), q);
                    // Process the file
                    CodeJava_Shared.ProcessFileResult res = qdata.processFile(data, mcUpload, new File(qPath), mcUploadPath);
                    switch(res)
                    {
                        case Error:
                            data.setTemplateData("error", "An unknown error occurred handling the upload.");
                            break;
                        case Invalid_Path_Offset:
                            data.setTemplateData("error", "Invalid upload path. This cannot leave the current directory and must contain only alpha-numeric characters or dot, hyphen or dash.");
                            break;
                        case Failed_Dest_Dir:
                            data.setTemplateData("error", "Unable to create destination directory.");
                            break;
                        case Failed_Temp_Dir:
                            data.setTemplateData("error", "Unable to create temporary directory.");
                            break;
                        case Invalid_Zip:
                            data.setTemplateData("error", "Invalid ZIP archive uploaded.");
                            break;
                        case Maximum_Files:
                            data.setTemplateData("error", "Maximum files ("+FILEUPLOAD_FILES_LIMIT+") exceeded.");
                            break;
                        case Temp_Missing:
                            data.setTemplateData("error", "Temporary web upload missing.");
                            break;
                        case Success:
                            // Persist the model
                            q.setData(qdata);
                            Question.PersistStatus psq = q.persist(data.getConnector());
                            switch(psq)
                            {
                                default:
                                    data.setTemplateData("error", "Failed to persist question data; error '"+psq.name()+"'!");
                                    break;
                                case Success:
                                    data.setTemplateData("success", "Successfully updated question.");
                                    break;
                            }
                    }
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit");
        data.setTemplateData("pals_content", "defaultqch/questions/codejava_edit");
        Utils.pageHookCodeMirror_Java(data);
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("q_title", qTitle != null ? qTitle : q.getTitle());
        data.setTemplateData("q_desc", qDesc != null ? qDesc : q.getDescription());
        data.setTemplateData("mc_text", mcText != null ? mcText : qdata.getText());
        data.setTemplateData("mc_type", mcType != null ? qt.getFormValue() : qdata.getType().getFormValue());
        data.setTemplateData("mc_skeleton", mcSkeleton != null ? mcSkeleton : qdata.getSkeleton());
        data.setTemplateData("mc_whitelist", mcWhitelist != null ? mcWhitelist : qdata.getWhitelistWeb());
        data.setTemplateData("mc_upload_path", mcUploadPath);
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("code_names", qdata.getCodeNames());
        data.setTemplateData("file_names", qdata.getFileNames());
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
        // Ensure IAQ has been persisted; we'll need the identifier for compilation
        if(!iaq.isPersisted() && iaq.persist(data.getConnector()) != InstanceAssignmentQuestion.PersistStatus.Success)
            return false;
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
        HashMap<String,Object> kvs = new HashMap<>();
        // Check for postback
        RemoteRequest req =     data.getRequestData();
        int aqid =              iaq.getAssignmentQuestion().getAQID();
        UploadedFile file =     data.getRequestData().getFile("codejava_"+aqid+"_upload");
        String  reqReset =      req.getField("codejava_"+aqid+"_reset"),
                reqSubmitted =  req.getField("codejava_"+aqid+"_submitted"),
                reqViewCode =   req.getField("codejava_"+aqid+"_viewcode"),
                reqCompile =    req.getField("codejava_"+aqid+"_compile"),
                reqUploadPath = req.getField("codejava_"+aqid+"_upload_path");
        boolean submitted =     reqSubmitted != null && reqSubmitted.equals("1");
        boolean reset =         reqReset != null && reqReset.equals("1");
        boolean viewCode =      reqViewCode != null && reqViewCode.equals("1");
        boolean compile =       reqCompile != null && reqCompile.equals("1");
        String  download =      req.getField("codejava_"+aqid+"_download"),
                remove =        req.getField("codejava_"+aqid+"_remove");
        if(submitted && !viewCode)
        {
            // Check if to download file
            if(download != null && download.length() > 0)
            {
                boolean java = download.endsWith(".java");
                if(java && download.length() > 5)
                    download = download.substring(0,download.length()-5);
                byte[] bdata = adata.fetch(new File(Storage.getPath_tempIAQ(data.getCore().getPathShared(), iaq)), download);
                if(bdata != null)
                {
                    RemoteResponse resp = data.getResponseData();
                    resp.setBuffer(bdata);
                    resp.setHeader("Content-Disposition", "attachment; filename="+new File(download).getName()+(java ? ".java":""));
                    resp.setResponseType("application/octet-stream");
                    return true;
                }
                else
                    kvs.put("error", "Could not fetch file '"+download+"'.");
            }
            // Check if to remove file
            if(remove != null && remove.length() > 0)
            {
                if(!adata.remove(new File(Storage.getPath_tempIAQ(data.getCore().getPathShared(), iaq)), remove))
                    kvs.put("error", "Could not delete file '"+remove+"'.");
                else
                {
                    iaq.setData(adata);
                    iaq.persist(data.getConnector());
                }
            }
            // Check if to reset existing files
            if(reset)
            {
                // Reset files
                adata.reset(new File(Storage.getPath_tempIAQ(data.getCore().getPathShared(), iaq)));
                adata.setCompileStatus(CompilerResult.CompileStatus.Unknown);
                adata.errorClear();
                adata.setPrepared(false);
                // Attempt to persist model (in-case the uploading phase fails)
                iaq.setData(adata);
                InstanceAssignmentQuestion.PersistStatus iaqps = iaq.persist(data.getConnector());
                switch(iaqps)
                {
                    case Failed:
                    case Failed_Serialize:
                    case Invalid_AssignmentQuestion:
                    case Invalid_InstanceAssignment:
                        kvs.put("error", "Failed to update question ('"+iaqps.name()+"'), during reset!");
                        break;
                    case Success:
                        kvs.put("success", "Saved answer.");
                        break;
                }
            }
            // Check no error has occurred thus-far, before handling file-upload
            if(!kvs.containsKey("error") && file != null && reqUploadPath != null)
            {
                // Fetch the output directory
                File fIAQ = new File(Storage.getPath_tempIAQ(data.getCore().getPathShared(), iaq));
                // Process the file
                CodeJava_Shared.ProcessFileResult res = adata.processFile(data, file, fIAQ, reqUploadPath);
                switch(res)
                {
                    case Error:
                        kvs.put("error", "An unknown error occurred; please try again or contact an administrator.");
                        break;
                    case Failed_Dest_Dir:
                        kvs.put("error", "Failed to create destination directory; please try again or contact an administrator.");
                        break;
                    case Failed_Temp_Dir:
                        kvs.put("error", "Failed to create temporary directory; please try again or contact an administrator.");
                        break;
                    case Invalid_Zip:
                        kvs.put("error", "Invalid zip archive; please try again or contact an administrator.");
                        break;
                    case Maximum_Files:
                        kvs.put("error", "Maximum files ("+FILEUPLOAD_FILES_LIMIT+") exceeded; please try again or contact an administrator.");
                        break;
                    case Temp_Missing:
                        kvs.put("error", "Temporary web file missing; please try again or contact an administrator.");
                        break;
                    case Invalid_Path_Offset:
                        kvs.put("error", "Invalid upload path.");
                        break;
                    case Success:
                        // Update the iaq model
                        iaq.setData(adata);
                        // Attempt to persist
                        InstanceAssignmentQuestion.PersistStatus iaqps = iaq.persist(data.getConnector());
                        switch(iaqps)
                        {
                            case Failed:
                            case Failed_Serialize:
                            case Invalid_AssignmentQuestion:
                            case Invalid_InstanceAssignment:
                                kvs.put("error", "Failed to update question ('"+iaqps.name()+"'), during upload!");
                                break;
                            case Success:
                                kvs.put("success", "Saved answer.");
                                break;
                        }
                        break;
                }
            }
            // Check an error has not occurred...
            if(!kvs.containsKey("error") && compile)
            {
                // Copy question code, if available
                TreeMap<String,String> compileCode = CodeJava_Shared.copyCode(qdata, adata);
                // Compile the code
                CompilerResult cr = Utils.compile(data.getCore(), Storage.getPath_tempIAQ(data.getCore().getPathShared(), iaq), compileCode);
                // Update the model's status
                adata.setCompileStatus(cr.getStatus());
                // Handle the compile result
                adata.errorClear();
                switch(cr.getStatus())
                {
                    case Failed:
                        boolean exceptionLogged = false;
                        for(CodeError error : cr.getCodeErrors())
                        {
                            if(!exceptionLogged)
                            {
                                ModelException m = new ModelException(error.getMessage(), iaq);
                                m.persist(data.getConnector());
                            }
                            adata.errorsAdd(error);
                        }
                        break;
                }
                // Attempt to persist model
                adata.setPrepared(false);
                iaq.setData(adata);
                iaq.setAnswered(true);
                InstanceAssignmentQuestion.PersistStatus iaqps = iaq.persist(data.getConnector());
                switch(iaqps)
                {
                    case Failed:
                    case Failed_Serialize:
                    case Invalid_AssignmentQuestion:
                    case Invalid_InstanceAssignment:
                        kvs.put("error", "Failed to update question ('"+iaqps.name()+"'), during compilation!");
                        break;
                    case Success:
                        kvs.put("success", "Compiled code successfully.");
                        break;
                }
            }
        }
        // Set fields
        kvs.put("data", data);
        kvs.put("aqid", aqid);
        kvs.put("text", qdata.getText());
        if(!viewCode)
        {
            kvs.put("view_code", false);
            kvs.put("whitelist", qdata.getWhitelist());
            kvs.put("skeleton", qdata.getSkeleton());
            kvs.put("error_messages", adata.getErrors());
            kvs.put("code_names", adata.getCodeNames());
            kvs.put("file_names", adata.getFileNames());
            kvs.put("error_class", new ModelExceptionClass()); // Fetches hints through static calls
            kvs.put("upload_path", reqUploadPath);
        }
        else
        {
            kvs.put("view_code", true);
            kvs.put("code", adata.getCode());
        }
        // -- Compiler status
        CompilerResult.CompileStatus cs = adata.getStatus();
        switch(cs)
        {
            case Unknown:
                kvs.put("warning", cs.getText());
                break;
            case Failed_NoCode:
                kvs.put("error", "You have not uploaded any code which can be compiled.");
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
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/codejava_capture_upload"));
        Utils.pageHookCodeMirror_Java(data);
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
                adata.setPrepared(false);
                // Check we're not in reset-mode
                if(!resetMode)
                {
                    adata.codeAdd(parsedClassName, code);
                    // Check if to compile
                    if(compile != null && compile.equals("1"))
                    {
                        // Copy question code, if available
                        TreeMap<String,String> compileCode = CodeJava_Shared.copyCode(qdata, adata);
                        // -- If fails, set answered to false.
                        CompilerResult cr = Utils.compile(data.getCore(), Storage.getPath_tempIAQ(data.getCore().getPathShared(), iaq), compileCode);
                        // Update the model's status
                        adata.setCompileStatus(cr.getStatus());
                        // Handle the compile result
                        switch(cr.getStatus())
                        {
                            case Failed:
                                boolean exceptionLogged = false;
                                for(CodeError error : cr.getCodeErrors())
                                {
                                    if(!exceptionLogged)
                                    {
                                        ModelException m = new ModelException(error.getMessage(), iaq);
                                        m.persist(data.getConnector());
                                        exceptionLogged = true;
                                    }
                                    adata.errorsAdd(error);
                                }
                                break;
                        }
                    }
                }
                // Update the iaq model
                iaq.setData(adata);
                iaq.setAnswered(adata.getStatus() == CompilerResult.CompileStatus.Success);
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
        kvs.put("data", data);
        kvs.put("aqid", aqid);
        kvs.put("text", qdata.getText());
        kvs.put("whitelist", qdata.getWhitelist());
        if(!resetMode)
            kvs.put("code", code != null ? code : (adata.codeSize() == 1 ? adata.codeGetFirst() : null));
        kvs.put("skeleton", qdata.getSkeleton());
        kvs.put("error_messages", adata.getErrors());
        kvs.put("error_class", new ModelExceptionClass()); // Fetches hints through static calls
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
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/codejava_capture_fragment"));
        Utils.pageHookCodeMirror_Java(data);
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
        Utils.pageHookCodeMirror_Java(data);
        return true;
    }
}
