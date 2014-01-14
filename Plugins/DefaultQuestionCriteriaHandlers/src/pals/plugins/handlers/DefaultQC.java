package pals.plugins.handlers;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.assessment.AssignmentQuestion;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Question;
import pals.base.assessment.QuestionCriteria;
import pals.base.assessment.TypeCriteria;
import pals.base.assessment.TypeQuestion;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.Data_Answer_MultipleChoice;
import pals.plugins.handlers.defaultqch.Data_Criteria_Regex;
import pals.plugins.handlers.defaultqch.Data_Criteria_TextMatch;
import pals.plugins.handlers.defaultqch.Data_Question_MultipleChoice;
import pals.plugins.handlers.defaultqch.Data_Question_Written;

/**
 * A plugin for the default questions and criteria types.
 */
public class DefaultQC extends Plugin
{
    // Fields - Constants ******************************************************
    private static final String LOGGING_ALIAS = "Default QC";
    // Fields - Constants - UUIDs - Question Types *****************************
    private static final UUID UUID_QUESTIONTYPE_MULTIPLECHOICE = UUID.parse("f38c3f28-4f63-49b5-994c-fd618c654de0");
    private static final UUID UUID_QUESTIONTYPE_WRITTENRESPONSE = UUID.parse("fcfec4ec-3347-4219-a988-f69b632b657e");
    private static final UUID UUID_QUESTIONTYPE_CODEUPLOAD = UUID.parse("7905af64-06b3-4fc2-b69b-448b97627ca5");
    private static final UUID UUID_QUESTIONTYPE_CODEFRAGMENT = UUID.parse("8740706a-37f6-49b0-af12-b21621cbf101");
    // Fields - Constants - UUIDs - Criteria Types *****************************
    private static final UUID UUID_CRITERIATYPE_MANUALMARKING = UUID.parse("03830aa9-39d7-4bfe-9ab5-a9c765e6e426");
    private static final UUID UUID_CRITERIATYPE_TEXTMATCH = UUID.parse("b9a1143c-98cb-446b-9b39-42addac71f4f");
    private static final UUID UUID_CRITERIATYPE_REGEXMATCH = UUID.parse("3e6518e8-bb13-4878-bb4c-c0d687ad2e6e");
    private static final UUID UUID_CRITERIATYPE_JAVALOC = UUID.parse("13fdc5fe-ce6f-4203-93af-a560d80ed5a4");
    private static final UUID UUID_CRITERIATYPE_JAVACONTAINSCLASS = UUID.parse("f0f16511-1187-4b34-8dd1-cb014b28b220");
    private static final UUID UUID_CRITERIATYPE_JAVACONTAINSMETHOD = UUID.parse("ed121829-7380-463c-a386-39441ea02539");
    // Fields ******************************************************************
    
