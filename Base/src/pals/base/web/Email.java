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
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base.web;

import java.sql.Timestamp;
import java.util.ArrayList;
import org.joda.time.DateTime;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model which represents an e-mail in the e-mail queue.
 * 
 * @version 1.0
 */
public class Email
{
    // Fields ******************************************************************
    private int         emailid;
    private String      title,
                        content,
                        destination;
    private DateTime    lastAttempted;
    private int        attempts;
    // Methods - Constructors **************************************************
    /**
     * Creates a new unpersisted model.
     * 
     * @since 1.0
     */
    public Email()
    {
        this(null, null, null, null, 0);
        this.emailid = -1;
    }
    /**
     * Creates a new unpersisted model.
     * 
     * @param title The title of the e-mail.
     * @param content The content of the e-mail.
     * @param destination The destination e-mail address.
     * @since 1.0
     */
    public Email(String title, String content, String destination)
    {
        this(title, content, destination, null, 0);
    }
    /**
     * Creates a new unpersisted model.
     * 
     * @param title The title of the e-mail.
     * @param content The content of the e-mail.
     * @param destination The destination e-mail address.
     * @param lastAttempted The date-time of the last attempt to send the e-mail.
     * @param attempts The number of attempts of sending the e-mail.
     * @since 1.0
     */
    public Email(String title, String content, String destination, DateTime lastAttempted, int attempts)
    {
        this.emailid = -1;
        this.title = title;
        this.content = content;
        this.destination = destination;
        this.lastAttempted = lastAttempted;
        this.attempts = attempts;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads a model.
     * 
     * @param conn Database connector.
     * @param emailid The identifier of the model.
     * @return An instance of a model or null.
     * @since 1.0
     */
    public static Email load(Connector conn, int emailid)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_email_queue WHERE emailid=?;", emailid);
            return res.next() ? load(res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads multiple models.
     * 
     * @param conn Database connector.
     * @param limit The number of models to retrieve at a time.
     * @param offset The offset of models to skip.
     * @return Array of models; can be empty.
     * @since 1.0
     */
    public static Email[] load(Connector conn, int limit, int offset)
    {
        try
        {
            return loadArr(conn.read("SELECT * FROM pals_email_queue ORDER BY emailid ASC LIMIT ? OFFSET ?;", limit, offset));
        }
        catch(DatabaseException ex)
        {
            return new Email[0];
        }
    }
    /**
     * Loads the next emails to send.
     * 
     * @param conn Database connector.
     * @param ms The threshold for resending failed e-mails, in milliseconds.
     * @param limit The maximum number of e-mails to fetch.
     * @return Array of models; can be empty.
     * @since 1.0
     */
    public static Email[] loadSendNext(Connector conn, int ms, int limit)
    {
        try
        {
            return loadArr(conn.read("SELECT * FROM pals_email_queue WHERE (last_attempted IS NULL OR last_attempted < current_timestamp-CAST(? AS INTERVAL)) ORDER BY emailid DESC LIMIT ?;", ms+" milliseconds", limit));
        }
        catch(DatabaseException ex)
        {
            return new Email[0];
        }
    }
    /**
     * Loads multiple results.
     * 
     * @param res The result data.
     * @return Array of models; can be empty.
     * @since 1.0
     */
    public static Email[] loadArr(Result res)
    {
        try
        {
            ArrayList<Email> buffer = new ArrayList<>();
            Email t;
            while(res.next())
            {
                if((t = load(res)) != null)
                    buffer.add(t);
            }
            return buffer.toArray(new Email[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new Email[0];
        }
    }
    /**
     * Loads a single model.
     * 
     * @param res The result data; next() should be invoked.
     * @return An instance of the model or null.
     * @since 1.0
     */
    public static Email load(Result res)
    {
        try
        {
            Object t = res.get("last_attempted");
            Email e = new Email((String)res.get("title"), (String)res.get("content"), (String)res.get("destination"), t != null ? new DateTime(t) : null, (int)res.get("attempts"));
            e.emailid = (int)res.get("emailid");
            return e;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Deletes any old models.
     * 
     * @param conn Database connector.
     * @param maxAttempts The maximum attempts allowed; if a model surpasses
     * this value, it is deleted.
     * @return True = success, false = failed.
     * @since 1.0
     */
    public static boolean deleteOld(Connector conn, int maxAttempts)
    {
        try
        {
            conn.execute("DELETE FROM pals_email_queue WHERE attempts > ?;", maxAttempts);
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    /**
     * Unpersists the model.
     * 
     * @param conn Database connector.
     * @return True = successful, false = failed.
     * @since 1.0
     */
    public boolean delete(Connector conn)
    {
        try
        {
            if(emailid != -1)
            {
                conn.execute("DELETE FROM pals_email_queue WHERE emailid=?;", emailid);
                emailid = -1;
            }
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    /**
     * Deletes all of the e-mails pending in the queue.
     * 
     * @param conn Database connector.
     * @return True = successful, false = failed.
     * @since 1.0
     */
    public static boolean deleteAll(Connector conn)
    {
        try
        {
            conn.execute("DELETE FROM pals_email_queue;");
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    /**
     * Persists the model.
     * 
     * @param conn Database connector.
     * @return True = successful, false = failed.
     * @since 1.0
     */
    public boolean persist(Connector conn)
    {
        try
        {
            if(emailid == -1)
                emailid = (int)conn.executeScalar("INSERT INTO pals_email_queue (title, content, destination, last_attempted, attempts) VALUES(?,?,?,?,?) RETURNING emailid;",
                        title,
                        content,
                        destination,
                        lastAttempted == null ? null : new Timestamp(lastAttempted.toDate().getTime()),
                        attempts
                );
            else
                conn.execute("UPDATE pals_email_queue SET title=?, content=?, destination=?, last_attempted=?, attempts=? WHERE emailid=?;",
                        title,
                        content,
                        destination,
                        lastAttempted == null ? null : new Timestamp(lastAttempted.toDate().getTime()),
                        attempts,
                        emailid
                );
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the title of the e-mail.
     * 
     * @param title The title.
     * @since 1.0
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * Sets the content of the e-mail.
     * 
     * @param content The content.
     * @since 1.0
     */
    public void setContent(String content)
    {
        this.content = content;
    }
    /**
     * Sets the e-mail destination.
     * 
     * @param destination Destination e-mail.
     * @since 1.0
     */
    public void setDestination(String destination)
    {
        this.destination = destination;
    }
    /**
     * Sets the date-time of the last send attempt.
     * 
     * @param lastAttempted The date-time.
     * @since 1.0
     */
    public void setLastAttempted(DateTime lastAttempted)
    {
        this.lastAttempted = lastAttempted;
    }
    /**
     * Sets the number of attempts of sending the e-mail.
     * 
     * @param attempts The attempts.
     * @since 1.0
     */
    public void setAttempts(int attempts)
    {
        this.attempts = attempts;
    }
    /**
     * Increments the attempts by one.
     * 
     * @since 1.0
     */
    public void setAttemptsIncrement()
    {
        attempts++;
    }
    // Methods - Accessors *****************************************************
    /**
     * The e-mail identifier.
     * 
     * @return The identifier of the e-mail; -1 if the model has not been
     * persisted.
     * @since 1.0
     */
    public int getEmailID()
    {
        return emailid;
    }
    /**
     * Retrieves the title of the e-mail.
     * 
     * @return The title.
     * @since 1.0
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * Retrieves the content of the e-mail.
     * 
     * @return The content.
     * @since 1.0
     */
    public String getContent()
    {
        return content;
    }
    /**
     * Retrieves the destination e-mail address.
     * 
     * @return The destination e-mail.
     * @since 1.0
     */
    public String getDestination()
    {
        return destination;
    }
    /**
     * Date and time of when the last send attempt.
     * 
     * @return The date-time.
     * @since 1.0
     */
    public DateTime getLastAttempted()
    {
        return lastAttempted;
    }
    /**
     * The number of attempts of sending the e-mail.
     * 
     * @return The number of attempts.
     * @since 1.0
     */
    public long getAttempts()
    {
        return attempts;
    }    
}
