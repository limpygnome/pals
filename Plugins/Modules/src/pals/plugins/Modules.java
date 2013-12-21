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

/**
 * The default interface for modules.
 */
public class Modules extends Plugin
{
    // Fields ******************************************************************
    
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
        // -- Set fields
        
        return true;
    }
    private boolean pageAdminModule_delete(WebRequestData data, MultipartUrlParser mup, Module module)
    {
        return true;
    }
    private boolean pageAdminModule_enrollment(WebRequestData data, MultipartUrlParser mup, Module module)
    {
        return true;
    }
    private boolean pageAdminModule_assignments(WebRequestData data, MultipartUrlParser mup, Module module)
    {
        return true;
    }
}
