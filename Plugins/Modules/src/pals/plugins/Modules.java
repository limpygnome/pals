package pals.plugins;

import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.WebManager;
import pals.base.assessment.Module;
import pals.base.auth.User;
import pals.base.utils.JarIO;
import pals.base.web.MultipartUrlParser;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.base.web.security.Escaping;
import pals.plugins.web.Captcha;

/**
 * The default interface for modules.
 */
public class Modules extends Plugin
{
    // Methods - Constructors **************************************************
    public Modules(NodeCore core, UUID uuid, JarIO jario, Settings settings, String jarPath)
    {
        super(core, uuid, jario, settings, jarPath);
    }
    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginInstall(NodeCore core)
    {
        return true;
    }
    @Override
    public boolean eventHandler_pluginUninstall(NodeCore core)
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
        MultipartUrlParser mup = new MultipartUrlParser(data);
        switch(mup.getPart(0))
        {
            case "modules":
                if(mup.getPart(1) == null)
                    // Overview of all modules belonging to a user
                    return pageModules(data);
                else
                    // Overview of a specific module
                    return pageModule(data, mup);
            case "admin":
                switch(mup.getPart(1))
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
                            // Overview of a specific module
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
    // Methods - Pages *********************************************************
    private boolean pageModules(WebRequestData data)
    {
        if(data.getUser() == null)
            return false;
        // Setup the page
        data.setTemplateData("pals_title", "Modules");
        data.setTemplateData("pals_content", "modules/page_modules");
        return true;
    }
    private boolean pageModule(WebRequestData data, MultipartUrlParser mup)
    {
        User user = data.getUser();
        if(user == null)
            return false;
        // Fetch the module
        // Check the user is enrolled
        // Setup the page
        data.setTemplateData("pals_title", "Module - ");
        data.setTemplateData("pals_content", "modules/page_module");
        return true;
    }
    private boolean pageAdminModules(WebRequestData data)
    {
        User user = data.getUser();
        if(user == null || !user.getGroup().isAdminModules())
            return false;
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Modules");
        data.setTemplateData("pals_content", "modules/page_admin_modules");
        data.setTemplateData("modules", Module.loadAll(data.getConnector()));
        return true;
    }
    private boolean pageAdminModule_create(WebRequestData data)
    {
        User user = data.getUser();
        if(user == null || !user.getGroup().isAdminModules())
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
                Module m = Module.create();
                m.setTitle(moduleTitle);
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
        User user = data.getUser();
        if(user == null || !user.getGroup().isAdminModules())
            return false;
        // Load the module's information
        int moduleid = mup.parseInt(2, -1);
        if(moduleid == -1)
            return false;
        Module module = Module.load(data.getConnector(), moduleid);
        if(module == null)
            return false;
        // Handle the page
        String page = mup.getPart(3);
        if(page == null)
            return pageAdminModule_view(data, mup, module);
        else
        {
            switch(page)
            {
                case "assignments":
                    return pageAdminModule_assignments(data, mup, module);
                case "enrollment":
                    return pageAdminModule_enrollment(data, mup, module);
                case "delete":
                    return pageAdminModule_delete(data, mup, module);
                default:
                    return false;
            }
        }
    }
    private boolean pageAdminModule_view(WebRequestData data, MultipartUrlParser mup, Module module)
    {
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Module - "+Escaping.htmlEncode(module.getTitle()));
        data.setTemplateData("pals_content", "modules/page_admin_module");
        data.setTemplateData("module", module);
        // -- Assignments
        
        // -- Module Users
        data.setTemplateData("module_users", module.usersEnrolled(data.getConnector()));
        return true;
    }
    private boolean pageAdminModule_delete(WebRequestData data, MultipartUrlParser mup, Module module)
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
    private boolean pageAdminModule_enrollment(WebRequestData data, MultipartUrlParser mup, Module module)
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
    private boolean pageAdminModule_assignments(WebRequestData data, MultipartUrlParser mup, Module module)
    {
        return true;
    }
}
