package pals.plugins;

import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.UUID;
import pals.base.Version;
import pals.base.database.Connector;
import pals.base.utils.ExtendedThread;
import pals.base.utils.JarIO;

/**
 * A plugin responsible for sending e-mail from the e-mail queue.
 */
public class EmailSender extends Plugin
{
    // Constants ***************************************************************
    protected static final String LOGGING_ALIAS = "E-mail Sender";
    // Fields ******************************************************************
    private ExtendedThread  thQueue;
    // Methods - Constructors **************************************************
    public EmailSender(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
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
        // Start thread
        thQueue = new EmailThread(this);
        thQueue.start();
        core.getLogging().log(LOGGING_ALIAS, "Started sending thread.", Logging.EntryType.Info);
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Stop thread
        core.getLogging().log(LOGGING_ALIAS, "Shutting down thread...", Logging.EntryType.Info);
        thQueue.extended_stop();
        try
        {
            thQueue.join();
        }
        catch(InterruptedException ex)
        {
        }
        core.getLogging().log(LOGGING_ALIAS, "Thread stopped.", Logging.EntryType.Info);
    }
}
