package pals.plugins.handlers;

import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.UUID;
import pals.base.assessment.TypeCriteria;
import pals.base.assessment.TypeQuestion;
import pals.base.database.Connector;
import pals.base.utils.JarIO;

/**
 * A plugin for the default questions and criteria types.
 */
public class DefaultQC extends Plugin
{
    // Fields - Constants ******************************************************
    private static final String LOGGING_ALIAS = "Default QC";
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
        if((tqMultipleChoice = registerTypeQuestion(conn, core, UUID.parse("f38c3f28-4f63-49b5-994c-fd618c654de0"), "Multiple Choice", "Allows students to pick an answer, or multiple answers, from a set of possible answers.")) == null)
            return false;
        if((tqWrittenResponse = registerTypeQuestion(conn, core, UUID.parse("fcfec4ec-3347-4219-a988-f69b632b657e"), "Written Response", "Allows students to provide a written response.")) == null)
            return false;
        if((tqCodeUpload = registerTypeQuestion(conn, core, UUID.parse("7905af64-06b3-4fc2-b69b-448b97627ca5"), "Code Upload", "Allows students to upload code to be assessed.")) == null)
            return false;
        if((tqCodeFragment = registerTypeQuestion(conn, core, UUID.parse("8740706a-37f6-49b0-af12-b21621cbf101"), "Code Fragment", "Allows students to answer with a code-fragment and compare the output with the correct answer.")) == null)
            return false;
        // Register criteria types
        TypeCriteria    tcManualMarking,
                        tcTextMatch,
                        tcRegexMatch,
                        tcJavaLOC,
                        tcJavaContainsClass,
                        tcJavaContainsMethod;
        if((tcManualMarking = registerTypeCriteria(conn, core, UUID.parse("03830aa9-39d7-4bfe-9ab5-a9c765e6e426"), "Manual Marking", "Used by a lecturer to manually-mark a criteria of a question.")) == null)
            return false;
        if((tcTextMatch = registerTypeCriteria(conn, core, UUID.parse("b9a1143c-98cb-446b-9b39-42addac71f4f"), "Text Match", "Gives marks for an answer matching a piece of text.")) == null)
            return false;
        if((tcRegexMatch = registerTypeCriteria(conn, core, UUID.parse("3e6518e8-bb13-4878-bb4c-c0d687ad2e6e"), "Regex Match", "Gives marks based on a regex match.")) == null)
            return false;
        if((tcJavaLOC = registerTypeCriteria(conn, core, UUID.parse("13fdc5fe-ce6f-4203-93af-a560d80ed5a4"), "Java: Lines of Code", "Gives marks based on the lines-of-code.")) == null)
            return false;
        if((tcJavaContainsClass = registerTypeCriteria(conn, core, UUID.parse("f0f16511-1187-4b34-8dd1-cb014b28b220"), "Java: Contains Class", "Gives marks for the definition of a class.")) == null)
            return false;
        if((tcJavaContainsMethod = registerTypeCriteria(conn, core, UUID.parse("ed121829-7380-463c-a386-39441ea02539"), "Java: Contains Method", "Gives marks for the definition of a method.")) == null)
            return false;
        // Add criterias to questions, and persist
        // -- Multiple Choice
        tqMultipleChoice.criteriaAdd(tcManualMarking);
        tqMultipleChoice.criteriaAdd(tcRegexMatch);
        if(!persistQuestionCriteria(core, tqMultipleChoice, conn))
            return false;
        // -- Written Response
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
    public boolean eventHandler_handleHook(String event, Object[] args)
    {
        switch(event)
        {
            case "handle_question_setup":
                break;
            case "handle_criteria":
                break;
        }
        return false;
    }
    @Override
    public String getTitle()
    {
        return "PALS: Default Question-Criteria Handler";
    }
}
