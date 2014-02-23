package pals.plugins;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.DateTime;
import pals.base.Logging;
import pals.base.SettingsException;
import pals.base.database.Connector;
import pals.base.utils.ExtendedThread;
import pals.base.web.Email;

/**
 * Responsible for periodically sending e-mail.
 */
public class EmailThread extends ExtendedThread
{
    // Fields ******************************************************************
    private EmailSender p;
    // Methods - Constructors **************************************************
    public EmailThread(EmailSender p)
    {
        this.p = p;
    }
    // Methods *****************************************************************
    @Override
    public void run()
    {
        // Fetch host/auth settings
        String host, username, password, from;
        int port;
        boolean ssl;
        try
        {
            host = p.getCore().getSettings().get2("email/host");
            port = p.getCore().getSettings().get2("email/port");
            username = p.getCore().getSettings().get2("email/username");
            password = p.getCore().getSettings().get2("email/password");
            from = p.getCore().getSettings().get2("email/from");
            ssl = p.getCore().getSettings().get2("email/ssl");
        }
        catch(SettingsException ex)
        {
            p.getCore().getLogging().logEx(EmailSender.LOGGING_ALIAS, ex, Logging.EntryType.Error);
            return;
        }
        // Fetch intervals
        int interval = p.getSettings().getInt("poll_queue_ms", 30000);
        int resend = p.getSettings().getInt("resend_ms", 120000);
        // Create connector
        Connector conn = p.getCore().createConnector();
        // Loop until stopped
        Email[] em;
        while(!extended_isStopped())
        {
            // Fetch next models to send
            em = Email.loadSendNext(conn, resend, 10);
            // Send each email
            for(Email e : em)
            {
                try
                {
                    org.apache.commons.mail.Email msg = new SimpleEmail();
                    msg.setHostName(host);
                    msg.setSmtpPort(port);
                    msg.setAuthentication(username, password);
                    if(ssl)
                        msg.setSSLOnConnect(true);
                    msg.setFrom(from);
                    msg.setSubject(e.getTitle());
                    msg.setMsg(e.getContent());
                    msg.addTo(e.getDestination());
                    msg.send();
                    e.delete(conn);
                    p.getCore().getLogging().log(EmailSender.LOGGING_ALIAS, "E-mail sent to '"+e.getDestination()+"'.", Logging.EntryType.Info);
                }
                catch(EmailException ex)
                {
                    p.getCore().getLogging().logEx(EmailSender.LOGGING_ALIAS, ex, Logging.EntryType.Warning);
                    e.setAttemptsIncrement();
                    e.setLastAttempted(DateTime.now());
                    e.persist(conn);
                }
            }
            // Sleep...
            try
            {
                sleep(interval);
            }
            catch(InterruptedException ex)
            {
            }
        }
    }
}
