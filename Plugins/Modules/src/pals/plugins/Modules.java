package pals.plugins;

import java.util.Arrays;
import org.joda.time.DateTime;
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
import pals.base.assessment.Module;
import pals.base.assessment.Question;
import pals.base.auth.User;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.web.MultipartUrlParser;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.base.web.security.Escaping;
import pals.plugins.web.Captcha;

/**
 * The default web-interface for modules.
 */
public class Modules extends Plugin
{
    // Methods - Constructors **************************************************
    public Modules(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
    {
        super(core, uuid, jario, version, settings, jarPath);
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
        // Unregister URLs
        core.getWebManager().urlsUnregister(this);
        // Unregister templates
        core.getTemplates().remove(this);
    }
    @Override
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        if(!web.urlsRegister(this, new String[]{
            "modules",
            "admin/modules"
        }))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_registerTemplates(NodeCore core, TemplateManager manager)
    {
        if(!manager.load(this, "templates"))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        if(data.getUser() == null)
            return false;
        MultipartUrlParser mup = new MultipartUrlParser(data);
        String page;
        switch(mup.getPart(0))
        {
            case "modules":
                if(mup.getPart(1) == null)
                    // Overview of all modules belonging to a user
                    return pageModules(data);
                else
                    // Delegate to module controller
                    return pageModule(data, mup);
            case "admin":
                page = mup.getPart(1);
                if(page == null)
                    return false;
                switch(page)
                {
                    case "modules":
                        String p2 = mup.getPart(2);
                        if(p2 == null)
                            // Overview of all modules
                            return pageAdminModules(data);
                        else if(p2.equals("create"))
                            // Create a new module
                            return pageAdminModule_create(data);
                        else
                            // Specific module...
                            return pageAdminModule(data, mup);
                }
                break;
        }
        return false;
    }
    @Override
    public String getTitle()
    {
        return "PALS [WEB]: Modules";
    }
    // Methods - Pages - Main **************************************************
    private boolean pageModules(WebRequestData data)
    {
        // Fetch models
        ModelViewModules[] models = ModelViewModules.load(data.getConnector(), data.getUser());
        // Setup the page
        data.setTemplateData("pals_title", "Modules");
        data.setTemplateData("pals_content", "modules/page_modules");
        data.setTemplateData("models", models);
        return true;
    }
    private boolean pageModule(WebRequestData data, MultipartUrlParser mup)
    {
        User user = data.getUser();
        // Load the module model
        Module module = Module.load(data.getConnector(), mup.parseInt(1, -1));
        if(module == null)
            return false;
        // Check the user is enrolled
        if(!module.isEnrolled(data.getConnector(), user))
            return false;
        // Delegate request
        String page = mup.getPart(2);
        if(page == null)
            return pageModuleView(data, mup, module, user);
        else
        {
            switch(page)
            {
                case "history":
                    return pageModuleAssignmentHistory(data, mup, module, user);
            }
        }
        return false;
    }
    private boolean pageModuleView(WebRequestData data, MultipartUrlParser mup, Module module, User user)
    {
        // Fetch the module's assignments
        Assignment[] assignments = Assignment.load(data.getConnector(), module, true);
        // Create view models
        ModelViewModule[] models = new ModelViewModule[assignments.length];
        // Sum the weight of the assignments and create view models
        int total = 0;
        int offset = 0;
        for(Assignment ass : assignments)
        {
            total += ass.getWeight();
            models[offset++] = new ModelViewModule(data.getConnector(), ass, user);
        }
        // Setup the page
        data.setTemplateData("pals_title", "Module - "+Escaping.htmlEncode(module.getTitle()));
        data.setTemplateData("pals_content", "modules/page_module");
        // -- Fields
        data.setTemplateData("module", module);
        data.setTemplateData("assignments", models);
        data.setTemplateData("total_weight", total);
        return true;
    }
    private boolean pageModuleAssignmentHistory(WebRequestData data, MultipartUrlParser mup, Module module, User user)
    {
        final int ASSIGNMENTS_PER_PAGE = 10;
        // Load assignment model
        Assignment ass = Assignment.load(data.getConnector(), module, mup.parseInt(3, -1));
        if(ass == null)
            return false;
        // Parse the page being displayed
        int page = mup.parseInt(4, 1);
        // Fetch instance models
        InstanceAssignment[] ias = InstanceAssignment.load(data.getConnector(), ass, user, ASSIGNMENTS_PER_PAGE+1, (ASSIGNMENTS_PER_PAGE*page)-ASSIGNMENTS_PER_PAGE);
        // Setup the page
        data.setTemplateData("pals_title", "Module - "+Escaping.htmlEncode(module.getTitle()));
        data.setTemplateData("pals_content", "modules/page_module_assignment_history");
        // -- Fields
        data.setTemplateData("module", module);
        data.setTemplateData("assignment", ass);
        data.setTemplateData("assignments", ias.length > ASSIGNMENTS_PER_PAGE ? Arrays.copyOf(ias, ASSIGNMENTS_PER_PAGE) : ias);
        data.setTemplateData("page", page);
        if(page > 1)
            data.setTemplateData("page_prev", page-1);
        if(page < Integer.MAX_VALUE && ias.length > ASSIGNMENTS_PER_PAGE)
            data.setTemplateData("page_next", page+1);
        return true;
    }
    // Methods - Pages - Admin *************************************************
    private boolean pageAdminModules(WebRequestData data)
    {
        // Check permissions
        User user = data.getUser();
        if(!user.getGroup().isAdminModules())
            return false;
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Modules");
        data.setTemplateData("pals_content", "modules/page_admin_modules");
        data.setTemplateData("modules", Module.loadAll(data.getConnector()));
        return true;
    }
    private boolean pageAdminModule_create(WebRequestData data)
    {
        // Check permissions
        User user = data.getUser();
        if(!user.getGroup().isAdminModules())
            return false;
        // Check field data
        RemoteRequest request = data.getRequestData();
        String moduleTitle = request.getField("module_title");
        String csrf = request.getField("csrf");
        if(moduleTitle != null)
        {
            // Check security
            if(!CSRF.isSecure(data, csrf))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                Module m = new Module(moduleTitle);
                switch(m.persist(data.getConnector()))
                {
                    case Failed:
                        data.setTemplateData("error", "An error occurred; please try again!");
                        break;
                    case Failed_title_length:
                        data.setTemplateData("error", "Title must be "+m.getTitleMin()+" to "+m.getTitleMax()+" characters in length!");
                        break;
                    case Success:
                        data.getResponseData().setRedirectUrl("/admin/modules/"+m.getModuleID());
                        break;
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Modules - Create");
        data.setTemplateData("pals_content", "modules/page_admin_module_create");
        // -- Set fields
        data.setTemplateData("module_title", Escaping.htmlEncode(moduleTitle));
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAdminModule(WebRequestData data, MultipartUrlParser mup)
    {
        // Check permissions
        User user = data.getUser();
        if(!user.getGroup().isAdminModules())
            return false;
        // Parse the module
        int moduleid = mup.parseInt(2, -1);
        if(moduleid == -1)
            return false;
        // Load the model for the module
        Module module = Module.load(data.getConnector(), moduleid);
        if(module == null)
            return false;
        // Handle the page
        String page = mup.getPart(3);
        if(page == null)
            return pageAdminModule_view(data, module);
        else
        {
            switch(page)
            {
                case "assignments":
                    page = mup.getPart(4);
                    if(page == null)
                        return pageAdminModule_assignmentsView(data, module);
                    else
                        switch(page)
                        {
                            case "create":
                                return pageAdminModule_assignmentCreate(data, module);
                            default:
                                return pageAdminModule_assignment(data, module, page, mup);
                        }
                case "enrollment":
                    return pageAdminModule_enrollment(data, module);
                case "edit":
                    return pageAdminModule_edit(data, module);
                case "delete":
                    return pageAdminModule_delete(data, module);
                default:
                    return false;
            }
        }
    }
    private boolean pageAdminModule_view(WebRequestData data, Module module)
    {
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle()));
        data.setTemplateData("pals_content", "modules/page_admin_module");
        data.setTemplateData("module", module);
        // -- Fetch active assignments
        Assignment[] assignments = Assignment.loadActive(data.getConnector(), module, true);
        // -- Module Users
        data.setTemplateData("module_users", module.usersEnrolled(data.getConnector()));
        data.setTemplateData("assignments", assignments);
        return true;
    }
    private boolean pageAdminModule_edit(WebRequestData data, Module module)
    {
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String moduleTitle = req.getField("module_title");
        if(moduleTitle != null)
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Update the model
                module.setTitle(moduleTitle);
                // Attempt to persist
                Module.ModulePersistStatus mps = module.persist(data.getConnector());
                switch(mps)
                {
                    case Failed:
                        data.setTemplateData("error", "Failed to update model for an unknown reason!");
                        break;
                    case Failed_title_length:
                        data.setTemplateData("error", "Title must be "+module.getTitleMin()+" to "+module.getTitleMax()+" characters in length!");
                        break;
                    case Success:
                        data.setTemplateData("success", "Successfully updated.");
                        break;
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle()) + " - Edit");
        data.setTemplateData("pals_content", "modules/page_admin_module_edit");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("module", module);
        data.setTemplateData("module_title", moduleTitle != null ? moduleTitle : module.getTitle());
        return true;
    }
    private boolean pageAdminModule_delete(WebRequestData data, Module module)
    {
        // Check for postback
        RemoteRequest request = data.getRequestData();
        String deleteModule = request.getField("delete_module");
        String csrf = request.getField("csrf");
        if(deleteModule != null && deleteModule.equals("1"))
        {
            // Validate security
            if(!CSRF.isSecure(data, csrf))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else if(!Captcha.isCaptchaCorrect(data))
                data.setTemplateData("error", "Incorrect captcha verification code!");
            else
            {
                // Delete the module...
                module.delete(data.getConnector());
                // Redirect to modules page
                data.getResponseData().setRedirectUrl("/admin/modules");
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle()) + " - Delete");
        data.setTemplateData("pals_content", "modules/page_admin_module_delete");
        data.setTemplateData("module", module);
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAdminModule_enrollment(WebRequestData data, Module module)
    {
        // Check field data
        RemoteRequest request = data.getRequestData();
        String moduleUsersAdd = request.getField("module_users_add");
        String remove = request.getField("remove");
        String removeAll = request.getField("remove_all");
        String csrf = request.getField("csrf");
        // -- Adding users
        if(moduleUsersAdd != null && moduleUsersAdd.length() > 0)
        {
            if(!CSRF.isSecure(data, csrf))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Iterate each line of the input, load the user and add
                boolean failed = false;
                User user;
                String un;
                for(String line : moduleUsersAdd.replace("\r", "").split("\n"))
                {
                    un = line.trim();
                    if(un.length() > 0)
                    {
                        if((user = User.load(data.getConnector(), un)) != null)
                        {
                            module.usersAdd(data.getConnector(), user);
                        }
                        else
                        {
                            data.setTemplateData("error", "User '"+un+"' does not exist!");
                            failed = true;
                            break;
                        }
                    }
                }
                // End transaction
                if(!failed)
                {
                    data.setTemplateData("success", "Successfully enrolled users.");
                }
            }
        }
        // -- Removing a user
        if(remove != null && remove.length() > 0)
        {
            if(!CSRF.isSecure(data, csrf))
                data.setTemplateData("error2", "Invalid request; try again or contact an administrator!");
            else
            {
                try
                {
                    int userid = Integer.parseInt(remove);
                    User user = User.load(data.getConnector(), userid);
                    if(user == null)
                        data.setTemplateData("error2", "User does not exist!");
                    else if(!module.usersRemove(data.getConnector(), user))
                        data.setTemplateData("error2", "Could not remove user, please try again!");
                }
                catch(NumberFormatException ex)
                {
                    data.setTemplateData("error2", "Invalid request; try again or contact an administrator!");
                }
            }
        }
        // -- Removing all users
        if(removeAll != null && removeAll.equals("1"))
        {
            if(!CSRF.isSecure(data, csrf))
                data.setTemplateData("error2", "Invalid request; try again or contact an administrator!");
            else
            {
                // Remove all the users for the module
                module.usersRemoveAll(data.getConnector());
                // Redirect back to overview (to hide long url)
                data.getResponseData().setRedirectUrl("/admin/modules/"+module.getModuleID()+"/enrollment");
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle())+" - Enrollment");
        data.setTemplateData("pals_content", "modules/page_admin_enrollment");
        data.setTemplateData("module", module);
        data.setTemplateData("module_users", module.usersEnrolled(data.getConnector()));
        // -- Fields
        data.setTemplateData("module_users_add", moduleUsersAdd);
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAdminModule_assignmentsView(WebRequestData data, Module module)
    {
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle())+" - Assignments");
        data.setTemplateData("pals_content", "modules/page_admin_module_assignments");
        // Fetch the assignments
        Assignment[] assignments = Assignment.load(data.getConnector(), module, false);
        // Compute the total weight
        int total = 0;
        for(Assignment ass : assignments)
            total += ass.getWeight();
        // -- Fields
        data.setTemplateData("module", module);
        data.setTemplateData("assignments", assignments);
        data.setTemplateData("total_weight", total);
        return true;
    }
    private boolean pageAdminModule_assignmentCreate(WebRequestData data, Module module)
    {
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String assTitle = req.getField("ass_title");
        String assWeight = req.getField("ass_weight");
        String assMaxAttempts = req.getField("ass_max_attempts");
        String csrf = req.getField("csrf");
        if(assTitle != null && assWeight != null && assMaxAttempts != null)
        {
            // Validate request
            if(!CSRF.isSecure(data, csrf))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Parse field data
                // -- Max-attempts
                int maxAttempts;
                try
                {
                    maxAttempts = Integer.parseInt(assMaxAttempts);
                }
                catch(NumberFormatException ex)
                {
                    maxAttempts = 0;
                }
                // -- Weight
                int weight;
                try
                {
                    weight = Integer.parseInt(assWeight);
                }
                catch(NumberFormatException ex)
                {
                    weight = 0;
                }
                // Attempt to persist a new assignment
                Assignment ass = new Assignment(module, assTitle, weight, false, maxAttempts, null, false);
                Assignment.PersistStatus ps = ass.persist(data.getConnector());
                switch(ps)
                {
                    case Failed:
                    case Invalid_Module:
                    case Invalid_Due:
                        data.setTemplateData("error", "An unknown error occurred ('"+ps.name()+"'); please try again or contact an administrator!");
                        break;
                    case Invalid_Title:
                        data.setTemplateData("error", "Invalid title, must be "+ass.getTitleMin()+" to "+ass.getTitleMax()+" characters in length!");
                        break;
                    case Invalid_Weight:
                        data.setTemplateData("error", "Invalid weight, must be a numeric value and greater than zero!");
                        break;
                    case Invalid_MaxAttempts:
                        data.setTemplateData("error", "Invalid max-attempts, must be -1 or greater than zero.");
                        break;
                    case Success:
                        data.getResponseData().setRedirectUrl("/admin/modules/"+module.getModuleID()+"/assignments/"+ass.getAssID()+"/questions");
                        break;
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle())+" - Assignments - Create");
        data.setTemplateData("pals_content", "modules/page_admin_module_assignment_create");
        data.setTemplateData("module", module);
        // -- Fields
        data.setTemplateData("ass_title", assTitle);
        data.setTemplateData("ass_weight", assWeight);
        data.setTemplateData("ass_max_attempts", assMaxAttempts);
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAdminModule_assignment(WebRequestData data, Module module, String assId, MultipartUrlParser mup)
    {
        // Parse assignment identifier
        int assid = mup.parseInt(4, -1);
        if(assid == -1)
            return false;
        // Load the assignment model
        Assignment ass = Assignment.load(data.getConnector(), module, assid);
        if(ass == null)
            return false;
        // Delegate the request
        String page = mup.getPart(5);
        if(page == null)
            return pageAdminModule_assignmentView(data, module, ass, mup);
        else
        {
            switch(page)
            {
                case "edit":
                    return pageAdminModule_assignmentEdit(data, module, ass);
                case "delete":
                    return pageAdminModule_assignmentDelete(data, module, ass);
                case "questions":
                {
                    page = mup.getPart(6);
                    if(page == null)
                        return pageAdminModule_assignment_questions(data, module, ass);
                    else
                    {
                        switch(page)
                        {
                        case "add":
                            return pageAdminModule_assignment_questionAdd(data, module, ass);
                        default:
                            {
                                // Assume it's an assignment-question model
                                AssignmentQuestion aq = AssignmentQuestion.load(data.getCore(), data.getConnector(), ass, mup.parseInt(6, -1));
                                if(aq == null)
                                    return false;
                                switch(mup.getPart(7))
                                {
                                    case "edit":
                                        return pageAdminModule_assignment_questionEdit(data, module, ass, aq);
                                    case "remove":
                                        return pageAdminModule_assignment_questionRemove(data, module, ass, aq);
                                    default:
                                        return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    private boolean pageAdminModule_assignmentView(WebRequestData data, Module module, Assignment ass, MultipartUrlParser mup)
    {
        final int ASSIGNMENTS_PER_PAGE = 10;
        // Parse the current page
        int page = mup.parseInt(5, 1);
        // Fetch the assignments
        InstanceAssignment[] ia = InstanceAssignment.load(data.getConnector(), ass, null, ASSIGNMENTS_PER_PAGE+1, (ASSIGNMENTS_PER_PAGE*page)-ASSIGNMENTS_PER_PAGE);
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle())+" - Assignments - " + Escaping.htmlEncode(ass.getTitle()));
        data.setTemplateData("pals_content", "modules/page_admin_module_assignment");
        // -- Fields
        data.setTemplateData("module", module);
        data.setTemplateData("assignment", ass);
        data.setTemplateData("assignments", ia.length > ASSIGNMENTS_PER_PAGE ? Arrays.copyOf(ia, ASSIGNMENTS_PER_PAGE) : ia);
        data.setTemplateData("page", page);
        if(page < Integer.MAX_VALUE && ia.length > ASSIGNMENTS_PER_PAGE)
            data.setTemplateData("page_next", page+1);
        if(page > 1)
            data.setTemplateData("page_prev", page-1);
        return true;
    }
    private boolean pageAdminModule_assignmentDelete(WebRequestData data, Module module, Assignment ass)
    {
        // Check postback
        RemoteRequest req = data.getRequestData();
        String delete = req.getField("delete");
        if(delete != null && delete.equals("1"))
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else if(!Captcha.isCaptchaCorrect(data))
                data.setTemplateData("error", "Incorrect captcha verification code!");
            else
            {
                // Attempt to unpersist the model
                if(!ass.delete(data.getConnector()))
                    data.setTemplateData("error", "Failed to delete assignment for an unknown reason; please try again or contact an administrator!");
                else
                    data.getResponseData().setRedirectUrl("/admin/modules/"+module.getModuleID()+"/assignments");
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle())+" - Assignments - " + Escaping.htmlEncode(ass.getTitle()) + " - Delete");
        data.setTemplateData("pals_content", "modules/page_admin_module_assignment_delete");
        // -- Fields
        data.setTemplateData("module", module);
        data.setTemplateData("assignment", ass);
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAdminModule_assignmentEdit(WebRequestData data, Module module, Assignment ass)
    {
        // Check postback
        RemoteRequest req = data.getRequestData();
        String assTitle = req.getField("ass_title");
        String assWeight = req.getField("ass_weight");
        String assActive = req.getField("ass_active");
        String assMaxAttempts = req.getField("ass_max_attempts");
        // --- Due-date
        String assDue = req.getField("ass_due"); // Checkbox
        String assDueDay = req.getField("ass_due_day");
        String assDueMonth = req.getField("ass_due_month");
        String assDueYear = req.getField("ass_due_year");
        String assDueHour = req.getField("ass_due_hour");
        String assDueMinute = req.getField("ass_due_minute");
        int year = -1, month = -1, day = -1, hour = -1, minute = -1;
        boolean invalidDueDate = false;
        if(assTitle != null && assWeight != null && assMaxAttempts != null)
        {
            // Parse field data
            // -- Due-date
            DateTime due = null;
            if(assDue != null && assDue.equals("1"))
            {
                try
                {
                    year = Integer.parseInt(assDueYear);
                    month = Integer.parseInt(assDueMonth);
                    day = Integer.parseInt(assDueDay);
                    hour = Integer.parseInt(assDueHour);
                    minute = Integer.parseInt(assDueMinute);
                    due = new DateTime(year, month, day, hour, minute);
                    // Reset the assignment's handle in-case the time/date has changed
                    ass.setDueHandled(false);
                }
                catch(NullPointerException | IllegalArgumentException ex)
                {
                    data.setTemplateData("error", "Invalid due-date!");
                    invalidDueDate = true;
                }
            }
            if(!invalidDueDate)
            {
                // -- Max-attempts
                int maxAttempts;
                try
                {
                    maxAttempts = Integer.parseInt(assMaxAttempts);
                }
                catch(NumberFormatException ex)
                {
                    maxAttempts = 0;
                }
                // -- Weight
                int weight;
                try
                {
                    weight = Integer.parseInt(assWeight);
                }
                catch(NumberFormatException ex)
                {
                    weight = 0;
                }
                // Validate request
                if(!CSRF.isSecure(data))
                    data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
                else
                {
                    try
                    {
                        // Update the model
                        ass.setWeight(Integer.parseInt(assWeight));
                        ass.setTitle(assTitle);
                        ass.setActive(assActive != null);
                        ass.setMaxAttempts(maxAttempts);
                        ass.setDue(due);
                        // Attempt to persist
                        Assignment.PersistStatus aps = ass.persist(data.getConnector());
                        switch(aps)
                        {
                            case Invalid_Module:
                            case Failed:
                                data.setTemplateData("error", "Failed to update assignment for unknown reason ('"+aps.name()+"'); please try again or contact an administrator!");
                                break;
                            case Invalid_Title:
                                data.setTemplateData("error", "Invalid title, must be "+ass.getTitleMin()+" to "+ass.getTitleMax()+" characters in length!");
                                break;
                            case Invalid_Weight:
                                data.setTemplateData("error", "Invalid weight, must be numeric and greater than zero!");
                                break;
                            case Invalid_Due:
                                data.setTemplateData("error", "Invalid due-date, also make sure the time is in the future!");
                                break;
                            case Invalid_MaxAttempts:
                                data.setTemplateData("error", "Invalid max-attempts, must be -1 or greater than zero.");
                                break;
                            case Success:
                                data.setTemplateData("success", "Updated assignment successfully.");
                                break;
                        }
                    }
                    catch(NumberFormatException ex)
                    {
                        data.setTemplateData("error", "Weight must be numeric and greater than zero!");
                    }
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle())+" - Assignments - " + Escaping.htmlEncode(ass.getTitle()) + " - Edit");
        data.setTemplateData("pals_content", "modules/page_admin_module_assignment_edit");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("module", module);
        data.setTemplateData("assignment", ass);
        data.setTemplateData("ass_title", assTitle != null ? assTitle : ass.getTitle());
        data.setTemplateData("ass_weight", assWeight != null ? assWeight : ass.getWeight());
        if((assTitle == null && ass.isActive()) || assActive != null)
            data.setTemplateData("ass_active", true);
        data.setTemplateData("ass_max_attempts", assMaxAttempts != null ? assMaxAttempts : ass.getMaxAttempts());
        // -- -- Due
        if((assTitle != null && assDue != null && assDue.equals("1")) || (assTitle == null && ass.getDue() != null))
            data.setTemplateData("ass_due", true);
        data.setTemplateData("ass_year", DateTime.now().getYear());
        data.setTemplateData("ass_due_year", year != -1 ? year : ass.getDue() != null ? ass.getDue().getYear() : -1);
        data.setTemplateData("ass_due_month", month != -1 ? month : ass.getDue() != null ? ass.getDue().getMonthOfYear() : -1);
        data.setTemplateData("ass_due_day", day != -1 ? day : ass.getDue() != null ? ass.getDue().getDayOfMonth() : -1);
        data.setTemplateData("ass_due_hour", hour != -1 ? hour : ass.getDue() != null ? ass.getDue().getHourOfDay() : -1);
        data.setTemplateData("ass_due_minute", minute != -1 ? minute : ass.getDue() != null ? ass.getDue().getMinuteOfHour() : -1);
        return true;
    }
    private boolean pageAdminModule_assignment_questions(WebRequestData data, Module module, Assignment ass)
    {
        RemoteRequest req = data.getRequestData();
        // Check if we've received postback to bump an item
        String aqid = req.getField("aqid");
        String action = req.getField("action");
        if(aqid != null && action != null)
        {
            try
            {
                // Load the assignment-question
                AssignmentQuestion aq = AssignmentQuestion.load(data.getCore(), data.getConnector(), ass, Integer.parseInt(aqid));
                if(aq != null)
                {
                    // Apply action
                    switch(action)
                    {
                        case "page_up":
                            aq.setPage(aq.getPage()-1);
                            break;
                        case "page_down":
                            aq.setPage(aq.getPage()+1);
                            break;
                        case "order_up":
                            aq.setPageOrder(aq.getPageOrder()-1);
                            break;
                        case "order_down":
                            aq.setPageOrder(aq.getPageOrder()+1);
                            break;
                        default:
                            return false;
                    }
                    // Persist data
                    AssignmentQuestion.PersistStatus aqps = aq.persist(data.getConnector());
                    switch(aqps)
                    {
                        case Failed:
                        case Invalid_Assignment:
                        case Invalid_Question:
                        case Invalid_Weight:
                            data.setTemplateData("error", "Failed to apply action, error '"+(aqps.name())+"'; please try again or contact an administrator!");
                            break;
                        case Invalid_Page:
                        case Invalid_PageOrder:
                        case Success:
                            data.getResponseData().setRedirectUrl("/admin/modules/"+module.getModuleID()+"/assignments/"+ass.getAssID()+"/questions");
                            break;
                    }
                }
                else
                    return false;
            }
            catch(NumberFormatException ex)
            {
                return false;
            }
        }
        // Fetch the assignment-questions
        AssignmentQuestion[] questions = AssignmentQuestion.loadAll(data.getCore(), data.getConnector(), ass);
        // Sum the total weight
        int totalWeight = 0;
        for(AssignmentQuestion q : questions)
            totalWeight += q.getWeight();
        // Set the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle())+" - Assignments - Questions");
        data.setTemplateData("pals_content", "modules/page_admin_assignment_questions");
        // -- Fields
        data.setTemplateData("module", module);
        data.setTemplateData("assignment", ass);
        data.setTemplateData("questions", questions);
        data.setTemplateData("total_weight", totalWeight);
        return true;
    }
    private boolean pageAdminModule_assignment_questionAdd(WebRequestData data, Module module, Assignment ass)
    {
        RemoteRequest req = data.getRequestData();
        // Redirect if no question, to be added, has been specified or the model
        // cannot be loaded
        Question q = null;
        try
        {
            String qid = req.getField("qid");
            if(qid != null)
                q = Question.load(data.getCore(), data.getConnector(), Integer.parseInt(qid));
        }
        catch(NumberFormatException ex)
        {
        }
        if(q == null)
        {
            data.getResponseData().setRedirectUrl("/admin/questions?assid="+ass.getAssID());
            return true;
        }
        // Check for postback
        String qWeight = req.getField("q_weight");
        if(qWeight != null)
        {
            // Validate the request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                try
                {
                    // Create a new assignment-question model
                    AssignmentQuestion aq = new AssignmentQuestion(ass, q, Integer.parseInt(qWeight), 1, 1);
                    // Attempt to persist
                    AssignmentQuestion.PersistStatus aqps = aq.persist(data.getConnector());
                    switch(aqps)
                    {
                        case Failed:
                        case Invalid_Assignment:
                            data.setTemplateData("error", "An unknown error occurred ('"+aqps.name()+"'); please try again or contact an administrator!");
                            break;
                        case Invalid_Question:
                            data.setTemplateData("error", "Invalid question; please try again or select another question!");
                            break;
                        case Invalid_Weight:
                            data.setTemplateData("error", "Invalid weight; must be numeric and greater than zero!");
                            break;
                        case Invalid_Question_Not_Configured:
                            data.setTemplateData("error", "The selected question has not been configured properly; check it has been configured and has criteria!");
                            break;
                        case Success:
                            data.getResponseData().setRedirectUrl("/admin/modules/"+module.getModuleID()+"/assignments/"+ass.getAssID()+"/questions");
                            break;
                    }
                }
                catch(NumberFormatException ex)
                {
                    data.setTemplateData("error", "Invalid weight; must be numeric and greater than zero!");
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle())+" - Assignments - Questions - Add");
        data.setTemplateData("pals_content", "modules/page_admin_assignment_questions_add");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("weight", qWeight);
        data.setTemplateData("question", q);
        data.setTemplateData("assignment", ass);
        data.setTemplateData("module", module);
        data.setTemplateData("q_weight", qWeight);
        return true;
    }
    private boolean pageAdminModule_assignment_questionEdit(WebRequestData data, Module module, Assignment ass, AssignmentQuestion aq)
    {
        // Check for postbaack
        RemoteRequest req = data.getRequestData();
        String questionWeight = req.getField("question_weight");
        String questionPage = req.getField("question_page");
        String questionPageOrder = req.getField("question_page_order");
        if(questionWeight != null && questionPage != null && questionPageOrder != null)
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                boolean error = false;
                int weight = -1, page = -1, pageOrder = -1;
                try
                {
                    weight = Integer.parseInt(questionWeight);
                }
                catch(NumberFormatException ex)
                {
                    error = true;
                    data.setTemplateData("error", "Invalid weight; must be numeric and greater than zero!");
                }
                try
                {
                    page = Integer.parseInt(questionPage);
                }
                catch(NumberFormatException ex)
                {
                    error = true;
                    data.setTemplateData("error", "Invalid page; must be a numeric value!");
                }
                try
                {
                    pageOrder = Integer.parseInt(questionPageOrder);
                }
                catch(NumberFormatException ex)
                {
                    error = true;
                    data.setTemplateData("error", "Invalid page-order; must be a numeric value!");
                }
                if(!error)
                {
                    // Update the model
                    aq.setWeight(weight);
                    aq.setPage(page);
                    aq.setPageOrder(pageOrder);
                    // Attempt to persist
                    AssignmentQuestion.PersistStatus aqps = aq.persist(data.getConnector());
                    switch(aqps)
                    {
                        case Failed:
                        case Invalid_Assignment:
                        case Invalid_Question:
                            data.setTemplateData("error", "An unknown error occurred ('"+aqps.name()+"'); please try again or contact an administrator!");
                            break;
                        case Invalid_Page:
                            data.setTemplateData("error", "Page must be between 1 to "+AssignmentQuestion.PAGE_LIMIT+".");
                            break;
                        case Invalid_PageOrder:
                            data.setTemplateData("error", "Page order must be between 1 to "+AssignmentQuestion.PAGE_ORDER_LIMIT+".");
                            break;
                        case Invalid_Weight:
                            data.setTemplateData("error", "Weight must be greater than zero.");
                            break;
                        case Success:
                            data.setTemplateData("success", "Successfully updated settings.");
                            break;
                    }
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle())+" - Assignments - Questions - Edit");
        data.setTemplateData("pals_content", "modules/page_admin_assignment_questions_edit");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("module", module);
        data.setTemplateData("assignment", ass);
        data.setTemplateData("question", aq);
        data.setTemplateData("question_weight", questionWeight != null ? questionWeight : aq.getWeight());
        data.setTemplateData("question_page", questionPage != null ? questionPage : aq.getPage());
        data.setTemplateData("question_page_order", questionPageOrder != null ? questionPageOrder : aq.getPageOrder());
        return true;
    }
    private boolean pageAdminModule_assignment_questionRemove(WebRequestData data, Module module, Assignment ass, AssignmentQuestion aq)
    {
        // Check for postbaack
        RemoteRequest req = data.getRequestData();
        String delete = req.getField("delete");
        if(delete != null && delete.equals("1"))
        {
            // Validate the request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Unpersist the model
                if(aq.delete(data.getConnector()))
                    data.getResponseData().setRedirectUrl("/admin/modules/"+module.getModuleID()+"/assignments/"+ass.getAssID()+"/questions");
                else
                    data.setTemplateData("error", "Failed to unpersist model for an unknown reason!");
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle())+" - Assignments - Questions - Remove");
        data.setTemplateData("pals_content", "modules/page_admin_assignment_questions_remove");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("module", module);
        data.setTemplateData("assignment", ass);
        data.setTemplateData("question", aq);
        return true;
    }
}
