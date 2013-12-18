package pals.plugins;

import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.UUID;

/**
 * Cleans up old sessions and temporary web (upload) files.
 */
public class SessionCleaner extends Plugin
{
    // Fields ******************************************************************
    private SessionCleanerThread thread;
    // Methods - Constructors **************************************************
    public SessionCleaner(NodeCore core, UUID uuid, Settings settings, String jarPath)
    {
        super(core, uuid, settings, jarPath);
        thread = new SessionCleanerThread(this);
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
        // Start thread
        thread.start();
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Stop thread
        thread.stopRunning();
    }
    @Override
    public String getTitle()
    {
        return "PALS: Session Cleaner";
    }
    @Override
    public boolean isSystemPlugin()
    {
        return true;
    }
}