    // Methods - Constructors **************************************************
    public DefaultQC(NodeCore core, UUID uuid, JarIO jario, Settings settings, String jarPath)
    {
        super(core, uuid, jario, settings, jarPath);
    }
    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginInstall(NodeCore core, Connector conn)
    {
        // Define types of questions
        TypeQuestion    tqMultipleChoice,
                        tqWrittenResponse,
                        tqCodeUpload,
                        tqCodeFragment;
        // Register the types of questions
        if((tqMultipleChoice = registerTypeQuestion(conn, core, UUID_QUESTIONTYPE_MULTIPLECHOICE, "Multiple Choice", "Allows students to pick an answer, or multiple answers, from a set of possible answers.")) == null)
            return false;
        if((tqWrittenResponse = registerTypeQuestion(conn, core, UUID_QUESTIONTYPE_WRITTENRESPONSE, "Written Response", "Allows students to provide a written response.")) == null)
            return false;
        if((tqCodeUpload = registerTypeQuestion(conn, core, UUID_QUESTIONTYPE_CODEUPLOAD, "Code Upload", "Allows students to upload code to be assessed.")) == null)
            return false;
        if((tqCodeFragment = registerTypeQuestion(conn, core, UUID_QUESTIONTYPE_CODEFRAGMENT, "Code Fragment", "Allows students to answer with a code-fragment and compare the output with the correct answer.")) == null)
            return false;
        // Register criteria types
        TypeCriteria    tcManualMarking,
                        tcTextMatch,
                        tcRegexMatch,
                        tcJavaLOC,
                        tcJavaContainsClass,
                        tcJavaContainsMethod;
        if((tcManualMarking = registerTypeCriteria(conn, core, UUID_CRITERIATYPE_MANUALMARKING, "Manual Marking", "Used by a lecturer to manually-mark a criteria of a question.")) == null)
            return false;
        if((tcTextMatch = registerTypeCriteria(conn, core, UUID_CRITERIATYPE_TEXTMATCH, "Text Match", "Gives marks for an answer matching a piece of text.")) == null)
            return false;
        if((tcRegexMatch = registerTypeCriteria(conn, core, UUID_CRITERIATYPE_REGEXMATCH, "Regex Match", "Gives marks based on a regex match.")) == null)
            return false;
        if((tcJavaLOC = registerTypeCriteria(conn, core, UUID_CRITERIATYPE_JAVALOC, "Java: Lines of Code", "Gives marks based on the lines-of-code.")) == null)
            return false;
        if((tcJavaContainsClass = registerTypeCriteria(conn, core, UUID_CRITERIATYPE_JAVACONTAINSCLASS, "Java: Contains Class", "Gives marks for the definition of a class.")) == null)
            return false;
        if((tcJavaContainsMethod = registerTypeCriteria(conn, core, UUID_CRITERIATYPE_JAVACONTAINSMETHOD, "Java: Contains Method", "Gives marks for the definition of a method.")) == null)
            return false;
        // Add criterias to questions, and persist
        // -- Multiple Choice
        tqMultipleChoice.criteriaAdd(tcManualMarking);
        tqMultipleChoice.criteriaAdd(tcRegexMatch);
        tqMultipleChoice.criteriaAdd(tcTextMatch);
        if(!persistQuestionCriteria(core, tqMultipleChoice, conn))
            return false;
        // -- Written Response
        tqWrittenResponse.criteriaAdd(tcManualMarking);
        tqWrittenResponse.criteriaAdd(tcRegexMatch);
        tqWrittenResponse.criteriaAdd(tcTextMatch);
        if(!persistQuestionCriteria(core, tqWrittenResponse, conn))
            return false;
        // -- Code Upload
        //tqCodeUpload.criteriaAdd(tcJavaLOC);
        //tqCodeUpload.criteriaAdd(tcJavaContainsClass);
        //tqCodeUpload.criteriaAdd(tcJavaContainsMethod);
        // -- Code Fragment
        
        return true;
    }
    private boolean persistQuestionCriteria(NodeCore core, TypeQuestion tq, Connector conn)
    {
        TypeQuestion.CriteriaPersistStatus cps = tq.persistCriterias(conn);
        if(cps != TypeQuestion.CriteriaPersistStatus.Success)
        {
            core.getLogging().log(LOGGING_ALIAS, "Failed to persist criteria-types for question-type '"+tq.getTitle()+"' (persist status: '"+cps.name()+"') during installation!", Logging.EntryType.Error);
            return false;
        }
        else
            return true;
    }
    private TypeQuestion registerTypeQuestion(Connector conn, NodeCore core, UUID uuid, String title, String description)
    {
        TypeQuestion tq = new TypeQuestion(uuid, this.getUUID(), title, description);
        TypeQuestion.PersistStatus psq = tq.persist(conn);
        if(psq != TypeQuestion.PersistStatus.Success)
        {
            core.getLogging().log(LOGGING_ALIAS, "Failed to register type-question '"+title+"' during installation!", Logging.EntryType.Error);
            return null;
        }
        return tq;
    }
    private TypeCriteria registerTypeCriteria(Connector conn, NodeCore core, UUID uuid, String title, String description)
    {
        TypeCriteria tc = new TypeCriteria(uuid, this.getUUID(), title, description);
        TypeCriteria.PersistStatus psc = tc.persist(conn);
        if(psc != TypeCriteria.PersistStatus.Success)
        {
            core.getLogging().log(LOGGING_ALIAS, "Failed to register type-criteria '"+title+"' during installation!", Logging.EntryType.Error);
            return null;
        }
        return tc;
    }
    @Override
    public boolean eventHandler_pluginUninstall(NodeCore core, Connector conn)
    {
        // Remove question types
        
        // Remove criteria types
        return true;
    }
    @Override
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Unload templates
        core.getTemplates().remove(this);
    }
    @Override
    public boolean eventHandler_registerTemplates(NodeCore core, TemplateManager manager)
    {
        if(!manager.load(this, "templates"))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_handleHook(String event, Object[] args)
    {
        switch(event)
        {
            case "question_type.web_edit":
                return pageQuestionEdit(args);
            case "criteria_type.web_edit":
                return pageCriteriaEdit(args);
            case "question_type.question_capture":
                return pageQuestionCapture(args);
            case "criteria_type.mark":
                return criteriaMarking(args);
            case "question_type.question_display":
                return pageQuestionDisplay(args);
        }
        return false;
    }
    @Override
    public String getTitle()
    {
        return "PALS: Default Question-Criteria Handler";
    }
    // Methods - Pages - Edit - Question Types *********************************
    private boolean pageQuestionEdit(Object[] hookData)
    {
        if(hookData.length != 2 || !(hookData[0] instanceof WebRequestData) && !(hookData[1] instanceof Question))
            return false;
        WebRequestData data = (WebRequestData)hookData[0];
        Question q = (Question)hookData[1];
        // Delegate to question-type handler
        UUID qtype = q.getQtype().getUuidQType();
        if(qtype.equals(UUID_QUESTIONTYPE_MULTIPLECHOICE))
            return pageQuestionEdit_multipleChoice(data, q);
        else if(qtype.equals(UUID_QUESTIONTYPE_WRITTENRESPONSE))
            return pageQuestionEdit_writtenResponse(data, q);
        else if(qtype.equals(UUID_QUESTIONTYPE_CODEFRAGMENT))
        {
        }
        else if(qtype.equals(UUID_QUESTIONTYPE_CODEUPLOAD))
        {
        }
        return false;
    }
    private boolean pageQuestionEdit_multipleChoice(WebRequestData data, Question q)
    {
        // Load question data
        Data_Question_MultipleChoice qdata;
        if(q.getData() != null)
            qdata = q.getData();
        else
            qdata = new Data_Question_MultipleChoice();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String mcText = req.getField("mc_text");
        String mcSingleAnswer = req.getField("mc_single_answer");
        String mcAnswers = req.getField("mc_answers");
        if(mcText != null && mcAnswers != null)
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Update question data
                qdata.setText(mcText);
                qdata.setSingleAnswer(mcSingleAnswer != null && mcSingleAnswer.equals("1"));
                qdata.setAnswers(mcAnswers.replace("\r", "").split("\n"));
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
        data.setTemplateData("pals_content", "defaultqch/questions/multiplechoice_edit");
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("mc_text", mcText != null ? mcText : qdata.getText());
        if((mcSingleAnswer != null && mcSingleAnswer.equals("1")) || (mcText == null && qdata.isSingleAnswer()))
            data.setTemplateData("mc_single_answer", true);
        data.setTemplateData("mc_answers", mcAnswers != null ? mcAnswers : qdata.getAnswersWebFormat());
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageQuestionEdit_writtenResponse(WebRequestData data, Question q)
    {
        // Load question data
        Data_Question_Written qdata;
        if(q.getData() != null)
            qdata = q.getData();
        else
            qdata = new Data_Question_Written();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String questionText = req.getField("question_text");
        if(questionText != null)
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Update data model
                qdata.setText(questionText);
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
        data.setTemplateData("pals_content", "defaultqch/questions/written_response_edit");
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("question_text", questionText != null ? questionText : qdata.getText());
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    // Methods - Pages - Edit - Criteria Types *********************************
    private boolean pageCriteriaEdit(Object[] hookData)
    {
        if(hookData.length != 2 || !(hookData[0] instanceof WebRequestData) && !(hookData[1] instanceof QuestionCriteria))
            return false;
        WebRequestData data = (WebRequestData)hookData[0];
        QuestionCriteria qc = (QuestionCriteria)hookData[1];
        // Delegate to question-type handler
        UUID ctype = qc.getCriteria().getUuidCType();
        if(ctype.equals(UUID_CRITERIATYPE_MANUALMARKING))
            return pageCriteriaEdit_manual(data, qc);
        else if(ctype.equals(UUID_CRITERIATYPE_TEXTMATCH))
            return pageCriteriaEdit_textMatch(data, qc);
        else if(ctype.equals(UUID_CRITERIATYPE_REGEXMATCH))
            return pageCriteriaEdit_regexMatch(data, qc);
        else if(ctype.equals(UUID_CRITERIATYPE_JAVALOC))
        {
        }
        else if(ctype.equals(UUID_CRITERIATYPE_JAVACONTAINSCLASS))
        {
        }
        else if(ctype.equals(UUID_CRITERIATYPE_JAVACONTAINSMETHOD))
        {
        }
        return false;
    }
    private boolean pageCriteriaEdit_manual(WebRequestData data, QuestionCriteria qc)
    {
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        if(critTitle != null && critWeight != null)
        {
            // Validate security
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                try
                {
                    int weight = Integer.parseInt(critWeight);
                    // Update qc model
                    qc.setTitle(critTitle);
                    qc.setWeight(weight);
                    // Persist qc model
                    QuestionCriteria.PersistStatus qcps = qc.persist(data.getConnector());
                    switch(qcps)
                    {
                        case Failed:
                        case Failed_Serialize:
                        case Invalid_Criteria:
                        case Invalid_Question:
                            data.setTemplateData("error", "Failed to update model due to an unknown error ('"+qcps.name()+"'); try again or contact an administrator!");
                            break;
                        case Invalid_Title:
                            data.setTemplateData("error", "Invalid title; must be "+qc.getTitleMin()+" to "+qc.getTitleMax()+" characters in length!");
                            break;
                        case Invalid_Weight:
                            data.setTemplateData("error", "Invalid weight; must be numeric and greater than zero!");
                            break;
                        case Success:
                            data.setTemplateData("success", "Updated criteria settings successfully.");
                            break;
                    }
                }
                catch(NumberFormatException ex)
                {
                    data.setTemplateData("error", "Weight must be numeric!");
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/manual_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        
        return true;
    }
    private boolean pageCriteriaEdit_textMatch(WebRequestData data, QuestionCriteria qc)
    {
        // Load criteria data
        Data_Criteria_TextMatch cdata;
        if(qc.getData() != null)
            cdata = (Data_Criteria_TextMatch)qc.getData();
        else
            cdata = new Data_Criteria_TextMatch();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        String critMatch = req.getField("crit_match");
        // -- Optional
        String critSensitive = req.getField("crit_sensitive");
        if(critTitle != null && critWeight != null && critMatch != null)
        {
            // Validate security
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                try
                {
                    int weight = Integer.parseInt(critWeight);
                    // Update the data model
                    cdata.setText(critMatch);
                    cdata.setCaseSensitive(critSensitive != null && critSensitive.equals("1"));
                    // Update qc model
                    qc.setData(cdata);
                    qc.setTitle(critTitle);
                    qc.setWeight(weight);
                    // Persist qc model
                    QuestionCriteria.PersistStatus qcps = qc.persist(data.getConnector());
                    switch(qcps)
                    {
                        case Failed:
                        case Failed_Serialize:
                        case Invalid_Criteria:
                        case Invalid_Question:
                            data.setTemplateData("error", "Failed to update model due to an unknown error ('"+qcps.name()+"'); try again or contact an administrator!");
                            break;
                        case Invalid_Title:
                            data.setTemplateData("error", "Invalid title; must be "+qc.getTitleMin()+" to "+qc.getTitleMax()+" characters in length!");
                            break;
                        case Invalid_Weight:
                            data.setTemplateData("error", "Invalid weight; must be numeric and greater than zero!");
                            break;
                        case Success:
                            data.setTemplateData("success", "Updated criteria settings successfully.");
                            break;
                    }
                }
                catch(NumberFormatException ex)
                {
                    data.setTemplateData("error", "Weight must be numeric!");
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/textmatch_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_match", critMatch != null ? critMatch : cdata.getText());
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        if((critMatch == null && cdata.isCaseSensitive()) || (critSensitive != null && critSensitive.equals("1")))
            data.setTemplateData("crit_sensitive", cdata.isCaseSensitive());
        
        return true;
    }
    private boolean pageCriteriaEdit_regexMatch(WebRequestData data, QuestionCriteria qc)
    {
        // Load criteria data
        Data_Criteria_Regex cdata;
        if(qc.getData() != null)
            cdata = (Data_Criteria_Regex)qc.getData();
        else
            cdata = new Data_Criteria_Regex();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        String critRegex = req.getField("crit_regex");
        // -- Optional
        String critMultiline = req.getField("crit_multiline");
        String critCase = req.getField("crit_case");
        String critDotall = req.getField("crit_dotall");
        if(critRegex != null && critTitle != null && critWeight != null)
        {
            try
            {
                int weight = Integer.parseInt(critWeight);
                // Validate security
                if(!CSRF.isSecure(data))
                    data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
                else
                {
                    // Test compiling the regex pattern
                    // -- Exception is caught by try-catch
                    Pattern.compile(critRegex);
                    // Update data model
                    cdata.setMode(
                            (critMultiline != null && critMultiline.equals("1") ? Pattern.MULTILINE : 0) | (critCase != null && critCase.equals("1") ? Pattern.CASE_INSENSITIVE : 0) | (critDotall != null && critDotall.equals("1") ? Pattern.DOTALL : 0)
                    );
                    cdata.setRegexPattern(critRegex);
                    // Update question criteria model
                    qc.setData(cdata);
                    qc.setWeight(weight);
                    qc.setTitle(critTitle);
                    QuestionCriteria.PersistStatus qcps = qc.persist(data.getConnector());
                    switch(qcps)
                    {
                        case Failed:
                        case Failed_Serialize:
                        case Invalid_Criteria:
                        case Invalid_Question:
                            data.setTemplateData("error", "Failed to update model due to an unknown error ('"+qcps.name()+"'); try again or contact an administrator!");
                            break;
                        case Invalid_Title:
                            data.setTemplateData("error", "Invalid title; must be "+qc.getTitleMin()+" to "+qc.getTitleMax()+" characters in length!");
                            break;
                        case Invalid_Weight:
                            data.setTemplateData("error", "Invalid weight; must be numeric and greater than zero!");
                            break;
                        case Success:
                            data.setTemplateData("success", "Updated criteria settings successfully.");
                            break;
                    }
                }
            }
            catch(NumberFormatException ex)
            {
                data.setTemplateData("error", "Weight must be numeric!");
            }
            catch(PatternSyntaxException ex)
            {
                data.setTemplateData("error", "Regex pattern cannot compile: '"+ex.getMessage()+"'!");
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/regex_edit");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_regex", critRegex != null ? critRegex : cdata.getRegexPattern());
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        
        // -- Note: critRegex is used to test for postback; if a box was previously selected, unselecting and posting-back may cause it to change state
        if( (critRegex == null && ((cdata.getMode() & Pattern.MULTILINE) == Pattern.MULTILINE)) || (critMultiline != null && critMultiline.equals("1")))
            data.setTemplateData("crit_multiline", data);
        if( (critRegex == null && ((cdata.getMode() & Pattern.CASE_INSENSITIVE) == Pattern.CASE_INSENSITIVE)) || (critCase != null && critCase.contains("1")))
            data.setTemplateData("crit_case", data);
        if( (critRegex == null && ((cdata.getMode() & Pattern.DOTALL) == Pattern.DOTALL)) || (critDotall != null && critDotall.equals("1")))
            data.setTemplateData("crit_dotall", data);
        
        return true;
    }
    // Methods - Pages - Question Types - Capture ******************************
    private boolean pageQuestionCapture(Object[] hookData)
    {
        // Validate hook-data
        if(hookData.length != 6 || !(hookData[0] instanceof WebRequestData) || !(hookData[1] instanceof InstanceAssignment) || !(hookData[2] instanceof AssignmentQuestion) || (!(hookData[3] instanceof InstanceAssignmentQuestion) && hookData[3] != null) || !(hookData[4] instanceof StringBuilder) || !(hookData[5] instanceof Boolean))
            return false;
        // Parse hook-data
        WebRequestData data = (WebRequestData)hookData[0];
        InstanceAssignment ia = (InstanceAssignment)hookData[1];
        AssignmentQuestion question = (AssignmentQuestion)hookData[2];
        InstanceAssignmentQuestion iaq = (InstanceAssignmentQuestion)hookData[3];
        StringBuilder html = (StringBuilder)hookData[4];
        boolean secure = (Boolean)hookData[5];
        // Create unpersisted instance-data model if unset/null
        if(iaq == null)
            iaq = new InstanceAssignmentQuestion(question, ia, null, false, 0);
        // Delegate to be rendered and question-data captured
        UUID qtype = question.getQuestion().getQtype().getUuidQType();
        if(qtype.equals(UUID_QUESTIONTYPE_MULTIPLECHOICE))
            return pageQuestionCapture_multipleChoice(data, ia, iaq, html, secure);
        else if(qtype.equals(UUID_QUESTIONTYPE_WRITTENRESPONSE))
            return pageQuestionCapture_writtenResponse(data, ia, iaq, html, secure);
        else if(qtype.equals(UUID_QUESTIONTYPE_CODEUPLOAD))
            ;
        else if(qtype.equals(UUID_QUESTIONTYPE_CODEFRAGMENT))
            ;
        return false;
    }
    private boolean pageQuestionCapture_writtenResponse(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure)
    {
        // Load question data
        Data_Question_Written qdata = (Data_Question_Written)iaq.getAssignmentQuestion().getQuestion().getData();
        // Load answer data
        String adata = (String)iaq.getData();
        // Check postback
        int aqid = iaq.getAssignmentQuestion().getAQID();
        String answer = data.getRequestData().getField("written_response_"+aqid);
        HashMap<String,Object> kvs = new HashMap<>();
        if(secure && answer != null)
        {
            // Update the iaq model and persist
            iaq.setData(answer);
            iaq.setAnswered(true);
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
        // Render the template
        kvs.put("text", qdata != null ? qdata.getText() : "No question defined...");
        kvs.put("answer", answer != null ? answer : adata);
        kvs.put("aqid", aqid);
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/written_response_capture"));
        return true;
    }
    private boolean pageQuestionCapture_multipleChoice(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure)
    {
        // Load question data
        Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iaq.getAssignmentQuestion().getQuestion().getData();
        // Load answer data
        Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iaq.getData();
        // -- New attempt; create random indexes and persist...
        if(adata == null)
        {
            adata = new Data_Answer_MultipleChoice(data.getCore().getRNG(), qdata);
            iaq.setData(adata);
            iaq.persist(data.getConnector());
        }
        // Check postback
        int aqid = iaq.getAssignmentQuestion().getAQID();
        String pb = data.getRequestData().getField("multiple_choice_pb_"+aqid);
        HashMap<String,Object> kvs = new HashMap<>();
        if(secure && pb != null)
        {
            // Process answers
            adata.processAnswers(aqid, data.getRequestData(), qdata);
            // Update the iaq model and persist
            iaq.setData(adata);
            iaq.setAnswered(true);
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
        // Render the template
        kvs.put("text", qdata != null ? qdata.getText() : "No question defined...");
        kvs.put("choices", adata.getViewModels(aqid, data.getRequestData(), qdata, pb != null));
        if(qdata.isSingleAnswer())
            kvs.put("single_choice", true);
        kvs.put("aqid", aqid);
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/multiplechoice_capture"));
        return true;
    }
    // Methods - Pages - Question Types - Display ******************************
    private boolean pageQuestionDisplay(Object[] hookData)
    {
        // Validate hook-data
        if(hookData.length != 7 || !(hookData[0] instanceof WebRequestData) || !(hookData[1] instanceof InstanceAssignment) || !(hookData[2] instanceof AssignmentQuestion) || (!(hookData[3] instanceof InstanceAssignmentQuestion) && hookData[3] != null) || !(hookData[4] instanceof StringBuilder) || !(hookData[5] instanceof Boolean) && !(hookData[6] instanceof Boolean))
            return false;
        // Parse hook-data
        WebRequestData data = (WebRequestData)hookData[0];
        InstanceAssignment ia = (InstanceAssignment)hookData[1];
        AssignmentQuestion question = (AssignmentQuestion)hookData[2];
        InstanceAssignmentQuestion iaq = (InstanceAssignmentQuestion)hookData[3];
        StringBuilder html = (StringBuilder)hookData[4];
        boolean secure = (Boolean)hookData[5];
        boolean editMode = (Boolean)hookData[6];
        // Create unpersisted instance-data model if unset/null
        if(iaq == null)
            iaq = new InstanceAssignmentQuestion(question, ia, null, false, 0);
         // Delegate to be rendered
        UUID qtype = question.getQuestion().getQtype().getUuidQType();
        if(qtype.equals(UUID_QUESTIONTYPE_MULTIPLECHOICE))
            return pagepageQuestionDisplay_multipleChoice(data, ia, iaq, html, secure, editMode);
        else if(qtype.equals(UUID_QUESTIONTYPE_WRITTENRESPONSE))
            return pagepageQuestionDisplay_writtenResponse(data, ia, iaq, html, secure, editMode);
        else if(qtype.equals(UUID_QUESTIONTYPE_CODEUPLOAD))
            ;
        else if(qtype.equals(UUID_QUESTIONTYPE_CODEFRAGMENT))
            ;
        return false;
    }
    private boolean pagepageQuestionDisplay_writtenResponse(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure, boolean editMode)
    {
        // Load question data
        Data_Question_Written qdata = (Data_Question_Written)iaq.getAssignmentQuestion().getQuestion().getData();
        // Load answer data
        String adata = (String)iaq.getData();
        // Render the template
        HashMap<String,Object> kvs = new HashMap<>();
        kvs.put("text", qdata.getText());
        kvs.put("answer", adata != null ? adata : "");
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/written_response_display"));
        return true;
    }
    private boolean pagepageQuestionDisplay_multipleChoice(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, StringBuilder html, boolean secure, boolean editMode)
    {
        // Load question data
        Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iaq.getAssignmentQuestion().getQuestion().getData();
        // Load answer data
        Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iaq.getData();
        // Render the template
        HashMap<String,Object> kvs = new HashMap<>();
        kvs.put("text", qdata.getText());
        kvs.put("answers", adata != null ? adata.getAnswers(qdata) : new String[0]);
        html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/questions/multiplechoice_display"));
        return true;
    }
    // Methods - Criteria Types - Marking **************************************
    private boolean criteriaMarking(Object[] hookData)
    {
        // Parse hook data
        if(hookData.length != 2 || !(hookData[0] instanceof Connector) || !(hookData[1] instanceof InstanceAssignmentCriteria))
            return false;
        Connector conn = (Connector)hookData[0];
        InstanceAssignmentCriteria iac = (InstanceAssignmentCriteria)hookData[1];
        // Delegate to the correct method for marking
        UUID ctype = iac.getQC().getCriteria().getUuidCType();
        if(ctype.equals(UUID_CRITERIATYPE_MANUALMARKING))
            return criteriaMarking_manualMarking(conn, iac);
        else if(ctype.equals(UUID_CRITERIATYPE_TEXTMATCH))
            return criteriaMarking_textMatch(conn, iac);
        else if(ctype.equals(UUID_CRITERIATYPE_REGEXMATCH))
            return criteriaMarking_regexMatch(conn, iac);
        else if(ctype.equals(UUID_CRITERIATYPE_JAVALOC))
            ;
        else if(ctype.equals(UUID_CRITERIATYPE_JAVACONTAINSCLASS))
            ;
        else if(ctype.equals(UUID_CRITERIATYPE_JAVACONTAINSMETHOD))
            ;
        return false;
    }
    private boolean criteriaMarking_manualMarking(Connector conn, InstanceAssignmentCriteria iac)
    {
        // This method should never be actually reached
        // -- Fail-safe: reset status to manualmarking
        iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
        return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
    }
    private boolean criteriaMarking_textMatch(Connector conn, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
        {
            iac.setMark(0);
            return true;
        }
        else
        {
            UUID qtype = iac.getIAQ().getAssignmentQuestion().getQuestion().getQtype().getUuidQType();
            Data_Criteria_TextMatch cdata = (Data_Criteria_TextMatch)iac.getQC().getData();
            String match = cdata.isCaseSensitive() ? cdata.getText() : cdata.getText().toLowerCase();
            boolean matched = false;
            if(qtype.equals(UUID_QUESTIONTYPE_WRITTENRESPONSE))
            {
                
                String text = (String)iac.getIAQ().getData();
                if(!cdata.isCaseSensitive())
                    text = text.toLowerCase();
                matched = text.equals(match);
            }
            else if(qtype.equals(UUID_QUESTIONTYPE_MULTIPLECHOICE))
            {
                Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iac.getQC().getQuestion().getData();
                Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iac.getIAQ().getData();
                String[] text = adata.getAnswers(qdata);
                for(String t : text)
                {
                    if((cdata.isCaseSensitive() ? t : t.toLowerCase()).equals(match))
                    {
                        matched = true;
                        break;
                    }
                }
            }
            else
                return false;
            // Update and persist the mark
            iac.setMark(matched ? 100 : 0);
            iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
            return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
        }
    }
    private boolean criteriaMarking_regexMatch(Connector conn, InstanceAssignmentCriteria iac)
    {
        if(!iac.getIAQ().isAnswered())
        {
            iac.setMark(0);
            return true;
        }
        else
        {
            Data_Criteria_Regex cdata = (Data_Criteria_Regex)iac.getQC().getData();
            // Compile regex pattern
            Pattern p;
            try
            {
                p = Pattern.compile(cdata.getRegexPattern(), cdata.getMode());
            }
            catch(PatternSyntaxException ex)
            {
                getCore().getLogging().logEx("Default QC", "Could not compile regex pattern for question-criteria '"+iac.getQC().getQCID()+"' (QCID).", ex, Logging.EntryType.Warning);
                iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
            }
            // Perform matching
            UUID qtype = iac.getIAQ().getAssignmentQuestion().getQuestion().getQtype().getUuidQType();
            boolean matched = false;
            if(qtype.equals(UUID_QUESTIONTYPE_MULTIPLECHOICE))
            {
                Data_Question_MultipleChoice qdata = (Data_Question_MultipleChoice)iac.getQC().getQuestion().getData();
                Data_Answer_MultipleChoice adata = (Data_Answer_MultipleChoice)iac.getIAQ().getData();
                String[] text = adata.getAnswers(qdata);
                for(String t : text)
                {
                    if(p.matcher(t).matches())
                    {
                        matched = true;
                        break;
                    }
                }
            }
            else if(qtype.equals(UUID_QUESTIONTYPE_WRITTENRESPONSE))
            {
                String text = (String)iac.getIAQ().getData();
                matched = p.matcher(text).matches();
            }
            else
                return false;
            // Update and persist the mark
            iac.setMark(matched ? 100 : 0);
            iac.setStatus(InstanceAssignmentCriteria.Status.Marked);
            return iac.persist(conn) == InstanceAssignmentCriteria.PersistStatus.Success;
        }
    }
}
