package pals.base;

import pals.base.database.Connector;

/**
 *
 * @author limpygnome
 */
public class Plugin
{
    // plugins are based in a directory
    // -- therefore when we distribute a plugin, we distribute the entire directory?
    
    
    public String test()
    {
        return "default";
    }
    public String test(int a, int b)
    {
        return "default";
    }
    
    
    // events -> hash-table -> <name> -> plugins; with eventHandler_<name> invoked?
    // name -> priorityqueue
    
    // -- mandatory
    public boolean eventHandler_pluginStart(Core core, Connector connector)
    {
        return false;
    }
    public boolean eventHandler_pluginStop(Core core)
    {
        return false;
    }
    public boolean eventHandler_pluginInstall()
    {
        return false;
    }
    public boolean eventHandler_pluginUninstall()
    {
        return false;
    }
    
    
    // -- optional + ideas currently
    public boolean eventHandler_pluginAction(Core core)
    {
        return false;
    }
    
    public boolean eventHandler_webRequestStart(Core core)
    {
        return false;
    }
    public boolean eventHandler_webRequestEnd(Core core)
    {
        return false;
    }
    public boolean eventHandler_webRequestHandle(Core core)
    {
        return false;
    }
    
    public boolean eventHandler_questionType_Render(Core core)
    {
        return false;
    }
    public boolean eventHandler_questionType_NewQuestionWizard(Core core)
    {
        return false;
    }
}
