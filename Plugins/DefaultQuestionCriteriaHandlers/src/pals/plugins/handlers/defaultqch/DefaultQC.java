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
package pals.plugins.handlers.defaultqch;

import java.util.regex.Pattern;
import pals.plugins.handlers.defaultqch.questions.*;
import pals.plugins.handlers.defaultqch.criterias.*;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.Version;
import pals.base.WebManager;
import pals.base.assessment.Assignment;
import pals.base.assessment.AssignmentQuestion;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Module;
import pals.base.assessment.Question;
import pals.base.assessment.QuestionCriteria;
import pals.base.assessment.TypeCriteria;
import pals.base.assessment.TypeQuestion;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.web.MultipartUrlParser;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.base.web.security.Escaping;
import pals.plugins.handlers.defaultqch.logging.ModelException;
import pals.plugins.handlers.defaultqch.logging.ModelExceptionClass;

/**
 * A plugin for the default questions and criteria types.
 */
public class DefaultQC extends Plugin
{
    // Static ******************************************************************
    public static final Pattern pattMatchNodeException;
    static
    {
        pattMatchNodeException = Pattern.compile("Exception: (.+) - cause: (.+)");
    }
    // Fields - Constants ******************************************************
    public static final String LOGGING_ALIAS = "Default QC";
    // Methods - Constructors **************************************************
    public DefaultQC(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
    {
        super(core, uuid, jario, version, settings, jarPath);
    }
    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginInstall(NodeCore core, Connector conn)
    {
        // Define types of questions
        TypeQuestion    tqMultipleChoice,
                        tqWrittenResponse,
                        tqCodeJava;
        // Register the types of questions
        if((tqMultipleChoice = registerTypeQuestion(conn, core, MCQ.UUID_QTYPE, MCQ.TITLE, MCQ.DESCRIPTION)) == null)
            return false;
        if((tqWrittenResponse = registerTypeQuestion(conn, core, WrittenResponse.UUID_QTYPE, WrittenResponse.TITLE, WrittenResponse.DESCRIPTION)) == null)
            return false;
        if((tqCodeJava = registerTypeQuestion(conn, core, CodeJava.UUID_QTYPE, CodeJava.TITLE, CodeJava.DESCRIPTION)) == null)
            return false;
        // Register criteria types
        TypeCriteria    tcManualMarking,
                        tcTextMatch,
                        tcRegexMatch,
                        tcMultipleChoice,
                        tcJavaTestInputs,
                        tcJavaCodeMetrics,
                        tcJavaMethodExists,
                        tcJavaClassExists,
                        tcJavaFieldExists,
                        tcJavaTestProgram;
        if((tcManualMarking = registerTypeCriteria(conn, core, ManualMarking.UUID_CTYPE, ManualMarking.TITLE, ManualMarking.DESCRIPTION)) == null)
            return false;
        if((tcTextMatch = registerTypeCriteria(conn, core, TextMatch.UUID_CTYPE, TextMatch.TITLE, TextMatch.DESCRIPTION)) == null)
            return false;
        if((tcRegexMatch = registerTypeCriteria(conn, core, RegexMatch.UUID_CTYPE, RegexMatch.TITLE, RegexMatch.DESCRIPTION)) == null)
            return false;
        if((tcMultipleChoice = registerTypeCriteria(conn, core, MultipleChoice.UUID_CTYPE, MultipleChoice.TITLE, MultipleChoice.DESCRIPTION)) == null)
            return false;
        if((tcJavaTestInputs = registerTypeCriteria(conn, core, JavaTestInputs.UUID_CTYPE, JavaTestInputs.TITLE, JavaTestInputs.DESCRIPTION)) == null)
            return false;
        if((tcJavaCodeMetrics = registerTypeCriteria(conn, core, JavaCodeMetrics.UUID_CTYPE, JavaCodeMetrics.TITLE, JavaCodeMetrics.DESCRIPTION)) == null)
            return false;
        if((tcJavaMethodExists = registerTypeCriteria(conn, core, JavaExistsMethod.UUID_CTYPE, JavaExistsMethod.TITLE, JavaExistsMethod.DESCRIPTION)) == null)
            return false;
        if((tcJavaClassExists = registerTypeCriteria(conn, core, JavaExistsClass.UUID_CTYPE, JavaExistsClass.TITLE, JavaExistsClass.DESCRIPTION)) == null)
            return false;
        if((tcJavaFieldExists = registerTypeCriteria(conn, core, JavaExistsField.UUID_CTYPE, JavaExistsField.TITLE, JavaExistsField.DESCRIPTION)) == null)
            return false;
        if((tcJavaTestProgram = registerTypeCriteria(conn, core, JavaTestProgram.UUID_CTYPE, JavaTestProgram.TITLE, JavaTestProgram.DESCRIPTION)) == null)
            return false;
        // Add criterias to questions, and persist
        // -- Multiple Choice
        tqMultipleChoice.criteriaAdd(tcManualMarking);
        tqMultipleChoice.criteriaAdd(tcRegexMatch);
        tqMultipleChoice.criteriaAdd(tcTextMatch);
        tqMultipleChoice.criteriaAdd(tcMultipleChoice);
        if(!persistQuestionCriteria(core, tqMultipleChoice, conn))
            return false;
        // -- Written Response
        tqWrittenResponse.criteriaAdd(tcManualMarking);
        tqWrittenResponse.criteriaAdd(tcRegexMatch);
        tqWrittenResponse.criteriaAdd(tcTextMatch);
        if(!persistQuestionCriteria(core, tqWrittenResponse, conn))
            return false;
        // -- Code: Java
        tqCodeJava.criteriaAdd(tcManualMarking);
        tqCodeJava.criteriaAdd(tcRegexMatch);
        tqCodeJava.criteriaAdd(tcTextMatch);
        tqCodeJava.criteriaAdd(tcJavaTestInputs);
        tqCodeJava.criteriaAdd(tcJavaCodeMetrics);
        tqCodeJava.criteriaAdd(tcJavaMethodExists);
        tqCodeJava.criteriaAdd(tcJavaClassExists);
        tqCodeJava.criteriaAdd(tcJavaFieldExists);
        tqCodeJava.criteriaAdd(tcJavaTestProgram);
        if(!persistQuestionCriteria(core, tqCodeJava, conn))
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
    private void unregisterTypeQuestion(Connector conn, UUID uuid)
    {
        TypeQuestion tq = TypeQuestion.load(conn, uuid);
        if(tq != null)
            tq.remove(conn);
    }
    private void unregisterTypeCriteria(Connector conn, UUID uuid)
    {
        TypeCriteria tc = TypeCriteria.load(conn, uuid);
        if(tc != null)
            tc.remove(conn);
    }
    @Override
    public boolean eventHandler_pluginUninstall(NodeCore core, Connector conn)
    {
        // Remove question types
        unregisterTypeQuestion(conn, CodeJava.UUID_QTYPE);
        unregisterTypeQuestion(conn, MCQ.UUID_QTYPE);
        unregisterTypeQuestion(conn, WrittenResponse.UUID_QTYPE);
        // Remove criteria types
        unregisterTypeCriteria(conn, JavaExistsClass.UUID_CTYPE);
        unregisterTypeCriteria(conn, JavaExistsMethod.UUID_CTYPE);
        unregisterTypeCriteria(conn, JavaExistsField.UUID_CTYPE);
        unregisterTypeCriteria(conn, JavaCodeMetrics.UUID_CTYPE);
        unregisterTypeCriteria(conn, JavaTestInputs.UUID_CTYPE);
        unregisterTypeCriteria(conn, JavaTestProgram.UUID_CTYPE);
        unregisterTypeCriteria(conn, ManualMarking.UUID_CTYPE);
        unregisterTypeCriteria(conn, MultipleChoice.UUID_CTYPE);
        unregisterTypeCriteria(conn, RegexMatch.UUID_CTYPE);
        unregisterTypeCriteria(conn, TextMatch.UUID_CTYPE);
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
        core.getWebManager().urlsUnregister(this);
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
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        if(!web.urlsRegister(this, new String[]{
            "admin/stats"
        }))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        MultipartUrlParser mup = new MultipartUrlParser(data);
        String page;
        switch(mup.getPart(0))
        {
            case "admin":
                page = mup.getPart(1);
                if(page == null || data.getUser() == null || !(data.getUser().getGroup().isAdmin() || data.getUser().getGroup().isMarkerGeneral()))
                    return false;
                switch(page)
                {
                    case "stats":
                    {
                        page = mup.getPart(2);
                        if(page == null)
                            return pageStats_home(data);
                        else
                        {
                            switch(page)
                            {
                                case "overview":
                                    return pageStats_overview(data);
                                case "view":
                                    return pageStats_view(data);
                            }
                        }
                        break;
                    }
                }
                break;
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
        // -- Multiple choice
        if(qtype.equals(MCQ.UUID_QTYPE))
            return MCQ.pageQuestionEdit(data, q);
        // -- Written Response
        else if(qtype.equals(WrittenResponse.UUID_QTYPE))
            return WrittenResponse.pageQuestionEdit(data, q);
        // -- Code: Java
        else if(qtype.equals(CodeJava.UUID_QTYPE))
            return CodeJava.pageQuestionEdit(data, q);
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
        if(ctype.equals(ManualMarking.UUID_CTYPE))
            return ManualMarking.pageCriteriaEdit(data, qc);
        else if(ctype.equals(TextMatch.UUID_CTYPE))
            return TextMatch.pageCriteriaEdit(data, qc);
        else if(ctype.equals(RegexMatch.UUID_CTYPE))
            return RegexMatch.pageCriteriaEdit(data, qc);
        else if(ctype.equals(MultipleChoice.UUID_CTYPE))
            return MultipleChoice.pageCriteriaEdit(data, qc);
        
        else if(ctype.equals(JavaExistsClass.UUID_CTYPE))
            return JavaExistsClass.pageCriteriaEdit(data, qc);
        else if(ctype.equals(JavaExistsMethod.UUID_CTYPE))
            return JavaExistsMethod.pageCriteriaEdit(data, qc);
        else if(ctype.equals(JavaExistsField.UUID_CTYPE))
            return JavaExistsField.pageCriteriaEdit(data, qc);
        else if(ctype.equals(JavaCodeMetrics.UUID_CTYPE))
            return JavaCodeMetrics.pageCriteriaEdit(data, qc);
        else if(ctype.equals(JavaTestInputs.UUID_CTYPE))
            return JavaTestInputs.pageCriteriaEdit(data, qc);
        else if(ctype.equals(JavaTestProgram.UUID_CTYPE))
            return JavaTestProgram.pageCriteriaEdit(data, qc);
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
        if(qtype.equals(MCQ.UUID_QTYPE))
            return MCQ.pageQuestionCapture(data, ia, iaq, html, secure);
        else if(qtype.equals(WrittenResponse.UUID_QTYPE))
            return WrittenResponse.pageQuestionCapture(data, ia, iaq, html, secure);
        else if(qtype.equals(CodeJava.UUID_QTYPE))
            return CodeJava.pageQuestionCapture(data, ia, iaq, html, secure);
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
        if(qtype.equals(MCQ.UUID_QTYPE))
            return MCQ.pageQuestionDisplay(data, ia, iaq, html, secure, editMode);
        else if(qtype.equals(WrittenResponse.UUID_QTYPE))
            return WrittenResponse.pageQuestionDisplay(data, ia, iaq, html, secure, editMode);
        else if(qtype.equals(CodeJava.UUID_QTYPE))
            return CodeJava.pageQuestionDisplay(data, ia, iaq, html, secure, editMode);
        return false;
    }
    private boolean pageStats_home(WebRequestData data)
    {
        // Fetch modules
        Module[] modules = Module.loadAll(data.getConnector());
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Stats");
        data.setTemplateData("pals_content", "defaultqch/stats/home");
        data.appendHeaderCSS("/content/css/defaultqch_stats.css");
        // -- Fields
        data.setTemplateData("modules", modules);
        return true;
    }
    private boolean pageStats_overview(WebRequestData data)
    {
        RemoteRequest req = data.getRequestData();
        // Load models
        ModelExceptionClass[] models;
        String type = req.getField("type");
        String rawTid = req.getField("tid");
        String clear = req.getField("clear");
        // Parse filter
        String rawFilter = req.getField("filter");
        ModelExceptionClass.LoadRemoveFilter filter = ModelExceptionClass.LoadRemoveFilter.parse(rawFilter);
        // Parse/delete type
        boolean doClear = clear != null && clear.equals("1") && CSRF.isSecure(data);
        if(type == null || rawTid == null || type.length() == 0 || rawTid.length() == 0)
        {
            if(doClear)
                ModelExceptionClass.delete(data.getConnector(), filter);
            models = ModelExceptionClass.load(data.getConnector(), filter);
        }
        else
        {
            int tid;
            try
            {
                tid = Integer.parseInt(rawTid);
            }
            catch(NumberFormatException ex)
            {
                return false;
            }
            switch(type)
            {
                case "m": // Module
                    Module module = Module.load(data.getConnector(), tid);
                    if(module == null)
                        return false;
                    if(doClear)
                        ModelExceptionClass.delete(data.getConnector(), module, filter);
                    models = ModelExceptionClass.load(data.getConnector(), module, filter);
                    break;
                case "a": // Assignment
                    Assignment ass = Assignment.load(data.getConnector(), null, tid);
                    if(ass == null)
                        return false;
                    if(doClear)
                        ModelExceptionClass.delete(data.getConnector(), ass, filter);
                    models = ModelExceptionClass.load(data.getConnector(), ass, filter);
                    break;
                case "q": // Question
                    Question q = Question.load(data.getCore(), data.getConnector(), tid);
                    if(q == null)
                        return false;
                    if(doClear)
                        ModelExceptionClass.delete(data.getConnector(), q, filter);
                    models = ModelExceptionClass.load(data.getConnector(), q, filter);
                    break;
                default:
                    return false;
            }
        }
        // Sum cum freq
        long totalFreq = 0;
        for(ModelExceptionClass m : models)
            totalFreq += m.getFrequency();
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Stats - Overview");
        data.setTemplateData("pals_content", "defaultqch/stats/overview");
        data.appendHeaderCSS("/content/css/defaultqch_stats.css");
        // -- Fields
        data.setTemplateData("models", models);
        if(models.length > 0)
            data.setTemplateData("total_freq", totalFreq);
        data.setTemplateData("type", Escaping.htmlEncode(type));
        data.setTemplateData("tid", Escaping.htmlEncode(rawTid));
        data.setTemplateData("filter", rawFilter);
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageStats_view(WebRequestData data)
    {
        final int ITEMS_PER_PAGE = 15;
        
        RemoteRequest req = data.getRequestData();
        String type = req.getField("type");
        String rawTid = req.getField("tid");
        String clear = req.getField("clear");
        String rawPage = req.getField("page");
        // Parse identifier of class
        String rawEcid = req.getField("ecid");
        int ecid;
        try
        {
            ecid = Integer.parseInt(rawEcid);
        }
        catch(NumberFormatException ex)
        {
            return false;
        }
        // Fetch ecid data
        ModelExceptionClass ec = ModelExceptionClass.loadSingle(data.getConnector(), ecid);
        if(ec == null)
            return false;
        // Parse page
        int page;
        try
        {
            if((page = Integer.parseInt(rawPage)) < 1)
                page = 1;
        }
        catch(NumberFormatException ex)
        {
            page = 1;
        }
        int offset = (page * ITEMS_PER_PAGE)-ITEMS_PER_PAGE;
        int limit = ITEMS_PER_PAGE+1;
        // Check if to clear data
        boolean doClear = clear != null && clear.equals("1") && CSRF.isSecure(data);
        // Parse type
        ModelException[] models;
        if(type == null || type.length() == 0)
        {
            models = ModelException.load(data.getConnector(), ecid, limit, offset);
            if(doClear)
                ModelException.delete(data.getConnector(), ecid);
        }
        else
        {
            int tid;
            try
            {
                tid = Integer.parseInt(rawTid);
            }
            catch(NumberFormatException ex)
            {
                return false;
            }
            switch(type)
            {
                case "m": // Module
                    Module module = Module.load(data.getConnector(), tid);
                    if(module == null)
                        return false;
                    if(doClear)
                        ModelException.delete(data.getConnector(), ecid, module);
                    models = ModelException.load(data.getConnector(), ecid, module, limit, offset);
                    break;
                case "a": // Assignment
                    Assignment ass = Assignment.load(data.getConnector(), null, tid);
                    if(ass == null)
                        return false;
                    if(doClear)
                        ModelException.delete(data.getConnector(), ecid, ass);
                    models = ModelException.load(data.getConnector(), ecid, ass, limit, offset);
                    break;
                case "q": // Question
                    Question q = Question.load(data.getCore(), data.getConnector(), tid);
                    if(q == null)
                        return false;
                    if(doClear)
                        ModelException.delete(data.getConnector(), ecid, q);
                    models = ModelException.load(data.getConnector(), ecid, q, limit, offset);
                    break;
                default:
                    return false;
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Stats - Overview");
        data.setTemplateData("pals_content", "defaultqch/stats/view");
        data.appendHeaderCSS("/content/css/defaultqch_stats.css");
        // -- Fields
        data.setTemplateData("ec", ec);
        data.setTemplateData("models", models);
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("page", page);
        if(page > 1)
            data.setTemplateData("page_prev", page-1);
        if(page < Integer.MAX_VALUE && models.length > ITEMS_PER_PAGE)
            data.setTemplateData("page_next", page+1);
        return true;
    }
    // Methods - Criteria ******************************************************
    private boolean criteriaMarking(Object[] hookData)
    {
        // Parse hook data
        if(hookData.length != 3 || !(hookData[0] instanceof Connector) || !(hookData[1] instanceof NodeCore) || !(hookData[2] instanceof InstanceAssignmentCriteria))
            return false;
        Connector conn = (Connector)hookData[0];
        NodeCore core = (NodeCore)hookData[1];
        InstanceAssignmentCriteria iac = (InstanceAssignmentCriteria)hookData[2];
        // Delegate to the correct method for marking
        UUID ctype = iac.getQC().getCriteria().getUuidCType();
        if(ctype.equals(ManualMarking.UUID_CTYPE))
            return ManualMarking.criteriaMarking(conn, core, iac);
        else if(ctype.equals(TextMatch.UUID_CTYPE))
            return TextMatch.criteriaMarking(conn, core, iac);
        else if(ctype.equals(RegexMatch.UUID_CTYPE))
            return RegexMatch.criteriaMarking(conn, core, iac);
        else if(ctype.equals(MultipleChoice.UUID_CTYPE))
            return MultipleChoice.criteriaMarking(conn, core, iac);
        
        else if(ctype.equals(JavaExistsClass.UUID_CTYPE))
            return JavaExistsClass.criteriaMarking(conn, core, iac);
        else if(ctype.equals(JavaExistsMethod.UUID_CTYPE))
            return JavaExistsMethod.criteriaMarking(conn, core, iac);
        else if(ctype.equals(JavaExistsField.UUID_CTYPE))
            return JavaExistsMethod.criteriaMarking(conn, core, iac);
        else if(ctype.equals(JavaCodeMetrics.UUID_CTYPE))
            return JavaCodeMetrics.criteriaMarking(conn, core, iac);
        else if(ctype.equals(JavaTestInputs.UUID_CTYPE))
            return JavaTestInputs.criteriaMarking(conn, core, iac);
        else if(ctype.equals(JavaTestProgram.UUID_CTYPE))
            return JavaTestProgram.criteriaMarking(conn, core, iac);
        
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
        if(ctype.equals(TextMatch.UUID_CTYPE))
            return TextMatch.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(RegexMatch.UUID_CTYPE))
            return RegexMatch.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(ManualMarking.UUID_CTYPE))
            return ManualMarking.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(MultipleChoice.UUID_CTYPE))
            return MultipleChoice.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(JavaExistsClass.UUID_CTYPE))
            return JavaExistsClass.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(JavaExistsMethod.UUID_CTYPE))
            return JavaExistsMethod.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(JavaExistsField.UUID_CTYPE))
            return JavaExistsMethod.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(JavaCodeMetrics.UUID_CTYPE))
            return JavaCodeMetrics.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(JavaTestInputs.UUID_CTYPE))
            return JavaTestInputs.criteriaDisplay(data, ia, iaq, iac, html);
        else if(ctype.equals(JavaTestProgram.UUID_CTYPE))
            return JavaTestProgram.criteriaDisplay(data, ia, iaq, iac, html);
        
        return false;
    }
}
