package pals.plugins;

import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.Version;
import pals.base.WebManager;
import pals.base.assessment.Assignment;
import pals.base.assessment.Module;
import pals.base.assessment.Question;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.web.MultipartUrlParser;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.base.web.security.Escaping;
import pals.plugins.stats.ModelException;
import pals.plugins.stats.ModelExceptionClass;

/**
 * A plugin for logging statistical data and provide feedback for errors during
 * compilation and runtime.
 */
public class Stats extends Plugin
{
    // Methods - Constructors **************************************************
    public Stats(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
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
    public boolean eventHandler_registerTemplates(NodeCore core, TemplateManager manager)
    {
        if(!manager.load(this, "templates"))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        if(!core.getWebManager().urlsRegister(this, new String[]{
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
        return "PALS: Stats";
    }
    // Methods - Pages *********************************************************
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
        String  type = req.getField("type"),
                rawTid = req.getField("tid"),
                clear = req.getField("clear"),
                rawPage = req.getField("page"),
                hint = req.getField("hint");
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
        // Check for request operations
        boolean csrf = CSRF.isSecure(data);
        // -- Check if to update hint
        if(hint != null && csrf)
        {
            if(!ModelExceptionClass.persistHint(data.getConnector(), hint, ecid))
                data.setTemplateData("error", "Unable to update hint.");
            else
                data.setTemplateData("success", "Updated hint.");
        }
        // -- Check if to clear data
        boolean doClear = clear != null && clear.equals("1") && csrf;
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
        data.setTemplateData("hint", hint != null ? hint : ec.getHint());
        return true;
    }
}
