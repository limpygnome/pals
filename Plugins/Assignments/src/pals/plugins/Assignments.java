package pals.plugins;

import java.util.HashMap;
import org.joda.time.DateTime;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.PluginManager;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.WebManager;
import pals.base.assessment.Assignment;
import pals.base.assessment.AssignmentQuestion;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.utils.Misc;
import pals.base.web.MultipartUrlParser;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.base.web.security.Escaping;

/**
 * A plugin used for taking/sitting and marking assignments.
 */
public class Assignments extends Plugin
{
    // Methods - Constructors **************************************************
    public Assignments(NodeCore core, UUID uuid, JarIO jario, Settings settings, String jarPath)
    {
        super(core, uuid, jario, settings, jarPath);
    }
    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginInstall(NodeCore core, Connector conn)
    {
        return true;
    }
    @Override
    public boolean eventHandler_pluginUninstall(NodeCore core, Connector conn)
    {
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
        // Remove URLs
        core.getWebManager().urlsUnregister(this);
        // Remove templates
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
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        if(!web.urlsRegister(this, new String[]{
            "assignments"
        }))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        // Check the user is logged-in (all requests require login)
        if(data.getUser() == null)
            return false;
        // Delegate the request
        MultipartUrlParser mup = new MultipartUrlParser(data);
        String page = mup.getPart(0);
        if(page != null)
        {
            switch(page)
            {
                case "assignments":
                {
                    page = mup.getPart(1);
                    if(page != null)
                    {
                        switch(page)
                        {
                            case "take":
                                return pageAssignments_take(data, mup);
                            case "instance":
                                return pageAssignments_instance(data, mup);
                        }
                    }
                }
            }
        }
        return false;
    }
    @Override
    public String getTitle()
    {
        return "PALS [WEB]: Assignments";
    }
    // Methods - Pages *********************************************************
    private boolean pageAssignments_take(WebRequestData data, MultipartUrlParser mup)
    {
        // Fetch the assignment
        Assignment ass = Assignment.load(data.getConnector(), null, mup.parseInt(2, -1));
        if(ass == null || !ass.getModule().isEnrolled(data.getConnector(), data.getUser()))
            return false;
        // Check the assignment is active
        if(!ass.isActive() || ass.isDueSurpassed())
            return false;
        // Fetch the latest assignment by the user
        InstanceAssignment ia = InstanceAssignment.getLastAssignment(data.getConnector(), ass, data.getUser());
        if(ia == null || ia.getStatus() == InstanceAssignment.Status.Marked)
        {
            ia = null;
            // Check the user has not reached the maximum number of submissions for a new assignment
            if(ass.getMaxAttempts() != -1 && InstanceAssignment.getAttempts(data.getConnector(), ass, data.getUser()) >= ass.getMaxAttempts())
                return false;
            // Check postback to continue
            RemoteRequest req = data.getRequestData();
            String confirm = req.getField("confirm");
            if(confirm != null && confirm.equals("1"))
            {
                // Validate the request
                if(!CSRF.isSecure(data))
                    data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
                else
                {
                    // Create a new instance of the assignment, persist
                    ia = new InstanceAssignment(data.getUser(), ass, InstanceAssignment.Status.Active, DateTime.now(), null, 0);
                    InstanceAssignment.PersistStatus iaps = ia.persist(data.getConnector());
                    switch(iaps)
                    {
                        default:
                            ia = null;
                            data.setTemplateData("error", "Failed to create a new instance of the assignment; please try again or contact an administrator ('"+iaps.name()+"').");
                            break;
                        case Success:
                            // Do nothing...
                            break;
                    }
                }
            }
            // Setup the confirmation page
            data.setTemplateData("pals_title", "Assignment - Confirm");
            data.setTemplateData("pals_content", "assignment/confirm");
            // -- Fields
            data.setTemplateData("csrf", CSRF.set(data));
            data.setTemplateData("assignment", ass);
            data.setTemplateData("module", ass.getModule());
        }
        if(ia != null)
        {
            // Redirect to the assignment
            data.getResponseData().setRedirectUrl("/assignments/instance/"+ia.getAIID());
        }
        return true;
    }
    private boolean pageAssignments_instance(WebRequestData data, MultipartUrlParser mup)
    {
        // Load the instance assignment model
        InstanceAssignment ia =  InstanceAssignment.load(data.getConnector(), null, data.getUser(), mup.parseInt(2, -1));
        if(ia == null)
            return false;
        Assignment ass = ia.getAss();
        boolean captureMode = ia.getStatus() == InstanceAssignment.Status.Active && !ass.isDueSurpassed();
        boolean editMode = data.getRequestData().getField("edit") != null && (data.getUser().getGroup().isMarkerGeneral() || data.getUser().getGroup().isAdminModules());
        // Load the question pages
        Integer[] pages = ass.questionsPagesDb(data.getConnector());
        // Setup the page
        data.setTemplateData("pals_title", "Assignment - "+Escaping.htmlEncode(ass.getTitle()));
        data.setTemplateData("pals_content", "assignment/instance_page");
        // -- Fields
        data.setTemplateData("plugin", this);
        data.setTemplateData("pages", pages);
        data.setTemplateData("module", ass.getModule());
        data.setTemplateData("assignment", ass);
        data.setTemplateData("assignment_instance", ia);
        // -- Flags
        data.setTemplateData("edit_mode", editMode);
        data.setTemplateData("capture_mode", captureMode);
        data.setTemplateData("marked_mode", ia.getStatus() == InstanceAssignment.Status.Marked);
        // Delegate to page content handler
        String page = mup.getPart(3);
        if(page == null)
            page = "1";
        switch(page)
        {
            case "submit":
                return pageAssignments_instanceSubmit(data, ia, pages, editMode, captureMode);
            default:
                // Assume it's a question page
                return pageAssignments_instanceQuestions(data, ia, pages, mup.parseInt(3, 1), editMode, captureMode);
        }
    }
    private boolean pageAssignments_instanceQuestions(WebRequestData data, InstanceAssignment ia, Integer[] pages, int page, boolean editMode, boolean captureMode)
    {
        Assignment ass = ia.getAss();
        RemoteRequest req = data.getRequestData();
        // Check the current requested page exists
        if(!Misc.arrayContains(pages, page))
            return false;
        // Load the current page of questions
        if(!ass.questionsLoad(data.getCore(), data.getConnector(), page))
            data.setTemplateData("error", "Failed to load the questions for this page.");
        else
        {
            AssignmentQuestion[] questions = ass.questions(page);
            QuestionData[] qdata = new QuestionData[questions.length];
            // Check if the current request is secure
            String csrf = req.getField("csrf");
            boolean secure = CSRF.isSecure(data, csrf);
            if(!secure && csrf != null)
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            // Invoke each question-handler to generate the question's HTML
            Plugin p;
            PluginManager pm = data.getCore().getPlugins();
            StringBuilder html;
            String error;
            HashMap<String,Object> kvs;
            UUID plugin;
            InstanceAssignmentQuestion iaq;
            for(int i = 0; i < questions.length; i++)
            {
                error = null;
                html = new StringBuilder();
                // Load instance data
                iaq = InstanceAssignmentQuestion.load(data.getCore(), data.getConnector(), ia, questions[i]);
                // Fetch the plugin responsible
                plugin = questions[i].getQuestion().getQtype().getUuidPlugin();
                p = plugin != null ? pm.getPlugin(plugin) : null;
                // Delegate to plugin to render and handle data for the current request
                if(p == null)
                    error = "Plugin '"+questions[i].getQuestion().getQtype().getUuidPlugin().getHexHyphens()+"' is not loaded in the runtime!";
                // Different handler based on the capture, or non-capture mode (display), mode
                else if(!p.eventHandler_handleHook(captureMode ? "question_type.question_capture" : "question_type.question_display", captureMode ? new Object[]{data, ia, questions[i], iaq, html, secure} : new Object[]{data, ia, questions[i], iaq, html, secure, editMode}))
                    error = "Plugin '"+questions[i].getQuestion().getQtype().getUuidPlugin().getHexHyphens()+"' did not handle question-type '"+questions[i].getQuestion().getQtype().getUuidQType().getHexHyphens()+"'!";
                // Check if to render a failed template
                if(error != null)
                {
                    kvs = new HashMap<>();
                    kvs.put("error", error);
                    html.append(data.getCore().getTemplates().render(data, kvs, "assignment/question_failed"));
                }
                // Process criterias
                if(editMode)
                {
                }
                // Update model
                qdata[i] = new QuestionData(i+1, questions[i], iaq, html.toString());
            }
            // Set the page
            data.setTemplateData("instance_page", "assignment/instance_page_render_questions");
            // -- Fields
            data.setTemplateData("csrf", CSRF.set(data));
            data.setTemplateData("questions", qdata);
            data.setTemplateData("current_page", page);
        }
        return true;
    }
    private boolean pageAssignments_instanceSubmit(WebRequestData data, InstanceAssignment ia, Integer[] pages, boolean editMode, boolean captureMode)
    {
        // Check the assignment is active
        if(ia.getStatus() != InstanceAssignment.Status.Active)
            return false;
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String confirm = req.getField("confirm");
        if(confirm != null && confirm.equals("1"))
        {
            // Create instance-criterias to be marked
            InstanceAssignmentCriteria.CreateInstanceStatus cis = InstanceAssignmentCriteria.createForInstanceAssignment(data.getConnector(), ia, InstanceAssignmentCriteria.Status.AwaitingMarking);
            // Handle the, possible, creation of the instance criteria
            switch(cis)
            {
                default:
                case Failed:
                    data.setTemplateData("error", "Failed to prepare assignment for marking!");
                    break;
                case Failed_NoInstanceQuestions:
                    // Set the mark of the assignment to 0
                    ia.setMark(0);
                    // Update the status to marked
                    ia.setStatus(InstanceAssignment.Status.Marked);
                    break;
                case Success:
                    // Update the assignment as submitted/pending marking
                    ia.setStatus(InstanceAssignment.Status.Submitted);
                    break;
            }
            if(cis == InstanceAssignmentCriteria.CreateInstanceStatus.Failed_NoInstanceQuestions || cis == InstanceAssignmentCriteria.CreateInstanceStatus.Success)
            {
                // Set the date-time of submission
                ia.setTimeEnd(DateTime.now());
                // Persist the model - this should work, else something has gone critically wrong...
                InstanceAssignment.PersistStatus iaps = ia.persist(data.getConnector());
                switch(iaps)
                {
                    default:
                        data.setTemplateData("error", "Failed to submit assignment ('"+iaps.name()+"'); please try again or contact an administrator!");
                        break;
                    case Success:
                        // Redirect to review page...
                        data.getResponseData().setRedirectUrl("/assignments/instance/"+ia.getAIID());
                        break;
                }
            }
        }
        // Fetch the instance of the questions
        InstanceAssignmentQuestion[] questions = InstanceAssignmentQuestion.loadAll(data.getCore(), data.getConnector(), ia);
        // Setup the page
        data.setTemplateData("instance_page", "assignment/instance_page_submit");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("instance_questions", questions);
        return true;
    }
    public String pageAssignments_instanceRenderCriteria(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria criteria)
    {
        // Fetch the plugin responsible
        UUID p = criteria.getQC().getCriteria().getUuidCType();
        Plugin plugin = p != null ? getCore().getPlugins().getPlugin(p) : null;
        StringBuilder html = new StringBuilder();
        if(plugin != null && plugin.eventHandler_handleHook("criteria_type.display_feedback", new Object[]{data, ia, iaq, criteria, html}))
            return html.toString();
        return "";
    }
}
