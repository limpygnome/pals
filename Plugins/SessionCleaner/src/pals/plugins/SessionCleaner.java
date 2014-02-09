package pals.plugins;

import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.UUID;
import pals.base.Version;
import pals.base.database.Connector;
import pals.base.utils.JarIO;

/**
 * Cleans up old sessions and temporary web (upload) files.
 */
public class SessionCleaner extends Plugin
{
    // Constants ***************************************************************
    static final String    LOGGING_ALIAS = "Session Cleaner";
    // Fields ******************************************************************
    private SessionCleanerThread thread;
    // Methods - Constructors **************************************************
    public SessionCleaner(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
    {
        super(core, uuid, jario, version, settings, jarPath);
        thread = new SessionCleanerThread(this);
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
        // Start thread
        thread.start();
        core.getLogging().log(LOGGING_ALIAS, "Started cleaner thread.", Logging.EntryType.Info);
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Stop thread
        try
        {
            core.getLogging().log(LOGGING_ALIAS, "Stopping cleaner thread...", Logging.EntryType.Info);
            thread.extended_stop();
            thread.join();
            core.getLogging().log(LOGGING_ALIAS, "Stopped cleaner thread.", Logging.EntryType.Info);
        }
        catch(InterruptedException ex)
        {
        }
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
