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

import java.util.Arrays;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.PluginManager;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.Version;
import pals.base.WebManager;
import pals.base.database.Connector;
import pals.base.utils.ExtendedThread;
import pals.base.utils.JarIO;
import pals.base.utils.Misc;
import pals.base.web.Email;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.base.web.security.Escaping;

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
        // Dispose URLs
        core.getWebManager().urlsUnregister(this);
        // Dispose templates
        core.getTemplates().remove(this);
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
        // Dispose hook
        core.getPlugins().globalHookUnregister(this);
    }
    @Override
    public boolean eventHandler_registerHooks(NodeCore core, PluginManager plugins)
    {
        if(!plugins.globalHookRegister(this, "base.web.email.wake"))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_handleHook(String event, Object[] args)
    {
        switch(event)
        {
            case "base.web.email.wake":
                thQueue.interrupt();
                return true;
        }
        return false;
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
        if(!web.urlsRegister(this, new String[]{
            "admin/email"
        }))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        // Check the user is a system-admin
        if(data.getUser() == null || !data.getUser().getGroup().isAdminSystem())
            return false;
        // Delete request
        switch(data.getRequestData().getRelativeUrl())
        {
            case "admin/email":
                return pageAdminEmails(data);
        }
        return false;
    }
    @Override
    public String getTitle()
    {
        return "PALS: E-mail Sender";
    }
    // Methods - Pages *********************************************************
    private boolean pageAdminEmails(WebRequestData data)
    {
        final int EMAILS_PER_PAGE = 15;
        RemoteRequest req = data.getRequestData();
        // Parse page
        int page = Misc.parseInt(req.getField("page"), 1);
        if(page < 1)
            page = 1;
        // Check for postback
        String  deleteAll = req.getField("delete_all"),
                delete = req.getField("delete");
        if(deleteAll != null || delete != null)
        {
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator.");
            else if(deleteAll != null && deleteAll.equals("1"))
                Email.deleteAll(data.getConnector());
            else if(delete != null)
            {
                try
                {
                    Email e = Email.load(data.getConnector(), Integer.parseInt(delete));
                    if(e != null)
                        e.delete(data.getConnector());
                }
                catch(NumberFormatException ex)
                {
                }
            }
            data.getResponseData().setRedirectUrl("/admin/email");
        }
        // Fetch models
        Email[] emails = Email.load(data.getConnector(), EMAILS_PER_PAGE+1, (page*EMAILS_PER_PAGE)-EMAILS_PER_PAGE);
        // Setup the page
        data.setTemplateData("pals_title", "Admin - E-mails");
        data.setTemplateData("pals_content", "emailqueue/list");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("emails", emails.length > EMAILS_PER_PAGE ? Arrays.copyOf(emails, EMAILS_PER_PAGE) : emails);
        data.setTemplateData("email_page", page);
        if(page > 1)
            data.setTemplateData("email_prev", page-1);
        if(page < Integer.MAX_VALUE && emails.length > EMAILS_PER_PAGE)
            data.setTemplateData("email_next", page+1);
        return true;
    }
}
