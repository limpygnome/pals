package pals.plugins.handlers.defaultqch.questions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import pals.base.Logging;
import pals.base.Storage;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Question;
import pals.base.web.RemoteRequest;
import pals.base.web.UploadedFile;
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
    public static final UUID    UUID_QTYPE = UUID.parse("3b452432-d939-4e39-a450-3867655412a3");
    public static final String  TITLE = "Code: Java";
    public static final String  DESCRIPTION = "Allows students to upload or provide fragments of Java code.";
    public static final int     FILEUPLOAD_FILES_LIMIT = 64;
    // Methods - Pages *********************************************************
    public static boolean pageQuestionEdit(WebRequestData data, Question q)
    {
        // Load question data
        CodeJava_Question qdata = (CodeJava_Question)q.getData();
        if(q.getData() == null)
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
        Utils.pageHookCodeMirror_Java(data);
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
        String reqReset =       req.getField("codejava_"+aqid+"_reset");
        String reqSubmitted =   req.getField("codejava_"+aqid+"_submitted");
        String reqViewCode =    req.getField("codejava_"+aqid+"_viewcode");
        String reqCompile =     req.getField("codejava_"+aqid+"_compile");
        boolean submitted =     reqSubmitted != null && reqSubmitted.equals("1");
        boolean reset =         reqReset != null && reqReset.equals("1");
        boolean viewCode =      reqViewCode != null && reqViewCode.equals("1");
        boolean compile =       reqCompile != null && reqCompile.equals("1");
        if(submitted && !viewCode)
        {
            // Check if to reset existing files
            if(reset)
            {
                try
                {
                    // Reset model
                    adata.filesClear();
                    adata.codeClear();
                    adata.setCompileStatus(CompilerResult.CompileStatus.Unknown);
                    adata.errorClear();
                    // Reset directory for IAQ
                    File fIaq = new File(Storage.getPath_tempIAQ(data.getCore().getPathShared(), iaq));
                    if(fIaq.exists())
                        FileUtils.deleteDirectory(fIaq);
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
                catch(IOException ex)
                {
                    kvs.put("error", "Failed to delete instance data directory; please try again or contact an administrator.");
                }
            }
            // Check no error has occurred thus-far, before handling file-upload
            if(!kvs.containsKey("error") && file != null)
            {
                // Fetch the path of the file
                File fZip = new File(Storage.getPath_tempWeb(data.getCore().getPathShared())+"/"+file.getTempName());
                if(!fZip.exists())
                    kvs.put("error", "Temporary file upload missing; please try again or contact an administrator.");
                // Create dir for unzipping
                else
                {
                    // Create temp dir
                    File fOut = new File(Storage.getPath_tempWebDir(data.getCore().getPathShared(), data.getRequestData().getIpAddress()));
                    if(!fOut.mkdir())
                        kvs.put("error", "Failed to setup temporary directory; please try again or contact an administrator.");
                    else
                    {
                        try
                        {
                            // Iterate the zip archive
                            ZipFile zip = new ZipFile(fZip);
                            Enumeration<? extends ZipEntry> ents = zip.entries();
                            ZipEntry ent;
                            InputStream is;
                            // -- Setup IAQ dir
                            String dirIAQ = Storage.getPath_tempIAQ(data.getCore().getPathShared(), iaq);
                            File fIAQ = new File(dirIAQ);
                            if(!fIAQ.exists())
                                fIAQ.mkdir();
                            // -- Setup buffers
                            StringBuilder       codeBuffer;
                            InputStreamReader   isr;
                            char[]              buffer = new char[4096];
                            int                 bufferlen;
                            String              code;
                            int                 files = 0;
                            File                f;
                            FileOutputStream    fos;
                            while(ents.hasMoreElements())
                            {
                                ent = ents.nextElement();
                                if(files >= FILEUPLOAD_FILES_LIMIT)
                                    kvs.put("error", "Maximum number of files, "+FILEUPLOAD_FILES_LIMIT+", reached.");
                                // Place java files into model, other files into model dir
                                else if(!ent.isDirectory())
                                {
                                    is = zip.getInputStream(ent);
                                    // Read source-files into model
                                    if(ent.getName().endsWith(".java"))
                                    {
                                        codeBuffer = new StringBuilder();
                                        isr = new InputStreamReader(is);
                                        while((bufferlen = isr.read(buffer, 0, 4096)) != -1)
                                            codeBuffer.append(buffer, 0, bufferlen);
                                        code = codeBuffer.toString();
                                        adata.codeAdd(Utils.parseFullClassName(code), code);
                                    }
                                    // Read files into IAQ/model dir
                                    // -- Ignore .class files, most likely a student/user forgetting to delete their build files
                                    // -- -- Remove them to avoid conflict with our own compile process.
                                    else if(!ent.getName().endsWith(".class"))
                                    {
                                        f = new File(fIAQ, ent.getName());
                                        // Ensure all dirs have been created, in-case the file is within a new sub-dir
                                        f.getParentFile().mkdirs();
                                        // Create new file
                                        f.createNewFile();
                                        // Open up a stream to the file destination, copy source to dest stream, dispose
                                        fos = new FileOutputStream(f);
                                        IOUtils.copy(is, fos);
                                        fos.flush();
                                        fos.close();
                                        // Add to model
                                        adata.filesAdd(ent.getName());
                                    }
                                    files++;
                                }
                            }
                            // Delete directory
                            fOut.delete();
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
                        }
                        catch(ZipException ex)
                        {
                            kvs.put("error", "Unable to read file; corrupt or not a zip archive.");
                        }
                        catch(IOException ex)
                        {
                            kvs.put("error", "Exception occurred reading zip.");
                            data.getCore().getLogging().logEx("[CodeJava]", ex, Logging.EntryType.Warning);
                        }
                    }
                }
            }
            // Check an error has not occurred...
            if(!kvs.containsKey("error") && compile)
            {
                CompilerResult cr = Utils.compile(data.getCore(), Storage.getPath_tempIAQ(data.getCore().getPathShared(), iaq), adata.getCodeMap());
                // Update the model's status
                adata.setCompileStatus(cr.getStatus());
                // Handle the compile result
                switch(cr.getStatus())
                {
                    case Failed:
                        for(CodeError error : cr.getCodeErrors())
                            adata.errorsAdd(error);
                        break;
                }
                // Attempt to persist model
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
                // Check we're not in reset-mode
                if(!resetMode)
                {
                    adata.codeAdd(parsedClassName, code);
                    // Check if to compile
                    if(compile != null && compile.equals("1"))
                    {
                        // -- If fails, set answered to false.
                        CompilerResult cr = Utils.compile(data.getCore(), Storage.getPath_tempIAQ(data.getCore().getPathShared(), iaq), adata.getCodeMap());
                        // Update the model's status
                        adata.setCompileStatus(cr.getStatus());
                        // Handle the compile result
                        switch(cr.getStatus())
                        {
                            case Failed:
                                for(CodeError error : cr.getCodeErrors())
                                    adata.errorsAdd(error);
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
