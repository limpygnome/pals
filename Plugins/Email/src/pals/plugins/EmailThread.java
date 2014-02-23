package pals.plugins;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.joda.time.DateTime;
import pals.base.Logging;
import pals.base.SettingsException;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
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
        int maxAttempts;
        try
        {
            host = p.getCore().getSettings().get2("email/host");
            port = p.getCore().getSettings().get2("email/port");
            username = p.getCore().getSettings().get2("email/username");
            password = p.getCore().getSettings().get2("email/password");
            from = p.getCore().getSettings().get2("email/from");
            ssl = p.getCore().getSettings().get2("email/ssl");
            maxAttempts = p.getSettings().get2("max_send_attempts");
        }
        catch(SettingsException ex)
        {
            p.getCore().getLogging().logEx(EmailSender.LOGGING_ALIAS, ex, Logging.EntryType.Error);
            return;
        }
        final String finUser = username;
        final String finPass = password;
        // Fetch intervals
        int interval = p.getSettings().getInt("poll_queue_ms", 30000);
        int resend = p.getSettings().getInt("resend_ms", 120000);
        // Create connector
        Connector conn = p.getCore().createConnector();
        // Loop until stopped
        Email[] em;
        while(!extended_isStopped())
        {
            System.err.flush();
            try
            {
                // Lock email table
                conn.tableLock("pals_email_queue", false);
                // Fetch next models to send
                em = Email.loadSendNext(conn, resend, 10);
                // Update all the models as handled by us
                // -- O(n^2) to send all the e-mails, but we assume sending each e-mail will take a lot longer due to contacting server possibly outside the network
                // -- -- Thus cheaper, time-wise, in reality because multiple nodes will have higher-throughput if we dont lock the table when sending the e-mails
                for(Email e : em)
                    conn.execute("UPDATE pals_email_queue SET last_attempted=current_timestamp WHERE emailid=?;", e.getEmailID());
                // Unlock table
                conn.tableUnlock(false);
                // Send each email
                for(Email e : em)
                {
                    try
                    {
                        // Setup host configuration
                        Properties props = new Properties();
                        if(ssl)
                        {
                            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
                            props.put("mail.smtp.socketFactory.class", javax.net.ssl.SSLSocketFactory.class.getName());
                        }
                        props.put("mail.smtp.starttls.enable", "true");
                        props.put("mail.smtp.auth", "true");
                        props.put("mail.smtp.host", host);
                        props.put("mail.smtp.port", String.valueOf(port));
                        // Create session
                        Session session = Session.getInstance(props, new Authenticator()
                        {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication()
                            {
                                return new PasswordAuthentication(finUser, finPass);
                            }
                        });
                        // Create message
                        MimeMessage msg = new MimeMessage(session);
                        // -- From/to info
                        msg.setFrom(new InternetAddress(from));
                        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(e.getDestination()));
                        // -- Content
                        msg.setSubject(e.getTitle());
                        msg.setContent(e.getContent(), "text/html; charset=utf-8");
                        Transport.send(msg);
                        // Delete model and log event
                        e.delete(conn);
                        p.getCore().getLogging().log(EmailSender.LOGGING_ALIAS, "E-mail sent to '"+e.getDestination()+"'.", Logging.EntryType.Info);
                    }
                    catch(MessagingException ex)
                    {
                        p.getCore().getLogging().logEx(EmailSender.LOGGING_ALIAS, ex, Logging.EntryType.Warning);
                        e.setAttemptsIncrement();
                        if(e.getAttempts() > maxAttempts)
                            e.delete(conn);
                        else
                        {
                            e.setLastAttempted(DateTime.now());
                            e.persist(conn);
                        }
                    }
                }
            }
            catch(DatabaseException ex)
            {
                p.getCore().getLogging().logEx(EmailSender.LOGGING_ALIAS, ex, Logging.EntryType.Warning);
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
