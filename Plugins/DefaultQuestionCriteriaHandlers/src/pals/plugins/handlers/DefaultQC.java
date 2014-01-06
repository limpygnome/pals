package pals.plugins.handlers;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.assessment.Question;
import pals.base.assessment.QuestionCriteria;
import pals.base.assessment.TypeCriteria;
import pals.base.assessment.TypeQuestion;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
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
        }
        return false;
    }
    @Override
    public String getTitle()
    {
        return "PALS: Default Question-Criteria Handler";
    }
    // Methods - Pages - Question Types ****************************************
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
                qdata.text = mcText;
                qdata.singleAnswer = mcSingleAnswer != null && mcSingleAnswer.equals("1");
                qdata.answers = mcAnswers.replace("\r", "").split("\n");
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
        data.setTemplateData("mc_text", mcText != null ? mcText : qdata.text);
        data.setTemplateData("mc_single_answer", (mcSingleAnswer != null && mcSingleAnswer.equals("1")) || qdata.singleAnswer);
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
    // Methods - Pages - Criteria Types ****************************************
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
                    cdata.mode = (critMultiline != null && critMultiline.equals("1") ? Pattern.MULTILINE : 0) | (critCase != null && critCase.equals("1") ? Pattern.CASE_INSENSITIVE : 0) | (critDotall != null && critDotall.equals("1") ? Pattern.DOTALL : 0);
                    cdata.regexPattern = critRegex;
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
        data.setTemplateData("crit_regex", critRegex != null ? critRegex : cdata.regexPattern);
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        
        // -- Note: critRegex is used to test for postback; if a box was previously selected, unselecting and posting-back may cause it to change state
        if( (critRegex == null && ((cdata.mode & Pattern.MULTILINE) == Pattern.MULTILINE)) || (critMultiline != null && critMultiline.equals("1")))
            data.setTemplateData("crit_multiline", data);
        if( (critRegex == null && ((cdata.mode & Pattern.CASE_INSENSITIVE) == Pattern.CASE_INSENSITIVE)) || (critCase != null && critCase.contains("1")))
            data.setTemplateData("crit_case", data);
        if( (critRegex == null && ((cdata.mode & Pattern.DOTALL) == Pattern.DOTALL)) || (critDotall != null && critDotall.equals("1")))
            data.setTemplateData("crit_dotall", data);
        
        return true;
    }
}
