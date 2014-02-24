/*
    The MIT License (MIT)

    Copyright (c) 2014 Marcus Craske <limpygnome@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    ----------------------------------------------------------------------------
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
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
