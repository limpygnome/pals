package pals.plugins;

import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.UUID;

/**
 * A very simple plugin to update the active-time, periodically, of a node in
 * the database.
 */
public class NodeActive extends Plugin
{
    // Fields ******************************************************************
    private NodeActiveThread nat;
    // Methods - Constructor ***************************************************
    public NodeActive(NodeCore core, UUID uuid, Settings settings, String jarPath)
    {
        super(core, uuid, settings, jarPath);
        nat = new NodeActiveThread(this);
    }
    @Override
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        // Setup thread to update the database
        nat.start();
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Dispose thread
        nat.stopRunning();
    }
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
    public String getTitle()
    {
        return "PALS: Node Active";
    }
}
