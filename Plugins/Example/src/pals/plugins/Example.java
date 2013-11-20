package pals.plugins;

import pals.base.NodeCore;
import pals.base.UUID;

/**
 *
 * @author limpygnome
 */
public class Example extends pals.base.Plugin
{
    public Example(UUID uuid)
    {
        super(uuid);
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
}
