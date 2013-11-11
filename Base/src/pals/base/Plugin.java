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
    public boolean eventHandler_pluginStart(NodeCore core, Connector connector)
    {
        return false;
    }
    public boolean eventHandler_pluginSNodeCoreCore core)
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
    public boolean eventHandler_plugiNodeCoreion(Core core)
    {
        return false;
    }
    
    public boolean eventHandler_webRNodeCorestStart(Core core)
    {
        return false;
    }
    public boolean eventHandleNodeCorebRequestEnd(Core core)
    {
        return false;
    }
    public boolean eventHandlNodeCoreebRequestHandle(Core core)
    {
        return false;
    }
    
    public boolean eventHandNodeCorequestionType_Render(Core core)
    {
        return false;
    }
    public boolean eventHandler_queNodeCorenType_NewQuestionWizard(Core core)
    {
        return false;
    }
}
