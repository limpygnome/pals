package pals.plugins.handlers.defaultqch;

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
import pals.base.web.WebRequestData;
import pals.plugins.handlers.defaultqch.Handler_Question_MCQ;

/**
 * A plugin for the default questions and criteria types.
 */
public class DefaultQC extends Plugin
{
    // Fields - Constants ******************************************************
    private static final String LOGGING_ALIAS = "Default QC";
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
                        tqWrittenResponse;
        // Register the types of questions
        if((tqMultipleChoice = registerTypeQuestion(conn, core, Handler_Question_MCQ.UUID_QTYPE, "Multiple Choice", "Allows students to pick an answer, or multiple answers, from a set of possible answers.")) == null)
            return false;
        if((tqWrittenResponse = registerTypeQuestion(conn, core, Handler_Question_WrittenResponse.UUID_QTYPE, "Written Response", "Allows students to provide a written response.")) == null)
            return false;
        //if((tqCodeUpload = registerTypeQuestion(conn, core, UUID_QUESTIONTYPE_CODEUPLOAD, "Code Upload", "Allows students to upload code to be assessed.")) == null)
        //    return false;
        //if((tqCodeFragment = registerTypeQuestion(conn, core, UUID_QUESTIONTYPE_CODEFRAGMENT, "Code Fragment", "Allows students to answer with a code-fragment and compare the output with the correct answer.")) == null)
        //    return false;
        // Register criteria types
        TypeCriteria    tcManualMarking,
                        tcTextMatch,
                        tcRegexMatch,
                        tcMultipleChoice;
        if((tcManualMarking = registerTypeCriteria(conn, core, Handler_Criteria_ManualMarking.UUID_CTYPE, "Manual Marking", "Used by a lecturer to manually-mark a criteria of a question.")) == null)
            return false;
        if((tcTextMatch = registerTypeCriteria(conn, core, Handler_Criteria_TextMatch.UUID_CTYPE, "Text Match", "Gives marks for an answer matching a piece of text.")) == null)
            return false;
        if((tcRegexMatch = registerTypeCriteria(conn, core, Handler_Criteria_RegexMatch.UUID_CTYPE, "Regex Match", "Gives marks based on a regex match.")) == null)
            return false;
        if((tcMultipleChoice = registerTypeCriteria(conn, core, Handler_Criteria_MultipleChoice.UUID_CTYPE, "Multiple Choice", "Gives marks based on the same choices selected.")) == null)
            return false;
        // Add criterias to questions, and persist
        // -- Multiple Choice
        tqMultipleChoice.criteriaAdd(tcManualMarking);
        tqMultipleChoice.criteriaAdd(tcRegexMatch);
        tqMultipleChoice.criteriaAdd(tcTextMatch);
        tqMultipleChoice.criteriaAdd(tcRegexMatch);
        tqMultipleChoice.criteriaAdd(tcMultipleChoice);
        if(!persistQuestionCriteria(core, tqMultipleChoice, conn))
            return false;
        // -- Written Response
        tqWrittenResponse.criteriaAdd(tcManualMarking);
        tqWrittenResponse.criteriaAdd(tcRegexMatch);
        tqWrittenResponse.criteriaAdd(tcTextMatch);
        if(!persistQuestionCriteria(core, tqWrittenResponse, conn))
            return false;
        
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
            case "criteria_type.display_feedback":
                return criteriaFeedback(args);
        }
        return false;
    }
    @Override
    public String getTitle()
    {
        return "PALS: Default Question-Criteria Handler";
    }
    // Methods - Pages *********************************************************
    private boolean pageQuestionEdit(Object[] hookData)
    {
        if(hookData.length != 2 || !(hookData[0] instanceof WebRequestData) && !(hookData[1] instanceof Question))
            return false;
        WebRequestData data = (WebRequestData)hookData[0];
        Question q = (Question)hookData[1];
        // Delegate to question-type handler
        UUID qtype = q.getQtype().getUuidQType();
        if(qtype.equals(Handler_Question_MCQ.UUID_QTYPE))
            return Handler_Question_MCQ.pageQuestionEdit_multipleChoice(data, q);
        else if(qtype.equals(Handler_Question_WrittenResponse.UUID_QTYPE))
            return Handler_Question_WrittenResponse.pageQuestionEdit_writtenResponse(data, q);
        return false;
    }
    private boolean pageCriteriaEdit(Object[] hookData)
    {
        if(hookData.length != 2 || !(hookData[0] instanceof WebRequestData) && !(hookData[1] instanceof QuestionCriteria))
            return false;
        WebRequestData data = (WebRequestData)hookData[0];
        QuestionCriteria qc = (QuestionCriteria)hookData[1];
        // Delegate to question-type handler
        UUID ctype = qc.getCriteria().getUuidCType();
        if(ctype.equals(Handler_Criteria_ManualMarking.UUID_CTYPE))
            return Handler_Criteria_ManualMarking.pageCriteriaEdit(data, qc);
        else if(ctype.equals(Handler_Criteria_TextMatch.UUID_CTYPE))
            return Handler_Criteria_TextMatch.pageCriteriaEdit(data, qc);
        else if(ctype.equals(Handler_Criteria_RegexMatch.UUID_CTYPE))
            return Handler_Criteria_RegexMatch.pageCriteriaEdit(data, qc);
        else if(ctype.equals(Handler_Criteria_MultipleChoice.UUID_CTYPE))
            return Handler_Criteria_MultipleChoice.pageCriteriaEdit(data, qc);
        return false;
    }
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
        if(qtype.equals(Handler_Question_MCQ.UUID_QTYPE))
            return Handler_Question_MCQ.pageQuestionCapture_multipleChoice(data, ia, iaq, html, secure);
        else if(qtype.equals(Handler_Question_WrittenResponse.UUID_QTYPE))
            return Handler_Question_WrittenResponse.pageQuestionCapture_writtenResponse(data, ia, iaq, html, secure);
        return false;
    }
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
        if(qtype.equals(Handler_Question_MCQ.UUID_QTYPE))
            return Handler_Question_MCQ.pagepageQuestionDisplay_multipleChoice(data, ia, iaq, html, secure, editMode);
        else if(qtype.equals(Handler_Question_WrittenResponse.UUID_QTYPE))
            return Handler_Question_WrittenResponse.pagepageQuestionDisplay_writtenResponse(data, ia, iaq, html, secure, editMode);
        return false;
    }
    // Methods - Criteria ******************************************************
    private boolean criteriaMarking(Object[] hookData)
    {
        // Parse hook data
        if(hookData.length != 2 || !(hookData[0] instanceof Connector) || !(hookData[1] instanceof InstanceAssignmentCriteria))
            return false;
        Connector conn = (Connector)hookData[0];
        InstanceAssignmentCriteria iac = (InstanceAssignmentCriteria)hookData[1];
        // Delegate to the correct method for marking
        UUID ctype = iac.getQC().getCriteria().getUuidCType();
        if(ctype.equals(Handler_Criteria_ManualMarking.UUID_CTYPE))
            return Handler_Criteria_ManualMarking.criteriaMarking(conn, iac);
        else if(ctype.equals(Handler_Criteria_TextMatch.UUID_CTYPE))
            return Handler_Criteria_TextMatch.criteriaMarking(conn, iac);
        else if(ctype.equals(Handler_Criteria_RegexMatch.UUID_CTYPE))
            return Handler_Criteria_RegexMatch.criteriaMarking(getCore(), conn, iac);
        else if(ctype.equals(Handler_Criteria_MultipleChoice.UUID_CTYPE))
            return Handler_Criteria_MultipleChoice.criteriaMarking(conn, iac);
        return false;
    }
    private boolean criteriaFeedback(Object[] hookData)
    {
        // Validate and parse hook-data
        if(hookData.length != 5 || !(hookData[0] instanceof WebRequestData) || !(hookData[1] instanceof InstanceAssignment) || !(hookData[2] instanceof InstanceAssignmentQuestion) || !(hookData[3] instanceof InstanceAssignmentCriteria) || !(hookData[4] instanceof StringBuilder))
            return false;
        WebRequestData data = (WebRequestData)hookData[0];
        InstanceAssignment ia = (InstanceAssignment)hookData[1];
        InstanceAssignmentQuestion iaq = (InstanceAssignmentQuestion)hookData[2];
        InstanceAssignmentCriteria iac = (InstanceAssignmentCriteria)hookData[3];
        StringBuilder html = (StringBuilder)hookData[4];
        // Delegate to the suitable method based on criteria UUID
        UUID ctype = iac.getQC().getCriteria().getUuidCType();
        if(ctype.equals(Handler_Criteria_TextMatch.UUID_CTYPE))
            return Handler_Criteria_TextMatch.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(Handler_Criteria_RegexMatch.UUID_CTYPE))
            return Handler_Criteria_RegexMatch.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(Handler_Criteria_ManualMarking.UUID_CTYPE))
            ;
        else if(ctype.equals(Handler_Criteria_MultipleChoice.UUID_CTYPE))
            return Handler_Criteria_MultipleChoice.criteriaDisplay(data, ia, iaq, iac, html);
        return false;
    }
}
